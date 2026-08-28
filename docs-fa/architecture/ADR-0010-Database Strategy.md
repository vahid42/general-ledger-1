# ADR-0010 — استراتژی پایگاه داده (Database Strategy)

* **وضعیت:** پذیرفته شده (Accepted)
* **تاریخ:** 2026-08-22
* **ADR مرتبط:** General Ledger — Domain & Bounded Context Decision Baseline

---

## 1. زمینه (Context)

سیستم **General Ledger** بر اساس **Clean Architecture** و **Domain-Driven Design (DDD)** طراحی می‌شود.

بر اساس معماری مرجع فعلی:

```text
General Ledger
│
└── General Ledger Bounded Context
        │
        └── General Ledger Microservice
                │
                ├── Account Head Module
                ├── Account Module
                └── Journal Entry Module
```

سه Subdomain اصلی سیستم عبارت‌اند از:

* Account Head — Supporting Subdomain
* Account — Supporting Subdomain
* Journal Entry — Core Subdomain

این سه Subdomain در حال حاضر در یک **Bounded Context** و یک **Microservice** قرار دارند.

در سطح Tactical DDD نیز سه Aggregate Root اصلی وجود دارد:

* `AccountHead`
* `Account`
* `JournalEntry`

پایگاه داده و تکنولوژی Persistence جزئی از Infrastructure محسوب می‌شوند و نباید مدل Domain را تعیین کنند.

هدف این ADR تعیین اصول مربوط به:

* Persistence
* Repository
* Data Ownership
* Transaction
* Database Schema
* Migration
* Read Model
* Database Testing

است.

---

# 2. تصمیم (Decision)

## 2.1 Database به عنوان جزئیات Infrastructure

Database یک جزئیات زیرساختی است و در **Infrastructure Layer** قرار دارد.

Domain نباید به تکنولوژی Persistence وابسته باشد.

بنابراین Domain نباید وابستگی مستقیم به موارد زیر داشته باشد:

* JPA
* Hibernate
* JDBC
* SQL
* Database Driver
* ORM-specific API
* Transaction API

به‌عنوان مثال، Aggregateهای Domain نباید برای اجرای منطق کسب‌وکار به `EntityManager`، `JpaRepository` یا APIهای مشابه وابسته باشند.

مدل کلی:

```text
Domain
   │
   ▼
Repository / Persistence Port
   │
   ▼
Infrastructure
   │
   ▼
Persistence Technology
   │
   ▼
Database
```

---

# 3. وضعیت فعلی Persistence

در وضعیت فعلی پروژه، Persistence به‌صورت **In-Memory** پیاده‌سازی می‌شود.

بنابراین در حال حاضر:

```text
Application / Domain
        │
        ▼
Repository Port
        │
        ▼
In-Memory Repository
        │
        ▼
In-Memory Data
```

استفاده فعلی از In-Memory یک تصمیم درباره **پیاده‌سازی فعلی Persistence** است و به معنی تعهد معماری به In-Memory Database نیست.

در صورت نیاز، implementation مربوط به Persistence می‌تواند در آینده بدون تغییر Business Ruleهای Domain تغییر کند.

برای مثال:

```text
                 Repository Port
                       │
          ┌────────────┴────────────┐
          ▼                         ▼
 InMemory Repository        Relational Repository
                                    │
                                    ▼
                              SQL Database
```

تغییر Persistence نباید باعث تغییر در Aggregateهای Domain شود، مگر در مواردی که خود نیازهای Domain تغییر کرده باشند.

---

# 4. Repository Strategy

Repository به‌عنوان یک **Persistence Port** تعریف می‌شود.

Repository باید بر اساس Aggregate طراحی شود و نه بر اساس جدول Database.

در General Ledger، Repositoryهای اصلی عبارت‌اند از:

```text
AccountHeadRepository
AccountRepository
JournalEntryRepository
```

مدل معماری:

```text
Aggregate
    │
    ▼
Repository Port
    │
    ▼
Infrastructure Implementation
```

Repository Implementation در Infrastructure قرار می‌گیرد.

Repository نباید به Aggregate اجازه دهد که مستقیماً implementation مربوط به Persistence را فراخوانی کند.

---

# 5. Aggregate-Oriented Persistence

Persistence باید با مرز Aggregateها هماهنگ باشد.

Aggregateهای اصلی عبارت‌اند از:

```text
General Ledger
│
├── AccountHead Aggregate
│
├── Account Aggregate
│
└── JournalEntry Aggregate
```

هر Aggregate مسئول Invariantهای خودش است.

بنابراین Repository نیز باید بر اساس Aggregate طراحی شود.

برای مثال:

```text
JournalEntry
      │
      ▼
JournalEntryRepository
```

و نه:

```text
JournalEntry
      │
      ▼
AccountTableRepository
JournalLineTableRepository
PostingTableRepository
...
```

جزئیات نحوه ذخیره‌سازی داخلی Aggregate می‌تواند در Infrastructure تغییر کند و نباید مرز Aggregate را به Schema Database وابسته کند.

---

# 6. Data Ownership

مالکیت داده باید بر اساس **Domain Model و Aggregate Boundary** مشخص شود.

در General Ledger:

```text
AccountHead Module
        │
        └── AccountHead data

Account Module
        │
        └── Account data

JournalEntry Module
        │
        └── JournalEntry data
```

هر Module مسئول داده‌های Domain خود است.

وجود یک Database مشترک در آینده به معنی Shared Ownership نیست.

اصل معماری:

> **Shared Database does not mean Shared Data Ownership.**

مالکیت منطقی داده باید مستقل از نحوه استقرار فیزیکی Database تعریف شود.

---

# 7. Database Ownership ≠ Bounded Context Ownership

از آنجا که Account Head، Account و Journal Entry در یک Bounded Context قرار دارند، وجود یک Database مشترک در این Context ذاتاً یک نقض معماری محسوب نمی‌شود.

اما حتی در یک Database مشترک نیز باید مرز مالکیت Aggregateها حفظ شود.

بنابراین:

```text
General Ledger Database
│
├── AccountHead-owned data
├── Account-owned data
└── JournalEntry-owned data
```

این تفکیک منطقی باید در Repository و Module boundaries نیز حفظ شود.

Database Schema نباید باعث شود Moduleها مستقیماً به داده‌های مالکیت‌شده توسط Aggregate دیگر دسترسی پیدا کنند.

---

# 8. Cross-Module Data Access

سه Module موجود:

```text
Account Head
Account
Journal Entry
```

در یک Bounded Context قرار دارند؛ بنابراین ارتباط آنها **Inter-Bounded-Context Communication** محسوب نمی‌شود.

با این حال، مرز Module و Aggregate همچنان باید حفظ شود.

به‌عنوان نمونه، هنگام ایجاد Journal Entry ممکن است Application Use Case نیاز داشته باشد:

```text
CreateJournalEntry
        │
        ├── Validate Account
        │
        ├── Validate AccountHead
        │
        └── Create JournalEntry
```

این هماهنگی در Application Layer انجام می‌شود.

اما Business Ruleهای مربوط به Journal Entry باید در Domain باقی بمانند.

به‌عنوان مثال:

```text
Application Layer
       │
       ├── load Account
       ├── load AccountHead
       └── create JournalEntry
                    │
                    ▼
              Domain Rules
                    │
                    ▼
             JournalEntry
```

Aggregate نباید برای انجام این هماهنگی Repository مربوط به Aggregate دیگری را مستقیماً فراخوانی کند.

---

# 9. Repository Port ≠ Internal Contract

Repository تنها یکی از انواع Portهای سیستم است.

دو مفهوم باید از یکدیگر تفکیک شوند:

```text
Repository Port
    │
    └── Persistence abstraction


Internal Contract / Port
    │
    └── Capability / communication abstraction
```

وجود یک Interface در Domain یا Application به‌تنهایی به معنی Repository بودن آن نیست.

این تفکیک از وابستگی اشتباه Domain به Infrastructure جلوگیری می‌کند.

---

# 10. Transaction Strategy

Transaction در سطح **Application Use Case** مدیریت می‌شود.

Domain نباید مسئول شروع، commit یا rollback Transaction باشد.

مدل کلی:

```text
API
 │
 ▼
Application Use Case
 │
 ├── Load required Aggregates
 │
 ├── Execute Domain behavior
 │
 ├── Persist changes
 │
 └── Transaction Boundary
          │
          ▼
      Persistence
```

در وضعیت فعلی که Persistence به‌صورت In-Memory است، مفهوم Transaction Database ممکن است وجود نداشته باشد یا به شکل متفاوتی پیاده‌سازی شود.

این موضوع نباید باعث ورود Transaction API به Domain شود.

در صورت مهاجرت به Database تراکنشی، Transaction Management در Infrastructure/Application Integration پیاده‌سازی خواهد شد.

---

# 11. Aggregate Consistency Boundary

Transaction و Aggregate Boundary باید با یکدیگر اشتباه گرفته نشوند.

Aggregate مرز اصلی **Consistency و Invariant** است.

Transaction می‌تواند یک یا چند Aggregate را در یک Use Case هماهنگ کند، اما این موضوع به معنی تبدیل چند Aggregate به یک Aggregate نیست.

برای مثال:

```text
CreateJournalEntry
       │
       ├── Account Aggregate
       │
       ├── AccountHead Aggregate
       │
       └── JournalEntry Aggregate
```

این Use Case می‌تواند چند Aggregate را هماهنگ کند، در حالی که هر Aggregate همچنان مرز Consistency خودش را حفظ می‌کند.

---

# 12. Database Technology

در صورت مهاجرت به Persistence دائمی، استفاده از **Relational Database** به‌عنوان گزینه اصلی مورد قبول است.

تکنولوژی‌هایی مانند:

* Spring Data JPA
* Hibernate
* JDBC

می‌توانند در Infrastructure استفاده شوند.

اما انتخاب این تکنولوژی‌ها نباید باعث وابستگی Domain به آنها شود.

مدل:

```text
Domain
   │
   ▼
Repository Port
   │
   ▼
Infrastructure
   │
   ├── Spring Data JPA
   ├── Hibernate
   └── JDBC
          │
          ▼
    Relational Database
```

انتخاب نهایی تکنولوژی Persistence می‌تواند در ADR جداگانه مشخص شود.

---

# 13. Persistence Model vs Domain Model

Domain Model و Persistence Model از نظر مفهومی مستقل هستند.

در صورت وجود تفاوت معنادار بین آنها:

```text
Domain Model
     │
     │ Mapping
     ▼
Persistence Model
     │
     ▼
Database
```

Persistence Model نباید به مدل اصلی Domain تبدیل شود.

خصوصاً Aggregateهای Core Domain، یعنی `JournalEntry`، نباید صرفاً بر اساس محدودیت‌های ORM یا Database طراحی شوند.

در صورت ساده بودن مدل و نبودن وابستگی معماری، استفاده مشترک از برخی ساختارها می‌تواند بررسی شود؛ اما این موضوع نباید باعث وابستگی Domain به ORM شود.

---

# 14. Database Schema Strategy

در صورت استفاده از Relational Database، Schema باید تا حد امکان با مرزهای مالکیت Domain هماهنگ باشد.

برای General Ledger می‌توان ساختاری مشابه زیر در نظر گرفت:

```text
General Ledger Database
│
├── AccountHead-owned structures
│
├── Account-owned structures
│
└── JournalEntry-owned structures
```

استفاده از Schemaهای فیزیکی جداگانه برای هر Module الزام معماری نیست.

برای مثال، این طراحی:

```text
Database
│
├── account_head_schema
├── account_schema
└── journal_entry_schema
```

در صورت وجود نیاز عملیاتی می‌تواند استفاده شود، اما صرفاً به دلیل وجود سه Module الزامی نیست.

مرز اصلی باید ابتدا در **Domain، Module و Repository** حفظ شود و سپس در صورت نیاز در Database Schema نیز منعکس شود.

---

# 15. Database Migration

در صورت استفاده از Database دائمی، تمام تغییرات Schema باید به‌صورت Versioned Migration مدیریت شوند.

Migrationها باید:

* Version داشته باشند.
* در Source Control نگهداری شوند.
* قابل تکرار باشند.
* بین محیط‌های مختلف قابل اجرا باشند.
* از تغییر دستی و کنترل‌نشده Schema جلوگیری کنند.

ابزارهایی مانند:

* Flyway
* Liquibase

می‌توانند برای این منظور استفاده شوند.

انتخاب ابزار Migration در صورت نیاز می‌تواند در ADR جداگانه مشخص شود.

---

# 16. Query و Read Model

Queryهای پیچیده نباید باعث آلودگی Aggregateهای Domain شوند.

در صورت نیاز به Queryهای پیچیده، Reporting یا Read-heavy operations، استفاده از Read Model یا Projection مجاز است.

مدل:

```text
Write Side

Command
   │
   ▼
Application
   │
   ▼
Domain
   │
   ▼
Aggregate
   │
   ▼
Repository
```

و:

```text
Read Side

Query
   │
   ▼
Query Handler
   │
   ▼
Read Model / Projection
   │
   ▼
Persistence
```

Read Model الزاماً نباید با Domain Model یکسان باشد.

---

# 17. Performance Strategy

بهینه‌سازی Persistence باید بر اساس Measurement انجام شود.

موارد زیر در صورت استفاده از Database دائمی باید مورد توجه قرار گیرند:

* Indexing
* Query Optimization
* Pagination
* Batch Processing
* Connection Pooling
* N+1 Query Detection
* Loading Strategy
* Transaction Size
* Database Locking
* Query Latency

هیچ Optimization پیچیده‌ای نباید بدون وجود Bottleneck قابل اندازه‌گیری وارد معماری شود.

در وضعیت فعلی In-Memory، این موارد صرفاً به‌عنوان الزامات آینده Persistence در نظر گرفته می‌شوند.

---

# 18. Persistence Testing

تست Persistence باید متناسب با implementation انجام شود.

در وضعیت فعلی:

```text
Unit Test
    │
    └── In-Memory Repository
```

می‌تواند برای بررسی رفتار Repository و Use Caseها استفاده شود.

پس از اضافه شدن Persistence واقعی:

```text
Integration Test
    │
    ├── Repository
    ├── Persistence Mapping
    ├── Transaction
    └── Database
```

برای Repositoryهای مبتنی بر Database واقعی، استفاده از Database واقعی یا Testcontainers ترجیح داده می‌شود.

Unit Test نباید جایگزین تست واقعی SQL، Mapping یا Transaction behavior شود.

---

# 19. Architectural Rules

قوانین زیر برای Persistence در General Ledger الزامی هستند:

1. Domain نباید به Database وابسته باشد.

2. Domain نباید به JPA، Hibernate، JDBC یا ORM-specific API وابسته باشد.

3. Repository باید بر اساس Aggregate طراحی شود.

4. Repository Implementation در Infrastructure قرار می‌گیرد.

5. Aggregate نباید Repository مربوط به Aggregate دیگر را مستقیماً فراخوانی کند.

6. Application Layer مسئول orchestration بین Aggregateها است.

7. Business Ruleهای Aggregate باید در Domain باقی بمانند.

8. Database Ownership باید با Domain Ownership اشتباه گرفته نشود.

9. Shared Database به معنی Shared Data Ownership نیست.

10. Module نباید مستقیماً به Persistence structure مالکیت‌شده توسط Module دیگر وابسته شود.

11. Transaction Boundary در سطح Application Use Case مدیریت می‌شود.

12. Domain نباید مسئول مدیریت Transaction باشد.

13. تغییر تکنولوژی Persistence نباید مستلزم تغییر Business Ruleهای Domain باشد.

14. Queryهای پیچیده نباید Aggregateهای Domain را برای نیازهای Read تغییر دهند.

15. در صورت استفاده از Database دائمی، Schema Changes باید از طریق Versioned Migration انجام شوند.

---

# 20. Consequences

## مزایا

* استقلال Domain از Persistence Technology
* حفظ مرز Aggregateها
* کاهش Coupling بین Moduleها
* مشخص بودن Data Ownership
* امکان استفاده فعلی از In-Memory Persistence
* امکان مهاجرت آینده به Relational Database
* جلوگیری از تبدیل Domain Model به ORM Model
* امکان تست مستقل Domain
* امکان تست Integration برای Persistence واقعی
* حفظ Core بودن `JournalEntry`
* امکان تغییر تکنولوژی Persistence بدون تغییر Business Logic

## معایب

* نیاز به تعریف Repository Port
* احتمال ایجاد Mapping بین Domain و Persistence Model
* افزایش کد Infrastructure
* نیاز به مدیریت Migration در صورت استفاده از Database دائمی
* نیاز به طراحی دقیق Query و Read Model
* پیچیدگی بیشتر در برخی Cross-Aggregate Use Caseها

---

# 21. وضعیت فعلی پیاده‌سازی (Implementation Status)

در وضعیت فعلی:

```text
General Ledger Microservice
        │
        ├── Account Head
        ├── Account
        └── Journal Entry
                │
                ▼
        Repository Ports
                │
                ▼
        In-Memory Implementations
```

Persistence فعلی **In-Memory** است.

بنابراین استفاده از Relational Database، JPA، Hibernate، Flyway یا Liquibase در این مرحله الزام اجرایی این ADR نیست.

این ADR معماری Persistence را مستقل از implementation فعلی تعریف می‌کند تا در صورت تغییر Persistence، مرزهای Domain و Application حفظ شوند.

---

# 22. References

1. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**

2. Vaughn Vernon — **Implementing Domain-Driven Design**

3. Robert C. Martin — **Clean Architecture: A Craftsman's Guide to Software Structure and Design**

4. Martin Fowler — **Patterns of Enterprise Application Architecture**

5. Chris Richardson — **Microservices Patterns**

6. Martin Fowler — **Database Migration**

7. Spring Data JPA — **Reference Documentation**

8. Hibernate ORM — **Documentation**

9. Flyway — **Documentation**

10. Liquibase — **Documentation**

11. Testcontainers — **Documentation**

---

# 23. Final Decision

استراتژی Persistence در General Ledger بر اساس اصول زیر تعریف می‌شود:

```text
General Ledger Bounded Context
            │
            ▼
General Ledger Microservice
            │
            ├── AccountHead Aggregate
            │       │
            │       ▼
            │   Repository Port
            │
            ├── Account Aggregate
            │       │
            │       ▼
            │   Repository Port
            │
            └── JournalEntry Aggregate
                    │
                    ▼
                Repository Port
                    │
                    ▼
             Infrastructure
                    │
                    ▼
        Current: In-Memory
        Future: Relational DB
```

اصل کلیدی:

> **Persistence یک جزئیات Infrastructure است؛ Aggregate و Domain Model نباید توسط Database تعیین شوند.**

> **در وضعیت فعلی، Persistence به‌صورت In-Memory است و این موضوع نباید باعث تغییر در مرزهای Domain، Aggregate و Repository Port شود.**

> **در صورت مهاجرت به Database دائمی، مالکیت داده همچنان بر اساس Module و Aggregate حفظ می‌شود و تکنولوژی Persistence در Infrastructure باقی می‌ماند.**
