# `.p2lrt` 2.0 형식

[문서 목록](../../README.md) · [中文](../../zh/spec/P2LRT_V2.md) · [English](../../en/spec/P2LRT_V2.md) · [런타임](RUNTIME.md) · [프로젝트, 런타임, 내보내기 경계](RUNTIME_EXPORT_ARCHITECTURE_AND_GAPS.md)

상태: 컨테이너, 핵심 청크, `PGUI`, `META`, 확장 청크 `BONE`, `SKIN`, `COLL`, `SIMS`와 보조 청크 `EXPR`, `HITA`, `UDAT`, `POSE`가 구현되어 있으며, 내보내기는 기본으로 버전 2를 씁니다. 리더는 모든 압축 방식, 배열 코덱, 텍스처 페이지 종류를 지원하고, 라이터는 deflate와 zstd 압축, f32 배열, 내장 PNG 텍스처 페이지를 쓸 수 있습니다.

## 목표

- **진화 규칙을 한 번에 확정**: 2.0 이후 새 기능은 청크를 추가하기만 하며, 기존 레코드 레이아웃은 바뀌지 않고 메이저 버전도 다시 올리지 않습니다.
- **Cubism과 동등한 핵심 계층**: 핵심 청크만 읽어도 완전히 재생할 수 있으며, 결과는 v1과 비트 단위로 같고 의미는 moc3와 일대일로 대응합니다.
- **선택적 확장 계층**: 런타임 스키닝, 정점 시뮬레이션, 충돌 같은 고급 데이터는 선택적 청크에 들어가며, 이를 모르는 런타임은 Cubism 결과로 돌아갑니다(한 파일에 두 모드).
- **불러오기 쉬움**: 임의 접근, 필요할 때 불러오기, 제로 카피 배열을 지원하며, 파일 하나로 기존 사이드카의 내용을 담을 수 있습니다.
- **재현 가능**: 같은 IR은 항상 같은 바이트를 쓰며, 타임스탬프 같은 가변 데이터가 없습니다.

목표가 아닌 것: 범용 장면 형식, 편집 가능한 프로젝트 형식(프로젝트는 계속 `.psd2live`), 암호화나 난독화.

## 컨테이너

리틀 엔디언입니다. 파일은 헤더, 청크 표(TOC), 청크 데이터로 이루어집니다.

```
Header  (32 bytes)
  u8[8]  magic        "P2LRT\0\0\0"
  u16    major        = 2
  u16    minor        highest minor version the writer knows; informational only
  u32    flags        bit0 names stripped; the rest reserved, written as 0
  u32    chunk_count
  u32    toc_offset   = 32
  u32    reserved[2]  written as 0
TOC     chunk_count × 40 bytes
  u8[4]  tag          four ASCII characters; upper case for standard chunks, lower case for vendor/private chunks
  u16    version      chunk version (layout and evaluation semantics)
  u16    flags        bit0 required; bit1 has CRC; bit2–3 compression method (see below); the rest written as 0
  u64    offset       offset of the chunk data from the file header, 16-byte aligned
  u64    length       bytes the chunk occupies in the file (without padding; the compressed length when compressed)
  u64    raw_length   bytes after decompression; equal to length when uncompressed
  u32    crc32        IEEE CRC32 over the stored (compressed) bytes (0 when bit1 is 0)
  u32    reserved     written as 0
Chunks  chunk data, padded with 0 between chunks to 16-byte alignment
```

- 헤더 `magic`의 처음 5바이트는 v1과 같습니다. v1에서는 `magic` 뒤에 `u32 version = 1`이 오고, v2에서는 같은 위치에 `u16 major = 2, u16 minor`가 옵니다. 리더는 u32를 읽어 값이 1이면 v1로, 하위 16비트가 2이면 v2로 파싱하고, 그 밖의 값은 거부합니다.
- 표준 태그는 파일마다 최대 한 번 나타납니다. 청크는 파일 안에서 어떤 순서로 나타나도 되며, 라이터는 이 페이지의 「핵심 청크」 표 순서대로 씁니다.
- 청크는 서로 겹치면 안 되고 모두 파일 안에 있어야 합니다. 청크 뒤, 파일 끝 너머에는 추가 데이터가 허용되지 않습니다(패딩 제외).
- 오프셋과 길이는 u64입니다. 주소 폭이 더 작은 리더(WASM32 등)는 범위를 넘는 파일을 잘라 읽지 않고 거부합니다.

### 청크 압축

| flags bit2–3 | 방식 | 저장 내용 |
| --- | --- | --- |
| 0 | 없음 | 원본 청크 데이터 |
| 1 | deflate | zlib 스트림(RFC 1950) |
| 2 | zstd | 단일 zstd 프레임(RFC 8878), 외부 사전 없음 |
| 3 | 예약 | 리더가 거부 |

- 리더는 deflate와 zstd를 지원해야 합니다. 압축을 푼 길이는 `raw_length`와 같아야 하며, 다르면 파일을 거부합니다. 압축을 푼 결과는 16바이트 정렬 버퍼에 넣은 뒤 압축되지 않은 청크로 파싱합니다.
- 압축은 저장 방식만 바꾸며 청크 내용은 바꾸지 않습니다. 청크 안의 오프셋과 배열 정렬은 압축을 푼 데이터를 기준으로 합니다.
- 압축된 청크는 제로 카피가 불가능합니다. 메모리 매핑이나 스트리밍 불러오기가 필요한 호스트는 압축되지 않은 파일을 선택해야 합니다.
- 라이터는 기본적으로 압축하지 않습니다. 내보내기 옵션에서 압축을 켜면 크기가 줄어드는 청크만 압축하며, 이미 압축된 텍스처 페이로드(PNG, KTX2)를 담은 `TEXR`은 압축하지 않습니다. 압축한 데이터가 원본보다 작지 않으면 그 청크는 압축하지 않고 씁니다.
- 바이트 안정성은 같은 라이터 버전과 같은 압축 설정에서만 요구됩니다. 일치성 비교는 압축을 푼 청크 내용으로 합니다.

### 호환성 규칙

| 경우 | 리더 동작 |
| --- | --- |
| `major`가 1이나 2가 아님 | 거부 |
| 알려진 태그, `version` ≤ 지원 버전 | 해당 버전으로 파싱. 정확히 `length` 바이트를 읽어야 함 |
| 알려진 태그, 더 높은 `version` | 알 수 없는 청크로 취급 |
| 알 수 없는 청크, 필수 아님 | 건너뜀 |
| 알 수 없는 청크, 필수 | 거부. 오류에 태그를 명시("requires feature XXXX") |
| 필수 핵심 청크가 없음 | 거부 |
| CRC 불일치(호스트가 검증을 요청할 때만 확인) | 거부 |
| 압축 방식이 예약 값이거나, 압축 해제에 실패하거나, 압축을 푼 길이가 `raw_length`와 다름 | 거부 |

`minor`는 이 판단에 참여하지 않으며 진단에만 쓰입니다.

### 진화 규칙

1. 같은 메이저 버전 안에서 공개된 청크 버전의 레이아웃과 평가 의미는 절대 바뀌지 않습니다.
2. 기존 객체에 속성을 추가하려면 원래 레코드를 바꾸지 않고, 객체 인덱스별로 그 열을 저장하는 「병렬 청크」를 추가합니다. 예를 들어 메시의 스키닝은 `SKIN` 청크(메시 인덱스 → 스키닝 데이터)로 쓰고 `MESH`는 그대로 둡니다.
3. 청크에는 「어떤 플래그가 1이면 몇 바이트를 더 읽는」 형태의 조건부 필드가 있으면 안 됩니다. 선택적 내용은 항상 별도 청크에 넣거나 명시적인 길이로 감쌉니다.
4. 청크의 평가 의미를 바꾸려면 그 청크의 버전을 올려야 하며, 이전 버전은 여전히 이전 의미로 평가합니다.
5. 핵심 청크는 확장 청크에 의존하지 않습니다. 확장 청크는 핵심 청크와 먼저 정의된 확장 청크만 참조할 수 있습니다.
6. 벤더용이나 실험적 청크는 소문자 태그를 쓰고 필수가 아니어야 합니다. 표준 청크는 대문자 태그를 씁니다.

## 기본 인코딩

| 이름 | 인코딩 |
| --- | --- |
| `u8/u16/u32/i32/f32` | 리틀 엔디언. f32는 유한값이어야 함 |
| `ref` | u32 인덱스. null이 가능한 참조는 i32를 쓰고 null은 -1 |
| `sref` | u32 문자열 표 인덱스(`STRS`), null은 `0xFFFFFFFF` |
| `rgb` | 3 × f32 |
| `str` | `STRS` 안에서만 사용. 다른 곳의 문자열은 항상 `sref` |
| `array<T>` | 먼저 청크 안에서 4바이트 정렬까지 0으로 채운 뒤 `u8 codec, u8 reserved[3], u32 count`를 쓰고, 이어서 `count`개의 요소 |
| `grid<F>` | v1과 같음: `u16 axes`(`0xFFFF`는 격자 없음) × `{ ref parameter, array<f32> keys }`, `u32 cells` × `{ u16 coordinate[axes], F }` |
| `channels` | v1과 같음: `u8 n` × `{ u8 channel, u8 kind, grid<value> }`. 채널은 순서대로 DrawOrder, Opacity, MultiplyColor, ScreenColor, FlipX, FlipY, GlueIntensity. kind 0은 스칼라, 1은 색, 2는 플래그 |
| `bindings<S>` | v1과 같음: `u16 n` × `{ ref parameter, array<f32> keys, u32 neutral, u8 present + S per key, u16 n × { ref limit_parameter, u16 n × { f32 value, f32 weight } } }` |

배열 코덱(`codec`):

| 값 | 요소 | 용도 |
| --- | --- | --- |
| 0 | f32 | 기본값. 모든 일치성 테스트에서 사용 |
| 1 | f16 | 선택적 크기 최적화(위치, 오프셋) |
| 2 | unorm16 | 선택적 크기 최적화(UV, 범위 [0,1]) |
| 16 | u16 | 정점 수가 65535 이하일 때의 인덱스 |
| 17 | u32 | 인덱스 |
| 32 | u8 | 바이트 문자열(텍스처 페이로드) |

부동소수점 배열의 코덱 1과 2는 호스트가 「배포용 크기 최적화」를 명시적으로 선택할 때만 씁니다. 리더는 모든 코덱을 지원해야 하며 디코딩한 값을 f32와 똑같이 다룹니다. 배열은 스스로 정렬됩니다. 헤더가 청크 안에서 4바이트 정렬 위치에서 시작하고 길이가 8바이트이므로 요소도 4바이트 정렬되며, 청크 자체가 16바이트 정렬되므로 코덱 0/16/17 배열은 슬라이스로 바로 읽을 수 있습니다. 배열 앞의 패딩을 제외하면 레코드에는 다른 패딩이 없으며, 스칼라는 정렬되지 않을 수 있습니다.

## 핵심 청크

모두 필수 청크(flag bit0)입니다. `PHYS`를 제외한 모든 청크 버전은 1이며, 평가 의미는 [런타임](RUNTIME.md)의 「평가 규칙」, 「물리」, 「모션 클립」 절을 따릅니다. 필드 순서와 개수 폭은 v1과 같습니다. 차이는 문자열이 `sref`가 되고, 부동소수점 배열, 인덱스, 바이트 문자열이 `array<T>`가 되며, 텍스처 페이지에 종류가 추가된다는 점뿐입니다.

| 태그 | 내용 | 필수 존재 |
| --- | --- | --- |
| `STRS` | 문자열 표 | 예 |
| `CANV` | 캔버스: f32 width, height, originX, originY, pixelsPerUnit(-1은 없음) | 예 |
| `PARM` | 파라미터 | 예(0개 가능) |
| `DEFM` | 디포머, 부모가 먼저 | 예(0개 가능) |
| `PART` | 파츠 | 예 |
| `MESH` | 메시 | 예 |
| `GLUE` | Glue | 아니요(없으면 0개) |
| `DRAW` | 그리기 트리 | 예 |
| `TEXR` | 텍스처 페이지 | 아니요(없으면 텍스처 없음) |
| `PHYS` | 물리 그룹 | 아니요 |
| `CLIP` | 모션 클립 | 아니요 |
| `ROLE` | 파라미터 역할 | 아니요 |

모션 클립 이벤트, 파라미터가 아닌 커브, 커브 페이드는 필수가 아닌 보조 청크 `CEXT`에 있습니다(아래 「보조 청크」 참고).

「필수 존재」가 아니요인 청크는 없으면 빈 집합으로 취급합니다. 있을 때는 핵심 평가에 참여하므로 여전히 필수로 표시됩니다.

### `STRS`

```
u32 count
u32 offsets[count + 1]   start and end offset of each string in the blob
u8  blob[...]            UTF-8, not 0-terminated
```

라이터는 중복을 제거합니다. 헤더 flags bit0(이름 제거)이 1이면 모든 `name` 필드가 null `sref`가 되고 `id`는 유지되며, 호스트는 id로 파라미터와 메시를 찾습니다.

### `PARM`

`u32 n` × `{ sref id, sref name, f32 min, max, default, u8 flags (1 blend, 2 repeat) }`

### `DEFM`

`u32 n` × 디포머. 부모는 자식보다 앞에 와야 합니다.

```
u8 kind (0 Warp, 1 Rotation), sref id, i32 parent, i32 part,
u8 flags (1 visible, 2 enabled, 4 flipX, 8 flipY, 16 bilinear),
f32 opacity, rgb multiply, rgb screen
Warp:     u32 columns, rows, grid<array<f32> points>, channels,
          bindings<{ array<f32> points, f32 opacity, rgb multiply, rgb screen }>
Rotation: f32 baseAngle, grid<{ f32 x, y, angle, scale }>, channels,
          bindings<{ f32 x, y, angle, scale, u8 flipX, u8 flipY, f32 opacity, rgb multiply, rgb screen }>
```

### `PART`

```
u32 n × { sref id, sref name, u8 flags (1 visible, 2 sketch), u8 groupMode, i32 drawOrder,
          u32 n × { u8 kind (0 part, 1 mesh), ref target },
          channels, composite,
          bindings<{ f32 drawOrder, f32 opacity, rgb multiply, rgb screen }> }
composite = { u8 blend, u8 alphaBlend, u32 n × ref maskMesh, u32 n × ref maskPart,
              u8 invertMask, f32 opacity, rgb multiply, rgb screen }
```

### `MESH`

```
u32 n × { sref id, sref name, i32 parent, u8 blend, u8 alphaBlend, u32 n × ref maskMesh,
          u8 flags (1 invertMask, 2 culling, 4 visible, 8 geometry), i32 texturePage,
          [geometry] array<f32> positions, array<f32> uvs, array<u16|u32> indices,
          grid<array<f32> deltas>, channels,
          f32 drawOrder, f32 opacity, rgb multiply, rgb screen,
          bindings<{ array<f32> deltas, f32 drawOrder, f32 opacity, rgb multiply, rgb screen }> }
```

`[geometry]`는 flags bit3이 1일 때만 있습니다. 마찬가지로 `CLIP`의 베지어 구간 제어점은 구간 유형이 1일 때만 있습니다. 이 둘은 v1에서 물려받은 조건부 필드로, v1과 일대일로 대응하기 위해 유지되며 규칙 3의 유일한 예외입니다.

### `GLUE`

`u32 n × { sref id, ref meshA, ref meshB, f32 intensity, u32 n × { u32 a, u32 b, f32 weightA, f32 weightB }, channels }`

### `DRAW`

그리기 트리의 루트 그룹이며 재귀적입니다: `{ i32 part, i32 drawOrder, channels, u8 hasComposite, [composite], u32 n × { u8 kind (0 group, 1 mesh), group | ref mesh } }`

### `TEXR`

```
u32 n × { u8 kind, u32 width, height, payload }
kind 0  PNG            payload = array<u8> png      all runtimes must support it
kind 1  KTX2           payload = array<u8> ktx2     optional; runtimes without support report the page as unavailable
kind 2  external file  payload = sref uri           path relative to the model file, loaded by the host
```

`p2l_texture_png`은 PNG가 아닌 페이지에 대해 빈 값을 반환합니다. 새로 추가된 `p2l_texture_info(i)`는 종류, 너비와 높이, uri를 반환합니다.

### `PHYS`

`f32 fps (0 runtime default), u32 n × { sref id, sref name, inputs, outputs, segments, normalization }`, 각 필드는 v1과 같습니다.

입력과 출력의 `u8 source`는 0이 수평(X), 1이 각도, 2가 수직(Y, Cubism physics3의 `Y`)입니다. 청크 버전 2에서 수직을 허용합니다. 라이터는 수직을 쓸 때만 버전 2를 쓰므로, 이전 런타임은 잘못 읽는 대신 "requires PHYS version 2"를 보고합니다. 버전 1의 파일 레이아웃은 완전히 같습니다. 수직 입력은 정규화된 값을 루트의 y 이동에 더하고, 수직 출력은 해당 정점의 이전 정점에 대한 y 오프셋을 읽습니다. 수평과 마찬가지로 이동 출력의 스케일은 0입니다(Cubism의 동작). 버전 1 파일(내보내기 옵션 `v1`)은 수직 입력과 출력을 쓰지 않으며 이를 손실 보고서에 기록합니다.

### `CLIP`

v1과 같으며 문자열만 `sref`로 바뀝니다.

### `ROLE`

`u32 n × { sref role, u32 n × ref parameter }`

## 파라미터 UI 청크 `PGUI`(필수 아님)

파라미터 패널을 표시하는 호스트를 위한 메타데이터입니다. 평가에는 참여하지 않으며, 이를 모르거나 필요 없는 런타임은 건너뜁니다. 청크 버전 1:

```
u32 n × { ref parameter, array<f32> keys }        snap values: only parameters with snap values, keys ascending
u32 n × { ref horizontal, ref vertical }          2D sticks: two parameters forming one planar control
u32 n × node                                      parameter group tree, expanded in pre-order
node = { u8 kind (0 parameter, 1 group), u8 open, u8 label (0 none, 1 preset, 2 custom), u8 reserved,
         i32 parameter (-1 for groups), sref id, sref name, sref preset, u32 argb, u32 children }
```

- 노드는 고정 길이 레코드이며 모든 필드를 항상 씁니다. 파라미터 노드는 `id`, `name`, `preset`이 null이고, 그룹 노드는 `parameter`가 -1입니다. `label`이 1이 아니면 `preset`은 null이고, 2가 아니면 `argb`는 0입니다. `children`은 바로 뒤따르는 직계 자식 노드 수입니다.
- 헤더 flags bit0(이름 제거)이 1이면 그룹 `name`은 null입니다.
- 그룹 트리에 나열되지 않은 파라미터는 호스트가 `PARM` 순서대로 맨 끝에 둡니다.
- `RigIR.restPose`는 기본 메시를 캔버스 공간에 저장하는 형식(Spine 등)에서만 쓰입니다. p2lrt는 파라미터에서 바로 평가하므로 이를 쓰지 않습니다.

## 정보 청크 `META`(필수 아님)

`u32 n × { sref key, sref value }`. 표준 키: `generator`, `generator_version`, `source_hash`(프로젝트 소스 데이터의 해시, 캐시와 진단용), `capabilities`(파일에 있는 확장 태그를 공백으로 구분한 목록, 호스트가 불러오기 전에 표시하기 위함). 파일을 재현 가능하게 유지하기 위해 타임스탬프는 쓰지 않습니다.

## 평가 단계와 확장 훅

핵심 평가는 아래의 고정 단계로 이루어집니다. 확장 청크는 표시된 훅에서만 작용할 수 있으며, 청크 명세에는 어느 훅에 붙는지와 어떤 기여를 대체하는지를 명시해야 합니다.

| 번호 | 단계 | 핵심 내용 | 확장 훅 |
| --- | --- | --- | --- |
| 1 | 파라미터 | 모션 클립 → 절차적 동작 → Cubism 진자 → clamp / wrap | **H1 파라미터 재정의**: 대체된 파라미터는 이번 평가에서 기본값을 취하고, 대체된 진자 그룹은 건너뜀 |
| 2 | 디포머 | 부모 먼저, Warp / 회전 프레임 | **H2 프레임 대체**: 지정한 디포머의 프레임을 정확히 합성한 프레임(접힌 링크 등)으로 대체 |
| 3 | 뼈 프레임 | 없음(핵심 계층에는 뼈가 없음) | **H3 뼈 프레임**: 디포머 프레임이나 가상 프레임에서 각 뼈의 캔버스 변환을 유도해 H4, 콜라이더, 호스트 부착에 사용 |
| 4 | 메시 | 정지 위치 + 키폼 오프셋 + 블렌드 셰이프 → 부모 변환 | **H4 메시 기여 대체**: 지정한 메시의 지정한 기여(스키닝 축의 키폼, 지정한 블렌드 셰이프)를 확장의 결과로 대체하고, 나머지는 평소대로 진행 |
| 5 | Glue | 정점 쌍이 서로 당김 | 없음 |
| 6 | 시간 단계 후처리 | 없음 | **H6 정점 시뮬레이션**: dt가 필요함. 이번 평가의 정점을 목표로 고정 단계 시뮬레이션을 실행하고 대상 메시의 정점을 덮어씀. 충돌은 이 단계 안에서 해결 |
| 7 | 그리기 순서 | 그리기 트리 정렬 | 없음 |

모든 훅에 공통인 규칙:

- 확장을 하나도 켜지 않으면 모든 훅은 아무 일도 하지 않으며, 코드 경로는 핵심 평가와 같습니다.
- H1 재정의는 평가 전체에 적용됩니다. 라이터는 대체된 기하만 구동하는 파라미터(`ParamSim*` 등)만 나열할 수 있으므로 채널은 영향을 받지 않습니다.
- `p2l_evaluate`(dt 없음)는 H6을 진행하지 않습니다. 시뮬레이션 대상 메시는 이전 시뮬레이션 상태를 보이고, 한 번도 진행하지 않았으면 목표를 보입니다.

### 확장 청크 공통 헤더

각 확장 청크의 페이로드는 통일된 재정의 선언으로 시작하므로, 리더는 청크 내용을 이해하지 않고도 무엇을 대체하는지 알 수 있습니다(진단과 `META` 표시용).

```
u16 feature_bit           feature bit for p2l_set_advanced
u16 hooks                 bit set: H1=1, H2=2, H3=4, H4=8, H6=32
u32 n × ref parameter     parameters H1 overrides to their default
u32 n × ref physicsGroup  pendulum groups H1 skips
u32 n × ref deformer      deformers whose frame H2 replaces
u32 n × { ref mesh, u32 n × ref axisParameter, u32 n × u32 binding }
                          contributions H4 replaces: these keyform axes take their default, these blend shapes are not counted
u32 n × u8[4] depends_on  tags of other extension chunks depended on
```

이어서 청크 고유의 내용이 옵니다. 의존하는 청크가 없거나 켜져 있지 않으면 확장은 자동으로 꺼집니다.

### 기능 비트와 모드 전환

| 비트 | 청크 | 설명 |
| --- | --- | --- |
| 1 | `SKIN`(가상 뼈를 참조하면 `BONE` 필요) | 런타임 스키닝 |
| 2 | `BONE`의 호 | 정확한 링크(피벗을 호를 따라 보간) |
| 4 | `SIMS` | 실시간 정점 단위 시뮬레이션 |
| 8 | `COLL`(`SIMS` 필요. 시뮬레이션과 함께 켜짐) | 충돌 |

- `p2l_advanced_available()`은 파일에 들어 있고 런타임이 지원하는 비트를 반환합니다. `p2l_set_advanced(bits)`는 실제로 켜진 비트를 반환하며, 0은 Cubism 모드(기본값)입니다. 전환하면 시뮬레이션 상태가 초기화됩니다.
- `p2l_bone_count / p2l_bone_id / p2l_bone_transform`은 H3 뼈 프레임을 출력합니다.
- 시간 단계 확장은 `PHYS`와 마찬가지로 고정 단계(단계 크기는 청크에 저장)를 사용하고, 고정된 순회 순서로 단일 스레드에서 실행하며, 업데이트당 단계 수에 상한을 두어 초과 시간은 버리고, 출력은 마지막 두 단계 사이를 보간해야 합니다.

## 확장 청크 `BONE`(버전 1)

```
common header               feature 2, hooks H2 | H3, deformers = deformers with arcs
u32 n × deformer            virtual bones: rotation deformer records in DEFM format, the i-th has index n_DEFM + i;
                            the parent is a deformer in DEFM or an earlier virtual bone
u32 n × { ref deformer, sref id }            bones hosts can attach to (DEFM or virtual bones)
u32 n × { ref deformer, ref parameter, f32 cx, cy }   arcs
```

- **가상 뼈**: 뼈대를 베이크할 때 직접 붙은 메시가 없었거나, 가지치기되거나 접힌 뼈입니다. 런타임 스키닝에서만 평가되며 핵심 평가에는 참여하지 않습니다. 뼈 파라미터와 각도의 관계는 베이크 전의 회전 디포머와 같습니다. 각도 = 파라미터 값 × 방향이며, 피벗은 뼈의 위치에 있습니다.
- **호**: 이 회전 디포머의 피벗은 `parameter` 축에만 키가 있고, 키들의 위치는 (cx, cy)를 중심으로 하는 같은 원 위에 있습니다(부모 공간). 정확한 링크를 켜면 인접한 두 키 사이의 피벗 위치를 현이 아니라 각도와 반지름으로 보간합니다. 라이터는 키가 4개 이상이고 모두 한 원 위에 있으며, 회전 방향이 일정하고 한 단계에 반 바퀴 미만으로 움직이는 피벗에만 호를 씁니다.
- `p2l_bone_count / p2l_bone_id / p2l_bone_transform`은 부착할 수 있는 뼈를 나열합니다. 가상 뼈의 변환은 현재 파라미터로부터 즉석에서 계산합니다.

## 확장 청크 `SKIN`(버전 1)

```
common header               feature 1, hooks H4, replaces no contribution
u32 n × {
  ref mesh                  its parent is a rotation deformer that is among the bones
  u32 n × { ref parameter, array<f32> keys }   skinning axes: parameters sampled when baking, and their keys
  u32 n × ref deformer      bones: rotation deformers in DEFM or the virtual bones
  array<u16|u32> from, array<u16|u32> to, array<f32> weight   two-bone weights per vertex; fitted by the runtime when length is 0
}
```

평가(H4, 메시의 부모 공간에서, 부모 변환 전):

1. 정점을 싣는 각 뼈 b의 정지 포즈 대비 변환: A_b = H⁻¹ · B · B_rest⁻¹ · H_rest. 여기서 H는 메시 부모(홈)의 프레임, B는 뼈의 프레임이며, 둘 다 현재 파라미터에서의 캔버스 변환입니다.
2. 두 뼈 선형 블렌딩: LBS(v) = (1 − w) · A_from(v) · p + w · A_to(v) · p. 여기서 p는 정지 위치입니다.
3. 메시에 LBS(current) − Σ_c w_c · LBS(c)를 더합니다. c는 현재 값 양쪽에 있는 스키닝 축 키들의 조합을 순회하고, w_c는 베이크할 때와 같은 다중 선형 가중치이며, 다른 파라미터는 현재 값을 유지합니다.

이 보정은 모든 키 조합에서 정확히 0이므로, 고급 모드는 베이크된 모든 키에서 Cubism 모드와 일치합니다. 키 사이에서는 베이크된 현이 뼈의 호로 대체됩니다. 가중치가 주어지지 않으면 런타임이 켜질 때 가중치를 맞춥니다. 모든 키 조합(최대 1024개, 넘으면 축별로)에 대해 정점마다 베이크된 위치를 가장 잘 재현하는 뼈 쌍과 가중치를 고르며, 동점이면 단일 뼈를 강체로 따르는 쪽을 우선합니다.

라이터는 부모가 회전 디포머이고 그 아래에(회전 디포머로 연결되며 가상 뼈를 포함할 수 있음) 메시의 축에 키가 있는 회전 디포머가 있는 메시에 대해 `SKIN`을 쓰며, 뼈는 최대 16개, 축은 최대 8개입니다. 스탠스 Warp에 붙은 다리 메시는 베이크된 상태로 남습니다.

## 확장 청크 `COLL`(버전 1)

```
common header               feature 8, hooks H6
u32 n × { sref id, u8 capsule, i32 deformer, i32 mesh, u32 vertexA, u32 vertexB,
          f32 ax, ay, bx, by, radiusA, radiusB, friction }
```

콜라이더는 원(capsule 0, 끝점 a만 사용)이거나, a에서 b까지 반지름이 radiusA에서 radiusB로 줄어드는 캡슐입니다. 반지름은 캔버스 픽셀 단위입니다. 위치는 다음 중 한 가지 방식으로 정해집니다.

- **메시 정점 추종**: mesh가 -1이 아니면 두 끝점은 이번 평가에서 그 메시의 vertexA / vertexB 위치입니다. 편집기가 내보내는 콜라이더는 모두 이 종류이므로 편집기와 런타임이 같은 위치를 봅니다.
- **디포머 추종**: deformer가 -1이 아니면(가상 뼈일 수 있음) (ax, ay)와 (bx, by)는 그 디포머의 로컬 공간에 있는 점입니다.
- **고정**: 둘 다 -1이면 두 끝점은 캔버스 위의 고정점입니다.

friction은 0–1로, 접촉 시 상쇄되는 미끄러짐의 비율입니다. 충돌은 시뮬레이션에서만 작용합니다. `SIMS`의 각 시뮬레이션이 피할 콜라이더를 나열합니다.

## 확장 청크 `SIMS`(버전 1)

```
common header               feature 4, hooks H1 | H4 | H6; parameters and physics groups are the union of each simulation's replacements
u32 n × {
  sref id, f32 fps, u32 substeps, f32 gravityX, gravityY, windX, windY, pinCompliance
  u32 n × { ref mesh, u32 vertexCount }                 target meshes; particles are ordered by this
  array<f32> invMass, damping, windFactor, pinWeight, goalCompliance, goalOffsetX, goalOffsetY
  array<u32> anchorMesh (0xFFFFFFFF follows itself), array<u32> anchorVertex
  array<u32> stretchA, stretchB; array<f32> rest, compliance, compressionCompliance
  array<u32> triA, triB, triC; array<f32> areaCompliance
  array<u32> bendT1, bendT2; array<f32> bendCompliance
  array<u32> weldA, weldB; array<f32> weightA, weightB, weldCompliance
  array<u32> longParticle, longRoot; array<f32> maxDistance
  u32 n × ref parameter                                 baked-mode parameters this simulation replaces
  u32 n × u32 physicsGroup                              pendulum groups this simulation replaces (PHYS indices)
  u32 n × { ref parameter, array<f32> keys, u32 n × { ref mesh, keys × array<f32> offsets } }   static corrections added by baking
  u32 n × u32 collider                                  COLL indices
}
```

장면은 편집기의 XPBD 솔버(`XpbdSolver`)와 항목별로 대응합니다. 월드 공간은 캔버스 x와 부호를 바꾼 캔버스 y(y 위쪽)입니다. 음수 compliance는 제약이 없음을 뜻합니다(편집기에서는 무한대).

시뮬레이션을 켜면:

- **H1**: 대체된 모드 파라미터는 기본값에 머물고, 해당 진자 그룹은 건너뜁니다.
- **H4**: 대상 메시에서 정적 보정을 뺍니다(베이크가 키폼에 쓸 때와 같은 방식으로 키 사이를 선형 보간). 그러면 평가된 대상 메시는 베이크되지 않은 리그가 되어 각 입자의 목표가 됩니다. 고정된 입자는 자기 위치나 따르는 메시 정점의 위치를 따릅니다.
- **H6**: `p2l_update`는 장면 고유의 fps로 고정 단계씩 진행합니다. 업데이트마다 최대 4단계를 진행하고 초과 시간은 버리며, 그리기는 마지막 두 단계 사이를 보간합니다. `p2l_evaluate`는 이전 단계를 그리기만 합니다. 첫 업데이트에서 입자는 그 순간의 포즈로 정지합니다. `p2l_sim_reset`은 다음 업데이트가 처음부터 시작하게 하고, `p2l_sim_wind`는 호스트의 바람을 더합니다.

내보낼 때(런타임과 웹 대상에 한해) 편집기는 베이크된 각 시뮬레이션을 최종 리그 위에서 다시 만들고 보정합니다. 기본 포즈에서는 모드 파라미터가 0이고 정적 보정이 정지 키에 있으므로 장면은 베이크할 때와 같습니다.

## 보조 청크(필수 아님, 버전 1, 공통 헤더 없음)

### `EXPR`

```
u32 n × { sref id, sref name, f32 fadeIn, f32 fadeOut, u32 n × { ref parameter, u8 blend (0 Add, 1 Multiply, 2 Overwrite), f32 value } }
```

표정은 Cubism exp3와 일치합니다. 가중치 w는 사인 이징을 적용한 페이드 계수이며, 각 블렌드는 다음과 같이 계산합니다.

- Add: value + v·w
- Multiply: value · (1 + (v − 1)·w)
- Overwrite: value · (1 − w) + v·w

런타임은 한 번에 한 표정만 재생하며, 전환하면 이전 표정이 페이드 아웃됩니다. 업데이트 순서는 모션 클립 → 표정 → 절차적 동작 → 물리입니다. 내보낼 때 편집기는 리그에 있는 파라미터로 웃음, 슬픔, 화남, 놀람의 네 가지 표정을 만들고, 리그에 해당 모듈의 파라미터가 있으면 수줍음, 졸림, 우울함도 만들며, 각각 페이드 인과 페이드 아웃은 0.5초입니다.

### `HITA`

```
u32 n × { sref id, sref name, u32 n × ref mesh }
```

히트 영역은 그 메시들이 그리는 영역을 덮습니다. `p2l_hit_test(x, y)`는 파일 순서에서 그 캔버스 점을 포함하는 보이는 메시가 있는 첫 영역을 반환합니다. 내보내기는 문서에서 설정한 히트 영역을 씁니다([내보내기 대상](EXPORT_TARGETS.md#히트-영역) 참고). 하나도 없으면 `HitAreaHead`(얼굴)와 `HitAreaBody`(상의, 없으면 목이나 하의)를 생성합니다.

### `UDAT`

```
u32 n × { u8 kind (0 = mesh), ref target, sref value }
```

메시의 사용자 데이터(가져온 CMO3에서 온 것)로, `p2l_mesh_user_data`로 읽습니다.

### `POSE`

```
f32 fadeIn, u32 n × { u32 n × { ref part, u32 n × ref linkedPart } }
```

파츠 배타 그룹은 Cubism pose3와 일치합니다. 각 그룹은 한 번에 한 파츠만 보여 줍니다. 전환하면 선택된 파츠는 fadeIn초에 걸쳐 페이드 인되고, 나머지는 Cubism 규칙(φ = 0.5, 뒷면 불투명도 임계값 0.15)에 따라 페이드 아웃됩니다. 연결된 파츠는 소속 파츠를 따릅니다.

평가 후의 한 단계로, 메시의 불투명도에 모든 조상 파츠의 포즈 불투명도를 곱합니다. 어떤 그룹에도 없는 파츠는 1이므로 `POSE`가 없는 파일은 평가 결과가 바뀌지 않습니다. 핵심 평가에서 파츠 불투명도는 여전히 메시 불투명도에 반영되지 않습니다. 처음에는 각 그룹이 첫 파츠를 보여 줍니다.

호스트는 `p2l_pose_show(group, entry)`로 전환합니다. 내보내기 설정 `pose_groups`는 그룹을 `part|part; part|part` 형식으로 지정하며, `part+linkedPart`는 연결을 나타냅니다.

### `CEXT`

```
u32 n × { ref clip,
          u32 n × { f32 time, sref value },                                    events, ascending by time
          u32 n × { u8 target, i32 part, f32 fadeIn, f32 fadeOut,
                    f32 startTime, f32 startValue, u32 n × segment },          non-parameter curves
          u32 n × { u32 curve, f32 fadeIn, f32 fadeOut } }                     fades of the clip's own curves
```

모션 클립의 확장으로, Cubism motion3에서 파라미터 커브 이외의 내용에 대응합니다. 필수가 아니며, 이를 모르는 런타임은 `CLIP`의 파라미터 커브만 재생합니다. 각 클립에는 레코드가 최대 하나입니다.

- **이벤트**(motion3 `UserData`): 클립 시간이 이벤트 시간을 지나면 `p2l_event_count` / `p2l_event`를 통해 호스트에 보고합니다. 반복 클립은 반복마다 한 번 발생하며, 탐색(`p2l_clip_seek`)으로 건너뛴 이벤트는 발생하지 않습니다.
- **파라미터가 아닌 커브**: `target` 1은 파츠 불투명도(`part`는 파츠 인덱스), 2는 모델 불투명도, 3은 눈 깜빡임 효과, 4는 립싱크 효과입니다(뒤의 셋은 `part`에 -1을 씀). 커브 구간은 `CLIP`과 같은 방식으로 인코딩합니다. 파츠 불투명도와 모델 불투명도는 커브 값으로 바로 설정되며(Cubism과 마찬가지로 페이드의 영향을 받지 않음), 파츠 아래의 메시와 모델 전체에 대한 배수로 작용합니다. 눈 깜빡임 효과는 `EyeBlink` 역할 파라미터에 곱하고, 립싱크 효과는 `LipSync` 역할 파라미터에 더합니다. 클립에 그 파라미터의 커브가 있으면 커브 값에 작용하고, 없으면 클립 가중치를 적용한 현재 값에 작용합니다.
- **페이드**: 음수 값은 클립의 설정을 사용한다는 뜻입니다. `curve`는 `CLIP`에 쓰인 그 커브의 인덱스입니다(파일에 있는 파라미터만 셈). 자체 페이드가 있는 커브는 Cubism 규칙에 따라 가중치를 얻습니다. 페이드 인 계수는 커브의 페이드 인(또는 클립의 것)을, 페이드 아웃 계수는 커브의 페이드 아웃(또는 클립의 것)을 쓰고, 둘을 곱합니다.

확장 청크에 공통인 제약:

- 핵심 평가의 특정 기여(교정 키폼이나 `ParamSim*`가 구동하는 셰이프 등)를 대체하거나 보충하기만 합니다. 파라미터, 모션, 동작, Cubism 진자는 평소대로 구동합니다.
- 기본적으로 꺼져 있으며, 호스트가 `p2l_set_advanced(features)`로 켭니다. 꺼져 있으면 평가 결과는 핵심 청크만 있는 파일과 비트 단위로 같습니다.
- 라이터는 대응하는 Cubism 베이크 결과를 항상 핵심 청크에 남깁니다.

## 런타임과 라이터

- **읽기**: v1과 v2를 모두 지원합니다. 둘 다 같은 메모리 구조 `Rig`로 파싱되며, 평가 코드는 하나뿐입니다.
- **쓰기**: `P2lrtTarget`은 기본으로 v2를 씁니다. 내보내기 옵션 `v1`은 핵심 내용만 v1 레이아웃으로 쓰며, 이때 `CapabilityScan`은 확장 데이터를 베이크된 형태로 유지했다고 보고합니다. `compress`는 크기가 줄어드는 청크를 deflate로 압축하고, `strip_names`는 이름을 제거합니다.
- **기능 질의**: 새로 추가된 `p2l_format_support()`는 지원하는 메이저 버전과 청크 태그 및 버전 목록을 반환합니다. `p2l_rig_load_ex(bytes, len, flags)`의 flags bit0은 CRC 검증을 요청합니다.
- **압축 의존성**: Rust 리더는 순수 Rust인 `miniz_oxide`(deflate)와 `ruzstd`(zstd 디코딩)를 사용하며, 이는 WASM으로 컴파일됩니다. 런타임에는 디코딩만 필요합니다. 그 결과 WASM 빌드는 208 KB에서 400 KB로 커지며, 그중 버전 2 읽기와 deflate가 약 79 KB, zstd가 약 113 KB를 차지합니다. Kotlin 라이터는 deflate에 JDK `Deflater`를, zstd에 순수 Java인 aircompressor를 사용합니다.
- **바이트 안정성**: 같은 IR을 두 번 쓰면 같은 바이트가 나옵니다. 이름 제거와 배열 코덱은 내보내기 옵션으로 정해지며 환경의 영향을 받지 않습니다.

## 검증

- **v1/v2 비트 단위 일치**: `RuntimeConformanceTool`의 모든 무작위 모델, 샘플, 물리 궤적을 v1과 v2로 각각 쓰고, 런타임 평가 결과를 `to_bits()`로 비교합니다. 정점, 불투명도, 색, 그리기 순서가 정확히 같아야 합니다.
- **리더**: v1 수락, 최소 v2 파일 수락, 알 수 없는 선택적 청크 건너뜀, 알 수 없는 필수 청크는 태그를 보고하며 거부, 범위를 벗어난 청크, 겹침, 초과 읽기, 미달 읽기, 중복 표준 태그, 필수 핵심 청크 누락, 잘못된 정렬, CRC 불일치(검증을 켰을 때)는 모두 거부. Rust(`runtime/src/tests.rs`)와 Kotlin(`P2lrtTest.kt`)에 각각 테스트 모음이 있습니다.
- **재현성**: 같은 IR을 같은 설정으로 두 번 쓰면 같은 바이트가 나옵니다. `STRS` 중복 제거 후에는 쓰는 순서와 관계없이 내용이 같습니다.
- **압축**: 같은 모델을 압축 없이, deflate로, zstd로 쓰면 압축을 푼 청크 내용이 바이트 단위로 같고 평가가 비트 단위로 같습니다. 일치하지 않는 `raw_length`, 잘린 압축 스트림, 예약된 압축 방식은 모두 거부됩니다. Kotlin 쓰기와 Rust 및 WASM 읽기를 모두 다룹니다.
- **`PGUI`**: 쓴 뒤 다시 읽은 그룹 트리, 2D 스틱, 스냅 값이 `RigIR`과 일치합니다. `PGUI`를 제거해도 평가는 바뀌지 않습니다.
- **배열 코덱**: f16 / unorm16 모델의 f32 모델 대비 정점 오차는 코덱의 정밀도 이내이며, 따로 보고하고 비트 단위 일치 코퍼스에는 포함하지 않습니다.
- **언어 간**: Kotlin이 쓰고 Rust가 읽습니다. WASM 빌드도 같은 리더 테스트를 통과합니다.
- **확장이 있어도 Cubism 모드는 그대로**: 확장 청크가 있고 기능 비트가 0인 파일은 확장 청크를 제거한 같은 파일과 비트 단위로 같게 평가됩니다. 켰다가 다시 꺼도 마찬가지입니다(남는 상태 없음).
- **확장의 참조 구현**: 모든 확장 청크에는 Kotlin 참조 구현이 있으며, `RuntimeConformanceTool`의 무작위 코퍼스에 포함되어 런타임과 비교됩니다.

## 고급 모드 로드맵과의 관계

이 형식은 런타임 고급 모드(런타임 스키닝, 정확한 링크, 정점 시뮬레이션, 충돌, 한 파일에 두 모드)를 위한 컨테이너 기반으로, 그 로드맵의 「0단계 형식 파이프라인」에 해당하며, 「v1 본문 뒤에 청크를 덧붙인다」는 로드맵의 원래 구상을 대체합니다. 핵심 내용도 청크로 다시 나누었으며, 모든 내용이 같은 진화 규칙을 따릅니다. 이후 단계에서 각각 `BONE`/`SKIN`, `SIMS`, `COLL`의 청크 내부 레이아웃을 정의하며, 훅, 공통 헤더, 기능 비트는 이 페이지를 따릅니다.

로드맵에서 측정으로 결정하기로 남겨 둔 항목은 다음과 같이 결정되었습니다.

- **런타임 ARAP**: 구현하지 않습니다. 스키닝 보정은 모든 키에서 0이므로 고급 모드는 베이크된 모든 키에서 정확히 일치하며(모든 일치성 샘플이 0), 키 사이에서는 이미 뼈의 호를 따릅니다.
- **스탠스 Warp에 붙은 메시(다리)**: 베이크된 상태로 남습니다. 다리는 스탠스 Warp 안에서 굽고, 뼈대는 다리 뼈에 회전 프레임을 만들지 않으므로 스키닝이 가져올 프레임이 없습니다.
- **네 뼈 가중치**: 필요 없습니다. 편집기의 스키닝은 정점마다 최대 두 뼈에 걸칩니다.

## 확정된 결정

- 청크 압축: deflate와 zstd를 정의하며 리더는 둘 다 지원해야 합니다. 라이터는 기본적으로 압축하지 않고, 내보내기 설정 `compress`는 deflate를, 여기에 `zstd`를 더하면 zstd를 선택합니다.
- 오프셋과 길이 폭: u64.
- 파라미터 그룹, 2D 스틱, 스냅 값은 필수가 아닌 `PGUI` 청크에 넣어 `PARM`을 v1과 일대일로 유지합니다. `restPose`는 쓰지 않습니다.
