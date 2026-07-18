# Agent Instructions

## Role
- Act as the implementation agent for `AndrewWebSite`.
- Inspect the repository and relevant documentation before editing.
- Make the smallest safe change that fully satisfies the task.
- Avoid unrelated refactors and preserve user-authored changes.
- Raise architectural or security-sensitive ambiguities before implementation.

## Current Repository State
- The repository currently contains GitHub/Jules automation and documentation.
- Next.js and Spring Boot application scaffolds have not landed yet.
- Treat `.github/JULES_AUTOMATION.md` as the canonical automation guide.
- Do not invent application commands, dependencies, or conventions before their manifests exist.

## Package Manager
- None is configured yet.
- After scaffolding, use the package manager declared by committed manifests and lockfiles; never mix package managers.

## File-Scoped Commands
| Task | Command |
| --- | --- |
| Validate one workflow | `actionlint .github/workflows/<file>.yml` |
| Validate all workflows | `actionlint .github/workflows/*.yml` |
- Application checks must use scripts declared by the relevant application manifest once available.

## Security and Automation
- Never commit, expose, log, or request secrets, tokens, credentials, or private keys.
- Keep third-party GitHub Actions pinned to full commit SHAs.
- Do not weaken actor, event, branch, permission, signature, or replay-protection checks.
- The `pull_request_target` relay must never check out or execute pull-request code.
- Jules automation may create reviewable pull requests; it must never merge them.
- Keep `.github/JULES_AUTOMATION.md` synchronized with automation behavior.

## Git and Pull Requests
- Allowed push branches: `main`, `task-*`, `fix-*`, and `codex/**`.
- Use Conventional Commit titles: `feat|fix|docs|test|refactor|perf|chore|ci` with an optional scope.
- Keep commits atomic and run checks relevant to every changed file.
- Add a regression test for bug fixes when technically possible.

## Key Conventions
- Use `set -euo pipefail` in multi-line Bash workflow steps.
- Validate all external input at the system boundary.
- Preserve least-privilege GitHub token permissions.
- Update existing documentation instead of duplicating it.

## Commit Attribution
AI-authored commits must include the agent's own attribution footer, never a human identity:

```text
Co-Authored-By: <agent name/model> <agent-provided noreply address>
```
