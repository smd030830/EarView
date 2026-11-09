"""BL-001/003: one-shot exploratory score scaling with source-separated ESC-50 folds.

Only applies a per-sound candidate if held-out recall improves without adding
nominal false-positive clips. This is not probability calibration or release QA.
"""
import argparse
import csv
import hashlib
import io
import json
import re
from concurrent.futures import ThreadPoolExecutor

import numpy as np

from evaluate import CATEGORIES, GROUPS, HERE, LABELS, ROOT, THRESHOLDS, fetch, load_audio, windows

REVISION = '33c8ce9eb2cf0b1c2f8bcf322eb349b6be34dbb6'
NAMES = ['YAMNET', 'EFFICIENTAT', 'PANNS']
ASSETS = [ROOT / 'app/src/main/assets/yamnet.tflite',
          ROOT / 'app/src/main/assets/ensemble/efficientat_mn04_as.tflite',
          ROOT / 'app/src/main/assets/ensemble/panns_mobilenetv2.tflite']


def metrics(peaks, truth, index):
    positive = truth == index
    negative = ~positive
    predicted = peaks >= THRESHOLDS[index]
    return dict(positives=int(positive.sum()), true_positives=int((predicted & positive).sum()),
                negatives=int(negative.sum()), false_positive_clips=int((predicted & negative).sum()),
                recall=float(predicted[positive].mean()) if positive.any() else None)


def fit_scale(peaks, positive, threshold):
    if positive.sum() < 8 or (~positive).sum() < 40:
        return 1.0
    # Keep zero at zero, monotonic order, and avoid amplifying near-zero noise.
    candidates = np.unique(np.r_[peaks, .02, threshold, 1.0])
    candidates = candidates[(candidates >= .02) & (candidates <= 1)]
    ranked = []
    for boundary in candidates:
        prediction = peaks >= boundary
        fp = int(prediction[~positive].sum())
        if fp / (~positive).sum() <= .01:
            tp = int(prediction[positive].sum())
            ranked.append(((tp, -fp, -abs(np.log(boundary / threshold))), boundary))
    return float(np.float32(threshold / max(ranked)[1])) if ranked else 1.0


def corrected_peaks(frames, scales):
    corrected = np.clip(frames.astype(np.float64) * scales[None, :, None, :], 0, 1).astype(np.float32)
    # Mirrors Kotlin's sum of score/3 and converts before frame-wise averaging.
    return np.sum(corrected / np.float32(3), axis=1, dtype=np.float32).max(axis=1)


def evaluate(frames, truth, folds):
    train = folds <= 3
    test = folds >= 4
    scales = np.ones((3, len(LABELS)), dtype=np.float32)
    model_peaks = frames.max(axis=2)
    for model in range(3):
        for sound in range(len(LABELS)):
            scales[model, sound] = fit_scale(model_peaks[train, model, sound], truth[train] == sound, THRESHOLDS[sound])
    baseline = corrected_peaks(frames, np.ones_like(scales))
    candidate = corrected_peaks(frames, scales)
    accepted = np.ones_like(scales)
    results = {}
    for sound, label in enumerate(LABELS):
        before = metrics(baseline[test, sound], truth[test], sound)
        after = metrics(candidate[test, sound], truth[test], sound)
        passes = (after['positives'] >= 8 and after['negatives'] >= 40
                  and after['true_positives'] > before['true_positives']
                  and after['false_positive_clips'] <= before['false_positive_clips'])
        if passes:
            accepted[:, sound] = scales[:, sound]
        results[label] = dict(accepted=bool(passes), baseline=before, candidate=after, applied=after if passes else before,
                             candidate_scales=scales[:, sound].tolist(), applied_scales=accepted[:, sound].tolist())
    return accepted, results


def apply_to_kotlin(scales):
    path = ROOT / 'app/src/main/java/com/example/earview/detection/ScoreCalibration.kt'
    source = path.read_text(encoding='utf-8')
    for model, name in enumerate(NAMES):
        pattern = r'(SoundModel\.' + name + r' to ModelScoreCalibration\(\s*mapOf\()(.*?)(\n      \),)'
        match = re.search(pattern, source, re.S)
        if match is None:
            raise ValueError(f'Missing {name} configuration block')
        block = match.group(2)
        for sound, label in enumerate(LABELS):
            replacement = f'DangerSound.{label} to ScoreCorrection(scale = {float(scales[model, sound]):.9g}f)'
            block, count = re.subn(r'DangerSound\.' + label + r' to ScoreCorrection\([^)]*\)', replacement, block)
            if count != 1:
                raise ValueError(f'Missing correction for {name}/{label}')
        source = source[:match.start(2)] + block + source[match.end(2):]
    path.write_text(source, encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    cache = HERE / '.cache' / REVISION
    base = f'https://raw.githubusercontent.com/karolpiczak/ESC-50/{REVISION}/'
    metadata = fetch(base + 'meta/esc50.csv', cache / 'esc50.csv')
    fetch(base + 'LICENSE', cache / 'LICENSE')
    rows = list(csv.DictReader(io.StringIO(metadata.decode())))
    selected = []
    for category in sorted({row['category'] for row in rows}):
        for fold in range(1, 6):
            subset = sorted([r for r in rows if r['category'] == category and int(r['fold']) == fold], key=lambda r: r['filename'])
            selected.extend(subset if category in CATEGORIES else subset[:2])
    def download(row):
        fetch(base + 'audio/' + row['filename'], cache / row['filename'])
    print(f'Downloading/caching {len(selected)} public fixtures', flush=True)
    with ThreadPoolExecutor(max_workers=6) as pool:
        list(pool.map(download, selected))
    train_sources = {r['src_file'] for r in selected if int(r['fold']) <= 3}
    test_sources = {r['src_file'] for r in selected if int(r['fold']) >= 4}
    if train_sources & test_sources:
        raise ValueError('Calibration/evaluation source leakage')
    import tensorflow as tf
    models = []
    for asset in ASSETS:
        model = tf.lite.Interpreter(model_path=str(asset), num_threads=2)
        model.allocate_tensors()
        models.append(model)
    all_frames = []
    records = []
    for number, row in enumerate(selected, 1):
        audio = load_audio(cache / row['filename'])
        clip = []
        for window in windows(audio):
            scores = []
            for name, model in zip(NAMES, models):
                inp = model.get_input_details()[0]
                length = int(np.prod(inp['shape']))
                model.set_tensor(inp['index'], window[-length:].reshape(inp['shape']))
                model.invoke()
                score = model.get_tensor(model.get_output_details()[0]['index']).reshape(-1)
                if name == 'YAMNET':
                    score = np.array([max(score[g]) for g in GROUPS])
                if score.shape != (len(LABELS),) or not np.all(np.isfinite(score)) or not np.all((score >= 0) & (score <= 1)):
                    raise ValueError('Invalid scores')
                scores.append(score)
            clip.append(scores)
        if len(clip) != 7:
            raise ValueError('Expected seven complete aligned windows per five-second clip')
        all_frames.append(np.array(clip).transpose(1, 0, 2))
        records.append(dict(filename=row['filename'], fold=int(row['fold']), source=row['src_file'], category=row['category'],
                            sha256=hashlib.sha256((cache / row['filename']).read_bytes()).hexdigest()))
        if number % 50 == 0:
            print(f'Scored {number}/{len(selected)}', flush=True)
    frames = np.array(all_frames)
    truth = np.array([CATEGORIES.get(r['category'], -1) for r in records])
    folds = np.array([r['fold'] for r in records])
    scales, results = evaluate(frames, truth, folds)
    report = dict(dataset_revision=REVISION, runtime=f'PC TensorFlow {tf.__version__}', output_order=LABELS,
                  models=NAMES, model_hashes={name: hashlib.sha256(path.read_bytes()).hexdigest() for name, path in zip(NAMES, ASSETS)},
                  protocol='Calibration folds 1-3, held-out acceptance folds 4-5; source-disjoint; one candidate per sound',
                  fit='Per-model clip recall maximized at <=1% nominal calibration clip FPR; minimum raw boundary .02; zero offset',
                  gate='Apply only if fused held-out recall increases with no additional nominal false-positive clips',
                  limitation='Pilot acceptance uses held-out data once; further independent release/real-device validation needed. Scores are not probabilities.',
                  calibration_clips=int((folds <= 3).sum()), validation_clips=int((folds >= 4).sum()),
                  results=results, applied_scales=scales.tolist(), records=records)
    (HERE / 'tuning-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(results, indent=2), flush=True)
    if args.apply:
        apply_to_kotlin(scales)
        print('Applied gated coefficients to Kotlin; run Android tests before shipping', flush=True)


if __name__ == '__main__':
    main()
