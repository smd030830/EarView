package com.example.earview.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.earview.R
import com.example.earview.detection.DangerSound
import com.example.earview.detection.NoOpSoundDetector
import com.example.earview.theme.EarViewTheme
import com.example.earview.theme.StatusDetected
import com.example.earview.theme.StatusIdle
import com.example.earview.theme.StatusOff

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(NoOpSoundDetector()) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  MainScreenContent(
    state = state,
    onToggleDetection = viewModel::onToggleDetection,
    onDismissDetection = viewModel::onDismissDetection,
    modifier = modifier,
  )
}

/** Stateless main screen. Every status is shown with color + icon + text (FR-12). */
@Composable
internal fun MainScreenContent(
  state: MainScreenUiState,
  onToggleDetection: () -> Unit,
  onDismissDetection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val background = state.backgroundColor()
  val dismissLabel = stringResource(R.string.action_dismiss_detection)
  val dismissModifier =
    if (state is MainScreenUiState.Detected) {
      Modifier.clickable(onClickLabel = dismissLabel, onClick = onDismissDetection)
    } else {
      Modifier
    }

  Box(modifier.fillMaxSize().background(background).then(dismissModifier)) {
    Column(
      modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      StatusHeader(state)
      Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        ToggleButton(isRunning = state != MainScreenUiState.Off, contentColor = background, onClick = onToggleDetection)
      }
      TargetSoundList()
    }
  }
}

@Composable
private fun StatusHeader(state: MainScreenUiState, modifier: Modifier = Modifier) {
  val text = state.statusText()
  val spoken = state.spokenText()
  Row(
    modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
      contentDescription = spoken
      liveRegion = LiveRegionMode.Polite
    },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(imageVector = state.icon(), contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
    Spacer(Modifier.width(12.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = Color.White,
      textAlign = TextAlign.Center,
      modifier = Modifier.weight(1f, fill = false),
    )
  }
}

@Composable
private fun ToggleButton(isRunning: Boolean, contentColor: Color, onClick: () -> Unit) {
  val label = stringResource(if (isRunning) R.string.button_stop else R.string.button_start)
  val spoken = stringResource(if (isRunning) R.string.button_stop_a11y else R.string.button_start_a11y)
  Button(
    onClick = onClick,
    shape = CircleShape,
    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = contentColor),
    modifier = Modifier.size(200.dp).semantics { contentDescription = spoken },
  ) {
    Text(text = label, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun TargetSoundList(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(R.string.target_sounds, DangerSound.entries.joinedNames()),
    style = MaterialTheme.typography.bodyLarge,
    color = Color.White,
    textAlign = TextAlign.Center,
    modifier = modifier.fillMaxWidth(),
  )
}

private fun MainScreenUiState.backgroundColor(): Color = when (this) {
  MainScreenUiState.Off -> StatusOff
  MainScreenUiState.Idle -> StatusIdle
  is MainScreenUiState.Detected -> StatusDetected
}

private fun MainScreenUiState.icon(): ImageVector = when (this) {
  MainScreenUiState.Off -> Icons.Filled.Close
  MainScreenUiState.Idle -> Icons.Filled.CheckCircle
  is MainScreenUiState.Detected -> Icons.Filled.Warning
}

@Composable
private fun MainScreenUiState.statusText(): String = when (this) {
  MainScreenUiState.Off -> stringResource(R.string.status_off)
  MainScreenUiState.Idle -> stringResource(R.string.status_idle)
  is MainScreenUiState.Detected ->
    detectedText(R.string.status_detected, R.string.status_detected_overflow, R.string.status_detected_generic)
}

@Composable
private fun MainScreenUiState.spokenText(): String = when (this) {
  MainScreenUiState.Off -> stringResource(R.string.status_off_a11y)
  MainScreenUiState.Idle -> stringResource(R.string.status_idle_a11y)
  is MainScreenUiState.Detected ->
    detectedText(R.string.status_detected_a11y, R.string.status_detected_overflow_a11y, R.string.status_detected_generic_a11y)
}

@Composable
private fun MainScreenUiState.Detected.detectedText(
  @StringRes listRes: Int,
  @StringRes overflowRes: Int,
  @StringRes genericRes: Int,
): String {
  if (sounds.isEmpty()) return stringResource(genericRes)
  val names = sounds.joinedNames()
  return if (overflowCount > 0) stringResource(overflowRes, names, overflowCount) else stringResource(listRes, names)
}

@Composable
private fun List<DangerSound>.joinedNames(): String {
  val separator = stringResource(R.string.sound_separator)
  return map { stringResource(it.labelRes()) }.joinToString(separator)
}

@StringRes
private fun DangerSound.labelRes(): Int = when (this) {
  DangerSound.FIRE_ALARM -> R.string.sound_fire_alarm
  DangerSound.SIREN -> R.string.sound_siren
  DangerSound.BABY_CRYING -> R.string.sound_baby_crying
  DangerSound.DOORBELL -> R.string.sound_doorbell
  DangerSound.CAR_HORN -> R.string.sound_car_horn
}

@Preview(showBackground = true)
@Composable
private fun MainScreenOffPreview() {
  EarViewTheme { MainScreenContent(MainScreenUiState.Off, onToggleDetection = {}, onDismissDetection = {}) }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenIdlePreview() {
  EarViewTheme { MainScreenContent(MainScreenUiState.Idle, onToggleDetection = {}, onDismissDetection = {}) }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenDetectedPreview() {
  EarViewTheme {
    MainScreenContent(
      MainScreenUiState.Detected(listOf(DangerSound.FIRE_ALARM, DangerSound.SIREN, DangerSound.BABY_CRYING), overflowCount = 2),
      onToggleDetection = {},
      onDismissDetection = {},
    )
  }
}
