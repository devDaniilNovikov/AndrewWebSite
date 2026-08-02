#!/bin/bash
set -euo pipefail

# This script reads NUL-separated file paths from standard input
# and determines if there is any frontend-relevant file.
# It prints "relevant" if there is, or "skip" if not.

is_frontend_relevant() {
  local file="$1"
  # Frontend files
  if [[ "$file" =~ ^frontend/ ]]; then
    return 0
  fi
  # OpenAPI file
  if [[ "$file" == "docs/backend/openapi.yaml" ]]; then
    return 0
  fi
  # CI workflow itself
  if [[ "$file" == ".github/workflows/ci.yml" ]]; then
    return 0
  fi
  # Path/policy scripts (e.g., this script or any under .github/scripts)
  if [[ "$file" =~ ^\.github/scripts/ ]]; then
    return 0
  fi
  return 1
}

any_relevant=false

while IFS= read -r -d '' file || [ -n "$file" ]; do
  if [ -z "$file" ]; then
    continue
  fi
  if is_frontend_relevant "$file"; then
    any_relevant=true
  fi
done

if [ "$any_relevant" = true ]; then
  echo "relevant"
else
  echo "skip"
fi
