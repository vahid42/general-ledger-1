# ADR-0011 — Domain Layer Architecture

## Status

Accepted — 2026-08-22

## Purpose

Define the architecture, responsibilities, dependencies, packaging, and enforcement rules of the `Domain` Layer inside the `General Ledger Bounded Context`.

## Context

* `General Ledger` is one independent `Bounded Context` implemented as one `General Ledger Microservice`.
* It contains three Subdomains:

  * `Account Head` — Supporting Subdomain
  * `Account` — Supporting Subdomain
  * `Journal Entry` — Core Subdomain
* These Subdomains remain inside **one** `Bounded Context`; they are not separate Bounded Contexts.
* The Domain contains three primary Aggregate Roots:

  * `AccountHead`
  * `Account`
  * `JournalEntry` — **Core Aggregate**
* `JournalEntry` is the primary location for ledger Business Logic and Business Invariants.

## Core Architecture Rules

### Layering and Dependency Direction

The Microservice follows Clean Architecture / Onion Architecture:

```text
General Ledger Microservice
├── Presentation
├── Application
├── Domain
└── Infrastructure
```

Dependency direction:

```text
Presentation → Application → Domain
Infrastructure → Domain/Application
```

`Domain` must remain independent of outer layers:

```text
Domain ✕ Presentation
Domain ✕ Application
Domain ✕ Infrastructure
Domain ✕ Database
Domain ✕ Framework
```

### Domain Responsibilities

`Domain` owns the Business Model, including:

* Entities
* Value Objects
* Aggregate Roots and behavior
* Business Rules
* Business Invariants
* Domain Services
* Domain Events
* Domain Exceptions
* Domain Policies
* Domain Specifications

`Domain` must not contain:

* HTTP / REST / Controllers
* Database access
* JPA / Hibernate
* Messaging Infrastructure
* Authentication Infrastructure
* External API implementations
* Configuration
* Serialization Infrastructure
* Framework lifecycle code

## DDD Boundaries

The following distinctions are mandatory:

```text
Subdomain ≠ Bounded Context ≠ Module ≠ Microservice
```

Current architecture:

```text
3 Subdomains
    ↓
1 Bounded Context
    ↓
1 Microservice
    ├── Account Head Module
    ├── Account Module
    └── Journal Entry Module
```

`account-head`, `account`, and `journal-entry` are internal Modules, not independent Bounded Contexts or Microservices.

Three Aggregates or Business Boundaries do not imply three Bounded Contexts.

## Domain Modules

```text
domain/
├── common/
├── shared/
├── account-head/
├── account/
└── journal-entry/
```

Modules are organized around Business Boundaries and Aggregates to improve Cohesion, make boundaries visible, and reduce Coupling.

### Aggregate-oriented structure

```text
domain/
├── common/
├── shared/
├── account-head/
│   └── AccountHead Aggregate
├── account/
│   └── Account Aggregate
└── journal-entry/
    └── JournalEntry Aggregate
```

Each Aggregate owns its own Business Invariants and may contain only the Domain components it actually needs.

## Aggregate Rules

### AccountHead Aggregate

Owns rules related to:

* Identity
* Hierarchy
* Classification
* Placement
* Account-head-specific Business Rules

It must be a real Domain Model, not merely a CRUD Model.

### Account Aggregate

Owns rules related to:

* Identity
* Account type
* Status
* Account properties
* Account-specific Business Rules

Business behavior should remain inside the Aggregate whenever appropriate.

### JournalEntry Aggregate — Core Aggregate

`JournalEntry` is the **Core Aggregate** and primary location of ledger Business Logic.

It owns behavior such as:

* Debit/Credit validation
* Balance validation
* Journal line validation
* Posting-rule validation
* Posting-state determination
* Posting

`JournalEntry` must remain a **Rich Domain Model** and must not become:

* CRUD-only Entity
* Anemic Domain Model
* Repository-driven Business Logic
* Application-Service-driven Business Logic

Core Domain protection is mandatory: Supporting Subdomains (`AccountHead`, `Account`) must support `JournalEntry` without unnecessarily complicating or weakening its model.

## Rich Domain Model

Business behavior belongs primarily in Domain Objects.

Preferred:

```java
account.debit(amount);
```

Not:

```java
accountService.debit(account, amount);
```

`Application` owns Use Case orchestration; `Domain` owns Business Behavior and Invariants.

## Business Rule Placement

Place each Business Rule in the closest appropriate Domain component:

```text
Aggregate / Entity
        ↓
Value Object
        ↓
Domain Service
```

`Application Service` is **not** the default location for Business Rules.

Use `Application` when orchestration across multiple Domain Objects/Aggregates is required.

## Aggregate Interaction

Aggregates must remain as independent as practical.

An Aggregate must not directly depend on or invoke Repository Implementations or Infrastructure.

Example of forbidden default coupling:

```text
JournalEntry → AccountRepository
```

When a Use Case requires multiple Aggregates:

```text
CreateJournalEntry
├── load / validate Account
├── validate AccountHead
└── create JournalEntry
```

Responsibilities:

* `Application` → orchestration
* `Account` → Account rules
* `AccountHead` → AccountHead rules
* `JournalEntry` → JournalEntry rules

Application orchestration must not replace the Domain Model.

## Repository Abstraction

Repositories are **Persistence Abstractions**, not Business Services or Domain Services.

Possible Repository Ports:

```text
AccountHeadRepository
AccountRepository
JournalEntryRepository
```

Conceptual dependency:

```text
Domain / Application
        ↓
Repository Port
        ↓
Infrastructure
        ↓
Database
```

Rules:

* Repository Port must be separated from Persistence Implementation.
* Database/ORM implementation belongs in `Infrastructure`.
* Aggregates must not know Repository Implementations.
* Not every Interface is a Repository.
* Internal Ports/Contracts may represent capabilities or communication rather than persistence.

## Domain Ports / External Capabilities

If Domain behavior genuinely requires an external capability, depend on an appropriate Contract rather than a technology implementation.

Example:

```java
interface ExchangeRateProvider {
    MoneyRate getRate(Currency source, Currency target);
}
```

Domain may know the Contract, but must not know:

* HTTP Client
* API Client
* External API implementation
* Messaging technology

Not every Interface should automatically be placed in `Domain`; placement depends on dependency ownership and the relevant Use Case.

## Framework Independence

`Domain` must be Framework Independent and testable without a Spring Application Context.

Forbidden by default:

```text
@Component
@Service
@Repository
@Autowired
@Configuration
@Entity
```

Domain must not depend on:

```text
Spring
Spring Boot
JPA
Hibernate
Servlet API
HTTP
REST
Database Driver
Message Broker
```

## Persistence Independence

The Domain Model must not be designed around Database or ORM constraints.

```text
Domain Model
    ↓
Mapping / Persistence Adapter
    ↓
Persistence Model
    ↓
Database
```

Database and ORM must adapt to Domain requirements, not define the Business Model.

## External Concerns

Business-relevant external concerns such as:

* Time
* Randomness
* External capabilities
* Environment-dependent behavior

may be abstracted when genuinely required by Business Logic.

Example:

```java
interface Clock {
    Instant now();
}
```

Do not create abstractions merely for every Java API or technical dependency; abstract only Business-relevant dependencies.

## Domain Exceptions

Business-rule failures must be represented as Domain Errors / Domain Exceptions.

Example:

```java
class InvalidJournalEntryException extends DomainException {}
```

Domain Exceptions must remain independent of HTTP and Frameworks.

Forbidden:

```java
throw new ResponseStatusException(...);
```

Mapping Domain Errors to HTTP/API errors belongs outside `Domain`.

## Domain Events

Domain Events represent Business events and must remain technology-independent.

```text
Domain
  ↓
Domain Event
  ↓
Application / Infrastructure
  ↓
Messaging Infrastructure
```

Example Business Event:

```text
JournalEntryPosted
```

Infrastructure concepts such as `KafkaMessage` or `RabbitMessage` are not Domain Events.

## `common` Rules

`domain/common/` is only for foundational Domain concepts that are not specific to one Aggregate.

Examples:

```text
common/
├── AggregateRoot
├── Entity
├── ValueObject
├── Identifier
├── DomainEvent
└── DomainException
```

`common` must **not** become a dumping ground for:

* Aggregate-specific Business Logic
* Generic Utilities
* Helpers
* Shared Services
* Logic moved there only to avoid duplication

Business Logic belonging to `AccountHead`, `Account`, or `JournalEntry` stays within its appropriate boundary.

## `shared` Rules

`domain/shared/` is only for concepts genuinely shared by multiple parts of the General Ledger Domain Model.

Examples:

```text
shared/
├── Money
├── Currency
├── FiscalPeriod
└── Shared Value Objects / Enumerations
```

`shared` must not become a generic container for:

* Utilities
* Helpers
* Generic Services
* Common Business Logic
* Code moved only to eliminate duplication

This internal `shared` is **not** a DDD `Shared Kernel` between Bounded Contexts. The current architecture has only one `General Ledger Bounded Context`.

## Aggregate Packaging

Each Aggregate should have a visible package boundary.

Example:

```text
account/
├── Account.java
├── AccountId.java
├── AccountRepository.java
├── policies/
└── events/

journal-entry/
├── JournalEntry.java
├── JournalEntryId.java
├── JournalLine.java
├── JournalEntryRepository.java
├── policies/
└── events/
```

These components are optional; create them only when required by the Domain.

## Module Boundary Rules

`account-head`, `account`, and `journal-entry`:

* Are internal Modules.
* Are not independent Bounded Contexts.
* Are not independent Microservices.
* Do not require independent Databases.
* Must not directly consume each other's internal Contracts merely because they are Modules.
* Must preserve Aggregate boundaries and minimize Coupling.

A Module does not automatically justify a new Microservice.

## Domain Service Rules

The existence of an Aggregate does not imply a corresponding Service.

Do **not** automatically create:

```text
AccountHeadService
AccountService
JournalEntryService
```

Use a `Domain Service` only when:

1. The rule is genuine Domain Logic.
2. It does not naturally belong to one specific Aggregate/Domain Object.
3. Placing it in an Aggregate would create inappropriate Coupling.

`Domain Service` complements the Rich Domain Model; it does not replace Aggregates.

## Domain Testability

`Domain` must be testable without:

* Spring Context
* Database
* Network
* Message Broker
* External API
* Infrastructure Configuration

Domain Unit Tests must be:

* Fast
* Deterministic
* Infrastructure-independent
* Focused directly on Business Invariants

`JournalEntry` must have direct Domain Unit Tests covering its Core Business Rules and Invariants.

## Architecture Enforcement

The following rules must be enforced through Architecture Tests and Code Review:

* `Domain` must not depend on `Presentation`.
* `Domain` must not depend on `Application`.
* `Domain` must not depend on `Infrastructure`.
* `Domain` must not depend on Database, Spring, JPA, or Hibernate.
* `Domain` must not contain HTTP-specific code.
* `Domain` must not contain Infrastructure Configuration.
* Domain Model must not be designed from Persistence Model constraints.
* Aggregate Business Rules should remain inside the relevant Aggregate whenever possible.
* `Application` orchestrates Use Cases; it does not own Domain Business Rules.
* Aggregates must not invoke Repository Implementations.
* Repository Ports must remain separate from Persistence Implementations.
* Aggregate existence alone does not justify a Domain Service.
* Subdomain existence alone does not justify a new Bounded Context.
* Module existence alone does not justify a new Microservice.
* `account-head`, `account`, and `journal-entry` remain Modules of the same `General Ledger Bounded Context` unless a new architectural decision is recorded.
* Aggregate-specific Business Logic must not be placed in `common` or `shared`.
* `shared` must not become a generic Utility/Business Logic container.
* Each Aggregate must have a clear Consistency boundary.
* Aggregates must not depend on Infrastructure Implementations.
* `JournalEntry` must remain a Core Aggregate and be protected from CRUD-centric design.

## Final Structure

```text
General Ledger Microservice
├── Presentation
├── Application
├── Domain
│   ├── common/
│   ├── shared/
│   ├── account-head/
│   │   └── AccountHead Aggregate
│   ├── account/
│   │   └── Account Aggregate
│   └── journal-entry/
│       └── JournalEntry Aggregate
└── Infrastructure
```

DDD model:

```text
General Ledger
├── Account Head
│   └── Supporting Subdomain
├── Account
│   └── Supporting Subdomain
└── Journal Entry
    └── Core Subdomain
        └── Core Aggregate
```

## Consequences

### Benefits

* Framework-independent Business Logic
* High testability
* Lower Coupling
* Higher Aggregate-level Cohesion
* Explicit Domain boundaries
* Rich Domain Model
* Protection of Core Domain
* Visible Aggregate boundaries in code
* Separation of Persistence from Domain Model
* Database/ORM replaceability
* Independently evolvable internal Modules
* Alignment between code structure and Domain Model
* Future evolution toward additional Bounded Contexts when justified
* Possible future Module extraction when supported by a real architectural reason

### Costs

* More classes and design effort
* Explicit Domain/Persistence Mapping when models are separated
* More careful Port/Abstraction design
* Greater complexity than CRUD architecture
* Continuous Architecture Testing and Code Review
* Risk of `common`/`shared` misuse
* Need for strict Aggregate interaction rules
* Continuous decisions about correct Business Rule placement

## Implementation Status

This ADR is the baseline architecture for the `Domain` Layer of the `General Ledger Bounded Context`.

Current implementation model:

```text
1 General Ledger Bounded Context
        ↓
1 General Ledger Microservice
        ↓
Aggregate-oriented Domain Modules
        ├── account-head
        ├── account
        └── journal-entry
```

`JournalEntry` remains the Core Aggregate and primary center of ledger Business Logic.

Detailed Domain concerns are defined separately in ADRs for:

* Entity & Aggregate Strategy
* Value Object Strategy
* Domain Service Strategy
* Domain Event Strategy
* Repository Abstraction Strategy
* Domain Exception & Error Strategy
* Domain Identity Strategy

## Agent Instructions

* Treat this ADR as the baseline for `Domain Layer` architecture within `General Ledger`.
* Preserve the distinction between `Subdomain`, `Bounded Context`, `Module`, and `Microservice`.
* Keep `AccountHead`, `Account`, and `JournalEntry` inside the same `General Ledger Bounded Context` unless a new ADR changes this decision.
* Treat `JournalEntry` as the `Core Aggregate` and protect its Business Logic from CRUD-centric or Anemic design.
* Keep Business Rules in the most appropriate Domain Object; use `Domain Service` only when justified.
* Keep Use Case orchestration in `Application`.
* Never make `Domain` depend on `Presentation`, `Application`, `Infrastructure`, Database, ORM, Spring, HTTP, or other technical Frameworks.
* Keep Repository Ports separate from Persistence Implementations.
* Never allow Aggregates to invoke Infrastructure/Repository Implementations directly.
* Keep Aggregate-specific logic out of `common` and `shared`.
* Treat `shared` as Domain-model sharing, not as a generic Utility package or cross-Context `Shared Kernel`.
* Preserve Aggregate Consistency boundaries and minimize direct Aggregate coupling.
* Enforce architecture through Architecture Tests and Code Review.
* Do not introduce new Bounded Contexts, Microservices, or Domain Services merely because a Subdomain, Module, or Aggregate exists.
