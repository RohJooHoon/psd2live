# MCP 사용법과 인터페이스

[문서 목록](../../README.md) · [中文](../../zh/agent/MCP_AUTHORING.md) · [English](../../en/agent/MCP_AUTHORING.md) · [설계와 검수](AGENT_DESIGN.md) · [실측 기록](../STATUS.md)

공개 정의는 애플리케이션 계층의 [WorkspaceAuthoringOperations.kt](../../../src/main/kotlin/io/github/psd2live/application/WorkspaceAuthoringOperations.kt), [WorkspaceDocumentBatch.kt](../../../src/main/kotlin/io/github/psd2live/application/WorkspaceDocumentBatch.kt), [WorkspaceAuxiliaryOperations.kt](../../../src/main/kotlin/io/github/psd2live/application/WorkspaceAuxiliaryOperations.kt), [WorkspaceOperations.kt](../../../src/main/kotlin/io/github/psd2live/application/WorkspaceOperations.kt)에 있으며, MCP 바인딩은 [AgentToolCatalog.kt](../../../src/main/kotlin/io/github/psd2live/agent/AgentToolCatalog.kt)에 있습니다. 도구는 `domain_operation` 형식의 이름을 사용합니다. 예전 분기 도구는 제거되었으며 호환용 별칭도 없습니다. `workspace_list_operations`로 기능을 페이지 단위로 찾아본 다음, `workspace_get_operation`으로 작업 하나의 전체 스키마를 읽으세요.

## 연결

1. 데스크톱 앱을 시작하고 작업 공간을 불러오거나 만듭니다.
2. **설정 → MCP**(제목 표시줄 오른쪽의 톱니바퀴)를 열고 사용하는 호스트에 맞는 명령이나 설정을 복사합니다. Claude Code 명령, Codex TOML, 일반 `mcpServers` JSON(주소 필드는 호스트마다 다름), Stdio 프록시가 있습니다. **설정 프롬프트 복사**는 에이전트가 스스로 설정할 수 있게 하는 짧은 프롬프트를 만듭니다.
3. Bearer 토큰을 쓰는 Streamable HTTP를 우선 사용하세요. 사용 중단된 `/sse` 엔드포인트로 바꾸지 마세요.
4. Stdio만 지원하는 호스트는 Python 3으로 루트의 `mcp_proxy.py`를 실행합니다. 프록시는 `PSD2LIVE_MCP_ENDPOINT`와 `PSD2LIVE_MCP_TOKEN`을 읽으며, Windows에서 이 값이 설정되어 있지 않으면 앱이 저장한 포트와 토큰을 읽습니다. 스크립트와 명령줄에서도 사용할 수 있습니다. `python mcp_proxy.py call <operation> [JSON | @file] [--edit] [--out dir]`(`--edit`는 현재 `state`를 채우고, 이미지는 PNG로 저장됨)와 `python mcp_proxy.py operations`이며, Python에서는 `from mcp_proxy import Client` 후 `call` / `edit`를 사용합니다. 두 도구 프로필 모두에서 동작하며, 백그라운드 작업은 끝난 뒤 결과를 반환합니다.
5. 도구 목록을 확인한 뒤 `workspace_inspect`를 호출해 실제 오브젝트 ID, 파라미터, 현재 `project_id`와 `state`를 읽습니다.

같은 대화 상자에서 서버 사용 여부, 포트(1024–65535, 기본값 23871), 액세스 토큰(32–256자의 영문자, 숫자, `-`, `_`. 다시 생성하거나 직접 입력), 도구 프로필을 설정합니다. 이 값들은 사용자 설정에 저장되며, 적용하면 엔드포인트가 다시 시작됩니다. 다시 시작해도 프로세스 안의 작업과 요청 중복 제거 기록은 유지됩니다. 연결된 호스트는 다시 연결해야 하며, 포트나 토큰이 바뀌었다면 호스트 설정도 갱신해야 합니다.

토큰은 현재 작업 공간을 편집할 수 있게 하므로 이 컴퓨터의 호스트 설정 안에만 두어야 합니다. 도구에는 이미지 생성 모델이 없습니다. 새 이미지는 기존 픽셀, 페인트, 또는 호스트의 이미지 생성기에서 얻을 수 있습니다.

## 도구 프로필

프로필은 `tools/list`가 어떤 도구를 공개할지만 결정합니다. 모든 작업은 두 프로필 모두에서 호출할 수 있으며, 실행과 검증도 같습니다.

- **간결(기본값)**: 123개 작업을 직접 다루는 27개 도구(입력 스키마와 설명은 약 85,000자. 전체 프로필은 약 760,000자이며, 여기에 출력 스키마가 약 426만 자 더해짐).
  - 단일 도구: `workspace_overview`, `workspace_inspect`, `workspace_list_operations`, `workspace_get_operation`, `workspace_apply_edits`, `workspace_preview_edits`, `rig_deform`, `keyform_apply`, 그리고 의도 도구 `author_axis`, `author_physics`, `author_compile`([의도 도구](#의도-도구) 참고).
  - 계열 도구: `view`, `parameter`, `motion`, `expression`, `hit_area`, `pose`, `skeleton`, `path`, `physics`, `simulation`, `swing`, `source_paint`, `snapshot`, `history`, `project`, `job`. `{"op":"set_key","request":{...},"wait_ms":20000}`를 호출하면 `<family>_<op>`(여기서는 `motion_set_key`)를 실행합니다. 결과의 `operation`은 그 작업 ID이며, `request`는 그 작업의 전체 스키마로 검증됩니다. 구성원은 그 접두사를 가진 모든 레지스트리 작업이며, 비공개 초안, 시험 재생, 실시간 미리보기 세션(`skeleton_draft_*`, `physics_audition*`, `*_preview*`), 히스토리 주석, 전역 물리 프리셋 라이브러리는 제외됩니다. 계열 도구는 구성원 필드의 평탄한 합집합을 공개하며(`oneOf`, `const`, `$ref` 없음. 구성원마다 형태가 다른 필드는 설명만 붙음), 각 op의 필수 필드는 도구 설명에 나열됩니다.
  - 그 밖의 작업은 `workspace_call`로 호출합니다: `{"operation":"layer_classify","request":{...}}`. 작업 ID는 `workspace_apply_edits`에서의 구성원 이름이기도 합니다.
  - `outputSchema`는 공개하지 않습니다(결과는 여전히 `structuredContent`로 돌아오며, 계약은 `workspace_get_operation`에 있음). 원자적 일괄 처리의 `edits` 구성원에 대해서는 작업 이름의 열거형과 `request` 객체만 공개되며, 구성원 필드는 실행 시 단일 작업 스키마로 엄격하게 검증됩니다.
  - `request_id`와 `project_id`는 생략할 수 있습니다. `project_id`의 기본값은 현재 불러온 프로젝트입니다. `state`는 여전히 필요하며, 다른 불러오기에 속한 `state`는 평소처럼 `state_conflict`를 보고합니다. `request_id`는 작업과 그 인수(`state` 포함)에서 파생되므로, 같은 인수로 재시도하면 원래 결과를 돌려받습니다. 이전의 동일한 호출이 실패했다면(작업이 `failed/cancelled`로 끝난 경우 포함) 다시 호출할 때 새로운 시도로 실행됩니다.
  - 백그라운드 작업은 `wait_ms`(0–30000, 기본값 20000)를 받습니다. 한도 안에 작업이 끝나면 최종 상태와 결과를 바로 반환하며(`job_wait`와 같은 구조), 그렇지 않으면 실행 중인 작업을 반환하므로 `job_wait`로 이어서 기다립니다. `wait_ms: 0`은 작업 핸들을 즉시 반환합니다.
- **전체**: 작업마다 도구 하나이며, 입력과 출력 스키마를 모두 공개하고, 파생 컨텍스트와 작업 대기가 없습니다. 도구를 필요할 때 불러오는 호스트나 각 계약이 필요한 클라이언트에 적합합니다.

## 의도 도구

의도 도구는 레지스트리의 일반 공개 작업이며(두 프로필 모두에서 호출 가능), [WorkspaceIntentOperations.kt](../../../src/main/kotlin/io/github/psd2live/application/WorkspaceIntentOperations.kt)에 정의되어 있습니다. 쓰기 도구는 요청의 `state`에 대해 캡처한 모델에서 의도를 기존 일괄 처리 구성원으로 컴파일한 다음, `workspace_apply_edits`와 같은 원자적 일괄 처리(`startDocumentBatch`)를 거칩니다. 다시 빌드, 기하 검사, CAS가 동일하며, 한 번 성공하면 히스토리 노드가 정확히 하나 추가되고, 새로운 저널 의미는 추가되지 않습니다. 작업 결과는 `workspace_apply_edits`와 같습니다(`edit_count`는 컴파일된 구성원 수). 컴파일 중에 발견된 문제는 즉시 `invalid_argument`를 반환하고, 실패한 구성원은 구성원 인덱스와 작업을 담은 `invalid_edit`를 반환합니다. 캡처한 버전이 요청의 `state`와 다르면 `state_conflict`를 보고합니다.

| 도구 | 컴파일 결과 | 비고 |
| --- | --- | --- |
| `workspace_overview` | 읽기 전용 조회 | 한 번의 캡처로 캔버스, 파라미터, 레이어, 디포머 계층(부모와 자식 수 포함), 메시 수, 물리 그룹(입력/출력 파라미터), 모션 클립을 반환합니다. 각 목록은 `total`을 가지며 `limit`(1–500, 기본값 200)으로 잘립니다. 파라미터는 `kind`와 `repeat`(반복 파라미터)를 가집니다 |
| `author_axis` | `parameter_create`(파라미터가 없을 때) → `keyform_apply` seed → `rig_deform` | `parameter.id`가 없으면 만듭니다(`name`의 기본값은 ID). 있으면 지정한 `min/max/default`가 현재 값과 일치해야 합니다. 각 형태 대상은 먼저 파라미터 기본값에서 seed되어 현재 형태가 중립 형태가 됩니다(축에 이미 기본값의 키가 있고 `key`를 지정하지 않으면 건너뜀). 그다음 `rig_deform`의 `operations/selection`이 `value`에서 실행됩니다. 대상에 직접 바인딩된 다른 축은 각자의 기본값을 취하며, `key`로 조합 각도의 값을 줄 수 있습니다. `value`가 기본값과 같으면 거부됩니다. `mirror: true`이면 각 형태가 반대쪽 값(`2×default − value`, 범위 안이어야 함)에도 미러되므로 한쪽만 작성하면 양쪽이 만들어집니다. 엔진은 새 축의 키를 min/default/max에 만듭니다 |
| `author_physics` | `physics_put` → `physics_fit` | 필드는 `physics_put`과 같고, 추가로 `fit_target`(10–300, 기본값 100. `null`이면 피팅을 건너뜀)이 있습니다. `id`와 `fit_target`만 주면 기존 그룹만 다시 피팅합니다. 둘 다 아무것도 바꾸지 않으면 거부됩니다. 출력 파라미터에는 이미 작성된 형태가 있어야 합니다 |
| `author_compile` | 읽기 전용 조회 | 의도가 커밋할 구성원을 반환합니다. 아래 참고 |

의도 도구 자체에는 시험 실행이 없습니다. `author_compile`은 같은 의도를 요청의 `state`에 대해 컴파일하고 커밋할 구성원을 반환하며(`{"intent":"author_axis","request":{...}}`. `request`에는 `state`를 넣고 `request_id/project_id`는 넣지 않음), 프로젝트는 바꾸지 않습니다. 반환된 구성원을 확인하거나 수정한 뒤 `workspace_preview_edits`에 넘겨 기하 구성원을 시험 실행하거나, `workspace_apply_edits`에 넘겨 커밋할 수 있으며, 결과는 의도를 직접 호출한 것과 같습니다.

## 공개 도구 참조

| 도구 | 요청 구조 | 용도 |
| --- | --- | --- |
| `workspace_list_operations` | 선택 사항 `domain`, `kind`, `query`, `offset`, `limit` | 도구, 업무 유형, 실행 메타데이터를 페이지 단위로 찾습니다. `query`의 모든 단어가 작업 ID나 설명에 있어야 합니다(대소문자 무시). 목록 항목에는 설명의 첫 문장만 있으며, 전체 설명은 `workspace_get_operation`에 있습니다 |
| `workspace_get_operation` | `id` | 작업 하나의 스키마, 필드 설명, 실행 메타데이터를 읽습니다 |
| `workspace_apply_edits` | `state`, `edits`(1–128), 선택 사항 `summary` | 순서가 있는 편집을 하나의 후보 문서에 백그라운드로 적용하고, 모두 성공하면 히스토리 노드 하나를 커밋합니다. 작업 핸들을 반환합니다. 구성원 지원 여부는 `batchable`을 참고하세요 |
| `workspace_preview_edits` | `state`, `edits`(1–128), 공유 요청 컨텍스트 | 기하 작성 편집을 백그라운드로 시험 실행하고 후보 리비전과 진단을 반환합니다. 문서, 포즈, 히스토리, 리소스를 게시하지 않습니다 |
| `workspace_preview_regeneration` | `state`, `edits`(1–128, 리소스 레이어 작업 제외), 공유 요청 컨텍스트 | 리그를 다시 생성하는 편집(설정, 분류, 분할, `rig_update_generation` 등)을 백그라운드로 시험 실행하고, 후보 리비전, 기록하게 될 체크포인트와 그 병합 문제, 추가되고 제거되는 오브젝트를 반환합니다. 아무것도 게시하지 않습니다 |
| `workspace_inspect` | `request` 안의 `scope` / `target` | scope는 `project/settings/preview/generation/objects/layers/parameters/physics/swings/paths/simulations/vertex_groups`입니다. 레이어 요약에는 유효한 메시 설정과 의미 `tags`가 포함됩니다. 마지막 생성 이후 배치되었거나 생성을 기다리는 분류가 있는 레이어는 `pending:true`를 가지며, 소재에서 온 레이어는 `material_id`를 가집니다. `generation`은 프로젝트의 생성 여부, 대기 중인 레이어, 대기 중인 분류, 기하를 새로 고칠 수 있는 레이어를 반환합니다. 프로젝트가 불러와져 있으면 `project`는 `quality.overrides`를 가집니다. 이는 세 가지 검사를 담은 관찰 보고서(버전 4)로, 기록대로 적용되지 않는 생성 결과 재정의(conflict / orphan, 둘 다 warning. 색이나 불투명도 같은 채널의 재정의는 `evidence.channel`에 그 채널을 명시), 이후의 버전 2 분할로 대체된 메시만 참조해 아무 효과 없이 재생된 저널 항목(`SUPERSEDED_ENTRY_SKIPPED`, info, `target`은 `journal:<index>`), 설정이 요구하지만 만들 수 없었던 앞머리 위 복사본(`SEE_THROUGH_DROPPED`, info, `target`은 원본 `mesh:<id>`, `evidence.reason`은 `original_missing` / `masks_missing` / `id_taken`, 또는 앞머리에 난 눈 파츠 창의 경우 `bangs_clipped`(앞머리에 자체 마스크가 있음) / `target_unsupported`(런타임 대상에 반전 마스크가 없음))입니다. 또한 `quality.regeneration`도 가집니다. 이는 두 가지 검사를 담은 관찰 보고서(버전 2)로, 먼저 저널의 마지막 체크포인트에서 재생성 병합이 깔끔하게 이전하지 못한 것을 나열하고(`REGENERATION_CONFLICT`와 `REGENERATION_TOPOLOGY_KEPT`는 warning, `REGENERATION_DROPPED`는 warning / validity. 재부착, 생성기의 후속 오브젝트를 따른 재부착(`REGENERATION_FOLLOWED`), 폐기된 오브젝트 유지, 부모 변경, 정점 이전, 생성을 따른 사용자 토폴로지(`REGENERATION_TOPOLOGY_FOLLOWED`), 이 빌드가 다시 병합한 체크포인트(`REGENERATION_REMERGED`)는 info), 이어서 이전 버전이 병합한 재생성 체크포인트 중 이 빌드라면 리그에 영향을 주는 다른 구조로 병합할 것을 나열합니다(`REGENERATION_REPAIR_AVAILABLE`, warning, `target`은 `journal:<index>`, `evidence.detail`은 구조가 다른 오브젝트를 나열. `rig_update_generation`을 실행하면 복구됨. 모델마다 첫 검사는 다시 병합해야 하므로 몇 초 걸릴 수 있음). `can_proceed`는 항상 true입니다. 또한 `quality.skeleton`도 가집니다. 이는 관찰 보고서(버전 1)로, 뼈에 바인딩되었지만 그 팔다리 뼈로 구동되지 않는 메시(`SKELETON_BINDING_INEFFECTIVE`)와 호흡 Warp 아래에서 뼈대의 몸통 Warp를 우회하는 메시(`SKELETON_TORSO_BYPASSED`)를 나열하며 둘 다 warning이고, 리그에 없는 메시에 대한 바인딩(`SKELETON_BINDING_MISSING`, info / validity)도 나열합니다. `target`은 `mesh:<ID>`, `evidence.bone`은 그것을 바인딩한 뼈이며, `can_proceed`는 항상 true입니다 |
| `layer_classify` | `request` 안의 `state`, `layer_id`와 분류 필드 | 기존 원본 레이어의 유형, 파츠, 좌우, 파라미터 연결, 전환 ID를 백그라운드로 갱신합니다. `part`는 레이어가 파츠 안에서 무엇인지 정합니다(`highlight`, `pupil`, `lower`, `lid`, `shadow`, `blush`, `side`, `ahoge`, `ponytail`, `braid`, `animal`, `ribbon`, `pendant`, `rigid`, `sleeve`, `skirt`, `upperlip`, `lowerlip`, `upperline`, `lowerline`. 역할 없음은 `none`, 분석에 맡기려면 `auto`). 이 값은 리그 모듈만 읽습니다. 생략한 필드는 캡처한 값을 유지합니다. 생성된 레이어는 즉시 다시 생성되지 않습니다. 결과에 `pending:true`가 붙고 변경은 다음 `rig_generate`에서 적용됩니다. 효과를 바로 보려면 같은 `workspace_apply_edits` 일괄 처리의 끝에 `rig_generate`를 추가하세요. 아직 생성되지 않은 레이어는 변경을 즉시 반영합니다 |
| `layer_mesh_update` | `request` 안의 `state`, `layer_id`, `changes` 또는 `reset` | 레이어별 적응형 메시 파라미터를 백그라운드로 재정의하거나 초기화합니다. 현재 값은 `workspace_inspect scope=layers`로 읽습니다 |
| `source_sample_color` | `layer_id`, `x`, `y` | 정수 캔버스 좌표에서 원본 레이어의 RGBA를 읽습니다. 문서와 히스토리는 바꾸지 않습니다 |
| `source_paint_brush / source_paint_pencil / source_paint_eraser / source_paint_bucket / source_paint_shape / source_paint_clear` | `request` | `brush/pencil/eraser/bucket/shape/clear`. 좌표는 캔버스 단위이고, 획은 레이어의 래스터 밀도로 그려지며, 제스처 하나가 히스토리 노드 하나입니다 |
| `preview_set / preview_reset` | `request` 안의 `state` | `set/reset`. `set`은 `expression`(표정 ID)을 지정해, 애니메이션 패널의 미리보기처럼 `values`를 먼저 적용한 커밋된 포즈 위에 그 표정을 최대 가중치로 표시할 수 있습니다. 현재 미리보기 파라미터 값과 잠금 상태를 바꾸며 키폼은 기록하지 않습니다 |
| `snapshot_create / snapshot_update / snapshot_delete / snapshot_apply` | `state`. update/delete/apply는 `id` 필요. create/update는 `name`, `values`를 지정할 수 있음 | 파라미터 스냅숏을 저장, 이름 변경/덮어쓰기, 삭제, 적용합니다. 적용 시 잠금을 유지하고, 삭제된 파라미터는 무시하며, 현재 범위로 클램프합니다. 타임라인 키프레임은 기록하지 않습니다 |
| `snapshot_get / snapshot_list` | get은 `id`를 받음. list는 `offset`, `limit`를 받을 수 있음 | 같은 캡처에서 프로젝트 상태와 스냅숏 ID/번호를 반환합니다. 목록은 값을 생략하고, 상세는 값을 반환합니다 |
| `history_annotation_put / history_annotation_delete / history_annotation_get` | `node_id`. 쓰기에는 `title`, `note`, `hidden`도 필요 | 히스토리 노드의 표시용 주석을 관리합니다. 보조 데이터는 프로젝트와 함께 저장되며, 히스토리 노드와 리그 리비전은 바뀌지 않습니다 |
| `timeline_bookmark_set / timeline_shy_set / timeline_freeze_set / timeline_marker_put / timeline_marker_delete` | `state`. 북마크는 `parameter`, `bookmarked`를 받음. 나머지는 `clip_id`(편집기의 모션 ID, 생성된 모션은 `preset:<Name>`)와 `parameter`, `shy` / `frozen`(고정에는 `value` 필요), 또는 마커의 `time`, `label`, `color`, `id`를 받음 | 애니메이션 편집기의 타임라인 보조 기능입니다. 파라미터 북마크, shy 트랙과 고정 트랙, 타임라인 마커. 프로젝트와 함께 저장되는 보조 데이터이며 히스토리 단계도 내보내기도 없습니다. 고정은 편집기가 모션을 재생할 때만 값을 붙잡을 뿐 `motion_*`의 키 편집을 막지 않습니다. `id`가 없는 마커는 다음 `marker_<n>`을 받습니다 |
| `timeline_aids_get` | `clip_id`를 받을 수 있음 | 북마크와 각 모션의 shy 트랙, 고정 값, 마커를 읽습니다 |
| `canvas_guides_get / canvas_guides_set` | `set`은 `state`, `guides` 필요 | 편집 캔버스의 눈금자 가이드를 읽거나 교체합니다. 각각 `{axis: x / y, position, id?}`(캔버스 픽셀, `x`는 세로선)입니다. `id`가 없는 가이드는 다음 `guide_<n>`을 받으며 최대 256개이고, 빈 목록은 모두 지웁니다. 캔버스 눈금자와 같은 보조 CAS를 사용하며, 프로젝트와 함께 저장되고, 히스토리가 없으며, 내보내지 않습니다 |
| `module_catalog` | 없음 | 읽기 전용이며 열린 프로젝트가 필요 없습니다. 리그의 영역(눈, 눈썹, 입과 얼굴 아래쪽, 머리와 얼굴, 머리카락, 몸, 옷과 액세서리, 표정, 모션), 각 영역의 베이스(서로 배타적인 구성 방식. 첫 번째는 모듈 이전의 `legacy`)와 모듈(더해지는 기능으로 각각 따로 켬. `tier` core / major / minor는 중요도를, `requires`는 기반으로 하는 것을 나타냄)을 조정 가능한 값(단위, 범위, 기본값)과 함께 나열합니다. 프로젝트가 열려 있으면 `modern`(모듈 이전에 저장된 프로젝트는 false이며 예전 방식대로 생성. `settings_update` `changes.rig: {}`로 업그레이드), 각 영역의 현재 베이스, 모듈별로 그림에 적용되는지, 켜져 있는지, 비용(추가하는 파라미터, 키폼, 블렌드 셰이프 폼, 물리 그룹. 켜져 있으면 측정값, 꺼져 있으면 추정값 `estimated:true`)을 추가합니다. [리그 프리셋 아키텍처](../spec/RIG_PRESETS.md)를 참고하세요 |
| `settings_update` | `request` 안의 `state`, `changes` | 자동 리그, 메시, 텍스처, 고해상도화, 물리 프리셋, 모션, 내보내기 설정을 백그라운드로 변경합니다. `rigTuning`은 리그 값을 필드별로 병합합니다(머리 회전, 눈, 눈썹, 입, 코와 귀, 머리카락, 그리고 몸의 회전, 위아래, 기울기, 크기 변화, 몸 Z와 호흡, 깊이와 원근. 단위는 모델 프리셋의 각 영역 카드에 있는 **값**과 같으며, 범위를 벗어나거나 알 수 없는 필드는 거부됨). `rig`는 리그 레시피를 바꿉니다. `bases: {area: {base, values}}`는 영역의 베이스를 고르고, `modules: {module: {on, values}}`는 모듈을 켜고 끄며 값을 설정합니다. 유지되는 베이스나 모듈의 `values`는 현재 값과 병합되고, `null` 항목은 기본값으로 돌아갑니다(모듈은 켜짐으로). `rig: {}`는 모듈 이전에 저장된 프로젝트를 업그레이드하고, `rig: null`은 원래 생성 방식으로 되돌립니다. 예전 스위치를 대신하는 모듈(`eye.see-through`, `eye.jelly`, `brow.see-through`, `mouth.outline`)은 그 `on`을 해당 설정에 기록합니다. `moves: {feature: {handle: [dx, dy] or null}}`는 맞춰진 이목구비의 핸들을 옮깁니다(이목구비는 `eye.L` / `eye.R` / `brow.L` / `brow.R` / `mouth`. 핸들은 눈의 `inner` / `outer` / `upper` / `lower`, 눈썹의 `inner` / `outer` / `arch`, 입의 `left` / `right` / `upper` / `lower`. 그림에 맞춘 위치로부터의 캔버스 픽셀. `null`은 핸들 하나나 이목구비 전체를 맞춘 위치로 되돌림). 알 수 없는 영역, 베이스, 모듈, 값, 이목구비, 핸들과 범위를 벗어난 값은 거부됩니다(영역과 모듈은 `module_catalog` 참고). 소스를 끄면 그것이 구동하던 작성 포즈를 작업 공간별로 파라미터 기본값으로 되돌리며(잠긴 파라미터는 건너뜀), 설정과 같은 커밋에서 처리됩니다. 문서는 원래 설정을 유지합니다. 먼저 `workspace_inspect scope=settings`로 읽으세요 |
| `project_import_psd` | `path`, 선택 사항 `discard_unsaved` | 절대 PSD 경로에서 새 프로젝트를 백그라운드로 만듭니다. 프로젝트가 불러와져 있어도 전환할 수 있으며, 기본적으로 저장하지 않은 변경이 있으면 거부합니다 |
| `project_create_artwork` | `width`, `height`, `layers`, 선택 사항 `discard_unsaved` | 로컬 PNG/WebP/TIFF/BMP 파일을 위치에 맞춰 배치해 새 프로젝트를 만들며, 레이어는 아래에서 위 순서입니다. 백그라운드 작업이며, 전환하려면 저장하거나 명시적으로 버려야 합니다 |
| `project_create` | `width`, `height`, 선택 사항 `discard_unsaved` | 소재를 가져와 생성 전에 배치할 수 있도록 생성되지 않은 빈 프로젝트를 백그라운드로 만듭니다. [소재와 Layout](#소재와-layout)을 참고하세요 |
| `project_import_layout` | `path`, 선택 사항 `discard_unsaved` | [Layout 매니페스트](../spec/LAYOUT_MANIFEST.md)(`.json`의 절대 경로, 또는 루트에 매니페스트가 있는 `.zip`)에서 생성되지 않은 Layout 프로젝트를 백그라운드로 만듭니다 |
| `project_import_cmo3` | `path`, `mode`(`new/replace`), 선택 사항 `discard_unsaved` | 절대 경로의 CMO3를 백그라운드로 가져옵니다. new는 기본적으로 저장하지 않은 변경이 있으면 전환을 거부합니다. replace는 오브젝트 ID로 갱신하고, 파일에 없는 오브젝트는 유지하며, 히스토리 편집 하나로 커밋합니다 |
| `project_open` | `path`, 선택 사항 `discard_unsaved` | 절대 경로의 프로젝트 아카이브를 백그라운드로 엽니다. 기본적으로 저장하지 않은 변경이 있으면 거부하므로 먼저 저장하거나 명시적으로 버리세요. 빈 작업 공간에서도 호출할 수 있습니다 |
| `project_save` / `project_save_as` | save는 추가 필드 없음. save as는 `path` 필요 | 프로젝트를 백그라운드로 저장합니다. 첫 저장은 절대 경로로 save as를 사용하고, 이후 저장은 현재 경로를 사용합니다. 바뀐 것이 없으면 히스토리 노드를 추가하지 않습니다 |
| `project_export_model` | `request_id`, `state`, `output_directory` | 모델 파일 묶음을 백그라운드로 내보냅니다. 작업 핸들을 반환하며, 디렉터리는 절대 경로여야 합니다 |
| `project_export_target` | `request_id`, `state`, `target`, `output_directory`, 선택 사항 `settings` | **파일 → 다른 형식으로 내보내기**처럼(실험적 대상 제외) 커밋된 모델을 내보내기 대상 하나로 백그라운드에서 내보냅니다. `settings`는 그 대상 고유의 설정 키입니다(`psd2live targets`가 나열). 결과에는 파일과 손실이 들어 있습니다 |
| `project_export_psd` | `request_id`, `state`, `path` | PSD를 백그라운드로 내보냅니다. 1/2/4×와 생성 레이어 옵션을 지원합니다 |
| `job_get` / `job_wait` | `id`. 대기에는 `timeout_ms`(0–30000)를 줄 수 있음 | 진행 상황, 최종 상태, 결과를 조회합니다. 대기를 취소해도 작업은 취소되지 않습니다 |
| `job_list` | 선택 사항 `project_id`, `offset`, `limit` | 앱 안의 작업을 페이지 단위로 봅니다 |
| `job_cancel` | `request_id`, `id` | 취소를 요청합니다. 이미 끝난 커밋은 완료 상태를 유지합니다 |
| `rig_deform` | `request` 안의 `state`, `changes` | 명시적인 파라미터 키에서 연속적인 Mesh / Warp 형태를 편집합니다. `selection.indices`는 인덱스로 점을 고르며(`mesh_get_topology`가 나열), `soft_radius`(`radius`와 같은 정규화 프레임)와 `falloff`(기본값 `smooth`, 또는 `sphere/root/inverse_square/sharp/linear/constant/random`)를 주면 편집기의 소프트 선택처럼 주변 점이 거리에 따라 따라옵니다. 임시 격자가 선택한 점에 하는 일은 `landmarks`로 표현합니다 |
| `keyform_apply` | `request` 안의 `state`, `changes` | `op: seed/copy/set/delete/mirror/reset/interpolate/paste/multi_key`. 키폼 집합, 스칼라 / 색 채널, 회전 형태를 편집합니다. 회전 `set`이 일부 필드만 주면(예: `angle`만) 나머지 필드는 그 키에서 회전의 현재 값을 취합니다 |
| `rig_create_warp` | `name`, `targets`. 선택 사항 `id`, `rows/columns`, `fit_local` | 부모 Warp를 공유하는 메시들에 대해 독립된 Warp를 백그라운드로 만듭니다. 원자적 일괄 처리 구성원으로도 쓸 수 있으며, 최종 상태는 `target`을 반환합니다 |
| `object_edit_appearance` | `request` 안의 `state`, `edits` | 이름, 가시성, 구조 등에 대한 순서 있는 편집입니다. 메시 `bind`에서 `space=local`은 로컬 좌표와 키폼을 유지하고(상속되는 움직임과 위치가 새 부모에 따라 바뀜), `space=canvas`는 기본 메시, 키폼, 블렌드 셰이프를 새 부모를 통해 변환해 기본 포즈에서 메시가 캔버스 위 같은 자리에 머물게 합니다(계층 드래그 앤 드롭과 인스펙터의 부모 변경이 이를 사용). 디포머 `move`는 `space=local`만 받습니다 |
| `rig_edit_structure` | `request` 안의 `state`, `edits` | 정적 오브젝트 속성(`blend_mode`는 `Normal`, `Multiply`, `Screen`처럼 `workspace_inspect`가 돌려주는 블렌드 모드 이름을 받음), 디포머 삭제와 Part 소속, 파라미터 폴더와 XY 연결 |
| `object_paste / mesh_duplicate / object_rename / object_convert_id` | `request` 안의 `state`와 각자의 필드 | 오브젝트 복사와 붙여넣기, 메시의 토폴로지를 다른 메시로 복사, 일괄 이름 변경과 ID 변환. 모두 일괄 처리 가능하며 [오브젝트 복사, 찾아 바꾸기, ID 변환](#오브젝트-복사-찾아-바꾸기-id-변환)을 참고하세요 |
| `canvas_warp / canvas_rotation / canvas_glue / canvas_topology` | `request` | `warp/rotation/glue/topology`. `glue`는 서로 다른 아트 메시 두 개 `mesh_a`와 `mesh_b`를 지정해야 합니다. `rotation`의 `origin`은 캔버스 픽셀(y 아래 방향)이며 커밋할 때 부모 디포머의 로컬 공간으로 변환됩니다. `preservePose`의 기본값은 true로, 기본 포즈에서 메시를 제자리에 유지합니다. `topology`는 아래의 [메시 토폴로지](#메시-토폴로지)를 참고하세요 |
| `view_render_model / view_render_layer / view_render_context / view_render_poses / view_render_pad / view_check_coverage / view_compare_history / view_sample_motion` | `request` | `model/layer/context/poses/pad/coverage/compare/motion`. 모션 샘플링은 읽기 전용 작업 핸들을 반환하며, 그 최종 상태에 샘플 시트와 파라미터 범위가 들어 있습니다 |
| `parameter_create / parameter_update / parameter_delete` | `request` | `create/update/delete`. 삭제는 마지막 기본값에 가장 가까운 기록된 키 단면을 골라 축을 접습니다. 범위를 좁히면 그 파라미터의 모션 키와 핸들이 새 범위로 클램프되며, 파라미터를 삭제하면 모션에서 그 커브도 제거됩니다 |
| `layer_import_images` | `request` | PNG, 무손실 WebP, TIFF, BMP 파일에서 원본 레이어 여러 개를 한 번에 가져오며, 백그라운드 작업이 끝나면 레이어와 메시 핸들을 반환합니다 |
| `layer_get_texture / atlas_get / atlas_render_page` | `layer_id`. `atlas_get`은 선택 사항 `page`. 렌더링은 `page` 필요, 선택 사항 `max_size` | 한 번의 캡처에서 레이어의 캔버스 사각형, 래스터 픽셀, 고유 밀도, 텍스처 재정의와 타일을 읽거나, 아틀라스 예산, 맞춤, 알림, 페이지 점유율과 모든 타일을 읽습니다. 페이지 렌더링은 읽기 전용 백그라운드 작업이며 최종 상태가 PNG를 반환합니다 |
| `layer_set_canvas_rect / layer_replace_image / layer_set_pixel_density / atlas_set_tile / atlas_set_budget / atlas_pack` | `request` 안의 `state`와 각자의 필드 | 레이어의 캔버스 사각형 변경, 임의 해상도로 이미지 교체, 텍스처 밀도와 잠금 설정, 타일 이동, 아틀라스 예산과 자동 패킹 변경, 또는 한 번 패킹을 백그라운드로 수행합니다. 모두 원자적 일괄 처리에 참여할 수 있습니다. [텍스처와 아틀라스](#텍스처와-아틀라스)를 참고하세요 |
| `source_get_components / source_split_components / source_split_polygon / source_split_depth / rig_update_generation / asset_prepare_reference / asset_import_png / asset_register / asset_preview_composite / layer_add_from_asset / layer_set_placement / layer_finalize_placement / asset_inspect / asset_reprocess / layer_delete / layer_restore` | `request` | 원본 분할, 분할 기록 업그레이드, 그리고 에셋 참조, 가져오기, 등록, 미리보기, 추가, 위치 지정, 확정, 검사, 재처리, 소프트 삭제와 복원 |
| `swing_put / swing_delete` | `request` | `put/delete`. Warp나 Mesh에 좌우 / 상하 흔들림과 진자를 생성합니다(Mesh는 자동으로 Warp로 감쌈). `motions`는 좌우와 상하를 조합하고, `parallel`은 여러 머리카락 가닥이 나란히 흔들리게 하며, `tilt` / `offset_along` / `offset_across`는 흔들림 사각형을 회전하고 평행 이동합니다. `delete`는 일반 키로 `bake`할 수 있습니다. [흔들림 생성](../guide/SWING.md)을 참고하세요 |
| `physics_put / physics_delete / physics_simulate / physics_fit / physics_config / physics_import` | `request` | `put/delete/simulate/fit/config/import`: ID로 임의의 물리 그룹(생성된 프리셋, 뼈대와 흔들림 그룹 포함)을 만들거나 일부 수정합니다. 사용자 그룹을 삭제하거나 생성된 값을 복원합니다. 읽기 전용 스텝 샘플링을 백그라운드로 실행해 작업 결과로 최댓값과 안정 시간을 반환합니다. 표준 흔들기나 `observed_peaks`의 측정 최댓값으로 출력 스케일을 조정합니다. 평가 순서와 물리 FPS를 설정합니다. physics3.json을 가져옵니다. [물리](../guide/PHYSICS.md)를 참고하세요 |
| `simulation_put / simulation_delete / simulation_simulate / simulation_compare / simulation_bake / simulation_clear_bake` | `request` 안의 `state`, `id` | `put/delete/bake/clear_bake`는 프로세스 작업 핸들을 반환합니다(최종 상태에 원래 편집 결과와 베이크 진단이 들어 있음). `simulate`와 `compare`도 마찬가지로 프로젝트를 바꾸지 않는 읽기 전용 작업 핸들을 반환하며, `compare`는 `motions`의 모션으로 베이크와 기준 시뮬레이션을 비교해 시각적 검사 결과를 반환합니다. 메시 위의 2D 천 / 머리카락 시뮬레이션은 편집기 안에서만 실행됩니다. `bake`는 이를 -30…30 범위의 `ParamSim<id>_<k>` 파라미터(`keys`개의 키, `blend_shapes`가 인코딩을 선택하며 기본은 자동)와 피팅된 진자 `PhysicsSim_<id>`로 베이크합니다. 끄덕임, 몸의 위아래, 기울기는 따로 `ParamSim<id>_Y`로 베이크되며, 평행 이동 입력 진자 `PhysicsSim_<id>_y`가 이를 구동합니다. 몸을 움직이지 않는 입력은 제외됩니다. 각 입력이 읽는 값, 테스트 모션에 대한 시각적 검사(진폭, 지연, 안정, 떨림 등), R², 오차, 파라미터 사용률, 끝값에 닿는 프레임 비율, 저크 비율을 반환합니다. `force_inputs`는 입력을 강제로 유지하고, `training_clips`는 모델 자체의 모션을 학습에 참여시킵니다. `exaggeration`은 다시 베이크하지 않고 모드 흔들림을 증폭합니다. `auto_bake`가 켜져 있으면(기본값) `put`이 같은 단계에서 다시 베이크합니다. `inputs`가 없는 새 시뮬레이션은 기본 입력을 받으며, 빈 배열은 입력 없음을 뜻합니다. `output_names`는 베이크된 파라미터와 진자의 이름을 바꿉니다. `input_ranges`는 입력별 학습 범위를 설정하고, `vertical`은 상하 파라미터를 켜고 끄며(`null`은 자동), `outputs`는 생성된 모드 파라미터 ID별로 내보내는 ID, 범위(±1…±100), 게인(0…3)을 다시 베이크하지 않고 다시 씁니다. `obstacles`(통째로 교체)는 입자가 들어오지 못하게 하는 장애물로, 다른 메시와 함께 변형되는 원(정점 `a`)이나 캡슐(`a`에서 `b`까지)이며, `radius`에서 `radius_b`로 점차 변하고, `friction`은 0…1입니다. 장애물은 시뮬레이션과 베이크에 참여하며 p2lrt / web 대상으로 내보내집니다. 물리 패널에서 시뮬레이션 진자에 가한 재정의는 다시 베이크한 뒤 3방향 병합됩니다. [시뮬레이션과 베이크](../guide/SIMULATION.md)를 참고하세요 |
| `model_apply_preset` | `request` 안의 `preset`, `state`, `layers`, `sway` | `front_hair/back_hair/clothing/auto_weights/classic_front_hair/classic_back_hair/remove_clothing`: 가중치 그룹과 프리셋 물리 바디를 백그라운드로 생성하고 베이크합니다. 작업 핸들을 반환하며 성공하면 히스토리 노드를 정확히 하나 추가합니다. 머리카락 프리셋은 그 머리카락 유형의 예전 흔들림 파라미터, 디포머, 진자를 제거하고, `classic_*`는 이를 복원하며(`sway:false`는 클래식 흔들림을 끔), `remove_clothing`은 모든 옷 프리셋 물리 바디를 삭제합니다. `clothing`은 상의, 하의, 목 장식, 소매, 다리 의류의 헐렁한 부분만 시뮬레이션하며, 각 메시의 영역(하의는 치마 / 바지 판정과 허리선, 가랑이, 밑단도 포함), 헐렁함 비율, 드레이프 시작점, 시뮬레이션 여부를 반환합니다. [시뮬레이션과 베이크](../guide/SIMULATION.md)의 「모델 프리셋」 절을 참고하세요 |
| `vertex_group_update` | `request` 안의 `state`, `target`, `name` | 이 소프트웨어에서만 쓰는 정점 가중치 그룹(고정, 강성 등)을 규칙 `fill/outline/gradient/glue/region`으로 생성하거나 `delete`합니다. 내보내지 않습니다 |
| `skeleton_get / skeleton_propose / skeleton_auto / skeleton_put / skeleton_enable / skeleton_bone / skeleton_move / skeleton_bind / skeleton_remove / skeleton_delete / skeleton_pose` | `request` | `get/propose/auto/put/enable/bone/move/bind/remove/delete/pose`: 뼈대를 읽거나 추론하고, 완전한 뼈대를 커밋하며, 뼈와 바인딩을 편집합니다. `delete`는 뼈대 전체를 제거합니다(그 뒤에는 `auto`/`put`을 다시 사용할 수 있음). `pose`는 히스토리를 기록하지 않고 FK/IK 파라미터 값을 풉니다 |
| `motion_list / motion_get / motion_sample / motion_put / motion_delete / motion_seed_builtin / motion_set_key / motion_delete_key / motion_remove_curve` | `request` | `list/get/sample/put/delete/seed_builtin/set_key/delete_key/remove_curve`: 보간된 포즈를 읽고 모션 클립, 파라미터 트랙, 타임라인 키프레임을 영구적으로 편집합니다 |
| `path_get / path_list / path_preview / path_put / path_delete / path_deform` | `request` | `get/list/preview/put/delete/deform` |
| `history_checkpoint / history_list / history_checkout` | `request` | 체크포인트를 만들고, 히스토리를 읽거나, 노드를 전환합니다 |

표는 업무 필드를 나열합니다. 모든 변경 작업은 `request_id`도 가지며, 작업 공간 변경 작업은 `project_id`와 `state`를 가집니다(간결 프로필에서는 앞의 두 개를 생략할 수 있음. [도구 프로필](#도구-프로필) 참고). 읽기 전용 백그라운드 샘플러 `physics_simulate/simulation_simulate/simulation_compare/view_sample_motion`도 중복 제거와 샘플링 버전 고정을 위해 이 세 필드가 필요합니다. 필드는 작업마다 다르므로, 호출하기 전에 실행 중인 서비스가 제공하는 JSON Schema를 읽으세요. 모든 공개 도구는 `{"request": {...}}` 래퍼를 사용합니다. 공개와 검증은 같은 `oneOf`, `const`, 필드 제약, 설명을 유지하며, 바깥 객체와 업무 객체 모두 알 수 없는 필드를 거부합니다. 결과는 일관되게 `{"ok":true,"operation":"...","data":{...}}`이고, 오류는 `ok:false`와 `error.code/message`를 포함하며, 필드 검증 오류에는 `field`도 붙습니다. PNG는 MCP 이미지 콘텐츠로 돌아옵니다.

195개 공개 작업(그중 70개는 백그라운드, 91개는 일괄 처리 가능)은 모두 완전한 `outputSchema`를 선언해야 합니다. 전체 프로필이 이를 공개하며, 기능 상세의 `output_schema`가 이와 일치합니다. 성공 데이터와 실패 오류는 엄격하게 상호 배타적입니다. 레지스트리는 실행과 중복 제거 경계에서 업무 결과를 검증하고, MCP는 반환되는 래퍼 전체를 검증하며, 결과 계약이 없는 작업은 등록할 수 없습니다. 백그라운드 작업에는 최종 `job_result_schema`도 있어야 하며, 백그라운드가 아닌 작업에는 그 필드가 있으면 안 됩니다. 기능 상세는 로컬 `$defs/$ref`로 중첩 스키마를 설명하므로 자기 스키마를 조회해도 검증됩니다. 실제 HTTP는 모든 루트 제약을 유지합니다. `output_contract`는 서비스 구현이 선언과 맞지 않는 결과를 반환했다는 뜻이며, 변경이 롤백되었다는 근거로 볼 수 없습니다.

이미지는 `layer_import_images`로 추가합니다. `state`와 1–128개의 절대 경로를 담은 `paths`가 필요하며, 선택 사항 `parent_deformer_id`로 기존 부모 디포머를 지정합니다. 생략하면 레이어는 모델 루트에 바인딩됩니다. 선택 사항 `anchor_layer_id`는 일괄로 가져온 이미지를 그 레이어(또는 메시) 바로 앞에, `in_front=false`이면 바로 뒤에 그리며, 첫 번째 이미지가 나머지보다 앞에 오고, 명시적인 그리기 순서 재정의로 기록됩니다. 선택 사항 `at` `[x, y]`는 각 이미지를 그 캔버스 지점의 중앙에 놓습니다(캔버스 안으로 유지). `as_reference=true`는 레이어 목록에 참조로 끌어다 놓을 때처럼 레이어 스택에 참조 레이어로 가져오며(메시 없음, 내보내지 않음), 여전히 히스토리 노드 하나입니다. 투명한 가장자리는 잘라 내고 이미지를 중앙에 놓습니다. 래스터는 원래 해상도를 유지하며, 캔버스보다 크면 캔버스 사각형만 비례해 줄여 맞추고(레이어 밀도는 캔버스 단위당 1픽셀 초과), 메시는 캔버스 해상도로 생성됩니다. 파일 하나는 최대 64 MiB, 1,600만 픽셀이며, 일괄 처리 전체는 최대 3,200만 픽셀입니다. 한 번 성공하면 히스토리 노드가 정확히 하나 추가되며, 파일 하나라도 실패하면 일괄 처리의 어떤 것도 게시되지 않습니다. 파일을 읽기 때문에 원자적 문서 일괄 처리의 구성원이 될 수 없습니다. 새 원본 레이어와 메시 핸들은 작업의 최종 `affectedLayerIds/affectedObjectIds`에서 얻습니다. 원본 픽셀이 프로젝트에 기록되므로 나중에 다시 열 때 입력 파일에 의존하지 않습니다. 각 이미지는 작성된 리그 안에 자신의 메시를 받으며(`canvas_mesh_create`, 지정한 부모 아래), 생성기는 이를 다시 생성하지 않습니다. 실행 취소하면 가져오기가 취소됩니다.

레이어 전체는 `layer_transform`으로 변환합니다(가져온 것이든 PSD에서 온 것이든, 루트에 있든 어떤 디포머 아래에 있든, 바인딩 여부와 관계없이 모든 레이어). `state/layer_id`가 필요하며, 그다음 `matrix [a,b,c,d,e,f]`(`x' = a·x + c·y + e`, `y' = b·x + d·y + f`, 캔버스 단위, y 아래 방향) 또는 `translate [x,y]`, `scale`(숫자 또는 `[sx,sy]`), `rotate`(도, 화면에서 시계 방향), `pivot [x,y]` 중 하나를 줍니다. 레이어의 기존 변환과 합성됩니다. 픽셀, 아틀라스 타일, 텍스처 좌표는 바뀌지 않고 재생성도 일어나지 않습니다. 레이어의 각 메시는 기본 포즈에서 변환만큼 움직이고(키폼은 오프셋을 유지), 레이어는 픽셀이 표시되는 위치를 기록하며, 페인트도 이를 따릅니다(회전된 레이어도 마찬가지). 실행 취소할 수 있는 히스토리 커밋 하나이며, 원자적 문서 일괄 처리에 참여할 수 있습니다.

메시는 `layer_mesh_rebuild`로 다시 만듭니다(`state/layer_id` 필요). 레이어의 현재 픽셀과 메시 설정으로 메시를 다시 생성하고, 키폼, 경로, 가중치, Glue를 새 토폴로지로 이전하며, 전체가 변환된 레이어는 제자리에 유지합니다. 메시는 가져올 때, 이 명령으로, 페인트와 이미지 교체의 `rebuild_mesh` 옵션으로, 메시 설정을 바꿀 때, 그리고 재생성할 때만 다시 생성됩니다. 메시가 없는 레이어는 페인트가 처음으로 보이는 픽셀을 만들 때 메시를 받습니다. 메시 밖에 칠한 픽셀은 표시되지 않습니다. UI의 메시 패널은 그 비율을 보여 주고 **현재 픽셀로 재구성**을 제공합니다.

텍스처 구조를 따라 리메시하려면 `layer_mesh_structure`를 사용합니다(`state/target` 필요. target은 메시 id 또는 `mesh:<id>`. 선택 사항 `settings`: `sensitivity` 0–1, `min_length`는 원본 픽셀 단위, `near_density`는 유지되는 가장자리 행에 대한 선을 따른 간격(기본값 1), `fill`은 선 사이에 정점을 추가할지 여부(기본값 false: 가장자리 행과 선에만 정점이 있음), `far_density`는 이전 내부 변 길이에 대한 채우기 변 길이, `bands`는 그려진 주름 양쪽에 행을 하나씩 두고 가로대를 걸칠지 여부(기본값 false), `kinds`는 `FOLD`/`SHADE`/`PATTERN`의 부분 집합, `max_lines` ≤64, `max_vertices` ≤16000). 윤곽 행과 가장자리 띠는 유지되며, 내부는 텍스처에 그려진 주름, 음영 경계, 무늬 선을 따라 다시 만들어집니다. 키폼, 경로, 가중치, Glue는 `canvas_mesh_rebuild` 기록 하나를 통해 평소처럼 이전됩니다. 내부 효과가 있는 메시에서는 감지된 TOPOLOGY 가이드가 교체되고(그린 가이드와 FOLD로 표시된 가이드는 유지) 효과가 같은 히스토리 노드에서 다시 베이크됩니다(`interior_rebaked`). 메시화할 수 없는 영역이나 `max_vertices`를 넘는 결과는 아무것도 바꾸지 않고 실패합니다. `layer_mesh_structure_preview`(읽기 전용, `state`를 뺀 같은 인수)는 찾은 선(종류, `followed`, `banded`, 원본 픽셀과 캔버스 단위)과 새 메시의 캔버스 점과 삼각형을 반환합니다.

### 메시 토폴로지

`canvas_topology`(`request.mode`는 `topology`)는 편집기의 토폴로지 동작과 같은 `canvas_topology` 작성 저널 항목을 기록합니다. `state/id/action`이 필요합니다(`id`는 메시 ID. `vertices`는 현재 메시의 정점 인덱스이며 기본값은 없음). `anchors`의 각 항목은 기존 정점 `{vertex}`이거나 자유 점 `{x, y}`이며, `add_points`는 자유 점만 받습니다. 거부되면 `topology_refused`로 실패합니다. 인덱스는 현재 메시에서 오므로 최신 `state`를 사용하세요. `mesh_get_topology`는 정점 인덱스와 기본 위치(메시 자체 공간이며 `add_points` 앵커가 사용하는 공간), 삼각형, 윤곽 변을 읽습니다. 커밋하면 결과의 `new_vertices`가 동작이 만들었거나 선택된 상태로 남긴 정점을 새 메시의 인덱스로 나열합니다(`add_points`가 만든 점, `knife` 절단을 따라 생긴 점, `merge`가 만든 정점). 일괄 처리 구성원은 이를 반환하지 않습니다. `action`:

| `action` | 입력 | 결과 |
| --- | --- | --- |
| `merge` / `duplicate` / `delete` | `vertices` | 한 점으로 병합하거나, 정점과 그 정점들이 완전히 덮는 면을 복제하거나, 삭제하고 빈 곳을 채웁니다(정점이 세 개 이상 남아야 함) |
| `connect` | 정확히 두 개의 `vertices` | 두 정점 사이의 직선이 가로지르는 변을 분할하고 새 변으로 잇습니다. 이미 이어져 있거나 그 선을 따라 새 변을 자를 수 없으면 실패합니다 |
| `subdivide` / `split` | `vertices` 또는 `edges` | 선택한 정점이 걸치는 변, 또는 지정한 변을 세분화합니다 |
| `knife` | `anchors` | 폴리라인을 따라 변을 놓습니다(편집기의 메시 점 Line). 각 앵커는 메시 위에 놓이고 이웃한 두 앵커마다 변으로 이어집니다. 앵커는 메시 자체 기본 공간의 `{vertex}` 또는 `{x, y}`입니다 |
| `add_points` | `anchors`(`knife`와 마찬가지로 메시 자체 기본 공간의 `{x, y}`) | 점마다 정점 하나를 추가합니다. 삼각형 안이면 1→3, 변 위면 그 변을 분할하고, 메시 밖이면 가장 가까운 보이는 윤곽 변에서 삼각형 하나가 자랍니다. 기존 정점 위의 점은 무시됩니다. 새 정점의 UV, 키폼, 블렌드 셰이프, 가중치는 하나의 가중치 집합으로 주변 정점에서 파생되며, 그다음 텍스처 매핑이 그대로인 곳에서 변을 뒤집습니다 |
| `delete_edges` | `edges`(`[[a, b], …]`, 필수. `vertices`는 어떤 변을 지울지 결정하지 않음) | 내부 변은 그 사각형의 다른 대각선으로 바뀌고(사각형이 기본 상태, 어떤 키폼, 어떤 블렌드 폼에서든 볼록하지 않으면 실패), 윤곽 변은 그 삼각형과 더 이상 쓰이지 않는 정점을 함께 제거합니다. 어떤 정점이 두 부채꼴 사이에 끼이거나 삼각형이 하나도 남지 않게 되면 일괄 처리 전체가 실패합니다 |
| `retriangulate` | `vertices`(메시 전체는 빈 값) | 이 정점들 주변의 변을 Delaunay 쪽으로 뒤집으며(기본 포즈의 캔버스 공간에서 판정), 텍스처 매핑이 그대로이고 기본 상태와 모든 키폼 및 블렌드 폼에서 볼록한 사각형만 대상입니다. 정점과 키폼은 바뀌지 않습니다. 뒤집을 것이 없으면 변경 없음입니다(`applied:false`, 히스토리 없음) |

적용되지 않는 동작은 오류를 반환하고 아무것도 기록하지 않습니다. 키폼, 블렌드 셰이프, 정점 그룹, Glue는 토폴로지와 함께 이전되며(삭제된 정점의 Glue 쌍은 제거됨) 경로는 다시 바인딩됩니다. 편집 전에 메시와 맞았던 내부 효과가 메시에 있으면, 같은 노드에서 새 메시에 효과를 다시 베이크합니다(베이크에 실패해도 편집은 거부되지 않으며, 효과가 메시가 바뀌었다고 보고합니다). 결과는 [기하 검사](#기하-검사와-시험-실행)를 거칩니다. 알고리즘과 Cubism 메시 편집과의 비교는 [메시 토폴로지](../spec/MESH_TOPOLOGY_AND_SPLIT.md#수동-토폴로지-편집)를 참고하세요.

### 오브젝트 복사, 찾아 바꾸기, ID 변환

`object_paste`(`mesh:/warp:/rotation:/part:/parameter:` 핸들로 된 `sources`. 선택 사항 `parent`, `part`, `parameter_group`, `ids`)는 메시, 디포머(하위 트리와 그 메시 포함), Part(자식 포함), 파라미터를 복사합니다. 새 ID는 편집이 기록될 때 고정되므로(기본값은 `<id>_copy`, 충돌하면 번호가 붙고, `ids`로 소스별로 지정 가능) 재생해도 같은 ID가 나옵니다. 복사본들 사이의 부모, 마스크, Glue, 경로, 정점 그룹은 복사본을 가리키고, 복사되지 않은 오브젝트에 대한 참조는 그대로 유지됩니다. `parent`를 주면 메시는 캔버스 공간에서 다시 바인딩되고 디포머는 로컬 공간에서 이동합니다. 붙여넣기는 작성된 리그 위에서 계획됩니다. 흔들림, 시뮬레이션, 내부 효과가 생성하는 오브젝트는 거기에 없으므로, 그런 오브젝트를 지정하면 거부되고 복사된 하위 트리에서도 빠집니다. 복사본은 그 레이어의 메시가 아닙니다. 레이어의 메시를 조회하면(선택, 그리고 분할과 삭제가 유지하는 ID) 원본만 찾으며, 레이어를 삭제하면 그 복사본도 함께 삭제됩니다. 복사본은 자기 메시 ID로 그리기 순서를 취하며, 레이어 변환은 복사본을 움직이지 않습니다. 단독으로 호출하면 결과의 `ids`가 각 소스 핸들을 기록된 그대로의 새 ID에 대응시키고(요청은 캡처한 버전에서 모든 새 ID를 고정해 커밋됨), `paths`는 메시와 함께 복사된 변형 경로를 대응시킵니다(`path:<old id>`에서 새 ID로). 일괄 처리 구성원이라면 `changed`에서 새 핸들을 읽으세요.

`mesh_duplicate`(`source`, `targets`)는 소스 메시의 토폴로지를 캔버스 위치에 맞춰 각 대상 메시에 덮어씌우며, UV는 대상 자체의 텍스처에서 가져옵니다. 각 대상은 `canvas_mesh_rebuild` 기록 하나를 받으므로 키폼, 경로, 가중치, Glue는 평소처럼 이전됩니다.

`object_find`(읽기 전용)는 `kinds` 중에서 이름이나 ID(`field`)를 검색하며, `find`/`replace`, `regex`, `match_case`, `prefix`, `suffix`, `swap_sides`(독립된 단어로 쓰인 L/R, Left/Right, 左/右를 맞바꿈), `standard_ids`(Cubism 2 스타일 ID를 Cubism 3 표준 ID로)를 지원하고, 일치 항목마다 변경 전후를 반환합니다. `object_rename`은 같은 인수를 한 번에 커밋합니다. 둘 다 작성된 리그의 오브젝트만 나열하므로, 저널 이후에 생성기가 추가하는 것은 생성기가 준 이름과 ID를 유지합니다. ID 이름 변경과 `object_convert_id`(`renames`: `target`과 새 `id`)는 `convert_id` 저널 항목 하나를 기록합니다. 키폼, 블렌드 셰이프, 마스크, Glue, 경로, 정점 그룹, 모션 커브, 표정, 포즈, 히트 영역, 사용자 물리, 흔들림 대상이 따라오며(아직 문서에 들어오지 않은 표준 표정은 새 ID로 표준 파라미터를 찾음. 앞머리 위 복사본은 원본에서 ID를 가져오며, 그 자체는 변환, 붙여넣기, 메시 복제에서 `generated_copy`로 거부됨), 그 뒤의 생성 재정의와 지연된 패널 편집은 새 ID로 재생되고, 내장 물리, 모션 프리셋, 표정은 새 ID로 표준 파라미터를 다시 찾습니다. 파라미터 스냅숏, 각 작업 공간의 작성 포즈, 타임라인 북마크, shy 트랙과 고정 트랙은 같은 커밋에서 새 ID를 취하고, 실행 취소하거나 변환 이전으로 체크아웃하면 예전 ID로 돌아갑니다. ID는 문자로 시작하는 1–64자의 ASCII 영문자, 숫자, 밑줄이어야 합니다. 같은 종류 안의 중복, 이미 쓰인 ID, 생성기가 소유한 오브젝트(시뮬레이션, 내부 효과, 흔들림 파라미터)는 아무것도 바꾸지 않고 거부되며, 뼈대가 바인딩하거나 가중치를 준 메시, 뼈 파라미터, 뼈 디포머도 마찬가지입니다(뼈대는 저널 이전에 원래 ID로 베이크됨). 또한 뼈대는 ID가 변환되었거나 저널이 만든 메시(붙여넣은 복사본, 캔버스에서 만든 메시)를 새로 바인딩할 수 없으며, 자동 뼈대는 이를 제외합니다. 삭제된 파라미터의 남은 모션 커브나 표정 항목이 여전히 가리키는 ID로 변환하면 거부되며, 그 모션이나 표정을 알려 줍니다. 분할, 레이어 삭제, 재생성은 기본 단계에서 메시를 변환 전 ID로 유지하고 저널 항목이 이름을 바꾸게 합니다. 재생성의 3방향 병합도 생성된 리그를 변환된 ID에 대응시킵니다. 내보내기는 실제로 쓰는 모델의 ID를 확인합니다. 한 ID 아래에 같은 종류의 오브젝트가 둘 있으면(Warp, Rotation, 런타임의 가상 뼈는 하나의 ID 공간을 공유하며, 파라미터 폴더, 시뮬레이션, 충돌체도 포함) 내보내기를 거부하고, 허용 문자 밖의 ID는 경고만 합니다.


### 텍스처와 아틀라스

레이어의 캔버스 사각형(캔버스 단위, 소수 허용)과 래스터 픽셀은 서로 독립적입니다. 고유 밀도는 캔버스 단위당 래스터 픽셀 수이며, 아틀라스 타일은 `raster pixels × density × fit`을 저장합니다. 밀도의 기본값은 1입니다. 예산을 넘으면 잠기지 않은 타일이 하나의 fit(≤1)을 공유해 함께 줄어들고, 맞출 수 없는 잠금과 고정은 해제됩니다(설명은 도구 결과가 아니라 프로그램 로그에만 기록됨). 기하는 아틀라스를 알지 못합니다. 밀도, 예산, 고정, 재패킹은 타일과 바인딩된 UV만 바꾸며 메시는 바꾸지 않습니다.

- `layer_get_texture`(조회): `layer_id`. `canvas_rect`, 정수 경계 `bounds`, `raster`, `native_density`, `override{density,lock,pin}`, `deleted`, `tile`(페이지, 텍스처 픽셀 사각형, 래스터 픽셀당 텍스처 픽셀 `scale_x/scale_y`, 밀도, 잠금과 고정. 투명하거나 삭제된 레이어는 null), `atlas_fit`을 모두 같은 캡처 버전에서 반환합니다.
- `atlas_get`(조회): 선택 사항 `page`로 타일을 거릅니다. 유효한 `budget{page_size,max_pages,padding}`, `fit`, `notices`, `auto`(매번 자동으로 패킹하는지), `pages[{index,width,height,tile_count,occupancy}]`, `tiles`를 반환합니다. 메시로 패킹된 타일의 `occupancy`는 메시가 실제로 쓰는 셀만 셉니다. `notices`는 예산을 넘는 페이지 수와, 페이지에 맞추기 위해 설정한 밀도보다 낮게 저장된 타일을 알려 줍니다. 저장된 배치는 자기 `fit`을 유지하므로, 더 높은 해상도의 이미지로 바꾼 뒤에는 두 경우 모두 `fit`에 나타나지 않습니다. 텍스처 편집과 이를 포함한 `workspace_apply_edits` 일괄 처리의 결과에도 `atlas_fit`과 `notices`가 붙습니다. `atlas_set_budget`에 `atlas_pack`을 더하면(교체와 같은 일괄 처리에 넣을 수 있음) 예산 안으로 되돌아옵니다.
- `atlas_render_page`(읽기 전용 백그라운드 작업, `request_id/project_id/state` 필요): `page`, 선택 사항 `max_size`(64–16384, 기본값 2048, 긴 변 기준으로 축소). 최종 상태에는 `revision`, 원래 크기와 렌더링 크기, `sha256`, 그 페이지의 레이어가 들어 있습니다. PNG는 이미지 콘텐츠로 돌아오며, `job_get/job_wait`로 반복해서 가져올 수 있습니다.
- `layer_set_canvas_rect`: `layer_id`, `rect{left,top,width,height}`. 캔버스가 그 사각형 안에 레이어의 픽셀을 표시하도록 레이어를 이동하고 크기를 조정합니다. 즉 현재 표시 위치에서 시작하는 `layer_transform` 하나입니다(회전된 레이어는 거부되므로 `layer_transform`을 사용). 픽셀, 텍스처, 생성 입력은 바뀌지 않으며, 메시는 키폼과 함께 이동합니다. 모든 레이어에서 동작합니다. `layer_get_texture`의 `canvas_rect`가 레이어가 현재 표시되는 위치입니다. 같은 사각형이면 변경 없음입니다.
- `layer_replace_image`: `layer_id`와 함께 `path`(절대 경로, PNG/WebP/TIFF/BMP, 단일 호출 전용), `png_base64`, `material_id`(라이브러리의 소재. 레이어가 그 소재에 연결됨) 중 하나, 그리고 선택 사항 `fit`(기본값 `stretch`는 꽉 채움. `contain`은 종횡비를 유지하고 중앙에 놓은 뒤 투명으로 채움)과 `rebuild_mesh`(기본값 false). 캔버스 사각형은 바뀌지 않고, 생성 입력은 첫 교체 이전의 픽셀로 고정되며, 메시, 키폼, 바인딩은 유지되고, 타일과 UV만 바뀝니다. `rebuild_mesh=true`는 대신 새 픽셀로 레이어의 메시를 다시 생성하며, 레이어에 작성된 편집이나 구체화된 기하가 있으면 거부됩니다. 단일 호출은 파일을 읽기 전에 상태를 확인합니다. 일괄 처리 구성원은 `png_base64`만 받으며, `path`를 주면 일괄 처리 전체가 `invalid_edit`로 실패합니다. 최대 1,600만 픽셀이며, 픽셀이 동일하면 변경 없음입니다.
- `atlas_check_placement`(조회): `placements`는 1–128개의 `{layer_id, page?, x?, y?, rotation?, density?}`이며, 생략한 필드는 커밋된 값을 유지합니다. `clear`와 타일별 목표 `page/x/y/width/height/rotation`, `outside_page`(회전된 경계 상자가 페이지를 벗어나거나 페이지 번호가 예산을 넘는지), `overlaps`(그 메시가 교차할 타일)를 반환합니다. 커밋된 다른 타일과 함께 검사하는 타일을 대상으로, 커밋할 때와 정확히 같은 규칙으로 비교합니다. 두 타일의 경계 상자를 패딩만큼 키웠을 때 교차하는 경우에만, 메시가 실제로 쓰는 셀(패딩만큼 키움)을 비교합니다. 둘 다 메시가 없고 회전되지 않았다면 사각형을 비교합니다. 아틀라스 뷰도 드래그, 크기 조정, 회전할 때 같은 규칙을 씁니다. `clear`인 배치는 항상 지정한 그대로 놓이며, 충돌하는 배치는 아래 명령들이 거부합니다.
- `layer_set_pixel_density`: 중복 없는 1–128개의 `layer_ids`, 필수 `density`(1/64–16의 연속값, `null`은 1로 복원), 선택 사항 `lock`(생략하면 현재 값 유지). 기존 배치가 있으면 각 타일은 왼쪽 위 모서리를 유지합니다. 커지면서 페이지 밖으로 나가거나 다른 타일의 메시에 들어가게 되면 명령은 `tile_collides`로 거부되고 아무것도 바뀌지 않습니다.
- `atlas_set_tile`: `layer_id`와 `pin{page,x,y,rotation?}`(텍스처 픽셀. `x`, `y`는 회전하지 않은 사각형의 왼쪽 위이고, `rotation`은 타일 중심을 축으로 한 임의의 각도이며 기본값 0) 또는 빈 공간에 놓으려면 `null`. 페이지 번호는 예산 안이어야 합니다. 위치는 배치(`atlasArrangement` 설정)에 저장됩니다. 자동 패킹된 아틀라스는 먼저 현재 레이아웃을 저장하며(`auto`가 false가 됨), 기존 배치가 있으면 각 타일을 먼저 현재 위치에 메시 커버리지를 채워 저장하므로 다른 타일은 움직이지 않고 비워진 공간으로 밀려 들어가지도 않습니다. 회전 후 페이지를 벗어나거나 다른 타일의 메시와 교차하는 위치는 `tile_collides`로 거부되고 아무것도 바뀌지 않습니다. 먼저 `atlas_check_placement`로 확인하세요. 이동과 크기 조정을 한 번에 하려면(예: 반대쪽 모서리를 고정한 크기 조정, 두 타일 맞바꾸기) `layer_set_pixel_density`와 `atlas_set_tile`을 같은 `workspace_apply_edits`에 넣으세요. 아틀라스 뷰의 조정 세션에서 **적용**할 때와 정확히 같이, 일괄 처리 전체가 끝난 뒤의 레이아웃만 검사합니다.
- `atlas_set_budget`: `page_size`(256–16384의 2의 거듭제곱), `max_pages`(1–64), `padding`(0–32), `auto`. 하나 이상 지정해야 하며, 생략한 값은 현재 값을 유지합니다. 예산은 `atlas` 설정에 기록됩니다. `auto=true`는 저장된 배치를 삭제하며, 그 뒤로는 다시 빌드할 때마다 사각형(MaxRects)으로 자동 패킹합니다. `auto=false`는 현재 레이아웃을 저장합니다.
- `atlas_pack`: 한 번 패킹하고 결과를 저장합니다(`auto`가 false가 됨). 선택 사항 `shape`: `mesh`(기본값)는 각 레이어의 최종 메시가 덮는 텍스처 좌표 영역(래스터 픽셀 4개 단위의 셀과 패딩)으로 패킹하므로 사각형끼리 겹쳐 넣을 수 있고 각 타일은 자기 셀만 씁니다. `rect`는 사각형으로 패킹합니다. 타일은 점유 면적이 큰 것부터 작은 것 순으로 배치되며, 예산 페이지에 들어가지 않으면 잠기지 않은 모든 타일이 1/64 단위로 함께 줄어듭니다. 선택 사항 `layer_ids`는 그 타일만 옮기고 나머지는 위치와 fit을 유지합니다. 자동 패킹의 고정과 더 이상 없는 레이어의 재정의도 지웁니다. 같은 입력을 다시 패킹하면 변경 없음입니다. 나중의 메시 편집이 패킹 당시의 커버리지를 넘어서면 `atlas_get`의 `notices`가 다시 패킹하도록 제안합니다. 타일의 `shaped`는 메시 커버리지로 패킹되었다는 뜻입니다.

여섯 가지 편집은 모두 프로세스 작업이자 원자적 일괄 처리 구성원이며, `WorkspaceTextureEdits` 순수 후보, 전체 다시 빌드, CAS를 거쳐 커밋됩니다. 한 번 성공하면 히스토리 노드가 정확히 하나 추가되며, 실행 취소하면 이전 타일이 복원됩니다. 최종 상태는 `project_id/state/history_node_id/revision/applied/layers/atlas_fit`이며(`geometry_diagnostics`가 붙을 수 있음), `layers`는 텍스처가 실제로 바뀐 레이어입니다.


## 프로젝트와 내보내기 작업

새로운 대화형 업무 진입점도 `workspace_get_operation`에서 완전한 스키마를 얻습니다.

| 도구 | 공통 동작 |
| --- | --- |
| `preview_pose` | 작성 포즈와 선택한 모션의 자동 키 설정을 같은 CAS에서 처리합니다. 일시적인 재생 프레임은 이 편집을 일으키지 않습니다 |
| `preview_playback / preview_playback_get / preview_physics` | 프로세스가 소유하는 재생, 한 번 재생 모션, 애니메이션, 마우스 추적, 물리 평가. 일시적인 프레임은 프로젝트 포즈나 히스토리를 기록하지 않습니다 |
| `canvas_deform_stroke` | brush/smooth/inflate, 여러 메시, 연결성 제한, Glue 보존. 샘플은 캔버스 픽셀이며, edit는 이미지를 유지하고 deform은 정확한 키폼을 기록합니다. 획 전체가 한 번에 커밋됩니다 |
| `canvas_glue_edit / vertex_group_paint` | Glue 브러시, 방향성 가중치, 바인딩 해제, 다시 연결, 시뮬레이션 가중치 브러시/그라데이션/반전. 연속적인 Glue 순서와 애니메이션 채널을 유지합니다 |
| `layer_draw_order` | `target`에 0–1000의 명시적인 그리기 순서를 설정합니다. `order:null`은 원래 생성된 순서나 작성된 애니메이션 순서로 초기화합니다 |
| `hit_area_put / hit_area_delete / hit_area_preset` | 문서의 히트 영역(`rigEdits.hitAreas`)으로, 인스펙터와 계층 컨텍스트 메뉴가 공유하는 일괄 처리 가능한 문서 편집입니다. `put`은 영역 `id`(Cubism ID 문자 집합, 예: `HitAreaHead`)를 만들거나 바꿉니다. `meshes`는 `mesh:<id>` 핸들 목록이며(새 영역에는 필수, 지정하면 목록을 교체), `name`은 호스트가 요청하는 이름(기본값은 `HitArea`를 뺀 ID), `new_id`는 이름 변경, `before_id`는 다른 영역 앞에 배치합니다(`null`: 마지막). `delete`는 `id`를 제거합니다. `preset`은 의미 정보에서 Head(얼굴)와 Body(상의, 없으면 목이나 하의)를 추가하며, 이미 그 ID를 가진 영역은 유지합니다. 영역은 목록 순서대로 검사되고 처음 맞은 것이 이깁니다. 내보내기는 model3.json `HitAreas`(항목마다 ArtMesh 하나이므로 메시 여러 개로 된 영역은 같은 이름의 항목 여러 개가 됨)와 `.p2lrt` `HITA` 청크를 씁니다. 설정된 영역이 없으면 런타임은 자동 Head/Body 영역을 사용합니다. 단일 호출의 `changed`는 건드린 영역을 `hit_area:<id>`로 나열합니다(이름이 바뀐 영역은 두 ID 모두, `preset`이 추가한 영역) |
| `hit_area_list / hit_area_test` | 커밋된 캡처 하나에 대한 조회입니다. `list`는 검사 순서대로 영역과 존재하는 메시, 리그에 더 이상 없는 `missing` 메시를 반환하며, 설정된 것이 없으면 `automatic=true`입니다. `test`는 선택 사항 `pose`에서 런타임의 규칙(점을 포함하는 삼각형이 있고 불투명도가 0보다 큰 보이는 메시)으로 캔버스 점 `x, y`(캔버스 단위, y 아래 방향)를 히트 테스트하고 `hit`과 영역의 `id/name/meshes`를 반환합니다. 모델에 없는 파라미터를 지정한 `pose`는 거부되며, 불러온 프로젝트가 없으면 `invalid_state`로 실패합니다 |
| `warp_get_controls` | 커밋된 캡처 하나에서 베지어 분할 수와 편집 컨트롤을 읽습니다. `detail=summary`에는 제어점이 없고, `points`는 앵커, 접선 순서로 페이지를 나눕니다(페이지당 최대 256). 좌표는 부모 로컬 공간이며, `persisted=false`는 샘플링된 격자에서 컨트롤을 재구성했다는 뜻입니다. CMO3는 샘플링된 기하와 분할 정보만 저장하며 이 편집 컨트롤은 저장하지 않습니다 |
| `mesh_get_topology` | 커밋된 캡처 하나에서 메시 하나의 토폴로지(`target`은 `mesh:ID`)를 `canvas_topology`가 인덱스로 부르는 형태로 읽습니다. `section`은 `vertices`(기본값, 메시 자체 기본 공간의 `[index, x, y]`), `triangles`(`[index, a, b, c]`), `outline`(삼각형 하나만 쓰는 윤곽 변, `[a, b]`) 중 하나를 고르며, `offset`/`limit`(1–4096, 기본값 1024)로 `items`에 페이지를 나눕니다. 정점, 삼각형, 윤곽 변의 수와 `bounds`는 항상 돌아옵니다. 인덱스가 계속 유효하도록 반환된 `state`를 `canvas_topology`에 넘기세요 |
| `warp_set_topology` | 모든 일반 키폼과 블렌드 키폼을 백그라운드로 다시 샘플링하고, 부모 로컬 공간에서의 실제 곡면 샘플링 오차를 반환합니다 |
| `warp_bezier_divisions / warp_bezier_anchor / warp_bezier_handle / warp_bezier_reset` | 분할 수, 앵커, 접선을 백그라운드로 저장하거나 초기화합니다. 컨트롤 좌표는 부모 로컬 공간입니다. 완전한 편집 컨트롤과 샘플링된 격자가 저장됩니다 |
| `swing_preview / swing_preview_get / swing_preview_render / swing_preview_commit` | 비공개 흔들림 초안, 시험 재생 시계, PNG 관찰. 확정하면 히스토리 커밋 하나이며, 취소하면 초안을 게시하지 않습니다 |
| `skeleton_draft_open / skeleton_draft_list / skeleton_draft_get / skeleton_draft_edit / skeleton_draft_preview_transfer / skeleton_draft_commit / skeleton_draft_cancel` | 뼈 편집 도구와 공유하는 비공개 뼈대 초안입니다. 열면 자체 포즈 CAS를 가진 기본 포즈로 돌아가며, 반환된 `state`는 초안 계보입니다. `edit`는 1–128개의 유형 있는 의도(일괄 변환, 복제/미러, 세분화/해제, 꼬리/날개 체인, 유연한 뼈 체인 `flex`(갈래는 유연한 자식 뼈 여러 개를 가진 유연한 뼈 하나)와 `straighten`(먼저 관절을 그림의 중심선에 맞추고 기본 상태에서 곧게 하며, 그림은 포즈로 저장), 가중치 칠하기/정리/지우기/전송, `revert` 등)로 초안을 전부 바꾸거나 전혀 바꾸지 않습니다. `commit`은 그 계보에서만 CAS하므로, 이후의 포즈/문서 변경, 프로젝트 다시 열기, 새 초안은 모두 충돌합니다. 뼈대가 바뀌지 않았으면 히스토리 노드를 만들지 않습니다 |
| `physics_audition / physics_audition_step / physics_audition_get` | 물리 패널의 진자와 공유하는, 선택한 그룹의 시험 재생 세션이며 문서, 히스토리, 저장된 포즈를 바꾸지 않습니다. `start`는 작업 공간의 작성 포즈(선택적으로 `values`를 겹침)로 `group_id`를 구동하고 그 작업 공간의 이전 세션을 끝냅니다. `target`은 진자 루트를 -1…1로 드래그하며, 그 밖에 `release/reset/reset_peaks/stop`이 있습니다. `step`은 dt ≤ 0.1초로 1–240 스텝을 진행하며 복사본에서 풀고, 거부된 요청은 앞부분도 남기지 않습니다. `get`은 시계를 진행하지 않습니다. 같은 불러오기 안의 문서 편집은 그룹을 갱신합니다. 프로젝트를 다시 열거나 그룹을 삭제하면 세션은 `stale`이 됩니다. 반환된 `peaks`는 `physics_fit`의 `observed_peaks`로 바로 넘길 수 있습니다 |
| `canvas_visibility / canvas_visibility_get` | `workspace_id/canvas_id/mode`(edit/preview)로 지정하는 로컬 레이어/디포머 가시성과 솔로로, 계층의 눈 아이콘 및 솔로와 처리기를 공유합니다. `action`은 `layers/deformers/all_layers/invert_layers/solo/unsolo`입니다. 변경은 `state`를 진행하지만 문서, 히스토리, 다른 캔버스, 내보내기는 바꾸지 않으며, 변경이 없으면 진행하지 않습니다. 조회는 각 캔버스 세션의 가시성, 솔로 기록, `hidden_layer_ids`를 반환하며, 한 번도 편집하지 않은 캔버스는 모두 보입니다. 내보내기 가시성은 여전히 `object_edit_appearance`로 바꿉니다 |
| `physics_preset_list / physics_preset_put / physics_preset_rename / physics_preset_delete / physics_apply_preset` | 안정적인 ID를 가진 전역 입력/진자 프리셋 라이브러리. 라이브러리 변경은 `library_state`를 사용하며, 적용은 일괄 처리 가능한 문서 편집입니다 |
| `simulation_preview / simulation_preview_step / simulation_preview_get / simulation_preview_render` | 비공개 실시간 시뮬레이션 장면. 시작/재시작/정지와 명시적 dt 스텝 진행은 백그라운드 세션이며, 읽기/렌더링은 시계를 진행하지 않습니다 |
| `paint_session_begin / paint_session_list / paint_session_control / paint_session_commit` | 실행 취소/다시 실행/이동/색 추출/PNG를 갖춘 비공개 다중 획 초안입니다. 제스처와 색 추출은 캔버스 단위를 사용하며, 초안은 레이어 자체의 래스터(`canvas_rect`를 덮는 `width/height` 픽셀)입니다. 확정하기 전에는 프로젝트 픽셀이 바뀌지 않으며, 확정은 히스토리 커밋 하나입니다. `paint_session_begin`에 (`layer_id` 대신) `material_id`를 넘기면 라이브러리 항목 자체의 픽셀에 칠합니다. 결과에 `material_id`가 붙고, 커밋된 다른 편집은 세션에 영향을 주지 않으며, 그 소재의 픽셀이 바뀔 때만 세션이 무효화됩니다. 커밋은 `material_set_pixels` 하나로 기록되고, 그 소재를 표시하는 레이어가 함께 갱신되며, `rebuild_mesh`는 적용되지 않습니다. `paint_session_control`의 `select` 동작은 세션의 선택 영역을 바꿉니다(`rectangle` / `ellipse` / `lasso` / `wand` / `quick`(`points`와 `radius`로 된 브러시 획이 비슷한 색을 따라 가장자리까지 번짐, `tolerance` 기본값 32) 또는 `opaque`(레이어의 알파)를 `op` replace, add, subtract, intersect로 조합하거나, `all` / `none` / `invert` / `feather` / `expand` / `contract`이며 마지막 세 개는 `radius`를 받음). 이후의 제스처와 획은 그 안에만 그려집니다. 선택 영역은 획 히스토리에 들어가지 않으며, `session.selection`은 그 래스터 경계 상자입니다. 제스처에는 `transform`(캔버스 단위의 `matrix` `[a,b,c,d,e,f]`가 선택 영역의 픽셀(선택 영역이 없으면 레이어의 내용)과 선택 영역을 함께 옮김. `smooth:false`는 최근접 샘플링), `blur`(`opacity`가 강도), `fill`(선택 영역, 없으면 레이어 자체 영역과 칠한 부분), `gradient`(`end_color`, `kind: linear / radial`)도 있습니다. 페인트 통은 `contiguous:false`를 받으며, `eraser` / `clear`를 제외한 모든 제스처는 이미 칠한 픽셀만 바꾸는 `lock_alpha`를 받습니다(배경색은 대신 브러시로 칠하세요). `brush` / `pencil` / `eraser` / `blur`는 `symmetry`(`vertical` / `horizontal` / `both`)를 받아 편집기의 대칭 페인트처럼 같은 획 안에서 캔버스 중앙을 기준으로 획을 미러하며, 겹치는 부분은 한 번만 칠해집니다. 올가미는 최대 4096개의 점을 받습니다 |
| `motion_create / motion_duplicate / motion_rename / motion_properties / motion_pose / motion_move_keys / motion_delete_keys / motion_paste_keys / motion_replace_keys / motion_preset / motion_close_loop` | 모션 수명 주기, 포즈 키 설정, 키 이동/삭제/붙여넣기/교체, 생성 프리셋. 모두 원자적 일괄 처리에 참여할 수 있습니다. `motion_close_loop`는 나열된 커브(기본값은 전체)를 이음매 없는 루프로 닫습니다. 0과 끝에 키를 두고, 끝은 시작의 값을 취하며(반복 파라미터는 진행 방향으로 몇 바퀴 돌았는지를 유지하고 64바퀴를 넘으면 실패), 끝으로 들어가는 구간이 이징되는 곳에서는 시작의 기울기로 이징합니다. `id` 없이 호출한 `motion_create` / `motion_duplicate`는 타임라인 보조 데이터가 남아 있는 삭제된 모션의 ID를 건너뜁니다. `repeat: true`로 만들거나 갱신한 파라미터의 키는 범위를 넘어 최대 64바퀴까지 갈 수 있으며, 키를 잡은 그대로 내보내고 런타임이 감쌉니다(Cubism 5.3 대상 필요. 이전 대상에서는 범위를 넘어 키가 잡힌 각 모션 커브가 손실 보고서에 나열되고 런타임이 클램프함). `cmo3`는 `isRepeat`를 받습니다 |
| `motion_set_event / motion_delete_event / motion_events` | 모션 이벤트(motion3 `UserData`)를 넣고, 옮기고(`from_time`), 삭제합니다. `motion_events`는 읽기 전용으로 모든 이벤트와 메시의 사용자 데이터를 나열합니다 |
| `expression_create / expression_put / expression_update / expression_set_entries / expression_capture / expression_delete / expression_duplicate / expression_move / expression_reset / expression_list / expression_get` | 표정(exp3): 포즈, 스냅숏, 현재 파라미터에서 생성, 항목 블렌드 `ADD` / `MULTIPLY` / `OVERWRITE`, 페이드와 순서. 편집은 원자적 일괄 처리에 참여할 수 있고 기록하는 항목만 검사하며, 읽기 결과의 `missing`은 리그에 더 이상 없는 파라미터를 나열합니다 |
| `pose_put / pose_add_group / pose_set_group / pose_delete_group / pose_update / pose_clear / pose_get` | 포즈(pose3) 그룹, 연결된 파츠, 전환 페이드. 편집은 원자적 일괄 처리에 참여할 수 있고 추가하는 파츠만 검사하며, `pose_get`의 `missing_parts`는 리그에 더 이상 없는 파츠를 나열합니다. 그룹은 인덱스로 지정하며, 하나를 삭제하면 뒤의 그룹이 앞으로 당겨집니다. 한 일괄 처리 안에서는 삭제하기 전에 변경하거나, 가장 큰 인덱스부터 삭제하세요 |

제어 요청도 변경 작업의 중복 제거와 작업 공간 토큰 규칙을 따릅니다. 세션은 자체 토큰을 가질 수 있으며, 전역 물리 라이브러리는 별도의 라이브러리 토큰을 사용합니다. 읽기, 렌더링, 재생 프레임, 비공개 초안은 문서 커밋이 아닙니다. `kind/job_backed/batchable` 탐색 메타데이터로 실행 방식을 구별합니다.

GUI와 MCP의 프로젝트 저장과 열기는 같은 애플리케이션 계층 수명 주기 인터페이스를 호출하며, 데스크톱 어댑터가 가진 프로젝트 컨트롤러가 실행합니다. GUI 진입점은 먼저 사용자에게 저장하지 않은 변경을 확인받은 뒤 버리기 플래그를 명시적으로 넘길 수 있습니다. MCP는 확인 대화 상자를 호출하지 않으며, 기본적으로 저장하지 않은 변경이 있으면 전환을 거부합니다. GUI 호출은 시작할 때의 프로젝트와 상태를 유지하며 신뢰된 사용자로서 커밋합니다.

GUI 필드 완성은 애플리케이션 계층 대기열에서 비동기로 다시 빌드되고 커밋되며, 저장은 먼저 대기 중인 완성을 기다립니다. 거부된 초안이 저장을 통해 프로젝트를 조용히 덮어쓸 수는 없습니다. 대기열은 자기 자신의 연속 커밋만 이어 가며, 외부 변경이나 프로젝트 다시 열기는 여전히 이전 상태를 무효화합니다. 조회는 계속 커밋된 캡처를 읽으며, 끝나지 않은 UI 초안을 게시하지 않습니다. `workspace_inspect`와 뼈대/모션 복합 조회는 별도의 세션 하나를 사용하므로, 반환되는 상태, 모델, 설정, 히스토리는 같은 캡처에 속합니다. 조회 중의 커밋이나 다시 열기는 그 결과에 섞이지 않습니다. `scope=preview`는 커밋된 포즈/잠금을 읽으며, 애니메이션 평가 프레임과 커밋되지 않은 GUI 값은 그 포즈를 대체하지 않습니다. 목록 탐색은 `items/total`과 해당하는 경우 `next`를 반환합니다. inspect 결과에는 `scope`가 있고, 대상 상세는 항상 `object`이며, 빈 페이지도 scope를 구별합니다. `scope=settings`는 기본값을 채운 저장된 생성 설정과 문서의 메시 재정의를 반환합니다. 예전 확장 필드는 프로젝트 문서에 계속 저장됩니다.

`workspace_apply_edits`, `project_import_psd`, `project_create_artwork`, `project_import_cmo3`, `project_open`, `project_save`, `project_save_as`, `project_export_model`, `project_export_target`, `project_export_psd`는 `{"ok":true,"operation":"...","data":{"id":"job-...","status":"..."}}`를 반환합니다. 결과는 `job_wait`나 `job_get`으로 `data.result`에서 읽습니다. 프로젝트 작업은 `project_id`, `state`, `history_node_id`를, 내보내기는 파일과 경고를 반환합니다. 저장이 끝난 뒤의 늦은 취소는 completed와 기록된 결과를 유지하며, 아카이브 교체 전의 취소는 이전 파일을 유지합니다. 작업은 앱이 소유하므로 연결이 끊겼다가 다시 연결해도 조회할 수 있습니다. 앱을 다시 시작한 뒤에는 복원되지 않습니다.

이 작업들의 기능 상세에는 완료된 업무 결과를 설명하는 `job_result_schema`도 포함됩니다. 시작과 조회의 outputSchema도 작업 상태를 설명합니다. `queued/running/cancelling`은 terminal이 false이고 result/error가 없습니다. `cancelled`는 result/error가 없고, `failed`는 구조화된 오류가 필요하며, `completed`는 작업에 맞는 결과가 필요합니다. 작업 목록은 `items/total`과 해당하는 경우 next를 반환하며, 성공 결과는 생략하지만 실패 오류는 유지합니다. 완료 결과와 영구 완료 표시는 같은 업무 검증을 사용하며, 커밋 후의 취소도 실제 성공을 유지합니다.

`settings_update/layer_mesh_update/layer_classify`도 마찬가지로 프로세스 작업 핸들을 반환하며 `request_id/project_id/state`가 필요합니다. 새 상태와 히스토리 노드는 `job_get/job_wait`의 `data.result`에서 얻습니다. 세 작업은 별도의 애플리케이션 명령을 공유합니다. 준비와 다시 빌드는 진행 상황을 보고하며, 커밋 전의 취소나 동시 충돌은 원래 문서와 히스토리를 유지합니다. CAS 이후에는 늦은 취소, UI 새로 고침 실패, 원래 요청의 재시도가 완전한 성공 최종 상태를 유지합니다. 생략한 분류 필드는 시작 상태 확인 후 캡처한 모델에서 병합됩니다. 변경이 없으면 상태를 진행하지도 히스토리를 추가하지도 않습니다. 연속적인 GUI 드래그와 분류 텍스트 입력은 세션이 끝날 때 한 번 커밋되며, 임시 단일 레이어 메시 미리보기는 정식 영구 후보를 대체하지 않습니다.

PSD 가져오기와 그림으로 프로젝트 만들기는 별도의 애플리케이션 계층 가져오기 모듈을 공유합니다. `project_import_psd`는 절대 `path`가 필요합니다. `project_create_artwork`는 캔버스 `width/height`와 1–32개의 `layers`를 주며, 각 레이어에는 절대 `path`와 `name`이 필요하고, 선택 사항으로 정수 캔버스 위치 `x/y`와 열거형 분류 `role/side`를 줄 수 있습니다. 좌표와 열거형에 대해 공개된 스키마는 실제 검증과 일치합니다. 둘 다 빈 작업 공간에서 만들거나 불러온 프로젝트를 전환하며, 기본적으로 저장하지 않은 변경이 있으면 거부합니다. 먼저 저장하거나 `discard_unsaved:true`를 명시적으로 주세요. 상태는 읽기 전에 캡처됩니다. 다시 빌드한 뒤 새 프로젝트 ID/불러오기 세대가 설치되며, 이전 리그/저널, 레이어/메시/그리기 순서 재정의, 보조 데이터는 새 원본 그림으로 넘어가지 않습니다. 성공한 작업 결과에는 `layers`에 새 원본 레이어 ID도 들어 있습니다.

CMO3 가져오기는 별도의 애플리케이션 계층 가져오기 모듈을 공유합니다. GUI 진입점은 확인 후 신뢰된 사용자 신원으로 이를 호출하며, MCP는 대화 상자에 들어가지 않습니다. `mode=new`는 새 프로젝트 ID와 불러오기 세대를 사용합니다. `mode=replace`는 불러온 프로젝트가 필요하며, 파일에 없는 오브젝트와 파라미터 스냅숏, 히스토리 주석을 유지하고, 문서와 포즈 초기화를 같은 커밋에서 게시합니다. 가져온 모델에는 PSD 자동 리그나 모션 프리셋이 적용되지 않습니다. 작업 진행 상황은 파일 읽기, 후보 준비, 다시 빌드, 커밋을 차례로 다룹니다. 커밋 전의 취소는 원래 프로젝트를 유지하고, 커밋 후의 취소는 완료된 결과를 유지합니다.

한 프로젝트 안에서 같은 `request_id`로 같은 작업, 인수, 작성자로 재시도하면 원래 결과를 반환합니다(내보내기는 원래 작업 핸들을 반환). 무엇이든 바뀌면 새 ID를 사용해야 합니다. 최근 512개의 성공 및 실패 결과가 유지되며 그보다 오래된 것은 지워집니다. 지워진 ID로 재시도하면 다시 실행됩니다(오래된 `state`를 가진 변경 작업은 충돌로 실패하며 두 번 적용되지 않음). 이 중복 제거는 앱 프로세스 안에서 공유되며 MCP 연결에 의존하지 않습니다. 작업 진행 상황은 이미 끝난 호출의 진행 토큰이 아니라 조회를 통해 반환됩니다. 예전 이름 `export`와 `export_psd`는 제거되었습니다.

```json
{"request":{"request_id":"export-001","project_id":"project-id-from-inspect","state":"opaque-state-from-inspect","output_directory":"D:/exports/model"}}
```

모든 변경 작업에는 `request_id`가 필요하며, 작업 공간 변경 작업에는 `project_id`와 `state`도 필요합니다. `workspace_inspect`는 아무것도 불러오지 않았을 때의 상태 토큰을 포함해 항상 현재 상태를 반환합니다. 빈 작업 공간에서 만들거나 가져올 때 `project_id`는 `null`입니다. `state`는 불러오기 세대와 영구 버전을 담은 불투명한 토큰입니다. 같은 히스토리 노드를 다시 열어도 바뀌며, 히스토리 HEAD로 채울 수 없습니다. 문서 쓰기는 자체 커밋 토큰과 `history_node_id`를 반환하며, 변경이 없으면 노드를 만들지 않습니다. 공유 컨텍스트는 레지스트리가 공개하고 검증하며 커밋하는 쪽에서 다시 확인합니다. 요청 대기 중에 연결이 끊겨도 프로세스가 가진 실행은 취소되지 않습니다. 파라미터 스냅숏과 히스토리 주석은 같은 애플리케이션 명령과 영구 버전을 사용하며, 리그 히스토리 노드를 추가하지 않습니다. GUI와 MCP의 명시적인 포즈 변경도 상태를 진행하지만, 재생/물리 평가 프레임과 스냅숏 호버는 진행하지 않습니다. GUI에서 파라미터를 바꾸면 먼저 로컬에 표시한 뒤 순서대로 커밋하며, 커밋이 반영될 때까지 `scope=preview`는 여전히 커밋된 포즈를 반환합니다. 그 사이에 MCP가 포즈나 다른 변경을 커밋하면, GUI의 대기 중인 변경은 충돌로 처리되어 커밋된 포즈로 되돌아갑니다.

`physics_simulate/simulation_simulate`는 시작하기 전에 요청한 프로젝트와 상태를 확인하고 별도의 조회 세션 하나를 캡처합니다. 이후의 변경이나 프로젝트 다시 열기는 이 샘플을 바꾸지 않습니다. 완료된 `result`에는 원래 샘플링 진단과 캡처 시점의 `project_id/state/revision`이 들어 있습니다. `job_get/job_wait`로 조회하고, 보정, 안정화, 프레임별 풀이는 `job_cancel`로 중단합니다. 대기를 취소하거나 연결을 끊어도 작업은 계속되며, 원래 요청을 재시도하면 같은 작업을 돌려받습니다. 이들은 `read_only:true`를 유지하며, 히스토리를 추가하지 않고, 영구 버전을 바꾸지 않으며, 문서 일괄 처리에 참여할 수 없습니다. `simulation_simulate`의 `hold/release`는 각각 0–20초입니다.

```json
{"request":{"request_id":"sample-001","project_id":"project-id-from-inspect","state":"opaque-state-from-inspect","id":"simulation-id-from-inspect","hold":0.5,"release":1.5}}
```

## 오류와 복구

즉시 실행되는 호출이 실패하면 도구는 `ok:false`와 구조화된 `error`를 반환합니다. 백그라운드 작업이 실패해도 `job_get/job_wait` 조회 자체는 `ok:true`를 반환하므로 `data.status`를 확인하세요. `failed` 작업의 `data.error`는 같은 구조를 사용하며 성공 `result`가 없습니다. 작업 조회, 목록, 연결 끊김과 재연결, 원래 요청의 재시도는 모두 원래 오류를 유지합니다. 문제를 고친 뒤 새로 시도할 때는 새 `request_id`를 사용해야 합니다. 예전 ID를 재사용하면 원래 결과를 받습니다.

| `error.code` | 대처 방법 |
| --- | --- |
| `invalid_request` | 현재 스키마를 읽으세요. `field`가 해당 필드를 가리킵니다 |
| `output_contract` | 서비스 구현이 선언과 맞지 않는 결과를 반환했습니다. `operation/field`가 계약 문제의 위치를 알려 줍니다. 먼저 프로젝트나 작업의 실제 상태를 조회하세요. 이를 근거로 변경이 롤백되었다고 가정하지 말고, 단순히 다시 실행하지도 마세요 |
| `invalid_argument` / `invalid_edit` | 인수나 편집을 고치세요. 일괄 처리 오류에는 `edit_index`와 `edit_operation`도 붙습니다 |
| `not_found` / `id_conflict` / `owned_by_generator` / `key_incomplete` / `topology_refused` | 구별 가능한 거부로, `target`(`kind:id`, 예: `parameter:ParamAngleX`, `hit_area:HitAreaHead`)과 일괄 처리 구성원이라면 `edit_index`, `edit_operation`이 붙습니다. 각각 지정한 오브젝트, 키, 항목이 없음, 새 ID나 변환할 ID가 이미 사용 중(삭제된 파라미터의 남은 모션 커브나 표정 항목 포함), 오브젝트가 흔들림, 시뮬레이션, 내부 효과, 뼈대에 속하므로 해당 패널에서 다뤄야 함, 키폼 편집의 키에 대상이 직접 바인딩된 축이 빠져 있음, 토폴로지 동작이 이 메시와 선택에 적용되지 않음을 뜻합니다. 아무것도 커밋되지 않았습니다 |
| `tile_collides` | 이동, 회전, 밀도 변경 후 `layer_ids`의 타일이 배치된 위치에서 밀려나게 되어(페이지를 벗어나거나 다른 타일의 메시와 교차) 변경 전체가 커밋되지 않았습니다. `atlas_check_placement`로 다른 위치를 찾거나, 관련된 이동을 같은 일괄 처리에 넣으세요 |
| `state_conflict` | 프로젝트를 다시 읽으세요. 오류에 `expected_state`와 `actual_state`가 붙습니다 |
| `project_conflict` | 대상 프로젝트를 확인하세요. 오류에 null일 수 있는 `expected_project`와 `actual_project`가 붙습니다 |
| `request_id_reused` | 같은 ID가 다른 작업, 인수, 작성자에 사용되었습니다. 새 요청 ID를 사용하세요 |
| `unsaved_changes` | 먼저 저장하거나, 지원되는 버리기 플래그를 명시적으로 넘기세요 |
| `workspace_busy` | 진행 중인 작업이 끝나기를 기다린 뒤 현재 상태를 읽으세요 |
| `io_error` / `permission_denied` | `message`에 따라 파일 경로, 접근 권한, 디스크를 확인하세요 |
| `invalid_state` / `operation_failed` | `message`와 현재 프로젝트 상태로 업무 전제 조건이나 실행 문제를 진단하세요 |

`cancelled`는 오류 코드가 아니라 별도의 작업 상태입니다. 커밋에 성공한 뒤의 늦은 취소나 새로 고침 예외는 `completed`와 커밋된 결과를 유지하며, 변경이 롤백되었다고 주장할 근거가 되지 않습니다.

`rig_create_warp`는 캡처한 후보 모델에서 `mesh:<id>`를 해석하며, 중복 없는 대상과 공통 부모 Warp가 필요합니다. `id`를 생략하면 한 번만 할당됩니다. 명시적인 `id`는 같은 일괄 처리의 뒤 항목에서 참조할 수 있으며, 새 Warp는 일괄 처리의 `changed`에도 나타납니다. `rows/columns`의 기본값은 16이고 범위는 1–32입니다. 실제 격자는 부모 격자에 맞춰 정렬되어 축마다 최대 64가 되므로, 오브젝트 상세의 실제 분할 수를 기준으로 하세요. `fit_local`의 기본값은 true로, 일반 키폼과 독립 블렌드 셰이프를 겹쳐 쌓은 보수적인 외곽으로 잘라 내고 그에 맞춰 기하와 블렌드 셰이프를 다시 매핑하며, UV, 스칼라 채널, 경로, 정점 가중치는 유지합니다. 외곽이 부모 영역을 넘으면 피팅이 거부됩니다. 먼저 부모 영역을 넓히거나, `fit_local:false`로 부모 영역 전체를 유지하세요. 자유롭게 배치하는 캔버스 Warp는 여전히 `canvas_warp`를 사용합니다. 단일 호출의 최종 `job_wait/job_get.result`에는 `target/state/history_node_id`가 들어 있습니다. 커밋 전의 실패나 취소는 새 Warp를 게시하지 않으며, 커밋 후의 새로 고침 실패나 늦은 취소는 성공과 핸들을 유지합니다.

## 원자적 문서 편집

`workspace_apply_edits`는 바깥의 `request_id`, `project_id`, `state`를 공유합니다. `edits`의 각 항목은 `{"operation":"...","request":{...}}`이며, 안쪽 요청은 그 단일 도구의 업무 필드만 채우고 컨텍스트 필드는 반복하지 않습니다. 구성원은 단일 도구가 공개하는 것과 같은 업무 스키마를 사용하며, 알 수 없는 필드와 지원되지 않는 구성원은 후보 편집 전에 거부됩니다.

### 기하 검사와 시험 실행

`rig_deform`, `keyform_apply`, `rig_edit_structure`, `object_edit_appearance`, `canvas_warp/rotation/glue/topology`, `canvas_deform_stroke`, `path_deform`의 기하 작성 저널 항목은 애플리케이션 계층의 전체 다시 빌드 후, 정식 CAS 전에 기하 검사를 거칩니다. 대응하는 GUI 저널과 MCP는 같은 규칙을 사용합니다. 일괄 처리는 최종 후보만 검사하므로, 앞 항목이 일시적으로 품질을 떨어뜨리고 뒤 항목이 이를 바로잡아도 됩니다. 다른 문서 작업은 이 규칙 때문에 더 넓은 검사를 받지 않습니다.

`workspace_preview_edits`는 정식 커밋과 같은 순서 있는 후보 준비, 다시 빌드, 검사를 사용하지만, 투영이나 CAS를 호출하지 않으며 상태, 히스토리, 저장되지 않음 표시, 포즈, 리소스를 바꾸지 않습니다. 읽기 전용 백그라운드 작업이지만 여전히 `request_id/project_id/state`가 필요합니다. `job_wait/job_get.result`는 `dry_run:true`, 입력 `revision`, `candidate_revision`, `would_change`, `would_commit`, 새 오브젝트의 `changed` 핸들, `diagnostics`를 줍니다. Warp, Rotation, Glue를 만들 때는 명시적인 `id`를 주고, 같은 상태에서 같은 편집을 `workspace_apply_edits`에 제출하세요. 그래야만 후보 리비전을 반복 가능하게 비교할 수 있습니다. 시험 실행은 오브젝트 ID를 예약하지 않으며, 이후의 상태 충돌 검사를 건너뛰어도 된다는 허가도 아닙니다. 기하 외의 구성원은 시험 실행에서 받아들여지지 않으며, 무엇이 지원되는지는 작업의 스키마가 정의합니다.

`workspace_preview_regeneration`(간결 프로필에서는 `workspace_call`로 호출)은 커밋 전에 재생성 결과를 보기 위해, 모든 문서 작업(리소스 레이어 작업 제외)을 같은 방식으로 시험 실행합니다. `checkpoints`는 커밋이 저널에 기록할 체크포인트를 나열하고(`index`는 후보 저널에서의 위치. `kind: "regeneration"`은 사용자의 리그 위에 병합된 생성 출력이며, `issues`는 `rig_update_generation`과 같은 형식. `kind: "materialized"`는 새 항목 앞에서 구체화된 현재 작성 상태), `added`/`removed`는 후보가 추가하고 제거하는 오브젝트의 핸들이며, 그 밖에 `dry_run:true`, `revision`, `candidate_revision`, `would_change`가 있습니다.

검사는 영향을 받는 오브젝트의 부모 로컬 기하를 다루며, 일반 키, 블렌드 셰이프 키, 가중치 제한점의 조합을 포함합니다. 유한하지 않은 좌표, 잘못된 토폴로지, 새로 생긴 면적 0의 퇴화는 커밋을 막으며 `geometry_unsafe`와 구조화된 `diagnostics.violations`를 반환합니다. 새로 생긴 국소 뒤집힘과 기준 삼각형 면적의 1% 미만으로 줄어든 국소 붕괴는 `diagnostics.warnings`에 남고 기본적으로 막지 않습니다. 기존 작성 작업 흐름은 의도적인 접힘을 허용하므로, 뒤집힘 수를 리깅 실패로 볼 수는 없습니다. 기존의 뒤집힘, 퇴화, 붕괴는 따로 세며, 새 결함을 추가하지 않는 편집을 막지 않습니다. 곡면 전체의 가역적인 아핀 미러와 압축은 통과하며, 면적이 0이 되는 변환은 여전히 거부됩니다. 토폴로지 변경으로 이전 삼각형의 정체성을 유지할 수 없으면 새 메시의 기준 기하를 사용합니다. 블렌드 좌표의 데카르트 곱이 16384를 넘으면 대신 결정적인 분산 샘플링을 사용하며, 양 끝의 조합은 유지하고, 후보를 더 이상 GEOMETRY_SAMPLING_LIMIT으로 거부하지 않습니다. 보고서의 범위 설명은 큰 조합 영역을 샘플링했다고 명시합니다. 이는 모든 조합에 대한 증명이 아닙니다.

앞머리 위 복사본(`<original id>_SeeThrough`)은 빌드할 때마다 설정에서 새로 만들어지며 직접 편집할 수 없습니다. 참조 필드(`target`, `id`, `layer_id`, `meshes` 등)에서 복사본을 지정하는 문서 작업은 모두 `generated_copy`를 반환하며, 이름이나 다른 자유 텍스트는 해당하지 않습니다(`details.target`은 복사본, `details.original`은 대신 편집할 원본). 원본이나 설정 `seeThroughBrows` / `seeThroughEyes` / `seeThroughOpacity`를 편집하세요. `objects`로 복사본을 조회하면 원본의 레이어를 `topologyInfo.layerId`로, 원본을 `topologyInfo.seeThroughOf`로 줍니다. 눈은 대신 앞머리의 창을 통해 보입니다. 완성된 리그는 눈 위의 각 앞머리를 눈 파츠로 반전 클리핑하고, 그 눈 파츠로 클리핑된 앞머리 복사본(`<bang id>_SeeThrough`, `seeThroughOf`는 앞머리)을 그립니다. 이 마스크는 완성된 리그에만 있으며, 앞머리의 `topologyInfo.seeThroughWindow`가 이를 나열하고, 문서 안의 앞머리 자체 마스크는 비어 있습니다. 이를 앞머리의 `masked_by`로 다시 기록하지 마세요.

후보가 시작 문서에 비해 작성 저널 항목만 추가한 경우, 검사 범위는 새 명령에서 도출됩니다. 명령이 직접 대상으로 하는 메시, Warp, Rotation과 그 모든 자식, 관련 메시의 Glue 상대, 그리고 베이크되지 않은 흔들림과 베이크된 시뮬레이션의 대상(이 두 생성기는 재생할 때마다 대상을 다시 계산함)입니다. 오브젝트를 만들거나 다시 부착하는 명령, 메시 다시 빌드, 그 밖에 영향받는 오브젝트를 하나하나 나열할 수 없는 명령, 그리고 저널 밖의 문서 내용도 바꾸는 후보는 모든 오브젝트를 비교하는 방식으로 돌아갑니다. 범위 안에서의 분류는 전체 비교와 같습니다. 보고서의 `coverage`는 `mode`(`full` 또는 `scoped`), 실제로 비교한 오브젝트 수 `checkedTargets`, 두 모델의 오브젝트 총수 `totalTargets`를 줍니다. 범위가 좁혀졌으면 `scope` 텍스트가 다른 오브젝트는 비교하지 않았다고 알립니다.

진단은 이 샘플 지점만 다룹니다. 키 사이의 보간, 부모 변형과의 조합, Glue, 마스크, 픽셀 커버리지, 물리, 시각적 품질은 증명하지 않습니다. 시험 실행 결과는 입력 버전에 속하며, 프로젝트가 편집되거나 다시 열리면 그 결과는 새 버전의 근거가 되지 않습니다.

정식 기하 작성 커밋도 같은 차단 항목과 경고를 담은 `geometry_diagnostics`를 반환할 수 있습니다. 기하 검사에 관련된 오브젝트가 없으면 이 필드는 생략됩니다. 뒤집힘 경고를 변경 롤백으로 표현할 필요는 없습니다.

면적 0 감지에는 부동소수점 허용 오차가 포함됩니다. 부호 있는 면적의 절댓값이 `1e-12` 미만이거나, 면적 비율이 기준 형태의 `1e-6` 이하인 삼각형은 수치적으로 퇴화한 것으로 봅니다. 겹친 정점이 아주 작은 음의 면적을 남기는 경우는 단순한 뒤집힘 경고로 보고되지 않습니다.

GUI 파라미터 정의, 폴더 위치, 파라미터 키는 하나의 공유 커밋으로 합칠 수 있습니다. 캔버스 저널, 내부 유형 편집, 필드 완성 대기열도 같은 후보/다시 빌드/CAS 경계로 들어갑니다. GUI 흔들림 세션, 필드 편집, 시뮬레이션 변경, 오프라인 베이크는 시작할 때의 상태를 유지하므로, 오래된 결과가 다시 열렸거나 바뀐 프로젝트에 커밋되지 않습니다. GUI 작성자는 신뢰된 어댑터가 `user`로 지정합니다.

파라미터 정의는 이제 실제 편집 순서대로 저널에 기록되며, 삭제는 그 앞의 키폼과 마지막 기본값을 읽습니다. 예전 프로젝트의 정적 파라미터 재정의는 호환을 위해 계속 읽을 수 있으며, 히스토리는 다시 쓰지 않습니다. 불투명도나 색 같은 순수 채널 변경도 저장됩니다. 동일한 기존 키를 다시 캡처하면 히스토리를 추가하지 않습니다.

현재 87개 작업이 일괄 처리를 지원합니다(소재 레이어 추가, 등록 위치 지정, 확정, 레이어 전체 변환, 메시 다시 빌드, 여섯 가지 텍스처 편집 포함). 설정, 레이어 분류/메시 설정, 원본 다각형/구성 요소/깊이 분할과 삭제/복원, 파라미터 정의, 독립 Warp 생성, 리그 변형/키폼/구조/외형/정점 그룹, 원본 페인트 작업 여섯 가지, 뼈대 변경 일곱 가지, 모션 변경 여섯 가지, 캔버스 편집 네 가지, 경로 put/delete/deform, 흔들림 put/delete, 물리 put/delete/config/fit, 시뮬레이션 put/delete/bake/clear_bake와 model_apply_preset입니다. 설정, 분류, 메시 설정, 원본 분할, 이미지 위치 지정/취소, 레이어 소프트 삭제/복원, 독립 Warp, 페인트, 물리, 시뮬레이션, 프리셋의 단일 호출은 백그라운드 작업을 사용합니다. 일괄 처리는 같은 애플리케이션 계층 후보를 직접 실행하며, 분할, 페인트, 베이크, 피팅의 진행 상황에는 소속된 일괄 처리 구성원이 포함되고, 준비 중에는 일괄 처리 전체를 취소할 수 있으며, 구성원의 단일 작업 작업은 중첩되지 않습니다. `classic_front_hair/classic_back_hair`는 `sway:false`를 지원하며, 시뮬레이션을 제거한 뒤 그 유형의 클래식 흔들림을 끕니다. 탐색 도구의 `batchable:true`만이 지원을 뜻합니다. GUI 필드 완성은 비동기 후보 대기열에 연결되어 있으며, 생성 설정/분류/메시 초안은 공유 순수 후보로 변환됩니다.

소재 레이어 추가는 가져온 모델과 직접 만든 부모 디포머를 지원합니다. 애플리케이션 후보는 원본 캔버스 기하를 실제 부모의 중립 좌표로 변환하고 메시 생성 기록을 저장합니다. 다시 등록하면 원래 처리된 픽셀을 사용해 생성된 기하를 제자리에서 교체하며, 오브젝트 ID와 부모는 유지합니다. 기존의 전용 바인딩이나 마스크 의존성이 있으면 여전히 단일 레이어 위치 지정을 거부합니다. 소재 레이어는 소재의 원래 해상도를 유지합니다. 투명한 가장자리를 잘라 낸 뒤 픽셀이 바로 레이어 래스터가 되고, 소재 배치나 등록으로 정한 캔버스 사각형이 레이어 사각형이 됩니다(`canvasUnitsPerPixelX/Y`가 실제 비율). 예를 들어 32단위 영역에 등록한 1024픽셀 소재는 32단위, 1024픽셀 레이어입니다. 평행 이동/크기 조정에 의한 등록(행과 열만 뒤집는 명시적 미러 포함)에는 보간이 없으며, 회전이 있는 등록만 소재 자체의 밀도로 경계 상자에 래스터화합니다. 소재 매니페스트와 레이어 모두 원래 픽셀을 유지하며 더 이상 캔버스 해상도로 줄이지 않습니다. 할당하기 전에 좌표, 1,600만 픽셀 경계 상자, 1,600만 픽셀 래스터, 밀도 한도를 확인하며, 준비 단계는 취소에 응답합니다. 연속적인 GUI 배치는 별도의 애플리케이션 세션을 거치며, 저장은 미리보기를 제외하고 정식 확정을 기다립니다.

각 항목은 이전 항목의 후보 문서와 다시 빌드된 모델 위에서 실행되므로, 같은 일괄 처리 안에서 파라미터를 만든 뒤 키폼이나 모션 트랙에 바인딩할 수 있고, 앞에서 만든 캔버스 오브젝트를 뒤의 편집에서 참조할 수 있습니다. 뒤 항목이 참조하는 오브젝트에는 안정적인 ID를 주는 것이 좋습니다. 선택 사항인 ID를 생략하면 결과의 `changed`에 생성된 오브젝트의 `kind:id` 핸들이 들어 있으며, 같은 요청을 재시도하면 원래 핸들을 반환합니다.

최종 CAS가 성공해야만 문서, 모델, 히스토리 노드 하나가 게시됩니다. 구성원 편집이나 다시 빌드 실패, 커밋 전의 취소, 상태 충돌은 모두 원래 작업 공간을 유지합니다. 변경이 없거나 원래 문서로 되돌아가는 일괄 처리는 상태를 진행하지도 히스토리를 추가하지도 않습니다. 구성원 실행 오류는 0부터 시작하는 `edit_index`와 `edit_operation`을 담은 `invalid_edit`를 반환하고, 스키마 오류는 필드 경로를 담은 `invalid_request`를 반환합니다. 세션 변경, 프로젝트 전환, 리소스 I/O, 읽기 전용 시간 시뮬레이션(simulation_simulate), 중첩된 일괄 처리는 구성원이 될 수 없습니다. 일괄 처리는 프로세스 작업 핸들을 반환하므로 `job_wait/job_get`으로 상태와 `data.result`를 읽습니다. 성공한 결과에는 `project_id/state/history_node_id/revision/applied/edit_count/changed`가 들어 있습니다. 각 항목을 준비하고 베이크하는 동안 진행 상황이 보고됩니다. 커밋 전의 취소는 후보를 게시하지 않으며, CAS가 성공한 뒤에는 완전한 결과가 즉시 유지되고, 늦은 취소나 새로 고침 실패도 여전히 completed를 반환합니다. 업무 실패는 작업의 `error`에 위의 분류와 인덱스를 유지하며, 스키마 오류는 작업이 만들어지기 전에 반환됩니다. 연결을 끊거나 대기를 취소해도 작업은 멈추지 않으며, 원래 요청 ID로 재시도하면 같은 작업을 돌려받습니다. GUI 내부의 직접 호출도 같은 명령 경계를 사용합니다.

```json
{
  "request": {
    "request_id": "batch-001",
    "project_id": "project-id-from-inspect",
    "state": "opaque-state-from-inspect",
    "summary": "Create an axis and its motion key",
    "edits": [
      {"operation": "parameter_create", "request": {"parameter_id": "ParamAccessory", "name": "Accessory", "min": -1, "max": 1}},
      {"operation": "motion_put", "request": {"clip": {"id": "accessory", "name": "Accessory", "duration": 1}}},
      {"operation": "motion_set_key", "request": {"id": "accessory", "parameter": "ParamAccessory", "key": {"time": 0, "value": 1}}}
    ]
  }
}
```

## 파라미터 스냅숏과 히스토리 주석

`snapshot_create`에서 `values`를 생략하면 현재 작성 포즈를 저장합니다. 재생 중에 평가된 값을 저장하려면 샘플링한 값을 명시적으로 넘기세요. `snapshot_update`는 이름만 바꾸거나, 값만 교체하거나, 둘 다 할 수 있으며, 생략한 필드는 값을 유지합니다. 스냅숏을 삭제해도 다른 스냅숏의 번호는 바뀌지 않습니다. GUI 스냅숏 바와 MCP는 중립적인 편집 명령을 공유하며, 데이터는 런타임에서 캡처되고, 저장/다시 열기는 스냅숏 ID, 번호, 파라미터 값, 히스토리 주석을 유지합니다.

`snapshot_apply`는 잠긴 파라미터의 값을 유지하고, 스냅숏 안의 삭제된 파라미터는 무시하며, 예전 값은 현재 파라미터 범위로 클램프합니다. GUI에서 스냅숏을 적용하는 것은 MCP와 같으며 타임라인 자동 키 토글의 영향을 받지 않습니다. 타임라인에 기록하려면 모션 편집 도구를 따로 호출하세요. 변경이 없으면 원래 `state`를 유지하고, 실제로 바뀌면 자체 `state`, `project_id`, `history_node_id`를 반환합니다. 히스토리 주석은 노드의 표시 제목, 메모, 숨김 플래그만 바꾸며, 노드 내용과 분기 관계는 그대로입니다.

`preview_set`에서 생략한 파라미터와 잠금은 런타임의 커밋된 포즈에 있는 값을 유지합니다. `snapshot_apply`도 마찬가지로 커밋된 잠금을 따르며, 적용되지 않는 파라미터는 값을 유지합니다. 애니메이션 평가 프레임과 커밋되지 않은 GUI 표시 값은 병합 기준이 아닙니다. `preview_reset`은 모든 기본값을 복원하고 잠금을 해제합니다. 이 세 가지는 별도의 애플리케이션 명령과 보조 CAS를 공유하며, 리그 히스토리나 자동 키프레임을 추가하지 않습니다. 논리적으로 변경이 없으면 상태를 진행하지 않지만, 올바른 UI 포즈를 복원하고 재생을 멈출 수는 있습니다. 저장도 작업 공간별로 캡처된 영구 포즈와 잠금을 사용하며, 복제된 작업 공간의 포즈는 즉시 등록됩니다. 아카이브에서는 이들이 `workspace.json`의 작업 공간 필드로 투영됩니다.

## 최소 호출 예

아래 JSON은 각 도구의 인수이며 HTTP나 JSON-RPC 봉투가 아닙니다. 예시 ID, `state`, 좌표는 현재 프로젝트에서 반환된 값으로 바꾸세요.

`workspace_inspect`:

```json
{
  "request": {
    "scope": "project"
  }
}
```

레이어 분류는 먼저 `workspace_inspect scope=layers`가 반환하는 `type/role/side/parameter/switch_id`를 읽은 다음 필요에 따라 갱신합니다. 예를 들어 기존 소재를 전환 차분으로 바꾸려면 다음과 같이 합니다.

```json
{
  "request": {
    "project_id": "project-id-from-inspect",
    "request_id": "unique-request-id",
    "state": "opaque-state-from-inspect",
    "layer_id": "actualLayerId",
    "type": "switch",
    "parameter": "expression",
    "switch_id": 1
  }
}
```

이 세 필드는 UI 레이어 표에 대응합니다. `parameter_update`는 Cubism 파라미터 정의만 바꾸며, 원본 레이어의 분류나 차분 연결은 바꾸지 않습니다.

필요에 따라 조회한 다음 오브젝트 하나를 읽습니다.

```json
{
  "request": {
    "scope": "objects",
    "query": "hair",
    "limit": 24
  }
}
```

```json
{
  "request": {
    "target": "mesh:actualMeshId"
  }
}
```

`view_render_poses`로 고정 카메라 포즈 비교:

```json
{
  "request": {
    "viewport": {
      "mode": "canvas_rect",
      "left": 0,
      "top": 0,
      "width": 1000,
      "height": 1000
    },
    "poses": [
      {
        "ParamAngleX": -30
      },
      {
        "ParamAngleX": 0
      },
      {
        "ParamAngleX": 30
      }
    ],
    "columns": 3,
    "target_long_edge": 1024
  }
}
```

`rig_deform`:

```json
{
  "request": {
    "project_id": "project-id-from-inspect",
    "request_id": "unique-request-id",
    "state": "opaque-state-from-inspect",
    "changes": [
      {
        "target": "warp:actualWarpId",
        "key": {
          "ParamCustom": 1
        },
        "operations": [
          {
            "type": "translate",
            "delta": [
              0.02,
              0
            ]
          }
        ]
      }
    ]
  }
}
```

여기서 `key`는 대상에 직접 바인딩된 관련 축을 모두 포함해야 합니다. 관찰 포즈의 부모 파라미터를 모두 오브젝트 바인딩에 복사하지 마세요. 그런 다음 반환된 새 `state`를 사용해 실제 모델을 렌더링해 확인하세요.

## 상태와 히스토리

- 모델 히스토리를 진행하는 쓰기 호출에는 최신 `state`가 필요하며, 성공하면 이후 호출에 쓸 상태를 반환합니다. 오래된 상태라면 무작정 덮어쓰지 말고 다시 inspect해 편집을 조정하세요.
- 일괄 처리 안의 편집은 먼저 검증되고 다시 빌드된 뒤 성공하면 커밋됩니다. `rig_deform` / `keyform_apply` 하나에는 1–128개의 변경을 넣을 수 있고, 변형 하나에는 1–16개의 동작을 넣을 수 있습니다.
- 흔들림과 시뮬레이션이 생성한 키폼(키에 기본값이 아닌 생성 파라미터가 있음): `rig_deform`과 `keyform_apply`의 `set`은 생성 결과 재정의로 기록되어 생성기 뒤에 병합됩니다. 이 격자점으로의 또는 이 격자점에서의 `copy`, `delete`, 그리고 생성기의 축에 작용하는 `parameter_keys`는 거부되며, 오류에 생성기 이름이 나옵니다. 생성 파라미터를 다른 오브젝트에서 사용하면(키 설정, 이름 변경, 범위 변경) 먼저 그 파라미터를 생성기가 현재 정의하는 대로 프로젝트에 기록하며, 그 뒤로는 일반 파라미터처럼 동작합니다. 생성기 자체의 파라미터는 `parameter_delete`로 삭제할 수 없습니다.
- 변경이 없는 일반 쓰기는 히스토리 노드를 만들지 않아야 합니다. `checkpoint`는 명시적인 예외입니다. 복원은 쓰기이며 HEAD를 옮깁니다. 이전 노드에서 계속 편집하면 분기가 생기고, 원래 분기는 유지됩니다.
- `project_save_as`는 처음 저장하거나 다른 위치에 저장할 때 절대 경로를 줍니다. `project_save`는 현재 위치에 저장합니다. `project_open`은 프로젝트 아카이브를 엽니다. 저장하지 않은 변경이 있으면 먼저 저장 작업이 성공하기를 기다리거나 `discard_unsaved=true`를 명시적으로 주세요. 이 작업들은 GUI 대화 상자를 열지 않습니다. `project_export_model`은 납품 파일을 쓰지만 프로젝트 저장을 대신하지 않습니다. 대기 중인 에셋은 모델에 추가되지도 디스크에 저장되지도 않습니다.
- 시간 초과나 연결 끊김 뒤에는 `workspace_inspect`와 `history_list`로 변경이 커밋되었는지 확인한 다음 다음 단계를 정하세요.
- 여러 도구에 걸친 호출 간 트랜잭션, 구조화된 `history_diff`, `task_*` 실행 제어는 현재 공개 인터페이스에 포함되지 않습니다.

## 형태, 경로, 물리

### 뼈대와 모션

먼저 `skeleton_propose`를 호출해 현재 레이어에서 추론한 뼈대를 확인하세요. `skeleton_auto`만이 이를 프로젝트에 기록합니다. `skeleton_get`은 뼈 좌표, 계층, 아트 메시 바인딩, 관절 각도 제한, 관절 대역폭을 포함한 완전한 `spec`을 반환합니다. `skeleton_put`은 반환된 `spec`으로 통째로 교체할 수 있습니다. `bone`은 뼈 하나의 필드를 병합하고, `move`는 연결된 관절도 함께 옮기며, `bind`는 아트 메시를 지정한 뼈에 바인딩하고(`bone_id`를 생략하면 바인딩 해제), `remove`는 몸이 아닌 뼈를 삭제하며, `delete`는 뼈대 전체를 삭제합니다. 아트 메시 ID는 `workspace_inspect scope=objects`에서 얻습니다. 쓰기는 최신 `state`를 사용하며, 성공하면 다시 빌드하고 히스토리를 커밋합니다. 유효하지 않은 뼈대나 존재하지 않는 아트 메시는 쓰기를 거부합니다.

```json
{"request":{"mode":"propose"}}
```

```json
{
  "request": {
    "project_id": "project-id-from-inspect",
    "request_id": "unique-request-id",
    "state": "opaque-state-from-inspect",
    "bone_id": "actualBoneId",
    "end": "tail",
    "point": [
      420,
      610
    ]
  }
}
```

`skeleton_pose`는 캔버스 픽셀 좌표로부터 뼈의 방향을 풉니다. `ik: true`는 끝 뼈와 최대 두 단계 위 부모의 각도를 풉니다. 반환된 `values`는 계산 결과로, `view_render_poses`에 넘겨 렌더링으로 확인하거나(사용자의 파라미터 슬라이더는 움직이지 않음), `motion_set_key`나 `motion_pose`로 모션의 파라미터 트랙에 기록할 수 있습니다. 뼈대, 키폼, 히스토리는 바꾸지 않습니다. 뼈의 형태는 파라미터, 디포머, 메시 키폼으로 베이크되므로, 형태 보정에는 여전히 `keyform_apply` / `rig_deform`을, 물리에는 `physics_put`과 다른 물리 작업을 사용합니다.

```json
{
  "request": {
    "bone_id": "actualBoneId",
    "target": [
      560,
      440
    ],
    "ik": true
  }
}
```

`motion_list / motion_get`은 영구 `MotionClip`을 읽고, `sample`은 주어진 시간에 클립에서 보간한 파라미터 값을 반환합니다. `seed_builtin`은 생성된 모션을 같은 이름의 편집 가능한 재정의 클립으로 바꿉니다. `put`은 클립을 만들거나 통째로 교체하며, `get`이 반환하는 JSON은 그대로 왕복할 수 있습니다. 클립에는 길이, 반복, FPS, 페이드인/아웃, 파라미터 커브를 설정할 수 있으며, 키는 `LINEAR`, `BEZIER`, `STEPPED`, `INVERSE_STEPPED`와 `in` / `out` 핸들을 지원합니다. `set_key`는 주어진 시간에 키를 쓰거나 교체하고, `delete_key`와 `remove_curve`는 키나 트랙 전체를 삭제합니다. 파라미터가 존재해야 하며, 키 값과 시간은 파라미터와 클립의 범위 안에 있어야 합니다. 모션을 내보내려면 `settings_update exportMotions`가 필요합니다. 생성되는 기본 모션(Idle, Blink, Nod, Shake)에는 `settings_update motionBasic`도, 뼈대 프리셋에는 `settings_update motionSkeleton`도 필요합니다.

```json
{
  "request": {
    "project_id": "project-id-from-inspect",
    "request_id": "unique-request-id",
    "state": "opaque-state-from-inspect",
    "clip": {
      "id": "wave_custom",
      "name": "WaveCustom",
      "duration": 2,
      "curves": [
        {
          "parameter": "ParamArmRA",
          "keys": [
            {
              "time": 0,
              "value": 0
            },
            {
              "time": 1,
              "value": 45
            },
            {
              "time": 2,
              "value": 0
            }
          ]
        }
      ]
    }
  }
}
```

### 일반 형태와 물리

`rig_deform`의 동작에는 `translate`, `scale`, `rotate`, `arc`, `curve`, `landmarks`가 있습니다. 좌표는 고정된 입력 경계 안의 정규화 규약을 사용하며, X는 오른쪽, Y는 아래쪽, 각도는 도 단위입니다. 선택과 감쇠로 영향 범위를 제한할 수 있으며, 임의의 점별 메시 배열은 받지 않습니다.

`keyform_apply op=seed`는 주어진 키에서 기존 형태를 샘플링하고, `copy`는 명시적인 소스 키를 복사하며, `set`은 채널이나 회전 형태를 쓰고, `delete`는 해당 키 데이터를 제거합니다. 새 파라미터는 그 자체로는 움직임을 만들지 않으며, 형태를 바인딩해야 합니다.

`keyform_apply op=mirror`는 캔버스의 세로선을 기준으로 키폼을 반사합니다. 커밋할 때 일반 `set`(과 `copy`) 항목으로 펼쳐지므로, 재생할 때 반사를 다시 계산하지 않습니다.

| 필드 | 의미 |
| --- | --- |
| `target` | 소스 메시, Warp, Rotation 디포머 |
| `destination` | 같은 종류의 대상. 기본값은 `target`(같은 오브젝트). `auto`는 ID나 이름의 L/R, Left/Right, 左/右로 반대쪽 오브젝트를 찾음 |
| `from` / `all_keys` | 소스 키. 또는 `all_keys: true`로 소스의 모든 키폼 셀. 오브젝트가 하나일 때는 `side`(`positive`가 기본값, 또는 `negative`) 쪽의 키만 |
| `key` | 대상 키. 기본적으로 `from`에서 도출됨. 좌우 파라미터는 기본값을 기준으로 뒤집히고, 좌우가 있는 파라미터(ParamEyeLOpen)는 대상이 그 반대쪽을 바인딩하거나 둘 다 바인딩하지 않으면 반대쪽으로 바뀌며, 빠진 대상 축은 기본값을 취함. 명시적으로 줄 때는 대상에 직접 바인딩된 모든 축을 포함해야 함 |
| `reflect` | 뒤집히는 파라미터. 기본값은 범위가 기본값을 중심으로 대칭이고, 2D 연결의 가로축이거나 ID가 X 또는 Z로 끝나는 일반 파라미터 |
| `axis_x` | 미러 선의 캔버스 x. 기본값은 두 오브젝트의 기본 경계 상자 중심의 중점(오브젝트가 하나면 자기 중심) |
| `channels` | 옮길 것: `geometry`와 `copy` 채널 이름. 기본값은 기하만 |

미러는 오브젝트 자체의 변형을 다룹니다. 두 오브젝트의 점은 기본 포즈에서 각자의 부모를 거쳐 캔버스로 매핑되고, 각 소스 점의 기본 형태로부터의 이동은 x가 반전된 뒤, 대상 점의 반사 위치에서 소스 삼각형(Warp의 셀은 모서리에서 모서리로 분할) 위에서 보간되어 대상의 부모 공간으로 다시 매핑됩니다. Rotation 디포머는 원점의 이동을 미러하고 각도 변화를 반전합니다. 대칭인 오브젝트와 쌍은 점 대 점으로 대응합니다. 블렌드 셰이프 키는 지원하지 않습니다.

`keyform_apply op=reset / interpolate / paste`는 키의 기하 전체(또는 `vertices`가 지정한 점만)를 씁니다. 커밋할 때 각각 일반 `set`으로 펼쳐지므로, 재생할 때 다른 키를 읽지 않습니다. 생성기가 소유한 키폼에 대한 기하 `set`은 생성 재정의로 기록됩니다. `paste`는 이동량을 씁니다. 메시는 기본 형태로부터, Warp나 Rotation 디포머는 기본 키 형태로부터의 이동입니다. 소스는 그 명령이 컴파일될 때 샘플링되므로, MCP 일괄 처리에서 뒤의 `paste`는 앞 구성원이 쓴 후보를 읽습니다(따라서 `paste` 두 번으로는 한 일괄 처리에서든 두 번의 커밋에서든 A와 B를 맞바꿀 수 없습니다. 두 번째가 첫 번째가 쓴 것을 읽기 때문입니다. 맞바꾸려면 먼저 A를 빈 키 C에 `copy`한 다음, B를 A에, C를 B에 붙여넣으세요. 한 일괄 처리에서 이 순서로 할 수 있습니다). 편집기는 복사할 때 소스를 고정하고, 붙여넣기 전의 모델에서 모든 `set`을 계산합니다.

| 필드 | 의미 |
| --- | --- |
| `target` | 메시, Warp, Rotation 디포머. `paste`에서는 소스 |
| `key` | 쓰는 키. 대상에 직접 바인딩된 모든 축을 포함해야 함 |
| `vertices` | 이 점들만 바뀜(메시 정점 또는 Warp 제어점 인덱스). 나머지는 키의 현재 형태를 유지. Rotation 디포머에는 쓸 수 없음 |
| `to` (reset) | `default`(기본값): `along` 파라미터를 기본값으로 둔 형태. `rest`: 메시 전용, 변형되지 않은 메시 |
| `along` (reset) | `to: default`와 함께: 기본값으로 되돌릴 파라미터. 기본값은 키의 모든 파라미터 |
| `parameter` (interpolate) | 보간할 축. 기본값은 키가 두 키 사이에 놓인 유일한 축이며, 그런 축이 여럿이면 필수. 이웃 키의 형태 사이에서 위치에 따라 선형 보간 |
| `from` (paste) | 소스 키. 소스에서 빠진 축은 기본값을 취하며, 그 지점에서 보간된 형태를 읽음 |
| `destination` (paste) | 기본값은 `target`. 기하의 경우 다른 오브젝트는 같은 부모 아래에서 같은 삼각형(Warp는 행과 열)을 가진 같은 종류여야 하며, 아니면 붙여넣기가 거부됨 |
| `all_keys` (reset) | `key` 대신: 오브젝트의 모든 키폼을 기본 키 형태로 되돌림(디포머 형태 복원) |
| `ratio` (paste) | 0–1, 기본값 1: 키가 붙여넣는 값 쪽으로 움직이는 정도. 1 미만이면 블렌드 |
| `flip` (paste) | `horizontal` 또는 `vertical`: Warp는 열 또는 행을 뒤집고, 메시는 거울상 기본 위치로 정점을 짝지으며(기본 형태가 그 방향으로 대칭이어야 하며, 아니면 거부됨), 각 점은 짝의 이동을 반전해 취함. Rotation은 각도 변화를 반전 |
| `channels` (paste) | `geometry`, `opacity`, `drawOrder`, `multiplyColor`, `screenColor` 중 원하는 것. 기본값은 기하만. 채널 값도 `ratio`로 블렌드됨 |

`keyform_apply op=multi_key`는 여러 오브젝트의 많은 키폼의 채널을 한 번에 바꾸며, 키폼마다 채널 전용 `set` 하나로 펼쳐집니다. `targets`(메시, 디포머, 파츠), `keys`(키폼이 그중 하나와 일치해야 하는 부분 키)와 `ranges`(파라미터별로 허용하는 키 값. 그 파라미터에 없는 키폼은 통과), 기본값은 모든 키폼입니다. `opacity`(0–1), `drawOrder`(0–1000, 디포머에는 쓸 수 없음), `multiplyColor`, `screenColor`(`[r, g, b]`), `method`는 `overwrite`(기본값), `add`, `multiply`, `screen`(그리기 순서는 앞의 두 가지)이며, 결과는 유효 범위로 클램프됩니다. 각 채널은 자기 트랙의 키에, 트랙이 없으면 오브젝트의 기하 키에 기록되며, `keys` / `ranges`가 이를 거릅니다. 기록은 생성 재정의가 아닌 일반 채널 키폼입니다. 일치하는 키폼이 없으면 거부됩니다.

`path_get / path_list / path_preview / path_put / path_delete / path_deform`의 점은 메시 로컬 좌표를 사용하며, 읽기 전용 조회, 미리보기, 바인딩, 키폼 베이크를 지원합니다. 경로는 편집 보조 도구입니다. 호환성은 [변형 경로](../guide/DEFORM_PATHS.md)를 참고하세요.

물리 그룹은 Cubism 진자입니다. `inputs`(`parameter`, `weight` 0–100, `type`은 위치 X인 `x` 또는 기울기 중력인 `angle`, `reflect`)가 1–16개의 `segments`(`length`, `mobility`, `delay`, `acceleration`)를 밀고, `outputs`(`parameter`, `vertex`, `scale`, `weight`, `reflect`)는 마디 `vertex` 끝의 이전 마디에 대한 각도를 읽습니다. `scale`은 라디안당 파라미터 값입니다(네이티브 Cubism 런타임은 평행 이동형 출력의 스케일을 읽지 않으므로 항상 0이며, 각도 출력만 제공됩니다). 내보낸 physics3.json과 CMO3는 프로젝트의 물리 FPS(기본값 60)를 선언합니다. `normalization.position/angle`의 `min/default/max`는 입력 범위가 매핑되는 목표입니다.

- `physics_put`은 지정한 필드만 바꿉니다. 기존 ID(생성된 그룹 포함)는 현재 그룹에서, 새 ID는 「머리와 몸 입력, 길이 10의 마디 하나, 출력 없음」에서 시작합니다. 목록 필드는 통째로 교체됩니다. `length`는 체인의 총 길이로 마디를 비례해 스케일하고, `mobility/delay/acceleration`과 `output_scale`은 모든 마디 / 출력에 적용되며, `segment_count`는 마디 수를 조정합니다. `enabled`만 주면 그룹을 켜거나 끄기만 합니다. 수정된 생성 그룹은 `physics_delete`까지 생성 버전을 대체합니다. 예전 `input_parameter` / `output_parameter`는 여전히 단일 입력과 단일 출력으로 읽힙니다.
- 한 파라미터는 활성 그룹 하나만 구동할 수 있습니다. 사용자 그룹이 생성 그룹의 출력을 구동하면 생성 그룹이 양보합니다. `workspace_inspect scope=physics`는 `fps`와 평가 순서대로의 그룹을 반환하며, 각 그룹에는 `origin`, `enabled`, `active`, `overridden`, `replaced_by`, `issue`가 있습니다.
- `physics_config`: `order`는 먼저 평가할 그룹 ID를 나열하며, 나머지 그룹은 원래 순서대로 뒤따릅니다. Cubism은 순서대로 평가하므로, 뒤의 그룹은 같은 스텝에서 앞 그룹의 출력을 읽습니다. `fps`는 프로젝트의 물리 프레임 속도로 1–240의 정수이며, 0은 무제한입니다(물리가 모든 렌더 프레임에서 실행되고, 내보내기는 `Fps`를 선언하지 않음). 미리보기 프레임 출력은 이 값의 제한을 받지 않습니다. 미리보기 프레임 속도는 기본적으로 디스플레이를 따르는 사용자 설정입니다.
- `physics_import`: `path`는 physics3.json의 절대 경로입니다. 파일의 그룹은 사용자 그룹이 되며(같은 ID는 기존 그룹을 교체하고, 생성 그룹은 `physics_delete`까지 교체된 상태로 남음), 기존 그룹 뒤에 파일 순서대로 놓입니다. 같은 출력을 구동하는 다른 사용자 그룹은 꺼집니다. 파일의 `Fps`가 프로젝트의 물리 FPS가 됩니다. 가져온 ID, 꺼진 그룹, 없는 파라미터를 반환합니다.
- `physics_fit`: 패널 응답 커브의 표준 흔들기(1초 동안 오른쪽으로 당긴 뒤 놓기)로 그룹을 실행하고, 최댓값이 파라미터 끝의 정확히 `target`%(기본값 100)에 이르도록 각 출력의 스케일을 조정합니다. 움직이지 않는 출력은 스케일을 유지합니다. `observed_peaks`(키는 출력 인덱스 `"0"`, `"1"`…, 값은 `physics_audition`이 반환한 도달 비율이며 1이 파라미터 끝)를 주면 대신 그 측정 응답에 맞추며, 패널에서 진자를 드래그한 뒤의 **최댓값에 배율 맞추기**와 후보를 공유합니다. 잘못된 인덱스, 음이 아닌 숫자가 아닌 값, 출력이 전혀 움직이지 않는 경우는 커밋 전에 거부됩니다.

`physics_put/physics_delete/physics_config/physics_import/physics_fit`는 프로세스 작업 핸들을 반환하며 모두 `request_id/project_id/state`가 필요합니다. 커밋된 상태와 결과는 `job_wait/job_get`으로 읽습니다. 가져오기 결과에는 `imported`와 해당하는 경우 `disabled/missing_parameters/fps`도 들어 있습니다. 읽기, 피팅, 다시 빌드, 커밋은 진행 상황을 보고하며, 피팅의 안정화와 프레임별 풀이는 취소를 지원합니다. 커밋 전의 취소나 상태 충돌은 원래 프로젝트를 유지하고, 커밋 후의 늦은 취소나 UI 새로 고침 예외는 성공 결과를 유지합니다. 연결을 끊어도 작업은 멈추지 않으며, 원래 요청을 재시도하면 같은 작업을 돌려받습니다. 피팅은 원자적 일괄 처리를 지원하며 앞의 후보를 읽습니다. 파일 가져오기는 일괄 처리를 지원하지 않습니다. 이미 목표 스케일에 있는 그룹을 다시 피팅하면 히스토리 노드를 만들지 않으며, 출력 응답이 전혀 없는 그룹은 여전히 오류를 보고합니다.
- 출력 파라미터의 모션 끝 형태를 먼저 만든 다음 물리를 붙이세요. 정적인 포즈 시트는 시간을 진행하지 않습니다. 진폭, 오버슈트, 안정 시간은 스텝 입력으로 `physics_simulate`를 써서 확인하고, 모션 전체는 `view_sample_motion`으로 확인하세요(그 네이티브 샘플링 환경이 있어야 함).

## 보기와 공간 매핑

보기는 모델 데이터에서 PNG를 렌더링하며 데스크톱 스크린숏에 의존하지 않습니다. `canvas_rect`는 캔버스 사각형을 주고, `focus_layers`는 오브젝트를 중심으로 프레임을 잡습니다. 출력 픽셀 크기와 모델의 캔버스 크기는 서로 다른 것입니다.

`view_render_layer/view_render_context/view_render_model/view_render_poses/view_check_coverage`는 각각 캡처한 프로젝트 버전 하나를 읽습니다. 여러 포즈 렌더링의 모든 이미지는 그 모델과 보이는 레이어를 공유합니다. 이후의 편집, 다시 열기, 프로젝트 전환은 결과에 섞이지 않으며 이미 캡처한 조회를 무효화하지 않습니다. PNG, 공간 매핑, 리비전은 일관되게 유지되며, 이미지와 공간 참조는 캡처한 그대로 프로젝트에 유지됩니다.

- `poses`는 같은 버전과 캔버스 사각형에서 1–9개의 포즈를 비교해 레이블이 붙은 시트 하나를 반환합니다. 전체 크기 예산은 시트 전체에 적용됩니다.
- 비교 포즈는 캡처한 커밋 버전에서 백그라운드로 렌더링되며, 작성 포즈나 GUI 파라미터 슬라이더를 읽거나 바꾸지 않습니다. 비교 이미지를 만들기 위해 `preview_set`으로 포즈를 바꾸지 마세요. 사용자의 작성 포즈가 바뀌고 `state`가 진행됩니다.
- 요청에 주지 않은 파라미터는 사용자의 현재 작성 포즈가 아니라 **파라미터 기본값**으로 렌더링됩니다. 현재 포즈에서 시작하려면 먼저 `workspace_inspect scope=preview`로 커밋된 포즈를 읽어 공유 `parameters`에 넣은 다음, `poses`에는 차이만 쓰세요.
- `include_layer_ids`를 생략하면 활성 캔버스의 로컬 가시성을 사용합니다. 사용자가 숨긴 레이어, 숨겨진 디포머 아래의 레이어, 솔로 밖의 레이어는 이미지에 나타나지 않습니다. 사용자의 캔버스와 무관한 안정적인 결과를 원하면 레이어 ID를 명시적으로 넘기세요(`workspace_inspect scope=layers`에서 얻을 수 있음).
- `view_compare_history`는 히스토리 버전을 비교하고, `view_sample_motion`은 시간에 따라 샘플링하며, `view_check_coverage`는 주어진 사각형과 레이어의 알파 커버리지만 측정합니다.
- 반환된 픽셀↔캔버스 매핑으로 위치를 찾으세요. 여러 포즈 시트 전체를 단일 에셋의 공간 참조로 바로 쓸 수는 없습니다.
- 알파 커버리지, 메시 진단, 성공적으로 기록된 파일은 그림 품질 점수가 아니며, 샘플링하지 않은 포즈가 괜찮다는 것을 증명하지도 않습니다.

`view_sample_motion`은 읽기 전용 작업 핸들을 반환하며 `request_id/project_id/state`도 필요합니다. `frames`는 2–32개의 엄격한 프레임 객체(`time/parameters`)이며, 시간은 0부터 엄격하게 증가하고 최대 10초입니다. 생략한 파라미터는 이전 값을 이어받습니다. `samples`는 모션 길이 안의 1–9개 시점이고, `fps`는 15–120(기본값 60)이며, 결과 시간은 샘플링 프레임 속도에 맞춰 내림 양자화됩니다. 모델, 보이는 레이어, 리소스 대상은 작업이 시작되기 전에 캡처되며, 이후의 변경, 다시 열기, GUI 애니메이션은 샘플링에 섞이지 않습니다. 네이티브 Cubism 평가, 이미지별 렌더링, 시트는 진행 상황을 보고하고 취소를 지원합니다. 선택 사항인 네이티브 런타임이 설치되어 있어야 합니다.

최종 진단과 PNG는 `job_get/job_wait`로 읽으며, 반복해서 읽어도 같은 이미지를 반환합니다. 완료된 결과에는 `project_id/state/revision`이 들어 있으며, 기존 시트의 `revisionId`는 같은 캡처 버전을 가리킵니다. 네이티브 샘플링은 임시 모델을 사용하므로 GUI의 실시간 애니메이션과 물리 상태를 바꾸지 않습니다. 대기열에서 취소된 작업은 실행되지 않으며, 프레임 루프 중에 취소하면 모델과 임시 파일을 해제합니다. 이 읽기 전용 작업은 문서 일괄 처리를 지원하지 않습니다. 히스토리 비교는 여전히 즉시 조회이지만, 이것도 한 번 캡처한 히스토리 트리를 사용하므로 프로젝트를 다시 열어도 비교 기준이 바뀌지 않습니다.

## 소재와 Layout

프로젝트 안의 소재 라이브러리와 「먼저 배치하고 나중에 생성」 작업 흐름입니다. 설계와 경계는 [MATERIAL_LAYOUT_WORKSPACE.md](../spec/MATERIAL_LAYOUT_WORKSPACE.md)를 참고하세요. 문서 작업은 모두 `workspace_apply_edits` 구성원이 될 수 있습니다. 단독으로 호출하면 프로세스 작업 핸들을 반환하며, `job_wait/job_get`의 `data.result`는 일괄 처리 결과(`state/history_node_id/edit_count/changed`)와 같고, 새 소재는 `changed`에 `material:<id>`로, 새 레이어는 `layer:<id>`로 나타납니다.

| 도구 | 주요 인수 | 비고 |
| --- | --- | --- |
| `project_create` | `width`, `height`, 선택 사항 `discard_unsaved` | 생성되지 않은 빈 프로젝트를 만듭니다 |
| `project_import_layout` | `path`(매니페스트 `.json` 또는 매니페스트가 든 `.zip`의 절대 경로), 선택 사항 `discard_unsaved` | [Layout 매니페스트](../spec/LAYOUT_MANIFEST.md)에서 생성되지 않은 프로젝트를 만듭니다. 이미지는 원래 픽셀을 유지한 채 `rect` 위에 놓이고, 레이어는 매니페스트의 쌓는 순서, 그룹, 태그, 차분, 가시성, 불투명도를 유지하며, 각 레이어는 소재로도 등록되어 연결됩니다. 결과는 `project_create`와 같습니다 |
| `material_import` | `state`, `paths`(1–64개의 절대 경로), 선택 사항 `folder_id`, `as_reference`, `reference_under`(`as_reference`와 함께: 같은 히스토리 편집에서 모델 뒤에) | 이미지(PNG/BMP/TIFF/WebP/JPEG), PSD(레이어 그룹은 하위 폴더가 되고 각 레이어는 위치를 기록), CMO3 ArtMesh 이미지(`project_import_cmo3`가 레이어에 주는 것과 같은 픽셀), SVG(자체 크기로 래스터화), Layout 매니페스트나 이를 담은 zip(그룹은 하위 폴더가 되고, 각 레이어는 `rect`를 기본 위치로 취하며 태그를 가져옴), 폴더(중첩 구조를 유지하며 하위 폴더는 소재 폴더가 되고, 숨긴 항목과 안의 매니페스트나 zip은 건너뜀)를 읽어 히스토리 노드 하나로 소재 라이브러리에 추가합니다. `as_reference=true`이면 첫 번째가 참조 이미지도 됩니다. 각 이미지는 최대 1,600만 픽셀입니다 |
| `material_add` | `state`, `staging`, 선택 사항 `folder_id`, `as_reference`, `reference_under` | `material_import`가 읽은 뒤 실제로 커밋하는 구성원입니다. `staging`은 프로세스 안에 대기 중인 내용 다이제스트입니다. 보통 직접 호출하지 않습니다 |
| `material_list` | 선택 사항 `folder_id`, `recursive`, `query`, `kind`, `unused`, `offset`, `limit`(≤256) | 폴더와 항목을 나열합니다. `query`는 이름과 태그(폴더 태그 포함)에 일치시키며, `references`는 그것을 사용하는 레이어 수입니다. PSD / CMO3에서 읽은 폴더에는 `origin` 파일 이름이 붙습니다 |
| `material_get` | `id`, 선택 사항 `include_svg` | 항목이나 폴더의 상세입니다. 항목은 그것을 사용하는 `layers`를 반환하고, SVG에는 `vector_size`도 있으며, `include_svg`는 저장된 SVG 소스를 첨부합니다 |
| `material_preview` | `id`, 선택 사항 `size`(16–4096, 기본값 512) | 항목이나 폴더의 합성 PNG를 반환합니다 |
| `material_folder_create` | `state`, `name`, 선택 사항 `parent_id`, `id` | 폴더를 만듭니다. `id`를 주면 같은 일괄 처리의 뒤 항목이 참조할 수 있습니다 |
| `material_move` / `material_rename` | `ids` + `folder_id`(최상위는 null) / `id` + `name` | 정리. 폴더를 자기 자신 안으로 옮길 수는 없습니다 |
| `material_delete` | `ids` | 항목과 폴더(내용 전부 포함)를 삭제합니다. 삭제되지 않은 레이어가 아직 사용 중이면 그 레이어를 나열하며 거부합니다 |
| `material_tag` | `ids`, `set` 또는 `add`/`remove` | 항목과 폴더의 태그. 배치할 때 레이어 태그에 병합됩니다 |
| `material_set_resolution` | `id`, `width`, `height`(각 변 ≤4096) | SVG의 텍스처 해상도. 그것을 사용하는 레이어의 캔버스 사각형은 그대로이며 픽셀이 교체됩니다 |
| `material_paint` | `state`, `id`, 그리고 획 하나(`mode`와 그 인수) 또는 `gestures`(1–512개의 획, 각각 `mode`와 그 인수를 가지며 순서대로 하나의 변경으로 적용). `mode`는 `brush/pencil/eraser/bucket/shape/clear`이고, 인수는 `points`+`radius`+`hardness`, `point`+`tolerance`, `from`+`to`+`shape`+`stroke_width`+`filled`. 선택 사항 `color`(RGBA 바이트), `opacity` | `paint`와 같은 알고리즘으로 항목 자체의 픽셀 좌표(원점은 왼쪽 위)에 획을 칠합니다. 그것을 사용하는 레이어의 캔버스 사각형은 그대로이며 픽셀이 교체됩니다. SVG는 비트맵이 됩니다 |
| `material_fork` | `state`, `layer_ids`(1–128) | 다른 레이어와 소재를 공유하는 레이어에 자신만의 복사본을 줍니다(라이브러리에서 원본 옆). 픽셀은 바뀌지 않습니다. 소재가 없는 레이어는 소재를 받습니다. 모든 레이어는 소재 하나를 표시하며 그것이 유일한 소스입니다. 소재를 공유하는 레이어에서 `source_paint_*`와 `paint_session_begin`(`layer_id`)을 쓰면 소재에 칠해지고 그것을 사용하는 모든 레이어가 따라옵니다. 레이어의 픽셀을 바꾸는 다른 편집은 그 레이어만 소재를 사용하면 소재가 따라오게 하고, 그렇지 않으면 레이어에 복사본을 주므로 연결이 깨지지 않습니다. `material_unlink`는 예전 이름입니다 |
| `material_adopt_layers` | `state` | 소재가 없는 모든 레이어에 소재를 등록합니다(`Layers` 폴더에, 그룹은 하위 폴더로). 이제는 가져오기와 편집이 스스로 등록하므로 예전 프로젝트용입니다 |
| `material_set_pixels` | `state`, `id`, `staging` | 소재 페인트 세션이 대기시킨 픽셀을 씁니다. `paint_session_commit`이 발행하며 보통 직접 호출하지 않습니다 |
| `material_adjust` | `state`, `id`, 그리고 다음 중 하나 이상: `flip`(`horizontal/vertical`), `hue`(±180), `saturation`/`lightness`/`contrast`(±100) | 먼저 뒤집은 다음 색을 조정합니다. 크기는 바뀌지 않습니다. 레이어 동기화와 SVG 규칙은 위와 같습니다 |
| `material_export_psd` | `state`, `folder_id`, `path`(절대 `.psd`) | 폴더를 백그라운드로 PSD에 씁니다. 항목은 기본 위치에(없으면 왼쪽 위부터 배치), 하위 폴더는 레이어 그룹으로 씁니다 |
| `material_export_png` | `state`, `id`, `path`(절대 `.png`) | 항목 자체의 픽셀을 그대로(스케일 없이) 백그라운드로 PNG에 씁니다. 예를 들어 참조의 확대 이미지를 외부 도구에 넘겨 더 선명한 파츠로 다시 그리게 할 때 씁니다 |
| `layout_set_reference` | `state`, 선택 사항 `id`(기본값은 첫 번째이며, 없으면 만듦), `material_id`(null은 제거), `rect`, `opacity`(0–1, 기본값 0.5), `visible`, `rotation`(중심을 축으로 시계 방향 각도), `locked`, `under`(모델 아래), `points`(키폼 점의 배열로 각각 파라미터 ID → 값. 그중 어느 점에서든 표시. `[]`는 고정 해제) 또는 단일 `pin` | 참조 이미지: 배치와 비교를 위해 항목을 캔버스 위(또는 모델 아래)에 겹쳐 표시합니다. 레이어가 아니며, 생성, 내보내기, 리그에서 보이지 않습니다. `rect` 없이 새로 설정하면 캔버스에 맞도록 비례해 확대하거나 축소한 뒤 중앙에 놓습니다. 생략한 필드는 값을 유지합니다. 참조 조회와 렌더링은 첫 번째를 사용합니다. `material_list`는 첫 번째(`reference`)와 전체(`references`)를 반환합니다 |
| `layout_add_reference` | `state`, `material_id`, 그 밖의 필드는 위와 같음 | 참조 이미지를 하나 더 추가하며, 다른 것들 위에 그려집니다 |
| `layout_order_reference` | `state`, `id`, `index` | 참조 이미지를 그리기 순서의 `index`로 옮깁니다(0이 가장 먼저 그려짐) |
| `reference_set_mode` | `state`, `mode`(`background` / `layer` / `foreground`), 그리고 `reference_id` 또는 `layer_id` | 참조 하나를 배경, 레이어 스택, 전경으로 옮깁니다. 스택에 들어가는 참조 이미지는 참조 레이어가 되고, 스택을 떠나는 참조 레이어는 참조 이미지가 됩니다. 소재, 사각형, 불투명도, 가시성, 고정은 이어지며, 참조 이미지의 회전과 잠금은 레이어로 이어지지 않습니다 |
| `reference_set_opacity` | `state`, `opacity`(0–1), 그리고 `reference_id` 또는 `layer_id` | 생성 전이든 후든 참조 이미지나 참조 레이어의 불투명도를 설정합니다. 히스토리 편집 하나입니다 |
| `material_crop` | `state`, `id`, `rect`(항목 픽셀) 또는 `canvas_rect`(캔버스 단위. 항목이 참조이거나 이 캔버스에 배치되어 있어야 함), 선택 사항 `name`, `new_id`, `folder_id` | 이 픽셀이 캔버스 위 어디에 있는지 기억하는 새 항목을 무손실로 잘라 냅니다. 그래서 `rect` 없이 `layer_place`하면 참조 위 정확히 원래 자리에 놓입니다. 태그와 폴더를 유지합니다 |
| `material_set_placement` | `state`, `id`, `canvas_rect`(null은 지움) 또는 `from`(다른 항목) | 픽셀을 바꾸지 않고 캔버스 위 항목의 기본 위치를 설정합니다. 확대 이미지에서 외부로 생성한 고해상도 이미지는 그 확대 이미지의 위치를 취하며, 늘어난 픽셀은 단순히 더 높은 텍스처 밀도가 됩니다 |
| `material_peel` | `state`, `id`(현재 이미지), `revealed`(파츠를 제거한 같은 크기의 생성 이미지), 선택 사항 `region`(같은 크기의 마스크. 파츠가 있을 수 있는 곳이 흰색이거나 불투명), `threshold`(색 차이 [low, high] 0–255, 기본값 [8, 30]), `grow`, `core`, `close`(기본값 2), `band`(기본값 24), `color_match`(기본값 true), `keep`(파츠 위의 `[x, y]` 점, 항목 픽셀, 1–64), `compare_scale`(1–8, 기본값 1), `name`, `part_id`, `base_id` | 파츠 하나를 벗겨 내어, `id`의 위치에 놓인 항목 두 개를 만듭니다. 파츠(`id` 자체의 픽셀이며, 투명도는 두 이미지의 차이에서 오고 `region`에 `grow`를 더한 범위로 제한됨. `core`는 축소한 마스크를 강제로 포함하고, `keep`은 그 점들에 연결된 조각만 남기고 멀리 떨어진 얼룩을 버림)와 그 아래의 이미지(파츠 영역 안에서만 `revealed`를 취하며, 폭 `band` 픽셀의 바뀌지 않은 주변 영역에 맞춰 Lab 색을 맞춤)입니다. 파츠 밖에서는 원래 픽셀이 유지되므로 파츠를 다시 쌓으면 원본이 됩니다. 이미지 전체의 톤 이동(채널별 차이의 중앙값)은 차이로 세지 않습니다. 생성 이미지가 더 작은 크기에서 확대된 것이라면 `compare_scale`로 확대 배율을 주며, 비교는 원래 생성 크기에서 실행되므로 확대로 생긴 선 차이는 세지 않습니다. 가장 앞의 파츠부터 시작해 새 베이스에서 다음 파츠를 벗겨 내세요 |
| `material_draw` | `state`, `id`, 선택 사항 `svg`, `edits`, `density`(0.05–16, 캔버스 단위당 텍스처 픽셀), `name`, `folder_id` | 파츠를 벡터로 그리거나 다시 그립니다. `id`가 비어 있으면 만듭니다. SVG의 `viewBox`는 캔버스 위 파츠의 위치(캔버스 단위)이며, 경로는 캔버스 좌표로 바로 씁니다. `density`로 렌더링합니다(생략하면 항목의 현재 밀도, 새 항목은 1. 각 변 ≤4096 픽셀). Skia가 렌더링할 수 있는 것(경로, 그라데이션, 클리핑, 마스크, `feGaussianBlur` 같은 필터, 불투명도) 외에, `<style>` 안의 요소, `#id`, `.class` 규칙을 지원합니다. `var(--name)`은 그리기 키트에서 색을 가져오고, 경로의 `data-taper`(시작에서 끝까지 고르게 분포한 폭 계수, 예: `"0.1 1 0.2"`)는 획을 양 끝이 가늘어지는 채운 선으로 바꿉니다. 블렌드 모드와 사용자 정의 CSS 변수는 렌더링되지 않습니다. 그것을 사용하는 레이어는 픽셀이 교체되며, 이전 위치에 배치된 레이어는 새 `viewBox`로 이동합니다. `edits`는 현재 그림(또는 함께 준 `svg`)을 요소 `id`로 국소적으로 바꿉니다. `set`은 속성을 설정하고, null은 속성을 제거하며, `text`는 `<style>`의 텍스트를 교체합니다. 예를 들어 경로 하나의 `d`나 색 하나만 바꿀 수 있습니다. `svg`와 `edits`가 모두 없으면 새 `density`로 다시 렌더링합니다 |
| `layout_set_drawing_kit` | `state`, 선택 사항 `colors`(이름 → `#rgb`/`#rrggbb`, 병합되며 null은 삭제), `styles`(class 규칙 스타일시트), `defs`(SVG 정의: 그라데이션, 필터) | 모든 그림이 공유하는 색, class 규칙, 정의입니다. `styles`와 `defs`는 통째로 교체되며, 그림 자체의 같은 이름 규칙과 id가 우선합니다. 모든 벡터 항목이 원래 해상도로 다시 렌더링되고, 변경이 레이어에 동기화됩니다. 어떤 그림에 색이 빠지게 되면 거부됩니다 |
| `reference_sample` | `points`(≤256개의 [x, y]), `radius`(캔버스 단위, 평균), `rect`, `colors`(1–16, 기본값 6) 중 하나 이상 | 참조의 색을 읽습니다. 각 점의 색과 알파, 그리고 `rect` 안 불투명 픽셀의 주요 색(밝기에 따라 어두운 것부터 밝은 것 순, 비율 포함)을 주어 키트 색을 고르는 데 씁니다 |
| `reference_trace` | `seed`([x, y]), 선택 사항 `tolerance`(Lab 거리, 기본값 12), `compare`(`seed`/`neighbor`), `simplify`(캔버스 단위, 기본값 0.6) | 마술봉: 시드에서 시드 색에 가까운 불투명 픽셀로 퍼져 나가며(`neighbor`: 인접 픽셀에 가까운 것으로, 부드러운 음영을 넘어 선까지 퍼질 수 있음), 바깥 윤곽의 단순화된 다각형 `polygon`과 그것을 지나는 매끄러운 닫힌 경로 `path`(캔버스 좌표, 그림의 첫 스케치로 바로 사용 가능), 면적, 경계 상자, 평균 색, 내부 구멍 수를 반환합니다. `touches_edge`는 참조의 가장자리까지 퍼졌다는 뜻입니다. `holes`(0–16)는 가장 큰 구멍들의 윤곽도 `hole_outlines`로 반환합니다. 배경에서 선택하면 그것이 캐릭터 실루엣입니다(캐릭터가 이미지 가장자리에 닿으면 실루엣은 바깥 윤곽에 있음) |
| `reference_snap` | `points`(1–256), 선택 사항 `radius`(캔버스 단위, 기본값 4) | 점을 `radius` 안에서 가장 가까운 선의 중심에 붙입니다(주변보다 확실히 어두운 픽셀 중 가장 가까운 것을 찾은 뒤, 그 근처의 가장 어두운 지점). 근처에 선이 없는 점은 그대로이며 `snapped`가 false입니다 |
| `layer_place` | `state`, `material_id`, 선택 사항 `rect`(단일 항목), `at`([x, y] 중심), `layer_id`(단일 항목의 새 레이어 ID), `reference`(`true` / `false`는 모든 레이어를 참조 또는 모델 레이어로 배치. 생략하면 각 항목이 자기 표시를 따름) | 항목이나 폴더 전체를 새 원본 레이어로 배치하며, 기존 레이어 위에 그려집니다. 기본적으로 PSD 위치에 놓이고(PSD 캔버스를 프로젝트 캔버스 중앙에 맞추며, 더 크면 비례해 축소하고 크기가 같으면 제자리), 그 밖에는 소재 1픽셀 = 캔버스 1단위로 캔버스에 맞게 스케일합니다. 픽셀은 다시 샘플링하지 않습니다 |
| `layer_tag` | `state`, `layer_ids`(1–128), `set` 또는 `add`/`remove` | 의미 태그: 파츠 단어와 `left/right`가 분류를 결정하고(예: `{hair, front}`, `{eye, iris, left}`), 나머지는 사용자 태그입니다. 생성된 레이어의 분류 변경은 `rig_generate`를 기다립니다 |
| `layer_set_variant` | `state`, `layer_id`, `variant`(`none/toggle/switch`), 선택 사항 `parameter`, `switch_id` | 차분 속성. 태그는 유지됩니다 |
| `layer_mark_reference` | `state`, `layer_ids`(1–128), `reference` | 레이어를 참조 레이어로 만들거나(`true`) 모델로 되돌립니다(`false`). 참조 레이어는 캔버스와 쌓는 순서에 남지만 어떤 생성도 읽지 않으며 리그와 내보내기에서 빠집니다. 아직 생성되지 않은 레이어, 대기 중인 레이어, 참조 레이어만 바뀌며, 생성된 레이어는 거부됩니다. 생성된 프로젝트에서 되돌린 레이어는 `rig_generate`를 기다립니다. `workspace_inspect scope=generation`이 이들을 `reference_layers`에 나열합니다 |
| `layer_pin_reference` | `state`, `layer_ids`, `points`(키폼 점의 배열) 또는 단일 `pin` | 참조 레이어를 키폼 점에 고정합니다. 각 점은 파라미터 ID를 값에 대응시키며(각각 범위 안, 최대 16개 파라미터), 참조는 그 점 중 어디에서든(모든 파라미터가 범위의 0.5% 이내) 표시되고 다른 포즈에서는 숨겨집니다. `[]`, `{}`, null은 고정을 해제합니다. 이전 점들을 교체합니다 |
| `reference_place_grid` | `state`, `material_id`, `x_parameter`, 선택 사항 `y_parameter`, `x_values`, `y_values`, `columns`, `rows`, `pixel_rect`, `rect`, `id_prefix` | 키폼 격자(예: 3×3 머리 각도)로 그린 참조 시트를 균등한 셀로 잘라, 각 셀을 소재로 잘라 내고 그 키폼에 고정된 참조 레이어로 배치합니다. 열은 왼쪽에서 오른쪽으로 `x_values`를, 행은 위에서 아래로 `y_values`를 따르며, 이는 `view_render_pad`가 쓰는 순서입니다. 생략한 값은 파라미터의 키폼 값이며(2D 패드처럼 y는 큰 값부터), 그때는 개수가 `columns`/`rows`와 같아야 합니다. `y_parameter`가 없으면 시트는 한 행입니다. `pixel_rect`는 이미지 픽셀 기준의 격자 영역(기본값은 전체), `rect`는 모든 셀이 캔버스 위에 놓일 위치입니다(기본값은 각 셀을 캔버스에 맞춰 중앙에 배치). 레이어와 소재 ID는 `<id_prefix>_<row>_<column>`입니다. 히스토리 노드 하나입니다 |
| `material_mark_reference` | `state`, `ids`(항목 또는 폴더, 1–1024), `reference` | `layer_place`가 지정하지 않을 때 항목(폴더 안의 모든 항목)을 참조 레이어로 배치할지 여부 |
| `layer_reorder` | `state`, `layer_ids`(1–128), `to`(`front/back/forward/backward/above/below`). `above/below`는 `target` 필요 | 쌓는 순서(즉 기본 그리기 순서)를 바꿉니다. 맨 앞이나 맨 뒤로, 움직이지 않는 레이어 하나를 넘어서, 또는 `target` 바로 위나 아래로 옮기며, 옮긴 레이어들은 상대 순서를 유지합니다. 생성기가 읽지 않은 레이어(생성되지 않은 프로젝트의 레이어, 대기 중인 레이어)만 받습니다. 생성된 레이어는 오류를 반환하며, 그 그리기 순서는 `layer_draw_order`로 바꿉니다 |
| `rig_generate` | `state`, 선택 사항 `refresh_geometry` | 생성하거나 다시 생성합니다. 생성되지 않은 프로젝트는 현재의 모든 레이어로 구성됩니다. 생성된 프로젝트는 마지막 생성 이후 배치된 레이어와 대기 중인 분류를 받아들이며, 3방향 병합으로 사용자 편집을 유지하고, 깔끔하게 이전되지 않은 것은 `quality.regeneration`에 남깁니다. `refresh_geometry`는 현재 픽셀로 메시를 다시 만들 생성된 레이어를 나열합니다. 생략하면 이미지가 교체된 모든 레이어이며, `[]`는 모두 유지합니다 |

생성되지 않은 프로젝트, 빈 캔버스, 대기 중인 레이어는 일반 편집 작업 공간을 사용하며, 메시, 디포머, 파라미터, 계층, 회전, 텍스처 편집이 가능합니다. 내보내기에는 현재 모델이 포함됩니다. `rig_generate`는 기존 메시 토폴로지 위에서 태그로부터 바인딩과 파라미터를 맞추며, 명시적인 `refresh_geometry`는 그림을 다시 추적합니다. 가져온 PSD 프로젝트도 각 레이어를 소재로 등록하고 연결합니다.

일반적인 흐름: `project_create` → `material_import` → `material_list` → 여러 번의 `layer_place`(또는 PSD 폴더 전체 배치) → `layer_tag` → `layer_reorder` → `rig_generate` → 작성 편집 → 추가 `layer_place` → `rig_generate`. 외부 도구가 이미 파츠를 나눴다면 Layout 매니페스트를 작성하고 `project_import_layout`으로 배치가 끝난 생성 전 프로젝트를 한 번에 얻은 뒤, 확인하고 `rig_generate`하세요.

`view_render_model`은 참조 레이어(`layer_mark_reference`)를 모델 아래에 그립니다(`reference.mode`가 `difference`이면 모델만 비교). `view_render_model`에는 `grid`(일정 캔버스 단위마다 좌표 레이블이 붙은 격자선. 너무 촘촘하면 ×2, ×5, ×10…으로 넓어지며, 결과의 `gridStep`이 실제 간격)와 `outline_material_ids`(캔버스 위치가 있는 항목, 즉 그림과 잘라 낸 확대 이미지의 불투명 가장자리를 각자의 색과 id로 뷰 위에 그림)도 있어, 좌표를 읽고 그림을 참조와 비교하는 데 씁니다. 참조만 보려면 `include_layer_ids: []`와 `reference: {mode: under, opacity: 1}`을 주세요.

`view_render_pad`는 키폼 격자 위에 모델을 펼쳐 레이블이 붙은 시트 하나에 2D 패드 전체를 한 번에 보여 줍니다. 열은 왼쪽에서 오른쪽으로 `x_values`에서의 `x_parameter`이고, 행은 위에서 아래로 `y_values`에서의 `y_parameter`입니다. 생략한 값은 파라미터의 키폼 값이며(y는 큰 값부터), `y_parameter`를 주지 않으면 2D 패드의 파라미터는 그 짝을 가져옵니다. `columns` / `rows`(`[from, to]`, 그 값들에 대한 양 끝 포함 인덱스)로 임의의 부분 사각형을 잘라 낼 수 있습니다. 예: 5×3 패드의 `columns: [1, 3]`, `rows: [0, 1]`. `show`는 각 셀에 무엇을 담을지 고릅니다. `model`(모델만), `reference`(그 키폼에서 표시되는 참조 레이어. 그곳에 고정되었거나 고정되지 않은 것), `under` / `over`(모델 아래의 참조, 또는 `reference_opacity`로 모델 위에 겹친 참조), `difference`(일치하는 곳은 검은색이며 각 셀에 0–1의 `difference`가 붙음), `side_by_side`(모델 | 참조). 각 셀의 `imageRect` `[x,y,width,height]`는 `canvasRect`에 대응하고, 셀은 표시하는 참조 레이어를 나열합니다. 셀은 최대 64개이며, `target_long_edge` / `max_bytes`가 시트 전체를 제한합니다. 다른 곳에서 같은 순서로 그린 참조 격자는 `reference_place_grid`로 고정된 참조로 다시 읽어 들일 수 있습니다.

참조에서 파츠를 그리는 흐름: `layout_set_reference` → `reference_sample`로 색 확인, `layout_set_drawing_kit`로 색과 선 class 설정 → `grid`를 켠 `view_render_model`로 파츠 확대, `reference_trace`로 윤곽, `reference_snap`으로 선 위의 점 보정 → `material_draw`(가려진 부분은 형태를 따라 완전하게 그리고, 그림의 결함은 따라 그리지 않음) → `outline_material_ids`로, 또는 배치 후 `reference`로 비교 → 반복 수정 → `layer_place`, `layer_tag` → `rig_generate` → `view_render_poses`로 회전 시 드러나는 부분 확인.

참조에서 파츠를 나누는 흐름: 참조를 `material_import` → `layout_set_reference` → `material_crop`(`canvas_rect`)으로 확대 이미지 → `material_export_png` → 외부에서 확대하거나 다시 그림 → `material_import` → `material_set_placement`(확대 이미지에서 `from`) → 레이어별로: 외부에서 가장 앞의 파츠를 제거한 이미지를 생성 → `material_peel` → 새 베이스에서 계속 → `layer_place` → `view_render_model`의 `reference`(`difference`로 정렬 확인. `referenceDifference`는 평균 차이)와 `density`(텍스처 밀도 히트맵. `texelDensity`는 각 레이어의 캔버스 단위당 텍스처 픽셀을 나열)로 확인 → `layer_set_canvas_rect` / `layer_transform`으로 미세 조정 → `layer_tag` → `rig_generate` → `view_render_poses`로 움직임 확인.

## 에셋 작업 흐름 (asset_*)

`asset_prepare_reference / asset_import_png / asset_register / asset_reprocess`는 프로세스 작업 핸들을 반환하며 모두 `request_id/project_id/state`가 필요합니다. 업무 결과와 커밋된 `state/project_id/history_node_id`는 `job_wait/job_get`의 `data.result`에서 읽습니다. 참조 이미지, 등록 미리보기, 재처리된 이미지는 반복해서 가져올 수 있습니다. 연결을 끊거나 대기를 취소해도 작업은 멈추지 않으며, 원래 요청을 재시도하면 같은 작업을 받습니다. 준비 실패, 커밋 전의 취소, 상태 충돌은 에셋을 게시하지 않으며, CAS 이후의 늦은 취소는 completed와 실제 결과를 유지합니다.

네 가지 쓰기 작업은 별도의 애플리케이션 계층 에셋 세션을 공유합니다. 먼저 비공개 후보를 준비한 다음, 보조 CAS 안에서 변경 불가능한 파일과 에셋 매니페스트를 게시합니다. 실제로 에셋을 추가하면 이전 상태가 무효화되지만 리그 리비전은 바뀌지 않고 히스토리도 추가되지 않습니다. 같은 내용과 매핑을 다시 가져오면 변경 없음입니다. 검사와 시험 합성은 GUI 초안이나 이후의 등록이 아니라 캡처한 문서와 에셋 매니페스트를 사용합니다. 저장은 캡처 시점에 프로젝트에 속해 있던 에셋(`workspace.json`의 `assetCatalog`)만 복사합니다. PNG 가져오기는 픽셀 메모리를 할당하기 전에 1,600만 픽셀 한도를 확인합니다.

새 에셋은 보통 `reference → import → register → preview → add`를 사용하며, 필요하면 `place → finalize`를 추가합니다. `project_create_artwork`는 배치된 에셋으로 새 프로젝트를 백그라운드에서 만들 수 있으며, 전환할 때 기본적으로 저장하지 않은 변경이 있으면 거부합니다. `source_split_polygon`은 캔버스 다각형으로 안쪽과 나머지로 나누고, `source_split_components`는 기존 픽셀을 현재 메시의 연결 구성 요소에 따라 나눕니다. 둘 다 가려진 내용을 그려 넣지는 않습니다.

`source_get_components`는 `layer_id`를 받는 읽기 전용 조회로, 캡처한 `project_id/state/revision`, `can_split/count`, 구성 요소 중심 `canvas_x/canvas_y`를 반환합니다. 구성 요소는 원본 Y, 그다음 X 순으로 정렬됩니다. 이 순서대로 `source_split_components`의 `names`에 이름을 붙이며, 선택 사항으로 같은 길이의 `sides`와 중복 없는 `piece_ids`를 줍니다. 이름은 앞뒤 공백을 제거한 뒤 비어 있지 않고 중복이 없어야 하며, 개수는 현재 구성 요소 수와 맞아야 합니다. 좌우를 생략하거나 `none`을 쓰면 원래 레이어의 것을 이어받습니다. 구성 요소 감지는 조회가 캡처한 현재 메시를 사용합니다. 이후의 변경은 이전 조회 결과를 바꾸지 않으며, 커밋에는 그 캡처한 상태를 넘겨야 합니다.

`source_split_polygon/source_split_components`는 `request_id/project_id/state`가 필요하며 백그라운드 작업 핸들을 반환합니다. `job_get/job_wait`의 `result`는 새 상태, 히스토리 노드, `layers`의 새 원본 레이어 ID를 담습니다. 다각형에는 3–32개의 캔버스 정점과 두 개의 `names`(안쪽, 나머지)가 필요하며, 양쪽 모두 보이는 픽셀이 있어야 합니다. 중복 없는 `piece_ids` 두 개는 선택 사항입니다. 새 ID를 주면 같은 원자적 일괄 처리의 뒤 구성원이 이를 참조할 수 있으며, 일괄 처리의 `changed`에는 새 원본 레이어의 `layer:<id>` 핸들과 생성된 오브젝트 핸들이 모두 포함됩니다. 준비/다시 빌드는 취소할 수 있으며, 커밋 전의 충돌이나 실패는 원래 문서를 유지합니다. 커밋 후의 늦은 취소나 새로 고침 예외는 완전한 성공 최종 상태를 유지하며, 원래 요청은 같은 작업을 돌려받습니다.

조각은 원래 원본 레이어를 대체합니다. 원래 레이어는 소스에서 빠지며(`deletedLayerIds`에 들어가지 않음), 조각은 분류, 부모, 가시성, 레이어별 메시와 그리기 순서 재정의를 이어받은 일반 원본 레이어가 됩니다. 분할은 재생성입니다. 조각은 원래 레이어의 위치에서 생성에 참여하며, 원래 레이어에 있던 사용자의 키폼, 채널, 블렌드 셰이프, 경로, 정점 가중치, Glue는 3방향 병합으로 조각에 이어집니다(흔들림과 시뮬레이션 생성 축 제외). 이후의 얼굴 설정, 자세, 뼈대, 분류 변경도 조각에 반영되며, 시뮬레이션 대상, 베이크 오프셋, 연결 역할은 조각으로 이전됩니다. 저널은 분할의 `art_primitive` 기록 뒤에 체크포인트를 쓰며, 그 기록 자체는 더 이상 재생되지 않습니다. 아틀라스와 내보내기에는 더 이상 원래 레이어가 포함되지 않습니다. `layer_restore`(`layer_ids`를 생략한 전체 복원 포함)로는 되살릴 수 없으며, 실행 취소/히스토리 전환으로 분할 이전으로 돌아가야만 됩니다. 이후에 원래 레이어나 메시 ID를 참조하는 요청은 실패하며, 오류 메시지가 그것을 대체한 조각 ID를 나열합니다. `source_get_components`도 마찬가지입니다. 조각은 다시 분할할 수 있습니다. 가져온 CMO3 모델은 여전히 예전 규칙대로 원래 레이어를 소프트 삭제합니다. 기록 형식은 [문서 계층](../spec/DOCUMENT_LAYER.md#분할-실체화-아트-프리미티브-레코드-art_primitive)을 참고하세요. 일반, 분할, 신규, 가져온 CMO3 메시는 단일 레이어/전역 메시 갱신과 초기화(삭제된 메시 복원 포함)에서 순서 있는 교체를 통해 바인딩과 기존 시뮬레이션 오프셋을 유지합니다. 다각형 분할로 생긴 새 절단점과 경계 정점 복사본은 방향성 Glue를 통해 원래 정점을 소유한 조각을 따르므로, 절단면 양쪽이 붙어 있습니다. 삭제된 소스, 마스크(클리핑)된 소스, 채널 마스크된 소스, normal이 아닌 블렌드 소스는 분할할 수 없습니다. 구성 요소 조회는 실제 텍스처 주소를 사용하며 원본 패딩, 회전, 스케일을 지원하므로, 편집된 일반 대상도 `can_split:true`를 반환할 수 있습니다. GUI의 메시 연결 구성 요소 분할 대화 상자는 같은 애플리케이션 명령을 호출하고, 감지 시점의 상태를 유지하며, 여러 레이어에 대한 결정을 사용자 히스토리 커밋 하나로 커밋합니다. 다각형 분할은 현재 MCP 진입점이므로, 이를 근거로 GUI에 올가미 분할 메뉴가 있다고 주장할 수는 없습니다.

`rig_update_generation`은 이 빌드의 생성기로 리그를 다시 생성합니다(GUI의 **모델 → 생성 결과 업데이트**에 해당). 저널에 체크포인트가 있는 프로젝트는 체크포인트를 기록한 시점의 생성기 출력을 유지하며, 프로그램을 업그레이드해도 자동으로 바뀌지 않습니다. 이 작업은 체크포인트에 저장된 생성 스냅숏, 같은 입력에 대해 이 빌드가 지금 생성하는 결과, 현재 작성된 리그를 3방향 병합합니다. 사용자가 바꾸지 않은 오브젝트는 새 출력을 따르고, 사용자의 변경은 유지되며, 깔끔하게 이전되지 않은 것은 `issues`에 나열되고(`workspace_inspect`의 `quality.regeneration`에도 들어감), 결과는 새 체크포인트로 저널 끝에 추가됩니다. 저널의 이전 재생성 체크포인트를 이 빌드가 병합하면 다른 구조(다른 부모, 정점 수, 키폼 축, 블렌드 셰이프)가 나오는 경우, 먼저 이 빌드로 다시 병합하고, 그 수정은 3방향 병합으로 이후 체크포인트와 현재 작성된 리그로 이어집니다(issue `remerged`, `target`은 `journal:<index>`, `detail`은 구조가 다른 오브젝트를 나열). 이후의 편집은 유지됩니다. 요청에는 `request_id/project_id/state`만 필요하며, 백그라운드 작업 핸들을 반환하고, 원자적 일괄 처리 구성원이기도 합니다. 작업 `result`는 `state/project_id/history_node_id/revision/applied`, `updated`, `issues`(각각 `{kind, target, detail?}`)입니다. 생성 출력이 같고 모든 병합이 일치하면 `updated:false`이며 히스토리 노드를 추가하지 않습니다. 체크포인트가 없는 프로젝트는 어차피 빌드할 때마다 이 빌드로 생성되고, 가져온 CMO3 모델에는 생성 출력이 없으므로, 둘 다 업데이트할 필요가 없습니다(후자는 거부됨).

`source_split_depth`는 GUI의 앞뒤 분할 메뉴, 다중 선택, 대화 상자에 해당합니다. 현재 메시의 원래 `source_id`와 비어 있지 않고 중복 없는 `middle_ids`(`mesh:` 접두사 없이, 소스는 제외)를 넘깁니다. 선택 사항 `names`는 뒤 레이어와 앞 레이어의 비어 있지 않고 서로 다른 이름 두 개이며, 선택 사항 `front_layer_id/front_mesh_id/back_layer_id/back_mesh_id/glue_id`는 새 ID를 줍니다. 소스 메시와 그 레이어를 뒤와 앞의 새 레이어와 메시 두 쌍으로 교체하며, 둘 다 원래 픽셀을 가지고 각각 따로 칠할 수 있고, 중간 메시들이 그 사이에 놓입니다. 결과의 `layers`는 앞, 뒤 순서입니다. 두 레이어 모두 소스 메시의 현재 부모, 기하, 채널 키폼, 블렌드 셰이프, 경로, 정점 가중치를 취합니다. 뒤 레이어는 소스 메시의 Glue, 마스크, 시뮬레이션 대상을 넘겨받으며, 방향성 Glue가 앞 레이어를 뒤 레이어에 따라오게 합니다. 두 레이어는 고정된 그리기 순서를 사용하며, 원래의 그리기 순서 애니메이션을 대체합니다. 입과 파생 입술은 선택한 메시만 복사하며 추가 입술을 생성하지 않습니다. 지워진 앞 레이어도 토폴로지는 유지합니다. 가져온 CMO3 모델은 예전 기록을 유지합니다. 소스 메시와 그 레이어는 남고, 그 위에 앞 레이어와 방향성 Glue가 복제됩니다(새 뒤 레이어 없음).

단일 호출도 마찬가지로 `request_id/project_id/state`가 필요하며, 작업의 최종 `layers`는 새 레이어 ID입니다(가져온 CMO3 모델은 새 앞 레이어만). 중복 없는 `front_layer_id/front_mesh_id/back_layer_id/back_mesh_id/glue_id`를 주면 같은 원자적 일괄 처리의 뒤 구성원이 새 오브젝트를 칠하거나 편집할 수 있으며, 일괄 처리의 `changed`는 `layer:<id>/mesh:<id>/glue:<id>`를 반환합니다. GUI는 메뉴나 대화 상자를 열었을 때의 상태를 유지하며, 보조 데이터의 변경도 이전 확인을 무효화합니다. 준비는 취소할 수 있으며, 커밋 후에는 정확한 최종 상태가 유지됩니다. 히스토리 재생, 저장과 다시 열기, CMO3 다시 읽기는 모션과 Glue 연결 가중치를 유지합니다. 이 작업은 기존 내용을 복사할 뿐이며, 가려진 부분은 여전히 그려 넣어야 합니다.

기존 PSD는 로컬 절대 경로에서 `project_import_psd`로 엽니다. 여섯 가지 페인트 작업 `source_paint_brush / source_paint_pencil / source_paint_eraser / source_paint_bucket / source_paint_shape / source_paint_clear`는 프로세스 작업 핸들을 반환하며, 커밋된 상태, 히스토리 노드, 생성된 핸들은 `job_wait/job_get`을 통해 `data.result`에서 얻습니다. 기능 상세에는 `job_backed:true`, `batchable:true`가 표시되어 있고 `job_result_schema`가 제공됩니다. 브러시, 지우개, 페인트 통, 도형은 UI의 래스터 알고리즘을 사용합니다. 점, 반경, 선 폭은 캔버스 단위이며 레이어 자체의 래스터에 그려집니다. 래스터 밀도(래스터 픽셀 / 캔버스 단위, [레이어별 크기](../spec/DOCUMENT_LAYER.md#레이어별-크기) 참고)가 1인 레이어는 이전과 같이 동작하고, 32단위 사각형에 1024² 래스터를 저장한 레이어에서는 반경 2의 브러시가 래스터에 반경 64픽셀의 획을 그리며, 커밋 후 래스터는 캔버스 해상도로 줄어들지 않고 원래 해상도를 유지합니다. 사각형 밖에 칠하면 같은 밀도로 사각형이 확장되고, 불투명 픽셀로 잘라 내도 밀도는 유지됩니다. 완전히 지운 레이어는 캔버스 해상도의 투명 픽셀 하나를 유지합니다. 비공개 초안도 레이어 래스터를 기반으로 합니다. `paint_session_begin` 등의 결과에 있는 `width/height`는 초안 래스터 픽셀이고, `canvas_rect`는 초안이 덮는 캔버스 사각형 `[left, top, width, height]`이며, 색 추출은 캔버스 픽셀 중심에서 래스터를 읽습니다. 초안이 1,600만 픽셀을 넘으면 사각형 주변의 칠할 수 있는 여백이 줄어듭니다. 기본값 `rebuild_mesh:false`는 기존 메시와 모든 바인딩을 유지하고 레이어의 메시로 클리핑합니다. 메시 위에 떨어진 픽셀은 레이어에 남고(소재에 연결된 레이어는 소재에 기록되며 연결된 모든 레이어가 함께 갱신됨), 메시 밖의 픽셀은 바로 위의 새 레이어로 분리됩니다(원래 이름에 `+`를 붙인 이름, 자체 메시를 가지며 같은 부모와 파츠 아래에 있고, 원래 레이어의 소재가 있는 폴더에 새 소재로 놓여 연결됨). 메시 밖을 지우는 것은 효과가 없습니다. 아직 메시가 없는 레이어는 클리핑되지 않습니다. 레이어 자체의 메시가 새 영역을 덮어야 한다면 `rebuild_mesh:true`를 넘기세요. 레이어가 모든 픽셀로 확장되고 키폼, 블렌드 셰이프, 경로, 정점 그룹, Glue가 이전됩니다. 두 방식 모두 커밋 하나, 히스토리 노드 하나입니다. 기존 키폼, Warp, Glue는 더 이상 페인트를 막지 않습니다. `clear`나 모든 픽셀 지우기는 레이어와 바인딩을 유지하며, 마지막 레이어도 지우고 다시 칠할 수 있습니다. 레이어 삭제는 `layer_soft_delete`로 합니다. 앞뒤 분할의 앞 레이어는 항상 토폴로지를 유지합니다. 편집된 일반 레이어, 앞뒤 분할 레이어, 가져온 CMO3 레이어의 삭제/복원은 먼저 편집을 재생한 다음 활성 오브젝트를 걸러 내며, 복원에 필요한 ID, 키폼, 바인딩을 유지합니다. 삭제 중에 메시 설정을 바꿔도 숨겨진 메시의 다시 바인딩 결과는 저장됩니다. 예전 프로젝트에서 처음 실제로 삭제하거나 복원할 때 새 후보에 정체성과 생성 기준선을 고정하므로, 레이어를 복원해도 기존 편집이 다른 메시로 바뀌지 않습니다. 예전 히스토리 노드는 다시 쓰지 않습니다. `layer_mesh_update`는 단일 레이어의 메시 파라미터를 바꾸며, 초기화한 뒤에는 전역 값을 이어받습니다.

일반 투명 레이어는 처음으로 보이는 픽셀이 그려질 때 먼저 `rebuild_mesh:true`를 넘기지 않아도 자동으로 메시를 만듭니다. 입에 처음 칠하면 파생 입술과 그 생성 키폼도 만들어집니다. 단일 및 일괄 작업 결과의 `changed`에는 새 오브젝트의 `mesh:<id>` 같은 핸들이 들어 있어 계속 편집할 수 있습니다. 같은 요청을 재시도하면 같은 작업을 돌려받으며, 다시 만들거나 히스토리를 추가하지 않습니다. 다시 빌드 플래그를 넘겨도 완전히 지우면 메시와 바인딩이 유지됩니다.

생성된 입술은 명시적으로 다시 빌드할 때나 처음 만들 때 저장된 윤곽과 색을 따릅니다. 메시를 유지하는 이후의 페인트는 파생 텍스처를 다시 생성하지 않습니다. GUI의 고정된 픽셀과 MCP 제스처는 별도의 `WorkspaceRasterCommands`를 통해 문서 후보, 다시 빌드, 상태 확인을 공유합니다. 생성 데이터는 순서 있는 저널에 들어가며, 저장/다시 열기와 내보내기는 현재 텍스처를 다시 해석합니다. 준비는 진행 상황을 보고하고 취소를 확인합니다. 커밋 전의 취소는 원래 픽셀과 히스토리를 유지하며, 커밋에 성공한 뒤의 늦은 취소나 새로 고침 실패도 완전한 completed 결과를 유지합니다. 연결을 끊거나 대기를 취소해도 작업은 멈추지 않습니다. 새 메시에 대한 단일 레이어 설정 갱신도 문서에 기록되며, ID를 유지하고 경로와 가중치를 다시 바인딩합니다.

`layer_delete`와 `layer_restore`는 둘 다 프로세스 작업을 반환하고 원자적 일괄 처리를 지원합니다. 실제 상태, 히스토리 노드, 영향받은 레이어는 `job_wait/job_get`의 `result`에서 읽습니다. `layer_delete`는 레이어를 실제로 삭제합니다. 픽셀이 프로젝트에서 빠지고, 그 메시는 저널 기록 `layer_delete`를 통해 그것을 참조하는 마스크, Glue, 경로, 가중치와 함께 작성된 리그에서 빠집니다. 생성기가 읽는 레이어는 생성 입력에서도 빠지므로, 이후의 재생성은 더 이상 그 레이어의 메시를 생성하지 않으며 그 레이어가 어떤 구조에도 영향을 주지 않습니다. 이전 히스토리 노드에는 남아 있으며, `history_checkout`이 실행 취소입니다. 마지막 레이어는 삭제할 수 없습니다. 입술 띠나 예전의 좌우 반쪽처럼 독립된 원본 레이어가 아닌 행은 여전히 예전 방식으로 숨깁니다. `layer_restore`는 이전 버전이 소프트 삭제한 레이어만 복원합니다. 중복 없는 1–256개의 `layer_ids`를 넘기거나, 생략해 모두 복원합니다(분할로 대체된 원래 레이어는 포함되지 않으며, 그런 레이어를 지정하면 조각을 나열하는 오류를 보고). 삭제된 것이 없으면 변경 없음이며 히스토리를 추가하지 않습니다.

전역 메시 단위는 `settings_update`의 `meshUnits`입니다. `DOCUMENT`는 문서의 긴 변 2048픽셀을 기준으로 하므로 같은 이미지를 확대해도 같은 밀도를 사용하고, `PIXELS`는 원본 픽셀 길이를 사용합니다. 두 모드 모두 레이어별 파라미터는 여전히 선택한 단위의 길이를 뜻합니다. `meshSpacing`은 있지만 단위가 없는 예전 v1 설정은 `PIXELS`를 유지하므로 다시 열어도 예전 메시가 바뀌지 않습니다. 새 설정, 생성 기준선, 내보내기는 실제 단위를 저장합니다. GUI의 단위 토글과 공개 변경은 같은 후보 재생성과 토폴로지 이전을 사용합니다. `meshTrace`는 윤곽이 무엇을 읽을지 정합니다. `TEXTURE`(새 프로젝트의 기본값)는 레이어 자체의 픽셀을 최대 알파로 줄여 메시 단위보다 최대 4배 고운 해상도로 읽으므로, 캔버스 픽셀보다 가는 획도 메시 안에 남고 정점 간격은 바뀌지 않습니다. `CANVAS`는 캔버스 해상도로 스케일한 뷰를 읽습니다. 이 필드가 없는 기존 설정은 `CANVAS`로 읽히며, `CANVAS`는 필드를 쓰지 않으므로 예전 프로젝트의 설정 텍스트와 메시는 그대로입니다. `meshWrap`(0–64 메시 단위, 기본값 0)은 감싸기 토폴로지입니다. 윤곽을 추적하기 전에 값보다 좁은 틈과 홈을 닫아, 속눈썹이나 머리카락 끝 같은 작은 돌출부가 하나의 외곽을 공유하고 그려진 모든 픽셀이 메시 안에 남습니다. `layer_mesh`의 `changes.wrap`이 레이어별로 재정의하며, `inspect`의 레이어 메시 설정은 실제 `wrap`을 반환합니다. 0이면 필드를 쓰지 않으며, 필드가 없는 설정은 0으로 읽힙니다. [메시 토폴로지](../spec/MESH_TOPOLOGY_AND_SPLIT.md#감싸기-토폴로지)를 참고하세요.

`import`는 기존 로컬 PNG의 절대 경로나 이미지 바이트를 받습니다. 모델이 Base64를 한 글자씩 생성하게 하지 마세요. `solid_background`를 생략하면 원래 알파를 유지합니다. 배경을 제거하려면 실제 단색을 명시적으로 주세요. 체커보드 스크린숏은 투명 에셋이 아닙니다.

`register`는 프레임, 대응 앵커, 또는 절대 변환을 사용할 수 있습니다. 앵커 목표 위치는 캔버스 좌표이고, 생성 이미지 좌표는 원래의 전체 PNG 픽셀입니다. 미러와 불균일한 늘이기는 명시적으로 선언해야 합니다. 해상도가 높다고 모델에서의 차지 면적이 자동으로 커져서는 안 됩니다.

정식으로 추가하거나 교체 레이어를 삭제하기 전에 먼저 시험 합성하고 부모, 그리기 순서, 마스크, 모션 여유를 확인하세요. 이미 독립 바인딩, 키폼, 완료된 위치 지정이 있는 에셋은 전체 재배치를 거부할 수 있으므로, 실제 오류와 스키마를 기준으로 하세요.

## 관련 구현

- [공개 스키마와 적응](../../../src/main/kotlin/io/github/psd2live/application/WorkspaceAuthoringOperations.kt)
- [에셋 도구](../../../src/main/kotlin/io/github/psd2live/application/WorkspaceAssetCommands.kt)
- [경로 도구](../../../src/main/kotlin/io/github/psd2live/application/WorkspacePathEdits.kt)
- [프로젝트 저장소](../../../src/main/kotlin/io/github/psd2live/project/WorkspaceStore.kt)

이 페이지는 인터페이스를 기록한 것이며, [실측 기록](../STATUS.md)의 평가를 올리는 근거가 아닙니다. 실제 결과는 여전히 실제 모델, 호스트, 작업 샘플로 검증해야 합니다.

### 내부 조형 작성

`interior_put`은 네이티브 도크와 공유하는 백그라운드, 일괄 처리 가능 작업입니다. `state`와 `target`(메시 id 또는 `mesh:<id>`)이 필요하며, 선택 사항으로 `enabled`, `settings`, `motion`, `parameters` id 두 개, `guides`(최대 64개)가 있습니다. enabled만 갱신하면 다시 베이크하지 않고 레시피를 유지합니다. 바인딩을 생략하면 몸 X/Y를 우선하며, 빈 id를 명시하면 비공개 소스를 만듭니다. 저장된 네이티브 필드와 물리는 표준 내보내기 경로를 사용합니다. 범위와 안전성/복구 동작은 내부 조형 명세를 참고하세요.

`interior_delete`는 효과와 그것이 소유한 물리를 제거하되, 외부에서 참조하는 파라미터는 유지합니다. `scope=interiors`를 지정한 `workspace_inspect`는 형태 배열 없이 전체 settings/guides, 입력/출력 ID, 문제 상태를 반환합니다. 중첩된 settings/motion 스키마는 부분 업데이트를 허용하며 범위/단위를 설명합니다.

### 눈 구조 보정

`module_catalog.eye_structure`는 눈 구성 요소와 그 `sourceStamp`를 보고합니다. `settings`에서 레이어 ID, 그다음 구성 요소 ID를 키로 하는 `changes.rig.eyeParts`를 사용해 `side`(left/right), `region`(upper/lower), `primitive`(line/tuft/side/skin/auto), `contacts`, `path`를 보정합니다. 구성 요소의 첫 패치에는 그 `sourceStamp`가 필요합니다. 좌표는 눈 로컬이며, 눈 폭으로 정규화되고, 두 번째 축은 얼굴 아래쪽을 향합니다. null은 구성 요소나 레이어를 초기화합니다. 원본 스탬프가 맞지 않는 보정은 저장된 채로 남지만 적용되지 않습니다. 이 편집은 일반 설정 히스토리와 프로젝트 저장을 사용합니다.
