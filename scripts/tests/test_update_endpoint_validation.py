import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from validate_update_url import valid_endpoint


class UpdateEndpointValidationTest(unittest.TestCase):
    def test_https_endpoint_with_optional_port_and_query(self):
        self.assertTrue(valid_endpoint('https://example.org:8443/feed.json?channel=debug'))

    def test_missing_host_and_malformed_port_rejected(self):
        for value in ('', 'https://?', 'http://example.org/feed', 'https://example.org:65536/feed'):
            with self.subTest(value=value):
                self.assertFalse(valid_endpoint(value))

    def test_nonliteral_and_credentialed_values_rejected(self):
        for value in ('https://user@example.org/feed', 'https://example.org/#fragment', 'https://example.org/a b'):
            with self.subTest(value=value):
                self.assertFalse(valid_endpoint(value))
