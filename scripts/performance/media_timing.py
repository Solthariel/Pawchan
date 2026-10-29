"""Opt-in numeric media timing capture. Never clears logcat, app data or caches."""
import argparse
import datetime
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import uuid
import zipfile

TAG = "PawchanMediaTiming"
LINE = re.compile(r"probe=(-?\d+) elapsed_ms=(\d+) event=([A-Z_]+) value=(-?\d+)")


def resolve_adb(root, supplied=None):
    candidates = [supplied, shutil.which("adb")]
    for key in ("ANDROID_SDK_ROOT", "ANDROID_HOME"):
        if os.environ.get(key):
            candidates.append(str(Path(os.environ[key]) / "platform-tools" / "adb.exe"))
    properties = root / "local.properties"
    if properties.is_file():
        match = re.search(r"^sdk\.dir=(.+)$", properties.read_text(encoding="utf-8"), re.M)
        if match:
            sdk = match.group(1).strip().replace("\\:", ":").replace("\\\\", "\\")
            candidates.append(str(Path(sdk) / "platform-tools" / "adb.exe"))
    if os.environ.get("LOCALAPPDATA"):
        candidates.append(str(Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk/platform-tools/adb.exe"))
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return str(Path(candidate).resolve())
    raise RuntimeError("ADB not found. Pass --adb with the platform-tools adb executable.")


def run(adb, *args):
    result = subprocess.run([adb, *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode:
        raise RuntimeError("ADB command failed: " + " ".join(args[:3]))
    return result.stdout.strip()


def choose_device(output, serial):
    devices = [parts[0] for line in output.splitlines() if len(parts := line.split()) >= 2 and parts[1] == "device"]
    if serial and serial in devices:
        return serial
    if serial or len(devices) != 1:
        raise RuntimeError("Connect one authorized device or pass --serial. No settings changed.")
    return devices[0]


def summarize(text):
    probes = {}
    for match in LINE.finditer(text):
        probe, elapsed, event, value = match.groups()
        probes.setdefault(probe, []).append({"elapsed_ms": int(elapsed), "event": event, "value": int(value)})
    return probes


def stop_owned(process):
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)


def capture(adb, serial, seconds, output):
    old = run(adb, "-s", serial, "shell", "getprop", "log.tag." + TAG)
    model = run(adb, "-s", serial, "shell", "getprop", "ro.product.model")
    sdk = run(adb, "-s", serial, "shell", "getprop", "ro.build.version.sdk")
    output.mkdir(parents=True, exist_ok=False)
    process = None
    restore_error = None
    changed = False
    try:
        run(adb, "-s", serial, "shell", "setprop", "log.tag." + TAG, "DEBUG")
        changed = True
        if run(adb, "-s", serial, "shell", "getprop", "log.tag." + TAG) != "DEBUG":
            raise RuntimeError("Device did not enable the timing tag.")
        with (output / "timing.log").open("w", encoding="utf-8") as log, (output / "adb-stderr.txt").open("w", encoding="utf-8") as error:
            process = subprocess.Popen([adb, "-s", serial, "logcat", "-v", "threadtime", "-T", "1", TAG + ":D", "*:S"], stdout=log, stderr=error)
            print("CAPTURING. Now open images/videos in the diagnostic build. Ctrl+C finishes early.", flush=True)
            deadline = time.monotonic() + seconds
            try:
                while time.monotonic() < deadline:
                    if process.poll() is not None:
                        raise RuntimeError("logcat stopped unexpectedly; inspect adb-stderr.txt.")
                    time.sleep(min(0.25, max(0, deadline - time.monotonic())))
            except KeyboardInterrupt:
                print("Finishing capture.")
            finally:
                stop_owned(process)
    finally:
        if process is not None:
            stop_owned(process)
        if changed:
            try:
                # Explicit remote quoting preserves the empty property on adb versions that join shell arguments.
                run(adb, "-s", serial, "shell", "setprop", "log.tag." + TAG, "'" + old.replace("'", "'\"'\"'") + "'")
            except RuntimeError as error:
                restore_error = str(error)
        metadata = {"model": model, "android_sdk": sdk, "capture_finished": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                    "requested_seconds": seconds, "previous_tag_level": old, "restore_error": restore_error,
                    "note": "-T 1 may include one old line. IDs are temporary, not URL hashes. No performance PASS."}
        (output / "capture.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
        log = output / "timing.log"
        (output / "events.json").write_text(json.dumps(summarize(log.read_text(encoding="utf-8", errors="replace") if log.exists() else ""), indent=2), encoding="utf-8")
        archive = output.with_suffix(".zip")
        with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as zipped:
            for file in sorted(output.iterdir()):
                if file.is_file():
                    zipped.write(file, file.name)
        print("SEND REPORT:", archive)
        if restore_error:
            print("WARNING: tag restoration failed. Reconnect and run: adb shell setprop log.tag." + TAG + ' ""')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb")
    parser.add_argument("--serial")
    parser.add_argument("--seconds", type=int, default=120)
    args = parser.parse_args()
    if not 10 <= args.seconds <= 600:
        parser.error("--seconds must be between 10 and 600")
    root = Path(__file__).resolve().parents[2]
    adb = resolve_adb(root, args.adb)
    serial = choose_device(run(adb, "devices"), args.serial)
    output = root.parent / ("Pawchan-media-timing-" + uuid.uuid4().hex)
    capture(adb, serial, args.seconds, output)


if __name__ == "__main__":
    main()
