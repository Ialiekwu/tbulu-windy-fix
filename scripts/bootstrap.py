#!/usr/bin/env python3
"""Fetch pinned build/test dependencies with SHA-256 verification. Requires Python 3."""
import concurrent.futures
import hashlib
import json
import pathlib
import shutil
import urllib.request
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for data in iter(lambda: f.read(1024 * 1024), b''):
            h.update(data)
    return h.hexdigest()


def fetch(entry):
    target = ROOT / entry['path']
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists() or digest(target) != entry['sha256']:
        temporary = target.with_suffix(target.suffix + '.download')
        request = urllib.request.Request(entry['url'], headers={'User-Agent': 'WindyFixBuild/1.0'})
        with urllib.request.urlopen(request, timeout=90) as response, temporary.open('wb') as output:
            shutil.copyfileobj(response, output)
        if digest(temporary) != entry['sha256']:
            raise RuntimeError('Dependency checksum mismatch: ' + entry['path'])
        temporary.replace(target)
    if 'extract_to' in entry:
        destination = (ROOT / entry['extract_to']).resolve()
        with zipfile.ZipFile(target) as archive:
            for name in archive.namelist():
                if not (destination / name).resolve().is_relative_to(destination):
                    raise RuntimeError('Unsafe archive path')
            archive.extractall(destination)
    print('Verified ' + entry['path'], flush=True)


if __name__ == '__main__':
    entries = json.loads((ROOT / 'dependencies.lock.json').read_text())['files']
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as executor:
        list(executor.map(fetch, entries))
