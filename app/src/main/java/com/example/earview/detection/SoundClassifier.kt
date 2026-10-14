package com.example.earview.detection

/** Scores a window of audio against the YAMNet classes. */
interface SoundClassifier : AutoCloseable {
  /** Number of samples [classify] needs. */
  val windowSize: Int

  /** Returns one score per YAMNet class for [window], which holds [windowSize] samples. */
  fun classify(window: FloatArray): FloatArray
}
