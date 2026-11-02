package com.example.earview.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityAlertVibratorTest {
  private val delegate = FakeAlertVibrator()
  private var elapsed = 0L
  private val vibrator = PriorityAlertVibrator(delegate) { elapsed }

  @Test
  fun simultaneousLowerPrioritySoundsDoNotReplaceFireAlarm() {
    DangerSound.entries.forEach(vibrator::vibrate)

    assertEquals(listOf(DangerSound.FIRE_ALARM), delegate.vibrations)
  }

  @Test
  fun higherPrioritySoundInterruptsImmediately() {
    vibrator.vibrate(DangerSound.DOORBELL)
    elapsed = 1L
    vibrator.vibrate(DangerSound.SIREN)

    assertEquals(listOf(DangerSound.DOORBELL, DangerSound.SIREN), delegate.vibrations)
  }

  @Test
  fun duplicateDoesNotRestartAnActivePattern() {
    vibrator.vibrate(DangerSound.FIRE_ALARM)
    elapsed = 100L
    vibrator.vibrate(DangerSound.FIRE_ALARM)

    assertEquals(listOf(DangerSound.FIRE_ALARM), delegate.vibrations)
  }

  @Test
  fun lowerPrioritySoundCanStartExactlyWhenThePatternFinishes() {
    vibrator.vibrate(DangerSound.FIRE_ALARM)
    elapsed = DangerSound.FIRE_ALARM.vibrationTimings.sum() - 1L
    vibrator.vibrate(DangerSound.DOORBELL)
    assertEquals(listOf(DangerSound.FIRE_ALARM), delegate.vibrations)

    elapsed++
    vibrator.vibrate(DangerSound.DOORBELL)

    assertEquals(listOf(DangerSound.FIRE_ALARM, DangerSound.DOORBELL), delegate.vibrations)
  }

  @Test
  fun higherPriorityInterruptionUsesTheNewPatternsFullDuration() {
    vibrator.vibrate(DangerSound.CAR_HORN)
    elapsed = 100L
    vibrator.vibrate(DangerSound.FIRE_ALARM)
    elapsed += DangerSound.FIRE_ALARM.vibrationTimings.sum() - 1L
    vibrator.vibrate(DangerSound.SIREN)
    assertEquals(listOf(DangerSound.CAR_HORN, DangerSound.FIRE_ALARM), delegate.vibrations)

    elapsed++
    vibrator.vibrate(DangerSound.SIREN)

    assertEquals(listOf(DangerSound.CAR_HORN, DangerSound.FIRE_ALARM, DangerSound.SIREN), delegate.vibrations)
  }

  @Test
  fun cancelReleasesPriorityAndAllowsImmediateRestart() {
    vibrator.vibrate(DangerSound.FIRE_ALARM)
    vibrator.cancel()
    vibrator.vibrate(DangerSound.DOOR_KNOCK)

    assertEquals(1, delegate.cancelCount)
    assertEquals(listOf(DangerSound.FIRE_ALARM, DangerSound.DOOR_KNOCK), delegate.vibrations)
  }

  @Test
  fun duplicatePatternCanRestartAfterItsDuration() {
    vibrator.vibrate(DangerSound.SIREN)
    elapsed = DangerSound.SIREN.vibrationTimings.sum()
    vibrator.vibrate(DangerSound.SIREN)

    assertEquals(listOf(DangerSound.SIREN, DangerSound.SIREN), delegate.vibrations)
  }

  @Test
  fun suppressingLowerPriorityDoesNotExtendTheCurrentPattern() {
    vibrator.vibrate(DangerSound.SIREN)
    elapsed = DangerSound.SIREN.vibrationTimings.sum() - 1L
    vibrator.vibrate(DangerSound.DOORBELL)
    elapsed++
    vibrator.vibrate(DangerSound.DOORBELL)

    assertEquals(listOf(DangerSound.SIREN, DangerSound.DOORBELL), delegate.vibrations)
  }
}
