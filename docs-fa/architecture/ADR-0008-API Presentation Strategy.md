# ADR-0008 — استراتژی API و لایه Presentation

* **وضعیت:** Accepted
* **تاریخ:** 2026-08-22
* **ADRهای مرتبط:**

  * ADR-0003 
  * ADR-0004 
  * ADR-0005 
  * ADR-0006 

---

# 1. مسئله و زمینه (Context)

General Ledger به‌عنوان یک **Bounded Context** مستقل، از سه Business Module اصلی تشکیل می‌شود:

```text
General Ledger Bounded Context
│
├── Account Head Module
├── Account Module
└── Journal Entry Module ⭐ Core
```

این Bounded Context به‌صورت یک **General Ledger Microservice** پیاده‌سازی می‌شود.

مطابق ADR-0006، Domain Model سیستم Rich Domain Model است و Business Ruleها باید در Domain باقی بمانند.

بنابراین لازم است مرز مشخصی بین:

```text
External World
      │
      ▼
Presentation
      │
      ▼
Application
      │
      ▼
Domain
```

وجود داشته باشد.

Presentation نباید به محل اجرای Business Logic تبدیل شود.

در صورت قرار گرفتن Business Logic در Controller یا API Adapter، مشکلات زیر ایجاد می‌شود:

* وابستگی Business Logic به HTTP
* افزایش حجم Controllerها
* دشوار شدن تست Domain
* تکرار Business Logic در APIهای مختلف
* وابستگی Domain به REST یا Framework
* Coupling بین API Contract و Domain Model
* دشوار شدن Evolution API
* از بین رفتن مرزهای Domain و Application

بنابراین Presentation باید صرفاً نقش Adapter بین دنیای خارجی و Application Layer را داشته باشد.

---

# 2. تصمیم (Decision)

سیستم از یک **Presentation Layer مبتنی بر API Adapter** استفاده خواهد کرد.

Presentation مسئول موارد زیر است:

* دریافت Request
* تبدیل External Contract به Application Input
* Input Validation
* دریافت Authentication Context
* اعمال Authorization در سطح Interface
* فراخوانی Application Use Case
* تبدیل Application Result به Response DTO
* تعیین HTTP Status
* مدیریت Errorهای API
* مستندسازی API
* مدیریت Concernهای مرتبط با Protocol

Presentation مسئول Business Logic نیست.

مدل کلی:

```text
External Client
      │
      ▼
Presentation / API Adapter
      │
      ▼
Request DTO
      │
      ▼
Application Use Case
      │
      ▼
Domain Model
      │
      ▼
Application Result
      │
      ▼
Response DTO
      │
      ▼
Presentation / API Adapter
      │
      ▼
External Client
```

---

# 3. API به‌عنوان Adapter

API در معماری سیستم به‌عنوان یک Adapter در مرز بیرونی سیستم در نظر گرفته می‌شود.

```text
                 External World
                       │
                       ▼
              ┌─────────────────┐
              │   REST API      │
              │     Adapter     │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │   Application   │
              └────────┬────────┘
                       │
                       ▼
                 Domain Model
```

API نباید مستقیماً Domain Model را کنترل یا Business Ruleهای آن را اجرا کند.

---

# 4. REST API

برای APIهای HTTP، سبک اصلی ارتباط:

> **RESTful HTTP API**

خواهد بود.

Endpointها باید تا حد امکان بر اساس Resource و Business Capability طراحی شوند.

نمونه:

```text
GET    /api/v1/accounts
GET    /api/v1/accounts/{accountId}
POST   /api/v1/accounts
PATCH  /api/v1/accounts/{accountId}
```

در مواردی که یک Business Operation مشخص وجود دارد، Action Endpoint در صورت نیاز مجاز است:

```text
POST /api/v1/accounts/{accountId}/close
```

یا برای عملیات Core Domain:

```text
POST /api/v1/journal-entries
POST /api/v1/journal-entries/{journalEntryId}/post
```

این Endpointها صرفاً Contractهای Presentation هستند و Business Behavior همچنان در Domain Model قرار دارد.

---

# 5. API Versioning

APIهای عمومی باید قابلیت Versioning داشته باشند.

در نسخه اولیه، Version در URI استفاده می‌شود:

```text
/api/v1/...
```

برای مثال:

```text
/api/v1/accounts
/api/v1/journal-entries
```

در صورت ایجاد Breaking Change، نسخه جدید API ایجاد خواهد شد:

```text
/api/v2/...
```

Versioning مربوط به API Contract است و نباید باعث Versioning مستقل Domain Model شود.

---

# 6. Request DTO

Requestهای API باید با DTOهای مخصوص Presentation دریافت شوند.

مثال:

```java
public record CreateAccountRequest(
        String name,
        String currency
) {
}
```

این DTO بخشی از Domain Model نیست.

مسیر ترجیحی:

```text
CreateAccountRequest
        │
        ▼
Application Input / Command
        │
        ▼
Domain Model
```

و نه:

```text
CreateAccountRequest
        │
        ▼
Account Entity
```

Presentation Contract نباید مستقیماً ساختار داخلی Domain را expose کند.

---

# 7. Response DTO

Response نیز باید DTO مخصوص Presentation داشته باشد.

مثال:

```java
public record AccountResponse(
        String id,
        String name,
        String currency,
        String status
) {
}
```

Domain Entity نباید مستقیماً از Controller به Consumer خارجی بازگردانده شود.

نامطلوب:

```java
@GetMapping
public Account getAccount(...) {
    ...
}
```

ترجیحی:

```java
@GetMapping
public AccountResponse getAccount(...) {
    ...
}
```

---

# 8. Domain Model نباید API Contract باشد

Domain Entity نباید به‌عنوان API Contract استفاده شود.

در صورت استفاده مستقیم از Domain Model در API، تغییر Domain می‌تواند باعث Breaking Change در API شود.

مدل صحیح:

```text
External API Contract
        │
        ▼
Presentation DTO
        │
        ▼
Application Input / Result
        │
        ▼
Domain Model
```

نه:

```text
External API Contract
        │
        ▼
Domain Entity
```

API Contract باید بتواند مستقل از Evolution داخلی Domain تغییر کند.

---

# 9. Controller Responsibility

Controller باید تا حد امکان Thin باشد.

وظایف Controller:

```text
Receive Request
      │
      ▼
Validate Input
      │
      ▼
Map Input
      │
      ▼
Call Use Case
      │
      ▼
Map Result
      │
      ▼
Return Response
```

Controller نباید شامل موارد زیر باشد:

* Business Rule
* Business Calculation
* Domain Decision
* Persistence Logic
* SQL
* Repository Access
* Transaction Orchestration
* دسترسی مستقیم به Entityهای داخلی Moduleهای دیگر

---

# 10. Controller و Application Layer

Controller باید Application Use Case را فراخوانی کند.

مثال:

```text
CreateAccountController
          │
          ▼
CreateAccountUseCase
          │
          ▼
Account Aggregate
```

یا:

```text
PostJournalEntryController
          │
          ▼
PostJournalEntryUseCase
          │
          ▼
JournalEntry Aggregate ⭐ Core
```

مدل نامطلوب:

```text
Controller
    │
    ├── Repository
    ├── Business Rule
    ├── Database
    └── Domain Logic
```

Application Layer مرز ورود به Use Caseهای سیستم است.

---

# 11. Input Validation

Validation مربوط به شکل و ساختار اولیه Request در Presentation انجام می‌شود.

برای مثال:

```java
@NotBlank
String name
```

یا:

```java
@NotNull
Currency currency
```

این Validation مربوط به:

```text
Required Field
Format
Length
Syntax
Type
```

است.

اما Business Invariant باید در Domain enforce شود.

مثال:

```text
Presentation Validation
        │
        ▼
"نام خالی نباشد"
```

در مقابل:

```text
Domain Invariant
        │
        ▼
"Account بسته‌شده قابل فعال‌سازی نیست"
```

---

# 12. Validation در چند سطح

Validation به دو دسته اصلی تقسیم می‌شود.

## 12.1 Input Validation

در Presentation/Application Boundary:

```text
Required Field
Format
Length
Syntax
Type
Basic Structural Validation
```

## 12.2 Business Validation

در Domain:

```text
Business Rule
Invariant
State Transition
Consistency
Posting Rule
Business Decision
```

اصل:

```text
Input Validation
       │
       ▼
Presentation / Application Boundary
```

و:

```text
Business Invariant
       │
       ▼
Domain
```

Controller نباید مالک Business Invariant باشد.

---

# 13. Error Handling

خطاهای API باید دارای ساختار استاندارد و قابل استفاده برای Consumer باشند.

ساختار پیشنهادی:

```json
{
  "code": "ACCOUNT_NOT_FOUND",
  "message": "Account was not found",
  "traceId": "..."
}
```

در صورت نیاز می‌توان اطلاعات بیشتری اضافه کرد:

```text
details
fieldErrors
timestamp
path
```

ساختار نهایی Error Response باید به‌عنوان بخشی از API Contract مدیریت شود.

---

# 14. HTTP Status Codes

از HTTP Status Code بر اساس معنای واقعی Response استفاده خواهد شد.

نمونه:

```text
200 OK
201 Created
202 Accepted
204 No Content

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity

500 Internal Server Error
```

انتخاب Status Code باید Consistent و قابل پیش‌بینی باشد.

---

# 15. Business Error و Technical Error

خطاها باید از یکدیگر تفکیک شوند.

## Business Error

نمونه:

```text
ACCOUNT_ALREADY_CLOSED
INVALID_ACCOUNT_STATE
JOURNAL_ENTRY_NOT_BALANCED
POSTING_RULE_VIOLATION
```

این خطاها از Business Ruleهای Domain ناشی می‌شوند.

## Technical Error

نمونه:

```text
PERSISTENCE_UNAVAILABLE
EXTERNAL_SERVICE_TIMEOUT
MESSAGE_BROKER_UNAVAILABLE
```

Presentation مسئول تبدیل این خطاها به API Response مناسب است.

منبع Business Error باید Domain باشد، اما نحوه نمایش آن به Consumer مسئولیت Presentation است.

---

# 16. Global Exception Handling

مدیریت Exceptionهای API باید به‌صورت متمرکز انجام شود.

در Spring Boot می‌توان از:

```text
@ControllerAdvice
```

یا سازوکار معادل Framework استفاده کرد.

مدل مفهومی:

```text
Controller
    │
    ▼
Application / Domain Exception
    │
    ▼
Global Exception Handler
    │
    ▼
Standard Error Response
```

Controller نباید در هر Endpoint منطق Error Handling تکراری داشته باشد.

---

# 17. API Contract

API Contract مستقل از Implementation داخلی خواهد بود.

Contract شامل مواردی مانند:

* Endpoint
* HTTP Method
* Request Schema
* Response Schema
* Status Code
* Error Schema
* Authentication Requirements
* Authorization Requirements
* Version

است.

تغییر در Domain یا Application Implementation نباید بدون دلیل Contract API را تغییر دهد.

---

# 18. OpenAPI

APIهای عمومی باید با **OpenAPI** مستند شوند.

Documentation باید در صورت مرتبط بودن شامل موارد زیر باشد:

* Endpoint
* HTTP Method
* Request
* Response
* Validation
* Error
* Authentication
* Authorization
* Examples
* Version

OpenAPI باید نمایانگر Contract واقعی API باشد.

---

# 19. Authentication

Authentication یک Concern مربوط به Presentation و Security Infrastructure است.

Presentation باید بتواند هویت درخواست‌کننده را دریافت کرده و Context موردنیاز را به Application منتقل کند.

Domain نباید مستقیماً به موارد زیر وابسته شود:

```text
HTTP Request
JWT
SecurityContext
Spring Security
Servlet API
```

مدل:

```text
External Authentication
        │
        ▼
Presentation / Security Adapter
        │
        ▼
Application Context
        │
        ▼
Domain
```

---

# 20. Authorization

Authorization می‌تواند در چند سطح انجام شود.

## 20.1 Interface-Level Authorization

برای مثال:

```text
ROLE_ACCOUNT_MANAGER
ROLE_ADMIN
```

این سطح می‌تواند در Presentation/Security Layer enforce شود.

## 20.2 Business-Level Authorization

اگر Authorization بخشی از Business Rule باشد، نباید صرفاً به Controller Annotation محدود شود.

برای مثال:

```text
Only authorized actor can post this JournalEntry
```

در چنین شرایطی Rule باید در Application/Domain Boundary مناسب مدل شود.

اصل:

> Authorization تکنیکی و Authorization مبتنی بر Business Rule نباید با یکدیگر اشتباه گرفته شوند.

---

# 21. Pagination

Endpointهایی که مجموعه‌ای از Resourceها را برمی‌گردانند، در صورت نیاز باید از Pagination پشتیبانی کنند.

مثال:

```text
GET /api/v1/accounts?page=0&size=20
```

Response می‌تواند شامل موارد زیر باشد:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

Pagination یک Concern مربوط به API/Application Query است و نباید به Domain Model منتقل شود.

---

# 22. Filtering و Sorting

Filtering و Sorting باید از API Contract به مدل مناسب Application تبدیل شوند.

مثال:

```text
GET /api/v1/accounts?status=ACTIVE&sort=name
```

مسیر ترجیحی:

```text
HTTP Query
    │
    ▼
Query DTO
    │
    ▼
Application Query
    │
    ▼
Query Port / Adapter
    │
    ▼
Persistence
```

Query String نباید بدون کنترل و Mapping مستقیماً به Database Query تبدیل شود.

---

# 23. Idempotency

برای Use Caseهای حساس مالی که احتمال Retry یا ارسال تکراری آنها وجود دارد، Idempotency باید در صورت نیاز پشتیبانی شود.

مثال:

```text
POST /api/v1/journal-entries
Idempotency-Key: ...
```

هدف:

```text
Retry
  │
  ▼
Duplicate Request
  │
  X
  ▼
Duplicate Business Operation
```

این قابلیت برای تمام Endpointها اجباری نیست و باید بر اساس ماهیت Use Case تصمیم‌گیری شود.

جزئیات Persistence یا Storage مربوط به Idempotency در ADRهای اختصاصی Persistence/Infrastructure تعیین خواهد شد.

---

# 24. API و Transaction

Controller نباید مسئول مدیریت مستقیم Transaction باشد.

مرز Transaction باید با Use Case و Business Consistency موردنظر هماهنگ شود.

مدل ترجیحی:

```text
Controller
    │
    ▼
Application Use Case
    │
    ▼
Transaction Boundary
    │
    ▼
Domain
```

نه:

```text
Controller
    │
    ▼
Business Logic
```

در این معماری Application Layer مسئول هماهنگی Transaction Boundary است.

جزئیات تکنولوژی Transaction در Infrastructure/Configuration تعیین خواهد شد.

---

# 25. API و Async Operations

برای عملیات طولانی، API نباید الزاماً تا پایان پردازش منتظر بماند.

در صورت نیاز:

```text
POST /api/v1/operations
        │
        ▼
202 Accepted
        │
        ▼
Operation ID
```

و وضعیت عملیات می‌تواند بعداً قابل پیگیری باشد:

```text
GET /api/v1/operations/{operationId}
```

این الگو برای عملیات Batch یا پردازش‌های طولانی قابل استفاده است.

---

# 26. API Contract Evolution

تغییرات API به دو دسته اصلی تقسیم می‌شوند.

## Non-Breaking Changes

مانند:

* اضافه کردن Optional Field
* اضافه کردن Endpoint جدید
* اضافه کردن Error Detail غیرالزامی
* اضافه کردن Capability جدید بدون تغییر Contract قبلی

## Breaking Changes

مانند:

* حذف Field
* تغییر Type
* تغییر Semantic یک Field
* حذف Endpoint
* تغییر رفتار Contract به‌گونه‌ای که Consumer قبلی را مختل کند

Breaking Change باید با Version جدید یا Strategy سازگار برای Migration مدیریت شود.

---

# 27. API و Module Boundary

Presentation باید با Business Moduleهای موجود در General Ledger هم‌راستا باشد.

ساختار فعلی:

```text
General Ledger Bounded Context
│
├── Account Head Module
│
├── Account Module
│
└── Journal Entry Module ⭐ Core
```

APIها نیز باید تا حد امکان این Business Boundaryها را منعکس کنند.

برای مثال:

```text
Account Module
    │
    └── /api/v1/accounts
```

```text
Account Head Module
    │
    └── /api/v1/account-heads
```

```text
Journal Entry Module
    │
    └── /api/v1/journal-entries
```

این به معنی آن نیست که URL الزاماً باید دقیقاً نام Module داخلی را منعکس کند؛ اصل مهم حفظ Business Ownership و جلوگیری از Controllerهای بدون Boundary مشخص است.

---

# 28. Journal Entry API و Core Domain

از آنجا که `JournalEntry` Core Aggregate سیستم است، API مربوط به آن نباید باعث انتقال Business Logic به Presentation شود.

مدل صحیح:

```text
POST /api/v1/journal-entries
        │
        ▼
CreateJournalEntryUseCase
        │
        ▼
JournalEntry Aggregate ⭐ Core
        │
        ├── validate lines
        ├── validate debit / credit
        ├── validate balancing
        └── enforce posting rules
```

Controller فقط Contract را دریافت و Use Case را فراخوانی می‌کند.

نامطلوب:

```text
JournalEntryController
        │
        ├── calculate debit / credit
        ├── validate balancing
        ├── execute posting rules
        └── manipulate persistence
```

Core Domain باید در Domain باقی بماند.

---

# 29. Account و Account Head API

Presentation باید Business Capabilityهای Account و Account Head را از طریق Application Use Caseهای مربوطه expose کند.

مثال:

```text
CreateAccountController
        │
        ▼
CreateAccountUseCase
        │
        ├── AccountHead Capability / Port
        │
        ├── Account Domain
        │
        └── AccountRepository
```

در این مدل:

```text
Presentation
    =
API Adapter
```

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
Persistence Abstraction
```

این ساختار با ADR-0006 سازگار است.

---

# 30. Internal Module Contract و External API

دو نوع Contract باید از یکدیگر تفکیک شوند.

## 30.1 External API

برای مصرف‌کنندگان خارج از General Ledger:

```text
REST
OpenAPI
Versioning
Authentication
Authorization
Error Contract
```

## 30.2 Internal Module Contract

برای ارتباط بین Business Moduleهای داخل General Ledger:

```text
Application Contract
Internal Port
Domain Event
Capability Contract
```

این دو Contract الزاماً مدل مشترک ندارند.

مثال:

```text
External API
    │
    ▼
CreateAccountRequest
```

در حالی که ارتباط داخلی ممکن است:

```text
Account Application
    │
    ▼
AccountHeadPort
```

باشد.

---

# 31. API Naming

نام‌گذاری API باید:

* Consistent
* Predictable
* Resource-oriented
* Versioned
* مستقل از Implementation داخلی

باشد.

نمونه:

```text
/api/v1/accounts
/api/v1/accounts/{accountId}

/api/v1/account-heads
/api/v1/account-heads/{accountHeadId}

/api/v1/journal-entries
/api/v1/journal-entries/{journalEntryId}
```

برای Business Operationهای مشخص، Action Endpoint در صورت نیاز مجاز است:

```text
POST /api/v1/accounts/{accountId}/close
POST /api/v1/journal-entries/{journalEntryId}/post
```

---

# 32. API Security

API باید حداقل ملاحظات زیر را رعایت کند:

* Authentication
* Authorization
* Input Validation
* Secure Headers
* Rate Limiting در صورت نیاز
* عدم افشای اطلاعات حساس
* عدم بازگرداندن Stack Trace
* Audit Logging برای عملیات حساس در صورت نیاز

جزئیات Security در ADRهای اختصاصی Security تعریف خواهد شد.

---

# 33. Current Persistence State

در وضعیت فعلی، General Ledger از **In-Memory Persistence** استفاده می‌کند.

بنابراین این ADR هیچ تصمیمی درباره انتخاب:

```text
JPA
Hibernate
SQL Database
Redis
MongoDB
```

اتخاذ نمی‌کند.

Presentation نباید به دلیل In-Memory بودن Persistence فعلی، مستقیماً به ساختار In-Memory دسترسی داشته باشد.

مدل همچنان:

```text
Presentation
      │
      ▼
Application
      │
      ▼
Repository Port
      │
      ▼
Current In-Memory Implementation
```

است.

در آینده اگر Persistence به Database یا تکنولوژی دیگری منتقل شود، Presentation نباید نیازمند تغییر باشد.

تصمیم مربوط به Persistence Strategy باید در ADR مستقل خود ثبت شود.

---

# 34. Architectural Rules

قوانین زیر برای Presentation Layer پذیرفته می‌شوند:

### Rule 1

Controller باید Thin باشد.

### Rule 2

Controller نباید Business Logic داشته باشد.

### Rule 3

Controller نباید مستقیماً Repository را فراخوانی کند.

### Rule 4

Controller نباید مستقیماً Persistence Implementation را فراخوانی کند.

### Rule 5

Domain Entity نباید مستقیماً در API Response استفاده شود.

### Rule 6

Request و Response باید Contractهای Presentation باشند.

### Rule 7

Input Validation در Presentation/Application Boundary انجام شود.

### Rule 8

Business Invariant در Domain enforce شود.

### Rule 9

Application Layer مالک Use Case و Orchestration باشد.

### Rule 10

Transaction Boundary در Application Use Case تعریف شود.

### Rule 11

Exception Handling API به‌صورت متمرکز انجام شود.

### Rule 12

API Contract مستقل از Domain Model باشد.

### Rule 13

APIهای عمومی Versioned باشند.

### Rule 14

APIها تا حد امکان با Business Module Boundaryها هم‌راستا باشند.

### Rule 15

Presentation نباید Entity داخلی Module دیگری را مصرف کند.

### Rule 16

Presentation نباید Repository Implementation را مصرف کند.

### Rule 17

Presentation نباید مستقیماً به In-Memory یا Database Storage دسترسی داشته باشد.

### Rule 18

Core Domain یعنی JournalEntry نباید به دلیل API Design به CRUD Logic تقلیل پیدا کند.

### Rule 19

Authentication Context باید بدون وابسته کردن Domain به Framework به Application منتقل شود.

### Rule 20

API Contract و Internal Module Contract باید مستقل از یکدیگر مدیریت شوند.

---

# 35. Architectural Enforcement

اصول این ADR باید تا حد امکان توسط Architectural Test و Code Review enforce شوند.

برای مثال:

```text
Controller
    must not depend on
Repository Implementation
```

```text
Controller
    must not depend on
Infrastructure Implementation
```

```text
Controller
    must depend on
Application Use Case / Contract
```

```text
Presentation
    must not access
Domain Entity of another Module directly
```

```text
Presentation
    must not access
Persistence directly
```

```text
Domain
    must not depend on
Presentation
```

همچنین باید موارد زیر کنترل شوند:

* Controller Thinness
* Presentation → Application Dependency
* API Contract Isolation
* Domain Model Isolation
* Repository Access Rules
* Module Boundaries
* Infrastructure Isolation
* Circular Dependencies

برای این منظور استفاده از **ArchUnit** و Code Review معماری توصیه می‌شود.

---

# 36. پیامدهای مثبت

این تصمیم باعث می‌شود:

* API از Domain مستقل بماند.
* Controllerها ساده و قابل تست باشند.
* Business Logic در Domain باقی بماند.
* Core Domain یعنی JournalEntry بهتر محافظت شود.
* API Contract پایدارتر شود.
* تغییر API الزاماً Domain را تغییر ندهد.
* Error Handling یکپارچه شود.
* API Versioning امکان Evolution سیستم را فراهم کند.
* Module Boundaryها در سطح Presentation نیز قابل مشاهده باشند.
* تغییر Persistence فعلی از In-Memory به تکنولوژی دیگر، Presentation را تحت تأثیر قرار ندهد.
* Domain و Application از HTTP و Frameworkهای Presentation مستقل باقی بمانند.

---

# 37. پیامدهای منفی

این معماری هزینه‌هایی نیز دارد:

* تعداد DTOها افزایش می‌یابد.
* Mapping بین API و Application ایجاد می‌شود.
* API Contract نیازمند مدیریت Lifecycle است.
* Versioning نیازمند مدیریت و Migration است.
* OpenAPI باید نگهداری شود.
* برای Use Caseهای ساده ممکن است تعداد Layerها بیشتر از یک CRUD ساده باشد.

این هزینه‌ها برای حفظ استقلال Domain، پایداری API و محافظت از Core Domain پذیرفته می‌شوند.

با این حال:

> DDD و Layering نباید باعث ایجاد Abstractionهای غیرضروری برای Use Caseهای ساده شوند.

---

# 38. تصمیم نهایی

> **Presentation در General Ledger به‌عنوان یک API Adapter در مرز بیرونی Bounded Context در نظر گرفته می‌شود. Presentation مسئول دریافت External Request، Input Validation، تبدیل Contractها، Authentication Context، Authorization در سطح Interface، فراخوانی Application Use Case، تبدیل Result به Response و مدیریت Errorهای API است.**

> **Controllerها باید Thin باشند و نباید مالک Business Logic، Domain Rule، Persistence یا Transaction Orchestration باشند.**

> **API Contract باید از Domain Model مستقل باشد. Request و Response باید DTOهای Presentation باشند و Domain Entity نباید مستقیماً به Consumer خارجی expose شود.**

> **Business Rule و Business Invariant باید مطابق ADR-0006 در Domain Model باقی بمانند و Application Layer مسئول Orchestration و اجرای Use Case باشد.**

> **APIهای General Ledger باید تا حد امکان با Business Moduleهای موجود یعنی Account Head، Account و Journal Entry هم‌راستا باشند. Journal Entry به‌عنوان Core Domain باید از تبدیل شدن به یک CRUD API صرف محافظت شود.**

> **Repository و Persistence Implementation نباید مستقیماً توسط Controller مصرف شوند. Presentation باید فقط از Application Use Caseها و Contractهای مناسب استفاده کند.**

> **در وضعیت فعلی، Persistence سیستم In-Memory است و Presentation نباید به این Implementation وابسته شود. تغییر Persistence در آینده باید بدون تغییر غیرضروری در Presentation امکان‌پذیر باشد.**

> **APIهای عمومی Versioned خواهند بود و OpenAPI به‌عنوان Contract Documentation مورد استفاده قرار خواهد گرفت.**

---

# 39. مدل نهایی Presentation

```text
                         External Client
                                │
                                ▼
                    ┌─────────────────────┐
                    │   REST API Adapter  │
                    │     Controller      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │    Request DTO      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Application UseCase │
                    └──────────┬──────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
          Account Module  AccountHead     JournalEntry
                              Module          Module
                                              ⭐ Core
                │              │              │
                └──────────────┼──────────────┘
                               │
                               ▼
                         Domain Model
                               │
                               ▼
                      Application Result
                               │
                               ▼
                        Response DTO
                               │
                               ▼
                       REST API Adapter
                               │
                               ▼
                         External Client
```

مرزهای کلی سیستم:

```text
                    General Ledger
                    Bounded Context
                           │
                           ▼
                 General Ledger Microservice
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
    Account Head        Account       Journal Entry
       Module            Module          Module
                                         ⭐ Core
```

و جهت وابستگی Presentation:

```text
Presentation
      │
      ▼
Application
      │
      ▼
Domain

Infrastructure
      │
      └── implements required Ports
```

نه:

```text
Presentation
      │
      X
      ▼
Infrastructure
```

---

# 40. رابطه با ADRهای مرتبط

## ADR-0003 — General Ledger Boundary

ADR-0003 ساختار سطح بالای General Ledger را مشخص می‌کند:

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

ADR-0008 این Boundary را تغییر نمی‌دهد و فقط نحوه ارائه API در مرز بیرونی آن را مشخص می‌کند.

---

## ADR-0004 — Dependency Direction و Internal Communication

ADR-0004 جهت Dependency و ارتباط داخلی را مشخص می‌کند.

مدل کلی:

```text
Presentation
      │
      ▼
Application
      │
      ├── Internal Contract / Port
      │
      ├── Repository Port
      │
      ▼
Domain
```

و:

```text
Infrastructure
      │
      └── implements Ports
```

ADR-0008 این اصول را در Presentation Layer اعمال می‌کند.

---

## ADR-0005 — Domain-Driven Design

ADR-0005 اصول کلی DDD و ساختار Strategic/Tactical Domain را مشخص می‌کند.

ADR-0008 از همان مدل استفاده می‌کند و API را بر اساس Business Boundaryهای تعریف‌شده طراحی می‌کند.

---

## ADR-0006 — Domain Model Strategy

ADR-0006 استراتژی Rich Domain Model را مشخص می‌کند:

```text
Entity
Value Object
Aggregate
Aggregate Root
Domain Service
Domain Event
```

ADR-0008 مسئولیت Presentation را در مقابل این Domain Model مشخص می‌کند.

اصل ارتباط این دو ADR:

```text
ADR-0006
    │
    └── Domain Decides
              ▲
              │
ADR-0008 ─────┘
    │
    └── Presentation Adapts
```

یا به‌صورت ساده:

```text
Presentation
    =
Adapt

Application
    =
Orchestrate

Domain
    =
Decide
```

---

# 41. مدل تصمیم‌های معماری مرتبط

```text
ADR-0003
    │
    └── General Ledger Boundary
             │
             ▼
      Bounded Context
             │
             ▼
        Microservice
             │
             ▼
ADR-0005 ──► DDD Structure
             │
             ▼
ADR-0006 ──► Rich Domain Model
             │
             ▼
       Aggregate / Entity
             │
             ▼
ADR-0004 ──► Dependency / Ports
             │
             ▼
ADR-0008 ──► Presentation / API
```

بنابراین ADR-0008 نباید Domain Boundary، Aggregate Boundary یا Persistence Strategy را دوباره تعریف کند.

---

# 42. Status

**Status: Accepted**

این ADR باید به‌عنوان مبنای تصمیم‌گیری برای موارد زیر استفاده شود:

* REST API
* Controller
* Request DTO
* Response DTO
* API Contract
* API Versioning
* Input Validation
* API Error Handling
* HTTP Status Mapping
* Authentication Context
* Authorization Boundary
* Pagination
* Filtering
* Sorting
* Idempotency در سطح API
* API Transaction Boundary
* OpenAPI
* Presentation Module Structure

هر تصمیمی که اصول این ADR را تغییر دهد باید از طریق یک Architectural Decision جدید ثبت شود یا این ADR به‌صورت رسمی اصلاح گردد.

---

# 43. منابع (References)

## 43.1 Robert C. Martin — Clean Architecture

**Robert C. Martin — *Clean Architecture: A Craftsman's Guide to Software Structure and Design***

مبنای تصمیم‌های مرتبط با:

* Dependency Rule
* Boundary
* Separation of Concerns
* Framework Independence
* Interface Adapters
* Use Cases
* Business Rule Independence

---

## 43.2 Eric Evans — Domain-Driven Design

**Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of Software***

مبنای تصمیم‌های مرتبط با:

* Domain Model
* Bounded Context
* Entity
* Aggregate
* Domain Service
* Business Model

---

## 43.3 Martin Fowler — Patterns of Enterprise Application Architecture

**Martin Fowler — *Patterns of Enterprise Application Architecture***

مبنای تصمیم‌های مرتبط با:

* Service Layer
* Data Transfer Object
* Remote Facade
* Repository
* Data Mapper
* Layered Architecture

---

## 43.4 Mark Richards & Neal Ford — Fundamentals of Software Architecture

**Mark Richards, Neal Ford — *Fundamentals of Software Architecture***

مبنای تکمیلی برای:

* Architecture Boundaries
* Coupling
* Cohesion
* Architectural Characteristics
* Evolutionary Architecture

---

## 43.5 OpenAPI Specification

**OpenAPI Specification**

مبنای مستندسازی و تعریف API Contract.

---

# 44. جمع‌بندی منابع مورد استفاده

| موضوع                   | منبع اصلی                                 |
| ----------------------- | ----------------------------------------- |
| Architecture Boundaries | Robert C. Martin                          |
| Dependency Direction    | Robert C. Martin                          |
| Interface Adapter       | Robert C. Martin                          |
| Use Case Boundary       | Robert C. Martin                          |
| Domain Model            | Eric Evans                                |
| Aggregate               | Eric Evans                                |
| Bounded Context         | Eric Evans                                |
| Service Layer           | Martin Fowler                             |
| DTO                     | Martin Fowler                             |
| Repository              | Martin Fowler / Eric Evans                |
| API Contract            | OpenAPI Specification                     |
| Architectural Coupling  | Mark Richards / Neal Ford                 |
| API Evolution           | OpenAPI / Architectural Design Principles |

> **این منابع مبنای نظری و معماری این ADR هستند؛ اما تصمیم نهایی این سند متناسب با Boundaryها، Business Rules و ساختار واقعی General Ledger Microservice اتخاذ شده است.**

> **در وضعیت فعلی، General Ledger از In-Memory Persistence استفاده می‌کند. این موضوع یک Implementation Detail است و نباید باعث وابستگی Presentation یا API Contract به Persistence فعلی شود. تصمیمات مربوط به انتخاب و تکامل Persistence باید در ADR مستقل مربوط به Persistence ثبت شوند.**
