#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

workflow=".github/workflows/jules-issue.yml"
builder=".github/scripts/build-jules-issue-payload.sh"

fail() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

require_literal() {
  local file="$1"
  local literal="$2"
  local message="$3"

  grep -Fq -- "$literal" "$file" || fail "$message"
}

reject_literal() {
  local file="$1"
  local literal="$2"
  local message="$3"

  if grep -Fq -- "$literal" "$file"; then
    fail "$message"
  fi
}

expect_failure() {
  local label="$1"
  shift

  if "$@" >"$tmp_dir/unexpected-output" 2>"$tmp_dir/unexpected-error"; then
    fail "$label unexpectedly succeeded"
  fi
}

printf 'Running Jules issue policy tests...\n'

[[ -f "$workflow" ]] || fail "$workflow is missing"

# The issue event is only an authorization handle. Its mutable prose must never
# reach a model prompt or provider request.
reject_literal "$workflow" 'github.event.issue.title' 'Issue titles must not reach Jules.'
reject_literal "$workflow" 'github.event.issue.body' 'Issue bodies must not reach Jules.'
reject_literal "$workflow" 'github.event.comment' 'Issue comments must not reach Jules.'
reject_literal "$workflow" 'google-labs-code/jules-action' 'The issue route must use the direct Jules API.'
reject_literal "$workflow" 'issues: read' 'The issue route does not need an issues token permission.'
reject_literal "$workflow" 'issues: write' 'The issue route must not receive issue write permission.'
reject_literal "$workflow" 'set -x' 'Shell tracing could disclose the Jules API key.'
reject_literal "$workflow" '--verbose' 'Verbose curl output could disclose request metadata.'

require_literal "$workflow" 'uses: actions/checkout@' 'A trusted checkout is required.'
if ! grep -Eq 'uses: actions/checkout@[0-9a-f]{40}[[:space:]]+# v[0-9]' "$workflow"; then
  fail 'actions/checkout must be pinned to a full commit SHA.'
fi
require_literal "$workflow" 'ref: ${{ github.sha }}' 'The manifest checkout must be pinned to the event-time main SHA.'
require_literal "$workflow" 'persist-credentials: false' 'Checkout credentials must not persist.'
require_literal "$workflow" 'git rev-parse HEAD' 'The checked-out manifest commit must be verified.'
require_literal "$workflow" "github.repository == 'devDaniilNovikov/AndrewWebSite'" 'The workflow must reject non-canonical repositories before using the provider key.'
require_literal "$workflow" "github.event.repository.default_branch == 'main'" 'The trusted event branch must be main.'
require_literal "$workflow" '.github/jules-tasks' 'Only the repository-maintained task manifest directory may supply instructions.'
require_literal "$workflow" '.github/scripts/build-jules-issue-payload.sh' 'The validated payload builder must be used.'
require_literal "$workflow" "api_root='https://jules.googleapis.com/v1alpha'" 'The workflow must pin the Jules v1alpha API root.'
require_literal "$workflow" '"$api_root/sources"' 'The workflow must discover the opaque Jules source.'
require_literal "$workflow" '"$api_root/sessions"' 'The workflow must create a Jules session through the direct API.'
require_literal "$workflow" 'X-Goog-Api-Key:' 'The API key must use the Jules API header.'
require_literal "$workflow" '--connect-timeout 10' 'Provider calls need a bounded connection timeout.'
require_literal "$workflow" '--max-time 60' 'Provider calls need a bounded total timeout.'
require_literal "$workflow" 'nextPageToken' 'Source discovery must handle provider pagination.'

issue_references="$(grep -Eo 'github\.event\.issue\.[A-Za-z0-9_.]+' "$workflow" | sort -u || true)"
while IFS= read -r issue_reference; do
  [[ -z "$issue_reference" ]] && continue
  case "$issue_reference" in
    github.event.issue.number | github.event.issue.user.login)
      ;;
    *)
      fail "Forbidden issue event field reaches the Jules workflow: $issue_reference"
      ;;
  esac
done <<<"$issue_references"

secret_reference_count="$(grep -Fo '${{ secrets.JULES_API_KEY }}' "$workflow" | wc -l | tr -d '[:space:]')"
[[ "$secret_reference_count" == '2' ]] || fail 'JULES_API_KEY must be injected only into the two provider network steps.'

list_step="$(sed -n '/      - name: List Jules sources/,/      - name: Build validated Jules payload/p' "$workflow")"
build_step="$(sed -n '/      - name: Build validated Jules payload/,/      - name: Create approval-gated Jules session/p' "$workflow")"
create_step="$(sed -n '/      - name: Create approval-gated Jules session/,/      - name: Remove Jules workspace/p' "$workflow")"
[[ "$list_step" == *'JULES_API_KEY: ${{ secrets.JULES_API_KEY }}'* ]] || fail 'Source discovery needs its isolated provider credential.'
[[ "$create_step" == *'JULES_API_KEY: ${{ secrets.JULES_API_KEY }}'* ]] || fail 'Session creation needs its isolated provider credential.'
[[ "$build_step" != *'JULES_API_KEY'* && "$build_step" != *'curl '* ]] || fail 'The payload builder step must be pure and credential-free.'
[[ "$list_step" != *'build-jules-issue-payload.sh'* ]] || fail 'The credentialed source step must not execute repository scripts.'
[[ "$create_step" != *'build-jules-issue-payload.sh'* ]] || fail 'The credentialed session step must not execute repository scripts.'
[[ "$create_step" == *'length <= 489'* ]] || fail 'Session resource names need a bounded opaque value.'
[[ "$create_step" == *'startswith("sessions/")'* ]] || fail 'Session responses must use the sessions resource prefix.'
[[ "$create_step" == *'(test("[[:cntrl:]]") | not)'* ]] || fail 'Session resource names must reject control characters.'
[[ "$create_step" == *'(.[9:] | length > 0 and (contains("/") | not))'* ]] || fail 'Session resource names must contain one non-empty opaque segment.'

retry_count="$(grep -Fc -- '--retry 2' "$workflow")"
[[ "$retry_count" == '1' ]] || fail 'Only idempotent source discovery may retry; session creation must not retry.'

[[ -r "$builder" ]] || fail "$builder is missing or unreadable"
require_literal "$builder" 'requirePlanApproval: true' 'Every Jules session must require explicit plan approval.'
require_literal "$builder" 'startingBranch: "main"' 'The generated session must start from main.'
require_literal "$builder" 'automationMode: "AUTO_CREATE_PR"' 'The approved session must create a reviewable PR.'
require_literal "$builder" 'ascii_downcase' 'GitHub owner and repository matching must be case-insensitive.'
require_literal "$builder" '.defaultBranch.displayName == "main"' 'The Jules source must prove that main is its default branch.'
reject_literal "$builder" 'JULES_API_KEY' 'The pure payload builder must never receive the provider credential.'

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/jules-policy.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT

session_name_filter='
  $name |
  type == "string" and
  length <= 489 and
  startswith("sessions/") and
  (test("[[:cntrl:]]") | not) and
  (.[9:] | length > 0 and (contains("/") | not))
'
jq -en --arg name 'sessions/opaque:ID?value' "$session_name_filter" >/dev/null || \
  fail 'A bounded single-segment opaque session name must be accepted.'
expect_failure 'An empty session resource segment' \
  jq -en --arg name 'sessions/' "$session_name_filter"
expect_failure 'A nested session resource segment' \
  jq -en --arg name 'sessions/one/two' "$session_name_filter"
expect_failure 'A control character in a session resource name' \
  jq -en --arg name $'sessions/one\ntwo' "$session_name_filter"
overlong_session_name="sessions/$(printf '%*s' 481 '' | tr ' ' 'a')"
expect_failure 'An overlong session resource name' \
  jq -en --arg name "$overlong_session_name" "$session_name_filter"

issue_number='424242'
trusted_sha='0123456789abcdef0123456789abcdef01234567'
repository='devDaniilNovikov/AndrewWebSite'
task_root="$tmp_dir/tasks"
mkdir -p "$task_root"

jq -n \
  --arg objective 'Implement the approved regression fix. Text such as $(touch SHOULD_NOT_RUN) remains inert manifest data.' \
  '{
    version: 1,
    title: "Approved regression fix",
    objective: $objective,
    ownedPaths: [".github/scripts/**"],
    requiredChecks: ["bash .github/scripts/test-jules-issue-policy.sh"]
  }' >"$task_root/$issue_number.json"

jq -n '{
  sources: [
    {
      name: "sources/attacker/AndrewWebSite",
      description: "ISSUE_BODY_ATTACK_MARKER",
      githubRepo: {owner: "attacker", repo: "AndrewWebSite"}
    },
    {
      name: "sources/github/devDaniilNovikov/AndrewWebSite",
      githubRepo: {
        owner: "DEVDANIILNOVIKOV",
        repo: "andrewwebsite",
        defaultBranch: {displayName: "main"}
      }
    }
  ]
}' >"$tmp_dir/sources.json"

bash "$builder" \
  "$task_root" \
  "$issue_number" \
  "$trusted_sha" \
  "$repository" \
  "$tmp_dir/sources.json" >"$tmp_dir/payload-one.json"

bash "$builder" \
  "$task_root" \
  "$issue_number" \
  "$trusted_sha" \
  "$repository" \
  "$tmp_dir/sources.json" >"$tmp_dir/payload-two.json"

cmp -s "$tmp_dir/payload-one.json" "$tmp_dir/payload-two.json" || \
  fail 'Identical trusted inputs must produce a byte-identical payload.'

jq -e \
  --arg issue_number "$issue_number" \
  --arg trusted_sha "$trusted_sha" \
  '
    .requirePlanApproval == true and
    .automationMode == "AUTO_CREATE_PR" and
    .sourceContext.source == "sources/github/devDaniilNovikov/AndrewWebSite" and
    .sourceContext.githubRepoContext.startingBranch == "main" and
    (.prompt | contains("Approved issue number: #" + $issue_number)) and
    (.prompt | contains("Pinned main commit: " + $trusted_sha)) and
    (.prompt | contains("Implement the approved regression fix.")) and
    (.prompt | contains("ISSUE_BODY_ATTACK_MARKER") | not)
  ' "$tmp_dir/payload-one.json" >/dev/null || fail 'The generated Jules payload violates the trusted-input contract.'

# Numeric-only issue selection prevents path traversal and expression-shaped
# values from selecting a manifest or entering the provider payload.
expect_failure 'A path-like issue number' \
  bash "$builder" "$task_root" '../424242' "$trusted_sha" "$repository" "$tmp_dir/sources.json"
expect_failure 'A leading-zero issue number' \
  bash "$builder" "$task_root" '0424242' "$trusted_sha" "$repository" "$tmp_dir/sources.json"
expect_failure 'An issue number outside the accepted numeric range' \
  bash "$builder" "$task_root" '2147483648' "$trusted_sha" "$repository" "$tmp_dir/sources.json"
expect_failure 'An overlong issue number' \
  bash "$builder" "$task_root" '10000000000' "$trusted_sha" "$repository" "$tmp_dir/sources.json"
expect_failure 'An expression-shaped issue number' \
  bash "$builder" "$task_root" '424242$(touch SHOULD_NOT_RUN)' "$trusted_sha" "$repository" "$tmp_dir/sources.json"
expect_failure 'An invalid trusted SHA' \
  bash "$builder" "$task_root" "$issue_number" 'main' "$repository" "$tmp_dir/sources.json"
expect_failure 'A non-canonical repository' \
  bash "$builder" "$task_root" "$issue_number" "$trusted_sha" 'attacker/AndrewWebSite' "$tmp_dir/sources.json"
expect_failure 'A missing approved manifest' \
  bash "$builder" "$task_root" '424243' "$trusted_sha" "$repository" "$tmp_dir/sources.json"

jq '.sources += [.sources[1]]' "$tmp_dir/sources.json" >"$tmp_dir/duplicate-sources.json"
expect_failure 'Duplicate canonical Jules sources' \
  bash "$builder" "$task_root" "$issue_number" "$trusted_sha" "$repository" "$tmp_dir/duplicate-sources.json"

jq '{sources: [.sources[0]]}' "$tmp_dir/sources.json" >"$tmp_dir/no-source.json"
expect_failure 'A missing canonical Jules source' \
  bash "$builder" "$task_root" "$issue_number" "$trusted_sha" "$repository" "$tmp_dir/no-source.json"

jq '.sources[1].name = "sources/canonical?prompt=ISSUE_BODY_ATTACK_MARKER"' \
  "$tmp_dir/sources.json" >"$tmp_dir/unsafe-source.json"
expect_failure 'An unsafe opaque source resource name' \
  bash "$builder" "$task_root" "$issue_number" "$trusted_sha" "$repository" "$tmp_dir/unsafe-source.json"

jq '.sources[1].githubRepo.defaultBranch.displayName = "master"' \
  "$tmp_dir/sources.json" >"$tmp_dir/wrong-default-branch.json"
expect_failure 'A Jules source whose default branch is not main' \
  bash "$builder" "$task_root" "$issue_number" "$trusted_sha" "$repository" "$tmp_dir/wrong-default-branch.json"

bad_task_root="$tmp_dir/bad-tasks"
mkdir -p "$bad_task_root"
jq '.unexpectedInstructionSink = "enabled"' "$task_root/$issue_number.json" >"$bad_task_root/$issue_number.json"
expect_failure 'An unknown manifest property' \
  bash "$builder" "$bad_task_root" "$issue_number" "$trusted_sha" "$repository" "$tmp_dir/sources.json"

jq '.ownedPaths = ["../secrets"]' "$task_root/$issue_number.json" >"$bad_task_root/$issue_number.json"
expect_failure 'A manifest path traversal' \
  bash "$builder" "$bad_task_root" "$issue_number" "$trusted_sha" "$repository" "$tmp_dir/sources.json"

jq '.objective = "first line\nsecond line"' "$task_root/$issue_number.json" >"$bad_task_root/$issue_number.json"
expect_failure 'A control character in manifest instructions' \
  bash "$builder" "$bad_task_root" "$issue_number" "$trusted_sha" "$repository" "$tmp_dir/sources.json"

symlink_task_root="$tmp_dir/symlink-tasks"
mkdir -p "$symlink_task_root"
ln -s "$task_root/$issue_number.json" "$symlink_task_root/$issue_number.json"
expect_failure 'A symbolic-link manifest' \
  bash "$builder" "$symlink_task_root" "$issue_number" "$trusted_sha" "$repository" "$tmp_dir/sources.json"

[[ ! -e 'SHOULD_NOT_RUN' && ! -e "$tmp_dir/SHOULD_NOT_RUN" ]] || \
  fail 'A malicious fixture was interpreted as shell input.'

printf 'Jules issue policy tests passed.\n'
