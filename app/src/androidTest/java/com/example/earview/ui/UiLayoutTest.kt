package com.example.earview.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.earview.detection.DangerSound
import com.example.earview.detection.DetectionState
import com.example.earview.theme.EarViewTheme
import com.example.earview.ui.main.MainScreenContent
import com.example.earview.ui.onboarding.OnboardingContent
import com.example.earview.ui.onboarding.OnboardingUiState
import com.example.earview.ui.onboarding.PermissionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real rendered states for visual review, including narrow layouts and enlarged text. */
class UiLayoutTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private fun render(dark: Boolean = false, large: Boolean = false, short: Boolean = false, content: @Composable () -> Unit) {
    compose.setContent {
      val density = LocalDensity.current
      EarViewTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 2f else 1f)) {
          Box(
            when {
              short -> Modifier.width(320.dp).height(360.dp)
              large -> Modifier.width(320.dp).fillMaxSize()
              else -> Modifier.fillMaxSize()
            },
          ) { content() }
        }
      }
    }
  }

  private fun save(name: String) {
    compose.waitForIdle()
    val folder = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "ui-review").apply { mkdirs() }
    File(folder, "$name.png").outputStream().use { stream ->
      assertTrue(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
    }
  }

  @Test
  fun mainOff() {
    render { MainScreenContent(DetectionState.Off, {}, {}) }
    compose.onNodeWithContentDescription("소리 감지 시작").assertIsDisplayed()
    save("main-off")
  }

  @Test
  fun mainIdle() {
    render { MainScreenContent(DetectionState.Idle, {}, {}) }
    compose.onNodeWithContentDescription("소리 감지 종료").assertIsDisplayed()
    save("main-idle")
  }

  @Test
  fun mainDetectedWithOverflow() {
    render {
      MainScreenContent(DetectionState.Detected(listOf(DangerSound.FIRE_ALARM, DangerSound.SIREN, DangerSound.GLASS_BREAKING), 3), {}, {})
    }
    compose.onNodeWithText("화재경보, 사이렌, 유리 깨짐 외 3건 감지").assertIsDisplayed()
    save("main-detected")
  }

  @Test
  fun mainNarrowLargeText_keepsButtonAndTargetsReachable() {
    render(large = true) { MainScreenContent(DetectionState.Idle, {}, {}) }
    compose.onNodeWithContentDescription("소리 감지 종료").performScrollTo().assertIsDisplayed()
    val layouts = mutableListOf<TextLayoutResult>()
    compose.onNodeWithText("종료", useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
    assertTrue(layouts.isNotEmpty())
    save("main-large-button")
    val layout = layouts.first()
    // A wrap-content Text can retain a wider paragraph constraint than its actual glyph bounds.
    // Check painted lines and visible characters, not hasVisualOverflow's paragraph-width flag.
    repeat(layout.lineCount) { line ->
      assertTrue("Control text extends beyond its width", layout.getLineRight(line) <= layout.size.width + 1f)
      assertTrue("Control text extends beyond its height", layout.getLineBottom(line) <= layout.size.height + 1f)
      assertFalse("Control text is ellipsized", layout.isLineEllipsized(line))
    }
    assertTrue("Control label must be fully visible", layout.getLineEnd(layout.lineCount - 1, visibleEnd = true) == "종료".length)
    compose.onNodeWithContentDescription("감지 대상: 화재경보, 사이렌, 유리 깨짐, 초인종, 문 두드림, 차량 경적").performScrollTo().assertIsDisplayed()
    save("main-large-targets")
  }

  @Test
  fun mainShortViewport_keepsControlsReachable() {
    render(short = true) { MainScreenContent(DetectionState.Idle, {}, {}) }
    compose.onNodeWithContentDescription("소리 감지 종료").performScrollTo().assertIsDisplayed()
    save("main-short-button")
    compose.onNodeWithContentDescription("감지 대상: 화재경보, 사이렌, 유리 깨짐, 초인종, 문 두드림, 차량 경적").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun onboardingLight() {
    render { OnboardingContent(OnboardingUiState(PermissionStatus.Required, PermissionStatus.Required), {}, {}, {}) }
    save("onboarding-light")
    compose.onNodeWithText("시작하기").performScrollTo().assertIsDisplayed()
    save("onboarding-light-bottom")
  }

  @Test
  fun onboardingDarkMixed() {
    render(dark = true) { OnboardingContent(OnboardingUiState(PermissionStatus.Denied, PermissionStatus.Granted), {}, {}, {}) }
    compose.onNodeWithContentDescription("마이크 권한, 허용되지 않음").assertExists()
    save("onboarding-dark")
  }

  @Test
  fun onboardingNarrowLargeText_keepsControlsReachable() {
    render(large = true) { OnboardingContent(OnboardingUiState(PermissionStatus.Required, PermissionStatus.Required), {}, {}, {}) }
    compose.onNodeWithText("마이크 허용").performScrollTo().assertIsDisplayed()
    save("onboarding-large-permission")
    compose.onNodeWithText("알림 허용").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("시작하기").performScrollTo().assertIsDisplayed()
    save("onboarding-large-bottom")
  }
}
