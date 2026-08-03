# Plans

When you ask a Cursor agent to plan a non-trivial feature (a new rule
type, the alert lifecycle, extracting the rule engine into its own
deployable), save the generated plan here before implementation starts.

## Naming
`NNN-short-feature-name.md` — numbered in the order they were created,
e.g. `001-mvp-transaction-recording.md`, `002-alert-lifecycle.md`.
Numbering, not dating, so the order of decisions is obvious at a glance
regardless of when someone opens the folder.

## Why bother
- **Resuming work:** if a feature spans more than one session, the next
  session (yours or a teammate's) starts from the plan, not from
  re-deriving it.
- **Team documentation:** teammates working on the ERD or API docs can
  read a plan to understand a decision without pinging whoever wrote the
  code.
- **Historical context:** six months from now (or six hours from now,
  mid-hackathon), "why did we do it this way" has an answer on disk.

## What goes in a plan
Whatever the agent produces when asked to plan — steps, file-level
changes, open questions. Don't hand-edit these into polished
documentation; they're working notes, not deliverables. If a plan turns
out to be wrong partway through, don't delete it — add a short "revised"
note at the top pointing to what changed and why, so the history stays
honest.

An example stub is in `001-example-mvp-transaction-recording.md` — copy
its shape for real plans, then delete it once you have your first real
one.
