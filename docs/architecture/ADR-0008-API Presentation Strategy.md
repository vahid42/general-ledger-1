# ADR-0008 — API & Presentation Layer Strategy — Ultra Compact

## Status

* **Status:** Accepted
* **Date:** 2026-08-22
* **Related ADRs:** ADR-0003, ADR-0004, ADR-0005, ADR-0006

## Purpose

Define the API and Presentation architecture for the **General Ledger Bounded Context**.

General Ledger is implemented as a **General Ledger Microservice** containing:

* Account Head Module
* Account Module
* Journal Entry Module — **Core**

Presentation is an external boundary adapter. It must remain independent from Domain implementation, Persistence, and Framework-specific details.

Core dependency direction:

```text
External World
      │
      ▼
Presentation / API Adapter
      │
      ▼
Application
      │
      ▼
Domain
```

## Core Decision

Presentation uses an **API Adapter** model.

Presentation is responsible for:

* Receiving external Requests
* Mapping external Contracts to Application Inputs
* Input Validation
* Receiving Authentication Context
* Interface-level Authorization
* Invoking Application Use Cases
* Mapping Application Results to Response DTOs
* HTTP Status mapping
* API Error handling
* API documentation
* Protocol-specific concerns

Presentation **must not contain Business Logic**.

Conceptual flow:

```text
External Client
      │
      ▼
REST API Adapter
      │
      ▼
Request DTO
      │
      ▼
Application Use Case
      │
      ▼
Domain Model
      │
      ▼
Application Result
      │
      ▼
Response DTO
      │
      ▼
REST API Adapter
      │
      ▼
External Client
```

## Architecture Rules

### Presentation Boundary

* API is an Adapter at the external system boundary.
* API must not directly control Domain Model.
* API must not execute Business Rules.
* Domain must not depend on HTTP, REST, Spring Security, Servlet API, or Presentation Frameworks.
* Presentation must depend on Application contracts/use cases, not Infrastructure implementations.

### Thin Controller

Controller flow:

```text
Receive Request
      │
      ▼
Validate Input
      │
      ▼
Map Input
      │
      ▼
Call Use Case
      │
      ▼
Map Result
      │
      ▼
Return Response
```

Controller must **not** contain:

* Business Rules
* Business Calculations
* Domain Decisions
* Persistence Logic
* SQL
* Direct Repository Access
* Transaction Orchestration
* Direct access to internal Entities of other Modules

Preferred:

```text
Controller
    │
    ▼
Application Use Case
    │
    ▼
Domain Aggregate
```

Not allowed:

```text
Controller
    ├── Repository
    ├── Database
    ├── Business Logic
    └── Domain Logic
```

## REST API

Primary HTTP API style: **RESTful HTTP API**.

Prefer Resource- and Business-Capability-oriented endpoints:

```text
GET    /api/v1/accounts
GET    /api/v1/accounts/{accountId}
POST   /api/v1/accounts
PATCH  /api/v1/accounts/{accountId}

GET    /api/v1/account-heads
GET    /api/v1/journal-entries
POST   /api/v1/journal-entries
```

Action endpoints are allowed when representing an explicit Business Operation:

```text
POST /api/v1/accounts/{accountId}/close
POST /api/v1/journal-entries/{journalEntryId}/post
```

Endpoints are Presentation Contracts only; Business Behavior remains in Application/Domain.

## API Versioning

Public APIs must be Versioned.

Initial strategy:

```text
/api/v1/...
```

Breaking changes require a new API version or an explicit compatible migration strategy:

```text
/api/v2/...
```

API Versioning applies to the **API Contract**, not independently to the Domain Model.

## Request DTO

API Requests must use Presentation-specific DTOs.

Example:

```java
public record CreateAccountRequest(
    String name,
    String currency
) {}
```

Preferred:

```text
CreateAccountRequest
      │
      ▼
Application Input / Command
      │
      ▼
Domain Model
```

Not:

```text
CreateAccountRequest
      │
      ▼
Account Entity
```

Presentation DTOs must not expose the internal Domain structure.

## Response DTO

API Responses must use Presentation-specific DTOs.

Example:

```java
public record AccountResponse(
    String id,
    String name,
    String currency,
    String status
) {}
```

Domain Entities must **not** be returned directly to external consumers.

Preferred:

```java
@GetMapping
public AccountResponse getAccount(...) {
    ...
}
```

Not:

```java
@GetMapping
public Account getAccount(...) {
    ...
}
```

## Domain/API Contract Isolation

Domain Entity must never become the API Contract.

Required boundary:

```text
External API Contract
      │
      ▼
Presentation DTO
      │
      ▼
Application Input / Result
      │
      ▼
Domain Model
```

The API Contract must evolve independently from internal Domain implementation.

## Validation

Validation has two distinct levels.

### Input Validation

Performed at the Presentation/Application boundary:

* Required fields
* Format
* Length
* Syntax
* Type
* Basic structural validation

Example:

```java
@NotBlank
String name

@NotNull
Currency currency
```

### Business Validation

Performed by Domain:

* Business Rules
* Invariants
* State Transitions
* Consistency
* Posting Rules
* Business Decisions

Example:

```text
"Name must not be empty"
    → Input Validation

"Closed Account cannot be reactivated"
    → Domain Invariant
```

**Controller must never own Business Invariants.**

## Error Handling

API Errors must use a consistent Consumer-facing Contract.

Recommended structure:

```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "Account was not found",
  "traceId": "..."
}
```

Optional fields may include:

```text
details
fieldErrors
timestamp
path
```

Error Contract is part of the public API Contract.

### Business vs Technical Errors

Business Errors originate from Domain Rules:

```text
ACCOUNT_ALREADY_CLOSED
INVALID_ACCOUNT_STATE
JOURNAL_ENTRY_NOT_BALANCED
POSTING_RULE_VIOLATION
```

Technical Errors represent infrastructure/system failures:

```text
PERSISTENCE_UNAVAILABLE
EXTERNAL_SERVICE_TIMEOUT
MESSAGE_BROKER_UNAVAILABLE
```

Presentation maps both categories into appropriate API responses.

**Domain owns Business Error semantics; Presentation owns external representation.**

## Global Exception Handling

API Exception handling must be centralized.

Spring Boot may use:

```text
@ControllerAdvice
```

Conceptual flow:

```text
Controller
    │
    ▼
Application / Domain Exception
    │
    ▼
Global Exception Handler
    │
    ▼
Standard Error Response
```

Endpoint-specific duplicated Error Handling should be avoided.

## HTTP Status Codes

Use HTTP Status Codes according to actual Response semantics:

```text
200 OK
201 Created
202 Accepted
204 No Content

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity

500 Internal Server Error
```

Status mapping must be consistent and predictable.

## API Contract

API Contract is independent from internal implementation.

Contract includes:

* Endpoint
* HTTP Method
* Request Schema
* Response Schema
* Status Codes
* Error Schema
* Authentication Requirements
* Authorization Requirements
* Version

Domain/Application implementation changes must not unnecessarily change the external Contract.

## OpenAPI

Public APIs must be documented with **OpenAPI**.

Documentation should represent the actual API Contract and, where applicable, include:

* Endpoint
* HTTP Method
* Request
* Response
* Validation
* Errors
* Authentication
* Authorization
* Examples
* Version

## Authentication

Authentication is a Presentation/Security concern.

Presentation receives the authenticated identity/context and transfers the required context to Application.

Domain must not depend on:

```text
HTTP Request
JWT
SecurityContext
Spring Security
Servlet API
```

Preferred:

```text
External Authentication
      │
      ▼
Presentation / Security Adapter
      │
      ▼
Application Context
      │
      ▼
Domain
```

## Authorization

Authorization has two levels.

### Interface-Level Authorization

Technical/interface authorization may be enforced in Presentation/Security:

```text
ROLE_ACCOUNT_MANAGER
ROLE_ADMIN
```

### Business-Level Authorization

If authorization is itself a Business Rule, it must be modeled at the appropriate Application/Domain boundary.

Example:

```text
Only authorized actor can post this JournalEntry
```

**Technical Authorization and Business Authorization must not be conflated.**

## Pagination

Collection endpoints should support Pagination when required.

Example:

```text
GET /api/v1/accounts?page=0&size=20
```

Possible response:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

Pagination is an API/Application Query concern and must not leak into Domain Model.

## Filtering & Sorting

HTTP filtering/sorting must be mapped into controlled Application Query models.

Example:

```text
GET /api/v1/accounts?status=ACTIVE&sort=name
```

Preferred:

```text
HTTP Query
    │
    ▼
Query DTO
    │
    ▼
Application Query
    │
    ▼
Query Port / Adapter
    │
    ▼
Persistence
```

Query parameters must not be converted directly into uncontrolled Database queries.

## Idempotency

Idempotency should be supported for sensitive Financial Use Cases where retries or duplicate submissions are possible.

Example:

```text
POST /api/v1/journal-entries
Idempotency-Key: <key>
```

Goal:

```text
Retry
  │
  ▼
Duplicate Request
  │
  X
  ▼
Duplicate Business Operation
```

Idempotency is **not mandatory for every endpoint**. It must be decided according to Use Case semantics.

Persistence/Storage details belong to a dedicated Persistence/Infrastructure ADR.

## Transaction Boundary

Controller must not manage Transactions directly.

Transaction boundary must align with Application Use Case and required Business Consistency.

Preferred:

```text
Controller
    │
    ▼
Application Use Case
    │
    ▼
Transaction Boundary
    │
    ▼
Domain
```

Application owns Transaction Boundary coordination. Technology-specific Transaction configuration belongs to Infrastructure/Configuration.

## Async Operations

Long-running operations should not necessarily keep the HTTP request open until completion.

Preferred pattern:

```text
POST /api/v1/operations
      │
      ▼
202 Accepted
      │
      ▼
Operation ID
```

Status can later be queried:

```text
GET /api/v1/operations/{operationId}
```

Applicable to Batch and long-running processing.

## API Contract Evolution

### Non-Breaking Changes

Examples:

* Add Optional Field
* Add new Endpoint
* Add optional Error Detail
* Add new Capability without breaking the existing Contract

### Breaking Changes

Examples:

* Remove Field
* Change Field Type
* Change Field Semantic
* Remove Endpoint
* Change behavior in a way that breaks existing Consumers

Breaking Changes require a new Version or compatible migration strategy.

## Business Module Alignment

Presentation should align with General Ledger Business Module boundaries:

```text
General Ledger Bounded Context
│
├── Account Head Module
├── Account Module
└── Journal Entry Module ⭐ Core
```

Typical API organization:

```text
Account Head Module
    └── /api/v1/account-heads

Account Module
    └── /api/v1/accounts

Journal Entry Module
    └── /api/v1/journal-entries
```

URL naming does not have to exactly mirror internal Module names. The key requirement is preserving **Business Ownership and clear Boundaries**.

## Journal Entry — Core Domain

`JournalEntry` is the Core Aggregate.

Its API must not turn Core Domain behavior into CRUD logic.

Preferred:

```text
POST /api/v1/journal-entries
      │
      ▼
CreateJournalEntryUseCase
      │
      ▼
JournalEntry Aggregate ⭐ Core
      │
      ├── Validate Lines
      ├── Validate Debit/Credit
      ├── Validate Balancing
      └── Enforce Posting Rules
```

Not:

```text
JournalEntryController
      ├── Calculate Debit/Credit
      ├── Validate Balancing
      ├── Execute Posting Rules
      └── Manipulate Persistence
```

**Core Domain behavior must remain in Domain.**

## Account & Account Head API

Presentation exposes Account and Account Head capabilities through Application Use Cases.

Preferred:

```text
CreateAccountController
      │
      ▼
CreateAccountUseCase
      │
      ├── AccountHead Capability / Port
      ├── Account Domain
      └── AccountRepository
```

Responsibility model:

```text
Presentation
    = API Adapter

Application
    = Use Case Orchestration

Account
    = Account Business Responsibility

AccountHead
    = Account Head Business Responsibility

Repository
    = Persistence Abstraction
```

## External API vs Internal Module Contract

These Contracts must remain separate.

### External API

```text
REST
OpenAPI
Versioning
Authentication
Authorization
Error Contract
```

### Internal Module Contract

```text
Application Contract
Internal Port
Domain Event
Capability Contract
```

They do not need to share the same models.

Example:

```text
External:
CreateAccountRequest

Internal:
AccountHeadPort
```

## API Naming

API naming must be:

* Consistent
* Predictable
* Resource-oriented
* Versioned
* Independent from internal implementation

Examples:

```text
/api/v1/accounts
/api/v1/accounts/{accountId}

/api/v1/account-heads
/api/v1/account-heads/{accountHeadId}

/api/v1/journal-entries
/api/v1/journal-entries/{journalEntryId}
```

Action endpoints are allowed for explicit Business Operations:

```text
POST /api/v1/accounts/{accountId}/close
POST /api/v1/journal-entries/{journalEntryId}/post
```

## API Security

API must consider:

* Authentication
* Authorization
* Input Validation
* Secure Headers
* Rate Limiting when required
* Sensitive-information protection
* No Stack Trace exposure
* Audit Logging for sensitive operations when required

Detailed Security decisions belong to dedicated Security ADRs.

## Current Persistence State

Current General Ledger Persistence is **In-Memory**.

This ADR does **not** decide the use of:

```text
JPA
Hibernate
SQL Database
Redis
MongoDB
```

Presentation must not depend on current In-Memory implementation.

Required architecture:

```text
Presentation
      │
      ▼
Application
      │
      ▼
Repository Port
      │
      ▼
Current In-Memory Implementation
```

Future migration to Database or another Persistence technology must not require unnecessary Presentation changes.

Persistence Strategy must be defined in a dedicated ADR.

## Architectural Rules

1. Controller must be Thin.
2. Controller must not contain Business Logic.
3. Controller must not directly call Repository.
4. Controller must not directly call Persistence Implementation.
5. Domain Entity must not be directly returned as API Response.
6. Request/Response must be Presentation Contracts.
7. Input Validation belongs at the Presentation/Application boundary.
8. Business Invariants must be enforced in Domain.
9. Application owns Use Case Orchestration.
10. Transaction Boundary belongs to Application Use Case.
11. API Exception Handling must be centralized.
12. API Contract must be independent from Domain Model.
13. Public APIs must be Versioned.
14. APIs should align with Business Module Boundaries.
15. Presentation must not consume internal Entities of another Module directly.
16. Presentation must not consume Repository Implementations.
17. Presentation must not access In-Memory or Database Storage directly.
18. JournalEntry Core Domain must not be reduced to CRUD behavior because of API design.
19. Authentication Context must reach Application without coupling Domain to Security Frameworks.
20. External API Contract and Internal Module Contract must be managed independently.

## Architectural Enforcement

These rules should be enforced through Architectural Tests and Code Review.

Examples:

```text
Controller
    must not depend on
Repository Implementation
```

```text
Controller
    must not depend on
Infrastructure Implementation
```

```text
Controller
    must depend on
Application Use Case / Contract
```

```text
Presentation
    must not directly access
Domain Entity of another Module
```

```text
Presentation
    must not directly access
Persistence
```

```text
Domain
    must not depend on
Presentation
```

Architectural checks should cover:

* Controller Thinness
* Presentation → Application Dependency
* API Contract Isolation
* Domain Model Isolation
* Repository Access Rules
* Module Boundaries
* Infrastructure Isolation
* Circular Dependencies

**ArchUnit** and architectural Code Review are recommended enforcement mechanisms.

## Positive Consequences

This architecture provides:

* API independence from Domain implementation
* Thin and testable Controllers
* Business Logic retained in Domain
* Protection of `JournalEntry` Core Domain
* More stable API Contracts
* Independent API Evolution
* Centralized Error Handling
* API Versioning
* Visible Business Module Boundaries
* Presentation independence from Persistence Evolution
* Domain/Application independence from HTTP and Presentation Frameworks

## Negative Consequences

Costs include:

* More DTOs
* API ↔ Application Mapping
* API Contract Lifecycle Management
* Versioning/Migration overhead
* OpenAPI maintenance
* More Layers for simple Use Cases than a basic CRUD implementation

These costs are accepted to preserve Domain independence, API stability, and Core Domain protection.

However:

> DDD and Layering must not create unnecessary Abstractions for genuinely simple Use Cases.

## Final Decision

```text
Presentation
    = Adapt

Application
    = Orchestrate

Domain
    = Decide
```

Presentation is an API Adapter at the external boundary of the General Ledger Bounded Context.

It owns:

* External Request handling
* Input Validation
* Contract mapping
* Authentication Context
* Interface-level Authorization
* Application Use Case invocation
* Result mapping
* Response generation
* API Error handling
* HTTP Protocol concerns

Controllers must remain Thin and must not own:

* Business Logic
* Domain Rules
* Persistence
* Transaction Orchestration

API Requests and Responses must use Presentation DTOs. Domain Entities must not be exposed directly.

Business Rules and Invariants remain in Domain according to ADR-0006. Application owns Use Case Orchestration.

API organization should align with:

```text
Account Head
Account
Journal Entry ⭐ Core
```

`JournalEntry` must remain a protected Core Domain and must not be reduced to CRUD.

Presentation must consume Application Use Cases/Contracts rather than Repository or Persistence Implementations.

Current Persistence is In-Memory, but Presentation must remain independent from it.

Public APIs are Versioned and documented through OpenAPI.

## Final Presentation Model

```text
                         External Client
                                │
                                ▼
                    ┌─────────────────────┐
                    │   REST API Adapter  │
                    │      Controller     │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     Request DTO     │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Application UseCase │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
        Account Module   Account Head      Journal Entry
                           Module              Module
                                                ⭐ Core
              │                │                │
              └────────────────┼────────────────┘
                               │
                               ▼
                         Domain Model
                               │
                               ▼
                      Application Result
                               │
                               ▼
                         Response DTO
                               │
                               ▼
                       REST API Adapter
                               │
                               ▼
                         External Client
```

General Ledger boundary:

```text
General Ledger Bounded Context
            │
            ▼
General Ledger Microservice
            │
   ┌────────┼────────┐
   │        │        │
   ▼        ▼        ▼
Account  Account   Journal Entry
 Head     Module       Module
Module                ⭐ Core
```

Dependency direction:

```text
Presentation
      │
      ▼
Application
      │
      ▼
Domain

Infrastructure
      │
      └── implements required Ports
```

Never:

```text
Presentation
      │
      X
      ▼
Infrastructure
```

## Relationship with Related ADRs

### ADR-0003 — General Ledger Boundary

Defines:

```text
General Ledger
      │
      ▼
General Ledger Bounded Context
      │
      ▼
General Ledger Microservice
```

and current Business Modules:

```text
Account Head
Account
Journal Entry ⭐ Core
```

ADR-0008 does not change these boundaries; it defines API Presentation at their external boundary.

### ADR-0004 — Dependency Direction & Internal Communication

Defines Dependency direction and internal Ports/Contracts:

```text
Presentation
      │
      ▼
Application
      │
      ├── Internal Contract / Port
      ├── Repository Port
      │
      ▼
Domain

Infrastructure
      │
      └── implements Ports
```

ADR-0008 applies these principles to Presentation.

### ADR-0005 — Domain-Driven Design

Defines the overall DDD and Strategic/Tactical Domain structure.

ADR-0008 exposes capabilities according to established Business Boundaries.

### ADR-0006 — Domain Model Strategy

Defines the Rich Domain Model:

```text
Entity
Value Object
Aggregate
Aggregate Root
Domain Service
Domain Event
```

Relationship:

```text
ADR-0006
    │
    └── Domain Decides
            ▲
            │
ADR-0008 ───┘
    │
    └── Presentation Adapts
```

Therefore:

```text
Presentation
    = Adapt

Application
    = Orchestrate

Domain
    = Decide
```

ADR-0008 must not redefine:

* Domain Boundaries
* Aggregate Boundaries
* Persistence Strategy

## Decision Chain

```text
ADR-0003
    │
    └── General Ledger Boundary
            │
            ▼
       Bounded Context
            │
            ▼
        Microservice
            │
            ▼
ADR-0005 ──► DDD Structure
            │
            ▼
ADR-0006 ──► Rich Domain Model
            │
            ▼
       Aggregate / Entity
            │
            ▼
ADR-0004 ──► Dependency / Ports
            │
            ▼
ADR-0008 ──► Presentation / API
```

## Agent Instructions

When designing, reviewing, or modifying Presentation/API code:

1. Treat Presentation as an external **API Adapter**.
2. Keep Controllers Thin.
3. Route Business behavior through Application Use Cases.
4. Never place Business Rules or Invariants in Controllers.
5. Never access Repository Implementations directly from Presentation.
6. Never access Persistence directly from Presentation.
7. Never expose Domain Entities directly as API Responses.
8. Use Presentation-specific Request/Response DTOs.
9. Keep API Contracts independent from Domain Models.
10. Separate Input Validation from Business Validation.
11. Keep Business Invariants in Domain.
12. Keep Use Case Orchestration in Application.
13. Keep Transaction Boundaries at the Application Use Case level.
14. Centralize API Exception Handling.
15. Use consistent HTTP Status mappings.
16. Keep Business Errors distinct from Technical Errors.
17. Version public APIs.
18. Keep OpenAPI aligned with the real API Contract.
19. Align API organization with Business Module ownership where appropriate.
20. Protect `JournalEntry` as the Core Domain; do not reduce it to CRUD.
21. Keep External API Contracts separate from Internal Module Contracts.
22. Keep Authentication Framework details outside Domain.
23. Distinguish Interface-level Authorization from Business-level Authorization.
24. Map Filtering, Sorting, and Pagination through Application Query models.
25. Apply Idempotency selectively to sensitive/retry-prone Use Cases.
26. Keep Persistence implementation replaceable without Presentation changes.
27. Do not introduce unnecessary Abstractions solely because of DDD/Layering.
28. If a change violates this ADR, require an ADR amendment or a new Architectural Decision.

## Status

**Accepted**

This ADR governs decisions concerning:

```text
REST API
Controller
Request DTO
Response DTO
API Contract
API Versioning
Input Validation
API Error Handling
HTTP Status Mapping
Authentication Context
Authorization Boundary
Pagination
Filtering
Sorting
API Idempotency
API Transaction Boundary
OpenAPI
Presentation Module Structure
```

Any decision that changes these principles must be recorded through a new Architectural Decision or a formal amendment to ADR-0008.
