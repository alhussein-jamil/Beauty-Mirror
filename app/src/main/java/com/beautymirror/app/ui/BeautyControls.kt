package com.beautymirror.app.ui

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beautymirror.app.BuildConfig
import com.beautymirror.app.R
import com.beautymirror.app.rendering.FrameTimingCollector
import com.beautymirror.app.settings.AppLanguage
import com.beautymirror.app.settings.BeautyPreset
import com.beautymirror.app.settings.BeautySettings
import com.beautymirror.app.settings.LanguagePreferences
import com.beautymirror.app.settings.QualityLevel
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
    @StringRes val guideTitleRes: Int,
    @StringRes val guideDescriptionRes: Int,
) {
    QUICK(
        R.string.page_quick,
        BeautyFocus.OVERVIEW,
        R.string.guide_quick_title,
        R.string.guide_quick_desc,
    ),
    SKIN(
        R.string.page_skin,
        BeautyFocus.SKIN,
        R.string.guide_skin_title,
        R.string.guide_skin_desc,
    ),
    UNDER_EYES(
        R.string.page_under_eyes,
        BeautyFocus.UNDER_EYES,
        R.string.guide_under_eyes_title,
        R.string.guide_under_eyes_desc,
    ),
    EYES(
        R.string.page_eyes,
        BeautyFocus.EYES,
        R.string.guide_eyes_title,
        R.string.guide_eyes_desc,
    ),
    LIPS(
        R.string.page_lips,
        BeautyFocus.LIPS,
        R.string.guide_lips_title,
        R.string.guide_lips_desc,
    ),
    SHAPE(
        R.string.page_shape,
        BeautyFocus.SHAPE,
        R.string.guide_shape_title,
        R.string.guide_shape_desc,
    ),
    SYSTEM(
        R.string.page_system,
        BeautyFocus.SYSTEM,
        R.string.guide_system_title,
        R.string.guide_system_desc,
    ),
}

@Composable
fun BeautyControls(
    settings: BeautySettings,
    runtimeQuality: QualityLevel,
    timing: FrameTimingCollector.Snapshot?,
    onChange: (BeautySettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableStateOf(StudioPage.QUICK) }
    val context = LocalContext.current
    val language = LanguagePreferences.get(context)
    var activeFixes by rememberSaveable { mutableStateOf(setOf<String>()) }
    val fixSnapshots = remember { mutableMapOf<String, BeautySettings>() }

    fun preserveSystem(target: BeautySettings): BeautySettings = target.copy(
        qualityLevel = settings.qualityLevel,
        debugOverlay = settings.debugOverlay,
        mirrorPreview = settings.mirrorPreview,
    )

    fun toggleFix(id: String, apply: (BeautySettings) -> BeautySettings) {
        if (id in activeFixes) {
            val prior = fixSnapshots.remove(id)
            if (prior != null) {
                onChange(preserveSystem(prior).copy(preset = BeautyPreset.CUSTOM).clamped())
            }
            activeFixes = activeFixes - id
        } else {
            fixSnapshots[id] = settings
            onChange(preserveSystem(apply(settings)).copy(preset = BeautyPreset.CUSTOM).clamped())
            activeFixes = activeFixes + id
        }
    }

    fun custom(block: BeautySettings.() -> BeautySettings) {
        onChange(settings.block().copy(preset = BeautyPreset.CUSTOM).clamped())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(BmSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(R.string.beauty_studio), color = BmText, fontSize = 19.sp)
                Text(
                    text = stringResource(
                        R.string.studio_subtitle,
                        stringResource(settings.preset.labelRes()),
                    ),
                    color = BmTextMuted,
                    fontSize = 11.sp,
                )
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done), color = BmAccent)
            }
        }

        PresetSelector(
            selected = settings.preset,
            onSelect = { onChange(preserveSystem(BeautySettings.fromPreset(it, settings))) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            StudioPage.entries.forEach { item ->
                val selected = item == page
                Text(
                    text = stringResource(item.labelRes),
                    color = if (selected) BmBg else BmTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) BmAccent else BmSurfaceStrong)
                        .clickable { page = item }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        BeautyFeatureGuide(
            focus = page.focus,
            title = stringResource(page.guideTitleRes),
            description = stringResource(page.guideDescriptionRes),
        )

        Column(
            modifier = Modifier
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (page) {
                StudioPage.QUICK -> {
                    SettingSlider(
                        title = stringResource(R.string.overall_intensity),
                        subtitle = stringResource(R.string.overall_intensity_sub),
                        value = settings.globalStrength,
                    ) {
                        onChange(
                            preserveSystem(
                                BeautySettings.fromGlobalStrength(it, settings),
                            ),
                        )
                    }
                    Text(stringResource(R.string.one_tap_corrections), color = BmText, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SmartAction(
                            title = stringResource(R.string.action_fresh_eyes),
                            subtitle = stringResource(R.string.action_fresh_eyes_sub),
                            active = "fresh_eyes" in activeFixes,
                            onClick = {
                                toggleFix("fresh_eyes") { s ->
                                    s.copy(
                                        underEyeStrength = maxOf(s.underEyeStrength, 0.72f),
                                        underEyeSmoothing = maxOf(s.underEyeSmoothing, 0.55f),
                                        underEyeMaximumLift = maxOf(s.underEyeMaximumLift, 0.24f),
                                        underEyeColorCorrection = maxOf(s.underEyeColorCorrection, 0.60f),
                                        eyeClarity = maxOf(s.eyeClarity, 0.38f),
                                        eyeSparkle = maxOf(s.eyeSparkle, 0.32f),
                                        eyeBrightening = maxOf(s.eyeBrightening, 0.22f),
                                    )
                                }
                            },
                        )
                        SmartAction(
                            title = stringResource(R.string.action_clear_blemishes),
                            subtitle = stringResource(R.string.action_clear_blemishes_sub),
                            active = "clear_blemishes" in activeFixes,
                            onClick = {
                                toggleFix("clear_blemishes") { s ->
                                    s.copy(
                                        blemishControl = maxOf(s.blemishControl, 0.78f),
                                        rednessCorrection = maxOf(s.rednessCorrection, 0.55f),
                                        complexionEvenness = maxOf(s.complexionEvenness, 0.52f),
                                        smoothingStrength = maxOf(s.smoothingStrength, 0.58f),
                                        smoothingRadius = maxOf(s.smoothingRadius, 5.2f),
                                    )
                                }
                            },
                        )
                        SmartAction(
                            title = stringResource(R.string.action_even_skin),
                            subtitle = stringResource(R.string.action_even_skin_sub),
                            active = "even_skin" in activeFixes,
                            onClick = {
                                toggleFix("even_skin") { s ->
                                    s.copy(
                                        smoothingStrength = maxOf(s.smoothingStrength, 0.65f),
                                        smoothingRadius = maxOf(s.smoothingRadius, 5.8f),
                                        complexionEvenness = maxOf(s.complexionEvenness, 0.50f),
                                        blemishControl = maxOf(s.blemishControl, 0.48f),
                                        rednessCorrection = maxOf(s.rednessCorrection, 0.34f),
                                        shineControl = maxOf(s.shineControl, 0.40f),
                                        skinGlow = maxOf(s.skinGlow, 0.22f),
                                    )
                                }
                            },
                        )
                        SmartAction(
                            title = stringResource(R.string.action_defined_features),
                            subtitle = stringResource(R.string.action_defined_features_sub),
                            active = "defined_features" in activeFixes,
                            onClick = {
                                toggleFix("defined_features") { s ->
                                    s.copy(
                                        eyeClarity = maxOf(s.eyeClarity, 0.40f),
                                        eyeSparkle = maxOf(s.eyeSparkle, 0.34f),
                                        browDefinition = maxOf(s.browDefinition, 0.32f),
                                        lipEnhancement = maxOf(s.lipEnhancement, 0.28f),
                                        lipDefinition = maxOf(s.lipDefinition, 0.38f),
                                        lipTintStrength = maxOf(s.lipTintStrength, 0.30f),
                                        contourStrength = maxOf(s.contourStrength, 0.30f),
                                    )
                                }
                            },
                        )
                        SmartAction(
                            title = stringResource(R.string.action_stage_ready),
                            subtitle = stringResource(R.string.action_stage_ready_sub),
                            active = "stage_ready" in activeFixes,
                            onClick = {
                                toggleFix("stage_ready") { _ -> BeautySettings.stage() }
                            },
                        )
                    }
                    InfoCard(stringResource(R.string.info_compare))
                }

                StudioPage.SKIN -> {
                    SettingSlider(
                        stringResource(R.string.skin_smoothing),
                        stringResource(R.string.skin_smoothing_sub),
                        settings.smoothingStrength,
                    ) { custom { copy(smoothingStrength = it) } }
                    SettingSlider(
                        stringResource(R.string.smoothing_radius),
                        stringResource(R.string.smoothing_radius_sub),
                        ((settings.smoothingRadius - 0.5f) / 7.5f).coerceIn(0f, 1f),
                    ) { custom { copy(smoothingRadius = 0.5f + it * 7.5f) } }
                    SettingSlider(
                        stringResource(R.string.texture_retention),
                        stringResource(R.string.texture_retention_sub),
                        settings.detailRetention,
                    ) { custom { copy(detailRetention = it) } }
                    SettingSlider(
                        stringResource(R.string.complexion_evenness),
                        stringResource(R.string.complexion_evenness_sub),
                        settings.complexionEvenness,
                    ) { custom { copy(complexionEvenness = it) } }
                    SettingSlider(
                        stringResource(R.string.acne_blemishes),
                        stringResource(R.string.acne_blemishes_sub),
                        settings.blemishControl,
                    ) { custom { copy(blemishControl = it) } }
                    SettingSlider(
                        stringResource(R.string.redness_correction),
                        stringResource(R.string.redness_correction_sub),
                        settings.rednessCorrection,
                    ) { custom { copy(rednessCorrection = it) } }
                    SettingSlider(
                        stringResource(R.string.shine_control),
                        stringResource(R.string.shine_control_sub),
                        settings.shineControl,
                    ) { custom { copy(shineControl = it) } }
                    SettingSlider(
                        stringResource(R.string.skin_glow),
                        stringResource(R.string.skin_glow_sub),
                        settings.skinGlow,
                    ) { custom { copy(skinGlow = it) } }
                    SettingSlider(
                        stringResource(R.string.face_light),
                        stringResource(R.string.face_light_sub),
                        ((settings.faceExposure + 0.3f) / 0.8f).coerceIn(0f, 1f),
                    ) { custom { copy(faceExposure = it * 0.8f - 0.3f) } }
                    SettingSlider(
                        stringResource(R.string.shadow_lift),
                        stringResource(R.string.shadow_lift_sub),
                        settings.shadowLift * 2f,
                    ) { custom { copy(shadowLift = it * 0.5f) } }
                    SettingSlider(
                        stringResource(R.string.highlight_protection),
                        stringResource(R.string.highlight_protection_sub),
                        settings.highlightProtection,
                    ) { custom { copy(highlightProtection = it) } }
                    SettingSlider(
                        stringResource(R.string.warmth),
                        stringResource(R.string.warmth_sub),
                        (settings.warmth + 0.5f).coerceIn(0f, 1f),
                    ) { custom { copy(warmth = it - 0.5f) } }
                    SettingSlider(
                        stringResource(R.string.local_contrast),
                        stringResource(R.string.local_contrast_sub),
                        settings.localContrast / 0.6f,
                    ) { custom { copy(localContrast = it * 0.6f) } }
                }

                StudioPage.UNDER_EYES -> {
                    SettingSlider(
                        stringResource(R.string.dark_circle_correction),
                        stringResource(R.string.dark_circle_correction_sub),
                        settings.underEyeStrength,
                    ) { custom { copy(underEyeStrength = it) } }
                    SettingSlider(
                        stringResource(R.string.puffiness_soften),
                        stringResource(R.string.puffiness_soften_sub),
                        settings.underEyeSmoothing,
                    ) { custom { copy(underEyeSmoothing = it) } }
                    SettingSlider(
                        stringResource(R.string.maximum_lift),
                        stringResource(R.string.maximum_lift_sub),
                        (settings.underEyeMaximumLift / 0.4f).coerceIn(0f, 1f),
                    ) { custom { copy(underEyeMaximumLift = it * 0.4f) } }
                    SettingSlider(
                        stringResource(R.string.blue_purple_neutralization),
                        stringResource(R.string.blue_purple_neutralization_sub),
                        settings.underEyeColorCorrection,
                    ) { custom { copy(underEyeColorCorrection = it) } }
                    InfoCard(stringResource(R.string.info_under_eyes))
                }

                StudioPage.EYES -> {
                    SettingSlider(
                        stringResource(R.string.eye_clarity),
                        stringResource(R.string.eye_clarity_sub),
                        settings.eyeClarity / 0.8f,
                    ) { custom { copy(eyeClarity = it * 0.8f) } }
                    SettingSlider(
                        stringResource(R.string.eye_brightening),
                        stringResource(R.string.eye_brightening_sub),
                        settings.eyeBrightening,
                    ) { custom { copy(eyeBrightening = it) } }
                    SettingSlider(
                        stringResource(R.string.eye_sparkle),
                        stringResource(R.string.eye_sparkle_sub),
                        settings.eyeSparkle,
                    ) { custom { copy(eyeSparkle = it) } }
                    SettingSlider(
                        stringResource(R.string.brow_definition),
                        stringResource(R.string.brow_definition_sub),
                        settings.browDefinition,
                    ) { custom { copy(browDefinition = it) } }
                    SettingSlider(
                        stringResource(R.string.teeth_whitening),
                        stringResource(R.string.teeth_whitening_sub),
                        settings.teethWhitening,
                    ) { custom { copy(teethWhitening = it) } }
                }

                StudioPage.LIPS -> {
                    SettingSlider(
                        stringResource(R.string.lip_enhancement),
                        stringResource(R.string.lip_enhancement_sub),
                        settings.lipEnhancement,
                    ) { custom { copy(lipEnhancement = it) } }
                    SettingSlider(
                        stringResource(R.string.lip_tint),
                        stringResource(R.string.lip_tint_sub),
                        settings.lipTintStrength,
                    ) { custom { copy(lipTintStrength = it) } }
                    SettingSlider(
                        stringResource(R.string.lip_definition),
                        stringResource(R.string.lip_definition_sub),
                        settings.lipDefinition,
                    ) { custom { copy(lipDefinition = it) } }
                    SettingSlider(
                        stringResource(R.string.lip_gloss),
                        stringResource(R.string.lip_gloss_sub),
                        settings.lipGloss,
                    ) { custom { copy(lipGloss = it) } }
                }

                StudioPage.SHAPE -> {
                    SettingSlider(
                        stringResource(R.string.face_slimming),
                        stringResource(R.string.face_slimming_sub),
                        settings.faceSlimming,
                    ) { custom { copy(faceSlimming = it) } }
                    SettingSlider(
                        stringResource(R.string.eye_enlargement),
                        stringResource(R.string.eye_enlargement_sub),
                        settings.eyeEnlargement,
                    ) { custom { copy(eyeEnlargement = it) } }
                    SettingSlider(
                        stringResource(R.string.nose_refinement),
                        stringResource(R.string.nose_refinement_sub),
                        settings.noseRefinement,
                    ) { custom { copy(noseRefinement = it) } }
                    SettingSlider(
                        stringResource(R.string.contour),
                        stringResource(R.string.contour_sub),
                        settings.contourStrength,
                    ) { custom { copy(contourStrength = it) } }
                    SettingSlider(
                        stringResource(R.string.blush),
                        stringResource(R.string.blush_sub),
                        settings.blushStrength,
                    ) { custom { copy(blushStrength = it) } }
                    InfoCard(stringResource(R.string.info_shape))
                }

                StudioPage.SYSTEM -> {
                    PerformanceCard(
                        selectedQuality = settings.qualityLevel,
                        runtimeQuality = runtimeQuality,
                        timing = timing,
                    )
                    Text(stringResource(R.string.language), color = BmText, fontSize = 13.sp)
                    LanguageSelector(
                        selected = language,
                        onSelect = { next ->
                            if (next == language) return@LanguageSelector
                            LanguagePreferences.set(context, next)
                            (context as? Activity)?.recreate()
                        },
                    )
                    ToggleRow(
                        title = stringResource(R.string.mirror_preview),
                        subtitle = stringResource(R.string.mirror_preview_sub),
                        checked = settings.mirrorPreview,
                    ) { onChange(settings.copy(mirrorPreview = it)) }
                    Text(stringResource(R.string.quality_ceiling), color = BmText, fontSize = 13.sp)
                    QualitySelector(
                        selected = settings.qualityLevel,
                        onSelect = { onChange(settings.copy(qualityLevel = it)) },
                    )
                    InfoCard(stringResource(R.string.info_quality))
                    if (BuildConfig.DEBUG_OVERLAY_AVAILABLE) {
                        ToggleRow(
                            title = stringResource(R.string.diagnostics),
                            subtitle = stringResource(R.string.diagnostics_sub),
                            checked = settings.debugOverlay,
                        ) { onChange(settings.copy(debugOverlay = it)) }
                    }
                }
            }
        }

        HorizontalDivider(color = BmTextMuted.copy(alpha = 0.13f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    activeFixes = emptySet()
                    fixSnapshots.clear()
                    onChange(preserveSystem(BeautySettings.natural()))
                },
            ) {
                Text(stringResource(R.string.reset_look), color = BmTextMuted)
            }
            Text(
                text = stringResource(R.string.private_on_device),
                color = BmAccent,
                fontSize = 10.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BmAccent.copy(alpha = 0.10f))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SmartAction(
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(172.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) BmAccent.copy(alpha = 0.22f) else BmSurfaceStrong)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(title, color = if (active) BmAccent else BmText, fontSize = 13.sp)
        Text(subtitle, color = BmTextMuted, fontSize = 10.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(if (active) R.string.quick_fix_on else R.string.quick_fix_off),
            color = BmAccent,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SettingSlider(
    title: String,
    subtitle: String,
    value: Float,
    onValue: (Float) -> Unit,
) {
    val safe = value.coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(BmSurfaceStrong.copy(alpha = 0.55f))
            .padding(horizontal = 11.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.78f)) {
                Text(title, color = BmText, fontSize = 12.sp)
                Text(subtitle, color = BmTextMuted, fontSize = 9.sp)
            }
            Text("${(safe * 100).toInt()}%", color = BmAccent, fontSize = 11.sp)
        }
        Slider(
            value = safe,
            onValueChange = onValue,
            modifier = Modifier
                .height(34.dp)
                .semantics { contentDescription = "$title ${(safe * 100).toInt()} percent" },
            colors = SliderDefaults.colors(
                thumbColor = BmAccent,
                activeTrackColor = BmAccent,
                inactiveTrackColor = BmTextMuted.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun InfoCard(text: String) {
    Text(
        text = text,
        color = BmTextMuted,
        fontSize = 10.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BmAccent.copy(alpha = 0.07f))
            .padding(horizontal = 11.dp, vertical = 9.dp),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(BmSurfaceStrong)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.78f)) {
            Text(title, color = BmText, fontSize = 12.sp)
            Text(subtitle, color = BmTextMuted, fontSize = 9.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BmBg,
                checkedTrackColor = BmAccent,
            ),
        )
    }
}

@Composable
private fun PerformanceCard(
    selectedQuality: QualityLevel,
    runtimeQuality: QualityLevel,
    timing: FrameTimingCollector.Snapshot?,
) {
    val fps = timing?.cameraFps ?: 0.0
    val protecting = runtimeQuality != selectedQuality
    val healthy = fps <= 0.0 || fps >= 29.0
    val statusColor = when {
        !healthy -> BmDanger
        protecting -> BmAccent
        else -> BmText
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BmSurfaceStrong)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = when {
                !healthy -> stringResource(R.string.motion_optimizing)
                protecting -> stringResource(R.string.motion_protecting)
                else -> stringResource(R.string.motion_stable)
            },
            color = statusColor,
            fontSize = 14.sp,
        )
        Text(
            text = stringResource(
                R.string.perf_runtime_line,
                if (fps > 0.0) "%.1f FPS".format(fps) else stringResource(R.string.measuring_fps),
                stringResource(runtimeQuality.labelRes()),
                stringResource(selectedQuality.labelRes()),
            ),
            color = BmTextMuted,
            fontSize = 10.sp,
        )
        if (timing != null && timing.gpuFrameMs > 0.0) {
            Text(
                text = stringResource(
                    R.string.perf_render_line,
                    timing.gpuFrameMs,
                    timing.p95FrameMs,
                    timing.slowFrameRatio * 100.0,
                ),
                color = BmTextMuted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun QualitySelector(
    selected: QualityLevel,
    onSelect: (QualityLevel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QualityLevel.userChoices.forEach { level ->
            val active = selected == level
            Text(
                text = stringResource(level.labelRes()),
                color = if (active) BmBg else BmTextMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) BmAccent else BmSurfaceStrong)
                    .clickable { onSelect(level) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LanguageSelector(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppLanguage.entries.forEach { language ->
            val active = selected == language
            val label = when (language) {
                AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                AppLanguage.FRENCH -> stringResource(R.string.language_french)
            }
            Text(
                text = label,
                color = if (active) BmBg else BmTextMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) BmAccent else BmSurfaceStrong)
                    .clickable { onSelect(language) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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
