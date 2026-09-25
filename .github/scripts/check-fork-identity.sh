#!/usr/bin/env bash
#
# Guards the fork's own identity against upstream merges.
#
# This repository is a fork of Sumire-Labs/Celeritas-Extra and renames the project: mod id,
# language namespace, Java root package, mixin config and icon all moved. A plain `git merge` of
# upstream changes succeeds quietly even when it re-adds files under the old package or reverts
# gradle.properties, because those are additive or trivially merged edits. This check turns that
# silent breakage into a failed build, so the upstream-sync pull request has to be fixed before it
# can be merged.
#
# Run from the repository root: .github/scripts/check-fork-identity.sh

set -uo pipefail

status=0

fail() {
    echo "::error::$*" >&2
    status=1
}

check_property() {
    local key="$1" expected="$2"
    if ! grep -qxF "${key} = ${expected}" gradle.properties; then
        fail "gradle.properties: ${key} must be '${expected}' (an upstream merge reverted it?)"
    fi
}

check_property "root_package" "dev.mxxfoxx"
check_property "mod_id" "actiniumextra"
check_property "mod_name" "Actinium Extra"

mixin_config="src/main/resources/actiniumextra.mixin.json"
if [ ! -f "$mixin_config" ]; then
    fail "missing ${mixin_config}: the fork's mixin config is named after the mod id"
elif ! grep -q '"package": *"dev\.mxxfoxx\.actiniumextra\.mixin"' "$mixin_config"; then
    fail "${mixin_config}: \"package\" must be dev.mxxfoxx.actiniumextra.mixin"
fi

if [ ! -f "src/main/resources/assets/actiniumextra/icon.png" ]; then
    fail "missing src/main/resources/assets/actiniumextra/icon.png (mod_logo_path points at it)"
fi

# Old identifiers must not come back. Upstream code lands under jp.s12kuma01 and its language keys
# and page ids use the celeritasextra namespace, so either showing up means upstream files were
# merged without being reparented.
if grep -rqE 'jp[./]s12kuma01' src gradle.properties; then
    fail "the old Java root package jp.s12kuma01 is back in src/ or gradle.properties"
fi
if grep -rq 'celeritasextra' src; then
    fail "the old celeritasextra namespace is back in src/ (lang keys, mixin config or page ids)"
fi

if [ "$status" -eq 0 ]; then
    echo "fork identity intact: mod id actiniumextra, root package dev.mxxfoxx"
fi

exit "$status"
