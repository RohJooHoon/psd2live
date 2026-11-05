# 선택 사항: Cubism 네이티브 미리보기

[문서 목록](../../README.md) · [中文](../../zh/guide/CUBISM_SDK_SETUP.md) · [English](../../en/guide/CUBISM_SDK_SETUP.md) · [日本語](../../ja/guide/CUBISM_SDK_SETUP.md) · [사용자 가이드](USER_GUIDE.md) · [CI와 릴리스](CUBISM_CI_RELEASE.md)

내장 렌더러와 기본 내보내기에는 공식 SDK가 필요하지 않습니다. 이 페이지는 공식 런타임의 렌더링과 물리를 관찰하기 위해 저장소의 네이티브 미리보기 브리지를 설정하는 방법을 설명합니다. 모든 편집기 기능이나 픽셀이 똑같이 동작한다고 보장하지는 않습니다.

## 요구 사항

### Windows x64
- CMake 3.16+
- Visual Studio 2022 C++ / MSVC 143
- 로컬 Cubism 5 SDK for Native(5-r.5 구조)
- 필요한 파일: `Core/lib/windows/x86_64/143/Live2DCubismCore_MT.lib`

### Linux x86_64 / arm64
- 지원 플랫폼: X11/GLX가 있는 Linux x86_64와 arm64(XWayland와 `xvfb-run` 포함). arm64는 SDK의 실험적 Cubism Core를 사용
- 지원하지 않음: XWayland가 없는 순수 Wayland, musl/Alpine 시스템
- CMake 3.16+
- C++14를 지원하는 GCC 또는 Clang
- OpenGL / GLX 개발 패키지. 예: Debian/Ubuntu는 `libgl1-mesa-dev`와 `libglx-dev`(또는 `libglx-mesa-dev`), Fedora는 `mesa-libGL-devel`
- X11 개발 라이브러리(`libx11-dev` / `libX11-devel`)
- 실행 환경: 동작하는 X11 `DISPLAY`(데스크톱 세션, 또는 `xvfb-run`을 통한 헤드리스)
- 로컬 Cubism 5 SDK for Native(5-r.5 구조)
- 필요한 파일: `Core/lib/linux/x86_64/libLive2DCubismCore.a`(arm64: `Core/lib/experimental/linux/arm64/libLive2DCubismCore.a`)

## 빌드와 배치

### Windows

```powershell
$env:CUBISM_SDK_ROOT = "D:\path\to\CubismSdkForNative-5-r.5"
.\native\build_live2d_renderer.bat -Clean -Deploy
```

저장소 루트에서 실행합니다. `-Deploy`는 DLL과 셰이더를 아래의 gitignore된 디렉터리로 복사합니다. `-Clean`은 이전 빌드 캐시 없이 다시 빌드합니다.

```text
src/main/resources/cubism/windows-x86_64/
├── live2d_renderer.dll
└── FrameworkShaders/
```

### Linux

```bash
export CUBISM_SDK_ROOT=/path/to/CubismSdkForNative-5-r.5
./native/build_live2d_renderer.sh --clean --deploy
```

저장소 루트에서 실행합니다. `--deploy`는 공유 라이브러리와 셰이더를 아래의 gitignore된 디렉터리로 복사합니다. `--clean`은 이전 빌드 캐시 없이 다시 빌드합니다.

```text
src/main/resources/cubism/linux-x86_64/   (arm64: linux-arm64/)
├── liblive2d_renderer.so
└── FrameworkShaders/
```

## 확인

애플리케이션을 시작하고 모델을 불러온 다음 렌더러 상태와 로그를 확인합니다. 네이티브 리소스가 없거나 불러올 수 없으면 미리보기는 PSD2Live 런타임(p2lrt)으로 전환되고, 로그에 Cubism을 사용할 수 없는 이유가 기록됩니다. 런타임 라이브러리도 사용할 수 없을 때만 편집 캔버스의 그리기로 대체합니다. 모델이 보인다는 것만으로는 SDK가 로드되었다는 증거가 되지 않습니다.

## 다른 위치와 문제 해결

또는 저장소의 `cubism/windows-x86_64/`(또는 `cubism/linux-x86_64/`), `CUBISM_SDK_PATH`, `LIVE2D_SDK_PATH`, JVM 속성 `psd2live.cubism.path`로 배치된 리소스를 가리킬 수 있습니다. 이 런타임 경로는 컴파일에 쓰는 SDK 소스 루트 `CUBISM_SDK_ROOT`와 다릅니다.

네이티브 라이브러리와 FrameworkShaders는 함께 두세요.

### Windows
VCRUNTIME / MSVCP 오류는 오래된 /MD 빌드를 뜻할 수 있습니다. 현재 스크립트는 /MT와 정적 Core를 사용합니다. `dumpbin /DEPENDENTS`로 확인하세요.

### Linux
`ldd liblive2d_renderer.so`로 의존성을 확인합니다. 필요한 시스템 라이브러리: `libGL.so`, `libGLX.so`(또는 Mesa GLX), `libX11.so`, `libpthread.so`, `libdl.so`.

오프스크린 미리보기는 GLX 컨텍스트를 열며 유효한 X11 `DISPLAY`가 필요합니다. 헤드리스 호스트에서는 Xvfb를 설치하고 예를 들어 `xvfb-run -a ./gradlew run`(또는 `xvfb-run -a java -jar …`)으로 실행합니다. `DISPLAY`가 없으면 네이티브 초기화가 실패하고, 미리보기는 PSD2Live 런타임으로 전환됩니다(런타임 라이브러리도 사용할 수 없을 때만 편집 캔버스의 그리기로 대체).

환경 변수를 바꾼 뒤에는 실행하는 프로세스를 다시 시작하세요.

오픈 소스 브리지 코드만 제공됩니다. 공식 Core, Framework, 셰이더는 따로 받아 각각의 라이선스를 따르세요. 이 가이드는 그 구성 요소에 대한 어떠한 권리도 부여하지 않습니다.

자동 빌드와 Cubism을 포함한 릴리스는 [CUBISM_CI_RELEASE](CUBISM_CI_RELEASE.md)를 참고하세요(공개 앱 저장소의 공개 Release에 SDK zip을 첨부하지 마세요).

[네이티브 빌드](../../../native/live2d_renderer/README.md) · [빌드 스크립트](../../../native/) · [서드파티 고지](../../../THIRD_PARTY_NOTICES.md)
