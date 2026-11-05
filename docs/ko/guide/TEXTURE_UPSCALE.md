# 텍스처 고해상도화

[문서 목록](../../README.md) · [中文](../../zh/guide/TEXTURE_UPSCALE.md) · [English](../../en/guide/TEXTURE_UPSCALE.md) · [사용자 빠른 참조](USER_GUIDE.md) · [개발과 명령줄](DEVELOPMENT.md)

이 문서는 앱 내 입문자 코스의 19강에 해당합니다. 고해상도화는 아틀라스 패킹 전에 레이어별로 텍스처를 확대하며 기본적으로 꺼져 있습니다. 원본 PSD, 캔버스 크기, 바인딩은 바뀌지 않습니다. 미리보기와 내보내기는 처리된 텍스처를 사용합니다.

## 설정과 사용

**모델 → 텍스처 고해상도화…**(기본 `Ctrl+U`)를 열고 다음을 설정합니다.

| 필드 | 입력할 내용 |
| --- | --- |
| Python | nunif 의존성이 설치된 Python 실행 파일. 인수가 붙은 명령 문자열이 아님 |
| nunif 디렉터리 | `nunif`와 `waifu2x` 하위 디렉터리가 있는 로컬 소스 디렉터리 |
| 모델 디렉터리 | Art `.pth` 가중치가 실제로 들어 있는 디렉터리. 앱이 `swin_unet_v3/art`와 `swin_unet/art` 경로를 감지할 수 있음 |
| 배율 | 1은 끔. 2× 또는 4×로 확대 |
| 노이즈 제거 | -1은 확대만 함. 0–3은 사용하는 모델이 지원하는 노이즈 제거 단계이며 기본값은 1 |
| Neural Alpha | 기본으로 켜짐. 끄면 알파는 쌍선형으로 확대됨 |
| 입력 타일 | 64–512, 기본값 256. 비디오 메모리가 부족하면 줄임 |

적용하기 전에 로컬 Python, PyTorch, nunif와 그에 맞는 가중치를 준비하세요. 특정 업스트림 버전의 설치 요구 사항은 해당 버전에 포함된 안내를 참고하세요. 릴리스 패키지에는 완전한 추론 환경이 포함되어 있지 않습니다. 적용하면 미리보기가 다시 만들어지고 설정은 프로젝트와 함께 저장됩니다. 다른 컴퓨터에서는 로컬 경로를 다시 확인하세요.

4×는 네이티브 4× 가중치를 우선 사용합니다. 없으면 현재 워커가 지원되는 2× 모델을 두 번 실행할 수 있습니다. 맞는 모델이 없으면 오류이며, 단순 보간을 초해상도로 내세우는 일은 없습니다.

## 명령줄 예시

```powershell
.\gradlew.bat run --args="--input examples/tml/psd-input/tml.psd --output build/upscaled-model --upscale 2 --upscale-python C:/venv/Scripts/python.exe --nunif-dir C:/nunif --upscale-model C:/nunif/waifu2x/pretrained_models/swin_unet_v3/art --upscale-tile 256"
```

Neural Alpha는 `--no-upscale-neural-alpha`로 끕니다. 다른 옵션은 [CLI 표](DEVELOPMENT.md)를 참고하세요. 예시 경로는 자신의 컴퓨터에 있는 경로로 바꾸고, 공백이 있는 경로는 실행기가 요구하는 방식으로 따옴표로 감쌉니다.

## 결과와 리소스

RGB와 알파는 따로 처리되며, 샘플링되는 가장자리의 오염을 줄이기 위해 색을 투명 영역까지 확장합니다. 그래도 Neural Alpha는 반투명 윤곽을 바꿀 수 있습니다. 머리카락 가닥, 속눈썹, 입술 선, 마스크 가장자리를 밝은 배경과 어두운 배경 모두에서 움직이며 확인하세요.

추론은 레이어별로 하나씩 실행되며, 출력과 아틀라스 패킹도 시스템 메모리를 사용합니다. 캐시는 사용자 디렉터리의 `.psd2live/cache/upscale`에 있고 소재, 설정, 코드, 가중치를 키로 합니다. 앱을 종료한 뒤 지울 수 있습니다. 모델 오류, 크기 제한, 시간 초과가 발생하면 아틀라스만 키워서 다시 시도하지 말고 로그를 확인하세요.

구현: [설정과 프로세스 관리](../../../src/main/kotlin/io/github/psd2live/core/TextureUpscale.kt), [nunif 워커](../../../src/main/resources/upscale/nunif_worker.py).
