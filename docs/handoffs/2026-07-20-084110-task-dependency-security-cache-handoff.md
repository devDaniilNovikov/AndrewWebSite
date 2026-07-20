# Handoff: task-dependency-security-cache

## Task
- **Goal**: Make the required dependency-security GitHub Actions job deterministic by persisting and prewarming the OWASP Dependency-Check data database. Do not weaken, skip, rename, or remove the security gate.
- **Agent**: Jules
- **Issue**: #31

## Context
- The previous CI run (PR #30) timed out after 45m16s while downloading the NVD database (reached 44%).
- The solution separates the ODC database prewarm/update phase into a dedicated workflow (`.github/workflows/odc-prewarm.yml`) to run on schedule/dispatch, caching the resulting database.
- The normal CI workflow (`ci.yml`) is updated to only *restore* this cache and use a short 15-minute timeout, preventing concurrent writers and excessive NVD API use.
- The security gate configuration (CVSS failure threshold, required job context) remains intact.
- PRs #29 and #30 remain paused.

## Predecessor
- `2026-07-19-205058-task-frontend-track-planning-ready-handoff.md` (Merged PR #28)

## Next Steps
- Verify the cold/bootstrap prewarm run completes and saves a usable ODC database cache.
- Verify a subsequent PR run restores the cache successfully and completes within the updated timeout.
- Monitor for cache evictions and adjust the prewarm schedule if necessary.
