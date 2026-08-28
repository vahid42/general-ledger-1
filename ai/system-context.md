# System Context

## Purpose

Defines the system architecture, layer boundaries, and authoritative sources for the AI Agent.

Business Rules and Use Cases are NOT defined here. Read them from relevant `docs/`.

## Architecture

System follows:

`DDD | Clean Architecture | Separation of Concerns | Modular Design`

Core goals:

- Keep Domain independent from technology.
- Preserve Layer, Module, and Bounded Context boundaries.
- Keep Business Logic in the Domain.
- Minimize technical impact on Domain.

## DDD

Domain owns business concepts and behavior:

`Entity | Value Object | Aggregate | Aggregate Root | Domain Service | Policy | Domain Event | Repository Abstraction`

Business Rules MUST NOT be moved to Application or Infrastructure for convenience.

## Layers

Dependency direction:

```text
Presentation → Application → Domain
Infrastructure → Domain/Application abstractions
````

### Domain

Owns business behavior, invariants, domain services/policies/events, and repository abstractions.

MUST NOT depend on Presentation or Infrastructure.

### Application

Owns use cases, commands/queries, orchestration, coordination, and repository calls.

MUST NOT become the primary owner of Core Business Rules.

### Infrastructure

Owns persistence, external services, messaging, serialization, framework integration, and technical configuration.

MUST NOT own Core Business Rules.

### Presentation

Owns APIs, controllers, DTOs, transport validation, and mapping.

Controllers MUST remain thin and contain no Core Business Logic.

## Modules

Each Business Capability belongs to its owning Module.

Before introducing a Module dependency, verify:

1. Concept ownership
2. Necessity of dependency
3. Available abstraction
4. Bounded Context impact

Avoid unnecessary Module coupling.

## Documentation

Authoritative project knowledge is under:

```text
docs/
├── analysis/
├── architecture/
└── ...
```

Inspect relevant documentation before making Architecture or Business decisions.

## Source Priority

When sources conflict:

1. Project Requirements
2. Approved Architecture Docs
3. Approved Domain Analysis
4. Approved Use Cases
5. Implementation
6. Tests
7. General Engineering Conventions

Existing code does NOT automatically define correct Architecture or Business Rules.

## Business Rules

Business Rules MUST come from project documentation.

NEVER invent undocumented Business Rules.

If a required rule is missing or ambiguous, report it when it affects correctness.

## Review Behavior

The Agent acts as an Architectural Reviewer and Engineering Assistant.

MUST:

* Read relevant documentation.
* Preserve architecture and boundaries.
* Protect Domain invariants.
* Check ownership before structural changes.
* Avoid unnecessary abstractions/refactoring.
* Distinguish Defects from Suggestions.
* Provide evidence and reasoning for Findings.

MUST NOT:

* Treat personal coding preferences as project rules.
* Assume existing implementation is correct.
* Propose structural changes without checking ownership and boundaries.

## Review Policy

Code Review workflow and rules:

`.ai/review-policy.md`

Agent execution configuration:

`.ai/agent-config.yaml`

Follow these files during review.

## Core Principle

Understand documented Architecture and Intent first; evaluate Implementation against them second.
