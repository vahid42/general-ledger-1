# طراحی DDD استراتژیک و تاکتیکی

## ۱. هدف

پروژه `general-ledger` بر اساس اصول Domain-Driven Design (DDD) طراحی می‌شود.

در طراحی این پروژه، دو دیدگاه DDD از یکدیگر تفکیک می‌شوند:

* DDD استراتژیک (Strategic DDD)
* DDD تاکتیکی (Tactical DDD)

این دو دیدگاه نباید با یکدیگر اشتباه گرفته شوند.

---

# ۲. DDD استراتژیک

DDD استراتژیک درباره شناخت حوزه کسب‌وکار، مرزهای کسب‌وکار و تقسیم‌بندی آن به Subdomainها است.

## ۲.۱ حوزه کسب‌وکار (Business Domain)

حوزه اصلی کسب‌وکار پروژه:

```text
Accounting
حسابداری
```

---

# ۳. Bounded Context

در این پروژه، یکی از Bounded Contextهای اصلی:

```text
General Ledger
```

دفترکل است.

نمای کلی:

```text
Accounting Domain
       │
       ▼
General Ledger
       │
       ▼
General Ledger Bounded Context
```

Bounded Context مشخص‌کننده مرز مدل دامنه و زبان مشترک مربوط به دفترکل است.

---

# ۴. Subdomainها

در Bounded Context مربوط به دفترکل، در مدل فعلی سه حوزه کسب‌وکاری اصلی در نظر گرفته شده است:

```text
General Ledger Bounded Context
│
├── Account Structure
│
├── Account Management
│
└── Journal & Posting
```

معادل فارسی:

```text
General Ledger
│
├── ساختار سرفصل‌ها
│
├── مدیریت حساب‌ها
│
└── سند و ثبت حسابداری
```

---

# ۵. دسته‌بندی Subdomainها

در مدل فعلی پروژه، Subdomainها به شکل زیر دسته‌بندی می‌شوند:

| Subdomain          | نوع        | مسئولیت                              |
| ------------------ | ---------- | ------------------------------------ |
| Account Structure  | Supporting | مدیریت ساختار و سلسله‌مراتب سرفصل‌ها |
| Account Management | Supporting | مدیریت حساب‌های واقعی                |
| Journal & Posting  | Core       | مدیریت اسناد و عملیات ثبت حسابداری   |

---

# ۶. Core Subdomain

## Journal & Posting

در مدل فعلی، `Journal & Posting` به عنوان Core Subdomain در نظر گرفته می‌شود.

این بخش شامل قوانین اصلی کسب‌وکار حسابداری است، از جمله:

* ایجاد سند حسابداری
* ثبت بدهکار و بستانکار
* اعتبارسنجی سند
* کنترل توازن بدهکار و بستانکار
* ثبت سند
* Posting
* اعمال اثر سند روی حساب‌ها
* کنترل قوانین مربوط به ثبت حسابداری

به دلیل اهمیت این بخش، بیشترین تمرکز در طراحی Business Ruleها روی این Subdomain خواهد بود.

---

# ۷. Supporting Subdomain

## ۷.۱ Account Structure

این Subdomain مسئول تعریف ساختار سرفصل‌های حسابداری است.

نمونه:

```text
دارایی‌ها
│
├── دارایی‌های جاری
│   ├── موجودی نقد
│   └── بانک
│
└── دارایی‌های غیرجاری

بدهی‌ها
│
├── بدهی‌های جاری
└── بدهی‌های بلندمدت

حقوق مالکانه

درآمدها

هزینه‌ها
```

مفهوم اصلی این بخش:

```text
AccountHeading
```

است.

---

# ۸. Account Management

این Subdomain مسئول مدیریت حساب‌های واقعی سیستم است.

هر حساب تحت یک سرفصل قرار می‌گیرد.

مثال:

```text
دارایی‌های جاری
│
├── حساب بانک ملت
├── حساب بانک صادرات
└── حساب صندوق
```

مفهوم اصلی این بخش:

```text
Account
```

است.

---

# ۹. Generic Subdomain

Generic Subdomainها قابلیت‌هایی هستند که ارزش کسب‌وکاری اختصاصی برای سیستم دفترکل ایجاد نمی‌کنند و معمولاً راهکارهای عمومی یا آماده برای آن‌ها وجود دارد.

نمونه‌ها:

```text
Authentication
Authorization
Logging
Monitoring
Notification
```

این قابلیت‌ها الزاماً بخشی از Bounded Context دفترکل نیستند و در صورت نیاز می‌توانند توسط زیرساخت یا Bounded Contextهای دیگر تأمین شوند.

---

# ۱۰. DDD تاکتیکی

DDD تاکتیکی درباره نحوه پیاده‌سازی مدل دامنه در داخل Bounded Context صحبت می‌کند.

در این مرحله مفاهیمی مانند موارد زیر مطرح می‌شوند:

* Aggregate
* Aggregate Root
* Entity
* Value Object
* Domain Service
* Domain Event
* Domain Rule
* Invariant

---

# ۱۱. Aggregateهای فعلی

در طراحی فعلی پروژه، برای هر یک از Subdomainهای اصلی یک Aggregate Root اولیه در نظر گرفته شده است:

```text
General Ledger Bounded Context
│
├── Account Structure
│   │
│   └── AccountHeading
│       └── Aggregate Root
│
├── Account Management
│   │
│   └── Account
│       └── Aggregate Root
│
└── Journal & Posting
    │
    └── Journal
        └── Aggregate Root
```

بنابراین Mapping فعلی به صورت زیر است:

| Subdomain          | Aggregate Root |
| ------------------ | -------------- |
| Account Structure  | AccountHeading |
| Account Management | Account        |
| Journal & Posting  | Journal        |

---

# ۱۲. نکته مهم درباره Subdomain و Aggregate

در DDD این قانون وجود ندارد که:

```text
هر Subdomain = دقیقاً یک Aggregate
```

این Mapping صرفاً تصمیم فعلی پروژه است.

ممکن است یک Subdomain در آینده چند Aggregate داشته باشد.

برای مثال:

```text
Journal & Posting
│
├── Journal Aggregate
├── Posting Aggregate
└── FiscalPeriod Aggregate
```

بنابراین Subdomain و Aggregate دو مفهوم متفاوت هستند.

---

# ۱۳. مرز Aggregate

Aggregate یک مرز Consistency در مدل دامنه است.

Aggregate Root مسئول کنترل قوانین و Invariantهای مربوط به Aggregate خود است.

Aggregateهای فعلی:

```text
AccountHeading
Account
Journal
```

هر Aggregate باید تا حد امکان مستقل باشد.

ارتباط بین Aggregateها ترجیحاً با Identity انجام می‌شود.

برای مثال:

```java
public class Account {

    private AccountId id;

    private AccountHeadingId accountHeadingId;
}
```

به جای نگهداری مستقیم Aggregate دیگر:

```java
public class Account {

    private AccountId id;

    private AccountHeading accountHeading;
}
```

این کار باعث می‌شود مرز Aggregateها واضح باقی بماند و Coupling کاهش پیدا کند.

---

# ۱۴. تفاوت Strategic DDD و Tactical DDD

## Strategic DDD

به این سؤال پاسخ می‌دهد:

> چه بخش‌هایی از کسب‌وکار داریم و مرز آن‌ها کجاست؟

ساختار کلی:

```text
Business Domain
       │
       ▼
Bounded Context
       │
       ▼
Subdomain
       │
       ├── Core
       ├── Supporting
       └── Generic
```

---

## Tactical DDD

به این سؤال پاسخ می‌دهد:

> داخل این Bounded Context، مدل دامنه را چگونه طراحی کنیم؟

ساختار کلی:

```text
Bounded Context
       │
       ▼
Subdomain
       │
       ▼
Aggregate
       │
       ▼
Aggregate Root
       │
       ├── Entity
       ├── Value Object
       ├── Domain Service
       └── Domain Event
```

---

# ۱۵. نقشه کامل Domain

مدل فعلی پروژه به صورت زیر است:

```text
                         DDD استراتژیک
                              │
                              ▼
                       حوزه حسابداری
                              │
                              ▼
                    General Ledger BC
                         (دفترکل)
                              │
             ┌────────────────┼────────────────┐
             │                │                │
             ▼                ▼                ▼
      Account Structure  Account Management  Journal & Posting
       ساختار سرفصل‌ها      مدیریت حساب‌ها     سند و ثبت حسابداری
          Supporting         Supporting            Core
             │                │                │
             └────────────────┼────────────────┘
                              │
                              ▼
                       DDD تاکتیکی
                              │
             ┌────────────────┼────────────────┐
             │                │                │
             ▼                ▼                ▼
      AccountHeading       Account           Journal
      Aggregate Root    Aggregate Root    Aggregate Root
```

---

# ۱۶. تفکیک مفاهیم DDD

مفاهیم زیر نباید با یکدیگر یکسان در نظر گرفته شوند:

```text
Subdomain
    ≠
Bounded Context
    ≠
Aggregate
    ≠
Aggregate Root
```

تعریف هرکدام:

| مفهوم                | معنی                                   |
| -------------------- | -------------------------------------- |
| Domain               | کل حوزه کسب‌وکار                       |
| Bounded Context      | مرز یک مدل دامنه                       |
| Subdomain            | یک بخش یا قابلیت کسب‌وکاری             |
| Core Subdomain       | بخش اصلی و دارای ارزش کسب‌وکاری متمایز |
| Supporting Subdomain | بخش پشتیبان Core                       |
| Generic Subdomain    | قابلیت عمومی و غیرمتمایز               |
| Aggregate            | مرز Consistency                        |
| Aggregate Root       | نقطه ورود و هویت Aggregate             |

---

# ۱۷. تصمیم فعلی پروژه

ساختار فعلی Domain به صورت زیر پذیرفته می‌شود:

```text
Accounting Domain
│
└── General Ledger Bounded Context
    │
    ├── Account Structure
    │   └── AccountHeading Aggregate Root
    │
    ├── Account Management
    │   └── Account Aggregate Root
    │
    └── Journal & Posting
        └── Journal Aggregate Root
```

این ساختار، مدل اولیه پروژه است و با افزایش نیازمندی‌های کسب‌وکار می‌تواند تغییر کند.

همچنین Core، Supporting و Generic بودن Subdomainها یک تصمیم کسب‌وکاری است و صرفاً بر اساس ساختار فنی پروژه تعیین نمی‌شود.

---

# ۱۸. ترتیب طراحی

طراحی Domain باید با ترتیب زیر انجام شود:

```text
شناخت کسب‌وکار
       │
       ▼
DDD استراتژیک
       │
       ▼
Bounded Context
       │
       ▼
شناسایی Subdomainها
       │
       ▼
Core / Supporting / Generic
       │
       ▼
DDD تاکتیکی
       │
       ▼
تعریف Aggregate Boundary
       │
       ▼
Aggregate Root
       │
       ▼
Entity / Value Object
       │
       ▼
Business Rule
       │
       ▼
Domain Service / Domain Event
```

---

# ۱۹. اصل نهایی طراحی

مدل دامنه باید بر اساس نیازهای کسب‌وکار و قوانین حسابداری شکل بگیرد.

ساختار فنی، Database، JPA، Spring و Infrastructure نباید تعیین‌کننده مدل Domain باشند.

به عبارت دیگر:

```text
Business Rules
       ↓
Domain Model
       ↓
Application
       ↓
Infrastructure
       ↓
Presentation
```

و نه:

```text
Database
       ↓
Entity
       ↓
Business Logic
```

هدف این است که Domain مستقل از تکنولوژی و زیرساخت باقی بماند و Business Ruleهای اصلی سیستم در Domain قابل مشاهده و قابل تست باشند.
