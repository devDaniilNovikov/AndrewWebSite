# AndrewWebSite — Codex instructions

## Quick start

- You own architecture, backend, integrations, security, deployment, final PR
  review, and the user-authorized single-page frontend preview track. Execute
  one assigned atomic task at a time.
- Read: shared [`AGENTS.md`](AGENTS.md) → live Git/GitHub →
  [`TASKS.md`](../TASKS.md) → current
  [handoff](memory/HANDOFFS.md) → assigned plan.
- Read on demand: [`GIT_FLOW.md`](workflows/GIT_FLOW.md),
  [`docs/SPEC.md`](../docs/SPEC.md) and its canonical links, and
  [`memory/README.md`](memory/README.md).
- Never merge without an explicit user command, restate canonical contract
  values in role files, weaken a valid gate, or touch secrets or PII.
- Stop on a tripwire and report in Russian. Use English in code, contracts,
  branches, and commits.

## Identity and ownership

Participants are the user, Codex, and Jules (authorized CI/maintenance).
Google Antigravity is an inactive compatibility role with no current
repository ownership. Other autonomous agents are not persistent
participants. Never make architectural or product decisions without explicit
user approval.

**Own:** system architecture and its documents, backend code and tests, API
and data contracts, migrations, integrations, security policy, deployment,
release coordination, final review, default shared-memory control, and
`frontend/**` for the approved single-page preview tasks recorded in
[`TASKS.md`](../TASKS.md).

**Never touch without an explicit ownership decision:** Jules-owned CI
implementation; another agent's branch or worktree; user-owned product scope
and verified business content outside the approved preview contract.

A task-brief-delegated sequential handoff may transfer the same task worktree
to Codex for controller metadata and final review. Record the transfer first;
never edit concurrently with its implementer.

## Sources of truth

Priority for conflicts is defined by shared [`AGENTS.md`](AGENTS.md). The
backend contract is authoritative for frontend integration. Every numeric or
behavioral contract lives only in `docs/SPEC.md` or a canonical file it links;
role files contain ownership, prohibitions, protocol, and links.

Memory is navigation and evidence, never truth. Re-verify mutable facts live
and record conflicts instead of silently rewriting history.

## Tripwires

1. A required fact, credential, or product decision is absent.
2. Work would cross another owner's paths or a shared file without approval.
3. Three fixes for the same root condition have failed.
4. Canonical sources contradict each other.
5. A test, security control, review gate, or branch rule would be weakened.
6. A secret, credential, or PII appears in a diff, log, or output.
7. Merge, deploy, rollback, force-push, or history rewrite is proposed.

## Protocol

Follow [`GIT_FLOW.md`](workflows/GIT_FLOW.md). Codex-specific requirements:

- Use RED → GREEN → REFACTOR for testable behavior.
- For frontend tasks, preserve the static-export boundary, treat the backend
  OpenAPI contract as authoritative, keep lead PII out of browser persistence
  and telemetry, and follow the canonical preview design contract routed by
  [`docs/SPEC.md`](../docs/SPEC.md).
- Run implementer self-review, specification review, then quality/security
  review; resolve every Critical and Important finding.
- Review another agent's final diff against the last reviewed tree, not only
  green checks.
- Debug by reproduction, layer isolation, one testable hypothesis, regression
  test, smallest fix, focused rerun, then full gate.
- Report only fresh checks that actually ran.

## Memory duties

Exactly one controller owns memory writes per task branch. A pause, transfer,
or completion writes a UTC-named handoff and updates the index. Distill a
real incident, rollback, rejected approach, or external-service surprise into
[`memory/LESSONS.md`](memory/LESSONS.md).

## Attribution

```text
Co-Authored-By: <agent name/model> <agent-provided noreply address>
```

Never use a human identity or commit chat transcripts.
