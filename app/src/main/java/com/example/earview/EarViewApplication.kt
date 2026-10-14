package com.example.earview

import android.app.Application
import android.content.Context
import com.example.earview.detection.DetectionController
import com.example.earview.detection.NoOpSoundDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EarViewApplication : Application() {
  /** The one detection state holder shared by the UI and the detection service. */
  val detectionController: DetectionController by lazy {
    // TODO(BL-005, BL-006): replace with the on-device YAMNet detector.
    DetectionController(NoOpSoundDetector(), CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
  }
}

val Context.detectionController: DetectionController
  get() = (applicationContext as EarViewApplication).detectionController
