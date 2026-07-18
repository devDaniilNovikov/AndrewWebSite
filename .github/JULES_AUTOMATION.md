# Jules and GitHub automation

This repository has three independent event paths:

1. The native Jules GitHub App starts a task when an authorized user adds the
   `jules` label to an issue.
2. GitHub Actions can start Jules with a custom prompt for an owner-approved
   `jules-action` issue or a trusted same-repository `CI` push failure.
3. Pull request lifecycle events can be relayed to the application backend as
   signed HTTPS requests.

All automation creates reviewable pull requests. Nothing in these workflows
merges a pull request.

## One-time Jules setup

1. Install and authorize the Google Labs Jules GitHub App for
   `devDaniilNovikov/AndrewWebSite` in the Jules web app.
2. Generate a new Jules API key. Do not reuse a key that has appeared in chat,
   source control, logs, or a pull request.
3. Add the key in GitHub under **Settings → Secrets and variables → Actions →
   Secrets** as `JULES_API_KEY`.
4. Add `JULES_ALLOWED_ACTOR` as a repository variable with the GitHub login of
   the only person allowed to start Jules automation.
5. Add the repository variable `JULES_AUTOMATION_ENABLED` with value `true`.

Keep `JULES_AUTOMATION_ENABLED` unset or set to `false` until the App and secret
are both ready. The API key must never be placed in an issue, workflow input,
`.env` file, or repository file.

## Starting work from an issue

- `jules`: uses Jules' native issue integration. No custom Action workflow is
  involved.
- `jules-action`: uses `.github/workflows/jules-issue.yml` and the repository
  secret. Only `JULES_ALLOWED_ACTOR` can author the issue and trigger this path
  by applying the label. For an external report, create a new sanitized issue
  under the allowed account instead of forwarding hostile issue text directly.

Do not put both labels on one issue: that can create duplicate Jules sessions.
The official Jules Action currently auto-approves the plan and asks Jules to
create a PR, so every generated PR must receive human review before merge.

## Repairing CI failures

`.github/workflows/jules-ci-failure.yml` listens for the workflow named `CI`.
It accepts only failed `push` runs from this repository on `main`, `task-*`,
`fix-*`, or `codex/**` branches, and only when the push actor matches
`JULES_ALLOWED_ACTOR`. It deliberately ignores fork, pull-request, bot, and
Jules-generated runs; Jules' native CI fixer can handle failures on PRs created
by Jules.

## Receiving pull request events

`.github/workflows/pr-event-relay.yml` is disabled by default. Once the Spring
Boot backend has a public HTTPS receiver:

1. Generate a random webhook secret with at least 32 characters.
2. Store the receiver URL as the Actions secret `PR_WEBHOOK_URL`.
3. Store the signing secret as `PR_WEBHOOK_SECRET`.
4. Configure the receiver to verify the raw request body against
   `X-Hub-Signature-256` using HMAC-SHA256 before parsing JSON.
5. Require `sentAt` to be no more than five minutes old.
6. Deduplicate deliveries using `X-GitHub-Delivery`. The value is derived from
   the canonical source event and therefore stays stable across Actions reruns;
   `attemptId` identifies an individual delivery attempt.
7. Set the repository variable `PR_WEBHOOK_ENABLED` to `true`.

The relay uses `pull_request_target` only to access repository secrets for fork
PR events. It never checks out or executes pull-request code, and its
`GITHUB_TOKEN` has no permissions. The receiver must rate-limit requests and
reject invalid signatures before doing any work.

## Verification

- Open an issue, then have the repository owner add `jules-action`.
- Trigger a failing push on an allowed branch and confirm one Jules session is
  created.
- Send a PR event to a test receiver and verify the signature and delivery ID.
- Confirm every resulting Jules PR requires normal review and CI checks.
