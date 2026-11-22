"""Shared packaging preflight: only accept a complete absolute HTTPS endpoint."""
import sys
from urllib.parse import urlsplit


def valid_endpoint(value):
    if not value or not value.isascii() or any(ord(c) <= 32 or ord(c) == 127 or c in '\\"' for c in value):
        return False
    try:
        url = urlsplit(value)
        return bool(url.scheme == 'https' and url.hostname and not url.fragment and
                    url.username is None and url.password is None and url.port != 0)
    except ValueError:
        return False


if __name__ == '__main__' and not valid_endpoint(sys.argv[1] if len(sys.argv) == 2 else ''):
    sys.exit('PAWCHAN_UPDATE_MANIFEST_URL must be an absolute HTTPS URL with a host')
