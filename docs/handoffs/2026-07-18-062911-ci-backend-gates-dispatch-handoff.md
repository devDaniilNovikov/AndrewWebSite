# CI backend gates dispatch handoff

**Task:** `task-ci-backend-gates`  
**Controller:** Codex  
**Implementer:** Jules  
**Predecessor:** [`2026-07-18-131926-backend-skeleton-review-ready-handoff.md`](2026-07-18-131926-backend-skeleton-review-ready-handoff.md)

## Verified startup evidence

- A fresh fetch placed `origin/main` at `16cb7014dea62cd304f4d242e47c5285f4fc118c`.
- GitHub reports PR #21 merged with that squash commit, and the commit is reachable from `origin/main`.
- The post-merge `main` CI and Semgrep runs for `16cb7014dea62cd304f4d242e47c5285f4fc118c` completed successfully.
- No open pull request or Issue conflicts with `task-ci-backend-gates`.
- The current GitHub actor matches the configured allowed Jules actor. Jules automation is enabled and the required `JULES_API_KEY` secret name is present; no secret value was read or recorded.
- The user explicitly authorized dispatch of this task, but did not authorize merge of its future PR.
- Owner-authored [Issue #22](https://github.com/devDaniilNovikov/AndrewWorkWebSite/issues/22) was created with exactly one label, `jules-action`; the `jules` label was not applied.
- The guarded [Jules workflow run](https://github.com/devDaniilNovikov/AndrewWorkWebSite/actions/runs/29646383219) completed successfully, confirming dispatch. No Jules PR had been verified at this snapshot.

## Scope and boundaries

Jules owns the atomic CI implementation described by Task 2 of [`2026-07-18-backend-foundation.md`](../superpowers/plans/2026-07-18-backend-foundation.md): Temurin Java 25, `./mvnw -B verify`, the existing JaCoCo threshold of at least 80%, Testcontainers-compatible GitHub-hosted execution, dependency review, Java security analysis, Maven caching, immutable action SHAs, and least-privilege permissions.

Dispatch must use a sanitized Issue authored by the allowed repository owner and exactly one `jules-action` label. The `jules` label is forbidden. Jules may open a reviewable Draft PR and must never merge it. `task-backend-deploy-stub` remains blocked until this task's PR is separately reviewed, explicitly authorized, and merged.

## Conditional continuation

After dispatch, verify that the guarded Jules workflow accepted the Issue and wait for a Jules session or PR. Reconcile the Issue and PR URLs in `TASKS.md` only from live GitHub evidence. Review the complete implementation and run fresh applicable checks; do not mark Ready or merge from inferred or stale results.
