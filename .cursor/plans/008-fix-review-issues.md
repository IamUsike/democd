# 008 — Fix review issues (rules config + graphs UX)

## Scope
1. Tests: RuleEngine enabled skip; RuleConfigService type-scoped updates; RuleController API
2. Frontend: toggle revert on save failure; demo/LIVE badge UX; legend %; transactions overflow
3. Docs: milestones checkboxes; API_ENDPOINT rules section

## Approach
- TDD for backend service/engine first
- Type-scoped PUT: only apply fields relevant to ruleType; ignore or reject foreign params (prefer ignore foreign + only set allowed — cleaner than 400 for extra fields from old clients; reject with IllegalArgumentException if foreign non-null fields present for clearer API)
- Choose: reject unknown/foreign params with clear exception → 400 via handler if needed
