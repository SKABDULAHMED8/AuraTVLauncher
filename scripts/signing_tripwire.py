#!/usr/bin/env python3
"""Signing tripwire for Aura Launcher.

Fails the build if anything that protects the production identity has changed:

  * the production `signingConfigs` block in app/build.gradle.kts
  * the `release` build type in app/build.gradle.kts
  * the production application ID (must stay com.auratv.launcher, defined once)
  * the release workflow .github/workflows/build.yml

It also fails if the release signing config is referenced more than once, or if a
build type is cloned with initWith() (which would copy production signing settings).

It reads only files in the working tree. It never reads, prints or needs a secret.

Usage:
  python3 scripts/signing_tripwire.py                  check the working tree
  python3 scripts/signing_tripwire.py --write-baseline compute a new baseline from git
                                                       (v1.0.0 gradle file, main workflow);
                                                       only the repository owner should do this
"""
import hashlib
import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
GRADLE = ROOT / "app" / "build.gradle.kts"
WORKFLOW = ROOT / ".github" / "workflows" / "build.yml"
BASELINE = ROOT / "scripts" / "signing-baseline.json"
PROD_ID = "com.auratv.launcher"


def norm(text: str) -> str:
    return text.replace("\r\n", "\n")


def sha(text: str) -> str:
    return hashlib.sha256(norm(text).encode("utf-8")).hexdigest()


def block(text: str, start_pattern: str) -> str:
    m = re.search(start_pattern, text)
    if not m:
        raise SystemExit(f"TRIPWIRE: could not find block /{start_pattern}/ in app/build.gradle.kts")
    i = text.index("{", m.start())
    depth = 0
    for j in range(i, len(text)):
        if text[j] == "{":
            depth += 1
        elif text[j] == "}":
            depth -= 1
            if depth == 0:
                return text[m.start() : j + 1]
    raise SystemExit(f"TRIPWIRE: unbalanced braces after /{start_pattern}/")


def measure(gradle_text: str, workflow_text: str) -> dict:
    gradle_text = norm(gradle_text)
    signing = block(gradle_text, r"signingConfigs\s*\{")
    build_types = block(gradle_text, r"buildTypes\s*\{")
    release = block(build_types, r"release\s*\{")
    return {
        "signing_block_sha256": sha(signing),
        "release_build_type_sha256": sha(release),
        "workflow_sha256": sha(workflow_text),
        "application_ids": re.findall(r'applicationId\s*=\s*"([^"]+)"', gradle_text),
        "release_signing_references": len(re.findall(r'signingConfigs\.getByName\("release"\)', gradle_text)),
        "initwith_used": "initWith" in gradle_text,
    }


def git_show(ref_path: str) -> str:
    return subprocess.check_output(["git", "show", ref_path], cwd=ROOT, text=True)


def write_baseline() -> None:
    gradle = git_show("v1.0.0:app/build.gradle.kts")
    workflow = git_show("main:.github/workflows/build.yml")
    data = measure(gradle, workflow)
    data["source"] = "app/build.gradle.kts from tag v1.0.0; build.yml from main at v1.1.0 branch start"
    BASELINE.write_text(json.dumps(data, indent=2) + "\n")
    print("Baseline written:", json.dumps(data, indent=2))


def check() -> int:
    base = json.loads(BASELINE.read_text())
    now = measure(GRADLE.read_text(), WORKFLOW.read_text())
    problems = []
    if now["signing_block_sha256"] != base["signing_block_sha256"]:
        problems.append("production signingConfigs block changed")
    if now["release_build_type_sha256"] != base["release_build_type_sha256"]:
        problems.append("release build type changed")
    if now["workflow_sha256"] != base["workflow_sha256"]:
        problems.append(".github/workflows/build.yml changed")
    if now["application_ids"] != [PROD_ID]:
        problems.append(f"applicationId must be defined exactly once as {PROD_ID}, found {now['application_ids']}")
    if now["release_signing_references"] != 1:
        problems.append(
            f"release signing config referenced {now['release_signing_references']} times (expected exactly 1)"
        )
    if now["initwith_used"]:
        problems.append("initWith() is used; it can copy production signing settings into another build type")
    if problems:
        print("SIGNING TRIPWIRE FAILED:")
        for p in problems:
            print(" -", p)
        print("Production signing files are protected. Do not change them without the owner's approval.")
        return 1
    print("Signing tripwire passed: production signing, release build type, application ID and build.yml are unchanged.")
    return 0


if __name__ == "__main__":
    if "--write-baseline" in sys.argv:
        write_baseline()
        sys.exit(0)
    sys.exit(check())
