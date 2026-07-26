# AndrewWebSite — shared agent instructions

## Role routing

- The user owns product decisions, task authorization, merges, and production.
- Codex coordinates architecture, backend, integration, security, deployment,
  and final review. Codex must read [`CODEX.md`](CODEX.md).
- Google Antigravity owns assigned frontend tasks. Its native entry is
  [`../GEMINI.md`](../GEMINI.md), which routes to
  [`ANTIGRAVITY.md`](ANTIGRAVITY.md).
- Jules owns separately authorized CI and maintenance tasks; follow
  [`workflows/JULES_AUTOMATION.md`](workflows/JULES_AUTOMATION.md).
- [`CLAUDE.md`](CLAUDE.md) is a retired compatibility notice, not an active
  role.

## Startup

- Verify live Git/GitHub first, then reconcile [`../TASKS.md`](../TASKS.md).
- Read the assigned role file, current
  [handoff](memory/HANDOFFS.md), executable task plan, and relevant canonical
  contracts routed by [`../docs/SPEC.md`](../docs/SPEC.md).
- Follow [`workflows/GIT_FLOW.md`](workflows/GIT_FLOW.md): one authorized
  atomic task, one new external worktree, one branch from fresh `origin/main`,
  and one non-stacked PR.
- Do not reuse another task's branch or worktree.

## Sources and ownership

- Priority: explicit current user decision → live Git/GitHub → reconciled
  `TASKS.md` → Git Flow → role file → `docs/SPEC.md` links → memory → plan.
- Change only declared owned paths. Stop on ownership, contract, or canonical
  source conflicts.
- Historical handoffs are immutable; memory is evidence, never live truth.

## Package managers

- Backend: committed `./mvnw`; do not use a system Maven installation.
- Before F1, frontend tooling is undefined. During F1, its explicit selection
  delegation applies; afterward, manifest and lockfile are authoritative.

## Safety and quality

- Never expose secrets or lead PII; validate external input at boundaries.
- Never weaken tests, security controls, actor checks, or required gates.
- Push, PR publication, Ready transition, merge, deploy, rollback, and history
  rewrite each require an explicit current user authorization.
- Report only checks that actually ran. Russian with the user; English in
  code, branches, commits, and technical contracts.

## File-scoped commands

| Task | Command |
| --- | --- |
| One workflow | `actionlint .github/workflows/<file>.yml` |
| All workflows | `actionlint .github/workflows/*.yml` |

## Commit attribution

AI commits must include the acting agent's model/name and agent-provided noreply address; never use a human identity.
