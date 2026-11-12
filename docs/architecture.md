# EarView 아키텍처 구조도

EarView는 주변 소리를 기기 안에서 분류해 화면과 진동으로 알리는 Android 앱이다. 아래 구조도는 현재 코드에서 연결된 경로를 보여 준다.

```mermaid
flowchart TD
    subgraph UI["화면 · Jetpack Compose"]
        NAV["MainNavigation"]
        ON["권한 안내"]
        MAIN["메인 화면<br/>감지 시작·종료 및 알림 표시"]
        VM["MainScreenViewModel"]
        SETTINGS["설정 화면<br/>민감도 · 노이즈 억제 · 대역통과 필터"]
        RECORD["녹음 화면<br/>실시간 파형 · 원본 미저장"]
        NAV --> ON
        NAV --> MAIN
        ON -->|권한 안내 완료| MAIN
        MAIN -->|설정 열기| SETTINGS
        MAIN -->|사용자 조작| VM
        SETTINGS -->|감지 중이 아닐 때| RECORD
    end

    subgraph RUNTIME["감지 실행 · 앱 프로세스"]
        SERVICE["DetectionService<br/>마이크 포그라운드 서비스"]
        CONTROLLER["DetectionController<br/>감지 상태 및 알림 제어"]
        NOTIFY["Android 알림<br/>감지 실행 중 · 예기치 않은 종료"]
        VIBRATE["Android 진동<br/>위험음별 패턴"]
        SERVICE -->|감지 시작·종료| CONTROLLER
        SERVICE --> NOTIFY
        CONTROLLER --> VIBRATE
    end

    subgraph INFERENCE["위험음 분류 · 기기 내부"]
        AUDIO["AudioRecordSource<br/>16 kHz 모노 PCM"]
        WINDOW["SlidingAudioWindow"]
        MODELS["YAMNet · EfficientAT · PANNs<br/>TensorFlow Lite"]
        ENSEMBLE["모델별 점수 보정 → 동일 가중치 평균"]
        RULES["고정된 소리별 임계값으로 판정"]
        AUDIO --> WINDOW --> MODELS --> ENSEMBLE --> RULES
    end

    subgraph WAVEFORM["녹음 파형 · 별도 마이크 세션"]
        CAPTURE["AudioRecordSource<br/>감지 꺼짐 상태에서 최대 30초"]
        PEAKS["PCM 구간별 최대 진폭 계산"]
        CAPTURE --> PEAKS
    end

    MIC["기기 마이크"]
    VM -->|시작·종료 요청| SERVICE
    VM -->|화면 알림 닫기| CONTROLLER
    CONTROLLER -->|감지 실행| AUDIO
    RULES -->|위험음 이벤트| CONTROLLER
    CONTROLLER -->|StateFlow| VM
    VM -->|꺼짐 · 대기 · 감지됨| MAIN
    MIC --> AUDIO
    MIC --> CAPTURE
    RECORD -->|녹음 시작·종료| CAPTURE
    PEAKS -->|진폭만 전달| RECORD
```

- **감지 흐름:** [EarViewApplication](../app/src/main/java/com/example/earview/EarViewApplication.kt)이 공유 컨트롤러를 만들고, [DetectionService](../app/src/main/java/com/example/earview/detection/DetectionService.kt)가 사용자의 시작·종료 요청을 전달한다. [YamnetSoundDetector](../app/src/main/java/com/example/earview/detection/YamnetSoundDetector.kt)는 앙상블 점수를 소리별 임계값과 비교한다.
- **화면과 알림:** 메인 화면 하단에는 화재경보, 사이렌, 유리 깨짐, 차량 경적을 표시한다. 분류 대상은 여전히 초인종과 문 두드림을 포함한 6종이다. 감지되면 [DetectionController](../app/src/main/java/com/example/earview/detection/DetectionController.kt)가 화면 상태와 진동을 갱신한다. 화면 알림은 탭하거나 10초가 지나면 대기 상태로 돌아간다.
- **설정과 녹음:** [설정 화면](../app/src/main/java/com/example/earview/ui/main/DetectionFeaturePreview.kt)의 민감도, 노이즈 억제, 대역통과 필터 조작은 감지 경로에 연결되지 않는다. 실시간 파형은 실제 마이크 입력에서 계산한다. 녹음 등록·보관은 구현되지 않아 녹음 관리 목록은 비어 있다.
- **데이터 경계:** 감지와 파형에 쓰는 원본 오디오는 메모리에서만 처리한다. 디스크에 쓰거나 네트워크로 보내지 않는다.
