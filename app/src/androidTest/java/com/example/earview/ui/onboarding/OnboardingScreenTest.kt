package com.example.earview.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.earview.theme.EarViewTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** UI tests for [OnboardingContent]. */
class OnboardingScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun show(
    state: OnboardingUiState = OnboardingUiState(PermissionStatus.Required, PermissionStatus.Required),
    onMicrophone: () -> Unit = {},
    onNotification: () -> Unit = {},
    onStart: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      EarViewTheme { OnboardingContent(state, onMicrophone, onNotification, onStart) }
    }
  }

  @Test
  fun explainsWhyEachPermissionIsNeeded() {
    show()

    composeTestRule.onNode(hasText("위험한 소리를 듣기 위해", substring = true)).assertExists()
    composeTestRule.onNode(hasText("고정 알림", substring = true)).assertExists()
  }

  @Test
  fun showsAssistiveToolDisclaimer() {
    show()

    composeTestRule.onNode(hasText("보조 도구", substring = true)).performScrollTo().assertExists()
  }

  @Test
  fun required_showsAllowButtons_andStatusText() {
    show()

    composeTestRule.onNodeWithText("마이크 허용").assertExists()
    composeTestRule.onNodeWithText("알림 허용").assertExists()
    composeTestRule.onNode(hasContentDescription("마이크 권한, 허용 필요")).assertExists()
  }

  @Test
  fun granted_hidesAllowButton_andShowsStatusText() {
    show(OnboardingUiState(PermissionStatus.Granted, PermissionStatus.Required))

    composeTestRule.onNodeWithText("마이크 허용").assertDoesNotExist()
    composeTestRule.onNode(hasContentDescription("마이크 권한, 허용됨")).assertExists()
  }

  @Test
  fun denied_offersSettingsButton() {
    var clicks = 0
    show(OnboardingUiState(PermissionStatus.Denied, PermissionStatus.Required), onMicrophone = { clicks++ })

    composeTestRule.onNode(hasContentDescription("마이크 권한, 허용되지 않음")).assertExists()
    composeTestRule.onNodeWithText("설정에서 마이크 허용").performClick()

    assertEquals(1, clicks)
  }

  @Test
  fun startButton_invokesCallback_evenWithoutPermissions() {
    var starts = 0
    show(onStart = { starts++ })

    composeTestRule.onNodeWithText("시작하기").performScrollTo().performClick()

    assertEquals(1, starts)
  }
}
