# Design

## Source of truth
- Status: Active
- Last refreshed: 2026-11-02
- Primary product surfaces: 권한 안내, 메인(꺼짐·대기·감지), 권한 안내 대화상자.
- Evidence reviewed: docs/prd.md, docs/backlog.md, MainScreen.kt, OnboardingScreen.kt, theme files, UI tests.
- 기존 디자인 이미지·브랜드 가이드·별도 DESIGN.md는 없었다. 아래 시각 선택은 요구사항에 근거한 구현 가정이다.

## Brand
- Personality: 차분하고 명확한 청각 보조 도구.
- Trust signals: 상태·작동 여부·기기 내 처리 사실을 쉬운 한국어로 표시.
- Avoid: 장식적 경고, 번쩍임, 과도한 그라디언트, 정확도·안전을 보장하는 문구.

## Product goals
- Goals: 한눈에 감지 상태를 이해하고 큰 버튼으로 시작/종료한다.
- Non-goals: 화면·탭·설정·감지 기능 추가, 인식 엔진 변경.
- Success signals: 기존 조작/권한 테스트 통과, 큰 글씨·작은 화면에서 내용과 버튼에 접근 가능.

## Personas and jobs
- Primary personas: 농인·난청인.
- User jobs: 주변 소리 인지, 감지 켜기/끄기, 권한 이해와 허용.
- Key contexts of use: 실내·외출·수면 전; 화면 읽기와 큰 글씨 사용 포함.

## Information architecture
- Primary navigation: 최초 권한 안내 → 메인. 새 화면 없음.
- Core routes/screens: 현재 두 화면 유지.
- Content hierarchy: 메인 브랜드 → 상태/설명 → 큰 시작·종료 버튼 → 감지 대상 4종 목록.
- 권한 안내: 브랜드/소개 → 마이크/알림 권한 카드 → 시작하기 → 보조 도구 고지.

## Design principles
- 상태는 색상·아이콘·텍스트를 함께 표시한다.
- 조작 버튼을 가장 큰 터치 대상으로 유지하고 권한 목적을 먼저 설명한다.
- Tradeoffs: 꾸밈보다 대비·줄바꿈·동작 보존을 우선한다.

## Visual language
- Color: 메인 꺼짐은 짙은 회색, 대기는 짙은 초록, 감지는 짙은 빨강. 흰색 본문·버튼과 대비.
- 권한 안내는 밝은 뉴트럴/짙은 뉴트럴 바탕에 초록 포인트; 동적 배경색으로 의미색이 바뀌지 않게 한다.
- Typography: 시스템 글꼴, 본문 16sp 이상, 상태 28sp 이상; 읽기 쉬운 행간.
- Spacing/layout rhythm: 8dp 기준, 기본 가장자리 24dp(작은 화면 20dp).
- Shape/radius/elevation: 원형 주요 버튼, 20~28dp 카드, 얇은 구분선, 과한 그림자 없음.
- Motion: 지속 애니메이션·점멸 없음.
- Imagery/iconography: 기존 Material 아이콘과 단순 벡터/도형. 사진이나 모델 생성 이미지 불필요.

## Components
- Existing components to reuse: Material3 버튼·카드·대화상자, 기존 상태/권한 함수.
- New/changed components: 브랜드 헤더, 상태 설명, 원형 조작 버튼 외곽 링, 4종 이름 배열, 권한 카드.
- Variants and states: Off/Idle/Detected, 권한 Required/Granted/Denied.
- Token/component ownership: theme/Color.kt·Theme.kt·Type.kt, 두 화면의 로컬 컴포넌트.

## Accessibility
- Target standard: 색상에 의존하지 않는 의미 전달, 일반 텍스트 대비 4.5:1 목표, 터치 최소 48dp.
- Keyboard/focus behavior: 표시 순서대로 접근; 주요 버튼에 명시적 조작 이름.
- Contrast/readability: 작은 안내도 14sp 이상, 핵심 본문 16sp 이상. 큰 글씨를 임의로 축소하지 않는다.
- Screen-reader semantics: 상태와 감지 이름 한 문장, liveRegion 유지; 장식 아이콘 읽기 제외.
- Reduced motion and sensory considerations: 점멸·계속 움직이는 파형 없음. 가상의 실시간 음량·정확도 표시 없음.

## Responsive behavior
- Supported breakpoints/devices: 휴대전화 세로/가로, 320dp 너비와 글꼴 배율 2.0 검증.
- Layout adaptations: 화면 높이에 맞는 주요 버튼 크기/여백; 내용이 길면 스크롤. 넓은 화면은 콘텐츠 폭 제한.
- Touch/hover differences: 터치와 TalkBack 조작 우선.

## Interaction states
- Loading: 새 로딩 상태를 만들지 않는다.
- Empty: 꺼짐에서 시작 안내, 감지 목록이 비어도 경고 상태/해제 안내 유지.
- Error: 기존 권한 대화상자/설정 이동 유지.
- Success: 권한 허용에 체크·허용됨 함께 표시.
- Disabled: 실제 권한 로직을 바꾸지 않는다.
- Offline/slow network: 감지는 기존대로 오프라인; 네트워크 UI 없음.

## Content voice
- Tone: 짧고 직접적인 존댓말.
- Terminology: 시작·종료·정상 대기 중·감지; 감지 대상 6종 명칭 유지.
- Microcopy rules: Android 전용 제품으로 소개하지 않는다. iOS가 이미 구현됐다고 표시하지 않는다.

## Implementation constraints
- Framework/styling system: Kotlin·Jetpack Compose·기존 Material3.
- Design-token constraints: 상태색 의미와 중앙 주요 버튼 유지; 의존성 추가 없음.
- Performance constraints: 새 타이머·오디오·네트워크·배경 작업 없음.
- Compatibility constraints: 최소 API 26, 기존 ViewModel/서비스·권한 로직 유지.
- Test/screenshot expectations: 기존 UI 동작 테스트, 각 상태/권한·작은 화면·큰 글씨·다크 모드 검증. 실제 화면 캡처로 검토.

## Open questions
- [ ] 실사용자에게 가장 구분하기 쉬운 상태색·여백·버튼 크기는 후속 사용자 테스트로 확인한다.

## 감지 타일 (BL-009/014/015)
- 감지 중인 모든 소리 타일에 밝은 경고색 배경, 경고 아이콘, “감지” 문구를 함께 표시한다.
- 상단 요약은 최대 3개를 유지하며 타일은 초과 감지 소리도 강조한다. 기존 3초 만료·해제·종료 상태에 따라 강조를 제거한다.
