# ADR-0014 — Domain Service Strategy 

## Purpose

Define when and how to introduce a `Domain Service`, while preventing misuse as a generic location for Business Logic, CRUD, Repository queries, Application orchestration, or transaction management.

## Core Rules

* Create a `Domain Service` only when a Business Concept/Operation has no natural owner in a `Value Object`, `Entity`, or `Aggregate`.
* `Domain Service` is **not the default location** for Business Logic.
* Decision order:

  1. `Value Object`
  2. `Entity` / `Aggregate`
  3. `Domain Service`
  4. If none applies → redesign the Domain Model.
* A `Domain Service` must represent a specific, meaningful Business Operation or Business Capability.
* `Domain Service` should be Stateless; Domain state belongs in `Entity` / `Aggregate`.
* Multiple Aggregates alone do not justify a `Domain Service`.
* A `Domain Service` must not become a God Service or generic Business Logic container.
* Naming must use Ubiquitous Language and express a concrete Business capability.
* Avoid generic names such as `CommonService`, `UtilityService`, `HelperService`, `Manager`, `Processor`, `Handler`, or `GenericService`.

## Domain Ownership Rules

### Value Object

Business meaning and intrinsic/structural validation belong in the `Value Object`.

Example:

```java
public record AccountHeadingCode(String value) {
    public AccountHeadingCode {
        if (value == null || value.isBlank()) {
            throw new InvalidAccountHeadingCodeException();
        }
    }
}
```

Repository-dependent checks such as uniqueness remain outside the `Value Object`.

### Entity / Aggregate

An `Entity` / `Aggregate` must own its natural Business Behavior and Invariants.

Do **not** extract an Aggregate's own rules into a `Domain Service` merely because a Service exists.

If a Business Rule repeatedly requires strongly consistent changes across multiple Aggregates, first question whether the `Aggregate Boundary` is incorrectly designed.

`Domain Service` must not be used to bypass an `Aggregate Boundary`.

### Domain Service

Use when the operation is an independent Domain concept or genuinely spans multiple Aggregates.

Example:

```java
public class FundsTransferService {
    public void transfer(
            Account source,
            Account destination,
            Money amount) {
        source.withdraw(amount);
        destination.deposit(amount);
    }
}
```

Before introducing such a Service, verify that the involved concepts truly need separate Aggregates.

## Repository Rules

* Repository dependency alone does **not** justify a `Domain Service`.
* Queries such as `existsBy...`, `findById(...)`, `findByNumber(...)`, or `findBy...` do not inherently constitute a `Domain Service`.
* CRUD operations belong to Repository/Application responsibilities, not Domain Services.
* A class that only wraps Repository queries must not automatically become `XxxDomainService`.
* If Repository access is only part of Use Case orchestration, it may remain in the `Application Layer`.

### Uniqueness

Uniqueness may be a Business Rule but its final guarantee is usually Persistence-dependent.

Use:

```text
Domain
 └── Value Object / Domain Rule

Application
 └── optional pre-check for user feedback

Database
 └── UNIQUE constraint for actual guarantee
```

Application pre-checks do not guarantee uniqueness because of race conditions.

`Domain Service` must never replace a Database uniqueness constraint.

## Domain Service vs Application Service

### Domain Service

Responsible for:

* Business Logic
* Business Decisions
* Independent Business Operations
* Cross-Aggregate Domain Operations

### Application Service

Responsible for:

* Use Case execution
* Orchestration
* Loading Aggregates
* Calling Domain behavior
* Persistence coordination
* Transaction boundary

Typical flow:

```text
Application Service
 ├── Load Aggregate A
 ├── Load Aggregate B
 ├── Call Domain behavior / Domain Service
 └── Save Aggregates
```

`Application Service` must not become the owner of core Business Rules.

`Domain Service` must not become an `Application Service`.

## Transaction Rules

* Transaction management belongs to `Application Layer` / `Infrastructure`.
* `Domain Service` must not define transaction boundaries.
* Avoid framework transaction annotations such as `@Transactional` in `Domain Service`.
* Transaction boundaries are defined around the Use Case, not by the Domain Service.

## External Dependency Rules

A `Domain Service` may depend on a Domain abstraction when an external capability is required for a Business Decision.

```text
Domain Service
      │
      ▼
Domain Abstraction / Port
      ▲
      │
Infrastructure
      │
      ▼
External API
```

Example:

```java
public interface CreditScoreProvider {
    CreditScore getScore(CustomerId customerId);
}
```

The Domain knows the abstraction; API/Infrastructure implementations remain outside Domain.

## Framework Independence

`Domain Service` must be Framework Independent.

Do not use:

```java
@Service
@Component
@Autowired
@Transactional
```

unless an explicit ADR approves the exception.

A `Domain Service` must be Unit Testable without:

* Spring Context
* Database
* HTTP
* Message Broker
* Real External Services

External dependencies should be represented by Domain abstractions and replaced with Mock/Fake in Unit Tests.

## Domain Service vs Policy

`Policy` and `Domain Service` are distinct:

```text
Domain Service
 └── Business Operation / Capability

Policy
 └── Replaceable or variable Business Rule / Decision
```

Example:

```text
FundsTransferService
 └── performs transfer

TransferFeePolicy
 └── determines transfer fee
```

A `Policy` may be used by a `Domain Service` when the decision is variable or replaceable.

## General Ledger Rules

General Ledger contains these primary Aggregates:

```text
General Ledger
 ├── AccountHead
 ├── Account
 └── JournalEntry
```

Their existence does **not** imply corresponding Domain Services.

Do not automatically create:

```text
AccountHeadDomainService
AccountDomainService
JournalEntryDomainService
```

Each `Domain Service` requires a real Business need.

### JournalEntry

`JournalEntry` is a Core Aggregate and should retain as much of its Business Behavior and Invariants as naturally belongs to the Aggregate.

`Domain Service` must not extract the core `JournalEntry` Business Logic merely for convenience.

### AccountHead

For `AccountHeadingCode`:

```text
CreateAccountHeading
 ├── Validate AccountHeadingCode
 ├── Optional repository uniqueness pre-check
 ├── AccountHeading.create(...)
 └── repository.save(...)
```

`repository.existsByCode(code)` alone does not justify `AccountHeadingDomainService`.

Database must enforce actual uniqueness.

## Anti-Patterns

Avoid:

```text
Anemic Entity/Aggregate
        │
        ▼
Domain Service
        └── Everything
```

Preferred:

```text
Entity / Aggregate
 └── Own Business Behavior

Value Object
 └── Own Value Rules

Domain Service
 └── Independent Domain Operation
```

Do not create a Domain Service merely because:

* a Repository is required;
* Persistence is required;
* a query exists;
* several Aggregates exist;
* the class would otherwise be placed in Application;
* an Aggregate has Business Logic.

## Decision Rules

Before creating a `Domain Service`:

```text
Business Rule
    │
    ├── belongs to Value Object? ──► Value Object
    │
    ├── belongs to Entity/Aggregate? ──► Entity/Aggregate
    │
    ├── independent Business Concept? ──► Domain Service
    │
    └── otherwise ──► Redesign Domain Model
```

For Repository-dependent logic:

```text
Needs Repository?
    │
    └── Is it an independent Business Operation?
            │
            ├── No ──► Application Layer
            └── Yes ─► Domain Service
```

## Agent Instructions

When evaluating or proposing a `Domain Service`:

1. Identify the Business Rule first.
2. Determine whether its natural owner is a `Value Object`.
3. If not, determine whether it belongs to an `Entity` / `Aggregate`.
4. Preserve Aggregate ownership of its own Invariants.
5. Check whether the operation is genuinely an independent Business Concept.
6. For cross-Aggregate logic, verify that the `Aggregate Boundary` is correct before introducing a Service.
7. Do not treat Repository dependency as sufficient justification.
8. Do not move CRUD, Repository queries, Application orchestration, or transaction management into Domain Service.
9. Keep Domain Service Stateless and Framework Independent.
10. Keep external implementations outside Domain; depend only on Domain abstractions.
11. Require every Domain Service to have explicit Business Meaning.
12. In General Ledger, do not create one Domain Service per Aggregate by convention.
13. Keep `JournalEntry` core Business Behavior inside the `JournalEntry Aggregate` whenever naturally applicable.
14. Test Domain Services as isolated Business Logic without infrastructure dependencies.
