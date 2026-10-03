# Roadmap

## Template guidance

Track actual priorities, dependencies, and delivery status. Use explicit YYYY-MM-DD dates or "Unscheduled"; do not use relative windows. Mark work complete only after its acceptance evidence is available.

Adopting projects replace the template-maintenance entries with their own priorities.

## Reusable milestone template

- Milestone: [REQUIRED: deliverable and goal]
- Priority: Now | Next | Later
- Status: Planned | In progress | Blocked | Complete
- Owner: [REQUIRED: accountable person or team]
- Dependencies: [REQUIRED: prerequisites or Not applicable with reason]
- Target: [REQUIRED: explicit date/range or Unscheduled]
- Acceptance criteria: [REQUIRED: observable completion conditions]
- Evidence and related decisions: [REQUIRED: references; unresolved while work is pending]

## Template-maintenance milestones

### Documentation-only framework refinement

- Priority: Now
- Status: Complete
- Owner: Requesting project maintainer
- Dependencies: Approved refinement plan
- Target: Unscheduled
- Acceptance criteria: Consistent ownership and adoption conventions; scoped approval and role handoffs; actionable security and evaluation guidance; executable scaffold removed; links and diff reviewed.
- Evidence and related decisions: [ADR-001](DECISIONS.md); verification record below.

### Validate adoption in a downstream project

- Priority: Next
- Status: Planned
- Owner: Project maintainer
- Dependencies: Verified documentation refinement and a selected downstream project
- Target: Unscheduled
- Acceptance criteria: Project fields completed, policies approved, and a representative change passes the documented workflow.
- Evidence and related decisions: Unresolved - no downstream project selected.

## Refinement verification record - 2026-10-02

- Scope and approval: documentation-only refinement and scaffold removal explicitly requested by the project maintainer; approval recorded in ADR-001.
- Automated checks: all 29 relative document links resolved; required documents remained; obsolete scaffold paths were absent; the shared instructions pointer was unchanged; no obsolete runtime or relative scheduling requirements remained.
- Ignore checks: root and nested environment files were ignored; example files and documentation remained visible.
- Static review: document ownership, completion markers, approval boundaries, review gates, and final diff checked; deletions matched the approved scaffold list and no credentials were introduced. Corrected tool-output validation timing and rechecked it.
- Sample workflow: a proposed prompt change goes from read-only Planner scope and acceptance criteria to explicit approval, scoped Implementer changes and evaluation evidence, Docs updates before merge, Reviewer blocker resolution and recheck, then Release readiness. With no publication authorization, the workflow ends at readiness.
- Handoffs: Planner -> Implementer -> Docs -> Reviewer; the wording correction returned to Docs and Reviewer, then to Release for unreleased notes and readiness. The same agent performed these responsibilities in this conversation.
- Outcome and limits: no unresolved blocking findings; ready for maintainer review. Runtime tests are not applicable to this documentation-only repository. Downstream adoption and actual release have not been performed.
