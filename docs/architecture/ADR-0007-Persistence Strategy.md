# ADR-0007 — Persistence Strategy 

## Purpose

Define the Persistence Strategy for the General Ledger Microservice while preserving:

* Domain independence from persistence technology.
* Aggregate and Transaction boundaries.
* Module Data Ownership.
* Data Integrity.
* Auditability.
* Performance and scalability.
* Core Domain protection, especially `JournalEntry`.

**Status:** Accepted
**Date:** 2026-08-22

---

## Core Decision

Use a **Repository Port + Persistence Adapter** strategy.

```text
Application / Domain
        │
        ▼
Repository Port
        ▲
        │ implements
        │
Infrastructure
        │
Persistence Adapter
        │
JPA / Hibernate
        │
Database
```

The primary relational Persistence technology is:

```text
JPA / Hibernate
```

JPA/Hibernate is an **Infrastructure concern**, not a Domain dependency.

---

## Dependency Rules

### Required

```text
Domain / Application
        │
        ▼
Repository Port
        ▲
        │
Infrastructure
        │
        ▼
JPA / Hibernate
        │
        ▼
Database
```

### Forbidden

```text
Domain
   │
   ▼
JPA / Hibernate
   │
   ▼
Database
```

Domain must not directly depend on:

* JPA
* Hibernate
* `EntityManager`
* Hibernate Session
* Database Driver
* SQL/JPQL
* Persistence-specific APIs

---

## Repository Port

Repository interfaces are Persistence abstractions defined according to Domain/Application needs.

Example:

```java
public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    void save(Account account);
}
```

Repository contracts must be **Business-oriented**, not technology-oriented.

Repository Ports must not expose:

```text
JpaRepository
EntityManager
Hibernate Session
Criteria API
JPQL
SQL
JPA Specification
Hibernate Proxy
```

Repositories must not become generic Database gateways.

### Preferred

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

### Forbidden

```text
GenericRepository
 ├── findAnything()
 ├── executeAnything()
 ├── queryAnything()
 └── updateAnything()
```

---

## Persistence Adapter

Repository implementations belong to `Infrastructure`.

Recommended structure:

```text
Infrastructure
└── persistence
    ├── entity
    │   ├── AccountJpaEntity
    │   ├── AccountHeadJpaEntity
    │   └── JournalEntryJpaEntity
    │
    ├── repository
    │   ├── AccountJpaRepository
    │   ├── AccountHeadJpaRepository
    │   └── JournalEntryJpaRepository
    │
    ├── mapper
    │   ├── AccountPersistenceMapper
    │   ├── AccountHeadPersistenceMapper
    │   └── JournalEntryPersistenceMapper
    │
    └── adapter
        ├── AccountRepositoryAdapter
        ├── AccountHeadRepositoryAdapter
        └── JournalEntryRepositoryAdapter
```

Flow:

```text
AccountRepository
        ▲
        │ implements
        │
AccountRepositoryAdapter
        │
        ▼
AccountJpaRepository
        │
        ▼
AccountJpaEntity
        │
        ▼
Database
```

---

## Domain Entity vs Persistence Entity

Default rule:

```text
Domain
└── Account

Infrastructure
└── AccountJpaEntity
```

Domain and Persistence entities should normally be separate.

Benefits:

* Domain remains independent of JPA.
* Persistence concerns do not leak into Domain.
* Schema changes do not necessarily change Domain Model.
* Mapping remains explicit and controlled.
* Framework coupling is reduced.

However, separation is **not absolute**.

If Domain and Persistence models can safely be shared without inappropriate coupling, sharing may be intentionally approved.

> Business Model must never be compromised merely for Persistence convenience.

---

## Mapping

Mapping belongs to `Infrastructure`.

### Write

```text
Domain Aggregate
      │
      ▼
Persistence Mapper
      │
      ▼
Persistence Entity
      │
      ▼
Database
```

### Read

```text
Database
   │
   ▼
Persistence Entity
   │
   ▼
Persistence Mapper
   │
   ▼
Domain Aggregate
```

Default:

```text
Domain Model != Persistence Model
```

unless an explicit architectural decision permits model sharing.

---

## Database as Infrastructure

Database is an Infrastructure detail.

```text
Domain
  ▲
  │
Repository Port
  ▲
  │
Infrastructure
  │
  ├── JPA
  └── SQL
       │
       ▼
    Database
```

Changing Persistence technology must not require changing Domain Business Rules or core entities such as:

```text
JournalEntry
Account
AccountHead
```

---

# Module Data Ownership

Each Module owns the Logical Data associated with its Business Responsibility.

```text
General Ledger Microservice
│
├── Account Head Module
│   └── owns Account Head data
│
├── Account Module
│   └── owns Account data
│
└── Journal Entry Module
    └── owns Journal Entry data
```

A shared physical Database is allowed:

```text
General Ledger Database
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

But:

```text
Physical Database Sharing
        !=
Shared Data Ownership
```

and:

```text
Physical Database Sharing
        !=
Shared Domain Model
```

---

# Cross-Module Persistence Access

A Module must not directly access another Module's:

* Table
* Persistence Entity
* Repository Implementation
* Internal Persistence Model

Forbidden:

```text
Journal Entry Module
        X
        ▼
Account Table
```

```text
Account Module
        X
        ▼
AccountHeadJpaEntity
```

```text
Module A
        X
        ▼
Module B Repository Implementation
```

Use instead:

```text
Module A
   │
   ▼
Business Capability / Internal Contract / Port
   │
   ▼
Module B
```

or, when appropriate:

```text
Module A
   │
   ▼
Domain Event
   │
   ▼
Module B
```

---

# Aggregate Persistence

Repository is defined for the **Aggregate Root**.

```text
AccountHead Aggregate
        │
        ▼
AccountHeadRepository
```

```text
Account Aggregate
        │
        ▼
AccountRepository
```

```text
JournalEntry Aggregate
        │
        ▼
JournalEntryRepository
```

Internal entities must not have independent repositories unless they become independent Aggregate Roots.

Example:

```text
JournalEntry
└── JournalLine
```

Default:

```text
JournalEntryRepository
```

No independent:

```text
JournalLineRepository
```

---

# Core Domain Protection

`JournalEntry` is the Core Aggregate.

Supporting Modules must not leak their Persistence implementation into `JournalEntry` Domain.

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
AccountJpaEntity
```

Preferred:

```text
PostJournalEntryUseCase
        │
        ├──► Account Capability
        │
        └──► JournalEntry Aggregate
```

Core Domain must depend on Business capabilities, not Supporting Module Persistence implementation.

---

# Transaction Strategy

Transaction Boundary is primarily defined at the **Application Use Case** level.

```text
Use Case
   │
   ▼
Transaction Boundary
   │
   ├── Load Aggregate
   ├── Execute Domain Behavior
   ├── Persist Changes
   └── Commit
```

Repository methods such as:

```text
save()
```

must not independently define the Business Transaction Boundary.

Transaction scope must consider:

* Use Case
* Business Consistency Requirement
* Aggregate Boundary
* Business Operation

---

# Aggregate and Transaction Boundary

Aggregate is the primary Domain Consistency Boundary.

```text
Aggregate
    │
    ▼
Consistency Boundary
    │
    ▼
Transaction Requirement
```

But:

```text
1 Aggregate = 1 Transaction
```

is **not an absolute rule**.

A Use Case may involve multiple Aggregates.

Decision must consider:

* Whether atomic consistency is actually required.
* Whether one Transaction may cover multiple Aggregates.
* Whether resulting coupling is acceptable.
* Whether Eventual Consistency is preferable.

> Transaction Boundary is determined by Business Consistency Requirement, not merely Database structure.

A shared Transaction does not merge Aggregate boundaries.

```text
Account Aggregate
        +
JournalEntry Aggregate
```

remain separate Aggregates even if one Use Case updates both in one Transaction.

---

# JournalEntry Transaction

Example:

```text
PostJournalEntry
       │
       ▼
Transaction Boundary
       │
       ├── Load required Account capability/data
       ├── Execute JournalEntry behavior
       ├── Persist JournalEntry
       └── Commit
```

The exact Transaction scope must follow actual Business Consistency requirements.

---

# Lazy Loading

Lazy Loading must never become part of Business Logic.

Domain must not depend on:

```text
Entity
  ↓
Lazy Proxy
  ↓
Database
```

Persistence must explicitly determine the data required by each Use Case.

Avoid:

* `LazyInitializationException`
* N+1 Query
* Unintended Database Access
* Unexpected Query Explosion

Domain must not depend on ORM navigation to retrieve required data.

---

# Command vs Query

Persistence distinguishes logically between State-changing operations and Reads.

## Command

```text
Command
   │
   ▼
Application Use Case
   │
   ▼
Aggregate
   │
   ▼
Business Rule
   │
   ▼
Repository
```

Commands modify Domain State through Domain behavior.

## Query

```text
Query
   │
   ▼
Query Adapter / Read Model
   │
   ▼
Database
```

Complex Queries do not necessarily need to load the complete Domain Aggregate.

---

# Query Model / Read Model

Complex, Reporting, and Performance-sensitive Queries may use a dedicated Read Model.

```text
Reporting Query
      │
      ▼
Query Service
      │
      ▼
Read Model
      │
      ▼
Database
```

Read Model is not necessarily a Domain Entity.

Goals:

* Reduce Query cost.
* Avoid unnecessary Aggregate loading.
* Improve Read Performance.
* Simplify Reporting.
* Reduce Query coupling to Domain Model.

---

# CQRS

CQRS is **not globally mandatory**.

Use CQRS locally when justified by:

* High Read volume.
* Complex Queries.
* Different Reporting Model.
* Performance requirements requiring a dedicated Read Model.

```text
Command Side
     │
     ▼
Domain Model
```

```text
Query Side
     │
     ▼
Read Model
```

> CQRS is an optional local optimization based on real requirements.

---

# Database Schema

Database Schema belongs to Persistence Model and must not define the Domain Model.

Preferred direction:

```text
Business Requirement
        │
        ▼
Domain Model
        │
        ▼
Persistence Model
        │
        ▼
Database Schema
```

Forbidden architectural direction:

```text
Database Schema
        │
        ▼
JPA Entity
        │
        ▼
Domain Model
```

Database must support the Business Model rather than define it.

---

# Database Migration

Database changes must be:

* Versioned.
* Repeatable.
* Stored in the project Repository.
* Executed sequentially.
* Predictable across Environments.

Example:

```text
Migration V1
Migration V2
Migration V3
...
```

Manual Schema changes must not be the primary Database management mechanism.

Migration is part of the Software Lifecycle.

---

# Data Integrity

Data Integrity must be protected at two levels.

## Domain Integrity

Business Invariants belong to Domain.

Example:

```text
JournalEntry must be balanced
```

must be enforced by:

```text
JournalEntry Aggregate
```

## Database Integrity

Database should enforce structural Data Integrity using appropriate constraints:

```text
Primary Key
Foreign Key
Unique Constraint
Not Null
Check Constraint
```

> Database is not the sole location for Business Rules, but Database Integrity capabilities must not be ignored.

---

# Optimistic Locking

Aggregates exposed to Concurrent Updates should consider Optimistic Locking.

```text
Aggregate
   │
   └── Version
```

Update behavior:

```text
Expected Version
       │
       ▼
Database
       │
       ├── Version matches
       │       │
       │       ▼
       │      Update
       │
       └── Version changed
               │
               ▼
             Conflict
```

Purpose:

* Prevent Lost Updates.
* Detect Concurrent Modification.

Usage must be determined by each Aggregate's actual Concurrency requirements.

---

# Auditing

Because General Ledger is a financial system, Data Change Auditing is required where applicable.

Audit may capture:

* What changed.
* When it changed.
* What operation occurred.
* Which Actor or Context initiated it.
* Previous and new values when required.

Audit must not pollute core Domain Business Logic.

ORM Auditing, when used, remains an Infrastructure concern.

---

# Soft Delete

Soft Delete is **not globally mandatory**.

Deletion semantics must follow Business Meaning.

Possible Business concepts include:

```text
Deactivate
Close
Archive
Cancel
```

rather than simply:

```text
deleted = true
```

> Never introduce Soft Delete merely for Persistence convenience.

---

# Repository vs Query Responsibility

Repository owns Aggregate Persistence.

Complex Query and Reporting responsibilities may belong to separate Query Services / Read Adapters.

```text
Command
   │
   ▼
Aggregate Repository
```

```text
Query
   │
   ▼
Query Service / Read Adapter
   │
   ▼
Database
```

This allows:

* Command Model optimization for Business behavior.
* Query Model optimization for Read Performance.
* Simpler Repositories.
* Domain independence from Reporting requirements.

---

# Performance Rules

Persistence strategy depends on operation type.

### Command

```text
Use Case
   │
   ▼
Aggregate
   │
   ▼
Repository
   │
   ▼
Database
```

### Simple Query

```text
Query
   │
   ▼
Query Adapter
   │
   ▼
Database
```

### Complex Query

```text
Query
   │
   ▼
Optimized SQL / Read Model
   │
   ▼
Database
```

Domain Aggregates do not need to be used for every Query.

> Domain Model is optimized for Business Behavior; Read Model may be optimized for Read Performance.

---

# N+1 Query

N+1 Query is a Persistence Anti-Pattern.

Query design must explicitly determine:

* Required data.
* Required Relationships.
* Expected Queries.
* Database Round Trips.

ORM Object Graph navigation must not unintentionally generate Database Queries.

Particular attention is required for:

```text
JournalEntry
JournalLine
Account
AccountHead
```

---

# Persistence Technology

Primary relational Persistence technology:

```text
JPA / Hibernate
```

Architecture:

```text
Domain
   │
   ▼
Repository Port
   ▲
   │
Infrastructure
   │
   ▼
JPA / Hibernate
   │
   ▼
Relational Database
```

JPA/Hibernate is a technology decision inside `Infrastructure`, not part of Domain Model.

---

# Physical Database Sharing

General Ledger Microservice may use one physical Database for multiple Modules.

```text
General Ledger Database
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

But:

```text
Physical Database Sharing
        !=
Shared Domain Model
```

and:

```text
Physical Database Sharing
        !=
Unrestricted Table Access
```

Physical sharing must not violate Module Data Ownership.

---

# Domain Events

Persistence mechanisms must not be confused with Domain Events.

Domain flow:

```text
Aggregate
    │
    ▼
Domain Event
    │
    ▼
Application / Infrastructure
    │
    ▼
Event Handling / Publication
```

Database Triggers or ORM Callbacks must not replace Business Domain Events except for purely technical concerns.

---

# Integration Boundary

Repository is an internal Persistence mechanism and must not be used for Microservice Integration.

Forbidden:

```text
Another Microservice
        X
        ▼
JournalEntryRepository
```

Integration must use:

```text
API
Messaging
Contract
```

Preferred:

```text
Another Microservice
        │
        ▼
Integration Boundary
        │
        ▼
General Ledger Application
```

Repository is only for Persistence inside the same Microservice Boundary.

---

# Architectural Rules

1. Domain must not depend on JPA/Hibernate.
2. Repository Port must be defined at the appropriate inner layer.
3. Repository Implementation belongs to Infrastructure.
4. Persistence Entity belongs to Infrastructure.
5. Domain Entity and Persistence Entity are separate by default; intentional sharing is allowed.
6. Domain/Persistence Mapping belongs to Infrastructure.
7. A Module must not directly Query or Modify another Module's Tables.
8. A Module must not consume another Module's Persistence Entities.
9. A Module must not consume another Module's Repository Implementation.
10. Transaction Boundary is determined by Use Case and Consistency Requirement.
11. Aggregate Root is the primary Persistence reference.
12. Internal Aggregate Entities do not receive independent Repositories unless they become Aggregate Roots.
13. Complex Queries may use Query Adapters or Read Models.
14. CQRS is local and requirement-driven.
15. Database Migrations must be Versioned and Repeatable.
16. Business Rules must not exist only in Database.
17. Database Constraints must protect appropriate Data Integrity.
18. Optimistic Locking should be considered for Aggregates with Concurrent Updates.
19. Audit must not pollute core Domain Logic.
20. Soft Delete requires Business justification.
21. Repository must not become a Generic Database Gateway.
22. N+1 Query and unintended Database access must be controlled.
23. Database Ownership is maintained at Module level.
24. Physical Database Sharing does not imply Shared Domain/Data Ownership.
25. `JournalEntry` must not depend on Supporting Module Persistence implementation.
26. Repository must not be used as a Microservice Integration mechanism.

---

# Architectural Enforcement

These rules should be enforced through Architectural Tests and Code Review.

Expected architectural constraints:

```text
Domain
    must not depend on
JPA / Hibernate
```

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
JournalEntry Domain
    must not access
AccountJpaEntity
```

```text
Module A
    must not access
Module B Persistence Entity
```

```text
Module A
    must not access
Module B Table
```

```text
Application
    may depend on
Repository Port
```

```text
Infrastructure
    implements
Repository Port
```

```text
Query Model
    may bypass
Domain Aggregate
```

Recommended enforcement mechanisms:

```text
ArchUnit
Code Review
Integration Tests
Persistence Tests
```

---

# Positive Consequences

This strategy provides:

* Domain independence from Database.
* Domain independence from JPA/Hibernate.
* Easier Persistence technology replacement.
* Protection of Business Model from Schema-driven design.
* Preservation of Aggregate Boundaries.
* Explicit Module Data Ownership.
* Reduced Cross-Module Database Coupling.
* Stronger protection of `JournalEntry` as Core Domain.
* Independent Command/Query optimization.
* Local CQRS capability.
* Domain + Database Data Integrity.
* Financial Auditability.

---

# Negative Consequences

Accepted costs include:

* Domain/Persistence Mapping.
* More classes.
* More Infrastructure code.
* More sophisticated Repository design.
* More careful Transaction Boundary design.
* Database Migration management.
* Query Performance management.
* Explicit Data Ownership design.
* Risk of Over-Engineering if separation is applied unnecessarily.

> Separation must remain proportional to actual Business Complexity.

---

# Final Decision

```text
Persistence
    │
    ▼
Infrastructure Concern
```

`Domain` and `Application` interact with Persistence through `Repository Port`.

```text
Domain / Application
        │
        ▼
Repository Port
        ▲
        │
Infrastructure Adapter
        │
        ▼
Persistence Model
        │
        ▼
Database
```

Primary relational Persistence technology:

```text
JPA / Hibernate
```

but Domain must remain independent from it.

Default:

```text
Domain Model != Persistence Model
```

Mapping belongs to Infrastructure.

Each Module owns its Logical Data.

```text
Physical Database Sharing
        is allowed
```

but:

```text
Cross-Module Table Access
        is forbidden

Cross-Module Persistence Entity Access
        is forbidden

Cross-Module Repository Implementation Access
        is forbidden
```

Transaction Boundary is based on:

```text
Use Case
+
Business Consistency Requirement
```

Aggregate remains the primary Domain Consistency Boundary.

For Commands:

```text
Domain Aggregate + Repository
```

For complex Queries:

```text
Query Adapter / Read Model
```

CQRS is optional and local.

Database Constraints protect structural Data Integrity, while Business Invariants remain enforced by Domain.

Repository is strictly for internal Persistence and must not become:

```text
Generic Database Gateway
```

or:

```text
Microservice Integration Mechanism
```

`JournalEntry` must remain protected from Supporting Module Persistence implementation.

---

# Architectural Model

```text
              GENERAL LEDGER MICROSERVICE
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
   Account Head         Account       Journal Entry
      Module             Module           Module
          │                │                │
          ▼                ▼                ▼
 AccountHead           Account        JournalEntry
  Aggregate           Aggregate        Aggregate
                                           ⭐ Core
          │                │                │
          └────────────────┼────────────────┘
                           │
                           ▼
                    Repository Ports
                           ▲
                           │
                ┌──────────┴──────────┐
                │    Infrastructure   │
                │                     │
                │ Adapter             │
                │ Mapper              │
                │ Persistence Entity  │
                │ JPA / Hibernate     │
                └──────────┬──────────┘
                           │
                           ▼
                General Ledger Database
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
   Account Head Data   Account Data   Journal Entry Data
```

