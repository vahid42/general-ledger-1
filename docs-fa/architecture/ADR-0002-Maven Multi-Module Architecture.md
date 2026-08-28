# ADR-0002: Maven Multi-Module Architecture

* **Status:** Accepted
* **Date:** 2026-08-20
* **ADRهای مرتبط:** ADR-0004

## Context

پروژه `general-ledger` یک سیستم نرم‌افزاری با چندین بخش مستقل است که هر بخش مسئولیت مشخصی در معماری سیستم دارد.

در ساختار پروژه، لایه‌ها و مسئولیت‌های اصلی شامل موارد زیر هستند:

* Domain
* Application
* Infrastructure
* Presentation
* Bootstrap
* Test

قرار دادن تمام این بخش‌ها در یک Maven Module باعث می‌شود مرزبندی بین مسئولیت‌ها صرفاً در سطح Package باقی بماند.

در چنین ساختاری، امکان ایجاد وابستگی‌های ناخواسته بین بخش‌های مختلف وجود دارد. برای مثال، کد Domain می‌تواند به صورت مستقیم به Infrastructure وابسته شود، بدون اینکه Maven بتواند این وابستگی را در سطح Module کنترل کند.

همچنین Build، Dependency Management و Lifecycle تمام بخش‌ها در یک Module انجام خواهد شد که استقلال بخش‌های مختلف سیستم را کاهش می‌دهد.

بنابراین نیاز است ساختار Maven پروژه نیز با مرزبندی معماری سیستم هم‌راستا باشد.

## Decision

پروژه `general-ledger` به صورت **Maven Multi-Module Project** پیاده‌سازی خواهد شد.

پروژه Root با نام:

```text
general-ledger
```

به عنوان Maven Aggregator و Parent پروژه عمل خواهد کرد و خود آن شامل کد اجرایی نخواهد بود.

ساختار Moduleها به صورت زیر خواهد بود:

```text
general-ledger
│
├── ledger-domain
├── ledger-application
├── ledger-infrastructure
├── ledger-presentation
├── ledger-test
└── ledger-bootstrap
```

### ledger-domain

مسئول مدل و قوانین اصلی کسب‌وکار سیستم است.

این Module نباید به تکنولوژی‌های Infrastructure یا Frameworkهای اجرایی وابسته باشد.

```text
ledger-domain
```

### ledger-application

مسئول اجرای Use Caseهای سیستم و orchestration بین Domain و Portها است.

این Module می‌تواند به `ledger-domain` وابسته باشد، اما نباید به Infrastructure وابستگی مستقیم داشته باشد.

```text
ledger-application
        │
        ▼
ledger-domain
```

### ledger-infrastructure

مسئول پیاده‌سازی جزئیات فنی سیستم است، از جمله:

* Persistence
* Database
* External Services
* Messaging
* سایر جزئیات فنی

این Module می‌تواند به Application و Domain وابسته باشد.

```text
ledger-infrastructure
        │
        ├── ledger-application
        │        │
        │        ▼
        │   ledger-domain
        │
        └── ledger-domain
```

### ledger-presentation

مسئول تعامل سیستم با مصرف‌کنندگان خارجی است، مانند:

* REST API
* Request/Response
* Controller
* Validation مربوط به ورودی API
* Mapping بین DTO و Application Layer

Presentation نباید مسئول اجرای Business Logic باشد.

### ledger-bootstrap

نقطه ورود اجرای برنامه است و مسئول راه‌اندازی Spring Boot و Composition Root خواهد بود.

این Module شامل:

* `main` application
* Spring Boot configuration
* Application startup configuration

خواهد بود.

### ledger-test

محل تست‌های مستقل معماری و تست‌هایی است که نیاز به یک Module جداگانه دارند.

این Module می‌تواند برای مواردی مانند:

* Architecture Tests
* ArchUnit
* Cross-module tests

استفاده شود.

## Module Isolation

هر Module باید `pom.xml` مستقل خود را داشته باشد و Dependencyهای موردنیاز خود را به صورت صریح تعریف کند.

وابستگی یک Module به Module دیگر تنها در صورتی مجاز است که در معماری سیستم تعریف شده باشد.

بنابراین ایجاد وابستگی صرفاً برای دسترسی آسان به کلاس‌های Module دیگر مجاز نیست.

## Consequences

### Positive

* مرزبندی معماری در سطح Maven نیز enforce می‌شود.
* Dependencyهای هر بخش به صورت صریح مشخص هستند.
* امکان ایجاد وابستگی‌های ناخواسته کاهش پیدا می‌کند.
* Build و Lifecycle هر Module قابل مدیریت است.
* تغییرات Infrastructure تأثیر مستقیمی بر Domain نخواهد داشت.
* Domain می‌تواند مستقل از Frameworkها توسعه و تست شود.
* ساختار پروژه با معماری سیستم هم‌راستا خواهد بود.
* امکان توسعه و تست مستقل بخش‌های مختلف فراهم می‌شود.

### Negative

* تعداد `pom.xml`ها افزایش پیدا می‌کند.
* مدیریت Dependencyها نسبت به پروژه Single-Module پیچیده‌تر می‌شود.
* Build پروژه ممکن است در پروژه‌های بزرگ زمان بیشتری نیاز داشته باشد.
* تغییرات بین Moduleها نیازمند توجه به Dependency Graph است.

## Dependency Principle

Maven Multi-Module بودن به تنهایی تضمین‌کننده معماری صحیح نیست.

بنابراین علاوه بر تفکیک Moduleها، Dependency Direction باید به صورت مستقل تعریف و کنترل شود.

جزئیات Dependency Direction در:

```text
ADR-0004: Module Dependency Direction
```

مشخص خواهد شد.

## Resulting Structure

ساختار مورد قبول پروژه:

```text
general-ledger/
│
├── pom.xml
│
├── ledger-domain/
│   └── pom.xml
│
├── ledger-application/
│   └── pom.xml
│
├── ledger-infrastructure/
│   └── pom.xml
│
├── ledger-presentation/
│   └── pom.xml
│
├── ledger-test/
│   └── pom.xml
│
└── ledger-bootstrap/
    └── pom.xml
```

# References

### 1. Apache Maven — *Introduction to the POM*

مبنای تصمیم‌های مربوط به `pom.xml`، Parent و Dependency Management.

### 2. Apache Maven — *Guide to Working with Multiple Modules*

مبنای استفاده از Maven Multi-Module و ساختار Parent/Child Moduleها.

### 3. Robert C. Martin — *Clean Architecture*

مبنای Separation of Concerns، Dependency Rule و جهت وابستگی بین لایه‌های معماری.

### 4. Robert C. Martin — *Agile Software Development, Principles, Patterns, and Practices*

مبنای اصول Dependency Inversion و مدیریت وابستگی بین Moduleها و Packageها.

### 5. Mark Richards, Neal Ford — *Fundamentals of Software Architecture*

مبنای تصمیم‌های مرتبط با Modularity، Coupling و ساختار وابستگی‌های معماری.
