# Semgrep AppSec Platform integration

The native Semgrep workflow is intentionally disabled until the backend skeleton
has been merged into `main`. When enabled, it runs `semgrep ci` against the
repository's Semgrep AppSec Platform policies for pull requests to `main`,
pushes to `main`, manual runs, and one weekly scheduled run.

## Activation

Complete these steps in order:

1. Revoke every Semgrep token that has appeared in chat, source control, logs,
   pull requests, or other exposed locations.
2. In Semgrep AppSec Platform, create a new **Agent (CI)** token with only the
   access required for this repository's policy scans.
3. Store the new value directly in GitHub under **Settings → Secrets and
   variables → Actions → Secrets** as `SEMGREP_APP_TOKEN`.
4. After the backend skeleton pull request has merged into `main`, set the
   repository variable `SEMGREP_ENABLED` to exactly `true` under **Settings →
   Secrets and variables → Actions → Variables**.

Do not place the token in an issue, pull request, workflow input, `.env` file,
repository file, or chat. The workflow skips all scans until
`SEMGREP_ENABLED` is exactly `true`; leaving the variable unset or setting it
to any other value keeps it disabled.

## Security boundaries

- The workflow uses only `contents: read` and does not upload SARIF.
- It does not use `pull_request_target`.
- Manual tokenized scans can be started only by the repository owner.
- Tokenized pull-request scans run only when both the pull-request author and
  the triggering actor are the repository owner. Pull requests from forks,
  Dependabot, collaborators, and repository automation are skipped, so their
  code never receives the Semgrep token. A full scan still runs after an
  accepted change reaches `main`.
- The Semgrep image is pinned by digest, and the checkout action is pinned by
  commit SHA.

## Enforcement status

This workflow is monitoring-only while any pull-request category is skipped.
Do not configure `Semgrep policy scan` as a required branch-protection check:
GitHub can report a conditionally skipped job without proving that a scan ran.
Before making Semgrep a required merge gate, add a separate always-running
status job that fails whenever an enabled scan was expected but did not finish
successfully, and define an explicit policy for automation and external pull
requests.
