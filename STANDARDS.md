# Project Standards

## Template guidance

Select conventions and verification methods appropriate to the project. This framework requires evidence, not a particular language, UI toolkit, test runner, or directory layout. Record adopted standards and approval in [CONTEXT.md](CONTEXT.md).

## Baseline rules

### Changes and documentation

- Keep changes within approved scope and make interfaces and failure behavior understandable.
- Update affected documents in the same reviewed change, using the ownership and update triggers in [README.md](README.md).
- Record major design or policy tradeoffs in [DECISIONS.md](DECISIONS.md).
- Keep guidance, unresolved project fields, proposals, and approved policy distinguishable. Do not invent approvals, test results, release dates, or milestone completion.
- Version prompts and relevant configuration so verification evidence can identify what was evaluated.

### Verification and evaluation evidence

- Define intended behavior and acceptance criteria before implementing a behavior change.
- Cover representative successful inputs and relevant failure cases, including invalid outputs and unavailable dependencies.
- For AI behavior changes, include cases for applicable untrusted-content, permission, and sensitive-data boundaries. Compare against the previous behavior when it exists.
- Prefer reproducible checks; when outputs vary, record the evaluation method, repetitions, and acceptance thresholds rather than claiming certainty from one sample.
- Record the changed revision, prompt/model/configuration identifiers where applicable, cases, method or commands, expected outcome, actual results, and limitations.
- Protect evaluation fixtures and reports according to [SECURITY.md](SECURITY.md). Offline checks are preferred when external calls are unnecessary.
- Documentation-only changes require link, consistency, completion-state, and diff review. They do not require artificial application tests.

### Review and release gates

- Use `vMAJOR.MINOR.PATCH` for release labels and Git tags, for example `v0.0.0` or `v0.1.0`. This format does not assign the current project a version.
- A reviewed change includes scope, rationale, affected documentation, verification evidence, and unresolved risks.
- A **blocking** finding is an unmet acceptance criterion, relevant policy violation, defect preventing intended behavior, or missing evidence needed to establish readiness. Resolve it and recheck the affected behavior before approval or release.
- A **nonblocking** finding is an improvement that does not prevent readiness. Record its rationale and, if deferred, its owner and follow-up location.
- Review outcomes are changes requested or ready within the documented scope. A ready result is not authorization to publish or deploy.
- Before release, confirm required checks passed, documentation is current, and no blocking findings remain. Finalize release notes and obtain authorization for the intended release action.

## Project fields

- Implementation conventions: [REQUIRED: chosen languages, formatting, interface conventions, and repository layout, or justified exclusions]
- Verification methods: [REQUIRED: applicable tools/commands or manual procedures and when each runs]
- Acceptance criteria: [REQUIRED: project quality thresholds and evaluation expectations for its actual use cases]
- Review ownership: [REQUIRED: accountable reviewers and whether any changes require independent review]
- Operations and rollout: [REQUIRED: logging, monitoring, rollout checks, rollback procedure, and accountable operators, or justified exclusions]

## Completion criteria

A contributor can determine the checks required for a proposed change, produce traceable evidence, classify review findings, and establish readiness without assuming publication permission.
