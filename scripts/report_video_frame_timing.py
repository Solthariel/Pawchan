"""Export only complete, current device measurements. Missing drop counters are not zero."""
import argparse
import csv
from pathlib import Path
from statistics import median


FIELDS = ("case", "trial", "first_ms", "from_ms", "to_ms", "decoder", "decoder_drops", "output_drops", "model", "api")
CASES = ("fhd60", "v4k60")
TRIALS = (1, 2, 3, 4, 5)


def results(lines, run):
    rows = {}
    for line in lines:
        _, marker, data = line.partition("PAW_FRAME ")
        if not marker:
            continue
        values = dict(item.split("=", 1) for item in data.split() if "=" in item)
        if values.get("run") != run:
            continue
        if any(not values.get(k) for k in FIELDS):
            raise ValueError("Incomplete PAW_FRAME measurement")
        key = (values["case"], int(values["trial"]))
        if key in rows or key[0] not in CASES or key[1] not in TRIALS:
            raise ValueError("Unexpected or duplicate case/trial")
        if values["decoder"] not in ("hw", "sw", "unknown") or int(values["first_ms"]) < 0:
            raise ValueError("Invalid decoder/first frame")
        if not 0 <= int(values["from_ms"]) < int(values["to_ms"]):
            raise ValueError("Invalid frame-counter window")
        for counter in ("decoder_drops", "output_drops"):
            if values[counter] != "NA" and int(values[counter]) < 0:
                raise ValueError("Invalid drop counter")
        rows[key] = {k: values[k] for k in FIELDS}
    expected = [(case, trial) for case in CASES for trial in TRIALS]
    if set(rows) != set(expected) or len({(r["model"], int(r["api"])) for r in rows.values()}) != 1:
        raise ValueError("Need five fresh measurements per clip from one run on one device")
    return [rows[k] for k in expected]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path)
    parser.add_argument("--run", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    raw = args.log.read_bytes()
    encoding = "utf-16" if raw.startswith((b"\xff\xfe", b"\xfe\xff")) else "utf-8-sig"
    try:
        rows = results(raw.decode(encoding, errors="replace").splitlines(), args.run)
    except ValueError as error:
        parser.exit(1, f"No valid device report: {error}\n")
    with args.output.open("w", newline="", encoding="utf-8-sig") as out:
        writer = csv.DictWriter(out, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    for case in CASES:
        times = [int(r["first_ms"]) for r in rows if r["case"] == case]
        print(f"{case}: first-frame median {median(times):g} ms ({len(TRIALS)} new sessions after warm-up)")
    print(f"Frame-drop windows and actual decode modes: {args.output}")


if __name__ == "__main__":
    main()
