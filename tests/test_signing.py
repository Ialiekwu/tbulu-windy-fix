import hashlib
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from check_signing import ROOT, check_key


class SigningIdentityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.temp.cleanup)
        cls.directory = Path(cls.temp.name)
        cls.key = cls.directory / "fixture.jks"
        cls.password = cls.directory / "password"
        cls.password.write_text("test-only-not-a-release-password")
        cls.password.chmod(0o600)
        subprocess.run([
            "keytool", "-genkeypair", "-keystore", str(cls.key),
            "-storepass:file", str(cls.password), "-keypass:file", str(cls.password),
            "-alias", "fixture", "-keyalg", "RSA", "-keysize", "2048",
            "-validity", "1", "-dname", "CN=Disposable Signing Test",
        ], check=True, capture_output=True)
        certificate = subprocess.run([
            "keytool", "-exportcert", "-keystore", str(cls.key),
            "-storepass:file", str(cls.password), "-alias", "fixture",
        ], check=True, capture_output=True).stdout
        cls.fingerprint = hashlib.sha256(certificate).hexdigest()

    def test_same_identity_accepted(self):
        self.assertEqual(check_key(self.key, self.password, "fixture", self.fingerprint), self.fingerprint)

    def test_different_release_identity_rejected(self):
        with self.assertRaisesRegex(ValueError, "certificate mismatch"):
            check_key(self.key, self.password, "fixture", (ROOT / "signing/certificate.sha256").read_text())

    def test_missing_key_never_generated(self):
        missing = self.directory / "missing.jks"
        with self.assertRaisesRegex(ValueError, "keystore missing"):
            check_key(missing, self.password, "fixture", self.fingerprint)
        self.assertFalse(missing.exists())

    def test_missing_password_rejected(self):
        with self.assertRaisesRegex(ValueError, "password file missing"):
            check_key(self.key, self.directory / "missing-password", "fixture", self.fingerprint)

    def test_wrong_alias_rejected(self):
        with self.assertRaisesRegex(ValueError, "Cannot read"):
            check_key(self.key, self.password, "missing-alias", self.fingerprint)


if __name__ == "__main__":
    unittest.main()
