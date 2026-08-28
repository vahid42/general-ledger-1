# ADR-0015 — Domain Event Strategy  

## Purpose

Define how Domain Events are modeled, created, stored, dispatched, separated from Integration Events, and handled across Aggregate/Module boundaries with reliable delivery and idempotency.

## Core Rules

* A Domain Event represents a **past Business event** with Business Meaning.
* Event names must use **Past Tense**: `AccountOpened`, `AccountBlocked`, `JournalEntryPosted`, `PaymentCompleted`.
* Commands/Operations such as `OpenAccount` or `PostJournalEntry` are not Events.
* Domain Events are part of the **Domain Model** and are defined in the `Domain` Layer.
* Domain Events must be **Immutable**; Java `record` is preferred for simple Events.
* An Event must represent a meaningful Business occurrence, not an internal Entity change or technical operation.
* Do not create Events merely for logging, setters, field changes, or persistence.
* Domain creates/raises Events; Domain **does not publish** them.
* Domain must not depend on Kafka, RabbitMQ, ActiveMQ, JMS, Spring Events, HTTP, Message Brokers, or other Infrastructure messaging technologies.
* Aggregate Root Identity inside a Domain Event must use the Domain's Identity Value Object (`AccountId`, `JournalEntryId`, etc.), not primitives. Primitive conversion belongs to Integration Mapping when required.
* Domain Events must not bypass Aggregate Boundaries.

## Domain Event Model

```java
public interface DomainEvent {
    Instant occurredAt();
}
```

If Event Identity is required:

```java
public interface DomainEvent {
    EventId eventId();
    Instant occurredAt();
}
```

Exact `EventId` design follows related identity decisions (e.g. ADR-0018).

Example:

```java
public record AccountOpened(
    AccountId accountId,
    Instant occurredAt
) implements DomainEvent {}
```

## Aggregate Rules

A Domain Event normally results from a valid Business Operation:

```text
Application
    ↓
Aggregate Root
    ├── Validate Invariants
    ├── Change State
    └── Raise Domain Event
```

The Event is a consequence of valid Domain behavior, not a replacement for that behavior.

A Domain Service may also cause a Domain Event when performing a Business Operation:

```text
TransferService
    ├── source.withdraw()
    ├── destination.deposit()
    └── TransferCompleted
```

A Domain Service must never become an Event Publisher or depend on Messaging Infrastructure.

## Domain Event vs Integration Event

These are separate concepts:

```text
Aggregate
    ↓
Domain Event
    ↓
Application / Integration Boundary
    ↓
Integration Event
    ↓
Other Module / External System
```

Rules:

* Every Domain Event is **not necessarily** an Integration Event.
* Domain Events describe Business occurrences within the Domain/Bounded Context.
* Integration Events are messages intended to cross a Module/System boundary.
* Mapping from Domain Event to Integration Event belongs outside the Domain Model.
* Infrastructure metadata such as Kafka partition/offset, HTTP headers, or broker metadata must not pollute Domain Events.

## Modular Monolith Rules

Domain Events may reduce Coupling between Modules:

```text
Account Module
    ↓
AccountOpened
    ↓
Application Event Handler
    ├── Reporting Module
    └── Notification Module
```

Do not use Events automatically for every Module interaction.

Use synchronous Application Ports when a Use Case requires direct synchronous communication.

Prefer Events when:

* The Business occurrence is independently meaningful.
* Producer and Consumers should remain decoupled.
* Eventual Consistency is acceptable.
* Multiple Consumers may react independently.

## Transaction Rules

Domain Event creation normally occurs within the same Transaction as the Aggregate state change:

```text
Transaction
    ├── Aggregate State Change
    └── Domain Event Creation
```

State change and Event persistence/registration should be Atomic when reliability requires it.

Creating a Domain Event does not create a separate Transaction.

## Reliable Publication

Do not rely on direct publication after DB commit:

```text
DB Transaction
    ↓
COMMIT
    ↓
Publish Event
    ↓
Failure
```

This can leave Database state committed while Event publication fails.

For Events that must reliably cross a Transaction/Module/System boundary, prefer **Transactional Outbox**:

```text
Application Transaction
    ├── Aggregate State
    └── Outbox Event
            ↓
          COMMIT
            ↓
     Outbox Publisher
            ↓
      Message Broker
```

Rules:

* Aggregate state and Outbox Message are persisted atomically.
* Outbox Publisher processes committed Outbox Messages independently.
* Transactional Outbox is **not mandatory for every Domain Event**.
* Use it when reliable delivery beyond the Transaction boundary is required.
* Domain Model must not directly depend on an Outbox table or ORM Entity.

## Idempotency

Integration Event Consumers should be **Idempotent** because duplicate delivery is possible.

Possible mechanisms:

* `EventId`
* `ProcessedEvent`
* Inbox Pattern
* Business Idempotency Key

A duplicate Event must not incorrectly repeat Business operations.

## Event Metadata

Domain Events may contain domain-relevant metadata:

* `EventId`
* `OccurredAt`
* `AggregateId`
* `AggregateType`
* `EventVersion`
* `CorrelationId`
* `CausationId`

Do not include Infrastructure-specific metadata such as:

* Kafka Partition
* Kafka Offset
* HTTP Header
* Broker Metadata

## Event Versioning

Events crossing Domain/Module boundaries must support evolution.

Rules:

* Do not change Event Schema without considering existing Consumers.
* Prefer backward-compatible Schema Evolution where possible.
* Use Event Versioning or introduce a new Event Type when necessary.
* Integration Event versioning may be defined at the Integration Layer.

Example:

```text
PaymentCompleted v1
PaymentCompleted v2
```

## Event Ordering

Never assume Events are globally delivered in creation order.

If ordering has Business Meaning, it must be explicitly designed.

Example:

```text
AccountOpened
    ↓
AccountActivated
    ↓
AccountClosed
```

When required, ordering should be designed around an Aggregate or Business Key.

## Event Sourcing

```text
Domain Event ≠ Event Sourcing
```

Domain Events may be used for:

* Decoupling
* Business notifications
* Process triggering
* Integration

**Event Sourcing is not used by default** and requires a separate ADR.

## Domain Event Creation Decision

Create a Domain Event only when:

1. A real Business Event occurred.
2. It has Business Meaning.
3. It belongs to the Business Language/Domain Model.
4. Another Domain/Application/Module Consumer has a meaningful reason to react, where applicable.
5. It is not merely logging, persistence, or a technical state change.

Decision flow:

```text
Real Business Event?
    ├── No → No Domain Event
    └── Yes
         ↓
Business Meaning?
    ├── No → No Domain Event
    └── Yes
         ↓
Meaningful Consumer?
    ├── No → Usually no Event
    └── Yes
         ↓
Stays inside Domain?
    ├── Yes → Domain Event
    └── No
         ↓
Domain Event
    ↓
Integration Mapping
    ↓
Integration Event
    ↓
Outbox / Messaging
```

## General Ledger Guidance

Do not create a Domain Event for every Entity change.

Example:

```text
accountHeading.changeName(...)
```

does not automatically require:

```text
AccountHeadingNameChanged
```

Create an Event when the occurrence is a meaningful Business Event with real Consumers.

Example:

```text
JournalEntryPosted
```

is appropriate when `Reporting`, `Account Balance`, or another Module needs to react to the posting.

Aggregate Root IDs such as `AccountId`, `AccountHeadingId`, and `JournalEntryId` remain their Domain Value Object types inside Domain Events.

## Constraints

* Domain Event must be a real Business Event.
* Domain Event must be Immutable.
* Event names must describe completed occurrences.
* Domain Events belong to the Domain Layer.
* Domain Model creates/raises Events.
* Domain never publishes Events.
* Domain must not depend on Messaging Infrastructure.
* Domain Event and Integration Event must remain separate.
* Not every Domain Event becomes an Integration Event.
* Aggregate Root Identity must use Domain Identity Types.
* Events must not be created merely for logging or technical changes.
* Integration Consumers should be Idempotent.
* Reliable cross-boundary publication should use Transactional Outbox when required.
* Transactional Outbox is not mandatory for every Event.
* Integration Events require Schema Evolution/versioning consideration.
* Event Ordering must be explicitly designed when Business-significant.
* Domain Event does not imply Event Sourcing.
* Event Sourcing is not enabled by default.
* Domain Events must not bypass Aggregate Boundaries.

## Agent Instructions

When designing or reviewing a Domain Event:

* Verify that it represents a **meaningful Business occurrence**, not an implementation detail.
* Use Past-Tense naming.
* Keep the Event Immutable.
* Define it in `Domain`.
* Use Aggregate Root Identity Value Objects.
* Never place Broker/HTTP/Infrastructure concerns inside the Domain Event.
* Let Domain create the Event; never let Domain publish it.
* Distinguish Domain Event from Integration Event.
* Do not introduce Events merely to decouple code when synchronous Application communication is the correct model.
* If an Event crosses a Transaction boundary and reliable delivery matters, use Transactional Outbox.
* Assume duplicate Integration Event delivery and design Consumers for Idempotency.
* Do not assume Event ordering without an explicit Business requirement and design.
* Do not introduce Event Sourcing unless a separate architectural decision explicitly approves it.
* In General Ledger, prefer meaningful Events such as `JournalEntryPosted` over Events for routine Entity field changes.
