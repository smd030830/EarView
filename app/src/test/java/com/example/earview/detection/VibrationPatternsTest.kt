package com.example.earview.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrationPatternsTest {
  @Test
  fun everySoundHasAPattern() {
    DangerSound.entries.forEach { assertTrue("$it has no pattern", it.vibrationTimings.size >= 2) }
  }

  @Test
  fun patternsAreDistinctPerSound() {
    val patterns = DangerSound.entries.map { it.vibrationTimings.toList() }

    assertEquals(DangerSound.entries.size, patterns.toSet().size)
  }

  @Test
  fun patternsStartImmediately_soTheAlertIsNotDelayed() {
    DangerSound.entries.forEach { assertEquals("$it", 0L, it.vibrationTimings.first()) }
  }

  @Test
  fun patternsAreWellFormed_andShortEnough() {
    DangerSound.entries.forEach { sound ->
      val timings = sound.vibrationTimings
      assertTrue("$sound must end with a vibration", timings.size % 2 == 0)
      assertTrue("$sound has a non-positive step", timings.drop(1).all { it > 0 })
      assertTrue("$sound is too long", timings.sum() <= MAX_VIBRATION_MILLIS)
    }
  }
}
