#!/usr/bin/env bash
# PSD2Live를 설치 없이 현재 소스(체크아웃된 브랜치) 그대로 빌드해서 실행합니다.
#
#   ./start.command            GUI 실행
#   ./start.command --lang ko  앱 인수 전달 (예: 언어)
#
# Finder에서 더블클릭해도 실행됩니다. 처음 실행은 빌드 때문에 몇 분 걸립니다.
# JDK 21이 필요합니다: JetBrains Runtime(빌드 캐시) → Homebrew openjdk@21 → 시스템 JDK 21 순서로 찾습니다.

set -euo pipefail
cd "$(dirname "$0")"

find_jdk() {
	if [[ -n "${JAVA_HOME_21:-}" && -x "$JAVA_HOME_21/bin/java" ]]; then echo "$JAVA_HOME_21"; return; fi
	local jbr
	jbr="$(ls -d "$HOME"/Library/Caches/psd2live-build/jbrsdk-21*/Contents/Home 2>/dev/null | tail -1 || true)"
	if [[ -n "$jbr" && -x "$jbr/bin/java" ]]; then echo "$jbr"; return; fi
	for brew in /opt/homebrew/opt/openjdk@21 /usr/local/opt/openjdk@21; do
		if [[ -x "$brew/libexec/openjdk.jdk/Contents/Home/bin/java" ]]; then echo "$brew/libexec/openjdk.jdk/Contents/Home"; return; fi
	done
	if /usr/libexec/java_home -v 21 >/dev/null 2>&1; then /usr/libexec/java_home -v 21; return; fi
	return 1
}

if ! JDK="$(find_jdk)"; then
	echo "JDK 21을 찾지 못했습니다. 설치: brew install openjdk@21" >&2
	echo "(다른 위치의 JDK 21을 쓰려면 JAVA_HOME_21=/path/to/Home ./start.command)" >&2
	exit 1
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo '?')"
echo "==> PSD2Live 소스 실행: 브랜치 $BRANCH ($COMMIT)"
echo "==> JDK: $("$JDK/bin/java" -version 2>&1 | head -1)"

ARGS=()
if [[ $# -gt 0 ]]; then ARGS=(--args="$*"); fi

# 입을 선 한 장으로만 그린 PSD를 위·아래 입술선으로 나누고, 입 키트(~/.psd2live/mouth-kit.psd)가 있으면 입 안쪽에 씁니다.
# 끄려면: PSD2LIVE_MOUTH_SYNTHESIS=0 ./start.command
export PSD2LIVE_MOUTH_SYNTHESIS="${PSD2LIVE_MOUTH_SYNTHESIS:-1}"
# 입이 조금 벌어졌을 때 입 안(혀·어두운 부분)을 어둡게 하는 처리를 끕니다. 켜려면: PSD2LIVE_MOUTH_SHADING=1 ./start.command
export PSD2LIVE_MOUTH_SHADING="${PSD2LIVE_MOUTH_SHADING:-0}"

JAVA_HOME="$JDK" exec ./gradlew run -Porg.gradle.java.installations.paths="$JDK" ${ARGS[@]+"${ARGS[@]}"}
