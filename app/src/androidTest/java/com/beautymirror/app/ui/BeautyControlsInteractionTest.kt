package com.beautymirror.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.beautymirror.app.settings.AdaptivePerformanceState
import com.beautymirror.app.settings.BeautyPreset
import com.beautymirror.app.settings.BeautySettings
import com.beautymirror.app.settings.QualityLevel
import com.beautymirror.app.settings.QuickFixSession
import com.beautymirror.app.settings.ReflectionScene
import com.beautymirror.app.ui.theme.BeautyTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class BeautyControlsInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyStudioPageCanBeOpenedAndLakeControlsUpdateState() {
        var current by mutableStateOf(BeautySettings.off())
        compose.setContent {
            BeautyTheme {
                BeautyControls(
                    settings = current,
                    runtimeQuality = QualityLevel.MEDIUM,
                    performanceState = AdaptivePerformanceState.FULL,
                    timing = null,
                    onChange = { current = it },
                    onDismiss = {},
                )
            }
        }

        listOf("looks", "skin", "eyes", "lips", "shape", "scene", "system").forEach { page ->
            compose.onNodeWithTag("page_$page").performScrollTo().performClick()
        }

        compose.onNodeWithTag("page_scene").performScrollTo().performClick()
        compose.onNodeWithTag("scene_lake").performClick()
        compose.runOnIdle {
            assertThat(current.reflectionScene).isEqualTo(ReflectionScene.DARK_LAKE)
        }

        compose.onNodeWithTag("slider_lake_motion")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { set -> set(0.82f) }
        compose.runOnIdle {
            assertThat(current.lakeMotion).isWithin(0.02f).of(0.82f)
        }
    }

    @Test
    fun oneTapActionsPresetsAndResetRemainInteractive() {
        var current by mutableStateOf(BeautySettings.off())
        compose.setContent {
            BeautyTheme {
                BeautyControls(
                    settings = current,
                    runtimeQuality = QualityLevel.MEDIUM,
                    performanceState = AdaptivePerformanceState.FULL,
                    timing = null,
                    onChange = { current = it },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithTag("action_fresh_eyes").performScrollTo().performClick()
        compose.runOnIdle {
            assertThat(current.underEyeStrength).isAtLeast(0.72f)
        }

        compose.onNodeWithTag("action_stage_ready").performScrollTo().performClick()
        compose.runOnIdle {
            assertThat(current.preset).isEqualTo(BeautyPreset.CUSTOM)
        }

        compose.onNodeWithTag("reset_look").performClick()
        compose.runOnIdle {
            assertThat(current.preset).isEqualTo(BeautyPreset.OFF)
            assertThat(current.effectsEnabled).isFalse()
        }
    }

    @Test
    fun oneTapTogglesSurviveControlsRemount() {
        val session = QuickFixSession()
        var current by mutableStateOf(BeautySettings.off())
        var showControls by mutableStateOf(true)
        compose.setContent {
            BeautyTheme {
                if (showControls) {
                    BeautyControls(
                        settings = current,
                        runtimeQuality = QualityLevel.MEDIUM,
                        performanceState = AdaptivePerformanceState.FULL,
                        timing = null,
                        onChange = { current = it },
                        onDismiss = {},
                        quickFixSession = session,
                    )
                }
            }
        }

        compose.onNodeWithTag("action_fresh_eyes").performScrollTo().performClick()
        compose.runOnIdle {
            assertThat(current.underEyeStrength).isAtLeast(0.72f)
            assertThat(session.activeIds).contains("fresh_eyes")
        }

        compose.runOnIdle { showControls = false }
        compose.waitForIdle()
        compose.runOnIdle { showControls = true }
        compose.waitForIdle()

        compose.onNodeWithTag("action_fresh_eyes").assertExists()
        compose.onNodeWithTag("action_fresh_eyes").performScrollTo().performClick()
        compose.runOnIdle {
            assertThat(session.activeIds).doesNotContain("fresh_eyes")
            assertThat(current.underEyeStrength).isWithin(1e-3f).of(0f)
            assertThat(current.preset).isEqualTo(BeautyPreset.OFF)
        }
    }

    @Test
    fun presetSelectClearsOneTapStack() {
        val session = QuickFixSession()
        var current by mutableStateOf(BeautySettings.off())
        compose.setContent {
            BeautyTheme {
                BeautyControls(
                    settings = current,
                    runtimeQuality = QualityLevel.MEDIUM,
                    performanceState = AdaptivePerformanceState.FULL,
                    timing = null,
                    onChange = { current = it },
                    onDismiss = {},
                    quickFixSession = session,
                )
            }
        }

        compose.onNodeWithTag("action_fresh_eyes").performScrollTo().performClick()
        compose.runOnIdle {
            assertThat(session.activeIds).contains("fresh_eyes")
        }

        compose.onNodeWithTag("preset_off").performScrollTo().performClick()
        compose.runOnIdle {
            assertThat(session.activeIds).isEmpty()
            assertThat(current.preset).isEqualTo(BeautyPreset.OFF)
            assertThat(current.effectsEnabled).isFalse()
        }
    }

    @Test
    fun togglesChangeExactlyOncePerClick() {
        var current by mutableStateOf(BeautySettings.off())
        compose.setContent {
            BeautyTheme {
                BeautyControls(
                    settings = current,
                    runtimeQuality = QualityLevel.MEDIUM,
                    performanceState = AdaptivePerformanceState.FULL,
                    timing = null,
                    onChange = { current = it },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithTag("page_system").performScrollTo().performClick()
        val initial = current.mirrorPreview
        compose.onNodeWithTag("toggle_mirror").performScrollTo().performClick()
        compose.runOnIdle {
            assertThat(current.mirrorPreview).isEqualTo(!initial)
        }
        compose.onNodeWithTag("toggle_mirror").performClick()
        compose.runOnIdle {
            assertThat(current.mirrorPreview).isEqualTo(initial)
        }

        compose.onNodeWithTag("about_developed_by").performScrollTo().assertExists()
    }
}
