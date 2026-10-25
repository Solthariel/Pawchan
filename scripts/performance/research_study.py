"""Fixed four-pair alignment experiment. Counts are viewport passes, not all Compose nodes."""
import argparse
import json
import re
import statistics
from pathlib import Path
import verify_benchmark

PREFIX = "PAW_RESEARCH "
ORDERS = ["CandidateFirst", "BaselineFirst", "BaselineFirst", "CandidateFirst"]


def load(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def require_int(value, name, minimum=0):
    if isinstance(value, bool) or not isinstance(value, int) or value < minimum:
        raise ValueError(f"Invalid integer {name}")
    return value


def read_actions(root):
    logs = list(root.rglob("logcat-*ComposerLayoutBenchmark-warmErrorLayout.txt"))
    if len(logs) != 1:
        raise ValueError(f"Expected one testcase logcat, found {len(logs)}")
    records = []
    for line in logs[0].read_text(encoding="utf-8-sig", errors="strict").splitlines():
        if PREFIX not in line:
            continue
        row = json.loads(line.split(PREFIX, 1)[1])
        require_int(row.get("schema"), "schema", 1)
        if row.get("schema") != 1:
            raise ValueError("Unknown action schema")
        for name in ("pid", "action", "measureCalls", "placeCalls", "measureNs"):
            require_int(row.get(name), name, 1 if name in ("pid", "action") else 0)
        if row["measureCalls"] == 0 and row["measureNs"] != 0:
            raise ValueError("Zero measure calls with nonzero duration")
        records.append(row)
    groups = {}
    for row in records:
        groups.setdefault(row["pid"], []).append(row)
    if len(groups) != 5:
        raise ValueError(f"Expected five restarted target processes, found {len(groups)}")
    measured = []
    for iteration, rows in enumerate(groups.values(), 1):
        if [r["action"] for r in rows] != list(range(1, 13)):
            raise ValueError("Expected exactly two warmups then ten completed actions per process")
        measured.extend(dict(row, iteration=iteration) for row in rows[2:])
    if len(measured) != 50:
        raise ValueError("Expected fifty measured actions")
    return measured


def conditions(root):
    result = []
    for phase in ("before", "after"):
        d = root / ("device-" + phase)
        def field(file, pattern):
            text = (d / file).read_text(encoding="utf-8-sig")
            match = re.search(pattern, text, re.M)
            if not match:
                raise ValueError(f"Missing condition {file}/{pattern}")
            return match.group(1)
        result.append({
            "batteryTemperatureC": int(field("battery.txt", r"^\s*temperature:\s*(\d+)\s*$")) / 10,
            "thermal": int(field("thermal.txt", r"Thermal Status:\s*(\d+)")),
            "hz": float(field("display.txt", r"mActiveRenderFrameRate=([\d.]+)")),
            "brightness": field("settings.txt", r"^screen_brightness=(.*)$").strip(),
            "brightnessMode": field("settings.txt", r"^screen_brightness_mode=(.*)$").strip(),
            "usb": field("battery.txt", r"^\s*USB powered:\s*(\w+)\s*$"),
        })
    return result


def validate_conditions(samples):
    if any(s["thermal"] > 1 or s["usb"] != "true" for s in samples):
        raise ValueError("Thermal/USB conditions invalid; no automatic retry")
    if max(s["batteryTemperatureC"] for s in samples) - min(s["batteryTemperatureC"] for s in samples) > 2:
        raise ValueError("Pair battery temperature spread exceeds registered 2C")
    if len({(s["brightness"], s["brightnessMode"]) for s in samples}) != 1:
        raise ValueError("Display settings changed within pair")
    if max(s["hz"] for s in samples) - min(s["hz"] for s in samples) > 0.01:
        raise ValueError("Render refresh changed within pair")


def analyze_pair(root):
    summary = load(root / "performance-summary.json")
    if summary.get("status") != "COLLECTED_REVIEW_REQUIRED" or summary.get("research") is not True:
        raise ValueError("Incomplete or non-research pair")
    sources = load(root / "source-pair.json")
    diff = [k for k in sources["baseline"].keys() | sources["candidate"].keys()
            if sources["baseline"].get(k) != sources["candidate"].get(k)]
    if diff != ["feature/composer/src/main/kotlin/com/solthariel/pawchan/composer/ComposerStatusLine.kt"]:
        raise ValueError("Uncontrolled source difference")
    variants = {}
    for name in ("baseline", "candidate"):
        d = root / name
        mode = load(d / "measurement-mode.json")
        if mode.get("research") is not True:
            raise ValueError("Research mode not declared")
        verify_benchmark.validate(d, diagnostic=True)
        actions = read_actions(d)
        variants[name] = {
            "actions": actions,
            "meanMeasureCalls": statistics.mean(r["measureCalls"] for r in actions),
            "meanMeasureNs": statistics.mean(r["measureNs"] for r in actions),
            "meanPlaceCalls": statistics.mean(r["placeCalls"] for r in actions),
            "conditions": conditions(d),
        }
    validate_conditions([x for v in variants.values() for x in v["conditions"]])
    baseline_actions = variants["baseline"]["actions"]
    candidate_actions = variants["candidate"]["actions"]
    # Subtract integer totals BEFORE division, avoiding 1.2 - 1.1 rounding below 0.1.
    call_delta = (sum(x["measureCalls"] for x in candidate_actions)
                  - sum(x["measureCalls"] for x in baseline_actions)) / 50
    base_ns = sum(x["measureNs"] for x in baseline_actions)
    candidate_ns = sum(x["measureNs"] for x in candidate_actions)
    return {"order": summary["order"], "variants": variants, "candidateSources": sources["candidate"],
            "callDelta": call_delta, "durationDeltaNs": (candidate_ns - base_ns) / 50,
            "durationRatio": candidate_ns / base_ns if base_ns > 0 else None,
            "performancePass": False}


def decide(pairs):
    if len(pairs) != 4:
        raise ValueError("Exactly four complete paired blocks required")
    count_signal = all(p["callDelta"] >= 0.1 for p in pairs)
    duration_signal = all(p["durationDeltaNs"] >= 500000 and p["durationRatio"] is not None
                          and p["durationRatio"] >= 1.2 for p in pairs)
    if count_signal:
        outcome = "REPEATED_MEASURE_COUNT_SIGNAL_REVIEW_REQUIRED"
    elif duration_signal:
        outcome = "REPEATED_MEASURE_DURATION_SIGNAL_REVIEW_REQUIRED"
    elif all(abs(p["callDelta"]) < 0.1 for p in pairs):
        outcome = "EXTRA_MEASURE_CALL_HYPOTHESIS_NOT_SUPPORTED_WITHIN_SENSITIVITY"
    else:
        outcome = "INCONSISTENT_OR_INCONCLUSIVE"
    return {"outcome": outcome, "countSignal": count_signal, "durationSignal": duration_signal,
            "performancePass": False, "automaticAdditionalRuns": False,
            "limitations": "Four paired blocks, one device, noisy elapsed time; screening signals, not proof of global causality."}


def analyze_study(root):
    plan = load(root / "study-plan.json")
    if plan != load(Path(__file__).with_name("alignment-study-plan.json")) or plan["orders"] != ORDERS:
        raise ValueError("Registered plan differs")
    frozen = load(root / "expected-source-hashes.json")
    pairs = []
    for i, order in enumerate(ORDERS, 1):
        pair = analyze_pair(root / f"pair{i}")
        expected_order = ["candidate", "baseline"] if order == "CandidateFirst" else ["baseline", "candidate"]
        if pair["order"] != expected_order:
            raise ValueError("Observed order differs from registered plan")
        for item in frozen["changes"] + frozen["requirements"]:
            if pair["candidateSources"].get(item["path"]) != item["after"]:
                raise ValueError("Source hashes changed during study")
        pairs.append(pair)
    if any(p["candidateSources"] != pairs[0]["candidateSources"] for p in pairs):
        raise ValueError("Source inventory changed across pairs")
    all_conditions = [s for p in pairs for v in p["variants"].values() for s in v["conditions"]]
    if len({(s["brightness"], s["brightnessMode"]) for s in all_conditions}) != 1:
        raise ValueError("Display settings changed across study")
    if max(s["hz"] for s in all_conditions) - min(s["hz"] for s in all_conditions) > 0.01:
        raise ValueError("Refresh changed across study")
    return {"protocol": plan["protocol"], "decision": decide(pairs), "pairs": pairs}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--pair", type=Path)
    group.add_argument("--study", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = analyze_pair(args.pair) if args.pair else analyze_study(args.study)
    args.output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(result.get("decision", {"status": "PAIR_VALID_RESEARCH_ONLY"}))


if __name__ == "__main__":
    main()
