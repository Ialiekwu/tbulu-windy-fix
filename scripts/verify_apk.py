#!/usr/bin/env python3
"""Verify release APK signature, pinned identity and manifest version."""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def verify(apk):
    build_tools = Path(os.environ.get("WINDY_BUILD_TOOLS", ROOT / ".tools/android/android-15"))
    result = subprocess.run([
        str(build_tools / "apksigner"), "verify", "--verbose", "--print-certs", str(apk),
    ], check=True, capture_output=True, text=True)
    expected = (ROOT / "signing/certificate.sha256").read_text().strip().lower()
    certificates = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$", result.stdout, re.M)
    if [value.lower() for value in certificates] != [expected]:
        raise ValueError("APK does not have the single pinned release certificate.")
    for scheme in (2, 3):
        if not re.search(rf"^Verified using v{scheme} scheme .*: true$", result.stdout, re.M):
            raise ValueError(f"APK Signature Scheme v{scheme} did not verify.")
    badging = subprocess.run([
        str(build_tools / "aapt2"), "dump", "badging", str(apk),
    ], check=True, capture_output=True, text=True).stdout
    manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
    android = "{http://schemas.android.com/apk/res/android}"
    package = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging, re.M)
    wanted = ("dev.local.windyfix", manifest.get(android + "versionCode"), manifest.get(android + "versionName"))
    if package is None or package.groups() != wanted:
        raise ValueError("APK package or version differs from the source manifest.")
    print(result.stdout.rstrip())
    print("Verified package: " + wanted[0] + " " + wanted[2] + " (" + wanted[1] + ")")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    try:
        verify(args.apk)
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print("APK verification failed: " + str(error), file=sys.stderr)
        sys.exit(1)
