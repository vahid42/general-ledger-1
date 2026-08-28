# ADR-0019 — Domain Policy Placement Strategy

**Status:** Accepted

**Date:** 2026-08-22

**Related ADRs:**

* ADR-0011  
* ADR-0012 
* ADR-0013  
* ADR-0014  
* ADR-0016  
* ADR-0018  

---

## 1. زمینه (Context)

در طراحی Domain پروژه General Ledger، Business Ruleهای مختلفی وجود دارند که باید در محل مناسبی enforce یا orchestrate شوند.

چالش اصلی این است که مشخص شود هر Rule باید در کدام بخش قرار گیرد:

```text
Aggregate
Value Object
Domain Service
Application Service
Repository
Infrastructure
```

برای مثال در `AccountHeading` با Policyهایی مانند:

```text
- حداکثر بودن Level
- Leaf بودن Level 5
- وجود Parent
- وراثت Nature
- وراثت allowNegativeBalance
- یکتا بودن Code
- وجود Child
- وجود Account
- امکان ایجاد Account فقط در Level 5
```

همه این Policyها ماهیت یکسانی ندارند.

برخی فقط به State داخلی Aggregate نیاز دارند و برخی برای تصمیم‌گیری به اطلاعات خارج از Aggregate نیازمند هستند.

هدف این ADR تعیین محل صحیح قرارگیری و اجرای Business Policyها است، به‌گونه‌ای که:

* Domain مستقل از Persistence باقی بماند.
* Aggregate دارای رفتار و Invariant واقعی باشد.
* Repository محل Business Logic نباشد.
* Domain Service فقط در صورت نیاز واقعی ایجاد شود.
* Application Service مسئول Use Case Orchestration باشد.
* Aggregate Boundary حفظ شود.
* Cross-Aggregate Ruleها به‌درستی مدیریت شوند.

---

# 2. مسئله (Problem)

Business Policyهای Domain را می‌توان بر اساس **دامنه اطلاعات مورد نیاز برای تصمیم‌گیری** دسته‌بندی کرد.

### نوع اول — Aggregate-Local Invariant

Policyهایی که برای تصمیم‌گیری فقط به State و رفتار همان Aggregate نیاز دارند.

مثلاً:

```text
آیا Level معتبر است؟

آیا این Heading می‌تواند Child داشته باشد؟

آیا Nature Child با Parent سازگار است؟

آیا allowNegativeBalance باید از Parent به Child منتقل شود؟

آیا Account فقط در Level 5 می‌تواند ایجاد شود؟
```

این قوانین باید توسط خود Aggregate enforce شوند.

---

### نوع دوم — Cross-Aggregate Policy

Policyهایی که برای تصمیم‌گیری نیازمند اطلاعات خارج از Aggregate هستند.

مثلاً:

```text
آیا Code قبلاً در Tree استفاده شده است؟

آیا Parent با این ID وجود دارد؟

آیا Heading دارای Child است؟

آیا Heading دارای Account است؟
```

این قوانین توسط Aggregate منفرد قابل تصمیم‌گیری نیستند.

---

### نوع سوم — Use Case Orchestration

برخی عملیات Business نیازمند هماهنگی چند Repository یا چند Aggregate هستند، اما منطق Business پیچیده‌ای ندارند.

مثلاً:

```text
Load Parent
      ↓
Validate Input
      ↓
Check Code
      ↓
Create Aggregate
      ↓
Save Aggregate
```

این نوع هماهنگی مسئولیت **Application Service** است.

---

### نوع چهارم — Cross-Aggregate Domain Rule

اگر یک Business Rule واقعاً متعلق به هیچ Aggregate منفردی نباشد و منطق آن فراتر از orchestration ساده باشد، باید در **Domain Service** قرار گیرد.

بنابراین صرف استفاده از Repository به معنی نیاز به Domain Service نیست.

---

# 3. تصمیم معماری (Decision)

قاعده اصلی پروژه به شکل زیر تعریف می‌شود:

> اگر یک Business Rule فقط به State و رفتار یک Aggregate نیاز دارد، Rule باید در همان Aggregate enforce شود.

اگر Rule برای تصمیم‌گیری به اطلاعات خارج از Aggregate نیاز داشته باشد:

> Aggregate نباید مستقیماً Repository یا Persistence را صدا بزند.

در این حالت:

```text
External Information
        │
        ▼
Repository Port
        │
        ▼
Application Service
        │
        ├── Simple orchestration
        │
        └── Complex domain rule
                    │
                    ▼
              Domain Service
```

بنابراین **وجود Repository dependency به‌تنهایی دلیل ایجاد Domain Service نیست.**

---

# 4. قانون اصلی Placement

تصمیم نهایی:

```text
                    Business Policy
                          │
                          ▼
              آیا فقط State داخلی
                 Aggregate کافی است؟
                    /           \
                  Yes            No
                   │              │
                   ▼              ▼
              Aggregate      External Data
                                  │
                                  ▼
                            Repository Port
                                  │
                                  ▼
                        آیا فقط Orchestration
                              ساده است؟
                           /          \
                         Yes          No
                          │            │
                          ▼            ▼
                    Application   Domain Service
                       Service
```

قاعده مهم:

```text
Simple Cross-Aggregate Coordination
            ↓
     Application Service
```

و:

```text
Cross-Aggregate Business Rule
            ↓
       Domain Service
```

---

# 5. Aggregate Invariant

هر Policy که فقط به State داخلی Aggregate نیاز دارد، باید در خود Aggregate enforce شود.

برای `AccountHeading` نمونه‌هایی از این قوانین:

```text
- Level معتبر باشد.
- Level از حداکثر مجاز بیشتر نباشد.
- Level 5 بتواند Leaf باشد.
- Leaf نتواند Child ایجاد کند.
- Nature طبق Rule Domain تغییر نکند.
- allowNegativeBalance طبق Rule Domain منتقل شود.
- Account فقط در Level 5 قابل ایجاد باشد.
```

مثلاً:

```java
public boolean canCreateAccount() {
    return level == 5;
}
```

یا:

```java
public AccountHeading createChild(...) {

    if (isLeaf()) {
        throw new InvalidAccountHeadingOperationException(...);
    }

    ...
}
```

در این حالت Aggregate خودش مسئول حفظ Invariantهای داخلی خود است.

---

# 6. Aggregate نباید Repository را صدا بزند

این طراحی ممنوع است:

```java
public class AccountHeading {

    private AccountHeadingRepository repository;

    public void createChild(...) {

        if (repository.existsByCode(code)) {
            ...
        }
    }
}
```

Aggregate نباید Dependency به Repository داشته باشد.

ساختار مطلوب:

```text
Aggregate
    │
    └── Domain State + Behavior
```

و نه:

```text
Aggregate
    │
    └── Repository
            │
            ▼
         Database
```

این قانون باعث حفظ استقلال Domain و Aggregate Boundary می‌شود.

---

# 7. Repository به‌عنوان Data Access Abstraction

Repository مسئول فراهم کردن اطلاعات مورد نیاز Use Case یا Domain Policy است.

مثلاً:

```java
public interface AccountHeadingRepository {

    Optional<AccountHeading> findById(AccountHeadingId id);

    boolean existsByCode(AccountHeadingCode code);

    boolean existsByParentId(AccountHeadingId parentId);

    boolean existsAccountByHeadingId(AccountHeadingId headingId);

    void save(AccountHeading accountHeading);

    void delete(AccountHeading accountHeading);
}
```

اما:

> Repository مسئول اجرای Business Rule نیست.

بنابراین این طراحی مناسب نیست:

```java
public interface AccountHeadingRepository {

    void validateAccountHeading(...);

}
```

یا:

```java
public interface AccountHeadingRepository {

    boolean createIfCodeDoesNotExist(...);

}
```

Repository باید **Data Access Capability** ارائه کند، نه Business Decision.

---

# 8. نکته مهم — Repository Query تضمین Business Invariant نیست

وجود Queryای مانند:

```java
boolean existsByCode(AccountHeadingCode code);
```

به‌تنهایی تضمین نمی‌کند که Code واقعاً Unique باقی بماند.

مثلاً:

```text
Request A                  Request B

existsByCode()
     ↓                          ↓
   false                      false
     ↓                          ↓
create A                   create B
```

هر دو Request ممکن است همزمان Code را آزاد ببینند.

بنابراین:

> Repository Query می‌تواند برای Decision/Validation استفاده شود، اما در شرایط Concurrent لزوماً Guarantee نهایی ایجاد نمی‌کند.

تضمین نهایی چنین Constraintهایی در ADRهای مربوط به:

```text
Concurrency
Persistence
Database Constraints
Transaction
```

مشخص خواهد شد.

در صورت نیاز، Database Constraint می‌تواند آخرین لایه تضمین Integrity باشد.

---

# 9. Unique Code

Policy:

> `AccountHeading.code` باید در محدوده تعریف‌شده توسط Domain یکتا باشد.

این Policy صرفاً Invariant داخلی یک `AccountHeading` نیست، زیرا یک Aggregate به تنهایی از وضعیت سایر Aggregateها اطلاع ندارد.

Repository می‌تواند اطلاعات لازم را فراهم کند:

```java
boolean existsByCode(AccountHeadingCode code);
```

Application Service می‌تواند از این اطلاعات برای اجرای Use Case استفاده کند.

مثلاً:

```text
Application Service
        │
        ├── validate input
        │
        ├── repository.existsByCode(...)
        │
        ├── create AccountHeading
        │
        └── save
```

اگر Rule پیچیده‌تر شود و واقعاً یک Domain Policy مستقل شکل بگیرد، Domain Service می‌تواند مسئول آن باشد.

اما:

> Query مربوط به Repository خودش Business Policy محسوب نمی‌شود.

---

# 10. Domain Service

Domain Service زمانی استفاده می‌شود که:

1. یک Business Rule واقعی وجود داشته باشد.
2. Rule متعلق به یک Aggregate منفرد نباشد.
3. Rule نیازمند همکاری چند Aggregate یا اطلاعات خارجی باشد.
4. منطق آن فراتر از Application Orchestration ساده باشد.

مثلاً:

```java
public class AccountHeadingDomainService {

    private final AccountHeadingRepository repository;

    public AccountHeadingDomainService(
            AccountHeadingRepository repository
    ) {
        this.repository = repository;
    }

    public void validateCodeUniqueness(
            AccountHeadingCode code
    ) {

        if (repository.existsByCode(code)) {
            throw new DuplicateAccountHeadingCodeException(code);
        }
    }
}
```

اما این Domain Service فقط زمانی ایجاد می‌شود که این Validation واقعاً بخشی از یک Business Policy مستقل باشد.

برای یک Check ساده در یک Use Case، ایجاد Domain Service ضروری نیست.

---

# 11. Application Service

Application Service مسئول هماهنگی Use Case است.

مسئولیت‌های اصلی:

```text
Use Case Orchestration
        +
Repository Access
        +
Aggregate Coordination
        +
Transaction Boundary Coordination
```

مثلاً:

```text
Create AccountHeading
        │
        ▼
Application Service
        │
        ├── Validate Input
        │
        ├── Load Parent
        │
        ├── Check external conditions
        │
        ├── Invoke Aggregate behavior
        │
        └── Save Aggregate
```

Application Service نباید Business Ruleهای متعلق به Aggregate را از Aggregate خارج کند.

نامناسب:

```java
if (heading.getLevel() == 5) {
    ...
}
```

اگر این Rule متعلق به خود `AccountHeading` باشد.

مناسب:

```java
heading.canCreateAccount();
```

یا:

```java
heading.createAccount(...);
```

---

# 12. Parent Existence

Policy:

> Child باید Parent داشته باشد.

این موضوع باید به دو مفهوم جدا تقسیم شود.

### 12.1 Parent State

اگر Parent در اختیار Domain باشد، Aggregate می‌تواند Rule مربوط به ایجاد Child را enforce کند:

```java
parent.createChild(...);
```

در این حالت:

```text
Parent Aggregate
       │
       ▼
createChild()
       │
       ▼
Child
```

رفتار Domain در خود Aggregate قرار دارد.

---

### 12.2 Parent Lookup

اگر Application فقط این را دریافت کند:

```text
parentId
```

پیدا کردن Parent مسئولیت Repository است:

```text
Application Service
        │
        ▼
Repository
        │
        ▼
Parent Aggregate
        │
        ▼
parent.createChild(...)
```

بنابراین:

> Lookup کردن Parent مسئولیت Repository است، اما Rule مربوط به رفتار Parent و Child متعلق به Domain است.

اگر Parent یک Aggregate مستقل باشد، وجود آن یک **Cross-Aggregate Business Condition** محسوب می‌شود و نباید به‌عنوان State داخلی Aggregate فرزند فرض شود.

---

# 13. حذف AccountHeading

Policy:

> Heading دارای Child نباید حذف شود.

اگر Childها Aggregateهای مستقل باشند، `AccountHeading` به تنهایی نمی‌تواند وجود Child را از State خودش تشخیص دهد.

بنابراین:

```java
boolean existsByParentId(AccountHeadingId parentId);
```

می‌تواند اطلاعات لازم را فراهم کند.

Application Service:

```text
Application Service
        │
        ├── existsByParentId(id)
        │
        ├── existsAccountByHeadingId(id)
        │
        └── delete(...)
```

اگر این قوانین به یک Cross-Aggregate Business Policy پیچیده تبدیل شوند، Domain Service می‌تواند مسئول آن شود.

---

# 14. Policyهای AccountHeading

| Policy                                           | محل تصمیم‌گیری / اجرا                               | دلیل                                 |
| ------------------------------------------------ | --------------------------------------------------- | ------------------------------------ |
| Level حداکثر 5                                   | Aggregate                                           | State داخلی کافی است                 |
| Level معتبر باشد                                 | Aggregate                                           | State داخلی کافی است                 |
| Leaf نتواند Child داشته باشد                     | Aggregate                                           | State داخلی کافی است                 |
| Nature در Child تغییر نکند                       | Aggregate                                           | Domain State کافی است                |
| `allowNegativeBalance` طبق Rule Domain منتقل شود | Aggregate                                           | Domain State کافی است                |
| Root بدون Parent باشد                            | Aggregate / Factory                                 | ساختار داخلی Aggregate               |
| Account فقط در Level 5 ایجاد شود                 | Aggregate                                           | State داخلی کافی است                 |
| Code از نظر ساختاری معتبر باشد                   | Value Object / Aggregate                            | Validation داخلی                     |
| Code یکتا باشد                                   | Application / Domain Policy + Persistence Guarantee | اطلاعات خارج Aggregate + Concurrency |
| بررسی وجود Code                                  | Repository                                          | Data Access                          |
| Parent با ID مشخص پیدا شود                       | Repository                                          | External Lookup                      |
| Child وجود داشته باشد                            | Repository                                          | اطلاعات خارج Aggregate               |
| Account برای Heading وجود داشته باشد             | Repository                                          | اطلاعات خارج Aggregate               |
| حذف Heading دارای Child                          | Application / Domain Policy                         | Cross-Aggregate Rule                 |
| حذف Heading دارای Account                        | Application / Domain Policy                         | Cross-Aggregate Rule                 |

**نکته:** وجود Repository در یک Rule به معنی قرار گرفتن Business Logic داخل Repository نیست.

---

# 15. مرز مسئولیت‌ها

## Aggregate

مسئول:

```text
Business Invariants
+
State Transition
+
Consistency داخل Aggregate
+
Business Behavior
```

نباید:

```text
Repository
Database
Infrastructure
HTTP
```

را بشناسد.

---

## Value Object

مسئول:

```text
Value Validation
+
Value-based Equality
+
Domain Meaning
```

مثلاً:

```text
AccountHeadingCode
Money
Currency
```

---

## Repository

مسئول:

```text
Load
Save
Delete
Query / Lookup
```

و نباید:

```text
Business Decision
Business Workflow
Domain Rule
```

را در خود پیاده کند.

---

## Application Service

مسئول:

```text
Use Case Orchestration
+
Repository Coordination
+
Aggregate Coordination
+
Transaction Coordination
```

---

## Domain Service

فقط زمانی استفاده می‌شود که:

```text
Business Rule
+
No Single Aggregate Ownership
+
Cross-Aggregate / External Information
+
Non-trivial Domain Logic
```

وجود داشته باشد.

---

# 16. Decision Matrix

قاعده تصمیم‌گیری پروژه:

```text
Business Rule
      │
      ▼
آیا فقط State یک Aggregate کافی است؟
      │
   ┌──┴──┐
  Yes    No
   │      │
   ▼      ▼
Aggregate External Information
            │
            ▼
       Repository Port
            │
            ▼
      آیا فقط Lookup /
       Orchestration است؟
         │          │
        Yes         No
         │           │
         ▼           ▼
   Application   Domain Service
      Service
```

بنابراین:

> **Application Service محل پیش‌فرض برای Orchestration است.**

و:

> **Domain Service راه‌حل پیش‌فرض برای هر Cross-Aggregate Operation نیست.**

---

# 17. Aggregate Boundary

Aggregateها نباید Object Reference مستقیم به Aggregateهای دیگر نگه دارند.

نامناسب:

```java
public class Account {

    private Customer customer;

}
```

اگر `Customer` یک Aggregate مستقل باشد.

مناسب:

```java
public class Account {

    private CustomerId customerId;

}
```

بنابراین:

```text
Account
   │
   └── CustomerId
```

به‌جای:

```text
Account
   │
   └── Customer Object
```

استفاده می‌شود.

این تصمیم با **ADR-0018 — Domain Identity Strategy** نیز هماهنگ است.

---

# 18. Repository Dependency Direction

Repository به‌عنوان Port باید مطابق ADR-0016 در مرز مناسب Domain/Application تعریف شود و Implementation آن در Infrastructure قرار گیرد.

مدل کلی:

```text
Domain / Application
        │
        │ depends on
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

Aggregate نباید مستقیماً این Port را مصرف کند.

---

# 19. Policy Enforcement vs Policy Evaluation

این تفکیک برای معماری پروژه مهم است.

### Policy Evaluation

یعنی:

```text
آیا Code وجود دارد؟
آیا Child وجود دارد؟
آیا Parent وجود دارد؟
```

ممکن است نیازمند Repository باشد.

### Policy Enforcement

یعنی:

```text
اجازه ایجاد Aggregate داده نشود.
State نامعتبر ایجاد نشود.
Invariant شکسته نشود.
```

باید در محل مالک Rule انجام شود.

مثلاً:

```text
Repository
    │
    └── existsByCode()

Application / Domain Policy
    │
    └── تصمیم Business

Aggregate
    │
    └── enforce internal invariant
```

این تفکیک مانع از تبدیل Repository به محل Business Logic می‌شود.

---

# 20. Concurrency و Integrity

برخی Business Policyها Cross-Aggregate هستند و اجرای آن‌ها ممکن است در شرایط Concurrent نیازمند مکانیزم Integrity باشند.

مثلاً:

```text
Request A → Code آزاد است
Request B → Code آزاد است

Request A → Create
Request B → Create
```

بنابراین Check:

```java
existsByCode(...)
```

به‌تنهایی Guarantee نهایی ایجاد نمی‌کند.

تضمین نهایی Integrity باید بر اساس نیاز سیستم در لایه مناسب، مانند:

```text
Database Constraint
+
Transaction
+
Concurrency Control
```

تعیین شود.

جزئیات این موضوع خارج از محدوده این ADR است.

---

# 21. Architectural Rules

قوانین زیر برای Domain Layer الزامی هستند:

### Rule 1

Business Rule متعلق به یک Aggregate باید در همان Aggregate enforce شود.

### Rule 2

Aggregate نباید Repository را صدا بزند.

### Rule 3

Aggregate نباید Persistence یا Infrastructure را بشناسد.

### Rule 4

Aggregateها نباید Object Reference مستقیم به Aggregateهای مستقل داشته باشند.

### Rule 5

Reference بین Aggregateها باید از طریق Typed Identity انجام شود.

### Rule 6

Repository مسئول Data Access است، نه Business Logic.

### Rule 7

Application Service مسئول Use Case Orchestration است.

### Rule 8

هر Cross-Aggregate Rule الزاماً نیازمند Domain Service نیست.

### Rule 9

Domain Service فقط برای Business Rule واقعی و غیرمتعلق به یک Aggregate منفرد ایجاد می‌شود.

### Rule 10

Repository Query به‌تنهایی تضمین‌کننده Business Invariant در شرایط Concurrent نیست.

### Rule 11

Integrity نهایی Policyهای حساس باید در ADRهای مربوط به Persistence و Concurrency مشخص شود.

### Rule 12

Value Objectها مسئول Validation مربوط به Value خود هستند.

### Rule 13

Business Logic نباید صرفاً به دلیل نیاز به Repository از Aggregate خارج شود.

---

# 22. تصمیم نهایی برای AccountHeading

برای `AccountHeading` مرزبندی اصلی به شکل زیر است:

```text
AccountHeading Aggregate
│
├── Level Rules
├── Leaf Rules
├── Parent/Child Domain Behavior
├── Nature Rules
├── allowNegativeBalance Rules
└── Account-at-Level-5 Rule
```

در خارج Aggregate:

```text
Repository
│
├── Find Parent
├── Check Code Existence
├── Check Child Existence
└── Check Account Existence
```

و:

```text
Application Service
│
├── Create Use Case
├── Delete Use Case
├── Load required Aggregates
├── Coordinate Repositories
└── Invoke Aggregate Behavior
```

در صورت وجود Business Rule پیچیده و Cross-Aggregate:

```text
Domain Service
│
└── Cross-Aggregate Domain Policy
```

---

# 23. Consequences

## مزایا

* Aggregate مستقل از Persistence باقی می‌ماند.
* Business Invariantها در نزدیک‌ترین محل مالکیت خود enforce می‌شوند.
* Repository به Business Logic آلوده نمی‌شود.
* Application Service مسئول Orchestration باقی می‌ماند.
* از ایجاد Domain Serviceهای غیرضروری جلوگیری می‌شود.
* Aggregate Boundary حفظ می‌شود.
* Typed Identityهای ADR-0018 در Reference بین Aggregateها قابل استفاده هستند.
* Domain Model قابل تست‌تر باقی می‌ماند.
* مسئولیت Policy Evaluation و Policy Enforcement از یکدیگر تفکیک می‌شود.

## معایب

* برخی Use Caseها نیازمند چند Repository Call هستند.
* Cross-Aggregate Policyها پیچیده‌تر از Invariantهای داخلی هستند.
* برخی Integrity Ruleها نیازمند هماهنگی با Persistence و Concurrency هستند.
* ممکن است در برخی موارد Domain Service لازم شود.
* بخشی از Orchestration در Application Service قرار می‌گیرد.

---

# 24. خارج از محدوده (Out of Scope)

این ADR درباره موارد زیر تصمیم‌گیری نمی‌کند:

```text
Transaction Boundary
Concurrency Control
Optimistic Locking
Pessimistic Locking
Distributed Transaction
Eventual Consistency
Database Constraint Strategy
ORM Mapping
Repository Implementation
```

این موارد در ADRهای تخصصی مربوط به Persistence، Transaction و Concurrency بررسی خواهند شد.

---

# 25. ارتباط با ADRهای پروژه

این ADR بر اساس تصمیمات قبلی Domain ساخته شده است:

```text
ADR-0011
Domain Layer Architecture
        │
        ▼
ADR-0012
Entity & Aggregate Strategy
        │
        ▼
ADR-0013
Value Object Strategy
        │
        ▼
ADR-0014
Domain Service Strategy
        │
        ▼
ADR-0016
Repository Abstraction Strategy
        │
        ▼
ADR-0018
Domain Identity Strategy
        │
        ▼
ADR-0019
Domain Policy Placement Strategy
```

ADR-0019 مشخص می‌کند که پس از تعریف:

```text
Aggregate
Value Object
Domain Service
Repository
Typed Identity
```

هر Business Policy باید بر اساس **اطلاعات مورد نیاز برای تصمیم‌گیری و مالکیت واقعی Rule** در محل مناسب قرار گیرد.

---

# 26. Decision Summary

مدل نهایی:

```text
                 Business Rule
                       │
                       ▼
          ┌─────────────────────────┐
          │ فقط State Aggregate ؟    │
          └────────────┬────────────┘
                       │
              ┌────────┴────────┐
             Yes                No
              │                  │
              ▼                  ▼
         Aggregate        External Information
                                  │
                                  ▼
                           Repository Port
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ Simple           │
                         │ Orchestration?   │
                         └────────┬─────────┘
                                  │
                           ┌──────┴──────┐
                          Yes            No
                           │              │
                           ▼              ▼
                     Application     Domain Service
                        Service
```

اصل کلیدی:

> **Aggregate مالک Invariantهای داخلی خودش است.**

> **Aggregate نباید Repository را صدا بزند.**

> **Repository فقط Data Access Capability فراهم می‌کند و Business Policy را اجرا نمی‌کند.**

> **Application Service محل پیش‌فرض Orchestration است.**

> **Domain Service فقط زمانی ایجاد می‌شود که یک Business Rule واقعی وجود داشته باشد که متعلق به یک Aggregate منفرد نباشد و منطق آن صرفاً Orchestration ساده نباشد.**

> **Repository Query به‌تنهایی تضمین Integrity در شرایط Concurrent نیست.**

---

# 27. Final Decision

سیستم از یک **Policy Placement Strategy مبتنی بر مالکیت Business Rule و دامنه اطلاعات مورد نیاز برای تصمیم‌گیری** استفاده می‌کند.

Business Rules داخلی Aggregate در خود Aggregate enforce می‌شوند.

اطلاعات خارج از Aggregate از طریق Repository Port در اختیار Application یا Domain Policy قرار می‌گیرد.

Application Service مسئول Orchestration ساده Use Case است.

Domain Service فقط برای Business Ruleهای واقعی Cross-Aggregate که متعلق به یک Aggregate منفرد نیستند و پیچیدگی Domain دارند استفاده می‌شود.

Repository صرفاً مسئول Data Access و ارائه قابلیت‌های لازم برای دسترسی به اطلاعات است و نباید Business Logic را در خود پیاده کند.

همچنین Checkهای Repository مانند `existsByCode()` به‌تنهایی تضمین‌کننده Integrity در شرایط Concurrent نیستند و Guarantee نهایی چنین Policyهایی در ADRهای Persistence/Concurrency تعیین خواهد شد.

 