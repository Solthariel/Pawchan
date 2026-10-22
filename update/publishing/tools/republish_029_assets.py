"""Transport and integrity checks for the explicitly approved 0.1.29 release assets."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.error
import urllib.request
import zipfile

REPO = 'Solthariel/Pawchan'
TAG = 'v0.1.29'
RELEASE_ID = 420811273
COMMIT = 'aeab2264ead1cff79014928a1c3b7866ebca282d'
APK_HASH = '10f45fc7bc12b96de2a3638889331206ecf0d8438f4d4aa24716d6e93c19933b'
APK_SIZE = 32178429
ROOT = Path(__file__).resolve().parents[3]
APK = 'Pawchan-0.1.29.apk'
SOURCE = 'Pawchan-0.1.29-source.zip'


def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()


def api(path, method='GET', payload=None):
    request = urllib.request.Request(
        'https://api.github.com/repos/' + REPO + '/' + path,
        headers={'Authorization': 'Bearer ' + os.environ['GH_TOKEN'],
                 'Accept': 'application/vnd.github+json', 'User-Agent': 'Pawchan-029-republish'},
        method=method, data=None if payload is None else json.dumps(payload).encode())
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            data = response.read()
            return json.loads(data) if data else None
    except urllib.error.HTTPError as error:
        if error.code == 404 and method == 'GET':
            return None
        raise


def asset_identity(release):
    return sorted((x['id'], x['name'], x['size'], x.get('digest')) for x in release['assets'])


def verify_apk(directory):
    path = directory / APK
    if path.stat().st_size != APK_SIZE or hashlib.sha256(path.read_bytes()).hexdigest() != APK_HASH:
        raise ValueError('APK differs from the already published and verified 0.1.29')
    expected = APK_HASH + '  ' + APK
    if (directory / (APK + '.sha256')).read_text(encoding='ascii').strip() != expected:
        raise ValueError('The original APK checksum file differs')


def verify_zip(path):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if not {'gradlew.bat', 'settings.gradle.kts', 'app/build.gradle.kts'} <= set(names):
            raise ValueError('Source ZIP does not have a flat project root')
        forbidden = {'.idea', '.git', '.gradle', '__pycache__', 'build'}
        if any(forbidden.intersection(name.split('/')) or name.startswith('/') or '..' in name.split('/') for name in names):
            raise ValueError('Source ZIP contains local, generated or unsafe paths')
        if archive.testzip() is not None:
            raise ValueError('Source ZIP is corrupt')




def verify_candidate(directory, expected):
    for name in (APK, APK + '.sha256'):
        if (directory / name).read_bytes() != (expected / name).read_bytes():
            raise ValueError('Uploaded rebuilt APK or checksum differs')
    digest = hashlib.sha256((directory / APK).read_bytes()).hexdigest()
    if (directory / (APK + '.sha256')).read_text(encoding='ascii').strip() != digest + '  ' + APK:
        raise ValueError('Rebuilt APK checksum is invalid')
