# ADR-0013 — استراتژی Value Object (Value Object Strategy)

* **وضعیت:** پذیرفته شده (Accepted)

* **تاریخ:** 2026-08-22

* **ADRهای مرتبط:**

  * ADR-0011 
  * ADR-0012 
  * ADR-0018 
  * ADR-0010 

---

# زمینه (Context)

در **General Ledger Bounded Context** بسیاری از مفاهیم Domain دارای Identity مستقل نیستند و با مقدار و ویژگی‌های خود شناخته می‌شوند.

نمونه‌هایی از این مفاهیم می‌توانند شامل موارد زیر باشند:

* Money
* Currency
* AccountNumber
* Percentage
* DateRange
* FiscalPeriod

در صورت مدل‌سازی این مفاهیم صرفاً با Primitive Typeها، Business Ruleهای مرتبط با آن‌ها در بخش‌های مختلف Domain پراکنده شده و امکان استفاده نادرست از مقادیر افزایش پیدا می‌کند.

به عنوان مثال، استفاده مستقیم از:

```java
BigDecimal amount;
String currency;
String accountNumber;
```

اطلاعات مهم Domain را در Type System منعکس نمی‌کند.

در مقابل:

```java
Money amount;
Currency currency;
AccountNumber accountNumber;
```

باعث می‌شود مفهوم Domain و قواعد مرتبط با آن در خود مدل قابل مشاهده باشند.

همچنین در معماری General Ledger، Identity مربوط به Entityها و Aggregate Rootها نیز به صورت Typeهای Domain-specific مدل می‌شود.

برای مثال:

```java
public record AccountId(UUID value) {
}
```

در این مدل، `AccountId` خود یک Value Object است، اما `Account` یک Entity و Aggregate Root محسوب می‌شود.

هدف این ADR تعیین استراتژی استفاده، طراحی، اعتبارسنجی، رفتار، Equality و Persistence Mapping مربوط به Value Objectها در General Ledger Bounded Context است.

---

# تصمیم (Decision)

## 1. Value Object برای مفاهیم بدون Identity

هر مفهوم Domain که:

* Identity مستقل ندارد.
* با Value خود تعریف می‌شود.
* دارای Business Meaning است.
* و یا دارای Business Rule مرتبط با مقدار خود است.

کاندید مناسبی برای مدل‌سازی به عنوان **Value Object** است.

به عنوان مثال:

نامناسب:

```java
String accountNumber;
BigDecimal amount;
String currency;
```

ترجیحاً:

```java
AccountNumber accountNumber;
Money amount;
Currency currency;
```

Value Object باید نماینده یک مفهوم مشخص در **Ubiquitous Language** باشد.

---

# 2. Identity Value Object

Identity مربوط به Entity و Aggregate Root باید به صورت Type مشخص و Domain-specific مدل شود.

به عنوان مثال:

```java
public record AccountId(UUID value) {
}
```

در این مدل:

```text
Account
│
├── Aggregate Root / Entity
│
└── AccountId
       │
       └── Value Object
```

همین الگو برای Aggregate Rootهای اصلی General Ledger اعمال می‌شود:

```text
Account
└── AccountId

AccountHead
└── AccountHeadId

JournalEntry
└── JournalEntryId
```

بنابراین:

```text
Entity / Aggregate Root
        │
        ▼
Identity Value Object
```

`AccountId`، `AccountHeadId` و `JournalEntryId` خود Entity یا Aggregate نیستند.

آن‌ها Value Objectهایی هستند که Identity مربوط به Entity یا Aggregate Root را به صورت Type-safe و Domain-specific مدل می‌کنند.

این Value Objectها:

* Identity مستقل ندارند.
* Lifecycle مستقل ندارند.
* Repository مستقل ندارند.
* بخشی از مدل Entity/Aggregate هستند.

جزئیات naming، ساختار و نحوه تولید Identity در **ADR-0018 — Domain Identity Strategy** تعریف می‌شود.

---

# 3. Value Object باید Immutable باشد

Value Objectها باید **Immutable** باشند.

پس پس از ایجاد Value Object، State داخلی آن نباید مستقیماً تغییر کند.

نامناسب:

```java
money.setAmount(5000);
```

ترجیحاً:

```java
Money increased =
    money.add(Money.of(5000, Currency.IRR));
```

در این مدل، عملیات Domain یک Value Object جدید ایجاد می‌کند و Object قبلی تغییر نمی‌کند.

Immutable بودن باعث می‌شود Value Objectها:

* قابل پیش‌بینی‌تر باشند.
* ساده‌تر تست شوند.
* از تغییرات ناخواسته State جلوگیری کنند.
* در استفاده‌های همزمان رفتار امن‌تری داشته باشند.

---

# 4. Equality بر اساس Value

Value Object دارای Identity مستقل نیست.

دو Value Object زمانی برابر هستند که تمام بخش‌های معنی‌دار آن‌ها برابر باشند.

مثال:

```text
Money(100, IRR)
=
Money(100, IRR)
```

اما:

```text
Money(100, IRR)
≠
Money(100, USD)
```

بنابراین Equality در Value Object باید بر اساس Value تعریف شود، نه Object Reference.

برای Value Objectهای ساده، استفاده از Java `record` انتخاب مناسبی است.

مثال:

```java
public record Currency(String code) {
}
```

استفاده از `record` یک الزام برای تمام Value Objectها نیست و Value Objectهای پیچیده‌تر می‌توانند به صورت Class معمولی پیاده‌سازی شوند.

---

# 5. Value Object مسئول اعتبار خودش است

Value Object نباید اجازه ایجاد State نامعتبر را بدهد.

برای مثال:

```java
public record Percentage(BigDecimal value) {

    public Percentage {
        if (value == null) {
            throw new InvalidPercentageException();
        }

        if (value.compareTo(BigDecimal.ZERO) < 0 ||
            value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidPercentageException();
        }
    }
}
```

بنابراین:

```text
Percentage(50)
```

معتبر است، اما:

```text
Percentage(150)
```

نباید قابل ایجاد باشد.

هدف این است که سایر بخش‌های Domain بتوانند به Invariantهای داخلی Value Object اعتماد کنند.

---

# 6. جلوگیری از Primitive Obsession

برای مفاهیم مهم Domain باید از Primitive Obsession جلوگیری شود.

نامناسب:

```java
String accountNumber;
String currency;
BigDecimal amount;
```

ترجیحاً:

```java
AccountNumber accountNumber;
Currency currency;
Money amount;
```

این کار باعث می‌شود:

* Type Safety افزایش پیدا کند.
* Business Rule در محل مناسب قرار گیرد.
* استفاده اشتباه از مقادیر کاهش پیدا کند.
* Ubiquitous Language در کد قابل مشاهده باشد.

---

# 7. Value Object باید Business Meaning داشته باشد

هر Primitive Type الزاماً نباید به Value Object تبدیل شود.

Value Object زمانی ایجاد می‌شود که مفهوم موردنظر دارای **Business Meaning** یا **Business Rule** باشد.

مثلاً:

```java
int retryCount;
boolean active;
```

لزوماً Value Object نیستند.

اما:

```text
AccountId
AccountNumber
Money
Currency
Percentage
FiscalPeriod
```

در صورت داشتن Business Meaning و Rule مناسب، کاندید Value Object هستند.

هدف این ADR ایجاد تعداد زیادی Wrapper نیست؛ بلکه مدل‌سازی صحیح مفاهیم مهم Domain است.

---

# 8. Value Object باید رفتار مرتبط با مفهوم خود را داشته باشد

Value Object نباید صرفاً Wrapper یک Primitive باشد.

اگر یک Value Object دارای Business Behavior است، این Behavior باید تا حد امکان در خود Value Object قرار گیرد.

مثلاً:

```java
public final class Money {

    private final BigDecimal amount;
    private final Currency currency;

    public Money add(Money other) {
        // Business Rule
    }

    public Money subtract(Money other) {
        // Business Rule
    }

    public boolean isGreaterThan(Money other) {
        // Business Rule
    }
}
```

به این ترتیب Business Rule مربوط به مفهوم `Money` در Serviceهای مختلف پراکنده نمی‌شود.

---

# 9. Value Object و Domain Invariant

Value Object باید Invariantهای مربوط به مفهوم خودش را محافظت کند.

برای مثال:

```text
Money

├── amount نباید نامعتبر باشد
├── currency نباید نامعتبر باشد
└── عملیات بین Currencyهای ناسازگار نباید بدون Rule مشخص انجام شود
```

یا:

```text
DateRange

├── start باید معتبر باشد
├── end باید معتبر باشد
└── start نباید بعد از end باشد
```

یا:

```text
AccountNumber

└── format و constraints مربوط به شماره حساب
```

در نتیجه Domain می‌تواند به معتبر بودن Value Object اعتماد کند.

---

# 10. Value Object نباید وابسته به Framework باشد

Value Object باید **Framework Independent** باشد.

نباید به موارد زیر وابسته باشد:

* Spring
* JPA
* Hibernate
* HTTP
* Database
* Messaging Framework

مثلاً Domain نباید به شکل زیر باشد:

```java
@Entity
public class Money {
}
```

مگر اینکه در یک ADR مشخص، استثنای معماری تعریف شده باشد.

Persistence Mapping باید خارج از Domain و در Infrastructure انجام شود.

---

# 11. Value Object و Entity

Entity و Value Object دو مفهوم متفاوت هستند.

| ویژگی           | Entity                | Value Object  |
| --------------- | --------------------- | ------------- |
| Identity        | دارد                  | ندارد         |
| Equality        | بر اساس Identity      | بر اساس Value |
| Lifecycle مستقل | می‌تواند داشته باشد   | ندارد         |
| Mutability      | ممکن است Mutable باشد | Immutable     |
| مثال            | Account               | Money         |
| مثال            | JournalEntry          | AccountNumber |
| مثال            | AccountHead           | Currency      |

قاعده تصمیم‌گیری:

> اگر مفهوم با Identity و Lifecycle آن شناخته می‌شود، Entity کاندید مناسبی است؛ اگر مفهوم با مقدار و ویژگی‌های خود شناخته می‌شود، Value Object کاندید مناسبی است.

تصمیم نهایی باید بر اساس مدل واقعی Domain گرفته شود و صرفاً بر اساس شکل داده انجام نشود.

---

# 12. Value Object در Aggregate

Value Object معمولاً بخشی از Aggregate است.

برای مثال:

```text
Account Aggregate

Account
│
├── AccountId
├── AccountNumber
├── Currency
├── AccountStatus
└── ...
```

یا:

```text
JournalEntry Aggregate

JournalEntry
│
├── JournalEntryId
├── PostingState
├── JournalLines
│   ├── AccountId
│   ├── Money
│   └── ...
└── ...
```

در این مدل Value Object بخشی از State Aggregate است و Lifecycle مستقلی خارج از Aggregate ندارد.

Value Object نباید صرفاً به دلیل استفاده در چند Aggregate به Entity یا Aggregate تبدیل شود.

---

# 13. Value Object و Aggregate Identity

Identity Value Objectها بخشی از State مربوط به Entity یا Aggregate Root هستند.

برای مثال:

```text
Account Aggregate
│
└── AccountId
      └── Value Object
```

بنابراین:

```java
AccountId accountId;
```

به این معنی نیست که `AccountId` یک Aggregate مستقل است.

بلکه:

```text
Account
    │
    └── AccountId
```

رابطه مالکیت Domain را نشان می‌دهد.

همین قاعده برای:

```text
AccountHeadId
JournalEntryId
```

نیز برقرار است.

Identity Value Objectها نباید دارای Repository یا Lifecycle مستقل باشند.

---

# 14. Value Object نباید Aggregate باشد

Value Object فاقد Identity مستقل است و بنابراین نباید به عنوان Aggregate Root مدل شود.

برای مثال:

```text
Money
Currency
AccountNumber
Percentage
AccountId
AccountHeadId
JournalEntryId
```

به خودی خود Aggregate محسوب نمی‌شوند.

اگر یک مفهوم به مرور دارای:

* Identity مستقل
* Lifecycle مستقل
* Invariantهای مستقل
* Repository مستقل

شد، باید بررسی شود که آیا دیگر Value Object نیست و در واقع Entity یا Aggregate است.

---

# 15. Value Object نباید Repository داشته باشد

Value Object دارای Identity مستقل و Lifecycle مستقل نیست.

بنابراین Repository برای آن تعریف نمی‌شود.

نامناسب:

```text
MoneyRepository
CurrencyRepository
AccountNumberRepository
AccountIdRepository
```

اگر نیاز به Repository مستقل ایجاد شد، ابتدا باید بررسی شود که آیا مفهوم موردنظر در واقع Entity یا Aggregate نیست.

---

# 16. Value Object و Persistence

Domain Model نباید به دلیل محدودیت Database، Value Object را به Primitive تبدیل کند.

برای مثال:

```text
Money
```

می‌تواند در Persistence به چند Column نگهداری شود:

```text
amount
currency
```

اما Domain همچنان باید `Money` را به عنوان یک مفهوم واحد بشناسد.

مدل کلی:

```text
Domain
  │
  ▼
Money
  │
  ├── amount
  └── currency
       │
       ▼
Persistence Mapping
       │
       ├── amount_column
       └── currency_column
              │
              ▼
           Database
```

این تصمیم با **ADR-0010 — Database Strategy** و **ADR-0011 — Domain Layer Architecture** هماهنگ است.

---

# 17. Value Object Creation

ساخت Value Object باید باعث ایجاد State معتبر شود.

برای Value Objectهای ساده می‌توان از Constructor یا Java `record` استفاده کرد.

مثال:

```java
Currency currency = Currency.of("IRR");
```

برای Value Objectهای پیچیده‌تر استفاده از Factory Method ترجیح داده می‌شود:

```java
Money money = Money.of(
    BigDecimal.valueOf(1000),
    Currency.IRR
);
```

یا:

```java
AccountNumber accountNumber =
    AccountNumber.of("123456789");
```

Factory Method یا Constructor باید Invariantهای Value Object را در زمان Creation محافظت کند.

---

# 18. Nullability

Value Objectهای ضروری Domain نباید بدون دلیل در State نامعتبر قرار بگیرند.

به جای اینکه Business Logic با مقدار نامشخص کار کند:

```java
Money balance = null;
```

Aggregate باید تا حد امکان State معتبر داشته باشد.

با این حال، `null` به صورت مطلق ممنوع نیست.

اگر یک مفهوم از نظر Business واقعاً Optional باشد، Optional بودن باید بخشی از مدل Domain باشد.

مثلاً:

```text
Account

├── mandatory AccountNumber
├── mandatory Currency
└── optional Description
```

در این حالت Optional بودن `Description` بخشی از مدل است.

---

# 19. Value Object Naming

نام Value Object باید از **Ubiquitous Language** گرفته شود و مفهوم واقعی Domain را بیان کند.

نامناسب:

```text
StringValue
NumberWrapper
GenericAmount
DataObject
CommonValue
```

مناسب:

```text
Money
Currency
AccountNumber
Percentage
DateRange
FiscalPeriod
```

نام Value Object نباید صرفاً نوع داده داخلی آن را توصیف کند.

---

# 20. Shared Value Objects

وجود یک Value Object در چند Aggregate به تنهایی دلیل ایجاد Shared Kernel نیست.

برای مثال ممکن است `Currency` یا `Money` در چند Aggregate استفاده شوند:

```text
Account
   │
   └── Currency

JournalEntry
   │
   └── Money
```

اما محل قرارگیری این مفاهیم باید بر اساس مالکیت واقعی Domain و تصمیم Shared Kernel مشخص شود.

نباید صرفاً برای جلوگیری از Duplicate Code، تمام Value Objectها در یک `shared` عمومی قرار گیرند.

اصل:

```text
Shared
  ≠
Generic Common Types
```

Shared Domain Code باید فقط شامل مفاهیمی باشد که واقعاً بین بخش‌های مدل مشترک هستند.

---

# 21. Value Object و Aggregate Boundary

Value Objectها بخشی از Aggregate State هستند و نباید باعث شکستن Aggregate Boundary شوند.

مثلاً:

```text
JournalEntry Aggregate

JournalEntry
│
├── JournalEntryId
├── PostingState
├── Money
└── JournalLines
```

وجود `Money` یا `AccountNumber` نباید باعث ایجاد Aggregate مستقل شود.

مرز Aggregate همچنان بر اساس Business Invariant و Consistency Boundary تعیین می‌شود.

جزئیات Aggregate Boundary در **ADR-0012 — Entity & Aggregate Strategy** تعریف شده است.

---

# قوانین (Rules)

* Value Object Identity مستقل ندارد.
* Equality در Value Object بر اساس Value است.
* Value Object باید Immutable باشد.
* Value Object باید State معتبر ایجاد کند.
* Business Rule مربوط به Value باید تا حد امکان داخل خود Value Object قرار گیرد.
* Value Object نباید به Framework وابسته باشد.
* Value Object نباید Repository مستقل داشته باشد.
* Value Object نباید صرفاً Wrapper یک Primitive بدون Business Meaning باشد.
* Primitive Obsession برای مفاهیم مهم Domain باید کاهش داده شود.
* Value Object معمولاً بخشی از Aggregate است.
* Value Object نباید صرفاً به دلیل استفاده در چند Aggregate به Shared Kernel منتقل شود.
* Persistence Model نباید باعث حذف Value Object از Domain شود.
* Persistence Mapping مربوط به Value Object در Infrastructure انجام می‌شود.
* Value Object باید بر اساس Ubiquitous Language نام‌گذاری شود.
* هر Value Object باید Invariantهای مربوط به مفهوم خودش را محافظت کند.
* ایجاد Value Object نامعتبر نباید امکان‌پذیر باشد.
* Optional بودن یک مفهوم باید بر اساس Business Model تعیین شود.
* Identity مربوط به Aggregate Root می‌تواند و در این معماری به صورت Value Object تخصصی مدل می‌شود.
* Identity Value Object نباید Repository یا Lifecycle مستقل داشته باشد.
* Identity Value Object نباید به عنوان Entity یا Aggregate مدل شود.
* مرز Aggregate بر اساس Identity Value Object تعیین نمی‌شود؛ بلکه بر اساس Business Invariant و Consistency Boundary تعیین می‌شود.

---

# پیامدها (Consequences)

## مزایا

* افزایش خوانایی Domain Model
* کاهش Primitive Obsession
* افزایش Type Safety
* تمرکز Business Ruleها
* جلوگیری از ایجاد State نامعتبر
* Equality قابل پیش‌بینی
* افزایش Testability
* کاهش Coupling
* استقلال Domain از Persistence و Framework
* نمایش بهتر Ubiquitous Language در کد
* حفظ Aggregate Boundary
* Type-safe شدن Identityهای Domain
* جلوگیری از اشتباه بین Identityهای مختلف Domain
* کاهش استفاده مستقیم از `UUID` و Primitiveهای مشابه در Business Logic

## معایب

* افزایش تعداد Typeها و کلاس‌ها
* نیاز به Mapping در Persistence Layer
* نیاز به طراحی دقیق Immutable Objectها
* احتمال Over-Engineering در صورت ایجاد Value Object برای مفاهیم بسیار ساده
* افزایش Complexity اولیه Domain Model
* نیاز به تصمیم‌گیری دقیق درباره Shared Value Objects
* نیاز به تعریف Type مستقل برای Identity هر Entity/Aggregate Root

---

# وضعیت اجرا (Implementation Status)

این استراتژی برای **General Ledger Bounded Context** اعمال می‌شود.

در طراحی هر مفهوم Domain ابتدا باید مشخص شود:

```text
                 Domain Concept
                       │
                       ▼
             دارای Identity مستقل؟
                  /           \
                Yes            No
                 │              │
                 ▼              ▼
              Entity      دارای Business
                           Meaning / Rules؟
                            /          \
                          Yes           No
                           │             │
                           ▼             ▼
                    Value Object      Primitive
```

برای Entityها و Aggregate Rootها، Identity باید به صورت Type مشخص Domain-specific مدل شود.

در General Ledger، نمونه‌های اصلی عبارت‌اند از:

```text
Account
└── AccountId

AccountHead
└── AccountHeadId

JournalEntry
└── JournalEntryId
```

که:

```text
AccountId
AccountHeadId
JournalEntryId
```

همگی Value Objectهای مربوط به Identity هستند.

این Identity Value Objectها:

1. Identity مستقل ندارند.
2. Lifecycle مستقل ندارند.
3. Repository مستقل ندارند.
4. بخشی از State مربوط به Entity/Aggregate هستند.
5. Equality مبتنی بر Value دارند.
6. باید Immutable باشند.
7. باید Domain-specific و Type-safe باشند.

در طراحی سایر مفاهیم نیز ابتدا باید مشخص شود که مفهوم موردنظر:

1. دارای Identity و Lifecycle مستقل است → Entity
2. فاقد Identity مستقل و مبتنی بر Value است → Value Object
3. مجموعه‌ای از Entityها و Value Objectها با یک Consistency Boundary است → Aggregate

جزئیات مربوط به Entity و Aggregate در **ADR-0012 — Entity & Aggregate Strategy** و جزئیات مربوط به Identity در **ADR-0018 — Domain Identity Strategy** تعریف می‌شوند.

---

# References

1. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**
2. Vaughn Vernon — **Implementing Domain-Driven Design**
3. Vaughn Vernon — **Domain-Driven Design Distilled**
4. Martin Fowler — **Value Object**
5. Martin Fowler — **Patterns of Enterprise Application Architecture**
6. Robert C. Martin — **Clean Architecture: A Craftsman's Guide to Software Structure and Design**
7. Joshua Bloch — **Effective Java**
8. Java SE Documentation — **Records**

---

##مهم

```text
Aggregate Root
      │
      ▼
Entity
      │
      └── Identity
             │
             ▼
       Identity Value Object
```

پس در General Ledger:

```text
Account       → AccountId       → Value Object
AccountHead   → AccountHeadId   → Value Object
JournalEntry  → JournalEntryId  → Value Object
```

و این با ADR-0012 کاملاً سازگار است:

**خود `Account`، `AccountHead` و `JournalEntry` Entity/Aggregate Root هستند؛ ID آن‌ها Value Object است، نه اینکه خود ID یک Entity باشد.**
