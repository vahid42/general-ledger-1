# ADR-0017 — استراتژی خطا و Exception دامنه (Domain Error & Exception Strategy)

- **وضعیت:** پذیرفته شده (Accepted)
- **تاریخ:** 2026-08-22
- **ADRهای مرتبط:**
  - ADR-0011 
  - ADR-0012 
  - ADR-0013 
  - ADR-0014 
  - ADR-0016
  - ADR-0018 

---

## زمینه (Context)

در معماری مبتنی بر **DDD** و **Clean Architecture**، خطاهای مربوط به Business Domain باید از خطاهای فنی و جزئیات Transport جدا باشند.

Domain ممکن است در شرایط مختلف نتواند یک Business Operation را انجام دهد، برای مثال:

- موجودی کافی نیست.
- حساب بسته شده است.
- انتقال بین Currencyهای ناسازگار انجام می‌شود.
- سقف انتقال نقض شده است.
- وضعیت Aggregate اجازه انجام عملیات را نمی‌دهد.
- یک Business Invariant نقض شده است.

این خطاها نباید با جزئیات زیر مدل شوند:

- HTTP Status
- REST Exception
- Spring Exception
- JPA/Hibernate Exception
- Database Exception
- Message Broker Exception

همچنین لازم است مشخص شود:

- Domain Error چیست؟
- چه زمانی Domain Exception ایجاد می‌شود؟
- آیا تمام خطاهای Domain باید Exception باشند؟
- Error Code چگونه تعریف می‌شود؟
- کدام Layer مسئول Error Translation است؟
- Technical Error چگونه از Domain Error جدا می‌شود؟
- Validation در کدام Layer انجام می‌شود؟
- Logging و Monitoring در کدام Layer انجام می‌شود؟

هدف این ADR تعیین یک استراتژی یکپارچه برای مدل‌سازی و مدیریت خطاهای Domain است.

---

# تصمیم (Decision)

## 1. تفکیک Domain Error از Technical Error

خطاهای سیستم حداقل به دو دسته اصلی تقسیم می‌شوند:

```text
Error
│
├── Domain / Business Error
│
└── Technical / Infrastructure Error
````

### Domain Error

Domain Error زمانی رخ می‌دهد که یک Business Rule یا Business Invariant اجازه انجام عملیات را نمی‌دهد.

مثال:

```text
InsufficientBalance
AccountAlreadyClosed
AccountAlreadyBlocked
TransferLimitExceeded
InvalidAccountStateTransition
IncompatibleCurrency
```

### Technical Error

Technical Error ناشی از شکست فنی یا زیرساختی است.

مثال:

```text
DatabaseConnectionFailure
Timeout
NetworkFailure
SerializationFailure
MessageBrokerUnavailable
ExternalServiceUnavailable
```

این دو نوع Error نباید با یکدیگر ترکیب شوند.

---

## 2. Domain Error یک مفهوم است، نه الزاماً یک تکنیک پیاده‌سازی

**Domain Error** یک مفهوم معماری است که نشان‌دهنده عدم امکان انجام یک عملیات به دلیل Business Rule است.

در Java، برای پروژه حاضر، **Domain Exception به عنوان مکانیزم پیش‌فرض انتقال Domain Error انتخاب می‌شود.**

بنابراین:

```text
Domain Error
     │
     ▼
Domain Exception
```

اما این ADR ادعا نمی‌کند که هر Business Failure در تمام شرایط و تمام Use Caseها الزاماً باید با Exception پیاده‌سازی شود.

در صورت وجود نیاز معماری مشخص، می‌توان از مدل‌هایی مانند:

```text
Result
Outcome
Either
```

نیز استفاده کرد.

با این حال، الگوی پیش‌فرض پروژه برای Business Ruleهای نقض‌شده، **Domain Exception** خواهد بود.

---

## 3. Domain Exception باید Framework Independent باشد

Domain Exception نباید به Framework یا Transport وابسته باشد.

نامناسب:

```java
throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Insufficient balance"
);
```

یا:

```java
throw new HttpClientErrorException(...);
```

یا:

```java
throw new DataAccessException(...);
```

Domain فقط باید Business Error خود را بیان کند.

---

## 4. Base Domain Exception

در صورت استفاده از Exception-based Domain Error، تمام Domain Exceptionها باید از یک Base Type مشخص Domain استفاده کنند.

مثلاً:

```java
public abstract class DomainException
        extends RuntimeException {

    private final DomainErrorCode errorCode;

    protected DomainException(
            DomainErrorCode errorCode,
            String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainErrorCode getErrorCode() {
        return errorCode;
    }
}
```

هدف:

* تشخیص مشخص Domain Errorها
* نگهداری Error Code
* جلوگیری از استفاده مستقیم از Exceptionهای Framework
* فراهم کردن امکان Error Mapping در لایه‌های بالاتر

---

## 5. Error Code باید Type-Safe و پایدار باشد

Business Errorهای مهم باید دارای Error Code پایدار باشند.

به جای پراکنده کردن Stringها:

```java
"ACCOUNT.INSUFFICIENT_BALANCE"
```

ترجیح داده می‌شود Error Code به صورت Type مشخص Domain مدل شود.

مثلاً:

```java
public enum AccountErrorCode implements DomainErrorCode {

    INSUFFICIENT_BALANCE,
    ALREADY_CLOSED,
    ALREADY_BLOCKED
}
```

یا در صورت نیاز به ساختار مشترک:

```java
public interface DomainErrorCode {

    String value();
}
```

Error Code باید:

* Stable باشد.
* مستقل از متن Message باشد.
* قابل استفاده برای Logging و Monitoring باشد.
* قابلیت Mapping به API Error Contract را داشته باشد.
* به HTTP Status وابسته نباشد.

---

## 6. Error Message، Contract اصلی نیست

Message نباید به عنوان Contract اصلی بین Domain و Client استفاده شود.

نامناسب:

```text
if message == "Insufficient balance"
```

مناسب:

```text
code = ACCOUNT.INSUFFICIENT_BALANCE
```

Message می‌تواند برای موارد زیر استفاده شود:

* Developer
* Logging
* Debugging
* User-facing Message
* Localization

اما Client نباید Business Decision خود را بر اساس متن Message انجام دهد.

---

## 7. Domain Error باید Business Meaning داشته باشد

Domain Exception باید بیانگر یک Business Concept مشخص باشد.

مناسب:

```text
InsufficientBalanceException
AccountAlreadyClosedException
AccountAlreadyBlockedException
TransferLimitExceededException
InvalidAccountStateTransitionException
```

نامناسب:

```text
ValidationException
ProcessingException
OperationException
GeneralException
CommonException
RuntimeException
```

نام Error باید از **Ubiquitous Language** پروژه گرفته شود.

---

## 8. Exception باید در نزدیک‌ترین نقطه به Rule ایجاد شود

Business Rule باید در همان جایی که مالک آن Rule است بررسی شود.

مثلاً اگر `Account` مالک Rule مربوط به برداشت است:

```java
public void withdraw(Money amount) {

    if (balance.isLessThan(amount)) {
        throw new InsufficientBalanceException();
    }

    balance = balance.subtract(amount);
}
```

در این حالت Entity خودش از Invariant مربوط به Balance محافظت می‌کند.

Business Rule نباید صرفاً برای راحتی به Application Service منتقل شود.

---

## 9. Aggregate مسئول محافظت از Invariantهای خود است

بر اساس ADR-0012، Aggregate Boundary مرز اصلی Consistency و Business Invariant است.

بنابراین اگر یک Invariant متعلق به Aggregate است، نقض آن باید در همان Aggregate متوقف شود.

مثلاً:

```java
public void changeStatus(AccountStatus newStatus) {

    if (!status.canTransitionTo(newStatus)) {
        throw new InvalidAccountStateTransitionException();
    }

    this.status = newStatus;
}
```

نباید Aggregate اجازه دهد State نامعتبر ایجاد شود و بعداً Application Layer آن را اصلاح کند.

---

## 10. Domain Service نیز می‌تواند Domain Error ایجاد کند

بر اساس ADR-0014، Domain Service زمانی استفاده می‌شود که Business Logic مالک طبیعی در Entity یا Value Object نداشته باشد.

در این حالت Domain Service نیز می‌تواند Domain Error ایجاد کند.

مثلاً:

```java
public void transfer(
        Account source,
        Account destination,
        Money amount
) {

    if (!policy.isAllowed(source, destination, amount)) {
        throw new InvalidTransferException();
    }

    source.withdraw(amount);
    destination.deposit(amount);
}
```

اما Domain Service نباید برای خطاهای Infrastructure مانند Database یا HTTP Exception ایجاد کند.

---

## 11. Value Object نیز می‌تواند Domain Error ایجاد کند

بر اساس ADR-0013، Value Object مسئول محافظت از Invariantهای مربوط به خودش است.

مثلاً:

```java
public record Percentage(BigDecimal value) {

    public Percentage {

        if (value == null ||
            value.compareTo(BigDecimal.ZERO) < 0 ||
            value.compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new InvalidPercentageException();
        }
    }
}
```

بنابراین:

```text
Entity
   │
   └── protects Entity Invariants

Value Object
   │
   └── protects Value Invariants

Domain Service
   │
   └── protects its Domain Rules
```

---

## 12. Domain نباید HTTP Status Code را بشناسد

Domain نباید چیزی درباره موارد زیر بداند:

```text
HTTP 400
HTTP 404
HTTP 409
HTTP 422
HTTP 500
```

Domain فقط Business Error تولید می‌کند.

تبدیل Domain Error به HTTP Response در Presentation Layer انجام می‌شود.

```text
Domain Exception
       │
       ▼
Application
       │
       ▼
Presentation Exception Handler
       │
       ▼
HTTP Response
```

---

## 13. Domain Error و Application Error یکی نیستند

هر Domain Error الزاماً نیاز به Application Exception جدید ندارد.

در حالت پیش‌فرض:

```text
Domain Error
     │
     ▼
Domain Exception
     │
     ▼
Application
```

Application Layer می‌تواند Domain Exception را:

* Propagate کند.
* Handle کند.
* به Application Error تبدیل کند.
* Retry یا Compensation انجام دهد؛ در صورت مناسب بودن.

Application Error فقط زمانی ایجاد می‌شود که Use Case واقعاً به یک Contract مستقل نیاز داشته باشد.

نباید برای هر Domain Exception یک Application Exception مصنوعی ایجاد شود.

---

## 14. Validation باید از Business Rule جدا شود

Validation به دو دسته اصلی تقسیم می‌شود:

```text
Validation
│
├── Structural / Input Validation
│
└── Business Validation
```

### Structural / Input Validation

مثلاً:

```text
email is blank
amount is null
name exceeds max length
field format is invalid
```

این موارد معمولاً در مرز سیستم و Application/Presentation Validation بررسی می‌شوند.

### Business Validation

مثلاً:

```text
Account is closed
Balance is insufficient
Transfer limit exceeded
Currency is incompatible
```

این موارد متعلق به Domain هستند.

---

## 15. Domain نباید Technical Exception ایجاد کند

Domain نباید Exceptionهایی مانند موارد زیر را ایجاد یا وابسته به آن‌ها شود:

```text
SQLException
DataAccessException
TimeoutException
ConnectException
JsonProcessingException
KafkaException
HttpClientErrorException
```

این Exceptionها متعلق به Infrastructure یا سایر لایه‌های فنی هستند.

---

## 16. Technical Exception در Infrastructure مدیریت می‌شود

Infrastructure مسئول مدیریت خطاهای مربوط به Technology است.

مثلاً:

```text
Infrastructure
     │
     ├── Database Error
     ├── Network Error
     ├── Serialization Error
     └── Messaging Error
```

در صورت نیاز، Infrastructure می‌تواند Technical Exception را به یک Error Type مناسب برای Application تبدیل کند.

مثلاً:

```java
try {

    repository.save(account);

} catch (DataAccessException ex) {

    throw new AccountPersistenceException(ex);
}
```

اما نباید:

```java
catch (DataAccessException ex) {
    throw new DomainException(...);
}
```

انجام شود.

---

## 17. Domain Exception نباید Logging انجام دهد

Domain Exception نباید خودش Logging انجام دهد.

نامناسب:

```java
public class InsufficientBalanceException
        extends DomainException {

    public InsufficientBalanceException() {

        log.error("Insufficient balance");

    }
}
```

Domain باید فقط Error را بیان کند.

Logging باید در Layer مناسب و بر اساس Context انجام شود.

این کار از موارد زیر جلوگیری می‌کند:

* Duplicate Logging
* وابستگی Domain به Logging Framework
* Log Noise
* Coupling

---

## 18. Business Error معمولاً Expected است

Business Error معمولاً بخشی از رفتار قابل انتظار سیستم است.

مثلاً:

```text
InsufficientBalance
AccountAlreadyClosed
InvalidStateTransition
```

بنابراین هر Domain Exception الزاماً نباید با سطح `ERROR` لاگ شود.

Severity باید در Layer مسئول Logging و بر اساس Context تعیین شود.

مثلاً:

```text
Expected Business Failure
        │
        └── INFO / WARN / no log

Unexpected Technical Failure
        │
        └── ERROR
```

تصمیم دقیق Logging در ADR مربوط به Observability مشخص خواهد شد.

---

## 19. Error Translation در مرزها انجام می‌شود

هر Layer فقط Errorهای مربوط به Boundary خودش را ترجمه می‌کند.

```text
Domain Error
     │
     ▼
Application Error
     │
     ▼
API Error Contract
```

برای مثال:

```text
InsufficientBalance
        │
        ▼
DomainException
        │
        ▼
API Error Mapping
        │
        ▼
HTTP Response
```

انتخاب HTTP Status در Domain انجام نمی‌شود.

---

## 20. Presentation مسئول API Error Contract است

Presentation Layer مسئول تبدیل Domain/Application Error به API Error Contract است.

مثلاً:

```json
{
  "code": "ACCOUNT.INSUFFICIENT_BALANCE",
  "message": "Insufficient balance",
  "traceId": "..."
}
```

ساختار دقیق API Error Contract در ADR مستقل مربوط به API و Error Response مشخص خواهد شد.

---

## 21. Error Code نباید با HTTP Status یکی باشد

این دو مفهوم باید مستقل باشند.

مثلاً:

```text
Domain:

ACCOUNT.INSUFFICIENT_BALANCE
```

و در Presentation:

```text
HTTP 409
```

ممکن است در آینده Mapping تغییر کند بدون اینکه Domain Error Code تغییر کند.

بنابراین:

```text
Domain Error Code
        ≠
HTTP Status Code
```

---

## 22. Exception Wrapping

Technical Exception در صورت نیاز می‌تواند در Infrastructure یا Application Layer Wrap شود.

مثلاً:

```java
try {

    externalClient.call();

} catch (TimeoutException ex) {

    throw new ExternalServiceUnavailableException(ex);
}
```

اما Technical Error نباید به صورت مصنوعی به Domain Error تبدیل شود.

---

## 23. Error Handling در Application Layer

Application Layer مسئول تصمیم‌گیری در سطح Use Case است.

در صورت دریافت Error ممکن است:

* Error را Propagate کند.
* Error را به Application Error تبدیل کند.
* Retry انجام دهد.
* Compensation انجام دهد.
* عملیات را متوقف کند.

مثلاً:

```text
Application Use Case
       │
       ├── Domain Error
       │
       ├── Infrastructure Error
       │
       └── External Service Error
```

Application Layer نباید Business Rule اصلی را صرفاً به دلیل Error Handling در خود قرار دهد.

---

## 24. Error Handling و Retry

Retry نباید در Domain انجام شود.

مثلاً Domain نباید چیزی درباره:

```text
retry
backoff
timeout
circuit breaker
```

بداند.

Retry تصمیمی مربوط به Application/Infrastructure است و باید بر اساس نوع Failure و Idempotency انجام شود.

---

## 25. Domain Error و Transaction

Domain Error نباید باعث طراحی Transactionهای بزرگ و غیرضروری شود.

مثلاً:

```text
Application Use Case
       │
       ▼
Transaction
       │
       ▼
Aggregate
       │
       ▼
Domain Rule
       │
       └── Domain Error
```

در صورت رخ دادن Domain Error، Use Case باید طبق قواعد Transaction آن عملیات را متوقف یا Rollback کند.

جزئیات Transaction در ADR مربوط به Transaction Strategy مشخص خواهد شد.

---

## 26. Error Contract نباید Domain Model را به API وابسته کند

این مدل نامناسب است:

```java
public class InsufficientBalanceException {

    private HttpStatus status;
    private String responseBody;
}
```

Domain Exception نباید شامل:

* HTTP Status
* HTTP Headers
* REST Response
* JSON Response
* Controller Information

باشد.

---

## 27. Domain Exception باید اطلاعات لازم برای Business Error را حمل کند

در صورت نیاز، Domain Exception می‌تواند اطلاعات Domain موردنیاز برای Error را حمل کند.

مثلاً:

```java
public final class InsufficientBalanceException
        extends DomainException {

    private final Money balance;
    private final Money requestedAmount;

    public InsufficientBalanceException(
            Money balance,
            Money requestedAmount
    ) {
        super(
            AccountErrorCode.INSUFFICIENT_BALANCE,
            "Insufficient balance"
        );

        this.balance = balance;
        this.requestedAmount = requestedAmount;
    }
}
```

این اطلاعات باید Domain-oriented باشند و شامل جزئیات Transport یا Infrastructure نباشند.

---

## 28. Error Message نباید الزاماً User-Facing باشد

Message موجود در Domain Exception لزوماً همان Message نهایی Client نیست.

مثلاً:

```text
Domain Message
       │
       ▼
Application
       │
       ▼
Localization / API Message
```

در صورت نیاز، Presentation Layer می‌تواند Error Code را به Message مناسب User تبدیل کند.

---

# قوانین (Rules)

* Domain Error باید Business Meaning داشته باشد.
* Domain Exception باید Framework Independent باشد.
* Domain نباید HTTP Status Code را بشناسد.
* Domain نباید HTTP Response یا API Contract را بشناسد.
* Domain نباید Logging انجام دهد.
* Domain نباید Technical Exception ایجاد کند.
* Business Error و Technical Error باید از یکدیگر جدا باشند.
* Domain Exception مکانیزم پیش‌فرض پروژه برای انتقال Business Failure است.
* استفاده از Result/Outcome در صورت وجود نیاز معماری مشخص مجاز است.
* Error Code باید پایدار و مستقل از Message باشد.
* Error Code نباید با HTTP Status Code یکی باشد.
* Client نباید بر اساس متن Message تصمیم‌گیری کند.
* Entity باید Invariantهای متعلق به خودش را محافظت کند.
* Aggregate Root باید Invariantهای Aggregate را محافظت کند.
* Value Object باید Invariantهای مقدار خود را محافظت کند.
* Domain Service می‌تواند Domain Error ایجاد کند.
* Input Validation و Business Validation باید تفکیک شوند.
* Technical Exception باید در Infrastructure/Application مدیریت شود.
* Presentation مسئول تبدیل Error به API Error Contract است.
* Retry و Recovery نباید در Domain انجام شود.
* Domain Exception نباید مسئول Logging باشد.
* هر Domain Exception الزاماً نباید به Application Exception جدید تبدیل شود.
* Domain Error نباید باعث ایجاد Transactionهای غیرضروری بین Aggregateها شود.

---

# Error Flow

## Domain Error

```text
┌──────────────────────────────┐
│            Domain            │
│                              │
│   Entity / Value Object      │
│   Aggregate / Domain Service │
│             │                │
│             ▼                │
│      Domain Exception        │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│         Application          │
│                              │
│  Propagate / Handle / Map    │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│         Presentation         │
│                              │
│     Exception Handler        │
│             │                │
│             ▼                │
│      API Error Contract      │
└──────────────────────────────┘
```

## Technical Error

```text
┌──────────────────────────────┐
│       Infrastructure         │
│                              │
│ DB / Network / Messaging     │
│             │                │
│             ▼                │
│      Technical Exception     │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│         Application          │
│                              │
│ Retry / Recovery / Mapping   │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│         Presentation         │
│                              │
│      API Error Contract      │
└──────────────────────────────┘
```

---

# تصمیم درباره Base Exception

استفاده از یک Base Domain Exception در Domain پذیرفته می‌شود:

```java
DomainException
    │
    ├── AccountDomainException
    │       ├── InsufficientBalanceException
    │       ├── AccountAlreadyClosedException
    │       └── InvalidAccountStateTransitionException
    │
    ├── TransferDomainException
    │       └── TransferLimitExceededException
    │
    └── PaymentDomainException
```

با این حال، ایجاد Hierarchyهای عمیق و غیرضروری ممنوع است.

در صورتی که یک Exception Business Meaning مشخصی دارد، همان Exception Concrete باید ترجیح داده شود.

---

# تصمیم درباره Error Code

برای Business Errorهای قابل مصرف خارج از Domain، Error Code پایدار تعریف می‌شود.

مثلاً:

```text
ACCOUNT.INSUFFICIENT_BALANCE
ACCOUNT.ALREADY_CLOSED
ACCOUNT.ALREADY_BLOCKED
TRANSFER.LIMIT_EXCEEDED
TRANSFER.INVALID_STATE
PAYMENT.INVALID_STATE
```

Error Code باید:

* Stable باشد.
* Unique باشد.
* مستقل از Message باشد.
* مستقل از HTTP باشد.
* مستقل از Persistence Technology باشد.
* قابل استفاده در Monitoring و API Mapping باشد.

---

# پیامدها (Consequences)

## مزایا

* جداسازی Business Error از Technical Error
* استقلال Domain از HTTP و Framework
* حفظ Business Invariantها
* Error Contract پایدارتر
* افزایش خوانایی Domain Model
* جلوگیری از انتشار Exceptionهای Infrastructure به Domain
* امکان مدیریت مناسب Logging و Monitoring
* تست‌پذیری بهتر Business Ruleها
* جلوگیری از وابستگی Client به متن Message
* امکان Mapping مستقل Domain Error به API Error
* هماهنگی با Entity، Aggregate، Value Object و Domain Service Strategy

## معایب

* نیاز به Error Mapping در Boundaryها
* افزایش تعداد Domain Error Typeها
* نیاز به تعریف و نگهداری Error Codeها
* نیاز به طراحی دقیق Error Hierarchy
* احتمال Over-Engineering در صورت تعریف Exception برای هر خطای بسیار جزئی
* پیچیدگی بیشتر نسبت به استفاده از یک Exception عمومی

---

# وضعیت اجرا (Implementation Status)

تمام Business Ruleهای Domain باید در صورت نقض، Domain Error مناسب ایجاد کنند.

در پروژه، **Domain Exception مکانیزم پیش‌فرض برای انتقال Business Failure در Domain است.**

Domain Exceptionها در Domain Layer تعریف می‌شوند و نباید هیچ وابستگی به:

* Spring
* HTTP
* Database
* JPA
* Hibernate
* Message Broker
* Transport

داشته باشند.

مالکیت Error بر اساس محل Business Rule تعیین می‌شود:

```text
Entity
   │
   └── Entity Business Error

Value Object
   │
   └── Value Business Error

Aggregate Root
   │
   └── Aggregate Invariant Error

Domain Service
   │
   └── Domain Operation Error
```

Technical Errorها در Infrastructure/Application مدیریت می‌شوند.

Presentation مسئول تبدیل Domain/Application Error به API Error Contract است.

ساختار دقیق API Error Response در ADR مستقل مربوط به **API & Error Response Strategy** مشخص خواهد شد.

---

# References

1. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**
2. Vaughn Vernon — **Implementing Domain-Driven Design**
3. Vaughn Vernon — **Domain-Driven Design Distilled**
4. Robert C. Martin — **Clean Architecture: A Craftsman's Guide to Software Structure and Design**
5. Martin Fowler — **Patterns of Enterprise Application Architecture**
6. RFC 9457 — **Problem Details for HTTP APIs**
7. Spring Framework — **Exception Handling Documentation**

````

### یک نکته مهم برای ادامه ADRها

با این اصلاح، زنجیره ADRهای Domain ما خیلی تمیزتر می‌شود:

```text
ADR-0011
Domain Layer
     │
     ├── ADR-0012 → Entity / Aggregate
     │                  │
     │                  └── Invariant Ownership
     │
     ├── ADR-0013 → Value Object
     │                  │
     │                  └── Value Invariant
     │
     ├── ADR-0014 → Domain Service
     │                  │
     │                  └── Cross-Entity / Domain Operation
     │
     ├── ADR-0015 → Domain Event
     │
     ├── ADR-0016 → Repository
     │
     └── ADR-0017 → Domain Error
````

یعنی **ADR-0017 دیگر فقط «Exception Strategy» نیست؛ بلکه Error Ownership را هم مشخص می‌کند**. این مهم است، چون وقتی بعداً کد `AccountHeading` را بررسی کنیم، دقیقاً می‌دانیم Exception مربوط به `code already exists` باید کجا قرار بگیرد و آیا واقعاً Domain Service مالک آن Rule هست یا Repository فقط داده لازم برای تصمیم را فراهم می‌کند.
