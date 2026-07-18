# CI backend gates ready handoff

**Task:** `task-ci-backend-gates`
**Controller and final reviewer:** Codex
**Initial implementer:** Jules
**Predecessor:** [`2026-07-18-062911-ci-backend-gates-dispatch-handoff.md`](2026-07-18-062911-ci-backend-gates-dispatch-handoff.md)

## Delivered scope

- [PR #23](https://github.com/devDaniilNovikov/AndrewWorkWebSite/pull/23) replaces the placeholder application job with Temurin Java 25 and `./mvnw -B verify`, retaining the JaCoCo 80% gate and Maven caching.
- Java CodeQL runs with least-privilege `security-events: write` only on trusted push events; all third-party Actions use immutable commit SHAs.
- The user-approved GitHub Dependency Review replacement is OWASP Dependency-Check `12.2.2`, backed by the `NVD_API_KEY` Actions secret, with `failOnError=true` and a CVSS 7.0 failure threshold.
- The first effective OWASP scan correctly blocked vulnerable Tomcat `11.0.22`. The remediation pins Spring Boot-managed Tomcat `11.0.24` and Log4j `2.25.5`, matching Apache fixed patch releases; no suppression or non-failing bypass was added.

## Fresh verification evidence

- Local `./mvnw -B verify`: 32 tests passed and the JaCoCo gate passed.
- Local dependency tree resolved `tomcat-embed-core:11.0.24` and `log4j-api:2.25.5`.
- Local actionlint, effective-POM validation, diff check, immutable-action scan, credential-pattern scan, permissions review, specification review, and full quality/security review passed with no actionable finding.
- PR head `c4d6240e2755839c731e23ce062cbdcf85447e54` was mergeable and green for Repository policy, verify, OWASP `dependency-security`, Java CodeQL, Semgrep, and Snyk. Expected event-condition skips were not failures.

## Boundary

The task is ready for review but not authorized to merge. Do not close Issue #22, merge PR #23, or start `task-backend-deploy-stub` without a separate explicit user decision. Before any future merge, recheck the live PR head, mergeability, required checks, unresolved conversations, and branch protection; never rely solely on this snapshot.
