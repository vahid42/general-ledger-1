# ADR-0016 — استراتژی انتزاع Repository (Repository Abstraction Strategy)

- **وضعیت:** پذیرفته شده (Accepted)
- **تاریخ:** 2026-08-22

## ADRهای مرتبط

- ADR-0004 
- ADR-0011 
- ADR-0012 
- ADR-0013 
- ADR-0018 

---

# زمینه (Context)

در معماری Domain-Driven Design و Clean Architecture، Repository یک Abstraction برای دسترسی و ذخیره‌سازی Aggregateها است.

Domain باید بتواند Aggregateهای خود را بدون وابستگی به تکنولوژی Persistence مانند:

- JPA
- Hibernate
- Spring Data
- JDBC
- SQL
- Database

بازیابی و ذخیره کند.

در مقابل، جزئیات مربوط به Persistence باید در Infrastructure باقی بماند.

بنابراین لازم است مشخص شود:

- Repository Interface کجا قرار می‌گیرد؟
- Repository برای چه چیزی تعریف می‌شود؟
- Repository برای Entity داخلی Aggregate تعریف می‌شود یا Aggregate Root؟
- Implementation کجا قرار می‌گیرد؟
- Identity Repository با چه Typeای مدل می‌شود؟
- Queryهای Domain و Queryهای Read چگونه تفکیک می‌شوند؟
- آیا Repository باید Generic باشد؟
- Transaction چه لایه‌ای را کنترل می‌کند؟
- Persistence Model چگونه از Domain Model جدا می‌شود؟

---

# تصمیم (Decision)

## 1. Repository یک Domain Abstraction است

Repository در Domain به عنوان یک Contract تعریف می‌شود و Implementation آن در Infrastructure قرار می‌گیرد.

```text
Domain
   │
   ▼
Repository Interface
   ▲
   │
Infrastructure
   │
   ▼
JPA / Hibernate / JDBC
   │
   ▼
Database
````

Domain فقط Contract مربوط به Repository را می‌شناسد.

Domain نباید Implementation مربوط به Persistence را بشناسد.

---

# 2. Repository فقط برای Aggregate Root تعریف می‌شود

بر اساس ADR-0012، Repository فقط برای Aggregate Root ایجاد می‌شود.

مثلاً:

```text
Order Aggregate
│
├── Order          ← Aggregate Root
│
├── OrderLine      ← Internal Entity
│
└── ShippingAddress ← Value Object
```

بنابراین:

```java
public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    void save(Order order);
}
```

اما Repository مستقلی برای Entityهای داخلی Aggregate ایجاد نمی‌شود:

```java
OrderLineRepository        // ❌
ShippingAddressRepository  // ❌
```

مگر اینکه آن مفهوم در آینده به یک Aggregate مستقل تبدیل شود.

---

# 3. Repository با Identity تایپ‌شده Aggregate کار می‌کند

Identity مربوط به Aggregate Root باید از Type مشخص Domain استفاده کند.

این موضوع با ADR-0018 — Domain Identity Strategy هماهنگ است.

مثلاً:

```java
public record AccountId(UUID value) {
}
```

Repository نیز باید از همین Identity استفاده کند:

```java
public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    void save(Account account);
}
```

به جای:

```java
Optional<Account> findById(UUID id);
```

استفاده از Typed Identity ترجیح داده می‌شود.

هدف این است که Type System نیز مرزهای Domain را نشان دهد.

---

# 4. Repository نباید Generic CRUD Abstraction باشد

استفاده از Generic Repository در Domain به صورت پیش‌فرض مجاز نیست.

نامناسب:

```java
public interface GenericRepository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    void delete(T entity);

    List<T> findAll();
}
```

این نوع Abstraction معمولاً:

* Business Meaning مشخصی ندارد.
* CRUD را به عنوان مدل اصلی تحمیل می‌کند.
* Aggregate Boundary را نادیده می‌گیرد.
* امکان ایجاد Repository برای Entityهای داخلی را افزایش می‌دهد.
* API مربوط به Persistence را بیش از حد عمومی می‌کند.

Repository باید بر اساس نیاز واقعی Domain طراحی شود.

---

# 5. Repository Contract باید Domain-Oriented باشد

Methodهای Repository باید نیاز Domain را بیان کنند، نه جزئیات Database را.

مثلاً:

```java
public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    Optional<Account> findActiveByCustomerId(CustomerId customerId);

    void save(Account account);
}
```

در مقابل، Contractهایی مانند:

```java
findByStatusAndDeletedFalseAndVersionGreaterThan(...)
```

باید با دقت بررسی شوند.

اگر این نام صرفاً انعکاس جزئیات Persistence باشد، Repository Contract احتمالاً بیش از حد به Persistence Model نزدیک شده است.

---

# 6. Repository مسئول Persistence است، نه Business Logic

Repository مسئول:

* Load کردن Aggregate
* Persist کردن Aggregate
* ارائه Queryهای مورد نیاز برای Domain
* حذف Aggregate در صورت وجود Business Meaning برای حذف

است.

Repository مسئول موارد زیر نیست:

* Business Rule
* Business Decision
* Business Validation
* Business Policy
* Transaction Orchestration
* HTTP
* API Validation
* Authentication
* Authorization
* Event Handling
* Message Publishing

مثلاً:

```java
public void saveIfCustomerIsEligible(Account account) {
    // ❌ Business Decision
}
```

نامناسب است.

تصمیم Eligibility باید در Domain Model یا Domain Service انجام شود.

---

# 7. Repository نباید جای Entity یا Aggregate Behavior را بگیرد

Repository نباید Business Logic مربوط به Aggregate را اجرا کند.

مثلاً:

```java
accountRepository.activate(account);
```

نامناسب است.

ترجیحاً:

```java
account.activate();

accountRepository.save(account);
```

در این مدل:

```text
Application
    │
    ▼
Repository.load()
    │
    ▼
Aggregate
    │
    ├── Business Behavior
    └── Invariant Protection
    │
    ▼
Repository.save()
```

Business Behavior در Domain باقی می‌ماند.

---

# 8. Repository Implementation در Infrastructure قرار می‌گیرد

Implementation مربوط به Repository باید در Infrastructure قرار گیرد.

مثلاً:

```text
module/
│
├── domain/
│   ├── model/
│   │   └── account/
│   │       ├── Account.java
│   │       └── AccountId.java
│   │
│   └── repository/
│       └── AccountRepository.java
│
├── application/
│
└── infrastructure/
    └── persistence/
        ├── AccountRepositoryImpl.java
        ├── AccountJpaEntity.java
        └── SpringDataAccountRepository.java
```

Domain فقط:

```java
AccountRepository
```

را می‌شناسد.

Infrastructure جزئیات Persistence را پیاده‌سازی می‌کند.

---

# 9. Domain Repository نباید به JPA وابسته باشد

Domain Repository نباید موارد زیر را استفاده کند:

```java
JpaRepository
Page
Pageable
Specification
EntityManager
CriteriaQuery
Predicate
Query
EntityManagerFactory
```

مثلاً این طراحی نامناسب است:

```java
public interface AccountRepository
        extends JpaRepository<AccountEntity, UUID> {
}
```

زیرا:

```text
Domain
   ↓
Spring Data / JPA
```

را ایجاد می‌کند.

در حالی که جهت صحیح وابستگی:

```text
Domain
   ▲
   │
Infrastructure
   │
   ▼
JPA / Hibernate
```

است.

---

# 10. Persistence Model از Domain Model جدا است

Domain Entity و Persistence Entity یک مفهوم یکسان نیستند.

مثلاً:

```text
Domain

Account
AccountId
Money
AccountStatus
```

در مقابل:

```text
Infrastructure

AccountJpaEntity
AccountIdEmbeddable
AccountBalanceEntity
...
```

Repository Implementation وظیفه Mapping بین این دو مدل را بر عهده دارد.

```text
Database
   │
   ▼
JPA Entity
   │
   ▼
Repository Implementation
   │
   ▼
Domain Aggregate
```

و هنگام ذخیره:

```text
Domain Aggregate
   │
   ▼
Repository Implementation
   │
   ▼
JPA Entity
   │
   ▼
Database
```

Domain نباید برای راحتی ORM تغییر کند.

---

# 11. Repository باید Aggregate معتبر برگرداند

Repository هنگام Load کردن Aggregate باید آن را در یک State معتبر در اختیار Domain قرار دهد.

```text
Database
   │
   ▼
Repository
   │
   ▼
Valid Aggregate
```

نباید Aggregate به صورت ناقص یا Invalid وارد Domain شود:

```text
Database
   │
   ▼
Repository
   │
   ▼
Partial / Invalid Aggregate  ❌
```

اگر Persistence Model دارای داده‌ای باشد که برای ساخت Aggregate معتبر کافی نیست، Repository Implementation باید مسئله Mapping و Reconstruction را مدیریت کند.

---

# 12. Aggregate باید به صورت یک واحد Persistence مدیریت شود

Repository باید Aggregate Root را به عنوان واحد اصلی Persistence در نظر بگیرد.

مثلاً:

```text
Order Aggregate
│
├── Order
├── OrderLine
├── OrderLine
└── ShippingAddress
```

Application نباید:

```text
OrderRepository
OrderLineRepository
ShippingAddressRepository
```

را برای تغییر اجزای داخلی Aggregate استفاده کند.

بلکه:

```java
Order order = orderRepository.findById(orderId);

order.changeLineQuantity(lineId, quantity);

orderRepository.save(order);
```

استفاده می‌شود.

---

# 13. Repository و Transaction Boundary

Repository مسئول تعیین Transaction Boundary نیست.

Transaction توسط Application Layer و Use Case مدیریت می‌شود.

مثلاً:

```text
Application Use Case
       │
       ▼
Transaction Boundary
       │
       ├── Repository.load()
       │
       ├── Domain Behavior
       │
       └── Repository.save()
```

Repository در Transaction موجود فعالیت می‌کند.

در نتیجه:

```java
@Transactional
```

نباید بخشی از Domain Repository Contract باشد.

جزئیات Transaction Management در Infrastructure/Application مدیریت می‌شود.

---

# 14. Repository و Aggregate Consistency

Repository باید با Aggregate Boundary تعریف‌شده در ADR-0012 هماهنگ باشد.

اگر یک Aggregate دارای Invariantهایی است که باید همیشه با هم حفظ شوند، Repository باید Aggregate را به عنوان یک واحد معتبر Load و Persist کند.

Repository نباید Aggregate Boundary را دور بزند.

مثلاً:

```text
Account Aggregate
│
├── Account
├── Balance
└── AccountStatus
```

نباید Persistence API به گونه‌ای طراحی شود که خارج از Aggregate بتوان:

```text
Balance
AccountStatus
```

را مستقل تغییر داد.

---

# 15. Queryهای Domain با Read Queryها یکی نیستند

تمام Queryهای سیستم الزاماً نباید از Domain Repository عبور کنند.

اگر Query برای اجرای Business Logic و تصمیم Domain لازم است، می‌تواند بخشی از Domain Repository Contract باشد.

مثلاً:

```java
Optional<Account> findById(AccountId id);

Optional<Account> findActiveByCustomerId(CustomerId customerId);
```

اما اگر Query صرفاً برای نمایش اطلاعات است:

```text
AccountSummary
CustomerDashboard
AccountReport
AccountSearchResult
```

لزومی ندارد Domain Aggregate را Load کنیم.

در چنین مواردی می‌توان از Query Model یا Read Model استفاده کرد.

```text
Application Query
       │
       ▼
Query Repository
       │
       ▼
Projection / Read Model
       │
       ▼
Database
```

این Query Repository الزاماً Domain Repository نیست.

---

# 16. Command و Query می‌توانند Contract جدا داشته باشند

برای Write/Domain Operations:

```java
public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    void save(Account account);
}
```

و برای Readهای پیچیده:

```java
public interface AccountQueryRepository {

    AccountSummary findSummary(AccountId id);

    List<AccountSummary> search(AccountSearchCriteria criteria);
}
```

این تفکیک از Load کردن Aggregate برای Queryهایی که صرفاً جنبه Read دارند جلوگیری می‌کند.

---

# 17. Query Repository نباید Domain Aggregate را تحمیل کند

اگر هدف یک Query فقط نمایش یا گزارش است، Query Repository نباید مجبور به بازسازی Aggregate شود.

نامناسب:

```text
Database
   │
   ▼
Account Aggregate
   │
   ▼
AccountSummary
```

در حالی که می‌توان مستقیماً:

```text
Database
   │
   ▼
Projection
   │
   ▼
AccountSummary
```

را تولید کرد.

این موضوع مخصوصاً برای Queryهای:

* Reporting
* Search
* Dashboard
* List
* Pagination
* Aggregation

مناسب است.

---

# 18. Pagination Concern مربوط به Query است

Pagination معمولاً Concern مربوط به Read Model است و نباید APIهای Framework مانند:

```java
Page<T>
Pageable
Slice<T>
```

را وارد Domain Repository کند.

برای Queryهای Application می‌توان Abstraction مناسب تعریف کرد.

مثلاً:

```java
public interface AccountQueryRepository {

    PageResult<AccountSummary> search(
            AccountSearchCriteria criteria,
            PageRequest pageRequest
    );
}
```

نوع دقیق این Abstraction باید در Application/Query Layer تعریف شود، نه بر اساس API مستقیم Spring Data در Domain.

---

# 19. Repository و Concurrency

Repository باید در صورت وجود نیاز Business، امکان استفاده از مکانیزم‌های Concurrency Control را فراهم کند.

روش‌های رایج:

* Optimistic Locking
* Pessimistic Locking

اما جزئیات تکنولوژی Locking نباید وارد Domain Repository Contract شود.

مثلاً Domain نباید بداند که:

```java
@Version
```

در Persistence Model وجود دارد.

Domain فقط باید رفتار مورد نیاز خود را بیان کند.

---

# 20. Repository و حذف Aggregate

حذف Aggregate نباید صرفاً به دلیل وجود متد CRUD مانند:

```java
delete(...)
```

انجام شود.

ابتدا باید مشخص شود که حذف واقعاً یک Business Operation معتبر است یا خیر.

در بسیاری از Domainها به جای حذف فیزیکی ممکن است:

```text
Close
Deactivate
Archive
Cancel
```

Business Behavior مناسب‌تری باشد.

بنابراین:

```java
void delete(Account account);
```

فقط زمانی در Repository قرار می‌گیرد که حذف واقعی بخشی از Domain Requirement باشد.

---

# 21. Repository و External Systems

Repository برای Persistence Aggregate است.

External System نباید صرفاً با نام Repository مدل شود.

مثلاً:

```text
PaymentRepository
```

برای ذخیره Payment مناسب است.

اما ارتباط با درگاه پرداخت:

```text
PaymentGateway
```

یا:

```text
PaymentProvider
```

است.

به همین ترتیب:

```text
ExchangeRateProvider
CreditScoreProvider
NotificationSender
```

نباید صرفاً Repository نامیده شوند.

نوع Abstraction باید نقش واقعی آن Dependency در Domain را نشان دهد.

---

# 22. Repository نباید Transaction Manager باشد

Repository نباید مسئول:

```java
@Transactional
```

یا مدیریت Transaction باشد.

Transaction Boundary توسط Application Use Case تعیین می‌شود.

مثلاً:

```text
Application Service
       │
       ▼
Transaction
       │
       ├── Load Aggregate
       ├── Execute Domain Behavior
       └── Save Aggregate
```

Repository صرفاً در این Boundary فعالیت می‌کند.

---

# 23. Repository و Domain Event

Repository مسئول Publish کردن Domain Event نیست.

Aggregate ممکن است Domain Event تولید کند:

```text
Aggregate
    │
    ▼
Domain Event
```

اما Repository نباید:

```text
save()
   │
   └──► Kafka.publish()  ❌
```

را انجام دهد.

انتشار Event باید مطابق ADR-0015 انجام شود.

برای Integration Eventهای حیاتی، Transactional Outbox می‌تواند مورد استفاده قرار گیرد.

```text
Application Transaction
       │
       ├──► Aggregate State
       │
       └──► Outbox
              │
              ▼
         Outbox Publisher
```

---

# 24. Repository در Modular Monolith

در Modular Monolith، هر Module مالک Domain Model و Repository Contract مربوط به Aggregateهای خودش است.

مثلاً:

```text
account/
├── domain/
│   ├── model/
│   └── repository/
│       └── AccountRepository
│
└── infrastructure/
    └── persistence/
```

Module دیگر نباید Repository داخلی این Module را برای دسترسی مستقیم به داده‌های آن استفاده کند.

ارتباط بین Moduleها باید از طریق Contractهای تعریف‌شده در Application/Domain یا Domain Event/Integration Mechanism مناسب انجام شود.

---

# 25. Repository Contract نباید برای Database طراحی شود

Repository باید ابتدا بر اساس Domain و Use Case طراحی شود، نه بر اساس جدول Database.

نامناسب:

```java
findByTableName(...)

findByColumn(...)

findByDeletedFlag(...)

findByDatabaseStatus(...)

```

مناسب:

```java
findById(AccountId id);

findActiveByCustomerId(CustomerId customerId);
```

Database Schema باید Implementation Detail باقی بماند.

---

# 26. Repository Testing

Repository Implementation باید با Integration Test بررسی شود.

موارد مهم:

* Mapping
* Persistence
* Query
* Aggregate Reconstruction
* Transaction Behavior
* Concurrency
* Constraint Handling

در صورت امکان استفاده از:

```text
Testcontainers
```

برای تست Persistence واقعی ترجیح داده می‌شود.

اما Domain Test نباید برای تست Business Ruleهای Aggregate به Database واقعی نیاز داشته باشد.

---

# 27. محل Repository Interface

Repository Interface باید در Domain Module و نزدیک به Aggregateای قرار گیرد که مالک آن است.

مثلاً:

```text
account/
└── domain/
    ├── model/
    │   └── Account.java
    │
    └── repository/
        └── AccountRepository.java
```

یا در ساختاری که Repository Contract را در کنار Aggregate قرار می‌دهد:

```text
account/
└── domain/
    └── account/
        ├── Account.java
        ├── AccountId.java
        └── AccountRepository.java
```

انتخاب دقیق Package Structure می‌تواند بر اساس ساختار Module انجام شود، اما اصل مهم این است:

> Repository Contract متعلق به Domain است و نباید به Infrastructure وابسته باشد.

---

# 28. تصمیم درباره Generic Repository

استفاده از Generic Repository در Domain به صورت پیش‌فرض **ممنوع** است.

دلایل:

1. Business Meaning مشخصی ندارد.
2. CRUD را به عنوان Abstraction اصلی تحمیل می‌کند.
3. ممکن است Aggregate Boundary را نادیده بگیرد.
4. احتمال ایجاد Repository برای Entityهای داخلی را افزایش می‌دهد.
5. Contract را از Domain به Persistence نزدیک می‌کند.
6. معمولاً باعث ایجاد APIهایی می‌شود که برای همه Aggregateها مناسب نیستند.

بنابراین:

```java
GenericRepository<T, ID>
```

به عنوان Repository استاندارد Domain استفاده نمی‌شود.

اگر در آینده یک Abstraction عمومی واقعاً دارای Business Meaning باشد، باید در ADR مستقل بررسی شود.

---

# قوانین (Rules)

* Repository Contract در Domain تعریف می‌شود.
* Repository Implementation در Infrastructure قرار می‌گیرد.
* Repository فقط برای Aggregate Root تعریف می‌شود.
* Repository از Typed Identity مربوط به Aggregate استفاده می‌کند.
* Repository نباید Generic CRUD Abstraction باشد.
* Repository نباید Business Logic داشته باشد.
* Repository نباید Business Decision بگیرد.
* Repository نباید Transaction Boundary را تعیین کند.
* Repository نباید Domain Event را Publish کند.
* Domain Repository نباید به JPA/Hibernate/Spring Data وابسته باشد.
* Domain Repository نباید `JpaRepository` را Extend کند.
* Persistence Model باید از Domain Model جدا باشد.
* Repository باید Aggregate را در State معتبر Load کند.
* Entityهای داخلی Aggregate نباید Repository مستقل داشته باشند.
* Query Repository و Domain Repository می‌توانند Contractهای جداگانه داشته باشند.
* Read Queryهای پیچیده نباید الزاماً Aggregate را Load کنند.
* APIهای Persistence مانند `Page` و `Pageable` نباید وارد Domain Repository شوند.
* حذف Aggregate فقط در صورت وجود Business Meaning انجام می‌شود.
* External Service نباید صرفاً به دلیل Persistence بودن با نام Repository مدل شود.
* Repository باید Domain-oriented باشد.
* Repository باید با Aggregate Boundary تعریف‌شده در ADR-0012 هماهنگ باشد.
* Domain نباید از جزئیات Persistence اطلاع داشته باشد.

---

# Repository Decision Checklist

قبل از ایجاد Repository باید موارد زیر بررسی شوند:

```text
آیا این مفهوم Aggregate Root است؟
        │
       No ──► Repository ایجاد نکن
        │
       Yes
        │
        ▼
آیا Persistence مستقل برای آن لازم است؟
        │
       No ──► Repository ایجاد نکن
        │
       Yes
        │
        ▼
آیا Contract دارای Business Meaning است؟
        │
       No ──► Generic CRUD ایجاد نکن
        │
       Yes
        │
        ▼
آیا Query صرفاً برای Read/Report است؟
        │
       Yes ──► Query Repository / Read Model
        │
       No
        │
        ▼
Domain Repository
```

---

# پیامدها (Consequences)

## مزایا

* استقلال Domain از Persistence Technology
* حفظ مرز Clean Architecture
* حفظ Aggregate Boundary
* Repositoryهای معنادار و Domain-oriented
* جلوگیری از Generic CRUD Abstraction
* کاهش Coupling با JPA/Hibernate
* تست‌پذیری بهتر Domain
* امکان تغییر Persistence Technology
* امکان استفاده از Read Model مستقل
* جلوگیری از Repository برای Entityهای داخلی
* استفاده از Typed Identity در Persistence Contract
* کنترل بهتر Transaction Boundary

## معایب

* نیاز به Mapping بین Domain و Persistence Model
* افزایش تعداد Interfaceها و Implementationها
* کدنویسی بیشتر نسبت به استفاده مستقیم از Spring Data
* نیاز به طراحی دقیق Repository Contract
* پیچیدگی بیشتر در Queryهای پیچیده
* احتمال ایجاد Abstractionهای اضافی در صورت طراحی نادرست
* نیاز به Integration Test برای Persistence Layer

---

# وضعیت اجرا (Implementation Status)

این استراتژی برای تمام Moduleهای Domain اعمال می‌شود.

برای هر Aggregate:

```text
Module
│
├── domain/
│   ├── model/
│   │   ├── AggregateRoot
│   │   ├── Entity
│   │   └── ValueObject
│   │
│   └── repository/
│       └── AggregateRepository
│
├── application/
│   └── usecase/
│
└── infrastructure/
    └── persistence/
        ├── AggregateRepositoryImpl
        ├── JpaEntity
        └── SpringDataRepository
```

Domain فقط Contract را می‌شناسد:

```text
Domain
   │
   ▼
Repository Interface
```

و Infrastructure جزئیات Persistence را پیاده‌سازی می‌کند:

```text
Infrastructure
   │
   ├── Repository Implementation
   ├── JPA Entity
   ├── Spring Data
   └── Database
```

Queryهای Read-heavy، Reporting و Projection در صورت نیاز از Domain Repository جدا شده و با Query Model مستقل پیاده‌سازی می‌شوند.

---

# جمع‌بندی تصمیم

مدل نهایی Repository در پروژه به صورت زیر است:

```text
                    Application
                         │
                         ▼
                  Use Case / Transaction
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
       Domain Repository      Query Repository
              │                     │
              │                     │
              ▼                     ▼
      Aggregate Root          Read Model / DTO
              │                     │
              └──────────┬──────────┘
                         │
                         ▼
                   Infrastructure
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
          JPA/Hibernate        Query Engine
              │                     │
              └──────────┬──────────┘
                         ▼
                      Database
```

اصل کلیدی:

> **Repository برای Aggregate Root است، Contract آن در Domain قرار دارد، Implementation آن در Infrastructure است و Repository نباید تبدیل به Generic CRUD یا محل Business Logic شود.**

---

# References

1. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**
2. Vaughn Vernon — **Implementing Domain-Driven Design**
3. Vaughn Vernon — **Domain-Driven Design Distilled**
4. Martin Fowler — **Patterns of Enterprise Application Architecture — Repository**
5. Martin Fowler — **Domain Model**
6. Robert C. Martin — **Clean Architecture: A Craftsman's Guide to Software Structure and Design**
7. Chris Richardson — **Microservices Patterns**
8. Spring Data JPA — **Reference Documentation**
9. Hibernate ORM — **Documentation**

```
```