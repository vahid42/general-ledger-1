# ADR-0004 — Dependency Direction and Internal Communication in General Ledger Microservice

## Status

**Accepted**

**Date:** 2026-08-22

**Related ADRs:** ADR-0003, ADR-0005

## Purpose

Define dependency direction, internal communication, orchestration, contracts/ports, Domain Services, Domain Events, Repository access, Aggregate interaction, Module boundaries, and coupling rules inside the `General Ledger Microservice`.

This ADR applies to **internal communication within the Microservice**. Communication across Microservice boundaries is `Integration Communication`.

## Architectural Context

`General Ledger` is an independently built, tested, deployed, versioned, and scalable **Microservice**.

```text
System
└── Microservices Architecture
    └── General Ledger Microservice
        └── General Ledger Bounded Context
            ├── Account Head
            ├── Account
            └── Journal Entry ⭐ Core
```

These concepts must remain distinct:

```text
Subdomain ≠ Bounded Context ≠ Module ≠ Microservice
```

The three Subdomains are implemented as Domain-oriented Modules inside the same Runtime:

```text
General Ledger Microservice
├── Account Head Module
├── Account Module
└── Journal Entry Module
```

## Core Architecture Rules

### Dependency Direction

```text
Presentation
    │
    ▼
Application
    │
    ▼
Domain
    ▲
    │
Infrastructure
```

Allowed:

```text
Presentation → Application
Presentation → Domain
Application  → Domain
Infrastructure → Application
Infrastructure → Domain
```

Forbidden:

```text
Domain → Infrastructure
Domain → Presentation
Application → Presentation
Presentation → Infrastructure
```

**Rule:** `Domain` must not depend on technical `Infrastructure` details.

### Layer Responsibilities

```text
Application   = Use Case Orchestration
Domain        = Business Rules / Business Decisions
Repository    = Persistence Abstraction
Infrastructure = Technical Implementation
```

`Application` coordinates operations across Business Boundaries and Aggregates but must not own Business Invariants belonging to the `Domain`.

## Internal Module Communication

Internal Module communication is allowed when a Business Dependency exists, but Modules must communicate through explicit Boundaries and Contracts.

```text
Application
    │
    ├── Internal Contract / Port
    │
    └── Domain
```

A Consumer must not depend directly on another Module's internal implementation.

```text
CreateAccountUseCase
    │
    ▼
AccountHeadPort
    │
    ▼
Account Head Capability
```

Not:

```text
CreateAccountUseCase
    X
    ▼
AccountHeadJpaRepository
```

**Rule:** Internal implementation details must remain hidden from Consumers.

## Internal Contract / Port vs Repository Port

These are distinct concepts:

```text
Repository Port
└── Persistence Abstraction

Internal Contract / Port
└── Business Capability / Communication Abstraction
```

Example:

```text
AccountRepository
```

is a Persistence Port.

```text
AccountHeadPort
```

may expose an Account Head Business Capability.

**Rule:** Not every Interface in `Domain` or `Application` is a Repository.

## Repository Rules

Repository Ports define Persistence abstractions:

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

```text
Application / Domain
        │
        ▼
Repository Port
        │
        ▼
Infrastructure
        │
        ▼
Database
```

Implementations belong to `Infrastructure`.

```text
Domain
└── AccountRepository
        ▲
        │ implements
        │
Infrastructure
└── JpaAccountRepository
```

### Aggregate Repository Access

Aggregates must **not** call Repositories directly.

Forbidden:

```text
JournalEntry → AccountRepository
Account       → AccountHeadRepository
```

When data from another Aggregate is required, `Application` orchestrates loading and coordination:

```text
Application Use Case
    ├── Load Account
    ├── Load Account Head
    └── Create Journal Entry
```

## Aggregate Rules

The primary Aggregates are:

```text
AccountHead
Account
JournalEntry ⭐ Core
```

Each Aggregate owns its own Consistency Boundary and Business Invariants:

```text
AccountHead  → Account Head Invariants
Account      → Account Invariants
JournalEntry → Journal Entry Invariants
```

An Aggregate must not become responsible for another Aggregate's Invariants.

### Aggregate References

Prefer Identity/Reference over direct Object Graphs between Aggregates:

```text
Account
└── accountHeadId
```

```text
JournalEntry
└── accountId
```

Avoid:

```text
Account
└── AccountHeadEntity
```

**Rule:** Aggregates must remain independently bounded with explicit boundaries.

## Domain Service Rules

Use a `Domain Service` only when a Business Rule or Business Operation does not naturally belong to a specific `Entity`, `Value Object`, or `Aggregate`.

Valid:

```text
AccountDomainService
└── Account-specific Business Decision
```

```text
JournalEntryDomainService
└── Journal Entry-specific Domain Decision
```

A `Domain Service` must not become a generic Use Case coordinator.

Forbidden:

```text
DomainService
├── Load Account
├── Load Account Head
├── Save Account
├── Call Journal Entry
└── Execute unrelated operations
```

Correct responsibility:

```text
Domain Service
└── Business Decision
```

Not:

```text
Domain Service
└── Use Case Orchestration
```

### Domain Service and Repository

A Domain Service must not depend on Repositories merely to orchestrate cross-Module operations.

Avoid:

```text
AccountDomainService
└── AccountHeadRepository
```

Prefer:

```text
CreateAccountUseCase
├── AccountHeadPort
├── AccountDomain
└── AccountRepository
```

## Business Rule vs Orchestration

This distinction is mandatory.

### Application Orchestration

```text
CreateAccountUseCase
├── Load Account Head
├── Check required capability
├── Create Account
└── Save Account
```

### Domain Business Rules

```text
Account
├── Account code rules
├── Account status transition rules
└── Account invariants
```

Therefore:

```text
Application  = Coordination
Domain       = Business Decision
Repository   = Persistence Abstraction
Infrastructure = Technical Implementation
```

## Cross-Aggregate Business Rules

Example:

> Every Account must belong to a valid Account Head.

The Business Rule belongs to `Domain`, but this does not require the Domain to directly call `AccountHeadRepository`.

Preferred:

```text
Application
    │
    ├── Load / validate required Account Head information
    ▼
Account Domain
    │
    └── Enforce Account Business Rule
```

or:

```text
Application
    │
    ▼
AccountDomainService
    │
    └── validate(...)
```

**Rule:** Application may provide data required for a Domain decision; the Business Decision itself must remain in `Domain`.

## Internal Communication Modes

```text
Internal Communication
├── Synchronous
│   └── Internal Contract / Port
└── Asynchronous
    └── Internal Domain Event
```

Direct access to another Module's implementation is never implied by internal communication.

### Synchronous Internal Contract

Use `Internal Contract / Port` when the Use Case requires an immediate response.

Suitable when:

* Immediate response is required.
* The Use Case needs the operation result.
* Immediate consistency matters.
* A clear Business Dependency exists.

```text
CreateAccountUseCase
    │
    ▼
AccountHeadPort
    │
    ▼
Account Head Capability
    │
    ▼
Result
```

### Internal Domain Event

Use an `Internal Domain Event` when a Business Fact occurs and other internal components need to react.

```text
Account
  │
  ▼
AccountCreated
  │
  ▼
Internal Event Dispatcher
  ├── Handler A
  └── Handler B
```

The Producer should not need direct knowledge of its Consumers.

## Domain Event vs Integration Event

These concepts must remain separate.

### Internal Domain Event

```text
Account
  │
  ▼
AccountCreated
  │
  ▼
Internal Event Dispatcher
  │
  ▼
Internal Handler
```

Used **inside the same Microservice**.

### Integration Event

```text
General Ledger Microservice
    │
    ▼
Integration Event
    │
    ▼
Message Broker
    │
    ▼
Another Microservice
```

Used across a Microservice Boundary.

**Rule:**

```text
Event inside General Ledger = Internal Domain Event
Event crossing Microservice Boundary = Integration Event
```

### Event Is Not a Generic Method-Call Replacement

Do not convert every internal interaction into Event-driven communication.

Avoid:

```text
Account
  ↓
Event
  ↓
Account Head
  ↓
Event
  ↓
Journal Entry
```

If an immediate response is required, use a `Contract / Port`.

**Rule:** An Event publishes a Business Fact; it is not a generic replacement for a Method Call.

## Internal Entity Sharing

Modules must not directly consume another Module's internal Entity.

Forbidden:

```text
Journal Entry
    X
    ▼
AccountEntity
```

```text
Account
    X
    ▼
AccountHeadEntity
```

Prefer:

```text
AccountId
AccountHeadId
```

or explicit References/Contracts:

```text
AccountReference
AccountHeadReference
```

**Rule:** Each Module owns its internal model.

## Shared Database Rules

A physical shared Database does not imply shared logical ownership.

```text
General Ledger Database
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

**Rule:**

```text
Shared Database ≠ Shared Ownership
```

Each Module owns the data associated with its Business Responsibility.

A Module must not use another Module's internal Table as a Business Integration mechanism.

Forbidden:

```text
Account
  X
  ▼
SELECT FROM AccountHeadTable
```

## Direct Database Access

Direct access to another Module's internal Tables is forbidden even when all Modules use the same Database.

Forbidden:

```text
Journal Entry
    X
    ▼
Account Internal Table
```

```text
Account
    X
    ▼
Account Head Internal Table
```

Reason: direct Database access creates hidden Coupling and prevents independent Module evolution.

## Circular Dependency

Circular Dependencies between Modules must be prevented.

Forbidden:

```text
Account Head
    ↓
Account
    ↓
Account Head
```

```text
Account
    ↓
Journal Entry
    ↓
Account
```

A cycle requires review of:

* Business Boundary
* Ownership
* Dependency Direction
* Use Case Design
* Aggregate Boundary

### Business Relationship vs Code Dependency

A valid Business Relationship does not require a Circular Code Dependency.

```text
Account Head
    ↓
Account
```

may be a valid Business Relationship while Code Dependencies remain one-directional.

**Rule:** Remove Circular Dependencies from the Code/Architecture Graph without removing valid Business Relationships.

## Application Coordination Across Aggregates

`Application` may coordinate multiple Aggregates for a Use Case.

Example:

```text
CreateAccountUseCase
├── Account Head Capability
└── Account Aggregate
```

```text
PostJournalEntryUseCase
├── Account Capability
└── JournalEntry Aggregate
```

`Application` owns ordering and coordination.

Each Aggregate remains responsible for its own Business Rules and Invariants.

## Journal Entry — Core Domain Protection

`Journal Entry` is the **Core Subdomain and Core Aggregate** according to ADR-0003.

Internal Dependencies must not make `JournalEntry` depend on Account or Account Head implementations.

Preferred:

```text
PostJournalEntryUseCase
├── Account Capability
└── JournalEntry Aggregate
```

Forbidden:

```text
JournalEntry
    X
    └── AccountJpaRepository
```

```text
JournalEntry
    X
    └── AccountEntity
```

**Rule:** Core Domain must remain protected from Supporting Domain implementation details.

## External Microservice Communication

Crossing the `General Ledger Microservice` Boundary changes the communication type from Internal to Integration.

```text
Another Microservice
    │
    ▼
General Ledger Microservice
```

Use Integration mechanisms such as:

```text
REST API
Message
Integration Event
```

External Microservices must not directly access General Ledger:

* Internal Packages
* Internal Modules
* Aggregates
* Repositories
* Entities
* Database Tables

## Internal vs External Boundaries

### Internal Boundary

```text
General Ledger Microservice
├── Account Head
├── Account
└── Journal Entry
```

Communication:

```text
Internal Contract / Port
Internal Domain Event
```

### External Boundary

```text
General Ledger Microservice
    │
    ▼
Another Microservice
```

Communication:

```text
REST
Message
Integration Event
```

## Module Evolution and Possible Extraction

This ADR does **not** require one Microservice per Module.

The goal is to establish clear Boundaries first.

```text
General Ledger Microservice
    ↓
Stable Internal Module
    ↓
Business Boundary
    ↓
Operational Need
    ↓
Possible Microservice Extraction
```

A Module may later be extracted when genuine needs exist, such as:

* Independent Scaling
* Independent Deployment
* Independent Database
* Different SLA
* Independent Team Ownership
* Independent Release Cycle
* Operational Isolation

## Mandatory Architecture Rules

1. `Account Head`, `Account`, and `Journal Entry` remain inside the same `General Ledger Microservice`.
2. These Modules are not independent Microservices in the current architecture.
3. Internal Module communication is allowed when a Business Dependency exists.
4. `Application` is the primary Use Case Orchestration layer.
5. `Domain Service` owns Business Rules/Business Decisions, not generic orchestration.
6. `Domain Service` must not become a generic cross-Module coordinator.
7. Aggregates must not directly call Repositories.
8. `Domain` must not depend on `Infrastructure`.
9. A Module must not directly access another Module's Repository Implementation.
10. A Module must not directly consume another Module's internal Entity.
11. A Module must not directly access another Module's internal Database Table.
12. Synchronous internal communication must use an `Internal Contract / Port`.
13. `Internal Domain Event` may publish Business Facts inside the Microservice.
14. `Internal Domain Event` and `Integration Event` are distinct concepts.
15. Events must not be used as a generic replacement for Method Calls.
16. Circular Dependencies in the Architecture Graph are forbidden.
17. Communication with other Microservices must use an Integration Contract, API, Message, or `Integration Event`.
18. `Journal Entry` as Core Domain must not depend on Supporting Module implementations.

## Enforcement

Architecture rules should be enforced automatically, primarily using **ArchUnit**.

Required checks include:

```text
Account Domain
    must not depend on
AccountHead Infrastructure
```

```text
JournalEntry Domain
    must not depend on
Account Infrastructure
```

```text
Account Domain
    must not access
AccountHead Internal Entity
```

```text
Application
    may depend on
Internal Public Contract
```

```text
Domain
    must not depend on
Infrastructure
```

Also enforce:

* Module Boundaries
* Dependency Direction
* Forbidden Dependencies
* Circular Dependencies
* Infrastructure Isolation
* Internal Contract Usage

## Consequences

### Positive

* `Application` remains responsible for Orchestration.
* `Domain` remains responsible for Business Rules.
* `Domain` remains independent of Infrastructure.
* Aggregates retain explicit Boundaries.
* Modules collaborate without exposing internal implementations.
* Artificial Microservice decomposition is avoided.
* Internal Coupling is controlled.
* Synchronous Contracts remain available where needed.
* Domain Events remain available where appropriate.
* Shared Database does not become an Integration Coupling mechanism.
* Future Module extraction remains possible.
* `Journal Entry` remains protected as Core Domain.

### Negative

* Internal Contracts must be designed.
* Appropriate Ports must be defined.
* Boundary model mapping may be required.
* Architectural Testing is required.
* Circular Dependencies require active control.
* Direct Contract vs Domain Event must be chosen correctly.
* Application Orchestration and Domain Business Rules require explicit separation.

These costs are accepted to preserve Modularity and prevent Architecture Drift.

## Final Decision

`General Ledger` is an independent Microservice. `Account Head`, `Account`, and `Journal Entry` run within the same Microservice and Runtime as Domain-oriented Modules.

Internal collaboration is allowed when a Business Dependency exists, but a Module must not bypass another Module's Boundary through direct access to its implementation, Repository Implementation, Entity, or Database.

`Application` is the primary Use Case Orchestrator and may use multiple `Internal Contract / Port` abstractions.

`Domain` and `Aggregate` own their Business Rules and Business Invariants and must not directly invoke Repository or Infrastructure implementations.

A `Domain Service` is used only when a Business Rule or Business Operation does not naturally belong to a specific Aggregate, Entity, or Value Object. It must not become a generic coordinator.

Use:

```text
Synchronous internal communication → Internal Contract / Port
Business Fact publication → Internal Domain Event
Cross-Microservice communication → Integration mechanism / Integration Event
```

The fundamental principle is:

> **Modules may collaborate, but they must not bypass each other's Boundaries.**

Internal communication is therefore based on **Responsibility and Boundary**, not merely on the fact that Modules share a Runtime or Database.

## Final Architecture Model

```text
SYSTEM
│
└── Microservices Architecture
    │
    ▼
General Ledger Microservice
    │
    └── General Ledger Bounded Context
        │
        ├── Account Head Module
        │   └── AccountHead Aggregate
        │
        ├── Account Module
        │   └── Account Aggregate
        │
        └── Journal Entry Module ⭐ Core
            └── JournalEntry Aggregate

Application Layer
    │
    ├── Internal Contract / Port
    │   └── Synchronous
    │
    └── Internal Domain Event
        └── Asynchronous
```

External communication:

```text
General Ledger Microservice
    │
    ▼
Integration Boundary
    ├── REST
    ├── Message
    └── Integration Event
            │
            ▼
    Another Microservice
```

## Relationship with ADR-0003

```text
ADR-0003
└── Defines Domain boundaries and structure

ADR-0004
└── Defines dependency direction and communication
    inside those boundaries
```

`ADR-0003` establishes the `General Ledger Bounded Context`, its Microservice boundary, and its Modules.

`ADR-0004` defines how those Modules communicate and depend on one another within the same Microservice.

## Status and Governance

**Status: Accepted**

This ADR is the governing reference for:

* Internal Module Dependencies
* Application Orchestration
* Domain Services
* Aggregate Interaction
* Repository Access
* Internal Contracts / Ports
* Domain Events
* Shared Database Access
* Dependency Direction
* Circular Dependencies
* Inter-Module Communication

Any decision that changes these principles must be recorded through a new Architectural Decision or an official amendment to this ADR.
