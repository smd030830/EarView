package com.example.earview.detection

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SlidingAudioWindowTest {
  private fun samples(vararg values: Int) = FloatArray(values.size) { values[it].toFloat() }

  private fun FloatArray?.ints() = this?.map { it.toInt() }?.toIntArray()

  @Test
  fun returnsNothingUntilTheWindowIsFull() {
    val window = SlidingAudioWindow(4)

    assertNull(window.append(samples(1, 2)))
  }

  @Test
  fun returnsTheWindowOnceFull() {
    val window = SlidingAudioWindow(4)
    window.append(samples(1, 2))

    assertArrayEquals(intArrayOf(1, 2, 3, 4), window.append(samples(3, 4)).ints())
  }

  @Test
  fun slidesByTheChunkSize_oldestSamplesFirst() {
    val window = SlidingAudioWindow(4)
    window.append(samples(1, 2))
    window.append(samples(3, 4))

    assertArrayEquals(intArrayOf(3, 4, 5, 6), window.append(samples(5, 6)).ints())
    assertArrayEquals(intArrayOf(5, 6, 7, 8), window.append(samples(7, 8)).ints())
  }

  @Test
  fun chunksOfAnySize_fillTheWindowSample_bySample() {
    val window = SlidingAudioWindow(4)

    assertNull(window.append(samples(1)))
    assertNull(window.append(samples(2, 3)))
    assertArrayEquals(intArrayOf(1, 2, 3, 4), window.append(samples(4)).ints())
    assertArrayEquals(intArrayOf(2, 3, 4, 5), window.append(samples(5)).ints())
  }

  @Test
  fun chunkLargerThanTheWindow_keepsTheNewestSamples() {
    val window = SlidingAudioWindow(3)

    assertArrayEquals(intArrayOf(3, 4, 5), window.append(samples(1, 2, 3, 4, 5)).ints())
  }
}
