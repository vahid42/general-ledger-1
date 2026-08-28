# ADR-0001: استفاده از Architecture Decision Records

* **Status:** Accepted
* **Date:** 2026-08-20

## Context

پروژه `general-ledger` یک سیستم نرم‌افزاری برای مدیریت دفترکل و فرآیندهای حسابداری است.

در طول توسعه پروژه، تصمیمات معماری متعددی در زمینه‌های زیر اتخاذ خواهد شد:

* سبک معماری
* ساختار و مرزبندی ماژول‌ها
* وابستگی بین لایه‌ها
* Domain-Driven Design
* مدل‌سازی Domain
* Persistence
* Database
* API و Presentation
* Transaction Management
* Testing
* قوانین و مفاهیم حسابداری

در صورت عدم ثبت این تصمیمات، با گذشت زمان ممکن است دلیل انتخاب یک راهکار مشخص از بین برود و تصمیمات جدید با تصمیمات قبلی ناسازگار شوند.

همچنین توسعه‌دهندگان جدید برای درک چرایی ساختار فعلی سیستم نیاز به بررسی تاریخچه تغییرات معماری خواهند داشت.

بنابراین لازم است تصمیمات مهم معماری به صورت مستند، نسخه‌پذیر و قابل ردیابی در خود پروژه نگهداری شوند.

## Decision

در پروژه `general-ledger` از **Architecture Decision Records (ADR)** برای ثبت و نگهداری تصمیمات مهم معماری استفاده می‌کنیم.

ADRها در مسیر زیر قرار خواهند گرفت:

```text
docs/architecture/
```

هر تصمیم معماری در یک فایل مستقل ثبت خواهد شد.

نام‌گذاری فایل‌ها به صورت ترتیبی خواهد بود:

```text
0001-record-architecture-decisions.md
0002-<decision-name>.md
0003-<decision-name>.md
```

هر ADR حداقل شامل بخش‌های زیر خواهد بود:

* Context
* Decision
* Consequences

همچنین هر ADR دارای وضعیت مشخص خواهد بود:

* `Proposed`
* `Accepted`
* `Deprecated`
* `Superseded`

در صورتی که یک تصمیم توسط تصمیم جدیدی جایگزین شود، ADR قبلی حذف نخواهد شد و وضعیت آن به `Superseded` تغییر خواهد کرد و به ADR جایگزین‌کننده ارجاع داده خواهد شد.

ADR برای تصمیمات مهم و دارای اثر معماری استفاده می‌شود و برای تصمیمات جزئی پیاده‌سازی مورد استفاده قرار نمی‌گیرد.

## Consequences

### Positive

* تصمیمات معماری قابل ردیابی خواهند بود.
* دلیل تصمیمات در کنار Source Code نگهداری می‌شود.
* تاریخچه تصمیمات معماری حفظ خواهد شد.
* تغییرات معماری قابل بررسی و مقایسه خواهند بود.
* onboarding اعضای جدید تیم ساده‌تر خواهد شد.
* احتمال اتخاذ تصمیمات متناقض کاهش پیدا می‌کند.

### Negative

* ایجاد و نگهداری ADRها نیازمند نظم و انضباط است.
* تعداد زیاد ADRهای غیرضروری می‌تواند مستندات پروژه را پیچیده کند.
* ADRها باید همزمان با تغییرات مهم معماری به‌روزرسانی شوند.

## Implementation

ساختار مستندات معماری پروژه به صورت زیر خواهد بود:

```text
general-ledger/
│
├── docs/
│   └── architecture/
│       └── 0001-record-architecture-decisions.md
│
├── ledger-domain/
├── ledger-application/
├── ledger-infrastructure/
├── ledger-presentation/
├── ledger-test/
└── ledger-bootstrap/
```

هر تصمیم معماری مهم باید قبل یا همزمان با پیاده‌سازی آن در قالب یک ADR ثبت شود.


## References

1. Michael Nygard — *Documenting Architecture Decisions*
2. Martin Fowler — *Documenting Architecture Decisions*
3. Joel Parker Henderson — *Architecture Decision Records*