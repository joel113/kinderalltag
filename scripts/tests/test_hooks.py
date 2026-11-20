"""Exercise hook behavior in disposable Git repositories, without Gradle or an SDK."""

import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class HookTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="kinderalltag-hooks-")
        self.addCleanup(self.directory.cleanup)
        self.repo = Path(self.directory.name)
        self.git("init", "-q")
        self.git("config", "user.email", "hooks@example.invalid")
        self.git("config", "user.name", "Hook tests")
        (self.repo / ".githooks").mkdir()
        for name in ["pre-commit", "pre-commit-android", "pre-commit-workflows"]:
            shutil.copy2(ROOT / ".githooks" / name, self.repo / ".githooks" / name)
        (self.repo / "scripts").mkdir()
        shutil.copy2(ROOT / "scripts/check-workflows.sh", self.repo / "scripts/check-workflows.sh")
        (self.repo / ".github/workflows").mkdir(parents=True)
        self.workflow = self.repo / ".github/workflows/check.yml"
        self.workflow.write_text("name: Check\n")
        (self.repo / ".prettierrc.json").write_text('{}\n')
        (self.repo / "node_modules/.bin").mkdir(parents=True)
        prettier = self.repo / "node_modules/.bin/prettier"
        prettier.write_text("""#!/bin/sh
printf 'prettier %s\\n' "$*" >> workflow-calls.log
if [ -f fail-workflow-format ]; then exit 1; fi
case "$*" in
    *--write*) if [ -f change-workflow-format ]; then printf '# formatted\\n' >> .github/workflows/check.yml; fi ;;
esac
""")
        prettier.chmod(0o755)
        (self.repo / ".tools/bin").mkdir(parents=True)
        actionlint = self.repo / ".tools/bin/actionlint"
        actionlint.write_text("""#!/bin/sh
printf 'actionlint %s\\n' "$*" >> workflow-calls.log
if [ -f fail-workflow-lint ]; then exit 1; fi
""")
        actionlint.chmod(0o755)
        (self.repo / "app/src").mkdir(parents=True)
        self.source = self.repo / "app/src/Example.kt"
        self.source.write_text("val value = 1\n")
        (self.repo / "README.md").write_text("Fixture\n")
        (self.repo / "gradlew").write_text("""#!/bin/sh
printf '%s\\n' "$*" >> calls.log
case "$*" in
    *spotlessApply*)
        if [ -f fail-format ]; then exit 1; fi
        if [ -f change-format ]; then printf '// formatted\\n' >> app/src/Example.kt; fi
        ;;
    *lintDebug*) if [ -f fail-lint ]; then exit 1; fi ;;
esac
""")
        (self.repo / "gradlew").chmod(0o755)
        self.git("add", ".")
        self.git("commit", "--no-verify", "-qm", "Fixture")

    def git(self, *arguments):
        return subprocess.run(["git", *arguments], cwd=self.repo, check=True, capture_output=True, text=True).stdout.strip()

    def stage_source(self):
        self.source.write_text("val value = 2\n")
        self.git("add", "app/src/Example.kt")

    def hook(self):
        return subprocess.run(["sh", ".githooks/pre-commit"], cwd=self.repo, capture_output=True, text=True)

    def calls(self):
        file = self.repo / "calls.log"
        return file.read_text().splitlines() if file.exists() else []

    def test_docs_only_skips_gradle(self):
        (self.repo / "README.md").write_text("Documentation update\n")
        self.git("add", "README.md")
        self.assertEqual(0, self.hook().returncode)
        self.assertEqual([], self.calls())

    def test_formatted_app_runs_formatter_and_lint(self):
        self.stage_source()
        self.assertEqual(0, self.hook().returncode)
        self.assertEqual(["--console=plain spotlessApply", "--console=plain spotlessCheck :app:lintDebug"], self.calls())

    def test_partial_staging_stops_before_mutating_files(self):
        self.stage_source()
        self.source.write_text("val value = 3\n")
        result = self.hook()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("stage or stash", result.stderr)
        self.assertEqual([], self.calls())
        self.assertEqual("val value = 3\n", self.source.read_text())
        self.assertEqual("val value = 2", self.git("show", ":app/src/Example.kt"))

    def test_formatter_changes_require_review_without_auto_staging(self):
        self.stage_source()
        (self.repo / "change-format").touch()
        result = self.hook()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Review and stage", result.stderr)
        self.assertEqual(["--console=plain spotlessApply"], self.calls())
        self.assertIn("// formatted", self.source.read_text())
        self.assertEqual("val value = 2", self.git("show", ":app/src/Example.kt"))

    def test_lint_failure_blocks_commit(self):
        self.stage_source()
        (self.repo / "fail-lint").touch()
        self.assertNotEqual(0, self.hook().returncode)
        self.assertEqual(2, len(self.calls()))

    def test_formatter_failure_blocks_commit_before_lint(self):
        self.stage_source()
        (self.repo / "fail-format").touch()
        self.assertNotEqual(0, self.hook().returncode)
        self.assertEqual(["--console=plain spotlessApply"], self.calls())

    def stage_workflow(self):
        self.workflow.write_text("name: Updated check\n")
        self.git("add", ".github/workflows/check.yml")

    def workflow_calls(self):
        file = self.repo / "workflow-calls.log"
        return file.read_text().splitlines() if file.exists() else []

    def test_workflow_only_formats_and_lints_without_gradle(self):
        self.stage_workflow()
        self.assertEqual(0, self.hook().returncode)
        self.assertEqual([], self.calls())
        self.assertEqual(3, len(self.workflow_calls()))
        self.assertIn("--write", self.workflow_calls()[0])
        self.assertIn("--check", self.workflow_calls()[1])
        self.assertTrue(self.workflow_calls()[2].startswith("actionlint "))

    def test_mixed_changes_run_both_checks(self):
        self.stage_source()
        self.stage_workflow()
        self.assertEqual(0, self.hook().returncode)
        self.assertEqual(2, len(self.calls()))
        self.assertEqual(3, len(self.workflow_calls()))

    def test_partial_workflow_staging_preserves_index_and_worktree(self):
        self.stage_workflow()
        self.workflow.write_text("name: Unstaged work\n")
        result = self.hook()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("stage or stash", result.stderr)
        self.assertEqual([], self.workflow_calls())
        self.assertEqual("name: Updated check", self.git("show", ":.github/workflows/check.yml"))
        self.assertEqual("name: Unstaged work\n", self.workflow.read_text())

    def test_preflight_checks_all_scopes_before_any_formatter(self):
        self.stage_source()
        self.stage_workflow()
        self.workflow.write_text("name: Unstaged work\n")
        self.assertNotEqual(0, self.hook().returncode)
        self.assertEqual([], self.calls())
        self.assertEqual([], self.workflow_calls())

    def test_workflow_format_changes_require_review_without_auto_staging(self):
        self.stage_workflow()
        (self.repo / "change-workflow-format").touch()
        result = self.hook()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Review and stage", result.stderr)
        self.assertEqual(1, len(self.workflow_calls()))
        self.assertIn("# formatted", self.workflow.read_text())
        self.assertEqual("name: Updated check", self.git("show", ":.github/workflows/check.yml"))

    def test_workflow_lint_failure_blocks_commit(self):
        self.stage_workflow()
        (self.repo / "fail-workflow-lint").touch()
        self.assertNotEqual(0, self.hook().returncode)
        self.assertEqual(3, len(self.workflow_calls()))

    def test_workflow_formatter_failure_blocks_commit_before_lint(self):
        self.stage_workflow()
        (self.repo / "fail-workflow-format").touch()
        self.assertNotEqual(0, self.hook().returncode)
        self.assertEqual(1, len(self.workflow_calls()))

    def test_missing_workflow_tool_gives_install_instructions(self):
        self.stage_workflow()
        (self.repo / ".tools/bin/actionlint").unlink()
        result = self.hook()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("install-dev-tools.sh", result.stderr)

    def test_deleted_last_workflow_needs_no_tools(self):
        self.workflow.unlink()
        self.git("add", ".github/workflows/check.yml")
        self.assertEqual(0, self.hook().returncode)
        self.assertEqual([], self.workflow_calls())
        self.assertEqual([], self.calls())

    def test_untracked_workflow_draft_is_not_formatted(self):
        self.stage_workflow()
        draft = self.repo / ".github/workflows/draft.yaml"
        draft.write_text("untracked:    draft\n")
        self.assertEqual(0, self.hook().returncode)
        self.assertTrue(all("draft.yaml" not in call for call in self.workflow_calls()))
        self.assertEqual("untracked:    draft\n", draft.read_text())

    def install(self):
        return subprocess.run(["sh", str(ROOT / "scripts/install-hooks.sh")], cwd=self.repo, capture_output=True, text=True)

    def test_installer_activates_tracked_hook(self):
        self.assertEqual(0, self.install().returncode)
        self.assertEqual(".githooks", self.git("config", "--get", "core.hooksPath"))

    def test_installer_preserves_existing_hooks_path(self):
        self.git("config", "core.hooksPath", "custom-hooks")
        self.assertNotEqual(0, self.install().returncode)
        self.assertEqual("custom-hooks", self.git("config", "--get", "core.hooksPath"))

    def test_installer_preserves_existing_pre_commit(self):
        existing = self.repo / ".git/hooks/pre-commit"
        existing.write_text("# existing hook\n")
        self.assertNotEqual(0, self.install().returncode)
        self.assertEqual("# existing hook\n", existing.read_text())


if __name__ == "__main__":
    unittest.main()
