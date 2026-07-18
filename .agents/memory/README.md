# Shared agent memory

This directory is the file-first shared memory surface for Codex, Claude Code,
and Jules. It records durable context and evidence; it is a navigation aid, not
a replacement for live state, canonical specifications, process rules, or an
assigned task plan.

## Startup and context routing

An agent must assemble context from all of the following before changing work.
The order is intentional, but no source alone is sufficient:

1. Inspect live Git and GitHub state: `origin/main`, the task branch, open pull
   requests, checks, and prerequisite status.
2. Read `TASKS.md` as the living task queue and status tracker, then reconcile
   it against that live state when the agent owns the task. `TASKS.md` is not
   long-term memory and is not copied into this directory.
3. Read the shared development process and Git Flow in
   [`../workflows/GIT_FLOW.md`](../workflows/GIT_FLOW.md).
4. Read role and ownership rules in [`../AGENTS.md`](../AGENTS.md),
   [`../CLAUDE.md`](../CLAUDE.md), and the Jules guide when applicable.
5. Read the product architecture, API, operations, and task-specific canonical
   documents that govern the assigned scope.
6. Read this file, [`HANDOFFS.md`](HANDOFFS.md), the current handoff, and any
   directly relevant decision, lesson, or question.
7. Read the executable plan assigned to this task. A plan applies only to its
   own task and never silently authorizes another task.

Shared memory provides accumulated context and evidence after those sources are
located. It must never be treated as a copy of the task queue, a substitute for
live Git/GitHub, or a way to bypass ownership and review.

## Memory taxonomy

| Kind | Location | Purpose | Default use |
| --- | --- | --- | --- |
| Semantic | `DECISIONS.md`, `OPEN_QUESTIONS.md` | Settled, reviewed choices and unresolved decision prompts | Read when a task touches the recorded scope |
| Episodic | [`../../docs/handoffs/`](../../docs/handoffs/) and `HANDOFFS.md` | Append-only snapshots, evidence, and transfer context | Read the indexed current handoff, then relevant predecessors |
| Procedural | `../AGENTS.md`, `../CLAUDE.md`, `../workflows/`, [`../../.github/JULES_AUTOMATION.md`](../../.github/JULES_AUTOMATION.md) | Canonical process, roles, and operating rules | Link to and follow; do not duplicate here |

## Authority map

| Subject | Canonical authority | Memory's role |
| --- | --- | --- |
| Current user decision | The explicit, current user decision | Record evidence and supersession; never override it with older text |
| Mutable repository, PR, check, and branch state | Live Git and GitHub, then reconciled `TASKS.md` | Snapshot only; always recheck live |
| Living task order and status | `TASKS.md`, reconciled with live Git/GitHub | Navigation only; do not duplicate its queue here |
| Product architecture and backend behavior | [`../../docs/backend/architecture.md`](../../docs/backend/architecture.md) | Context and links only |
| HTTP contract | [`../../docs/backend/openapi.yaml`](../../docs/backend/openapi.yaml) | Context and links only |
| Operations | [`../../docs/backend/operations.md`](../../docs/backend/operations.md) | Context and links only |
| Process, ownership, and automation | `../AGENTS.md`, `../CLAUDE.md`, `../workflows/GIT_FLOW.md`, and [`../../.github/JULES_AUTOMATION.md`](../../.github/JULES_AUTOMATION.md) | Explain provenance; do not replace rules |
| Assigned executable work | The plan explicitly assigned to the current task | Evidence and history only; no cross-task authorization |

If sources conflict, preserve the conflict. Add an entry with status,
supersession, evidence, and review metadata; do not silently rewrite history.
An explicit current user decision outranks older planning text. A mutable fact
must be verified live before it can change a task decision.

## Controller and write rules

- Exactly one active controller owns a task branch and its memory write at a
  time. Other agents are read-only until ownership is explicitly yielded.
- The controller records only verified, task-relevant facts. It does not copy
  raw tool output, raw issue text, secrets, credentials, or lead PII.
- A memory record has a stable ID and a status. Use `active`, `deferred`,
  `superseded`, or `resolved` as applicable; never delete a conflict merely
  because it was resolved.
- A task may update only its declared memory scope. Cross-owner changes require
  the normal ownership decision and Codex review where required.

## Security

Memory stores concise, verified context only. Never add secrets, credentials,
private keys, lead PII, raw issue text, raw chat transcripts, or raw tool
output. Refer to canonical records by link and summarize only the minimum
non-sensitive evidence needed to explain a decision or task boundary.

## Handoff lifecycle

Handoffs use `YYYY-MM-DD-HHMMSS-<task>-handoff.md` in
[`../../docs/handoffs/`](../../docs/handoffs/). A pause, transfer, or completion
creates a uniquely named committed handoff and updates `HANDOFFS.md` in the same
task branch.

The current handoff may receive focused corrections only until a successor
links to it. Once a successor link exists, it is historical and immutable.
Handoffs are never deleted or moved to the archive. Every handoff must name its
predecessor when one exists, identify the snapshot evidence, state scope and
boundaries, and give conditional next steps that require a live check rather
than predicting future state.

## Record schema and archive policy

Decision, lesson, and question records use a stable `ID` and fields for
`status`, `date`, `scope`, statement, `evidence`, `canonical source`,
`supersedes`, and `review` as relevant. Handoff filenames are their episodic
IDs; `HANDOFFS.md` records their chronological status and predecessor.

[`archive/`](archive/) contains only superseded non-handoff records. It is not
read by default. A record moves there only after its replacement and review are
linked. Handoffs never enter the archive and are never removed.

## `codex-mem` status

`codex-mem` is **deferred**, **inactive**, and **advisory**. It is not a startup
dependency, source of truth, or production integration. Any future pilot needs
an explicit user decision, a scoped task, and a reviewed migration rule.
