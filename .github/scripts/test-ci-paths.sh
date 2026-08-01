#!/bin/bash
set -euo pipefail

# This is the CI path and security policy compliance test script.
# It runs assertions against .github/workflows/ci.yml and other scripts.

echo "Running CI Policy Compliance Tests..."

WORKFLOW=".github/workflows/ci.yml"

# 1. Check for missing or renamed 'Frontend quality' job name
if ! grep -q "name: Frontend quality" "$WORKFLOW"; then
  echo "Error: Missing or renamed 'Frontend quality' job name." >&2
  exit 1
fi

# 2. Check for job-level skip or workflow-level paths filters
if grep -q "paths:" "$WORKFLOW" || grep -q "paths-ignore:" "$WORKFLOW"; then
  echo "Error: Workflow-level paths/paths-ignore filters are forbidden." >&2
  exit 1
fi

# The frontend-quality job itself must not have an 'if:' skip condition at the job level
if grep -A 5 "^  frontend-quality:" "$WORKFLOW" | grep -q "^    if:"; then
  echo "Error: Job-level skip condition ('if:') is forbidden on frontend-quality." >&2
  exit 1
fi

# 3. Check for mutable action refs
while read -r line; do
  if [[ "$line" =~ uses:[[:space:]]*([^@]+)@([^#[:space:]]+) ]]; then
    action="${BASH_REMATCH[1]}"
    version="${BASH_REMATCH[2]}"
    if [[ ! "$version" =~ ^[0-9a-f]{40}$ ]]; then
      echo "Error: Mutable action reference found for '$action': @$version" >&2
      exit 1
    fi
  fi
done < "$WORKFLOW"

# 4. Check pull_request_target, secrets, or write permissions in frontend job
FRONTEND_JOB_BLOCK=$(sed -n '/^  frontend-quality:/,$p' "$WORKFLOW")

if echo "$FRONTEND_JOB_BLOCK" | grep -q "pull_request_target"; then
  echo "Error: pull_request_target is forbidden in frontend job." >&2
  exit 1
fi

if echo "$FRONTEND_JOB_BLOCK" | grep -q "secrets\."; then
  echo "Error: Use of secrets is forbidden in frontend job." >&2
  exit 1
fi

if echo "$FRONTEND_JOB_BLOCK" | grep -E "permissions:" -A 5 | grep -q "write"; then
  echo "Error: Write permissions are forbidden in frontend job." >&2
  exit 1
fi

# 5. Check global corepack/package manager, corepack enable, action-setup, bare pnpm
if echo "$FRONTEND_JOB_BLOCK" | grep -q "npm install -g corepack"; then
  echo "Error: 'npm install -g corepack' is forbidden." >&2
  exit 1
fi

if echo "$FRONTEND_JOB_BLOCK" | grep -q "corepack enable"; then
  echo "Error: 'corepack enable' is forbidden." >&2
  exit 1
fi

if echo "$FRONTEND_JOB_BLOCK" | grep -q "action-setup" || echo "$FRONTEND_JOB_BLOCK" | grep -q "pnpm-setup"; then
  echo "Error: pnpm/action-setup is forbidden." >&2
  exit 1
fi

# Bare pnpm check (pnpm not preceded by corepack)
if echo "$FRONTEND_JOB_BLOCK" | grep -E "\bpnpm\b" | grep -v "corepack pnpm"; then
  echo "Error: Bare pnpm call is forbidden. Use 'corepack pnpm'." >&2
  exit 1
fi

# 6. Check for missing exact assertions, frozen install, with-deps chromium, full corepack pnpm run verify
if ! echo "$FRONTEND_JOB_BLOCK" | grep -q 'G+ui7ZUxTzgwRc45pi7OhOybKFnGpxVDp0khf+eFdw/gcQmZfme4nUh4Z4URY9YPoaZYP86zNZmqV/T2Bf5/rA=='; then
  echo "Error: Missing exact Corepack integrity hash assertion." >&2
  exit 1
fi

if ! echo "$FRONTEND_JOB_BLOCK" | grep -q 'actual_node=$(node --version)'; then
  echo "Error: Missing node version assertion." >&2
  exit 1
fi

if ! echo "$FRONTEND_JOB_BLOCK" | grep -q 'actual_corepack=$(corepack --version)'; then
  echo "Error: Missing corepack version assertion." >&2
  exit 1
fi

if ! echo "$FRONTEND_JOB_BLOCK" | grep -q 'actual_pm=$(corepack pnpm --version)'; then
  echo "Error: Missing pnpm version assertion." >&2
  exit 1
fi

if ! echo "$FRONTEND_JOB_BLOCK" | grep -q "corepack pnpm install --frozen-lockfile"; then
  echo "Error: Missing 'corepack pnpm install --frozen-lockfile'." >&2
  exit 1
fi

if ! echo "$FRONTEND_JOB_BLOCK" | grep -q "corepack pnpm exec playwright install --with-deps chromium"; then
  echo "Error: Missing Playwright chromium installation with dependencies." >&2
  exit 1
fi

if ! echo "$FRONTEND_JOB_BLOCK" | grep -q "corepack pnpm run verify"; then
  echo "Error: Missing 'corepack pnpm run verify'." >&2
  exit 1
fi

# 7. Test verify-ci-paths.sh logic with path fixtures (docs/frontend)
is_skip_paths() {
  local input="$1"
  local result
  result=$(printf "%b" "$input" | .github/scripts/verify-ci-paths.sh)
  if [ "$result" != "skip" ]; then
    echo "Error: Expected skip for path: $input, got $result" >&2
    exit 1
  fi
}

is_run_paths() {
  local input="$1"
  local result
  result=$(printf "%b" "$input" | .github/scripts/verify-ci-paths.sh)
  if [ "$result" != "relevant" ]; then
    echo "Error: Expected relevant for path: $input, got $result" >&2
    exit 1
  fi
}

is_run_paths "frontend/package.json\0"
is_run_paths "frontend/app/page.tsx\0pom.xml\0"
is_run_paths ".github/workflows/ci.yml\0"
is_run_paths ".github/scripts/verify-ci-paths.sh\0"

is_skip_paths "src/main/java/ru/andrew/website/AndrewWebsiteApplication.java\0"
is_skip_paths "pom.xml\0"
is_skip_paths "docs/SPEC.md\0README.md\0"
is_skip_paths "src/test/java/ru/andrew/website/web/ProblemResponseTest.java\0CHANGELOG.md\0"
is_skip_paths "frontend-tasks/F1A-frontend-ci-gates.md\0"

echo "All CI Policy Compliance Tests Passed successfully!"
