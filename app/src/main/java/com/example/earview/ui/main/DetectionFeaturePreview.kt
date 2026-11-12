package com.example.earview.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.earview.R
import com.example.earview.detection.AudioRecordSource
import com.example.earview.detection.SAMPLE_RATE_HZ
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

@Composable
internal fun FeatureSettingsScreen(canRecord: Boolean, onBack: () -> Unit) {
  Surface(Modifier.fillMaxSize()) {
    Column(
      Modifier.safeDrawingPadding().widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TextButton(onClick = onBack) { Text(stringResource(R.string.feature_settings_back)) }
      Text(
        stringResource(R.string.feature_settings_open),
        Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
      )
      Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        FeatureSettingsContent(canRecord)
      }
    }
  }
}

@Composable
private fun FeatureSettingsContent(canRecord: Boolean) {
  var registrationOpen by remember { mutableStateOf(false) }
  var moreSensitive by rememberSaveable { mutableStateOf(false) }
  var noiseReduction by rememberSaveable { mutableStateOf(false) }
  var bandPass by rememberSaveable { mutableStateOf(false) }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
  ) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(stringResource(R.string.feature_preview_notice), style = MaterialTheme.typography.bodyMedium)
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        PreviewHeading(stringResource(R.string.feature_custom_sound))
        Text(stringResource(R.string.feature_custom_sound_description), style = MaterialTheme.typography.bodyMedium)
        if (!canRecord) {
          Text(stringResource(R.string.feature_recording_detection_running), style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = { registrationOpen = true }, enabled = canRecord) {
          Text(stringResource(R.string.feature_custom_sound_open))
        }
        PreviewHeading(stringResource(R.string.feature_recordings_title))
        Text(stringResource(R.string.feature_recordings_empty), style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider()
        PreviewHeading(stringResource(R.string.feature_sensitivity))
        Column(Modifier.selectableGroup()) {
          listOf(false to R.string.feature_sensitivity_standard, true to R.string.feature_sensitivity_high)
            .forEach { (option, label) ->
              Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                  .selectable(moreSensitive == option, role = Role.RadioButton) { moreSensitive = option },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                RadioButton(selected = moreSensitive == option, onClick = null)
                Text(stringResource(label))
              }
            }
        }
        Text(stringResource(R.string.feature_sensitivity_description), style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider()
        PreviewHeading(stringResource(R.string.feature_preprocessing))
        Text(stringResource(R.string.feature_preprocessing_description), style = MaterialTheme.typography.bodyMedium)
        PreviewSwitch(stringResource(R.string.feature_noise_reduction), noiseReduction) { noiseReduction = it }
        PreviewSwitch(stringResource(R.string.feature_band_pass), bandPass) { bandPass = it }
      }
    }
  }

  if (registrationOpen && canRecord) {
    RegistrationPreviewDialog(onDismiss = { registrationOpen = false })
  }
}

@Composable
private fun RegistrationPreviewDialog(onDismiss: () -> Unit) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var recording by remember { mutableStateOf(false) }
  var levels by remember { mutableStateOf<FloatArray?>(null) }
  var status by remember { mutableStateOf(R.string.feature_waveform_ready) }
  val requestMicrophone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) {
      recording = true
    } else {
      status = R.string.feature_waveform_permission
    }
  }
  LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
    if (recording) {
      recording = false
      status = R.string.feature_waveform_stopped
    }
  }
  LaunchedEffect(recording) {
    if (!recording) return@LaunchedEffect
    levels = null
    status = R.string.feature_waveform_starting
    try {
      withTimeoutOrNull(30_000L) {
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        AudioRecordSource().chunks(SAMPLE_RATE_HZ / 12)
          .map { samples ->
            FloatArray(32) { bar ->
              val from = bar * samples.size / 32
              val end = (bar + 1) * samples.size / 32
              var peak = 0f
              for (i in from until end) peak = maxOf(peak, abs(samples[i]))
              peak.coerceIn(0f, 1f)
            }
          }
          .flowOn(Dispatchers.IO)
          .collect {
            levels = it
            status = R.string.feature_waveform_recording
          }
      }
      status = R.string.feature_waveform_stopped
      recording = false
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      status = R.string.feature_waveform_failed
      recording = false
    }
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.feature_custom_sound)) },
    text = {
      Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(stringResource(R.string.feature_custom_sound_steps))
        Text(stringResource(status), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        levels?.let { MicrophoneWaveform(it) }
        Button(onClick = {
          if (recording) {
            recording = false
            status = R.string.feature_waveform_stopped
          } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            recording = true
          } else {
            requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
          }
        }) {
          Text(stringResource(if (recording) R.string.feature_waveform_stop else R.string.feature_waveform_start))
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_preview_close)) }
    },
  )
}

@Composable
private fun MicrophoneWaveform(levels: FloatArray) {
  val color = MaterialTheme.colorScheme.onSecondaryContainer
  Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
    Canvas(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp)) {
      val step = size.width / levels.size
      levels.forEachIndexed { index, level ->
        val halfHeight = size.height * level / 2
        val x = (index + 0.5f) * step
        drawLine(
          color, Offset(x, center.y - halfHeight), Offset(x, center.y + halfHeight),
          strokeWidth = minOf(3.dp.toPx(), step / 2), cap = StrokeCap.Round,
        )
      }
    }
  }
}

@Composable
private fun PreviewHeading(text: String) {
  Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun PreviewSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    Modifier.fillMaxWidth().heightIn(min = 48.dp)
      .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(label, Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = null)
  }
}
