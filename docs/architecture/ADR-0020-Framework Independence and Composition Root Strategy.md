# ADR-0020 — Framework Independence & Composition Root Strategy

## Status

**Accepted** — 2026-08-22
**Type:** Architectural
**Scope:** All Bounded Contexts

## Purpose

Keep `Domain` and `Application` Framework-independent while using Spring only at the system boundary for Composition, Dependency Injection, Configuration, and Lifecycle management.

**Core principle:** Business Logic must not depend on Spring for execution, construction, dependency lookup, or lifecycle.

---

## Architecture Model

```text
                    Spring Framework
                           │
                           ▼
                   ledger-bootstrap
                   Composition Root
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   Infrastructure     Application     Presentation
          │                │
          │                ▼
          └────────────> Domain
```

### Framework Boundary

* `Domain` → Plain Java; no Framework dependency.
* `Application` → Plain Java; no Framework dependency.
* `Infrastructure` → May be Framework-aware.
* `Presentation` → Framework-aware.
* `Bootstrap` → Framework-aware; acts as `Composition Root`.

---

## Core Rules

### 1. Framework Independence

`Domain` and `Application` MUST NOT directly depend on Spring.

Forbidden in `Domain` and `Application`:

```text
@Service
@Component
@Repository
@Autowired
@Configuration
@Bean
ApplicationContext
BeanFactory
```

Business Objects must remain constructible as ordinary Java objects.

### 2. Spring Responsibility

Spring is responsible for:

```text
Object Creation
Dependency Injection
Object Lifecycle
Configuration
Application Startup
```

Spring is NOT responsible for:

```text
Business Rules
Domain Invariants
Use Case Logic
Domain Decisions
```

### 3. Composition Root

`ledger-bootstrap` is the system `Composition Root`.

It is responsible for:

```text
Create Objects
Connect Dependencies
Select Implementations
Configure Infrastructure
Expose Application to Framework
```

It MUST NOT contain Business Logic.

Conceptually:

```text
Composition Root
    ↓
Create + Configure + Connect
    ↓
Application / Domain / Infrastructure Objects
```

---

## Domain Rules

The following MUST remain Framework-independent:

```text
Aggregate
Entity
Value Object
Domain Service
Domain Repository Interface
Domain Policy
Domain Event
```

Domain knows only its own Business Logic and abstractions.

Example:

```java
public class AccountHeadingDomainService {

    private final AccountHeadingRepository repository;

    public AccountHeadingDomainService(
            AccountHeadingRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }
}
```

No Spring annotation, `ApplicationContext`, or `BeanFactory` is allowed.

### Domain Service Creation Rule

A Repository dependency alone does NOT justify creating a `Domain Service`.

A `Domain Service` exists only when a real Business Rule:

1. does not belong to a single Aggregate, and
2. is not merely Application orchestration.

---

## Application Rules

`Application` MUST remain Plain Java.

`Application Service` is responsible for:

```text
Use Case
Orchestration
Transaction Intent
Aggregate Interaction
Calling Ports
```

It is NOT responsible for:

```text
Object Lifecycle
Dependency Lookup
Framework Configuration
```

Example:

```java
public class CreateAccountHeadingService {

    private final AccountHeadingRepository repository;
    private final AccountHeadingDomainService domainService;

    public CreateAccountHeadingService(
            AccountHeadingRepository repository,
            AccountHeadingDomainService domainService) {
        this.repository = Objects.requireNonNull(repository);
        this.domainService = Objects.requireNonNull(domainService);
    }
}
```

No `@Service`, `@Component`, `@Autowired`, or `ApplicationContext`.

---

## Infrastructure Rules

`Infrastructure` is the integration boundary and MAY be Framework-aware.

Allowed technologies include:

```text
Spring Data
JPA
Hibernate
Kafka
Redis
HTTP Clients
Database Drivers
```

Dependency rule:

```text
Domain         ❌ Spring Data / JPA / Hibernate / Kafka / Redis
Application   ❌ Spring Data / JPA / Hibernate / Kafka / Redis
Infrastructure ✅ Framework integration
```

Exceptions require an explicit ADR.

Framework dependencies MUST NOT leak into `Domain` or `Application`.

---

## Repository Rules

Repository interfaces are defined as Domain/Application abstractions:

```java
public interface AccountHeadingRepository {
    Optional<AccountHeading> findById(AccountHeadingId id);
    boolean existsByCode(AccountHeadingCode code);
    void save(AccountHeading accountHeading);
}
```

Implementations belong to `Infrastructure`:

```java
public class JpaAccountHeadingRepository
        implements AccountHeadingRepository {
}
```

Dependency flow:

```text
Domain/Application Repository Port
             ▲
             │ implements
             │
JpaAccountHeadingRepository
             │
             ▼
Composition Root
             │
             ▼
Spring
```

Domain MUST NOT know about:

```text
JPA
Hibernate
Spring Data
Database
```

---

## Wiring Rules

### Application Service

Application Services are constructed and registered by the `Composition Root`:

```java
@Bean
public CreateAccountHeadingService createAccountHeadingService(
        AccountHeadingRepository repository,
        AccountHeadingDomainService domainService) {

    return new CreateAccountHeadingService(
            repository,
            domainService);
}
```

Spring manages the Object Graph; Business Logic remains in `Application` and `Domain`.

### Domain Service

Domain Services are also constructed by the `Composition Root`:

```java
@Bean
public AccountHeadingDomainService accountHeadingDomainService(
        AccountHeadingRepository repository) {

    return new AccountHeadingDomainService(repository);
}
```

### Dependency Injection

Mandatory dependencies MUST use Constructor Injection.

Benefits:

```text
Explicit Dependencies
Immutable Dependencies
Fail-Fast Construction
Easy Unit Testing
```

Business Objects MUST NOT retrieve dependencies from a container.

Forbidden:

```java
ApplicationContext.getBean(...);
```

Dependency Injection means:

```text
Container → provides Dependency → Object
```

not:

```text
Object → asks Container → for Dependency
```

---

## Spring Annotation Policy

| Layer                       | Spring Annotations                         |
| --------------------------- | ------------------------------------------ |
| Domain Aggregate            | ❌ Forbidden                                |
| Domain Entity               | ❌ Forbidden                                |
| Value Object                | ❌ Forbidden                                |
| Domain Service              | ❌ Forbidden                                |
| Domain Repository Interface | ❌ Forbidden                                |
| Domain Event                | ❌ Forbidden                                |
| Application Service         | ❌ Forbidden                                |
| Application Policy          | ❌ Forbidden                                |
| Application DTO             | ❌ Framework-specific annotations forbidden |
| Infrastructure              | ✅ Allowed when required                    |
| Presentation Controller     | ✅ Allowed                                  |
| Bootstrap Configuration     | ✅ Allowed                                  |
| Bootstrap Bean Definition   | ✅ Allowed                                  |

Therefore:

```java
@Service
public class CreateAccountHeadingService {}
```

is forbidden.

Instead:

```java
public class CreateAccountHeadingService {}
```

with Bootstrap wiring:

```java
@Configuration
public class AccountHeadingConfiguration {

    @Bean
    public CreateAccountHeadingService createAccountHeadingService(...) {
        return new CreateAccountHeadingService(...);
    }
}
```

---

## Presentation Rules

`Presentation` is a Framework-aware adapter.

Spring MVC annotations are allowed:

```text
@RestController
@Controller
@RequestMapping
```

Example:

```java
@RestController
@RequestMapping("/account-headings")
public class AccountHeadingController {

    private final CreateAccountHeadingService createService;

    public AccountHeadingController(
            CreateAccountHeadingService createService) {
        this.createService = createService;
    }
}
```

Presentation may know `Application`, but Domain/Application Business Objects MUST NOT know Spring.

---

## Dependency Direction

The fundamental Dependency Rule is:

```text
                Domain
               ▲      ▲
               │      │
         Application  Infrastructure
               ▲
               │
         Presentation
               ▲
               │
            Bootstrap
```

Rules:

```text
Domain
  ❌ Infrastructure
  ❌ Presentation
  ❌ Spring

Application
  ❌ Infrastructure implementation
  ❌ Spring

Infrastructure
  ✅ Domain/Application abstractions

Presentation
  ✅ Application

Bootstrap
  ✅ Required outer layers
```

`Bootstrap` has a **Composition Dependency**, not a Business Dependency.

---

## Unit Testing

`Domain` and `Application` Business Logic SHOULD be testable without Spring.

Example:

```java
@Test
void shouldCreateAccountHeading() {

    AccountHeadingRepository repository =
            new InMemoryAccountHeadingRepository();

    AccountHeadingDomainService domainService =
            new AccountHeadingDomainService(repository);

    CreateAccountHeadingService service =
            new CreateAccountHeadingService(
                    repository,
                    domainService);
}
```

By default, Domain/Application tests MUST NOT require:

```text
@SpringBootTest
ApplicationContext
Spring Context
```

Framework-aware Integration Tests MAY use Spring Context.

---

## Composition Root Constraints

Configuration MUST NOT contain Business Rules.

Forbidden:

```java
@Configuration
public class AccountHeadingConfiguration {

    @Bean
    public CreateAccountHeadingService service(...) {

        if (...) {
            // Business Rule — forbidden
        }

        ...
    }
}
```

Composition Root performs only:

```text
Create
Configure
Connect
```

Business responsibilities remain:

```text
Application → Use Case
Domain      → Business Rule
```

---

## Bean Creation Rule

Any object requiring Spring-managed lifecycle MAY be registered as a Spring Bean in the `Composition Root`.

This MUST NOT introduce Spring dependency into Business Code.

```text
Plain Java Object
       ↓
Composition Root
       ↓
@Bean
       ↓
Spring ApplicationContext
```

---

## Architecture Test Rules

These rules SHOULD be enforced through `ArchUnit` or equivalent architecture tests:

```text
Domain must not depend on Spring
Application must not depend on Spring

Domain must not depend on Infrastructure
Application must not depend on Infrastructure

Infrastructure may depend on Domain/Application abstractions

Presentation may depend on Application

Bootstrap may depend on required outer layers
```

---

## AccountHeading Object Graph

```text
GeneralLedgerApplication
        │
        ▼
AccountHeadingConfiguration
        │
   ┌────┼─────────────────────┐
   ▼    ▼                     ▼
Repository  Domain Service  Application Service
   │          │                  │
   ▼          │                  ├── Create
JpaRepository │                  ├── Update
              │                  ├── Delete
              │                  └── Search
              ▼
   AccountHeadingDomainService
```

Runtime request flow:

```text
AccountHeadingController
        ↓
CreateAccountHeadingService
        ↓
AccountHeadingDomainService
        ↓
AccountHeadingRepository
        ↓
JpaAccountHeadingRepository
```

Spring only composes and manages this Object Graph.

---

## Rejected Alternatives

### Framework-aware Application

```java
@Service
class CreateAccountHeadingService {}
```

**Rejected:** introduces Spring coupling into `Application`.

### Framework-aware Domain Service

```java
@Component
class AccountHeadingDomainService {}
```

**Rejected:** Domain Model becomes lifecycle-dependent on Spring.

### Spring Annotation on Domain Repository Interface

```java
@Repository
interface AccountHeadingRepository {}
```

**Rejected:** Repository Interface is a Domain/Application abstraction; persistence implementation belongs to `Infrastructure`.

### Full Manual DI in `main`

```java
public static void main(String[] args) {}
```

**Rejected:** increases `main` responsibility, complicates Configuration, Environment handling, and Lifecycle management, while underusing Spring Container.

### Service Locator

```java
ApplicationContext.getBean(...);
```

**Rejected:** Business Objects must receive dependencies explicitly rather than locating them through the container.

---

## Layer Classification Rule

For every new class, first determine its Layer.

### Domain

```text
Plain Java
No Spring
```

### Application

```text
Plain Java
No Spring
```

### Infrastructure

```text
Framework integration when required
```

### Presentation

```text
Framework-aware
```

### Bootstrap

```text
Spring-aware
Composition Root
```

---

## Scope

Applies to all Bounded Contexts and all new:

```text
Domain Entity
Aggregate
Value Object
Domain Service
Domain Repository Interface
Domain Event
Application Service
Application Policy
Application DTO
```

Exceptions:

```text
Presentation → Framework-aware
Bootstrap    → Framework-aware
Infrastructure → May be Framework-aware
```

Infrastructure Framework dependencies MUST NOT leak into Domain/Application.

---

## Benefits

```text
Framework Independence
Testability
Reduced Coupling
Explicit Dependencies
Clear Composition Root
Separation of Concerns
Framework Replaceability
Clean Domain Model
```

---

## Trade-offs

Accepted costs:

```text
More Configuration
More Bean Definitions
More Bootstrap Boilerplate
Object Graph Management
Greater Composition Root Discipline
```

These costs are accepted in exchange for Domain/Application independence.

---

## ADR Relationship

The conceptual relationship with previous decisions is:

```text
ADR-0019
Business Policy Placement
        ↓
Where does Business Logic live?
        ↓
Aggregate / Application Service / Domain Service
        ↓
ADR-0020
Framework Boundary & Wiring
        ↓
How are those Objects constructed and connected?
        ↓
Composition Root
        ↓
Spring
```

**ADR-0019 defines Business Logic placement.
ADR-0020 defines how that Business Logic is connected to Runtime without Framework coupling.**

---

## Final Decision

General Ledger adopts the following invariant:

```text
Framework
    ↓
ledger-bootstrap
    ↓
Composition Root
    ↓
┌──────────────┬──────────────┬──────────────┐
│              │              │
▼              ▼              ▼
Infrastructure Application Presentation
│              │
│              ▼
└──────────> Domain
```

### Non-Negotiable Principles

1. **Framework MUST remain at the system boundary; Business Logic MUST NOT depend on Framework for execution.**
2. **Application Service owns Use Case orchestration; Domain Service owns applicable Business Logic; Spring owns Composition and Lifecycle.**
3. **Composition Root is the place where interfaces are connected to implementations and the Object Graph is created.**
4. **Dependencies MUST be injected into Objects; Business Objects MUST NOT retrieve dependencies from the container.**
5. **Bootstrap MUST contain Composition logic only, never Business Logic.**
6. **Infrastructure MAY be Framework-aware, but Framework dependencies MUST NOT leak into Domain or Application.**
7. **Domain and Application MUST remain Plain Java and independently testable without Spring.**
8. **All Bounded Contexts and new Aggregates MUST comply with this ADR.**

## Agent Instructions

When creating or reviewing code:

```text
IF Layer == Domain:
    reject Spring dependency/annotation

IF Layer == Application:
    reject Spring dependency/annotation

IF Layer == Infrastructure:
    allow Framework integration when required

IF Layer == Presentation:
    allow Framework adapters

IF Layer == Bootstrap:
    allow Spring and Composition configuration
    reject Business Logic

FOR mandatory dependencies:
    require Constructor Injection

FOR dependency lookup:
    reject ApplicationContext.getBean(...) and Service Locator patterns

FOR Repository:
    define abstraction in Domain/Application
    implement persistence integration in Infrastructure

FOR Domain Service:
    require a genuine Business Rule
    reject creation solely because a Repository dependency exists

FOR tests:
    Domain/Application unit tests should run without Spring Context

ALWAYS:
    keep Framework dependencies from leaking inward
    keep Business Logic outside Composition Root
```
