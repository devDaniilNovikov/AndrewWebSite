# AndrewWebSite — Project Specification (canonical index)

**This file is the routing root for every contract in the project.** A
numeric or behavioral contract lives in exactly one canonical file linked
below; role files (`AGENTS.md`, `CLAUDE.md`, the Jules guide) link here and
never restate values. On conflict, the linked canonical file wins over any
role file, plan, or memory record; an explicit current user decision wins
over older planning text.

## Product

B2B website for a commercial refrigeration repair company (Moscow). Primary
conversion: a phone call; secondary: the lead form. Canonical product scope,
content status, acceptance criteria, and open product questions live in the
[Russian technical brief](product/technical-brief.ru.md). Recommendations,
assumptions, and open questions in that brief are not approved requirements;
prices, requisites, guarantees, legal texts, and other missing facts are never
invented and remain launch blockers until verified.

## Architecture

Canonical: [`backend/architecture.md`](backend/architecture.md) — runtime
topology, backend and frontend stacks, storage, the transactional outbox
delivery to Telegram (states, worker, retry, privacy semantics), and trust
boundaries. Nothing from that document is restated anywhere else.

## Contracts

- **HTTP API:** [`backend/openapi.yaml`](backend/openapi.yaml) — request and
  response shapes, statuses, error format.
- **Data model:** [`backend/architecture.md`](backend/architecture.md)
  § data model; once the Flyway baseline task lands, versioned migrations
  under the backend source tree become the canonical schema history.
- **Async delivery semantics:** [`backend/architecture.md`](backend/architecture.md)
  — queue states, transitions, retry policy, delivery guarantees.

## Security and privacy

Canonical: [`backend/architecture.md`](backend/architecture.md) — public
surface, rate limiting, forwarded-header trust rules, PII lifecycle and
anonymization; [`backend/operations.md`](backend/operations.md) — secrets
handling, backups, retention. Credentials ever exposed in chat or logs are
compromised: rotate, never reuse.

## Process and quality gates

Canonical: [`../.agents/workflows/GIT_FLOW.md`](../.agents/workflows/GIT_FLOW.md)
— lifecycle, PR contract, required checks, branch protection; role files —
per-agent gates. Never weaken a valid failing test or a security control.

## Production gates

Canonical: [`backend/operations.md`](backend/operations.md), plus explicit
user authorization for any production merge or deploy.

## Open parameters

Undecided values an agent must never invent (stop and ask; see
`OPEN_QUESTIONS.md`):

- Trusted proxy CIDRs / `X-Forwarded-For` trust — blocked on Timeweb proxy
  verification (Q-20260718-001).
- Analytics vendor — undecided.
- Legal texts and verified company requisites — blocked on the sanitized
  brief (Q-20260718-003).
