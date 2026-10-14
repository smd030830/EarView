package com.example.earview.detection

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * On-device [SoundDetector]: microphone chunks go through a sliding window into the classifier and
 * the scores are turned into danger sounds (BL-005, BL-006).
 *
 * Audio lives only in memory, in arrays that are overwritten as new audio arrives (BL-007).
 * The microphone and the model are opened when collection starts and released when it ends.
 */
class YamnetSoundDetector(
  private val source: AudioSource,
  private val classifierFactory: () -> SoundClassifier,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SoundDetector {
  override fun detect(): Flow<DangerSound> = flow {
    classifierFactory().use { classifier ->
      val window = SlidingAudioWindow(classifier.windowSize)
      // Windows overlap by half, so a sound is heard within about a window and a half of starting.
      val hop = classifier.windowSize / WINDOWS_OVERLAP
      source.chunks(hop).collect { chunk ->
        val audio = window.append(chunk) ?: return@collect
        detectedSounds(classifier.classify(audio)).forEach { emit(it) }
      }
    }
  }.flowOn(dispatcher)

  private companion object {
    const val WINDOWS_OVERLAP = 2
  }
}
