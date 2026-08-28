# ADR-0017 — Domain Error & Exception Strategy 

## Purpose

Define a unified strategy for modeling, owning, propagating, translating, and handling Domain/Business Errors while keeping Domain independent from HTTP, Frameworks, Persistence, Messaging, and other technical concerns.

## Core Rules

* Separate **Domain/Business Error** from **Technical/Infrastructure Error**.
* **Domain Error** means a Business Rule or Business Invariant prevents an operation.
* **Technical Error** means a technology/infrastructure failure such as DB, network, serialization, messaging, timeout, or external-service failure.
* These error categories must never be conflated.
* In Java, **Domain Exception is the default mechanism** for propagating Business Failure.
* `Result`, `Outcome`, or `Either` are allowed only when a clear architectural need exists.
* Domain Exceptions must be **Framework Independent**.
* Domain must not depend on:

  * HTTP/REST
  * Spring
  * JPA/Hibernate
  * Database exceptions
  * Message Broker exceptions
  * Transport/API contracts
  * Logging frameworks
* Domain Exception must represent a concrete Business Concept from the project's **Ubiquitous Language**.
* Generic exceptions such as `ValidationException`, `ProcessingException`, `OperationException`, `GeneralException`, and `CommonException` are discouraged for Domain Rules.

## Error Classification

```text
Error
├── Domain / Business Error
└── Technical / Infrastructure Error
```

### Domain Error

Examples:

```text
InsufficientBalance
AccountAlreadyClosed
AccountAlreadyBlocked
TransferLimitExceeded
InvalidAccountStateTransition
IncompatibleCurrency
```

### Technical Error

Examples:

```text
DatabaseConnectionFailure
Timeout
NetworkFailure
SerializationFailure
MessageBrokerUnavailable
ExternalServiceUnavailable
```

## Domain Exception

A Base Domain Exception is accepted:

```java
public abstract class DomainException extends RuntimeException {
    private final DomainErrorCode errorCode;

    protected DomainException(
            DomainErrorCode errorCode,
            String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainErrorCode getErrorCode() {
        return errorCode;
    }
}
```

Rules:

* All Exception-based Domain Errors must derive from a recognized Domain Base Type.
* The Base Type enables Domain Error identification, stable Error Code propagation, and upper-layer mapping.
* Avoid deep or unnecessary Exception hierarchies.
* Prefer concrete Exceptions with explicit Business Meaning.

Example hierarchy:

```text
DomainException
├── AccountDomainException
│   ├── InsufficientBalanceException
│   ├── AccountAlreadyClosedException
│   └── InvalidAccountStateTransitionException
├── TransferDomainException
│   └── TransferLimitExceededException
└── PaymentDomainException
```

## Error Code Rules

Business Errors exposed outside Domain must have stable, type-safe Error Codes.

```text
ACCOUNT.INSUFFICIENT_BALANCE
ACCOUNT.ALREADY_CLOSED
ACCOUNT.ALREADY_BLOCKED
TRANSFER.LIMIT_EXCEEDED
TRANSFER.INVALID_STATE
PAYMENT.INVALID_STATE
```

`DomainErrorCode` may be modeled as an enum/interface:

```java
public interface DomainErrorCode {
    String value();
}
```

Error Codes must be:

* Stable.
* Unique.
* Independent of Message.
* Independent of HTTP Status.
* Independent of Persistence Technology.
* Usable for Logging/Monitoring.
* Mappable to API Error Contracts.

```text
Domain Error Code ≠ HTTP Status Code
```

Example:

```text
ACCOUNT.INSUFFICIENT_BALANCE
        ↓
HTTP 409
```

The HTTP mapping may change without changing the Domain Error Code.

## Error Message Rules

* Message is not the primary Contract.
* Clients must never make Business decisions based on Message text.
* Error Code is the machine-readable contract.
* Message may serve Developer, Logging, Debugging, User-facing, or Localization purposes.
* Domain Message does not have to equal the final Client Message.
* Presentation may translate Error Codes into localized/user-facing Messages.

```text
Domain Error Code
      ↓
Application
      ↓
Localization / API Message
```

## Error Ownership

A Business Rule must be enforced by the component that owns the Rule.

### Entity

Entity protects its own Business Invariants.

```java
public void withdraw(Money amount) {
    if (balance.isLessThan(amount)) {
        throw new InsufficientBalanceException();
    }
    balance = balance.subtract(amount);
}
```

### Aggregate

Aggregate Root protects Aggregate-level Consistency and Invariants.

```java
public void changeStatus(AccountStatus newStatus) {
    if (!status.canTransitionTo(newStatus)) {
        throw new InvalidAccountStateTransitionException();
    }
    this.status = newStatus;
}
```

An Aggregate must never allow invalid state and rely on Application to repair it later.

### Value Object

Value Object protects its own Value Invariants.

```java
public record Percentage(BigDecimal value) {
    public Percentage {
        if (value == null ||
            value.compareTo(BigDecimal.ZERO) < 0 ||
            value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidPercentageException();
        }
    }
}
```

### Domain Service

Domain Service may create Domain Errors when it owns Business Logic that has no natural Entity/Value Object owner.

```text
Entity
└── Entity Invariants

Value Object
└── Value Invariants

Aggregate Root
└── Aggregate Invariants

Domain Service
└── Domain Operation Rules
```

Domain Service must not create Domain Errors for Infrastructure failures.

## Validation Rules

Separate validation into:

```text
Validation
├── Structural / Input Validation
└── Business Validation
```

### Structural / Input Validation

Examples:

```text
email is blank
amount is null
name exceeds max length
field format is invalid
```

Usually handled at system/Application/Presentation boundaries.

### Business Validation

Examples:

```text
Account is closed
Balance is insufficient
Transfer limit exceeded
Currency is incompatible
```

Must be enforced by the Domain owner of the Rule.

## Technical Error Rules

* Domain must never create or depend on technical exceptions such as:

  * `SQLException`
  * `DataAccessException`
  * `TimeoutException`
  * `ConnectException`
  * `JsonProcessingException`
  * `KafkaException`
  * `HttpClientErrorException`
* Infrastructure owns technology failures.
* Infrastructure may wrap technical exceptions into appropriate Infrastructure/Application errors.
* Technical Errors must never be artificially converted into Domain Errors.

```java
try {
    repository.save(account);
} catch (DataAccessException ex) {
    throw new AccountPersistenceException(ex);
}
```

Incorrect:

```java
catch (DataAccessException ex) {
    throw new DomainException(...);
}
```

## Error Translation

Translation happens at layer boundaries.

```text
Domain Error
    ↓
Domain Exception
    ↓
Application
    ├── Propagate
    ├── Handle
    ├── Map to Application Error
    ├── Retry
    └── Compensation
    ↓
Presentation
    ↓
API Error Contract
```

Rules:

* Domain does not know HTTP Status Codes.
* Domain does not know API Response structures.
* Domain does not contain `HttpStatus`, HTTP Headers, REST Response, JSON Response, or Controller information.
* Presentation owns the API Error Contract.
* Application only creates a separate Application Error when the Use Case requires an independent Contract.
* Do not create one Application Exception for every Domain Exception.

Example API mapping:

```json
{
  "code": "ACCOUNT.INSUFFICIENT_BALANCE",
  "message": "Insufficient balance",
  "traceId": "..."
}
```

The exact API Error Contract is defined by the dedicated API/Error Response strategy.

## Logging & Monitoring

* Domain Exceptions must not perform Logging.
* Domain only expresses the Error.
* Logging belongs to the appropriate upper layer and must use operational Context.
* Business Errors are usually expected failures and must not automatically be logged at `ERROR`.
* Severity is determined outside Domain.

```text
Expected Business Failure
└── INFO / WARN / no log

Unexpected Technical Failure
└── ERROR
```

Exact Observability/Logging policy belongs to the dedicated Observability ADR.

## Retry & Recovery

* Retry must never be implemented in Domain.
* Domain must know nothing about:

  * Retry
  * Backoff
  * Timeout
  * Circuit Breaker
* Retry/Recovery belongs to Application/Infrastructure.
* Retry decisions must consider Failure Type and Idempotency.

## Transaction Rules

* Domain Error must not cause unnecessary large Transactions.
* When a Domain Error occurs, the Use Case must stop or Rollback according to Transaction rules.
* Transaction details belong to the dedicated Transaction Strategy ADR.
* Domain Error must not be used as a reason to create unnecessary cross-Aggregate Transactions.

## Domain Exception Payload

A Domain Exception may carry Domain-oriented information required to describe the Business Error.

```java
public final class InsufficientBalanceException
        extends DomainException {

    private final Money balance;
    private final Money requestedAmount;

    public InsufficientBalanceException(
            Money balance,
            Money requestedAmount) {
        super(
            AccountErrorCode.INSUFFICIENT_BALANCE,
            "Insufficient balance"
        );
        this.balance = balance;
        this.requestedAmount = requestedAmount;
    }
}
```

Payload must remain Domain-oriented and must not contain Transport or Infrastructure details.

## Forbidden Dependencies

Domain Exceptions must not depend on:

```text
Spring
HTTP
REST
Database
JPA
Hibernate
Message Broker
Transport
Logging Framework
```

Forbidden example:

```java
public class InsufficientBalanceException {
    private HttpStatus status;
    private String responseBody;
}
```

## Error Flows

### Domain Error

```text
Domain
(Entity / Value Object / Aggregate / Domain Service)
        ↓
Domain Exception
        ↓
Application
(Propagate / Handle / Map / Retry / Compensation)
        ↓
Presentation
(Exception Handler)
        ↓
API Error Contract
```

### Technical Error

```text
Infrastructure
(DB / Network / Messaging / Serialization)
        ↓
Technical Exception
        ↓
Application
(Retry / Recovery / Mapping)
        ↓
Presentation
        ↓
API Error Contract
```

## Agent Instructions

When analyzing or implementing Domain Error handling:

1. Classify every failure as **Domain/Business** or **Technical/Infrastructure**.
2. Locate the owner of the Business Rule before deciding where the Error belongs.
3. Entity owns Entity Invariants.
4. Aggregate Root owns Aggregate Invariants.
5. Value Object owns Value Invariants.
6. Domain Service owns Domain Rules without a natural Entity/Value Object owner.
7. Use a concrete Domain Exception with explicit Business Meaning for Domain Business Failures by default.
8. Use a stable, type-safe Domain Error Code independent of Message and HTTP.
9. Never place HTTP, REST, Spring, JPA, Hibernate, Database, Broker, or Transport concerns in Domain Exceptions.
10. Keep Structural/Input Validation separate from Business Validation.
11. Do not move Domain Rules into Application merely for convenience of Error Handling.
12. Do not create an Application Exception for every Domain Exception.
13. Technical Exceptions belong to Infrastructure/Application and must not be disguised as Domain Errors.
14. Retry, Backoff, Timeout, Circuit Breaker, Recovery, and Compensation are not Domain responsibilities.
15. Domain Exceptions must not perform Logging.
16. Presentation translates Domain/Application Errors into the API Error Contract.
17. Client decisions must use Error Codes, never Message text.
18. Avoid deep Exception hierarchies and unnecessary Exception types.
19. Preserve Business Meaning and Ubiquitous Language in Error names.
20. When reviewing a Domain class, determine whether the class owns the violated Rule before assigning Exception ownership.
