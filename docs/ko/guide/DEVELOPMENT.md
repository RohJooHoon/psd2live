# 개발과 명령줄

[문서 목록](../../README.md) · [中文](../../zh/guide/DEVELOPMENT.md) · [English](../../en/guide/DEVELOPMENT.md) · [日本語](../../ja/guide/DEVELOPMENT.md)

이 문서는 소스에서 실행하거나, 명령줄을 쓰거나, 코드를 기여하려는 분을 위한 것입니다. 릴리스 패키지에는 자체 런타임이 포함되어 있어 여기의 어떤 것도 필요하지 않습니다.

## 요구 사항

- JDK 21, 가급적 JetBrains Runtime(JBR) 21. Windows 릴리스 패키지에 포함되어 있으며, JBR에서는 앱이 그리는 제목 표시줄 아래에서 주 창이 네이티브 프레임을 유지합니다(시스템이 최대화, 스냅, 크기 조절을 담당). 다른 JDK로도 빌드, 테스트, 실행할 수 있으며, 이때 주 창은 장식 없는 창이 되고 제목 표시줄 전체를 앱이 그립니다. Windows 패키징(`createDistributable`, `packageExe`)에는 JBR 21 SDK(jpackage 포함)가 필요하며, `-Ppsd2live.allowNonJbr=true`를 주면 다른 JDK로 패키징합니다. macOS DMG(`packageDmg`)는 Gradle을 실행하는 JDK로 빌드하며, Swift 셸이 시스템 창 프레임을 유지하므로 JBR이 필요 없습니다. Gradle은 포함된 Wrapper로 실행되므로 따로 설치할 필요가 없습니다.
- Windows에서는 `.\gradlew.bat`, Linux / macOS에서는 `./gradlew`를 씁니다. 아래 예시는 `./gradlew`를 사용합니다.
- 공식 Cubism SDK는 필요하지 않습니다. 소스 빌드는 내장 렌더러를 씁니다. 네이티브 브리지는 [Cubism 네이티브 미리보기](CUBISM_SDK_SETUP.md)를 참고하세요.

## 실행

```bash
./gradlew run                     # no arguments: start the GUI
./gradlew run --args="--help"     # CLI help
python tools/examples/fetch.py --case tml
./gradlew run --args="--input examples/tml/psd-input/tml.psd --output build/example-output"
```

Windows에서는 저장소 루트의 `run-gui.bat`으로도 GUI를 시작할 수 있습니다. 인수가 없으면 GUI가 시작되고, 인수가 있으면 CLI가 실행되며 `--input`이 필요합니다(`--help` 제외). CLI는 PSD에서 바로 내보내기 파일을 기록하며, 편집 가능한 `.psd2live` 프로젝트는 만들지 않습니다.

## CLI 옵션

| 옵션 | 기본값 | 의미 |
| --- | --- | --- |
| `--input <path>` | 필수 | 레이어로 나뉜 입력 PSD |
| `--output <path>` | PSD 옆의 `psd2live-output` | 출력 디렉터리 |
| `--lang <zh\|en\|ja\|ko>` | 시스템 언어 | 로그 언어 |
| `--clear-user-data` | — | 단독으로 쓰면 현재 사용자의 설정, 작업 공간 저장소, `~/.psd2live` 아래의 런타임과 캐시를 삭제하고 종료합니다. 편집기가 실행 중이면 아무것도 삭제하지 않고 1을 반환합니다. Windows 제거 프로그램은 사용자 데이터 삭제를 요청받으면 이를 실행합니다 |
| `--atlas <size>` | 4096 | 텍스처 아틀라스 크기 |
| `--mesh-spacing <px>` | 64 | 메시 간격 |
| `--mesh-pixels` | 꺼짐 | 메시 길이를 긴 변 2048 px로 축소한 문서의 픽셀 대신 원본 픽셀로 잼 |
| `--mesh-wrap <units>` | 0 | 감싸기 토폴로지: 이보다 좁은 윤곽 틈(메시 단위)을 닫아 가는 돌출부가 하나의 외곽을 공유하게 함 |
| `--head-strength <value>` | 1.0 | 머리 변형 강도 |
| `--body-strength <value>` | 1.0 | 몸 변형 강도 |
| `--mesh-only` | 꺼짐 | 메시만 생성 |
| `--no-deformers` | 꺼짐 | 디포머 건너뛰기 |
| `--no-see-through` | 꺼짐 | 앞머리 너머로 눈썹과 눈을 모두 보이지 않게 함(기본은 둘 다 보이므로, 이전 버전과 비교해 출력에 눈썹 복사본, 앞머리의 눈 창과 그 복사본이 들어 있음) |
| `--no-see-through-eyes` | 꺼짐 | 앞머리 너머로 눈을 보이지 않게 함(이제 기본값인 `--see-through-eyes`도 여전히 받음) |
| `--see-through-opacity <0..1>` | 0.5 | 앞머리 너머로 눈썹과 눈이 비치는 정도 |
| `--no-motions` | 꺼짐 | 모션 건너뛰기 |
| `--no-physics` | 꺼짐 | 물리 건너뛰기 |
| `--no-cmo3` | 꺼짐 | CMO3 건너뛰기 |
| `--no-moc3` | 꺼짐 | MOC3 건너뛰기 |
| `--no-json` | 꺼짐 | 진단 JSON 건너뛰기 |
| `--upscale <1\|2\|4>` | 1 | 텍스처 고해상도화 배율. 1이면 끔 |
| `--upscale-python <path>` | `python`(Windows), 그 밖에는 `python3` | nunif 의존성이 설치된 Python |
| `--nunif-dir <path>` | 비어 있음 | nunif 소스 디렉터리 |
| `--upscale-model <path>` | 비어 있음 | 가중치 디렉터리 |
| `--upscale-tile <64..512>` | 256 | 추론 타일 크기 |
| `--upscale-noise <-1..3>` | 1 | 노이즈 제거 수준. -1이면 노이즈 제거를 끔 |
| `--no-upscale-neural-alpha` | 꺼짐 | 대신 알파를 쌍선형으로 확대 |

참고:

- 기본값은 [`Main.kt`](../../../src/main/kotlin/io/github/psd2live/Main.kt)에서 옵니다. GUI의 초기 메시 간격은 CLI의 64가 아니라 `PipelineConfig`(40)에서 옵니다.
- CMO3, MOC3, 진단 JSON 중 적어도 하나는 남겨 두세요.
- 신경망 알파는 기본으로 켜져 있으며, `--upscale-neural-alpha`는 이전 명령을 위해서만 남아 있습니다.
- 고해상도화 설정은 [텍스처 고해상도화 가이드](TEXTURE_UPSCALE.md)에 설명되어 있습니다.

### 헤드리스 MCP 서버

`./gradlew run --args="mcp [--port N] [--token T] [--store DIR] [--profile core|full]"`은 창 없이 MCP 엔드포인트만 시작하며, 에이전트가 단독으로 작업할 때 씁니다. 작업 공간은 뷰 모델이나 Compose 상태 없이 애플리케이션 계층 호스트에서 실행됩니다([헤드리스 호스트](../spec/HEADLESS_HOST.md) 참고). `endpoint`, `token`, `profile`, `store`, `capabilities`(`view_sample_motion=yes|no`, 빌드에 Cubism Core가 들어 있는지)를 출력하고 프로세스가 끝날 때까지 서비스합니다. 스크립트는 `PSD2LIVE_MCP_ENDPOINT`와 `PSD2LIVE_MCP_TOKEN`이 이 서버를 가리키게 하고 `python mcp_proxy.py call <operation> [JSON] --edit` 또는 Python의 `mcp_proxy.Client`로 호출합니다. 작업 공간 저장소의 기본값은 사용자 홈의 `psd2live-mcp`이며 편집기의 저장소와 분리되어 있습니다(히스토리나 인스턴스 잠금을 공유하지 않음). `--token`은 URL 안전 Base64 문자 32–256자여야 하며, 생략하면 무작위로 정해집니다.

## 다른 형식으로 내보내기

`export` 명령은 `.psd2live` 프로젝트(현재 히스토리 상태) 또는 PSD(기본 설정으로 생성)를 중립 IR을 거쳐 아무 대상으로나 내보내고 손실 보고서를 기록합니다.

```bash
./gradlew run --args="targets"                                            # list the export targets
./gradlew run --args="export model.psd2live --target gif --set clip=Nod --set size=512"
./gradlew run --args="export model.psd2live --target psd-pose --set pose=ParamAngleX=20 --output out/pose"
```

- 파일은 `--output`(기본은 입력 옆의 `<name>-<target>`)에 손실 보고서 `<name>.<target>.report.json`과 함께 기록됩니다. `--name`은 기본 이름을 정합니다.
- `--set key=value`는 반복할 수 있습니다. 각 대상의 설정은 [중립 리그 IR과 내보내기 대상](../spec/EXPORT_TARGETS.md)에 나열되어 있습니다.
- 종료 상태: 성공하면 0, 내보내기가 실패하면 1, 사용법 오류면 2.

## 테스트와 패키징

```bash
./gradlew test                                   # all tests (CI runs them on Ubuntu and Windows)
./gradlew test --tests "io.github.psd2live.scenario.CatalogContractsTest"   # one test class
./gradlew createDistributable                    # application directory with runtime
./gradlew packageDistributionForCurrentOS        # installer for this platform
./gradlew packageDmg                             # macOS only: DMG with the Swift app shell
```

- `packageDmg`는 macOS 전용입니다. `createDistributable`의 런타임을 포함하고 실행기를 `packaging/macos`의 Swift 셸로 바꾸므로, Dock 이름, 문서 유형, 창 단추가 시스템의 것이 됩니다. 메뉴 막대는 화면 메뉴 막대이며, 창 안의 메뉴는 모든 명령을 나열하므로 그대로 남습니다. DMG는 빌드한 Mac(Apple silicon 또는 Intel)과 같은 아키텍처이며 macOS 13 이상이 필요합니다. 공증되지 않았으므로 인터넷에서 내려받은 사본은 Finder에서 보조 클릭으로 열어야 합니다. `PSD2Live`를 응용 프로그램으로 끌어 놓으세요.
- **CI** Actions 워크플로는 풀 리퀘스트와 푸시에서 Ubuntu, Windows, macOS로 테스트를 실행합니다. macOS 작업은 Apple silicon과 Intel 모두를 테스트하고 패키징하며, 워크플로를 수동으로 실행해 아티팩트에서 DMG를 내려받을 수도 있습니다. 네이티브 실행기 실패를 테스트하고, 마운트한 패키지를 포함된 런타임으로 실행합니다. 공유 `.github/actions/package-macos` 액션은 릴리스 워크플로에도 Cubism SDK 없이 두 DMG를 공급합니다.
- 두 패키징 작업 모두 빌드 호스트의 네이티브 라이브러리만 포함하며, 애플리케이션 리소스에 `LICENSE`, `THIRD_PARTY_NOTICES.md`, `licenses/`를 넣습니다.
- 공식 SDK 리소스(`src/main/resources/cubism/`)는 `-Ppsd2live.includeCubism=true`를 주거나 `PSD2LIVE_INCLUDE_CUBISM=true`를 설정하지 않는 한 제외됩니다. SDK가 들어간 패키지는 공개 배포하면 안 됩니다. [CI와 릴리스](CUBISM_CI_RELEASE.md)를 참고하세요.
- Linux에서 `./native/package_linux.sh`는 시스템 JDK 21이 필요한 로컬 실행기 패키지를 빌드합니다(`dist/linux-<timestamp>/`에 기록). [native/README.md](../../../native/README.md)를 참고하세요.
- 별도의 lint 작업은 없으며, 코드 스타일은 `kotlin.code.style=official`입니다.
- Rust 런타임은 `runtime/`에서 `cargo test`와 `cargo build --release`로 빌드합니다. [런타임](../spec/RUNTIME.md)을 참고하세요. 쓸 수 있는 cargo가 없으면 Linux의 `./gradlew run`과 패키징은 CI(`runtime-prebuilt.yml`)가 같은 소스로 빌드한 라이브러리를 내려받습니다. `runtime/src`, `Cargo.toml`, `Cargo.lock`의 해시(`./gradlew printRuntimeHash`)로 맞추고 sha256으로 검증하며, 찾지 못하거나 오프라인이면 내장 평가기를 씁니다. `-Ppsd2live.prebuiltRuntime=false`는 내려받기를 끕니다. `runtime/`을 반복 수정할 때는 `-Ppsd2live.fastRuntime=true`로 LTO를 빼면 빌드가 훨씬 빨라집니다. 라이브러리가 느려지므로 이 옵션으로 패키징하지는 마세요.
- 테스트 스위트에는 빠른 단위 테스트와 계약 테스트만 둡니다. 전체 `./gradlew test`는 1분 안에 끝나야 합니다. 예제 PSD(tml, ds)로 전체 파이프라인을 돌리거나, 모든 단계를 콜드 재생과 비교하거나, 프레임 전체를 렌더링하는 테스트는 추가하지 마세요. 그런 검사는 아래의 개발 도구로 작성해 직접 실행하세요.

## 외부 예제와 인수 검사

에셋, 프로젝트, 쇼케이스 내보내기는 [psd2live-examples](https://github.com/tsunehimatoi/psd2live-examples)에서 관리하며, 따로 표시하지 않으면 기본적으로 CC0 1.0입니다. Python 3.11 이상에서 `python tools/examples/fetch.py --case tml`을 실행하거나, 고정된 모든 사례는 `--all`을 씁니다. 파일은 `examples/<id>/` 경로를 유지하며, 압축 파일은 `build/example-assets/`에 캐시됩니다.

`tools/examples/fixtures.lock.json`은 버전, URL, 크기, 압축 파일/파일 SHA-256 체크섬을 고정합니다. 새 에셋을 들일 때는 이 잠금 파일을 명시적으로 업데이트하세요. 내려받기나 무결성 검사가 실패하면 명령이 실패합니다.

일반 CI는 예제를 내려받지 않고 작은 합성 입력과 내부 계약 테스트를 씁니다. `Example acceptance` 워크플로를 수동으로 실행하면 고정된 `tml`과 `ds`를 가져와 생성, 내보내기, CMO3 가져오기를 거쳐 `ExportGoldenTool`을 실행하고, 네 변형 모두의 내보내기/미리보기 보고서를 요구합니다. 릴리스 전에 업로드된 아티팩트를 검토하세요. 이는 육안 인수 검사나 기준선 비교를 대체하지 않습니다. 내부 MCP 회귀 테스트는 이 저장소에 남고, 외부 사례와 탐색 검사는 예제 저장소에 있습니다.

## 개발 도구

`src/test/kotlin/io/github/psd2live/tools/`에는 결과를 눈으로 확인하고 측정하기 위한 개발 도구가 있습니다. 파이프라인 내부에 접근할 수 있도록 테스트로 작성되어 있으며, `PSD2LIVE_TOOLS=1`일 때만 실행됩니다. 일반 `./gradlew test`는 이를 건너뜁니다. 출력은 `build/tools/`에 기록됩니다.

```bash
python tools/examples/fetch.py --case ds
PSD2LIVE_TOOLS=1 PSD2LIVE_SAMPLE=ds ./gradlew test --tests "io.github.psd2live.tools.MotionSheetTool.body"
```

| 도구 | 만드는 것 | 출력 |
| --- | --- | --- |
| `DeformerDeletionVisualTool` | 비대칭 합성 메시의 부모 디포머 삭제 전후, 그리고 MOC3 내보내기와 다시 읽기 후 | `deformer-deletion/comparison.png` |
| `MotionSheetTool.motions` | 모든 프리셋 모션을 시간순으로 배치, 대기 반복, 차이 이미지가 있는 호흡, 몸 Z | `motion-sheet/<sample>-*.png` |
| `MotionSheetTool.body` | 몸 X × 몸 Y, 다리와 상체 확대, 기울기, 비율, 다리 포즈. 뼈대 없이와 자동 뼈대에서 | `motion-sheet/<sample>-{stance,lean,size,legposes}*.png` |
| `MotionSheetTool.tracking` | 포인터가 캔버스를 천천히 도는 12초, 미리보기의 게인과 속도로 머리와 몸이 따라옴 | `motion-frames/<sample>-track/` |
| `MotionSheetTool.idle` | 대기 12초를 프레임별로 | `motion-frames/<sample>-idle/` |
| `ModelProfileTool.cmo3` | `.cmo3`의 파라미터, 디포머 트리(격자 축과 경계), 드로어블, 대역 모션 프로필과 몸 파라미터에 따른 드로어블별 움직임, 몸 X × 몸 Y 실루엣, 물리 그룹 | `model-profile/<name>.txt`, `.png`, `-physics.txt` |
| `RigStudyTool` | 완성된 Cubism 모델을 분석합니다(`PSD2LIVE_MOC3`: `.model3.json` 또는 그런 파일이 있는 디렉터리, 여러 모델 가능). 그룹과 용도가 있는 파라미터, 파츠 트리, 각 오브젝트의 키폼 축이 있는 디포머 트리, 메시 밀도, 마스크, 글루, 물리, 표정, 모션, VTube Studio 매핑. 파라미터별로 각 드로어블을 얼마나 움직이는지, 불투명도 / 그리기 순서 / 색 변화, 각 절반이 얼마나 비선형인지. 기본 포즈, 각 파라미터가 움직이는 부분으로 잘라 낸 5프레임, 머리 각도, 눈, 입, 몸 격자와 메시 와이어프레임. `PSD2LIVE_STUDY_ONLY`는 경로로 거르고, `PSD2LIVE_STUDY_REVIEW=1`은 검토 포즈(단계별로 감기, 크게 뜨기, 위아래 보기)의 눈만 렌더링합니다(`review-eye.png`). 큰 모델에는 `-Ppsd2live.testHeap=16g`가 필요합니다 | `rig-study/<folder>/profile.txt`, `effects.txt`, `*.png` |
| `FitDetectionTool` | 샘플(`PSD2LIVE_SAMPLE`)의 이목구비 맞춤을 원화 위에 그립니다. 각 눈의 눈머리와 눈꼬리, 윗눈꺼풀과 아랫눈꺼풀 가장자리, 그려진 감은 선, 열림 키마다 맞춘 눈꺼풀의 위쪽 가장자리(노랑에서 파랑). 벌린 입의 윗입술(흰색)과 아랫입술(청록) 가장자리. 입 레이어를 나열하고 눈이나 입이 원래 리그로 남겨진 이유를 출력합니다 | `fit-detection/<sample>.png` |
| `RigReviewTool` | 샘플(`PSD2LIVE_SAMPLE`)을 새 프로젝트의 레시피와 모듈 이전의 원래 생성으로 각각 생성해 검토 포즈와 클립에서 비교합니다. 부위별 포즈 시트(포즈마다 한 행), 클립별로 둘을 나란히 물리와 함께 재생하는 GIF(깜빡임, 느린 깜빡임, 웃으며 깜빡임, 시선, 말하기, 모음, 눈썹, 돌기, 끄덕임과 기울이기, 고개 젓기, 몸, 대기). `PSD2LIVE_REVIEW`는 부위를 고르고(eye, brow, mouth, head, hair, body, motion. 기본은 전부), `PSD2LIVE_REVIEW_SIZE`는 프레임 한 변의 길이입니다 | `rig-review/<sample>/<area>-poses.png`, `<clip>.gif` |
| `ModelProfileTool.sample` | 생성된 모델의 대역 모션 프로필(뼈대 없이와 자동 뼈대에서), 몸 레이어와 자동 본 | `model-profile/<sample>.txt` |
| `SkeletonAutoViewTool.view` | 샘플 위에 자동 뼈대를 그리고 모든 본을 출력해 관절과 갈래를 확인합니다. `PSD2LIVE_SAMPLES`는 여러 샘플이나 PSD 경로를 쉼표로 구분해 받고, `PSD2LIVE_ZOOM=left,top,right,bottom`(캔버스 픽셀)은 잘라 냅니다 | `skeleton-auto/<sample>.png` |
| `SimBakeSuite` | 모델 프리셋처럼 뒷머리, 앞머리, 옷을 설정하고(뒷머리는 위쪽 1/10을 따라 고정), 각각을 이전 방식과 입력별 방식으로 베이크한 다음, 맞춤에서 본 적 없는 움직임(추적 같은 포인터, 머리 굴리기, 내장 모션, 입력별 계단, 돌기, 끌기, 흔들기)에서 참조 시뮬레이션과 내보내기를 시각적 척도로 비교합니다 | `build/tools/sim-bake/<sample>/`: `summary.md`, 설정마다 JSON, 끝 궤적 그래프 |
| `SimRootDofExperiment` | 바디가 루트가 어떻게 운반되고 회전하는지에만 반응하는지: 각 입력이 루트를 얼마나 움직이고 그 밖에 바디를 얼마나 변형하는지. 실제 구동 대 강체 구동, 진폭, 루트를 같은 식으로 움직이는 입력들 | `build/tools/sim-root-dof/<sample>.md` |
| `SimBakeProfileTool` | `tml` 뒷머리를 스레드 하나와 전체 스레드로 베이크하고, 진행 표시 사이의 단계 시간을 재며 모든 스레드를 샘플링합니다 | 표준 출력 |
| `PendulumKernelTool` | 배열 진자 커널(정확 / 고속)을 `PhysicsEngine`과 비교: 프레임당 시간과 편차 | 표준 출력 |
| `CanvasPerfTool` | Xvfb에서 실제 창을 열고 편집 캔버스에서 대기, 호버, 휠 확대/축소, 가운데 버튼 이동, 변형 모드에서 얼굴의 모든 점 드래그를 GPU 렌더링과 소프트웨어 렌더링으로 각각 한 번씩 실행합니다. 단계별로 프레임 간격, UI 스레드 지연, UI 스레드 핫스폿, 편집 캔버스의 GPU 프레임 수를 보고합니다(GPU 회차에서 0이면 캔버스가 GPU로 그리지 않은 것). `PSD2LIVE_CANVAS_AA`는 캔버스 안티앨리어싱을 고르고, `PSD2LIVE_OVERLAY=1`은 성능 오버레이를 켭니다. `Robot`으로 실제 포인터를 움직이므로 전용 X 서버(`xvfb-run -a -s "-screen 0 1920x1080x24"`, 또는 GPU가 있는 기계의 별도 Xorg)에서 실행하고, 사용 중인 데스크톱에서는 절대 실행하지 마세요 | `canvas-perf/report.txt`, 단계별 캡처와 `.jfr` |
| `DragonBonesFidelityTool` | `tml`과 `ds`(일반과 자동 뼈대)를 DragonBones로 내보내고 `tools/dragonbones-check`(공식 DragonBones 5.7 런타임 코어, node 필요)로 재생합니다. 모든 키 프레임의 파라미터 애니메이션을 편집기의 평가기와 비교하고(내보내기가 보고하는 허용 오차 안에서) 클립을 측정합니다 | `dragonbones-fidelity/report.txt` |
| `RuntimeConformanceTool.generate` / `.physics` | Rust 런타임용 기준 데이터: `generate`는 무작위 리그(워프, 회전, 중첩, 성긴 격자, 블렌드 셰이프, 글루, 채널, 파츠), 샘플, `PSD2LIVE_RUNTIME_PROJECT`가 가리키는 프로젝트를 무작위 포즈에서 편집기로 평가합니다(뼈대 샘플은 가장 고운 베이크 `rig.fine.p2lrt`로도). `physics`는 무작위 진자 그룹과 샘플 물리의 프레임별 궤적을 기록합니다. `runtime/`의 `p2lrt-conformance`와 비교하세요 | `runtime-conformance/<case>/`, `runtime-physics/<case>/` |
| `RuntimeConformanceTool.simulation` / `.simulationRig` | 시뮬레이션 기준: `simulation`은 무작위 천 시트(위쪽을 따라 고정, 무작위 강성)를 흔들고, 돌고, 뛰는 목표와 고정으로 편집기의 XPBD 솔버에서 구동하며, 절반은 움직이는 캡슐 충돌체가 있습니다. `simulationRig`는 `tml` 뒷머리(위쪽 1/10 고정)를 베이크해 실시간 씬과 함께 내보내고, 베이크하지 않은 리그에서 머리와 몸이 움직일 때의 편집기 씬도 기록합니다. `p2lrt-conformance --sim`과 `--sim-rig`가 읽습니다 | `runtime-sim/<case>/trace.bin`, `runtime-sim-rig/back-hair/` |
| `RuntimeSamplesTool` | 플레이어를 눈으로 확인하기 위한 합성 리그: 그라데이션 위의 모든 색 블렌드 모드, 곱하기와 스크린 색, 마스크와 반전 마스크. 소프트웨어 래스터라이저의 렌더링을 기준으로 함 | `runtime-samples/features.p2lrt`, `features.png`, `layout.txt` |
| `WarpProbeTool` | 런타임의 독립 구현을 위한 편집기 평가기의 블랙박스 탐침: 격자 안팎의 워프 매핑, 워프 아래의 회전 좌표계, 뒤집기, 블렌드 셰이프, 성긴 격자 | `warp-probe/*.tsv` |
| `GeneratorCostTool` | 각 생성기의 시간(뼈대 유무에 따른 리그 빌드, 캐시 없음/있음의 뼈대 베이크, 물리 카탈로그, 캐시 없음/있음의 생성 모션, 리그 IR 컴파일, 흔들림 두 개의 생성과 재생, 시뮬레이션 베이크와 그 결과 반영)을 입력의 내용 해시 시간과 비교해, 생성 캐시가 어떤 생성기에 이득인지 판단합니다. `PSD2LIVE_SAMPLE`은 샘플을 고릅니다(앞머리와 뒷머리 필요) | `generator-cost/report.txt` |
| `ExportGoldenTool` | `tml`과 `ds`를 일반, 자동 뼈대, 작성한 모션 포함, 일반 cmo3를 새 프로젝트로 다시 가져온 경우로 내보낸 모든 파일과 미리보기 번들의 다이제스트(cmo3는 moc3로 낮춰 다시 읽은 결과와 물리 그룹 수로)를 만들어, 리팩터링 전후의 내보내기를 바이트 단위로 비교합니다. `PSD2LIVE_GOLDEN_LABEL`은 출력 이름을 정하고, `PSD2LIVE_GOLDEN_RECIPE=legacy`는 리그 모듈 이전에 저장한 프로젝트(레거시 레시피)로 만들어 이전 프로젝트가 여전히 예전처럼 내보내지는지 확인합니다 | `export-golden/<label>.txt`, `<label>/<sample>-<variant>/` |
| `ExportGoldenComparisonTool` | 기존 골든 보고서 두 개를 비교합니다. CAN3 XML은 짝을 이루는 CMO3의 무작위 파츠 GUID 참조를 파츠 ID로 풀어서 비교하고, 나머지 항목은 다이제스트를 비교합니다. 두 보고서와 그 CAN3 / CMO3 파일을 보관하세요. `PSD2LIVE_GOLDEN_BEFORE` / `PSD2LIVE_GOLDEN_AFTER`의 기본값은 `baseline` / `optimized`입니다 | `export-golden/comparison.txt` |
| `WorkspaceLifecyclePerfTool` | 노드 20,000개의 합성 히스토리와 캔버스가 4개씩인 작업 공간 20개. 공유 히스토리 스냅숏과 재사용한 표현 저장 ID를 즉시 재구성과 비교해 시간과 스레드 할당을 잽니다. 선택 실행 도구의 결과는 실제 렌더링 프레임 속도를 재지 않습니다 | `workspace-lifecycle/performance.txt` |
| `ExportPerfTool` | 샘플 PSD(`PSD2LIVE_SAMPLE`)를 정수 배율로 최근접 이웃 확대해(`PSD2LIVE_EXPORT_SCALES`, 기본 `1,2`. `PSD2LIVE_EXPORT_TARGETS`, 기본은 동영상이 아닌 모든 대상) 모든 내보내기 대상의 시간을 재고, 내보낸 모든 파일의 다이제스트(cmo3는 이미지 항목과 moc3로 낮춰 다시 읽은 결과로)를 기록해 변경 전후의 내보내기 속도와 바이트를 비교합니다. 래스터 대상은 확대 단계마다 1024 px, 5 fps로 렌더링합니다. `PSD2LIVE_GOLDEN_LABEL`은 출력 이름을 정하며, 큰 배율에는 `-Ppsd2live.testHeap=8g` 같은 더 큰 테스트 힙이 필요합니다 | `export-perf/<label>.txt`, `<label>.digest.txt` |
| `ArtPrimitiveV2VisualTool` | `tml`에서 다리를 좌우 컴포넌트로 나누고 왼쪽 속눈썹과 입을 다각형으로 나눈 것을 각각 버전 1과 버전 2 기록으로 작성합니다. 나누지 않은 리그, v1, v2, 그 차이(x4)를 눈 뜸/반쯤/감음과 입 다묾/벌림/웃음 포즈에서 보여 주어 이음매, 잘못 놓인 파츠, 빠진 픽셀을 확인합니다 | `art-primitive-v2/{legs,eye,mouth}.png` |
| `SafetyGoldenTool` | 자동 뼈대 리그에 시드를 고정한 무작위 기하 편집 24개를 적용한 전체 기하 안전 보고서(`coverage` 제외)로, 변경 전후 검사기의 분류를 바이트 단위로 비교합니다. `PSD2LIVE_GOLDEN_LABEL`은 출력 이름을 정합니다 | `safety-golden/<label>.txt` |
| `BundleProfileTool` | moc3 미리보기 번들의 단계별 시간(IR 컴파일, IR에서 퍼펫으로 되돌리기, 정지 메시를 캔버스 공간으로, physics3/motion3, moc 낮추기와 기록, cdi3)과 기하 안전 검사 시간. `PSD2LIVE_SAMPLER=1`은 스택 샘플러의 가장 뜨거운 프레임도 출력합니다 | 표준 출력만 |
| `CubismParityTool` | 샘플(`PSD2LIVE_SAMPLE`)의 미리보기 번들을 포함된 Cubism 런타임(`live2d_renderer`, 오프스크린)으로 렌더링해 편집기 자체 렌더러와 나란히 놓습니다. 전신, 얼굴, 입 확대를 정지, 돌린 상태, 입 벌림, 모음, 눈 감음 등에서. `PSD2LIVE_PARITY_RECIPE=legacy`는 모듈 이전의 생성을 씁니다. Windows 전용 | `cubism-parity/<sample>[-legacy]-<view>.png` |
| `CubismCoreCheckTool` | `tml`과 `ds` 미리보기 번들의 moc3를 공식 Cubism Core의 일관성 검사(`csmHasMocConsistency`)에 넘깁니다. Core 라이브러리를 가리키는 `PSD2LIVE_TEST_CUBISM_CORE`가 필요하며 없으면 건너뜁니다. `:umamo`의 `MocDefaultColorsTest`가 작은 새 모델을 같은 방식으로 검사합니다 | 표준 출력만 |
| `TextureWorkspaceTool` | `tml`에 밀도 몇 개와 잠금 하나를 설정하고 메시 점유 영역으로 한 번 배치한 다음, 아틀라스 페이지와 텍스처 패널(중국어와 영어. 단일, 다중, 선택 없음. 히트맵 켬과 끔. 밀도 슬라이더 미리보기), 아틀라스 픽셀과 원본 픽셀의 편집 캔버스를 렌더링합니다 | `texture-workspace/*.png` |
| `AtlasFramePerfTool` | `tml`에서 아틀라스 페이지와 텍스처 패널의 프레임 비용을 소프트웨어 페인터로 헤드리스 측정합니다. `ImageComposeScene`에 대기, 호버, 모서리(밀도) 드래그, 휠 확대/축소, 타일 드래그, 와이어프레임 없는 호버의 포인터 입력을 주고 모든 렌더링의 시간(중앙값, p90, 최대)을 잽니다. 모서리 드래그 도중의 프레임, 열린 편집 세션, 안정된 페이지, 가까운 확대를 저장합니다. GPU 렌더러에는 창의 Skia 컨텍스트가 필요하므로 `AtlasWindowPerfTool`과 `PreviewWindowTool`이 측정합니다 | `atlas-frame-perf/report.txt`, `{corner-drag,session,frame,zoomed}.png` |
| `AtlasWindowPerfTool` | 텍스처 작업 공간에서 실제 편집기 창을 열고(`PSD2LIVE_SAMPLE`은 `.psd`나 `.psd2live`를 가리킬 수 있음. 사본을 쓰세요) 포인터 입력을 AWT 이벤트로 보내므로 실제 커서는 그대로 있습니다. 뷰 모델을 통한 이동과 밀도 변경, 타일 드래그, 모서리 드래그, 서로 기다리지 않는 드래그 여섯 개. 프레임 간격, UI 지연, 각 바쁨 플래그가 내려간 시점, UI 스레드 핫스폿, 그려진 드래그 미리보기 수, 보기별 GPU 프레임 수를 보고합니다. 캡처는 화면이 아니라 창 자체의 Skia 프레임에서 가져옵니다 | `atlas-window-perf/report.txt`, `*.jfr`, `*-after.png` |
| `PreviewFpsTool` | 실제 편집기 창을 열고 미리보기를 재생합니다(`PSD2LIVE_WORKSPACE`는 작업 공간 프리셋을, `PSD2LIVE_BACKENDS`는 런타임을 고름). 프레임 속도와 프레임 간격 백분위수, 창의 프레임 주기 중 Compose의 비중, UI 스레드를 바쁘게 하는 것, 문서 상태가 얼마나 자주 바뀌는지를 보고합니다. `PSD2LIVE_VSYNC=0`은 디스플레이를 기다리지 않고 렌더링해 실제 비용을 보여 주고, `PSD2LIVE_PANELS=0`은 측면 패널을 숨기고, `PSD2LIVE_TRACE=1`은 각 컴포저블의 초당 구성 횟수를 세고, `PSD2LIVE_JFR=1`은 UI 스레드 핫스폿을 기록해 나열하고, `PSD2LIVE_SVG=1`은 마지막 프레임의 모든 그리기를 SVG로 기록하고 세며, `PSD2LIVE_INVALIDATIONS=1`은 창 컴포지터가 전체 그림을 다시 그리게 하는 요청이 어디서 오는지 나열하고, `PSD2LIVE_CAPTURE=1`은 일시 정지하고 GPU에서 창을 다시 읽으며(`PSD2LIVE_COMPOSITOR=0`은 비교를 위해 컴포지터를 끔), `PSD2LIVE_SCRUB=EDIT`(또는 `PREVIEW`)는 재생 대신 그 모드의 캔버스에서 패널이 하는 방식대로 파라미터 슬라이더를 드래그하고(`PSD2LIVE_SCRUB_PARAMETER`는 파라미터를, `PSD2LIVE_SCRUB_HZ`는 초당 샘플 수를 고름, 기본 240) 각 샘플의 UI 스레드 시간을 보고하며, `PSD2LIVE_GESTURE=hover`(또는 `deform`)는 편집기 자체의 진입점을 통해 편집 캔버스의 얼굴 메시 위에 호버합니다(또는 변형 모드에서 모든 점을 드래그). 시스템 포인터는 움직이지 않습니다. 보고서에는 컴포지터의 프레임 수와 전체 다시 그리기 횟수도 나옵니다 | `preview-fps/report.txt`, `{cubism,p2lrt}.{jfr,svg}`, `*-paused.png` |
| `PreviewWindowTool` | OpenGL 위의 Skia로 실제 편집기 창을 열고, 편집 캔버스와 두 미리보기 런타임이 창의 Skia 컨텍스트에서 그리는지, 재생 2초 동안 프레임이 몇 개 오는지 확인하며, 각 보기의 텍스처를 화면이 아니라 GPU에서 다시 읽습니다. 2분 동안 멈추면 스레드를 덤프하고 종료합니다 | `preview-window/report.txt`, `{edit,cubism,p2lrt}.png` |
| `TexturePerfTool` | 텍스처 명령(타일 고정, 밀도 변경, 배치, 예산 변경)의 커밋 시간: `runtime`은 애플리케이션 명령을 거치며 재구성 시간을 따로 잽니다. `TEXTURE_PERF_SCENARIO`는 `plain`, `skeleton`(자동 뼈대), `deleted`(레이어 하나를 소프트 삭제한 자동 뼈대) 중에서 고르고, `TEXTURE_PERF_JFR=1`은 그 회차를 `texture-perf/runtime.jfr`에도 기록합니다. `desktop`은 뷰 모델과 데스크톱 어댑터를 거치며 Swing 스레드 멈춤, UI 캡처, 페이지 PNG를 기록합니다. 환경 변수는 Gradle 작업 입력이 아니므로 시나리오를 바꿀 때는 `--rerun`을 추가하세요 | `texture-perf/runtime-<scenario>.txt`, `desktop.txt` |
| `ExportDialogTool` | 가져오기와 다른 형식으로 내보내기 하위 메뉴가 열린 파일 메뉴, 모든 대상의 다른 형식으로 내보내기 대화 상자, Live2D와 PSD 내보내기 대화 상자, 내보내기 완료 대화 상자(중국어와 영어, 다크 테마. 성공 대화 상자는 라이트도)로 메뉴 그룹, 레이블, 레이아웃을 확인합니다 | `export-dialog/<language>-<target or menu>.png` |
| `ModalDialogTool.renderDialogs` / `.depthSplit` | 공유 모달 프레임 위의 대화 상자(중국어)로 제목 행, 본문, 바닥글이 맞는지 확인합니다. `renderDialogs`는 설정, 도움말, 텍스처 고해상도화, 그리기 순서 대화 상자(다크 테마), `depthSplit`은 앞뒤 분리 대화 상자(다크, 라이트, 큰 글자) | `modal-dialog/<dialog>.png`, `depth-split-{dark,light,large-text}.png` |
| `CanvasLookSheetTool.meshLook` / `.toolIcons` | 캔버스가 그리는 것의 시트: `meshLook`은 편집 가능한 기하의 모양(`MeshLook`)을 각 상태(구조, 선택, 호버, 브러시 범위)에서 어두운, 중간, 밝은, 채도 높은 원화 위에 그리고, `toolIcons`는 모든 캔버스 도구 아이콘과 미리보기 레일의 토글을 두 테마로 그립니다 | `canvas-look/mesh-look.png`, `tool-icons-{dark,light}.png` |
| `CommitPerfTool.profile` / `.desktop` | 작성 커밋 하나의 벽시계 시간: `profile`은 애플리케이션 명령 경계를 거치며 단계(리비전, 설정 디코딩, 재구성, 기하 검사)별로 나누고, `desktop`은 데스크톱 뷰 모델과 어댑터를 거쳐 메시 정점 편집과 브러시 스트로크를 연달아 커밋하며 커밋 시간과 가장 긴 UI 스레드 멈춤을 보고합니다. 샘플링하려면 `JAVA_TOOL_OPTIONS=-XX:StartFlightRecording=...`와 함께 쓰세요 | `commit-perf/report.txt`, `desktop.txt` |
| `CommitPerfTool.baseline` | 자동 뼈대, 흔들림 두 개, 베이크된 시뮬레이션이 있는 프로젝트에서 커밋 경로의 단계별 기준선: 프로세스가 저장한 작성 리그를 빼고 기본 리그를 생성해 저널을 재생하는 전체 재구성의 모든 단계(분석, 아틀라스 패킹과 PNG 인코딩, 기본 리그, 뼈대 캐시 적중/실패, 저널 재생, 흔들림/시뮬레이션 반영, 재정의, IR, moc3 번들, `validateBundle`, 리비전 해시, 런타임 파일 기록), 그리고 기하 커밋, 작은 레이어 칠하기, 이미지 교체(같은 모양 / 메시 재구성), 관련 없는 토폴로지 편집 뒤의 뼈대 캐시, 저널 항목 50/200개를 덧붙였을 때의 커밋 시간과 증가량. 히스토리는 커밋마다 저장됩니다. 네이티브 미리보기 다시 불러오기(GL 컨텍스트 필요)는 측정하지 않습니다 | `commit-perf/baseline.json`, `baseline.md` |
| `CommitPerfTool.rigStages` | `baseline`의 준비 단계와 생성 커밋(레이어 분류, 레이어 메시)만 실행하며, 각 커밋에서 리그 빌드의 단계를 기록한 다음 결과를 전체 재구성합니다. `PSD2LIVE_RIG_MODES`를 `staged`나 `unstaged`로 설정하면 단계 캐시의 한 모드만 재며, 기본은 둘 다입니다 | `commit-perf/rig-stages.json`, `rig-stages.md` |
| `SkeletonCommitTool.profile` / `.equivalence` | 뼈대 커밋: `profile`은 `PSD2LIVE_SCALE`배(기본 2) 확대한 샘플에 `PSD2LIVE_GEOMETRY_EDITS`개(기본 24)의 기하 커밋을 한 다음, 명령 경계를 통해 본을 만들고 바인딩하고 관절을 옮기며(가장 큰 메시를 스키닝하는 팔다리에서 하나), 커밋 시간과 리그 빌드의 단계를 보고합니다. `equivalence`는 뼈대, 기하, 정점 그룹 커밋, 실행 취소, 다시 실행, 분기 전환 뒤의 런타임 모델을 빈 캐시에서 저장된 작성 리그 없이 콜드 재구성하고 재생한 결과와 비교합니다 | `skeleton-commit/report.txt`, `equivalence.txt` |
| `SkeletonBakeDiffTool` | 뼈대 베이크 전후: 확대한 샘플(`PSD2LIVE_SCALE`, 기본 2)의 모든 키폼 격자 칸과 블렌드 셰이프 키에서 스키닝된 모든 메시가 놓이는 곳을 캔버스 픽셀로 기록하고, 각 팔다리를 극단 포즈에서 렌더링합니다. `PSD2LIVE_LABEL`은 실행 이름을 정하고, `PSD2LIVE_REFERENCE`를 주면 그 실행과 비교합니다. 메시별 최대 정점 차이(픽셀)와 렌더링 차이 | `skeleton-bake/<label>.bin`, `<label>-limbs.png`, `<label>-vs-<reference>.png` |
| `OpenPerfTool.profile` | 앱처럼 프로젝트를 여는 시간(`ProjectRepository.open` 다음 새 `WorkspacePreviewBuilder`가 헤드를 빌드): 자동 뼈대, 흔들림 두 개, 베이크된 시뮬레이션이 있는 프로젝트를 지금 앱이 쓰는 방식(각 리비전의 작성 리그와 헤드 캐시), 헤드 캐시만(저장된 리그 없이 생성과 재생), 둘 다 없이의 세 가지 압축 파일로 저장하고, 각각을 빈 작성 리그 캐시와 뼈대 캐시에서 세 번씩 열어 압축 해제/시드와 헤드 빌드 시간을 재고 세 가지가 같은 모델을 만드는지 확인합니다 | `open-perf/report.json`, `report.md` |
| `SavePerfTool.profile` | 현실적인 크기의 생성 프로젝트(`PSD2LIVE_SAVE_LAYERS`, `PSD2LIVE_SAVE_SIZE`, `PSD2LIVE_SAVE_REVISIONS`)를 저장하는 시간: 같은 캡처의 저장 세 번(두 번은 다시 연 뒤)과 열기 한 번 | `save-perf/report.txt` |
| `ExportTargetsScenario`, `GenerateAndRigScenario`, `PaintAndReplaceScenario`, `SplitThenRegenerateScenario`, `Cmo3ImportScenario`, `MotionScenario` | 합성 캐릭터로 하는 긴 엔드투엔드 시나리오로, 스위트의 시나리오(`scenario/`)처럼 단계마다 콜드 재생, 실행 취소, 품질을 검사합니다. 모든 대상으로 내보내기, 가져오기부터 내보내기까지의 첫 세션, 뼈대 아래에서의 칠하기와 이미지 교체, 분할 후 재생성, cmo3를 가져와 편집하고 다시 내보내기, 내장 모션을 편집해 내보내기. 전체 파이프라인을 너무 오래 돌리므로 스위트에 넣지 않았습니다. 생성, 내보내기, 저장을 바꾼 뒤 실행하세요 | 테스트 결과만 |
| `RigSourcePerfTool.profile` | 생성 입력이 있는 큰 문서(샘플을 `PSD2LIVE_SCALE`배, 기본 3 확대)에서 재구성의 픽셀 비례 단계: 세 번째 레이어마다 몇 픽셀씩 잘라 내고(고정된 사각형으로 다시 채움), 다섯 번째마다 밀도를 두 배로 합니다. 콜드 빌드, 웜 재구성, 칠한 뒤 재구성의 단계 시간과 콜드 아틀라스 페이지 합성, 미리보기 PNG 인코딩 | `rig-source-perf/report.txt` |
| `MeshTraceTool.compare` / `.wrap` | `compare`는 합성 레이어(1024² 텍스처의 속눈썹 있는 눈, 가는 머리카락 가닥, 캔버스 해상도 레이어, 6000 px와 2048 px 문서의 레이어)를 캔버스 추적과 텍스처 추적으로 메시화해 나란히 렌더링하고(텍스처 알파는 회색, 메시는 파랑, 메시 밖의 불투명 텍스처 픽셀은 빨강), 정점, 시간, `detail`, 덮이지 않은 픽셀, 메시 면적을 보고합니다. `wrap`은 속눈썹, 머리카락 가닥, 손을 감싸기 0-32에서 텍스처 추적으로 메시화하고 정점, 윤곽 루프, 시간, 덮이지 않은 픽셀을 보고합니다 | `mesh-trace/<case>.png`, `report.txt`; `mesh-wrap/<case>.png`, `report.txt` |
| `ReadmeShotsTool` | 실제 편집기 창을 열고, 시작 페이지에서 전체 프리셋을 골라 `PSD2LIVE_SAMPLE`을 가져오고, 메시별로 레이어 나누기가 제안하는 모든 분할을 받아들이고, 시뮬레이션 베이크를 기다리고, 자동 뼈대를 커밋한 다음, 각 작업 공간과 캔버스 모드에서 창 자체의 프레임(화면이 아님)을 캡처하고 프로젝트를 `project.psd2live`로 저장합니다. `PSD2LIVE_SHOT_REUSE=1`은 저장한 프로젝트를 다시 열고, `PSD2LIVE_SHOT_W` / `_H`는 창 크기를 정합니다. 프로젝트 페이지 스크린샷용 | `readme-shots/<sample>/*.png`, `project.psd2live` |
| `ReadmeShowcaseTool` | 프로젝트 페이지용 테스트 플레이트: `meshStress`, `meshParams`, `meshWrap`은 합성 레이어를 메시화합니다(구멍과 가는 연결부, 가장자리 링 × 채우기, 감싸기 폭). `rigHead`, `rigFace`, `rigBody`는 두 샘플의 머리, 얼굴, 뼈대 프리셋 포즈를 보여 줍니다. `rigKnee`는 `PSD2LIVE_KNEE_SAMPLE`(프로젝트 또는 PSD)의 무릎을 굽힙니다. `bodyOrbit`은 `PSD2LIVE_ORBIT_PROJECTS`의 각 프로젝트에 몸 X / Y 클립(위아래, 좌우, 타원)을 추가하고 물리를 켜고 끈 PNG 프레임을 내보냅니다(`PSD2LIVE_ORBIT` 진폭, `PSD2LIVE_ORBIT_PERIOD` 한 바퀴의 초) | `readme-plates/*.png`, `readme-orbit/<project>-{on,off}/` |

| 변수 | 효과 |
| --- | --- |
| `PSD2LIVE_SAMPLE` | 예제 이름(`tml`, `ds`) 또는 PSD 경로. 기본은 `tml`. `CommitPerfTool.desktop`은 `.psd2live` 프로젝트도 받음 |
| `PSD2LIVE_CMO3` | `ModelProfileTool.cmo3`의 입력: `.cmo3` 파일 또는 그런 파일이 있는 디렉터리 |
| `PSD2LIVE_PROBES` | 대역 프로필이 탐침하는 파라미터, `id=value,...`. 기본은 몸 X, Y, Z의 양 끝 |
| `PSD2LIVE_SHEET_PARAM` | 실루엣을 몸 X × 몸 Y 대신 이 파라미터를 따라 배치 |
| `PSD2LIVE_BONES` | `MotionSheetTool.body`에서 자동 뼈대를 보정하는 본 위치, 캔버스 픽셀 단위의 `id=headX,headY,tailX,tailY;...` |
| `PSD2LIVE_BIND_LEGS` | `1`이면 다리와 신발 메시를 첫 번째 허벅지 본에 바인딩 |
| `PSD2LIVE_ZOOM` | 다리 확대의 프레임, 캔버스에 대한 비율로 `left,top,right,bottom` |
| `PSD2LIVE_VERBOSE` | `1`이면 `motions`가 모든 커브도 출력 |
| `PSD2LIVE_BAKE_CONFIGS` | `SimBakeSuite`의 설정, `modes:keys,...`. 기본은 `preset`(각 부위 자체의 설정) |
| `PSD2LIVE_SIM_PARTS` | `SimBakeSuite` / `SimRootDofExperiment`의 부위, 쉼표로 구분. 기본은 전부 |
| `PSD2LIVE_BAKE_METHODS` | `SimBakeSuite`의 베이크 방식, `legacy,dof` |
| `PSD2LIVE_SIM_INPUTS` | `SimBakeSuite`가 이 입력만 유지 |
| `PSD2LIVE_SIM_CSV` | `SimBakeSuite`가 끝 궤적, 모드 값, 입력을 CSV로 기록할 모션 |
| `PSD2LIVE_RUNTIME_PROJECT` | `RuntimeConformanceTool.generate`가 사례 `project`로 함께 기록하는 프로젝트(`.psd2live` 또는 PSD) |
| `PSD2LIVE_GEOMETRY_EDITS` | `SkeletonCommitTool.profile`이 뼈대 커밋 전에 하는 기하 커밋 수. 기본 24 |
| `PSD2LIVE_RIG_MODES` | `staged` 또는 `unstaged`이면 `CommitPerfTool.baseline` / `.rigStages`의 생성 커밋을 그 모드로 한정. 기본은 `both` |
| `PSD2LIVE_TEST_CUBISM_CORE` | 공식 Cubism Core 라이브러리 경로. `CubismCoreCheckTool`과 `:umamo`의 `MocDefaultColorsTest`용이며, 없으면 둘 다 건너뜀 |

## 코드 구조

소스는 의존성이 아래로만 향하는 Gradle 모듈로 나뉘며, 빌드가 이를 강제합니다. Umamo에서 이식한 엔진 `org.umamo.*`은 `:umamo`(`umamo/src/main/kotlin/`. 제품 코드에 의존할 수 없음)에 있습니다. 중립 리그 IR `:format-model`, 내보내기 프레임워크 `:format-compile`, 래스터 내보내기 `:targets:raster`, 런타임 리그 `:targets:runtime`, 런타임 바인딩 `:format-eval`은 MIT이며 GPL 모듈에 의존하지 않습니다. `:targets:cubism`(IR 변환기, moc3, cmo3)과 `:targets:psd`는 엔진을 씁니다. 제품 계층 `io.github.psd2live.*`은 루트 프로젝트(`src/main/kotlin/`)에 있습니다. [중립 리그 IR과 내보내기 대상](../spec/EXPORT_TARGETS.md)을 참고하세요. 엔진 패키지는 다음과 같습니다.

| 패키지 | 역할 |
| --- | --- |
| `org.umamo.runtime` | `PuppetModel`, 키폼 보간과 평가 |
| `org.umamo.format` | PSD, CMO3, MOC3, 이미지 형식 읽기와 쓰기 |
| `org.umamo.interop` | `PuppetModel`과 CMO3 / MOC3 사이의 변환 |
| `org.umamo.render` | LWJGL / OpenGL 미리보기 |
| `org.umamo.edit` | 모델에 대한 불변 편집 기본 연산 |
| `io.github.psd2live.core` | 생성 파이프라인(`PSD2LivePipeline`, `LayerClassifier`, `AdaptiveMeshGenerator`, `RigBuilder`, `MotionGenerator`, `PhysicsGenerator`)과 재생 가능한 편집 |
| `io.github.psd2live.project` | 중립 작업 공간 문서(`WorkspaceDocument`), 히스토리 저장소(`WorkspaceStore`), `.psd2live` 압축 파일. `application`, Compose, MCP에 의존하지 않음 |
| `io.github.psd2live.application` | 중립 런타임(`WorkspaceRuntime`), 명령 레지스트리와 스키마, 미리보기 세션, 프로세스 내 백그라운드 작업. UI와 MCP가 공유 |
| `io.github.psd2live.history` | 분기형 실행 취소 / 다시 실행 |
| `io.github.psd2live.agent` | 로컬 MCP 서버와 공개 도구 정의 |
| `io.github.psd2live.render` | 편집 캔버스와 p2lrt 미리보기의 GPU 렌더링(`CanvasGpu`, `GlCanvasRenderer`, `P2lrtGlRenderer`, `SkiaGpu`) |
| `io.github.psd2live.ui` | Compose UI: `state`(ViewModel, 단축키 레지스트리), `views`(작업 공간과 패널), `components`(대화 상자와 컨트롤), `tutorial`. 캔버스 편집과 페인트는 `ui` 루트 패키지에 있음 |
| `io.github.psd2live.i18n` | UI 문자열. 리소스는 `src/main/resources/i18n/`에 있음 |

생성과 내보내기 로직은 Compose 코드가 아니라 `core` / `project` / `application`에 두세요.

시작, 도움말, 설정은 앱 페이지(`ui/state/AppPages.kt`의 `AppPage`)이며 프로젝트의 일부가 아닙니다. 각 페이지는 편집기의 도크(`DockWorkspaceView`의 `DockSession`과 `DockTree`)를 재사용하고, 목차 패널과 주제 패널로 이루어지며(시작 페이지에는 상세 패널이 추가됨), 패널 ID에는 페이지의 접두사(`start.*`, `help.*`, `settings.*`)가 붙고, `PageChrome.kt`의 목차 행과 본문 텍스트를 공유합니다. 기본 배치는 `pageDockLayout`이며, 배치와 닫힌 패널은 로컬 환경 설정(`AppSettings.pageArrangement`)에 저장됩니다. 패널은 드래그되고, 떠 있게 되고, 다시 도킹되므로, 패널끼리 공유하는 상태는 한 패널의 컴포지션이 아니라 ViewModel이나 `StartPageModel`에 두어야 합니다. 새 기능 텍스트는 `src/main/resources/whatsnew/<language>.md`이며, 네 언어는 같은 릴리스와 항목을 나열해야 합니다(`WhatsNewTest`). 빌드는 버전을 `psd2live/version.properties`에 찍으며, 코드는 `AppVersion`을 통해 이를 읽습니다.

## 핵심 규칙: 재구성과 재생

`PuppetModel`은 저장되지 않습니다. 프로젝트는 원본 원화, 레이어 분류, 설정과 함께 직렬화 가능한 편집 기록(`RigEditOverlay`)을 저장합니다. 열거나 바뀔 때마다 다음을 수행합니다.

1. `RigBuilder`가 원본 원화에서 기본 리그를 다시 생성합니다.
2. 작성 단계(`RigEditOverlay.replayAuthored`)는 저널의 마지막 `rig_checkpoint` 기록이 저장한 리그에서 시작하거나, 없으면 기본 리그에서 레거시 정적 필드(파라미터 삭제 / 생성, 워프, 구조, 키폼)를 먼저 재생한 다음, 그 뒤의 `authoringJournal` 항목을 기록된 순서대로 재생합니다.
3. 마무리(`RigEditOverlay.finish`)는 생성기 그래프(`core/DocumentGenerators.kt`)의 순서대로 생성기(흔들림, 시뮬레이션, ...)를 실행하고, 생성 재정의(`generated_override`)를 3방향 병합한 다음, 생성된 파라미터를 가리키는 패널 편집을 재생합니다.

작성 리그가 저장된 리비전(이 프로세스에서 빌드했거나 압축 파일의 `rig/`에 저장된 경우)은 그 리그에서 빌드하며, 기본 리그를 생성하거나 저널을 재생하지 않습니다. [실체화된 리그](../spec/MATERIALIZED_RIG.md)와 [문서 계층](../spec/DOCUMENT_LAYER.md)을 참고하세요.

새 편집 기능에서 이것이 뜻하는 바는 다음과 같습니다.

- 변경을 직렬화 가능한 `RigEditOverlay` 필드에(가급적 작성 저널의 JSON 명령으로) 저장하고, 작업 공간 상태 코덱에서 저장 / 복원하세요. `PuppetModel`에만 한 변경은 재구성할 때 사라집니다.
- UI와 MCP는 같은 편집 명령을 써야 합니다. `org.umamo.edit`의 저수준 메서드가 자동으로 공개 인터페이스가 되는 것은 아닙니다.
- 인수 경로: 도메인 데이터 → 히스토리 재생 → 프로젝트 저장과 다시 열기 → 대상 Cubism 버전 처리 → 내보내기 다시 읽기 → 육안 확인. [런타임과 내보내기 참조](../spec/RUNTIME_EXPORT_ARCHITECTURE_AND_GAPS.md)를 참고하세요.

새 UI 문자열은 키 수가 같게 유지되도록 `Messages.properties`, `Messages_zh_CN.properties`, `Messages_ja.properties`, `Messages_ko.properties`에 함께 추가하세요(`CatalogContractsTest`가 검사).

## 내보내기 확인

- `.model3.json`이 참조하는 모든 파일을 납품하세요.
- 로그 패널과 진단 JSON의 경고를 검토하세요.
- 계속 편집하려면 `.psd2live` 프로젝트를 저장하세요. `.psd2live.json`은 보고서일 뿐입니다.

관련 문서: [Cubism 네이티브 미리보기](CUBISM_SDK_SETUP.md) · [CI와 릴리스](CUBISM_CI_RELEASE.md) · [`build.gradle.kts`](../../../build.gradle.kts)
