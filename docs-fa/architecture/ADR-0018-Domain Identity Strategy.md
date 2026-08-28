# ADR-0018 — استراتژی Identity در Domain (Domain Identity Strategy)

- **وضعیت:** پذیرفته شده (Accepted)
- **تاریخ:** 2026-08-22

## ADRهای مرتبط

- ADR-0011  
- ADR-0012  
- ADR-0013  
- ADR-0016  

---

# 1. زمینه (Context)

در Domain-Driven Design، Entity موجودیتی است که دارای **Identity پایدار** است.

تغییر وضعیت یا سایر ویژگی‌های Entity نباید باعث شود Domain آن را یک موجودیت جدید در نظر بگیرد.

برای مثال:

```text
Account

AccountId = A-100
Balance   = 1000
Status    = ACTIVE

        ↓

Account

AccountId = A-100
Balance   = 1500
Status    = ACTIVE
````

با وجود تغییر `Balance`، همچنان همان Account وجود دارد؛ زیرا Identity آن تغییر نکرده است.

در Domain باید بین مفاهیم زیر تفکیک روشنی وجود داشته باشد:

```text
Domain Identity
Business Identifier
Persistence Identifier
Entity Identity
Aggregate Identity
Value Object
```

Identity باید بخشی از مدل Domain باشد و نباید به Database، ORM یا مکانیزم Persistence وابسته شود.

---

# 2. مسئله (Problem Statement)

سیستم نیازمند یک Strategy مشخص برای Identity موجودیت‌های Domain است که:

1. مستقل از Persistence باشد.
2. مستقل از JPA/Hibernate باشد.
3. Identity را از Business Identifier تفکیک کند.
4. Aggregate Root را به‌صورت مشخص شناسایی کند.
5. امکان Reference کردن Aggregateهای دیگر را بدون Object Reference مستقیم فراهم کند.
6. Type Safety مناسبی در Domain ایجاد کند.
7. Identity را Immutable نگه دارد.
8. در Unit Testها بدون Database قابل استفاده باشد.
9. در آینده مانع تغییر تکنولوژی Persistence نشود.
10. برای معماری Modular Monolith و در صورت نیاز آینده، Distributed مناسب باشد.

---

# 3. تصمیم (Decision)

سیستم از **Domain-Owned, Immutable, Typed Identity** استفاده می‌کند.

هر Identity مهم Domain به‌صورت یک Type مستقل و Immutable تعریف می‌شود.

مثال:

```java
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(
                value,
                "AccountId cannot be null"
        );
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
```

Identityهای Typed مستقیماً Value Object هستند.

ساختار کلی:

```text
Value Object
    │
    ├── AccountId
    ├── AccountHeadingId
    └── JournalEntryId
```

## نکته مهم

در این معماری **Abstraction مشترکی با نام `DomainId` وجود ندارد**.

یعنی این ساختار انتخاب نشده است:

```text
ValueObject
    │
    └── DomainId
          │
          ├── AccountId
          ├── AccountHeadingId
          └── JournalEntryId
```

بلکه:

```text
ValueObject
    │
    ├── AccountId
    ├── AccountHeadingId
    └── JournalEntryId
```

استفاده می‌شود.

این تصمیم به معنی حذف مفهوم Domain Identity نیست؛ بلکه به این معنی است که برای Identity یک abstraction مشترک و بدون رفتار یا Contract واقعی ایجاد نمی‌کنیم.

---

# 4. Typed Domain Identity

Identityهای مهم Domain باید دارای Type مشخص باشند.

مثلاً:

```java
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value);
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
```

و:

```java
public record JournalEntryId(UUID value) {

    public JournalEntryId {
        Objects.requireNonNull(value);
    }

    public static JournalEntryId generate() {
        return new JournalEntryId(UUID.randomUUID());
    }
}
```

در نتیجه:

```java
AccountId accountId;
JournalEntryId journalEntryId;
```

دو مفهوم متفاوت Domain هستند.

این Type Safety از اشتباهاتی مانند موارد زیر جلوگیری می‌کند:

```java
journalEntryRepository.findById(accountId);
```

زیرا `AccountId` و `JournalEntryId` دو Type متفاوت هستند.

---

# 5. Identity به‌عنوان Value Object

مطابق ADR-0013، Identityهای Typed خودشان Value Object هستند.

بنابراین:

```text
AccountId
    ↓
Value Object
```

و نه:

```text
AccountId
    ↓
DomainId
    ↓
Value Object
```

Identity دارای Value است و Equality آن بر اساس Value انجام می‌شود.

برای مثال:

```text
AccountId(UUID-A)
=
AccountId(UUID-A)
```

اما:

```text
AccountId(UUID-A)
≠
AccountId(UUID-B)
```

Identity به دلیل Value بودن، Immutable است.

---

# 6. Identity Ownership

Identity متعلق به Entity است و Entity مالک Identity خود است.

مثال:

```java
public class Account {

    private final AccountId id;

    public Account(AccountId id) {
        this.id = Objects.requireNonNull(id);
    }

    public AccountId id() {
        return id;
    }
}
```

Identity پس از ایجاد Entity تغییر نمی‌کند.

الگوی ترجیحی:

```java
private final AccountId id;
```

است.

---

# 7. Identity Immutability

Identity یک Entity پس از Creation قابل تغییر نیست.

مجاز:

```text
AccountId = A-100
Balance   = 1000

        ↓

AccountId = A-100
Balance   = 1500
```

غیرمجاز:

```text
AccountId = A-100

        ↓

AccountId = A-200
```

تغییر Identity به معنی تغییر همان Entity نیست.

Identity باید در طول Lifecycle Entity ثابت باقی بماند.

---

# 8. Aggregate Root Identity

هر Aggregate Root باید Identity مستقل داشته باشد.

در General Ledger:

```text
AccountHeading
    └── AccountHeadingId

Account
    └── AccountId

JournalEntry
    └── JournalEntryId
```

بنابراین:

```text
AccountHeadingId
AccountId
JournalEntryId
```

Identityهای Aggregate Rootهای مربوط به خود هستند.

هر Aggregate Root از طریق Identity خود قابل شناسایی است.

---

# 9. Entity Identity داخل Aggregate

Entityهای داخلی Aggregate در صورت نیاز می‌توانند Identity داشته باشند.

اما داشتن Identity برای Entity داخلی به معنی داشتن **Global Identity** نیست.

مثلاً:

```text
JournalEntry
    │
    ├── JournalEntryId
    │
    ├── JournalEntryLine
    │      └── JournalEntryLineId
    │
    └── JournalEntryLine
           └── JournalEntryLineId
```

در این مدل:

```text
JournalEntryId
    → Aggregate Root Identity
```

و:

```text
JournalEntryLineId
    → Identity محلی Entity داخلی Aggregate
```

است.

اگر Entity داخلی بدون Identity مستقل قابل مدل‌سازی باشد، نباید صرفاً برای یکسان‌سازی ساختار به آن Identity اضافه شود.

---

# 10. Aggregate Reference

Aggregateها نباید با نگهداری Object Reference مستقیم به Aggregate مستقل دیگری متصل شوند.

نامناسب:

```java
public class Account {

    private AccountHeading accountHeading;
}
```

در صورتی که `AccountHeading` یک Aggregate مستقل باشد، ترجیحاً:

```java
public class Account {

    private AccountHeadingId accountHeadingId;
}
```

استفاده می‌شود.

در نتیجه:

```text
Account
    │
    └── AccountHeadingId
              │
              ▼
       AccountHeading
```

به‌جای:

```text
Account
    │
    └── AccountHeading Object
```

استفاده می‌شود.

این تصمیم باعث حفظ Aggregate Boundary و کاهش Coupling بین Aggregateها می‌شود.

---

# 11. Domain Identity در مقابل Business Identifier

Domain Identity و Business Identifier دو مفهوم متفاوت هستند.

برای مثال در یک Account ممکن است داشته باشیم:

```text
AccountId
AccountNumber
IBAN
```

که می‌توانند به شکل زیر مدل شوند:

```text
AccountId
    → Domain Identity

AccountNumber
    → Business Identifier / Value Object

IBAN
    → Business Identifier / Value Object
```

Unique بودن یک مقدار Business به این معنی نیست که آن مقدار الزاماً Domain Identity است.

برای مثال:

```text
AccountNumber = 010123456
```

ممکن است در Business یک مقدار Unique باشد، اما Domain باید مشخص کند که آیا این مقدار Identity Entity است یا صرفاً یک Business Identifier.

به‌صورت پیش‌فرض Business Identifier را با Domain Identity یکی نمی‌کنیم.

---

# 12. Domain Identity در مقابل Persistence Identifier

Database ممکن است Primary Key مخصوص Persistence داشته باشد.

برای مثال:

```text
Domain

AccountId = UUID

        ↓

Persistence Mapping

        ↓

Database

ID = BIGINT
```

این دو مفهوم الزاماً یکسان نیستند.

Domain نباید به مکانیزم‌هایی مانند:

```java
@Id
@GeneratedValue
```

وابسته باشد.

این Annotationها و مکانیزم‌های تولید ID متعلق به Persistence Layer هستند.

---

# 13. Persistence Mapping

Persistence Layer مسئول Mapping بین Domain Identity و Persistence Identifier است.

```text
Domain Model

AccountId
    │
    ▼
Persistence Mapper
    │
    ▼
Database Identifier
    │
    ▼
Database
```

Domain نباید بداند:

* Primary Key چگونه تولید می‌شود.
* Sequence چیست.
* Identity Column چیست.
* Hibernate چگونه Entity را مدیریت می‌کند.
* JPA چگونه Identity را Persist می‌کند.

---

# 14. Identity Generation

برای Domain Identity، UUID به‌عنوان **Strategy پیش‌فرض** انتخاب شده است.

مثال:

```java
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(value);
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
```

Identity می‌تواند قبل از Persistence تولید شود:

```text
Create Aggregate
      │
      ▼
Generate Identity
      │
      ▼
Create Domain Aggregate
      │
      ▼
Persist
```

در نتیجه ایجاد Identity به Database وابسته نیست.

---

# 15. دلیل انتخاب UUID

UUID به‌عنوان Strategy پیش‌فرض انتخاب شده است زیرا:

* تولید Identity بدون Database امکان‌پذیر است.
* برای محیط‌های Distributed مناسب است.
* نیازمند هماهنگی با Database مرکزی برای تولید ID نیست.
* Identity می‌تواند قبل از Persistence ایجاد شود.
* Domain به Sequence یا Auto Increment وابسته نمی‌شود.
* احتمال Collision در استفاده صحیح بسیار پایین است.

این تصمیم به معنی ممنوع بودن سایر Strategyها نیست.

در صورت وجود نیاز Domain-specific می‌توان Strategy دیگری را با تصمیم معماری مستقل معرفی کرد.

---

# 16. Value Object Identity ندارد

Value Object دارای Identity مستقل نیست.

مثال:

```java
public record Money(
        BigDecimal amount,
        Currency currency
) {
}
```

دو مقدار:

```text
Money(100, IRR)
Money(100, IRR)
```

از دید Domain بر اساس Value برابر هستند.

بنابراین نباید برای Value Object یک ID مصنوعی ایجاد کنیم:

```java
public class Money {

    private UUID id; // WRONG
}
```

قاعده:

```text
Entity
    → Identity-based

Value Object
    → Value-based
```

---

# 17. Entity Equality

Entityها در Domain بر اساس Identity خود شناخته می‌شوند.

برای مثال:

```text
Account
    id      = A-100
    balance = 1000
```

و:

```text
Account
    id      = A-100
    balance = 2000
```

از دید Domain دارای Identity یکسان هستند.

بنابراین تغییر State باعث تغییر Identity نمی‌شود.

در مقابل:

```text
Account
    id = A-100
```

و:

```text
Account
    id = A-200
```

دو Entity متفاوت هستند.

پیاده‌سازی دقیق `equals` و `hashCode` مربوط به Entity باید مطابق تصمیم ADR-0012 انجام شود.

---

# 18. Identity و Distributed Systems

Domain Identity نباید به یک Database یا Node خاص وابسته باشد.

در محیط Distributed:

```text
Node A → AccountId A
Node B → AccountId B
Node C → AccountId C
```

هر Node می‌تواند Identity تولید کند.

این ویژگی باعث می‌شود Identity Strategy با تغییرات احتمالی معماری سازگار باشد:

```text
Monolith
   ↓
Modular Monolith
   ↓
Distributed System
   ↓
Microservices
```

---

# 19. External System Identity

Identity سیستم‌های خارجی نباید مستقیماً جایگزین Domain Identity شود.

مثلاً:

```text
Payment
    ├── PaymentId
    └── ExternalTransactionId
```

در این مدل:

```text
PaymentId
    → Domain Identity

ExternalTransactionId
    → External System Identifier
```

سیستم Domain مالک `PaymentId` است، در حالی که `ExternalTransactionId` متعلق به سیستم خارجی است.

---

# 20. مثال در General Ledger

نمونه ساده یک Aggregate:

```java
public class Account {

    private final AccountId id;
    private final AccountHeadingId accountHeadingId;
    private Money balance;

    public Account(
            AccountId id,
            AccountHeadingId accountHeadingId,
            Money balance
    ) {
        this.id = Objects.requireNonNull(id);
        this.accountHeadingId =
                Objects.requireNonNull(accountHeadingId);
        this.balance =
                Objects.requireNonNull(balance);
    }

    public AccountId id() {
        return id;
    }

    public AccountHeadingId accountHeadingId() {
        return accountHeadingId;
    }

    public Money balance() {
        return balance;
    }
}
```

در این مدل:

```text
Account
    │
    ├── AccountId
    │      └── Aggregate Identity
    │
    ├── AccountHeadingId
    │      └── Reference to another Aggregate
    │
    └── Money
           └── Value Object
```

این مدل مرز Aggregateها را به‌صورت واضح حفظ می‌کند.

---

# 21. Architectural Rules

قوانین زیر برای Domain Layer الزامی هستند:

### Rule 1

هر Entity باید Identity مشخص داشته باشد.

### Rule 2

Identity بعد از Creation تغییر نمی‌کند.

### Rule 3

هر Aggregate Root دارای Identity مستقل است.

### Rule 4

Entityهای داخلی Aggregate فقط در صورت نیاز واقعی Identity خواهند داشت.

### Rule 5

Reference بین Aggregateها باید از طریق Typed Identity انجام شود.

### Rule 6

Value Object دارای Domain Identity مستقل نیست.

### Rule 7

Identityهای مهم Domain باید Typed باشند.

### Rule 8

Domain Identity نباید به JPA/Hibernate وابسته باشد.

### Rule 9

Database Primary Key الزاماً Domain Identity نیست.

### Rule 10

Business Identifier الزاماً Domain Identity نیست.

### Rule 11

Identity Generation نباید به Database وابسته باشد.

### Rule 12

Entity Equality باید بر اساس Identity و Value Object Equality بر اساس Value باشد.

### Rule 13

Identityهای Domain مستقیماً Value Object هستند.

### Rule 14

در مدل فعلی هیچ `DomainId` abstraction مشترکی ایجاد نمی‌شود.

### Rule 15

Aggregate مستقل نباید با Object Reference مستقیم در Aggregate دیگر نگهداری شود.

---

# 22. تصمیم درباره DomainId مشترک

استفاده از abstraction مشترک:

```java
public interface DomainId {

}
```

یا:

```java
public interface DomainId extends ValueObject {

}
```

در مدل فعلی **Rejected** است.

دلیل:

Identityهای مختلف فعلاً رفتار یا Contract مشترک معناداری ندارند که نیازمند abstraction مشترک باشد.

بنابراین:

```text
ValueObject
    │
    ├── AccountId
    ├── AccountHeadingId
    └── JournalEntryId
```

به ساختار زیر ترجیح داده می‌شود:

```text
ValueObject
    │
    └── DomainId
          │
          ├── AccountId
          ├── AccountHeadingId
          └── JournalEntryId
```

اگر در آینده رفتار یا Contract مشترک واقعی برای Identityها ایجاد شود، اضافه کردن abstraction مشترک می‌تواند در یک ADR مستقل بررسی شود.

---

# 23. Alternatives Considered

## Alternative 1 — Primitive ID

```java
private Long id;
```

### Rejected

زیرا:

* Type Safety پایین است.
* معنای Domain Identity مشخص نیست.
* `AccountId`، `AccountHeadingId` و `JournalEntryId` همگی یک Type خواهند بود.
* احتمال اشتباه در APIها و Repositoryها افزایش می‌یابد.

---

## Alternative 2 — Database Generated ID

```java
@Id
@GeneratedValue
private Long id;
```

### Rejected as Domain Strategy

زیرا Identity به Database وابسته می‌شود و Domain نمی‌تواند مستقل از Persistence Identity ایجاد کند.

استفاده از Database-generated ID در Persistence Layer ممنوع نیست؛ اما نباید Strategy مربوط به Domain Identity باشد.

---

## Alternative 3 — Business Identifier به‌عنوان Identity

مثلاً:

```text
AccountNumber = Entity Identity
```

### Rejected as General Strategy

زیرا Business Identifier ممکن است:

* تغییر کند.
* توسط سیستم دیگری تولید شود.
* فقط در یک Bounded Context معنا داشته باشد.
* صرفاً یک Business Attribute باشد.

تنها زمانی می‌توان Business Identifier را Identity دانست که Domain صراحتاً آن را Identity تعریف کرده باشد.

---

## Alternative 4 — Identity برای همه Objectها

### Rejected

Value Objectها Identity مستقل ندارند.

افزودن ID به تمام Objectها باعث تبدیل مصنوعی Value Object به Entity می‌شود.

---

## Alternative 5 — Object Reference Between Aggregates

```java
private AccountHeading accountHeading;
```

### Rejected

زیرا:

* Aggregate Boundary ضعیف می‌شود.
* Object Graph بزرگ ایجاد می‌شود.
* Coupling افزایش پیدا می‌کند.
* ORM و Lazy Loading ممکن است وارد Domain شوند.
* امکان ایجاد ناخواسته Transaction بین Aggregateها افزایش می‌یابد.

---

## Alternative 6 — Shared DomainId Abstraction

```java
public interface DomainId extends ValueObject {
}
```

### Rejected

این abstraction در مدل فعلی رفتار یا Contract مشترک معناداری برای تمام Identityها ایجاد نمی‌کند.

بنابراین ایجاد آن صرفاً برای جلوگیری از تکرار ساختاری، مصداق ایجاد Abstraction بدون نیاز واقعی خواهد بود.

---

# 24. پیامدها (Consequences)

## مزایا

### Domain Independence

Domain به Database و ORM وابسته نخواهد بود.

### Type Safety

استفاده از:

```text
AccountId
AccountHeadingId
JournalEntryId
```

به‌جای:

```text
Long
UUID
String
```

خطاهای معنایی را کاهش می‌دهد.

### Aggregate Boundary

ارتباط Aggregateها با Identity انجام می‌شود و Boundary آنها حفظ می‌شود.

### Distributed Ready

Identity می‌تواند بدون وابستگی به Database تولید شود.

### Testability

Entityها و Aggregateها بدون Spring و Database قابل ایجاد و تست هستند.

### Simple Identity Model

Identityهای Domain بدون ایجاد `DomainId` مشترک، مستقیماً به‌صورت Typed Value Object تعریف می‌شوند.

---

## معایب

### More Types

برای Identityهای مهم Domain Typeهای بیشتری خواهیم داشت:

```text
AccountId
AccountHeadingId
JournalEntryId
...
```

### Mapping Complexity

Persistence Layer باید Mapping بین Domain Identity و Database Identifier را انجام دهد.

### UUID Storage

استفاده از UUID ممکن است نسبت به `BIGINT` فضای بیشتری مصرف کند و در برخی Databaseها ملاحظات مربوط به Indexing و Storage داشته باشد.

### Repeated Identity Structure

با حذف `DomainId` ممکن است برخی Identityها ساختارهای مشابهی مانند Null Check و `generate()` داشته باشند.

این تکرار آگاهانه پذیرفته می‌شود تا زمانی که Contract یا رفتار مشترک واقعی شکل بگیرد.

---

# 25. Implementation Guidelines

Identityهای Domain باید به شکل Immutable Type تعریف شوند.

نمونه:

```java
public record AccountId(UUID value) {

    public AccountId {
        Objects.requireNonNull(
                value,
                "AccountId cannot be null"
        );
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
```

Aggregate:

```java
public class Account {

    private final AccountId id;

    public Account(AccountId id) {
        this.id = Objects.requireNonNull(id);
    }

    public AccountId id() {
        return id;
    }
}
```

Persistence:

```text
Domain
    │
    ▼
AccountId
    │
    ▼
Persistence Mapper
    │
    ▼
AccountJpaEntity
    │
    ▼
Database
```

Domain نباید به شکل زیر وابسته شود:

```text
Domain
    ↓
JPA Entity
    ↓
Database
```

---

# 26. Testing Rules

Unit Testهای Domain باید بتوانند بدون Database Identity ایجاد کنند.

مثال:

```java
@Test
void should_create_account_with_identity() {

    AccountId accountId = AccountId.generate();

    Account account =
            new Account(accountId);

    assertThat(account.id())
            .isEqualTo(accountId);
}
```

همچنین Equality خود Identity باید بر اساس Value عمل کند:

```java
@Test
void account_ids_with_same_value_should_be_equal() {

    UUID value = UUID.randomUUID();

    AccountId first =
            new AccountId(value);

    AccountId second =
            new AccountId(value);

    assertThat(first)
            .isEqualTo(second);
}
```

تست Equality خود Entity باید مطابق تصمیم:

```text
ADR-0012 — Entity & Aggregate Strategy
```

انجام شود.

---

# 27. Architectural Validation

در صورت استفاده از ArchUnit، می‌توان وابستگی Domain به Persistence را ممنوع کرد.

مثال:

```java
noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
                "..infrastructure..",
                "..persistence.."
        );
```

هدف این Rule تضمین این است که Identity Strategy واقعاً متعلق به Domain باقی بماند.

---

# 28. Decision Summary

تصمیم نهایی:

```text
                    Domain
                       │
                       ▼
              Domain-Owned Identity
                       │
                       ▼
              Typed Value Object
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   AccountId   AccountHeadingId   JournalEntryId
        │              │              │
        └──────────────┼──────────────┘
                       │
                       ▼
                Entity / Aggregate
```

بنابراین:

```text
Domain Identity
      │
      ▼
Typed Identity
      │
      ▼
Value Object
```

و **نه**:

```text
Domain Identity
      │
      ▼
DomainId
      │
      ├── AccountId
      ├── AccountHeadingId
      └── JournalEntryId
```

برای ارتباط Aggregateها:

```text
Aggregate A
    │
    └── AggregateBId
            │
            ▼
       Aggregate B
```

و نه:

```text
Aggregate A
    │
    └── Aggregate B Object
```

---

# 29. وضعیت اجرا (Implementation Status)

Identity Strategy برای تمام Moduleهای Domain اعمال می‌شود.

در طراحی هر Entity یا Aggregate باید مشخص شود:

1. Identity آن چیست؟
2. آیا Identity دارای Business Meaning مستقل است یا صرفاً Domain Identity است؟
3. آیا Identity باید Typed باشد؟
4. آیا Entity یک Aggregate Root است؟
5. آیا Reference به Aggregate دیگری وجود دارد؟
6. آیا Reference باید با Typed Identity مدل شود؟
7. آیا Identity باید قبل از Persistence تولید شود؟

در وضعیت فعلی:

```text
Domain Identity
    → Domain-Owned
    → Immutable
    → Typed
    → Value Object

Shared DomainId
    → Not Used

Default Identity Generation
    → UUID

Persistence Identity
    → Infrastructure Concern

Business Identifier
    → Separate from Domain Identity
```

---

# 30. Final Decision

سیستم از **Domain-Owned, Immutable, Typed Identity** استفاده می‌کند.

هر Identity مهم Domain به‌صورت یک Type مستقل و Immutable تعریف می‌شود و مستقیماً مطابق ADR-0013 به‌عنوان Value Object مدل می‌شود.

**هیچ abstraction مشترکی با نام `DomainId` در مدل فعلی ایجاد نمی‌شود.**

Domain Identity از Persistence Identity و Business Identifier مستقل خواهد بود.

Aggregate Rootها دارای Identity مستقل هستند.

Aggregateهای مستقل از طریق Typed Identity یکدیگر را Reference می‌کنند و نه Object Reference مستقیم.

Value Objectها Identity مستقل ندارند.

Entityهای داخلی Aggregate فقط در صورت نیاز واقعی دارای Identity خواهند بود و Identity آنها لزوماً Global نیست.

UUID به‌عنوان Strategy پیش‌فرض برای تولید Domain Identity انتخاب شده است، اما این تصمیم مانع استفاده از Strategyهای دیگر در موارد خاص Domain نمی‌شود.

---

# 31. References

1. **Eric Evans**, *Domain-Driven Design: Tackling Complexity in the Heart of Software*, Addison-Wesley, 2003.

   مباحث مرتبط با Entity، Identity، Value Object و Aggregate.

2. **Vaughn Vernon**, *Implementing Domain-Driven Design*, Addison-Wesley, 2013.

   مباحث مرتبط با Entity، Aggregate و Identity.

3. **Vaughn Vernon**, *Domain-Driven Design Distilled*, Addison-Wesley, 2016.

   مباحث Entity، Value Object و Aggregate.

4. **Martin Fowler**, *Patterns of Enterprise Application Architecture*, Addison-Wesley, 2002.

   مباحث مرتبط با Identity Field و Persistence Identity.

5. **Martin Fowler**, *Domain-Driven Design*.

   [https://martinfowler.com/bliki/DomainDrivenDesign.html](https://martinfowler.com/bliki/DomainDrivenDesign.html)

6. **Martin Fowler**, *Value Object*.

   [https://martinfowler.com/bliki/ValueObject.html](https://martinfowler.com/bliki/ValueObject.html)

7. **Martin Fowler**, *Identity Map*.

   [https://martinfowler.com/eaaCatalog/identityMap.html](https://martinfowler.com/eaaCatalog/identityMap.html)

8. **Mark Richards & Neal Ford**, *Fundamentals of Software Architecture: An Engineering Approach*, O'Reilly, 2020.

   مباحث مرتبط با Architectural Decisions، Trade-offs و مستندسازی تصمیمات معماری.

```

**یک نکته مهم:** در نسخه قبلی مثال `Account` شامل `LedgerId` بود، ولی با Baseline فعلی General Ledger ما بهتر است از `AccountHeadingId` استفاده کنیم؛ چون Aggregateهای مصوب فعلی شما `AccountHeading`، `Account` و `JournalEntry` هستند. این اصلاح را در نسخه بالا اعمال کردم تا ADR-0018 با مدل واقعی پروژه تناقض نداشته باشد.
```
