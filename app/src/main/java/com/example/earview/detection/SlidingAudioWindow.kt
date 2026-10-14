package com.example.earview.detection

/** Keeps the most recent [size] samples and hands them out once that many have arrived. */
internal class SlidingAudioWindow(private val size: Int) {
  private val window = FloatArray(size)
  private var filled = 0

  init {
    require(size > 0) { "The window must not be empty" }
  }

  /**
   * Adds [chunk] as the newest samples. Returns the latest [size] samples, oldest first, or null
   * while fewer than [size] samples have arrived. The returned array is reused by the next call,
   * so use it before appending again.
   */
  fun append(chunk: FloatArray): FloatArray? {
    val count = chunk.size
    if (count >= size) {
      chunk.copyInto(window, startIndex = count - size)
    } else {
      window.copyInto(window, destinationOffset = 0, startIndex = count, endIndex = size)
      chunk.copyInto(window, destinationOffset = size - count)
    }
    filled = minOf(size, filled + count)
    return if (filled == size) window else null
  }
}
