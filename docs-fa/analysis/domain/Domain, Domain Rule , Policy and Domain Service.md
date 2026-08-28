# Domain, Domain Rule, Policy and Domain Service

## 1. هدف

هدف این سند، تفکیک دقیق مفاهیم زیر در Domain-Driven Design است:

* Domain
* Domain Rule
* Invariant
* Policy
* Aggregate
* Domain Service
* Application Service

این تفکیک برای جلوگیری از اشتباه رایج زیر انجام می‌شود:

```text
Policy = همیشه کلاس جدا
Domain Rule = همیشه داخل Service
Domain Service = هر Service داخل Domain
```

این برداشت‌ها صحیح نیستند.

در DDD، مهم‌ترین مسئله این است که **Business Rule در کجا مالکیت و enforcement مناسبی دارد**.

---

# 2. Domain چیست؟

Domain همان حوزه کسب‌وکار و مسئله‌ای است که نرم‌افزار برای آن ساخته می‌شود.

مثلاً در یک سیستم حسابداری:

```text
Accounting Domain
│
├── Account
├── AccountHeading
├── Journal
├── Document
├── FiscalPeriod
├── Debit / Credit
├── Balance
└── Accounting Rules
```

Domain شامل:

* مفاهیم Business
* قوانین Business
* رفتارهای Business
* محدودیت‌ها
* تصمیم‌های Business
* روابط بین مفاهیم

است.

Domain Model باید این دانش و قواعد را در قالب مدل نرم‌افزاری بیان کند. DDD بر ساخت Domain Model غنی از رفتار، قواعد و زبان مشترک Domain تأکید دارد.

---

# 3. Domain Rule چیست؟

Domain Rule یک قانون Business است که باید در Domain برقرار باشد.

مثال:

```text
AccountHeading در Level 5 نمی‌تواند Child داشته باشد.
```

یا:

```text
Code یک AccountHeading باید Unique باشد.
```

یا:

```text
Account فقط زیر Leaf قابل ایجاد است.
```

هر سه Domain Rule هستند.

اما اینکه این Rule **کجا enforce شود** به Scope قانون بستگی دارد.

---

# 4. Invariant چیست؟

Invariant یک Domain Rule است که باید برای معتبر ماندن State یک Aggregate همیشه برقرار باشد.

به زبان ساده:

> Invariant قانونی است که Aggregate اجازه نمی‌دهد State خودش آن را نقض کند.

مثال در `AccountHeading`:

```text
Level باید بین 0 و 5 باشد.
```

یا:

```text
Level 5 نمی‌تواند Child داشته باشد.
```

یا:

```text
Child باید یک Level بیشتر از Parent داشته باشد.
```

این قوانین فقط با State خود Aggregate قابل بررسی هستند.

بنابراین مالک آن‌ها:

```text
AccountHeading Aggregate
```

است.

Microsoft نیز Invariant را از مهم‌ترین مسئولیت‌های Aggregate می‌داند و enforcement آن را بر عهده Aggregate قرار می‌دهد.

---

# 5. Aggregate چیست؟

Aggregate یک مرز consistency در Domain است.

Aggregate مشخص می‌کند:

```text
کدام Objectها باید با هم
در یک consistency boundary
مدیریت شوند؟
```

هر Aggregate یک Aggregate Root دارد.

در مدل ما:

```text
AccountHeading
```

خود Aggregate Root است.

بنابراین:

```text
AccountHeading Aggregate
│
├── Identity
├── State
├── Invariants
└── Behaviors
```

Aggregate Root مسئول حفظ consistency و Invariantهای داخل Aggregate است. منابع DDD نیز Aggregate را یک cluster از Domain Objects با یک Root معرفی می‌کنند که Root مسئول integrity آن است.

---

# 6. مثال AccountHeading

فرض کنیم:

```text
AccountHeading
```

دارای State زیر باشد:

```text
id
parentId
code
name
nature
allowNegativeBalance
level
```

و این قوانین را داشته باشد:

```text
Level بین 0 و 5 باشد.

Root در Level 0 باشد.

Root Parent نداشته باشد.

Child یک Level بیشتر از Parent داشته باشد.

Level 5 Leaf باشد.

Leaf نتواند Child داشته باشد.

Code خالی نباشد.

Name خالی نباشد.

Nature در Child از Parent به ارث برسد.

allowNegativeBalance در Child از Parent به ارث برسد.
```

همه این قوانین متعلق به consistency داخلی `AccountHeading` هستند.

بنابراین:

```text
AccountHeading
      │
      └── Invariants
```

---

# 7. Policy چیست؟

Policy یک Business Rule یا Decision Rule است که مشخص می‌کند:

> در یک شرایط مشخص، چه تصمیمی باید گرفته شود؟

مثلاً:

```text
آیا این سند قابل تأیید است؟
```

یا:

```text
آیا این AccountHeading Code مجاز است؟
```

یا:

```text
آیا این عملیات برای این شرایط مجاز است؟
```

Policy الزاماً یک کلاس جدا نیست.

این نکته بسیار مهم است:

```text
Policy
≠
Separate Class
```

Policy یک **مفهوم Domain** است.

محل پیاده‌سازی آن به Scope و اطلاعات موردنیاز بستگی دارد.

---

# 8. Policy می‌تواند داخل Aggregate باشد

فرض کنیم Policy این باشد:

```text
Level 5 AccountHeading
نباید Child داشته باشد.
```

این یک Business Rule است.

اما برای تصمیم‌گیری فقط State خود `AccountHeading` کافی است:

```java
isLeaf()
```

بنابراین لازم نیست کلاس جدا بسازیم:

```java
LeafPolicy
```

بلکه Aggregate خودش Rule را enforce می‌کند:

```java
public AccountHeading createChild(...) {

    if (isLeaf()) {
        throw new IllegalStateException(...);
    }

    ...
}
```

پس:

```text
Policy / Domain Rule
        ↓
AccountHeading
```

این کاملاً معتبر است.

---

# 9. Policy می‌تواند کلاس مستقل باشد

گاهی یک Business Decision:

* پیچیده است؛
* در چند Use Case استفاده می‌شود؛
* به یک Entity خاص تعلق ندارد؛
* یا بهتر است به صورت یک مفهوم مستقل در Domain مدل شود.

در این شرایط می‌توان Policy را کلاس جدا کرد.

مثال:

```java
public class DocumentApprovalPolicy {

    public boolean canApprove(Document document,
                               FiscalPeriod period) {

        ...
    }
}
```

در اینجا:

```text
DocumentApprovalPolicy
```

یک Policy مستقل است.

اما این الزام وجود ندارد که هر Policy حتماً به کلاس مستقل تبدیل شود.

---

# 10. Domain Service چیست؟

Domain Service یک Service در Domain است که منطق Business را انجام می‌دهد، اما آن منطق به‌طور طبیعی متعلق به یک Entity یا Value Object خاص نیست.

Martin Fowler در توضیح دسته‌بندی Evans، Service را یک عملیات مستقل در Context Domain معرفی می‌کند.

Microsoft نیز Domain Service را برای Business Ruleهایی که بین چند Entity یا Aggregate قرار می‌گیرند توضیح می‌دهد.

به زبان ساده:

```text
اگر منطق Business را نمی‌توان
به شکل طبیعی روی یک Aggregate
قرار داد
        ↓
ممکن است Domain Service مناسب باشد.
```

---

# 11. مثال Domain Service

در `AccountHeading` این Rule را داریم:

```text
Code باید در کل AccountHeadingها Unique باشد.
```

برای بررسی آن، یک `AccountHeading` به تنهایی کافی نیست.

باید سایر Headingها را بررسی کنیم:

```java
repository.existsByCode(code)
```

بنابراین این Rule متعلق به consistency داخلی یک `AccountHeading` نیست.

می‌توانیم آن را در:

```java
AccountHeadingDomainService
```

قرار دهیم.

مثلاً:

```java
public void ensureCodeIsUnique(String code) {

    if (repository.existsByCode(code)) {
        throw new IllegalStateException(...);
    }
}
```

ساختار:

```text
Code Uniqueness
       │
       ▼
Domain Rule / Policy
       │
       ▼
AccountHeadingDomainService
       │
       ▼
AccountHeadingRepository
```

---

# 12. چرا Code Uniqueness داخل AccountHeading نیست؟

چون `AccountHeading` فقط خودش را می‌شناسد.

فرض کنیم:

```text
AccountHeading A
code = 101000000
```

این Object به تنهایی نمی‌تواند بداند:

```text
آیا AccountHeading دیگری
code = 101000000
دارد؟
```

برای این تصمیم باید اطلاعات خارج از Aggregate را بخوانیم.

بنابراین:

```text
AccountHeading
    ❌ نمی‌تواند به تنهایی تصمیم بگیرد

Repository
    ↓
اطلاعات خارج از Aggregate

Domain Service
    ↓
تصمیم Domain
```

---

# 13. آیا هر استفاده از Repository یعنی Domain Service؟

خیر.

صرف استفاده از Repository دلیل کافی برای Domain Service بودن نیست.

باید ابتدا سؤال کنیم:

```text
آیا این منطق Business است؟
```

اگر نه، احتمالاً Infrastructure/Application concern است.

اما اگر:

```text
Business Rule
+
اطلاعات خارج از Aggregate
```

داشته باشیم، Domain Service یکی از گزینه‌های مناسب است.

---

# 14. مثال مهم: تأیید سند

فرض کنیم Business Rule این باشد:

```text
Document فقط زمانی قابل Approve است که:

1. تمام Lineها معتبر باشند.
2. Fiscal Period باز باشد.
3. Document وضعیت مناسبی داشته باشد.
```

اگر تمام اطلاعات داخل `Document` باشد:

```text
Document
   │
   └── approve()
```

و خود Aggregate می‌تواند Rule را enforce کند.

اما اگر نیاز داشته باشیم:

```text
Document
FiscalPeriod
UserAuthorization
```

را با هم بررسی کنیم، Rule دیگر فقط متعلق به `Document` نیست.

در این شرایط می‌توانیم داشته باشیم:

```text
DocumentApprovalPolicy
```

یا:

```text
DocumentApprovalDomainService
```

مثلاً:

```text
Document
     │
     │
     ├──────────────┐
     │              │
FiscalPeriod    Authorization
     │              │
     └──────┬───────┘
            ▼
    Approval Decision
```

---

# 15. Policy و Domain Service چه تفاوتی دارند؟

این دو مفهوم خیلی به هم نزدیک هستند اما یکی نیستند.

### Policy

روی **Business Decision / Rule** تمرکز دارد:

```text
آیا این عملیات مجاز است؟
```

### Domain Service

روی **Domain Logic مستقل از یک Entity خاص** تمرکز دارد:

```text
چه کسی باید این منطق را اجرا کند؟
```

بنابراین ممکن است:

```text
Policy
   ↓
در یک Domain Service پیاده‌سازی شود.
```

اما:

```text
Policy
```

الزاماً Domain Service نیست.

مثلاً:

```java
DocumentApprovalPolicy
```

می‌تواند یک Policy مستقل باشد.

در مقابل:

```java
AccountHeadingDomainService
```

ممکن است یک یا چند Domain Rule را با استفاده از Repository enforce کند.

---

# 16. Domain Service و Application Service متفاوت‌اند

این دو را نباید با هم اشتباه گرفت.

## Domain Service

مسئول:

```text
Business Logic
Business Rules
Domain Decisions
```

است.

مثال:

```java
AccountHeadingDomainService
```

## Application Service

مسئول:

```text
Use Case Coordination
Transaction Coordination
Calling Domain Objects
Calling Repositories
```

است.

Application Service نباید محل اصلی Business Ruleها باشد.

Application Service معمولاً Domain Objectها را orchestrate می‌کند و منطق Domain را به Domain Model واگذار می‌کند.

---

# 17. مثال کامل Create AccountHeading

فرض کنیم Use Case این است:

```text
Create AccountHeading
```

Application Service می‌تواند:

```text
CreateAccountHeadingApplicationService
             │
             ├── check code uniqueness
             │
             ├── create AccountHeading
             │
             └── save
```

اما Business Ruleها در Domain هستند:

```text
Application Service
        │
        ▼
AccountHeadingDomainService
        │
        └── Code Uniqueness

        +

AccountHeading
        │
        ├── Level
        ├── Parent
        ├── Name
        ├── Code
        └── Other Invariants
```

Application Service فقط orchestration انجام می‌دهد.

---

# 18. چرا Delete را داخل AccountHeading نیاوردیم؟

Rule:

```text
Heading دارای Child نباید حذف شود.
```

و همچنین:

```text
Heading دارای Account نباید حذف شود.
```

این‌ها Business Rule هستند.

اما `AccountHeading` به تنهایی نمی‌داند:

```text
آیا Child دارد؟
آیا Account دارد؟
```

این اطلاعات خارج از State آن قرار دارد.

بنابراین فعلاً:

```text
AccountHeading
    ❌ delete()
```

نداریم.

به جای آن:

```text
Delete Use Case
      │
      ├── check children
      ├── check accounts
      │
      └── delete
```

انجام می‌شود.

این به معنی خارج بودن Rule از Domain نیست.

بلکه:

```text
Domain Rule
      │
      └── Enforcement خارج از Aggregate
```

است.

---

# 19. Account Creation Under Leaf

Rule:

```text
Account فقط زیر Leaf ساخته می‌شود.
```

در `AccountHeading` فقط این Query را داریم:

```java
public boolean isLeaf() {
    return level == MAX_LEVEL;
}
```

چرا `canCreateAccount()` نداریم؟

چون سؤال `AccountHeading` این است:

```text
آیا من Leaf هستم؟
```

نه:

```text
آیا کل فرآیند ایجاد Account مجاز است؟
```

پس:

```text
AccountHeading
       │
       └── isLeaf()
```

و:

```text
Account Creation Logic
       │
       └── uses isLeaf()
```

این تفکیک مسئولیت مناسب‌تری است.

---

# 20. Decision Matrix

| سؤال                                                    | Owner مناسب                                          |
| ------------------------------------------------------- | ---------------------------------------------------- |
| آیا Level معتبر است؟                                    | `AccountHeading`                                     |
| آیا Leaf می‌تواند Child داشته باشد؟                     | `AccountHeading`                                     |
| آیا Child باید Nature والد را بگیرد؟                    | `AccountHeading`                                     |
| آیا Code خالی است؟                                      | `AccountHeading`                                     |
| آیا Code در کل Headingها Unique است؟                    | Domain Service / Policy                              |
| آیا Heading دارای Child قابل حذف است؟                   | Domain Logic خارج از Aggregate با اطلاعات Repository |
| آیا Account زیر Heading قابل ایجاد است؟                 | Account Creation Logic با استفاده از `isLeaf()`      |
| آیا سند قابل تأیید است و فقط اطلاعات Document کافی است؟ | `Document` Aggregate                                 |
| آیا تأیید سند به Document + FiscalPeriod نیاز دارد؟     | Policy / Domain Service                              |
| هماهنگ کردن Repository و Aggregate برای یک Use Case     | Application Service                                  |

---

# 21. رابطه مفاهیم

```text
                         Domain
                           │
             ┌─────────────┴─────────────┐
             │                           │
        Domain Rules                Domain Model
             │                           │
       ┌─────┴─────┐              ┌──────┴──────┐
       │           │              │             │
   Invariants   Policies       Aggregate     Domain Service
       │           │              │             │
       │           │              │             │
       ▼           ▼              ▼             ▼
  داخل Aggregate  داخل یا      AccountHeading  Code
                  خارج                         Uniqueness
                  Aggregate
```

---

# 22. مهم‌ترین اصل

نباید از ابتدا تصمیم بگیریم:

```text
این Rule را بگذاریم داخل Service.
```

ابتدا باید سؤال کنیم:

```text
1. این یک Business Rule است؟

2. آیا Rule فقط به State همین Aggregate نیاز دارد؟

3. اگر بله:
       Aggregate

4. اگر نه:
       آیا یک Business Decision مستقل داریم؟
          ↓
       Policy

5. آیا برای اجرای آن به چند Object،
   Aggregate یا اطلاعات خارجی نیاز داریم؟
          ↓
       Domain Service
```

---

# 23. خلاصه AccountHeading

در مدل فعلی:

```text
AccountHeading
│
├── Aggregate Root
│
├── Invariants
│   ├── Level 0..5
│   ├── Root rules
│   ├── Parent rules
│   ├── Leaf rules
│   ├── Code validation
│   ├── Name validation
│   └── Inheritance rules
│
├── Behaviors
│   ├── createRoot()
│   ├── createFirstLevel()
│   ├── createChild()
│   └── rename()
│
└── Queries
    ├── isRoot()
    └── isLeaf()
```

و:

```text
AccountHeadingDomainService
│
└── Code Uniqueness
        │
        └── AccountHeadingRepository
```

و:

```text
Delete Heading
│
└── Domain Rule
      │
      └── نیازمند اطلاعات خارج Aggregate
```

و:

```text
Account Creation
│
└── Domain Rule
      │
      └── AccountHeading.isLeaf()
```

---

# 24. Final Definitions

### Domain

حوزه کسب‌وکار و مجموعه مفاهیم، رفتارها و قواعدی که نرم‌افزار باید مدل کند.

### Domain Rule

قانون Business که باید در Domain برقرار باشد.

### Invariant

Domain Ruleای که برای معتبر ماندن State یک Aggregate باید همیشه برقرار باشد و Aggregate مسئول enforcement آن است.

### Policy

Business Rule یا Decision Rule که مشخص می‌کند در شرایط خاص چه تصمیمی باید گرفته شود. Policy الزاماً کلاس جدا نیست و می‌تواند داخل Aggregate یا به صورت یک Domain Object مستقل پیاده‌سازی شود.

### Domain Service

Object بدون State کسب‌وکاری مهم که Domain Logicای را انجام می‌دهد که به‌طور طبیعی متعلق به یک Entity یا Value Object خاص نیست و معمولاً ممکن است چند Domain Object، Aggregate یا اطلاعات خارجی را درگیر کند.

### Application Service

هماهنگ‌کننده Use Case است و باید Domain Logic را به Domain Model واگذار کند، نه اینکه Business Ruleهای اصلی را در خود نگه دارد.

---

# 25. References

## Eric Evans

مرجع اصلی این مفاهیم:

**Eric Evans — Domain-Driven Design: Tackling Complexity in the Heart of Software**

DDD در اصل با کتاب Evans به‌عنوان یک مجموعه الگو و واژگان مشخص برای مدل‌سازی Domain شناخته شد.

## Martin Fowler — Domain-Driven Design

توضیحی درباره DDD، Domain Model، Entity، Value Object، Service و Aggregate:

[Martin Fowler — Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html?utm_source=chatgpt.com)

## Martin Fowler — Evans Classification

برای Entity، Value Object و Service:

[Martin Fowler — Evans Classification](https://martinfowler.com/bliki/EvansClassification.html?utm_source=chatgpt.com)

## Martin Fowler — DDD Aggregate

برای Aggregate و Aggregate Root:

[Martin Fowler — DDD Aggregate](https://martinfowler.com/bliki/DDD_Aggregate.html?utm_source=chatgpt.com)

## Microsoft — Domain Model

برای Aggregate، Aggregate Root، Entity و رفتارهای Domain:

[Microsoft Learn — Design a microservice domain model](https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/microservice-domain-model?utm_source=chatgpt.com)

## Microsoft — Tactical DDD

برای Aggregate و Domain Service:

[Microsoft Learn — Use Tactical DDD to Design Microservices](https://learn.microsoft.com/en-ca/azure/architecture/microservices/model/tactical-ddd?utm_source=chatgpt.com)

## Martin Fowler — Anemic Domain Model

برای تفاوت Domain Model غنی از رفتار با مدل Anemic:

[Martin Fowler — Anemic Domain Model](https://www.martinfowler.com/bliki/AnemicDomainModel.html?utm_source=chatgpt.com)
