package com.example.earview.detection

import kotlinx.coroutines.flow.Flow

/** Sample rate the classifier expects. */
internal const val SAMPLE_RATE_HZ = 16_000

/**
 * Live microphone audio as mono 16 kHz float samples in [-1, 1].
 *
 * Audio only ever passes through memory: implementations must not write it anywhere.
 */
interface AudioSource {
  /**
   * Cold flow of consecutive chunks of [chunkSize] samples. Collecting it opens the microphone and
   * cancelling the collection releases it. The flow fails if the microphone cannot be used.
   */
  fun chunks(chunkSize: Int): Flow<FloatArray>
}
