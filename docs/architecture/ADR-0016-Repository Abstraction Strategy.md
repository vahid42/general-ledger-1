# ADR-0016 — Repository Abstraction Strategy — Ultra Compact

## Purpose

Define the Repository abstraction boundary in a DDD + Clean Architecture system.

**Core principle:** Repository is a Domain abstraction for Aggregate Root persistence. Its Contract belongs to `Domain`; its implementation and Persistence details belong to `Infrastructure`.

## Core Rules

* Repository Contract is defined in `Domain`.
* Repository Implementation is defined in `Infrastructure`.
* Repository is created **only for Aggregate Root**.
* Internal Entities and Value Objects do not have independent Repositories unless they later become independent Aggregate Roots.
* Repository uses the Aggregate's **Typed Identity** (`AccountId`, `OrderId`, etc.), not primitive IDs such as `UUID`.
* Repository must be **Domain-oriented**, not Database-oriented.
* Generic CRUD Repository is **forbidden by default**.
* Repository must not contain Business Logic, Business Decisions, Validation, Policy, HTTP, Authentication, Authorization, Event Publishing, or Transaction Orchestration.
* Repository must not replace Aggregate behavior.
* Aggregate behavior remains inside the Aggregate/Domain Service:
  `load → domain behavior → save`.
* Persistence Model must remain separate from Domain Model.
* Repository Implementation owns mapping/reconstruction between Persistence Model and Domain Aggregate.
* Repository must return a valid, fully reconstructed Aggregate state.
* Aggregate Boundary must never be bypassed through Persistence APIs.

## Architecture Rules

### Dependency Direction

```text
Domain
  │
  ▼
Repository Interface
  ▲
  │
Infrastructure
  │
  ▼
JPA / Hibernate / JDBC
  │
  ▼
Database
```

`Domain` must not depend on JPA, Hibernate, Spring Data, JDBC, SQL, or Database concerns.

### Repository Location

Repository Contract belongs to the Domain Module and should be located near the Aggregate it serves:

```text
account/
└── domain/
    ├── model/
    │   └── Account.java
    └── repository/
        └── AccountRepository.java
```

or:

```text
account/
└── domain/
    └── account/
        ├── Account.java
        ├── AccountId.java
        └── AccountRepository.java
```

Exact Package Structure is flexible; ownership is not.

### Infrastructure

```text
infrastructure/
└── persistence/
    ├── AccountRepositoryImpl.java
    ├── AccountJpaEntity.java
    └── SpringDataAccountRepository.java
```

`Domain` sees only `AccountRepository`; `Infrastructure` contains all Persistence technology details.

## Aggregate Rules

Repository operates on the **Aggregate Root as the unit of persistence**.

Valid:

```java
Order order = orderRepository.findById(orderId);
order.changeLineQuantity(lineId, quantity);
orderRepository.save(order);
```

Invalid:

```text
OrderRepository
OrderLineRepository
ShippingAddressRepository
```

Internal Aggregate state must not be independently persisted or mutated through separate Repository APIs.

If an internal concept becomes an independent Aggregate, its Repository may then be introduced.

## Identity Rules

Repository contracts must use Domain-specific Typed Identity:

```java
public record AccountId(UUID value) {}

public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    void save(Account account);
}
```

Avoid:

```java
Optional<Account> findById(UUID id);
```

Typed Identity allows the Type System to express Domain boundaries.

## Repository Contract Rules

Repository methods must express Domain/Use Case needs rather than Persistence implementation details.

Preferred:

```java
Optional<Account> findById(AccountId id);

Optional<Account> findActiveByCustomerId(CustomerId customerId);

void save(Account account);
```

Avoid contracts that merely expose Database structure, columns, flags, or ORM-derived naming.

Example to review carefully:

```java
findByStatusAndDeletedFalseAndVersionGreaterThan(...)
```

If a method primarily reflects Persistence Model details rather than Domain meaning, redesign the Contract.

## Generic Repository Rule

Generic CRUD Repository is **not a standard Domain abstraction**.

Forbidden by default:

```java
interface GenericRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    void delete(T entity);
    List<T> findAll();
}
```

Reasons:

* No explicit Business Meaning.
* Imposes CRUD as the primary abstraction.
* Ignores Aggregate Boundaries.
* Encourages Repositories for internal Entities.
* Creates overly generic Persistence-oriented APIs.
* Produces Contracts that are not necessarily valid for every Aggregate.

A genuinely Business-meaningful generic abstraction requires a separate ADR.

## Domain Isolation Rules

Domain Repository must **not**:

```java
JpaRepository
Page
Pageable
Specification
EntityManager
CriteriaQuery
Predicate
Query
EntityManagerFactory
```

It must not extend:

```java
JpaRepository<AccountEntity, UUID>
```

No Persistence framework dependency may leak into the Domain Repository Contract.

## Persistence Model Rules

Domain and Persistence Models are separate:

```text
Domain:
Account
AccountId
Money
AccountStatus

Infrastructure:
AccountJpaEntity
AccountIdEmbeddable
AccountBalanceEntity
...
```

Repository Implementation performs:

```text
Database
  ↓
JPA Entity
  ↓
Repository Implementation
  ↓
Valid Domain Aggregate
```

and:

```text
Domain Aggregate
  ↓
Repository Implementation
  ↓
JPA Entity
  ↓
Database
```

Domain Model must not be changed merely to simplify ORM mapping.

## Transaction Rules

Repository does **not** define the Transaction Boundary.

Transaction Boundary belongs to the Application Use Case:

```text
Application Use Case
        ↓
Transaction Boundary
        ├── Repository.load()
        ├── Domain Behavior
        └── Repository.save()
```

`@Transactional` and Transaction Management details must not be part of the Domain Repository Contract.

Repository operates within the Transaction established by the Application/Infrastructure mechanism.

## Query Separation Rules

Not every system Query should use a Domain Repository.

### Domain Query

If a Query is required for Domain behavior or Business Decision, it may belong to the Domain Repository:

```java
Optional<Account> findById(AccountId id);

Optional<Account> findActiveByCustomerId(CustomerId customerId);
```

### Read Query

For Reporting, Search, Dashboard, List, Pagination, Aggregation, or Projection, loading a Domain Aggregate is often unnecessary.

Use a separate Query Repository / Read Model:

```text
Application Query
      ↓
Query Repository
      ↓
Projection / Read Model
      ↓
Database
```

Example:

```java
interface AccountQueryRepository {
    AccountSummary findSummary(AccountId id);
    List<AccountSummary> search(AccountSearchCriteria criteria);
}
```

Query Repository does not have to be a Domain Repository.

## Query Repository Rules

* Read-only queries may have separate Contracts.
* Read-heavy queries should not unnecessarily reconstruct Aggregates.
* Query Repository may directly produce Projection/DTO models.
* Query Repository should be used for Reporting, Search, Dashboard, List, Pagination, and Aggregation where appropriate.
* Domain Aggregate must not be imposed on read-only use cases.

Preferred:

```text
Database
  ↓
Projection
  ↓
AccountSummary
```

rather than:

```text
Database
  ↓
Account Aggregate
  ↓
AccountSummary
```

## Pagination Rules

Pagination is primarily a Read/Query concern.

Framework-specific APIs such as:

```java
Page<T>
Pageable
Slice<T>
```

must not leak into Domain Repository Contracts.

Application/Query Layer may define its own abstraction:

```java
PageResult<AccountSummary> search(
    AccountSearchCriteria criteria,
    PageRequest pageRequest
);
```

The abstraction must not be coupled to Spring Data APIs.

## Concurrency Rules

Repository may support Business-required Concurrency Control such as:

* Optimistic Locking
* Pessimistic Locking

However, Persistence-specific locking details must remain outside Domain.

For example, Domain must not know about:

```java
@Version
```

or other ORM-specific mechanisms.

## Delete Rules

`delete()` must not exist merely because CRUD provides it.

Physical deletion is allowed only when deletion has genuine Business Meaning.

Prefer explicit Domain behavior where applicable:

```text
Close
Deactivate
Archive
Cancel
```

Only introduce:

```java
void delete(Account account);
```

when actual deletion is a Domain Requirement.

## External Dependency Rules

Not every external dependency is a Repository.

Repository represents Aggregate Persistence.

External systems should use abstractions expressing their actual role:

```text
PaymentGateway
PaymentProvider
ExchangeRateProvider
CreditScoreProvider
NotificationSender
```

Do not model external integrations as `Repository` merely because they are accessed through an abstraction.

## Domain Event Rules

Repository must not publish Domain Events or Integration Events.

Invalid:

```text
save()
  └──► Kafka.publish()  ❌
```

Aggregate may produce Domain Events:

```text
Aggregate
   ↓
Domain Event
```

Event publication follows ADR-0015.

For reliable Integration Event publication, Transactional Outbox may be used:

```text
Application Transaction
       ├── Aggregate State
       └── Outbox
             ↓
       Outbox Publisher
```

## Modular Monolith Rules

In a Modular Monolith:

* Each Module owns its Domain Model and Repository Contracts.
* A Module must not directly access another Module's internal Repository.
* Cross-Module interaction must use appropriate Application/Domain Contracts, Domain Events, or Integration mechanisms.

Example:

```text
account/
├── domain/
│   ├── model/
│   └── repository/
│       └── AccountRepository
└── infrastructure/
    └── persistence/
```

Other Modules must not bypass this boundary to access Account persistence directly.

## Database Independence Rules

Repository Contract is designed from Domain and Use Case requirements, **not Database Schema**.

Avoid:

```java
findByTableName(...)
findByColumn(...)
findByDeletedFlag(...)
findByDatabaseStatus(...)
```

Prefer:

```java
findById(AccountId id);

findActiveByCustomerId(CustomerId customerId);
```

Database Schema remains an Infrastructure implementation detail.

## Testing Rules

Repository Implementations require Integration Tests covering, where applicable:

* Mapping
* Persistence
* Queries
* Aggregate Reconstruction
* Transaction behavior
* Concurrency
* Constraint handling

`Testcontainers` is preferred where practical for real Persistence testing.

Domain tests must not require a real Database merely to verify Aggregate Business Rules.

## Decision Checklist

Before creating a Repository:

```text
Is this an Aggregate Root?
        │
       No ──► Do not create Repository
        │
       Yes
        ↓
Is independent Persistence required?
        │
       No ──► Do not create Repository
        │
       Yes
        ↓
Does the Contract have Business Meaning?
        │
       No ──► Do not create Generic CRUD Repository
        │
       Yes
        ↓
Is the Query only for Read/Report?
        │
       Yes ──► Query Repository / Read Model
        │
       No
        ↓
Domain Repository
```

## Module Structure

Recommended baseline:

```text
Module
├── domain/
│   ├── model/
│   │   ├── AggregateRoot
│   │   ├── Entity
│   │   └── ValueObject
│   └── repository/
│       └── AggregateRepository
│
├── application/
│   └── usecase/
│
└── infrastructure/
    └── persistence/
        ├── AggregateRepositoryImpl
        ├── JpaEntity
        └── SpringDataRepository
```

## Agent Instructions

When designing or reviewing a Repository:

1. Treat `Repository` as a **Domain abstraction**, not a Persistence framework abstraction.
2. Verify that the Repository belongs to an **Aggregate Root**.
3. Reject Repositories for internal Entities/Value Objects.
4. Prefer Typed Domain Identity over primitive IDs.
5. Reject Generic CRUD Repository unless a separate ADR explicitly justifies it.
6. Keep Repository Contracts Domain-oriented.
7. Reject JPA/Hibernate/Spring Data types in Domain Repository APIs.
8. Keep Persistence Model separate from Domain Model.
9. Ensure Repository reconstructs a valid Aggregate.
10. Never move Business Logic or Business Decisions into Repository.
11. Never use Repository to bypass Aggregate Boundaries.
12. Keep Transaction Boundary in Application/Use Case.
13. Keep Domain Event publication outside Repository.
14. Separate Domain Queries from read-only/reporting Queries when appropriate.
15. Keep `Page`, `Pageable`, `Slice`, and similar framework APIs out of Domain.
16. Treat physical deletion as a Business Decision, not automatic CRUD behavior.
17. Model External Systems according to their actual role, not as Repositories.
18. Ensure Module boundaries prevent direct access to another Module's Repository.
19. Test Repository implementations with Integration Tests; test Domain behavior independently of Database.
20. Design Repository from Domain/Use Case needs, never from Database Schema.

## Final Decision

```text
                    Application
                         │
                         ▼
                Use Case / Transaction
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
      Domain Repository       Query Repository
              │                     │
              ▼                     ▼
       Aggregate Root         Read Model / DTO
              │                     │
              └──────────┬──────────┘
                         ▼
                  Infrastructure
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
         JPA/Hibernate         Query Engine
              │                     │
              └──────────┬──────────┘
                         ▼
                      Database
```

**Repository is for Aggregate Root persistence; its Contract belongs to `Domain`, its Implementation belongs to `Infrastructure`, and it must never become a Generic CRUD abstraction or a location for Business Logic.**
