package com.beautymirror.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "beauty_settings")

class SettingsRepository(private val context: Context) {
    private val presetKey = stringPreferencesKey("preset")
    private val effectsSchemaKey = intPreferencesKey("effects_schema")
    private val globalKey = floatPreferencesKey("global")
    private val qualityKey = stringPreferencesKey("quality")
    private val debugKey = booleanPreferencesKey("debug")
    private val mirrorKey = booleanPreferencesKey("mirror_preview")
    private val reflectionSceneKey = stringPreferencesKey("reflection_scene")
    private val lakeIntensityKey = floatPreferencesKey("lake_intensity")
    private val lakeMotionKey = floatPreferencesKey("lake_motion")
    private val lakeDarknessKey = floatPreferencesKey("lake_darkness")
    private val lakeFaceClarityKey = floatPreferencesKey("lake_face_clarity")
    private val lakeCameraBlendKey = floatPreferencesKey("lake_camera_blend")
    private val lakeDeformationKey = floatPreferencesKey("lake_deformation")
    private val lakeSwirlKey = floatPreferencesKey("lake_swirl")
    private val lakeSettledWaterKey = floatPreferencesKey("lake_settled_water")
    private val lakeSettledCameraKey = floatPreferencesKey("lake_settled_camera")
    private val lakeRippleRegionsKey = floatPreferencesKey("lake_ripple_regions")
    private val lakeRippleSpeedKey = floatPreferencesKey("lake_ripple_speed")
    private val lakeWaveDetailKey = floatPreferencesKey("lake_wave_detail")
    private val lakeSpecularKey = floatPreferencesKey("lake_specular")
    private val lakeSkyBlueKey = floatPreferencesKey("lake_sky_blue")
    private val lakeSunlightKey = floatPreferencesKey("lake_sunlight")
    private val lakeWaterWarmthKey = floatPreferencesKey("lake_water_warmth")
    private val lakeSaturationKey = floatPreferencesKey("lake_saturation")
    private val lakeFoamKey = floatPreferencesKey("lake_foam")
    private val lakeCloudsKey = floatPreferencesKey("lake_clouds")
    private val revealDurationKey = floatPreferencesKey("reveal_duration_seconds")
    private val smoothKey = floatPreferencesKey("smooth")
    private val smoothRadiusKey = floatPreferencesKey("smooth_radius")
    private val detailRetentionKey = floatPreferencesKey("detail_retention")
    private val complexionKey = floatPreferencesKey("complexion_evenness")
    private val rednessKey = floatPreferencesKey("redness_correction")
    private val blemishKey = floatPreferencesKey("blemish_control")
    private val shineKey = floatPreferencesKey("shine_control")
    private val skinGlowKey = floatPreferencesKey("skin_glow")
    private val underEyeKey = floatPreferencesKey("undereye")
    private val underEyeSmoothKey = floatPreferencesKey("undereye_smoothing")
    private val underEyeLiftKey = floatPreferencesKey("undereye_lift")
    private val underEyeColorKey = floatPreferencesKey("undereye_color")
    private val exposureKey = floatPreferencesKey("exposure")
    private val shadowKey = floatPreferencesKey("shadow")
    private val highlightKey = floatPreferencesKey("highlight")
    private val warmthKey = floatPreferencesKey("warmth")
    private val contrastKey = floatPreferencesKey("contrast")
    private val contourKey = floatPreferencesKey("contour")
    private val blushKey = floatPreferencesKey("blush")
    private val eyeKey = floatPreferencesKey("eye")
    private val eyeBrightKey = floatPreferencesKey("eye_bright")
    private val eyeSparkleKey = floatPreferencesKey("eye_sparkle")
    private val browDefinitionKey = floatPreferencesKey("brow_definition")
    private val teethKey = floatPreferencesKey("teeth")
    private val lipsKey = floatPreferencesKey("lips")
    private val lipTintKey = floatPreferencesKey("lip_tint")
    private val lipDefinitionKey = floatPreferencesKey("lip_definition")
    private val lipGlossKey = floatPreferencesKey("lip_gloss")
    private val detailPreserveKey = floatPreferencesKey("detail_preserve")
    private val slimKey = floatPreferencesKey("face_slim")
    private val eyeSizeKey = floatPreferencesKey("eye_size")
    private val noseKey = floatPreferencesKey("nose_refine")

    val settingsFlow: Flow<BeautySettings> = context.dataStore.data.map { prefs ->
        val preset = runCatching { BeautyPreset.valueOf(prefs[presetKey] ?: BeautyPreset.STAGE.name) }
            .getOrDefault(BeautyPreset.STAGE)
        val quality = QualityLevel.fromName(prefs[qualityKey] ?: QualityLevel.MEDIUM.name)
        val debug = prefs[debugKey] ?: false
        val mirror = prefs[mirrorKey] ?: true
        val reflectionScene = runCatching {
            ReflectionScene.valueOf(prefs[reflectionSceneKey] ?: ReflectionScene.DARK_LAKE.name)
        }.getOrDefault(ReflectionScene.DARK_LAKE)
        val effectsSchema = prefs[effectsSchemaKey] ?: 1
        // Schema 15: settled mix / ripple / sky-blue. Schema 16: sunlight, warmth, sat, foam, clouds.
        val lakeIntensity = if (effectsSchema < 15) 0.92f else (prefs[lakeIntensityKey] ?: 0.92f)
        val lakeMotion = if (effectsSchema < 15) 0.55f else (prefs[lakeMotionKey] ?: 0.55f)
        val lakeDarkness = if (effectsSchema < 15) 0.08f else (prefs[lakeDarknessKey] ?: 0.08f)
        val lakeFaceClarity = if (effectsSchema < 15) 0.90f else (prefs[lakeFaceClarityKey] ?: 0.90f)
        val lakeCameraBlend = if (effectsSchema < 15) 0.58f else (prefs[lakeCameraBlendKey] ?: 0.58f)
        val lakeDeformation = if (effectsSchema < 15) 0.40f else (prefs[lakeDeformationKey] ?: 0.40f)
        val lakeSwirl = if (effectsSchema < 15) 0.70f else (prefs[lakeSwirlKey] ?: 0.70f)
        val lakeSettledWater = if (effectsSchema < 15) 0.42f else (prefs[lakeSettledWaterKey] ?: 0.42f)
        val lakeSettledCamera = if (effectsSchema < 15) 0.72f else (prefs[lakeSettledCameraKey] ?: 0.72f)
        val lakeRippleRegions = if (effectsSchema < 15) 0.40f else (prefs[lakeRippleRegionsKey] ?: 0.40f)
        val lakeRippleSpeed = if (effectsSchema < 15) 0.55f else (prefs[lakeRippleSpeedKey] ?: 0.55f)
        val lakeWaveDetail = if (effectsSchema < 15) 0.55f else (prefs[lakeWaveDetailKey] ?: 0.55f)
        val lakeSpecular = if (effectsSchema < 15) 0.50f else (prefs[lakeSpecularKey] ?: 0.50f)
        val lakeSkyBlue = if (effectsSchema < 15) 0.78f else (prefs[lakeSkyBlueKey] ?: 0.78f)
        val lakeSunlight = if (effectsSchema < 16) 0.70f else (prefs[lakeSunlightKey] ?: 0.70f)
        val lakeWaterWarmth = if (effectsSchema < 16) 0.22f else (prefs[lakeWaterWarmthKey] ?: 0.22f)
        val lakeSaturation = if (effectsSchema < 16) 0.78f else (prefs[lakeSaturationKey] ?: 0.78f)
        val lakeFoam = if (effectsSchema < 16) 0.55f else (prefs[lakeFoamKey] ?: 0.55f)
        val lakeClouds = if (effectsSchema < 16) 0.55f else (prefs[lakeCloudsKey] ?: 0.55f)
        val revealDurationSeconds = if (effectsSchema < 10) {
            10f
        } else {
            prefs[revealDurationKey] ?: 10f
        }
        when (preset) {
            BeautyPreset.CUSTOM -> {
                val global = prefs[globalKey] ?: 0.45f
                if (effectsSchema < 2) {
                    // Older releases were tuned so conservatively that most sliders looked like
                    // face exposure only. Rebase legacy custom looks onto the stronger v2 curve.
                    BeautySettings.fromGlobalStrength(global).copy(
                        qualityLevel = quality,
                        debugOverlay = debug,
                        mirrorPreview = mirror,
                        reflectionScene = reflectionScene,
                        lakeIntensity = lakeIntensity,
                        lakeMotion = lakeMotion,
                        lakeDarkness = lakeDarkness,
                        lakeFaceClarity = lakeFaceClarity,
                        lakeCameraBlend = lakeCameraBlend,
                        lakeDeformation = lakeDeformation,
                        lakeSwirl = lakeSwirl,
                        lakeSettledWater = lakeSettledWater,
                        lakeSettledCamera = lakeSettledCamera,
                        lakeRippleRegions = lakeRippleRegions,
                        lakeRippleSpeed = lakeRippleSpeed,
                        lakeWaveDetail = lakeWaveDetail,
                        lakeSpecular = lakeSpecular,
                        lakeSkyBlue = lakeSkyBlue,
                        lakeSunlight = lakeSunlight,
                        lakeWaterWarmth = lakeWaterWarmth,
                        lakeSaturation = lakeSaturation,
                        lakeFoam = lakeFoam,
                        lakeClouds = lakeClouds,
                        revealDurationSeconds = revealDurationSeconds,
                    ).clamped()
                } else {
                    BeautySettings(
                        preset = BeautyPreset.CUSTOM,
                        globalStrength = global,
                        smoothingStrength = prefs[smoothKey] ?: (global * 0.88f),
                        smoothingRadius = prefs[smoothRadiusKey] ?: (4.0f + global * 3.0f),
                        detailRetention = prefs[detailRetentionKey] ?: (0.78f - global * 0.30f),
                        complexionEvenness = prefs[complexionKey] ?: (global * 0.64f),
                        rednessCorrection = prefs[rednessKey] ?: (global * 0.42f),
                        blemishControl = prefs[blemishKey] ?: (global * 0.58f),
                        shineControl = prefs[shineKey] ?: (global * 0.46f),
                        skinGlow = prefs[skinGlowKey] ?: (global * 0.34f),
                        underEyeStrength = prefs[underEyeKey] ?: (global * 0.72f),
                        underEyeSmoothing = prefs[underEyeSmoothKey] ?: (global * 0.60f),
                        underEyeMaximumLift = prefs[underEyeLiftKey] ?: 0.18f,
                        underEyeColorCorrection = prefs[underEyeColorKey] ?: 0.34f,
                        faceExposure = prefs[exposureKey] ?: (global * 0.08f),
                        shadowLift = prefs[shadowKey] ?: (global * 0.15f),
                        highlightProtection = prefs[highlightKey] ?: 0.52f,
                        warmth = prefs[warmthKey] ?: (global * 0.10f),
                        localContrast = prefs[contrastKey] ?: (global * 0.14f),
                        contourStrength = prefs[contourKey] ?: (global * 0.38f),
                        blushStrength = prefs[blushKey] ?: (global * 0.24f),
                        eyeClarity = prefs[eyeKey] ?: (global * 0.50f),
                        eyeBrightening = prefs[eyeBrightKey] ?: (global * 0.28f),
                        eyeSparkle = prefs[eyeSparkleKey] ?: (global * 0.42f),
                        browDefinition = prefs[browDefinitionKey] ?: (global * 0.36f),
                        teethWhitening = prefs[teethKey] ?: (global * 0.24f),
                        lipEnhancement = prefs[lipsKey] ?: (global * 0.58f),
                        lipTintStrength = prefs[lipTintKey] ?: (global * 0.56f),
                        lipDefinition = prefs[lipDefinitionKey] ?: (global * 0.50f),
                        lipGloss = prefs[lipGlossKey] ?: (global * 0.34f),
                        detailPreservation = prefs[detailPreserveKey] ?: 0.78f,
                        faceSlimming = prefs[slimKey] ?: (global * 0.34f),
                        eyeEnlargement = prefs[eyeSizeKey] ?: (global * 0.23f),
                        noseRefinement = prefs[noseKey] ?: (global * 0.18f),
                        qualityLevel = quality,
                        debugOverlay = debug,
                        mirrorPreview = mirror,
                        reflectionScene = reflectionScene,
                        lakeIntensity = lakeIntensity,
                        lakeMotion = lakeMotion,
                        lakeDarkness = lakeDarkness,
                        lakeFaceClarity = lakeFaceClarity,
                        lakeCameraBlend = lakeCameraBlend,
                        lakeDeformation = lakeDeformation,
                        lakeSwirl = lakeSwirl,
                        lakeSettledWater = lakeSettledWater,
                        lakeSettledCamera = lakeSettledCamera,
                        lakeRippleRegions = lakeRippleRegions,
                        lakeRippleSpeed = lakeRippleSpeed,
                        lakeWaveDetail = lakeWaveDetail,
                        lakeSpecular = lakeSpecular,
                        lakeSkyBlue = lakeSkyBlue,
                        lakeSunlight = lakeSunlight,
                        lakeWaterWarmth = lakeWaterWarmth,
                        lakeSaturation = lakeSaturation,
                        lakeFoam = lakeFoam,
                        lakeClouds = lakeClouds,
                        revealDurationSeconds = revealDurationSeconds,
                    ).clamped()
                }
            }
            else -> BeautySettings.fromPreset(preset).copy(
                qualityLevel = if (preset == BeautyPreset.STAGE || preset == BeautyPreset.GLAM) {
                    BeautySettings.fromPreset(preset).qualityLevel
                } else {
                    quality
                },
                debugOverlay = debug,
                mirrorPreview = mirror,
                reflectionScene = reflectionScene,
                lakeIntensity = lakeIntensity,
                lakeMotion = lakeMotion,
                lakeDarkness = lakeDarkness,
                lakeFaceClarity = lakeFaceClarity,
                lakeCameraBlend = lakeCameraBlend,
                lakeDeformation = lakeDeformation,
                lakeSwirl = lakeSwirl,
                lakeSettledWater = lakeSettledWater,
                lakeSettledCamera = lakeSettledCamera,
                lakeRippleRegions = lakeRippleRegions,
                lakeRippleSpeed = lakeRippleSpeed,
                lakeWaveDetail = lakeWaveDetail,
                lakeSpecular = lakeSpecular,
                lakeSkyBlue = lakeSkyBlue,
                lakeSunlight = lakeSunlight,
                lakeWaterWarmth = lakeWaterWarmth,
                lakeSaturation = lakeSaturation,
                lakeFoam = lakeFoam,
                lakeClouds = lakeClouds,
                revealDurationSeconds = revealDurationSeconds,
            )
        }
    }

    suspend fun save(settings: BeautySettings) {
        context.dataStore.edit { prefs ->
            prefs[effectsSchemaKey] = CURRENT_EFFECTS_SCHEMA
            prefs[presetKey] = settings.preset.name
            prefs[globalKey] = settings.globalStrength
            prefs[qualityKey] = settings.qualityLevel.name
            prefs[debugKey] = settings.debugOverlay
            prefs[mirrorKey] = settings.mirrorPreview
            prefs[reflectionSceneKey] = settings.reflectionScene.name
            prefs[lakeIntensityKey] = settings.lakeIntensity
            prefs[lakeMotionKey] = settings.lakeMotion
            prefs[lakeDarknessKey] = settings.lakeDarkness
            prefs[lakeFaceClarityKey] = settings.lakeFaceClarity
            prefs[lakeCameraBlendKey] = settings.lakeCameraBlend
            prefs[lakeDeformationKey] = settings.lakeDeformation
            prefs[lakeSwirlKey] = settings.lakeSwirl
            prefs[lakeSettledWaterKey] = settings.lakeSettledWater
            prefs[lakeSettledCameraKey] = settings.lakeSettledCamera
            prefs[lakeRippleRegionsKey] = settings.lakeRippleRegions
            prefs[lakeRippleSpeedKey] = settings.lakeRippleSpeed
            prefs[lakeWaveDetailKey] = settings.lakeWaveDetail
            prefs[lakeSpecularKey] = settings.lakeSpecular
            prefs[lakeSkyBlueKey] = settings.lakeSkyBlue
            prefs[lakeSunlightKey] = settings.lakeSunlight
            prefs[lakeWaterWarmthKey] = settings.lakeWaterWarmth
            prefs[lakeSaturationKey] = settings.lakeSaturation
            prefs[lakeFoamKey] = settings.lakeFoam
            prefs[lakeCloudsKey] = settings.lakeClouds
            prefs[revealDurationKey] = settings.revealDurationSeconds
            prefs[smoothKey] = settings.smoothingStrength
            prefs[smoothRadiusKey] = settings.smoothingRadius
            prefs[detailRetentionKey] = settings.detailRetention
            prefs[complexionKey] = settings.complexionEvenness
            prefs[rednessKey] = settings.rednessCorrection
            prefs[blemishKey] = settings.blemishControl
            prefs[shineKey] = settings.shineControl
            prefs[skinGlowKey] = settings.skinGlow
            prefs[underEyeKey] = settings.underEyeStrength
            prefs[underEyeSmoothKey] = settings.underEyeSmoothing
            prefs[underEyeLiftKey] = settings.underEyeMaximumLift
            prefs[underEyeColorKey] = settings.underEyeColorCorrection
            prefs[exposureKey] = settings.faceExposure
            prefs[shadowKey] = settings.shadowLift
            prefs[highlightKey] = settings.highlightProtection
            prefs[warmthKey] = settings.warmth
            prefs[contrastKey] = settings.localContrast
            prefs[contourKey] = settings.contourStrength
            prefs[blushKey] = settings.blushStrength
            prefs[eyeKey] = settings.eyeClarity
            prefs[eyeBrightKey] = settings.eyeBrightening
            prefs[eyeSparkleKey] = settings.eyeSparkle
            prefs[browDefinitionKey] = settings.browDefinition
            prefs[teethKey] = settings.teethWhitening
            prefs[lipsKey] = settings.lipEnhancement
            prefs[lipTintKey] = settings.lipTintStrength
            prefs[lipDefinitionKey] = settings.lipDefinition
            prefs[lipGlossKey] = settings.lipGloss
            prefs[detailPreserveKey] = settings.detailPreservation
            prefs[slimKey] = settings.faceSlimming
            prefs[eyeSizeKey] = settings.eyeEnlargement
            prefs[noseKey] = settings.noseRefinement
        }
    }

    companion object {
        private const val CURRENT_EFFECTS_SCHEMA = 16
    }
}
