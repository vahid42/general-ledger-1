# ADR-0004 — جهت وابستگی و ارتباط داخلی در General Ledger Microservice

* **Status:** Accepted
* **Date:** 2026-08-22
* **Related ADRs:**

  * ADR-0003
  * ADR-0005

---

# 1. Context

مطابق ADR-0003، `General Ledger` در سطح سیستم یک **Microservice مستقل** است.

ساختار کلی:

```text
System
   │
   └── Microservices Architecture
            │
            └── General Ledger Microservice
```

`General Ledger Microservice` دارای:

* Build مستقل
* Test مستقل
* Runtime مستقل
* Deployment مستقل
* Versioning مستقل
* قابلیت Scale مستقل

است.

در داخل این Microservice، یک `General Ledger Bounded Context` وجود دارد که شامل سه Subdomain اصلی است:

```text
General Ledger Bounded Context

├── Account Head
├── Account
└── Journal Entry ⭐ Core
```

این Subdomainها Microservice مستقل نیستند و همگی در یک Runtime اجرا می‌شوند.

در سطح کد، این Subdomainها به‌صورت Business/Domain-oriented Module سازمان‌دهی می‌شوند:

```text
General Ledger Microservice

├── Account Head Module
├── Account Module
└── Journal Entry Module
```

بنابراین باید بین مفاهیم زیر تمایز حفظ شود:

```text
Subdomain
    ≠
Bounded Context
    ≠
Module
    ≠
Microservice
```

---

# 2. هدف این ADR

هدف این ADR تعیین اصول مربوط به:

* Dependency Direction
* Internal Communication
* Application Orchestration
* Internal Contract / Port
* Domain Service
* Domain Event
* Repository Access
* Aggregate Interaction
* Module Boundaries
* Coupling Control

در داخل `General Ledger Microservice` است.

این ADR درباره ارتباط بین Microserviceهای مستقل نیست.

برای مثال:

```text
Another Microservice
        │
        ▼
General Ledger Microservice
```

یک **Integration Communication** محسوب می‌شود.

اما:

```text
Account
   │
   ▼
Account Head
```

در صورت وجود Business Dependency، یک **Internal Communication** داخل همان Microservice است.

---

# 3. Architectural Decision

ارتباط بین Moduleهای داخلی General Ledger مجاز است، اما این ارتباط باید از طریق Boundary و Contract مشخص انجام شود.

اصل کلی:

```text
Presentation
      │
      ▼
Application
      │
      ├── Internal Contract / Port
      │
      └── Domain
             │
             └── Business Rules
```

بنابراین:

> **Application Layer محل اصلی Orchestration Use Case است.**

و:

> **Domain Layer مسئول Business Rule و Business Decision است.**

و:

> **Infrastructure مسئول Implementation جزئیات تکنیکی و Persistence است.**

---

# 4. Dependency Direction

جهت وابستگی باید از جزئیات تکنیکی به سمت Abstraction و Domain باشد.

مدل کلی:

```text
Presentation
      │
      ▼
Application
      │
      ▼
Domain
      ▲
      │
Infrastructure
```

وابستگی‌های مجاز:

```text
Presentation → Application
Presentation → Domain
Application → Domain
Infrastructure → Application
Infrastructure → Domain
```

وابستگی‌های غیرمجاز:

```text
Domain → Infrastructure
Domain → Presentation
Application → Presentation
Presentation → Infrastructure
```

اصل کلیدی:

> **Domain نباید به جزئیات تکنیکی Infrastructure وابسته باشد.**

---

# 5. Application Layer به‌عنوان Orchestrator

وقتی یک Use Case نیازمند همکاری چند Business Boundary یا Aggregate باشد، Application Layer مسئول هماهنگی عملیات است.

برای مثال:

```text
CreateAccountUseCase

      │
      ├── Load Account Head
      │
      ├── Validate required data
      │
      ├── Create Account
      │
      └── Save Account
```

در این مدل:

```text
Application
    =
Use Case Orchestration
```

در حالی که:

```text
Domain
    =
Business Decision
```

Application Layer نباید مالک Business Invariantهایی باشد که متعلق به Domain هستند.

---

# 6. Internal Contract / Port

ارتباط داخلی بین Moduleها در صورت نیاز باید از طریق یک Contract یا Port مشخص انجام شود.

برای مثال:

```text
CreateAccountUseCase
        │
        ▼
AccountHeadPort
        │
        ▼
Account Head Capability
```

به‌جای:

```text
CreateAccountUseCase
        │
        X
        ▼
AccountHeadingJpaRepository
```

هدف این است که Consumer به Implementation داخلی Provider وابسته نشود.

اصل:

> **Implementation داخلی هر Module باید تا حد امکان از Consumerهای آن Module مخفی بماند.**

---

# 7. Internal Contract / Port ≠ Repository Port

Repository تنها یکی از انواع Port است.

دو مفهوم باید از یکدیگر جدا باقی بمانند:

```text
Repository Port
    │
    └── Persistence Abstraction
```

و:

```text
Internal Contract / Port
    │
    └── Business Capability / Communication Abstraction
```

برای مثال:

```text
AccountRepository
```

یک Persistence Port است.

در حالی که:

```text
AccountHeadPort
```

می‌تواند یک Capability Contract برای استفاده یک Use Case از قابلیت Account Head باشد.

بنابراین:

> هر Interface داخل Domain یا Application الزاماً Repository نیست.

---

# 8. Repository Port

Repository Port وظیفه تعریف Abstraction مربوط به Persistence را دارد.

نمونه:

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

ساختار:

```text
Application / Domain
        │
        ▼
Repository Port
        │
        ▼
Infrastructure
        │
        ▼
Database
```

Implementation مربوط به Persistence در Infrastructure قرار می‌گیرد.

برای مثال:

```text
Domain

   └── AccountRepository
            ▲
            │ implements
            │
Infrastructure

   └── JpaAccountRepository
```

Aggregate نباید به Implementation مربوط به Persistence وابسته باشد.

---

# 9. Aggregate نباید Repository را صدا بزند

Aggregate نباید مستقیماً Repository را فراخوانی کند.

طراحی زیر نامطلوب است:

```text
JournalEntry
      │
      ▼
AccountRepository
```

یا:

```text
Account
      │
      ▼
AccountHeadRepository
```

اگر یک Use Case نیاز به داده‌ای از Aggregate دیگر داشته باشد، Application Layer باید آن نیاز را Orchestrate کند.

مدل:

```text
Application Use Case
        │
        ├── Load Account
        │
        ├── Load Account Head
        │
        └── Create Journal Entry
```

---

# 10. Aggregate Interaction

Aggregateها مرز Consistency و Business Invariantهای خودشان هستند.

سه Aggregate اصلی General Ledger عبارت‌اند از:

```text
AccountHead
Account
JournalEntry
```

هر Aggregate باید مسئول Invariantهای خودش باشد.

بنابراین:

```text
AccountHead
    └── Account Head Invariants

Account
    └── Account Invariants

JournalEntry
    └── Journal Entry Invariants
```

ارتباط بین Aggregateها نباید باعث شود یک Aggregate مسئول Invariantهای Aggregate دیگر شود.

---

# 11. Aggregate Reference

در صورت نیاز به ارتباط بین Aggregateها، ترجیح با استفاده از Identity/Reference است، نه نگهداری مستقیم Object Graph از Aggregate دیگر.

برای مثال:

```text
Account
    │
    └── accountHeadId
```

یا:

```text
JournalEntry
    │
    └── accountId
```

به‌جای:

```text
Account
    │
    └── AccountHeadEntity
```

هدف:

> **Aggregateها باید تا حد امکان مستقل و دارای Boundary مشخص باقی بمانند.**

---

# 12. Domain Service

Domain Service زمانی استفاده می‌شود که یک Business Rule یا Business Operation وجود داشته باشد که به‌صورت طبیعی متعلق به یک Entity، Value Object یا Aggregate مشخص نباشد.

برای مثال:

```text
AccountDomainService
        │
        └── Account-specific Business Decision
```

یا:

```text
JournalEntryDomainService
        │
        └── Journal Entry-specific Domain Decision
```

اما Domain Service نباید به Coordinator عمومی تبدیل شود.

نامطلوب:

```text
DomainService
      │
      ├── Load Account
      ├── Load Account Head
      ├── Save Account
      ├── Call Journal Entry
      └── Execute unrelated operations
```

اصل:

```text
Domain Service
      │
      └── Business Decision
```

نه:

```text
Domain Service
      │
      └── Use Case Orchestration
```

---

# 13. Domain Service و Repository

Domain Service نیز نباید صرفاً برای Orchestration به Repositoryهای بخش‌های دیگر وابسته شود.

طراحی نامطلوب:

```text
AccountDomainService
        │
        └──► AccountHeadRepository
```

اگر هدف فقط اجرای یک Use Case باشد، هماهنگی باید در Application Layer انجام شود.

مدل ترجیحی:

```text
CreateAccountUseCase
        │
        ├──► AccountHeadPort
        │
        ├──► AccountDomain
        │
        └──► AccountRepository
```

---

# 14. Business Rule در مقابل Orchestration

این تفکیک باید صریح باشد.

### Application Orchestration

```text
CreateAccountUseCase

    ├── Load Account Head
    ├── Check required capability
    ├── Create Account
    └── Save Account
```

این Orchestration است.

### Domain Business Rule

```text
Account

    ├── Account code must satisfy domain rules
    ├── Account status transitions must be valid
    └── Account invariants must hold
```

این Business Rule است.

بنابراین:

```text
Application
    =
Coordination

Domain
    =
Business Decision

Repository
    =
Persistence Abstraction

Infrastructure
    =
Technical Implementation
```

---

# 15. مثال: ایجاد Account زیر Account Head

فرض کنیم Use Case زیر وجود دارد:

> ایجاد یک Account زیر یک Account Head مشخص.

مدل پیشنهادی:

```text
Presentation
      │
      ▼
CreateAccountUseCase
      │
      ├──► AccountHeadPort
      │        │
      │        └── Validate / Retrieve required capability
      │
      ├──► Account Domain
      │        │
      │        └── Enforce Account Rules
      │
      └──► AccountRepository
               │
               └── Save Account
```

در این مدل:

### Presentation

Request را دریافت می‌کند و Use Case را فراخوانی می‌کند.

### Application

Use Case را Orchestrate می‌کند.

### Account Head

Capability موردنیاز خود را از طریق Contract ارائه می‌کند.

### Account Domain

Business Ruleهای Account را اجرا می‌کند.

### Repository

Persistence را انجام می‌دهد.

---

# 16. Business Rule بین Account و Account Head

فرض کنیم Rule این باشد:

> هر Account باید به یک Account Head معتبر تعلق داشته باشد.

این Rule باید در Domain enforce شود.

اما این Rule به‌تنهایی به معنی این نیست که Domain باید مستقیماً:

```text
AccountHeadRepository
```

را فراخوانی کند.

یک مدل مناسب:

```text
Application
      │
      ├── Validate / Load required Account Head information
      │
      ▼
Account Domain
      │
      └── Enforce Account Business Rule
```

یا:

```text
Application
      │
      ▼
AccountDomainService
      │
      └── validate(...)
```

نکته مهم:

> **داده موردنیاز برای تصمیم Domain می‌تواند توسط Application فراهم شود؛ اما خود Business Decision باید در Domain باقی بماند.**

---

# 17. Internal Communication

ارتباط داخلی بین Moduleهای General Ledger می‌تواند در دو شکل اصلی انجام شود:

```text
Internal Communication

        │
        ├── Synchronous
        │       │
        │       └── Internal Contract / Port
        │
        └── Asynchronous
                │
                └── Internal Domain Event
```

اما Direct Call نباید به معنی دسترسی مستقیم به Implementation داخلی Module دیگر باشد.

---

# 18. Synchronous Internal Contract

وقتی Use Case به پاسخ فوری نیاز دارد، استفاده از Contract/Port مناسب است.

برای مثال:

```text
CreateAccountUseCase
        │
        ▼
AccountHeadPort
        │
        ▼
Account Head Capability
        │
        ▼
Result
```

این روش زمانی مناسب است که:

* پاسخ فوری لازم باشد.
* Use Case به نتیجه عملیات نیاز داشته باشد.
* Consistency لحظه‌ای اهمیت داشته باشد.
* Business Dependency مشخص باشد.

---

# 19. Internal Domain Event

وقتی یک Business Fact رخ می‌دهد و سایر بخش‌ها باید از آن مطلع شوند، Internal Domain Event قابل استفاده است.

برای مثال:

```text
Account
   │
   ▼
AccountCreated
   │
   ▼
Internal Event Dispatcher
   │
   ├──► Handler A
   └──► Handler B
```

Producer نباید الزاماً Consumer را مستقیماً بشناسد.

---

# 20. Domain Event ≠ Integration Event

این دو مفهوم باید کاملاً جدا باشند.

### Internal Domain Event

ارتباط داخل همان Microservice:

```text
Account
   │
   ▼
AccountCreated
   │
   ▼
Internal Event Dispatcher
   │
   ▼
Internal Handler
```

### Integration Event

ارتباط از مرز Microservice:

```text
General Ledger Microservice
        │
        ▼
Integration Event
        │
        ▼
Message Broker
        │
        ▼
Another Microservice
```

بنابراین:

> Event داخل General Ledger یک **Internal Domain Event** است.

و:

> Eventی که از مرز General Ledger خارج می‌شود یک **Integration Event** است.

---

# 21. Event جایگزین عمومی Method Call نیست

وجود Domain Event به این معنی نیست که تمام ارتباط‌های داخلی باید Event-driven شوند.

طراحی زیر نامناسب است:

```text
Account
   │
   ▼
Event
   │
   ▼
Account Head
   │
   ▼
Event
   │
   ▼
Journal Entry
```

اگر یک Use Case به پاسخ فوری نیاز دارد، Contract/Port مناسب‌تر است.

اصل:

> **Event برای انتشار Business Fact است، نه جایگزین عمومی Method Call.**

---

# 22. Internal Entity Sharing

Moduleها نباید Entity داخلی یکدیگر را مستقیماً مصرف کنند.

نامطلوب:

```text
Journal Entry
      │
      X
      ▼
AccountEntity
```

یا:

```text
Account
      │
      X
      ▼
AccountHeadEntity
```

به‌جای آن از Reference یا Contract استفاده شود:

```text
AccountId
AccountHeadId
```

یا:

```text
AccountReference
AccountHeadReference
```

هدف:

> **هر Module مالک مدل داخلی خودش باقی بماند.**

---

# 23. Shared Database ≠ Shared Ownership

ممکن است General Ledger در یک Database فیزیکی قرار داشته باشد:

```text
General Ledger Database

├── Account Head Data
├── Account Data
└── Journal Entry Data
```

اما:

> **Shared Database به معنی Shared Ownership نیست.**

هر Module باید مالک منطقی داده‌های مربوط به Business Responsibility خودش باشد.

بنابراین این روش مجاز نیست:

```text
Account
   │
   X
   ▼
SELECT FROM AccountHeadTable
```

به‌عنوان روش Business Integration.

ارتباط باید از طریق Capability/Contract مناسب انجام شود.

---

# 24. Direct Database Access ممنوع

حتی در یک Database مشترک، یک Module نباید مستقیماً Table داخلی Module دیگر را مصرف کند.

نامطلوب:

```text
Journal Entry
      │
      X
      ▼
Account Internal Table
```

یا:

```text
Account
      │
      X
      ▼
Account Head Internal Table
```

این نوع وابستگی باعث ایجاد Coupling پنهان و جلوگیری از Evolution مستقل Moduleها می‌شود.

---

# 25. Circular Dependency

Circular Dependency بین Moduleهای داخلی باید جلوگیری شود.

نمونه نامطلوب:

```text
Account Head
      │
      ▼
Account
      │
      ▼
Account Head
```

یا:

```text
Account
      │
      ▼
Journal Entry
      │
      ▼
Account
```

وجود چنین چرخه‌ای باید باعث بررسی مجدد:

* Business Boundary
* Ownership
* Dependency Direction
* Use Case Design
* Aggregate Boundary

شود.

---

# 26. Business Relationship ≠ Code Dependency

وجود Relationship در مدل کسب‌وکار الزاماً به معنی Circular Dependency در Code نیست.

برای مثال:

```text
Account Head
      │
      ▼
Account
```

ممکن است Relationship کاملاً معتبر Business باشد.

اما در Code باید Dependency Direction مشخص و یک‌طرفه باقی بماند.

هدف:

> **حذف Circular Dependency در Code و Architecture Graph است، نه حذف Relationshipهای معتبر کسب‌وکاری.**

---

# 27. Application و چند Aggregate

Application Layer می‌تواند برای اجرای یک Use Case با چند Aggregate همکاری کند.

برای مثال:

```text
CreateAccountUseCase

      │
      ├──► AccountHead Capability
      │
      └──► Account Aggregate
```

یا در یک Use Case مرتبط با ثبت سند:

```text
PostJournalEntryUseCase

      │
      ├──► Account Capability
      │
      └──► JournalEntry Aggregate
```

Application مسئول ترتیب و هماهنگی است.

اما Business Rule هر Aggregate همچنان در همان Aggregate باقی می‌ماند.

---

# 28. Journal Entry به‌عنوان Core Domain

مطابق ADR-0003، `Journal Entry` Core Subdomain و Core Aggregate است.

بنابراین Dependencyهای داخلی نباید باعث شوند `Journal Entry` به Implementationهای Account یا Account Head وابسته شود.

مدل ترجیحی:

```text
PostJournalEntryUseCase
        │
        ├──► Account Capability
        │
        └──► JournalEntry Aggregate
```

نه:

```text
JournalEntry
        │
        X
        └──► AccountJpaRepository
```

و نه:

```text
JournalEntry
        │
        X
        └──► AccountEntity
```

Core Domain باید تا حد امکان از جزئیات Supporting Domainها مستقل باقی بماند.

---

# 29. ارتباط با Microserviceهای دیگر

وقتی ارتباط از مرز `General Ledger Microservice` عبور کند، دیگر Internal Communication نیست.

مثلاً:

```text
Another Microservice
        │
        ▼
General Ledger Microservice
```

در این حالت باید از Integration Mechanism استفاده شود:

```text
REST API
Message
Integration Event
```

مثلاً:

```text
General Ledger Microservice
        │
        ▼
Integration Event
        │
        ▼
Message Broker
        │
        ▼
Another Microservice
```

Microservice خارجی نباید به:

* Package داخلی
* Module داخلی
* Aggregate داخلی
* Repository
* Entity
* Database

General Ledger دسترسی مستقیم داشته باشد.

---

# 30. Internal Boundary و External Boundary

دو سطح Boundary باید کاملاً جدا باشند.

### Internal Boundary

```text
General Ledger Microservice
        │
        ├── Account Head
        ├── Account
        └── Journal Entry
```

ارتباط:

```text
Internal Contract / Port
Internal Domain Event
```

### External Boundary

```text
General Ledger Microservice
        │
        ▼
Another Microservice
```

ارتباط:

```text
REST
Message
Integration Event
```

---

# 31. Evolution و استخراج Module

هدف این ADR ایجاد Microservice برای هر Module نیست.

هدف، ایجاد Boundaryهای واضح است.

Evolution می‌تواند به این شکل باشد:

```text
General Ledger Microservice
        │
        ▼
Stable Internal Module
        │
        ▼
Business Boundary
        │
        ▼
Operational Need
        │
        ▼
Possible Microservice Extraction
```

اگر در آینده نیاز واقعی به مواردی مانند:

* Independent Scaling
* Independent Deployment
* Independent Database
* Different SLA
* Independent Team Ownership
* Independent Release Cycle
* Operational Isolation

ایجاد شود، Extraction یک Module به Microservice مستقل قابل بررسی خواهد بود.

---

# 32. Architecture Rules

### Rule 1

`Account Head`، `Account` و `Journal Entry` در یک General Ledger Microservice قرار دارند.

### Rule 2

هیچ‌کدام از این Moduleها در وضعیت فعلی Microservice مستقل نیستند.

### Rule 3

ارتباط داخلی بین Moduleها در صورت وجود Business Dependency مجاز است.

### Rule 4

Application Layer محل اصلی Orchestration Use Case است.

### Rule 5

Domain Service مسئول Business Rule و Business Decision است.

### Rule 6

Domain Service نباید به Coordinator عمومی بین Moduleها تبدیل شود.

### Rule 7

Aggregate نباید Repository را مستقیماً فراخوانی کند.

### Rule 8

Domain نباید به Infrastructure وابسته شود.

### Rule 9

دسترسی مستقیم به Repository Implementation یک Module دیگر ممنوع است.

### Rule 10

دسترسی مستقیم به Entity داخلی Module دیگر ممنوع است.

### Rule 11

دسترسی مستقیم به Table داخلی Module دیگر ممنوع است.

### Rule 12

ارتباط Synchronous داخلی باید از طریق Contract/Port انجام شود.

### Rule 13

Internal Domain Event برای انتشار Business Fact داخل Microservice مجاز است.

### Rule 14

Internal Domain Event با Integration Event متفاوت است.

### Rule 15

Event نباید صرفاً جایگزین عمومی Method Call شود.

### Rule 16

Circular Dependency در Architecture Graph ممنوع است.

### Rule 17

ارتباط با Microserviceهای دیگر باید از طریق Integration Contract، API یا Integration Event انجام شود.

### Rule 18

`Journal Entry` به‌عنوان Core Domain نباید به Implementation داخلی Supporting Moduleها وابسته شود.

---

# 33. Enforcement

این قوانین باید تا حد امکان به‌صورت خودکار بررسی شوند.

برای این منظور از **ArchUnit** استفاده خواهد شد.

نمونه قوانین:

```text
Account Domain
    must not depend on
AccountHead Infrastructure
```

```text
JournalEntry Domain
    must not depend on
Account Infrastructure
```

```text
Account Domain
    must not access
AccountHead Internal Entity
```

```text
Application
    may depend on
Internal Public Contract
```

```text
Domain
    must not depend on
Infrastructure
```

همچنین باید موارد زیر کنترل شوند:

* Module Boundaries
* Dependency Direction
* Forbidden Dependencies
* Circular Dependencies
* Infrastructure Isolation
* Internal Contract Usage

---

# 34. Consequences

## Positive Consequences

این تصمیم باعث می‌شود:

* Application مسئول Orchestration باقی بماند.
* Domain مسئول Business Rule باقی بماند.
* Domain از Infrastructure مستقل بماند.
* Aggregateها Boundary مشخص داشته باشند.
* Moduleها بتوانند بدون انتشار Implementation داخلی با یکدیگر همکاری کنند.
* از ایجاد Microserviceهای مصنوعی جلوگیری شود.
* Coupling داخلی کنترل شود.
* استفاده از Synchronous Contract در موارد لازم ممکن باشد.
* استفاده از Domain Event در موارد مناسب ممکن باشد.
* Shared Database باعث ایجاد Integration Coupling نشود.
* امکان استخراج آینده یک Module حفظ شود.
* Core Domain یعنی `Journal Entry` از جزئیات Supporting Moduleها محافظت شود.

## Negative Consequences

این تصمیم هزینه‌هایی نیز دارد:

* نیاز به تعریف Contractهای داخلی
* نیاز به تعریف Portهای مناسب
* نیاز به Mapping بین مدل‌های Boundaryها
* نیاز به Architectural Testing
* نیاز به کنترل Circular Dependency
* نیاز به تشخیص صحیح بین Direct Contract و Domain Event
* نیاز به تفکیک دقیق Application Orchestration از Domain Business Rule

این هزینه‌ها برای حفظ Modularity و جلوگیری از Architecture Drift پذیرفته می‌شوند.

---

# 35. Final Decision

> **General Ledger یک Microservice مستقل در سطح سیستم است. Account Head، Account و Journal Entry داخل همان Microservice و همان Runtime قرار دارند و به‌صورت Domain-oriented Module سازمان‌دهی می‌شوند.**

> **ارتباط بین Moduleها در صورت وجود Business Dependency مجاز است، اما این ارتباط نباید به معنی دسترسی مستقیم به Implementation، Repository Implementation، Entity یا Database داخلی Module دیگر باشد.**

> **Application Layer محل اصلی Orchestration Use Case است و می‌تواند برای اجرای یک Use Case از چند Internal Contract/Port استفاده کند.**

> **Domain و Aggregate مسئول Business Rule و Business Invariant خود هستند و نباید Repository یا Infrastructure را مستقیماً فراخوانی کنند.**

> **Domain Service فقط زمانی استفاده می‌شود که Business Rule یا Business Operation به‌صورت طبیعی متعلق به یک Aggregate، Entity یا Value Object مشخص نباشد. Domain Service نباید به Coordinator عمومی تبدیل شود.**

> **برای ارتباط Synchronous داخلی از Internal Contract/Port و برای انتشار Business Fact از Internal Domain Event استفاده می‌شود.**

> **Internal Domain Event با Integration Event که از مرز Microservice عبور می‌کند متفاوت است.**

> **در نتیجه، ارتباط داخلی General Ledger باید بر اساس Responsibility و Boundary انجام شود، نه صرفاً به دلیل اینکه Moduleها در یک Runtime و Database قرار دارند.**

---

# 36. Final Architecture Model

```text
                         SYSTEM
                           │
                  Microservices Architecture
                           │
                           ▼
              General Ledger Microservice
                           │
                           │
                 General Ledger
                  Bounded Context
                           │
            ┌──────────────┼──────────────┐
            │              │              │
            ▼              ▼              ▼
      Account Head      Account      Journal Entry
         Module          Module          Module
            │              │              │
            ▼              ▼              ▼
     AccountHead        Account      JournalEntry
       Aggregate       Aggregate      Aggregate
                                      ⭐ Core
                           │
                           ▼
                    Application Layer
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
     Internal Contract / Port     Internal Domain Event
         Synchronous                 Asynchronous
```

ارتباط با بیرون:

```text
General Ledger Microservice
            │
            ▼
    Integration Boundary
            │
      ┌─────┼─────┐
      │     │     │
      ▼     ▼     ▼
     REST  Message  Integration Event
            │
            ▼
    Another Microservice
```

---

# 37. Architectural Principle

> **ما در General Ledger، Moduleها را به دلیل وجود در یک Microservice آزاد نمی‌گذاریم که Implementation یکدیگر را مستقیماً مصرف کنند. آنها می‌توانند با یکدیگر همکاری کنند، اما از طریق Boundary و Contract مشخص. Application مسئول Orchestration است، Domain مسئول Business Decision، Aggregate مسئول Invariant، Repository مسئول Persistence Abstraction و Infrastructure مسئول Implementation تکنیکی است.**

> **بنابراین اصل ارتباط داخلی این نیست که «Moduleها نباید با هم صحبت کنند»؛ اصل این است که «Moduleها نباید Boundary یکدیگر را دور بزنند».**

---

# 38. Relationship with ADR-0003

این ADR بر مبنای تصمیم‌های ADR-0003 است و آن‌ها را در سطح Dependency و Internal Communication دقیق‌تر می‌کند.

ADR-0003 تعیین می‌کند:

```text
General Ledger
      │
      └── General Ledger Bounded Context
                    │
                    └── General Ledger Microservice
```

و:

```text
General Ledger

├── Account Head
├── Account
└── Journal Entry ⭐ Core
```

ADR-0004 تعیین می‌کند که این Moduleها **چگونه در داخل همان Microservice با یکدیگر ارتباط داشته باشند**:

```text
Application
     │
     ├── Internal Contract / Port
     │
     ├── Domain
     │
     └── Internal Domain Event
```

بنابراین:

> **ADR-0003 = مرزها و ساختار Domain**

> **ADR-0004 = جهت وابستگی و نحوه ارتباط داخل آن مرزها**

---

# 39. Status

**Status: Accepted**

این ADR باید به‌عنوان مبنای تصمیم‌گیری برای موارد زیر استفاده شود:

* Internal Module Dependencies
* Application Orchestration
* Domain Service
* Aggregate Interaction
* Repository Access
* Internal Contract / Port
* Domain Event
* Shared Database Access
* Dependency Direction
* Circular Dependency
* Inter-module Communication

هر تصمیمی که این اصول را تغییر دهد باید از طریق یک Architectural Decision جدید ثبت شود یا این ADR به‌صورت رسمی اصلاح گردد.

---

# 40. References

### 1. Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of Software*

مبنای مفاهیم:

* Domain Model
* Bounded Context
* Aggregate
* Domain Service
* Domain Boundary

### 2. Vaughn Vernon — *Implementing Domain-Driven Design*

مبنای تصمیم‌های مربوط به:

* Aggregate
* Domain Service
* Application Service
* Domain Event
* Bounded Context
* Context Boundary

### 3. Robert C. Martin — *Clean Architecture*

مبنای تصمیم‌های مربوط به:

* Dependency Rule
* Dependency Inversion
* Separation of Concerns
* Domain Independence

### 4. Martin Fowler — *Patterns of Enterprise Application Architecture*

مبنای تفکیک:

* Application Service
* Domain Logic
* Repository
* Application Orchestration

### 5. Chris Richardson — *Microservices Patterns*

مبنای تصمیم‌های مربوط به:

* Microservice Boundary
* Integration Communication
* Integration Event
* Distributed Systems Concerns

### 6. Mark Richards & Neal Ford — *Fundamentals of Software Architecture*

مبنای تصمیم‌های مربوط به:

* Modularity
* Coupling
* Cohesion
* Architecture Characteristics
* Architectural Evolution

این منابع مبنای اصول و الگوهای معماری هستند؛ قوانین نهایی این ADR متناسب با Boundaryها و نیازمندی‌های `General Ledger Microservice` تعریف شده‌اند.
