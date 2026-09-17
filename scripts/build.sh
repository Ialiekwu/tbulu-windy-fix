#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
MODE="${1:-unsigned}"
case "$MODE" in
  unsigned|release) ;;
  *) echo 'Usage: bash scripts/build.sh [unsigned|release]' >&2; exit 2 ;;
esac
BUILD_TOOLS="${WINDY_BUILD_TOOLS:-$PWD/.tools/android/android-15}"
ANDROID_JAR="${WINDY_ANDROID_JAR:-$PWD/.tools/android/android-35-ext15/android.jar}"
XPOSED_JAR="$PWD/.tools/xposed-api-82.jar"
KEYSTORE="${WINDY_KEYSTORE:-$PWD/.tools/signing/windyfix.jks}"
PASSWORD_FILE="${WINDY_KEYSTORE_PASSWORD_FILE:-$PWD/.tools/signing/password}"
KEY_ALIAS="${WINDY_KEY_ALIAS:-windyfix}"
if [[ "$MODE" == release ]]; then
  python3 scripts/check_signing.py
fi
VERSION="$(python3 - <<'PY'
import re
import xml.etree.ElementTree as ET
version = ET.parse('app/src/main/AndroidManifest.xml').getroot().get('{http://schemas.android.com/apk/res/android}versionName')
if not version or not re.fullmatch(r'[0-9A-Za-z][0-9A-Za-z.+_-]*', version):
    raise SystemExit('Invalid manifest versionName')
print(version)
PY
)"
WORK="build/$MODE"
OUT="dist/$MODE"
rm -rf "$WORK"
mkdir -p "$WORK/classes" "$WORK/dex" "$OUT"
for executable in aapt2 d8 zipalign apksigner; do chmod +x "$BUILD_TOOLS/$executable"; done
"$BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o "$WORK/resources.zip"
"$BUILD_TOOLS/aapt2" link -o "$WORK/resources.apk" -I "$ANDROID_JAR" \
  --manifest app/src/main/AndroidManifest.xml -A app/src/main/assets --auto-add-overlay "$WORK/resources.zip"
find app/src/main/java -name '*.java' -print | sort > "$WORK/java-sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options \
  -cp "$ANDROID_JAR:$XPOSED_JAR" -d "$WORK/classes" "@$WORK/java-sources.txt"
jar cf "$WORK/classes.jar" -C "$WORK/classes" .
"$BUILD_TOOLS/d8" --release --min-api 27 --lib "$ANDROID_JAR" --classpath "$XPOSED_JAR" \
  --output "$WORK/dex" "$WORK/classes.jar"
python3 - "$WORK" <<'PY'
import pathlib, shutil, sys, zipfile
work = pathlib.Path(sys.argv[1])
shutil.copyfile(work / 'resources.apk', work / 'unsigned.apk')
with zipfile.ZipFile(work / 'unsigned.apk', 'a', compression=zipfile.ZIP_DEFLATED) as apk:
    for dex in sorted((work / 'dex').glob('*.dex')):
        apk.write(dex, dex.name)
PY
"$BUILD_TOOLS/zipalign" -f 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"
if [[ "$MODE" == unsigned ]]; then
  cp "$WORK/aligned.apk" "$OUT/windyfix-$VERSION-unsigned.apk"
  echo "Developer artifact (unsigned, not installable): $OUT/windyfix-$VERSION-unsigned.apk"
  exit 0
fi
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "file:$PASSWORD_FILE" \
  --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled false \
  --out "$OUT/windyfix-$VERSION.apk" "$WORK/aligned.apk"
python3 scripts/verify_apk.py "$OUT/windyfix-$VERSION.apk" > "$OUT/signature.txt"
cp signing/certificate.sha256 "$OUT/certificate.sha256"
(
  cd "$OUT"
  sha256sum "windyfix-$VERSION.apk" > SHA256SUMS
)
cat "$OUT/SHA256SUMS"
