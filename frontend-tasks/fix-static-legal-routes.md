# Static legal route redirects

Branch: `fix-static-legal-routes`; dedicated external worktree.

The user reproduced that the published footer legal links can navigate to
`restholod17.ru:8080/personal-data/` after a Timeweb build from the latest
commit.

Owned paths:

- `Dockerfile`
- `frontend/test/legal-documents.test.tsx`
- `frontend-tasks/fix-static-legal-routes.md`
- `TASKS.md`
- `.agents/memory/HANDOFFS.md`
- `docs/handoffs/2026-09-10-053903-fix-static-legal-routes-handoff.md`

Plan:

1. Reproduce the production symptom with live HTTP probes and inspect the
   current static route contract.
2. Harden the Nginx static route fallback and exact trailing-slash legal routes
   so exported legal pages are served before directory handling can redirect,
   prevent container-port leakage in redirects, and keep the Docker frontend
   package manager pinned to the declared version.
3. Run focused checks, publish a PR to `main`, and complete the authorized
   merge workflow after review.
