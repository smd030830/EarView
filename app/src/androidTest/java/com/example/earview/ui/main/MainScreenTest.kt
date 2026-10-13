package com.example.earview.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.earview.detection.DangerSound
import com.example.earview.theme.EarViewTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** UI tests for [MainScreenContent]. */
class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun show(state: MainScreenUiState, onToggle: () -> Unit = {}, onDismiss: () -> Unit = {}) {
    composeTestRule.setContent {
      EarViewTheme { MainScreenContent(state, onToggleDetection = onToggle, onDismissDetection = onDismiss) }
    }
  }

  @Test
  fun off_showsStatusText_andStartButton() {
    show(MainScreenUiState.Off)

    composeTestRule.onNodeWithText("꺼져 있음").assertExists()
    composeTestRule.onNodeWithText("시작").assertExists()
    composeTestRule.onNodeWithContentDescription("소리 감지 시작").assertExists()
  }

  @Test
  fun idle_showsStatusText_andStopButton() {
    show(MainScreenUiState.Idle)

    composeTestRule.onNodeWithText("정상 대기 중").assertExists()
    composeTestRule.onNodeWithText("종료").assertExists()
    composeTestRule.onNodeWithContentDescription("소리 감지 종료").assertExists()
  }

  @Test
  fun detected_showsSoundNames() {
    show(MainScreenUiState.Detected(listOf(DangerSound.FIRE_ALARM, DangerSound.SIREN), overflowCount = 0))

    composeTestRule.onNodeWithText("화재경보, 사이렌 감지").assertExists()
    composeTestRule.onNodeWithText("종료").assertExists()
  }

  @Test
  fun detected_withOverflow_showsOverflowCount() {
    show(
      MainScreenUiState.Detected(
        listOf(DangerSound.FIRE_ALARM, DangerSound.SIREN, DangerSound.BABY_CRYING),
        overflowCount = 2,
      ),
    )

    composeTestRule.onNodeWithText("화재경보, 사이렌, 아기 울음 외 2건 감지").assertExists()
  }

  @Test
  fun detected_isReadByTalkBackAsOneSentence() {
    show(MainScreenUiState.Detected(listOf(DangerSound.FIRE_ALARM, DangerSound.SIREN), overflowCount = 0))

    composeTestRule.onNode(hasContentDescription("화재경보, 사이렌 감지됨")).assertExists()
  }

  @Test
  fun toggleButton_invokesCallback() {
    var toggles = 0
    show(MainScreenUiState.Off, onToggle = { toggles++ })

    composeTestRule.onNodeWithText("시작").performClick()

    assertEquals(1, toggles)
  }

  @Test
  fun tapOnScreenWhileDetected_invokesDismiss() {
    var dismissals = 0
    show(MainScreenUiState.Detected(listOf(DangerSound.SIREN), overflowCount = 0), onDismiss = { dismissals++ })

    composeTestRule.onNodeWithText("사이렌 감지").performClick()

    assertEquals(1, dismissals)
  }

  @Test
  fun listsAllTargetSounds() {
    show(MainScreenUiState.Idle)

    composeTestRule.onNode(hasText("감지 대상:", substring = true)).assertExists()
  }
}
