# ADR-0019 — Domain Policy Placement Strategy 

## Purpose

Define where each Business Policy must be evaluated and enforced based on:

* Ownership of the Business Rule.
* Information required to make the decision.
* Aggregate boundaries.
* Separation of Domain, Application, Repository, and Infrastructure responsibilities.

## Core Rules

1. **Aggregate owns its internal invariants.**
2. If a rule requires only one Aggregate's state and behavior, enforce it inside that Aggregate.
3. **Aggregate must never depend on or call Repository, Persistence, or Infrastructure.**
4. Cross-Aggregate information must be obtained outside the Aggregate through Repository Ports.
5. **Repository provides Data Access Capability, not Business Decisions or Business Logic.**
6. **Application Service is the default location for simple Use Case orchestration.**
7. A Cross-Aggregate operation does **not** automatically require a Domain Service.
8. **Domain Service is used only for a real, non-trivial Business Rule that has no single Aggregate owner.**
9. Value Objects enforce validation and semantics of their own values.
10. Aggregate-to-Aggregate references use **Typed Identity**, not direct Object references.
11. Repository queries such as `existsByCode()` are evaluations/inputs to a decision, not guarantees of final Business Integrity under concurrency.
12. Final integrity guarantees for concurrency-sensitive rules belong to Persistence/Transaction/Concurrency mechanisms.

## Policy Placement Decision

```text
Business Rule
      │
      ▼
Does one Aggregate's state suffice?
      │
   ┌──┴──┐
  Yes    No
   │      │
   ▼      ▼
Aggregate  External Information
             │
             ▼
       Repository Port
             │
             ▼
   Simple orchestration only?
          │       │
         Yes      No
          │        │
          ▼        ▼
Application    Domain Service
Service
```

### Placement Matrix

| Rule Type                                 | Primary Location     | Reason                               |
| ----------------------------------------- | -------------------- | ------------------------------------ |
| Internal Aggregate invariant              | Aggregate            | Internal state is sufficient         |
| Value validation                          | Value Object         | Owns value semantics                 |
| Aggregate state transition                | Aggregate            | Domain behavior belongs to Aggregate |
| External lookup                           | Repository           | Data Access                          |
| Simple Cross-Aggregate coordination       | Application Service  | Use Case orchestration               |
| Non-trivial Cross-Aggregate Business Rule | Domain Service       | No single Aggregate owns the rule    |
| Persistence integrity                     | Persistence/Database | Final technical guarantee            |

## Aggregate Rules

For `AccountHeading`, the Aggregate owns:

* Valid `Level`.
* Maximum `Level` constraint.
* Leaf behavior and Child creation rules.
* Nature inheritance/compatibility.
* `allowNegativeBalance` inheritance.
* Account creation only at `Level 5`.
* Parent/Child domain behavior when required state is available.
* Internal state transitions and invariants.

Example:

```java
public boolean canCreateAccount() {
    return level == 5;
}

public AccountHeading createChild(...) {
    if (isLeaf()) {
        throw new InvalidAccountHeadingOperationException(...);
    }
    // ...
}
```

Aggregate must **not** contain Repository dependencies:

```java
// Forbidden
class AccountHeading {
    private AccountHeadingRepository repository;
}
```

## Value Object Rules

Value Objects own:

* Value validation.
* Value-based equality.
* Domain meaning.

Examples:

```text
AccountHeadingCode
Money
Currency
```

Structural validity of `AccountHeadingCode` belongs to the Value Object/Aggregate boundary.

## Repository Rules

Repository is a **Data Access Abstraction**.

Allowed capabilities include:

```java
Optional<AccountHeading> findById(AccountHeadingId id);

boolean existsByCode(AccountHeadingCode code);

boolean existsByParentId(AccountHeadingId parentId);

boolean existsAccountByHeadingId(AccountHeadingId headingId);

void save(AccountHeading accountHeading);

void delete(AccountHeading accountHeading);
```

Repository must **not** implement Business Logic:

```java
// Forbidden
void validateAccountHeading(...);

boolean createIfCodeDoesNotExist(...);
```

### Repository Query ≠ Business Guarantee

A query such as:

```java
existsByCode(code)
```

can support Policy Evaluation but cannot guarantee uniqueness under concurrency:

```text
Request A ── existsByCode() ── false ── create
Request B ── existsByCode() ── false ── create
```

Final integrity may require:

```text
Database Constraint
+
Transaction
+
Concurrency Control
```

## Application Service Rules

Application Service owns **Use Case Orchestration**:

```text
Validate Input
    ↓
Load required Aggregates
    ↓
Query external conditions
    ↓
Invoke Aggregate behavior
    ↓
Save Aggregate
    ↓
Coordinate transaction boundary
```

Typical responsibilities:

* Repository coordination.
* Aggregate coordination.
* External condition checks.
* Use Case orchestration.
* Transaction boundary coordination.

Application Service must not extract Aggregate-owned Business Rules:

```java
// Wrong if Level 5 is an AccountHeading invariant
if (heading.getLevel() == 5) {
    ...
}
```

Prefer:

```java
heading.canCreateAccount();
```

or:

```java
heading.createAccount(...);
```

## Domain Service Rules

Create a Domain Service only when all of the following apply:

```text
Real Business Rule
+
No Single Aggregate Ownership
+
Cross-Aggregate / External Information
+
Non-trivial Domain Logic
```

Example:

```java
public class AccountHeadingDomainService {

    private final AccountHeadingRepository repository;

    public AccountHeadingDomainService(
            AccountHeadingRepository repository) {
        this.repository = repository;
    }

    public void validateCodeUniqueness(
            AccountHeadingCode code) {
        if (repository.existsByCode(code)) {
            throw new DuplicateAccountHeadingCodeException(code);
        }
    }
}
```

This is justified only if code uniqueness has become an independent Domain Policy. A simple repository check inside one Use Case does **not** require a Domain Service.

## Policy Evaluation vs Policy Enforcement

### Policy Evaluation

Obtaining information required for a decision:

```text
Does Code exist?
Does Parent exist?
Does Child exist?
Does Account exist?
```

May require Repository access.

### Policy Enforcement

Preventing an invalid Business state or operation:

```text
Do not create invalid Aggregate state.
Do not violate an Aggregate invariant.
Do not allow a forbidden operation.
```

Must occur at the actual owner of the rule.

```text
Repository
    │
    └── existsByCode()

Application / Domain Policy
    │
    └── Business decision

Aggregate
    │
    └── Internal invariant enforcement
```

## Cross-Aggregate Rules

### Parent Existence

Separate **Parent behavior** from **Parent lookup**.

If Parent state is already available:

```text
Parent Aggregate
      ↓
parent.createChild(...)
```

Parent/Child behavior belongs to the Domain.

If only `parentId` is available:

```text
Application Service
      ↓
Repository
      ↓
Parent Aggregate
      ↓
parent.createChild(...)
```

Repository performs the lookup; Domain performs the behavior.

### Child Existence

If Child is an independent Aggregate:

```java
boolean existsByParentId(AccountHeadingId parentId);
```

Application Service may use this information before deletion.

### Account Existence

```java
boolean existsAccountByHeadingId(AccountHeadingId headingId);
```

Application Service may use this information before deleting an `AccountHeading`.

If these checks evolve into complex Cross-Aggregate Business Policies, a Domain Service may own the policy.

## AccountHeading Policy Map

| Policy                               | Location                                          |
| ------------------------------------ | ------------------------------------------------- |
| Maximum Level = 5                    | Aggregate                                         |
| Valid Level                          | Aggregate                                         |
| Leaf cannot create Child             | Aggregate                                         |
| Nature compatibility/inheritance     | Aggregate                                         |
| `allowNegativeBalance` inheritance   | Aggregate                                         |
| Root has no Parent                   | Aggregate / Factory                               |
| Account only at Level 5              | Aggregate                                         |
| Code structural validation           | Value Object / Aggregate                          |
| Code uniqueness                      | Application/Domain Policy + Persistence Guarantee |
| Check Code existence                 | Repository                                        |
| Find Parent by ID                    | Repository                                        |
| Check Child existence                | Repository                                        |
| Check Account existence              | Repository                                        |
| Prevent deletion when Child exists   | Application / Domain Policy                       |
| Prevent deletion when Account exists | Application / Domain Policy                       |

**Repository participation does not mean Business Logic belongs in Repository.**

## Aggregate Boundary

Independent Aggregates must not hold direct Object references to one another.

```java
// Wrong
class Account {
    private Customer customer;
}
```

Use Typed Identity:

```java
// Correct
class Account {
    private CustomerId customerId;
}
```

```text
Account
  └── CustomerId
```

not:

```text
Account
  └── Customer Object
```

## Repository Dependency Direction

Repository Ports are defined at the appropriate Domain/Application boundary and implemented by Infrastructure.

```text
Domain / Application
        │
        ▼
Repository Port
        ▲
        │ implements
Infrastructure
        │
        ▼
Database
```

**Aggregate does not consume the Repository Port.**

## Mandatory Architectural Rules

1. Business Rules owned by an Aggregate must be enforced inside that Aggregate.
2. Aggregate must not call Repository.
3. Aggregate must not know Persistence or Infrastructure.
4. Independent Aggregates must not hold direct Object references to each other.
5. Cross-Aggregate references use Typed Identity.
6. Repository is responsible for Data Access, not Business Logic.
7. Application Service is responsible for Use Case Orchestration.
8. Cross-Aggregate operations do not automatically require Domain Service.
9. Domain Service exists only for real, non-trivial Business Rules without a single Aggregate owner.
10. Repository queries do not guarantee Business Integrity under concurrency.
11. Final integrity mechanisms must be defined by Persistence/Concurrency decisions.
12. Value Objects enforce validation of their own values.
13. Repository dependency alone is never a reason to move Business Logic out of an Aggregate.

## AccountHeading Responsibility Boundary

```text
AccountHeading Aggregate
├── Level Rules
├── Leaf Rules
├── Parent/Child Domain Behavior
├── Nature Rules
├── allowNegativeBalance Rules
└── Account-at-Level-5 Rule

Repository
├── Find Parent
├── Check Code Existence
├── Check Child Existence
└── Check Account Existence

Application Service
├── Create Use Case
├── Delete Use Case
├── Load required Aggregates
├── Coordinate Repositories
└── Invoke Aggregate Behavior

Domain Service
└── Complex Cross-Aggregate Domain Policy
```

## Constraints

* Aggregate Boundary must remain intact.
* Domain must remain independent of Persistence.
* Repository must remain a Data Access abstraction.
* Simple orchestration must not be promoted to Domain Service.
* Aggregate invariants must not be duplicated in Application Service.
* Cross-Aggregate checks must not be mistaken for Aggregate-local state.
* `existsByCode()` and similar checks are not final concurrency guarantees.
* Persistence/Database constraints may be required for final Integrity.
* Transaction, Concurrency, Optimistic Locking, Pessimistic Locking, Distributed Transaction, Eventual Consistency, Database Constraint Strategy, ORM Mapping, and Repository Implementation are outside this ADR.

## Decision Rules

```text
IF rule needs only one Aggregate's state
→ enforce in Aggregate.

IF rule needs Value-specific validation
→ enforce in Value Object.

IF external data is only being loaded/queried
→ Repository.

IF multiple Aggregates/Repositories require simple coordination
→ Application Service.

IF a real Cross-Aggregate Business Rule exists
AND no single Aggregate owns it
AND logic is non-trivial
→ Domain Service.

IF rule is concurrency-sensitive
→ Repository check is insufficient;
   require an appropriate Persistence/Concurrency guarantee.
```

## Agent Instructions

When placing a Business Policy:

1. Identify the **true owner** of the rule.
2. Determine the minimum information required for the decision.
3. Keep Aggregate-local invariants inside the Aggregate.
4. Keep Value validation inside Value Objects.
5. Use Repository only to obtain external data.
6. Use Application Service for simple Use Case orchestration.
7. Introduce Domain Service only for non-trivial Cross-Aggregate Business Rules without a single Aggregate owner.
8. Never inject Repository/Persistence/Infrastructure into an Aggregate.
9. Never treat Repository queries as final Integrity guarantees under concurrency.
10. Preserve Aggregate boundaries and use Typed Identity between independent Aggregates.
11. Separate **Policy Evaluation** from **Policy Enforcement**.
12. Do not move Business Logic out of an Aggregate merely because external data is also required.
