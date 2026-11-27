"""Check the actual Git ignore rules, not just the text of .gitignore."""
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]


class QaOutputPathsTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.repo = Path(temporary.name)
        subprocess.run(["git", "init", "-q", str(self.repo)], check=True)
        (self.repo / ".gitignore").write_bytes((ROOT / ".gitignore").read_bytes())
        (self.repo / ".empty-global-excludes").touch()

    def ignored(self, path):
        result = subprocess.run(
            ["git", "-c", "core.excludesFile=.empty-global-excludes", "check-ignore",
             "-q", "--no-index", "--", path],
            cwd=self.repo, check=False,
        )
        self.assertIn(result.returncode, (0, 1))
        return result.returncode == 0

    def test_legacy_output_from_both_launchers_is_not_added_as_source(self):
        for path in ("interface-v11-full-check.log", "navigation-production-sample.zip",
                     "navigation-production-sample/full-app/xml/TEST-app.xml"):
            with self.subTest(path=path):
                self.assertTrue(self.ignored(path))

    def test_unrelated_zip_and_public_tls_fixture_are_not_excluded(self):
        for path in ("reference.zip", "docs/navigation-production-example.zip",
                     "data/network/src/test/resources/proxy-test.p12"):
            with self.subTest(path=path):
                self.assertFalse(self.ignored(path))
