# Decision Records

## Template guidance

Record major design and policy tradeoffs, including rejected alternatives and evidence. Proposed entries do not authorize implementation. Acceptance must identify the decision owner and approval evidence. Retain superseded records and link replacements.

## Reusable record template

The markers below belong to the reusable template, not to an actual decision.

- ID: [REQUIRED: unique ADR identifier]
- Date: [REQUIRED: actual record date in YYYY-MM-DD format]
- Status: Proposed | Accepted | Superseded | Deprecated
- Decision owner: [REQUIRED: accountable person or team]
- Context: [REQUIRED: problem, constraints, and affected scope]
- Decision: [REQUIRED: chosen approach and boundaries]
- Alternatives considered: [REQUIRED: viable alternatives and reasons rejected]
- Consequences: [REQUIRED: benefits, costs, risks, and compatibility effects]
- Supporting evidence: [REQUIRED: review, experiment, verification, or requirement references]
- Approval evidence: [REQUIRED for Accepted: approver, date, approved scope, and reference]
- Supersedes / superseded by: [REQUIRED: related IDs or Not applicable with reason]

## Recorded decisions

### ADR-001: Documentation-only, language-neutral framework

- Date: 2026-10-02
- Status: Accepted
- Decision owner: Dylan, requesting project maintainer
- Context: The framework mixed reusable documentation with a minimal executable starter and incomplete policy prompts.
- Decision: Keep the root document set, remove runtime scaffolding, define explicit adoption and role handoff rules, and let adopting projects select tools and providers.
- Alternatives considered: Keep the starter as optional material, or expand into a runnable AI starter. Both add executable maintenance outside the selected documentation-only scope.
- Consequences: Smaller maintenance surface and wider applicability; adopting projects must supply implementation tooling and evaluation execution.
- Supporting evidence: Repository inspection found a dependency list, setup/test scripts, placeholder directories, and an assertion-only smoke test; the planning review identified missing adoption and approval criteria.
- Approval evidence: Dylan selected the documentation-only direction and scaffold removal, then explicitly requested implementation of the full plan in this conversation on 2026-10-02.
- Supersedes / superseded by: Not applicable - first recorded decision.
