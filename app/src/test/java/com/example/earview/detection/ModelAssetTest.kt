package com.example.earview.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Guards the bundled model and the "audio never leaves the device" rule (BL-005, BL-007). */
class ModelAssetTest {
  // Unit tests run with the app module as the working directory.
  private val moduleDir = File(".")

  @Test
  fun bundledModelIsTheKnownYamnetFile() {
    val model = File(moduleDir, "src/main/assets/yamnet.tflite")

    val sha256 = MessageDigest.getInstance("SHA-256").digest(model.readBytes()).joinToString("") { "%02x".format(it) }

    assertEquals(YAMNET_SHA256, sha256)
  }

  @Test
  fun appCannotUseTheNetwork() {
    val manifest = File(moduleDir, "src/main/AndroidManifest.xml").readText()

    assertFalse(manifest.contains("android.permission.INTERNET"))
  }

  private companion object {
    // lite-model_yamnet_classification_tflite_1.tflite, from
    // https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/audio_classification/android/
    const val YAMNET_SHA256 = "10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de"
  }
}
