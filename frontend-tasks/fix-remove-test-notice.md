# Remove preview notice and repair legal navigation

Owner: Codex (GPT-6). Base: `origin/main` at `a4bd9df`.
Branch: `fix-remove-test-notice`; dedicated external worktree.

The user reported that the footer legal links do not open, asked that legal
links remain in the bottom-right footer instead of the lead form, and asked to
remove the visible local-test notice. This is a corrective continuation of the
footer legal-document task. The previously authorized PR-to-`main` and merge
workflow applies to this correction; production deployment remains outside the
task.

## Scope and owned paths

- Lead-form copy and status rendering in
  `frontend/components/leads/LeadForm.tsx`.
- Docker/Nginx routing for extensionless static-export document URLs.
- Focused component, container-routing, and browser tests.
- This brief, `TASKS.md`, and handoff/index metadata.

The footer links and confirmed `ИНН: 771549669484` remain unchanged. Preserve
all operational form statuses for disabled, sending, offline, retry, and
backend-response states.

## Plan

1. Reproduce the missing initial-state behavior and broken production redirect
   in regression tests.
2. Remove form-level legal anchors and the idle test notice, then serve
   extensionless legal routes from their exported `.html` files.
3. Run focused and applicable gates, review the diff, publish the PR, and
   complete the authorized merge workflow.
