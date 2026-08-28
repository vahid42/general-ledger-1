# زمینه سیستم (System Context)

## 1. هدف سند

این سند زمینه و ساختار کلی سیستم را برای AI Agent مشخص می‌کند.

هدف آن این است که Agent بداند:

* سیستم چیست.
* معماری سیستم چگونه است.
* مسئولیت هر لایه چیست.
* مستندات معتبر سیستم کجا قرار دارند.
* هنگام تحلیل یا Review از چه منابعی استفاده کند.

این سند شامل جزئیات قوانین کسب‌وکار و Use Caseهای اختصاصی نیست.

قوانین کسب‌وکار و جزئیات دامنه باید از مستندات موجود در `docs/` خوانده شوند.

---

# 2. معرفی سیستم

این پروژه یک سیستم نرم‌افزاری ماژولار است که بر اساس اصول زیر طراحی می‌شود:

* Domain-Driven Design (DDD)
* Clean Architecture
* Separation of Concerns
* Modular Design

هدف معماری این است که:

* منطق کسب‌وکار مستقل از تکنولوژی باشد.
* Domain وابستگی به Infrastructure نداشته باشد.
* هر مسئولیت در لایه مناسب خود قرار بگیرد.
* مرزهای بین Moduleها و Bounded Contextها حفظ شوند.
* تغییرات فنی کمترین تأثیر را بر Domain داشته باشند.

---

# 3. اصول معماری

## 3.1 Domain-Driven Design

Domain محل اصلی مفاهیم و رفتارهای کسب‌وکار است.

Agent باید بین مفاهیم زیر تفاوت قائل شود:

* Entity
* Value Object
* Aggregate
* Aggregate Root
* Domain Service
* Domain Policy
* Domain Event
* Repository Abstraction

قوانین کسب‌وکار نباید صرفاً به دلیل ساده‌تر بودن پیاده‌سازی به Application یا Infrastructure منتقل شوند.

---

# 4. معماری لایه‌ای

ساختار مفهومی وابستگی‌ها به شکل زیر است:

```text
Presentation
     ↓
Application
     ↓
Domain
     ↑
Infrastructure
```

اصل اصلی این است که وابستگی‌ها باید به سمت لایه‌های داخلی معماری حرکت کنند.

`Domain` نباید به `Infrastructure` یا `Presentation` وابسته باشد.

`Infrastructure` می‌تواند پیاده‌سازی Abstractionهایی باشد که توسط لایه‌های داخلی تعریف شده‌اند.

---

# 5. مسئولیت لایه‌ها

## 5.1 Domain

مسئول:

* Entityها
* Value Objectها
* Aggregateها
* Aggregate Rootها
* Domain Behavior
* Invariantها
* Domain Serviceها
* Domain Policyها
* Domain Eventها
* Repository Abstractionها

Domain باید تا حد امکان مستقل از Framework و جزئیات فنی باشد.

---

## 5.2 Application

مسئول:

* Use Caseها
* Application Serviceها
* Commandها
* Queryها
* Orchestration
* هماهنگی بین Domain Objectها
* فراخوانی Repository Abstractionها
* مدیریت جریان اجرای Use Case

Application نباید به محل اصلی پیاده‌سازی Business Rule تبدیل شود.

---

## 5.3 Infrastructure

مسئول:

* Persistence
* Repository Implementation
* Database Access
* External Service Integration
* Messaging
* Serialization
* Framework Integration
* Technical Configuration

Infrastructure باید جزئیات فنی را پیاده‌سازی کند و نباید Business Ruleهای اصلی را در خود جای دهد.

---

## 5.4 Presentation

مسئول:

* API
* Controller
* Request DTO
* Response DTO
* Input Validation مرتبط با Transport
* Mapping ورودی و خروجی

Presentation نباید شامل Core Business Logic باشد.

Controllerها باید تا حد امکان Thin باشند.

---

# 6. ساختار ماژولار

هر Business Capability اصلی باید در Module مناسب خود قرار گیرد.

در صورت نیاز، هر Module می‌تواند ساختاری مشابه زیر داشته باشد:

```text
module/
├── presentation/
├── application/
├── domain/
└── infrastructure/
```

Agent نباید بدون دلیل معماری، وابستگی مستقیم بین Moduleها ایجاد کند.

قبل از ایجاد وابستگی بین دو Module باید مشخص شود:

1. مالک مفهوم کدام Module است؟
2. وابستگی واقعاً ضروری است؟
3. آیا Abstraction مناسب وجود دارد؟
4. آیا این وابستگی مرز Bounded Context را نقض می‌کند؟

---

# 7. مستندات پروژه

مستندات پروژه منبع اصلی برای درک Business و Architecture هستند.

ساختار کلی مستندات:

```text
docs/
├── analysis/
├── architecture/
└── ...
```

محتوای این پوشه ممکن است در طول پروژه تغییر کند.

Agent باید قبل از تصمیم‌گیری درباره Business Rule یا Architecture، مستندات مرتبط را بررسی کند.

---

# 8. اولویت منابع

در صورت وجود اختلاف بین منابع مختلف، اولویت به ترتیب زیر است:

1. نیازمندی صریح پروژه
2. Architecture Documentation تأییدشده
3. Domain Analysis تأییدشده
4. Use Caseهای تأییدشده
5. Implementation موجود
6. Testها
7. Conventionهای عمومی مهندسی نرم‌افزار

وجود یک رفتار در Code به این معنی نیست که آن رفتار الزاماً صحیح یا مورد تأیید معماری است.

اگر Implementation با مستندات تأییدشده مغایرت داشته باشد، Agent باید مغایرت را گزارش کند.

---

# 9. قوانین کسب‌وکار

Business Ruleها باید از مستندات Domain استخراج شوند.

Agent نباید Business Rule جدیدی را از خودش ایجاد کند.

اگر برای تصمیم‌گیری یک Business Rule مورد نیاز است اما در مستندات وجود ندارد:

* ابهام را مشخص کند.
* فرض شخصی ایجاد نکند.
* در صورت تأثیرگذاری بر صحت Implementation، آن را به عنوان Finding گزارش کند.

---

# 10. بررسی مسئولیت‌ها

قبل از پیشنهاد تغییر ساختاری، Agent باید مشخص کند:

1. مفهوم موردنظر متعلق به کدام Domain یا Module است؟
2. مسئولیت موردنظر متعلق به کدام Layer است؟
3. آیا رفتار موردنظر Business Behavior است یا Technical Behavior؟
4. آیا تغییر باعث ایجاد Dependency نامناسب می‌شود؟
5. آیا تغییر یک Invariant را نقض می‌کند؟
6. آیا برای این موضوع الگوی موجودی در پروژه وجود دارد؟

---

# 11. رفتار مورد انتظار از AI

AI Agent باید مانند یک Architectural Reviewer و Engineering Assistant رفتار کند.

Agent باید:

* ابتدا مستندات مرتبط را بررسی کند.
* معماری تعریف‌شده پروژه را حفظ کند.
* Domain Invariantها را محافظت کند.
* از ایجاد Abstraction غیرضروری خودداری کند.
* از Refactoring غیرضروری خودداری کند.
* مشکلات معماری را صریح گزارش کند.
* Defect را از Suggestion جدا کند.
* برای هر Finding دلیل و Evidence ارائه کند.
* از ایجاد Business Rule بدون مستندات خودداری کند.

Agent نباید Coding Preference شخصی را به عنوان Rule پروژه در نظر بگیرد.

---

# 12. سیاست Review

قوانین و Workflow مربوط به Code Review در فایل زیر تعریف شده است:

```text
.ai/review-policy.md
```

Agent هنگام انجام Code Review باید از آن Policy پیروی کند.

---

# 13. تنظیمات Agent

تنظیمات اجرایی Agent در فایل زیر قرار دارد:

```text
.ai/agent-config.yaml
```

این فایل مشخص می‌کند Agent چگونه Context، Documentation و Review Policy را مصرف کند.

---

# 14. اصل اصلی

> ابتدا Architecture و Intent مستندشده سیستم را درک کن، سپس Implementation را بر اساس آن بررسی کن.

Implementation موجود نباید به‌صورت خودکار به عنوان تعریف Architecture یا Business Rule در نظر گرفته شود.
