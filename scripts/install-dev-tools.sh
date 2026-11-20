#!/bin/sh
set -eu

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

command -v npm >/dev/null 2>&1 || { echo 'Install Node.js 20+ and npm first.' >&2; exit 1; }
command -v go >/dev/null 2>&1 || { echo 'Install Go 1.25+ first.' >&2; exit 1; }

npm ci --ignore-scripts --no-audit --no-fund
mkdir -p .tools/bin
GOBIN="$repo_root/.tools/bin" go install github.com/rhysd/actionlint/cmd/actionlint@v1.7.12
echo 'Installed pinned Prettier and actionlint for workflow checks.'
