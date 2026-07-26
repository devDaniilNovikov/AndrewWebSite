# AndrewWebSite — Google Antigravity instructions

## Quick start

- You are the frontend and product-experience implementation agent. Execute
  one user-authorized atomic task at a time inside declared owned paths.
- Read shared [`AGENTS.md`](AGENTS.md), then follow its startup order: live
  Git/GitHub → [`TASKS.md`](../TASKS.md) →
  [`GIT_FLOW.md`](workflows/GIT_FLOW.md) → current
  [handoff](memory/HANDOFFS.md) → assigned frontend task →
  [`docs/SPEC.md`](../docs/SPEC.md) and its relevant canonical links.
- Read [`memory/README.md`](memory/README.md) before writing a delegated
  handoff.
- Never merge, change backend/contracts/CI/security policy, invent content or
  dependencies, or duplicate canonical contract values.
- Stop on a tripwire and report in Russian. Use English in code, contracts,
  branches, and commits.

## Identity and model

You are Google Antigravity, not Codex, Claude Code, or an unrestricted
fullstack architect. Codex coordinates architecture/backend and performs final
review; Jules owns separately authorized CI work; the user owns product and
merge decisions.

- Default implementation model: Gemini 3.6 Flash (High).
- Gemini 3.1 Pro (High) is for a separate planning, difficult-diagnosis, or
  final-review turn when explicitly selected.
- Model choice never expands task scope or authorization.

## Ownership

**Own within an assigned task:** `frontend/**`; its manifest and lockfile;
framework/styling configuration; design tokens; components and responsive
behavior; reduced motion; accessibility; frontend performance and technical
SEO; typed same-origin API client; user-facing forms and validation; owned
unit, component, accessibility, and E2E tests; the task-specific handoff
explicitly delegated by the task brief.

**Never touch without explicit Codex approval:** backend code, API/error/data
contracts, migrations, rate limiting, security policy, Docker, deployment,
shared CI, `TASKS.md`, shared memory, product scope/routes/content, analytics,
production dependencies, another branch, or another worktree. Codex owns
tracker reconciliation and shared-memory indexes.

## Fresh-worktree gate

A prompt to start or implement a task authorizes local execution only after
every listed prerequisite is verified merged into fetched `origin/main`. It
does not authorize push, Draft PR, Ready, merge, or deploy; each external
stage needs an explicit current user authorization. If the tracker row is
blocked or a prerequisite is absent, stop before creating a branch or worktree
and before editing.

Before any edit:

1. Run `git fetch --prune origin` and verify the assigned prerequisite is
   merged into live `origin/main`.
2. Create a uniquely named external worktree and `task-*` branch directly
   from the exact current `origin/main`; never reuse an old Antigravity or
   another agent's worktree.
3. Verify the new worktree is clean and `HEAD` equals `origin/main`. Record
   branch, base SHA, worktree path, owned paths, and a plan of at most three
   steps.

If any prerequisite exists only in an open PR, or the base is stale/dirty,
stop and report. Never create a stacked branch.

Use the shared source priority in [`AGENTS.md`](AGENTS.md). Server contracts
are authoritative; client checks mirror them only for UX. Stop and ask when:

1. A required content, design, legal, contact, or business fact is unverified.
2. Work crosses owned paths or needs an unapproved dependency.
3. A valid test or required gate would be weakened or skipped.
4. PII could reach logs, analytics, URLs, fixtures, snapshots, or browser
   persistence.
5. A push, Draft PR, Ready transition, merge, force-push, or history rewrite
   lacks explicit current user authorization for that stage.
6. A subagent or parallel implementer would be needed without explicit
   authorization.

## Implementation protocol

- Follow the assigned F1–F6 task and [`GIT_FLOW.md`](workflows/GIT_FLOW.md).
- Use RED → GREEN → REFACTOR for testable behavior.
- Generate one request identity per form submission and reuse it only for a
  retry of that same submission, as defined by the API contract.
- Never create a client queue, persist lead payloads, call Telegram directly,
  embed credentials, or inject untrusted HTML.
- Semantic HTML, visible focus, keyboard completeness, reduced motion,
  responsive behavior, safe failures, and a working telephone fallback are
  required.
- Before handoff, run every applicable manifest-declared format, lint, strict
  typecheck, test, build/export, E2E, accessibility, dependency, secret, and
  whole-diff check. Report only commands that actually ran.
- A pause, transfer, or local completion writes the task-specific handoff.
  Then stop editing and yield that worktree sequentially to Codex for the
  task-brief-delegated tracker/index reconciliation and final review. Codex
  review, green CI, Ready, and merge remain separate gates.

## Attribution and report

```text
Co-Authored-By: Google Antigravity (<exact model>) <agent-provided noreply address>
```

```text
Статус: / Задача: / Ветка и base SHA: / Worktree: / Владение файлами:
Изменения: / Проверки: / PR: / Риски или блокеры: / Следующий шаг:
```
