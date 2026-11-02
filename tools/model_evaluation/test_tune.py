import unittest

import numpy as np

from tune import corrected_peaks, evaluate, fit_scale


class TuningTest(unittest.TestCase):
    def fixture(self):
        frames = np.full((200, 3, 2, 6), .01, dtype=np.float32)
        truth = np.full(200, -1)
        truth[:12] = 2
        truth[100:112] = 2
        frames[truth == 2, :, :, 2] = .2
        folds = np.r_[np.ones(100), np.full(100, 4)]
        return frames, truth, folds

    def test_accepts_recall_gain_with_no_extra_nominal_false_positives(self):
        scales, report = evaluate(*self.fixture())
        self.assertTrue(report['GLASS_BREAKING']['accepted'])
        self.assertEqual(12, report['GLASS_BREAKING']['candidate']['true_positives'])
        np.testing.assert_allclose(scales[:, 2], 2.5)
        self.assertFalse(report['FIRE_ALARM']['accepted'])
        np.testing.assert_allclose(scales[:, 0], 1)

    def test_rejects_candidate_that_adds_held_out_false_positives(self):
        frames, truth, folds = self.fixture()
        frames[112:, :, :, 2] = .25
        scales, report = evaluate(frames, truth, folds)
        self.assertFalse(report['GLASS_BREAKING']['accepted'])
        np.testing.assert_allclose(scales, 1)

    def test_clips_each_model_before_averaging(self):
        frames = np.zeros((1, 3, 1, 6), dtype=np.float32)
        frames[0, 0, 0, 0] = .8
        scales = np.ones((3, 6), dtype=np.float32)
        scales[0, 0] = 2
        self.assertAlmostEqual(1 / 3, corrected_peaks(frames, scales)[0, 0], places=6)

    def test_requires_calibration_support(self):
        self.assertEqual(1, fit_scale(np.array([.2, .01]), np.array([True, False]), .5))


if __name__ == '__main__':
    unittest.main()
