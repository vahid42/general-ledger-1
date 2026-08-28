# ADR-0002 — Maven Multi-Module Architecture

**Status:** Accepted
**Date:** 2026-08-20
**Related ADR:** ADR-0004

## Context

`general-ledger` contains distinct architectural responsibilities:

`Domain | Application | Infrastructure | Presentation | Bootstrap | Test`

A single Maven Module would leave boundaries mainly at Package level, allowing unwanted dependencies such as `Domain → Infrastructure`.

It would also reduce module independence for Build, Dependency Management, and Lifecycle.

## Decision

Use a **Maven Multi-Module Project**.

Root project:

```text id="4b2xk9"
general-ledger
```

The Root acts as Maven **Aggregator + Parent** and contains no executable application code.

Modules:

```text id="c8q1wy"
general-ledger/
├── ledger-domain
├── ledger-application
├── ledger-infrastructure
├── ledger-presentation
├── ledger-test
└── ledger-bootstrap
```

## Module Responsibilities

### `ledger-domain`

Owns core Business Model and Business Rules.

Rules:

* MUST remain independent from Infrastructure technologies.
* SHOULD remain independent from runtime Frameworks.
* MUST NOT depend directly on Infrastructure or Presentation.

### `ledger-application`

Owns Use Cases and orchestration between Domain and Ports.

Allowed:

```text id="q1v8af"
ledger-application
        ↓
ledger-domain
```

Rules:

* MAY depend on `ledger-domain`.
* MUST NOT directly depend on Infrastructure.
* MUST NOT become the owner of Core Business Rules.

### `ledger-infrastructure`

Owns technical implementations:

`Persistence | Database | External Services | Messaging | Technical Details`

Allowed dependencies:

```text id="t9p3kc"
ledger-infrastructure
        ├── ledger-application
        └── ledger-domain
```

### `ledger-presentation`

Owns external system interaction:

`REST API | Request/Response | Controllers | API Validation | DTO Mapping`

Rules:

* MUST NOT own Business Logic.
* Controllers SHOULD remain thin.

### `ledger-bootstrap`

Application entry point and Composition Root.

Owns:

`main application | Spring Boot configuration | startup configuration`

### `ledger-test`

Dedicated tests requiring a separate Module.

Examples:

`Architecture Tests | ArchUnit | Cross-module Tests`

## Module Isolation

Every Module MUST have its own `pom.xml`.

Dependencies MUST be explicitly declared.

A Module may depend on another Module **only when that dependency is architecturally defined**.

Convenience-based dependencies for direct class access are NOT allowed.

## Consequences

### Positive

* Architecture boundaries are enforceable at Maven level.
* Module dependencies become explicit.
* Unwanted dependencies are reduced.
* Build/Lifecycle can be managed per Module.
* Infrastructure changes have less impact on Domain.
* Domain can be developed and tested independently of Frameworks.
* Project structure aligns with architectural boundaries.
* Independent development/testing becomes possible.

### Negative

* More `pom.xml` files.
* More complex Dependency Management.
* Larger projects may require longer Builds.
* Cross-module changes require Dependency Graph awareness.

## Dependency Principle

Maven Multi-Module structure **alone does not guarantee architectural correctness**.

Dependency Direction MUST be defined and enforced separately.

Detailed dependency direction is defined by:

```text id="m2r7vn"
ADR-0004: Module Dependency Direction
```

## Resulting Structure

```text id="7c5m1p"
general-ledger/
├── pom.xml
├── ledger-domain/
│   └── pom.xml
├── ledger-application/
│   └── pom.xml
├── ledger-infrastructure/
│   └── pom.xml
├── ledger-presentation/
│   └── pom.xml
├── ledger-test/
│   └── pom.xml
└── ledger-bootstrap/
    └── pom.xml
```

## Agent Rules

When reviewing Module architecture:

* Verify the changed Module against its defined responsibility.
* Reject dependencies not defined by the architecture.
* Treat `ledger-domain → infrastructure` as invalid.
* Treat `ledger-application → infrastructure` as invalid unless an authoritative ADR explicitly allows it.
* Do not consider Maven module separation sufficient; verify Dependency Direction.
* Check ADR-0004 for detailed dependency rules.
* Do not report a dependency as a violation solely from preference; verify the authoritative architecture.

## Core Rule

> **Maven Module boundaries must reflect architectural boundaries, and Module dependencies must follow the defined Dependency Direction.**
