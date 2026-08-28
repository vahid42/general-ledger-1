# ADR-0005 — طراحی مبتنی بر دامنه (Domain-Driven Design)

* **Status:** Accepted
* **Date:** 2026-08-22
* **Related ADRs:**

  * ADR-0003
  * ADR-0004
  * ADR-0011

---

# 1. مسئله و زمینه (Context)

سیستم موردنظر یک سامانه مالی مبتنی بر **General Ledger (دفترکل)** است و بخش مهمی از پیچیدگی آن ناشی از Business Ruleها و مفاهیم Domain است، نه صرفاً مسائل فنی و Persistence.

در Domain دفترکل مفاهیمی مانند:

* Account
* Account Head
* Journal Entry
* Debit
* Credit
* Balance
* Currency
* Branch
* Financial Rules
* Posting Rules

دارای معنا و رفتار کسب‌وکاری هستند.

اگر مدل نرم‌افزار صرفاً بر اساس Database Schema، Framework یا ساختار تکنولوژیک طراحی شود، احتمال ایجاد مشکلات زیر افزایش پیدا می‌کند:

* پراکنده شدن Business Ruleها
* ایجاد Serviceهای بزرگ و چندمنظوره
* وابستگی Domain به Persistence و Infrastructure
* ایجاد Anemic Domain Model
* تبدیل Entityهای Domain به Data Container
* افزایش Coupling بین Business Boundaryها
* دشوار شدن تغییر قوانین کسب‌وکار
* کاهش قابلیت فهم مدل سیستم
* ایجاد وابستگی مستقیم بین مدل‌های داخلی Moduleها

بنابراین تصمیم گرفته می‌شود که طراحی نرم‌افزار بر اساس **Domain-Driven Design (DDD)** انجام شود.

---

# 2. تصمیم (Decision)

سیستم بر اساس اصول **Domain-Driven Design** طراحی خواهد شد.

DDD مبنای تصمیم‌گیری برای موارد زیر است:

* شناخت و مدل‌سازی Domain
* Ubiquitous Language
* Subdomainها
* Bounded Context
* Business Boundary
* Entity
* Value Object
* Aggregate
* Aggregate Root
* Domain Service
* Repository
* Domain Event
* Module Boundary
* Internal Contract / Port

DDD در این پروژه صرفاً به معنی استفاده از تعدادی Pattern با نام‌های DDD نیست.

اصل تصمیم:

> **مدل نرم‌افزار باید تا حد امکان نمایانگر Business Domain و Business Rules واقعی باشد.**

ساختار مفهومی:

```text
Business Domain
      │
      ▼
Strategic Domain Model
      │
      ├── Subdomain
      │
      └── Bounded Context
              │
              ▼
       Domain / Business Boundary
              │
              ▼
            Module
              │
              ▼
          Application
              │
              ▼
        Infrastructure
```

بنابراین:

> **ساختار فنی پروژه باید تابع مدل Domain باشد، نه اینکه Domain مجبور شود خود را با ساختار Database یا تکنولوژی تطبیق دهد.**

---

# 3. جایگاه DDD در معماری فعلی

مطابق ADR-0003 و ADR-0004، ساختار فعلی General Ledger به شکل زیر است:

```text
General Ledger
│
├── Subdomain: Account Head
├── Subdomain: Account
└── Subdomain: Journal Entry ⭐ Core
        │
        ▼
General Ledger Bounded Context
        │
        ▼
General Ledger Microservice
        │
        ├── Account Head Module
        │      └── AccountHead Aggregate
        │
        ├── Account Module
        │      └── Account Aggregate
        │
        └── Journal Entry Module
               └── JournalEntry Aggregate ⭐ Core
```

بنابراین این ADR باید با تمایزهای زیر سازگار باقی بماند:

```text
Subdomain
    ≠
Bounded Context
    ≠
Module
    ≠
Microservice
```

در معماری فعلی:

```text
3 Subdomains
      │
      ▼
1 Bounded Context
      │
      ▼
1 Microservice
      │
      ▼
3 Domain-oriented Modules
      │
      ▼
3 Aggregate Roots
```

این رابطه یک تصمیم معماری خاص این سیستم است و یک قانون عمومی DDD محسوب نمی‌شود.

---

# 4. هدف استفاده از DDD

هدف از استفاده از DDD صرفاً استفاده از مفاهیمی مانند:

```text
Entity
Aggregate
Value Object
Repository
Domain Service
Domain Event
```

نیست.

هدف اصلی:

> **Business Model باید در ساختار نرم‌افزار قابل مشاهده و قابل فهم باشد.**

بنابراین تصمیم‌های Domain ابتدا بر اساس موارد زیر گرفته می‌شوند:

```text
Business Responsibility
Business Rule
Business Capability
Domain Boundary
Consistency Boundary
Ubiquitous Language
```

و سپس ساختار فنی بر اساس این تصمیم‌ها ایجاد می‌شود.

---

# 5. Ubiquitous Language

در General Ledger از **Ubiquitous Language** استفاده خواهد شد.

اصطلاحات مهم Business Domain باید تا حد امکان با معنای یکسان در موارد زیر استفاده شوند:

* مستندات
* ADRها
* Domain Model
* کد
* کلاس‌ها
* متدها
* APIها
* تست‌ها
* Eventها

برای مثال:

```text
Account
AccountHead
JournalEntry
```

نباید در بخش‌های مختلف سیستم با نام‌های متفاوتی برای یک مفهوم واحد نمایش داده شوند.

مدل:

```text
Business Term
      │
      ├── Documentation
      ├── Domain Model
      ├── Code
      ├── API
      └── Tests
```

Ubiquitous Language باید در محدوده همان Bounded Context معنا و consistency مشخص خود را داشته باشد.

---

# 6. Subdomain

در سطح Strategic DDD، General Ledger دارای سه Subdomain تصمیم‌گیری‌شده است:

```text
General Ledger
│
├── Account Head
│      └── Supporting Subdomain
│
├── Account
│      └── Supporting Subdomain
│
└── Journal Entry
       └── Core Subdomain ⭐
```

## 6.1 Account Head

Account Head یک Supporting Subdomain است و مسئول مفاهیمی مانند:

* ساختار سرفصل‌ها
* طبقه‌بندی
* Hierarchy
* جایگاه حساب در ساختار دفترکل
* قواعد مرتبط با ساختار سرفصل

است.

## 6.2 Account

Account یک Supporting Subdomain است و مسئول مفاهیمی مانند:

* Account Identity
* Account Type
* Account Status
* Account Attributes
* Account-specific Business Rules

است.

## 6.3 Journal Entry

Journal Entry به‌عنوان **Core Subdomain** شناخته می‌شود.

این بخش مرکز اصلی Business Complexity و Business Value در General Ledger است.

مسئولیت‌های اصلی آن شامل مواردی مانند:

* Journal Entry creation
* Debit / Credit validation
* Balancing
* Journal Line validation
* Posting Rules
* Posting State
* Posting

است.

بنابراین:

> **Journal Entry باید بالاترین سطح توجه Domain Modeling را دریافت کند.**

---

# 7. Subdomain ≠ Bounded Context

وجود سه Subdomain به معنی ایجاد سه Bounded Context نیست.

در معماری فعلی:

```text
General Ledger

├── Account Head
├── Account
└── Journal Entry
        │
        ▼
General Ledger Bounded Context
```

سه Subdomain در یک Bounded Context قرار دارند، زیرا:

* متعلق به یک Problem Space هستند.
* Ubiquitous Language آنها ارتباط نزدیکی دارد.
* Account و Account Head در مدل Journal Entry نقش پشتیبان دارند.
* ایجاد Context مستقل برای هر Subdomain در وضعیت فعلی مرز مدل واقعی ایجاد نمی‌کند.
* ایجاد Bounded Context مستقل در این مرحله پیچیدگی غیرضروری ایجاد می‌کند.

این تصمیم به معنی حذف Business Boundaryهای داخلی نیست.

هر Module همچنان باید مالک مدل داخلی و مسئولیت Domain خود باقی بماند.

---

# 8. Bounded Context

Bounded Context مرز مدل Domain و Ubiquitous Language است.

در معماری فعلی:

```text
General Ledger Bounded Context
│
├── Account Head
├── Account
└── Journal Entry
```

Bounded Context فعلی:

* یک Domain Model یکپارچه دارد.
* یک Ubiquitous Language دارد.
* مسئولیت مشخص در Problem Space دارد.
* به‌صورت یک Microservice مستقل deploy می‌شود.

اما:

> **Bounded Context و Microservice از نظر مفهومی یکی نیستند.**

در این سیستم رابطه زیر یک تصمیم معماری است:

```text
General Ledger Bounded Context
              │
              ▼
General Ledger Microservice
```

---

# 9. Module Boundary

در سطح کد، Domain بر اساس Business Responsibility به Moduleهای اصلی تقسیم می‌شود:

```text
General Ledger Microservice
│
├── account-head
├── account
└── journal-entry
```

این Moduleها:

* Microservice مستقل نیستند.
* Runtime مستقل ندارند.
* Deployment مستقل ندارند.
* در همان General Ledger Microservice اجرا می‌شوند.
* مدل داخلی خود را مالک هستند.
* باید Boundary مشخص داشته باشند.

بنابراین:

```text
Module
    ≠
Microservice
```

و:

```text
Module
    ≠
Bounded Context
```

Module یک Boundary ساختاری و کدی است که برای حفظ Cohesion و کاهش Coupling استفاده می‌شود.

---

# 10. Entity

برای مفاهیمی که Identity مستقل دارند از Entity استفاده می‌شود.

Entity باید:

* Identity مشخص داشته باشد.
* در طول زمان قابل شناسایی باشد.
* رفتار مرتبط با خودش را داشته باشد.
* Invariantهای مربوط به خودش را حفظ کند.

مثال:

```text
Account
├── AccountId
├── AccountCode
├── Name
├── Type
└── Status
```

Entity نباید صرفاً یک Data Container باشد.

مدل ترجیحی:

```text
Account
   │
   ├── Behavior
   ├── Business Rules
   └── Invariants
```

به جای:

```text
Account
   │
   └── Getters / Setters only
```

رفتار باید در جایی قرار گیرد که Business Ownership آن رفتار را دارد.

---

# 11. Value Object

برای مفاهیمی که Identity مستقل ندارند و بر اساس Value خود معنا پیدا می‌کنند، از Value Object استفاده می‌شود.

نمونه‌های احتمالی:

```text
Money
Currency
AccountCode
BranchId
AccountId
```

برای مثال:

```text
Money
├── amount
└── currency
```

Value Object باید تا حد امکان:

* Immutable باشد.
* Equality مبتنی بر Value داشته باشد.
* Validation مربوط به Value خودش را حفظ کند.
* رفتار مرتبط با مفهوم خودش را داشته باشد.

مثلاً:

```text
Money
   │
   ├── amount
   ├── currency
   └── monetary rules
```

---

# 12. Aggregate

Aggregate یک **Consistency Boundary** در Domain است.

Aggregate برای گروه‌بندی اشیایی استفاده می‌شود که باید یک مجموعه مشخص از Invariantها را به صورت یک واحد consistency حفظ کنند.

هر Aggregate دارای یک:

```text
Aggregate Root
```

است.

مدل مفهومی:

```text
Aggregate
┌───────────────────────────┐
│                           │
│     Aggregate Root        │
│          │                │
│          ▼                │
│       Entity              │
│          │                │
│          ▼                │
│     Value Object          │
│                           │
└───────────────────────────┘
```

دسترسی خارجی به Entityهای داخلی Aggregate باید از طریق Aggregate Root انجام شود.

Aggregate نباید صرفاً بر اساس:

* Database Table
* Foreign Key
* ORM Relationship
* Object Graph

طراحی شود.

معیار اصلی:

```text
Business Invariant
+
Consistency Requirement
```

است.

---

# 13. Aggregateهای General Ledger

مطابق Baseline معماری، General Ledger دارای سه Aggregate Root اصلی است:

```text
General Ledger
│
├── AccountHead Aggregate
│
├── Account Aggregate
│
└── JournalEntry Aggregate ⭐ Core
```

## 13.1 AccountHead Aggregate

مسئول Invariantهای مربوط به ساختار و قوانین Account Head است.

## 13.2 Account Aggregate

مسئول Invariantهای مربوط به Account است.

## 13.3 JournalEntry Aggregate

Core Aggregate سیستم است.

مسئولیت‌های اصلی آن شامل مواردی مانند:

```text
JournalEntry
├── Debit / Credit rules
├── Balancing rules
├── Journal Line rules
├── Posting rules
├── Posting state
└── Posting behavior
```

اصل:

> **Core Business Rules مربوط به Journal Entry باید تا حد امکان در JournalEntry Aggregate و Domain Model مرتبط با آن باقی بمانند.**

---

# 14. Aggregate ≠ Domain Service

وجود Aggregateهای:

```text
AccountHead
Account
JournalEntry
```

به معنی ایجاد خودکار:

```text
AccountHeadService
AccountService
JournalEntryService
```

به‌عنوان محل اصلی Business Logic نیست.

اصل:

```text
Business Rule
      │
      ▼
Aggregate / Entity / Value Object
```

اگر Business Rule ذاتاً متعلق به یک Aggregate، Entity یا Value Object مشخص نباشد، Domain Service می‌تواند مورد استفاده قرار گیرد.

---

# 15. Aggregate Interaction

Aggregateها باید تا حد امکان مستقل باقی بمانند.

Aggregate نباید:

* Aggregate دیگر را به‌عنوان Object Graph داخلی نگه دارد.
* Repository Aggregate دیگر را مستقیماً فراخوانی کند.
* Infrastructure Aggregate دیگر را بشناسد.
* Entity داخلی Aggregate دیگر را مصرف کند.

برای ارتباط بین Aggregateها ترجیح با Identity/Reference است:

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

به جای:

```text
Account
   │
   └── AccountHeadEntity
```

این تصمیم با اصل Aggregate Boundary و Reference by Identity سازگار است.

---

# 16. Repository

Repository یک Abstraction برای Persistence Access مربوط به Aggregate است.

نمونه:

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

مدل:

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
Database
```

Implementation مربوط به Persistence در Infrastructure قرار می‌گیرد.

برای مثال:

```text
Domain
└── AccountRepository

Infrastructure
└── JpaAccountRepository
```

Domain نباید به Implementationهای زیر وابسته شود:

```text
JPA
Hibernate
Spring Data
Database Driver
SQL
```

---

# 17. Repository ≠ Internal Contract / Port

Repository تنها یکی از انواع Port است.

دو مفهوم باید جدا باقی بمانند:

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

اما:

```text
AccountHeadPort
```

می‌تواند Contract یک Business Capability باشد که توسط یک Module دیگر ارائه می‌شود.

بنابراین:

> **هر Interface داخل Domain یا Application الزاماً Repository نیست.**

---

# 18. Repository ابزار Integration نیست

Repository متعلق به یک Module نباید به عنوان روش عمومی ارتباط با Module دیگر استفاده شود.

نامطلوب:

```text
AccountDomainService
        │
        ▼
AccountHeadRepository
```

اگر هدف این Dependency صرفاً هماهنگی یک Use Case باشد.

مدل ترجیحی:

```text
CreateAccountUseCase
        │
        ├──► AccountHeadPort
        │
        ├──► Account Domain
        │
        └──► AccountRepository
```

اصل:

> **Repository مسئول Persistence است؛ Internal Contract / Port مسئول ارائه Capability یا ارتباط داخلی است.**

---

# 19. Domain Service

Domain Service زمانی استفاده می‌شود که یک Business Operation یا Business Rule:

* متعلق به یک Entity مشخص نباشد.
* متعلق به یک Aggregate مشخص نباشد.
* متعلق به یک Value Object مشخص نباشد.
* اما همچنان بخشی واقعی از Business Domain باشد.

Domain Service باید:

* Business Meaning مشخص داشته باشد.
* Stateless یا تا حد امکان مستقل از Infrastructure باشد.
* Business Decision یا Domain Operation مشخصی را نمایندگی کند.

مثال مفهومی:

```text
JournalEntryDomainService
        │
        └── Domain-specific decision
```

Domain Service نباید به:

```text
GenericService
CommonService
Manager
Coordinator
```

تبدیل شود.

---

# 20. Application Service / Use Case

Application Layer مسئول اجرای Use Case و Orchestration است.

مسئولیت‌های اصلی:

* اجرای Use Case
* کنترل Flow
* هماهنگی چند Aggregate
* فراخوانی Repository Port
* فراخوانی Internal Contract / Port
* تعیین Transaction Boundary
* هماهنگی عملیات

مدل:

```text
Presentation
      │
      ▼
Application / Use Case
      │
      ├──► Aggregate
      ├──► Repository Port
      ├──► Internal Contract / Port
      └──► Domain Service
```

اصل:

> **Application Orchestrates; Domain Decides.**

---

# 21. Domain Service در مقابل Application Service

این دو مسئولیت باید کاملاً جدا باقی بمانند.

## Application Service

```text
Application Service
        │
        ├── Use Case
        ├── Orchestration
        ├── Transaction Boundary
        ├── Coordination
        └── Port invocation
```

## Domain Service

```text
Domain Service
        │
        └── Business Rule / Business Decision
```

بنابراین:

```text
Application
    =
Coordination
```

و:

```text
Domain
    =
Business Decision
```

Domain Service نباید صرفاً برای اینکه چند Module را به هم وصل کنیم ایجاد شود.

---

# 22. مثال: ایجاد Account زیر Account Head

فرض کنیم Use Case زیر وجود دارد:

> ایجاد یک Account زیر یک Account Head مشخص.

مدل پیشنهادی:

```text
CreateAccountUseCase
        │
        ├──► AccountHeadPort
        │       │
        │       └── بررسی Capability موردنیاز
        │
        ├──► Account Domain
        │       │
        │       └── اجرای Account Business Rules
        │
        └──► AccountRepository
                │
                └── Persistence
```

در این مدل:

```text
Presentation
    =
Request Handling

Application
    =
Use Case Orchestration

AccountHead
    =
Account Head Business Responsibility

Account
    =
Account Business Responsibility

Repository
    =
Persistence

Infrastructure
    =
Technical Implementation
```

---

# 23. Business Rule در مقابل Orchestration

فرض کنیم Rule این باشد:

> Account باید به یک Account Head معتبر تعلق داشته باشد.

نباید صرفاً به دلیل این Rule، `Account` مستقیماً Repository مربوط به Account Head را صدا بزند.

مدل ترجیحی:

```text
Application
      │
      ├──► AccountHeadPort
      │
      ▼
Account Domain
      │
      └── Enforce Account Business Rule
```

اصل مهم:

> **داده یا Capability موردنیاز برای تصمیم Domain می‌تواند توسط Application فراهم شود، اما خود Business Decision باید در Domain باقی بماند.**

---

# 24. جلوگیری از Anemic Domain Model

در این پروژه تلاش می‌شود Domain Model به مجموعه‌ای از DTOها و Getter/Setterها تبدیل نشود.

مدل ترجیحی:

```text
Account
    │
    ├── open()
    ├── close()
    ├── changeName()
    └── enforceAccountRules()
```

به جای:

```text
AccountService
    │
    ├── openAccount()
    ├── closeAccount()
    ├── changeAccountName()
    └── validateAccount()
```

اما این تصمیم به معنی قرار دادن همه Logic در Entity نیست.

رفتار باید بر اساس Business Ownership در جای مناسب قرار گیرد:

```text
Entity
Aggregate
Value Object
Domain Service
```

---

# 25. Domain Event

Domain Event باید بیانگر یک **Business Fact معنادار** باشد.

مثال:

```text
AccountCreated
JournalEntryPosted
```

ساختار:

```text
Aggregate
    │
    ▼
Domain Event
    │
    ▼
Event Handler
```

Domain Event نباید صرفاً یک Event تکنیکی مانند:

```text
DatabaseUpdated
EntitySaved
RowChanged
```

باشد.

---

# 26. Internal Domain Event

در داخل General Ledger Microservice می‌توان از Internal Domain Event برای انتشار Business Fact استفاده کرد.

مثال:

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

Producer نباید برای انتشار Event مجبور باشد Consumerها را مستقیماً بشناسد.

---

# 27. Internal Domain Event ≠ Integration Event

این دو مفهوم باید کاملاً از یکدیگر جدا باشند.

## Internal Domain Event

در داخل همان Microservice:

```text
General Ledger Microservice
        │
        ▼
Internal Domain Event
        │
        ▼
Internal Handler
```

## Integration Event

عبور از مرز Microservice:

```text
General Ledger Microservice
        │
        ▼
Integration Event
        │
        ▼
Message Broker / Integration Boundary
        │
        ▼
Another Microservice
```

بنابراین:

> **Internal Domain Event برای ارتباط داخل همان Microservice است.**

و:

> **Integration Event برای ارتباط از مرز Microservice استفاده می‌شود.**

---

# 28. Direct Contract در مقابل Domain Event

نوع ارتباط باید بر اساس نیاز Business انتخاب شود.

## Internal Contract / Port

زمانی مناسب است که:

* پاسخ فوری لازم باشد.
* Use Case به نتیجه نیاز داشته باشد.
* Business Dependency مستقیم باشد.
* Consistency لحظه‌ای اهمیت داشته باشد.

مثال:

```text
CreateAccountUseCase
        │
        ▼
AccountHeadPort
        │
        ▼
isValid(accountHeadId)
```

## Domain Event

زمانی مناسب است که:

* یک Business Fact رخ داده باشد.
* چند Consumer ممکن است به آن واکنش نشان دهند.
* Producer نباید Consumerها را مستقیماً بشناسد.
* پردازش می‌تواند مستقل باشد.

مثال:

```text
Account
   │
   ▼
AccountCreated
   │
   ▼
Internal Event Dispatcher
```

اصل:

> **Event برای انتشار Business Fact است، نه جایگزین عمومی Method Call.**

---

# 29. مالکیت Domain Model

هر Module مالک مدل داخلی خودش است.

مثلاً:

```text
Account Module
```

نباید مستقیماً:

```text
AccountHeadEntity
```

را مصرف کند.

نامطلوب:

```text
Account
    │
    X
    ▼
AccountHeadEntity
```

مدل ترجیحی:

```text
Account
    │
    └── AccountHeadId
```

یا در سطح Application:

```text
Application
    │
    ▼
AccountHeadPort
```

هدف:

> **هر Module باید بتواند مدل داخلی خود را بدون ایجاد تغییر زنجیره‌ای در سایر Moduleها تکامل دهد.**

---

# 30. Shared Database ≠ Shared Domain Ownership

ممکن است General Ledger از یک Database مشترک استفاده کند:

```text
General Ledger Database
│
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

اما این به معنی مالکیت مشترک Domain Model نیست.

هر Module همچنان مالک منطقی داده و Business Responsibility مربوط به خود است.

بنابراین:

```text
Account Module
      │
      X
      ▼
SELECT FROM AccountHeadInternalTable
```

به‌عنوان Business Integration مجاز نیست.

ارتباط باید از طریق Contract یا Capability مناسب انجام شود.

---

# 31. Direct Database Access ممنوع

Moduleها نباید Table داخلی Module دیگر را مستقیماً مصرف کنند.

نامطلوب:

```text
Journal Entry Module
        │
        X
        ▼
Account Internal Table
```

یا:

```text
Account Module
        │
        X
        ▼
Account Head Internal Table
```

این کار باعث:

* Coupling پنهان
* کاهش Encapsulation
* افزایش Architecture Drift
* دشوار شدن Evolution

می‌شود.

---

# 32. DDD و Dependency Direction

این تصمیم با ADR-0004 هماهنگ است.

Dependency باید به سمت Abstraction و Business Responsibility باشد.

مدل ترجیحی:

```text
Presentation
      │
      ▼
Application
      │
      ├──► Internal Contract / Port
      │
      ├──► Repository Port
      │
      ▼
Domain
```

Infrastructure:

```text
Infrastructure
      │
      ├── implements Repository Port
      └── implements technical adapters
```

نامطلوب:

```text
Domain
   │
   X
   ▼
Infrastructure
```

یا:

```text
AccountDomain
   │
   X
   ▼
AccountHeadJpaRepository
```

اصل:

> **Dependency باید به سمت Abstraction و Business Responsibility باشد، نه به سمت Implementation و Infrastructure.**

---

# 33. استقلال Domain از Persistence

مدل Domain نباید صرفاً بر اساس Database Schema طراحی شود.

مدل نامطلوب:

```text
Database Table
      │
      ▼
Domain Entity
```

مدل ترجیحی:

```text
Business Model
      │
      ▼
Domain Model
      │
      ▼
Persistence Model
      │
      ▼
Database
```

در صورت نیاز، Domain Model و Persistence Model از یکدیگر جدا می‌شوند.

هدف:

> تغییر Persistence Model نباید الزاماً باعث تغییر Business Model شود.

---

# 34. Mapping بین مدل‌ها

Mapping در مرزهای معماری مجاز و در صورت وجود تفاوت واقعی بین مدل‌ها مطلوب است.

برای مثال:

```text
Presentation DTO
      │
      ▼
Application Input
      │
      ▼
Domain Model
```

و:

```text
Domain Model
      │
      ▼
Persistence Model
      │
      ▼
Database
```

همچنین در ارتباط بین Business Boundaryها، Contract مستقل می‌تواند مدل موردنیاز Consumer را ارائه کند.

هدف:

> **مدل داخلی یک Boundary نباید صرفاً به دلیل راحتی Mapping در اختیار Boundary دیگر قرار گیرد.**

---

# 35. جلوگیری از Circular Dependency

Business Relationship دوطرفه الزاماً به معنی Code Dependency دوطرفه نیست.

مثلاً:

```text
Account Head
      │
      ▼
Account
```

می‌تواند یک Relationship معتبر Business باشد.

اما Code Dependency باید جهت مشخص داشته باشد.

نامطلوب:

```text
Account
   │
   ▼
Journal Entry
   │
   ▼
Account
```

یا:

```text
Account Head
   │
   ▼
Account
   │
   ▼
Account Head
```

Circular Dependency باید باعث بررسی مجدد موارد زیر شود:

* Business Ownership
* Boundary
* Aggregate Boundary
* Contract
* Event
* Dependency Direction

---

# 36. Journal Entry به عنوان Core Domain

`Journal Entry` Core Subdomain و Core Aggregate سیستم است.

بنابراین معماری باید از Core Domain در برابر جزئیات Supporting Subdomainها محافظت کند.

مدل ترجیحی:

```text
PostJournalEntryUseCase
        │
        ├──► Account Capability
        │
        └──► JournalEntry Aggregate
```

نامطلوب:

```text
JournalEntry
        │
        X
        └──► AccountJpaRepository
```

یا:

```text
JournalEntry
        │
        X
        └──► AccountEntity
```

Core Domain نباید به Implementation داخلی Supporting Moduleها وابسته شود.

---

# 37. DDD و ارتباط داخلی General Ledger

مطابق ADR-0004، DDD ارتباط بین Moduleهای داخلی General Ledger را ممنوع نمی‌کند.

آنچه ممنوع است، **دور زدن Boundary** است.

مدل:

```text
General Ledger Microservice
│
└── General Ledger Bounded Context
        │
        ├── Account Head Module
        │
        ├── Account Module
        │
        └── Journal Entry Module
                │
                ▼
        Application / Use Case
                │
        ┌───────┴────────┐
        │                │
        ▼                ▼
Internal Contract    Internal Domain Event
/ Port
```

اصل:

> **Moduleها می‌توانند با یکدیگر همکاری کنند، اما نباید Boundary یکدیگر را دور بزنند.**

---

# 38. قوانین اصلی DDD

### Rule 1

Business Rule باید در Domain قرار گیرد.

### Rule 2

Domain نباید به Infrastructure وابسته باشد.

### Rule 3

Entity باید رفتار و Invariantهای مربوط به خودش را حفظ کند.

### Rule 4

Value Object برای مفاهیم بدون Identity استفاده شود.

### Rule 5

Aggregate مرز Consistency را مشخص می‌کند.

### Rule 6

دسترسی به Entityهای داخل Aggregate از طریق Aggregate Root انجام شود.

### Rule 7

Aggregateها باید تا حد امکان مستقل باقی بمانند.

### Rule 8

ارتباط بین Aggregateها ترجیحاً از طریق Identity/Reference انجام شود.

### Rule 9

Repository Port برای Persistence Abstraction استفاده شود.

### Rule 10

Repository یک Module نباید به عنوان Integration Mechanism عمومی با Module دیگر استفاده شود.

### Rule 11

Internal Contract / Port با Repository Port یکی نیست.

### Rule 12

Domain Service فقط برای Business Behavior واقعی استفاده شود.

### Rule 13

Application Service مسئول Use Case و Orchestration است.

### Rule 14

Domain Service مسئول Business Rule و Business Decision است.

### Rule 15

Domain Service نباید به Coordinator عمومی تبدیل شود.

### Rule 16

Domain Event باید بیانگر Business Fact معنادار باشد.

### Rule 17

Internal Domain Event با Integration Event متفاوت است.

### Rule 18

Event نباید صرفاً جایگزین عمومی Method Call شود.

### Rule 19

مدل Domain نباید صرفاً انعکاس Database Schema باشد.

### Rule 20

Entity داخلی یک Module نباید مستقیماً توسط Module دیگر مصرف شود.

### Rule 21

Repository Implementation یک Module نباید توسط Module دیگر مصرف شود.

### Rule 22

Table داخلی یک Module نباید توسط Module دیگر مستقیماً مصرف شود.

### Rule 23

Circular Dependency در Code و Architecture Graph باید جلوگیری شود.

### Rule 24

Module Boundary باید بر اساس Business Responsibility و Domain Boundary تعیین شود.

### Rule 25

Journal Entry به عنوان Core Domain باید از Implementation داخلی Supporting Moduleها مستقل باقی بماند.

### Rule 26

DDD نباید صرفاً برای ایجاد ساختارهای فنی اضافی و Over-Engineering استفاده شود.

---

# 39. Enforcement

اصول DDD باید تا حد امکان با Architectural Test و Code Review enforce شوند.

برای مثال:

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
Account
    must not access
AccountHeadEntity
```

```text
JournalEntry Domain
    must not depend on
Account Infrastructure
```

```text
Application
    may depend on
Internal Contract / Port
```

```text
Domain Service
    must not become
Cross-Module Orchestrator
```

همچنین باید موارد زیر کنترل شوند:

* Module Boundaries
* Dependency Direction
* Forbidden Dependencies
* Circular Dependencies
* Infrastructure Isolation
* Repository Ownership
* Internal Contract Usage
* Aggregate Boundaries

برای این منظور استفاده از **ArchUnit** و Code Review معماری توصیه می‌شود.

---

# 40. پیامدهای مثبت

این تصمیم باعث می‌شود:

* Business Model شفاف‌تر شود.
* Business Ruleها جای مشخصی داشته باشند.
* Core Domain از جزئیات فنی محافظت شود.
* Journal Entry واقعاً به عنوان Core Domain باقی بماند.
* Aggregateها Boundary مشخص داشته باشند.
* Application و Domain مسئولیت‌های مشخص داشته باشند.
* Repository از Integration Mechanism جدا بماند.
* Coupling بین Moduleها کنترل شود.
* تغییر Persistence بدون تغییر غیرضروری Domain امکان‌پذیر باشد.
* Ubiquitous Language در کد و مستندات حفظ شود.
* Moduleها بتوانند مدل داخلی خود را مستقل‌تر تکامل دهند.
* از تبدیل Domain Service به Serviceهای بزرگ و عمومی جلوگیری شود.
* امکان Evolution و استخراج احتمالی Moduleها در آینده حفظ شود.

---

# 41. پیامدهای منفی

DDD هزینه‌هایی نیز ایجاد می‌کند:

* تحلیل اولیه پیچیده‌تر می‌شود.
* نیاز به شناخت عمیق Domain وجود دارد.
* تعداد Modelها و Classها ممکن است افزایش پیدا کند.
* Mapping ممکن است بیشتر شود.
* Internal Contractها نیازمند نگهداری هستند.
* Architectural Testing بیشتری لازم است.
* تشخیص درست Aggregate Boundary نیازمند تجربه و تحلیل است.
* استفاده نادرست از DDD می‌تواند به Over-Engineering منجر شود.

بنابراین:

> **DDD باید متناسب با پیچیدگی واقعی Business Domain استفاده شود، نه صرفاً برای افزایش تعداد لایه‌ها و کلاس‌ها.**

---

# 42. تصمیم نهایی

> **General Ledger بر اساس اصول Domain-Driven Design طراحی خواهد شد و Domain Model به عنوان منبع اصلی Business Rules و Business Concepts در نظر گرفته می‌شود.**

> **در سطح Strategic DDD، General Ledger دارای سه Subdomain شامل Account Head، Account و Journal Entry است که Journal Entry به عنوان Core Subdomain و Account Head و Account به عنوان Supporting Subdomain شناخته می‌شوند.**

> **این سه Subdomain در وضعیت فعلی در یک General Ledger Bounded Context قرار دارند و این Bounded Context به صورت یک General Ledger Microservice مستقل پیاده‌سازی می‌شود.**

> **در سطح Tactical DDD، سه Aggregate Root اصلی شامل AccountHead، Account و JournalEntry وجود دارد و JournalEntry Core Aggregate محسوب می‌شود.**

> **Entity، Value Object، Aggregate، Domain Service، Repository و Domain Event بر اساس Business Responsibility واقعی استفاده خواهند شد و صرفاً به عنوان الگوهای فنی مورد استفاده قرار نمی‌گیرند.**

> **Application Layer مسئول اجرای Use Case و Orchestration است، در حالی که Domain مسئول Business Rule و Business Decision است.**

> **Domain Service فقط زمانی استفاده می‌شود که Business Behavior به صورت طبیعی متعلق به Entity، Value Object یا Aggregate مشخصی نباشد.**

> **Repository Port برای Persistence Abstraction استفاده می‌شود و Repository Implementation در Infrastructure قرار می‌گیرد. Repository یک Module نباید به عنوان روش عمومی Integration با Module دیگر استفاده شود.**

> **ارتباط داخلی بین Moduleهای General Ledger در صورت نیاز از طریق Internal Contract / Port یا Internal Domain Event انجام می‌شود. دسترسی مستقیم به Entity داخلی، Repository Implementation، Infrastructure یا Table داخلی Module دیگر مجاز نیست.**

> **Aggregateها باید Boundary و Invariantهای خود را حفظ کنند و برای ارتباط با Aggregateهای دیگر ترجیحاً از Identity/Reference استفاده شود.**

> **DDD در این پروژه به معنی طراحی بر اساس Business Domain است؛ نه صرفاً استفاده از نام‌هایی مانند Entity، Service و Repository.**

---

# 43. مدل نهایی معماری DDD

```text
                         GENERAL LEDGER
                          Problem Space
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
        Account Head        Account       Journal Entry
        Supporting        Supporting          CORE
        Subdomain         Subdomain         Subdomain
              │                │                │
              └────────────────┼────────────────┘
                               │
                               ▼
                  General Ledger Bounded Context
                               │
                               ▼
                  General Ledger Microservice
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
       Account Head         Account       Journal Entry
          Module             Module           Module
              │                │                │
              ▼                ▼                ▼
     AccountHead          Account        JournalEntry
      Aggregate          Aggregate        Aggregate
                                            ⭐ Core
                               │
                               ▼
                      Application Layer
                               │
                    ┌──────────┴──────────┐
                    │                     │
                    ▼                     ▼
          Internal Contract / Port   Internal Domain Event
                    │
                    ▼
                  Domain
                    │
          ┌─────────┼──────────┐
          │         │          │
          ▼         ▼          ▼
       Entity   Aggregate   Domain Service
          │         │          │
          └─────────┼──────────┘
                    │
                    ▼
             Repository Port
                    │
                    ▼
              Infrastructure
                    │
                    ▼
                Persistence
```

---

# 44. اصل کلیدی

> **DDD در General Ledger به معنی این است که Business Domain منبع اصلی طراحی باشد. Business Rule در Domain باقی می‌ماند، Aggregate مسئول Invariantهای خود است، Application مسئول Orchestration است، Repository مسئول Persistence Abstraction است و Infrastructure مسئول Implementation تکنیکی است.**

> **Moduleها مالک مدل داخلی خود هستند و برای همکاری با Moduleهای دیگر از Boundary و Contract مشخص استفاده می‌کنند. آنها می‌توانند با یکدیگر صحبت کنند، اما نباید Boundary یکدیگر را دور بزنند.**

> **Core Domain یعنی Journal Entry باید از جزئیات Supporting Moduleها و Infrastructure مستقل باقی بماند.**

---

# 45. رابطه با ADRهای مرتبط

## ADR-0003

ADR-0003 مرزهای اصلی General Ledger را تعیین می‌کند:

```text
General Ledger
      │
      ▼
General Ledger Bounded Context
      │
      ▼
General Ledger Microservice
```

و Subdomainهای فعلی را مشخص می‌کند:

```text
Account Head
Account
Journal Entry ⭐ Core
```

## ADR-0004

ADR-0004 نحوه Dependency و ارتباط داخلی را مشخص می‌کند:

```text
Application
     │
     ├── Internal Contract / Port
     │
     ├── Repository Port
     │
     └── Domain
```

و قواعد زیر را enforce می‌کند:

```text
No direct Infrastructure dependency
No direct Repository Implementation access
No direct Entity sharing
No direct Table access
No Circular Dependency
```

## ADR-0005

این ADR اصول DDD را بر روی این ساختار اعمال می‌کند:

```text
Business Domain
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

بنابراین:

> **ADR-0003 = Domain / Bounded Context / Microservice Structure**

> **ADR-0004 = Dependency Direction / Internal Communication**

> **ADR-0005 = Domain-Driven Design Principles و Tactical Domain Modeling**

---

# 46. Status

**Status: Accepted**

این ADR باید به عنوان مبنای تصمیم‌گیری برای موارد زیر استفاده شود:

* Domain Modeling
* Ubiquitous Language
* Entity
* Value Object
* Aggregate
* Aggregate Boundary
* Domain Service
* Application Service
* Repository
* Domain Event
* Internal Contract / Port
* Module Boundary
* Business Rule Placement
* Domain Dependency
* Core Domain Protection

هر تصمیمی که اصول این ADR را تغییر دهد باید از طریق یک Architectural Decision جدید ثبت شود یا این ADR به صورت رسمی اصلاح گردد.

---

# 47. منابع (References)

## 47.1 Eric Evans — Domain-Driven Design

**Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of Software***

منبع اصلی مفاهیم:

* Domain Model
* Ubiquitous Language
* Bounded Context
* Entity
* Value Object
* Aggregate
* Repository
* Domain Service
* Strategic / Tactical Design

ناشر: Addison-Wesley Professional, 2003.

([Pearson][1])

---

## 47.2 Vaughn Vernon — Implementing Domain-Driven Design

**Vaughn Vernon — *Implementing Domain-Driven Design***

مبنای تصمیم‌های مربوط به:

* Domains
* Subdomains
* Bounded Contexts
* Context Maps
* Entities
* Value Objects
* Domain Services
* Domain Events
* Modules
* Aggregates
* Repositories
* Application Layer
* Integration between Bounded Contexts

این کتاب به‌صورت مشخص فصل‌های جداگانه‌ای برای Subdomain/Bounded Context، Services، Domain Events، Modules، Aggregates، Repositories و Application دارد. ([Pearson][2])

---

## 47.3 Vaughn Vernon — Domain-Driven Design Distilled

**Vaughn Vernon — *Domain-Driven Design Distilled***

مبنای تکمیلی برای:

* Strategic Design
* Bounded Context
* Ubiquitous Language
* Subdomain
* Core / Supporting Subdomain
* Aggregate
* Domain Event

ساختار کتاب به‌صورت مشخص Strategic Design، Subdomains، Bounded Contexts و Tactical Design با Aggregates و Domain Events را پوشش می‌دهد. ([Pearson][3])

---

## 47.4 Martin Fowler — Patterns of Enterprise Application Architecture

**Martin Fowler — *Patterns of Enterprise Application Architecture***

مبنای تصمیم‌های مربوط به:

* Domain Model
* Service Layer
* Repository
* Data Mapper
* Unit of Work
* Value Object
* Layering
* Separation between Domain Logic and Persistence

منبع رسمی Fowler نیز Repository را به‌عنوان واسط بین Domain و Data Mapping و Data Mapper را به‌عنوان لایه جداسازی Domain Object از Database توصیف می‌کند. ([martinfowler.com][4])

---

## 47.5 Robert C. Martin — Clean Architecture

**Robert C. Martin — *Clean Architecture: A Craftsman's Guide to Software Structure and Design***

مبنای اصول:

* Dependency Rule
* Dependency Inversion
* Separation of Concerns
* Independence of Business Rules
* Independence from Frameworks and Infrastructure

---

## 47.6 Chris Richardson — Microservices Patterns

**Chris Richardson — *Microservices Patterns***

مبنای تصمیم‌های مرتبط با:

* Microservice Boundary
* Service Collaboration
* Domain Events
* Integration Communication
* Event-driven Communication
* Service Independence

منبع Microservices.io نیز مجموعه Patternهای مربوط به Service Collaboration، Domain Event و Event-driven Architecture را ارائه می‌کند. ([microservices.io][5])

---

# 48. جمع‌بندی منابع مورد استفاده

| موضوع                             | منبع اصلی                                  |
| --------------------------------- | ------------------------------------------ |
| Domain-Driven Design              | Eric Evans                                 |
| Ubiquitous Language               | Eric Evans                                 |
| Bounded Context                   | Eric Evans / Vaughn Vernon                 |
| Subdomain                         | Vaughn Vernon                              |
| Core / Supporting Subdomain       | Vaughn Vernon                              |
| Entity                            | Eric Evans / Vaughn Vernon                 |
| Value Object                      | Eric Evans / Vaughn Vernon                 |
| Aggregate                         | Eric Evans / Vaughn Vernon                 |
| Aggregate Boundary                | Vaughn Vernon                              |
| Domain Service                    | Eric Evans / Vaughn Vernon                 |
| Repository                        | Eric Evans / Martin Fowler / Vaughn Vernon |
| Application Service               | Martin Fowler / Vaughn Vernon              |
| Domain Event                      | Vaughn Vernon                              |
| Module                            | Vaughn Vernon                              |
| Persistence Isolation             | Martin Fowler                              |
| Data Mapper                       | Martin Fowler                              |
| Dependency Direction              | Robert C. Martin                           |
| Microservice Boundary             | Chris Richardson                           |
| Integration / Event Communication | Chris Richardson                           |

> **این منابع مبنای نظری و معماری تصمیم‌های این ADR هستند؛ اما تصمیم نهایی این سند متناسب با نیازها، Boundaryها و ساختار واقعی General Ledger Microservice اتخاذ شده است.**

[1]: https://www.pearson.com/en-gb/subject-catalog/p/domain-driven-design-tackling-complexity-in-the-heart-of-software/P200000009375?utm_source=chatgpt.com "Domain-Driven Design: Tackling Complexity in the Heart of Software"
[2]: https://www.pearson.com/en-us/subject-catalog/p/implementing-domain-driven-design/P200000009616/9780321834577?utm_source=chatgpt.com "Implementing Domain-Driven Design"
[3]: https://www.pearson.com/en-us/subject-catalog/p/domain-driven-design-distilled/P200000009615?utm_source=chatgpt.com "Domain-Driven Design Distilled"
[4]: https://martinfowler.com/eaaCatalog/?utm_source=chatgpt.com "Catalog of Patterns of Enterprise Application Architecture"
[5]: https://microservices.io/patterns/?utm_source=chatgpt.com "A pattern language for microservices"
