"""Check changed source at Stop without editing files or the Git index."""

import json
import subprocess
import sys
from pathlib import Path


CHECKS = {
    "backend": ("spotlessCheck", "checkstyleMain", "checkstyleTest"),
    "android": ("spotlessCheck",),
}


def git(root, *arguments):
    return subprocess.check_output(["git", "-C", str(root), *arguments], stderr=subprocess.PIPE)


def changed_paths(root):
    try:
        tracked = git(root, "diff", "--name-only", "-z", "HEAD", "--")
    except subprocess.CalledProcessError:
        # An initialized repository may not have its first commit yet.
        git(root, "rev-parse", "--git-dir")
        tracked = git(root, "diff", "--name-only", "-z", "--")
        tracked += git(root, "diff", "--cached", "--name-only", "-z", "--")
    untracked = git(root, "ls-files", "--others", "--exclude-standard", "-z")
    return {p for p in (tracked + untracked).decode(errors="replace").split("\0") if p}


def relevant(path, area):
    prefix = area + "/"
    if not path.startswith(prefix):
        return False
    relative = path[len(prefix):]
    if area == "backend":
        return relative.endswith((".java", ".gradle", ".gradle.kts")) or relative in {
            "gradle.properties", "gradlew", "gradlew.bat"
        } or relative.startswith(("gradle/", "config/formatter/", "config/checkstyle/"))
    return relative.endswith((".kt", ".kts", ".gradle")) or relative in {
        ".editorconfig", "gradlew", "gradlew.bat", "gradle.properties"
    } or relative.startswith("gradle/")


def failure_result(message, already_continued):
    if already_continued:
        return {"systemMessage": "형식 검사 미통과: " + message
                + " 자동 재시도는 한 번으로 제한합니다. 완료 보고에 미해결 원인을 명시하세요."}
    return {"decision": "block", "reason": message
            + " 변경 영역의 spotlessApply로 수정하고 재검사하세요. 사용자 변경과 스테이징을 보존하고,"
            + " 실행 환경 문제라면 성공으로 보고하지 말고 원인을 설명하세요."}


def check(payload):
    root = Path(git(Path(payload["cwd"]), "rev-parse", "--show-toplevel").decode().strip())
    paths = changed_paths(root)
    areas = [area for area in CHECKS if any(relevant(p, area) for p in paths)]
    failures = []
    for area in areas:
        command = [str(root / area / "gradlew"), "--console=plain", *CHECKS[area]]
        try:
            result = subprocess.run(command, cwd=root / area, stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT, text=True, timeout=120)
            if result.returncode:
                failures.append(f"{area}: 검사 실패\n{result.stdout[-5000:]}")
        except (OSError, subprocess.TimeoutExpired) as error:
            failures.append(f"{area}: 검사 실행 불가 ({error})")
    if failures:
        return failure_result("\n".join(failures), payload.get("stop_hook_active", False))
    return {}


def main():
    payload = {}
    try:
        payload = json.load(sys.stdin)
        if not isinstance(payload, dict) or not isinstance(payload.get("cwd"), str):
            raise ValueError("훅 입력에 cwd 문자열이 필요합니다.")
        result = check(payload)
    except (ValueError, KeyError, OSError, subprocess.SubprocessError) as error:
        result = failure_result(f"Stop 형식 훅 실행 불가 ({error})",
                                payload.get("stop_hook_active", False) if isinstance(payload, dict) else False)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
