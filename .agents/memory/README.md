# Shared agent memory

File-first shared memory for Codex, Claude Code, and Jules. It records
durable context and evidence. It is navigation and evidence — never a
substitute for live Git/GitHub state, `TASKS.md`, canonical contracts,
process rules, or the assigned plan. Priority on conflict follows the
context stack defined in the role files; an explicit current user decision
outranks any older text.

## Hot layer — what a task start actually reads

Startup reading is a constant-size budget, not the whole directory:

1. This section of this README.
2. The `## Active` section of [`DECISIONS.md`](DECISIONS.md),
   [`LESSONS.md`](LESSONS.md), and [`OPEN_QUESTIONS.md`](OPEN_QUESTIONS.md)
   — signature lines only.
3. The `## Active chain` of [`HANDOFFS.md`](HANDOFFS.md) and the current
   handoff it points to.

**Budget: the hot layer stays under 80 lines total.** If an addition would
exceed it, compact first (close questions, supersede stale records, collapse
a merged chain) — then add. Full record bodies, closed chains, and archived
material are cold: read them only when a task touches their topic, located
by `grep` on tags, IDs, or `[active]` — not by reading files top to bottom.

## Record signature — one line that carries the decision

Every record's body starts with, and its `## Active` entry consists of, a
single signature line, at most 120 characters:

```text
DEC-YYYYMMDD-NNN [status] scope-tag: one normative sentence → evidence
```

Example:
`LES-20260718-005 [active] ci: diff agents' later pushes against the last reviewed tree → handoff 110204`

An agent must be able to act on the signature alone; the body exists for
provenance and edge cases. `grep '\[active\]' .agents/memory/*.md` returns
the entire live semantic memory.

## Topic vocabulary — closed set

`backend, frontend, product, ci, security, tracker, memory, deploy, process,
incident`. Used in signatures and the handoff index. Extending the
vocabulary is a decision (add a DEC record); free-form tags are rejected in
review.

## Taxonomy

| Kind | Location | Purpose |
| --- | --- | --- |
| Semantic | `DECISIONS.md`, `LESSONS.md`, `OPEN_QUESTIONS.md` | Settled choices, distilled experience, pending decisions |
| Episodic | [`../../docs/handoffs/`](../../docs/handoffs/) + `HANDOFFS.md` | Append-only snapshots and evidence |
| Procedural | Role files, `../workflows/` | Canonical process — link, never duplicate |

## Write rules

- Exactly one active controller per task branch owns memory writes; everyone
  else is read-only until ownership is explicitly yielded.
- Supersede, never delete. Status vocabulary: `active`, `deferred`,
  `resolved`, `superseded`.
- Never store secrets, credentials, PII, raw issue text, or transcripts —
  link canonical records and summarize the minimum non-sensitive evidence.
- **Tripwire → question:** every "stopped and asked the user" event creates a
  Q record before or with the ask; the answer resolves it with a link to the
  resulting decision or handoff. Before asking the user anything, `grep`
  OPEN_QUESTIONS — it may already be answered.
- **Distillation duty:** closing a task that produced an incident, rollback,
  rejected approach, or external-service surprise adds a LESSONS record in
  the same branch. Policy restatements are not lessons; a lesson requires a
  real incident with linked evidence.
- **Review-by:** each record names the condition that forces its re-review
  (e.g. `Review-by: any branch-protection change`). A task touching a
  record's topic with a triggered condition must re-verify it. Every fifth
  merged task, the controller sweeps all `[active]` signatures against live
  state and compacts.

## Handoff lifecycle

Filenames `YYYY-MM-DD-HHMMSS-<task>-handoff.md` in
[`../../docs/handoffs/`](../../docs/handoffs/), `HHMMSS` in **UTC** so
filename order equals chronology. Created, with an index update, in the same
task branch on every pause, transfer, or completion. The current handoff may
receive focused corrections only until a successor links to it; then it is
historical and immutable. Handoffs are never deleted or archived. When a
task's PR merges, its chain collapses to one line in `## Closed chains`.

Handoff body template — staleness is confined to the timestamped section:

```markdown
# <task> handoff
Signature: HND <task> [state] topics: <tags> → predecessor: <file|none>

## Durable — safe to cite later
Decisions made, evidence SHAs, rejected approaches, lesson candidates.

## Snapshot at YYYY-MM-DDTHH:MMZ — re-verify live before use
PR/branch/check statuses, tracker rows, environment facts.

## Next steps — conditional, each requires the stated live check
```

## Archive

[`archive/`](archive/) holds superseded non-handoff records, moved only
after a reviewed replacement links their ID. Never read at startup.
