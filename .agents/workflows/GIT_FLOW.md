# AndrewWebSite — Git Flow

## Quick start — minimum viable context

- Trunk-based: `main` only, everything lands through a PR, no direct pushes,
  no stacked branches, no long-lived side branches.
- One approved task = one branch = one dedicated external worktree = one PR.
- Branch names: `task-<kebab>` or `fix-<kebab>` — CI rejects anything else.
- Draft → Ready when CI is green and Codex review is resolved. **Ready never
  grants merge authorization** — only an explicit user command does.
- Squash merge; PR title in Conventional Commits becomes the final subject.

## Work unit and ownership

- Declare the owning agent and owned paths before editing.
- Parallel tasks use separate worktrees and must not touch overlapping paths;
  overlap is an ownership decision, not a race.
- A task depending on an unmerged PR waits for `main`; never build on top of
  an open branch.
- Never reuse another agent's branch or worktree.

## Task lifecycle

1. Fetch `origin`; verify prerequisite state live; read memory routing, the
   current handoff, and `TASKS.md`. Live Git/GitHub plus the reconciled
   tracker are authoritative for mutable state.
2. Create the worktree and branch from fresh `origin/main`.
3. Record scope, owned paths, dependencies, and a plan of at most three
   steps.
4. Implement the smallest complete change; RED → GREEN → REFACTOR for every
   testable behavior.
5. Run focused checks, then every applicable quality and security gate.
6. Review the full diff for secrets, unrelated edits, and missing tests.
7. Atomic Conventional Commits with the agent's attribution footer.
8. Push; open a **Draft** PR: what changed, why, checks actually run, risks,
   linked issues.
9. Resolve CI and review findings only within declared scope.
10. Mark **Ready** only with green required checks and resolved Codex review.
11. Merge only after explicit user authorization; squash only.
12. Confirm `main`; close the linked issue; apply the retention policy;
    remove the worktree only when clean; `git fetch --prune`.

## Pull request contract

- Titles: `feat|fix|docs|test|refactor|perf|chore|ci(scope): description`.
- Required checks: `Repository policy`, `verify`, and `dependency-security`.
  Renaming a required job updates branch protection in the same change —
  never leave an obsolete required context behind.
- Automation (Jules or any bot) may open PRs but never merges; auto-merge
  stays disabled repository-wide.
- Report only checks that actually ran; stale evidence is no evidence.

## Protected `main`

Require: PR before merge, the configured checks, branch up to date,
conversation resolution, linear history, squash-only. Enforce for
administrators; block force pushes and deletion.

**Branch retention after merge:** completed task and fix branches stay as
read-only history; removing a worktree never authorizes branch deletion.
Verify that GitHub's "Automatically delete head branches" setting stays
disabled — a platform setting can silently violate a documented policy.
Record the decision in `memory/DECISIONS.md`.

## Handoffs

A pause, transfer, or completion writes `docs/handoffs/
YYYY-MM-DD-HHMMSS-<task>-handoff.md` with `HHMMSS` in **UTC** (so filename
order equals chronology) and updates
[`memory/HANDOFFS.md`](../memory/HANDOFFS.md) in the same branch. A handoff
records verified scope and evidence, links its predecessor, and gives
conditional next steps requiring a live check. Once a successor links to it,
it is historical and immutable. Handoffs are never deleted or archived.

## Hotfix and rollback

Hotfixes start from current `origin/main` on `fix-*` under the same gates.
Revert a bad merge with a new `fix-revert-*` PR via `git revert`. Never
rewrite published history or bypass protection for an emergency. Production
rollback requires explicit user authorization.

## Exceptions

Any exception requires an explicit user decision and a synchronized update to
this document in the same change. Security, secret scanning, input
validation, and review requirements cannot be waived — by anyone, for any
reason.
