حتماً. این نسخه را بر اساس **Baseline جدید General Ledger** بازنویسی کردم؛ `Accounting` و `Accounting Period` حذف شده‌اند، `Account Head / Account / Journal Entry` جایگزین شده‌اند، Aggregateها و Moduleها از Subdomain جدا شده‌اند، و تعریف داخلی Microservice هم دقیق‌تر شده است.

# ADR-0003 — سبک معماری سیستم و ساختار داخلی General Ledger Microservice

* **Status:** Accepted
* **Date:** 2026-08-20
* **Decision Makers:** تیم معماری
* **Related ADRs:**

  * ADR-0001
  * ADR-0002
  * ADR-0005

---

# 1. Context

سیستم موردنظر یک سامانه مالی مبتنی بر **General Ledger (دفترکل)** است که با هدف پیاده‌سازی و نمایش عملی مفاهیم زیر طراحی می‌شود:

* Domain-Driven Design (DDD)
* Clean Architecture
* Onion Architecture
* Modular Architecture
* Separation of Concerns
* Dependency Inversion
* Architectural Testing

در سطح سیستم، General Ledger به‌عنوان یک **Microservice مستقل** طراحی و پیاده‌سازی می‌شود.

این Microservice دارای:

* Lifecycle مستقل
* Build مستقل
* Runtime مستقل
* Deployment مستقل
* Versioning مستقل
* قابلیت Scale مستقل

است.

در داخل این Microservice، یک **General Ledger Bounded Context** قرار دارد که شامل سه Subdomain اصلی است:

```text
General Ledger
│
├── Account Head
├── Account
└── Journal Entry ⭐ Core
```

این سه Subdomain متعلق به یک Problem Space واحد هستند و در طراحی فعلی در یک Bounded Context قرار گرفته‌اند.

> **Subdomain به معنی Bounded Context، Module یا Microservice نیست.**

---

# 2. Problem Space

Problem Space سیستم، **General Ledger** است.

در سطح Strategic DDD، این Problem Space شامل سه Subdomain اصلی است:

```text
General Ledger
│
├── Account Head
├── Account
└── Journal Entry ⭐
```

از نظر ارزش کسب‌وکاری:

```text
General Ledger
│
├── Core Subdomain
│   └── Journal Entry
│
├── Supporting Subdomain
│   └── Account
│
└── Supporting Subdomain
    └── Account Head
```

### 2.1 Core Subdomain — Journal Entry

`Journal Entry` به‌عنوان Core Subdomain شناخته می‌شود.

منطق اصلی و متمایزکننده General Ledger در فرآیندهایی مانند:

* ایجاد سند
* اعتبارسنجی سند
* اعتبارسنجی Debit / Credit
* کنترل Balance
* اعتبارسنجی Journal Lineها
* کنترل Posting Rules
* تعیین وضعیت سند
* Post کردن سند

قرار دارد.

بنابراین بیشترین تمرکز Domain Modeling و Business Ruleها باید روی `Journal Entry` باشد.

### 2.2 Supporting Subdomain — Account

`Account` یک Supporting Subdomain است.

مسئولیت آن شامل مفاهیمی مانند:

* هویت حساب
* نوع حساب
* وضعیت حساب
* ویژگی‌های حساب
* قواعد مرتبط با حساب

است.

### 2.3 Supporting Subdomain — Account Head

`Account Head` نیز یک Supporting Subdomain است.

مسئولیت آن شامل مفاهیمی مانند:

* ساختار سرفصل‌ها
* سلسله‌مراتب
* طبقه‌بندی
* جایگاه حساب در ساختار دفترکل
* قواعد مربوط به ساختار سرفصل

است.

---

# 3. Subdomain ≠ Bounded Context ≠ Module ≠ Microservice

این تفکیک یکی از اصول اصلی این معماری است.

```text
Subdomain
    ≠
Bounded Context
    ≠
Module
    ≠
Microservice
```

### Subdomain

بخشی از Problem Space و Business Domain است.

### Bounded Context

مرز یک مدل و زبان یکپارچه Domain است.

### Module

مرز ساختاری و کدی برای ایجاد Cohesion و کنترل Coupling در داخل Application است.

### Microservice

مرز Runtime و Deployment است که می‌تواند Lifecycle، Build، Deployment و Scale مستقل داشته باشد.

بنابراین وجود یک Subdomain به‌تنهایی دلیل ایجاد Microservice یا Bounded Context جدید نیست.

---

# 4. System-Level Architecture

در سطح سیستم، معماری بر مبنای Microservices است.

برای مثال:

```text
Banking Platform
│
├── Account Microservice
├── General Ledger Microservice
└── Payment Microservice
```

هر Microservice:

* مستقل Build می‌شود.
* مستقل اجرا می‌شود.
* مستقل Deploy می‌شود.
* Lifecycle مستقل دارد.
* در صورت نیاز می‌تواند مستقل Scale شود.

در این معماری:

```text
General Ledger Bounded Context
            │
            ▼
General Ledger Microservice
```

این رابطه یک **تصمیم معماری برای این سیستم** است و به معنی یک قانون عمومی برای تمام سیستم‌ها نیست.

> Bounded Context الزاماً همیشه معادل Microservice نیست.

---

# 5. General Ledger Microservice

`General Ledger` در سطح سیستم یک Microservice مستقل است.

این Microservice می‌تواند بدون اجرای سایر Microserviceها Build و Run شود.

```text
General Ledger Microservice
│
├── Build independently
├── Run independently
├── Deploy independently
├── Test independently
└── Scale independently
```

سرویس‌های دیگر ممکن است برای اجرای Use Caseهای خود با General Ledger ارتباط داشته باشند، اما این ارتباط نباید به معنی وابستگی Runtime برای Startup خود General Ledger باشد.

---

# 6. General Ledger Bounded Context

در طراحی فعلی، General Ledger یک **Bounded Context** اصلی دارد:

```text
General Ledger Bounded Context
│
├── Account Head
├── Account
└── Journal Entry
```

این سه Subdomain در یک مدل و زبان یکپارچه قرار دارند.

دلیل این تصمیم:

1. هر سه متعلق به یک Problem Space واحد هستند.
2. زبان دامنه آنها به‌شدت مرتبط است.
3. `Journal Entry` برای ثبت سند به `Account` نیاز دارد.
4. `Account` در ساختار General Ledger و Account Head معنا پیدا می‌کند.
5. ایجاد Bounded Context مستقل صرفاً به دلیل وجود Subdomain باعث پیچیدگی غیرضروری می‌شود.
6. در طراحی فعلی مرز اصلی مدل، `General Ledger` است.

بنابراین:

```text
General Ledger
└── General Ledger Bounded Context
    ├── Account Head
    ├── Account
    └── Journal Entry
```

---

# 7. Internal Domain-Oriented Modular Structure

داخل General Ledger Microservice، Subdomainها با مرزهای منطقی و کدی مناسب سازمان‌دهی می‌شوند.

ساختار پیشنهادی:

```text
general-ledger
│
├── account-head
├── account
└── journal-entry
```

اینها **Moduleهای داخلی** هستند.

اما:

```text
Account Head Module
        ≠
Account Head Microservice
```

و:

```text
Account Module
        ≠
Account Microservice
```

و:

```text
Journal Entry Module
        ≠
Journal Entry Microservice
```

این Moduleها در یک Runtime و یک General Ledger Microservice اجرا می‌شوند.

---

# 8. Internal Architecture

ساختار داخلی General Ledger Microservice بر اساس Domain-Oriented Modular Architecture و اصول Clean/Onion Architecture طراحی می‌شود.

مدل مفهومی:

```text
General Ledger Microservice
│
├── Account Head
│
├── Account
│
└── Journal Entry
```

و داخل هر Business Boundary، لایه‌های داخلی می‌توانند به شکل زیر سازمان‌دهی شوند:

```text
Business Boundary
│
├── Presentation
├── Application
├── Domain
└── Infrastructure
```

بنابراین به‌جای اینکه کل Application صرفاً بر اساس Technical Layerها سازمان‌دهی شود:

```text
controllers/
services/
repositories/
entities/
```

ساختار ابتدا بر اساس Business Boundaryها شکل می‌گیرد و سپس Layerهای معماری درون آنها تعریف می‌شوند.

---

# 9. Aggregate Structure

در سطح Tactical DDD، سه Aggregate Root اصلی در General Ledger تعریف می‌شوند:

```text
General Ledger Bounded Context
│
├── Account Head
│   └── AccountHead Aggregate Root
│
├── Account
│   └── Account Aggregate Root
│
└── Journal Entry
    └── JournalEntry Aggregate Root
```

بنابراین:

```text
AccountHead
Account
JournalEntry
```

سه Aggregate Root اصلی هستند.

وجود سه Aggregate به معنی وجود سه Microservice نیست.

---

# 10. AccountHead Aggregate

`AccountHead` مسئول حفظ Invariantهای مربوط به ساختار سرفصل است.

نمونه مسئولیت‌ها:

```text
AccountHead
├── Identity
├── Hierarchy
├── Classification
├── Placement Rules
└── Account Head Invariants
```

این Aggregate نباید مسئول Business Ruleهای متعلق به `Account` یا `JournalEntry` باشد.

---

# 11. Account Aggregate

`Account` مسئول حفظ Invariantهای مربوط به حساب است.

نمونه مسئولیت‌ها:

```text
Account
├── Identity
├── Account Type
├── Status
├── Account Attributes
└── Account Invariants
```

Business Ruleهای متعلق به خود Account باید در این Aggregate یا Domain Model مرتبط با آن قرار بگیرند.

---

# 12. JournalEntry Aggregate

`JournalEntry` مهم‌ترین Aggregate از نظر ارزش دامنه است.

این Aggregate مرکز Core Domain سیستم محسوب می‌شود.

نمونه مسئولیت‌ها:

```text
JournalEntry
├── Validate Debit / Credit
├── Validate Balance
├── Validate Journal Lines
├── Validate Posting Rules
├── Determine Posting State
└── Post
```

Business Ruleهای اصلی و متمایزکننده دفترکل نباید صرفاً در:

```text
Controller
Application Service
Repository
Infrastructure
```

قرار گیرند.

اصل تصمیم:

```text
JournalEntry Aggregate
        │
        ▼
Domain Behavior
        │
        ▼
Business Invariants
```

---

# 13. Aggregate ≠ Domain Service

وجود سه Aggregate به معنی ایجاد سه Domain Service نیست.

نباید صرفاً به دلیل وجود:

```text
AccountHead
Account
JournalEntry
```

به‌صورت خودکار Serviceهای زیر را به‌عنوان محل اصلی Business Logic ایجاد کنیم:

```text
AccountHeadService
AccountService
JournalEntryService
```

اصل تصمیم:

```text
Business Rule
     │
     ▼
Aggregate
```

اگر یک Business Rule ذاتاً بین چند Aggregate قرار داشته باشد و قرار دادن آن در یک Aggregate باعث ایجاد Coupling نامناسب شود، استفاده از Domain Service قابل بررسی است.

---

# 14. Aggregate Interaction

Aggregateها باید تا حد امکان مستقل باقی بمانند.

به‌خصوص Aggregate نباید مستقیماً Repository مربوط به Aggregate دیگری را فراخوانی کند.

طراحی زیر مجاز نیست:

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

هماهنگی چند Aggregate باید در سطح Application Use Case انجام شود.

برای مثال:

```text
CreateJournalEntry Use Case
            │
            ├── Validate Account
            │
            ├── Validate Account Head
            │
            └── Create JournalEntry
```

در این مدل:

* Application Layer مسئول Orchestration است.
* Aggregate مسئول Business Ruleهای خودش است.
* Repository مسئول Persistence است.

---

# 15. Application Layer

Application Layer مسئول اجرای Use Caseها و Orchestration است.

نمونه Use Caseها:

```text
CreateAccountUseCase
CreateJournalEntryUseCase
PostJournalEntryUseCase
```

Application Layer می‌تواند:

1. درخواست Use Case را دریافت کند.
2. Aggregateهای موردنیاز را از طریق Portها بازیابی کند.
3. عملیات لازم را روی Aggregateها اجرا کند.
4. چند Aggregate را برای یک Use Case هماهنگ کند.
5. Transaction Boundary را مدیریت کند.
6. نتیجه Use Case را برگرداند.

اما:

> Business Invariantهای Domain نباید صرفاً در Application Service قرار بگیرند.

---

# 16. Repository Ports

Repositoryها به‌عنوان Portهای Persistence تعریف می‌شوند.

نمونه:

```text
AccountHeadRepository
AccountRepository
JournalEntryRepository
```

ساختار کلی:

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
   │
   └── AccountRepository
          ▲
          │ implements
          │
Infrastructure
   │
   └── JpaAccountRepository
```

Aggregate نباید به Implementation مربوط به Persistence وابسته باشد.

---

# 17. Repository Port ≠ Internal Contract / Port

Repository تنها یکی از انواع Portها است.

این دو مفهوم باید از یکدیگر جدا باشند:

```text
Repository Port
    │
    └── Persistence Abstraction


Internal Contract / Port
    │
    └── Capability / Communication Abstraction
```

بنابراین هر Interface در Domain یا Application الزاماً Repository نیست.

این تفکیک برای جلوگیری از وابستگی اشتباه Domain به Infrastructure ضروری است.

---

# 18. Internal Business Boundaries

هر Business Boundary باید:

* مفاهیم مشخص خود را داشته باشد.
* مسئولیت مشخص خود را داشته باشد.
* Business Ruleهای مربوط به خود را نگهداری کند.
* از Implementation داخلی Boundaryهای دیگر استفاده نکند.

مدل:

```text
General Ledger
│
├── Account Head Boundary
│
├── Account Boundary
│
└── Journal Entry Boundary
```

هدف:

```text
High Cohesion
+
Low Coupling
```

است.

---

# 19. Communication Between Internal Boundaries

ارتباط بین Business Boundaryهای داخلی باید از طریق Contract یا Capability مشخص انجام شود.

برای مثال:

```text
Journal Entry
      │
      ▼
Account Capability
```

یا:

```text
Journal Entry
      │
      ▼
Internal Contract
      │
      ▼
Account
```

ارتباط مستقیم با Implementation داخلی Boundary دیگر مجاز نیست.

برای مثال:

```text
JournalEntry
      X
      │
      └──> Account Internal Entity
```

مجاز نیست.

---

# 20. Database Boundary

ممکن است تمام Business Boundaryهای General Ledger در ابتدا از یک Database فیزیکی استفاده کنند.

برای مثال:

```text
General Ledger Database
│
├── Account Head Data
├── Account Data
└── Journal Entry Data
```

اشتراک فیزیکی Database به معنی اشتراک مالکیت منطقی داده نیست.

هر Business Boundary باید مالک منطقی داده‌های مربوط به خودش باشد.

بنابراین یک Boundary نباید مستقیماً به جدول داخلی Boundary دیگر وابسته شود.

برای مثال:

```text
Journal Entry
      X
      │
      └── SELECT FROM account_internal_table
```

به‌عنوان روش ارتباط Domainی مجاز نیست.

---

# 21. API و Microservice Boundary

General Ledger Microservice از طریق Contractهای عمومی با سایر Microserviceها ارتباط برقرار می‌کند.

برای مثال:

```text
Account Microservice
        │
        │ API / Message
        ▼
General Ledger Microservice
        │
        ▼
General Ledger Bounded Context
```

سرویس‌های خارجی نباید به:

* Package داخلی
* Class داخلی
* Aggregate داخلی
* Repository داخلی
* Database داخلی

General Ledger دسترسی داشته باشند.

ارتباط خارجی فقط از طریق Contract عمومی Microservice انجام می‌شود.

---

# 22. Runtime Independence

General Ledger یک Runtime Unit مستقل است.

```text
General Ledger Microservice
│
├── Build independently
├── Test independently
├── Run independently
├── Deploy independently
└── Scale independently
```

در داخل آن:

```text
Account Head
Account
Journal Entry
```

در همان Runtime اجرا می‌شوند.

بنابراین:

```text
General Ledger Microservice
        │
        └── Single Runtime Unit
                │
                └── Domain-Oriented Internal Modules
```

---

# 23. چرا هر Subdomain یک Microservice نیست؟

صرف شناسایی سه Subdomain دلیل کافی برای ایجاد سه Microservice نیست.

طراحی زیر فعلاً رد شده است:

```text
Account Head Service
Account Service
Journal Entry Service
```

چنین تفکیکی بدون نیاز عملیاتی واقعی می‌تواند باعث افزایش موارد زیر شود:

* Network Communication
* Distributed Transactions
* Eventual Consistency
* Distributed Tracing
* Failure Handling
* Deployment Complexity
* Monitoring Complexity
* Infrastructure Complexity

بنابراین فعلاً سه Subdomain در یک General Ledger Microservice باقی می‌مانند.

---

# 24. Evolution Strategy

هدف معماری این نیست که هر Subdomain در آینده الزاماً Microservice شود.

هدف، ایجاد Boundaryهای صحیح است.

Evolution می‌تواند به شکل زیر باشد:

```text
General Ledger Microservice
            │
            ▼
Domain-Oriented Modules
            │
            ▼
Stable Business Boundaries
            │
            ▼
Evaluate Operational Need
            │
      ┌─────┴─────┐
      │           │
   No Need     Real Need
      │           │
      ▼           ▼
Keep Module   Extract Boundary
                  │
                  ▼
             Microservice
```

اگر در آینده یکی از Boundaryها نیاز واقعی به موارد زیر داشته باشد:

* Scale مستقل
* Deployment مستقل
* Database مستقل
* SLA متفاوت
* Team Ownership مستقل
* Release Cycle مستقل
* Operational Isolation

می‌توان Extraction آن Boundary را بررسی کرد.

---

# 25. اصل Evolution

Microservice شدن یک Boundary نتیجه Evolution معماری است، نه نتیجه مستقیم شناسایی Subdomain.

بنابراین:

```text
Subdomain
    │
    ▼
Clear Business Boundary
    │
    ▼
Operational Need
    │
    ▼
Possible Microservice Extraction
```

و نه:

```text
Subdomain
    │
    ▼
Automatically becomes Microservice
```

---

# 26. Clean Architecture

در داخل هر Business Boundary، اصول Clean Architecture رعایت می‌شوند.

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

### Domain

مرکز Business Logic و Domain Model است.

### Application

Use Caseها را اجرا و عملیات را Orchestrate می‌کند.

### Presentation

نقطه ورود درخواست‌های خارجی است.

برای مثال:

* REST Controller
* API Endpoint
* Message Consumer

### Infrastructure

جزئیات تکنیکی را پیاده‌سازی می‌کند.

برای مثال:

* Database
* JPA
* External API
* Message Broker
* Cache
* File System

---

# 27. Dependency Direction

اصل اصلی:

> **وابستگی‌های معماری باید به سمت Domain حرکت کنند.**

مدل:

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

---

# 28. Domain Independence

Domain باید تا حد امکان مستقل از Framework و Technology باشد.

Domain نباید مستقیماً به موارد زیر وابسته باشد:

```text
Spring
Spring Boot
JPA
Hibernate
REST
Kafka
Redis
Database
External API
```

هدف این است که Domain بتواند بدون اجرای Infrastructure تست شود.

Persistence نباید دلیل وابستگی Domain به Technology باشد.

---

# 29. Architectural Testing

قوانین معماری نباید صرفاً در Documentation باقی بمانند.

برای جلوگیری از Architecture Drift از **ArchUnit** استفاده خواهد شد.

نمونه قوانین:

```text
Domain
│
├── Must not depend on Infrastructure
├── Must not depend on Presentation
└── Must not depend on Framework-specific infrastructure
```

همچنین وابستگی مستقیم بین Business Boundaryها باید کنترل شود.

برای مثال:

```text
Journal Entry
      X
      │
      └──> Account Internal Implementation
```

نباید مجاز باشد.

Architectural Tests باید حداقل موارد زیر را کنترل کنند:

* Dependency Direction
* Domain Isolation
* Module Boundaries
* Forbidden Dependencies
* Infrastructure Isolation
* Internal Contract Usage

---

# 30. چرا Domain-Oriented Modular Architecture؟

سازمان‌دهی Domain-oriented مزایای زیر را دارد:

* Cohesion بالاتر
* Coupling پایین‌تر
* مشخص بودن Business Boundaryها
* جلوگیری از انتشار Business Logic
* امکان Architectural Testing
* تست‌پذیری بهتر
* امکان Evolution آینده
* جلوگیری از ایجاد Distributed Monolith
* کاهش پیچیدگی غیرضروری

ساختار اصلی به جای:

```text
Controller
Service
Repository
Entity
```

بر اساس Business Boundaryها شکل می‌گیرد:

```text
Account Head
Account
Journal Entry
```

و Layerهای معماری درون این Boundaryها اعمال می‌شوند.

---

# 31. چرا Layered Architecture صرف انتخاب نشد؟

Layered Architecture سنتی معمولاً ساختاری شبیه زیر ایجاد می‌کند:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

این ساختار بیشتر Technical Concernها را محور قرار می‌دهد.

در پروژه حاضر ابتدا Business Boundaryها مشخص می‌شوند:

```text
General Ledger
│
├── Account Head
├── Account
└── Journal Entry
```

سپس اصول Clean/Onion Architecture داخل این Boundaryها اعمال می‌شوند.

بنابراین معماری نهایی همزمان:

* Domain-oriented
* Modular
* Dependency-inverted
* Testable

است.

---

# 32. چرا هر Subdomain را Microservice نکردیم؟

دلایل اصلی:

1. هنوز نیاز عملیاتی مستقلی برای آنها وجود ندارد.
2. Domain آنها در یک Bounded Context قرار دارد.
3. ارتباط بین آنها در بسیاری از Use Caseها نزدیک است.
4. Distributed Transaction و Network Communication غیرضروری ایجاد می‌شود.
5. Complexity عملیاتی افزایش پیدا می‌کند.
6. یک Microservice مستقل برای هر Subdomain می‌تواند باعث Distributed Monolith شود.

بنابراین:

```text
Account Head
Account
Journal Entry
```

فعلاً در یک General Ledger Microservice باقی می‌مانند.

---

# 33. Alternatives Considered

| گزینه                                                              | تصمیم                             | دلیل                                    |
| ------------------------------------------------------------------ | --------------------------------- | --------------------------------------- |
| Layered Architecture به‌عنوان ساختار اصلی                          | رد شد                             | تمرکز بیش از حد بر Technical Layerها    |
| Onion Architecture                                                 | رد شد                        | بخاطر معماری Clean |
| Clean Architecture                                                 | پذیرفته شد                        | Separation of Concerns و استقلال Domain |
| Hexagonal Architecture                                             | قابل استفاده به‌عنوان الگوی داخلی | مناسب برای Port / Adapter               |
| Microservice برای هر Subdomain                                     | رد شد                             | پیچیدگی توزیع‌شده بدون نیاز عملیاتی     |
| یک Modular Monolith برای کل Banking Platform                       | رد شد                             | Microserviceها در سطح سیستم مستقل هستند |
| General Ledger Microservice + ساختار داخلی Domain-Oriented Modular | **پذیرفته شد**                    | متناسب با مرز سیستم و Domain            |
| General Ledger Microservice + DDD + Clean/Onion Architecture       | **پذیرفته شد**                    | معماری نهایی Ledger                     |

---

# 34. Final Architecture Model

مدل نهایی:

```text
                         GENERAL LEDGER
                         Problem Space
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
        Account Head       Account       Journal Entry
        Supporting        Supporting          CORE
        Subdomain         Subdomain         Subdomain
              │               │               │
              └───────────────┼───────────────┘
                              │
                              ▼
                  General Ledger Bounded Context
                              │
                              ▼
                  General Ledger Microservice
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
        Account Head       Account       Journal Entry
           Module           Module           Module
              │               │               │
              ▼               ▼               ▼
        AccountHead         Account       JournalEntry
         Aggregate        Aggregate        Aggregate
```

---

# 35. Internal Structure

ساختار منطقی داخل Microservice:

```text
General Ledger Microservice
│
├── Account Head Module
│   ├── Presentation
│   ├── Application
│   ├── Domain
│   │   └── AccountHead Aggregate
│   └── Infrastructure
│
├── Account Module
│   ├── Presentation
│   ├── Application
│   ├── Domain
│   │   └── Account Aggregate
│   └── Infrastructure
│
└── Journal Entry Module
    ├── Presentation
    ├── Application
    ├── Domain
    │   └── JournalEntry Aggregate
    └── Infrastructure
```

این ساختار یک **Business/Domain-oriented Modular Structure** است.

Moduleها در یک Runtime اجرا می‌شوند و هیچ‌کدام به‌صورت خودکار Microservice مستقل نیستند.

---

# 36. Architectural Hierarchy

Hierarchy نهایی:

```text
Problem Space
│
▼
General Ledger
│
├── Subdomain: Account Head
├── Subdomain: Account
└── Subdomain: Journal Entry ⭐ Core
        │
        ▼
Bounded Context
│
└── General Ledger Bounded Context
        │
        ▼
Microservice
│
└── General Ledger Microservice
        │
        ├── Module: Account Head
        │       └── AccountHead Aggregate
        │
        ├── Module: Account
        │       └── Account Aggregate
        │
        └── Module: Journal Entry
                └── JournalEntry Aggregate
```

---

# 37. Key Architectural Principles

### Rule 1 — Subdomain را با Bounded Context اشتباه نگیریم

وجود Subdomain جدید به‌تنهایی دلیل ایجاد Bounded Context جدید نیست.

### Rule 2 — Bounded Context را با Microservice یکی ندانیم

در این پروژه General Ledger Bounded Context به‌صورت یک Microservice مستقل پیاده‌سازی شده است، اما این رابطه قانون عمومی DDD نیست.

### Rule 3 — Subdomain را با Module یکی ندانیم

Module یک مرز ساختاری و کدی است؛ Subdomain یک مفهوم Strategic DDD است.

### Rule 4 — Module را با Microservice یکی ندانیم

Moduleهای داخلی General Ledger در یک Runtime اجرا می‌شوند.

### Rule 5 — Aggregate مرز Consistency است

هر Aggregate مسئول حفظ Invariantهای خودش است.

### Rule 6 — Aggregate نباید Repository را صدا بزند

Persistence از طریق Port و خارج از Aggregate مدیریت می‌شود.

### Rule 7 — Application Layer هماهنگ‌کننده است

Application Layer می‌تواند چند Aggregate را برای اجرای یک Use Case هماهنگ کند.

### Rule 8 — Business Rule باید در Domain باشد

Business Rule نباید صرفاً در Controller، Application Service یا Infrastructure قرار گیرد.

### Rule 9 — Core Domain باید واقعاً Core باقی بماند

`JournalEntry` مهم‌ترین بخش مزیت رقابتی سیستم است.

### Rule 10 — Internal Boundary باید قابل تست باشد

Architectural Rules باید توسط ArchUnit یا ابزارهای مشابه قابل Enforcement باشند.

---

# 38. Consequences

## Positive Consequences

این معماری مزایای زیر را دارد:

* استقلال General Ledger در سطح Microservice
* Build و Deployment مستقل
* مدل‌سازی Domain-oriented
* تفکیک واضح Subdomainها
* مرزهای داخلی مشخص
* Cohesion بالا
* Coupling پایین
* تست‌پذیری بالا
* استقلال Domain از Framework
* کاهش پیچیدگی نسبت به توزیع زودهنگام Subdomainها
* امکان Evolution آینده
* امکان استخراج یک Boundary به Microservice در صورت نیاز واقعی

## Negative Consequences

این معماری هزینه‌هایی نیز دارد:

* نیاز به تعریف دقیق Business Boundaryها
* نیاز به رعایت Module Boundaries
* نیاز به Architectural Testing
* احتمال ایجاد Coupling در صورت رعایت نکردن Boundaryها
* احتمال استفاده مشترک از Database در مراحل اولیه
* نیاز به Migration در صورت Extraction آینده
* نیاز به Discipline معماری برای جلوگیری از دسترسی مستقیم Moduleها به Implementation داخلی یکدیگر

---

# 39. Final Decision

تصمیم نهایی:

```text
SYSTEM LEVEL
────────────────────────────────────
Architecture: Microservices
        │
        ▼
GENERAL LEDGER LEVEL
────────────────────────────────────
General Ledger = Independent Microservice
        │
        ▼
DOMAIN LEVEL
────────────────────────────────────
General Ledger Bounded Context
        │
        ├── Account Head Subdomain
        ├── Account Subdomain
        └── Journal Entry Subdomain ⭐ Core
        │
        ▼
INTERNAL STRUCTURE
────────────────────────────────────
Domain-Oriented Modular Architecture
        │
        ├── Account Head Module
        │       └── AccountHead Aggregate
        │
        ├── Account Module
        │       └── Account Aggregate
        │
        └── Journal Entry Module
                └── JournalEntry Aggregate
        │
        ▼
INTERNAL ARCHITECTURE
────────────────────────────────────
Clean Architecture
+
Onion Architecture
+
Dependency Inversion
+
Architectural Testing
```

---

# 40. Architectural Baseline

این ADR باید همراه با **General Ledger — Domain & Bounded Context Decision Baseline** خوانده شود.

Baseline فعلی:

```text
Problem Space
    General Ledger

Subdomains
    ├── Account Head
    ├── Account
    └── Journal Entry ⭐ Core

Bounded Context
    General Ledger

Microservice
    General Ledger Microservice

Modules
    ├── Account Head
    ├── Account
    └── Journal Entry

Aggregates
    ├── AccountHead
    ├── Account
    └── JournalEntry ⭐ Core Aggregate
```

هر تصمیم معماری جدید درباره موارد زیر باید با این Baseline بررسی شود:

* Bounded Context
* Subdomain
* Module Boundary
* Business Boundary
* Aggregate Boundary
* Repository
* Domain Service
* Application Service
* Internal Contract
* Microservice Boundary
* Inter-module Communication

در صورت تغییر این تصمیم‌ها، ابتدا باید دلیل تغییر در قالب یک Architectural Decision جدید ثبت شود و سپس Baseline مربوطه به‌روزرسانی شود.

---

# 41. References

### 1. Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of Software*

مبنای مفاهیم:

* Domain
* Subdomain
* Bounded Context
* Domain Model
* Aggregate

### 2. Vaughn Vernon — *Implementing Domain-Driven Design*

مبنای تصمیم‌های مربوط به:

* Strategic DDD
* Bounded Context
* Aggregate
* Domain Model
* Context Boundaries

### 3. Robert C. Martin — *Clean Architecture*

مبنای تصمیم‌های مربوط به:

* Dependency Rule
* Dependency Inversion
* Domain Isolation
* Separation of Concerns

### 4. Mark Richards & Neal Ford — *Fundamentals of Software Architecture*

مبنای تصمیم‌های مربوط به:

* Modularity
* Coupling
* Cohesion
* Architecture Characteristics
* Architectural Evolution

### 5. Chris Richardson — *Microservices Patterns*

مبنای تصمیم‌های مربوط به:

* Microservice Architecture
* Service Boundaries
* Independent Deployment
* Distributed Systems Concerns

### 6. Martin Fowler — *Bounded Context*

مبنای تکمیلی مفهوم Bounded Context و مرز مدل دامنه.

### 7. Chris Richardson — *Domain-Oriented Modular Architecture / Modular Monolith*

مبنای تکمیلی تصمیم مربوط به سازمان‌دهی Domain-oriented داخل یک Runtime و ایجاد Boundaryهای قابل استخراج.

---

**Status:** Accepted

این سند بیان‌کننده ساختار معماری فعلی General Ledger است و هر تغییر اساسی در آن باید از طریق یک Architectural Decision جدید یا اصلاح رسمی همین ADR انجام شود.
