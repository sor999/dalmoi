#!/bin/sh
set -eu

repository_root=$(git rev-parse --show-toplevel)
cd "$repository_root"
existing_hooks_path=$(git config --local --get core.hooksPath || true)
if [ -n "$existing_hooks_path" ] && [ "$existing_hooks_path" != '.githooks' ]; then
    echo "기존 core.hooksPath를 보존합니다: $existing_hooks_path" >&2
    exit 1
fi
if [ ! -x .githooks/pre-commit ]; then
    echo '.githooks/pre-commit 실행 권한을 확인하세요.' >&2
    exit 1
fi
git config --local core.hooksPath .githooks
echo '커밋 형식 검사 훅 설치 완료'
