"""Regression checks for the Stop hook protocol; no Gradle or network required."""

import json
import subprocess
import tempfile
import unittest
from pathlib import Path


HOOK = Path(__file__).with_name("check_style.py")


class StopStyleHookTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="dalmoi-stop-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init", "-q")
        for area in ("backend", "android"):
            wrapper = self.root / area / "gradlew"
            wrapper.parent.mkdir()
            wrapper.write_text('#!/bin/sh\nprintf "%s\\n" "$*" >> checks.log\n'
                               'if [ -f fail-style ]; then echo "style failed"; exit 1; fi\n')
            wrapper.chmod(0o755)
        self.git("add", ".")
        self.git("-c", "user.name=Hook test", "-c", "user.email=hook@example.invalid",
                 "commit", "-qm", "baseline")

    def git(self, *args):
        subprocess.run(["git", *args], cwd=self.root, check=True, capture_output=True)

    def write(self, relative, content="example"):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)

    def invoke(self, active=False, subdirectory=""):
        payload = {"cwd": str(self.root / subdirectory), "hook_event_name": "Stop",
                   "stop_hook_active": active}
        result = subprocess.run(["python3", str(HOOK)], input=json.dumps(payload),
                                text=True, capture_output=True, check=True)
        return json.loads(result.stdout)

    def test_documentation_only_does_not_run_gradle(self):
        self.write("docs/notes.md")
        self.assertEqual({}, self.invoke())
        self.assertFalse((self.root / "backend/checks.log").exists())
        self.assertFalse((self.root / "android/checks.log").exists())

    def test_untracked_java_is_checked_from_a_subdirectory(self):
        self.write("backend/src/Example.java")
        self.assertEqual({}, self.invoke(subdirectory="backend"))
        self.assertIn("checkstyleMain checkstyleTest", (self.root / "backend/checks.log").read_text())
        self.assertFalse((self.root / "android/checks.log").exists())

    def test_staged_kotlin_checks_only_android(self):
        self.write("android/src/Example.kt")
        self.git("add", "android/src/Example.kt")
        self.assertEqual({}, self.invoke())
        self.assertIn("spotlessCheck", (self.root / "android/checks.log").read_text())
        self.assertFalse((self.root / "backend/checks.log").exists())

    def test_failure_requests_continuation_and_still_checks_both_areas(self):
        self.write("backend/src/Example.java")
        self.write("android/src/Example.kt")
        self.write("backend/fail-style")
        result = self.invoke()
        self.assertEqual("block", result["decision"])
        self.assertIn("style failed", result["reason"])
        self.assertTrue((self.root / "android/checks.log").exists())

    def test_repeated_failure_reports_without_an_infinite_continuation(self):
        self.write("backend/src/Example.java")
        self.write("backend/fail-style")
        result = self.invoke(active=True)
        self.assertNotIn("decision", result)
        self.assertIn("미통과", result["systemMessage"])

    def test_missing_wrapper_is_not_reported_as_success(self):
        (self.root / "android/gradlew").unlink()
        result = self.invoke()
        self.assertEqual("block", result["decision"])
        self.assertIn("실행 불가", result["reason"])


if __name__ == "__main__":
    unittest.main()
