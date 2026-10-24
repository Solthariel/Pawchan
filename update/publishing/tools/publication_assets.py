"""Authenticated release transport and byte-level artifact checks; never overwrite assets."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.error
import urllib.request
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[3]
REPO = 'Solthariel/Pawchan'
# Versions through 0.1.33 (code 34) may retain old public attachments.
LEGACY_LAST_CODE = 34


def run(*args, cwd=None):
    return subprocess.check_output(args, cwd=ROOT if cwd is None else cwd, text=True).strip()


def api(path):
    # A fresh read is required after creating tags/drafts; do not reuse a cached 404/list.
    separator = '&' if '?' in path else '?'
    url = f'https://api.github.com/repos/{REPO}/{path}{separator}pawchan_read={uuid.uuid4().hex}'
    request = urllib.request.Request(url, headers={
        'Authorization': 'Bearer ' + os.environ['GH_TOKEN'],
        'Accept': 'application/vnd.github+json', 'User-Agent': 'Pawchan-publication',
        'Cache-Control': 'no-cache, no-store', 'X-GitHub-Api-Version': '2022-11-28'})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def find_release(tag):
    """Tag lookup may omit drafts: search authenticated release pages before creating one."""
    release = api('releases/tags/' + tag)
    if release is not None:
        if not isinstance(release, dict) or release.get('tag_name') != tag:
            raise ValueError('Unexpected release response')
        return release
    matches = []
    for page in range(1, 101):
        batch = api(f'releases?per_page=100&page={page}')
        if not isinstance(batch, list) or len(batch) > 100:
            raise ValueError('Cannot safely inspect authenticated release list')
        for item in batch:
            if not isinstance(item, dict):
                raise ValueError('Invalid release list entry')
            if item.get('tag_name') == tag:
                matches.append(item)
        if len(matches) > 1:
            raise ValueError('Multiple releases share the requested tag; explicit review required')
        if len(batch) < 100:
            return matches[0] if matches else None
    raise ValueError('Release pagination limit reached; no automatic creation')


def names(version):
    return (f'Pawchan-{version}.apk',)


def historical_names(version):
    return (f'Pawchan-{version}.apk', f'Pawchan-{version}.apk.sha256',
            f'Pawchan-{version}-source.zip', f'Pawchan-{version}-source.zip.sha256')


def checksum(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify_zip(path):
    with zipfile.ZipFile(path) as archive:
        entries = archive.namelist()
        if not {'gradlew.bat', 'settings.gradle.kts', 'app/build.gradle.kts'} <= set(entries):
            raise ValueError('Source archive must contain the project directly at its root')
        forbidden = {'.git', '.idea', '.gradle', '.kotlin', '__pycache__', 'build', '.cxx'}
        for name in entries:
            parts = name.split('/')
            if name.startswith('/') or '..' in parts or forbidden.intersection(parts):
                raise ValueError('Unsafe/generated path in source archive: ' + name)
            if Path(name).suffix.lower() in {'.jks', '.keystore', '.p12', '.pfx'} or parts[-1] == 'local.properties' or parts[-1].startswith('.env'):
                raise ValueError('Local configuration or secret in source archive')
        if archive.testzip() is not None:
            raise ValueError('Corrupt source archive')


def verify_files(directory, version):
    apk = names(version)[0]
    path = directory / apk
    if not path.is_file() or path.stat().st_size == 0:
        raise ValueError('Missing or empty artifact: ' + apk)
    digest = checksum(path)
    sidecar = directory / (apk + '.sha256')
    if sidecar.exists() and sidecar.read_text(encoding='ascii').strip() != digest + '  ' + apk:
        raise ValueError('Checksum differs: ' + apk)
    return digest, path.stat().st_size


def verify_build(directory, version):
    digest, length = verify_files(directory, version)
    apk = names(version)[0]
    if (directory / (apk + '.sha256')).read_text(encoding='ascii').strip() != digest + '  ' + apk:
        raise ValueError('Local APK checksum differs')
    return digest, length


def download(release, directory):
    directory.mkdir(parents=True, exist_ok=True)
    if release['assets']:
        run('gh', 'release', 'download', release['tag_name'], '--repo', REPO, '--dir', str(directory))
    return directory


def compare_files(local, remote, entries):
    for name in entries:
        if (local / name).read_bytes() != (remote / name).read_bytes():
            raise ValueError('Existing release asset differs; refusing overwrite: ' + name)


def verify_legacy(directory, version):
    apk, apk_sha, source, source_sha = historical_names(version)
    digest, length = verify_build(directory, version)
    source_path = directory / source
    if source_path.exists():
        if (directory / source_sha).read_text(encoding='ascii').strip() != checksum(source_path) + '  ' + source:
            raise ValueError('Historical source checksum differs')
        verify_zip(source_path)
    return digest, length
