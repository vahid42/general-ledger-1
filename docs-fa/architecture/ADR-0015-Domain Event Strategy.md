# ADR-0015 — استراتژی Domain Event (Domain Event Strategy)

- **وضعیت:** پذیرفته شده (Accepted)
- **تاریخ:** 2026-08-20

## ADRهای مرتبط

- ADR-0011 
- ADR-0012 
- ADR-0013 
- ADR-0014 
- ADR-0018 

---

# 1. زمینه (Context)

در Domain-Driven Design، برخی رخدادها دارای Business Meaning هستند و نشان‌دهنده وقوع یک اتفاق مهم در Domain می‌باشند.

نمونه:

- حساب افتتاح شد.
- حساب مسدود شد.
- سند ثبت شد.
- پرداخت تکمیل شد.
- انتقال انجام شد.
- وضعیت حساب تغییر کرد.

این رخدادها ممکن است برای سایر بخش‌های Domain، Application یا Moduleهای دیگر اهمیت داشته باشند.

هدف این ADR تعیین نحوه:

- تعریف Domain Event
- ایجاد Domain Event
- نگهداری Domain Event
- انتشار Event
- تفکیک Domain Event از Integration Event
- مدیریت Event بین Aggregateها و Moduleها
- مدیریت Reliability و Idempotency

است.

---

# 2. تصمیم (Decision)

## 2.1. Domain Event چیست؟

Domain Event رخدادی است که **در گذشته در Domain اتفاق افتاده است** و دارای Business Meaning می‌باشد.

بنابراین نام Event باید معمولاً به صورت Past Tense باشد.

مناسب:

```text
AccountOpened
AccountBlocked
JournalEntryPosted
PaymentCompleted
TransferCompleted
````

نامناسب:

```text
OpenAccount
BlockAccount
PostJournalEntry
CompletePayment
TransferMoney
```

موارد دوم Command یا Operation هستند، نه Event.

---

# 2.2. Domain Event بخشی از Domain Model است

Domain Event یک مفهوم Domain است و باید در Domain Layer تعریف شود.

ساختار مفهومی:

```text
Domain
│
├── Entity
├── Value Object
├── Aggregate
├── Domain Service
└── Domain Event
```

Domain Event نباید به تکنولوژی‌های Infrastructure وابسته باشد.

Domain نباید چیزی درباره موارد زیر بداند:

```text
Kafka
RabbitMQ
ActiveMQ
JMS
Spring Events
HTTP
Message Broker
```

---

# 2.3. Domain Event باید Immutable باشد

Domain Event پس از ایجاد نباید تغییر کند.

در Java، برای Eventهای ساده استفاده از `record` ترجیح داده می‌شود.

مثال:

```java
public record AccountOpened(
        AccountId accountId,
        Instant occurredAt
) implements DomainEvent {
}
```

Event یک Snapshot از رخدادی است که اتفاق افتاده است.

---

# 2.4. Domain Event باید Business Meaning داشته باشد

هر تغییر داخلی Entity نباید باعث ایجاد Domain Event شود.

نامناسب:

```text
FieldUpdated
StatusSetterCalled
EntityModified
BalanceChangedInternally
```

مناسب:

```text
AccountOpened
AccountBlocked
AccountClosed
JournalEntryPosted
PaymentCompleted
TransferCompleted
```

Event باید یک اتفاق معنادار از دید Business را نمایش دهد.

---

# 2.5. Domain Event توسط Domain ایجاد می‌شود

وقتی یک Business Operation معتبر در Domain انجام می‌شود، Aggregate یا Domain Model می‌تواند Domain Event ایجاد کند.

مثال:

```java
public class Account {

    private AccountId id;
    private AccountStatus status;

    private final List<DomainEvent> domainEvents =
            new ArrayList<>();

    public void block() {

        if (status == AccountStatus.BLOCKED) {
            throw new IllegalStateException(
                    "Account is already blocked"
            );
        }

        status = AccountStatus.BLOCKED;

        domainEvents.add(
                new AccountBlocked(
                        id,
                        Instant.now()
                )
        );
    }
}
```

نکته مهم:

```text
Domain
    │
    └── Creates Event
```

اما:

```text
Domain
    └── Publish Event ❌
```

انتشار Event مسئولیت Domain نیست.

---

# 2.6. Domain Event و Aggregate

Domain Event معمولاً نتیجه یک Business Operation در Aggregate است.

به صورت مفهومی:

```text
Application
     │
     ▼
Aggregate Root
     │
     ├── Validate Invariant
     ├── Change State
     └── Raise Domain Event
```

بنابراین Event باید نتیجه یک تغییر معتبر Domain باشد، نه یک جایگزین برای Business Behavior.

---

# 2.7. Aggregate Root Identity در Domain Event

مطابق استراتژی Identity پروژه، Identity مربوط به Aggregate Root باید به صورت Value Object مدل شود.

بنابراین Domain Event نیز باید از همان Type استفاده کند.

مثال:

```java
public record AccountId(UUID value) {
}
```

و:

```java
public record AccountOpened(
        AccountId accountId,
        Instant occurredAt
) implements DomainEvent {
}
```

نباید صرفاً برای راحتی Event، Identity را به Primitive تبدیل کنیم:

```java
public record AccountOpened(
        UUID accountId
) {
}
```

مگر اینکه در لایه Integration Mapping انجام شود.

Domain Event باید Domain Model را منعکس کند.

---

# 2.8. ساختار پایه Domain Event

برای Eventهای Domain می‌توان یک Abstraction ساده تعریف کرد.

مثلاً:

```java
public interface DomainEvent {

    Instant occurredAt();
}
```

در صورت نیاز به Event Identity:

```java
public interface DomainEvent {

    EventId eventId();

    Instant occurredAt();
}
```

ساختار دقیق `EventId` و Identity در صورت نیاز مطابق ADR-0018 یا تصمیمات مرتبط پیاده‌سازی خواهد شد.

---

# 2.9. Event نباید صرفاً برای Logging ایجاد شود

Domain Event نباید جایگزین Log باشد.

نامناسب:

```text
AccountMethodCalled
AccountUpdated
FieldChanged
```

برای Logging باید از Logging Infrastructure استفاده شود.

Domain Event زمانی ایجاد می‌شود که رخداد دارای Business Meaning باشد.

---

# 2.10. Domain Event و Integration Event دو مفهوم متفاوت هستند

این دو مفهوم باید از یکدیگر تفکیک شوند.

### Domain Event

رخداد مهم داخل Domain یا Bounded Context است.

### Integration Event

پیامی است که برای ارتباط با Module یا سیستم دیگری منتشر می‌شود.

معماری:

```text
Aggregate
    │
    ▼
Domain Event
    │
    ▼
Application / Integration Boundary
    │
    ▼
Integration Event
    │
    ▼
Other Module / External System
```

هر Domain Event الزاماً نباید Integration Event شود.

---

# 2.11. Domain Event نباید مستقیماً به خارج Publish شود

Domain Layer نباید مسئول ارسال Event به:

```text
Kafka
RabbitMQ
HTTP
JMS
Database Queue
```

باشد.

Domain فقط Event را ایجاد می‌کند.

انتشار Event توسط Application یا Infrastructure انجام می‌شود.

---

# 2.12. Domain Event در Modular Monolith

در Modular Monolith، Domain Event می‌تواند برای کاهش Coupling بین Moduleها استفاده شود.

مثلاً:

```text
Account Module
       │
       ▼
AccountOpened
       │
       ▼
Application Event Handler
       │
       ├──► Reporting Module
       └──► Notification Module
```

اما Event نباید به صورت خودکار برای هر ارتباط بین Moduleها استفاده شود.

اگر یک Use Case نیازمند ارتباط مستقیم و synchronous باشد، Application Layer می‌تواند از Port مربوطه استفاده کند.

Event زمانی استفاده می‌شود که:

* رخداد Business مستقل باشد.
* Consumerها نباید به Producer وابسته باشند.
* Eventual Consistency قابل قبول باشد.
* نیاز به چند Consumer وجود داشته باشد.

---

# 2.13. Domain Event و Transaction Boundary

Domain Event معمولاً در همان Transactionای ایجاد می‌شود که تغییر Aggregate در آن اتفاق افتاده است.

مثلاً:

```text
Transaction
    │
    ├── Aggregate State Change
    │
    └── Domain Event Creation
```

ایجاد Event به معنی ایجاد یک Transaction جدید نیست.

در حالت عادی، تغییر Aggregate و ثبت Event باید Atomic باشند.

---

# 2.14. انتشار Event بعد از Commit

برای Eventهایی که فقط درون همان Process مورد استفاده قرار می‌گیرند، Dispatch می‌تواند بر اساس نیاز Application انجام شود.

اما برای Eventهایی که باید از مرز Transaction خارج شوند، انتشار مستقیم بعد از Commit قابل اعتماد نیست.

الگوی خطرناک:

```text
DB Transaction
      │
      ▼
   COMMIT
      │
      ▼
Publish Event
      │
      X
   Failure
```

در این حالت Database تغییر کرده ولی Event منتشر نشده است.

---

# 2.15. Transactional Outbox

برای Eventهایی که انتشار قابل اعتماد آن‌ها ضروری است، استفاده از Transactional Outbox ترجیح داده می‌شود.

```text
Application Transaction
        │
        ├──► Aggregate State
        │
        └──► Outbox Event
                  │
                  ▼
                COMMIT
                  │
                  ▼
            Outbox Publisher
                  │
                  ▼
            Message Broker
```

در این روش تغییر Domain State و ثبت Outbox Message در یک Transaction انجام می‌شوند.

سپس Publisher مستقل Outbox را پردازش می‌کند.

Transactional Outbox برای تمام Domain Eventها اجباری نیست؛ فقط زمانی استفاده می‌شود که Event باید به صورت قابل اعتماد از مرز Transaction یا سیستم خارج شود.

---

# 2.16. Event Idempotency

Consumerها باید تا حد امکان Idempotent باشند.

ممکن است یک Event بیش از یک بار دریافت شود.

مثلاً:

```text
PaymentCompleted
       │
       ├──► Consumer
       │
       └──► Consumer دوباره
```

Consumer نباید با دریافت مجدد Event، عملیات Business را به شکل نادرست تکرار کند.

در صورت نیاز می‌توان از موارد زیر استفاده کرد:

```text
EventId
ProcessedEvent
Inbox Pattern
Business Idempotency Key
```

---

# 2.17. Event Metadata

Domain Event می‌تواند دارای Metadata استاندارد باشد.

اطلاعات متداول:

```text
EventId
OccurredAt
AggregateId
AggregateType
EventVersion
CorrelationId
CausationId
```

اما Domain Event نباید با Metadataهای Infrastructure مانند:

```text
Kafka Partition
Kafka Offset
HTTP Header
Broker Metadata
```

آلوده شود.

این اطلاعات متعلق به Integration Layer هستند.

---

# 2.18. Event Versioning

Eventهایی که از مرز Domain یا Module خارج می‌شوند باید قابلیت Evolution داشته باشند.

تغییر Schema Event نباید بدون در نظر گرفتن Consumerهای قبلی انجام شود.

روش‌های ممکن:

```text
Event Version
Schema Evolution
Backward Compatible Changes
New Event Type
```

مثلاً:

```text
PaymentCompleted v1
PaymentCompleted v2
```

Versioning دقیق Integration Eventها می‌تواند در لایه Integration تعریف شود.

---

# 2.19. Event Ordering

نباید فرض شود Eventها همیشه در تمام شرایط به ترتیب ایجاد دریافت می‌شوند.

اگر ترتیب Eventها دارای Business Meaning است، باید این موضوع به صورت صریح طراحی شود.

مثلاً:

```text
AccountOpened
      ↓
AccountActivated
      ↓
AccountClosed
```

در صورت نیاز به Ordering باید بر اساس Aggregate یا Business Key طراحی شود.

---

# 2.20. Event Store و Event Sourcing

Domain Event به معنی Event Sourcing نیست.

در معماری فعلی:

```text
Domain Event
      ≠
Event Sourcing
```

Domain Event می‌تواند صرفاً برای:

* کاهش Coupling
* اطلاع‌رسانی Business Event
* Trigger کردن Processها
* Integration

استفاده شود.

Event Sourcing در این پروژه به صورت پیش‌فرض استفاده نمی‌شود و در صورت نیاز باید در ADR مستقل تصمیم‌گیری شود.

---

# 2.21. Domain Event و Persistence

Domain Event نباید صرفاً به دلیل نیاز Persistence طراحی شود.

اگر Event برای Reliability نیاز به Outbox داشته باشد:

```text
Domain Event
      │
      ▼
Application Mapping
      │
      ▼
Outbox Message
```

Domain Model نباید مستقیماً با جدول Outbox یا ORM Entity مرتبط شود.

---

# 2.22. Domain Event و Domain Service

Domain Service نیز در صورت انجام یک Business Operation می‌تواند باعث ایجاد Domain Event شود، اما مالک Event باید همچنان Business Model باشد.

مثلاً:

```text
TransferService
      │
      ├── source.withdraw()
      ├── destination.deposit()
      │
      └── TransferCompleted
```

اما Domain Service نباید تبدیل به Event Publisher شود.

```text
Domain Service
      │
      └── KafkaProducer ❌
```

---

# 2.23. چه زمانی Domain Event ایجاد کنیم؟

قبل از ایجاد Event این موارد باید بررسی شوند:

1. آیا یک Business Event واقعی اتفاق افتاده است؟
2. آیا Event برای Domain معنی دارد؟
3. آیا Consumer دیگری واقعاً به این رخداد نیاز دارد؟
4. آیا این Event بخشی از Business Language است؟
5. آیا Event صرفاً برای Logging یا Technical Notification ایجاد نشده است؟

اگر پاسخ منفی است، ایجاد Domain Event ضروری نیست.

---

# قوانین (Rules)

* Domain Event باید یک Business Event واقعی را نمایش دهد.
* Domain Event باید Immutable باشد.
* نام Event باید بیانگر اتفاقی باشد که رخ داده است.
* Domain Event در Domain Layer تعریف می‌شود.
* Domain Event توسط Domain Model ایجاد می‌شود.
* Domain نباید Event را Publish کند.
* Domain نباید به Message Broker وابسته باشد.
* Domain Event و Integration Event باید از یکدیگر تفکیک شوند.
* هر Domain Event الزاماً Integration Event نیست.
* Aggregate Root Identity در Domain Event باید از Identity Type مربوط به Domain استفاده کند.
* Event نباید صرفاً برای Logging یا تغییرات فنی ایجاد شود.
* Consumerهای Integration Event باید تا حد امکان Idempotent باشند.
* Eventهایی که از مرز Transaction خارج می‌شوند باید در صورت نیاز با Outbox قابل اعتماد شوند.
* Transactional Outbox برای تمام Domain Eventها اجباری نیست.
* Event Versioning برای Eventهای Integration باید در نظر گرفته شود.
* Event Ordering نباید بدون طراحی صریح فرض شود.
* Domain Event به معنی Event Sourcing نیست.
* Event Sourcing در معماری فعلی به صورت پیش‌فرض استفاده نمی‌شود.
* Domain Event نباید برای دور زدن Aggregate Boundary استفاده شود.

---

# Domain Event Decision Checklist

قبل از ایجاد Domain Event:

```text
آیا یک Business Event واقعی رخ داده است؟
        │
       No ──► Domain Event ایجاد نکن
        │
       Yes
        │
        ▼
آیا Event دارای Business Meaning است؟
        │
       No ──► Domain Event ایجاد نکن
        │
       Yes
        │
        ▼
آیا Consumer دیگری به این رخداد نیاز دارد؟
        │
       No ──► احتمالاً Event لازم نیست
        │
       Yes
        │
        ▼
آیا Event فقط داخل Domain مصرف می‌شود؟
        │
       Yes ──► Domain Event
        │
       No
        │
        ▼
آیا باید از مرز Module/System خارج شود؟
        │
       Yes
        │
        ▼
Domain Event
        │
        ▼
Integration Mapping
        │
        ▼
Integration Event
        │
        ▼
Outbox / Messaging
```

---

# پیامدها (Consequences)

## مزایا

* کاهش Coupling بین Moduleها
* مدل‌سازی صریح Business Events
* افزایش استقلال Aggregateها
* امکان Eventual Consistency
* مناسب برای Modular Monolith
* امکان Integration با سیستم‌های خارجی
* امکان استفاده از Transactional Outbox
* افزایش قابلیت Trace کردن رخدادهای Business
* جلوگیری از وابستگی Domain به Messaging Infrastructure

## معایب

* افزایش پیچیدگی Flow سیستم
* دشوارتر شدن Debugging
* نیاز به Idempotency
* نیاز به مدیریت Event Versioning
* احتمال Eventual Consistency
* نیاز به Outbox یا Message Broker برای Integrationهای مهم
* احتمال استفاده بیش از حد از Event در صورت طراحی نادرست

---

# وضعیت اجرا (Implementation Status)

این استراتژی برای تمام Moduleهای Domain اعمال می‌شود.

Domain Event:

* در Domain Layer تعریف می‌شود.
* توسط Aggregate یا Domain Model ایجاد می‌شود.
* Immutable است.
* Business Meaning دارد.
* به Infrastructure وابسته نیست.

انتشار Event مسئولیت Domain نیست.

در صورت نیاز به خروج Event از مرز Transaction یا Module:

```text
Domain Event
      │
      ▼
Application
      │
      ▼
Integration Event
      │
      ▼
Transactional Outbox
      │
      ▼
Message Broker / Consumer
```

Transactional Outbox فقط برای Eventهایی استفاده می‌شود که Reliability در انتشار آن‌ها مورد نیاز است.

Event Sourcing در معماری فعلی استفاده نمی‌شود و نیازمند ADR مستقل است.

---

# References

1. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**
2. Vaughn Vernon — **Implementing Domain-Driven Design**
3. Vaughn Vernon — **Domain-Driven Design Distilled**
4. Martin Fowler — **What do you mean by “Event-Driven”?**
5. Martin Fowler — **Event Sourcing**
6. Martin Fowler — **Transactional Outbox**
7. Chris Richardson — **Microservices Patterns**
8. Greg Young — **CQRS and Event Sourcing**
9. Enterprise Integration Patterns — **Event Message**
10. Enterprise Integration Patterns — **Guaranteed Delivery**

````

### یک نکته مهم برای پروژه خودمان

با توجه به ساختاری که تا الان برای **General Ledger** تعیین کردیم، من Domain Event را فعلاً **در حد نیاز واقعی نگه می‌دارم، نه اینکه برای هر تغییر Entity Event بسازیم**.

مثلاً برای `AccountHeading` اگر فقط این کار را داریم:

```java
accountHeading.changeName(...)
````

لازم نیست حتماً:

```text
AccountHeadingNameChanged
```

ایجاد کنیم.

اما اگر مثلاً:

```text
JournalEntryPosted
```

اتفاقی است که `Reporting`، `Account Balance` یا Module دیگری واقعاً به آن واکنش نشان می‌دهد، آنجا Domain Event کاملاً معنی‌دار است.

و مهم‌تر اینکه **`AccountId`، `AccountHeadingId`، `JournalEntryId` و سایر IDهای Aggregate Root در Eventها هم Value Object باقی می‌مانند**؛ این با ADR-0013 و ADR-0018 هماهنگ است.
