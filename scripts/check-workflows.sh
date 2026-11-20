#!/bin/sh
set -eu

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

mode=${1:---check}
case "$mode" in
    --check|--write) ;;
    *) echo 'Usage: scripts/check-workflows.sh [--check|--write]' >&2; exit 2 ;;
esac

# Include staged additions, but leave unrelated untracked workflow drafts alone.
set --
for workflow in .github/workflows/*.yml .github/workflows/*.yaml; do
    if [ -f "$workflow" ] && git ls-files --error-unmatch "$workflow" >/dev/null 2>&1; then
        set -- "$@" "$workflow"
    fi
done
if [ "$#" -eq 0 ]; then
    exit 0
fi

if [ ! -x node_modules/.bin/prettier ]; then
    echo 'Workflow checks: Prettier is missing. Run ./scripts/install-dev-tools.sh.' >&2
    exit 1
fi
node_modules/.bin/prettier --config .prettierrc.json "$mode" "$@"

# Formatting never downloads or runs a linter; review its output before linting.
if [ "$mode" = '--write' ]; then
    exit 0
fi
if [ ! -x .tools/bin/actionlint ]; then
    echo 'Workflow checks: actionlint is missing. Run ./scripts/install-dev-tools.sh.' >&2
    exit 1
fi
.tools/bin/actionlint -shellcheck= -pyflakes= "$@"
