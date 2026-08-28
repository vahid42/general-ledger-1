# General Ledger — Domain & Bounded Context Decision Baseline

> **Purpose:**
> این سند مبنای تصمیم‌گیری معماری برای طراحی Domain، Subdomain، Bounded Context، Microservice و Aggregateهای سیستم **General Ledger** است.
> در تصمیم‌های بعدی معماری، ابتدا این سند باید به‌عنوان baseline در نظر گرفته شود.

---

## 1. Problem Space

فضای مسئله محصول، **General Ledger (دفترکل)** است.

در سطح Strategic DDD، دفترکل از سه Subdomain اصلی تشکیل می‌شود:

```text
General Ledger
│
├── Account Head / سرفصل
├── Account / حساب
└── Journal Entry / سند
```

این سه مورد در مجموع قابلیت‌های اصلی حوزه دفترکل را تشکیل می‌دهند.

---

## 2. Subdomain Classification

سه Subdomain از نظر ارزش کسب‌وکاری یکسان نیستند.

### 2.1 Core Subdomain — Journal Entry

**Journal Entry / سند** به‌عنوان Core Subdomain شناخته می‌شود.

دلیل:

* منطق اصلی و متمایزکننده محصول در نحوه ایجاد، اعتبارسنجی و ثبت سند قرار دارد.
* Journal Entry بخش اصلی مزیت رقابتی سیستم محسوب می‌شود.
* پیچیدگی اصلی Business Ruleهای دفترکل در فرآیند ثبت و مدیریت سند متمرکز است.
* سرمایه‌گذاری معماری و طراحی Domain باید بیشترین تمرکز را روی این بخش داشته باشد.

```text
General Ledger
│
├── Core Subdomain ⭐
│   └── Journal Entry
│
├── Supporting Subdomain
│   └── Account
│
└── Supporting Subdomain
    └── Account Head
```

### 2.2 Supporting Subdomain — Account

Account یک Supporting Subdomain است.

مسئولیت‌های اصلی آن شامل مواردی مانند:

* هویت حساب
* وضعیت حساب
* نوع حساب
* ویژگی‌های حساب
* اطلاعات لازم برای استفاده حساب در فرآیندهای دفترکل

است.

### 2.3 Supporting Subdomain — Account Head

Account Head نیز یک Supporting Subdomain است.

مسئولیت‌های اصلی آن شامل مواردی مانند:

* ساختار سرفصل‌ها
* طبقه‌بندی
* سلسله‌مراتب سرفصل‌ها
* تعیین جایگاه حساب در ساختار دفترکل

است.

---

# 3. Subdomain ≠ Bounded Context

وجود سه Subdomain به‌صورت خودکار به معنی وجود سه Bounded Context نیست.

این تمایز باید در تمام تصمیم‌های معماری حفظ شود:

```text
Subdomain
    ≠
Bounded Context
    ≠
Module
    ≠
Microservice
```

Subdomain یک مفهوم **Strategic DDD** است و نشان‌دهنده بخشی از فضای مسئله و قابلیت کسب‌وکار است.

Bounded Context یک مرز **مدل و زبان دامنه** است.

Module یک مرز **ساختاری/کدی** داخل سیستم است.

Microservice یک مرز **استقرار و اجرای مستقل** است.

---

# 4. Bounded Context Decision

در طراحی فعلی، تصمیم این است که سه Subdomain:

* Account Head
* Account
* Journal Entry

در یک مدل یکپارچه دفترکل قرار بگیرند و در نتیجه در یک:

```text
General Ledger Bounded Context
```

قرار داشته باشند.

دلیل این تصمیم:

1. هر سه بخش متعلق به یک Problem Space واحد هستند.
2. زبان دامنه آنها به‌شدت به یکدیگر مرتبط است.
3. Journal Entry برای ثبت سند به Account نیاز دارد.
4. Account در ساختار دفترکل و Account Head معنا پیدا می‌کند.
5. ایجاد Bounded Context مستقل صرفاً به دلیل وجود Subdomain، بدون وجود مرز مدل واقعی، پیچیدگی غیرضروری ایجاد می‌کند.
6. مرز اصلی مدل فعلی، خود General Ledger است.

بنابراین:

```text
General Ledger
└── Bounded Context
    ├── Account Head
    ├── Account
    └── Journal Entry
```

---

# 5. Microservice Decision

Bounded Context دفترکل به‌صورت یک Microservice مستقل پیاده‌سازی می‌شود:

```text
General Ledger Bounded Context
                │
                ▼
      General Ledger Microservice
```

بنابراین در معماری فعلی:

```text
1 Bounded Context
        ↓
1 Microservice
```

اما این یک **تصمیم معماری این سیستم** است، نه یک قانون عمومی که:

```text
Bounded Context = Microservice
```

در همه سیستم‌ها باشد.

Microservice مرز deployment و runtime است، در حالی که Bounded Context مرز مدل دامنه است.

---

# 6. Tactical DDD Structure

در سطح Tactical DDD، General Ledger Microservice شامل سه Aggregate Root اصلی است:

```text
General Ledger Microservice
│
├── Account Head Aggregate
│   └── AccountHead
│
├── Account Aggregate
│   └── Account
│
└── Journal Entry Aggregate
    └── JournalEntry
```

بنابراین:

```text
General Ledger
│
├── AccountHead Aggregate Root
├── Account Aggregate Root
└── JournalEntry Aggregate Root
```

---

# 7. Aggregate Root Responsibility

## 7.1 AccountHead Aggregate

مسئول حفظ invariants مربوط به ساختار سرفصل است.

نمونه مسئولیت‌ها:

```text
AccountHead
├── identity
├── hierarchy
├── classification
└── placement rules
```

---

## 7.2 Account Aggregate

مسئول حفظ invariants مربوط به حساب است.

نمونه مسئولیت‌ها:

```text
Account
├── identity
├── account type
├── status
└── account rules
```

---

## 7.3 JournalEntry Aggregate

**مهم‌ترین Aggregate از نظر ارزش دامنه** است.

نمونه مسئولیت‌ها:

```text
JournalEntry
├── validate debit / credit
├── validate balancing
├── validate journal lines
├── validate posting rules
├── determine posting state
└── post
```

منطق اصلی و متمایزکننده دفترکل باید تا حد امکان در این Aggregate و مدل دامنه مرتبط با آن قرار گیرد.

---

# 8. Core Domain Must Remain Domain-Centric

Core بودن Journal Entry فقط یک برچسب نیست.

این تصمیم باید در معماری نیز منعکس شود.

نباید Journal Entry صرفاً به یک:

```text
CRUD Service
```

تبدیل شود.

یا Business Ruleهای آن در:

```text
Controller
Application Service
Repository
Infrastructure
```

پراکنده شوند.

اولویت باید این باشد:

```text
JournalEntry Aggregate
        │
        ▼
Domain Rules
        │
        ▼
Business Invariants
```

---

# 9. Aggregate ≠ Domain Service

وجود سه Aggregate Root به معنی وجود سه Domain Service نیست.

یعنی نباید صرفاً به دلیل داشتن:

```text
AccountHead
Account
JournalEntry
```

به‌صورت خودکار این Serviceها را ایجاد کنیم:

```text
AccountHeadService
AccountService
JournalEntryService
```

به‌عنوان محل اصلی Business Logic.

اصل تصمیم:

```text
Business Rule
     │
     ▼
Aggregate
```

اگر Business Rule ذاتاً بین چند Aggregate قرار داشته باشد و قرار دادن آن در یک Aggregate باعث ایجاد coupling نامناسب شود، در آن شرایط Domain Service قابل استفاده است.

---

# 10. Aggregate Interaction

Aggregateها باید تا حد امکان مستقل باقی بمانند.

به‌عنوان نمونه، این طراحی باید با احتیاط انجام شود:

```text
JournalEntry
      │
      ▼
AccountRepository
```

Domain Entity یا Aggregate نباید مستقیماً Repository را صدا بزند.

بهتر است orchestration در Application Layer انجام شود.

برای مثال:

```text
CreateJournalEntry
        │
        ├── validate Account
        │
        ├── validate AccountHead
        │
        └── create JournalEntry
```

در این مدل Application Layer مسئول هماهنگی است، ولی Business Ruleهای خود Journal Entry داخل Domain باقی می‌مانند.

---

# 11. Repository Ports

برای هر Aggregate می‌توان Repository Port مستقل تعریف کرد:

```text
AccountHeadRepository
AccountRepository
JournalEntryRepository
```

این Repositoryها Port هستند و implementation آنها در Infrastructure قرار می‌گیرد.

ساختار کلی:

```text
Application / Domain
        │
        ▼
Repository Port
        │
        ▼
Infrastructure
        │
        ▼
Database
```

Aggregate نباید وابسته به implementation مربوط به persistence باشد.

---

# 12. Internal Contract / Port ≠ Repository Port

Repository Port فقط یکی از انواع Portها است.

نباید هر Interface داخل Domain/Application را Repository در نظر گرفت.

دو مفهوم باید جدا باقی بمانند:

```text
Repository Port
    │
    └── Persistence abstraction

Internal Contract / Port
    │
    └── Communication / capability abstraction
```

این تفکیک از ایجاد وابستگی اشتباه بین Domain و Infrastructure جلوگیری می‌کند.

---

# 13. Module Structure

در سطح کد، هر Aggregate/Business Boundary می‌تواند Module ساختاری خود را داشته باشد، بدون اینکه الزاماً Bounded Context یا Microservice مستقل باشد.

مدل پیشنهادی:

```text
general-ledger
│
├── account-head
│
├── account
│
└── journal-entry
```

این Moduleها همگی متعلق به یک:

```text
General Ledger Bounded Context
```

و یک:

```text
General Ledger Microservice
```

هستند.

---

# 14. Architectural Hierarchy

مدل نهایی تصمیم به‌صورت زیر است:

```text
Problem Space
│
▼
General Ledger
│
├── Subdomain: Account Head
├── Subdomain: Account
└── Subdomain: Journal Entry ⭐ Core
        │
        ▼
Bounded Context
│
└── General Ledger Bounded Context
        │
        ▼
Microservice
│
└── General Ledger Microservice
        │
        ├── Module: Account Head
        │       └── AccountHead Aggregate
        │
        ├── Module: Account
        │       └── Account Aggregate
        │
        └── Module: Journal Entry
                └── JournalEntry Aggregate
```

---

# 15. Decision Summary

تصمیم فعلی معماری General Ledger:

| سطح                    | تصمیم                                |
| ---------------------- | ------------------------------------ |
| Problem Space          | General Ledger                       |
| Subdomainها            | Account Head، Account، Journal Entry |
| Core Subdomain         | Journal Entry                        |
| Supporting Subdomainها | Account Head، Account                |
| Bounded Context        | General Ledger                       |
| Microservice           | General Ledger                       |
| Aggregate Rootها       | AccountHead، Account، JournalEntry   |
| Core Aggregate         | JournalEntry                         |
| Moduleها               | Account Head، Account، Journal Entry |
| Repositoryها           | Aggregate-oriented                   |
| Domain Logic           | داخل Aggregateها و Domain Model      |
| Orchestration          | Application Layer                    |
| Persistence            | Infrastructure                       |

---

# 16. Rules for Future Decisions

هر تصمیم معماری جدید درباره General Ledger باید ابتدا این سؤالات را بررسی کند:

### Rule 1 — Subdomain را با Bounded Context اشتباه نگیریم

وجود یک Subdomain جدید به‌تنهایی دلیل ایجاد Bounded Context جدید نیست.

### Rule 2 — Bounded Context را با Microservice یکی ندانیم

در این پروژه General Ledger به‌صورت یک Microservice مستقل deploy می‌شود، اما این رابطه یک تصمیم معماری است، نه قانون DDD.

### Rule 3 — Core Domain باید واقعاً Core باقی بماند

Journal Entry مهم‌ترین بخش مزیت رقابتی سیستم است؛ بنابراین Business Logic آن نباید به CRUD یا Serviceهای صرفاً orchestration تبدیل شود.

### Rule 4 — Aggregate مرز Consistency است

Aggregate Root باید مسئول حفظ Invariantهای خودش باشد.

### Rule 5 — Aggregateها نباید Repository را صدا بزنند

Persistence از طریق Port و خارج از Aggregate مدیریت می‌شود.

### Rule 6 — Application Layer هماهنگ‌کننده است

Application Layer می‌تواند چند Aggregate را برای اجرای یک Use Case هماهنگ کند، اما نباید مالک Business Ruleهای Domain شود.

### Rule 7 — Supporting Subdomainها نباید Core Domain را تحت‌الشعاع قرار دهند

Account و Account Head باید نیازهای Core Domain را پشتیبانی کنند، بدون اینکه طراحی آنها باعث پیچیده یا وابسته شدن غیرضروری Journal Entry شود.

---

# 17. Final Mental Model

مدل ذهنی نهایی برای تصمیم‌های بعدی:

```text
                         GENERAL LEDGER
                         Problem Space
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
        Account Head       Account       Journal Entry
        Supporting        Supporting          CORE
        Subdomain         Subdomain         Subdomain
              │               │               │
              └───────────────┼───────────────┘
                              │
                              ▼
                   General Ledger
                    Bounded Context
                              │
                              ▼
                   General Ledger
                     Microservice
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
        AccountHead        Account        JournalEntry
         Aggregate        Aggregate        Aggregate
```

**اصل کلیدی:**

> ما سه Subdomain داریم، اما الزاماً سه Bounded Context نداریم.
> در طراحی فعلی، هر سه Subdomain در یک General Ledger Bounded Context قرار دارند و این Bounded Context به‌صورت یک General Ledger Microservice مستقل پیاده‌سازی می‌شود.
> در سطح Tactical DDD نیز سه Aggregate Root اصلی داریم: `AccountHead`، `Account` و `JournalEntry`؛ که `JournalEntry` Core Aggregate و مرکز منطق رقابتی سیستم است.

---

## 18. Status

**Status:** Accepted as the current architectural baseline.

این سند باید قبل از تصمیم‌گیری‌های بعدی درباره:

* Bounded Context
* Microservice boundaries
* Module boundaries
* Aggregate boundaries
* Repository design
* Domain Service
* Application Service
* Inter-module communication

به‌عنوان baseline بررسی شود.

در صورت تغییر این تصمیم‌ها، ابتدا باید دلیل تغییر در قالب یک تصمیم معماری جدید ثبت شود و سپس این baseline به‌روزرسانی گردد.
