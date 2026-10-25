"""Validate raw AndroidX 1.4.1 results; never award a performance PASS."""
import argparse
import json
import math
import xml.etree.ElementTree as ET
from pathlib import Path

CLASS = "com.solthariel.pawchan.performance.ComposerLayoutBenchmark"
METHOD = "warmErrorLayout"
METRICS = ("frameDurationCpuMs", "frameOverrunMs")
ITERATIONS = 5


def percentile(values, percent):
    ordered = sorted(values)
    position = (len(ordered) - 1) * percent / 100
    lo, hi = math.floor(position), math.ceil(position)
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (position - lo)


def distribution(values):
    return {"frames": len(values), **{f"p{p}": percentile(values, p) for p in (50, 90, 95, 99)}}


def validate(root, diagnostic=False):
    mode_file = root / "measurement-mode.json"
    mode = json.loads(mode_file.read_text(encoding="utf-8-sig")) if mode_file.exists() else {}
    if diagnostic:
        if mode.get("diagnostics") is not True or mode.get("protocol") != "PAW_DIAGNOSTIC_V1":
            raise ValueError("Diagnostic protocol declaration missing")
    elif mode.get("diagnostics") is True:
        raise ValueError("Diagnostic observations cannot be accepted as original-protocol results")
    files = list(root.rglob("*-benchmarkData.json"))
    if len(files) != 1:
        raise ValueError(f"Expected one benchmarkData.json, found {len(files)}")
    data = json.loads(files[0].read_text(encoding="utf-8-sig"))
    benchmarks = data.get("benchmarks", [])
    if len(benchmarks) != 1:
        raise ValueError("Expected exactly one benchmark")
    result = benchmarks[0]
    if result.get("className") != CLASS or result.get("name") != METHOD:
        raise ValueError("Unexpected benchmark identity")
    if result.get("repeatIterations") != ITERATIONS:
        raise ValueError("Expected five measured iterations")
    sampled = result.get("sampledMetrics", {})
    metrics = {}
    lengths = []
    for name in METRICS:
        runs = sampled.get(name, {}).get("runs", [])
        if len(runs) != ITERATIONS or any(not isinstance(r, list) or not r for r in runs):
            raise ValueError(f"Missing/empty per-iteration samples: {name}")
        for run in runs:
            if any(isinstance(v, bool) or not isinstance(v, (int, float)) or not math.isfinite(v) for v in run):
                raise ValueError(f"Invalid sample: {name}")
            if name == "frameDurationCpuMs" and any(v < 0 for v in run):
                raise ValueError("Negative CPU frame duration")
        lengths.append([len(r) for r in runs])
        metrics[name] = {"iterations": [distribution(r) for r in runs],
                         "pooled": distribution([v for r in runs for v in r])}
        if name == "frameOverrunMs":
            metrics[name]["overBudgetFractionByIteration"] = [sum(v > 0 for v in r) / len(r) for r in runs]
    if lengths[0] != lengths[1]:
        raise ValueError("Frame metric sample counts disagree")
    frame_counts = result.get("metrics", {}).get("frameCount", {}).get("runs", [])
    if frame_counts != lengths[0]:
        raise ValueError("Reported frameCount does not match raw per-frame samples")
    traces = [v for v in result.get("profilerOutputs", []) if v.get("type") == "PerfettoTrace"]
    if len(traces) < ITERATIONS:
        raise ValueError("Missing per-iteration Perfetto trace references")
    names = [Path(v["filename"]).name for v in traces]
    if len(set(names)) != len(names):
        raise ValueError("Duplicate trace reference")
    for name in names:
        matches = list(root.rglob(name))
        if len(matches) != 1 or matches[0].stat().st_size == 0:
            raise ValueError(f"Missing/empty/ambiguous trace: {name}")
    cases = []
    for xml in root.rglob("TEST-*.xml"):
        tree = ET.parse(xml)
        for node in tree.iter():
            if node.tag in ("testsuite", "testsuites"):
                for attribute in ("failures", "errors", "skipped", "disabled"):
                    if int(node.get(attribute, "0")) != 0:
                        raise ValueError(f"Nonzero suite {attribute}: {xml}")
            if node.tag in ("failure", "error", "skipped"):
                raise ValueError(f"Failed/skipped instrumentation: {xml}")
        cases.extend(tree.iter("testcase"))
    if len(cases) != 1 or cases[0].get("classname") != CLASS or cases[0].get("name") != METHOD:
        raise ValueError("Missing, duplicate, or unexpected instrumentation test")
    return {"status": "DIAGNOSTIC_RESULTS_VALID_REVIEW_REQUIRED" if diagnostic else "RAW_RESULTS_VALID_REVIEW_REQUIRED",
            "diagnostics": diagnostic, "metrics": metrics,
            "traces": names, "context": data.get("context"),
            "thermalThrottleSleepSeconds": result.get("thermalThrottleSleepSeconds"),
            "percentileMethod": "linear interpolation, pooled and each iteration retained",
            "performancePass": False,
            "limitations": "No agreed tolerances. Review thermal/refresh comparability and traces. Warm isolated composer only."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--diagnostic", action="store_true")
    args = parser.parse_args()
    result = validate(args.reports, diagnostic=args.diagnostic)
    args.output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print("Raw benchmark outputs validated; manual performance review still required.")


if __name__ == "__main__":
    main()
