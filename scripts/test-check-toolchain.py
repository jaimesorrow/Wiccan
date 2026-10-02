"""Exercise package/runtime drift with isolated copies of the real configuration."""

import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parent.parent


class ToolchainCheckTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="wiccan-toolchain-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for name in (
            "scripts/check-toolchain.py", "app/build.gradle.kts",
            "app/google-services.ci.json", "app/src/prod/google-services.json",
            "functions/package.json", "functions/package-lock.json",
            "functions/src/index.ts", ".nvmrc", ".firebaserc", "firebase.json",
            ".github/workflows/ci.yml", ".github/workflows/release.yml",
            "build.gradle.kts", "gradle/libs.versions.toml",
        ):
            destination = self.root / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, destination)
        shutil.copytree(ROOT / "app/schemas", self.root / "app/schemas")
        for source in (ROOT / "app/src").glob("*/kotlin"):
            shutil.copytree(source, self.root / source.relative_to(ROOT))

    def run_check(self):
        return subprocess.run(
            [sys.executable, str(self.root / "scripts/check-toolchain.py")],
            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        )

    def replace(self, name, old, new):
        path = self.root / name
        content = path.read_text()
        self.assertIn(old, content)
        path.write_text(content.replace(old, new, 1))

    def assert_rejected(self, message):
        result = self.run_check()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn(message, result.stdout)

    def test_current_configuration_passes(self):
        result = self.run_check()
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_ci_node_drift_is_rejected(self):
        self.replace(".github/workflows/ci.yml", "node-version: '22'", "node-version: '20'")
        self.assert_rejected("FAIL  CI uses Functions runtime Node 22")

    def test_lockfile_runtime_drift_is_rejected(self):
        self.replace("functions/package-lock.json", '"node": "22"', '"node": "20"')
        self.assert_rejected("FAIL  lockfile root engines match package.json")

    def test_namespace_drift_is_rejected(self):
        self.replace("app/build.gradle.kts", 'namespace = "com.jaimesorrow.wiccan"',
                     'namespace = "com.other.app"')
        self.assert_rejected("FAIL  Kotlin namespace matches Firebase package name")

    def test_development_suffix_is_rejected(self):
        self.replace("app/build.gradle.kts", 'create("dev") {',
                     'create("dev") {\n applicationIdSuffix = ".dev"')
        self.assert_rejected("FAIL  dev uses registered Firebase package")

    def test_wrong_development_firebase_package_is_rejected(self):
        path = self.root / "app/src/dev/google-services.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        content = (self.root / "app/src/prod/google-services.json").read_text()
        path.write_text(content.replace("com.jaimesorrow.wiccan", "com.other.app"))
        self.assert_rejected("FAIL  app/src/dev/google-services.json")

    def test_registered_development_package_passes(self):
        path = self.root / "app/src/dev/google-services.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        content = json.loads((self.root / "app/src/prod/google-services.json").read_text())
        content["project_info"]["project_id"] = "demo-wiccan-dev-test"
        path.write_text(json.dumps(content))
        result = self.run_check()
        self.assertEqual(result.returncode, 0, result.stdout)

    def test_old_source_package_is_rejected(self):
        self.replace("app/src/main/kotlin/com/jaimesorrow/wiccan/MainActivity.kt",
                     "package com.jaimesorrow.wiccan", "package com.other.app")
        self.assert_rejected("FAIL  every Kotlin source and test uses the Firebase package prefix")

    def test_missing_room_migration_schema_is_rejected(self):
        (self.root / "app/schemas/com.jaimesorrow.wiccan.data.local.WiccanDatabase/1.json").unlink()
        self.assert_rejected("FAIL  Room migration schemas follow the database class package")

    def test_legacy_kotlin_plugin_is_rejected(self):
        self.replace("app/build.gradle.kts", "plugins {",
                     "plugins {\n alias(libs.plugins.kotlin.android)")
        self.assert_rejected("FAIL  Gradle avoids conflicting legacy Kotlin plugin and DSL")

    def test_missing_configuration_fails_closed(self):
        (self.root / "functions/package-lock.json").unlink()
        self.assert_rejected("FAIL  Cannot validate configuration")


if __name__ == "__main__":
    unittest.main()
