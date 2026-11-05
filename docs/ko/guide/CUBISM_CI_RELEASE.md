# Cubism CI와 릴리스 워크플로

[문서 목록](../../README.md) · [中文](../../zh/guide/CUBISM_CI_RELEASE.md) · [English](../../en/guide/CUBISM_CI_RELEASE.md) · [Cubism 네이티브 미리보기](CUBISM_SDK_SETUP.md)

이 문서는 두 GitHub Actions 워크플로, 즉 공개 테스트(SDK 없음)와 선택적 Cubism 미리보기 패키징을 설명합니다. Live2D 라이선스를 대신하지 않으며, 독점 구성 요소의 재배포 권리를 부여하지도 않습니다.

## 두 워크플로

| 워크플로 | 파일 | 목적 |
| --- | --- | --- |
| **CI** | `.github/workflows/ci.yml` | `master`(와 `main`)에 대한 `push` / `pull_request`에서 Ubuntu와 Windows, x64와 arm64로 `./gradlew test`를 실행합니다. Cubism SDK를 내려받지 않으며 `PSD2LIVE_INCLUDE_CUBISM`을 설정하지 않습니다. |
| **Release Cubism** | `.github/workflows/release-cubism.yml` | 수동 `workflow_dispatch` 또는 `v*` 태그 푸시에서 SDK를 비공개로 가져오고, 네이티브 브리지를 빌드하고, Windows/Linux Cubism 빌드(Linux는 x64와 arm64)와 Cubism 없는 Windows ARM64 빌드를 패키징하며, GitHub Release를 게시할 수 있습니다. |

macOS 패키징은 미뤄져 있으며, 이 워크플로는 빌드하지 않습니다.

## 공개 저장소와 SDK 유출(필독)

**공개 저장소에서 GitHub Release 에셋은 공개됩니다.** 이 공개 애플리케이션 저장소의 Release에 `CubismSdkForNative-*.zip`을 첨부하지 마세요.

권장 방법:

1. SDK zip을 **비공개 형제 저장소**에 프리릴리스/태그(예: `internal-sdk/cubism-5-r.5`)로 저장하며, 에셋 이름은 기본적으로 `CubismSdkForNative-5-r.5.zip`입니다.
2. 이 앱 저장소의 저장소 변수 `CUBISM_SDK_SOURCE_REPO`를 그 비공개 저장소(`owner/name`)로 설정합니다.
3. 시크릿 `SDK_FETCH_TOKEN`을 그 비공개 Release를 읽을 수 있는 PAT(또는 세분화된 토큰)로 설정합니다. 설정하지 않으면 워크플로가 `GITHUB_TOKEN`으로 대체하는데, 이 토큰은 보통 다른 비공개 저장소를 **읽을 수 없습니다**.

대안: zip을 다른 비공개 저장소에 두고 가져오기 단계를 수정합니다. 현재 워크플로는 `gh release download`를 사용합니다.

## 변수와 시크릿

| 이름 | 종류 | 기본값 | 비고 |
| --- | --- | --- | --- |
| `CUBISM_SDK_SOURCE_REPO` | 저장소 변수 | 현재 `github.repository` | SDK Release를 호스팅하는 저장소. **공개 앱 저장소는 이를 비공개 저장소로 지정해야 합니다.** |
| `CUBISM_SDK_RELEASE_TAG` | 저장소 변수 | `internal-sdk/cubism-5-r.5` | 비공개 SDK Release / 태그 이름 |
| `CUBISM_SDK_ASSET_NAME` | 저장소 변수 | `CubismSdkForNative-5-r.5.zip` | 에셋 파일 이름 |
| `SDK_FETCH_TOKEN` | 시크릿 | (비어 있음 → `GITHUB_TOKEN`) | 비공개 SDK 저장소에서 가져올 때 사용 |

## 환경 보호

`build-windows`, `build-linux`, `release` 작업은 **`release-cubism`**이라는 GitHub Environment를 사용합니다. Settings → Environments에서 이를 만들고, 필수 검토자 / 배포 브랜치 규칙을 추가해 임의의 협업자가 혼자서 SDK가 포함된 패키지를 만들 수 없게 하세요.

## 릴리스 게시

전제 조건: SDK zip이 비공개 저장소에 업로드되어 있고, 위의 변수, 시크릿, `release-cubism` 환경이 설정되어 있어야 합니다. 아래의 `<version>`은 `3.7.0` 같은 대상 버전을 나타냅니다.

1. **먼저 버전을 올립니다.** 워크플로는 저장소를 수정하지 않습니다. `build.gradle.kts`의 `version`을 설정하고(`packageVersion`과 리소스 `psd2live/version.properties`가 여기서 유도됨), 명령줄 도움말의 버전(네 `Messages*.properties`의 `cli.usage` 항목)과 네 `src/main/resources/whatsnew/<language>.md` 파일의 첫 번째 릴리스도 바꿉니다. CI에서 `VersionConsistencyTest`가 이 모두를 빌드 버전과 맞는지 검사합니다. `docs/zh/CHANGELOG.md`에 릴리스를 추가하고 `.github/release-notes.md`에 릴리스 노트를 작성한 다음(중국어를 먼저, 영어는 접힌 `<details><summary>English</summary>` 블록에. 다운로드 섹션은 넣지 않으며, 릴리스 시 `.github/scripts/release_notes.py`가 실제 에셋으로 분류된 다운로드 배지를 만듦) 커밋하고 푸시합니다.
2. Actions → **Release Cubism** → **Run workflow**를 열고 `version`을 `<version>`(앞의 `v` 없이)으로 설정합니다.
3. 빌드 아티팩트만 원하는 경우가 아니면 `create_github_release`를 켜 둡니다. 미리보기 릴리스에는 `prerelease`를 체크하면 latest로 표시되지 않는 프리릴리스를 게시합니다.
4. `release-cubism` 환경에 필수 검토자가 있으면 여기서 게이트를 승인합니다. 없으면 빌드가 바로 시작됩니다. Windows와 Linux 작업을 기다립니다.
5. 게시가 켜져 있으면 워크플로가 태그 `v<version>`을 GitHub Release(기본은 정식 릴리스, `prerelease`를 체크하면 프리릴리스)로 만들거나 업데이트하며, 다음을 포함합니다.
   모두 `PSD2Live-<version>-<os>-<arch>[-<kind>][-ffmpeg].<ext>` 형식의 이름입니다. os는 `windows` / `linux`, arch는 `x64` / `arm64`, Windows의 kind는 `setup`(설치 프로그램) / `portable`입니다.
   - `PSD2Live-<version>-windows-x64-setup.exe`, `PSD2Live-<version>-windows-x64-portable.zip`
   - 위 두 가지 각각의 ffmpeg 포함 버전: `PSD2Live-<version>-windows-x64-setup-ffmpeg.exe`, `PSD2Live-<version>-windows-x64-portable-ffmpeg.zip`
   - `PSD2Live-<version>-windows-arm64-setup.exe`, `PSD2Live-<version>-windows-arm64-portable.zip`: Windows ARM64. Cubism 없음(Live2D가 이 플랫폼용 Cubism Core를 제공하지 않음), ffmpeg 빌드 없음
   - `PSD2Live-<version>-linux-x64.deb`, `PSD2Live-<version>-linux-arm64.deb`(arm64는 SDK의 실험적 Cubism Core 사용), 그리고 같은 내용의 `PSD2Live-<version>-linux-x64.tar.gz`, `PSD2Live-<version>-linux-arm64.tar.gz`(Deb이 없는 배포판용 앱 이미지)

   3.4.0 이하는 이전 이름(예: `PSD2Live-<version>.exe`, `-windows-x86_64-portable.zip`, `-linux-amd64.deb`)을 유지합니다.

태그 `v<version>`을 푸시해도 같은 워크플로가 실행됩니다. 전체 매트릭스를 두 번 실행하지 않도록 진입점은 하나만 쓰세요.

## 아티팩트와 플랫폼 제한

- Linux 미리보기에는 X11/GLX(XWayland / `xvfb-run` 포함)가 필요합니다. 순수 Wayland와 musl/Alpine은 지원하지 않습니다. [CUBISM_SDK_SETUP](CUBISM_SDK_SETUP.md)을 참고하세요.
- ARM64 패키지는 아직 실제 하드웨어에서 테스트되지 않았습니다. 에셋에 arm64 패키지가 있으면 `release_notes.py`가 이를 알리는 메모를 알려진 제한(Linux arm64의 실험적 Cubism Core, OpenGL 3.1만 지원하는 GPU에서는 캔버스가 그려지지 않을 수 있음)과 함께 추가합니다. 검증을 마치면 메모를 제거하세요.
- v1은 불안정한 GUI 스모크 테스트를 건너뛰며, Linux는 `.deb`가 존재하고 비어 있지 않은지만 확인합니다.
- Windows 패키지는 두 번 빌드합니다. 두 번째 빌드는 `-Ppsd2live.ffmpegDir=<dir>`를 추가해, 고정된 Gyan.dev ffmpeg essentials 빌드(GPLv3. SHA-256과 동영상 및 애니메이션 이미지 내보내기가 쓰는 인코더를 검사)를 `LICENSE.txt`, `README.txt`와 함께 앱의 `resources/ffmpeg/`에 넣습니다. ffmpeg를 업데이트하려면 워크플로의 `FFMPEG_URL`과 `FFMPEG_SHA256`을 함께 바꾸세요. Linux 패키지에는 ffmpeg가 들어가지 않습니다.
- Windows 설치 프로그램은 `packageExe`가 앱 이미지에서 Inno Setup 6과 `packaging/windows/psd2live.iss`로 빌드합니다. `ISCC.exe`는 `-Ppsd2live.iscc`, `ISCC` 환경 변수 또는 Inno Setup 6의 기본 설치 폴더에서 찾으며, 워크플로는 Chocolatey로 Inno Setup을 설치합니다. 파일은 MSI처럼 이전 파일을 먼저 `Config.Msi`로 옮기지 않고 제자리에 복사합니다. 기본적으로 모든 사용자용으로 설치하며(관리자 권한), 시작 시 대화 상자나 `/CURRENTUSER`로 현재 사용자 전용으로 설치할 수도 있습니다. 업그레이드는 설치된 폴더와 설치 모드로 돌아가며, 복사 전에 이전 버전의 `app`과 `runtime` 폴더를 삭제하고 설치 폴더 안의 사용자 파일은 유지합니다. ffmpeg 포함 빌드와 미포함 빌드는 AppId 하나를 공유하며 서로를 대체합니다. 설치 폴더에서 PSD2Live가 실행 중이면 설치 프로그램이 종료한 뒤 다시 시도하라고 묻습니다(자동 설치는 중단됨). 3.1.x 이하 버전은 jpackage MSI 패키지입니다. 설치 프로그램은 업그레이드 코드로 이를 찾아 그 폴더를 기본값으로 쓰고, 그 패키지가 기록한 폴더를 비우고(3.0.0 이하의 제거 프로그램은 폴더를 비움), 설치했던 파일을 삭제하고, 파일을 복사하기 전에 `msiexec /x`로 제거합니다. 그래서 Windows Installer는 등록 해제만 하고 `Config.Msi`로 아무것도 옮기지 않으며(로그는 설치 로그 옆의 `PSD2Live MSI removal.log`), 현재 사용자 설치가 모든 사용자 설치를 만나면 관리자 권한을 요청합니다. 제거할 때 사용자 데이터도 삭제할지 묻습니다(기본은 아니요. 제거 프로그램을 실행한 계정으로 앱의 `--clear-user-data`를 실행하며, 명령줄 제거는 `/CLEARUSERDATA=1`을 전달할 수 있음). 모든 설치와 제거는 `%TEMP%`에 로그(`Setup Log *.txt`)를 기록합니다.
- Cubism이 포함된 패키지는 라이선스가 허용하는 용도로만 쓰세요. 독점 바이너리를 공개 채널에서 재배포하지 **마세요**.

## 라이선스 안내

이 저장소의 브리지 코드는 GPL입니다. Live2D Cubism Core / Framework / 셰이더는 독점 소프트웨어이므로 직접 받아 Live2D의 약관을 따르세요. [THIRD_PARTY_NOTICES.md](../../../THIRD_PARTY_NOTICES.md)를 참고하세요.

[로컬 SDK 설정](CUBISM_SDK_SETUP.md) · [네이티브 스크립트](../../../native/README.md)
