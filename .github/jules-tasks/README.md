# Jules task manifests

The `jules-action` workflow treats GitHub issues as authorization handles only.
It never forwards issue titles, bodies, comments, attachments, or linked content
to Jules.

Before applying the `jules-action` label, add one reviewed manifest named
`<issue-number>.json` in this directory on `main`. The manifest must use schema
version `1` and contain only:

- `title`: concise task title;
- `objective`: trusted repository-maintained task text;
- `ownedPaths`: paths Jules may change;
- `requiredChecks`: commands Jules must run before publication.

Example:

```json
{
  "version": 1,
  "title": "Repair the frontend policy regression",
  "objective": "Restore the approved CI invariant and add a regression test.",
  "ownedPaths": [
    ".github/workflows/ci.yml",
    ".github/scripts/**"
  ],
  "requiredChecks": [
    "bash .github/scripts/test-ci-paths.sh"
  ]
}
```

The filename is selected only after the event issue number passes a canonical
positive-integer check. Unknown fields, control characters, absolute or
parent-traversing owned paths, missing checks, symbolic-link manifests, and a
manifest absent from the pinned `main` checkout fail closed.

The workflow validates the manifest with a closed schema, resolves the opaque
Jules source through the API, and creates a session with human plan approval
required. The issue's title, body, comments, attachments, and links are never
read or copied into the session request.

Do not remove and reapply `jules-action`: the provider's v1alpha session-create
request has no idempotency key. The workflow prevents concurrent duplicates,
but it cannot deduplicate two separate successful label events without adding
state or broader GitHub permissions.
