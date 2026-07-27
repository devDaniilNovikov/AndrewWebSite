# Handoff index

Navigation over the append-only [handoff directory](../../docs/handoffs/).
This index is not repository truth: re-verify any status live before acting.
Startup reads only `## Active chain`; closed chains are cold, found by tag.

## Active chain

| Task | Chain (chronological) | State | Topics |
| --- | --- | --- | --- |
| `fix-sec-04-send-retention-race` | `2026-07-27-053724-fix-sec-04-send-retention-race-handoff.md` | fresh branch from merged SEC-03; deterministic RED race next | backend, privacy, telegram, security, tracker |

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
| `task-dependency-security-github-native` | 5 | `2026-07-21-091553-task-dependency-security-github-native-handoff.md` | merged as `3ce978c`; retained source branch | ci, security, tracker |
| `task-backend-deploy-stub` | 3 | `2026-07-21-094937-task-backend-deploy-stub-handoff.md` | merged as `459d493`; retained source branch | backend, deploy, tracker |
| `task-backend-http-security` | 4 | `2026-07-21-121057-task-backend-http-security-handoff.md` | merged as `806b39d`; retained source branch | backend, security |
| `task-db-flyway-baseline` | 4 | `2026-07-24-154752-task-db-flyway-baseline-handoff.md` | merged as `ec56412`; retained source branch | backend, deploy, security |
| `fix-db-identity-generation-contract` | 2 | `2026-07-24-164212-fix-db-identity-generation-contract-handoff.md` | merged as `ac7d21a`; retained source branch | backend, tracker |
| `task-leads-api` | 2 | `2026-07-24-180342-task-leads-api-handoff.md` | merged as `731a17d`; retained source branch | backend, security, tracker |
| `fix-leads-api-json-boundary-replacement` | 3 | `2026-07-25-045958-fix-leads-api-json-boundary-replacement-ready-handoff.md` | merged as `2f565dd`; retained source branch | backend, security, tracker |
| `fix-leads-unicode-boundary` | 3 | `2026-07-25-074000-fix-leads-unicode-boundary-ready-handoff.md` | merged as `4da6448`; retained source branch | backend, security, tracker |
| `fix-production-http-invariants` | 5 | `2026-07-25-180844-fix-production-http-invariants-merged-handoff.md` | merged as `d29e788`; retained source branch | backend, security, deploy, tracker |
| `fix-code-review-hardening` | 1 | `2026-07-25-205559-fix-code-review-hardening-handoff.md` | merged as `0b0a62a`; retained source branch; corrective rollback merged as `848e94f`; corrected replacement merged as `b822770` | backend, deploy, tracker, process, incident |
| `task-telegram-client` | 8 | `2026-07-26-060315-task-telegram-client-merged-handoff.md` | merged as `3f35c1d`; retained source branch | backend, security, tracker |
| `task-telegram-worker` | 5 | `2026-07-26-113056-task-telegram-worker-review-fix-handoff.md` | merged as `857240f`; retained source branch; post-merge CI, Dependency Submission, and Semgrep green | backend, security, tracker |
| `fix-revert-code-review-hardening` | 5 | `2026-07-26-072227-fix-revert-code-review-hardening-merged-handoff.md` | merged as `848e94f`; retained source branch; corrected replacement merged as `b822770` | backend, deploy, tracker, process, incident |
| `fix-code-review-hardening-replacement` | 6 | `2026-07-26-082717-fix-code-review-hardening-replacement-final-closure-handoff.md` | merged as `b822770`; reconciled by PR #50 as `f515c59`; retained source branches | backend, deploy, tracker, process, incident |
| `task-antigravity-frontend-role` | 5 | `2026-07-26-104346-task-antigravity-frontend-role-ready-handoff.md` | merged as `ba2ad48`; retained source branch; post-merge CI, Dependency Submission, and Semgrep green | frontend, tracker, memory, process |
| `task-lead-retention` | 4 | `2026-07-26-133209-task-lead-retention-merged-handoff.md` | merged as `ceefd7a`; retained source branch; post-merge Repository policy, verify, Java security, Dependency Submission, and Semgrep green | backend, privacy, security, tracker |
| `task-backend-observability` | 5 | `2026-07-26-173224-task-backend-observability-merged-handoff.md` | merged as `27e6bb4`; retained source branch; post-merge CI, Dependency Submission, and Semgrep green | backend, security, telemetry, tracker |
| `fix-http-security-framework-native-deny` | 5 | `2026-07-26-183503-fix-http-security-framework-native-deny-ready-handoff.md` | merged as `9d33ff6`; retained source branch; post-merge CI, Dependency Submission, and Semgrep green | backend, security, tracker |
| `fix-sec-03-pii-anonymization-uuidv4` | 3 | `2026-07-27-052649-fix-sec-03-pii-anonymization-uuidv4-ready-handoff.md` | merged as `0419337`; retained source branch; post-merge CI, Dependency Submission, and Semgrep green | backend, privacy, security, tracker |

## Rules

- New filenames use UTC `HHMMSS` (LES-20260718-008) so lexicographic order
  matches chronology; the chronology columns stay authoritative regardless.
- A successor link makes its predecessor historical and immutable.
- On task merge, the controller collapses the chain into `## Closed chains`
  in the same branch. Handoff files themselves are never deleted or moved.
