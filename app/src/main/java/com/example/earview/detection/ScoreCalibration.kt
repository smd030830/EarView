package com.example.earview.detection

/** Identifies the source model independently of its position in the ensemble. */
internal enum class SoundModel {
  YAMNET,
  EFFICIENTAT,
  PANNS,
}

/** Internal score conversion: (raw * scale + offset), limited to the shared 0..1 range. */
internal data class ScoreCorrection(val scale: Float = 1f, val offset: Float = 0f) {
  init {
    require(scale.isFinite() && scale > 0f) { "Score scale must be finite and positive" }
    require(offset.isFinite()) { "Score offset must be finite" }
  }

  fun apply(raw: Float): Float {
    require(raw.isFinite() && raw in 0f..1f) { "Invalid raw model confidence" }
    // Double arithmetic prevents finite coefficients from overflowing before limiting the result.
    return (raw.toDouble() * scale + offset).coerceIn(0.0, 1.0).toFloat()
  }
}

/** All six sounds must be configured explicitly; absent entries never silently skip conversion. */
internal class ModelScoreCalibration(corrections: Map<DangerSound, ScoreCorrection>) {
  private val corrections = corrections.toMap()

  init {
    require(this.corrections.keys == DangerSound.entries.toSet()) { "Configure every danger sound" }
  }

  fun apply(rawScores: FloatArray): FloatArray {
    require(rawScores.size == DangerSound.entries.size) { "Expected six raw sound scores" }
    return FloatArray(rawScores.size) { index -> corrections.getValue(DangerSound.entries[index]).apply(rawScores[index]) }
  }
}

/**
 * Single configuration point for the 18 model/sound corrections (BL-003, BL-006).
 *
 * Pilot scale-only coefficients for glass/knock/horn passed one source-disjoint ESC-50
 * acceptance check; see tools/model_evaluation/tuning-report.json. Other sounds keep identity.
 * These are not calibrated probabilities or independently validated real-device coefficients.
 */
internal object SoundScoreCalibrations {
  private val byModel = mapOf(
    SoundModel.YAMNET to ModelScoreCalibration(
      mapOf(
        DangerSound.FIRE_ALARM to ScoreCorrection(scale = 1f),
        DangerSound.SIREN to ScoreCorrection(scale = 1f),
        DangerSound.GLASS_BREAKING to ScoreCorrection(scale = 11.636364f),
        DangerSound.DOORBELL to ScoreCorrection(scale = 1f),
        DangerSound.DOOR_KNOCK to ScoreCorrection(scale = 3.36842108f),
        DangerSound.CAR_HORN to ScoreCorrection(scale = 1.50588238f),
      ),
    ),
    SoundModel.EFFICIENTAT to ModelScoreCalibration(
      mapOf(
        DangerSound.FIRE_ALARM to ScoreCorrection(scale = 1f),
        DangerSound.SIREN to ScoreCorrection(scale = 1f),
        DangerSound.GLASS_BREAKING to ScoreCorrection(scale = 21.1146584f),
        DangerSound.DOORBELL to ScoreCorrection(scale = 1f),
        DangerSound.DOOR_KNOCK to ScoreCorrection(scale = 5.8444047f),
        DangerSound.CAR_HORN to ScoreCorrection(scale = 5.94003296f),
      ),
    ),
    SoundModel.PANNS to ModelScoreCalibration(
      mapOf(
        DangerSound.FIRE_ALARM to ScoreCorrection(scale = 1f),
        DangerSound.SIREN to ScoreCorrection(scale = 1f),
        DangerSound.GLASS_BREAKING to ScoreCorrection(scale = 22.8264236f),
        DangerSound.DOORBELL to ScoreCorrection(scale = 1f),
        DangerSound.DOOR_KNOCK to ScoreCorrection(scale = 1.70174873f),
        DangerSound.CAR_HORN to ScoreCorrection(scale = 4.56649971f),
      ),
    ),
  )

  fun forModel(model: SoundModel): ModelScoreCalibration = byModel.getValue(model)
}

/** Converts every model score before the ensemble sees it; audio and scores stay in memory. */
internal class CalibratedSoundClassifier(
  private val source: DangerScoreClassifier,
  private val calibration: ModelScoreCalibration,
) : DangerScoreClassifier {
  override val windowSize: Int get() = source.windowSize
  override val hopSize: Int get() = source.hopSize

  override fun dangerScores(window: FloatArray): FloatArray = calibration.apply(source.dangerScores(window))

  override fun close() = source.close()
}
