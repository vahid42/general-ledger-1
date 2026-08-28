# ADR-0013 — Value Object Strategy  

## Purpose

Define how Value Objects are identified, designed, validated, used, persisted, and distinguished from Entity/Aggregate in the **General Ledger Bounded Context**.

## Core Rules

* A **Value Object** represents a Domain concept with:

  * no independent Identity;
  * value-based definition;
  * Business Meaning and/or Business Rules.
* Use Value Objects for meaningful Domain concepts instead of raw primitives to reduce **Primitive Obsession**.
* Examples: `Money`, `Currency`, `AccountNumber`, `Percentage`, `DateRange`, `FiscalPeriod`.
* Not every primitive requires a Value Object; simple values such as `retryCount` or `active` do not automatically qualify.
* Value Object names must come from the **Ubiquitous Language**, not implementation types such as `StringValue` or `NumberWrapper`.

## Identity Value Objects

* Entity and Aggregate Root Identity must be modeled using Domain-specific, type-safe Value Objects.
* Examples:

  * `Account → AccountId`
  * `AccountHead → AccountHeadId`
  * `JournalEntry → JournalEntryId`
* `AccountId`, `AccountHeadId`, and `JournalEntryId` are Value Objects, **not Entity or Aggregate**.
* Identity Value Objects:

  * have no independent Identity;
  * have no independent Lifecycle;
  * have no Repository;
  * are part of the owning Entity/Aggregate State;
  * use value-based Equality;
  * must be Immutable and Domain-specific.
* Aggregate boundaries are determined by **Business Invariants and Consistency Boundaries**, not by Identity Value Objects.

## Immutability & Equality

* Value Objects must be **Immutable**.
* Domain operations should return new instances rather than mutate existing State.
* Equality must be based on all semantically significant values, never object reference.
* Java `record` is preferred for simple Value Objects, but is not mandatory; complex Value Objects may use regular classes.

## Validation & Invariants

* A Value Object must never allow invalid internal State.
* Creation through Constructor/Factory must enforce its Invariants.
* The Domain must be able to trust a successfully created Value Object.
* Examples:

  * `Percentage`: value must be within its defined valid range.
  * `DateRange`: `start <= end`.
  * `AccountNumber`: format and Domain constraints must be enforced.
  * `Money`: amount/currency validity and compatible-currency operations must follow explicit Business Rules.
* Business behavior and rules specific to a Value should reside in the Value Object whenever practical.
* Value Objects should not be passive primitive wrappers.

## Nullability

* Required Domain concepts should not normally be represented by invalid `null` State.
* Optionality is allowed when it is a real Business concept.
* Optional/mandatory status must be determined by the Domain model, not technical convenience.

## Entity vs Value Object

| Concern    | Entity                         | Value Object             |
| ---------- | ------------------------------ | ------------------------ |
| Identity   | Independent Identity           | No independent Identity  |
| Equality   | Identity-based                 | Value-based              |
| Lifecycle  | May have independent Lifecycle | No independent Lifecycle |
| Mutability | May be Mutable                 | Immutable                |
| Example    | `Account`                      | `Money`                  |

Decision rule:

```text
Domain Concept
      │
      ▼
Independent Identity?
   ┌──┴──┐
  Yes    No
   │      │
 Entity   ▼
       Business Meaning/Rules?
         ┌──┴──┐
        Yes    No
         │      │
         ▼      ▼
 Value Object Primitive
```

A concept with independent Identity, Lifecycle, Invariants, and Repository requirements must be reconsidered as an **Entity/Aggregate**, not a Value Object.

## Aggregate Rules

* Value Objects normally form part of Aggregate State.
* A Value Object does not become an Entity/Aggregate merely because multiple Aggregates use it.
* Value Objects must not create or cross Aggregate Boundaries.
* `Money`, `Currency`, `AccountNumber`, `Percentage`, and Identity Value Objects are not Aggregate Roots.
* A Value Object must not have an independent Repository.
* Examples of prohibited standalone repositories:

  * `MoneyRepository`
  * `CurrencyRepository`
  * `AccountNumberRepository`
  * `AccountIdRepository`
* If independent Repository/Lifecycle is required, reassess the concept's Domain classification.

## Shared Value Objects

* Reuse across Aggregates does **not** automatically justify a Shared Kernel.
* Do not create a generic `shared` package merely to avoid duplication.
* Shared Domain Code must represent concepts genuinely shared by the relevant model boundaries.
* `Shared != Generic Common Types`.

## Framework Independence

* Value Objects must remain **Framework Independent**.
* They must not depend on:

  * Spring
  * JPA
  * Hibernate
  * HTTP
  * Database
  * Messaging Framework
* Persistence concerns must remain outside the Domain.
* Framework-specific exceptions require an explicit architectural decision.

## Persistence

* Database representation must not force the Domain to replace a Value Object with primitives.
* One Value Object may map to multiple database columns.
* Example:

```text
Domain: Money
  ├── amount
  └── currency
        │
        ▼
Infrastructure Persistence Mapping
  ├── amount_column
  └── currency_column
```

* The Domain continues to use `Money`; Infrastructure handles the mapping.
* Persistence Mapping for Value Objects belongs in **Infrastructure**.

## Creation

* Every creation path must produce valid State.
* Simple Value Objects may use Constructor or `record`.
* Complex Value Objects should preferably expose Factory Methods such as:

  * `Currency.of(...)`
  * `Money.of(...)`
  * `AccountNumber.of(...)`
* Constructors/Factories must enforce Value Object Invariants.

## Aggregate Identity Model

```text
Aggregate Root
      │
      ▼
    Entity
      │
      ▼
   Identity
      │
      ▼
Identity Value Object
```

General Ledger:

```text
Account
└── AccountId        → Value Object

AccountHead
└── AccountHeadId    → Value Object

JournalEntry
└── JournalEntryId   → Value Object
```

`Account`, `AccountHead`, and `JournalEntry` are Entity/Aggregate Root concepts; their IDs are Value Objects and are never independent Entities.

## Constraints

* No independent Identity for Value Objects.
* Equality is value-based.
* Value Objects are Immutable.
* Invalid Value Object State must be impossible to create.
* Business Rules belonging to a Value should reside inside the Value Object where practical.
* Important Domain concepts should not be represented by raw primitives when Business Meaning/Rules exist.
* Do not create Value Objects solely as primitive wrappers.
* Value Objects are Framework Independent.
* No independent Repository or Lifecycle for Value Objects.
* Value Objects normally belong to an Aggregate.
* Persistence limitations must not alter the Domain model.
* Persistence Mapping belongs to Infrastructure.
* Value Objects must use Ubiquitous Language naming.
* Shared usage does not automatically imply Shared Kernel.
* Identity Value Objects are type-safe representations of Entity/Aggregate Identity.
* Identity Value Objects are not Entity/Aggregate.
* Aggregate boundaries are based on Business Invariants and Consistency Boundaries, not Identity Value Objects.
* Optionality must reflect actual Business semantics.
* Any concept requiring independent Identity, Lifecycle, Invariants, or Repository must be reconsidered as Entity/Aggregate.

## Agent Instructions

When modeling a General Ledger Domain concept:

1. Determine whether it has independent Identity and Lifecycle.
2. If yes, model it as an **Entity** candidate.
3. If no, determine whether it has Business Meaning or Business Rules.
4. If yes, model it as a **Value Object**.
5. Otherwise, a Primitive may be sufficient.
6. Make Value Objects Immutable and value-equal.
7. Enforce their Invariants during creation.
8. Keep their Business behavior inside the Value Object where appropriate.
9. Keep Value Objects Framework Independent.
10. Do not assign Repository/Lifecycle to Value Objects.
11. Model Entity/Aggregate IDs as dedicated Identity Value Objects.
12. Do not create Aggregate boundaries around Value Objects.
13. Do not move Value Objects into generic Shared Code merely because they are reused.
14. Preserve Value Objects in the Domain even when Persistence maps them to primitive columns.
15. Treat `AccountId`, `AccountHeadId`, and `JournalEntryId` as Value Objects, not Entities or Aggregates.
