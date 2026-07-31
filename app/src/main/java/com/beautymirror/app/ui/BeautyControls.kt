package com.beautymirror.app.ui

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.beautymirror.app.BuildConfig
import com.beautymirror.app.R
import com.beautymirror.app.ota.OtaController
import com.beautymirror.app.ota.OtaPreferences
import com.beautymirror.app.ota.UpdateStatus
import com.beautymirror.app.rendering.FrameTimingCollector
import com.beautymirror.app.settings.AdaptivePerformanceState
import com.beautymirror.app.settings.AppLanguage
import com.beautymirror.app.settings.BeautyPreset
import com.beautymirror.app.settings.BeautySettings
import com.beautymirror.app.settings.LanguagePreferences
import com.beautymirror.app.settings.QualityLevel
import com.beautymirror.app.settings.QuickFixSession
import com.beautymirror.app.settings.ReflectionScene
import androidx.compose.runtime.DisposableEffect
import com.beautymirror.app.ui.theme.BmAccent
import com.beautymirror.app.ui.theme.BmBg
import com.beautymirror.app.ui.theme.BmDanger
import com.beautymirror.app.ui.theme.BmSurface
import com.beautymirror.app.ui.theme.BmSurfaceStrong
import com.beautymirror.app.ui.theme.BmText
import com.beautymirror.app.ui.theme.BmTextMuted

private enum class StudioPage(
    @StringRes val labelRes: Int,
    val focus: BeautyFocus,
) {
    LOOKS(R.string.page_quick, BeautyFocus.OVERVIEW),
    SKIN(R.string.page_skin, BeautyFocus.SKIN),
    EYES(R.string.page_eyes, BeautyFocus.EYES),
    LIPS(R.string.page_lips, BeautyFocus.LIPS),
    SHAPE(R.string.page_shape, BeautyFocus.SHAPE),
    SCENE(R.string.page_scene, BeautyFocus.SCENE),
    SYSTEM(R.string.page_system, BeautyFocus.SYSTEM),
}

@Composable
fun BeautyControls(
    settings: BeautySettings,
    runtimeQuality: QualityLevel,
    performanceState: AdaptivePerformanceState,
    timing: FrameTimingCollector.Snapshot?,
    onChange: (BeautySettings) -> Unit,
    onDismiss: () -> Unit,
    onFocusChange: (BeautyFocus) -> Unit = {},
    otaController: OtaController? = null,
    /** Hoist above dismiss so one-tap toggles survive Done → reopen. */
    quickFixSession: QuickFixSession? = null,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableStateOf(StudioPage.LOOKS) }
    val context = LocalContext.current
    val language = LanguagePreferences.get(context)
    val ownedQuickFixes = remember { QuickFixSession() }
    val quickFixes = quickFixSession ?: ownedQuickFixes
    var activeFixes by remember(quickFixes) { mutableStateOf(quickFixes.activeIds) }
    var autoUpdate by remember {
        mutableStateOf(OtaPreferences.isAutoUpdateEnabled(context))
    }
    var otaStatus by remember { mutableStateOf(otaController?.updateService?.status ?: UpdateStatus.Idle) }
    DisposableEffect(otaController) {
        val service = otaController?.updateService
        if (service == null) {
            onDispose { }
        } else {
            val listener: (UpdateStatus) -> Unit = { otaStatus = it }
            service.onStatus = listener
            otaStatus = service.status
            onDispose {
                if (service.onStatus === listener) service.onStatus = null
            }
        }
    }

    fun preserveExperience(target: BeautySettings): BeautySettings = target.copy(
        qualityLevel = settings.qualityLevel,
        debugOverlay = settings.debugOverlay,
        mirrorPreview = settings.mirrorPreview,
        reflectionScene = settings.reflectionScene,
        lakeIntensity = settings.lakeIntensity,
        lakeMotion = settings.lakeMotion,
        lakeDarkness = settings.lakeDarkness,
        lakeFaceClarity = settings.lakeFaceClarity,
    )

    fun custom(block: BeautySettings.() -> BeautySettings) {
        onChange(settings.block().copy(preset = BeautyPreset.CUSTOM).clamped())
    }

    fun clearQuickFixes() {
        quickFixes.clear()
        activeFixes = emptySet()
    }

    fun toggleFix(id: String, apply: (BeautySettings) -> BeautySettings) {
        val next = quickFixes.toggle(id, settings, apply)
        activeFixes = quickFixes.activeIds
        // Keep restored baseline preset when the stack is empty (e.g. OFF → fix → undo).
        val committed = if (activeFixes.isEmpty()) {
            preserveExperience(next)
        } else {
            preserveExperience(next).copy(preset = BeautyPreset.CUSTOM)
        }
        onChange(committed.clamped())
    }

    fun applyLakeMood(intensity: Float, motion: Float, darkness: Float, clarity: Float) {
        onChange(
            settings.copy(
                reflectionScene = ReflectionScene.DARK_LAKE,
                lakeIntensity = intensity,
                lakeMotion = motion,
                lakeDarkness = darkness,
                lakeFaceClarity = clarity,
            ).clamped(),
        )
    }

    val panelHeight = (LocalConfiguration.current.screenHeightDp * 0.62f)
        .coerceIn(460f, 700f)
        .dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
            .background(BmSurface)
            .clickable(onClick = {})
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("beauty_studio"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(BmTextMuted.copy(alpha = 0.28f)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.beauty_studio), color = BmText, fontSize = 17.sp)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
            ) {
                Text(stringResource(R.string.done), color = BmAccent)
            }
        }

        StudioNavigation(
            selected = page,
            onSelected = {
                page = it
                onFocusChange(it.focus)
            },
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (page) {
                StudioPage.LOOKS -> {
                    PresetSelector(
                        selected = settings.preset,
                        onSelect = {
                            clearQuickFixes()
                            onChange(preserveExperience(BeautySettings.fromPreset(it, settings)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SettingSlider(
                        title = stringResource(R.string.overall_intensity),
                        value = settings.globalStrength,
                        testTag = "slider_overall",
                    ) {
                        clearQuickFixes()
                        onChange(preserveExperience(BeautySettings.fromGlobalStrength(it, settings)))
                    }
                    Text(stringResource(R.string.one_tap_corrections), color = BmText, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutcomeCard(
                            title = stringResource(R.string.action_fresh_eyes),
                            icon = Icons.Default.Visibility,
                            active = "fresh_eyes" in activeFixes,
                            modifier = Modifier.weight(1f),
                            testTag = "action_fresh_eyes",
                        ) {
                            toggleFix("fresh_eyes") { s ->
                                s.copy(
                                    underEyeStrength = maxOf(s.underEyeStrength, 0.72f),
                                    underEyeSmoothing = maxOf(s.underEyeSmoothing, 0.54f),
                                    underEyeMaximumLift = maxOf(s.underEyeMaximumLift, 0.23f),
                                    underEyeColorCorrection = maxOf(s.underEyeColorCorrection, 0.60f),
                                    eyeClarity = maxOf(s.eyeClarity, 0.38f),
                                    eyeSparkle = maxOf(s.eyeSparkle, 0.28f),
                                    eyeBrightening = maxOf(s.eyeBrightening, 0.22f),
                                )
                            }
                        }
                        OutcomeCard(
                            title = stringResource(R.string.action_even_skin),
                            icon = Icons.Default.Tune,
                            active = "even_skin" in activeFixes,
                            modifier = Modifier.weight(1f),
                            testTag = "action_even_skin",
                        ) {
                            toggleFix("even_skin") { s ->
                                s.copy(
                                    smoothingStrength = maxOf(s.smoothingStrength, 0.64f),
                                    smoothingRadius = maxOf(s.smoothingRadius, 5.7f),
                                    complexionEvenness = maxOf(s.complexionEvenness, 0.50f),
                                    blemishControl = maxOf(s.blemishControl, 0.48f),
                                    rednessCorrection = maxOf(s.rednessCorrection, 0.34f),
                                    shineControl = maxOf(s.shineControl, 0.38f),
                                    skinGlow = maxOf(s.skinGlow, 0.22f),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutcomeCard(
                            title = stringResource(R.string.action_clear_blemishes),
                            icon = Icons.Default.Tune,
                            active = "clear_blemishes" in activeFixes,
                            modifier = Modifier.weight(1f),
                            testTag = "action_clear_blemishes",
                        ) {
                            toggleFix("clear_blemishes") { s ->
                                s.copy(
                                    blemishControl = maxOf(s.blemishControl, 0.78f),
                                    rednessCorrection = maxOf(s.rednessCorrection, 0.55f),
                                    complexionEvenness = maxOf(s.complexionEvenness, 0.52f),
                                    smoothingStrength = maxOf(s.smoothingStrength, 0.58f),
                                    smoothingRadius = maxOf(s.smoothingRadius, 5.2f),
                                )
                            }
                        }
                        OutcomeCard(
                            title = stringResource(R.string.action_defined_features),
                            icon = Icons.Default.Compare,
                            active = "defined_features" in activeFixes,
                            modifier = Modifier.weight(1f),
                            testTag = "action_defined_features",
                        ) {
                            toggleFix("defined_features") { s ->
                                s.copy(
                                    eyeClarity = maxOf(s.eyeClarity, 0.42f),
                                    browDefinition = maxOf(s.browDefinition, 0.30f),
                                    lipEnhancement = maxOf(s.lipEnhancement, 0.34f),
                                    lipDefinition = maxOf(s.lipDefinition, 0.38f),
                                    lipTintStrength = maxOf(s.lipTintStrength, 0.30f),
                                    contourStrength = maxOf(s.contourStrength, 0.28f),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutcomeCard(
                            title = stringResource(R.string.action_stage_ready),
                            icon = Icons.Default.WaterDrop,
                            active = "stage_ready" in activeFixes,
                            modifier = Modifier.weight(1f),
                            testTag = "action_stage_ready",
                        ) {
                            toggleFix("stage_ready") { _ -> BeautySettings.stage() }
                        }
                        OutcomeCard(
                            title = stringResource(R.string.action_reflection_plus),
                            icon = Icons.Default.AutoAwesome,
                            active = "reflection_plus" in activeFixes,
                            modifier = Modifier.weight(1f),
                            testTag = "action_reflection_plus",
                        ) {
                            toggleFix("reflection_plus") { s ->
                                s.copy(
                                    smoothingStrength = maxOf(s.smoothingStrength, 0.82f),
                                    complexionEvenness = maxOf(s.complexionEvenness, 0.68f),
                                    blemishControl = maxOf(s.blemishControl, 0.70f),
                                    underEyeStrength = maxOf(s.underEyeStrength, 0.82f),
                                    underEyeSmoothing = maxOf(s.underEyeSmoothing, 0.68f),
                                    underEyeMaximumLift = maxOf(s.underEyeMaximumLift, 0.25f),
                                    underEyeColorCorrection = maxOf(s.underEyeColorCorrection, 0.66f),
                                    eyeClarity = maxOf(s.eyeClarity, 0.56f),
                                    eyeBrightening = maxOf(s.eyeBrightening, 0.34f),
                                    eyeSparkle = maxOf(s.eyeSparkle, 0.46f),
                                    browDefinition = maxOf(s.browDefinition, 0.42f),
                                    lipEnhancement = maxOf(s.lipEnhancement, 0.60f),
                                    lipTintStrength = maxOf(s.lipTintStrength, 0.56f),
                                    lipDefinition = maxOf(s.lipDefinition, 0.60f),
                                    lipGloss = maxOf(s.lipGloss, 0.38f),
                                    contourStrength = maxOf(s.contourStrength, 0.46f),
                                    blushStrength = maxOf(s.blushStrength, 0.28f),
                                    faceSlimming = maxOf(s.faceSlimming, 0.40f),
                                    eyeEnlargement = maxOf(s.eyeEnlargement, 0.28f),
                                    noseRefinement = maxOf(s.noseRefinement, 0.22f),
                                )
                            }
                        }
                    }
                }

                StudioPage.SKIN -> {
                    SettingSlider(stringResource(R.string.skin_smoothing), settings.smoothingStrength, "slider_smoothing") { custom { copy(smoothingStrength = it) } }
                    SettingSlider(stringResource(R.string.smoothing_radius), ((settings.smoothingRadius - 0.5f) / 7.5f).coerceIn(0f, 1f), "slider_smoothing_radius") { custom { copy(smoothingRadius = 0.5f + it * 7.5f) } }
                    SettingSlider(stringResource(R.string.texture_retention), settings.detailRetention, "slider_texture") { custom { copy(detailRetention = it) } }
                    SettingSlider(stringResource(R.string.complexion_evenness), settings.complexionEvenness, "slider_complexion") { custom { copy(complexionEvenness = it) } }
                    SettingSlider(stringResource(R.string.acne_blemishes), settings.blemishControl, "slider_blemish") { custom { copy(blemishControl = it) } }
                    SettingSlider(stringResource(R.string.redness_correction), settings.rednessCorrection, "slider_redness") { custom { copy(rednessCorrection = it) } }
                    SettingSlider(stringResource(R.string.shine_control), settings.shineControl, "slider_shine") { custom { copy(shineControl = it) } }
                    SettingSlider(stringResource(R.string.skin_glow), settings.skinGlow, "slider_glow") { custom { copy(skinGlow = it) } }
                    SettingSlider(stringResource(R.string.face_light), ((settings.faceExposure + 0.3f) / 0.8f).coerceIn(0f, 1f), "slider_face_light") { custom { copy(faceExposure = it * 0.8f - 0.3f) } }
                    SettingSlider(stringResource(R.string.shadow_lift), settings.shadowLift * 2f, "slider_shadow") { custom { copy(shadowLift = it * 0.5f) } }
                }

                StudioPage.EYES -> {
                    SectionLabel(stringResource(R.string.page_under_eyes))
                    SettingSlider(stringResource(R.string.dark_circle_correction), settings.underEyeStrength, "slider_dark_circles") { custom { copy(underEyeStrength = it) } }
                    SettingSlider(stringResource(R.string.puffiness_soften), settings.underEyeSmoothing, "slider_puffiness") { custom { copy(underEyeSmoothing = it) } }
                    SettingSlider(stringResource(R.string.maximum_lift), (settings.underEyeMaximumLift / 0.4f).coerceIn(0f, 1f), "slider_under_eye_lift") { custom { copy(underEyeMaximumLift = it * 0.4f) } }
                    SettingSlider(stringResource(R.string.blue_purple_neutralization), settings.underEyeColorCorrection, "slider_under_eye_color") { custom { copy(underEyeColorCorrection = it) } }
                    SectionLabel(stringResource(R.string.page_eyes))
                    SettingSlider(stringResource(R.string.eye_clarity), settings.eyeClarity / 0.8f, "slider_eye_clarity") { custom { copy(eyeClarity = it * 0.8f) } }
                    SettingSlider(stringResource(R.string.eye_brightening), settings.eyeBrightening, "slider_eye_bright") { custom { copy(eyeBrightening = it) } }
                    SettingSlider(stringResource(R.string.eye_sparkle), settings.eyeSparkle, "slider_eye_sparkle") { custom { copy(eyeSparkle = it) } }
                    SettingSlider(stringResource(R.string.brow_definition), settings.browDefinition, "slider_brows") { custom { copy(browDefinition = it) } }
                    SettingSlider(stringResource(R.string.teeth_whitening), settings.teethWhitening, "slider_teeth") { custom { copy(teethWhitening = it) } }
                }

                StudioPage.LIPS -> {
                    SettingSlider(stringResource(R.string.lip_enhancement), settings.lipEnhancement, "slider_lip_enhance") { custom { copy(lipEnhancement = it) } }
                    SettingSlider(stringResource(R.string.lip_tint), settings.lipTintStrength, "slider_lip_tint") { custom { copy(lipTintStrength = it) } }
                    SettingSlider(stringResource(R.string.lip_definition), settings.lipDefinition, "slider_lip_definition") { custom { copy(lipDefinition = it) } }
                    SettingSlider(stringResource(R.string.lip_gloss), settings.lipGloss, "slider_lip_gloss") { custom { copy(lipGloss = it) } }
                }

                StudioPage.SHAPE -> {
                    SettingSlider(stringResource(R.string.face_slimming), settings.faceSlimming, "slider_face_slim") { custom { copy(faceSlimming = it) } }
                    SettingSlider(stringResource(R.string.eye_enlargement), settings.eyeEnlargement, "slider_eye_size") { custom { copy(eyeEnlargement = it) } }
                    SettingSlider(stringResource(R.string.nose_refinement), settings.noseRefinement, "slider_nose") { custom { copy(noseRefinement = it) } }
                    SettingSlider(stringResource(R.string.contour), settings.contourStrength, "slider_contour") { custom { copy(contourStrength = it) } }
                    SettingSlider(stringResource(R.string.blush), settings.blushStrength, "slider_blush") { custom { copy(blushStrength = it) } }
                }

                StudioPage.SCENE -> {
                    ReflectionSceneSelector(
                        selected = settings.reflectionScene,
                        onSelect = { onChange(settings.copy(reflectionScene = it)) },
                    )
                    if (settings.reflectionScene == ReflectionScene.DARK_LAKE) {
                        Text(stringResource(R.string.scene_moods), color = BmText, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SceneMoodCard(
                                title = stringResource(R.string.scene_mood_still_well),
                                subtitle = stringResource(R.string.scene_mood_still_well_sub),
                                modifier = Modifier.weight(1f),
                                active = settings.lakeMotion <= 0.40f && settings.lakeDarkness >= 0.68f,
                                testTag = "mood_still_well",
                            ) { applyLakeMood(0.70f, 0.32f, 0.78f, 1f) }
                            SceneMoodCard(
                                title = stringResource(R.string.scene_mood_marsh),
                                subtitle = stringResource(R.string.scene_mood_marsh_sub),
                                modifier = Modifier.weight(1f),
                                active = settings.lakeMotion in 0.40f..0.65f && settings.lakeDarkness >= 0.70f,
                                testTag = "mood_marsh",
                            ) { applyLakeMood(0.78f, 0.55f, 0.74f, 1f) }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SceneMoodCard(
                                title = stringResource(R.string.scene_mood_ripple),
                                subtitle = stringResource(R.string.scene_mood_ripple_sub),
                                modifier = Modifier.weight(1f),
                                active = settings.lakeMotion >= 0.70f,
                                testTag = "mood_ripple",
                            ) { applyLakeMood(0.85f, 0.92f, 0.62f, 1f) }
                            SceneMoodCard(
                                title = stringResource(R.string.scene_mood_reveal),
                                subtitle = stringResource(R.string.scene_mood_reveal_sub),
                                modifier = Modifier.weight(1f),
                                active = settings.lakeIntensity >= 0.80f && settings.lakeFaceClarity >= 0.90f,
                                testTag = "mood_reveal",
                            ) { applyLakeMood(0.88f, 0.70f, 0.68f, 1f) }
                        }
                        SettingSlider(stringResource(R.string.lake_intensity), settings.lakeIntensity, "slider_lake_intensity") { onChange(settings.copy(lakeIntensity = it).clamped()) }
                        SettingSlider(stringResource(R.string.lake_motion), settings.lakeMotion, "slider_lake_motion") { onChange(settings.copy(lakeMotion = it).clamped()) }
                        SettingSlider(stringResource(R.string.lake_darkness), settings.lakeDarkness, "slider_lake_darkness") { onChange(settings.copy(lakeDarkness = it).clamped()) }
                        SettingSlider(stringResource(R.string.lake_face_clarity), settings.lakeFaceClarity, "slider_lake_clarity") { onChange(settings.copy(lakeFaceClarity = it).clamped()) }
                        Text(stringResource(R.string.lake_scene_hint), color = BmTextMuted, fontSize = 10.sp)
                    }
                }

                StudioPage.SYSTEM -> {
                    PerformanceCard(
                        selectedQuality = settings.qualityLevel,
                        runtimeQuality = runtimeQuality,
                        performanceState = performanceState,
                        timing = timing,
                    )
                    ToggleRow(
                        title = stringResource(R.string.mirror_preview),
                        subtitle = stringResource(R.string.mirror_preview_sub),
                        checked = settings.mirrorPreview,
                        testTag = "toggle_mirror",
                    ) { onChange(settings.copy(mirrorPreview = it)) }
                    Text(stringResource(R.string.quality_ceiling), color = BmText, fontSize = 12.sp)
                    QualitySelector(settings.qualityLevel) { onChange(settings.copy(qualityLevel = it)) }
                    Text(stringResource(R.string.language), color = BmText, fontSize = 12.sp)
                    LanguageSelector(language) { selected ->
                        if (selected != language) {
                            LanguagePreferences.set(context, selected)
                            (context as? Activity)?.recreate()
                        }
                    }
                    if (BuildConfig.OTA_ENABLED && otaController != null) {
                        ToggleRow(
                            title = stringResource(R.string.auto_updates),
                            subtitle = stringResource(R.string.auto_updates_sub),
                            checked = autoUpdate,
                            testTag = "toggle_auto_updates",
                        ) {
                            autoUpdate = it
                            OtaPreferences.setAutoUpdateEnabled(context, it)
                        }
                        TextButton(
                            onClick = { otaController.checkNow() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                                .testTag("check_updates"),
                        ) {
                            Text(stringResource(R.string.check_updates), color = BmAccent)
                        }
                        Text(
                            text = otaStatusLabel(otaStatus),
                            color = BmTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.testTag("ota_status"),
                        )
                    }
                    if (BuildConfig.DEBUG_OVERLAY_AVAILABLE) {
                        ToggleRow(
                            title = stringResource(R.string.diagnostics),
                            subtitle = stringResource(R.string.diagnostics_sub),
                            checked = settings.debugOverlay,
                            testTag = "toggle_diagnostics",
                        ) { onChange(settings.copy(debugOverlay = it)) }
                    }
                    SectionLabel(stringResource(R.string.about))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_section"),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.about_version,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                            ),
                            color = BmText,
                            fontSize = 13.sp,
                            modifier = Modifier.testTag("about_version"),
                        )
                        Text(
                            text = stringResource(R.string.about_developed_by),
                            color = BmTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("about_developed_by"),
                        )
                        Text(
                            text = stringResource(R.string.privacy_statement),
                            color = BmTextMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BmTextMuted.copy(alpha = 0.13f))
        TextButton(
            onClick = {
                clearQuickFixes()
                onChange(preserveExperience(BeautySettings.off()))
            },
            modifier = Modifier
                .align(Alignment.Start)
                .defaultMinSize(minHeight = 44.dp)
                .testTag("reset_look"),
        ) {
            Text(stringResource(R.string.reset_look), color = BmTextMuted)
        }
    }
}

@Composable
private fun StudioNavigation(selected: StudioPage, onSelected: (StudioPage) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        StudioPage.entries.forEach { page ->
            val active = page == selected
            Text(
                text = stringResource(page.labelRes),
                color = if (active) BmBg else BmTextMuted,
                fontSize = 12.sp,
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) BmAccent else BmSurfaceStrong)
                    .semantics {
                        role = Role.Tab
                        this.selected = active
                    }
                    .clickable { onSelected(page) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("page_${page.name.lowercase()}"),
            )
        }
    }
}

@Composable
private fun OutcomeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) BmAccent.copy(alpha = 0.22f) else BmSurfaceStrong)
            .semantics {
                role = Role.Switch
                this.selected = active
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = BmAccent, modifier = Modifier.size(18.dp))
        Text(title, color = if (active) BmAccent else BmText, fontSize = 12.sp, maxLines = 2)
        Text(
            text = stringResource(if (active) R.string.quick_fix_on else R.string.quick_fix_off),
            color = BmAccent,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ReflectionSceneSelector(selected: ReflectionScene, onSelect: (ReflectionScene) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SceneChoice(
            title = stringResource(R.string.mirror_scene),
            icon = Icons.Default.Visibility,
            active = selected == ReflectionScene.MIRROR,
            modifier = Modifier.weight(1f),
            testTag = "scene_mirror",
        ) { onSelect(ReflectionScene.MIRROR) }
        SceneChoice(
            title = stringResource(R.string.dark_lake),
            icon = Icons.Default.WaterDrop,
            active = selected == ReflectionScene.DARK_LAKE,
            modifier = Modifier.weight(1f),
            testTag = "scene_lake",
        ) { onSelect(ReflectionScene.DARK_LAKE) }
    }
}

@Composable
private fun SceneMoodCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    active: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) BmAccent.copy(alpha = 0.18f) else BmSurfaceStrong)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = if (active) BmAccent else BmText, fontSize = 12.sp)
        Text(subtitle, color = BmTextMuted, fontSize = 10.sp, lineHeight = 12.sp)
    }
}

@Composable
private fun SceneChoice(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) BmAccent.copy(alpha = 0.20f) else BmSurfaceStrong)
            .semantics {
                role = Role.RadioButton
                selected = active
                contentDescription = title
            }
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(if (active) BmAccent else BmTextMuted.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (active) BmBg else BmText, modifier = Modifier.size(18.dp))
        }
        Text(title, color = BmText, fontSize = 13.sp)
    }
}


@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    testTag: String,
    onValue: (Float) -> Unit,
) {
    val safe = value.coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BmSurfaceStrong.copy(alpha = 0.72f))
            .padding(horizontal = 11.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = BmText, fontSize = 12.sp)
            Text("${(safe * 100).toInt()}%", color = BmAccent, fontSize = 10.sp)
        }
        Slider(
            value = safe,
            onValueChange = onValue,
            modifier = Modifier
                .height(36.dp)
                .semantics { contentDescription = "$title ${(safe * 100).toInt()} percent" }
                .testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = BmAccent,
                activeTrackColor = BmAccent,
                inactiveTrackColor = BmTextMuted.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = BmAccent,
        fontSize = 10.sp,
        modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 1.dp),
    )
}


@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BmSurfaceStrong)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onChecked,
            )
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.78f)) {
            Text(title, color = BmText, fontSize = 12.sp)
            Text(subtitle, color = BmTextMuted, fontSize = 9.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedThumbColor = BmBg, checkedTrackColor = BmAccent),
        )
    }
}

@Composable
private fun PerformanceCard(
    selectedQuality: QualityLevel,
    runtimeQuality: QualityLevel,
    performanceState: AdaptivePerformanceState,
    timing: FrameTimingCollector.Snapshot?,
) {
    val fps = timing?.cameraFps ?: 0.0
    val cameraLimited = performanceState.cameraLimited
    val healthy = fps <= 0.0 || fps >= 29.0 || cameraLimited
    val protecting = !cameraLimited && (performanceState.protecting || runtimeQuality != selectedQuality)
    val statusColor = when {
        cameraLimited -> BmTextMuted
        !healthy -> BmDanger
        protecting -> BmAccent
        else -> BmText
    }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BmSurfaceStrong).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = when {
                cameraLimited -> stringResource(R.string.motion_camera_limited)
                !healthy -> stringResource(R.string.motion_optimizing)
                protecting -> stringResource(R.string.motion_protecting)
                else -> stringResource(R.string.motion_stable)
            },
            color = statusColor,
            fontSize = 13.sp,
        )
        Text(
            text = stringResource(
                R.string.perf_runtime_line,
                if (fps > 0.0) "%.1f FPS".format(fps) else stringResource(R.string.measuring_fps),
                stringResource(runtimeQuality.labelRes()),
                stringResource(selectedQuality.labelRes()),
            ),
            color = BmTextMuted,
            fontSize = 9.sp,
        )
        PerformancePressureBar(performanceState.pressure)
        Text(
            text = stringResource(R.string.performance_interpolation, (performanceState.pressure * 100).toInt()),
            color = BmTextMuted,
            fontSize = 9.sp,
        )
        if (timing != null && timing.gpuFrameMs > 0.0) {
            Text(
                text = stringResource(R.string.perf_render_line, timing.gpuFrameMs, timing.p95FrameMs, timing.slowFrameRatio * 100.0),
                color = BmTextMuted,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun PerformancePressureBar(pressure: Float) {
    Box(
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(BmTextMuted.copy(alpha = 0.14f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(pressure.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(CircleShape)
                .background(if (pressure > 0.72f) BmDanger else BmAccent),
        )
    }
}

@Composable
private fun QualitySelector(selected: QualityLevel, onSelect: (QualityLevel) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QualityLevel.userChoices.forEach { level ->
            val active = selected == level
            Text(
                text = stringResource(level.labelRes()),
                color = if (active) BmBg else BmTextMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (active) BmAccent else BmSurfaceStrong)
                    .clickable { onSelect(level) }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("quality_${level.name.lowercase()}"),
            )
        }
    }
}

@Composable
private fun otaStatusLabel(status: UpdateStatus): String = when (status) {
    UpdateStatus.Idle -> stringResource(R.string.ota_status_idle)
    UpdateStatus.Checking -> stringResource(R.string.ota_status_checking)
    UpdateStatus.UpToDate -> stringResource(R.string.ota_status_up_to_date)
    is UpdateStatus.Downloading -> stringResource(R.string.ota_status_downloading, status.percent)
    UpdateStatus.Installing -> stringResource(R.string.ota_status_installing)
    is UpdateStatus.Available -> stringResource(R.string.ota_status_available, status.version, status.build)
    is UpdateStatus.Error -> status.message
}

@Composable
private fun LanguageSelector(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { language ->
            val active = selected == language
            Text(
                text = stringResource(if (language == AppLanguage.ENGLISH) R.string.language_english else R.string.language_french),
                color = if (active) BmBg else BmTextMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (active) BmAccent else BmSurfaceStrong)
                    .clickable { onSelect(language) }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("language_${language.name.lowercase()}"),
            )
        }
    }
}

@StringRes
fun BeautyPreset.labelRes(): Int = when (this) {
    BeautyPreset.OFF -> R.string.preset_off
    BeautyPreset.NATURAL -> R.string.preset_natural
    BeautyPreset.SOFT -> R.string.preset_soft
    BeautyPreset.BRIGHT -> R.string.preset_bright
    BeautyPreset.STAGE -> R.string.preset_stage
    BeautyPreset.GLAM -> R.string.preset_glam
    BeautyPreset.CUSTOM -> R.string.preset_custom
}

@StringRes
fun QualityLevel.labelRes(): Int = when (this) {
    QualityLevel.PERFORMANCE -> R.string.quality_performance
    QualityLevel.LOW -> R.string.quality_low
    QualityLevel.MEDIUM -> R.string.quality_medium
    QualityLevel.HIGH -> R.string.quality_high
}
