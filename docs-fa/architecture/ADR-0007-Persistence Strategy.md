# ADR-0007 — استراتژی ماندگاری داده (Persistence Strategy)

* **وضعیت:** Accepted
* **تاریخ:** 2026-08-22
* **ADRهای مرتبط:**

  * ADR-0003 
  * ADR-0004 
  * ADR-0005 
  * ADR-0006 

---

## 1. مسئله و زمینه (Context)

سیستم یک سامانه مالی مبتنی بر **General Ledger** است و داده‌های آن باید به‌صورت پایدار، قابل اعتماد و سازگار ذخیره شوند.

Persistence در این سیستم صرفاً یک Concern تکنیکی نیست؛ زیرا نحوه ذخیره‌سازی داده‌ها می‌تواند مستقیماً بر موارد زیر تأثیر بگذارد:

* Domain Model
* Aggregate Boundary
* Transaction Boundary
* Consistency
* Data Integrity
* Performance
* Scalability
* Auditability
* Concurrency

مطابق ADR-0003، General Ledger به‌صورت یک **Bounded Context** و یک **Microservice مستقل** پیاده‌سازی می‌شود.

مطابق ADR-0005 و ADR-0006 نیز Domain Model باید مستقل از جزئیات Infrastructure و Persistence باقی بماند.

بنابراین باید مرز مشخصی میان:

```text
Business / Domain Model
```

و:

```text
Persistence Model
```

وجود داشته باشد.

هدف این ADR تعیین استراتژی Persistence و نحوه ارتباط Domain و Application با Database است.

---

# 2. تصمیم (Decision)

سیستم از یک **Persistence Strategy مبتنی بر Repository Port و Persistence Adapter** استفاده خواهد کرد.

جزئیات تکنولوژی Persistence در Infrastructure قرار می‌گیرد و Domain نباید مستقیماً به JPA، Hibernate یا Database وابسته باشد.

مدل کلی:

```text
                    General Ledger Microservice
                               │
              ┌────────────────┴────────────────┐
              │                                 │
          Application                         Domain
              │                                 │
              └──────────────┬──────────────────┘
                             │
                     Repository Port
                             ▲
                             │
                     Infrastructure
                             │
                     Persistence Adapter
                             │
                         JPA / Hibernate
                             │
                         Database
```

برای Persistence رابطه‌ای، تکنولوژی اصلی مورد استفاده:

```text
JPA / Hibernate
```

خواهد بود.

این تصمیم به معنی وابستگی Domain به JPA نیست؛ JPA صرفاً یکی از تکنولوژی‌های Infrastructure است.

---

# 3. اصل Dependency Inversion

Dependency باید به سمت Abstraction و Business Responsibility باشد، نه Implementation تکنیکی.

مدل صحیح:

```text
Domain / Application
        │
        ▼
Repository Port
        ▲
        │ implements
        │
Infrastructure
        │
        ▼
JPA / Hibernate
        │
        ▼
Database
```

مدل نامطلوب:

```text
Domain
   │
   ▼
JPA
   │
   ▼
Hibernate
   │
   ▼
Database
```

در نتیجه:

> Domain نباید برای انجام Persistence به JPA، Hibernate، EntityManager یا Database Driver وابسته شود.

---

# 4. Repository به‌عنوان Persistence Port

Repository Interface به‌عنوان Abstraction مربوط به Persistence تعریف می‌شود.

برای مثال:

```java
public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    void save(Account account);
}
```

Contract Repository باید بر اساس نیاز Domain/Application طراحی شود و نباید جزئیات Persistence Technology را آشکار کند.

موارد زیر نباید بخشی از Repository Port باشند:

```text
JpaRepository
EntityManager
Hibernate Session
Criteria API
JPQL
SQL
JPA Specification
Hibernate Proxy
```

Repository باید یک Business-Oriented Persistence Abstraction باقی بماند، نه یک API برای دسترسی عمومی به Database.

---

# 5. Repository Implementation و Persistence Adapter

Implementation مربوط به Repository Port در Infrastructure قرار می‌گیرد.

ساختار مفهومی:

```text
Infrastructure
│
└── persistence
    │
    ├── entity
    │   ├── AccountJpaEntity
    │   ├── AccountHeadJpaEntity
    │   └── JournalEntryJpaEntity
    │
    ├── repository
    │   ├── AccountJpaRepository
    │   ├── AccountHeadJpaRepository
    │   └── JournalEntryJpaRepository
    │
    ├── mapper
    │   ├── AccountPersistenceMapper
    │   ├── AccountHeadPersistenceMapper
    │   └── JournalEntryPersistenceMapper
    │
    └── adapter
        ├── AccountRepositoryAdapter
        ├── AccountHeadRepositoryAdapter
        └── JournalEntryRepositoryAdapter
```

ارتباط:

```text
AccountRepository
        ▲
        │ implements
        │
AccountRepositoryAdapter
        │
        ▼
AccountJpaRepository
        │
        ▼
AccountJpaEntity
        │
        ▼
Database
```

---

# 6. Domain Entity و Persistence Entity

در حالت پیش‌فرض، Domain Entity و Persistence Entity از یکدیگر جدا خواهند بود.

برای مثال:

```text
Domain
└── Account
```

و:

```text
Infrastructure
└── AccountJpaEntity
```

این جداسازی باعث می‌شود:

* Domain مستقل از JPA باقی بماند.
* Persistence Concern وارد Domain نشود.
* تغییر Schema الزاماً باعث تغییر Domain Model نشود.
* Mapping به‌صورت صریح و قابل کنترل انجام شود.
* Framework Leakage کاهش پیدا کند.

با این حال، مطابق ADR-0006، جداسازی Domain Entity و Persistence Entity یک قانون مطلق نیست.

اگر در یک مورد مشخص مدل Domain و Persistence بدون ایجاد Coupling نامناسب بتوانند یکی باشند، این تصمیم می‌تواند به‌صورت آگاهانه اتخاذ شود.

اصل معماری:

> Business Model نباید صرفاً برای راحتی Persistence قربانی شود.

---

# 7. Mapping Strategy

بین Domain Model و Persistence Model از Mapping استفاده خواهد شد.

مسیر Write:

```text
Domain Aggregate
       │
       ▼
Persistence Mapper
       │
       ▼
Persistence Entity
       │
       ▼
Database
```

مسیر Read:

```text
Database
   │
   ▼
Persistence Entity
   │
   ▼
Persistence Mapper
   │
   ▼
Domain Aggregate
```

بنابراین:

```text
Domain Model ≠ Persistence Model
```

مگر در مواردی که تصمیم معماری مشخص و آگاهانه‌ای برای اشتراک مدل گرفته شده باشد.

---

# 8. Database به‌عنوان Infrastructure

Database یک جزئیات Infrastructure محسوب می‌شود.

مدل کلی:

```text
                 Domain
                    ▲
                    │
             Repository Port
                    ▲
                    │
             Infrastructure
                    │
             ┌──────┴──────┐
             │             │
            JPA           SQL
             │             │
             └──────┬──────┘
                    ▼
                 Database
```

تغییر تکنولوژی Database یا Persistence نباید Business Ruleهای Domain را تغییر دهد.

برای مثال، تغییر:

```text
Hibernate
```

به:

```text
Another Persistence Technology
```

نباید باعث تغییر در:

```text
JournalEntry
Account
AccountHead
```

و Business Ruleهای آنها شود.

---

# 9. Database Ownership

در General Ledger Microservice، هر Module مالک Logical Data مربوط به Business Responsibility خودش است.

ساختار:

```text
General Ledger Microservice
│
├── Account Head Module
│   └── Account Head Data
│
├── Account Module
│   └── Account Data
│
└── Journal Entry Module
    └── Journal Entry Data
```

ممکن است تمام این داده‌ها از یک Database فیزیکی استفاده کنند:

```text
General Ledger Database
│
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

اما:

> Physical Database Sharing به معنی Shared Data Ownership نیست.

مالکیت Logical Data باید در سطح Module حفظ شود.

---

# 10. Cross-Module Database Access

هیچ Moduleای نباید مستقیماً Table یا Persistence Entity متعلق به Module دیگر را Query یا Modify کند.

نامطلوب:

```text
Journal Entry Module
        │
        X
        ▼
Account Table
```

یا:

```text
Account Module
        │
        X
        ▼
AccountHeadJpaEntity
```

ارتباط باید از طریق Business Capability یا Contract مناسب انجام شود:

```text
Module A
   │
   ▼
Internal Contract / Port
   │
   ▼
Module B
```

یا در شرایط مناسب:

```text
Module A
   │
   ▼
Domain Event
   │
   ▼
Module B
```

این قانون از ایجاد Database Coupling و شکستن Module Boundary جلوگیری می‌کند.

---

# 11. Transaction Strategy

Transaction Boundary در سطح Application Use Case مدیریت می‌شود.

مدل معمول:

```text
Use Case
   │
   ▼
Transaction Boundary
   │
   ├── Load Aggregate
   │
   ├── Execute Domain Behavior
   │
   ├── Persist Changes
   │
   └── Commit
```

Transaction نباید صرفاً بر اساس متدهای Repository طراحی شود.

برای مثال:

```text
save()
```

به‌تنهایی نباید مالک Transaction Boundary باشد.

Transaction باید با توجه به:

* Use Case
* Consistency Requirement
* Aggregate Boundary
* Business Operation

تعریف شود.

---

# 12. Aggregate و Transaction Boundary

Aggregate مرز اصلی Consistency در Domain است.

بنابراین رابطه مفهومی:

```text
Aggregate
    │
    ▼
Consistency Boundary
    │
    ▼
Transaction Requirement
```

اما این به معنی آن نیست که:

```text
1 Aggregate = 1 Transaction
```

به‌صورت مطلق باشد.

یک Use Case ممکن است به چند Aggregate نیاز داشته باشد.

در این شرایط باید مشخص شود که:

* آیا Consistency همزمان واقعاً لازم است؟
* آیا Transaction می‌تواند چند Aggregate را پوشش دهد؟
* آیا Coupling ایجادشده قابل قبول است؟
* آیا Eventual Consistency مناسب‌تر است؟

اصل:

> Transaction Boundary باید بر اساس Business Consistency Requirement تعیین شود، نه صرفاً بر اساس ساختار Database.

---

# 13. Persistence of Aggregate

Repository برای Aggregate Root تعریف می‌شود.

در General Ledger:

```text
AccountHead Aggregate
        │
        ▼
AccountHeadRepository
```

```text
Account Aggregate
        │
        ▼
AccountRepository
```

```text
JournalEntry Aggregate
        │
        ▼
JournalEntryRepository
```

Entityهای داخلی Aggregate Repository مستقل ندارند، مگر اینکه در مدل واقعی Domain خودشان Aggregate Root باشند.

برای مثال:

```text
JournalEntry
   │
   └── JournalLine
```

در حالت فعلی:

```text
JournalEntryRepository
```

مرجع Persistence کل Aggregate است و:

```text
JournalLineRepository
```

به‌صورت مستقل ایجاد نمی‌شود.

---

# 14. Lazy Loading

Lazy Loading نباید بخشی از Business Logic باشد.

Domain نباید به این موضوع وابسته باشد که Entity در زمان دسترسی:

```text
Entity
   ↓
Lazy Proxy
   ↓
Database
```

است.

Persistence Layer باید مشخص کند چه داده‌ای برای اجرای Use Case لازم است و Query مناسب را ایجاد کند.

هدف جلوگیری از مشکلاتی مانند:

```text
LazyInitializationException
N+1 Query
Unintended Database Access
Unexpected Query Explosion
```

است.

بنابراین Domain نباید به Navigationهای ORM برای دستیابی به داده وابسته شود.

---

# 15. Command و Query

در Persistence Strategy بین عملیات تغییر State و عملیات Read تفاوت منطقی در نظر گرفته می‌شود.

## Command

Command برای تغییر Domain استفاده می‌شود:

```text
Command
   │
   ▼
Application Use Case
   │
   ▼
Aggregate
   │
   ▼
Business Rule
   │
   ▼
Repository
```

## Query

Query برای خواندن اطلاعات استفاده می‌شود:

```text
Query
   │
   ▼
Query Adapter / Read Model
   │
   ▼
Database
```

در Queryهای پیچیده لازم نیست همیشه ابتدا Domain Aggregate کامل Load شود.

---

# 16. Read Model

برای Queryهای پیچیده، Reporting و Readهای Performance-Sensitive می‌توان از Read Model استفاده کرد.

مدل:

```text
Reporting Query
       │
       ▼
Query Service
       │
       ▼
Read Model
       │
       ▼
Database
```

Read Model الزاماً Domain Entity نیست.

هدف:

* کاهش هزینه Query
* جلوگیری از Load غیرضروری Aggregate
* بهبود Performance
* ساده‌تر شدن Reporting
* کاهش Coupling Query با Domain Model

---

# 17. CQRS

CQRS به‌صورت کامل و سراسری برای General Ledger اجباری نیست.

در نقاطی که:

* حجم Read بالا باشد،
* Query پیچیده باشد،
* Reporting نیازمند مدل متفاوت باشد،
* یا Performance نیازمند Read Model اختصاصی باشد،

می‌توان از CQRS به‌صورت موضعی استفاده کرد.

مدل:

```text
Command Side
     │
     ▼
Domain Model
```

و:

```text
Query Side
     │
     ▼
Read Model
```

بنابراین:

> CQRS یک تصمیم موضعی بر اساس نیاز واقعی است، نه یک الزام معماری برای تمام General Ledger Microservice.

---

# 18. Database Schema

Database Schema بخشی از Persistence Model است و نباید مستقیماً ساختار Domain را تعیین کند.

روند ترجیحی:

```text
Business Requirement
        │
        ▼
Domain Model
        │
        ▼
Persistence Model
        │
        ▼
Database Schema
```

نه:

```text
Database Schema
        │
        ▼
JPA Entity
        │
        ▼
Domain Model
```

Database باید نیازهای Persistence را برآورده کند، نه اینکه Business Model را تعریف کند.

---

# 19. Database Migration

تغییرات Database باید Versioned و قابل تکرار باشند.

Migrationها باید در Repository پروژه نگهداری شوند.

مدل:

```text
Migration V1
Migration V2
Migration V3
...
```

Environmentهای مختلف باید بتوانند Migrationها را به‌صورت ترتیبی و قابل پیش‌بینی اجرا کنند.

تغییر دستی Schema در Environmentهای مختلف نباید روش اصلی مدیریت Database باشد.

Migration باید بخشی از Lifecycle نرم‌افزار باشد.

---

# 20. Data Integrity

Data Integrity باید در دو سطح کنترل شود.

## 20.1 Domain Integrity

Business Invariantها در Domain enforce می‌شوند:

```text
Domain
   │
   ▼
Business Invariant
```

برای مثال:

```text
JournalEntry must be balanced
```

باید توسط:

```text
JournalEntry Aggregate
```

تضمین شود.

## 20.2 Database Integrity

Database نیز باید Integrity داده را با Constraintهای مناسب تضمین کند.

مانند:

```text
Primary Key
Foreign Key
Unique Constraint
Not Null
Check Constraint
```

اصل:

> Database نباید تنها محل اجرای Business Rule باشد، اما نباید از قابلیت‌های Database برای حفظ Data Integrity صرف‌نظر شود.

---

# 21. Optimistic Locking

برای Aggregateهایی که احتمال Concurrent Update دارند، Optimistic Locking در نظر گرفته خواهد شد.

مدل مفهومی:

```text
Aggregate
   │
   └── Version
```

در زمان Update:

```text
Expected Version
       │
       ▼
Database
       │
       ├── Version matches
       │       │
       │       ▼
       │     Update
       │
       └── Version changed
               │
               ▼
             Conflict
```

هدف جلوگیری از:

```text
Lost Update
```

و تشخیص Concurrent Modification است.

استفاده دقیق از Optimistic Locking باید بر اساس Concurrency Requirement هر Aggregate تعیین شود.

---

# 22. Auditing

با توجه به ماهیت مالی General Ledger، قابلیت Audit شدن تغییرات داده ضروری است.

Audit باید در صورت نیاز بتواند اطلاعاتی مانند موارد زیر را ثبت کند:

* چه داده‌ای تغییر کرده است؟
* چه زمانی تغییر کرده است؟
* چه عملیاتی انجام شده است؟
* چه Actor یا Contextی عملیات را انجام داده است؟
* مقدار قبلی و جدید در صورت نیاز چه بوده است؟

Audit Concern نباید Business Logic اصلی Domain را آلوده کند.

در صورت استفاده از ORM Auditing، این مکانیزم باید به‌عنوان Infrastructure Concern باقی بماند.

---

# 23. Soft Delete

Soft Delete به‌صورت سراسری برای تمام Aggregateها اجباری نیست.

تصمیم درباره حذف باید بر اساس Business Meaning انجام شود.

در یک سیستم مالی ممکن است مفهوم صحیح به جای:

```text
deleted = true
```

یکی از موارد زیر باشد:

```text
Deactivate
Close
Archive
Cancel
```

بنابراین برای هر Aggregate باید مشخص شود که Business Concept واقعی چیست.

اصل:

> نباید صرفاً به دلیل راحتی Persistence از Soft Delete استفاده شود.

---

# 24. Repository Abstraction

Repository باید حداقل عملیات موردنیاز Domain/Application را ارائه دهد.

Repository نباید به Gateway عمومی برای تمام عملیات Database تبدیل شود.

نامطلوب:

```text
GenericRepository
    ├── findAnything()
    ├── executeAnything()
    ├── queryAnything()
    └── updateAnything()
```

ترجیحی:

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

که Contract آنها بر اساس نیاز واقعی Aggregate و Use Case طراحی شده باشد.

---

# 25. Repository و Query Responsibility

Repository مسئول Persistence Aggregate است.

اما Queryهای پیچیده و Reporting الزاماً نباید توسط همان Repository انجام شوند.

مدل:

```text
Command
   │
   ▼
Aggregate Repository
```

در مقابل:

```text
Query
   │
   ▼
Query Service / Read Adapter
   │
   ▼
Database
```

این تفکیک اجازه می‌دهد:

* Command Model برای Domain بهینه باشد.
* Query Model برای Read Performance بهینه باشد.
* Repositoryها بیش از حد پیچیده نشوند.
* Domain Model تحت تأثیر Reporting Queryها قرار نگیرد.

---

# 26. Performance Strategy

Persistence باید با توجه به نوع Operation طراحی شود.

برای Command:

```text
Use Case
   │
   ▼
Aggregate
   │
   ▼
Repository
   │
   ▼
Database
```

برای Query ساده:

```text
Query
   │
   ▼
Query Adapter
   │
   ▼
Database
```

برای Query پیچیده:

```text
Query
   │
   ▼
Optimized SQL / Read Model
   │
   ▼
Database
```

استفاده از Domain Aggregate برای تمام Queryها الزامی نیست.

اصل:

> Domain Model باید برای Business Behavior بهینه باشد و Read Model می‌تواند برای Read Performance بهینه شود.

---

# 27. N+1 Query

N+1 Query یک Persistence Anti-Pattern محسوب می‌شود.

در طراحی Query و Repository باید مشخص باشد:

* چه داده‌ای لازم است.
* چه Relationshipهایی لازم است.
* چه Queryهایی اجرا می‌شوند.
* چه تعداد Round Trip به Database انجام می‌شود.

نباید به دلیل Navigation در Object Graph، Queryهای ناخواسته ایجاد شوند.

به‌خصوص برای:

```text
JournalEntry
JournalLine
Account
AccountHead
```

Query Strategy باید آگاهانه طراحی شود.

---

# 28. Persistence Technology

تکنولوژی اصلی Persistence رابطه‌ای:

```text
JPA / Hibernate
```

خواهد بود.

اما این تکنولوژی در Infrastructure قرار دارد:

```text
Domain
   │
   ▼
Repository Port
   ▲
   │
Infrastructure
   │
   ▼
JPA / Hibernate
   │
   ▼
Relational Database
```

بنابراین:

> JPA/Hibernate یک تصمیم تکنولوژیک در Infrastructure است و بخشی از Domain Model محسوب نمی‌شود.

---

# 29. Shared Physical Database

General Ledger Microservice می‌تواند از یک Database فیزیکی مشترک برای Moduleهای خود استفاده کند.

مدل:

```text
General Ledger Database
        │
        ├── Account Head Data
        │
        ├── Account Data
        │
        └── Journal Entry Data
```

اما این اشتراک فیزیکی نباید باعث Shared Domain Model یا Shared Persistence Ownership شود.

بنابراین:

```text
Physical Database Sharing
        ≠
Shared Domain Model
```

و:

```text
Physical Database Sharing
        ≠
Unrestricted Table Access
```

---

# 30. Module Data Ownership

Moduleها باید مالک Logical Data خود باشند.

```text
General Ledger Microservice
│
├── Account Head Module
│   └── owns Account Head data
│
├── Account Module
│   └── owns Account data
│
└── Journal Entry Module
    └── owns Journal Entry data
```

Module دیگر نباید برای انجام Business Logic خود مستقیماً به Persistence Model داخلی Module مالک وابسته شود.

مثال نامطلوب:

```text
Journal Entry Module
        │
        X
        ▼
AccountJpaEntity
```

مدل ترجیحی:

```text
Journal Entry Application
        │
        ▼
Account Capability / Internal Contract
```

---

# 31. Aggregate Persistence و Core Domain Protection

از آنجا که:

```text
JournalEntry
```

Core Aggregate است، Persistence Implementation مربوط به Supporting Moduleها نباید وارد Domain آن شود.

نامطلوب:

```text
JournalEntry Domain
        │
        X
        ▼
AccountJpaRepository
```

یا:

```text
JournalEntry
        │
        X
        ▼
AccountJpaEntity
```

مدل ترجیحی:

```text
PostJournalEntryUseCase
        │
        ├──► Account Capability
        │
        └──► JournalEntry Aggregate
```

در این مدل، Core Domain به Implementation داخلی Supporting Module وابسته نمی‌شود.

---

# 32. Transaction و Core Journal Entry

Transactionهای مربوط به Journal Entry باید بر اساس Business Consistency واقعی طراحی شوند.

برای مثال:

```text
PostJournalEntry
       │
       ▼
Transaction Boundary
       │
       ├── Load required Account capability/data
       │
       ├── Execute JournalEntry behavior
       │
       ├── Persist JournalEntry
       │
       └── Commit
```

اما:

> وجود Transaction مشترک به معنی قرار گرفتن Aggregateها در یک Aggregate Boundary نیست.

برای مثال:

```text
Account Aggregate
```

و:

```text
JournalEntry Aggregate
```

همچنان دو Aggregate مستقل باقی می‌مانند، حتی اگر یک Use Case هر دو را در یک Transaction درگیر کند.

---

# 33. Persistence و Domain Events

Persistence نباید Domain Event را با Database Event یا ORM Callback اشتباه بگیرد.

مدل Domain:

```text
Aggregate
    │
    ▼
Domain Event
```

و سپس در Application/Infrastructure:

```text
Domain Event
    │
    ▼
Event Handling / Publication
```

Database Trigger یا ORM Callback نباید جایگزین Business Domain Event شود، مگر برای یک Concern صرفاً تکنیکی.

---

# 34. Persistence و Integration Boundary

Persistence Concern داخلی General Ledger Microservice است.

نباید Repository به‌عنوان مکانیزم Integration با سایر Microserviceها استفاده شود.

نامطلوب:

```text
Another Microservice
        │
        X
        ▼
JournalEntryRepository
```

Integration باید از طریق API، Messaging یا Contract مناسب انجام شود.

```text
Another Microservice
        │
        ▼
Integration Boundary
        │
        ▼
General Ledger Application
```

Repository فقط برای Persistence داخل همان Service Boundary است.

---

# 35. Architectural Rules

قوانین زیر پذیرفته می‌شوند:

### Rule 1

Domain نباید به JPA/Hibernate وابسته باشد.

### Rule 2

Repository Port باید در لایه داخلی مناسب تعریف شود.

### Rule 3

Repository Implementation در Infrastructure قرار گیرد.

### Rule 4

Persistence Entity در Infrastructure قرار گیرد.

### Rule 5

Domain Entity و Persistence Entity در حالت پیش‌فرض جدا باشند، مگر با تصمیم آگاهانه.

### Rule 6

Mapping بین Domain و Persistence باید در Infrastructure انجام شود.

### Rule 7

یک Module نباید مستقیماً Table داخلی Module دیگر را Query یا Modify کند.

### Rule 8

یک Module نباید Persistence Entity داخلی Module دیگر را مصرف کند.

### Rule 9

یک Module نباید Repository Implementation مربوط به Module دیگر را مصرف کند.

### Rule 10

Transaction Boundary بر اساس Use Case و Consistency Requirement تعیین شود.

### Rule 11

Aggregate Root مرجع اصلی Persistence Aggregate باشد.

### Rule 12

Entity داخلی Aggregate Repository مستقل نداشته باشد، مگر اینکه Aggregate Root مستقل شود.

### Rule 13

Queryهای پیچیده می‌توانند از Query Adapter یا Read Model استفاده کنند.

### Rule 14

CQRS به‌صورت موضعی و بر اساس نیاز واقعی استفاده شود.

### Rule 15

Database Migrationها باید Versioned و قابل تکرار باشند.

### Rule 16

Business Rule نباید صرفاً در Database پیاده‌سازی شود.

### Rule 17

Database Constraintها باید برای Data Integrity مناسب استفاده شوند.

### Rule 18

Optimistic Locking در Aggregateهای دارای Concurrent Update در نظر گرفته شود.

### Rule 19

Audit Concern نباید Business Logic اصلی Domain را آلوده کند.

### Rule 20

Soft Delete نباید به‌صورت سراسری و بدون Business Requirement اعمال شود.

### Rule 21

Repository نباید به Generic Database Gateway تبدیل شود.

### Rule 22

N+1 Query و Unintended Database Access باید کنترل شوند.

### Rule 23

Database Ownership در سطح Module رعایت شود.

### Rule 24

Physical Database Sharing به معنی Shared Domain Model یا Shared Data Ownership نیست.

### Rule 25

JournalEntry Core Aggregate نباید به Persistence Implementation مربوط به Supporting Moduleها وابسته شود.

### Rule 26

Repository نباید به‌عنوان Integration Mechanism بین Microserviceها استفاده شود.

---

# 36. Architectural Enforcement

اصول این ADR باید تا حد امکان توسط Architectural Test و Code Review enforce شوند.

نمونه قوانین:

```text
Domain
    must not depend on
JPA / Hibernate
```

```text
Domain
    must not depend on
Infrastructure
```

```text
Account Domain
    must not access
AccountHeadJpaRepository
```

```text
JournalEntry Domain
    must not access
AccountJpaEntity
```

```text
Module A
    must not access
Module B Persistence Entity
```

```text
Module A
    must not access
Module B Table
```

```text
Application
    may depend on
Repository Port
```

```text
Infrastructure
    implements
Repository Port
```

```text
Query Model
    may bypass
Domain Aggregate
```

برای این منظور استفاده از:

```text
ArchUnit
Code Review
Integration Tests
Persistence Tests
```

توصیه می‌شود.

---

# 37. پیامدهای مثبت

این تصمیم باعث می‌شود:

* Domain از Database مستقل باقی بماند.
* Domain از JPA/Hibernate مستقل باقی بماند.
* تغییر Persistence Technology ساده‌تر شود.
* Business Model تحت تأثیر مستقیم Schema قرار نگیرد.
* Aggregate Boundaryها حفظ شوند.
* Moduleها مالک داده‌های خود باشند.
* Cross-Module Database Coupling کاهش یابد.
* Core Domain یعنی JournalEntry بهتر محافظت شود.
* Query و Command بتوانند به‌صورت مستقل بهینه شوند.
* CQRS موضعی امکان‌پذیر باشد.
* Data Integrity در دو سطح Domain و Database کنترل شود.
* Auditability سیستم مالی حفظ شود.

---

# 38. پیامدهای منفی

این معماری هزینه‌هایی دارد:

* Mapping بین Domain و Persistence
* افزایش تعداد کلاس‌ها
* افزایش کد Infrastructure
* پیچیدگی بیشتر Repositoryها
* نیاز به طراحی دقیق Transaction Boundary
* نیاز به مدیریت Migration
* نیاز به کنترل Performance Query
* نیاز به طراحی دقیق Data Ownership
* احتمال ایجاد Over-Engineering در صورت جداسازی غیرضروری

این هزینه‌ها با توجه به ماهیت مالی و Core بودن General Ledger پذیرفته می‌شوند، اما جداسازی باید متناسب با Complexity واقعی Business انجام شود.

---

# 39. تصمیم نهایی

> **Persistence به‌عنوان یک Concern متعلق به Infrastructure در نظر گرفته می‌شود. Domain و Application از طریق Repository Port با Persistence تعامل می‌کنند و Implementation این Portها در Infrastructure قرار می‌گیرد.**

> **تکنولوژی اصلی Persistence رابطه‌ای JPA/Hibernate است، اما Domain نباید به JPA/Hibernate یا سایر جزئیات Persistence وابسته شود.**

> **Domain Model و Persistence Model در حالت پیش‌فرض از یکدیگر جدا هستند و Mapping بین آنها در Infrastructure انجام می‌شود. با این حال، جداسازی Domain Entity و Persistence Entity یک الزام مطلق نیست و در موارد ساده می‌تواند با تصمیم آگاهانه کاهش یابد.**

> **هر Module مالک Logical Data مربوط به Business Responsibility خود است. استفاده از یک Database فیزیکی مشترک در General Ledger Microservice مجاز است، اما هیچ Moduleای مجاز به دسترسی مستقیم به Table، Persistence Entity یا Repository Implementation مربوط به Module دیگر نیست.**

> **Transaction Boundary بر اساس Use Case و Business Consistency Requirement تعیین می‌شود و Aggregate همچنان مرز اصلی Consistency در Domain باقی می‌ماند.**

> **برای Commandها، Domain Aggregate و Repository مسیر اصلی تغییر State هستند؛ برای Queryهای پیچیده، Read Model یا Query Adapter می‌تواند بدون Load کردن کامل Aggregate استفاده شود. CQRS تنها به‌صورت موضعی و بر اساس نیاز واقعی استفاده خواهد شد.**

> **Database Integrity با Constraintهای مناسب حفظ می‌شود، اما Database تنها محل اجرای Business Rule نیست. Business Invariantهای Domain باید توسط Domain Model enforce شوند.**

> **Repository مسئول Persistence است و نباید به Integration Mechanism یا Generic Database Gateway تبدیل شود.**

> **Persistence Strategy باید از Core Domain، به‌خصوص JournalEntry، در برابر وابستگی به Implementation داخلی Supporting Moduleها محافظت کند.**

---

# 40. مدل نهایی Persistence

```text
                  GENERAL LEDGER MICROSERVICE
                              │
              ┌───────────────┼────────────────┐
              │               │                │
              ▼               ▼                ▼
       Account Head        Account        Journal Entry
          Module           Module             Module
              │               │                │
              ▼               ▼                ▼
      AccountHead         Account         JournalEntry
       Aggregate         Aggregate         Aggregate
                                              ⭐ Core
              │               │                │
              └───────────────┼────────────────┘
                              │
                              ▼
                       Repository Port
                              ▲
                              │
                    ┌─────────┴─────────┐
                    │   Infrastructure  │
                    │                   │
                    │ Adapter           │
                    │ Mapper            │
                    │ Persistence Entity│
                    │ JPA / Hibernate   │
                    └─────────┬─────────┘
                              │
                              ▼
                    General Ledger Database
                              │
              ┌───────────────┼────────────────┐
              │               │                │
              ▼               ▼                ▼
       Account Head Data   Account Data   Journal Entry Data
```

---

# 41. رابطه با ADRهای مرتبط

## ADR-0003 — General Ledger Boundary

ADR-0003 مرز سطح بالای General Ledger را مشخص می‌کند:

```text
General Ledger
      │
      ▼
General Ledger Bounded Context
      │
      ▼
General Ledger Microservice
```

ADR-0007 این مرز را تغییر نمی‌دهد و صرفاً استراتژی Persistence داخل این Microservice را مشخص می‌کند.

---

## ADR-0004 — Dependency Direction و Internal Communication

ADR-0004 جهت Dependency و نحوه ارتباط داخلی را مشخص می‌کند:

```text
Application
     │
     ├── Internal Contract / Port
     │
     ├── Repository Port
     │
     └── Domain
```

و:

```text
Infrastructure
     │
     └── implements technical abstractions
```

ADR-0007 همین اصول را در حوزه Persistence اعمال می‌کند.

---

## ADR-0005 — Domain-Driven Design

ADR-0005 اصول کلی DDD و Strategic/Tactical Modeling را مشخص می‌کند:

```text
Problem Space
      │
      ▼
Subdomain
      │
      ▼
Bounded Context
      │
      ▼
Module
      │
      ▼
Aggregate
      │
      ▼
Entity / Value Object / Domain Service
```

ADR-0007 مشخص می‌کند که Persistence چگونه باید بدون نقض این Domain Boundaries پیاده‌سازی شود.

---

## ADR-0006 — Domain Model Strategy

ADR-0006 Rich Domain Model و استقلال Domain از Persistence را مشخص می‌کند.

ADR-0007 این تصمیم را به Persistence Architecture تبدیل می‌کند:

```text
Domain Model
      │
      ▼
Repository Port
      ▲
      │
Infrastructure Adapter
      │
      ▼
Persistence Model
      │
      ▼
Database
```

بنابراین:

```text
ADR-0003
=
General Ledger Boundary

ADR-0004
=
Dependency Direction / Internal Communication

ADR-0005
=
DDD Principles / Strategic & Tactical Modeling

ADR-0006
=
Domain Model Strategy / Rich Domain Model

ADR-0007
=
Persistence Strategy
```

---

# 42. Status

**Status: Accepted**

این ADR باید به‌عنوان مبنای تصمیم‌گیری برای موارد زیر استفاده شود:

* Repository Design
* Persistence Adapter
* JPA/Hibernate Usage
* Domain/Persistence Mapping
* Database Schema
* Database Ownership
* Transaction Boundary
* Aggregate Persistence
* Query Model
* Read Model
* CQRS
* Database Migration
* Data Integrity
* Optimistic Locking
* Auditing
* Soft Delete
* Persistence Performance
* Cross-Module Data Access

هر تصمیمی که اصول این ADR را تغییر دهد باید از طریق یک Architectural Decision جدید ثبت شود یا این ADR به‌صورت رسمی اصلاح گردد.

---

# 43. منابع (References)

## 43.1 Eric Evans — Domain-Driven Design

**Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of Software***

مبنای مفاهیم:

* Domain Model
* Entity
* Value Object
* Aggregate
* Repository
* Bounded Context
* Domain Service
* Strategic / Tactical Design

ناشر: Addison-Wesley Professional, 2003.

---

## 43.2 Vaughn Vernon — Implementing Domain-Driven Design

**Vaughn Vernon — *Implementing Domain-Driven Design***

مبنای تصمیم‌های مرتبط با:

* Entity
* Value Object
* Aggregate
* Repository
* Domain Service
* Domain Event
* Module
* Application Layer
* Persistence Boundary
* Bounded Context

---

## 43.3 Vaughn Vernon — Domain-Driven Design Distilled

**Vaughn Vernon — *Domain-Driven Design Distilled***

مبنای تکمیلی برای:

* Strategic Design
* Subdomains
* Core Domain
* Supporting Subdomain
* Bounded Context
* Aggregate
* Domain Event
* Tactical Design

---

## 43.4 Martin Fowler — Patterns of Enterprise Application Architecture

**Martin Fowler — *Patterns of Enterprise Application Architecture***

مبنای تصمیم‌های مرتبط با:

* Domain Model
* Repository
* Data Mapper
* Unit of Work
* Service Layer
* Value Object
* Layering
* Persistence Separation

---

## 43.5 Robert C. Martin — Clean Architecture

**Robert C. Martin — *Clean Architecture: A Craftsman's Guide to Software Structure and Design***

مبنای اصول:

* Dependency Rule
* Dependency Inversion
* Separation of Concerns
* Business Rule Independence
* Framework Independence
* Infrastructure Independence

---

## 43.6 Mark Richards & Neal Ford — Fundamentals of Software Architecture

**Mark Richards, Neal Ford — *Fundamentals of Software Architecture***

مبنای تکمیلی برای:

* Architecture Characteristics
* Modularity
* Coupling
* Cohesion
* Architectural Trade-offs
* Evolutionary Architecture

---

# 44. جمع‌بندی منابع مورد استفاده

| موضوع                      | منبع اصلی                                  |
| -------------------------- | ------------------------------------------ |
| Domain Model               | Eric Evans                                 |
| Entity                     | Eric Evans / Vaughn Vernon                 |
| Value Object               | Eric Evans / Vaughn Vernon                 |
| Aggregate                  | Eric Evans / Vaughn Vernon                 |
| Aggregate Boundary         | Vaughn Vernon                              |
| Repository                 | Eric Evans / Martin Fowler / Vaughn Vernon |
| Persistence Separation     | Martin Fowler / Robert C. Martin           |
| Data Mapper                | Martin Fowler                              |
| Transaction / Unit of Work | Martin Fowler / Vaughn Vernon              |
| Domain Service             | Eric Evans / Vaughn Vernon                 |
| Domain Event               | Vaughn Vernon                              |
| Application Layer          | Vaughn Vernon / Martin Fowler              |
| Dependency Direction       | Robert C. Martin                           |
| Module Ownership           | Vaughn Vernon                              |
| Core Domain Protection     | Eric Evans / Vaughn Vernon                 |
| CQRS / Read Model          | Martin Fowler / Vaughn Vernon              |
| Architecture Trade-offs    | Mark Richards / Neal Ford                  |

> **این منابع مبنای نظری و معماری این ADR هستند؛ اما تصمیم نهایی متناسب با Boundaryها، Business Rules، Aggregateها و ساختار واقعی General Ledger Microservice اتخاذ شده است.**
