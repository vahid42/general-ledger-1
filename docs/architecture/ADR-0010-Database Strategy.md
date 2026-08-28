# ADR-0010 — Database Strategy — Ultra Compact

**Status:** Accepted
**Date:** 2026-08-22
**Related:** General Ledger — Domain & Bounded Context Decision Baseline

## Purpose

Define Persistence architecture for the General Ledger Bounded Context, including:

* Persistence and Repository strategy
* Data Ownership
* Transaction boundaries
* Database/Schema strategy
* Migration
* Read Models
* Performance
* Persistence testing

## Architecture Context

```text
General Ledger
└── General Ledger Bounded Context
    └── General Ledger Microservice
        ├── Account Head Module
        ├── Account Module
        └── Journal Entry Module
```

Subdomains:

* `Account Head` — Supporting Subdomain
* `Account` — Supporting Subdomain
* `Journal Entry` — Core Subdomain

Primary Aggregate Roots:

* `AccountHead`
* `Account`
* `JournalEntry`

All three Subdomains currently exist inside one Bounded Context and one Microservice.

---

## Core Rules

1. Database and Persistence technology are **Infrastructure details**.
2. Domain must not depend directly on Database technology.
3. Persistence must not dictate the Domain Model or Aggregate boundaries.
4. Repository is a **Persistence Port**, not a Database abstraction exposed to Domain internals.
5. Repository must be designed around Aggregates, not Database tables.
6. Repository implementations belong to Infrastructure.
7. Application Layer orchestrates interactions between Aggregates.
8. Aggregate business rules remain inside Domain.
9. Aggregate must never directly call another Aggregate's Repository.
10. Data Ownership follows Domain/Module/Aggregate boundaries, not physical Database deployment.
11. Shared Database does **not** imply Shared Data Ownership.
12. Transaction management belongs to the Application Use Case / Infrastructure integration, not Domain.
13. Persistence technology must be replaceable without changing Domain Business Rules.
14. Complex read requirements must not distort Aggregates; use Read Models/Projections when appropriate.
15. Permanent Database Schema changes must use Versioned Migrations.

---

## Persistence Boundary

```text
Domain
  │
  ▼
Repository / Persistence Port
  │
  ▼
Infrastructure
  │
  ▼
Persistence Technology
  │
  ▼
Database
```

Domain must not directly depend on:

* JPA
* Hibernate
* JDBC
* SQL
* Database Driver
* ORM-specific APIs
* Transaction APIs

For example, Domain Aggregates must not require `EntityManager`, `JpaRepository`, or equivalent APIs to execute Business Rules.

---

## Current Persistence

Current implementation is **In-Memory**:

```text
Application / Domain
        │
        ▼
Repository Port
        │
        ▼
In-Memory Repository
        │
        ▼
In-Memory Data
```

This is an implementation choice, not an architectural commitment to In-Memory storage.

Future implementations may replace it without changing Domain Business Rules:

```text
             Repository Port
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
 In-Memory Repository   Relational Repository
                              │
                              ▼
                         SQL Database
```

Changing Persistence must not require changes to Aggregates unless Domain requirements themselves change.

---

## Repository Strategy

Repositories are Persistence Ports and are Aggregate-oriented.

Primary repositories:

```text
AccountHeadRepository
AccountRepository
JournalEntryRepository
```

Architecture:

```text
Aggregate
   │
   ▼
Repository Port
   │
   ▼
Infrastructure Implementation
```

Do **not** model repositories around internal Database tables such as:

```text
JournalEntry
  ├── AccountTableRepository
  ├── JournalLineTableRepository
  └── PostingTableRepository
```

Internal storage structures may change independently inside Infrastructure.

---

## Aggregate-Oriented Persistence

Aggregate boundaries remain the primary Persistence boundaries:

```text
General Ledger
├── AccountHead Aggregate
├── Account Aggregate
└── JournalEntry Aggregate
```

Each Aggregate owns its own Invariants and Consistency Boundary.

Persistence Schema must not redefine or weaken Aggregate boundaries.

---

## Data Ownership

Logical Data Ownership follows Domain and Aggregate boundaries:

```text
Account Head Module
└── AccountHead-owned data

Account Module
└── Account-owned data

JournalEntry Module
└── JournalEntry-owned data
```

A shared physical Database does not create shared ownership:

> **Shared Database does not mean Shared Data Ownership.**

Ownership must remain explicit in:

* Domain
* Module boundaries
* Repository boundaries
* Database structures where useful

---

## Database vs Bounded Context Ownership

All three Modules belong to the same Bounded Context. Therefore, a shared Database is not inherently an architectural violation.

A shared Database should still logically separate:

```text
General Ledger Database
├── AccountHead-owned data
├── Account-owned data
└── JournalEntry-owned data
```

Modules must not directly access Persistence structures owned by another Aggregate/Module merely because they share a Database.

Physical Database separation per Module is optional, not mandatory.

---

## Cross-Module / Cross-Aggregate Access

`Account Head`, `Account`, and `Journal Entry` are inside the same Bounded Context, so their communication is **not Inter-Bounded-Context Communication**.

Module and Aggregate boundaries still apply.

Example:

```text
CreateJournalEntry
  ├── Validate Account
  ├── Validate AccountHead
  └── Create JournalEntry
```

Coordination belongs to Application:

```text
Application Layer
  ├── load Account
  ├── load AccountHead
  └── create JournalEntry
             │
             ▼
        Domain Rules
             │
             ▼
       JournalEntry
```

`JournalEntry` must not directly invoke `AccountRepository` or `AccountHeadRepository`.

---

## Repository Port vs Internal Port

Do not equate every interface with a Repository.

```text
Repository Port
└── Persistence abstraction

Internal Contract / Port
└── Capability / communication abstraction
```

A Domain/Application interface is a Repository only when its responsibility is Persistence.

This distinction prevents accidental Infrastructure coupling.

---

## Transaction Strategy

Transaction boundaries are managed at the **Application Use Case** level.

```text
API
 │
 ▼
Application Use Case
 ├── Load required Aggregates
 ├── Execute Domain behavior
 ├── Persist changes
 └── Transaction Boundary
          │
          ▼
      Persistence
```

Domain must not:

* Start transactions
* Commit transactions
* Roll back transactions
* Depend on Transaction APIs

Current In-Memory Persistence may have no Database transaction or may require a different mechanism.

If transactional Database Persistence is introduced, transaction management belongs to Application/Infrastructure integration.

---

## Aggregate Consistency vs Transaction Boundary

These concepts must remain distinct.

* **Aggregate Boundary:** primary Consistency and Invariant boundary.
* **Transaction Boundary:** Application-level coordination mechanism.

A Use Case may coordinate multiple Aggregates without merging them:

```text
CreateJournalEntry
  ├── Account Aggregate
  ├── AccountHead Aggregate
  └── JournalEntry Aggregate
```

Each Aggregate retains its own Consistency Boundary.

---

## Persistence Technology

If permanent Persistence is introduced, a **Relational Database** is the accepted primary option.

Infrastructure may use:

* Spring Data JPA
* Hibernate
* JDBC

Example:

```text
Domain
  │
  ▼
Repository Port
  │
  ▼
Infrastructure
  ├── Spring Data JPA
  ├── Hibernate
  └── JDBC
          │
          ▼
   Relational Database
```

The specific Persistence technology may be defined by a separate ADR.

---

## Domain Model vs Persistence Model

Domain Model and Persistence Model are conceptually independent.

When meaningful differences exist:

```text
Domain Model
     │
     │ Mapping
     ▼
Persistence Model
     │
     ▼
Database
```

Persistence Model must not become the primary Domain Model.

This is especially important for the Core Domain Aggregate `JournalEntry`; it must not be designed primarily around ORM/Database constraints.

Shared structures may be considered when the model is simple and architectural coupling does not result, but Domain must remain independent from ORM.

---

## Database Schema Strategy

Schema organization should reflect logical Domain/Data Ownership:

```text
General Ledger Database
├── AccountHead-owned structures
├── Account-owned structures
└── JournalEntry-owned structures
```

Separate physical schemas are optional:

```text
Database
├── account_head_schema
├── account_schema
└── journal_entry_schema
```

They are **not required merely because three Modules exist**.

Priority of boundaries:

```text
Domain → Module → Repository → Database Schema
```

Database Schema should support these boundaries rather than define them.

---

## Database Migration

If permanent Database Persistence is introduced, all Schema changes must use **Versioned Migrations**.

Migration requirements:

* Versioned
* Stored in Source Control
* Repeatable/reproducible
* Executable consistently across environments
* Prevent uncontrolled/manual Schema changes

Possible tools:

* Flyway
* Liquibase

Tool selection may be defined in a separate ADR.

---

## Query & Read Model

Complex queries, Reporting, and Read-heavy operations must not pollute Aggregates.

A Read Model / Projection may be introduced:

```text
Write Side
Command
  │
  ▼
Application
  │
  ▼
Domain
  │
  ▼
Aggregate
  │
  ▼
Repository
```

```text
Read Side
Query
  │
  ▼
Query Handler
  │
  ▼
Read Model / Projection
  │
  ▼
Persistence
```

Read Model does not need to match Domain Model.

---

## Performance Rules

Persistence optimization must be **measurement-driven**.

For permanent Database Persistence, evaluate:

* Indexing
* Query Optimization
* Pagination
* Batch Processing
* Connection Pooling
* N+1 Query Detection
* Loading Strategy
* Transaction Size
* Database Locking
* Query Latency

Do not introduce complex optimization without a measurable Bottleneck.

For current In-Memory Persistence, these are future Database requirements.

---

## Persistence Testing

### Current

```text
Unit Test
  └── In-Memory Repository
```

### Future Database Persistence

```text
Integration Test
├── Repository
├── Persistence Mapping
├── Transaction
└── Database
```

For Database-backed repositories, a real Database or **Testcontainers** is preferred.

Unit Tests must not be considered a replacement for testing:

* Real SQL behavior
* Persistence Mapping
* Transaction behavior

---

## Architectural Rules

The following rules are mandatory:

1. Domain must not depend on Database.
2. Domain must not depend on JPA, Hibernate, JDBC, or ORM-specific APIs.
3. Repository must be Aggregate-oriented.
4. Repository Implementation belongs to Infrastructure.
5. Aggregate must not directly call another Aggregate's Repository.
6. Application Layer owns orchestration between Aggregates.
7. Aggregate Business Rules remain in Domain.
8. Database Ownership and Domain Ownership are distinct concepts.
9. Shared Database does not imply Shared Data Ownership.
10. Modules must not directly depend on Persistence structures owned by other Modules/Aggregates.
11. Transaction Boundary is managed at Application Use Case level.
12. Domain must not manage transactions.
13. Persistence technology changes must not require Domain Business Rule changes.
14. Complex queries must not reshape Aggregates for Read concerns.
15. Permanent Database Schema changes must use Versioned Migrations.

---

## Consequences

### Benefits

* Domain independence from Persistence technology
* Preserved Aggregate boundaries
* Reduced Module coupling
* Explicit Data Ownership
* Current support for In-Memory Persistence
* Future migration to Relational Database
* Prevents Domain from becoming an ORM Model
* Independent Domain testing
* Integration testing for real Persistence
* Preserves `JournalEntry` as Core Domain
* Persistence technology can change without changing Business Logic

### Costs

* Repository Ports must be defined
* Domain/Persistence Mapping may be required
* More Infrastructure code
* Migration management is required for permanent Database
* Query and Read Model design may become necessary
* Some Cross-Aggregate Use Cases become more complex

---

## Current Implementation Status

```text
General Ledger Microservice
├── Account Head
├── Account
└── Journal Entry
        │
        ▼
Repository Ports
        │
        ▼
In-Memory Implementations
```

Current Persistence is **In-Memory**.

Therefore, the following are **not currently required implementations**:

* Relational Database
* JPA
* Hibernate
* Flyway
* Liquibase

This ADR defines the architectural Persistence boundary independently of the current implementation.

---

## Final Decision

```text
General Ledger Bounded Context
          │
          ▼
General Ledger Microservice
          │
          ├── AccountHead Aggregate
          │       │
          │       ▼
          │   Repository Port
          │
          ├── Account Aggregate
          │       │
          │       ▼
          │   Repository Port
          │
          └── JournalEntry Aggregate
                  │
                  ▼
              Repository Port
                  │
                  ▼
             Infrastructure
                  │
                  ▼
        Current: In-Memory
        Future: Relational DB
```

### Key Architectural Principles

> **Persistence is an Infrastructure detail; Database must not define the Aggregate or Domain Model.**

> **Current Persistence is In-Memory and must not change Domain, Aggregate, or Repository boundaries.**

> **When permanent Database Persistence is introduced, Data Ownership remains based on Module/Aggregate boundaries and Persistence technology remains inside Infrastructure.**

## Agent Instructions

When modifying or implementing Persistence:

* Keep Domain independent from Database/ORM technology.
* Define Repository contracts around Aggregates.
* Put Repository implementations in Infrastructure.
* Keep cross-Aggregate orchestration in Application.
* Never move Business Rules into Persistence code.
* Never let Database Schema redefine Domain or Aggregate boundaries.
* Treat shared Database as physical sharing, not shared ownership.
* Keep transaction management outside Domain.
* Use Read Models for complex/read-heavy queries instead of weakening Aggregates.
* Use Versioned Migrations for permanent Database Schema changes.
* Preserve the ability to replace In-Memory Persistence with Relational Persistence without changing Domain Business Rules.
