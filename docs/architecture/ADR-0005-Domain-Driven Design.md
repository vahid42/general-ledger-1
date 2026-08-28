# ADR-0005 — Domain-Driven Design (DDD)  

## Status

**Accepted**

**Date:** 2026-08-22

**Related ADRs:** ADR-0003, ADR-0004, ADR-0011

---

## Purpose

General Ledger is a business-intensive financial system where complexity primarily comes from Domain concepts and Business Rules.

The system therefore follows **Domain-Driven Design (DDD)**.

### Core Principle

> The software model must represent the real Business Domain and Business Rules as directly as practical.

Technical structure must follow the Domain model, not force the Domain to conform to Database Schema, Frameworks, Persistence, or Infrastructure.

---

## DDD Scope

DDD governs:

* Domain modeling
* Ubiquitous Language
* Subdomains
* Bounded Context
* Business Boundaries
* Modules
* Entity
* Value Object
* Aggregate / Aggregate Root
* Domain Service
* Repository
* Domain Event
* Internal Contract / Port
* Business Rule placement
* Dependency direction

DDD is **not** merely the use of classes named `Entity`, `Service`, `Repository`, etc.

---

## Strategic Domain Model

```text
Business Domain
    │
    ▼
Strategic Domain Model
    ├── Subdomain
    └── Bounded Context
            │
            ▼
     Domain / Business Boundary
            │
            ▼
          Module
            │
            ▼
       Application
            │
            ▼
      Infrastructure
```

### General Ledger Structure

```text
General Ledger
├── Account Head        → Supporting Subdomain
├── Account             → Supporting Subdomain
└── Journal Entry       → Core Subdomain
        │
        ▼
General Ledger Bounded Context
        │
        ▼
General Ledger Microservice
        │
        ├── Account Head Module
        │      └── AccountHead Aggregate
        │
        ├── Account Module
        │      └── Account Aggregate
        │
        └── Journal Entry Module
               └── JournalEntry Aggregate (Core)
```

The following concepts are distinct:

```text
Subdomain ≠ Bounded Context ≠ Module ≠ Microservice
```

Current system mapping:

```text
3 Subdomains
    ↓
1 Bounded Context
    ↓
1 Microservice
    ↓
3 Domain-oriented Modules
    ↓
3 Aggregate Roots
```

This mapping is a **system-specific architectural decision**, not a general DDD rule.

---

## Subdomains

### Account Head

**Supporting Subdomain**

Responsible for:

* Account Head structure
* Classification
* Hierarchy
* Account placement within the General Ledger structure
* Account Head structural rules

### Account

**Supporting Subdomain**

Responsible for:

* Account Identity
* Account Type
* Account Status
* Account Attributes
* Account-specific Business Rules

### Journal Entry

**Core Subdomain**

Primary source of Business Complexity and Business Value.

Responsible for:

* Journal Entry creation
* Debit / Credit validation
* Balancing
* Journal Line validation
* Posting Rules
* Posting State
* Posting

> Journal Entry receives the highest level of Domain Modeling attention.

---

## Bounded Context

`Bounded Context` defines the boundary of the Domain Model and Ubiquitous Language.

Current context:

```text
General Ledger Bounded Context
├── Account Head
├── Account
└── Journal Entry
```

The three Subdomains remain inside one Bounded Context because:

* They belong to the same Problem Space.
* Their Ubiquitous Language is closely related.
* Account and Account Head support the Journal Entry model.
* Separate Contexts currently do not represent meaningful model boundaries.
* Separate Contexts would add unnecessary complexity.

Internal Business Boundaries still exist between Modules.

> Bounded Context and Microservice are conceptually different.

Current deployment mapping:

```text
General Ledger Bounded Context
        ↓
General Ledger Microservice
```

---

## Module Boundary

Modules are organized by Business Responsibility:

```text
General Ledger Microservice
├── account-head
├── account
└── journal-entry
```

Modules:

* Are not Microservices.
* Have no independent Runtime.
* Have no independent Deployment.
* Execute inside the same Microservice.
* Own their internal Domain Model.
* Must maintain explicit boundaries.
* Exist to preserve Cohesion and reduce Coupling.

```text
Module ≠ Microservice
Module ≠ Bounded Context
```

---

## Ubiquitous Language

Business terms must retain consistent meaning across:

* Documentation
* ADRs
* Domain Model
* Code
* Classes
* Methods
* APIs
* Tests
* Events

Canonical terms include:

```text
Account
AccountHead
JournalEntry
Debit
Credit
Balance
Currency
Branch
Posting
```

A single Business Concept must not receive unrelated names in different parts of the system.

Ubiquitous Language is defined within the consistency boundary of the same Bounded Context.

---

## Entity

Use `Entity` for concepts with independent Identity.

An Entity must:

* Have explicit Identity.
* Remain identifiable over time.
* Own relevant behavior.
* Preserve its own Invariants.

Example:

```text
Account
├── AccountId
├── AccountCode
├── Name
├── Type
└── Status
```

Entity must not be reduced to a Data Container.

Preferred:

```text
Account
├── Behavior
├── Business Rules
└── Invariants
```

Avoid:

```text
Account
└── Getters / Setters only
```

Business behavior belongs where its Business Ownership exists.

---

## Value Object

Use `Value Object` for concepts without independent Identity whose meaning is determined by Value.

Potential examples:

```text
Money
Currency
AccountCode
BranchId
AccountId
```

Example:

```text
Money
├── amount
└── currency
```

Value Objects should preferably:

* Be Immutable.
* Use Value-based Equality.
* Preserve their own Value validation.
* Own behavior related to their concept.

---

## Aggregate

`Aggregate` is a **Consistency Boundary**.

An Aggregate groups objects whose Invariants must be maintained as one consistency unit.

Every Aggregate has an:

```text
Aggregate Root
```

External access to internal Entities must occur through the Aggregate Root.

Aggregate boundaries must be determined by:

```text
Business Invariant
+
Consistency Requirement
```

They must **not** be determined merely by:

* Database Tables
* Foreign Keys
* ORM Relationships
* Object Graphs

---

## General Ledger Aggregates

```text
General Ledger
├── AccountHead Aggregate
├── Account Aggregate
└── JournalEntry Aggregate (Core)
```

### AccountHead Aggregate

Owns Invariants related to Account Head structure and rules.

### Account Aggregate

Owns Invariants related to Account.

### JournalEntry Aggregate

The **Core Aggregate**.

Owns rules related to:

```text
JournalEntry
├── Debit / Credit rules
├── Balancing rules
├── Journal Line rules
├── Posting rules
├── Posting state
└── Posting behavior
```

> Core Business Rules for Journal Entry must remain in the `JournalEntry` Aggregate and related Domain Model whenever naturally possible.

---

## Aggregate Interaction

Aggregates must remain as independent as practical.

An Aggregate must not:

* Hold another Aggregate as an internal Object Graph.
* Directly call another Aggregate's Repository.
* Depend on another Aggregate's Infrastructure.
* Consume another Aggregate's internal Entity.

Prefer Identity / Reference:

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

Use **Reference by Identity** across Aggregate boundaries.

---

## Domain Behavior Placement

Preferred decision hierarchy:

```text
Business Rule
    │
    ├── Entity
    ├── Value Object
    ├── Aggregate
    └── Domain Service
```

Business behavior should first belong to the Domain object that naturally owns it.

Do not automatically create:

```text
AccountService
AccountHeadService
JournalEntryService
```

as generic containers for Business Logic.

---

## Domain Service

Use `Domain Service` only when a real Business Rule or Business Operation:

* Does not naturally belong to an Entity.
* Does not naturally belong to a Value Object.
* Does not naturally belong to an Aggregate.
* Is still part of the actual Business Domain.

A Domain Service must:

* Have explicit Business Meaning.
* Represent a specific Business Decision or Domain Operation.
* Be Stateless or as independent from Infrastructure as practical.
* Remain independent from technical concerns.

Avoid generic:

```text
GenericService
CommonService
Manager
Coordinator
```

A Domain Service must not become a general-purpose Orchestrator.

---

## Application Service / Use Case

`Application Layer` executes Use Cases and performs orchestration.

Responsibilities:

* Execute Use Case.
* Control Flow.
* Coordinate multiple Aggregates.
* Invoke Repository Ports.
* Invoke Internal Contract / Ports.
* Define Transaction Boundary.
* Coordinate operations.

```text
Presentation
    │
    ▼
Application / Use Case
    ├──► Aggregate
    ├──► Repository Port
    ├──► Internal Contract / Port
    └──► Domain Service
```

### Core Rule

> **Application Orchestrates; Domain Decides.**

---

## Application Service vs Domain Service

```text
Application Service
├── Use Case
├── Orchestration
├── Transaction Boundary
├── Coordination
└── Port invocation
```

```text
Domain Service
└── Business Rule / Business Decision
```

Therefore:

```text
Application = Coordination
Domain      = Business Decision
```

A Domain Service must not be created merely to connect Modules.

---

## Example: Create Account under Account Head

```text
CreateAccountUseCase
    │
    ├──► AccountHeadPort
    │       └── provide required capability / validation
    │
    ├──► Account Domain
    │       └── enforce Account Business Rules
    │
    └──► AccountRepository
            └── Persistence
```

Responsibilities:

```text
Presentation
    = Request Handling

Application
    = Use Case Orchestration

AccountHead
    = Account Head Business Responsibility

Account
    = Account Business Responsibility

Repository
    = Persistence Abstraction

Infrastructure
    = Technical Implementation
```

---

## Business Rule vs Orchestration

Example Rule:

> An Account must belong to a valid Account Head.

`Account` must not directly access `AccountHeadRepository` merely because this Rule exists.

Preferred:

```text
Application
    │
    ├──► AccountHeadPort
    │
    ▼
Account Domain
    │
    └── Enforce Account Business Rule
```

> Application may provide the data or capability required for a Domain decision, but the Business Decision itself remains in the Domain.

---

## Repository

`Repository` is a Persistence Abstraction for an Aggregate.

Examples:

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

Architecture:

```text
Domain / Application
        │
        ▼
Repository Port
        ▲
        │ implements
        │
Infrastructure
        │
        ▼
Database
```

Example:

```text
Domain
└── AccountRepository

Infrastructure
└── JpaAccountRepository
```

Domain must not depend on:

```text
JPA
Hibernate
Spring Data
Database Driver
SQL
```

Repository Implementation belongs to Infrastructure.

---

## Repository ≠ Internal Contract / Port

These concepts must remain separate.

### Repository Port

```text
Repository Port
└── Persistence Abstraction
```

### Internal Contract / Port

```text
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

may represent a Business Capability exposed to another Module.

> Every Interface in Domain or Application is not necessarily a Repository.

---

## Repository Is Not an Integration Mechanism

A Module's Repository must not be used as a generic communication mechanism with another Module.

Avoid:

```text
AccountDomainService
    │
    ▼
AccountHeadRepository
```

when the purpose is cross-Module coordination.

Preferred:

```text
CreateAccountUseCase
    ├──► AccountHeadPort
    ├──► Account Domain
    └──► AccountRepository
```

> Repository = Persistence.
> Internal Contract / Port = Business Capability / Internal Communication.

---

## Domain Events

A `Domain Event` represents a meaningful **Business Fact**.

Examples:

```text
AccountCreated
JournalEntryPosted
```

Avoid technical events such as:

```text
DatabaseUpdated
EntitySaved
RowChanged
```

Basic model:

```text
Aggregate
    │
    ▼
Domain Event
    │
    ▼
Event Handler
```

---

## Internal Domain Event

Internal Domain Events may be used inside the General Ledger Microservice.

```text
Account
    │
    ▼
AccountCreated
    │
    ▼
Internal Event Dispatcher
    ├──► Handler A
    └──► Handler B
```

Producer should not need direct knowledge of Consumers.

---

## Internal Domain Event ≠ Integration Event

### Internal Domain Event

Used inside the same Microservice:

```text
General Ledger Microservice
    │
    ▼
Internal Domain Event
    │
    ▼
Internal Handler
```

### Integration Event

Used across Microservice boundaries:

```text
General Ledger Microservice
    │
    ▼
Integration Event
    │
    ▼
Message Broker / Integration Boundary
    │
    ▼
Another Microservice
```

Therefore:

* Internal Domain Event = communication inside the Microservice.
* Integration Event = communication across Microservice boundaries.

---

## Direct Contract vs Domain Event

Use `Internal Contract / Port` when:

* Immediate response is required.
* The Use Case needs the result.
* Dependency is direct Business Capability dependency.
* Immediate Consistency matters.

Example:

```text
CreateAccountUseCase
    │
    ▼
AccountHeadPort
    │
    ▼
isValid(accountHeadId)
```

Use `Domain Event` when:

* A meaningful Business Fact occurred.
* Multiple Consumers may react.
* Producer should not directly know Consumers.
* Processing can be independent.

> Event is for publishing a Business Fact, not a universal replacement for Method Calls.

---

## Module Domain Ownership

Every Module owns its internal Domain Model.

Avoid:

```text
Account
    X
    ▼
AccountHeadEntity
```

Preferred:

```text
Account
└── AccountHeadId
```

or:

```text
Application
    │
    ▼
AccountHeadPort
```

Goal:

> Each Module must be able to evolve its internal model without causing cascading changes in other Modules.

---

## Shared Database ≠ Shared Domain Ownership

The Microservice may use a shared Database:

```text
General Ledger Database
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

This does **not** imply shared Domain ownership.

Each Module remains the logical owner of:

* Its Domain Model.
* Its Business Responsibility.
* Its internal data representation.

Business integration must not occur through another Module's internal Tables.

---

## Direct Database Access

Modules must not directly consume another Module's internal Tables.

Forbidden:

```text
Journal Entry Module
    X
    ▼
Account Internal Table
```

```text
Account Module
    X
    ▼
Account Head Internal Table
```

Direct Table access causes:

* Hidden Coupling
* Reduced Encapsulation
* Architecture Drift
* Difficult Evolution

---

## Dependency Direction

DDD must remain aligned with ADR-0004.

Preferred:

```text
Presentation
    │
    ▼
Application
    ├──► Internal Contract / Port
    ├──► Repository Port
    └──► Domain
```

Infrastructure:

```text
Infrastructure
├── implements Repository Port
└── implements technical adapters
```

Forbidden:

```text
Domain
    X
    ▼
Infrastructure
```

```text
AccountDomain
    X
    ▼
AccountHeadJpaRepository
```

### Core Rule

> Dependencies must point toward Abstractions and Business Responsibilities, not Implementations or Infrastructure.

---

## Domain Independence from Persistence

Domain Model must not be derived merely from Database Schema.

Avoid:

```text
Database Table
    │
    ▼
Domain Entity
```

Preferred:

```text
Business Model
    │
    ▼
Domain Model
    │
    ▼
Persistence Model
    │
    ▼
Database
```

Domain Model and Persistence Model should be separated when their responsibilities differ.

> Changing Persistence Model must not necessarily require changing the Business Model.

---

## Mapping

Mapping is appropriate at architectural boundaries when models genuinely differ.

```text
Presentation DTO
    │
    ▼
Application Input
    │
    ▼
Domain Model
```

```text
Domain Model
    │
    ▼
Persistence Model
    │
    ▼
Database
```

Internal Business Boundaries may expose independent Contracts tailored to Consumers.

> An internal model must not be exposed to another Boundary merely to simplify Mapping.

---

## Circular Dependency

A Business Relationship does not imply a bidirectional Code Dependency.

Example:

```text
Account Head
    │
    ▼
Account
```

may be a valid Business Relationship while Code Dependency remains one-directional.

Circular dependencies must trigger review of:

* Business Ownership
* Boundary
* Aggregate Boundary
* Contract
* Event
* Dependency Direction

Avoid:

```text
Account
    ↓
Journal Entry
    ↓
Account
```

or:

```text
Account Head
    ↓
Account
    ↓
Account Head
```

---

## Journal Entry as Core Domain

`Journal Entry` is both:

* Core Subdomain
* Core Aggregate

Core Domain must be protected from Supporting Subdomain implementation details.

Preferred:

```text
PostJournalEntryUseCase
    ├──► Account Capability
    └──► JournalEntry Aggregate
```

Forbidden:

```text
JournalEntry
    X
    └──► AccountJpaRepository
```

```text
JournalEntry
    X
    └──► AccountEntity
```

Core Domain must not depend on internal implementations of Supporting Modules.

---

## Internal Module Communication

DDD does not prohibit communication between Modules inside the General Ledger Microservice.

What is prohibited is **Boundary Bypass**.

Preferred:

```text
General Ledger Microservice
└── General Ledger Bounded Context
    ├── Account Head Module
    ├── Account Module
    └── Journal Entry Module
            │
            ▼
      Application / Use Case
            │
       ┌────┴────┐
       ▼         ▼
Internal      Internal
Contract /    Domain Event
Port
```

> Modules may collaborate, but must not bypass each other's Boundaries.

---

## Core DDD Rules

1. Business Rules must reside in the Domain.
2. Domain must not depend on Infrastructure.
3. Entity owns its relevant behavior and Invariants.
4. Value Object is used for concepts without independent Identity.
5. Aggregate defines a Consistency Boundary.
6. External access to internal Aggregate Entities occurs through Aggregate Root.
7. Aggregates remain independent as practical.
8. Aggregate interaction preferably uses Identity / Reference.
9. Repository Port provides Persistence Abstraction.
10. Repository must not be used as a generic Module Integration mechanism.
11. Internal Contract / Port is distinct from Repository Port.
12. Domain Service is used only for genuine Domain Behavior.
13. Application Service executes Use Cases and orchestration.
14. Domain Service represents Business Rules / Decisions.
15. Domain Service must not become a generic Coordinator.
16. Domain Event represents a meaningful Business Fact.
17. Internal Domain Event differs from Integration Event.
18. Event is not a universal replacement for Method Calls.
19. Domain Model must not merely mirror Database Schema.
20. Internal Entity of one Module must not be directly consumed by another Module.
21. Repository Implementation must not be consumed by another Module.
22. Internal Tables must not be directly consumed by another Module.
23. Circular Code / Architecture Dependencies must be prevented.
24. Module Boundaries follow Business Responsibility and Domain Boundaries.
25. Journal Entry Core Domain must remain independent from Supporting Module implementations.
26. DDD must not be used to create unnecessary technical structures or Over-Engineering.

---

## Agent / Architecture Enforcement

AI Agents, Architectural Tests, and Code Review must enforce these constraints.

Forbidden examples:

```text
Domain
    → Infrastructure
```

```text
Account Domain
    → AccountHeadJpaRepository
```

```text
Account
    → AccountHeadEntity
```

```text
JournalEntry Domain
    → Account Infrastructure
```

```text
Module A
    → Module B internal Table
```

Allowed patterns:

```text
Application
    → Internal Contract / Port
```

```text
Application
    → Repository Port
```

```text
Infrastructure
    → implements Repository Port
```

```text
Aggregate
    → Domain Event
```

`Domain Service` must not become a Cross-Module Orchestrator.

Architectural enforcement should cover:

* Module Boundaries
* Dependency Direction
* Forbidden Dependencies
* Circular Dependencies
* Infrastructure Isolation
* Repository Ownership
* Internal Contract Usage
* Aggregate Boundaries

Use `ArchUnit` and Architecture Code Review where practical.

---

## Agent Instructions

When analyzing, designing, reviewing, or modifying General Ledger architecture:

1. Treat Business Domain as the primary source of design decisions.
2. Preserve Ubiquitous Language.
3. Do not confuse `Subdomain`, `Bounded Context`, `Module`, and `Microservice`.
4. Assume the current General Ledger contains three Subdomains inside one Bounded Context.
5. Treat `Journal Entry` as the Core Subdomain and Core Aggregate.
6. Keep `Account Head` and `Account` as Supporting Subdomains.
7. Keep each Module responsible for its own internal Domain Model.
8. Never access another Module's internal Entity, Repository Implementation, Infrastructure, or Table directly.
9. Use `Internal Contract / Port` for direct Business Capability communication.
10. Use `Repository Port` only for Persistence Abstraction.
11. Use `Internal Domain Event` for meaningful internal Business Facts.
12. Use `Integration Event` only across Microservice boundaries.
13. Keep Business Decisions inside Domain.
14. Keep Use Case orchestration inside Application.
15. Do not move Business Rules into generic Application Services.
16. Do not turn Domain Services into generic Coordinators.
17. Keep Domain independent from Infrastructure and Persistence technology.
18. Determine Aggregate boundaries from Invariants and Consistency Requirements.
19. Prefer Identity / Reference between Aggregates.
20. Reject circular Dependencies.
21. Protect Core Domain from Supporting Module implementations.
22. Reject solutions that introduce DDD patterns without actual Business responsibility.
23. Prefer the simplest model that correctly represents the Domain.
24. If a proposed architectural change violates this ADR, require an architectural decision or formal amendment to this ADR.

---

## Architectural Model

```text
                         GENERAL LEDGER
                           Problem Space
                                │
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
       Account Head          Account          Journal Entry
       Supporting           Supporting             CORE
       Subdomain            Subdomain            Subdomain
             │                  │                  │
             └──────────────────┼──────────────────┘
                                │
                                ▼
                  General Ledger Bounded Context
                                │
                                ▼
                  General Ledger Microservice
                                │
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
       Account Head          Account          Journal Entry
          Module               Module              Module
             │                  │                  │
             ▼                  ▼                  ▼
      AccountHead            Account          JournalEntry
       Aggregate            Aggregate          Aggregate
                                                    CORE
                                │
                                ▼
                       Application Layer
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
           Internal Contract / Port   Internal Domain Event
                    │
                    ▼
                  Domain
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
       Entity   Aggregate   Domain Service
          │         │         │
          └─────────┼─────────┘
                    │
                    ▼
             Repository Port
                    │
                    ▼
              Infrastructure
                    │
                    ▼
                Persistence
```

---

## Key Principle

> DDD in General Ledger means Business Domain is the primary source of design. Business Rules remain in Domain, Aggregates own their Invariants, Application owns orchestration, Repository provides Persistence Abstraction, and Infrastructure provides technical implementations.

> Modules own their internal models and collaborate only through explicit Boundaries and Contracts. They may communicate, but must not bypass each other's Boundaries.

> `Journal Entry` is the Core Domain and must remain protected from Supporting Module internals and Infrastructure.

---

## Architectural Decision Summary

```text
ADR-0003
    = Domain / Bounded Context / Microservice Structure

ADR-0004
    = Dependency Direction / Internal Communication

ADR-0005
    = DDD Principles / Tactical Domain Modeling
```

Combined model:

```text
Business Domain
    │
    ▼
Subdomain
    │
    ▼
Bounded Context
    │
    ▼
Module
    │
    ▼
Aggregate
    │
    ▼
Entity / Value Object / Domain Service
```

---

## Consequences

### Positive

* Clearer Business Model.
* Explicit Business Rule ownership.
* Protection of Core Domain from technical details.
* `Journal Entry` remains the Core Domain.
* Explicit Aggregate Boundaries.
* Clear separation between Application and Domain responsibilities.
* Repository remains separate from Integration.
* Controlled Module Coupling.
* Persistence can evolve without unnecessary Domain changes.
* Consistent Ubiquitous Language.
* Independent evolution of Module internals.
* Reduced risk of generic, oversized Domain Services.
* Future Module extraction remains possible.

### Negative

* Higher initial analysis complexity.
* Requires deeper Domain understanding.
* More Models / Classes may be required.
* Additional Mapping may be required.
* Internal Contracts require maintenance.
* More Architectural Testing is required.
* Correct Aggregate Boundaries require experience and analysis.
* Incorrect DDD usage can cause Over-Engineering.

> DDD must match actual Business complexity and must not be used merely to increase the number of layers or classes.

---

## Final Decision

General Ledger is designed according to DDD principles, with the Domain Model as the primary source of Business Concepts and Business Rules.

The current Strategic DDD structure is:

```text
Account Head → Supporting Subdomain
Account      → Supporting Subdomain
Journal Entry → Core Subdomain
```

All three exist inside one:

```text
General Ledger Bounded Context
```

which is deployed as:

```text
General Ledger Microservice
```

The Tactical DDD structure contains:

```text
AccountHead Aggregate
Account Aggregate
JournalEntry Aggregate → Core Aggregate
```

`Entity`, `Value Object`, `Aggregate`, `Domain Service`, `Repository`, and `Domain Event` must be introduced only according to actual Business Responsibility.

```text
Application
    → Use Case / Orchestration

Domain
    → Business Rules / Decisions

Repository Port
    → Persistence Abstraction

Infrastructure
    → Technical Implementation
```

Internal Module communication must use explicit:

```text
Internal Contract / Port
```

or:

```text
Internal Domain Event
```

and must never directly expose:

```text
Internal Entity
Repository Implementation
Infrastructure
Internal Table
```

Any architectural decision that materially changes these principles must be recorded through a new Architectural Decision or a formal amendment to this ADR.

---

## Status

**Accepted**

This ADR is authoritative for:

* Domain Modeling
* Ubiquitous Language
* Entity
* Value Object
* Aggregate
* Aggregate Boundary
* Domain Service
* Application Service
* Repository
* Domain Event
* Internal Contract / Port
* Module Boundary
* Business Rule Placement
* Domain Dependency
* Core Domain Protection
