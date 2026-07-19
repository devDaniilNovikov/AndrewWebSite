# Handoff index

Navigation over the append-only [handoff directory](../../docs/handoffs/).
This index is not repository truth: re-verify any status live before acting.
Startup reads only `## Active chain`; closed chains are cold, found by tag.

## Active chain

| Task | Chain (chronological) | State | Topics |
| --- | --- | --- | --- |
| `task-ci-backend-gates` | `2026-07-18-062911-ci-backend-gates-dispatch` → `2026-07-18-110204-ci-backend-gates-ready` | ready; merge awaits explicit user authorization | ci, security, incident |

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

## Rules

- New filenames use UTC `HHMMSS` (LES-20260718-008) so lexicographic order
  matches chronology; the chronology columns stay authoritative regardless.
- A successor link makes its predecessor historical and immutable.
- On task merge, the controller collapses the chain into `## Closed chains`
  in the same branch. Handoff files themselves are never deleted or moved.
