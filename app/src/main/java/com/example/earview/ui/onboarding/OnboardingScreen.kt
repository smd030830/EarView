package com.example.earview.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.earview.R
import com.example.earview.permission.AndroidPermissionChecker
import com.example.earview.permission.openAppSettings
import com.example.earview.permission.openNotificationSettings
import com.example.earview.theme.EarViewTheme
import com.example.earview.theme.StatusDetected
import com.example.earview.theme.StatusIdle
import com.example.earview.theme.StatusOff

/** Permission onboarding, shown once on first launch (BL-021 to BL-024). */
@Composable
fun OnboardingScreen(
  onFinished: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OnboardingViewModel = defaultOnboardingViewModel(),
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  // The user may have changed permissions in the system settings while we were in the background.
  LifecycleResumeEffect(viewModel) {
    viewModel.refresh()
    onPauseOrDispose {}
  }

  val microphoneLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::onMicrophoneResult)
  val notificationLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::onNotificationResult)

  OnboardingContent(
    state = state,
    onMicrophoneClick = {
      if (state.microphone == PermissionStatus.Required) {
        microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
      } else {
        context.openAppSettings()
      }
    },
    onNotificationClick = {
      // Before Android 13 there is no runtime dialog; notifications can only be switched on in settings.
      if (state.notification == PermissionStatus.Required && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        context.openNotificationSettings()
      }
    },
    onStart = onFinished,
    modifier = modifier,
  )
}

@Composable
private fun defaultOnboardingViewModel(): OnboardingViewModel {
  val context = LocalContext.current.applicationContext
  return viewModel { OnboardingViewModel(AndroidPermissionChecker(context)) }
}

/** Stateless onboarding screen. Permission status is shown with icon + text + color (FR-12). */
@Composable
internal fun OnboardingContent(
  state: OnboardingUiState,
  onMicrophoneClick: () -> Unit,
  onNotificationClick: () -> Unit,
  onStart: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
    modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = stringResource(R.string.onboarding_title),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.semantics { heading() },
    )
    Text(text = stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyLarge)

    PermissionCard(
      nameRes = R.string.permission_microphone,
      reasonRes = R.string.permission_microphone_reason,
      status = state.microphone,
      allowRes = R.string.permission_microphone_allow,
      allowInSettingsRes = R.string.permission_microphone_allow_in_settings,
      onClick = onMicrophoneClick,
    )
    PermissionCard(
      nameRes = R.string.permission_notification,
      reasonRes = R.string.permission_notification_reason,
      status = state.notification,
      allowRes = R.string.permission_notification_allow,
      allowInSettingsRes = R.string.permission_notification_allow_in_settings,
      onClick = onNotificationClick,
    )

    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
      Text(text = stringResource(R.string.onboarding_start), style = MaterialTheme.typography.titleMedium)
    }

    // Kept at the bottom of the screen (BL-024).
    Text(
      text = stringResource(R.string.disclaimer),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PermissionCard(
  @StringRes nameRes: Int,
  @StringRes reasonRes: Int,
  status: PermissionStatus,
  @StringRes allowRes: Int,
  @StringRes allowInSettingsRes: Int,
  onClick: () -> Unit,
) {
  val name = stringResource(nameRes)
  val statusText = stringResource(status.textRes())
  val spoken = stringResource(R.string.permission_status_a11y, name, statusText)

  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = status.icon(),
          contentDescription = null,
          tint = status.color(),
          modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = statusText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
      Text(text = stringResource(reasonRes), style = MaterialTheme.typography.bodyLarge)
      if (status != PermissionStatus.Granted) {
        val label = stringResource(if (status == PermissionStatus.Denied) allowInSettingsRes else allowRes)
        if (status == PermissionStatus.Denied) {
          OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
        } else {
          Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
        }
      }
    }
  }
}

@StringRes
private fun PermissionStatus.textRes(): Int = when (this) {
  PermissionStatus.Granted -> R.string.permission_status_granted
  PermissionStatus.Required -> R.string.permission_status_required
  PermissionStatus.Denied -> R.string.permission_status_denied
}

private fun PermissionStatus.icon(): ImageVector = when (this) {
  PermissionStatus.Granted -> Icons.Filled.CheckCircle
  PermissionStatus.Required -> Icons.Filled.Warning
  PermissionStatus.Denied -> Icons.Filled.Close
}

private fun PermissionStatus.color(): Color = when (this) {
  PermissionStatus.Granted -> StatusIdle
  PermissionStatus.Required -> StatusOff
  PermissionStatus.Denied -> StatusDetected
}

@Preview(showBackground = true)
@Composable
private fun OnboardingInitialPreview() {
  EarViewTheme {
    OnboardingContent(
      OnboardingUiState(PermissionStatus.Required, PermissionStatus.Required),
      onMicrophoneClick = {},
      onNotificationClick = {},
      onStart = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingMixedPreview() {
  EarViewTheme {
    OnboardingContent(
      OnboardingUiState(PermissionStatus.Denied, PermissionStatus.Granted),
      onMicrophoneClick = {},
      onNotificationClick = {},
      onStart = {},
    )
  }
}
