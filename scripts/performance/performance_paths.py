"""Plan short Windows benchmark/export paths without changing system settings."""
import argparse
import json
import ntpath
import re
import uuid
from pathlib import Path

PATH_BUDGET = 240
TRACE_NAME = "ComposerLayoutBenchmark_warmErrorLayout_iter000_2099-12-31-23-59-59.perfetto-trace"
VARIANTS = ("baseline", "candidate")


def units(value):
    return len(value.encode("utf-16-le")) // 2


def plan(root, project, output, model, android, token):
    for name, value in (("root", root), ("project", project), ("output", output)):
        if not ntpath.isabs(value):
            raise ValueError(f"Absolute Windows {name} path required")
    if not re.fullmatch(r"[0-9a-f]{10}", token):
        raise ValueError("Workspace token must be ten hex characters")
    if any(c in model + android for c in '<>:"/\\|?*\r\n') or not model or not android:
        raise ValueError("Unexpected device model/release; cannot safely predict export path")
    workspace = ntpath.join(ntpath.normpath(root), token)
    try:
        common = ntpath.normcase(ntpath.commonpath([workspace, project]))
    except ValueError:
        common = None
    if common in {ntpath.normcase(workspace), ntpath.normcase(ntpath.normpath(project))}:
        raise ValueError("Workspace must be outside the source checkout")
    paths = []
    for variant in VARIANTS:
        paths.append(ntpath.join(workspace, variant, "performance", "macrobenchmark", "build", "outputs",
                                 "connected_android_test_additional_output", "benchmark", "connected", model + " - " + android, TRACE_NAME))
        paths.append(ntpath.join(output, variant, "raw", TRACE_NAME))
    longest = max(paths, key=units)
    if units(longest) > PATH_BUDGET:
        raise ValueError(f"Artifact path would use {units(longest)} UTF-16 units (budget {PATH_BUDGET}). Choose a shorter WorkRoot/OutputPath before measuring.")
    return {"workspace": workspace, "pathBudget": PATH_BUDGET, "longestPredictedArtifact": longest,
            "longestPredictedUnits": units(longest), "expectedArtifactPaths": paths,
            "note": "Planning guard, not a Windows/adb execution test."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("root", "project", "output", "model", "android"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--plan-file", type=Path, required=True)
    args = parser.parse_args()
    result = plan(args.root, args.project, args.output, args.model, args.android, uuid.uuid4().hex[:10])
    args.plan_file.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(f"Short workspace planned: {result['workspace']}; artifact budget {result['longestPredictedUnits']}/{PATH_BUDGET}.")


if __name__ == "__main__":
    main()
