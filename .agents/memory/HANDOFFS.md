# Handoff index

Navigation over the append-only [handoff directory](../../docs/handoffs/).
This index is not repository truth: re-verify any status live before acting.
Startup reads only `## Active chain`; closed chains are cold, found by tag.

## Active chain

| Task | Chain (chronological) | State | Topics |
| --- | --- | --- | --- |
| `task-dependency-security-cache` | `2026-07-20-091111-task-dependency-security-cache-v2-handoff` | in_progress | ci, security |


Note: chain files named before the UTC rule (LES-20260718-008) keep their
original names and may sort out of true order; this table's chronology is
authoritative.

## Closed chains

One line per merged task with committed handoffs: final handoff, outcome,
topics. Merged tasks that predate the handoff process have no chain here by
design.

| Task | Handoffs | Final | Outcome | Topics |
| --- | --- | --- | --- | --- |
| `task-backend-contract-plans` | 1 | `2026-07-17-backend-superpowers-handoff.md` | merged; Superpowers backend plans committed | backend, process |
| `task-backend-skeleton` | 2 | `2026-07-18-094835-backend-skeleton-ready-handoff.md` | merged | backend |
| `fix-preserve-merged-branches` | 1 | `2026-07-18-101804-branch-retention-handoff.md` | merged; retention policy adopted (DEC-20260718-005) | process, incident |
| `task-agents-memory` | 1 | `2026-07-18-104909-task-agents-memory-handoff.md` | merged; shared memory established (DEC-20260718-001..004) | memory |
| `fix-backend-skeleton-review` | 2 | `2026-07-18-131926-backend-skeleton-review-ready-handoff.md` | merged | backend, process |
| `task-ci-backend-gates` | 2 | `2026-07-18-110204-ci-backend-gates-ready-handoff.md` | merged as `c703ae7`; retained source branch | ci, security, incident |
| `task-context-refactor` | 2 | `2026-07-19-182536-task-context-refactor-handoff.md` | merged as `66e2afa`; retained source branch | memory, process, tracker |
| `fix-tracker-canonical-links` | 2 | `2026-07-19-193125-fix-tracker-canonical-links-ready-handoff.md` | merged as `37d02cf`; retained source branch | process, tracker |
| `fix-ci-timeouts-annotations` | 2 | `2026-07-19-195143-fix-ci-timeouts-annotations-ready-handoff.md` | merged as `326c9f4`; retained source branch | ci, process |
| `task-product-technical-brief` | 2 | `2026-07-19-202411-task-product-technical-brief-ready-handoff.md` | merged as `2be8a2c`; retained source branch | product, process |
| `task-frontend-track-planning` | 2 | `2026-07-19-205058-task-frontend-track-planning-ready-handoff.md` | merged as `8940abe`; retained source branch | frontend, tracker |

## Rules

- New filenames use UTC `HHMMSS` (LES-20260718-008) so lexicographic order
  matches chronology; the chronology columns stay authoritative regardless.
- A successor link makes its predecessor historical and immutable.
- On task merge, the controller collapses the chain into `## Closed chains`
  in the same branch. Handoff files themselves are never deleted or moved.
