# Architecture

EarView uses feature-first clean architecture. Code is grouped by feature first, and each feature is split into layers inside it. Do not add a layer to a feature that does not need it.

## Data Flow

```
Microphone
  → detection/data       (audio capture, TFLite AudioClassifier running YAMNet)
  → detection/domain     (keep the 7 target classes, apply threshold and cooldown)
  → DetectionEvent       (published through a SharedFlow)
  → alert/domain         (map a detected class to an alert pattern)
  → alert/data           (vibration)
  → alert/presentation   (full-screen color UI, Compose)
```

`SoundDetectionService` connects these pieces. It starts and stops the pipeline and holds no detection or alert logic.

## Package Layout

```
com.example.earview
├── detection/
│   ├── domain/         # Pure Kotlin: class filter, threshold, cooldown, DetectionEventSource interface
│   └── data/           # Audio capture, TFLite AudioClassifier wrapper, SharedFlow implementation
├── alert/
│   ├── domain/         # Class-to-alert-pattern mapping
│   ├── data/           # Vibrator calls
│   └── presentation/   # Full-screen alert Activity and Compose UI
├── service/            # SoundDetectionService (foreground service)
└── MainActivity.kt
```

Create `data` or `presentation` only when the feature has code for that layer.

## Layer Rules

- `domain` imports only Kotlin and the standard library. It must not import `android.*` or `androidx.*`.
- `presentation` depends on `domain`. It does not depend on `data` directly.
- `data` implements interfaces defined in `domain`.
- Features do not import each other's `data` or `presentation`. Share data through `domain` models or interfaces.
- `service` coordinates features. It does not contain thresholds or alert rules.
- Dependencies are passed through constructors. Create them in `service` or `MainActivity`. Do not use a DI library.

## Model Asset

- The YAMNet model file goes in `app/src/main/assets/`.
- Use the YAMNet model with metadata so the label file is read from the model.

## Service and Permissions

- `SoundDetectionService` runs as a foreground service with `foregroundServiceType="microphone"`.
- Manifest permissions: `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, `VIBRATE`, `USE_FULL_SCREEN_INTENT`.
- `RECORD_AUDIO` and `POST_NOTIFICATIONS` are requested at runtime before the service starts.
- When a target sound is detected, the service posts a high-priority notification with a full-screen intent that opens the alert Activity.
- The service shows a persistent notification while it runs.

## Testing Boundaries

- Unit test `domain` with JUnit. No device needed.
- Test `data` with fakes for audio and inference inputs where possible.
- Test the full pipeline on a device, as described in `docs/testing.md`.
