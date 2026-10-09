#!/bin/sh
# 저장소의 .githooks/ 를 git 훅 폴더로 쓰게 한다. 클론한 뒤 한 번 돌린다.
set -e
cd "$(git rev-parse --show-toplevel)"
git config core.hooksPath .githooks
echo "core.hooksPath 를 .githooks 로 설정했다."
