# Plan: Multi-source design + docs refactor (1A + 2A)

## Goal

Refactor design docs, ERD, DFD, milestones, API notes, and agent rules for
soft multi-source tenancy (`BANK`/`MERCHANT` + `source_id` + `source_name`),
richer transaction fields, lean MVP (hardcoded rules), and phased
config-rules/security — align Flyway `V1` before the entity milestone.

## Locked decisions

- Soft tenancy; denormalized `source_name` (no sources master table)
- Lean MVP: hardcoded rules Phase 1; user-configurable rules Phase 2;
  encryption/masking Phase 4
- KPIs as aggregations, not row columns
- One public ingest contract for bank/merchant simulators

## Steps

1. Update `DATABASE_DESIGN.md` + ERD
2. Update `DFD-MVP.md` + `API_ENDPOINT.md`
3. Update milestones, team split, `backend-java.mdc`, README, AGENTS
4. Rewrite `V1__create_transactions.sql` (recreate local DB if old V1 applied)

## Result

Docs and V1 migration aligned on enriched multi-source schema. No JPA
entity/controllers yet — next milestone remains Transaction entity +
repository with the new fields.
