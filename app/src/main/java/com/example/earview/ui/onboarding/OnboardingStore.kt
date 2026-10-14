package com.example.earview.ui.onboarding

import android.content.Context
import androidx.core.content.edit

/** Remembers whether the one-time permission onboarding has been completed. */
interface OnboardingStore {
  var isCompleted: Boolean
}

class SharedPreferencesOnboardingStore(context: Context) : OnboardingStore {
  private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  override var isCompleted: Boolean
    get() = preferences.getBoolean(KEY_COMPLETED, false)
    set(value) = preferences.edit { putBoolean(KEY_COMPLETED, value) }

  private companion object {
    const val PREFERENCES_NAME = "onboarding"
    const val KEY_COMPLETED = "completed"
  }
}
