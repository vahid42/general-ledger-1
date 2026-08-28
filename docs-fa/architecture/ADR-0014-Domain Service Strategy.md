# ADR-0014 — استراتژی Domain Service (Domain Service Strategy)

* **وضعیت:** Accepted
* **تاریخ:** 2026-08-22
* **ADRهای مرتبط:**

  * ADR-0011 
  * ADR-0012 
  * ADR-0013 
  * ADR-0016 
  * ADR-0017 

---

# زمینه (Context)

در Domain-Driven Design، همه Business Ruleها به‌صورت طبیعی متعلق به یک Entity، Aggregate یا Value Object نیستند.

برخی Business Conceptها ممکن است:

* متعلق به یک Entity یا Aggregate مشخص نباشند.
* ذاتاً بین چند Aggregate قرار داشته باشند.
* یک Business Operation مستقل را مدل کنند.
* برای تصمیم‌گیری Business به چند مفهوم Domain نیاز داشته باشند.

در چنین شرایطی **Domain Service** می‌تواند محل مناسبی برای مدل‌سازی آن Business Logic باشد.

با این حال، Domain Service نباید به محل عمومی برای قرار دادن Business Logic، CRUD، Repository Query یا Application Orchestration تبدیل شود.

هدف این ADR تعیین:

* زمان ایجاد Domain Service
* مسئولیت آن
* رابطه آن با Entity و Aggregate
* رابطه آن با Application Service
* نحوه برخورد با Repository و External Dependency
* محدودیت‌های استفاده از Domain Service

است.

---

# تصمیم (Decision)

## 1. Domain Service چیست؟

Domain Service یک **Business Concept یا Business Operation بدون مالک طبیعی در یک Entity، Aggregate یا Value Object** است.

Domain Service زمانی ایجاد می‌شود که قرار دادن Rule در یک Domain Object مشخص باعث کاهش Cohesion یا ایجاد مسئولیت نامناسب شود.

مثال:

```java
public class FundsTransferService {

    public void transfer(
            Account source,
            Account destination,
            Money amount
    ) {
        source.withdraw(amount);
        destination.deposit(amount);
    }
}
```

در این مثال عملیات انتقال ذاتاً متعلق به یک Account نیست، زیرا دو Aggregate درگیر عملیات هستند.

---

# 2. Domain Service انتخاب اول نیست

Domain Service نباید اولین محل برای قرار دادن Business Logic باشد.

ترتیب تصمیم‌گیری:

```text
Business Rule
      │
      ▼
آیا Rule متعلق به یک Value Object است؟
      │
      ├── Yes ──► Value Object
      │
      ▼
آیا Rule متعلق به یک Entity / Aggregate است؟
      │
      ├── Yes ──► Entity / Aggregate
      │
      ▼
آیا Rule یک Business Concept مستقل است؟
      │
      ├── Yes ──► Domain Service
      │
      ▼
بازطراحی Domain Model
```

بنابراین:

> Domain Service نباید جایگزین رفتار طبیعی Aggregate یا Value Object شود.

---

# 3. Aggregate باید مالک Invariantهای خودش باشد

وجود چند Aggregate به‌تنهایی دلیل ایجاد Domain Service نیست.

اگر Business Rule متعلق به یک Aggregate است، باید تا حد امکان داخل همان Aggregate قرار گیرد.

مثلاً:

```java
public class Account {

    public void withdraw(Money amount) {

        if (balance.isLessThan(amount)) {
            throw new InsufficientBalanceException();
        }

        balance = balance.subtract(amount);
    }
}
```

نباید این Rule صرفاً به دلیل وجود Service به شکل زیر منتقل شود:

```java
public class AccountDomainService {

    public void withdraw(
            Account account,
            Money amount
    ) {
        // Account business rules
    }
}
```

---

# 4. Domain Service نباید صرفاً به دلیل Repository Dependency ایجاد شود

**صرف اینکه اجرای یک Rule به Repository نیاز دارد، دلیل کافی برای ایجاد Domain Service نیست.**

برای مثال:

```java
public void ensureCodeIsUnique(String code) {
    if (repository.existsByCode(code)) {
        ...
    }
}
```

این کد لزوماً Domain Service نیست.

اگر بررسی uniqueness بخشی از orchestration یک Use Case باشد، Application Layer می‌تواند Repository را برای این بررسی استفاده کند.

```text
CreateAccountHeading
        │
        ├── validate input
        │
        ├── check repository
        │
        ├── create AccountHeading
        │
        └── save AccountHeading
```

بنابراین کلاسی مانند:

```java
AccountHeadingDomainService
```

که صرفاً:

```text
existsByCode()
existsByCodeAndIdNot()
```

را فراخوانی می‌کند، به‌صورت پیش‌فرض ایجاد نمی‌شود.

---

# 5. Validation متعلق به Value Object باید در Value Object باشد

اگر یک مفهوم دارای Business Meaning و Validation مستقل باشد، بهتر است به صورت Value Object مدل شود.

مثلاً:

```java
public record AccountHeadingCode(String value) {

    public AccountHeadingCode {

        Objects.requireNonNull(
                value,
                "code must not be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Account heading code must not be blank"
            );
        }
    }
}
```

در این حالت:

```text
AccountHeadingCode
        │
        └── structural/domain validation
```

و:

```text
Application Layer
        │
        └── repository-dependent uniqueness check
```

از یکدیگر جدا هستند.

---

# 6. Uniqueness یک مورد خاص است

Unique بودن یک Attribute ممکن است Business Rule باشد، اما تضمین آن معمولاً به Persistence نیز وابسته است.

مثلاً:

```text
AccountHeading
      │
      └── code must be unique
```

Application Layer می‌تواند برای User Experience بهتر، قبل از ایجاد Entity بررسی کند:

```java
if (repository.existsByCode(code)) {
    throw new AccountHeadingCodeAlreadyExistsException(code);
}
```

اما این بررسی به تنهایی تضمین‌کننده uniqueness نیست.

برای جلوگیری از Race Condition، Database نیز باید Constraint مناسب داشته باشد:

```text
Domain
  │
  └── AccountHeadingCode

Application
  │
  └── optional pre-check

Database
  │
  └── UNIQUE(code)
```

بنابراین:

> Domain Service نباید برای جایگزین کردن Database Constraint استفاده شود.

---

# 7. Domain Service و چند Aggregate

یکی از کاربردهای اصلی Domain Service، Business Operationهایی است که واقعاً چند Aggregate را درگیر می‌کنند.

مثال:

```text
             Transfer
                 │
        ┌────────┴────────┐
        ▼                 ▼
 Source Account    Destination Account
```

در چنین شرایطی:

```java
transferService.transfer(
        sourceAccount,
        destinationAccount,
        amount
);
```

می‌تواند مناسب باشد.

اما قبل از ایجاد Domain Service باید بررسی شود که آیا این دو مفهوم واقعاً باید Aggregateهای جداگانه باشند یا خیر.

---

# 8. Domain Service نباید Aggregate Boundary را دور بزند

اگر یک Rule دائماً نیاز دارد چند Entity را به صورت strongly consistent تغییر دهد، ابتدا باید بررسی شود که Aggregate Boundary اشتباه طراحی نشده باشد.

مثلاً:

```text
Aggregate A
     │
     │
     └────── Business Rule ──────┐
                                 │
Aggregate B                      │
     │                           │
     └───────────────────────────┘
```

قبل از ایجاد Domain Service باید سؤال شود:

> آیا این دو مفهوم واقعاً باید در دو Aggregate جدا باشند؟

اگر پاسخ منفی باشد، باید Aggregate Boundary بازطراحی شود.

Domain Service نباید صرفاً راهی برای دور زدن Aggregate Boundary باشد.

---

# 9. Domain Service باید Stateless باشد

Domain Serviceها ترجیحاً Stateless هستند.

State مربوط به Business Entity نباید داخل Domain Service نگهداری شود.

مناسب:

```java
public class CurrencyExchangeService {

    public Money exchange(
            Money source,
            Currency target,
            ExchangeRate rate
    ) {
        return source.convert(rate, target);
    }
}
```

نامناسب:

```java
public class AccountService {

    private Account account;

    public void withdraw(Money amount) {
        // ...
    }
}
```

State اصلی Domain باید در Entity یا Aggregate قرار داشته باشد.

---

# 10. Domain Service نباید Application Service باشد

Domain Service و Application Service دو مسئولیت متفاوت دارند.

### Domain Service

مسئول:

```text
Business Logic
Business Decision
Business Operation
```

### Application Service

مسئول:

```text
Use Case
Orchestration
Loading
Calling Domain
Persistence Coordination
Transaction Boundary
```

مثال:

```text
Application Service
        │
        ├── Load Source Account
        │
        ├── Load Destination Account
        │
        ├── Call Domain Logic
        │
        ▼
Domain Service
        │
        └── Execute Transfer Rule
        │
        ▼
Application Service
        │
        └── Save Aggregates
```

Application Service نباید مالک Business Rule اصلی باشد.

---

# 11. Repository Query لزوماً Domain Service نیست

Repository-dependent operations باید با دقت بررسی شوند.

مواردی مانند:

```text
existsByCode()
findById()
findByNumber()
existsBy...
```

به‌تنهایی دلیل ایجاد Domain Service نیستند.

اگر این عملیات بخشی از اجرای Use Case باشند:

```text
Application Service
       │
       └── Repository Port
```

می‌تواند محل مناسبی برای orchestration باشد.

Domain Service زمانی ایجاد می‌شود که خود **Business Operation** مستقل ارزش مدل‌سازی داشته باشد.

---

# 12. Domain Service نباید Repository Service شود

Domain Service نباید به یک کلاس عمومی برای عملیات Database تبدیل شود.

نامناسب:

```java
class AccountService {

    Account findById(...);

    void save(...);

    void delete(...);

    List<Account> findAll(...);
}
```

این مسئولیت‌ها متعلق به Repository و Application Layer هستند.

Domain Service باید:

```text
Business Logic
```

داشته باشد، نه:

```text
CRUD Logic
```

---

# 13. Domain Service نباید Transaction Manager باشد

مدیریت Transaction مسئولیت Application Layer و Infrastructure است.

Domain Service نباید وابسته به:

```java
@Transactional
```

باشد.

مرز Transaction باید توسط Use Case و Application Layer تعیین شود.

```text
Application Use Case
        │
        ▼
Transaction Boundary
        │
        ├── Aggregate A
        ├── Aggregate B
        └── Domain Service
```

Domain Service نباید مسئول مدیریت Transaction باشد.

---

# 14. Domain Service و External Dependency

Domain Service ممکن است برای یک Business Decision به یک قابلیت خارجی نیاز داشته باشد.

در این حالت Domain فقط باید Abstraction را بشناسد.

مثال:

```java
public interface CreditScoreProvider {

    CreditScore getScore(CustomerId customerId);
}
```

Domain Service:

```java
public class LoanEligibilityService {

    private final CreditScoreProvider creditScoreProvider;

    public LoanEligibilityService(
            CreditScoreProvider creditScoreProvider
    ) {
        this.creditScoreProvider = creditScoreProvider;
    }

    public boolean isEligible(CustomerId customerId) {

        CreditScore score =
                creditScoreProvider.getScore(customerId);

        // Business Rule
        return score.isAcceptable();
    }
}
```

Implementation مربوط به API یا Infrastructure در خارج از Domain قرار می‌گیرد.

```text
Domain
  │
  ▼
CreditScoreProvider
  ▲
  │
Infrastructure
  │
  ▼
External API
```

---

# 15. Domain Service نباید Framework Dependency داشته باشد

Domain Service باید Framework Independent باشد.

استفاده از موارد زیر در Domain Service مجاز نیست:

```java
@Service
@Component
@Autowired
@Transactional
```

مگر اینکه در ADR مستقل، استثنای مشخصی تصویب شده باشد.

Domain Service باید بدون Spring Context قابل Unit Test باشد.

---

# 16. Domain Service باید Business-Oriented باشد

نام Domain Service باید از Ubiquitous Language گرفته شود.

مناسب:

```text
FundsTransferService
CurrencyExchangeService
LoanEligibilityService
SettlementService
InterestCalculationService
```

نامناسب:

```text
CommonService
UtilityService
HelperService
Manager
Processor
Handler
GenericService
```

نام باید یک Business Capability مشخص را نشان دهد.

---

# 17. Domain Service و Policy

Policy و Domain Service مفاهیم یکسانی نیستند.

### Policy

یک Business Rule یا Decision قابل تغییر یا قابل جایگزینی را مدل می‌کند.

### Domain Service

یک Business Operation یا Domain Capability را مدل می‌کند.

مثال:

```text
FundsTransferService
        │
        ▼
TransferFeePolicy
        │
        ▼
Calculate Fee
```

در این ساختار:

```text
FundsTransferService
    → عملیات انتقال

TransferFeePolicy
    → سیاست محاسبه کارمزد
```

جزئیات Policy در صورت نیاز می‌تواند در ADR مربوط به Domain Policy تعریف شود.

---

# 18. Domain Service نباید محل پیش‌فرض Logic باشد

این Anti-Pattern باید جلوگیری شود:

```text
Entity
   │
   └── Anemic
          │
          ▼
Domain Service
          │
          └── Everything
```

مدل مطلوب:

```text
Entity / Aggregate
        │
        └── Own Business Behavior

Value Object
        │
        └── Own Value Rules

Domain Service
        │
        └── Independent Domain Operation
```

---

# 19. Domain Service و Aggregateهای General Ledger

با توجه به **General Ledger Domain & Bounded Context Decision Baseline**، سه Aggregate اصلی داریم:

```text
General Ledger
│
├── AccountHead
├── Account
└── JournalEntry
```

وجود این سه Aggregate به معنی ایجاد سه Domain Service نیست.

بنابراین ایجاد Serviceهایی مانند:

```text
AccountHeadDomainService
AccountDomainService
JournalEntryDomainService
```

به‌صورت خودکار **ممنوع/غلط معماری** است.

هر کدام باید بر اساس یک Business Need واقعی ایجاد شوند.

به‌خصوص:

```text
JournalEntry
```

به عنوان Core Aggregate باید بیشترین Business Behavior را در خود Aggregate نگه دارد.

Domain Service نباید منطق اصلی Journal Entry را از Aggregate خارج کند.

---

# 20. مثال General Ledger — AccountHeading

فرض کنیم:

```java
AccountHeading
```

دارای:

```java
AccountHeadingCode
```

باشد.

Validation مربوط به خود Code:

```java
public record AccountHeadingCode(String value) {

    public AccountHeadingCode {

        if (value == null || value.isBlank()) {
            throw new InvalidAccountHeadingCodeException();
        }
    }
}
```

اما بررسی وجود Code در Repository:

```java
repository.existsByCode(code)
```

به‌تنهایی دلیل ایجاد:

```java
AccountHeadingDomainService
```

نیست.

Use Case می‌تواند این کار را انجام دهد:

```text
CreateAccountHeading
        │
        ├── Validate AccountHeadingCode
        │
        ├── Check repository
        │
        ├── AccountHeading.create(...)
        │
        └── repository.save(...)
```

و Database نیز باید uniqueness واقعی را تضمین کند.

---

# 21. Domain Service Testing

Domain Service باید با Unit Test مستقل تست شود.

تست باید Business Behavior را بررسی کند.

نباید برای Unit Test به موارد زیر نیاز داشته باشد:

* Spring Context
* Database
* HTTP
* Message Broker
* External Service واقعی

در صورت وجود External Dependency، باید Abstraction آن Mock یا Fake شود.

---

# قوانین (Rules)

* Domain Service فقط زمانی ایجاد می‌شود که Business Logic مالک طبیعی در Entity، Aggregate یا Value Object نداشته باشد.
* Domain Service باید یک Business Concept یا Business Operation مشخص داشته باشد.
* Domain Service جایگزین Entity Behavior نیست.
* Domain Service جایگزین Aggregate Behavior نیست.
* Domain Service ترجیحاً Stateless است.
* Domain Service نباید محل CRUD Logic باشد.
* Domain Service نباید صرفاً برای Repository Query ایجاد شود.
* Repository Dependency به‌تنهایی دلیل ایجاد Domain Service نیست.
* `existsBy...` و `findBy...` به‌تنهایی Domain Service ایجاد نمی‌کنند.
* Validation داخلی Value Object باید در خود Value Object قرار گیرد.
* Aggregate باید مالک Invariantهای خودش باشد.
* Domain Service نباید Aggregate Boundary را دور بزند.
* Domain Service نباید Application Orchestration انجام دهد.
* Domain Service نباید مسئول Transaction Management باشد.
* Domain Service نباید به Framework وابسته باشد.
* Domain Service می‌تواند به Domain Abstraction وابسته باشد.
* Implementation وابستگی‌های خارجی باید خارج از Domain قرار گیرد.
* Domain Service نباید محل عمومی برای Business Logicهای نامشخص باشد.
* هر Domain Service باید Business Meaning مشخص داشته باشد.
* وجود چند Aggregate به معنی وجود چند Domain Service نیست.

---

# Domain Service Decision Checklist

قبل از ایجاد Domain Service، این سؤالات باید بررسی شوند:

```text
Business Rule
      │
      ▼
آیا Rule متعلق به Value Object است؟
      │
     Yes
      ▼
Value Object

      No
      │
      ▼
آیا Rule متعلق به Entity / Aggregate است؟
      │
     Yes
      ▼
Entity / Aggregate

      No
      │
      ▼
آیا یک Business Concept مستقل است؟
      │
     Yes
      ▼
Domain Service

      No
      │
      ▼
بازطراحی Domain Model
```

همچنین:

```text
آیا فقط به Repository نیاز دارد؟
        │
       Yes
        ▼
آیا واقعاً Business Operation مستقل است؟
        │
       No
        ▼
Application Layer
```

---

# پیامدها (Consequences)

## مزایا

* جلوگیری از Anemic Domain Model
* حفظ Business Logic در محل مناسب
* جلوگیری از تبدیل Application Service به محل Business Logic
* مدل‌سازی Business Conceptهای مستقل
* مناسب برای عملیات واقعی بین چند Aggregate
* افزایش Testability
* کاهش Coupling
* جلوگیری از ایجاد Domain Serviceهای مصنوعی
* جلوگیری از تبدیل Domain Service به CRUD Service
* حفظ استقلال Domain از Framework

## معایب

* تشخیص مرز Domain Service نیازمند درک دقیق Domain است.
* احتمال ایجاد Domain Serviceهای غیرضروری وجود دارد.
* در صورت طراحی اشتباه ممکن است Domain Service به God Service تبدیل شود.
* برخی عملیات بین Aggregateها ممکن است Eventual Consistency نیاز داشته باشند.
* تشخیص اینکه یک Rule متعلق به Aggregate است یا Domain Service ممکن است پیچیده باشد.

---

# وضعیت اجرا (Implementation Status)

این استراتژی برای تمام Moduleهای Domain اعمال می‌شود.

قبل از ایجاد هر Domain Service باید مشخص شود که چرا Business Logic نمی‌تواند به صورت طبیعی در:

1. Value Object
2. Entity
3. Aggregate

قرار گیرد.

سپس بررسی شود که آیا Business Logic واقعاً یک:

```text
Independent Business Concept
```

یا:

```text
Cross-Aggregate Domain Operation
```

است.

همچنین صرف وجود Repository Dependency، Query یا نیاز به Persistence دلیل کافی برای ایجاد Domain Service محسوب نمی‌شود.

در General Ledger نیز وجود Aggregateهای:

```text
AccountHead
Account
JournalEntry
```

به معنی ایجاد Domain Service متناظر با هر Aggregate نیست.

به‌خصوص Business Logic مربوط به `JournalEntry` باید تا حد امکان در خود `JournalEntry Aggregate` باقی بماند.

---

# References

1. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**
2. Vaughn Vernon — **Implementing Domain-Driven Design**
3. Vaughn Vernon — **Domain-Driven Design Distilled**
4. Martin Fowler — **Domain Service**
5. Martin Fowler — **Service Layer**
6. Martin Fowler — **Anemic Domain Model**
7. Robert C. Martin — **Clean Architecture: A Craftsman's Guide to Software Structure and Design**
8. Chris Richardson — **Microservices Patterns**
