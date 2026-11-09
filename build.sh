#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")" && pwd)
OUTPUT=${1:-"$ROOT/artifacts/yoremote-source"}
UPSTREAM=56ea84e6b8f16e45c6467768b72e1bcd8f322f72

if [[ -e "$OUTPUT" ]]; then
    echo "目标已存在，保留现有源码。请指定一个新的目录：$OUTPUT" >&2
    exit 1
fi
mkdir -p "$OUTPUT"
OUTPUT=$(cd "$OUTPUT" && pwd)
git -C "$ROOT/src" archive "$UPSTREAM" | tar -x -C "$OUTPUT"
git -C "$OUTPUT" init -q
for PATCH in 0001-support-pc.patch 0002-direct-input.patch 0003-read-keys.patch; do
    git -C "$OUTPUT" apply "$ROOT/patches/$PATCH"
done
cp -R "$ROOT/res/." "$OUTPUT/app/src/main/res/"
python3 - "$OUTPUT" <<'PY'
from pathlib import Path
import re
import sys
root = Path(sys.argv[1])
p = root / 'app/build.gradle.kts'
s = p.read_text()
s = re.sub(r'applicationId = "[^"]*"', 'applicationId = "io.github.yohuohuohuo.btremote"', s)
s = re.sub(r'versionCode = \d+', 'versionCode = 17', s)
s = re.sub(r'versionName = "[^"]*"', 'versionName = "0.1.9"', s)
p.write_text(s)
for p in (root / 'app/src/main/res').glob('values*/strings.xml'):
    s = p.read_text().replace('Android TV', 'macOS/iOS/iPadOS/tvOS/Windows/Linux')
    s = re.sub(r'<string name="app_name">[^<]*</string>', '<string name="app_name">YoRemote</string>', s)
    p.write_text(s)
p = root / 'app/src/main/java/com/atharok/btremote/common/utils/Constants.kt'
s = p.read_text()
for key in ('SOURCE_CODE_LINK', 'WEB_SITE_LINK'):
    s = re.sub(r'const val '+key+r' = "[^"]*"', 'const val '+key+' = "https://github.com/jqssun/android-bt-remote"', s)
p.write_text(s)
PY
for PATCH in 0004-yoremote.patch 0005-optimized-test.patch; do
    git -C "$OUTPUT" apply "$ROOT/patches/$PATCH"
done
echo "YoRemote 源码已准备：$OUTPUT"
echo "使用 JDK 21、SDK 36 和原 ANDROID_USER_HOME 执行 ./gradlew testDefaultDebugUnitTest assembleDefaultDebug assembleDefaultOptimized assembleDefaultDebugAndroidTest lintDefaultDebug"
