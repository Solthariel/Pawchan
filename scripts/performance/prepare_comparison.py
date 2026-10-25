"""Make immutable-input comparison copies; never toggle the real checkout."""
import argparse
import hashlib
import json
import shutil
from pathlib import Path

STATUS = "feature/composer/src/main/kotlin/com/solthariel/pawchan/composer/ComposerStatusLine.kt"
AFTER = "Modifier.width(STATUS_SIDE_WIDTH).align(androidx.compose.ui.Alignment.Top),"
BEFORE = "Modifier.width(STATUS_SIDE_WIDTH),"
SKIP = {".git", ".gradle", ".kotlin", ".idea", "build", "__pycache__"}
BINARY = {".apk", ".aab", ".jks", ".keystore", ".zip"}


def ignored(_directory, names):
    return [n for n in names if n in SKIP or Path(n).suffix.lower() in BINARY]


def is_link(path):
    return path.is_symlink() or getattr(path, "is_junction", lambda: False)()


def inventory(root):
    result = {}
    for f in sorted(root.rglob("*")):
        if is_link(f):
            raise ValueError(f"Symbolic link refused: {f}")
        if f.is_file():
            result[f.relative_to(root).as_posix()] = hashlib.sha256(f.read_bytes()).hexdigest()
    return result


def prepare(project, output):
    project, output = project.resolve(), output.resolve()
    if output == project or project in output.parents or output in project.parents:
        raise ValueError("Comparison destination must be outside the project")
    # Refuse reuse rather than delete anything in an existing destination.
    output.mkdir(exist_ok=False, parents=True)
    source = (project / STATUS).read_text(encoding="utf-8-sig")
    if source.count(AFTER) != 1:
        raise ValueError("Expected final device-fix3 status alignment exactly once")
    def reject_links(directory, names):
        omitted = ignored(directory, names)
        for name in names:
            if name not in omitted and is_link(Path(directory) / name):
                raise ValueError(f"Symbolic link refused: {directory}/{name}")
        return omitted
    shutil.copytree(project, output / "candidate", ignore=reject_links)
    shutil.copytree(output / "candidate", output / "baseline")
    status = output / "baseline" / STATUS
    # A component-level counterfactual, not a claim of reproducing an older complete release.
    status.write_text(source.replace(AFTER, BEFORE), encoding="utf-8", newline="\n")
    before, after = inventory(output / "baseline"), inventory(output / "candidate")
    differences = [n for n in sorted(set(before) | set(after)) if before.get(n) != after.get(n)]
    if differences != [STATUS]:
        raise ValueError(f"Unexpected comparison differences: {differences}")
    data = {"baseline": before, "candidate": after, "differences": differences,
            "comparison": "Current source with only right-control align(Top) reverted vs current source"}
    (output / "source-pair.json").write_text(json.dumps(data, indent=2), encoding="utf-8")
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    prepare(args.project, args.output)


if __name__ == "__main__":
    main()
