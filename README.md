# AI Project Framework

## Purpose and audience

This is a documentation-only template for teams building or maintaining AI-enabled systems. It helps maintainers, contributors, and AI agents agree on project context, responsibilities, security boundaries, design decisions, and evidence needed to release a change.

The framework provides Markdown guidance, not an application, evaluation runner, or deployment system. Adopting projects choose their languages, tools, providers, and repository layout.

## Adopt the framework

1. Copy the documentation and local-secret ignore rules into your project. Preserve existing project history and merge existing policies deliberately.
2. Replace this overview with your project's purpose, audience, capabilities, and actual setup instructions. Retain the documentation map and adoption conventions.
3. Complete project-specific fields in the documents below, starting with context, owners, security, and standards. Record assumptions and unresolved decisions explicitly.
4. Have the designated owners approve applicable policies and record the approver, date, and scope. Naming an owner or copying this template does not constitute approval.
5. Walk a representative change through planning, implementation, documentation, review, and release using [AGENTS.md](AGENTS.md). Record the verification evidence.
6. Check links, document consistency, outstanding fields, and release readiness before declaring adoption complete.

## Guidance and adopted policy

Sections labeled **Template guidance** explain how to complete the framework. They are not evidence that a project has implemented or approved the described controls.

Sections labeled **Project fields** require project-specific answers. An unresolved answer uses `[REQUIRED: description]`. Replace each marker with an answer or `Not applicable - reason`. Missing information must remain visible; never infer approval from a blank field.

Sections labeled **Baseline rules** are proposed defaults for adoption. In this template repository, the workflow and documentation rules govern maintenance; security defaults govern repository handling. Adopting projects must review these defaults and record their approved policies. Until a relevant security decision is resolved, do not perform the affected action.

Required fields inside a reusable record template are instructions for future entries, not outstanding entries. Label templates explicitly and keep them separate from actual records.

## Documentation map and ownership

This table is the central inventory of required root documents and their update triggers. Owners are responsibility categories; assign actual people or teams in [CONTEXT.md](CONTEXT.md).

| Document | Accountable owner | Authoritative subject | Update when |
| --- | --- | --- | --- |
| [README.md](README.md) | Project owner | Purpose, audience, adoption, document inventory | Scope, setup, document ownership, or adoption process changes |
| [CONTEXT.md](CONTEXT.md) | Project owner | Stakeholders, assumptions, constraints, current state, outcomes | Users, owners, assumptions, constraints, or outcomes change |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Technical owner | Components, data flow, integrations, operational design | Boundaries, providers, data flow, or failure handling change |
| [SECURITY.md](SECURITY.md) | Security owner | Credentials, data permissions, AI restrictions, incident policy | Access, data handling, tool permissions, or security controls change |
| [STANDARDS.md](STANDARDS.md) | Quality owner | Conventions, evaluation evidence, review and release gates | Tooling, acceptance criteria, review, or operational procedures change |
| [DECISIONS.md](DECISIONS.md) | Technical owner | Decision history, rationale, approval evidence | A major design or policy tradeoff is proposed, accepted, or superseded |
| [ROADMAP.md](ROADMAP.md) | Project owner | Priorities, dependencies, milestone status | Priority, sequencing, ownership, dates, or delivery status change |
| [CHANGELOG.md](CHANGELOG.md) | Release owner | Notable changes and actual releases | A notable change is reviewed or a release is prepared |
| [AGENTS.md](AGENTS.md) | Workflow owner | Agent responsibilities, permissions, handoffs | Roles, authorization boundaries, or handoff rules change |
| [CLAUDE.md](CLAUDE.md) | Workflow owner | Pointer to shared agent instructions | The shared instructions location changes |

## Adoption completion checklist

- All mapped documents exist and relative links resolve.
- Every applicable project field is answered; exclusions have reasons.
- Named owners and policy approval records are present.
- Architecture reflects actual components and security boundaries.
- Verification methods and acceptance criteria fit the project.
- A sample workflow has complete handoffs and no unresolved blocking findings.
- Roadmap status and changelog entries reflect actual work; dates are not invented.
