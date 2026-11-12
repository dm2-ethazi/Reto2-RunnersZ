import subprocess
import sys


PROTECTED = {"main", "develop"}


def check(branch):
    return 1 if branch in PROTECTED else 0


def main():
    result = subprocess.run(
        ["git", "symbolic-ref", "--quiet", "--short", "HEAD"],
        text=True,
        capture_output=True,
        check=False,
    )
    if result.returncode == 1:
        return 0
    if result.returncode != 0:
        print("Cannot determine Git branch; refusing operation.", file=sys.stderr)
        return 1
    branch = result.stdout.strip()
    if check(branch):
        print(
            f"Direct commit/push blocked on {branch}. Use a feature branch and a PR.",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
