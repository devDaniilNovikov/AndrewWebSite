# Handoff: Dependency Security Cache (v2)

## Status
Task `task-dependency-security-cache` is `in_progress`.
The replacement branch `task-dependency-security-cache-v2` has been prepared with the required workflow modifications but has not been published yet.

## Changes
- Created `.github/workflows/odc-prewarm.yml` to prewarm the ODC cache on a daily schedule and workflow dispatch, restricting runs to `refs/heads/main` with `contents: read` permissions.
- Modified `.github/workflows/ci.yml`:
  - Added `dependency-security-prep` job to restore cache order strictly with a `COMPAT` stem, run `update-only`, and output exactly the generated cache key.
  - Updated `dependency-security` job to run unconditionally (`always()`) but require prep success. It restores ONLY the exact prepped cache key output from `dependency-security-prep` and runs the dependency check without updating the database (`autoUpdate=false`), preserving `NVD_API_KEY`.
- Concurrency group for ODC writing used literally in both cache-writing jobs without relying on variable expansion in contexts where it is not supported.

## Verification
- `actionlint` verified the workflows structure.
- `./mvnw -B verify` was attempted but did not complete because the Jules sandbox runtime could not satisfy the project Java 25 requirement; no Maven success is claimed.
- Allowed list of modified files reviewed and kept clean. No `README.md` pollution. No extra archives.

## Next Steps
- Publish the code for review.
- Open a Draft PR for Issue #31.
- Complete metadata update to change status from `in_progress` to `draft_pr` with the actual PR number.
- Record cache-hit evidence and cold/warm run logs in the PR.
