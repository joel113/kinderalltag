#!/bin/sh
set -eu

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

configured_path=$(git config --get core.hooksPath || true)
if [ -n "$configured_path" ] && [ "$configured_path" != '.githooks' ]; then
    echo "An existing hooks path is configured: $configured_path" >&2
    echo 'Integrate .githooks/pre-commit with your existing hooks before changing it.' >&2
    exit 1
fi
if [ -z "$configured_path" ] && [ -f "$(git rev-parse --git-path hooks/pre-commit)" ]; then
    echo 'An existing .git/hooks/pre-commit was found; integrate it before enabling .githooks.' >&2
    exit 1
fi

chmod +x .githooks/pre-commit
git config --local core.hooksPath .githooks
echo 'Enabled pre-commit formatting and Android lint for this checkout.'
