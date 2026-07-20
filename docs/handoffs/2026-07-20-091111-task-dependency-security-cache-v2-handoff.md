# Handoff: task-dependency-security-cache

## Task
- **Goal**: Make the required dependency-security GitHub Actions job deterministic by persisting and prewarming the OWASP Dependency-Check data database. Do not weaken, skip, rename, or remove the security gate.
- **Agent**: Jules
- **Issue**: #31
- **Branch**: task-dependency-security-cache-v2

## Context
- The previous CI run (PR #30) timed out after 45m16s while downloading the NVD database (reached 44%).
- The solution separates the ODC database prewarm/update phase into a two-stage flow:
  1. A prep job restores the cache using a strict schema-isolated prefix, runs `update-only`, validates the H2 DB, and saves an exact PR-scoped key.
  2. The normal `dependency-security` job unconditionally restores that exact key and runs `check` with auto-update disabled.
- A dedicated scheduled workflow (`odc-prewarm.yml`) is serialized on `main` to periodically refresh the base cache.
- The security gate configuration (CVSS failure threshold, required job context) remains intact.

## Predecessor
- `2026-07-19-205058-task-frontend-track-planning-ready-handoff.md` (Merged PR #28)

## Next Steps
- Verify the cold/bootstrap prewarm run completes and saves a usable ODC database cache.
- Verify a subsequent PR run restores the cache successfully and completes within the updated 15-minute timeout.
