"""Exercise validation failures in temporary repos without build toolchains.

Run from any directory with: python3 scripts/test-validate-repo.py
"""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


VALIDATOR = Path(__file__).resolve().with_name("validate-repo.sh")
REAL_GIT = shutil.which("git")


class ValidateRepoTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="wiccan-validate-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "mock-bin"
        self.bin.mkdir()
        self.log = self.root / "commands.log"
        self.env = os.environ.copy()
        self.env.update({
            "PATH": str(self.bin) + os.pathsep + self.env["PATH"],
            "VALIDATE_TEST_LOG": str(self.log),
            "VALIDATE_TEST_REAL_GIT": REAL_GIT,
        })
        self.write("scripts/validate-repo.sh", VALIDATOR.read_text(), executable=True)
        self.write("app/build.gradle.kts", 'applicationId = "com.jaimesorrow.wiccan"\nnamespace = "com.wiccan.app"\n')
        config = json.dumps({"client": [{"client_info": {"android_client_info": {
            "package_name": "com.jaimesorrow.wiccan",
        }}}]})
        self.write("app/google-services.ci.json", config)
        self.write("app/src/prod/google-services.json", config)
        self.write("functions/src/index.ts", "const packageName = process.env.ANDROID_PACKAGE_NAME;\n")
        self.write("firestore-tests/package.json", "{}\n")
        self.write(".github/workflows/ci.yml", "run: ./scripts/validate-repo.sh --structure\n")
        self.write("scripts/release-preflight.sh", "#!/usr/bin/env bash\necho 'PASS fixture preflight'\n", executable=True)
        self.write("scripts/check-16kb-alignment.sh", "#!/usr/bin/env bash\necho alignment >> \"$VALIDATE_TEST_LOG\"\n", executable=True)
        self.write("gradlew", """#!/usr/bin/env bash
echo "gradle $*" >> "$VALIDATE_TEST_LOG"
if [[ "$*" == *bundleProdRelease* ]]; then
  exit "${VALIDATE_TEST_BUNDLE_STATUS:-0}"
fi
""", executable=True)
        self.write("mock-bin/npm", """#!/usr/bin/env bash
echo "npm $*" >> "$VALIDATE_TEST_LOG"
if [[ "$1" == "ci" || "$1" == "install" ]]; then
  exit "${VALIDATE_TEST_NPM_STATUS:-0}"
fi
""", executable=True)
        self.write("mock-bin/git", """#!/usr/bin/env bash
if [[ "$1" == "grep" ]]; then
  case "${VALIDATE_TEST_GIT_ERROR_SCAN:-}:$2" in
    conflict:-nIE|secret:-lIE)
      echo 'simulated git scan error' >&2
      exit 2
      ;;
  esac
fi
exec "$VALIDATE_TEST_REAL_GIT" "$@"
""", executable=True)
        self.git("init", "-q")
        self.git("add", "app", "functions", "firestore-tests", "scripts", ".github", "gradlew")

    def write(self, relative, text, executable=False):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        if executable:
            path.chmod(0o755)

    def git(self, *args):
        subprocess.run([REAL_GIT, "-C", str(self.root), *args], check=True,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    def run_validator(self, *args):
        return subprocess.run(["bash", str(self.root / "scripts/validate-repo.sh"), *args],
                              cwd=self.root, env=self.env, text=True,
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

    def commands(self):
        return self.log.read_text().splitlines() if self.log.exists() else []

    def test_success_installs_locked_dependencies_even_with_existing_modules(self):
        (self.root / "functions/node_modules").mkdir()
        result = self.run_validator()
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn("npm ci", self.commands())
        self.assertEqual(self.commands().count("npm audit --audit-level=moderate"), 2)
        self.assertEqual(self.commands().count("alignment"), 1)
        for step in ("build", "test", "lint"):
            self.assertIn("npm run " + step, self.commands())

    def test_dependency_install_failure_cannot_pass_with_available_commands(self):
        self.env["VALIDATE_TEST_NPM_STATUS"] = "1"
        result = self.run_validator()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("FAIL  functions npm ci", result.stdout)
        self.assertFalse(any(line.startswith("npm run ") for line in self.commands()))

    def test_failed_bundle_does_not_validate_stale_output(self):
        self.write("app/build/outputs/bundle/prodRelease/app-prod-release.aab", "old bundle")
        self.env["VALIDATE_TEST_BUNDLE_STATUS"] = "1"
        result = self.run_validator()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("FAIL  bundle prodRelease", result.stdout)
        self.assertNotIn("alignment", self.commands())

    def test_conflict_scan_error_fails_closed(self):
        self.env["VALIDATE_TEST_GIT_ERROR_SCAN"] = "conflict"
        result = self.run_validator("--structure")
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("merge conflict marker scan failed", result.stdout)

    def test_secret_scan_error_fails_closed(self):
        self.env["VALIDATE_TEST_GIT_ERROR_SCAN"] = "secret"
        result = self.run_validator("--structure")
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("secret scan failed", result.stdout)

    def test_actual_merge_marker_is_rejected(self):
        self.write("conflicted.txt", "<" * 7 + " left\n")
        self.git("add", "conflicted.txt")
        result = self.run_validator("--structure")
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("merge conflict markers found", result.stdout)

    def test_actual_secrets_are_rejected(self):
        secrets = (
            "sk" + "-ant-" + "x" * 24,
            "sk" + "-proj-" + "x" * 24,
            "gh" + "p_" + "x" * 24,
            "github" + "_pat_" + "x" * 24,
            "AK" + "IA" + "A" * 16,
        )
        for index, secret in enumerate(secrets):
            with self.subTest(index=index):
                self.write("secret.txt", secret + "\n")
                self.git("add", "secret.txt")
                result = self.run_validator("--structure")
                self.assertNotEqual(result.returncode, 0, result.stdout)
                self.assertIn("private key material or an API secret is committed", result.stdout)

    def test_missing_referenced_python_script_is_rejected(self):
        self.write(".github/workflows/ci.yml", "run: python3 scripts/missing-check.py\n")
        result = self.run_validator("--structure")
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn("referenced but missing: scripts/missing-check.py", result.stdout)


if __name__ == "__main__":
    unittest.main()
