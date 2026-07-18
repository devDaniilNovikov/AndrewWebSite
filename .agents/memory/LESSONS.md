# Shared-memory lessons

## LES-20260718-001 — One controller writes task memory

- **Status:** active
- **Date:** 2026-07-18
- **Lesson:** One active controller owns the task branch and its memory write; non-owners stay read-only until ownership is yielded.
- **Evidence:** User-confirmed ownership rule for shared memory.
- **Applicability:** Every task with a branch, worktree, handoff, or memory update.
- **Invalidation:** Replace only through an explicit user-approved concurrency model and a reviewed superseding decision.

## LES-20260718-002 — Next steps must stay conditional

- **Status:** active
- **Date:** 2026-07-18
- **Lesson:** A handoff next step must not become self-referential or stale. It names a condition and requires a live check before action.
- **Evidence:** Existing handoffs distinguish snapshots from live branch and PR state.
- **Applicability:** Every pause, transfer, and completion handoff.
- **Invalidation:** None; revise only if the project adopts a verified durable workflow engine as a new canonical authority.

## LES-20260718-003 — Snapshot is not current truth

- **Status:** active
- **Date:** 2026-07-18
- **Lesson:** A handoff, decision, or index captures evidence at a time; it never replaces live Git/GitHub or reconciled `TASKS.md` for mutable state.
- **Evidence:** User-confirmed startup routing and live-authority rule.
- **Applicability:** Branches, PRs, checks, merge status, worktrees, and task queue status.
- **Invalidation:** None without an explicit authority-map change.

## LES-20260718-004 — Memory contains no sensitive transcripts

- **Status:** active
- **Date:** 2026-07-18
- **Lesson:** Do not record secrets, credentials, lead PII, raw issue text, raw transcripts, or raw tool output in memory or handoffs.
- **Evidence:** Repository security rules and shared-memory task boundary.
- **Applicability:** Every memory write, review note, and handoff.
- **Invalidation:** None; this is a security baseline.
