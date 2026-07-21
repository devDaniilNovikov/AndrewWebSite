# task-backend-deploy-stub handoff

Signature: HND task-backend-deploy-stub [draft_pr] topics: backend, deploy, tracker → predecessor: none

## Durable — safe to cite later

- T5 merged through PR #28 as `8940abe2dd92db6450f1934649581d4e00b6d1a1`; its source branch remains retained.
- The task adds only the container build/runtime boundary, Docker-context exclusions, and an executable contract test. It changes no HTTP API, application behavior, database contract, frontend, CI workflow, or production resource.
- `ContainerContractTest` was observed RED before implementation and GREEN afterward. Full `./mvnw -B verify` passed with 34 tests and the JaCoCo gate.
- The user chose to provide Docker later. No Docker-compatible runtime is currently available, so image build, image inspection, liveness smoke, and runtime-user verification have not run. PR #29 must remain Draft until they pass.

## Snapshot at 2026-07-19T21:21Z — re-verify live before use

- Draft PR #29 targets `8940abe2dd92db6450f1934649581d4e00b6d1a1` from `task-backend-deploy-stub`.
- Implementation head `80ecc3224397f43a727e4f3dc10dce38abda9a98` was mergeable; Repository policy and Snyk had passed while verify, dependency-security, Java security/CodeQL, and Semgrep were still running.
- Local focused tests, full Maven verification, diff, scope, specification, secret-pattern, quality, and security reviews passed for the implementation head.

## Next steps — conditional, each requires the stated live check

1. Verify PR checks and the complete diff on the final metadata head, but keep PR #29 Draft while the Docker gate is incomplete.
2. When Docker is available, build `andrew-website:local`, run only the named `andrew-website-smoke` container with the local profile, wait for healthy liveness, verify user `10001:10001`, inspect image environment/history for secrets, and remove only that container.
3. After the Docker gate passes, rerun Maven and all PR/security reviews, add a Ready successor handoff, and remove Draft only if the final head is green and review-complete.
4. Do not merge PR #29 or start `task-backend-http-security` without separate user authorization and the required predecessor merge.
