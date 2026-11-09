package com.example.earview.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
  Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = stringResource(R.string.onboarding_ui_tagline),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = stringResource(R.string.onboarding_title),
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          text = stringResource(R.string.onboarding_intro),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
          Spacer(Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.onboarding_ui_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
          text = stringResource(R.string.onboarding_ui_permissions),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.semantics { heading() },
        )
        PermissionCard(
          stepRes = R.string.onboarding_ui_step_microphone,
          nameRes = R.string.permission_microphone,
          reasonRes = R.string.permission_microphone_reason,
          status = state.microphone,
          allowRes = R.string.permission_microphone_allow,
          allowInSettingsRes = R.string.permission_microphone_allow_in_settings,
          onClick = onMicrophoneClick,
        )
        PermissionCard(
          stepRes = R.string.onboarding_ui_step_notification,
          nameRes = R.string.permission_notification,
          reasonRes = R.string.permission_notification_reason,
          status = state.notification,
          allowRes = R.string.permission_notification_allow,
          allowInSettingsRes = R.string.permission_notification_allow_in_settings,
          onClick = onNotificationClick,
        )
      }

      Button(onClick = onStart, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
        Text(text = stringResource(R.string.onboarding_start), style = MaterialTheme.typography.titleMedium)
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = stringResource(R.string.onboarding_ui_disclaimer_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.disclaimer),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun PermissionCard(
  @StringRes stepRes: Int,
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

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
          Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(stepRes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(text = name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = status.icon(), contentDescription = null, tint = status.color(), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = statusText, style = MaterialTheme.typography.bodyMedium, color = status.color(), fontWeight = FontWeight.SemiBold)
          }
        }
      }
      Text(text = stringResource(reasonRes), style = MaterialTheme.typography.bodyLarge)
      if (status != PermissionStatus.Granted) {
        val label = stringResource(if (status == PermissionStatus.Denied) allowInSettingsRes else allowRes)
        if (status == PermissionStatus.Denied) {
          OutlinedButton(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(label) }
        } else {
          Button(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(label) }
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

@Composable
private fun PermissionStatus.color(): Color = when (this) {
  PermissionStatus.Granted -> MaterialTheme.colorScheme.primary
  PermissionStatus.Required -> MaterialTheme.colorScheme.onSurfaceVariant
  PermissionStatus.Denied -> MaterialTheme.colorScheme.error
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
