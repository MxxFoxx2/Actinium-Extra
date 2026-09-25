#!/usr/bin/env bash
#
# Reports whatever upstream Celeritas Extra has added since the last sync: a pull request when the
# merge is clean, an issue when it conflicts. Called by .github/workflows/upstream-sync.yml and
# runnable locally (see --dry-run).
#
# Why a pull request rather than an automatic merge into main: this fork renamed the project, so
# upstream commits need judgement - files added under jp.s12kuma01 or with celeritasextra keys have
# to be reparented before they can land. The merge is therefore attempted on a throwaway branch,
# only a clean attempt is pushed, and the branch's own CI run (identity check + build + tests) is
# what proves the fork still works.
#
# Usage:   .github/scripts/upstream-sync.sh [--dry-run]
# Env:     UPSTREAM_URL, UPSTREAM_BRANCH, GH_REPO (defaults to the origin repository)

set -uo pipefail

UPSTREAM_URL="${UPSTREAM_URL:-https://github.com/Sumire-Labs/Celeritas-Extra.git}"
UPSTREAM_BRANCH="${UPSTREAM_BRANCH:-main}"
DRY_RUN=0
[ "${1:-}" = "--dry-run" ] && DRY_RUN=1

say() { printf '%s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

# The repository to report into. Never rely on gh's own remote heuristics: this clone has an
# `upstream` remote pointing at the original project, and gh may well pick that, which would open
# pull requests and issues on someone else's repository. GITHUB_REPOSITORY wins on a runner, then an
# explicit GH_REPO, and otherwise the slug is parsed out of `origin`.
slug_from_url() {
    # Handles https://host/o/r.git, git@host:o/r.git and ssh://git@host/o/r.git by normalising the
    # separators and taking the last two path segments.
    printf '%s' "$1" \
        | sed -E -e 's#\.git/?$##' -e 's#://#/#g' -e 's#:#/#g' -e 's#^[^@]*@##' -e 's#/*$##' \
        | awk -F/ 'NF>1 {print $(NF-1)"/"$NF}'
}

if [ -z "${GH_REPO:-}" ]; then
    origin_url="$(git remote get-url origin 2>/dev/null)" || die "no origin remote; set GH_REPO=owner/repo"
    GH_REPO="${GITHUB_REPOSITORY:-$(slug_from_url "$origin_url")}"
fi
case "$GH_REPO" in
    */*) ;;
    *) die "GH_REPO '${GH_REPO}' is not owner/repo" ;;
esac

target_ref="$(git symbolic-ref --short HEAD 2>/dev/null || git rev-parse HEAD)"
branch_body="$(mktemp)"
issue_body="$(mktemp)"
trap 'rm -f "$branch_body" "$issue_body"; git checkout -q "$target_ref" 2>/dev/null || true' EXIT

# Fetch by URL and work through FETCH_HEAD: no dependency on how the remote happens to be named,
# locally or on the runner.
git fetch --quiet "$UPSTREAM_URL" "$UPSTREAM_BRANCH" || die "could not fetch ${UPSTREAM_URL} (${UPSTREAM_BRANCH})"

upstream_sha="$(git rev-parse FETCH_HEAD)"
upstream_short="$(git rev-parse --short=7 FETCH_HEAD)"
behind="$(git rev-list --count "HEAD..FETCH_HEAD")"
branch="upstream/sync-${upstream_short}"
commits="$(git log --no-merges --format='- %h %s' "HEAD..FETCH_HEAD")"

say "upstream is at ${upstream_short}, ${behind} commit(s) not in this branch"
if [ "$behind" -eq 0 ]; then
    say "already in sync; nothing to do"
    exit 0
fi

if gh pr view --repo "$GH_REPO" --head "$branch" --json url -q .url >/dev/null 2>&1; then
    say "an open pull request already covers ${upstream_short}"
    exit 0
fi

body_common="Upstream \`Sumire-Labs/Celeritas-Extra@${upstream_short}\`, ${behind} commit(s):

${commits}
"

git config user.name >/dev/null 2>&1 || git config user.name "github-actions[bot]"
git config user.email >/dev/null 2>&1 || git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

git checkout -q -B "$branch" || die "could not create ${branch}"

conflicts=""
if git merge --no-edit FETCH_HEAD >/dev/null 2>&1; then
    say "merge onto ${branch} is clean"
else
    conflicts="$(git diff --name-only --diff-filter=U | sed 's/^/- /')"
    git merge --abort
    say "merge onto ${branch} conflicts in:"
    say "$conflicts"
fi

if [ "$DRY_RUN" = 1 ]; then
    say "--dry-run: nothing pushed"
    exit 0
fi

if [ -z "$conflicts" ]; then
    git push -q --force-with-lease origin "$branch" || die "could not push ${branch}"
    cat >"$branch_body" <<EOF
${body_common}
**Before merging:** this fork renamed the project. Any file upstream added under \`jp.s12kuma01\`, or
carrying \`celeritasextra\` language keys or mod id, has to be reparented to \`dev.mxxfoxx\` /
\`actiniumextra\` first - the identity check on this pull request fails otherwise. See README,
"Keeping in sync with upstream".
EOF
    gh pr create --repo "$GH_REPO" --base main --head "$branch" \
        --title "Sync upstream Celeritas Extra (${upstream_short})" \
        --body-file "$branch_body"
    say "opened pull request for ${branch}"
    exit 0
fi

cat >"$issue_body" <<EOF
${body_common}
Conflicting files:
${conflicts}

Merge them by hand and keep the fork's identity: \`dev.mxxfoxx\` packages, \`actiniumextra\` mod id and
language keys. A conflict reported at a \`dev/mxxfoxx/...\` path means upstream added the file under
the old \`jp/s12kuma01\` namespace: git matched it to this fork's rename, so take upstream's content
and keep the fork's path. See README, "Keeping in sync with upstream".
EOF

existing_issue="$(gh issue list --repo "$GH_REPO" --state open --label upstream-sync \
    --search "${upstream_short} in:title" --json number -q '.[0].number // ""')"
if [ -n "$existing_issue" ]; then
    gh issue edit "$existing_issue" --repo "$GH_REPO" --body-file "$issue_body"
    say "updated issue #${existing_issue}"
else
    gh issue create --repo "$GH_REPO" --title "Upstream Celeritas Extra has new changes (${upstream_short})" \
        --label upstream-sync --body-file "$issue_body"
    say "opened an issue for the conflicted sync"
fi
