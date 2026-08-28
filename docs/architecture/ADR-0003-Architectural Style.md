بله، اتفاقاً برای **LLM/RAG** بهتر است خیلی فشرده‌تر شود. نسخه قبلی هنوز بیش از حد توضیحی بود. باید **Ruleها و Constraintها بمانند، ولی rationale و تکرارها حذف شوند**.

این نسخه را می‌توان تقریباً به‌عنوان نسخه نهایی Ultra-Compact استفاده کرد:

# ADR-0003 — General Ledger Architecture — Ultra Compact

**Status:** Accepted
**Date:** 2026-08-20
**Decision Makers:** Architecture Team
**Related ADRs:** ADR-0001, ADR-0002, ADR-0005

## Purpose

Define the architecture, Domain boundaries, internal Modules, Aggregates, dependency rules, and evolution strategy of the **General Ledger Microservice**.

## Architectural Baseline

```text
System
└── Microservices
    └── General Ledger Microservice
        └── General Ledger Bounded Context
            ├── Account Head — Supporting Subdomain
            ├── Account — Supporting Subdomain
            └── Journal Entry — Core Subdomain ⭐
```

Current architecture:

```text
General Ledger Microservice
├── Account Head Module
│   └── AccountHead Aggregate
├── Account Module
│   └── Account Aggregate
└── Journal Entry Module
    └── JournalEntry Aggregate ⭐
```

Internal architecture:

```text
Each Module
├── Presentation
├── Application
├── Domain
└── Infrastructure
```

Architecture style:

```text
DDD
+
Domain-Oriented Modular Architecture
+
Clean Architecture
+
Onion Architecture
+
Dependency Inversion
+
Architectural Testing
```

---

## Core Rules

### 1. Boundary Definitions

```text
Subdomain ≠ Bounded Context ≠ Module ≠ Microservice
```

* **Subdomain:** Strategic DDD partition of the Problem Space.
* **Bounded Context:** Boundary of Domain Model and language.
* **Module:** Code/structural boundary.
* **Microservice:** Runtime/Deployment boundary.

A Subdomain does **not** automatically become a Bounded Context, Module, or Microservice.

A Bounded Context does **not** universally equal a Microservice.

---

### 2. General Ledger Subdomains

```text
General Ledger
├── Account Head → Supporting
├── Account → Supporting
└── Journal Entry → Core ⭐
```

`Journal Entry` owns the primary General Ledger logic:

* Journal creation
* Debit/Credit validation
* Balance validation
* Journal Line validation
* Posting Rules
* State determination
* Posting

`Account` owns Account-specific rules and invariants.

`Account Head` owns hierarchy, classification, placement, and structural invariants.

---

### 3. Bounded Context

The current General Ledger Bounded Context contains:

```text
Account Head
Account
Journal Entry
```

They remain in one Bounded Context unless a new Architectural Decision changes this.

---

### 4. Internal Modules

The three Subdomains are represented as internal Modules:

```text
Account Head Module
Account Module
Journal Entry Module
```

They:

* Share the same General Ledger Runtime.
* Are not separate Microservices.
* Must preserve clear Business Boundaries.
* Must not access each other's internal implementations.

---

### 5. Aggregate Rules

Primary Aggregate Roots:

```text
AccountHead
Account
JournalEntry ⭐
```

Rules:

* Each Aggregate protects its own Invariants.
* Aggregate ≠ Microservice.
* Aggregate MUST NOT directly call a Repository.
* Aggregate MUST NOT access another Aggregate's Repository.
* Business Rules belonging to an Aggregate MUST reside in its Domain Model.
* Domain Service is used only when a rule genuinely spans Aggregates and does not fit naturally inside one Aggregate.

---

### 6. JournalEntry Rules

`JournalEntry` is the Core Aggregate and Core Domain focus.

Its Domain must contain rules for:

```text
Debit / Credit
Balance
Journal Lines
Posting Rules
Posting State
Posting
```

These rules MUST NOT exist only in:

```text
Controller
Application Service
Repository
Infrastructure
```

---

### 7. Application Layer

Application Layer is responsible for:

* Use Case execution
* Orchestration
* Multi-Aggregate coordination
* Transaction Boundary
* Loading Aggregates through Ports
* Returning Use Case results

Example:

```text
CreateJournalEntry
├── Validate Account
├── Validate Account Head
└── Create JournalEntry
```

Application Layer coordinates; Aggregates enforce Domain Rules.

---

### 8. Repository Ports

Repositories are Persistence Ports:

```text
Application / Domain
        ↓
Repository Port
        ↓
Infrastructure
        ↓
Database
```

Examples:

```text
AccountHeadRepository
AccountRepository
JournalEntryRepository
```

Repository implementations belong to Infrastructure.

```text
AccountRepository
        ↑
JpaAccountRepository
```

Domain MUST NOT depend on Persistence implementations.

---

### 9. Repository ≠ Internal Contract

```text
Repository Port
└── Persistence Abstraction

Internal Contract / Port
└── Capability / Communication Abstraction
```

Not every Domain/Application Interface is a Repository.

---

### 10. Internal Boundary Communication

Communication between Modules MUST use an explicit Contract/Capability.

Allowed:

```text
Journal Entry
    ↓
Account Capability / Internal Contract
    ↓
Account
```

Forbidden:

```text
JournalEntry
    X
    └── Account Internal Entity
```

A Module MUST NOT directly depend on another Module's internal implementation.

---

### 11. Database Boundary

A shared physical Database is allowed initially:

```text
General Ledger Database
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

However:

* Each Business Boundary owns its logical data.
* Direct access to another Boundary's internal tables is forbidden.
* Database tables are not Domain communication Contracts.

Forbidden:

```text
Journal Entry
    X
    └── SELECT FROM account_internal_table
```

---

### 12. External Microservice Communication

External services communicate only through public Microservice Contracts:

```text
External Microservice
        ↓
API / Message
        ↓
General Ledger Microservice
```

External services MUST NOT access:

* Internal Packages
* Internal Classes
* Aggregates
* Repositories
* Internal Database
* Internal Implementations

---

## Dependency Rules

### Allowed

```text
Presentation → Application
Presentation → Domain
Application  → Domain
Infrastructure → Application
Infrastructure → Domain
```

### Forbidden

```text
Domain       → Infrastructure
Domain       → Presentation
Application  → Presentation
Presentation → Infrastructure
```

Domain MUST remain independent of:

```text
Spring
Spring Boot
JPA
Hibernate
REST
Kafka
Redis
Database
External API
```

---

## Architectural Testing

Architecture Rules MUST be enforceable through **ArchUnit** or equivalent tooling.

At minimum test:

```text
Dependency Direction
Domain Isolation
Module Boundaries
Business Boundaries
Forbidden Dependencies
Infrastructure Isolation
Internal Contract Usage
```

Examples:

```text
Domain
├── MUST NOT depend on Infrastructure
├── MUST NOT depend on Presentation
└── MUST NOT depend on Framework-specific Infrastructure
```

---

## Microservice Decision

The following is currently **rejected**:

```text
Account Head Microservice
Account Microservice
Journal Entry Microservice
```

Reasons:

* No independent operational requirement.
* Same Bounded Context.
* Strong Domain interaction.
* Unnecessary Network Communication.
* Distributed Transaction complexity.
* Eventual Consistency complexity.
* Operational and Infrastructure overhead.
* Risk of Distributed Monolith.

Current decision:

```text
Account Head
Account
Journal Entry
        ↓
One General Ledger Microservice
```

---

## Evolution Rules

Microservice extraction is an **evolution decision**, not an automatic Subdomain consequence.

```text
Subdomain
    ↓
Clear Business Boundary
    ↓
Operational Need
    ↓
Possible Microservice Extraction
```

Extraction may be considered when a Boundary requires independent:

* Scaling
* Deployment
* Database
* SLA
* Team Ownership
* Release Cycle
* Operational Isolation

Without such need, keep the Boundary as an internal Module.

---

## Architectural Principles

1. `Subdomain ≠ Bounded Context ≠ Module ≠ Microservice`.
2. `JournalEntry` is the Core Subdomain and Core Aggregate.
3. Each Aggregate owns its own Invariants.
4. Aggregates MUST NOT call Repositories.
5. Application Layer coordinates multiple Aggregates.
6. Business Rules belong in Domain.
7. Internal Modules MUST NOT access each other's internal implementations.
8. Cross-Boundary communication uses Contracts/Capabilities.
9. Direct cross-Boundary Database access is forbidden.
10. External Microservices use public Contracts only.
11. Domain MUST remain independent from Infrastructure/Frameworks.
12. Architectural boundaries MUST be enforced by tests.
13. A Subdomain MUST NOT automatically become a Microservice.
14. Microservice extraction requires explicit operational justification.
15. Any change to the architectural Baseline requires a new or updated Architectural Decision.

---

## Final Decision

```text
General Ledger
│
├── Account Head
│   └── Supporting Subdomain
│
├── Account
│   └── Supporting Subdomain
│
└── Journal Entry
    └── Core Subdomain ⭐
        │
        ▼
General Ledger Bounded Context
        │
        ▼
General Ledger Microservice
        │
        ├── Account Head Module
        │   └── AccountHead Aggregate
        │
        ├── Account Module
        │   └── Account Aggregate
        │
        └── Journal Entry Module
            └── JournalEntry Aggregate ⭐
```

**Status: Accepted**

This ADR is the current architectural Baseline for General Ledger. Any major change to its Subdomains, Bounded Context, Modules, Aggregates, Business Boundaries, Repository/Contract boundaries, or Microservice boundary MUST be introduced through a new or formally updated Architectural Decision.
