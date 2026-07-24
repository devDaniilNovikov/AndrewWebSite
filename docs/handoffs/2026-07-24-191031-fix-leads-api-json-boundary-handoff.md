# fix-leads-api-json-boundary handoff

- **UTC:** 2026-07-24T19:10:31Z
- **Branch:** `fix-leads-api-json-boundary`
- **Worktree:** `/Users/daniilnovikov/.codex/worktrees/leads-api-json-boundary/AndrewWorkWebSite`
- **Base:** `origin/main` at `731a17dbba5503b7a3ea94ac32ff9567f490d443`
- **PR:** Draft [#39](https://github.com/devDaniilNovikov/AndrewWebSite/pull/39)

## State

Draft PR #39 is open for the strict JSON boundary remediation. The code fix is
committed as `d5cd296` after the initial metadata commit `c31c082`. A final
metadata commit follows this handoff and is expected to become the PR head.

## Fixed Findings

- Non-canonical UUID strings no longer reach Jackson UUID conversion. The lead
  deserializer now accepts only exact 36-character hex/hyphen UUID strings,
  preserving upper- and lower-case hex and avoiding Base64 or trimming paths.
- `intent` no longer uses mapper enum conversion. The deserializer accepts only
  raw string literals `repair` and `maintenance`.
- Duplicate JSON keys now fail during tree reading through Jackson
  `DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY`.

## Verification

- `./mvnw -B -Dtest=LeadControllerContractTest test` passed with 62 tests.
- `./mvnw -B verify` passed with 172 tests, PostgreSQL 18.4 Testcontainers,
  and JaCoCo coverage check met.
- `git diff --check` passed.
- `semgrep scan --config auto --error --quiet .` passed with no findings.
- `trufflehog filesystem --no-update --fail --results=verified,unknown .`
  passed with 0 verified and 0 unknown secrets.
- Whole-diff review found only `TASKS.md`, memory handoff/lesson metadata,
  `LeadRequestDeserializer`, `StrictJsonConfiguration`, and
  `LeadControllerContractTest` changes.
- Repeat review across OpenAPI/HTTP boundary, Jackson/security, and
  tests/regressions found no Critical, Important, or actionable Minor issues.

## Documentation Evidence

- Spring Boot 4.1 documentation identifies
  `org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer`
  as the callback for customizing the auto-configured Jackson 3
  `JsonMapper.Builder`.
- Jackson Databind documentation for `FAIL_ON_READING_DUP_TREE_KEY` states
  duplicate property names throw while parsing JSON Trees when the feature is
  enabled; otherwise the last encountered value is used.
- Local Jackson 3.1.5 API inspection confirmed
  `MapperBuilder.enable(DeserializationFeature...)` and the
  `FAIL_ON_READING_DUP_TREE_KEY` enum constant exist in the project dependency.

## Boundaries

- No OpenAPI, Flyway, frontend, CI workflow, production configuration,
  production service, secret, or user-owned `receipts/` path was changed.
- Ready, merge, and production deploy remain unauthorized.
