"""Reject a release signed by a key incompatible with public Pawchan 0.1.49."""

import argparse
import re

# SHA-256 of the signer on the official v0.1.49 APK from the Pawchan GitHub release.
RELEASE_SIGNER_SHA256 = "72ee17e4c5f8cd61a5dfcab7ac9936f787b3483b6c05d2f90b8f5da05288fd9c"


def compatible_signer(configured: str, apk_signers: str) -> bool:
    """Require the configured key and the only APK signer to match the existing release."""
    configured = configured.strip().lower()
    signers = {line.strip().lower() for line in apk_signers.splitlines() if line.strip()}
    return bool(
        re.fullmatch(r"[0-9a-f]{64}", configured)
        and configured == RELEASE_SIGNER_SHA256
        and signers == {RELEASE_SIGNER_SHA256}
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--configured", required=True)
    parser.add_argument("--apk-signers", required=True)
    args = parser.parse_args()
    if not compatible_signer(args.configured, args.apk_signers):
        print("::error::Signing certificate differs from the official Pawchan 0.1.49 release; publication stopped")
        return 1
    print("APK signer matches the official Pawchan 0.1.49 certificate")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
