"""Exercise bundle audit edge cases with controlled readelf output."""

import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile


ROOT = Path(__file__).resolve().parent.parent


class AlignmentCheckTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="wiccan-alignment-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        readelf = self.root / "readelf"
        readelf.write_text("#!/bin/sh\ncat \"$2\"\n")
        readelf.chmod(0o755)

    def audit(self, library_headers):
        bundle = self.root / "app.aab"
        with zipfile.ZipFile(bundle, "w") as archive:
            archive.writestr("BundleConfig.pb", "fixture")
            for name, headers in library_headers.items():
                archive.writestr("base/lib/" + name, headers)
        env = dict(os.environ, PATH=str(self.root) + os.pathsep + os.environ["PATH"])
        return subprocess.run(
            [str(ROOT / "scripts/check-16kb-alignment.sh"), str(bundle)],
            env=env, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        )

    def test_bundle_without_native_libraries_passes(self):
        result = self.audit({})
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("nothing to check", result.stdout)

    def test_alignment_greater_than_16kb_passes(self):
        result = self.audit({"arm64-v8a/libfixture.so": "  LOAD fields 0x10000\n  LOAD fields 0x4000\n"})
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("0x4000", result.stdout)

    def test_all_64kb_load_segments_pass(self):
        result = self.audit({"x86_64/libfixture.so": "  LOAD fields 0x10000\n"})
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("0x10000", result.stdout)

    def test_any_4kb_load_segment_fails_on_64bit(self):
        result = self.audit({"arm64-v8a/libfixture.so": "  LOAD fields 0x4000\n  LOAD fields 0x1000\n"})
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("FAIL", result.stdout)

    def test_missing_load_segments_fail_on_64bit(self):
        result = self.audit({"x86_64/libfixture.so": "not ELF program headers\n"})
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("FAIL", result.stdout)

    def test_invalid_non_power_of_two_alignment_fails(self):
        result = self.audit({"arm64-v8a/libfixture.so": "  LOAD fields 0x6000\n"})
        self.assertNotEqual(result.returncode, 0, result.stdout)

    def test_zero_alignment_cannot_be_hidden_by_a_valid_segment(self):
        result = self.audit({"x86_64/libfixture.so": "  LOAD fields 0x0\n  LOAD fields 0x4000\n"})
        self.assertNotEqual(result.returncode, 0, result.stdout)

    def test_4kb_library_on_32bit_is_nonblocking(self):
        result = self.audit({"armeabi-v7a/libfixture.so": "  LOAD fields 0x1000\n"})
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("warn", result.stdout)


if __name__ == "__main__":
    unittest.main()
