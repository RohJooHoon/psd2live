#!/usr/bin/env bash
# PSD2Live macOS 앱 빌드 스크립트
#
#   ./build-mac.sh            PSD2Live.app 빌드
#   ./build-mac.sh --dmg      빌드 + DMG 파일 생성
#   ./build-mac.sh --install  빌드 + /Applications 에 설치 (기존 앱 교체)
#
# 옵션은 같이 써도 됩니다. 예: ./build-mac.sh --dmg --install
# 패키징에는 JetBrains Runtime(JBR) 21 SDK가 필요합니다. 없으면 자동으로 받아서 검증 후 캐시에 둡니다.
# 다른 JBR을 쓰려면 JBR_HOME=/path/to/Contents/Home ./build-mac.sh

set -euo pipefail

JBR_VERSION="21.0.11"
JBR_BUILD="b1163.125"
CACHE_DIR="$HOME/Library/Caches/psd2live-build"

MAKE_DMG=false
INSTALL=false
for arg in "$@"; do
	case "$arg" in
		--dmg) MAKE_DMG=true ;;
		--install) INSTALL=true ;;
		-h|--help) sed -n '2,10p' "$0"; exit 0 ;;
		*) echo "알 수 없는 옵션: $arg (--help 참고)" >&2; exit 1 ;;
	esac
done

cd "$(dirname "$0")"

if [[ "$(uname -s)" != "Darwin" ]]; then
	echo "macOS에서만 실행할 수 있습니다." >&2
	exit 1
fi

case "$(uname -m)" in
	arm64) JBR_ARCH="aarch64" ;;
	x86_64) JBR_ARCH="x64" ;;
	*) echo "지원하지 않는 CPU: $(uname -m)" >&2; exit 1 ;;
esac

# --- JetBrains Runtime 준비 ---
if [[ -z "${JBR_HOME:-}" ]]; then
	JBR_NAME="jbrsdk-${JBR_VERSION}-osx-${JBR_ARCH}-${JBR_BUILD}"
	JBR_HOME="$CACHE_DIR/$JBR_NAME/Contents/Home"
	if [[ ! -x "$JBR_HOME/bin/jpackage" ]]; then
		echo "==> JetBrains Runtime $JBR_VERSION 다운로드 중..."
		mkdir -p "$CACHE_DIR"
		URL="https://cache-redirector.jetbrains.com/intellij-jbr/${JBR_NAME}.tar.gz"
		TMP="$(mktemp -d)"
		trap 'rm -rf "$TMP"' EXIT
		curl -fL --progress-bar -o "$TMP/jbr.tar.gz" "$URL"
		EXPECTED="$(curl -fsSL "$URL.checksum" | awk '{print $1}')"
		ACTUAL="$(shasum -a 512 "$TMP/jbr.tar.gz" | awk '{print $1}')"
		if [[ -z "$EXPECTED" || "$EXPECTED" != "$ACTUAL" ]]; then
			echo "JBR 체크섬이 일치하지 않습니다. 중단합니다." >&2
			exit 1
		fi
		rm -rf "${CACHE_DIR:?}/$JBR_NAME"
		tar xzf "$TMP/jbr.tar.gz" -C "$CACHE_DIR"
	fi
fi

if ! grep -q 'JetBrains' "$JBR_HOME/release" 2>/dev/null; then
	echo "JetBrains Runtime이 아닙니다: $JBR_HOME" >&2
	exit 1
fi
echo "==> JDK: $("$JBR_HOME/bin/java" -version 2>&1 | head -1)"

# --- 빌드 ---
VERSION="$(sed -n 's/^version = "\(.*\)"/\1/p' build.gradle.kts)"
echo "==> PSD2Live $VERSION 빌드 중 (처음에는 몇 분 걸립니다)..."
JAVA_HOME="$JBR_HOME" ./gradlew createDistributable \
	-Porg.gradle.java.installations.paths="$JBR_HOME"

APP="build/compose/binaries/main/app/PSD2Live.app"
if [[ ! -d "$APP" ]]; then
	echo "빌드는 끝났지만 $APP 을 찾지 못했습니다." >&2
	exit 1
fi
echo "==> 앱: $PWD/$APP"

# --- DMG ---
if $MAKE_DMG; then
	DMG="build/PSD2Live-${VERSION}-macos-${JBR_ARCH}.dmg"
	STAGE="$(mktemp -d)"
	cp -R "$APP" "$STAGE/"
	ln -s /Applications "$STAGE/Applications"
	rm -f "$DMG"
	hdiutil create -volname "PSD2Live $VERSION" -srcfolder "$STAGE" -ov -format UDZO "$DMG" >/dev/null
	rm -rf "$STAGE"
	echo "==> DMG: $PWD/$DMG"
fi

# --- 설치 ---
if $INSTALL; then
	if pgrep -f "/Applications/PSD2Live.app/" >/dev/null; then
		echo "PSD2Live가 실행 중입니다. 종료한 뒤 다시 시도하세요." >&2
		exit 1
	fi
	rm -rf /Applications/PSD2Live.app
	cp -R "$APP" /Applications/
	echo "==> /Applications/PSD2Live.app 에 설치했습니다."
fi

echo "완료."
