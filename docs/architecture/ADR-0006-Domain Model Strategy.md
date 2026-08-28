# ADR-0006 — Domain Model Strategy — Ultra Compact

**Status:** Accepted
**Date:** 2026-08-22
**Related ADRs:** ADR-0003, ADR-0004, ADR-0005

## Purpose

Define the Domain Model strategy for the General Ledger Microservice, including:

* Business Behavior and Business Rule ownership.
* Entity, Value Object, Aggregate, Aggregate Root, Domain Service, Domain Event, and Domain Policy / Specification usage.
* Aggregate Boundaries and Invariants.
* Repository and Internal Contract / Port responsibilities.
* Application vs Domain responsibilities.
* Persistence and Framework independence.
* Protection of the Core `JournalEntry` Domain Model.

The system contains one `General Ledger Bounded Context` implemented as one `General Ledger Microservice`.

Current Subdomains:

```text
General Ledger
├── Account Head
├── Account
└── Journal Entry ⭐ Core
```

Current Aggregate Roots:

```text
AccountHead
Account
JournalEntry ⭐ Core
```

## Core Decision

The system uses a **Rich Domain Model**.

Domain Model must contain not only State but also Business Behavior, Business Rules, and Invariants according to Business Ownership.

```text
Domain Model
├── Entity
├── Value Object
├── Aggregate
│   └── Aggregate Root
├── Domain Service
├── Domain Event
└── Domain Policy / Specification
```

**Rich Domain Model does not mean putting all logic into Entities.**

The governing rule is:

> A Business Rule must be placed in the nearest Domain Concept that actually owns and can guarantee that rule.

Possible owners:

```text
Business Rule
    │
    ├── Entity
    ├── Value Object
    ├── Aggregate
    └── Domain Service
```

`Application` orchestrates; `Domain` decides.

---

## 1. Domain Model Structure

```text
General Ledger Bounded Context
        │
        ▼
General Ledger Microservice
        │
 ┌──────┼───────────────┐
 ▼      ▼               ▼
AccountHead          Account       JournalEntry
Module               Module        Module ⭐ Core
 │                     │              │
 ▼                     ▼              ▼
AccountHead           Account      JournalEntry
Aggregate             Aggregate    Aggregate
```

ADR-0006 extends the decisions of ADR-0003/0004/0005 and must not redefine their established boundaries.

---

## 2. Entity Rules

An `Entity` has independent `Identity`.

Example:

```text
Account
├── AccountId
├── Name
├── Status
├── Type
└── Business Behavior
```

Entities with different identities remain different even when State is identical:

```text
Account(1) ≠ Account(2)
```

Entity must own relevant Behavior and Invariants within its Business Responsibility.

### Entity Behavior

Preferred:

```java
account.close();
account.activate();
account.changeName(name);
```

Not preferred as the primary Domain API:

```java
accountService.close(account);
accountService.activate(account);
accountService.changeName(account, name);
```

Exception: behavior may belong to `Domain Service` when it:

* spans multiple Aggregates;
* has no natural Entity owner;
* represents an independent Domain Concept.

**Behavior placement is determined by Business Ownership.**

### Public Setter

Public Setter must not be the primary mechanism for changing Business State.

Not preferred:

```java
account.setStatus(CLOSED);
```

Preferred:

```java
account.close();
```

Domain methods should enforce relevant state and Business Rules.

This is not an absolute ban on all setters; it means Business State should normally change through meaningful Domain behavior.

### Creation

Entity must not be created in an invalid State.

Allowed creation mechanisms:

* Constructor
* Factory Method
* Factory

Preferred examples:

```text
Account.open(...)
Account.create(...)
```

Avoid:

```text
new Account()
 ├── setName(...)
 ├── setStatus(...)
 └── setType(...)
```

Creation must enforce creation-time Invariants.

---

## 3. Value Object Rules

Use `Value Object` for concepts without independent Identity whose meaning is determined by Value.

Potential General Ledger examples:

```text
Money
Currency
AccountCode
BranchCode
```

A Value Object should preferably be:

* Immutable.
* Equality-by-Value.
* Without independent Identity.
* Responsible for behavior and validation belonging to its concept.

Example:

```text
Money
├── amount
├── currency
├── add()
├── subtract()
└── multiply()
```

Value Object is not merely a collection of primitives.

### Immutability

Value Objects should be Immutable whenever practical.

Preferred:

```text
Money(100, IRR)
    │
    └── add(50)
          ▼
      Money(150, IRR)
```

The original object is not mutated.

---

## 4. Aggregate Strategy

An `Aggregate` is a **Consistency Boundary**.

Every Aggregate has an `Aggregate Root`.

Current General Ledger Aggregates:

```text
AccountHead Aggregate
Account Aggregate
JournalEntry Aggregate ⭐ Core
```

Aggregate Root is the external access point for internal Entities and Value Objects.

```text
External Consumer
       │
       ▼
Aggregate Root
       │
       ▼
Internal Entities / Value Objects
```

Every Aggregate must protect its own Invariants.

### Aggregate Boundary

Boundary must be determined by:

* Business Invariant.
* Consistency Requirement.
* Transactional Consistency.

Not by:

* Database Table.
* Foreign Key.
* ORM Relationship.
* Object Graph.

```text
Business Invariant
       │
       ▼
Consistency Requirement
       │
       ▼
Aggregate Boundary
```

A relationship between two Entities does not imply that they belong to the same Aggregate.

### Aggregate Size

Aggregate should be the **minimum effective Consistency Boundary** required to enforce its Invariants.

```text
Required Invariants
+
Required Consistency
        │
        ▼
Minimum Effective Aggregate
```

Do not enlarge Aggregates merely for Query or Navigation convenience.

Avoid:

```text
Huge Aggregate
+
Many Entities
+
High Contention
+
Unnecessary Coupling
```

---

## 5. General Ledger Aggregates

### AccountHead Aggregate

Owns Invariants and Business Rules related to Account Head:

```text
AccountHead
├── identity
├── hierarchy
├── classification
└── account-head rules
```

### Account Aggregate

Owns Account-related Invariants and Business Rules:

```text
Account
├── identity
├── type
├── status
└── account rules
```

### JournalEntry Aggregate ⭐ Core

`JournalEntry` is the Core Aggregate and owns the central General Ledger Business Logic:

```text
JournalEntry
├── journal lines
├── debit / credit rules
├── balancing rules
├── posting rules
├── posting state
└── posting behavior
```

Core Business Logic must remain Domain-centric and must not be scattered across Controller, Application Service, Repository, Infrastructure, or Database Trigger.

Conceptual behavior may include:

```text
addLine()
validateBalance()
validatePostingRules()
post()
state transition
```

Actual methods must be based on real Business Rules.

---

## 6. Aggregate Interaction

Aggregates must remain as independent as practical.

An Aggregate must not:

* Directly call another Aggregate's Repository.
* Hold another Aggregate's internal Entity.
* Depend on another Aggregate's Persistence Implementation.
* Depend on another Module's Infrastructure implementation.

Prefer Identity/Reference or an appropriate Contract:

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

```text
JournalEntry
└── AccountEntity
```

### Core Aggregate Protection

`JournalEntry` must not depend on internal implementations of Supporting Modules.

Forbidden:

```text
JournalEntry Domain
        X
        ▼
AccountJpaRepository
```

```text
JournalEntry
        X
        ▼
AccountEntity
```

Preferred:

```text
PostJournalEntryUseCase
        │
        ├──► Account Capability
        │
        └──► JournalEntry Aggregate
```

`JournalEntry` must depend on Account capability/contracts, not Account implementation details.

---

## 7. Repository Strategy

Repository is defined for an `Aggregate Root`.

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

Architecture:

```text
Aggregate Root
      │
      ▼
Repository Port
      │
      ▼
Infrastructure Implementation
```

Do not create separate Repositories for internal Entities unless that Entity is actually an independent Aggregate Root.

Preferred:

```text
JournalEntry Aggregate
       │
       ▼
JournalEntryRepository
```

Not:

```text
JournalLine
       │
       ▼
JournalLineRepository
```

unless `JournalLine` is explicitly modeled as an independent Aggregate Root.

---

## 8. Repository Port vs Internal Contract / Port

These are different abstractions.

```text
Repository Port
└── Persistence Abstraction
```

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

may represent a Business Capability Contract.

**Not every Domain/Application Interface is a Repository.**

### Repository Is Not an Integration Mechanism

Do not use another Module's Repository as a general communication mechanism.

Forbidden:

```text
Account Module
      X
      ▼
AccountHeadRepository
```

If the purpose is a Business Capability, use the appropriate Contract:

```text
Application
    │
    ▼
AccountHeadPort
```

Rule:

> Repository = Persistence.
> Internal Contract / Port = Business Capability / internal communication.

---

## 9. Persistence Independence

Domain Model must not be designed from Persistence Model or Database Schema.

Preferred dependency:

```text
Business Model
      │
      ▼
Domain Model
      │
      ▼
Persistence Mapping
      │
      ▼
Database
```

Not:

```text
Database Schema
      │
      ▼
JPA Entity
      │
      ▼
Domain Model
```

Changing Persistence Model should have minimal impact on Business Model.

### Domain Entity vs Persistence Entity

They may be separated:

```text
Domain
└── Account
```

```text
Infrastructure
└── AccountJpaEntity
```

with:

```text
Account
   │
   ▼
AccountMapper
   │
   ▼
AccountJpaEntity
```

Separation is valuable when there is a real architectural difference between Domain and Persistence Models.

### Shared Domain/Persistence Model

A single model is acceptable when:

* Domain is simple.
* Persistence requirements do not conflict with Domain.
* Framework Leakage is controlled and acceptable.
* Technical annotations do not distort the Business Model.
* Coupling remains appropriate.

Therefore:

> Separate Domain/Persistence Models are not mandatory; Business Model Independence is the architectural principle.

Never sacrifice Domain design merely for Persistence convenience.

---

## 10. Domain Service

Use `Domain Service` only when a Business Rule/Operation:

* Does not naturally belong to a specific Entity.
* Does not naturally belong to a specific Aggregate.
* Does not have an appropriate Value Object owner.
* Still represents a real Domain concept.

Example:

```text
TransferPolicy
```

or a genuine Business Decision spanning multiple Aggregates.

Do not create generic:

```text
EverythingService
GenericService
Manager
Coordinator
```

A Domain Service must represent a specific Business Concept.

### Domain Service vs Application Service

```text
Domain Service
└── Business Rule / Business Decision
```

```text
Application Service
├── Use Case
├── Orchestration
├── Coordination
├── Transaction Boundary
├── Repository Port
└── Internal Contract / Port
```

**Application orchestrates; Domain decides.**

Application Service must not become the owner of Core Domain Business Rules.

---

## 11. Application Service

Application Service executes Use Cases and coordinates Domain components.

Example:

```text
CreateAccountUseCase
        │
        ├──► AccountHeadPort
        ├──► Account Domain
        └──► AccountRepository
```

Application may:

* Load Aggregates.
* Request required capabilities.
* Coordinate multiple Aggregates.
* Control Transaction Boundary.
* Call Repository Ports.
* Call Domain Services when required.

Application must not own Business Decisions that belong to Domain.

### Example: Create Account Under Account Head

```text
CreateAccountUseCase
        │
        ├──► AccountHeadPort
        │       └── validate / resolve required capability
        │
        ├──► Account Domain
        │       └── enforce Account rules
        │
        └──► AccountRepository
                └── Persistence
```

Responsibilities:

```text
Application
= Use Case Orchestration

Account
= Account Business Responsibility

AccountHead
= Account Head Business Responsibility

Repository
= Persistence
```

---

## 12. Validation vs Business Invariant

Separate technical/input validation from Business Invariants.

### Input Validation

Examples:

```text
required field
format
length
syntax
```

May be handled at Presentation/Application boundaries.

### Business Invariant

Examples:

```text
Account cannot be closed in this state
JournalEntry must be balanced
Posting rules must be satisfied
```

Must be enforced by Domain.

```text
Input Validation
      │
      ▼
Application Boundary
```

```text
Business Invariant
      │
      ▼
Domain
```

Controller must not own Business Invariants.

### Invariant Placement

Invariant belongs in the smallest Domain Boundary capable of owning and guaranteeing it.

Examples:

```text
JournalEntry must be balanced
```

must be enforced by `JournalEntry Aggregate`.

A Business Decision naturally spanning multiple Aggregates may belong to a Domain Service.

---

## 13. Domain Events

`Domain Event` represents a meaningful Business Fact that has occurred.

Examples:

```text
AccountOpened
JournalEntryPosted
```

Domain Events must preferably be:

* Immutable.
* Expressed as completed Business Facts.
* Named using meaningful Domain language.
* Decoupled from Consumers.

Prefer:

```text
AccountOpened
```

over:

```text
OpenAccountEvent
```

### Internal Domain Event vs Integration Event

These are different concepts and Boundaries.

Internal:

```text
Aggregate
    │
    ▼
Internal Domain Event
    │
    ▼
Internal Handler
```

Integration:

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

Internal Domain Event and Integration Event must not be treated as equivalent.

### Event vs Method Call

Use an Event to publish a Business Fact.

If a Use Case needs an immediate result to continue a decision, use an `Internal Contract / Port`.

Synchronous capability:

```text
CreateAccountUseCase
        │
        ▼
AccountHeadPort
        │
        ▼
Capability Result
```

Business Fact:

```text
AccountCreated
        │
        ▼
Internal Event Handler
```

**Event is not a generic replacement for Method Call.**

---

## 14. DTO Boundary

DTO is not part of the Domain Model.

Preferred flow:

```text
Presentation DTO
       │
       ▼
Application Input / Command
       │
       ▼
Domain Model
```

Do not use API DTO as Domain Entity.

Not preferred:

```text
CreateAccountRequest
       │
       ▼
Account Entity
```

Preferred:

```text
CreateAccountRequest
       │
       ▼
CreateAccountCommand
       │
       ▼
Account.open(...)
```

DTO represents its own Boundary requirements, not the internal structure of Domain.

---

## 15. Database Independence

Database Schema must not dictate Domain structure.

Example Database:

```text
ACCOUNT
ACCOUNT_DETAIL
ACCOUNT_STATUS
```

does not require the Domain to mirror those structures.

Domain should represent Business Concepts:

```text
Account
├── identity
├── state
├── behavior
└── invariants
```

Persistence Layer maps Domain to Database.

---

## 16. Preventing Anemic Domain Model

Avoid:

```text
Account
├── id
├── name
├── status
├── getter
└── setter
```

with Business Logic concentrated in:

```text
AccountService
```

Prefer:

```text
Account
├── identity
├── state
├── open()
├── close()
├── rename()
└── enforce account rules
```

Only behavior actually owned by `Account` belongs there.

> Rich Domain Model does not mean Fat Entity.

---

## 17. Module Ownership

Each Module owns its internal Domain Model.

```text
Account Module
└── Account Aggregate
```

Other Modules must not directly consume internal Entities:

```text
Journal Entry Module
        X
        ▼
AccountEntity
```

Preferred:

```text
Journal Entry Application
        │
        ▼
Account Capability / Port
```

or an appropriate Domain Event when asynchronous Business Fact propagation is suitable.

Goal:

> Each Module must be able to evolve its internal model without causing cascading changes in other Modules.

---

## 18. Shared Database ≠ Shared Domain Model

A shared General Ledger Database does not imply a shared Domain Model.

```text
General Ledger Database
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

Each Module remains responsible for its own Business Responsibility and logical Domain Model.

A Module must not treat another Module's internal Table as its Business API.

---

## 19. Direct Database Access

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

Direct access creates:

* Hidden Coupling.
* Broken Encapsulation.
* Architecture Drift.
* Difficult Evolution.

---

## 20. Dependency Direction

Domain must remain independent from Infrastructure.

Preferred:

```text
Presentation
      │
      ▼
Application
      │
      ├──► Internal Contract / Port
      ├──► Repository Port
      └──► Domain
```

Infrastructure:

```text
Infrastructure
├── implements Repository Port
├── implements technical adapters
└── provides external integrations
```

Forbidden:

```text
Domain
   X
   ▼
Infrastructure
```

```text
Account Domain
   X
   ▼
AccountJpaRepository
```

Rule:

> Dependencies must point toward Abstractions and Business Responsibilities, not Implementations and Infrastructure.

---

## 21. Framework Independence

Domain Model should be as Framework Agnostic as practical.

Domain should preferably not depend on:

```text
Spring
Spring Boot
JPA
Hibernate
REST
Kafka
Redis
Database Driver
```

Frameworks are technical details.

Preferred:

```text
Domain
    X
    │
Infrastructure
```

Infrastructure implements Domain/Application abstractions.

---

## 22. Domain Model Complexity

DDD does not justify unnecessary complexity.

```text
Business Complexity
       │
       ▼
Required Domain Model Complexity
```

Simple Business Concept → Simple Model.

Complex Business Rule may require:

```text
Aggregate
Value Object
Domain Service
Domain Event
```

No DDD Abstraction should be introduced merely because DDD is being used.

Avoid both:

* Anemic Domain Model.
* Unnecessary DDD Over-Engineering.

---

## 23. Architectural Rules

1. Domain Model must be Rich.
2. Business Rules must live in Domain.
3. Entity must not be merely a Data Container.
4. Public Setter must not be the primary mechanism for Business State changes.
5. Value Objects should be Immutable where practical.
6. Aggregates must be defined by Business Invariants and Consistency Boundaries.
7. Aggregate size must be limited to the required Consistency Boundary.
8. Repository is defined for Aggregate Roots.
9. Domain must not depend on Persistence Implementations.
10. Repository Port and Internal Contract / Port are different abstractions.
11. Repository must not be used as a generic Integration Mechanism.
12. Application Service must not own Business Rules.
13. Domain Service must represent real Domain Behavior/Concepts.
14. Domain Events must represent meaningful Business Facts.
15. Internal Domain Events and Integration Events are different.
16. DTO must not replace Domain Model.
17. Domain Model must not be derived from Database Schema.
18. Modules must not directly consume each other's internal Entities.
19. Modules must not consume each other's Repository Implementations.
20. Modules must not directly consume each other's internal Tables.
21. Circular Dependencies between Domain Modules must be prevented.
22. `JournalEntry` Core Aggregate must remain independent from Supporting Module implementations.
23. DDD must not introduce unnecessary Over-Engineering.

---

## 24. Architectural Enforcement

These rules should be enforced through Architectural Tests and Code Review.

Required controls include:

* Module Boundaries.
* Dependency Direction.
* Forbidden Dependencies.
* Circular Dependencies.
* Infrastructure Isolation.
* Repository Ownership.
* Internal Contract / Port usage.
* Aggregate Boundaries.

Examples:

```text
Domain
  must not depend on
Infrastructure
```

```text
Account Domain
  must not access
AccountHeadJpaRepository
```

```text
Account Module
  must not access
AccountHeadEntity
```

```text
JournalEntry Domain
  must not depend on
Account Infrastructure
```

```text
Application
  may depend on
Internal Contract / Port
```

```text
Domain Service
  must not become
Cross-Module Orchestrator
```

`ArchUnit` and architectural Code Review are recommended enforcement mechanisms.

---

## 25. Consequences

### Positive

* Business Logic remains concentrated in Domain.
* Business Model becomes easier to understand.
* Anemic Domain Model is reduced.
* Invariants are enforced at appropriate Boundaries.
* Aggregate Boundaries are Business-driven.
* `JournalEntry` Core Domain receives stronger protection.
* Domain becomes more independent from Persistence and Frameworks.
* Repository remains separate from Integration Mechanisms.
* Modules own their internal Models.
* Business Rule testing becomes easier.
* Persistence technology changes have less impact on Domain.

### Negative / Cost

* More Domain Objects may exist.
* Domain/Persistence Mapping may be required.
* Initial design takes more time.
* Developers need stronger Domain understanding.
* Correct Aggregate Boundary design requires analysis and experience.
* Misused Domain Services can increase complexity.
* Excessive Domain/Persistence separation can cause Over-Engineering.

These costs are accepted due to the financial nature and Business complexity of the General Ledger Domain, while unnecessary complexity must still be avoided.

---

## 26. Final Decision

`General Ledger` uses a **Rich Domain Model**.

* Entities own their relevant Behavior and Invariants.
* Value Objects represent concepts without independent Identity.
* Aggregates are defined by Business Invariants and Consistency Boundaries.
* Main Aggregates are `AccountHead`, `Account`, and `JournalEntry`.
* `JournalEntry` is the Core Aggregate.
* Core Business Rules must not be placed primarily in Controller, Application Service, Repository, or Infrastructure.
* Application Layer owns Use Case orchestration.
* Domain owns Business Behavior and Business Decisions.
* Domain Service is used only when behavior has no natural Entity, Value Object, or Aggregate owner and represents a genuine Domain concept.
* Repository exists for Aggregate Roots and represents Persistence Abstraction.
* Internal Contract / Port represents Business Capability or internal communication and is distinct from Repository.
* Domain Model must not be driven by Persistence Model or Database Schema.
* Domain Entity and Persistence Entity may be separated when architecturally justified; separation is not mandatory.
* Aggregates must preserve their own Boundaries and Invariants.
* Cross-Aggregate communication should use Identity/Reference or an appropriate Contract.
* Direct access to another Module's internal Entity, Repository Implementation, Infrastructure, or Table is forbidden.
* DDD means Business Domain-driven design, not merely using Entity, Repository, and Service patterns.

---

## 27. Final Domain Model

```text
GENERAL LEDGER
      │
      ▼
General Ledger Bounded Context
      │
      ▼
General Ledger Microservice
      │
 ┌────┼──────────────────────┐
 ▼    ▼                      ▼
AccountHead                Account                JournalEntry
Module                     Module                 Module ⭐ Core
 │                          │                      │
 ▼                          ▼                      ▼
AccountHead                Account                JournalEntry
Aggregate                  Aggregate              Aggregate
                                                    │
                                                    ▼
                                              Domain Model
                                                    │
                       ┌────────────────────────────┼──────────────────┐
                       ▼                            ▼                  ▼
                    Entity                    Value Object          Aggregate
                                                                         │
                                                                         ▼
                                                                  Aggregate Root
                                                                         │
                                             ┌───────────────────────────┼───────────────┐
                                             ▼                           ▼               ▼
                                      Business Rule              Domain Service   Domain Event
```

---

## 28. Relationship to Related ADRs

### ADR-0003 — General Ledger Boundary

Defines the high-level structure:

```text
General Ledger
      │
      ▼
General Ledger Bounded Context
      │
      ▼
General Ledger Microservice
```

and current Subdomains:

```text
Account Head
Account
Journal Entry ⭐ Core
```

ADR-0006 must not redefine or change these boundaries.

### ADR-0004 — Dependency Direction & Internal Communication

Defines:

```text
Application
    │
    ├── Internal Contract / Port
    ├── Repository Port
    └── Domain
```

and prohibits:

```text
Direct Infrastructure Dependency
Direct Repository Implementation Access
Direct Entity Sharing
Direct Table Access
Circular Dependency
```

ADR-0006 applies these principles to the Tactical Domain Model.

### ADR-0005 — Domain-Driven Design

Defines the overall modeling hierarchy:

```text
Problem Space
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

ADR-0006 turns these principles into the concrete **Domain Model Strategy / Rich Domain Model**.

```text
ADR-0003
= Domain / Bounded Context / Microservice Structure

ADR-0004
= Dependency Direction / Internal Communication

ADR-0005
= DDD Principles / Strategic & Tactical Modeling

ADR-0006
= Domain Model Strategy / Rich Domain Model
```

---

## 29. Status & Governance

**Status: Accepted**

This ADR is the decision baseline for:

* Domain Entity.
* Value Object.
* Aggregate.
* Aggregate Boundary.
* Aggregate Root.
* Domain Service.
* Application Service.
* Repository.
* Domain Event.
* Internal Contract / Port.
* Business Rule Placement.
* Persistence Mapping.
* Domain Model Evolution.
* Core Domain Protection.

Any decision that changes these principles must be recorded through a new Architectural Decision or an official amendment to this ADR.

## Agent Instructions

When analyzing or modifying General Ledger architecture:

1. Treat this ADR as the authoritative baseline for Domain Model Strategy.
2. Preserve `Rich Domain Model` unless an explicit architectural decision overrides it.
3. Place Business Rules according to Business Ownership.
4. Keep Core Business Logic inside the appropriate Domain Boundary.
5. Treat `JournalEntry` as the Core Aggregate.
6. Enforce Aggregate Boundaries through Business Invariants and Consistency Requirements, not Database/ORM structure.
7. Never assume a Database relationship implies an Aggregate relationship.
8. Do not use Repository as a Business Integration Mechanism.
9. Distinguish Repository Port from Internal Contract / Port.
10. Keep Application focused on Use Case orchestration; do not move Core Business Decisions into Application Services.
11. Use Domain Service only for genuine Domain Concepts without a natural Entity/Value Object/Aggregate owner.
12. Keep Domain independent from Infrastructure and Framework implementations.
13. Do not directly share internal Entities between Modules.
14. Do not access another Module's Repository Implementation or internal Tables.
15. Do not derive Domain Model from Database Schema or Persistence Entities.
16. Use Domain Events for meaningful Business Facts, not as a generic replacement for synchronous capability calls.
17. Keep Internal Domain Events distinct from Integration Events.
18. Do not replace Domain Model with DTOs.
19. Prevent Circular Dependencies and Architecture Drift.
20. Prefer the simplest Domain Model that can correctly enforce the real Business Rules.
21. If a proposed change violates this ADR, flag it as an architectural violation and identify the violated Rule.
