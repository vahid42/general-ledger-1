# ADR-0012 — Entity & Aggregate Strategy 

## Purpose

Define Entity and Aggregate strategy for the **General Ledger Bounded Context**, including:

* Entity identity and lifecycle.
* Aggregate boundaries and Aggregate Root responsibilities.
* Business Invariant protection.
* Cross-Aggregate interaction and consistency.
* Repository and transaction boundaries.
* Persistence independence.
* Aggregate-oriented Domain packaging.

## Architecture Context

```text
General Ledger Bounded Context
│
├── AccountHead Aggregate    → Supporting Aggregate
├── Account Aggregate        → Supporting Aggregate
└── JournalEntry Aggregate   → Core Aggregate
```

All three Aggregates belong to the same:

```text
General Ledger Bounded Context
        ↓
General Ledger Microservice
```

```text
Aggregate ≠ Module ≠ Bounded Context ≠ Microservice
```

Three Aggregates do **not** imply three Bounded Contexts, and three Modules do **not** imply three Microservices.

## Core Rules

### Entity

* Entity has **stable Identity** and an explicit Lifecycle.
* Entity is identified by Identity, not by current Attribute values.
* Entity Identity remains stable while State may change.
* Every Domain concept is not necessarily an Entity; concepts without independent Identity/Lifecycle and defined by value should be modeled as Value Objects.
* Entity must contain relevant Business Behavior and must not become a pure Data Holder.
* Business State should change through Domain Behavior, not public Setters.
* Public Setter is allowed only when it does not represent Business Behavior or bypass a Business Rule.
* Entity Equality is based on Identity, not all Attributes.
* Typed Identity is preferred, e.g. `AccountId`, instead of exposing primitive/`UUID` values throughout Domain.
* Entity must have a valid Lifecycle and State transitions must enforce relevant Business Rules.

```java
account.withdraw(amount);
account.activate();
account.close();
```

instead of:

```java
account.setBalance(...);
account.setStatus(...);
```

### Aggregate

An Aggregate is a consistency boundary containing Entities and Value Objects required to protect a set of Business Invariants.

```text
Aggregate
└── Aggregate Root
    ├── Entity
    ├── Entity
    └── Value Object
```

Aggregate is the primary boundary for:

* Consistency
* Invariant Protection
* State Transition
* Domain Behavior
* Transaction Coordination

Aggregate must be modeled from the **Business Model**, not from Database or ORM structure.

## Aggregate Root Rules

* Every Aggregate has **exactly one Aggregate Root**.
* Aggregate Root is the only public entry point for changing internal Aggregate State/Behavior.
* External code must not directly mutate internal Entities.
* Internal Entity State Transitions must be controlled by the Aggregate Root.
* Aggregate Root must enforce Aggregate Invariants.
* Aggregate must be valid immediately after creation.
* Factory Method or Constructor must enforce initial Invariants.
* Aggregate must not invoke Repository directly.

```text
Application
    ↓
Aggregate Root
    ↓
Internal Entities / Value Objects
```

Never:

```text
Application ──→ Internal Entity
```

## General Ledger Aggregates

### AccountHead Aggregate

Supporting Aggregate responsible for Invariants related to Account Head structure, including:

* Identity
* Hierarchy
* Classification
* Placement
* Account-head rules

### Account Aggregate

Supporting Aggregate responsible for Account Invariants, including:

* Identity
* Account type
* Status
* Account properties
* Account rules

Typical behavior:

```java
account.activate();
account.deactivate();
account.close();
```

### JournalEntry Aggregate

**Core Aggregate** and primary location for Core Business Logic related to Journal Entry creation and posting.

Responsibilities include:

* Debit/Credit validation.
* Journal balancing.
* Journal Line validation.
* Posting Rules.
* Posting State determination.
* Posting behavior.

```text
JournalEntry
    ↓
Business Behavior
    ↓
Business Invariants
```

`JournalEntry` must **not** become a CRUD Entity. Core Business Logic must not be scattered across Controller, Application Service, Repository, or Infrastructure.

## Aggregate Boundary Rules

The primary criterion for defining an Aggregate Boundary is **Business Invariant**.

Evaluate:

1. Do the concepts share a Business Invariant?
2. Must they always remain Consistent?
3. Must changes be controlled by one Root?
4. Must they change as one Domain unit?
5. Would combining them create excessive Coupling?
6. Is the resulting Aggregate size/Complexity acceptable?

```text
Business Invariant
        ↓
Consistency Requirement
        ↓
Aggregate Boundary
```

Business relationship alone does **not** define an Aggregate Boundary.

## Aggregate Size

Aggregate should be the **smallest Boundary capable of reliably protecting its required Invariants**.

Avoid oversized Aggregates because they increase:

* Locking
* Transaction Scope
* Coupling
* Complexity
* Performance cost
* Concurrency contention

Aggregate must remain small and Cohesive.

## Aggregate Interaction

* Aggregates should remain independent.
* Do not keep a complete object graph of another Aggregate.
* Prefer referencing another Aggregate through Identity or an appropriate Contract.

Preferred:

```java
private AccountId accountId;
```

Avoid:

```java
private Account account;
```

Cross-Aggregate relationship does not imply a shared Aggregate Boundary.

## JournalEntry → Account / AccountHead

`JournalEntry` may require `Account` and `AccountHead` information during its Use Cases, but this does not make them one Aggregate.

Preferred orchestration:

```text
CreateJournalEntry
    ├── load / validate Account
    ├── validate AccountHead
    └── create JournalEntry
```

Application Layer owns orchestration between Aggregates.

Not permitted as a default pattern:

```text
JournalEntry
    └── AccountRepository
```

Aggregate must not invoke Repository to obtain or manipulate another Aggregate.

## Repository Boundary

Repository is defined **only for Aggregate Root**.

Correct:

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

Incorrect when `JournalLine` is an internal Entity:

```text
JournalEntryRepository
JournalLineRepository
```

Repository persists the Aggregate as a Domain unit; internal Entities are not independently persisted through their own Repository abstraction.

## Transaction & Consistency

Aggregate is a **Consistency Boundary**.

Normally, a Transaction should manage an Aggregate's changes as one Consistency unit.

However, a Use Case may coordinate multiple Aggregates through Application Layer when required. The rule is not "every Transaction must touch exactly one Aggregate"; the goal is to avoid unnecessarily large Transactions for consistency that does not require a shared Aggregate.

For cross-Aggregate Business Rules:

1. Re-evaluate whether the Aggregate Boundary is correct.
2. If atomic consistency is genuinely required, reconsider the Boundary.
3. If Aggregate independence is correct, use mechanisms such as:

   * Application Orchestration
   * Domain Event
   * Eventual Consistency
   * Process Manager
   * Saga

These mechanisms must not hide an incorrectly designed Aggregate Boundary.

## Invariant Protection

Aggregate Root must prevent external code from bypassing Business Rules.

```java
public void withdraw(Money amount) {
    if (balance.isLessThan(amount)) {
        throw new InsufficientBalanceException();
    }
    balance = balance.subtract(amount);
}
```

For `JournalEntry`, this is especially critical for balancing and Posting Rules.

State mutation must use Domain Behavior:

```java
journalEntry.post();
journalEntry.cancel();
```

not:

```java
journalEntry.setStatus(POSTED);
```

## Aggregate Creation

Aggregate must be valid immediately after creation.

Avoid:

```java
new Account(null, null, null, null);
```

Prefer:

```java
Account.open(accountId, accountType, currency);
```

Constructor/Factory must enforce initial Invariants.

## Persistence Independence

Domain Entity and Aggregate must not depend on ORM or Database structure.

Domain must not depend on:

* JPA Entity
* Hibernate Proxy
* Lazy Loading
* ORM Lifecycle
* Database Relationships
* Persistence Callbacks

Persistence Mapping belongs to Infrastructure:

```text
Domain Aggregate
      ↓
Persistence Adapter
      ↓
Persistence Model
      ↓
Database
```

Never design Aggregate boundaries from:

* Tables
* Foreign Keys
* Joins
* ORM Mapping
* Lazy Loading
* Database Relationships

Correct direction:

```text
Business Model
    ↓
Aggregate Boundary
    ↓
Persistence Mapping
    ↓
Database
```

## Aggregate Design Criteria

Aggregate decisions must consider:

1. Business Invariant
2. Consistency Requirement
3. Transaction Requirement
4. State Ownership
5. Aggregate Root Responsibility
6. Concurrency
7. Performance
8. Coupling
9. Lifecycle
10. Business Meaning

Database/Object Graph structure alone is never sufficient.

## Aggregate-Oriented Packaging

Code structure must make Aggregate boundaries visible:

```text
domain/
├── common/
├── shared/
├── account-head/
├── account/
└── journal-entry/
```

These Packages/Modules remain inside the same **General Ledger Bounded Context** and **General Ledger Microservice**.

## Domain Service Rule

An Aggregate does not imply a corresponding Domain Service.

Do not create services merely because Aggregates exist:

```text
AccountHeadService
AccountService
JournalEntryService
```

Business Rule belongs in the Aggregate first.

Use a Domain Service only when:

* The Rule is genuine Domain Logic.
* It does not naturally belong to one Aggregate.
* Placing it inside an Aggregate would create inappropriate Coupling.

## Domain Event Rule

Aggregate may emit a Domain Event for significant Domain changes:

```text
JournalEntry
    │ post()
    ↓
JournalEntryPosted
```

Domain Event is a Domain concept and must not depend directly on a Message Broker.

Messaging publication and Infrastructure interaction occur outside the Aggregate.

## Decision Rules

When making future Entity/Aggregate decisions:

1. Start with Business Invariant.
2. Determine required Consistency.
3. Determine State ownership.
4. Evaluate Transaction requirements.
5. Define the smallest valid Aggregate Boundary.
6. Assign exactly one Aggregate Root.
7. Keep Aggregates independent where possible.
8. Use Identity/Contract for Cross-Aggregate references.
9. Define Repository only for Aggregate Root.
10. Keep Domain independent of ORM and Database.
11. Re-evaluate Boundary before introducing Cross-Aggregate consistency mechanisms.
12. Record Aggregate Boundary changes as a new architectural decision.

## Agent Instructions

When analyzing or modifying the General Ledger Domain:

* Treat `JournalEntry` as the **Core Aggregate**.
* Treat `AccountHead` and `Account` as **Supporting Aggregates**.
* Do not create new Bounded Contexts or Microservices merely because a new Aggregate/Module exists.
* Never place Business Rules in Application Layer when they belong to an Aggregate.
* Never bypass Aggregate Root to mutate internal Entity State.
* Never create Repository abstractions for internal Entities.
* Never inject or invoke Repository implementations from Aggregates.
* Do not design Aggregate boundaries from Database/ORM relationships.
* Prefer small, Cohesive Aggregates.
* Prefer Identity-based Cross-Aggregate references.
* Preserve Domain independence from JPA/Hibernate and Persistence concerns.
* Before changing an Aggregate Boundary, evaluate Business Invariant, Consistency, Transaction, Concurrency, Coupling, Performance, Lifecycle, and Business Meaning.
* Any intentional Aggregate Boundary change must be documented as a new architectural decision.

## Implementation Status

Current General Ledger Aggregate model:

```text
General Ledger Bounded Context
│
├── AccountHead
│   └── Supporting Aggregate
│
├── Account
│   └── Supporting Aggregate
│
└── JournalEntry
    └── Core Aggregate
```

Current Domain structure:

```text
domain/
├── common/
├── shared/
├── account-head/
├── account/
└── journal-entry/
```

This ADR is the official Entity & Aggregate strategy for the **General Ledger Bounded Context**.
