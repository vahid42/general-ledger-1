# General Ledger — Domain & Bounded Context Decision Baseline

## Purpose

Defines the architectural baseline for `General Ledger` regarding:

* `Domain` / `Subdomain`
* `Bounded Context`
* `Microservice`
* `Module`
* `Aggregate` / `Aggregate Root`
* Repository and Domain/Application responsibilities

This document is the baseline for future architectural decisions. Changes to these decisions require a new architectural decision and subsequent baseline update.

## Problem Space
`General Ledger` is the product `Problem Space`.

### Subdomains

```text
General Ledger
├── Account Head       — Supporting Subdomain
├── Account            — Supporting Subdomain
└── Journal Entry      — Core Subdomain
```

## Subdomain Classification

### Core Subdomain — Journal Entry

`Journal Entry` is the Core Subdomain because:

* Its creation, validation, and posting contain the primary differentiating Business Rules.
* It represents the main competitive value of the system.
* Most General Ledger domain complexity is concentrated in Journal Entry processing.
* Architecture and Domain design investment should prioritize this area.

`Journal Entry` must remain genuinely domain-centric and must not degrade into a CRUD-oriented component.

### Supporting Subdomain — Account

`Account` manages:

* Account identity
* Account status
* Account type
* Account attributes
* Information required by General Ledger operations

### Supporting Subdomain — Account Head

`Account Head` manages:

* Head structure
* Classification
* Hierarchy
* Account placement within the General Ledger structure

## Strategic DDD Boundaries

These concepts must remain distinct:

```text
Subdomain
    ≠
Bounded Context
    ≠
Module
    ≠
Microservice
```

Definitions:

* `Subdomain` = strategic business/problem-space boundary.
* `Bounded Context` = domain model and ubiquitous-language boundary.
* `Module` = structural/code boundary.
* `Microservice` = independent deployment/runtime boundary.

A Subdomain does not automatically require its own `Bounded Context`, `Module`, or `Microservice`.

## Bounded Context Decision

The three Subdomains belong to one unified model:

```text
General Ledger Bounded Context
├── Account Head
├── Account
└── Journal Entry
```

Rationale:

1. All three belong to the same `Problem Space`.
2. Their domain language is strongly interconnected.
3. `Journal Entry` requires `Account`.
4. `Account` derives meaning from the General Ledger structure and `Account Head`.
5. Splitting them solely because they are separate Subdomains would add unnecessary complexity without a genuine model boundary.
6. The current primary model boundary is `General Ledger`.

## Microservice Decision

The `General Ledger Bounded Context` is implemented as one independently deployable Microservice:

```text
General Ledger Bounded Context
            │
            ▼
General Ledger Microservice
```

Current architecture:

```text
1 Bounded Context
        ↓
1 Microservice
```

This is a system-specific architectural decision, **not** a general DDD rule that:

```text
Bounded Context = Microservice
```

`Bounded Context` defines the domain-model boundary; `Microservice` defines deployment/runtime isolation.

## Tactical DDD Structure

The General Ledger Microservice contains three primary Aggregate Roots:

```text
General Ledger Microservice
├── Account Head Aggregate
│   └── AccountHead
├── Account Aggregate
│   └── Account
└── Journal Entry Aggregate
    └── JournalEntry
```

Therefore:

* `AccountHead` = Aggregate Root
* `Account` = Aggregate Root
* `JournalEntry` = Aggregate Root
* `JournalEntry` = Core Aggregate

## Aggregate Responsibilities

### AccountHead Aggregate

Maintains invariants related to Account Head structure:

```text
AccountHead
├── identity
├── hierarchy
├── classification
└── placement rules
```

### Account Aggregate

Maintains invariants related to Account:

```text
Account
├── identity
├── account type
├── status
└── account rules
```

### JournalEntry Aggregate

The most important Aggregate from a domain-value perspective.

Responsible for:

```text
JournalEntry
├── debit/credit validation
├── balancing validation
├── journal-line validation
├── posting-rule validation
├── posting-state determination
└── posting
```

Core and differentiating Business Rules should remain inside `JournalEntry` and its related Domain Model whenever possible.

## Core Domain Rule

`Journal Entry` being the Core Subdomain is an architectural constraint, not merely a label.

Do **not** turn it into:

```text
CRUD Service
```

Do **not** scatter its Business Rules across:

```text
Controller
Application Service
Repository
Infrastructure
```

Preferred responsibility:

```text
JournalEntry Aggregate
        │
        ▼
Domain Rules
        │
        ▼
Business Invariants
```

## Aggregate ≠ Domain Service

The existence of three Aggregates does not imply three corresponding Domain Services.

Do not automatically create:

```text
AccountHeadService
AccountService
JournalEntryService
```

as the primary location for Business Logic.

Default rule:

```text
Business Rule
      │
      ▼
Aggregate
```

A `Domain Service` is appropriate only when a Business Rule is genuinely cross-Aggregate and cannot reasonably belong to one Aggregate without inappropriate coupling.

## Aggregate Interaction

Aggregates should remain as independent as practical.

Avoid designs where a Domain Entity/Aggregate directly accesses another Aggregate's Repository:

```text
JournalEntry
      │
      ▼
AccountRepository
```

Cross-Aggregate coordination should normally occur in the `Application Layer`.

Example:

```text
CreateJournalEntry
        │
        ├── validate Account
        ├── validate AccountHead
        └── create JournalEntry
```

The `Application Layer` coordinates the Use Case; `Domain` remains the owner of Journal Entry Business Rules.

## Repository Ports

Repository Ports may be defined per Aggregate:

```text
AccountHeadRepository
AccountRepository
JournalEntryRepository
```

Repository Ports are abstractions; their implementations belong to `Infrastructure`.

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

Aggregates must not depend on persistence implementations.

## Repository Port ≠ Internal Contract / Port

Not every interface in `Domain` or `Application` is a Repository.

```text
Repository Port
└── Persistence abstraction

Internal Contract / Port
└── Communication / capability abstraction
```

Keep these concepts separate to prevent incorrect `Domain` → `Infrastructure` coupling.

## Module Structure

Modules may represent Aggregate or Business boundaries without becoming separate `Bounded Context`s or `Microservice`s.

```text
general-ledger
├── account-head
├── account
└── journal-entry
```

All modules belong to:

```text
General Ledger Bounded Context
        │
        ▼
General Ledger Microservice
```

## Architectural Hierarchy

```text
Problem Space
└── General Ledger
    ├── Subdomain: Account Head
    ├── Subdomain: Account
    └── Subdomain: Journal Entry ⭐ Core
            │
            ▼
      Bounded Context
      └── General Ledger Bounded Context
              │
              ▼
          Microservice
          └── General Ledger Microservice
                  │
                  ├── Module: Account Head
                  │   └── AccountHead Aggregate
                  │
                  ├── Module: Account
                  │   └── Account Aggregate
                  │
                  └── Module: Journal Entry
                      └── JournalEntry Aggregate
```

## Decision Matrix

| Level                 | Decision                             |
| --------------------- | ------------------------------------ |
| Problem Space         | General Ledger                       |
| Subdomains            | Account Head, Account, Journal Entry |
| Core Subdomain        | Journal Entry                        |
| Supporting Subdomains | Account Head, Account                |
| Bounded Context       | General Ledger                       |
| Microservice          | General Ledger                       |
| Aggregate Roots       | AccountHead, Account, JournalEntry   |
| Core Aggregate        | JournalEntry                         |
| Modules               | Account Head, Account, Journal Entry |
| Repositories          | Aggregate-oriented                   |
| Domain Logic          | Aggregates and related Domain Model  |
| Orchestration         | Application Layer                    |
| Persistence           | Infrastructure                       |

## Future Decision Rules

### Rule 1 — Do Not Equate Subdomain with Bounded Context

A new Subdomain alone is insufficient justification for creating a new `Bounded Context`.

### Rule 2 — Do Not Equate Bounded Context with Microservice

The current General Ledger `Bounded Context` is deployed as one Microservice, but this is a system-specific architectural decision.

### Rule 3 — Keep the Core Domain Actually Core

`Journal Entry` is the primary competitive domain. Its Business Logic must not become CRUD logic or merely orchestration logic.

### Rule 4 — Aggregate Is the Consistency Boundary

Each `Aggregate Root` is responsible for maintaining its own invariants.

### Rule 5 — Aggregates Must Not Call Repositories

Persistence must be handled outside Aggregates through appropriate Ports.

### Rule 6 — Application Layer Coordinates

`Application Layer` may coordinate multiple Aggregates for a Use Case, but must not become the owner of Domain Business Rules.

### Rule 7 — Supporting Subdomains Must Support the Core

`Account` and `Account Head` must support `Journal Entry` without introducing unnecessary coupling or complexity into the Core Domain.

## Agent Instructions

When making or reviewing a General Ledger architectural decision:

1. Treat this document as the current architectural baseline.
2. Preserve the distinction between `Subdomain`, `Bounded Context`, `Module`, and `Microservice`.
3. Assume one `General Ledger Bounded Context` containing `Account Head`, `Account`, and `Journal Entry`.
4. Assume one `General Ledger Microservice` for the current architecture.
5. Treat `Journal Entry` as the Core Subdomain and Core Aggregate.
6. Keep `JournalEntry` Business Rules inside the Domain Model whenever possible.
7. Keep Aggregate invariants inside their respective Aggregates.
8. Do not introduce Domain Services merely because Aggregates exist.
9. Use `Domain Service` only for genuinely cross-Aggregate Business Rules.
10. Do not let Aggregates directly access Repositories.
11. Use `Application Layer` for cross-Aggregate Use Case orchestration.
12. Keep persistence implementations in `Infrastructure`.
13. Distinguish `Repository Port` from other `Internal Contract / Port`s.
14. Do not create a new Bounded Context or Microservice solely because a new Subdomain or Module appears.
15. If changing these architectural boundaries, record the rationale as a new architectural decision before updating this baseline.

## Final Mental Model

```text
                    GENERAL LEDGER
                    Problem Space
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
     Account Head     Account      Journal Entry
      Supporting     Supporting        CORE
      Subdomain      Subdomain      Subdomain
          │              │              │
          └──────────────┼──────────────┘
                         │
                         ▼
              General Ledger
               Bounded Context
                         │
                         ▼
              General Ledger
                Microservice
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
     AccountHead       Account      JournalEntry
      Aggregate       Aggregate       Aggregate
```

### Key Principle

> Three Subdomains do **not** imply three Bounded Contexts.

Current design:

```text
3 Subdomains
      ↓
1 General Ledger Bounded Context
      ↓
1 General Ledger Microservice
      ↓
3 primary Aggregate Roots
      ├── AccountHead
      ├── Account
      └── JournalEntry ⭐ Core
```

## Status

**Accepted — Current Architectural Baseline**

This baseline must be reviewed before decisions concerning:

* `Bounded Context`
* `Microservice` boundaries
* `Module` boundaries
* `Aggregate` boundaries
* Repository design
* `Domain Service`
* `Application Service`
* Inter-module communication

If these decisions change, the rationale must first be recorded as a new architectural decision, followed by an update to this baseline.
