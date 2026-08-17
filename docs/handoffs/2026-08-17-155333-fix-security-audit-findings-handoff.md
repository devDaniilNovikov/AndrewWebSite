# fix-security-audit-findings implementation handoff

Signature: HND fix-security-audit-findings [in_progress] topics: backend, frontend, container, ci, security -> predecessor: none

## Durable — safe to cite later

- The user explicitly authorized one `fix-*` branch that remediates
  `SEC-RATE-01`, `FE-CSP-01`, `SC-IMG-01`, and `CI-AGENT-01`, publishes a pull
  request, runs CI/CD, and squash-merges only after every required gate is
  green.
- Codex owns the backend, frontend, container, security-policy, regression-test,
  tracker, and handoff paths for this task. The same explicit assignment grants
  task-scoped ownership of `.github/workflows/jules-issue.yml` and its tests;
  Jules retains its normal ownership outside this remediation.
- Production deployment, production requests, DAST against production,
  forwarded-header trust, Timeweb ingress mutation, secret access, PR #30,
  branch deletion, force push, admin bypass, and gate weakening remain outside
  scope.

## Verified start snapshot at 2026-08-17T15:53:33Z

- The dedicated external worktree and branch `fix-security-audit-findings` were
  created from exact fetched `origin/main`
  `df324aaf5149adb93ed1617b5d249a7740d631a3`, the same snapshot audited in the
  read-only predecessor session.
- Live GitHub showed PR #71 merged as `df324aa`, its post-merge CI, Dependency
  Submission, and Semgrep runs successful, the source branch retained, and zero
  deployments for that merge SHA. Draft PR #30 was the only open pull request.
- The original checkout was not modified; its user-owned untracked paths remain
  outside this worktree.

## Scope and three-step plan

Owned implementation paths are `src/main/**`, relevant `src/test/**`,
`frontend/**`, `Dockerfile`, `.github/workflows/jules-issue.yml`, focused
workflow/container regression scripts, and the minimum canonical
architecture/operations/process metadata needed to describe the controls.

1. Add failing regression tests that reproduce all four audit gaps.
2. Implement the smallest complete controls and make focused tests green.
3. Run all applicable backend, frontend, container, workflow, dependency,
   secret, static-analysis, and independent review gates; publish, mark Ready,
   and squash-merge only while the exact head remains fully green.

## Conditional continuation — re-verify live

Before publication, reconcile `origin/main`, record only checks that actually
ran, and create a successor handoff. Any contract conflict, missing immutable
image source, secret/PII signal, weakened gate, or third failed fix for the same
root condition is a tripwire.
