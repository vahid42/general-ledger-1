# ADR-0011 — معماری لایه دامنه (Domain Layer Architecture)

* **وضعیت:** پذیرفته شده (Accepted)
* **تاریخ:** 2026-08-22
* **ADR مرتبط:**

  * ADR-0003 
  * ADR-0004 
  * ADR-0005 
  * ADR-0006 
  * ADR-0010 

---

# 1. زمینه (Context)

سیستم **General Ledger** یک **Bounded Context** مستقل است که به‌صورت یک **General Ledger Microservice** پیاده‌سازی می‌شود.

درون این Bounded Context، سه Subdomain اصلی وجود دارد:

```text
General Ledger

├── Account Head       — Supporting Subdomain
├── Account            — Supporting Subdomain
└── Journal Entry      — Core Subdomain
```

این سه Subdomain در طراحی فعلی در یک مدل یکپارچه Domain قرار دارند و به سه Bounded Context مستقل تقسیم نشده‌اند.

در سطح Tactical DDD نیز سه Aggregate Root اصلی وجود دارد:

```text
General Ledger Bounded Context
│
├── AccountHead Aggregate
├── Account Aggregate
└── JournalEntry Aggregate
```

در این میان، `JournalEntry` به‌عنوان **Core Aggregate** و مرکز اصلی Business Logic و Business Invariants دفترکل شناخته می‌شود.

هدف این ADR تعیین معماری، مسئولیت، وابستگی، ساختار کدی و قوانین Domain Layer در این Bounded Context است.

جزئیات تخصصی مربوط به Entity، Aggregate، Value Object، Domain Service، Domain Event، Repository و Identity در ADRهای مستقل تعریف خواهند شد.

---

# 2. تصمیم (Decision)

## 2.1 Domain به‌عنوان هسته Business Model

Domain Layer هسته Business Model مربوط به General Ledger است.

این لایه مسئول نمایش مفاهیم و رفتارهای واقعی دامنه است و می‌تواند شامل موارد زیر باشد:

* Entities
* Value Objects
* Aggregate Roots
* Aggregate behavior
* Business Rules
* Business Invariants
* Domain Services
* Domain Events
* Domain Exceptions
* Domain Policies
* Domain Specifications

Domain نباید مسئول موارد زیر باشد:

* HTTP
* REST
* Controller
* Database access
* JPA / Hibernate
* Messaging Infrastructure
* Authentication Infrastructure
* External API implementation
* Configuration
* Serialization infrastructure
* Framework lifecycle

اصل کلی:

```text
Domain
  │
  └── Business Model + Business Rules
```

و نه:

```text
Domain
  │
  └── Technical Infrastructure
```

---

# 3. جایگاه Domain در معماری General Ledger

معماری داخلی General Ledger بر اساس Clean Architecture / Onion Architecture سازمان‌دهی می‌شود.

```text
General Ledger Microservice
│
├── Presentation
│
├── Application
│
├── Domain
│
└── Infrastructure
```

جهت وابستگی باید به سمت Domain باشد:

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

Domain نباید به لایه‌های بیرونی وابسته شود.

```text
Domain
  ✕ Presentation
  ✕ Application
  ✕ Infrastructure
  ✕ Database
  ✕ Framework
```

Domain باید مستقل از جزئیات تکنولوژیک باقی بماند.

---

# 4. Domain در محدوده General Ledger Bounded Context

در تصمیم فعلی، سه Subdomain زیر:

```text
Account Head
Account
Journal Entry
```

همگی متعلق به یک:

```text
General Ledger Bounded Context
```

هستند.

بنابراین Domain Layer نیز Domain Model یکپارچه همین Bounded Context را تشکیل می‌دهد.

```text
General Ledger Bounded Context
│
└── Domain
    │
    ├── Account Head
    ├── Account
    └── Journal Entry
```

وجود سه Business Boundary یا سه Aggregate به معنی وجود سه Bounded Context نیست.

همچنین وجود سه Module نیز به معنی وجود سه Microservice نیست.

اصل معماری:

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
     ├── Account Head Module
     ├── Account Module
     └── Journal Entry Module
```

---

# 5. Domain Modules

Domain Layer در سطح کد بر اساس Business Boundary و Aggregate سازمان‌دهی می‌شود.

ساختار اولیه:

```text
domain/

├── common/
├── shared/
├── account-head/
├── account/
└── journal-entry/
```

این Moduleها همگی بخشی از:

```text
General Ledger Bounded Context
```

هستند.

بنابراین:

```text
account-head
account
journal-entry
```

به‌تنهایی Bounded Context یا Microservice محسوب نمی‌شوند.

هدف این ساختار افزایش Cohesion، قابل مشاهده بودن Business Boundaryها و کاهش Coupling بین Aggregateها است.

---

# 6. Aggregate-Oriented Domain Model

Domain بر اساس Aggregateهای اصلی General Ledger سازمان‌دهی می‌شود:

```text
Domain
│
├── account-head/
│   └── AccountHead Aggregate
│
├── account/
│   └── Account Aggregate
│
└── journal-entry/
    └── JournalEntry Aggregate
```

سه Aggregate Root اصلی عبارت‌اند از:

* `AccountHead`
* `Account`
* `JournalEntry`

هر Aggregate مسئول حفظ Invariantهای مربوط به خودش است.

---

# 7. AccountHead Aggregate

`AccountHead` مسئول مدل‌سازی و حفظ قوانین مربوط به ساختار سرفصل‌های دفترکل است.

نمونه مسئولیت‌ها:

```text
AccountHead
│
├── identity
├── hierarchy
├── classification
├── placement
└── account-head rules
```

Business Ruleهای متعلق به Account Head باید در Domain Model همین Aggregate قرار گیرند.

این Aggregate نباید صرفاً به یک CRUD Model تبدیل شود.

---

# 8. Account Aggregate

`Account` مسئول مدل‌سازی و حفظ قوانین مربوط به حساب است.

نمونه مسئولیت‌ها:

```text
Account
│
├── identity
├── account type
├── status
├── account properties
└── account rules
```

Business Ruleهای مربوط به Account باید تا حد امکان در خود Aggregate قرار گیرند.

---

# 9. JournalEntry Aggregate — Core Aggregate

`JournalEntry` مهم‌ترین Aggregate در General Ledger است.

این Aggregate نماینده Core Business Capability سیستم بوده و باید محل اصلی Business Rules مربوط به ثبت سند باشد.

نمونه مسئولیت‌ها:

```text
JournalEntry
│
├── validate debit / credit
├── validate balancing
├── validate journal lines
├── validate posting rules
├── determine posting state
└── post
```

اصل معماری:

```text
JournalEntry
      │
      ▼
Domain Behavior
      │
      ▼
Business Invariants
```

`JournalEntry` نباید صرفاً به:

```text
CRUD Entity
```

یا:

```text
Anemic Data Model
```

تبدیل شود.

Core بودن Journal Entry باید در طراحی واقعی Domain نیز قابل مشاهده باشد.

---

# 10. Rich Domain Model

General Ledger از **Rich Domain Model** استفاده می‌کند.

Business Behavior باید تا حد امکان در Aggregate و Domain Model قرار گیرد.

مثال:

```java
public class Account {

    public void debit(Money amount) {
        // Business rules
    }
}
```

در مقابل، Business Logic نباید صرفاً در Application Service قرار گیرد:

```java
public class AccountService {

    public void debit(Account account, Money amount) {
        // Business rules
    }
}
```

Application Layer مسئول اجرای Use Case و orchestration است، در حالی که Domain مالک Business Behavior و Invariants است.

---

# 11. محل قرارگیری Business Rules

Business Rule باید در نزدیک‌ترین و مناسب‌ترین بخش Domain قرار گیرد.

اولویت کلی:

```text
Aggregate / Entity
        ↓
Value Object
        ↓
Domain Service
```

Application Service محل پیش‌فرض Business Rule نیست.

Application Service زمانی وارد عمل می‌شود که نیاز به orchestration چند Domain Object یا چند Aggregate وجود داشته باشد.

اصل:

```text
Business Rule
      │
      ▼
Domain
```

و:

```text
Use Case Orchestration
      │
      ▼
Application
```

---

# 12. Aggregate Interaction

Aggregateها باید تا حد امکان مستقل باقی بمانند.

یک Aggregate نباید برای اجرای رفتار خود مستقیماً به Repository یا Infrastructure وابسته شود.

به‌عنوان نمونه، طراحی زیر نباید به‌عنوان الگوی پیش‌فرض استفاده شود:

```text
JournalEntry
      │
      ▼
AccountRepository
```

در صورتی که یک Use Case نیازمند هماهنگی چند Aggregate باشد، این orchestration در Application Layer انجام می‌شود.

برای مثال:

```text
CreateJournalEntry
        │
        ├── load / validate Account
        │
        ├── validate AccountHead
        │
        └── create JournalEntry
```

در این مدل:

* Application مسئول orchestration است.
* Account قوانین خودش را حفظ می‌کند.
* AccountHead قوانین خودش را حفظ می‌کند.
* JournalEntry قوانین مربوط به سند را حفظ می‌کند.

بنابراین Application Layer جایگزین Domain Model نمی‌شود.

---

# 13. Repository Abstraction

Repository یک Persistence Abstraction است و نباید با Business Service یا Domain Service اشتباه گرفته شود.

در General Ledger می‌توان برای Aggregateهای اصلی Repository Portهای مستقل داشت:

```text
AccountHeadRepository
AccountRepository
JournalEntryRepository
```

Repository Port باید از Persistence Implementation جدا باشد.

ساختار مفهومی:

```text
Domain / Application
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

Implementation مربوط به Database و ORM در Infrastructure قرار می‌گیرد.

Aggregate نباید Repository Implementation را بشناسد یا مستقیماً آن را invoke کند.

جزئیات Repository Abstraction در:

**ADR-0016 — Repository Abstraction Strategy**

تعریف خواهد شد.

---

# 14. Domain Contract و Repository Port

هر Interface موجود در Domain یا Application الزاماً Repository نیست.

دو مفهوم باید از یکدیگر تفکیک شوند:

```text
Repository Port
    │
    └── Persistence abstraction
```

و:

```text
Internal Contract / Port
    │
    └── Capability / communication abstraction
```

این تفکیک مانع از آن می‌شود که تمام Abstractionهای سیستم به شکل Repository مدل شوند.

Implementation مربوط به تکنولوژی‌های خارجی باید خارج از Domain قرار گیرد.

---

# 15. Domain Interfaces

در صورتی که Business Logic برای انجام یک عملیات واقعاً به یک قابلیت خارجی نیاز داشته باشد، وابستگی باید از طریق Contract مناسب مدل شود.

Domain نباید Implementation تکنولوژیک را بشناسد.

برای مثال، در صورت نیاز واقعی به نرخ ارز:

```java
public interface ExchangeRateProvider {

    MoneyRate getRate(
        Currency source,
        Currency target
    );
}
```

Domain فقط Contract را می‌شناسد و نباید API Client، HTTP Client یا Implementation تکنولوژیک آن را بشناسد.

محل قرارگیری دقیق این Portها باید بر اساس مالکیت وابستگی و Use Case مشخص شود و نباید صرفاً تمام Interfaceها در Domain قرار داده شوند.

---

# 16. Framework Independence

Domain باید Framework Independent باشد.

استفاده مستقیم از Frameworkهایی مانند Spring در Domain مجاز نیست.

به‌صورت پیش‌فرض Annotationهایی مانند موارد زیر نباید در Domain استفاده شوند:

```java
@Component
@Service
@Repository
@Autowired
@Configuration
@Entity
```

همچنین Domain نباید به موارد زیر وابسته باشد:

```text
Spring
Spring Boot
JPA
Hibernate
Servlet API
HTTP
REST
Database Driver
Message Broker
```

هدف این است که Domain بدون اجرای Spring Application Context قابل تست باشد.

---

# 17. Persistence Independence

Domain Model نباید بر اساس Database یا ORM طراحی شود.

مدل مفهومی:

```text
Domain Model
      │
      │ Mapping / Persistence Adapter
      ▼
Persistence Model
      │
      ▼
Database
```

Database و ORM باید خود را با نیازهای Domain تطبیق دهند، نه اینکه Business Model بر اساس محدودیت‌های ORM طراحی شود.

این تصمیم با:

**ADR-0010 — Database Strategy**

هماهنگ است.

---

# 18. Time و سایر External Concerns

Domain باید از وابستگی مستقیم به محیط اجرایی اجتناب کند.

در مواردی مانند:

* Time
* Randomness
* External capability
* Environment-dependent behavior

در صورت وجود Business Requirement واقعی، باید Abstraction مناسب تعریف شود.

برای نمونه:

```java
public interface Clock {

    Instant now();
}
```

هدف این نیست که برای هر API یا کلاس Java یک Abstraction ایجاد شود؛ بلکه فقط وابستگی‌هایی که برای Business Behavior مهم هستند باید Abstract شوند.

---

# 19. Domain Exceptions

خطاهای ناشی از Business Rule باید به‌صورت Domain Error یا Domain Exception مدل شوند.

برای مثال:

```java
public class InvalidJournalEntryException
        extends DomainException {

}
```

Domain Exception نباید به HTTP یا Framework وابسته باشد.

برای مثال، Domain نباید چنین کدی داشته باشد:

```java
throw new ResponseStatusException(...);
```

تبدیل Domain Error به HTTP Response یا API Error باید خارج از Domain انجام شود.

جزئیات این موضوع در:

**ADR-0017 — Domain Exception & Error Strategy**

تعریف خواهد شد.

---

# 20. Domain Events

Domain می‌تواند برای اعلام رخدادهای مهم Business از Domain Event استفاده کند.

Domain Event باید یک مفهوم Business باشد و نباید مستقیماً به Message Broker یا تکنولوژی Messaging وابسته باشد.

مدل کلی:

```text
Domain
   │
   ▼
Domain Event
   │
   ▼
Application / Infrastructure
   │
   ▼
Messaging Infrastructure
```

برای مثال:

```text
JournalEntryPosted
```

یک Business Event است، در حالی که:

```text
KafkaMessage
RabbitMessage
```

مفهوم Infrastructure هستند.

جزئیات Domain Event در:

**ADR-0015 — Domain Event Strategy**

تعریف خواهد شد.

---

# 21. Domain Common

پوشه `common` فقط برای مفاهیم پایه و عمومی Domain استفاده می‌شود که متعلق به یک Aggregate خاص نیستند.

نمونه:

```text
common/

├── AggregateRoot
├── Entity
├── ValueObject
├── Identifier
├── DomainEvent
└── DomainException
```

`common` نباید محل قرار دادن Business Logic مشترک صرفاً به دلیل جلوگیری از Duplicate Code باشد.

به‌خصوص Business Logic اختصاصی Aggregateهای زیر نباید در `common` قرار گیرد:

```text
AccountHead
Account
JournalEntry
```

---

# 22. Domain Shared Concepts

پوشه `shared` برای مفاهیمی است که واقعاً در چند بخش از Domain مورد استفاده قرار می‌گیرند و بخشی از مدل مفهومی مشترک General Ledger هستند.

نمونه:

```text
shared/

├── Money
├── Currency
├── FiscalPeriod
└── shared value objects / enumerations
```

قرار گرفتن یک مفهوم در `shared` باید بر اساس اشتراک واقعی Domain Model باشد، نه صرفاً جلوگیری از Duplicate Code.

`shared` نباید به محل انباشت:

```text
Utility
Helper
Generic Service
Common Business Logic
```

تبدیل شود.

همچنین این `shared` داخلی Domain نباید با مفهوم **Shared Kernel بین Bounded Contextها** اشتباه گرفته شود.

در معماری فعلی تنها یک General Ledger Bounded Context وجود دارد.

---

# 23. Aggregate-Oriented Packaging

ساختار Domain باید مرز Aggregateها را در ساختار کد قابل مشاهده کند.

ساختار اولیه:

```text
domain/

├── common/
├── shared/
│
├── account-head/
│   └── AccountHead Aggregate
│
├── account/
│   └── Account Aggregate
│
└── journal-entry/
    └── JournalEntry Aggregate
```

هر Aggregate می‌تواند اجزای مورد نیاز خودش را در محدوده همان Package نگهداری کند.

برای مثال:

```text
account/

├── Account.java
├── AccountId.java
├── AccountRepository.java
├── policies/
└── events/
```

و:

```text
journal-entry/

├── JournalEntry.java
├── JournalEntryId.java
├── JournalLine.java
├── JournalEntryRepository.java
├── policies/
└── events/
```

این ساختار به این معنی نیست که هر Aggregate باید تمام این اجزا را داشته باشد.

هر جزء فقط در صورت وجود نیاز واقعی Domain ایجاد می‌شود.

---

# 24. Module Boundary و Aggregate Boundary

در طراحی فعلی، Moduleها بر اساس Business Boundary و Aggregateهای اصلی سازمان‌دهی می‌شوند:

```text
General Ledger Microservice
│
└── Domain
    │
    ├── account-head/
    │
    ├── account/
    │
    └── journal-entry/
```

این Moduleها:

* Bounded Context مستقل نیستند.
* Microservice مستقل نیستند.
* Database مستقل بودن آنها الزام معماری نیست.
* صرفاً به دلیل Module بودن، نباید Contractهای داخلی یکدیگر را مستقیماً مصرف کنند.

مرز Module باید به حفظ مرز Aggregate و کاهش Coupling کمک کند.

---

# 25. Core Domain Protection

از آنجا که `JournalEntry` Core Subdomain و Core Aggregate سیستم است، طراحی آن باید در اولویت معماری قرار گیرد.

Account و AccountHead به‌عنوان Supporting Subdomain باید نیازهای Core Domain را پشتیبانی کنند، بدون اینکه باعث پیچیده شدن غیرضروری مدل JournalEntry شوند.

اصل:

```text
AccountHead
      │
      └── Supporting

Account
      │
      └── Supporting

JournalEntry
      │
      └── CORE
```

Supporting Domainها نباید باعث شوند که Business Ruleهای Journal Entry در:

```text
Application Service
CRUD Service
Repository
Infrastructure
```

پراکنده شوند.

---

# 26. Domain Service

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

نیست.

Business Rule ابتدا باید در Aggregate یا سایر Domain Objects مناسب قرار گیرد.

Domain Service فقط زمانی استفاده می‌شود که:

1. Rule واقعاً Domain Logic باشد.
2. Rule ذاتاً متعلق به یک Aggregate مشخص نباشد.
3. قرار دادن Rule در یک Aggregate باعث ایجاد Coupling نامناسب شود.

بنابراین Domain Service ابزار تکمیل Rich Domain Model است، نه جایگزین Aggregate.

جزئیات این موضوع در:

**ADR-0014 — Domain Service Strategy**

تعریف خواهد شد.

---

# 27. Domain Testability

Domain باید بدون موارد زیر قابل تست باشد:

* Spring Context
* Database
* Network
* Message Broker
* External API
* Infrastructure Configuration

Unit Testهای Domain باید:

* سریع باشند.
* deterministic باشند.
* مستقل از Infrastructure باشند.
* Business Invariants را مستقیماً تست کنند.

به‌خصوص `JournalEntry` باید از طریق Unit Testهای Domain به‌صورت مستقیم تست شود.

---

# 28. Architecture Rules

قوانین زیر باید توسط Architecture Test و Code Review enforce شوند:

* Domain نباید به Presentation وابسته باشد.
* Domain نباید به Application وابسته باشد.
* Domain نباید به Infrastructure وابسته باشد.
* Domain نباید به Database وابسته باشد.
* Domain نباید به Spring وابسته باشد.
* Domain نباید به JPA / Hibernate وابسته باشد.
* Domain نباید شامل HTTP-specific Code باشد.
* Domain نباید شامل Infrastructure Configuration باشد.
* Domain Model نباید بر اساس Persistence Model طراحی شود.
* Business Ruleهای Aggregate باید تا حد امکان داخل همان Aggregate قرار گیرند.
* Application Layer مسئول orchestration است، نه مالک Business Ruleهای Domain.
* Aggregate نباید Repository Implementation را invoke کند.
* Repository Port باید از Persistence Implementation جدا باشد.
* وجود Aggregate به‌تنهایی دلیل ایجاد Domain Service نیست.
* وجود Subdomain به‌تنهایی دلیل ایجاد Bounded Context جدید نیست.
* وجود Module به‌تنهایی دلیل ایجاد Microservice جدید نیست.
* `account-head`، `account` و `journal-entry` باید به‌عنوان Moduleهای یک General Ledger Bounded Context باقی بمانند، مگر اینکه تصمیم معماری جدیدی ثبت شود.
* Business Logic اختصاصی Aggregateها نباید در `common` یا `shared` قرار گیرد.
* `shared` نباید به محل عمومی برای قرار دادن Utility و Business Logic تبدیل شود.
* Aggregateها باید مرز Consistency مشخص داشته باشند.
* Aggregateها نباید برای اجرای رفتار خود به Implementationهای Infrastructure وابسته شوند.
* JournalEntry باید به‌عنوان Core Aggregate از طراحی CRUD-centric مصون بماند.

---

# 29. ساختار نهایی Domain

ساختار مفهومی نهایی:

```text
General Ledger Microservice
│
├── Presentation
│
├── Application
│
├── Domain
│   │
│   ├── common/
│   │
│   ├── shared/
│   │
│   ├── account-head/
│   │   └── AccountHead Aggregate
│   │
│   ├── account/
│   │   └── Account Aggregate
│   │
│   └── journal-entry/
│       └── JournalEntry Aggregate
│
└── Infrastructure
```

و از دید DDD:

```text
General Ledger
│
├── Account Head
│   └── Supporting Subdomain
│
├── Account
│   └── Supporting Subdomain
│
└── Journal Entry
    └── Core Subdomain
            │
            ▼
      Core Aggregate
```

---

# 30. پیامدها (Consequences)

## مزایا

* استقلال Business Logic از Framework
* تست‌پذیری بالا
* کاهش Coupling
* افزایش Cohesion در سطح Aggregate
* حفظ مرزهای Domain
* جلوگیری از Anemic Domain Model
* تمرکز معماری روی Core Domain
* قابل مشاهده بودن Aggregate Boundary در ساختار کد
* جلوگیری از تبدیل JournalEntry به CRUD Model
* جداسازی Persistence از Domain Model
* امکان تغییر Database و ORM بدون تغییر Business Model
* امکان توسعه مستقل Moduleهای داخلی General Ledger
* هم‌راستایی ساختار کد با مدل Domain
* آماده بودن معماری برای تکامل Bounded Context در آینده
* امکان استخراج Module در آینده در صورت وجود دلیل معماری واقعی

## معایب

* افزایش تعداد کلاس‌ها
* نیاز به طراحی دقیق Aggregateها
* نیاز به Mapping بین Domain و Persistence در صورت جداسازی مدل‌ها
* نیاز به طراحی صحیح Portها و Abstractionها
* پیچیدگی بیشتر نسبت به معماری CRUD
* نیاز به Architecture Test و Code Review مستمر
* احتمال سوءاستفاده از `common` و `shared`
* نیاز به کنترل دقیق ارتباط بین Aggregateها
* نیاز به تصمیم‌گیری مستمر درباره محل مناسب Business Ruleها

---

# 31. وضعیت اجرا (Implementation Status)

این ADR به‌عنوان قانون پایه Domain Layer در **General Ledger Bounded Context** اعمال می‌شود.

General Ledger به‌صورت یک **Microservice مستقل** اجرا می‌شود و Domain داخلی آن بر اساس Moduleهای Aggregate-oriented سازمان‌دهی خواهد شد.

ساختار اولیه:

```text
domain/

├── common/
├── shared/
├── account-head/
├── account/
└── journal-entry/
```

در این ساختار:

```text
account-head
    → AccountHead Aggregate

account
    → Account Aggregate

journal-entry
    → JournalEntry Aggregate
```

`JournalEntry` به‌عنوان Core Aggregate و مرکز اصلی Business Logic دفترکل در نظر گرفته می‌شود.

جزئیات تخصصی اجزای Domain در ADRهای زیر مشخص خواهند شد:

* **ADR-0012 — Entity & Aggregate Strategy**
* **ADR-0013 — Value Object Strategy**
* **ADR-0014 — Domain Service Strategy**
* **ADR-0015 — Domain Event Strategy**
* **ADR-0016 — Repository Abstraction Strategy**
* **ADR-0017 — Domain Exception & Error Strategy**
* **ADR-0018 — Domain Identity Strategy**

---

# 32. References

1. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**
2. Robert C. Martin — **Clean Architecture: A Craftsman's Guide to Software Structure and Design**
3. Vaughn Vernon — **Implementing Domain-Driven Design**
4. Vaughn Vernon — **Domain-Driven Design Distilled**
5. Martin Fowler — **Domain Model**
6. Martin Fowler — **Anemic Domain Model**
7. Alistair Cockburn — **Hexagonal Architecture**
8. Jeffrey Palermo — **The Onion Architecture**
9. Chris Richardson — **Microservices Patterns**
10. ArchUnit — **Architecture Testing Documentation**
