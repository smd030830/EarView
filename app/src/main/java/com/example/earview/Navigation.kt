package com.example.earview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.earview.ui.main.MainScreen
import com.example.earview.ui.onboarding.OnboardingScreen
import com.example.earview.ui.onboarding.SharedPreferencesOnboardingStore

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val onboardingStore = remember { SharedPreferencesOnboardingStore(context) }
  // The onboarding is shown on the first launch only.
  val startKey: NavKey = if (onboardingStore.isCompleted) Main else Onboarding
  val backStack = rememberNavBackStack(startKey)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
    entryProvider {
      entry<Onboarding> {
        OnboardingScreen(
          onFinished = {
            onboardingStore.isCompleted = true
            // Replace the onboarding so that back from the main screen leaves the app.
            backStack.add(Main)
            backStack.remove(Onboarding)
          },
        )
      }
      entry<Main> {
        MainScreen()
      }
    },
  )
}
