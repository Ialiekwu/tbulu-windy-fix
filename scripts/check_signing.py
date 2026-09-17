#!/usr/bin/env python3
"""Check the existing release identity. Never generate or replace signing keys."""
import hashlib
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def check_key(keystore, password_file, alias, expected):
    if not Path(keystore).is_file():
        raise ValueError("Release keystore missing; restore the original private backup.")
    if not Path(password_file).is_file():
        raise ValueError("Release password file missing; restore the original private backup.")
    result = subprocess.run([
        "keytool", "-exportcert", "-keystore", str(keystore),
        "-storepass:file", str(password_file), "-alias", alias,
    ], capture_output=True)
    if result.returncode != 0 or not result.stdout:
        raise ValueError("Cannot read release certificate; check keystore, password file and alias.")
    actual = hashlib.sha256(result.stdout).hexdigest()
    if actual != expected.strip().lower():
        raise ValueError("Release certificate mismatch; refusing to sign with a different identity.")
    return actual


def main():
    try:
        fingerprint = check_key(
            os.environ.get("WINDY_KEYSTORE", ROOT / ".tools/signing/windyfix.jks"),
            os.environ.get("WINDY_KEYSTORE_PASSWORD_FILE", ROOT / ".tools/signing/password"),
            os.environ.get("WINDY_KEY_ALIAS", "windyfix"),
            (ROOT / "signing/certificate.sha256").read_text(),
        )
    except (OSError, ValueError) as error:
        print("Signing check failed: " + str(error), file=sys.stderr)
        return 1
    print("Release certificate verified: " + fingerprint)
    return 0


if __name__ == "__main__":
    sys.exit(main())
