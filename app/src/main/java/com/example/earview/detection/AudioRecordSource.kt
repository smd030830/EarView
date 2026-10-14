package com.example.earview.detection

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/** Reads the microphone with [AudioRecord]. Needs the RECORD_AUDIO permission to be granted. */
class AudioRecordSource : AudioSource {
  @SuppressLint("MissingPermission")
  override fun chunks(chunkSize: Int): Flow<FloatArray> = flow {
    val minBufferBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_MASK, ENCODING)
    check(minBufferBytes > 0) { "The microphone does not support $SAMPLE_RATE_HZ Hz mono audio" }

    val record =
      AudioRecord.Builder()
        .setAudioSource(MediaRecorder.AudioSource.MIC)
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE_HZ)
            .setChannelMask(CHANNEL_MASK)
            .build(),
        )
        // Room for two chunks so a slow classification does not overrun the microphone buffer.
        .setBufferSizeInBytes(maxOf(minBufferBytes, chunkSize * BYTES_PER_SAMPLE * 2))
        .build()
    try {
      check(record.state == AudioRecord.STATE_INITIALIZED) { "The microphone could not be opened" }
      record.startRecording()
      check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "The microphone could not be started" }

      while (currentCoroutineContext().isActive) {
        // A new array per chunk, so a consumer may keep it for as long as it likes.
        val chunk = FloatArray(chunkSize)
        var filled = 0
        while (filled < chunkSize) {
          val read = record.read(chunk, filled, chunkSize - filled, AudioRecord.READ_BLOCKING)
          check(read >= 0) { "Reading the microphone failed with error $read" }
          filled += read
        }
        emit(chunk)
      }
    } finally {
      record.release()
    }
  }

  private companion object {
    const val CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_FLOAT
    const val BYTES_PER_SAMPLE = 4
  }
}
