# 구현 개요

[문서 목록](../../README.md) · [中文](../../zh/spec/IMPLEMENTATION_COMPARISON.md) · [English](../../en/spec/IMPLEMENTATION_COMPARISON.md) · [日本語](../../ja/spec/IMPLEMENTATION_COMPARISON.md) · [서드파티 고지](../../../THIRD_PARTY_NOTICES.md)

이 페이지는 현재 구현과 주요 절충점을 파이프라인 단계별로 요약하고, 모든 모듈이 지켜야 하는 불변 조건을 나열합니다. 서드파티 소스는 [서드파티 고지](../../../THIRD_PARTY_NOTICES.md)에 나열되어 있습니다.

| 단계 | 구현과 절충점 |
| --- | --- |
| 소스 | PSD의 픽셀, 순서, 속성을 읽습니다. 소재를 준비할 때 복잡한 원본 효과는 래스터화합니다. |
| 분류 | NFKC 정규화, 별칭, 좌우 및 숫자 접미사 해석. 알 수 없는 레이어는 수정할 수 있도록 남겨 둡니다. |
| 메시 | 알파 윤곽에서 적응형 삼각분할. 밀도, 충실도, 비용의 균형을 맞춥니다. |
| 텍스처 | 아틀라스 페이지, 가장자리 패딩, 선택적 고해상도화. 해상도가 바뀌어도 캔버스 좌표는 다시 정의되지 않습니다. |
| 리그 | 머리/몸/이목구비/머리카락을 생성한 뒤 파라미터, 구조, 형태, 경로 편집을 적용합니다. |
| 상호작용 | 메인 캔버스의 모드 메뉴가 오브젝트 / 변형 / 편집 / 시뮬레이션 / 스켈레톤 / 페인트 / 미리보기를 다룹니다. 페인트는 별도의 세션을 유지합니다. |
| 히스토리 | 이식 가능한 아카이브 안의 추가 전용 분기와 내용 주소 기반 에셋. |
| 내보내기 | CMO3 / MOC3로 변환하고 부속 파일을 조립합니다. 다시 읽기와 기하 검사로 진단을 생성합니다. |

## 불변 조건

- 이미지/캔버스 원점은 왼쪽 위이며, X는 오른쪽, Y는 아래쪽입니다. 부모 로컬 공간은 오브젝트 종류에 따라 다릅니다.
- 토폴로지가 바뀌면 UV, 폼, Glue 참조, 경로 바인딩을 갱신해야 합니다.
- 원본 픽셀과 재생 가능한 편집이 프로젝트 복구를 정의합니다. 렌더 스냅숏은 그 대체물이 아닙니다.
- 상대 리소스 참조는 납품 번들 안에서 해석되어야 합니다.
- 파싱, 그대로 통과시키는 보존, 노출된 편집, 시각적 동등성은 각각 별도의 근거가 필요합니다.

[Pipeline](../../../src/main/kotlin/io/github/psd2live/core/PSD2LivePipeline.kt) · [RigBuilder](../../../src/main/kotlin/io/github/psd2live/core/RigBuilder.kt) · [CanvasEditor](../../../src/main/kotlin/io/github/psd2live/ui/CanvasEditor.kt) · [런타임 / 내보내기](RUNTIME_EXPORT_ARCHITECTURE_AND_GAPS.md)
