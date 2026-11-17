"""Execute the real hook with isolated fake build tools; never push Git refs."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

HOOK = Path(__file__).resolve().parents[2] / ".githooks/pre-push"
SHA = "a" * 40


@unittest.skipUnless(os.name == "posix", "shell hook tests run on Linux/CI")
class PrePushTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        (self.root / "oj-web").mkdir()
        (self.root / "oj-web/package.json").write_text("{}")
        self.env = dict(os.environ, PATH=str(self.bin), FAKE_ROOT=str(self.root),
                        FAKE_SHA=SHA, BUILD_LOG=str(self.root / "calls"))
        for command in ("sh", "mkdir"):
            (self.bin / command).symlink_to(shutil.which(command))
        self.tool("git", '''case "$1:$2" in
rev-parse:HEAD) printf '%s' "$FAKE_SHA" ;;
rev-parse:--show-toplevel) printf '%s' "$FAKE_ROOT" ;;
status:--porcelain) printf '%s' "${FAKE_DIRTY:-}" ;;
esac''')
        self.tool("mvn", 'printf "mvn %s\\n" "$*" >> "$BUILD_LOG"; exit "${FAIL_MVN:-0}"')
        self.tool("node", 'exit "${OLD_NODE:-0}"')
        self.tool("npm", '''printf 'npm %s\n' "$*" >> "$BUILD_LOG"
if [ "$1" = ci ]; then mkdir -p node_modules; fi
if [ "$1" = run ]; then exit "${FAIL_NPM_BUILD:-0}"; fi
exit "${FAIL_NPM:-0}"''')

    def tool(self, name, body):
        path = self.bin / name
        path.write_text("#!/bin/sh\n" + body + "\n")
        path.chmod(0o755)

    def run_hook(self, branch="dev"):
        return subprocess.run(["/bin/sh", str(HOOK)], input=f"refs/heads/{branch} {SHA} refs/heads/{branch} {'b'*40}\n",
                              text=True, capture_output=True, env=self.env, cwd=self.root)

    def test_dev_runs_tests_and_installs_missing_frontend_dependencies(self):
        result = self.run_hook()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        calls = (self.root / "calls").read_text()
        self.assertIn("mvn -q verify", calls)
        self.assertIn("npm ci", calls)
        self.assertIn("npm run build", calls)

    def test_missing_maven_cannot_report_success(self):
        (self.bin / "mvn").unlink()
        self.assertNotEqual(0, self.run_hook().returncode)

    def test_missing_node_cannot_skip_frontend(self):
        (self.bin / "node").unlink()
        self.assertNotEqual(0, self.run_hook().returncode)

    def test_old_node_is_rejected(self):
        self.env["OLD_NODE"] = "1"
        self.assertNotEqual(0, self.run_hook().returncode)

    def test_missing_frontend_project_cannot_be_skipped(self):
        (self.root / "oj-web/package.json").unlink()
        self.assertNotEqual(0, self.run_hook().returncode)

    def test_failed_test_or_frontend_build_blocks_push(self):
        (self.root / "oj-web/node_modules").mkdir()
        for variable in ("FAIL_MVN", "FAIL_NPM", "FAIL_NPM_BUILD"):
            with self.subTest(variable=variable):
                self.env[variable] = "1"
                self.assertNotEqual(0, self.run_hook().returncode)
                del self.env[variable]

    def test_different_pushed_commit_or_dirty_tree_is_rejected(self):
        self.env["FAKE_SHA"] = "c" * 40
        self.assertNotEqual(0, self.run_hook().returncode)
        self.env["FAKE_SHA"] = SHA
        self.env["FAKE_DIRTY"] = " M source.java"
        self.assertNotEqual(0, self.run_hook().returncode)

    def test_personal_branch_is_unaffected(self):
        (self.bin / "mvn").unlink()
        self.assertEqual(0, self.run_hook("feat/example").returncode)

    def test_explicit_override_remains_explicit(self):
        self.env["SKIP_BUILD_CHECK"] = "1"
        self.assertEqual(0, self.run_hook().returncode)


if __name__ == "__main__":
    unittest.main()
