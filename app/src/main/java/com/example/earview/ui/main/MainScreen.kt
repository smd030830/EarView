package com.example.earview.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.earview.R
import com.example.earview.detection.AndroidDetectionServiceStarter
import com.example.earview.detection.DangerSound
import com.example.earview.detection.DetectionState
import com.example.earview.detectionController
import com.example.earview.permission.AndroidPermissionChecker
import com.example.earview.permission.openAppSettings
import com.example.earview.theme.EarViewTheme
import com.example.earview.theme.StatusDetected
import com.example.earview.theme.StatusIdle
import com.example.earview.theme.StatusOff

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = defaultMainScreenViewModel(),
) {
  val context = LocalContext.current
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val permissionGuideVisible by viewModel.permissionGuideVisible.collectAsStateWithLifecycle()
  MainScreenContent(
    state = state,
    onToggleDetection = viewModel::onToggleDetection,
    onDismissDetection = viewModel::onDismissDetection,
    modifier = modifier,
    permissionGuideVisible = permissionGuideVisible,
    onDismissPermissionGuide = viewModel::onDismissPermissionGuide,
    onOpenSettings = {
      viewModel.onDismissPermissionGuide()
      context.openAppSettings()
    },
  )
}

@Composable
private fun defaultMainScreenViewModel(): MainScreenViewModel {
  val context = LocalContext.current.applicationContext
  return viewModel {
    MainScreenViewModel(context.detectionController, AndroidDetectionServiceStarter(context), AndroidPermissionChecker(context))
  }
}

/** Stateless main screen. Every status is shown with color + icon + text (FR-12). */
@Composable
internal fun MainScreenContent(
  state: DetectionState,
  onToggleDetection: () -> Unit,
  onDismissDetection: () -> Unit,
  modifier: Modifier = Modifier,
  permissionGuideVisible: Boolean = false,
  onDismissPermissionGuide: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
) {
  val background = state.backgroundColor()
  val dismissLabel = stringResource(R.string.action_dismiss_detection)
  val dismissModifier =
    if (state is DetectionState.Detected) {
      Modifier.clickable(onClickLabel = dismissLabel, onClick = onDismissDetection)
    } else {
      Modifier
    }

  BoxWithConstraints(modifier.fillMaxSize().background(background).safeDrawingPadding().then(dismissModifier)) {
    val minimumHeight = maxHeight
    val compact = maxHeight < 700.dp
    Column(
      modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
        .verticalScroll(rememberScrollState()).heightIn(min = minimumHeight).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      BrandHeader()
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 24.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 24.dp else 32.dp),
      ) {
        StatusHeader(state)
        ToggleButton(isRunning = state != DetectionState.Off, contentColor = background, compact = compact, onClick = onToggleDetection)
        Text(
          text = stringResource(if (state is DetectionState.Detected) R.string.main_ui_dismiss_hint else R.string.main_ui_privacy),
          color = Color.White.copy(alpha = 0.9f),
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
        )
      }
      TargetSoundList(activeSounds = (state as? DetectionState.Detected)?.activeSounds.orEmpty())
    }
  }

  if (permissionGuideVisible) {
    PermissionGuideDialog(onOpenSettings = onOpenSettings, onDismiss = onDismissPermissionGuide)
  }
}

@Composable
private fun BrandHeader() {
  if (LocalDensity.current.fontScale > 1.3f) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text("EarView", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
      Text(stringResource(R.string.main_ui_tagline), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
    }
    return
  }
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
    Text("EarView", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
    Text(
      stringResource(R.string.main_ui_tagline),
      color = Color.White.copy(alpha = 0.85f),
      style = MaterialTheme.typography.labelLarge,
      textAlign = TextAlign.End,
      modifier = Modifier.weight(1f).padding(start = 16.dp),
    )
  }
}

/** Explains why detection did not start and offers a way to the settings screen (BL-022, BL-023). */
@Composable
private fun PermissionGuideDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(imageVector = Icons.Filled.Warning, contentDescription = null) },
    title = { Text(stringResource(R.string.permission_guide_title)) },
    text = { Text(stringResource(R.string.permission_guide_message)) },
    confirmButton = { TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.permission_guide_open_settings)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.permission_guide_close)) } },
  )
}

@Composable
private fun StatusHeader(state: DetectionState, modifier: Modifier = Modifier) {
  val text = state.statusText()
  val spoken = state.spokenText()
  Column(
    modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
      contentDescription = spoken
      liveRegion = LiveRegionMode.Polite
      heading()
    },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.12f)) {
      Icon(imageVector = state.icon(), contentDescription = null, tint = Color.White, modifier = Modifier.padding(14.dp).size(32.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
      color = Color.White,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(
        when (state) {
          DetectionState.Off -> R.string.main_ui_off_description
          DetectionState.Idle -> R.string.main_ui_idle_description
          is DetectionState.Detected -> R.string.main_ui_detected_description
        },
      ),
      color = Color.White.copy(alpha = 0.9f),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun ToggleButton(isRunning: Boolean, contentColor: Color, compact: Boolean, onClick: () -> Unit) {
  val label = stringResource(if (isRunning) R.string.button_stop else R.string.button_start)
  val spoken = stringResource(if (isRunning) R.string.button_stop_a11y else R.string.button_start_a11y)
  Box(
    modifier = Modifier.size(if (compact) 224.dp else 256.dp).border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
      .padding(12.dp).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).padding(12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Button(
      onClick = onClick,
      shape = CircleShape,
      colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = contentColor),
      contentPadding = PaddingValues(12.dp),
      modifier = Modifier.fillMaxSize().semantics { contentDescription = spoken },
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.size(34.dp)) {
          val stroke = 3.dp.toPx()
          drawArc(contentColor, -55f, 290f, false, Offset(stroke, stroke), Size(size.width - stroke * 2, size.height - stroke * 2), style = Stroke(stroke, cap = StrokeCap.Round))
          drawLine(contentColor, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height * .45f), stroke, StrokeCap.Round)
        }
        Text(text = label, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun TargetSoundList(activeSounds: List<DangerSound>, modifier: Modifier = Modifier) {
  val spoken = stringResource(R.string.target_sounds, DangerSound.entries.joinedNames())
  Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.10f)) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.main_ui_targets), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.main_ui_target_count), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
      }
      Column(Modifier.semantics { contentDescription = spoken }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DangerSound.entries.chunked(2).forEach { pair ->
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { sound ->
              val detected = sound in activeSounds
              val name = stringResource(sound.labelRes())
              val label = if (detected) stringResource(R.string.status_detected, name) else name
              Row(
                modifier = Modifier.weight(1f)
                  .background(if (detected) Color(0xFFFFE2A8) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                  .clearAndSetSemantics { contentDescription = label }
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                if (detected) {
                  Icon(Icons.Filled.Warning, contentDescription = null, tint = StatusDetected, modifier = Modifier.size(20.dp))
                }
                Text(
                  text = label,
                  color = if (detected) StatusDetected else Color.White,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = if (detected) FontWeight.Bold else FontWeight.Normal,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun DetectionState.backgroundColor(): Color = when (this) {
  DetectionState.Off -> StatusOff
  DetectionState.Idle -> StatusIdle
  is DetectionState.Detected -> StatusDetected
}

private fun DetectionState.icon(): ImageVector = when (this) {
  DetectionState.Off -> Icons.Filled.Close
  DetectionState.Idle -> Icons.Filled.CheckCircle
  is DetectionState.Detected -> Icons.Filled.Warning
}

@Composable
private fun DetectionState.statusText(): String = when (this) {
  DetectionState.Off -> stringResource(R.string.status_off)
  DetectionState.Idle -> stringResource(R.string.status_idle)
  is DetectionState.Detected ->
    detectedText(R.string.status_detected, R.string.status_detected_overflow, R.string.status_detected_generic)
}

@Composable
private fun DetectionState.spokenText(): String = when (this) {
  DetectionState.Off -> stringResource(R.string.status_off_a11y)
  DetectionState.Idle -> stringResource(R.string.status_idle_a11y)
  is DetectionState.Detected ->
    detectedText(R.string.status_detected_a11y, R.string.status_detected_overflow_a11y, R.string.status_detected_generic_a11y)
}

@Composable
private fun DetectionState.Detected.detectedText(
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
  DangerSound.GLASS_BREAKING -> R.string.sound_glass_breaking
  DangerSound.DOORBELL -> R.string.sound_doorbell
  DangerSound.DOOR_KNOCK -> R.string.sound_door_knock
  DangerSound.CAR_HORN -> R.string.sound_car_horn
}

@Preview(showBackground = true)
@Composable
private fun MainScreenOffPreview() {
  EarViewTheme { MainScreenContent(DetectionState.Off, onToggleDetection = {}, onDismissDetection = {}) }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenIdlePreview() {
  EarViewTheme { MainScreenContent(DetectionState.Idle, onToggleDetection = {}, onDismissDetection = {}) }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenDetectedPreview() {
  EarViewTheme {
    MainScreenContent(
      DetectionState.Detected(listOf(DangerSound.FIRE_ALARM, DangerSound.SIREN, DangerSound.GLASS_BREAKING), overflowCount = 3),
      onToggleDetection = {},
      onDismissDetection = {},
    )
  }
}
