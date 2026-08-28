# ADR-0012 — استراتژی Entity و Aggregate (Entity & Aggregate Strategy)

* **وضعیت:** پذیرفته شده (Accepted)
* **تاریخ:** 2026-08-22
* **ADRهای مرتبط:**

  * ADR-0011 
  * ADR-0013 
  * ADR-0014 
  * ADR-0015 
  * ADR-0016 
  * ADR-0018 

---

# 1. زمینه (Context)

در معماری **Domain-Driven Design**، Domain Model نباید صرفاً مجموعه‌ای از داده‌ها باشد.

General Ledger از یک **Bounded Context** تشکیل شده است که درون آن سه Aggregate Root اصلی وجود دارد:

```text
General Ledger Bounded Context
│
├── AccountHead Aggregate
├── Account Aggregate
└── JournalEntry Aggregate
```

این Aggregateها متعلق به یک General Ledger Bounded Context و یک General Ledger Microservice هستند.

در این میان:

```text
AccountHead
    └── Supporting Aggregate

Account
    └── Supporting Aggregate

JournalEntry
    └── Core Aggregate
```

`JournalEntry` مهم‌ترین Aggregate از نظر Business Value و Core Domain است.

هدف این ADR تعیین استراتژی Entity و Aggregate، نحوه تعیین Aggregate Boundary، مسئولیت Aggregate Root، نحوه تعامل Aggregateها، Repository Boundary و قواعد حفظ Business Invariantها است.

---

# 2. Entity

Entity یک مفهوم Domain است که دارای **Identity پایدار** و Lifecycle مشخص است.

Entity با Identity خود شناخته می‌شود، نه صرفاً با مقدار Attributeهای آن.

برای مثال:

```java
public class Account {

    private AccountId id;
    private AccountStatus status;
}
```

تغییر `status` باعث ایجاد Entity جدید نمی‌شود؛ Identity حساب همچنان ثابت باقی می‌ماند.

بنابراین:

```text
Entity Identity
      │
      └── ثابت در طول Lifecycle

Entity State
      │
      └── قابل تغییر از طریق Business Behavior
```

هر مفهوم Domain الزاماً Entity نیست.

اگر یک مفهوم:

* Identity مستقل ندارد.
* Lifecycle مستقل ندارد.
* صرفاً بر اساس مقدار خود معنا دارد.

احتمالاً باید به‌عنوان Value Object مدل شود.

جزئیات Value Object در:

**ADR-0013 — Value Object Strategy**

تعریف خواهد شد.

---

# 3. Entity باید رفتار داشته باشد

Entity نباید صرفاً Data Holder باشد.

Business Behavior مربوط به Entity باید تا حد امکان در خود Entity یا Aggregate قرار گیرد.

نامناسب:

```java
account.setBalance(
    account.getBalance().subtract(amount)
);
```

ترجیحاً:

```java
account.withdraw(amount);
```

در روش دوم، Entity خودش مسئول اجرای رفتار و محافظت از Ruleهای مربوط به State خود است.

اصل:

```text
Data
 +
Behavior
 +
Invariant Protection
 =
Rich Domain Model
```

---

# 4. تغییر State از طریق Business Behavior

تا حد امکان تغییر State نباید از طریق Setterهای عمومی انجام شود.

نامناسب:

```java
account.setStatus(ACTIVE);
```

ترجیحاً:

```java
account.activate();
```

یا:

```java
account.close();
```

هدف این است که State فقط از طریق رفتارهای معتبر Domain تغییر کند.

Setter عمومی تنها زمانی قابل قبول است که واقعاً بخشی از Domain Behavior نباشد و استفاده از آن باعث دور زدن Business Rule نشود.

---

# 5. Aggregate

Aggregate یک مرز مشخص از Entityها و Value Objectهایی است که برای حفظ مجموعه‌ای از **Business Invariantها** و Consistency به یکدیگر وابسته‌اند.

Aggregate مرز اصلی برای:

* Consistency
* Invariant Protection
* State Transition
* Domain Behavior
* Transaction Coordination

است.

ساختار مفهومی:

```text
Aggregate
│
└── Aggregate Root
    │
    ├── Entity
    ├── Entity
    └── Value Object
```

Aggregate باید به‌عنوان یک واحد Domain Model طراحی شود، نه به‌عنوان انعکاس ساختار Database.

---

# 6. Aggregate Root

هر Aggregate دقیقاً یک Aggregate Root دارد.

Aggregate Root تنها نقطه عمومی برای تعامل با State و Behavior داخلی Aggregate است.

```text
Application
     │
     ▼
Aggregate Root
     │
     ├── Entity
     ├── Entity
     └── Value Object
```

کد خارج از Aggregate نباید مستقیماً State Entityهای داخلی را تغییر دهد.

برای مثال اگر `JournalEntry` دارای `JournalLine` داخلی باشد:

```text
JournalEntry
│
├── JournalLine
├── JournalLine
└── ...
```

تغییر Line باید تحت کنترل `JournalEntry` انجام شود، نه اینکه Application مستقیماً Entity داخلی را تغییر دهد.

---

# 7. Aggregateهای General Ledger

بر اساس baseline معماری General Ledger، سه Aggregate Root اصلی عبارت‌اند از:

```text
General Ledger
│
├── AccountHead Aggregate
├── Account Aggregate
└── JournalEntry Aggregate
```

این Aggregateها سه Bounded Context مستقل نیستند.

هر سه متعلق به:

```text
General Ledger Bounded Context
```

و:

```text
General Ledger Microservice
```

هستند.

---

# 8. AccountHead Aggregate

`AccountHead` Aggregate مسئول حفظ Invariantهای مربوط به ساختار سرفصل دفترکل است.

نمونه مفاهیم:

```text
AccountHead
│
├── identity
├── hierarchy
├── classification
├── placement
└── account-head rules
```

Business Ruleهای مربوط به ساختار و وضعیت Account Head باید در این Aggregate یا Domain Model مرتبط با آن قرار گیرند.

---

# 9. Account Aggregate

`Account` Aggregate مسئول حفظ Invariantهای مربوط به حساب است.

نمونه مفاهیم:

```text
Account
│
├── identity
├── account type
├── status
├── account properties
└── account rules
```

رفتارهای مربوط به Account باید تا حد امکان توسط خود Aggregate کنترل شوند.

برای مثال:

```java
account.activate();
account.deactivate();
account.close();
```

به جای تغییر مستقیم State:

```java
account.setStatus(...);
```

---

# 10. JournalEntry Aggregate — Core Aggregate

`JournalEntry` مهم‌ترین Aggregate در General Ledger است.

این Aggregate نماینده Core Business Capability دفترکل بوده و باید مرکز اصلی Business Rules مربوط به ایجاد و ثبت سند باشد.

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

اصل:

```text
JournalEntry
      │
      ▼
Business Behavior
      │
      ▼
Business Invariants
```

`JournalEntry` نباید به یک CRUD Entity تبدیل شود.

Business Logic اصلی نباید به:

```text
Controller
Application Service
Repository
Infrastructure
```

پراکنده شود.

---

# 11. Aggregate Boundary بر اساس Business Invariant

مهم‌ترین معیار تعیین Aggregate Boundary، **Business Invariant** است.

ارتباط مفهومی بین دو Object به‌تنهایی دلیل کافی برای قرار دادن آن‌ها در یک Aggregate نیست.

سؤالات اصلی:

1. آیا این مفاهیم یک Invariant مشترک دارند؟
2. آیا باید همیشه با هم Consistent باشند؟
3. آیا تغییر آن‌ها باید تحت کنترل یک Root انجام شود؟
4. آیا باید به‌عنوان یک واحد Domain تغییر کنند؟
5. آیا قرار دادن آن‌ها در یک Aggregate باعث ایجاد Coupling شدید نمی‌شود؟
6. آیا Aggregate همچنان اندازه و Complexity مناسبی دارد؟

بنابراین:

```text
Business Relationship
        ≠
Aggregate Boundary
```

بلکه:

```text
Business Invariant
        ↓
Consistency Requirement
        ↓
Aggregate Boundary
```

---

# 12. Aggregate نباید بر اساس Database طراحی شود

Aggregate نباید صرفاً بر اساس:

* Table
* Foreign Key
* Database Relationship
* ORM Mapping
* Join
* Lazy Loading

تعیین شود.

برای مثال وجود Foreign Key بین دو Table به‌تنهایی به معنی قرار گرفتن Entityهای مربوطه در یک Aggregate نیست.

Aggregate باید بر اساس Business Model طراحی شود.

```text
Business Model
      │
      ▼
Aggregate Boundary
      │
      ▼
Persistence Mapping
      │
      ▼
Database
```

نه:

```text
Database Tables
      │
      ▼
Aggregate Boundary
```

---

# 13. Aggregate باید تا حد امکان کوچک باشد

Aggregate باید به اندازه‌ای بزرگ باشد که Invariantهای لازم را حفظ کند، اما نباید شامل تمام Objectهای مرتبط با یک Business Concept شود.

Aggregate بیش از حد بزرگ می‌تواند باعث:

* افزایش Locking
* افزایش Transaction Scope
* کاهش Concurrency
* افزایش Coupling
* افزایش Complexity
* کاهش Performance

شود.

اصل:

> Aggregate باید کوچک‌ترین Boundaryای باشد که بتواند Invariantهای لازم را به‌صورت قابل اعتماد حفظ کند.

---

# 14. ارتباط بین Aggregateها

Aggregateها باید تا حد امکان مستقل باقی بمانند.

به‌صورت پیش‌فرض، Aggregate نباید Graph کامل Aggregate دیگری را در اختیار داشته باشد.

ترجیح داده می‌شود ارتباط بین Aggregateها از طریق Identity یا Contract مناسب انجام شود.

برای مثال:

```java
public class JournalEntry {

    private AccountId accountId;
}
```

به جای نگهداری مستقیم Aggregate دیگر:

```java
public class JournalEntry {

    private Account account;
}
```

هدف اصلی کاهش Coupling و جلوگیری از ایجاد Aggregate Graphهای بزرگ است.

---

# 15. تعامل JournalEntry با Account و AccountHead

در General Ledger، `JournalEntry` برای اجرای Use Caseهای مربوط به سند ممکن است به اطلاعات `Account` و `AccountHead` نیاز داشته باشد.

این نیاز به‌تنهایی به معنی قرار دادن این Aggregateها در یک Aggregate واحد نیست.

مدل پیشنهادی:

```text
CreateJournalEntry
        │
        ├── load / validate Account
        │
        ├── validate AccountHead
        │
        └── create JournalEntry
```

Application Layer مسئول orchestration است.

در مقابل:

```text
JournalEntry
        │
        └── AccountRepository
```

به‌عنوان الگوی مستقیم و پیش‌فرض مجاز نیست.

Aggregate نباید برای اجرای رفتار خود Repository را invoke کند.

---

# 16. Aggregate Root تنها نقطه دسترسی به Internal Entity

کد خارج از Aggregate نباید مستقیماً Entity داخلی Aggregate را تغییر دهد.

نامناسب:

```text
Application
   │
   ├──► Aggregate Root
   │
   └──► Internal Entity ❌
```

صحیح:

```text
Application
      │
      ▼
Aggregate Root
      │
      ├── Entity
      ├── Entity
      └── Value Object
```

Aggregate Root باید State Transitionهای داخلی را کنترل کند.

---

# 17. Repository فقط برای Aggregate Root

Repository باید برای Aggregate Root تعریف شود، نه برای Entityهای داخلی.

صحیح:

```text
AccountRepository
JournalEntryRepository
AccountHeadRepository
```

نامناسب:

```text
JournalEntryRepository
JournalLineRepository
```

در صورتی که `JournalLine` یک Entity داخلی `JournalEntry` باشد.

Repository مسئول Persistence Aggregate است، نه Persistence تک‌تک اجزای داخلی آن به‌صورت مستقل.

جزئیات این موضوع در:

**ADR-0016 — Repository Abstraction Strategy**

تعریف خواهد شد.

---

# 18. Aggregate و Transaction Boundary

Aggregate یک **Consistency Boundary** است.

در حالت معمول، Transaction باید تغییرات مربوط به یک Aggregate را به‌عنوان یک واحد Consistency مدیریت کند.

```text
Transaction
     │
     ▼
Aggregate
     │
     ├── Entity
     ├── Entity
     └── Value Object
```

اما این اصل به معنی آن نیست که هر Transaction در تمام شرایط فقط باید یک Aggregate را لمس کند.

اگر یک Use Case نیازمند هماهنگی چند Aggregate باشد، Application Layer می‌تواند آن‌ها را orchestration کند.

هدف اصلی جلوگیری از ایجاد Transactionهای بزرگ صرفاً برای حفظ Consistencyای است که واقعاً به یک Aggregate مشترک نیاز ندارد.

---

# 19. Cross-Aggregate Consistency

اگر یک Business Rule به چند Aggregate مربوط باشد، ابتدا باید بررسی شود که آیا Aggregate Boundary به‌درستی تعیین شده است.

اگر Rule واقعاً نیازمند Consistency اتمیک بین چند مفهوم است، احتمالاً باید مرز Aggregate دوباره بررسی شود.

اگر استقلال Aggregateها صحیح باشد، Consistency بین آن‌ها می‌تواند با مکانیزم‌هایی مانند:

* Application Orchestration
* Domain Event
* Eventual Consistency
* Process Manager
* Saga

مدیریت شود.

اما استفاده از این تکنیک‌ها نباید صرفاً برای پنهان کردن Aggregate Boundary اشتباه باشد.

---

# 20. Entity Lifecycle

Entity باید Lifecycle مشخصی داشته باشد.

برای مثال:

```text
Create
  ↓
Active
  ↓
State Changes
  ↓
Inactive / Closed
```

تغییر Lifecycle باید از طریق Business Behavior انجام شود.

نامناسب:

```java
account.setStatus(CLOSED);
```

ترجیحاً:

```java
account.close();
```

Business Rule مربوط به Transition باید توسط Domain Model enforce شود.

---

# 21. Entity Identity

Identity Entity باید مستقل از State آن باشد.

برای Entityهایی که Identity مشخص دارند، استفاده از Type مشخص برای Identity ترجیح داده می‌شود.

برای مثال:

```java
public record AccountId(UUID value) {
}
```

به جای انتشار مستقیم `UUID` در تمام بخش‌های Domain.

جزئیات Identity در:

**ADR-0018 — Domain Identity Strategy**

تعریف خواهد شد.

---

# 22. Entity Equality

Equality در Entity باید بر اساس Identity تعریف شود، نه تمام Attributeها.

برای مثال:

```text
Account A
id      = 100
balance = 1000

Account B
id      = 100
balance = 5000
```

از دید Entity Identity، این دو Object می‌توانند نماینده یک Account باشند.

در مقابل، Value Objectها معمولاً بر اساس Value Equality مقایسه می‌شوند.

جزئیات Value Object در ADR-0013 مشخص خواهد شد.

---

# 23. Aggregate Creation

Aggregate باید از لحظه ایجاد در یک State معتبر قرار داشته باشد.

نباید امکان ایجاد Aggregate با State ناقص یا نامعتبر وجود داشته باشد.

نامناسب:

```java
new Account(
    null,
    null,
    null,
    null
);
```

ترجیحاً:

```java
Account.open(
    accountId,
    accountType,
    currency
);
```

Factory Method یا Constructor باید Invariantهای اولیه Aggregate را enforce کند.

---

# 24. Invariant Protection

Aggregate Root مسئول حفاظت از Invariantهای Aggregate است.

برای مثال:

```java
public void withdraw(Money amount) {

    if (balance.isLessThan(amount)) {
        throw new InsufficientBalanceException();
    }

    balance = balance.subtract(amount);
}
```

کد خارج از Aggregate نباید بتواند با تغییر مستقیم State، Invariant را دور بزند.

برای `JournalEntry` این اصل اهمیت بیشتری دارد، زیرا Balanced بودن سند و سایر Posting Rules بخشی از Core Domain هستند.

---

# 25. Aggregate State Mutation

State Mutation باید از طریق رفتارهای Domain انجام شود.

نامناسب:

```java
journalEntry.setStatus(POSTED);
```

ترجیحاً:

```java
journalEntry.post();
```

یا:

```java
journalEntry.cancel();
```

هر State Transition باید در صورت نیاز Business Ruleهای مربوط به آن Transition را enforce کند.

---

# 26. Persistence Independence

Entity و Aggregate نباید بر اساس نیاز ORM طراحی شوند.

Domain Model نباید به موارد زیر وابسته باشد:

* JPA Entity
* Hibernate Proxy
* Lazy Loading
* ORM Lifecycle
* Database Relationship
* Persistence Callback

Persistence Mapping باید خارج از Domain و در Infrastructure انجام شود.

```text
Domain Aggregate
       │
       ▼
Persistence Adapter
       │
       ▼
Persistence Model
       │
       ▼
Database
```

---

# 27. Aggregate Design Criteria

برای تصمیم‌گیری درباره Aggregate Boundary، معیارهای زیر استفاده می‌شوند:

1. Business Invariant
2. Consistency Requirement
3. Transaction Requirement
4. State Ownership
5. Aggregate Root Responsibility
6. Concurrency
7. Performance
8. Coupling
9. Lifecycle
10. Business Meaning

ارتباط Database یا Object Graph به‌تنهایی معیار کافی نیست.

---

# 28. Aggregate-Oriented Packaging

ساختار Package باید مرز Aggregateها را در کد قابل مشاهده کند.

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

این Moduleها متعلق به یک Bounded Context هستند:

```text
General Ledger Bounded Context
```

و به‌صورت یک Microservice اجرا می‌شوند:

```text
General Ledger Microservice
```

بنابراین:

```text
Aggregate
    ≠
Module
    ≠
Bounded Context
    ≠
Microservice
```

---

# 29. Domain Service و Aggregate

وجود Aggregate به معنی ایجاد Domain Service برای آن Aggregate نیست.

نباید صرفاً بر اساس Aggregateهای موجود این Serviceها ایجاد شوند:

```text
AccountHeadService
AccountService
JournalEntryService
```

Business Rule ابتدا باید در Aggregate قرار گیرد.

Domain Service زمانی استفاده می‌شود که Rule:

* واقعاً Domain Logic باشد.
* به یک Aggregate مشخص تعلق نداشته باشد.
* قرار دادن آن در یک Aggregate باعث Coupling نامناسب شود.

جزئیات در:

**ADR-0014 — Domain Service Strategy**

تعریف خواهد شد.

---

# 30. Domain Event و Aggregate

Aggregate می‌تواند در نتیجه تغییرات مهم Domain Event ایجاد کند.

برای مثال:

```text
JournalEntry
      │
      │ post()
      ▼
JournalEntryPosted
```

Domain Event باید مفهوم Domain باشد و نباید مستقیماً به Message Broker وابسته شود.

انتشار Event و تعامل با Messaging Infrastructure خارج از Aggregate انجام می‌شود.

جزئیات در:

**ADR-0015 — Domain Event Strategy**

تعریف خواهد شد.

---

# قوانین (Rules)

* هر Aggregate دقیقاً یک Aggregate Root دارد.
* Aggregate Root تنها نقطه عمومی برای تغییر State داخلی Aggregate است.
* Entity داخلی Aggregate نباید خارج از Aggregate مستقیماً تغییر کند.
* Repository فقط برای Aggregate Root تعریف می‌شود.
* Aggregate نباید Repository Implementation را invoke کند.
* Aggregate Boundary باید بر اساس Business Invariant تعیین شود.
* Aggregate نباید صرفاً بر اساس Database Relationship طراحی شود.
* Aggregate نباید صرفاً بر اساس ORM Mapping طراحی شود.
* Aggregateها باید تا حد امکان کوچک و Cohesive باشند.
* Aggregateها نباید بدون دلیل به یکدیگر Coupled شوند.
* ارتباط بین Aggregateها ترجیحاً از طریق Identity یا Contract مناسب انجام می‌شود.
* Aggregateها نباید Graph کامل Aggregateهای دیگر را در خود نگهداری کنند.
* Application Layer مسئول orchestration بین Aggregateها است.
* Application Layer نباید مالک Business Ruleهای Aggregate باشد.
* Entity نباید صرفاً Data Holder باشد.
* Setter عمومی برای Business State تا حد امکان استفاده نمی‌شود.
* Entity Identity باید مستقل از State باشد.
* Aggregate باید از لحظه Creation در State معتبر قرار گیرد.
* Domain Entity و Aggregate نباید به JPA/Hibernate وابسته باشند.
* Aggregate باید Invariantهای خود را enforce کند.
* `AccountHead` یک Supporting Aggregate است.
* `Account` یک Supporting Aggregate است.
* `JournalEntry` یک Core Aggregate است.
* `JournalEntry` باید مرکز اصلی Business Logic مربوط به ثبت سند باقی بماند.
* وجود سه Aggregate به معنی وجود سه Bounded Context نیست.
* وجود سه Module به معنی وجود سه Microservice نیست.

---

# پیامدها (Consequences)

## مزایا

* حفظ Business Invariantها
* ایجاد Consistency Boundary مشخص
* کاهش Coupling
* افزایش Cohesion
* کنترل بهتر Concurrency
* افزایش Testability
* جلوگیری از Anemic Domain Model
* استقلال Domain از ORM
* استقلال Business Model از Database
* امکان تکامل مستقل Aggregateها
* کاهش احتمال ایجاد Aggregateهای بزرگ
* تمرکز معماری روی Core Aggregate یعنی `JournalEntry`
* هماهنگی ساختار کد با Domain Model
* امکان توسعه Moduleهای داخلی بدون تبدیل آن‌ها به Bounded Context مستقل

## معایب

* طراحی Aggregate نیازمند شناخت دقیق Business Domain است.
* Aggregate Boundary ممکن است در طول تکامل Domain تغییر کند.
* ممکن است Consistency بین Aggregateها به Eventual Consistency نیاز داشته باشد.
* Mapping بین Domain و Persistence Model می‌تواند پیچیده‌تر شود.
* Repositoryهای Aggregate-oriented ممکن است تعداد بیشتری داشته باشند.
* طراحی اشتباه Aggregate Boundary می‌تواند Performance و Concurrency را تحت تأثیر قرار دهد.
* نیاز به Architecture Test و Code Review مستمر وجود دارد.

---

# وضعیت اجرا (Implementation Status)

این ADR به‌عنوان استراتژی رسمی Entity و Aggregate در **General Ledger Bounded Context** اعمال می‌شود.

Aggregateهای فعلی General Ledger:

```text
General Ledger
│
├── AccountHead
│   └── Supporting Aggregate
│
├── Account
│   └── Supporting Aggregate
│
└── JournalEntry
    └── Core Aggregate
```

ساختار Domain:

```text
domain/

├── common/
├── shared/
├── account-head/
├── account/
└── journal-entry/
```

در تصمیم‌های بعدی درباره Aggregate Boundary، ابتدا Business Invariant و Consistency Requirement بررسی می‌شوند و سپس تصمیم درباره Entity، Aggregate Root و ارتباط بین Aggregateها اتخاذ خواهد شد.

در صورت نیاز به تغییر Aggregate Boundary، تغییر باید به‌عنوان یک تصمیم معماری جدید ثبت شود.

جزئیات مربوط به مفاهیم مرتبط در ADRهای زیر تعریف می‌شوند:

* **ADR-0013 — Value Object Strategy**
* **ADR-0014 — Domain Service Strategy**
* **ADR-0015 — Domain Event Strategy**
* **ADR-0016 — Repository Abstraction Strategy**
* **ADR-0018 — Domain Identity Strategy**

---

# References

1. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**
2. Vaughn Vernon — **Implementing Domain-Driven Design**
3. Vaughn Vernon — **Domain-Driven Design Distilled**
4. Martin Fowler — **DDD Aggregate**
5. Martin Fowler — **Patterns of Enterprise Application Architecture**
6. Robert C. Martin — **Clean Architecture: A Craftsman's Guide to Software Structure and Design**
7. Chris Richardson — **Microservices Patterns**
8. Greg Young — **CQRS and Event Sourcing**
