# ADR-0006 — استراتژی مدل دامنه (Domain Model Strategy)

* **وضعیت:** Accepted
* **تاریخ:** 2026-08-22
* **ADRهای مرتبط:**

  * ADR-0003
  * ADR-0004
  * ADR-0005

---

## 1. مسئله و زمینه (Context)

در معماری مبتنی بر DDD، صرفاً مشخص کردن وجود Domain کافی نیست.

باید مشخص شود که **Domain Model چگونه طراحی می‌شود، Business Behavior در کجا قرار می‌گیرد، Aggregateها چگونه تعریف می‌شوند و مرز Business Ruleها چگونه حفظ می‌شود.**

مطابق ADR-0003 و ADR-0005، فضای مسئله فعلی General Ledger شامل سه Subdomain اصلی است:

```text
General Ledger

├── Account Head
├── Account
└── Journal Entry ⭐ Core
```

این سه Subdomain در یک:

```text
General Ledger Bounded Context
```

قرار دارند و این Bounded Context به‌صورت یک:

```text
General Ledger Microservice
```

پیاده‌سازی می‌شود.

در سطح Tactical DDD نیز سه Aggregate Root اصلی وجود دارد:

```text
AccountHead
Account
JournalEntry ⭐ Core
```

Domain Model این سیستم شامل مفاهیمی مانند:

* Entity
* Value Object
* Aggregate
* Aggregate Root
* Domain Service
* Domain Event
* Domain Policy / Specification در صورت نیاز

خواهد بود.

اگر این مدل صرفاً به مجموعه‌ای از فیلدها و Getter/Setterها تبدیل شود، سیستم به سمت **Anemic Domain Model** حرکت خواهد کرد.

از طرف دیگر، قرار دادن تمام منطق در Entityها بدون توجه به Aggregate Boundary و Business Responsibility نیز می‌تواند باعث پیچیدگی و Coupling نامناسب شود.

بنابراین لازم است استراتژی مشخصی برای طراحی Domain Model تعیین شود.

---

# 2. تصمیم (Decision)

Domain Model سیستم به‌صورت:

> **Rich Domain Model**

طراحی خواهد شد.

Domain Model باید علاوه بر نگهداری State، در حد Business Responsibility خود مسئول:

* Business Behavior
* Business Rule
* Invariant

باشد.

مدل کلی:

```text
Domain Model

├── Entity
│   ├── Identity
│   ├── State
│   └── Behavior
│
├── Value Object
│
├── Aggregate
│   └── Aggregate Root
│
├── Domain Service
│
├── Domain Event
│
└── Domain Policy / Specification
```

اما Rich Domain Model به این معنی نیست که همه Logic باید داخل Entity قرار گیرد.

قاعده اصلی:

```text
Business Rule
      │
      ▼
Business Ownership
      │
      ├── Entity
      ├── Value Object
      ├── Aggregate
      └── Domain Service
```

---

# 3. اصل اساسی مدل دامنه

اصل اصلی این ADR:

> **Business Rule باید تا حد امکان در نزدیک‌ترین Domain Concept قرار گیرد که مالک واقعی آن Rule است.**

برای مثال اگر تغییر وضعیت Account دارای Business Rule باشد، ترجیح داده می‌شود Rule در خود Account یا Aggregate مربوط به آن قرار گیرد:

```text
Account
   │
   └── close()
```

به‌جای اینکه Business Rule صرفاً در یک Service عمومی قرار گیرد:

```text
AccountService
   │
   └── closeAccount(...)
```

اما اگر Rule ذاتاً متعلق به یک Entity یا Aggregate مشخص نباشد، Domain Service می‌تواند محل مناسب آن باشد.

بنابراین:

> **Rich Domain Model به معنی انتقال کورکورانه همه Logic به Entity نیست؛ بلکه به معنی قرار دادن Business Logic در محل صحیح Domain است.**

---

# 4. Domain Model در General Ledger

مطابق Baseline معماری:

```text
General Ledger
│
├── Account Head
│
├── Account
│
└── Journal Entry ⭐ Core
        │
        ▼
General Ledger Bounded Context
        │
        ▼
General Ledger Microservice
```

در سطح Tactical DDD:

```text
General Ledger Microservice

├── Account Head Module
│   └── AccountHead Aggregate
│
├── Account Module
│   └── Account Aggregate
│
└── Journal Entry Module
    └── JournalEntry Aggregate ⭐ Core
```

بنابراین این ADR باید در امتداد تصمیم‌های ADR-0003 و ADR-0005 باشد و مرزهای آنها را دوباره تعریف نکند.

---

# 5. Entity Strategy

Entity برای مفاهیمی استفاده می‌شود که دارای Identity مستقل هستند.

نمونه:

```text
Account

├── AccountId
├── Name
├── Status
├── Type
└── Business Behavior
```

Identity معیار اصلی تشخیص Entity است.

دو Entity با Identity متفاوت، حتی در صورت داشتن State یکسان، همچنان دو Entity متفاوت هستند:

```text
Account(id=1)
Account(id=2)
```

بنابراین:

```text
Account(1) ≠ Account(2)
```

Entity باید در محدوده Business Responsibility خود بتواند رفتار و Invariantهای مرتبط را حفظ کند.

---

# 6. Entity باید Behavior داشته باشد

Entity نباید صرفاً Data Container باشد.

برای مثال:

```java
account.close();

account.activate();

account.changeName(name);
```

بهتر از این است که تمام Business Behavior در یک Service عمومی قرار گیرد:

```java
accountService.close(account);

accountService.activate(account);

accountService.changeName(account, name);
```

اما این قانون مطلق نیست.

اگر Behavior:

* متعلق به چند Aggregate باشد،
* یا متعلق به یک Entity مشخص نباشد،
* یا نیازمند یک Domain Concept مستقل باشد،

می‌تواند در Domain Service یا سایر Domain Abstractionهای مناسب قرار گیرد.

اصل:

> **Behavior باید بر اساس Business Ownership در محل مناسب قرار گیرد.**

---

# 7. Setter عمومی

برای Domain Entityها استفاده از Setter عمومی برای تغییر Business State ترجیح داده نمی‌شود.

نامطلوب:

```java
account.setStatus(CLOSED);
```

ترجیحی:

```java
account.close();
```

زیرا متد Domain می‌تواند Invariantهای لازم را کنترل کند:

```text
close()

├── validate current state
├── validate business rules
└── change state
```

بنابراین State باید تا حد امکان از طریق رفتارهای معنادار Domain تغییر کند.

این تصمیم به معنی ممنوعیت مطلق Setter در تمام Domain Model نیست؛ بلکه:

> **Setter عمومی نباید راه اصلی تغییر Business State باشد.**

---

# 8. Constructor و Creation Strategy

Entity نباید بتواند در State نامعتبر ساخته شود.

Creation باید از طریق یکی از روش‌های مناسب Domain انجام شود:

* Constructor
* Factory Method
* Factory

مثال:

```text
Account.open(...)
```

یا:

```text
Account.create(...)
```

به‌جای:

```text
new Account()
    │
    ├── setName(...)
    ├── setStatus(...)
    ├── setType(...)
    └── ...
```

در زمان Creation باید Invariantهای مربوط به Creation رعایت شوند.

---

# 9. Value Object Strategy

برای مفاهیمی که Identity مستقل ندارند و بر اساس Value خود معنا پیدا می‌کنند، از Value Object استفاده می‌شود.

نمونه‌های احتمالی در General Ledger:

```text
Money
Currency
AccountCode
BranchCode
```

همچنین بسته به مدل نهایی Domain ممکن است مفاهیم دیگری به‌عنوان Value Object شناسایی شوند.

Value Object باید تا حد امکان:

* Immutable باشد.
* Equality مبتنی بر Value داشته باشد.
* Identity مستقل نداشته باشد.
* Behavior مرتبط با مفهوم خودش را داشته باشد.

مثال:

```text
Money

├── amount
├── currency
├── add()
├── subtract()
└── multiply()
```

بنابراین:

> **Value Object فقط مجموعه‌ای از Primitiveها نیست؛ بلکه می‌تواند مالک رفتار و Validation مربوط به مفهوم خودش باشد.**

---

# 10. Immutability

Value Objectها باید تا حد امکان Immutable باشند.

به جای:

```text
money.setAmount(...)
```

عملیات جدید ایجاد می‌شود:

```text
money.add(...)
money.subtract(...)
```

مثال:

```text
Money(100, IRR)
      │
      │ add(50)
      ▼
Money(150, IRR)
```

Object اولیه تغییر نمی‌کند.

این ویژگی باعث می‌شود Value Objectها برای استفاده در Domain Model قابل پیش‌بینی‌تر و ایمن‌تر باشند.

---

# 11. Aggregate Strategy

Aggregate به‌عنوان **Consistency Boundary** در Domain استفاده می‌شود.

هر Aggregate دارای یک:

```text
Aggregate Root
```

است.

در General Ledger:

```text
AccountHead Aggregate
Account Aggregate
JournalEntry Aggregate
```

هر Aggregate مسئول حفظ Invariantهای داخلی خودش است.

دسترسی خارجی به Entityهای داخلی Aggregate باید از طریق Aggregate Root انجام شود.

اصل:

```text
External Consumer
       │
       ▼
Aggregate Root
       │
       ▼
Internal Entities / Value Objects
```

---

# 12. Aggregate Boundary

Aggregate Boundary باید بر اساس:

* Business Invariant
* Consistency Requirement
* Transactional Consistency

تعریف شود.

نه صرفاً بر اساس:

* Database Table
* Foreign Key
* ORM Relationship
* Object Graph

مدل صحیح:

```text
Business Invariant
       │
       ▼
Consistency Requirement
       │
       ▼
Aggregate Boundary
```

بنابراین وجود رابطه بین دو Entity الزاماً به معنی قرار گرفتن آنها در یک Aggregate نیست.

---

# 13. Aggregate Size

Aggregateها باید بر اساس حداقل Boundary لازم برای حفظ Invariant طراحی شوند.

اصل دقیق‌تر از «Aggregate باید کوچک باشد» این است:

> **Aggregate باید به اندازه‌ای باشد که Invariantهای لازم را در یک Consistency Boundary حفظ کند، نه بزرگ‌تر.**

مدل مطلوب:

```text
Required Invariants
       +
Required Consistency
       │
       ▼
Minimum Effective Aggregate
```

بنابراین صرفاً برای راحتی Query یا Navigation نباید Entityهای زیادی در یک Aggregate قرار گیرند.

مدل نامطلوب:

```text
Huge Aggregate
      +
Many Entities
      +
High Contention
      +
Unnecessary Coupling
```

---

# 14. Aggregateهای General Ledger

مطابق Baseline فعلی:

```text
General Ledger

├── AccountHead Aggregate
│
├── Account Aggregate
│
└── JournalEntry Aggregate ⭐ Core
```

## 14.1 AccountHead Aggregate

مسئول Invariantهای مربوط به Account Head و ساختار آن است.

```text
AccountHead

├── identity
├── hierarchy
├── classification
└── account-head rules
```

## 14.2 Account Aggregate

مسئول Invariantهای مربوط به Account است.

```text
Account

├── identity
├── type
├── status
└── account rules
```

## 14.3 JournalEntry Aggregate

Core Aggregate سیستم است.

```text
JournalEntry

├── journal lines
├── debit / credit rules
├── balancing rules
├── posting rules
├── posting state
└── posting behavior
```

Business Logic اصلی و متمایزکننده General Ledger باید تا حد امکان در این Aggregate و Domain Model مرتبط با آن باقی بماند.

---

# 15. Aggregate Interaction

Aggregateها باید تا حد امکان مستقل باقی بمانند.

یک Aggregate نباید:

* Repository Aggregate دیگر را مستقیماً صدا بزند.
* Entity داخلی Aggregate دیگر را نگه دارد.
* به Persistence Implementation Aggregate دیگر وابسته شود.
* Infrastructure Module دیگر را بشناسد.

ارتباط بین Aggregateها ترجیحاً از طریق Identity/Reference انجام می‌شود.

مثال:

```text
Account

└── accountHeadId
```

یا:

```text
JournalEntry

└── accountId
```

به جای:

```text
Account
└── AccountHeadEntity
```

یا:

```text
JournalEntry
└── AccountEntity
```

---

# 16. Core Aggregate Protection

از آنجا که `JournalEntry` Core Aggregate است، باید از Implementation داخلی Supporting Moduleها محافظت شود.

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
AccountEntity
```

مدل ترجیحی:

```text
PostJournalEntryUseCase
        │
        ├──► Account Capability
        │
        └──► JournalEntry Aggregate
```

در این مدل، Journal Entry به Implementation داخلی Account وابسته نیست.

---

# 17. Repository Strategy

Repository برای Aggregate Root تعریف می‌شود.

نمونه:

```text
AccountRepository
AccountHeadRepository
JournalEntryRepository
```

ساختار:

```text
Aggregate Root
       │
       ▼
Repository Port
       │
       ▼
Infrastructure Implementation
```

برای Entityهای داخلی Aggregate Repository مستقل تعریف نمی‌شود، مگر اینکه آن Entity در واقع خودش Aggregate Root مستقلی باشد.

بنابراین:

```text
JournalEntry Aggregate
       │
       ▼
JournalEntryRepository
```

و نه:

```text
JournalLine
       │
       ▼
JournalLineRepository
```

مگر اینکه در مدل واقعی Domain، `JournalLine` به‌عنوان Aggregate Root مستقل تعریف شود.

---

# 18. Repository Port ≠ Internal Contract / Port

مطابق ADR-0004 و ADR-0005، Repository تنها یکی از انواع Port است.

دو مفهوم باید جدا باقی بمانند:

```text
Repository Port

└── Persistence Abstraction
```

و:

```text
Internal Contract / Port

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

می‌تواند Contract یک Business Capability باشد.

بنابراین:

> **هر Interface داخل Domain یا Application الزاماً Repository نیست.**

---

# 19. Repository ابزار Integration نیست

Repository یک Module نباید به‌عنوان روش عمومی ارتباط با Module دیگر استفاده شود.

نامطلوب:

```text
Account Module
       │
       X
       ▼
AccountHeadRepository
```

اگر هدف این Dependency، دریافت یک Business Capability از Account Head باشد، باید از Contract مناسب استفاده شود:

```text
Application
    │
    ▼
AccountHeadPort
```

اصل:

> **Repository مسئول Persistence است؛ Internal Contract / Port مسئول Business Capability و ارتباط داخلی است.**

---

# 20. Persistence Independence

Domain Model نباید بر اساس Persistence Model یا Database Schema طراحی شود.

مدل ترجیحی:

```text
Business Model
      │
      ▼
Domain Model
      │
      ▼
Persistence Mapping
      │
      ▼
Database
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

هدف این است که تغییر در Persistence Model تا حد امکان Business Model را تحت تأثیر قرار ندهد.

---

# 21. Domain Entity و Persistence Entity

در صورت وجود نیاز معماری، Domain Entity و Persistence Entity می‌توانند جدا باشند:

```text
Domain

└── Account
```

و:

```text
Infrastructure

└── AccountJpaEntity
```

سپس Mapping انجام می‌شود:

```text
Account
   │
   ▼
AccountMapper
   │
   ▼
AccountJpaEntity
```

این جداسازی زمانی ارزشمند است که تفاوت واقعی بین Domain Model و Persistence Model وجود داشته باشد.

---

# 22. چه زمانی Domain Entity و Persistence Entity می‌توانند یکی باشند؟

جداسازی Domain Entity و Persistence Entity یک قانون مطلق نیست.

اگر:

* مدل Domain ساده باشد،
* نیازهای Persistence با Domain تعارض نداشته باشد،
* Framework Leakage قابل قبول و کنترل‌شده باشد،
* Annotationهای تکنولوژی Business Model را مخدوش نکنند،
* و Coupling نامناسب ایجاد نشود،

می‌توان از یک مدل مشترک استفاده کرد.

اما این تصمیم باید آگاهانه باشد.

اصل:

> **Domain نباید صرفاً برای راحتی Persistence قربانی شود.**

بنابراین:

```text
Separate Model
```

یک الزام مطلق نیست؛

بلکه:

```text
Business Model Independence
```

اصل معماری است.

---

# 23. Domain Service Strategy

Domain Service زمانی استفاده می‌شود که یک Business Rule یا Business Operation:

* متعلق به Entity مشخصی نباشد،
* متعلق به Aggregate مشخصی نباشد،
* Value Object مناسب برای آن وجود نداشته باشد،
* و همچنان یک مفهوم واقعی از Domain باشد.

مثال مفهومی:

```text
TransferPolicy
```

یا یک Business Decision که واقعاً بین چند Aggregate قرار دارد.

اما Domain Service نباید تبدیل شود به:

```text
EverythingService
GenericService
Manager
Coordinator
```

اصل:

> **Domain Service باید یک Business Concept مشخص را نمایندگی کند.**

---

# 24. Domain Service در مقابل Application Service

این دو مفهوم باید کاملاً جدا باقی بمانند.

## Domain Service

```text
Domain Service

└── Business Rule / Business Decision
```

## Application Service

```text
Application Service

├── Use Case
├── Orchestration
├── Coordination
├── Transaction Boundary
├── Repository Port
└── Internal Contract / Port
```

اصل:

> **Application Orchestrates; Domain Decides.**

Application Service نباید مالک Business Ruleهای Core Domain شود.

---

# 25. Application Service Strategy

Application Service مسئول اجرای Use Case است.

برای مثال:

```text
CreateAccountUseCase

        │
        ├──► AccountHeadPort
        │
        ├──► Account Domain
        │
        └──► AccountRepository
```

Application Layer می‌تواند:

* Aggregate را Load کند.
* Capability موردنیاز را درخواست کند.
* چند Aggregate را هماهنگ کند.
* Transaction Boundary را کنترل کند.
* Repository Port را فراخوانی کند.
* Domain Service را در صورت نیاز فراخوانی کند.

اما Business Decision باید در Domain باقی بماند.

---

# 26. مثال: ایجاد Account زیر Account Head

فرض کنیم Use Case زیر وجود دارد:

> ایجاد یک Account زیر یک Account Head مشخص.

مدل پیشنهادی:

```text
CreateAccountUseCase
        │
        ├──► AccountHeadPort
        │       │
        │       └── validate / resolve required capability
        │
        ├──► Account Domain
        │       │
        │       └── enforce Account rules
        │
        └──► AccountRepository
                │
                └── Persistence
```

در این مدل:

```text
Application
    =
Use Case Orchestration
```

```text
Account
    =
Account Business Responsibility
```

```text
AccountHead
    =
Account Head Business Responsibility
```

```text
Repository
    =
Persistence
```

---

# 27. Business Rule در مقابل Input Validation

Validation باید بر اساس نوع مسئولیت تفکیک شود.

## Input Validation

مواردی مانند:

```text
required field
format
length
syntax
```

می‌توانند در Presentation/Application Validation بررسی شوند.

## Business Invariant

مواردی مانند:

```text
Account cannot be closed in this state
JournalEntry must be balanced
Posting rules must be satisfied
```

باید در Domain enforce شوند.

مدل:

```text
Input Validation
       │
       ▼
Application Boundary
```

و:

```text
Business Invariant
       │
       ▼
Domain
```

بنابراین Controller نباید مالک Business Invariant باشد.

---

# 28. Invariant Strategy

Invariant باید در نزدیک‌ترین Domain Boundary ممکن enforce شود.

مدل ترجیحی:

```text
Entity Invariant
       │
       ▼
Aggregate Invariant
       │
       ▼
Domain Service
```

اما این ترتیب به معنی انتقال Invariant از Entity به Aggregate یا از Aggregate به Domain Service نیست.

اصل واقعی:

> **Invariant باید در کوچک‌ترین Domain Boundaryای قرار گیرد که مالک و قادر به تضمین آن است.**

برای مثال:

```text
JournalEntry must be balanced
```

باید توسط `JournalEntry Aggregate` تضمین شود.

اما یک Business Decision که ذاتاً بین چند Aggregate قرار دارد می‌تواند در Domain Service قرار گیرد.

---

# 29. Domain Event Strategy

Domain Event برای بیان یک Business Fact معنادار استفاده می‌شود.

نمونه:

```text
AccountOpened
JournalEntryPosted
```

Domain Event باید:

* Immutable باشد.
* یک اتفاق رخ‌داده را بیان کند.
* نام معنادار Domain داشته باشد.
* Producer را از Consumerهای داخلی مستقل کند.

مثال:

```text
AccountOpened
```

بهتر از:

```text
OpenAccountEvent
```

است؛ زیرا Event بیانگر چیزی است که رخ داده است.

---

# 30. Internal Domain Event و Integration Event

این دو مفهوم باید از یکدیگر جدا باشند.

## Internal Domain Event

برای ارتباط داخل General Ledger Microservice:

```text
Aggregate
    │
    ▼
Internal Domain Event
    │
    ▼
Internal Handler
```

## Integration Event

برای عبور از مرز Microservice:

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

> **Internal Domain Event و Integration Event از نظر Boundary و مسئولیت یکسان نیستند.**

---

# 31. Domain Event جایگزین عمومی Method Call نیست

Event زمانی استفاده می‌شود که یک Business Fact رخ داده باشد و Consumerها بتوانند به آن واکنش نشان دهند.

اما اگر یک Use Case برای ادامه تصمیم خود به پاسخ فوری نیاز دارد، Internal Contract / Port مناسب‌تر است.

مثال:

```text
CreateAccountUseCase
        │
        ▼
AccountHeadPort
        │
        ▼
Capability Result
```

در مقابل:

```text
AccountCreated
        │
        ▼
Internal Event Handler
```

اصل:

> **Event برای انتشار Business Fact است، نه جایگزین عمومی Method Call.**

---

# 32. Domain Model و DTO

DTO بخشی از Domain Model نیست.

مدل‌ها باید بر اساس Boundary خود تفکیک شوند:

```text
Presentation DTO
       │
       ▼
Application Input / Command
       │
       ▼
Domain Model
```

بنابراین API DTO نباید صرفاً به‌عنوان Domain Entity استفاده شود.

نامطلوب:

```text
CreateAccountRequest
       │
       ▼
Account Entity
```

مدل ترجیحی:

```text
CreateAccountRequest
       │
       ▼
CreateAccountCommand
       │
       ▼
Account.open(...)
```

DTO باید نیاز Boundary مربوط به خودش را نمایندگی کند، نه ساختار داخلی Domain را.

---

# 33. Domain Model و Database Schema

Database Schema نباید تعیین‌کننده ساختار Domain باشد.

برای مثال اگر Database شامل:

```text
ACCOUNT
ACCOUNT_DETAIL
ACCOUNT_STATUS
```

باشد، Domain الزاماً نباید همان ساختار را منعکس کند.

Domain باید بر اساس Business Concept طراحی شود:

```text
Account

├── identity
├── state
├── behavior
└── invariants
```

Persistence Layer مسئول Mapping این مدل به Database است.

---

# 34. جلوگیری از Anemic Domain Model

مدل زیر نامطلوب است:

```text
Account

├── id
├── name
├── status
├── getter
└── setter
```

در حالی که Business Logic اصلی در:

```text
AccountService
```

قرار گرفته باشد.

مدل ترجیحی:

```text
Account

├── identity
├── state
│
├── open()
├── close()
├── rename()
└── enforce account rules
```

اما رفتار فقط زمانی به Entity اضافه می‌شود که واقعاً متعلق به آن Entity باشد.

بنابراین:

> **Rich Domain Model به معنی Fat Entity نیست.**

---

# 35. JournalEntry به‌عنوان Core Domain Model

مطابق ADR-0005، `Journal Entry` Core Subdomain و `JournalEntry` Core Aggregate است.

بنابراین Business Logic اصلی General Ledger باید در این Boundary باقی بماند.

برای مثال:

```text
JournalEntry

├── addLine()
├── validateBalance()
├── validatePostingRules()
├── post()
└── state transition
```

البته این متدها نمونه مفهومی هستند و طراحی نهایی باید بر اساس Business Rules واقعی انجام شود.

نباید Core Logic به شکل زیر پراکنده شود:

```text
Controller
    +
Application Service
    +
Repository
    +
Infrastructure
    +
Database Trigger
```

اصل:

> **Core Domain باید Domain-Centric باقی بماند.**

---

# 36. Domain Model و Framework

Domain Model باید تا حد امکان Framework Agnostic باشد.

ترجیح داده می‌شود Domain به موارد زیر وابسته نباشد:

```text
Spring
Spring Boot
JPA
Hibernate
REST
Kafka
Redis
Database Driver
```

Frameworkها جزئیات تکنیکی هستند.

Dependency ترجیحی:

```text
Domain
    │
    X
    │
Infrastructure
```

و:

```text
Infrastructure
    │
    └── implements Domain/Application abstractions
```

---

# 37. Module Ownership

هر Module مالک Domain Model داخلی خود است.

مثال:

```text
Account Module
    │
    └── Account Aggregate
```

نباید Module دیگر مستقیماً Entity داخلی آن را مصرف کند:

```text
Journal Entry Module
        │
        X
        ▼
AccountEntity
```

مدل ترجیحی:

```text
Journal Entry Application
        │
        ▼
Account Capability / Port
```

یا در صورت مناسب بودن:

```text
Account
    │
    ▼
Domain Event
```

هدف:

> **هر Module باید بتواند مدل داخلی خود را بدون ایجاد تغییر زنجیره‌ای در Moduleهای دیگر تکامل دهد.**

---

# 38. Shared Database ≠ Shared Domain Model

ممکن است General Ledger از یک Database مشترک استفاده کند:

```text
General Ledger Database

├── Account Head Data
├── Account Data
└── Journal Entry Data
```

اما این به معنی Shared Domain Model نیست.

هر Module همچنان مالک Business Responsibility و مدل منطقی خودش است.

بنابراین:

```text
Account Module
       │
       X
       ▼
AccountHead Internal Table
```

به‌عنوان Business Integration مجاز نیست.

---

# 39. Direct Database Access ممنوع

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
* Architecture Drift
* دشوار شدن Evolution

می‌شود.

---

# 40. Dependency Direction

مطابق ADR-0004، Domain Model باید از Infrastructure مستقل باشد.

مدل کلی:

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

و:

```text
Infrastructure

├── implements Repository Port
├── implements technical adapters
└── provides external integrations
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
Account Domain
   │
   X
   ▼
AccountJpaRepository
```

اصل:

> **Dependency باید به سمت Abstraction و Business Responsibility باشد، نه Implementation و Infrastructure.**

---

# 41. Domain Model Complexity

DDD به معنی پیچیده کردن همه بخش‌های سیستم نیست.

Complexity مدل باید متناسب با Complexity واقعی Business باشد.

```text
Business Complexity
       │
       ▼
Required Domain Model Complexity
```

بنابراین:

```text
Simple Concept
      │
      ▼
Simple Model
```

و:

```text
Complex Business Rule
      │
      ▼
Rich Domain Model
      │
      ├── Aggregate
      ├── Value Object
      ├── Domain Service
      └── Domain Event
```

هیچ‌یک از این Abstractionها صرفاً به دلیل استفاده از DDD نباید ایجاد شوند.

---

# 42. Architectural Rules

قوانین زیر پذیرفته می‌شوند:

### Rule 1

Domain Model باید Rich باشد.

### Rule 2

Business Rule باید در Domain قرار گیرد.

### Rule 3

Entity نباید صرفاً Data Container باشد.

### Rule 4

Setter عمومی نباید راه اصلی تغییر Business State باشد.

### Rule 5

Value Objectها تا حد امکان Immutable باشند.

### Rule 6

Aggregate بر اساس Business Invariant و Consistency Boundary تعریف شود.

### Rule 7

Aggregate فقط به اندازه Consistency Boundary لازم رشد کند.

### Rule 8

Repository برای Aggregate Root تعریف شود.

### Rule 9

Domain نباید به Persistence Implementation وابسته باشد.

### Rule 10

Repository Port با Internal Contract / Port متفاوت است.

### Rule 11

Repository نباید به‌عنوان Integration Mechanism عمومی استفاده شود.

### Rule 12

Application Service مالک Business Rule نیست.

### Rule 13

Domain Service فقط برای Business Behavior واقعی استفاده شود.

### Rule 14

Domain Event باید Business Fact معنادار را بیان کند.

### Rule 15

Internal Domain Event با Integration Event متفاوت است.

### Rule 16

DTO نباید جایگزین Domain Model شود.

### Rule 17

Domain Model نباید صرفاً بر اساس Database Schema طراحی شود.

### Rule 18

Moduleها نباید Entity داخلی یکدیگر را مستقیماً مصرف کنند.

### Rule 19

Moduleها نباید Repository Implementation یکدیگر را مصرف کنند.

### Rule 20

Moduleها نباید Table داخلی یکدیگر را مستقیماً مصرف کنند.

### Rule 21

Circular Dependency بین Domain Moduleها باید جلوگیری شود.

### Rule 22

JournalEntry به‌عنوان Core Aggregate باید از Implementation داخلی Supporting Moduleها مستقل باقی بماند.

### Rule 23

DDD نباید باعث Over-Engineering غیرضروری شود.

---

# 43. Architectural Enforcement

اصول این ADR باید تا حد امکان توسط Architectural Test و Code Review enforce شوند.

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
Account Module
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

# 44. پیامدهای مثبت

این تصمیم باعث می‌شود:

* Business Logic در Domain متمرکز شود.
* Business Model قابل فهم‌تر باشد.
* Anemic Domain Model کاهش یابد.
* Invariantها در محل مناسب enforce شوند.
* Aggregate Boundaryها بر اساس Business تعریف شوند.
* Core Domain یعنی JournalEntry بهتر محافظت شود.
* Domain از Persistence و Framework مستقل‌تر باقی بماند.
* Repository از Integration Mechanism جدا بماند.
* Moduleها مالک مدل داخلی خود باشند.
* تست Business Ruleها ساده‌تر شود.
* تغییر تکنولوژی Persistence تأثیر کمتری روی Domain داشته باشد.

---

# 45. پیامدهای منفی

این رویکرد هزینه‌هایی نیز ایجاد می‌کند:

* تعداد Domain Objectها ممکن است افزایش پیدا کند.
* Mapping بین Domain و Persistence ممکن است ایجاد شود.
* طراحی اولیه زمان بیشتری نیاز دارد.
* شناخت Domain برای توسعه‌دهندگان ضروری‌تر می‌شود.
* تشخیص صحیح Aggregate Boundary نیازمند تحلیل و تجربه است.
* استفاده نادرست از Domain Service می‌تواند پیچیدگی ایجاد کند.
* جداسازی بیش از حد Domain و Persistence می‌تواند Over-Engineering ایجاد کند.

این هزینه‌ها با توجه به ماهیت مالی و پیچیدگی Business Domain سیستم پذیرفته می‌شوند، اما باید از پیچیدگی غیرضروری جلوگیری شود.

---

# 46. تصمیم نهایی

> **Domain Model سیستم به‌صورت Rich Domain Model طراحی خواهد شد. Entityها مالک رفتار و Invariantهای مرتبط با خود خواهند بود، Value Objectها برای مفاهیم بدون Identity استفاده می‌شوند و Aggregateها بر اساس Business Invariant و Consistency Boundary تعریف خواهند شد.**

> **در General Ledger، Aggregateهای اصلی شامل AccountHead، Account و JournalEntry هستند و JournalEntry به‌عنوان Core Aggregate از اهمیت ویژه برخوردار است.**

> **Business Ruleهای Core Domain نباید صرفاً در Application Service، Controller، Repository یا Infrastructure قرار گیرند. Application Layer مسئول Orchestration و اجرای Use Case است، در حالی که Domain مسئول Business Behavior و Business Decision است.**

> **Domain Service تنها زمانی استفاده می‌شود که Business Behavior به‌صورت طبیعی متعلق به Entity، Value Object یا Aggregate مشخصی نباشد و همچنان یک مفهوم واقعی Domain باشد.**

> **Repository فقط برای Aggregate Rootها تعریف می‌شود و Repository Port صرفاً Abstraction مربوط به Persistence است. Internal Contract / Port برای ارائه Business Capability یا ارتباط داخلی استفاده می‌شود و این دو مفهوم نباید با یکدیگر اشتباه گرفته شوند.**

> **Domain Model نباید تابع Persistence Model یا Database Schema باشد. Domain Entity و Persistence Entity در صورت وجود نیاز معماری می‌توانند از یکدیگر جدا باشند، اما جداسازی آنها یک قانون مطلق نیست.**

> **Aggregateها باید Boundary و Invariantهای خود را حفظ کنند و برای ارتباط با Aggregateهای دیگر ترجیحاً از Identity/Reference یا Contract مناسب استفاده شود. دسترسی مستقیم به Entity داخلی، Repository Implementation، Infrastructure یا Table داخلی Module دیگر مجاز نیست.**

> **DDD در این پروژه به معنی طراحی بر اساس Business Domain است، نه صرفاً استفاده از الگوهایی مانند Entity، Repository و Service.**

---

# 47. مدل نهایی Domain Model

```text
                         GENERAL LEDGER
                              │
                              ▼
                    General Ledger
                    Bounded Context
                              │
                              ▼
                    General Ledger
                     Microservice
                              │
             ┌────────────────┼────────────────┐
             │                │                │
             ▼                ▼                ▼
      Account Head         Account       Journal Entry
          Module            Module           Module
             │                │                │
             ▼                ▼                ▼
      AccountHead          Account       JournalEntry
       Aggregate          Aggregate        Aggregate
                                             ⭐ Core
             │                │                │
             └────────────────┼────────────────┘
                              │
                              ▼
                        Domain Model
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
       Entity           Value Object          Aggregate
                                                  │
                                                  ▼
                                          Aggregate Root
                                                  │
                          ┌───────────────────────┼───────────────────┐
                          │                       │                   │
                          ▼                       ▼                   ▼
                     Business Rule          Domain Service      Domain Event
```

---

# 48. رابطه با ADRهای مرتبط

## ADR-0003 — General Ledger Boundary

ADR-0003 ساختار سطح بالا را مشخص می‌کند:

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

بنابراین ADR-0006 نباید این مرزها را دوباره تعریف یا تغییر دهد.

---

## ADR-0004 — Dependency Direction و Internal Communication

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

و اصول زیر را enforce می‌کند:

```text
No direct Infrastructure dependency

No direct Repository Implementation access

No direct Entity sharing

No direct Table access

No Circular Dependency
```

ADR-0006 این اصول را در طراحی Tactical Domain Model اعمال می‌کند.

---

## ADR-0005 — Domain-Driven Design

ADR-0005 اصول کلی DDD و مدل Strategic/Tactical را مشخص می‌کند:

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

ADR-0006 این تصمیم را به **استراتژی دقیق طراحی Domain Model** تبدیل می‌کند.

بنابراین:

```text
ADR-0003
    =
Domain / Bounded Context / Microservice Structure

ADR-0004
    =
Dependency Direction / Internal Communication

ADR-0005
    =
DDD Principles / Strategic & Tactical Modeling

ADR-0006
    =
Domain Model Strategy / Rich Domain Model
```

---

# 49. Status

**Status: Accepted**

این ADR باید به‌عنوان مبنای تصمیم‌گیری برای موارد زیر استفاده شود:

* Domain Entity
* Value Object
* Aggregate
* Aggregate Boundary
* Aggregate Root
* Domain Service
* Application Service
* Repository
* Domain Event
* Internal Contract / Port
* Business Rule Placement
* Persistence Mapping
* Domain Model Evolution
* Core Domain Protection

هر تصمیمی که اصول این ADR را تغییر دهد باید از طریق یک Architectural Decision جدید ثبت شود یا این ADR به‌صورت رسمی اصلاح گردد.

---

# 50. منابع (References)

## 50.1 Eric Evans — Domain-Driven Design

**Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of Software***

مبنای مفاهیم:

* Domain Model
* Ubiquitous Language
* Entity
* Value Object
* Aggregate
* Repository
* Domain Service
* Bounded Context
* Strategic / Tactical Design

ناشر: Addison-Wesley Professional, 2003.

## 50.2 Vaughn Vernon — Implementing Domain-Driven Design

**Vaughn Vernon — *Implementing Domain-Driven Design***

مبنای تصمیم‌های مربوط به:

* Entities
* Value Objects
* Aggregates
* Repositories
* Domain Services
* Domain Events
* Modules
* Application Layer
* Bounded Contexts

## 50.3 Vaughn Vernon — Domain-Driven Design Distilled

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

## 50.4 Martin Fowler — Patterns of Enterprise Application Architecture

**Martin Fowler — *Patterns of Enterprise Application Architecture***

مبنای تصمیم‌های مرتبط با:

* Domain Model
* Service Layer
* Repository
* Data Mapper
* Value Object
* Layering
* Persistence Separation

## 50.5 Robert C. Martin — Clean Architecture

**Robert C. Martin — *Clean Architecture: A Craftsman's Guide to Software Structure and Design***

مبنای اصول:

* Dependency Rule
* Dependency Inversion
* Separation of Concerns
* Business Rule Independence
* Framework Independence
* Infrastructure Independence

---

# 51. جمع‌بندی منابع مورد استفاده

| موضوع                  | منبع اصلی                                  |
| ---------------------- | ------------------------------------------ |
| Domain Model           | Eric Evans                                 |
| Entity                 | Eric Evans / Vaughn Vernon                 |
| Value Object           | Eric Evans / Vaughn Vernon                 |
| Aggregate              | Eric Evans / Vaughn Vernon                 |
| Aggregate Boundary     | Vaughn Vernon                              |
| Domain Service         | Eric Evans / Vaughn Vernon                 |
| Repository             | Eric Evans / Martin Fowler / Vaughn Vernon |
| Application Service    | Martin Fowler / Vaughn Vernon              |
| Domain Event           | Vaughn Vernon                              |
| Module                 | Vaughn Vernon                              |
| Persistence Isolation  | Martin Fowler                              |
| Data Mapper            | Martin Fowler                              |
| Dependency Direction   | Robert C. Martin                           |
| Core Domain Protection | Eric Evans / Vaughn Vernon                 |

> **این منابع مبنای نظری و معماری این ADR هستند؛ اما تصمیم نهایی این سند متناسب با Boundaryها، Business Rules و ساختار واقعی General Ledger Microservice اتخاذ شده است.**
