# ADR-0018 — Domain Identity Strategy  

## Status

**Accepted** — 2026-08-22

## Purpose

Define a Domain-owned Identity strategy that is:

* Independent of Persistence, JPA, and Hibernate.
* Separate from Business Identifier and Persistence Identifier.
* Immutable throughout an Entity lifecycle.
* Strongly typed.
* Suitable for Unit Testing without Database.
* Compatible with Modular Monolith and future Distributed/Microservices architectures.
* Explicit for Aggregate Root identification and Aggregate references.

## Core Decision

Use **Domain-Owned, Immutable, Typed Identity**.

Each important Domain Identity is a dedicated immutable Type and is directly modeled as a **Value Object**.

```text
Value Object
├── AccountId
├── AccountHeadingId
└── JournalEntryId
```

There is **no shared `DomainId` abstraction** in the current model.

```text
Value Object
├── AccountId
├── AccountHeadingId
└── JournalEntryId
```

Not:

```text
Value Object
└── DomainId
    ├── AccountId
    ├── AccountHeadingId
    └── JournalEntryId
```

A shared `DomainId` may only be introduced by a separate ADR if a real common behavior or contract emerges.

## Identity Rules

### Typed Identity

Important Domain identities MUST have dedicated Types.

```java
public record AccountId(UUID value) {
    public AccountId {
        Objects.requireNonNull(value, "AccountId cannot be null");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
```

`AccountId` and `JournalEntryId` are different Domain concepts and Types.

```text
AccountId != JournalEntryId
```

This prevents semantic errors such as passing an `AccountId` where a `JournalEntryId` is required.

### Identity as Value Object

Typed identities are directly `Value Object`s.

* Identity equality is value-based.
* Identity is immutable.
* Equal underlying values produce equal Identity objects.
* No intermediate `DomainId` layer exists.

```text
AccountId(UUID-A) == AccountId(UUID-A)
AccountId(UUID-A) != AccountId(UUID-B)
```

### Identity Ownership

An Entity owns its Identity.

```java
public class Account {
    private final AccountId id;

    public Account(AccountId id) {
        this.id = Objects.requireNonNull(id);
    }

    public AccountId id() {
        return id;
    }
}
```

Identity SHOULD be represented as `private final`.

### Identity Immutability

Identity MUST NOT change after Entity creation.

```text
AccountId=A-100, Balance=1000
        ↓
AccountId=A-100, Balance=1500   ✓
```

```text
AccountId=A-100
        ↓
AccountId=A-200                 ✗
```

Changing Identity means representing a different Entity, not changing the same Entity.

## Aggregate Identity

Every Aggregate Root MUST have its own independent Identity.

```text
AccountHeading → AccountHeadingId
Account         → AccountId
JournalEntry    → JournalEntryId
```

Aggregate Root Identity is the mechanism for identifying the Aggregate.

## Internal Entity Identity

Entities inside an Aggregate MAY have Identity when genuinely required.

```text
JournalEntry
├── JournalEntryId              → Aggregate Root Identity
├── JournalEntryLine
│   └── JournalEntryLineId      → Local Entity Identity
└── JournalEntryLine
    └── JournalEntryLineId      → Local Entity Identity
```

Internal Entity Identity is **not necessarily Global Identity**.

If an internal Entity can be modeled without Identity, do not add an artificial Identity merely for structural consistency.

## Aggregate References

Independent Aggregates MUST NOT be referenced through direct Object References.

Use Typed Identity:

```java
public class Account {
    private AccountHeadingId accountHeadingId;
}
```

Not:

```java
public class Account {
    private AccountHeading accountHeading;
}
```

Preferred model:

```text
Account
└── AccountHeadingId
        ↓
   AccountHeading
```

This preserves Aggregate Boundaries and reduces coupling, Object Graph size, ORM leakage, and unintended cross-Aggregate transactions.

## Domain Identity vs Business Identifier

Domain Identity and Business Identifier are distinct concepts.

Example:

```text
AccountId      → Domain Identity
AccountNumber  → Business Identifier / Value Object
IBAN           → Business Identifier / Value Object
```

A unique Business Identifier is **not automatically** Domain Identity.

Business Identifier MUST NOT be treated as Domain Identity by default.

It may become Identity only when the Domain explicitly defines it as such.

## Domain Identity vs Persistence Identifier

Persistence Identity and Domain Identity are independent.

```text
Domain
AccountId = UUID
      ↓
Persistence Mapping
      ↓
Database
ID = BIGINT
```

Domain MUST NOT depend on:

```java
@Id
@GeneratedValue
```

JPA/Hibernate Identity generation and Database Primary Keys belong to Persistence/Infrastructure.

The Domain MUST NOT know:

* How Database IDs are generated.
* Sequences.
* Identity columns.
* Hibernate Entity management.
* JPA Identity persistence mechanisms.

## Persistence Mapping

Persistence Layer owns the mapping between Domain Identity and Persistence Identifier.

```text
Domain Model
    ↓
AccountId
    ↓
Persistence Mapper
    ↓
Database Identifier
    ↓
Database
```

Domain MUST NOT depend on:

```text
Domain
  ↓
JPA Entity
  ↓
Database
```

## Identity Generation

**UUID is the default Domain Identity generation strategy.**

Identity SHOULD be generated before Persistence:

```text
Create Aggregate
      ↓
Generate Identity
      ↓
Create Domain Aggregate
      ↓
Persist
```

Therefore Domain Identity creation does not require Database access.

UUID is selected because it:

* Can be generated without Database.
* Works well in Distributed environments.
* Does not require a centralized ID generator.
* Allows Identity creation before Persistence.
* Avoids Domain dependency on Database Sequence/Auto Increment.
* Has very low collision probability when correctly used.

UUID is the default, **not an absolute prohibition on other strategies**. Domain-specific alternatives require an independent architectural decision.

## Value Object Rule

Value Objects MUST NOT have independent Identity.

```text
Entity
→ Identity-based

Value Object
→ Value-based
```

Correct:

```java
public record Money(
    BigDecimal amount,
    Currency currency
) {}
```

Incorrect:

```java
public class Money {
    private UUID id; // WRONG
}
```

Do not add artificial IDs to Value Objects.

## Entity Equality

Entity equality is based on Domain Identity.

```text
Account(id=A-100, balance=1000)
Account(id=A-100, balance=2000)
```

These represent the same Entity.

Different Identity means different Entity:

```text
Account(id=A-100)
Account(id=A-200)
```

Exact `equals`/`hashCode` implementation MUST follow **ADR-0012 — Entity & Aggregate Strategy**.

Value Object equality remains value-based.

## External System Identity

External identifiers MUST remain separate from Domain Identity.

```text
Payment
├── PaymentId              → Domain Identity
└── ExternalTransactionId  → External System Identifier
```

The Domain owns `PaymentId`; the external system owns `ExternalTransactionId`.

External System Identity MUST NOT directly replace Domain Identity.

## General Ledger Model

Example:

```java
public class Account {
    private final AccountId id;
    private final AccountHeadingId accountHeadingId;
    private Money balance;

    public Account(
        AccountId id,
        AccountHeadingId accountHeadingId,
        Money balance
    ) {
        this.id = Objects.requireNonNull(id);
        this.accountHeadingId = Objects.requireNonNull(accountHeadingId);
        this.balance = Objects.requireNonNull(balance);
    }

    public AccountId id() {
        return id;
    }

    public AccountHeadingId accountHeadingId() {
        return accountHeadingId;
    }

    public Money balance() {
        return balance;
    }
}
```

Model:

```text
Account
├── AccountId
│   └── Aggregate Identity
├── AccountHeadingId
│   └── Reference to another Aggregate
└── Money
    └── Value Object
```

## Architectural Rules

1. Every Entity MUST have explicit Identity.
2. Entity Identity MUST NOT change after creation.
3. Every Aggregate Root MUST have independent Identity.
4. Internal Entity Identity is allowed only when genuinely required.
5. References between independent Aggregates MUST use Typed Identity.
6. Value Objects MUST NOT have independent Domain Identity.
7. Important Domain identities MUST be strongly typed.
8. Domain Identity MUST NOT depend on JPA/Hibernate.
9. Database Primary Key is not necessarily Domain Identity.
10. Business Identifier is not necessarily Domain Identity.
11. Domain Identity generation MUST NOT depend on Database.
12. Entity equality MUST be Identity-based; Value Object equality MUST be Value-based.
13. Typed Domain Identities MUST directly be modeled as Value Objects.
14. The current model MUST NOT introduce a shared `DomainId` abstraction.
15. Independent Aggregates MUST NOT hold direct Object References to each other.

## `DomainId` Decision

A shared abstraction such as:

```java
public interface DomainId {}
```

or:

```java
public interface DomainId extends ValueObject {}
```

is **Rejected** in the current model.

Reason: Identity Types currently have no meaningful shared behavior or contract.

Do not create an abstraction merely to eliminate structural repetition.

A shared Identity abstraction may be reconsidered in a separate ADR if real common behavior or contract emerges.

## Alternatives Rejected

### Primitive IDs

```java
private Long id;
```

**Rejected** because they:

* Reduce Type Safety.
* Hide Domain Identity semantics.
* Make different identities the same Type.
* Increase semantic errors in APIs and Repositories.

### Database-Generated IDs

```java
@Id
@GeneratedValue
private Long id;
```

**Rejected as Domain Strategy.**

Database-generated IDs MAY exist in Persistence, but Domain Identity MUST remain Persistence-independent.

### Business Identifier as Identity

**Rejected as general strategy.**

A Business Identifier may change, originate externally, be scoped to one `Bounded Context`, or simply represent a Business attribute.

It is Identity only when explicitly defined as such by the Domain.

### Identity on Every Object

**Rejected.**

Value Objects do not have independent Identity. Artificial IDs would incorrectly turn Value Objects into Entities.

### Direct Aggregate Object References

**Rejected.**

They weaken Aggregate Boundaries, increase coupling/Object Graph size, risk ORM/Lazy Loading leakage, and can create unintended cross-Aggregate transactions.

### Shared `DomainId`

**Rejected.**

No meaningful common behavior or contract currently justifies the abstraction.

## Consequences

### Benefits

* **Domain Independence:** no Database/ORM dependency.
* **Type Safety:** `AccountId`, `AccountHeadingId`, and `JournalEntryId` prevent semantic ID misuse.
* **Aggregate Boundaries:** Aggregates communicate through Identity.
* **Distributed Ready:** Identity can be generated independently on different Nodes.
* **Testability:** Domain objects can be created and tested without Spring/Database.
* **Simple Identity Model:** Typed identities are direct Value Objects without unnecessary `DomainId`.

### Costs

* More Identity Types.
* Persistence mapping between Domain and Database identifiers is required.
* UUID may consume more storage and have Indexing implications than `BIGINT`.
* Similar Identity implementations may repeat null checks and `generate()` methods.
* This repetition is intentionally accepted until real shared behavior emerges.

## Implementation Rules

Identity Types SHOULD be immutable and follow this pattern:

```java
public record AccountId(UUID value) {
    public AccountId {
        Objects.requireNonNull(
            value,
            "AccountId cannot be null"
        );
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
```

Aggregate:

```java
public class Account {
    private final AccountId id;

    public Account(AccountId id) {
        this.id = Objects.requireNonNull(id);
    }

    public AccountId id() {
        return id;
    }
}
```

Persistence:

```text
Domain
  ↓
AccountId
  ↓
Persistence Mapper
  ↓
AccountJpaEntity
  ↓
Database
```

## Testing Rules

Domain Unit Tests MUST be able to create Identity without Database.

```java
@Test
void should_create_account_with_identity() {
    AccountId accountId = AccountId.generate();
    Account account = new Account(accountId);

    assertThat(account.id()).isEqualTo(accountId);
}
```

Identity equality MUST be value-based:

```java
@Test
void account_ids_with_same_value_should_be_equal() {
    UUID value = UUID.randomUUID();

    AccountId first = new AccountId(value);
    AccountId second = new AccountId(value);

    assertThat(first).isEqualTo(second);
}
```

Entity equality tests MUST follow ADR-0012.

## Architectural Validation

ArchUnit SHOULD prevent Domain dependency on Persistence/Infrastructure:

```java
noClasses()
    .that()
    .resideInAPackage("..domain..")
    .should()
    .dependOnClassesThat()
    .resideInAnyPackage(
        "..infrastructure..",
        "..persistence.."
    );
```

The objective is to guarantee that Identity Strategy remains owned by Domain.

## Agent Instructions

When designing or reviewing Domain models:

1. Identify the Identity of every Entity.
2. Identify every Aggregate Root Identity.
3. Use dedicated Typed Identity Types for important Domain identities.
4. Model Typed Identities directly as immutable Value Objects.
5. Do NOT introduce `DomainId` unless a separate ADR explicitly approves it.
6. Keep Identity immutable after Entity creation.
7. Distinguish Domain Identity from Business Identifier.
8. Distinguish Domain Identity from Persistence Identifier.
9. Never make Domain Identity dependent on JPA/Hibernate/Database generation.
10. Prefer UUID as the default Identity generation strategy.
11. Generate Identity independently of Persistence when appropriate.
12. Reference independent Aggregates through Typed Identity, never direct Object References.
13. Do not add Identity to Value Objects.
14. Give internal Entities Identity only when genuinely required.
15. Do not assume internal Entity Identity is Global.
16. Follow ADR-0012 for Entity equality.
17. Follow ADR-0013 for Value Object semantics.
18. Keep Persistence mapping outside Domain.
19. Preserve Aggregate Boundaries.
20. If a new shared Identity behavior/contract emerges, propose a separate ADR rather than silently introducing `DomainId`.

## Decision Summary

```text
Domain Identity
      ↓
Domain-Owned
      ↓
Immutable
      ↓
Typed
      ↓
Value Object
      ↓
Entity / Aggregate
```

Current Identity Types:

```text
AccountId
AccountHeadingId
JournalEntryId
```

Current policy:

```text
Shared DomainId
    → Not Used

Default Identity Generation
    → UUID

Persistence Identity
    → Infrastructure Concern

Business Identifier
    → Separate from Domain Identity

Aggregate Reference
    → Typed Identity

Value Object
    → No Identity
```

For independent Aggregates:

```text
Aggregate A
    ↓
AggregateBId
    ↓
Aggregate B
```

Never:

```text
Aggregate A
    ↓
Aggregate B Object
```

## Implementation Status

The Identity Strategy applies to all Domain Modules.

For every Entity/Aggregate, explicitly determine:

1. What is its Identity?
2. Is the Identity a Business Identifier or only Domain Identity?
3. Must the Identity be Typed?
4. Is the Entity an Aggregate Root?
5. Does it reference another Aggregate?
6. Should that reference use Typed Identity?
7. Should Identity be generated before Persistence?

Current baseline:

```text
Domain Identity
→ Domain-Owned
→ Immutable
→ Typed
→ Value Object

Shared DomainId
→ Not Used

Default Identity Generation
→ UUID

Persistence Identity
→ Infrastructure Concern

Business Identifier
→ Separate from Domain Identity
```
