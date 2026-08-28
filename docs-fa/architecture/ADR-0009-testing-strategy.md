# ADR-0009 — استراتژی تست (Testing Strategy)

* **وضعیت:** پذیرفته شده (Accepted)
* **تاریخ:** 2026-08-20
* **ADRهای مرتبط:**
  - ADR-0006
  - ADR-0007
  - ADR-0008



## زمینه (Context)

پروژه با استفاده از معماری **Clean Architecture**، رویکرد **Domain-Driven Design (DDD)** و ساختار **Modular Monolith** توسعه داده می‌شود.

برای حفظ کیفیت، جلوگیری از Regression و امکان توسعه پایدار، لازم است یک استراتژی تست استاندارد و چندلایه تعریف شود.

هدف این ADR تعیین انواع تست‌ها، مسئولیت هر لایه و ابزارهای مورد استفاده است.

---

## تصمیم (Decision)

استراتژی تست پروژه بر اساس **Test Pyramid** طراحی می‌شود و شامل لایه‌های زیر است:

### 1. Unit Test

بیشترین تعداد تست‌های پروژه باید از نوع Unit Test باشند.

موارد اصلی:

* منطق Domain باید به صورت کامل و مستقل تست شود.
* تست‌ها نباید به Database، Network یا Framework وابسته باشند.
* تست‌ها باید سریع و مستقل باشند.
* Mock فقط در صورت نیاز و عمدتاً در لایه‌های خارج از Domain استفاده شود.

**ابزارها:**

* JUnit 5
* AssertJ
* Mockito

---

### 2. Application Test

در این سطح، Use Caseهای سیستم و رفتار Application Layer تست می‌شوند.

موارد قابل بررسی:

* اجرای Command و Query
* تعامل Application با Domain
* مدیریت Transaction
* رفتار Use Caseها
* مدیریت خطاهای Application

هدف این تست‌ها بررسی رفتار Use Case بدون وابستگی غیرضروری به جزئیات Infrastructure است.

---

### 3. Integration Test

برای بررسی تعامل واقعی بین اجزای سیستم استفاده می‌شود.

موارد قابل تست:

* Repositoryها
* JPA Mapping
* Transactionها
* Database
* Infrastructure Components
* Integration بین Moduleها

در صورت نیاز از **Testcontainers** برای اجرای وابستگی‌های واقعی مانند Database استفاده می‌شود.

---

### 4. API Test

برای تست لایه Presentation و REST API استفاده می‌شود.

موارد قابل بررسی:

* Request Validation
* HTTP Status Code
* Response Body
* Serialization / Deserialization
* Error Handling
* API Contract

**ابزارهای پیشنهادی:**

* Spring MockMvc
* RestAssured

---

### 5. Architecture Test

قوانین معماری باید به صورت خودکار توسط تست‌های معماری بررسی شوند.

موارد قابل بررسی:

* Dependency Direction
* قوانین Clean Architecture
* قوانین Module Dependency
* وابستگی‌های Domain
* محدودیت وابستگی به Infrastructure
* قوانین مربوط به Bounded Contextها و Moduleها

**ابزار:**

* ArchUnit

تست‌های معماری بخشی از تست‌های خودکار پروژه هستند و باید در CI اجرا شوند.

---

### 6. End-to-End Test

تعداد محدودی تست End-to-End برای سناریوهای حیاتی سیستم ایجاد می‌شود.

این تست‌ها باید رفتار سیستم را از نقطه ورود تا نتیجه نهایی بررسی کنند.

نمونه سناریوها:

* ایجاد حساب
* ثبت سند
* انجام تراکنش
* تولید گزارش

تعداد E2E Testها باید محدود باشد تا زمان اجرای Pipeline بیش از حد افزایش پیدا نکند.

---

## Test Pyramid

ترتیب و تعداد تست‌ها به صورت زیر خواهد بود:

```text
             ┌─────────────────┐
             │   E2E Tests     │
             │     کمترین      │
             └─────────────────┘
           ┌─────────────────────┐
           │ Integration / API   │
           │       متوسط         │
           └─────────────────────┘
        ┌───────────────────────────┐
        │       Unit Tests          │
        │        بیشترین            │
        └───────────────────────────┘
```

اصل کلی این است که هرچه تست به سطح بالاتری از سیستم نزدیک شود، تعداد آن کمتر و هزینه اجرای آن بیشتر باشد.

---

## قوانین تست (Testing Rules)

* هر قابلیت جدید باید دارای تست مناسب باشد.
* هر Bug باید قبل از اصلاح با یک تست بازتولید شود.
* منطق تجاری بدون Unit Test پذیرفته نمی‌شود.
* تست‌ها باید مستقل و قابل تکرار (Repeatable) باشند.
* ترتیب اجرای تست‌ها نباید روی نتیجه آن‌ها تأثیر داشته باشد.
* تست‌ها نباید به داده‌های محیط Development یا Production وابسته باشند.
* تست‌ها نباید برای اجرای موفق به سرویس‌های خارجی واقعی وابسته باشند؛ مگر در تست‌های Integration یا E2E که صراحتاً برای این منظور تعریف شده‌اند.
* تست‌های معماری باید در CI اجرا شوند.
* Pull Request نباید در صورت شکست تست‌های الزامی Merge شود.

---

## پوشش تست (Test Coverage)

Coverage یک معیار کمکی برای کیفیت تست‌ها محسوب می‌شود و نباید تنها معیار کیفیت تست باشد.

حداقل اهداف اولیه پروژه:

| نوع کد             |                هدف Coverage |
| ------------------ | --------------------------: |
| Domain             |                       ≥ 90٪ |
| Application        |                       ≥ 80٪ |
| Infrastructure     |        بر اساس اهمیت و ریسک |
| API                |  تمام Endpointهای Critical |
| Architecture Rules | پوشش تمامی قوانین تعریف‌شده |

Coverage باید در کنار کیفیت سناریوها و رفتارهای تست‌شده ارزیابی شود.

---

## پیامدها (Consequences)

### مزایا

* کاهش Regression
* افزایش اطمینان هنگام Refactoring
* افزایش کیفیت کد
* جلوگیری از نقض معماری
* مستندسازی رفتار سیستم توسط تست‌ها
* کاهش وابستگی به تست‌های دستی
* امکان توسعه مستقل Moduleها

### معایب

* افزایش زمان توسعه اولیه
* نیاز به نگهداری تست‌ها
* افزایش زمان اجرای CI/CD
* نیاز به طراحی صحیح Test Infrastructure
* پیچیدگی بیشتر در تست Integration و E2E

---

## وضعیت اجرا (Implementation Status)

این استراتژی برای تمامی Moduleهای پروژه الزامی است.

هر Module باید بر اساس مسئولیت و نوع تعاملات خود، Testهای متناسب را داشته باشد. Unit Test برای Business Logic الزامی است و سایر Testها بر اساس نیاز Module تعریف می‌شوند.

تست‌های مربوط به هر Pull Request باید قبل از Merge با موفقیت اجرا شوند.

---

## References

1. Martin Fowler — **Test Pyramid**
2. Martin Fowler — **The Practical Test Pyramid**
3. Robert C. Martin — **Clean Architecture: A Craftsman's Guide to Software Structure and Design**
4. Vladimir Khorikov — **Unit Testing Principles, Practices, and Patterns**
5. Eric Evans — **Domain-Driven Design: Tackling Complexity in the Heart of Software**
6. ArchUnit — **Architecture Testing Documentation**
7. JUnit 5 — **JUnit 5 User Guide**
8. Testcontainers — **Testcontainers Documentation**
