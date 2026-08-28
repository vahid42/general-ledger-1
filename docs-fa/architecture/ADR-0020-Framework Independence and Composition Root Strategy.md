# ADR-0020 — Framework Independence and Composition Root Strategy

- **Status:** Accepted
- **Date:** 2026-08-22
- **ADR Type:** Architectural
- **Scope:** All Bounded Contexts

---

# 1. زمینه (Context)

پروژه‌ی General Ledger بر اساس معماری لایه‌ای، Clean Architecture و Domain-Driven Design طراحی شده است.

ساختار اصلی پروژه شامل لایه‌های زیر است:

```text
ledger-bootstrap
ledger-presentation
ledger-application
ledger-domain
ledger-infrastructure
````

در این معماری، Domain و Application باید مستقل از Framework مورد استفاده‌ی سیستم باقی بمانند.

در حال حاضر Spring Boot به عنوان Framework اصلی اجرای Application استفاده می‌شود.

بنابراین باید مشخص شود که:

> آیا Domain و Application باید مستقیماً Spring را بشناسند یا Spring باید فقط در مرز بیرونی سیستم قرار گیرد؟

دو رویکرد اصلی وجود دارد.

### روش اول — Framework-Aware Application

```java
@Service
public class CreateAccountHeadingService {
    ...
}
```

یا:

```java
@Component
public class AccountHeadingDomainService {
    ...
}
```

در این مدل، Application و Domain مستقیماً Spring را می‌شناسند.

---

### روش دوم — Framework-Independent Application

```java
public class CreateAccountHeadingService {
    ...
}
```

و:

```java
public class AccountHeadingDomainService {
    ...
}
```

در این مدل، Domain و Application به صورت Plain Java باقی می‌مانند و Framework در Composition Root مسئول ایجاد و اتصال Objectها است.

---

# 2. مسئله (Problem)

استفاده از Spring Annotationهایی مانند:

```text
@Service
@Component
@Repository
@Autowired
@Configuration
@Bean
```

از نظر فنی معتبر است.

اما استفاده از آن‌ها در Domain و Application باعث ایجاد Framework Coupling می‌شود.

برای مثال:

```text
Application
    │
    └── @Service
          │
          ▼
      Spring Framework
```

یا:

```text
Domain
    │
    └── @Component
          │
          ▼
      Spring Framework
```

در این حالت Business Logic برای اجرا و ساخت Objectهای خود به Framework وابسته می‌شود.

این موضوع با تصمیمات قبلی پروژه درباره‌ی Framework Independence و Dependency Direction سازگار نیست.

بنابراین باید مرز مشخصی بین:

```text
Business Logic
```

و:

```text
Framework / Infrastructure
```

وجود داشته باشد.

---

# 3. تصمیم معماری (Decision)

تصمیم نهایی پروژه:

> **Domain و Application باید Framework-Independent باقی بمانند و Spring فقط در Composition Root و Adapterهای Framework-aware مورد استفاده قرار گیرد.**

بنابراین:

```text
Domain
    ↓
Plain Java

Application
    ↓
Plain Java

Infrastructure
    ↓
May be Framework-aware

Presentation
    ↓
Framework-aware

Bootstrap
    ↓
Framework-aware
Composition Root
```

اصل کلیدی:

> **Business Logic نباید برای اجرا، ساخت یا مدیریت Lifecycle خود به Spring وابسته باشد.**

Spring مسئول:

```text
Object Creation
Dependency Injection
Object Lifecycle
Configuration
Application Startup
```

است.

اما مسئول:

```text
Business Rules
Domain Invariants
Use Case Logic
Domain Decisions
```

نیست.

---

# 4. Composition Root

ماژول:

```text
ledger-bootstrap
```

به عنوان **Composition Root** سیستم تعیین می‌شود.

Composition Root محل ایجاد و اتصال Object Graph سیستم است.

وظایف Composition Root:

```text
Create Objects
        +
Connect Dependencies
        +
Select Implementations
        +
Configure Infrastructure
        +
Expose Application to Framework
```

به صورت مفهومی:

```text
                 Composition Root
                 ledger-bootstrap
                        │
        ┌───────────────┼────────────────┐
        │               │                │
        ▼               ▼                ▼
    Application      Domain       Infrastructure
     Services        Objects          Adapters
        │               │                │
        └───────────────┼────────────────┘
                        │
                        ▼
                     Spring
```

Composition Root می‌تواند:

```text
Application Service
Domain Service
Repository Implementation
Gateway
Adapter
Configuration
```

را به یک Object Graph تبدیل کند.

اما Composition Root نباید شامل Business Logic باشد.

---

# 5. Domain Layer

Domain Layer باید Framework-Independent باشد.

بنابراین موارد زیر نباید به Spring وابسته باشند:

```text
Aggregate
Entity
Value Object
Domain Service
Domain Repository Interface
Domain Policy
Domain Event
```

برای مثال:

```java
public class AccountHeadingDomainService {

    private final AccountHeadingRepository repository;

    public AccountHeadingDomainService(
            AccountHeadingRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    ...
}
```

این کلاس نباید دارای:

```java
@Service
@Component
@Autowired
ApplicationContext
BeanFactory
```

باشد.

Domain فقط Business Logic و Domain Abstractionهای خودش را می‌شناسد.

---

# 6. Application Layer

Application Layer نیز باید Framework-Independent باقی بماند.

برای مثال:

```java
public class CreateAccountHeadingService {

    private final AccountHeadingRepository repository;
    private final AccountHeadingDomainService domainService;

    public CreateAccountHeadingService(
            AccountHeadingRepository repository,
            AccountHeadingDomainService domainService
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.domainService = Objects.requireNonNull(domainService);
    }

    ...
}
```

Application Service نباید دارای:

```java
@Service
@Component
@Autowired
ApplicationContext
```

باشد.

Application Service مسئول:

```text
Use Case
Orchestration
Transaction Intent
Aggregate Interaction
Calling Ports
```

است.

اما مسئول:

```text
Object Lifecycle
Dependency Lookup
Framework Configuration
```

نیست.

---

# 7. Infrastructure Layer

Infrastructure محل Implementation مربوط به Portها و Integration با سیستم‌های خارجی است.

برای مثال:

```text
Domain/Application Port
        │
        ▼
AccountHeadingRepository
        ▲
        │
Infrastructure
        │
        ▼
JpaAccountHeadingRepository
```

Infrastructure مجاز است Framework-aware باشد.

برای مثال:

```text
Spring Data
JPA
Hibernate
Kafka
Redis
HTTP Client
Database Driver
```

می‌توانند در Infrastructure مورد استفاده قرار گیرند.

اما این وابستگی نباید به Domain یا Application نشت کند.

بنابراین:

```text
Domain
    ❌ Spring Data

Application
    ❌ Spring Data

Infrastructure
    ✅ Spring Data
```

---

# 8. Repository Wiring

Repository Interface در Domain/Application تعریف می‌شود.

برای مثال:

```java
public interface AccountHeadingRepository {

    Optional<AccountHeading> findById(AccountHeadingId id);

    boolean existsByCode(AccountHeadingCode code);

    void save(AccountHeading accountHeading);
}
```

Implementation در Infrastructure قرار می‌گیرد:

```java
public class JpaAccountHeadingRepository
        implements AccountHeadingRepository {

    ...
}
```

Composition Root این دو را به یکدیگر متصل می‌کند:

```text
AccountHeadingRepository
        ▲
        │ implements
        │
JpaAccountHeadingRepository
        │
        ▼
ledger-bootstrap
        │
        ▼
Spring Bean
```

Domain هیچ اطلاعی از:

```text
Jpa
Hibernate
Spring Data
Database
```

ندارد.

---

# 9. Application Service Wiring

Application Service در Bootstrap ساخته و Register می‌شود.

برای مثال:

```java
@Bean
public CreateAccountHeadingService createAccountHeadingService(
        AccountHeadingRepository repository,
        AccountHeadingDomainService domainService
) {
    return new CreateAccountHeadingService(
            repository,
            domainService
    );
}
```

Spring در اینجا فقط Object Graph را مدیریت می‌کند.

Business Logic همچنان داخل:

```text
Application
Domain
```

باقی می‌ماند.

---

# 10. Domain Service Wiring

Domain Service نیز در Composition Root ساخته می‌شود.

برای مثال:

```java
@Bean
public AccountHeadingDomainService accountHeadingDomainService(
        AccountHeadingRepository repository
) {
    return new AccountHeadingDomainService(repository);
}
```

اما نکته مهم:

> **وجود Dependency به Repository به تنهایی دلیل ایجاد Domain Service نیست.**

Domain Service فقط زمانی ایجاد می‌شود که طبق ADR-0014 و ADR-0019 یک Business Rule واقعی وجود داشته باشد که:

```text
به یک Aggregate منفرد تعلق ندارد
```

و:

```text
صرفاً Application Orchestration ساده نیست.
```

---

# 11. Framework Boundary

مرز Framework در معماری پروژه به شکل زیر تعریف می‌شود:

```text
┌──────────────────────────────────────┐
│          Framework Boundary          │
│                                      │
│  Spring Boot                         │
│  Spring Configuration                │
│  Dependency Injection                │
│  ApplicationContext                  │
│                                      │
│  ledger-bootstrap                    │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│            Presentation              │
│                                      │
│     Spring MVC / REST Controllers    │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│            Application               │
│                                      │
│              Plain Java              │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│              Domain                 │
│                                      │
│              Plain Java              │
└──────────────────────────────────────┘
```

Infrastructure نیز در مرز بیرونی سیستم قرار دارد و در صورت نیاز می‌تواند Framework-aware باشد.

---

# 12. Dependency Direction

Dependency Direction پروژه باید به صورت زیر درک شود:

```text
             Bootstrap
            /    |     \
           /     |      \
          ▼      ▼       ▼
 Infrastructure Application Presentation
        │            │
        │            ▼
        └──────────> Domain
```

اما اصل Dependency Rule:

```text
Domain
   ▲
   │
Application
   ▲
   │
Infrastructure / Presentation / Bootstrap
```

به این معنی است که:

```text
Domain
```

نباید به:

```text
Infrastructure
Presentation
Spring
```

وابسته شود.

همچنین:

```text
Bootstrap
```

می‌تواند همه‌ی لایه‌های لازم برای Composition را بشناسد.

بنابراین Bootstrap یک Composition Dependency دارد، نه Business Dependency.

---

# 13. Constructor Injection

Dependencyهای اجباری باید از طریق Constructor دریافت شوند.

برای مثال:

```java
public CreateAccountHeadingService(
        AccountHeadingRepository repository,
        AccountHeadingDomainService domainService
) {
    this.repository = Objects.requireNonNull(repository);
    this.domainService = Objects.requireNonNull(domainService);
}
```

مزایا:

```text
Explicit Dependencies
+
Immutable Dependencies
+
Fail-Fast Construction
+
Easy Unit Testing
```

همچنین Business Object می‌تواند بدون Spring ساخته شود:

```java
AccountHeadingRepository repository =
        new InMemoryAccountHeadingRepository();

AccountHeadingDomainService domainService =
        new AccountHeadingDomainService(repository);

CreateAccountHeadingService service =
        new CreateAccountHeadingService(
                repository,
                domainService
        );
```

---

# 14. Unit Test بدون Spring

یکی از اهداف اصلی این ADR تست Business Logic بدون Spring است.

برای مثال:

```java
@Test
void shouldCreateAccountHeading() {

    AccountHeadingRepository repository =
            new InMemoryAccountHeadingRepository();

    AccountHeadingDomainService domainService =
            new AccountHeadingDomainService(repository);

    CreateAccountHeadingService service =
            new CreateAccountHeadingService(
                    repository,
                    domainService
            );

    ...
}
```

در تست‌های Domain و Application نباید به صورت پیش‌فرض نیاز به:

```text
@SpringBootTest
ApplicationContext
Spring Context
```

وجود داشته باشد.

Integration Testهای Framework-aware طبیعتاً می‌توانند Spring Context داشته باشند.

---

# 15. Composition Root نباید Business Logic داشته باشد

Configuration نباید محل Business Rule باشد.

این طراحی نامناسب است:

```java
@Configuration
public class AccountHeadingConfiguration {

    @Bean
    public CreateAccountHeadingService service(...) {

        if (...) {
            // Business Rule
        }

        ...
    }
}
```

Composition Root فقط باید:

```text
Create
Configure
Connect
```

کند.

بنابراین:

```text
Bootstrap
    ↓
Object Construction

Application
    ↓
Use Case

Domain
    ↓
Business Rule
```

---

# 16. Presentation Layer

Presentation می‌تواند Framework-aware باشد.

برای مثال:

```java
@RestController
@RequestMapping("/account-headings")
public class AccountHeadingController {

    private final CreateAccountHeadingService createService;

    public AccountHeadingController(
            CreateAccountHeadingService createService
    ) {
        this.createService = createService;
    }
}
```

استفاده از:

```text
@RestController
@Controller
@RequestMapping
```

در Presentation مجاز است.

زیرا Presentation یک Adapter در مرز سیستم است.

اما:

```text
AccountHeading
CreateAccountHeadingService
AccountHeadingDomainService
```

نباید Spring را بشناسند.

---

# 17. قانون Spring Annotation

قانون پیش‌فرض پروژه:

| Layer                       | Spring Annotation                     |
| --------------------------- | ------------------------------------- |
| Domain Aggregate            | ❌ ممنوع                               |
| Domain Entity               | ❌ ممنوع                               |
| Value Object                | ❌ ممنوع                               |
| Domain Service              | ❌ ممنوع                               |
| Domain Repository Interface | ❌ ممنوع                               |
| Domain Event                | ❌ ممنوع                               |
| Application Service         | ❌ ممنوع                               |
| Application Policy          | ❌ ممنوع                               |
| Application DTO             | ❌ Framework-specific Annotation ممنوع |
| Infrastructure              | ✅ در صورت نیاز مجاز                   |
| Presentation Controller     | ✅ مجاز                                |
| Bootstrap Configuration     | ✅ مجاز                                |
| Bootstrap Bean Definition   | ✅ مجاز                                |

بنابراین:

```java
@Service
public class CreateAccountHeadingService {
}
```

مورد قبول نیست.

اما:

```java
public class CreateAccountHeadingService {
}
```

مورد قبول است.

و:

```java
@Configuration
public class AccountHeadingConfiguration {

    @Bean
    public CreateAccountHeadingService createAccountHeadingService(...) {
        return new CreateAccountHeadingService(...);
    }
}
```

مورد قبول است.

---

# 18. Infrastructure Framework Dependency

Framework Independence به این معنی نیست که Infrastructure نیز الزاماً باید Framework-free باشد.

Infrastructure دقیقاً محل Integration با تکنولوژی‌های بیرونی است.

بنابراین این وابستگی‌ها مجاز هستند:

```text
Infrastructure
    │
    ├── Spring Data
    ├── JPA
    ├── Hibernate
    ├── Kafka
    ├── Redis
    └── HTTP Clients
```

اما:

```text
Domain
    ❌ Hibernate
    ❌ Spring Data
    ❌ Kafka
    ❌ Redis
```

و:

```text
Application
    ❌ Spring Data
    ❌ Hibernate
    ❌ Kafka
    ❌ Redis
```

مگر اینکه ADR جداگانه‌ای صراحتاً استثنا ایجاد کند.

---

# 19. مثال کامل AccountHeading

Object Graph:

```text
                 GeneralLedgerApplication
                           │
                           ▼
               AccountHeadingConfiguration
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
   Repository        Domain Service    Application Service
          │                │                │
          │                │                ├── Create
          │                │                ├── Update
          │                │                ├── Delete
          │                │                └── Search
          │                │
          ▼                │
JpaAccountHeadingRepository│
                           │
                           ▼
                AccountHeadingDomainService
```

و Presentation:

```text
AccountHeadingController
          │
          ▼
CreateAccountHeadingService
          │
          ▼
AccountHeadingDomainService
          │
          ▼
AccountHeadingRepository
          │
          ▼
JpaAccountHeadingRepository
```

Spring فقط Object Graph را Compose و Manage می‌کند.

---

# 20. Alternatives Considered

## Alternative 1 — استفاده از `@Service` در Application

```java
@Service
public class CreateAccountHeadingService {
}
```

### رد شد.

دلایل:

* Application به Spring وابسته می‌شود.
* Business Layer با Framework Metadata ترکیب می‌شود.
* Framework Coupling افزایش می‌یابد.

---

## Alternative 2 — استفاده از `@Component` در Domain Service

```java
@Component
public class AccountHeadingDomainService {
}
```

### رد شد.

Domain Service بخشی از Domain Model است و نباید برای Lifecycle خود به Spring وابسته باشد.

---

## Alternative 3 — استفاده از `@Repository` در Domain Repository

```java
@Repository
public interface AccountHeadingRepository {
}
```

### رد شد.

Repository Interface یک Domain/Application Abstraction است.

Implementation مربوط به Persistence باید در Infrastructure قرار گیرد.

---

## Alternative 4 — Manual DI کامل در Main

```java
public static void main(String[] args) {

    ...
}
```

### رد شد.

دلایل:

* استفاده نامناسب از Spring Container
* افزایش مسئولیت Main
* پیچیده شدن Configuration
* دشوار شدن Environment Configuration
* دشوار شدن مدیریت Lifecycle

---

## Alternative 5 — Service Locator

```java
ApplicationContext.getBean(...);
```

### رد شد.

Business Object نباید Dependencyهای خود را از Container جستجو کند.

Dependency باید به Object داده شود.

---

# 21. قانون ایجاد Bean

قاعده:

> **هر Objectی که نیازمند Lifecycle مدیریت‌شده توسط Spring است، می‌تواند در Composition Root به Spring معرفی شود؛ اما این موضوع نباید باعث وابستگی Business Code به Spring شود.**

بنابراین:

```text
Plain Java Object
        │
        ▼
Composition Root
        │
        ▼
@Bean
        │
        ▼
Spring ApplicationContext
```

---

# 22. Scope تصمیم

این تصمیم برای موارد زیر اعمال می‌شود:

```text
Domain Entity
Aggregate
Value Object
Domain Service
Domain Repository Interface
Domain Event
Application Service
Application Policy
Application DTO
```

و برای تمام Bounded Contextهای پروژه معتبر است.

Presentation و Bootstrap از این قانون مستثنی هستند.

Infrastructure نیز می‌تواند Framework-aware باشد، مشروط بر اینکه Framework Dependency به Domain و Application نشت نکند.

---

# 23. مزایا

### Framework Independence

Domain و Application مستقل از Spring باقی می‌مانند.

### Testability

Business Logic بدون Spring قابل تست است.

### Reduced Coupling

Business Logic به Framework وابسته نمی‌شود.

### Explicit Dependencies

Dependencyهای Objectها در Constructor مشخص هستند.

### Clear Composition Root

محل ساخت Object Graph مشخص است.

### Separation of Concerns

Business Logic از Configuration جدا می‌شود.

### Framework Replaceability

تغییر Framework در آینده کم‌هزینه‌تر خواهد بود.

### Clean Domain Model

Domain Model با Annotationهای Framework آلوده نمی‌شود.

---

# 24. معایب و Trade-offها

### Configuration بیشتر

به جای:

```java
@Service
```

ممکن است نیاز به:

```java
@Bean
```

باشد.

### Boilerplate بیشتر

برای برخی Objectها Bean Definition ایجاد می‌شود.

### مدیریت Object Graph

Bootstrap باید Dependencyهای Objectها را به شکل صحیح Compose کند.

### نیاز به نظم بیشتر

Composition Root باید ساختارمند و قابل نگهداری باشد.

این هزینه‌ها در مقابل استقلال Domain و Application پذیرفته می‌شوند.

---

# 25. ارتباط با ADRهای قبلی

این ADR بر اساس تصمیمات قبلی ساخته می‌شود:

```text
ADR-0010
Domain Layer Structure
        │
        ▼
ADR-0011
Domain Model Architecture
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
ADR-0019
Domain Policy Placement Strategy
        │
        ▼
ADR-0020
Framework Independence &
Composition Root Strategy
```

ارتباط مفهومی:

```text
ADR-0019
Business Policy Placement
        │
        ▼
ADR-0020
Framework Boundary & Wiring
```

ADR-0019 مشخص می‌کند:

> Business Policy کجا قرار گیرد؟

ADR-0020 مشخص می‌کند:

> این Objectها چگونه بدون وابستگی به Framework ساخته و به سیستم متصل شوند؟

---

# 26. قانون عملی پروژه

برای هر کلاس جدید ابتدا مشخص شود:

```text
این کلاس متعلق به کدام Layer است؟
```

سپس:

### اگر Domain است

```text
Plain Java
No Spring
```

### اگر Application است

```text
Plain Java
No Spring
```

### اگر Infrastructure است

```text
Framework integration در صورت نیاز
```

### اگر Presentation است

```text
Framework-aware
```

### اگر Bootstrap است

```text
Spring-aware
Composition Root
```

---

# 27. قوانین معماری قابل تست

این تصمیم باید در صورت امکان توسط Architecture Testها enforce شود.

برای مثال:

```text
Domain
    must not depend on Spring

Application
    must not depend on Spring

Domain
    must not depend on Infrastructure

Application
    must not depend on Infrastructure

Infrastructure
    may depend on Domain/Application abstractions

Presentation
    may depend on Application

Bootstrap
    may depend on required outer layers
```

این قوانین باید در کنار ArchUnit یا ابزار مشابه بررسی شوند.

---

# 28. موارد خارج از محدوده (Out of Scope)

این ADR درباره موارد زیر تصمیم‌گیری نمی‌کند:

* انتخاب Spring Boot به عنوان Framework اصلی
* انتخاب Spring MVC
* انتخاب JPA
* انتخاب Hibernate
* Transaction Management
* Persistence Strategy
* Database Configuration
* Bean Scopeهای خاص
* Caching
* Security Configuration
* HTTP Adapter Design
* Messaging Adapter Design
* Deployment Strategy
* Kubernetes Configuration

این موارد در ADRهای مربوط به Infrastructure، Persistence، Security و Deployment بررسی خواهند شد.

---

# 29. رفرنس‌ها (References)

1. Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart of Software*

   * Domain Model
   * Layered Architecture
   * Separation of Domain Logic

2. Vaughn Vernon — *Implementing Domain-Driven Design*

   * Application Services
   * Domain Services
   * Infrastructure
   * Dependency Management

3. Robert C. Martin — *Clean Architecture*

   * Dependency Rule
   * Frameworks as Details
   * Dependency Inversion

4. Spring Framework Documentation

   * Java-based Configuration
   * `@Configuration`
   * `@Bean`
   * Dependency Injection

5. Spring Boot Documentation

   * Spring Beans
   * Dependency Injection
   * Component Scanning

---

# 30. تصمیم نهایی برای General Ledger

قاعده نهایی پروژه:

```text
                  Spring Framework
                         │
                         ▼
                  ledger-bootstrap
                         │
                  Composition Root
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
 Infrastructure     Application    Presentation
          │              │              │
          │              ▼              │
          └──────────> Domain <─────────┘
```

از نظر Dependency Rule:

```text
                 Domain
                ▲      ▲
                │      │
        Application   Infrastructure
                ▲      ▲
                │      │
             Presentation
                ▲
                │
             Bootstrap
```

و اصل کلیدی:

> **Framework باید در مرز سیستم باقی بماند و Business Logic نباید برای اجرای خود به Framework وابسته باشد.**

اصل دوم:

> **Application Service و Domain Service مسئول Use Case و Business Logic هستند؛ Spring مسئول Composition و Lifecycle است.**

اصل سوم:

> **Composition Root محل اتصال Interfaceها به Implementationها و محل ایجاد Object Graph سیستم است.**

اصل چهارم:

> **Dependency Injection باید Dependency را به Object بدهد؛ Business Object نباید Dependency را از Container جستجو کند.**

اصل پنجم:

> **Bootstrap نباید محل Business Logic باشد؛ فقط باید Object Graph سیستم را Compose کند.**

اصل ششم:

> **Infrastructure مجاز است Framework-aware باشد، اما Framework Dependency نباید به Domain و Application نشت کند.**

---

## وضعیت نهایی

**Accepted**

از این ADR به عنوان استاندارد معماری پروژه General Ledger برای تعیین:

```text
Framework Boundary
Dependency Injection
Composition Root
Application Service Wiring
Domain Service Wiring
Repository Wiring
Framework Independence
```

استفاده خواهد شد.

تمام Bounded Contextها و Aggregateهای جدید پروژه باید این تصمیم را رعایت کنند.

در نتیجه، Application Serviceهایی مانند:

```text
CreateAccountHeadingService
UpdateAccountHeadingService
DeleteAccountHeadingService
SearchAccountHeadingsService
```

به صورت Plain Java باقی می‌مانند و در Composition Root، واقع در:

```text
ledger-bootstrap
```

به Spring ApplicationContext متصل می‌شوند.

اصل نهایی:

```text
Business Logic
       │
       ▼
Domain / Application
       │
       │  Framework Independent
       ▼
Composition Root
       │
       ▼
Spring
       │
       ▼
Infrastructure / Presentation
```

````

### یک نکته خیلی مهم

با این اصلاح، **ADR-0019 و ADR-0020 خیلی خوب روی هم قفل می‌شوند**:

```text
ADR-0019
Policy کجا زندگی کند؟
        │
        ├── Aggregate
        ├── Application Service
        └── Domain Service
                    │
                    ▼
ADR-0020
این Object چطور ساخته و Wire شود؟
                    │
                    ▼
              Composition Root
                    │
                    ▼
                  Spring
````

یعنی **ADR-0019 درباره‌ی محل Business Logic است و ADR-0020 درباره‌ی نحوه‌ی اتصال آن Business Logic به Runtime**. این تفکیک به نظرم برای baseline معماری General Ledger خیلی تمیزتر است.
