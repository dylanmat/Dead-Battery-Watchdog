# Architecture

## Template guidance

Document the system that exists or is explicitly proposed. Label current and proposed designs separately. Do not introduce orchestration, retrieval, memory, queues, or provider abstraction solely to fill this template. Mark unused components not applicable with a reason.

Use [SECURITY.md](SECURITY.md) for permission policy, [STANDARDS.md](STANDARDS.md) for verification gates, and [DECISIONS.md](DECISIONS.md) for major tradeoffs.

## Project fields

- Design status and principles: [REQUIRED: current/proposed scope, design constraints, and tradeoffs]
- Components and responsibilities: [REQUIRED: actual components, interfaces, accountable operators, and boundaries]
- End-to-end data flow: [REQUIRED: input sources, processing, model/tool calls, validation, outputs, and storage]
- Trust boundaries: [REQUIRED: where untrusted content enters, where data leaves the system, and where permissions are enforced]
- External integrations: [REQUIRED: APIs, providers, storage, authentication mechanism references, and dependency ownership; never include credentials]
- Model integration, if used: [REQUIRED: model/provider choices linked to security approval, configuration, timeouts, retry limits, rate-limit handling, and any fallback]
- Prompt and context handling, if used: [REQUIRED: prompt version identification, context sources, context limits, and handling of untrusted content]
- Retrieval or memory, if used: [REQUIRED: indexing, access filtering, provenance, persistence, and deletion behavior]
- Output and action boundaries: [REQUIRED: validation steps and approval enforcement before consequential actions]
- Failure handling: [REQUIRED: unavailable dependencies, invalid outputs, partial tool failures, retry safety, and how users learn of failures]
- Operations: [REQUIRED: deployment boundaries, observability without sensitive content, incident ownership, and recovery/rollback references]
- Verification mapping: [REQUIRED: critical design assumptions and corresponding tests, evaluations, or manual checks]

## Completion criteria

A reviewer can trace a representative request through the system, identify data exposure and action boundaries, and explain the expected behavior when a dependency fails. Proposed changes have decision records where meaningful tradeoffs exist.
