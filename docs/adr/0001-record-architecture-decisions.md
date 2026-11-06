# ADR-0001: Record architecture decisions as ADRs

- **Status:** Accepted
- **Date:** 2026-11-06
- **Stage:** 0

## Context

The project brief (`ledgerline.md`, section 0) asks for an ADR for every real
design decision. Ledgerline makes many decisions with long consequences: money
representation, locking, idempotency, event delivery and multi-tenancy. Each
needs to be findable, reviewable and challengeable later, without
reconstructing the reasoning from commit history.

## Decision

- Each decision gets one Markdown file in `docs/adr/`, named
  `NNNN-short-title.md` with a four-digit sequence number. Numbers are never
  reused.
- Files follow [`template.md`](template.md): Status, Date, Stage, Context,
  Decision, Alternatives considered, Consequences.
- Status moves `Proposed` → `Accepted`. An accepted ADR is not rewritten. If
  the decision changes, a new ADR supersedes it, and the old one's status
  becomes `Superseded by ADR-NNNN`.
- New ADRs start as `Proposed` and become `Accepted`
  when the project owner merges the stage PR.
- [`README.md`](README.md) in this folder indexes every ADR.
- An ADR may cite numbers only from saved runs under `docs/results/`.

## Alternatives considered

- **Decisions only in commit messages or PR descriptions**: hard to find,
  and they don't show the current state of a decision.
- **One big design document**: rewritten in place, so the history of *why*
  is lost, and nobody can tell which parts are settled.
- **A wiki outside the repo**: drifts from the code and isn't reviewed with it.

## Consequences

- Reviewers can check code against a written decision.
- Writing ADRs costs a little time per decision. Small, local choices (naming,
  a library's config value) do not need one.
