"""The old command must route to the same guarded publisher, not retain a second pipeline."""
import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import publish_feed_release
import release_entry


class CompatibilityTest(unittest.TestCase):
    def test_old_command_is_only_an_alias(self):
        self.assertIs(publish_feed_release.main, release_entry.main)
