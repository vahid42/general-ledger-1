# AccountHeading — Domain Analysis

## Definitions

### Aggregate

یک Aggregate مرز consistency در Domain است.
Aggregate Root مسئول حفظ Invariantهای مربوط به وضعیت و رفتار داخل این مرز است.

در این مدل، `AccountHeading` یک Aggregate Root است.

### Domain Rule

هر قانونی که بیان‌کننده یک الزام یا محدودیت Business در Domain باشد، یک Domain Rule است.

Domain Rule الزاماً محل پیاده‌سازی مشخصی ندارد. بر اساس Scope و اطلاعات موردنیاز، ممکن است:

* داخل Aggregate enforce شود.
* توسط یک Domain Policy enforce شود.
* توسط یک Domain Service و با استفاده از اطلاعات خارج از Aggregate enforce شود.

### Invariant

قانونی است که باید همیشه برای معتبر بودن State یک Aggregate برقرار باشد.

اگر یک Rule بخشی از consistency داخلی Aggregate باشد، Aggregate Root باید آن را enforce کند.

### Behavior

عملیاتی است که Aggregate برای ایجاد یا تغییر وضعیت خودش انجام می‌دهد.

Behavior باید از طریق Aggregate Root انجام شود تا Invariantهای Aggregate حفظ شوند.

### Policy

Policy یک Business Rule یا Decision Rule است که مشخص می‌کند در یک شرایط خاص چه تصمیمی باید گرفته شود.

Policy الزاماً یک کلاس جدا نیست.

Policy می‌تواند:

* داخل Aggregate پیاده‌سازی شود؛
* به صورت یک کلاس Policy مستقل در Domain قرار گیرد؛
* یا در یک Domain Service که برای تصمیم‌گیری به اطلاعات خارج از Aggregate نیاز دارد، enforce شود.

بنابراین:

> Policy به معنی «منطق خارج از Aggregate» نیست.

محل پیاده‌سازی Policy به Scope قانون و اطلاعات موردنیاز آن بستگی دارد.

### Domain Service

Domain Service زمانی استفاده می‌شود که یک منطق Domain:

* متعلق به یک Aggregate مشخص نباشد؛
* یا برای تصمیم‌گیری به اطلاعات خارج از Aggregate نیاز داشته باشد؛
* یا چند Domain Object / Aggregate را درگیر کند.

Domain Service بخشی از Domain Model است و نباید صرفاً به دلیل استفاده از Repository، به یک Application Service تبدیل شود.

---

# پنج مفهوم اصلی در مدل‌سازی Domain

## این پنج مفهوم از کجا می‌آیند؟

| Concept        | مبنای DDD                                 | توضیح                                                                                           |
| -------------- | ----------------------------------------- | ----------------------------------------------------------------------------------------------- |
| **Identity**   | Eric Evans — *Domain-Driven Design*       | `Entity` و `Aggregate` با Identity خود شناخته می‌شوند.                                          |
| **State**      | DDD به‌عنوان مفهوم عمومی Entity/Aggregate | وضعیت فعلی Entity از Attributeها تشکیل می‌شود؛ یک دسته‌بندی رسمی پنج‌گانه در DDD نیست.          |
| **Invariants** | Eric Evans + DDD عمومی                    | قوانینی که Aggregate باید برای معتبر ماندن State خود حفظ کند.                                   |
| **Behaviors**  | Eric Evans — *Domain-Driven Design*       | مدل Domain فقط مجموعه‌ای از Data نیست و باید رفتارهای مرتبط با Domain را نیز در خود داشته باشد. |
| **Policies**   | Eric Evans — *Domain-Driven Design*       | مفهومی برای منطق تصمیم‌گیری و Ruleهایی که ممکن است داخل Object یا خارج از آن قرار بگیرند.       |

## خلاصه

این پنج مفهوم یک **دسته‌بندی رسمی پنج‌گانه در کتاب DDD نیستند**؛ بلکه مجموعه‌ای از مفاهیم بنیادی DDD هستند که برای تحلیل و طراحی یک Aggregate کاربرد دارند:

* **Identity** → این شیء دقیقاً کدام موجودیت است؟
* **State** → وضعیت فعلی آن چیست؟
* **Invariants** → چه قوانینی باید همیشه برقرار باشند؟
* **Behaviors** → چه کارهایی می‌تواند انجام دهد؟
* **Policies** → بر اساس چه Business Ruleهایی تصمیم‌گیری می‌شود؟

---

# 1. Overview

`AccountHeading` نمایانگر یک سرفصل حساب در ساختار سلسله‌مراتبی حسابداری است.

این Aggregate حداکثر دارای **۶ سطح از 0 تا 5** است:

```text
Level 0 → Root
Level 1 → First Level
Level 2 → Child
Level 3 → Child
Level 4 → Child
Level 5 → Leaf
```

در Level 5 امکان ایجاد Heading فرزند وجود ندارد.

Level 5 می‌تواند محل ایجاد `Account` باشد.

---

# 2. Domain Concepts

## 2.1 Identity

هر `AccountHeading` دارای Identity از نوع:

```java
AccountHeadingId
```

است.

Identity توسط:

```java
AggregateRoot<AccountHeadingId>
```

مدیریت می‌شود.

---

## 2.2 Parent

هر `AccountHeading` به جز Root دارای یک Parent است که با:

```java
AccountHeadingId parentId
```

مشخص می‌شود.

Root دارای:

```text
parentId = null
```

است.

---

## 2.3 Level

سطح Heading از `0` تا `5` است:

```text
0 ≤ level ≤ 5
```

Root برابر Level `0` است.

هر Child دقیقاً یک Level بیشتر از Parent دارد:

```text
child.level = parent.level + 1
```

---

## 2.4 Leaf

Leaf از Level محاسبه می‌شود:

```java
level == MAX_LEVEL
```

بنابراین:

```text
Level 5 → Leaf
Level 0..4 → Non-Leaf
```

در پیاده‌سازی فعلی `leaf` به عنوان State ذخیره نمی‌شود و از `level` محاسبه می‌شود:

```java
public boolean isLeaf() {
    return level == MAX_LEVEL;
}
```

این تصمیم از Duplicate State جلوگیری می‌کند.

---

## 2.5 AccountNature

`AccountNature` ماهیت حساب را مشخص می‌کند.

در Root مقدار `nature` وجود ندارد.

در First Level مقدار Nature توسط ایجادکننده تعیین می‌شود.

در Childهای بعدی Nature از Parent به ارث می‌رسد.

---

## 2.6 Allow Negative Balance

خصوصیت:

```java
allowNegativeBalance
```

مشخص می‌کند آیا موجودی منفی برای ساختار حساب مربوطه مجاز است یا خیر.

در First Level توسط ایجادکننده تعیین می‌شود.

در Childهای بعدی مقدار آن از Parent به ارث می‌رسد.

---

# 3. Invariants

Invariantهای `AccountHeading` قوانینی هستند که Aggregate باید همیشه حفظ کند.

## 3.1 Level Range

Level باید بین `0` و `5` باشد:

```text
0 ≤ level ≤ 5
```

این قانون توسط:

```java
validateLevel(...)
```

enforce می‌شود.

---

## 3.2 Root Invariant

Root باید دارای شرایط زیر باشد:

```text
level = 0
parentId = null
```

Root از طریق:

```java
AccountHeading.createRoot(...)
```

ایجاد می‌شود.

---

## 3.3 Non-Root Parent

هر Heading غیر Root باید Parent داشته باشد.

در ایجاد Child:

```java
this.getId()
```

به عنوان `parentId` استفاده می‌شود.

بنابراین:

```text
Child.parentId = Parent.id
```

---

## 3.4 Level Progression

هر Child دقیقاً یک Level بیشتر از Parent دارد:

```text
child.level = parent.level + 1
```

این قانون در:

```java
createFirstLevel(...)
createChild(...)
```

با استفاده از:

```java
this.level + 1
```

enforce می‌شود.

---

## 3.5 Leaf Invariant

Heading در Level 5 یک Leaf است:

```java
level == MAX_LEVEL
```

بنابراین:

```text
Level 5 → Leaf
Level 0..4 → Non-Leaf
```

---

## 3.6 Leaf Cannot Have Child

Heading در Level 5 نمی‌تواند Child ایجاد کند.

در `createChild(...)`:

```java
if (isLeaf()) {
    throw new IllegalStateException(...);
}
```

بنابراین:

```text
Leaf
  ↓
No Child
```

این یک Invariant داخلی Aggregate است و باید توسط خود `AccountHeading` enforce شود.

---

## 3.7 Code Must Not Be Null or Blank

`code` نباید `null` یا blank باشد.

این قانون توسط:

```java
validateCode(...)
```

enforce می‌شود.

---

## 3.8 Name Must Not Be Null or Blank

`name` نباید `null` یا blank باشد.

این قانون توسط:

```java
validateName(...)
```

enforce می‌شود.

---

## 3.9 Code Is Immutable

`code` بعد از ایجاد Heading قابل تغییر نیست.

در `rename(...)` همان Code قبلی استفاده می‌شود:

```java
this.code
```

بنابراین `rename(...)` فقط Name را تغییر می‌دهد.

---

## 3.10 Parent Is Immutable

`parentId` بعد از ایجاد Heading تغییر نمی‌کند.

در `rename(...)` همان Parent قبلی استفاده می‌شود:

```java
this.parentId
```

هیچ Behaviorای برای تغییر Parent وجود ندارد.

---

## 3.11 Nature Inheritance

در Root مقدار Nature وجود ندارد.

در First Level مقدار Nature توسط ایجادکننده تعیین می‌شود.

در Childهای غیر Root، Nature از Parent دریافت می‌شود:

```text
child.nature = parent.nature
```

در نتیجه Child نمی‌تواند Nature مستقل از Parent داشته باشد.

---

## 3.12 AllowNegativeBalance Inheritance

در First Level مقدار:

```java
allowNegativeBalance
```

توسط ایجادکننده تعیین می‌شود.

در Childهای بعدی مقدار آن از Parent دریافت می‌شود:

```text
child.allowNegativeBalance
    =
parent.allowNegativeBalance
```

بنابراین Child نمی‌تواند مقدار مستقل از Parent داشته باشد.

---

# 4. Behaviors

## 4.1 createRoot(...)

Factory Method برای ایجاد Root است:

```java
AccountHeading.createRoot(...)
```

مشخصات Root:

```text
parentId = null
level = 0
nature = null
allowNegativeBalance = false
```

---

## 4.2 createFirstLevel(...)

برای ایجاد اولین Heading زیر Root استفاده می‌شود:

```java
createFirstLevel(...)
```

این Behavior فقط روی Root قابل اجرا است.

در صورت اجرا روی Heading غیر Root:

```text
IllegalStateException
```

ایجاد می‌شود.

در این Level:

```text
Nature
allowNegativeBalance
```

توسط Caller تعیین می‌شوند.

---

## 4.3 createChild(...)

برای ایجاد Child استفاده می‌شود:

```java
createChild(
    id,
    code,
    name
)
```

Child همیشه با Parent فعلی ساخته می‌شود:

```text
parentId = current.id
```

و Level آن:

```text
child.level = parent.level + 1
```

خواهد بود.

Nature و `allowNegativeBalance` نیز از Parent دریافت می‌شوند.

---

## 4.4 rename(...)

برای تغییر Name استفاده می‌شود:

```java
rename(String newName)
```

پیاده‌سازی فعلی بر اساس Immutable State است.

یک `AccountHeading` جدید با همان Identity و Stateهای immutable ایجاد می‌شود:

```text
Old AccountHeading
       │
       │ rename()
       ↓
New AccountHeading
       │
       ├─ same id
       ├─ same parentId
       ├─ same code
       ├─ new name
       ├─ same nature
       ├─ same allowNegativeBalance
       └─ same level
```

---

# 5. Query / Derived Behavior

## 5.1 isLeaf()

`isLeaf()` یک Query / Derived Behavior است و Command Behavior محسوب نمی‌شود:

```java
public boolean isLeaf() {
    return level == MAX_LEVEL;
}
```

این متد State را تغییر نمی‌دهد.

وظیفه آن فقط پاسخ به این سؤال Domain است:

> آیا این AccountHeading یک Leaf است؟

---

## 5.2 isRoot()

`isRoot()` نیز یک Query / Derived Behavior است:

```java
public boolean isRoot() {
    return level == ROOT_LEVEL;
}
```

---

# 6. Domain Rules و Policyها

تمام Business Ruleها الزاماً در یک کلاس Policy مستقل قرار نمی‌گیرند.

ابتدا باید Scope قانون را مشخص کنیم.

## 6.1 Rule داخلی Aggregate

اگر Rule فقط با State و Behavior خود `AccountHeading` قابل بررسی باشد، Aggregate باید آن را enforce کند.

مثال:

```text
Level 5 نمی‌تواند Child داشته باشد.
```

برای تصمیم‌گیری فقط State خود Aggregate کافی است:

```java
isLeaf()
```

بنابراین Rule داخل `AccountHeading` قرار می‌گیرد.

---

## 6.2 Policy می‌تواند داخل Aggregate باشد

Policy الزاماً به معنی کلاس جدا نیست.

مثلاً Rule زیر:

```text
Child باید یک Level بیشتر از Parent داشته باشد.
```

یک Business Rule است و می‌توان آن را داخل Behavior:

```java
createChild(...)
```

enforce کرد.

بنابراین Policy/Rule می‌تواند بخشی از منطق Aggregate باشد.

---

## 6.3 Policy می‌تواند کلاس جدا باشد

اگر Rule پیچیده‌تر باشد یا بخواهیم تصمیم‌گیری Domain را مستقل کنیم، می‌توان Policy را به صورت یک کلاس مستقل در Domain قرار داد.

مثلاً:

```text
DocumentApprovalPolicy
```

ممکن است تصمیم بگیرد:

```text
آیا سند قابل تأیید است؟
```

اما این تصمیم لزوماً به یک Aggregate خاص محدود نیست.

---

# 7. مثال: تأیید سند

برای روشن شدن تفاوت Aggregate، Domain Rule، Policy و Domain Service، سناریوی تأیید سند مثال خوبی است.

فرض کنیم قانون این باشد:

```text
سند فقط زمانی قابل تأیید است که:
1. تمام اقلام سند معتبر باشند.
2. دوره مالی باز باشد.
3. سند وضعیت قابل تأیید داشته باشد.
```

اگر تمام اطلاعات لازم داخل Aggregate سند وجود داشته باشد، Aggregate می‌تواند Rule را خودش enforce کند.

اما اگر بررسی نیازمند اطلاعات خارج از Aggregate باشد، مثلاً:

```text
FiscalPeriod
User Authorization
Other Aggregate State
```

دیگر Aggregate به تنهایی اطلاعات کافی ندارد.

در این حالت می‌توان از Policy یا Domain Service استفاده کرد.

مثلاً:

```text
DocumentApprovalPolicy
        │
        ├── Document state
        ├── Fiscal period
        └── Authorization
```

بنابراین تفاوت اصلی این است:

```text
Aggregate
    ↓
مالک consistency داخلی خودش

Policy
    ↓
منطق تصمیم‌گیری Business

Domain Service
    ↓
اجرای منطق Domain که به
Objectها یا اطلاعات خارج از
یک Aggregate نیاز دارد
```

---

# 8. Policies مربوط به AccountHeading

## 8.1 Code Uniqueness

Business Rule:

```text
AccountHeading.code باید در کل AccountHeadingها Unique باشد.
```

این Rule متعلق به یک `AccountHeading` منفرد نیست.

برای بررسی آن باید Repository یا سایر Headingها بررسی شوند:

```java
repository.existsByCode(code)
```

بنابراین این Rule نمی‌تواند صرفاً توسط خود `AccountHeading` enforce شود.

در پیاده‌سازی فعلی، این Rule توسط:

```java
AccountHeadingDomainService
```

enforce می‌شود.

---

## 8.2 Delete Heading With Children

Business Rule:

```text
Heading دارای Child نباید حذف شود.
```

برای بررسی این Rule باید بدانیم آیا Child برای Heading وجود دارد یا خیر.

این اطلاعات در State خود `AccountHeading` وجود ندارد.

بنابراین فعلاً Delete را داخل Aggregate قرار نمی‌دهیم.

این به معنی آن نیست که Delete یک Rule خارج از Domain است.

بلکه:

> Business Rule متعلق به Domain است، اما enforcement آن به اطلاعات خارج از Aggregate نیاز دارد.

فرآیند می‌تواند خارج از Aggregate انجام شود:

```text
Delete AccountHeading
        │
        ├── check children
        ├── check accounts
        │
        └── delete
```

---

## 8.3 چرا Delete را فعلاً داخل AccountHeading نیاوردیم؟

چون متدی مانند:

```java
delete()
```

به تنهایی نمی‌تواند تضمین کند که Heading:

```text
Child ندارد
Account ندارد
```

اگر `AccountHeading` بخواهد این اطلاعات را از Repository بگیرد، Aggregate مستقیماً به Repository وابسته می‌شود که مرز و مسئولیت Aggregate را نامناسب می‌کند.

به همین دلیل فعلاً:

```java
AccountHeading
```

مالک State و Invariantهای داخلی خودش باقی می‌ماند و فرآیند Delete خارج از آن orchestration می‌شود.

Repository فعلی نیز این اطلاعات را فراهم می‌کند:

```java
boolean hasAccounts(AccountHeadingId accountHeadingId);
```

و در صورت نیاز بررسی Childها نیز باید از مکانیزم مناسب Repository انجام شود.

---

# 9. Account Creation Under Leaf

Business Rule:

```text
Account فقط زیر Leaf قابل ایجاد است.
```

`AccountHeading` فقط مسئول تشخیص State خودش است:

```java
isLeaf()
```

بنابراین نیازی به متدی مانند:

```java
canCreateAccount()
```

در `AccountHeading` نداریم.

فرآیند ایجاد Account می‌تواند از:

```java
accountHeading.isLeaf()
```

استفاده کند:

```text
Create Account
       │
       ▼
AccountHeading.isLeaf()
       │
       ├── true  → creation allowed
       └── false → creation rejected
```

بنابراین:

> `AccountHeading` می‌گوید «من Leaf هستم یا نیستم»؛
> منطق ایجاد `Account` تصمیم می‌گیرد که آیا ایجاد Account در این شرایط مجاز است یا خیر.

---

# 10. AccountHeadingDomainService

در حال حاضر `AccountHeadingDomainService` برای Ruleهایی مناسب است که به اطلاعات خارج از Aggregate نیاز دارند.

پیاده‌سازی فعلی:

```java
public class AccountHeadingDomainService {

    private final AccountHeadingRepository repository;

    public void ensureCodeIsUnique(String code) {
        ...
    }

    public void ensureCodeIsUnique(
            AccountHeadingId accountHeadingId,
            String code
    ) {
        ...
    }
}
```

این Service مسئول بررسی:

```text
Code Uniqueness
```

است.

چون برای این تصمیم باید Repository را بررسی کند:

```java
repository.existsByCode(...)
repository.existsByCodeAndIdNot(...)
```

---

# 11. چه زمانی سراغ Domain Service برویم؟

قاعده تصمیم‌گیری:

```text
آیا Rule فقط با State خود Aggregate قابل تصمیم‌گیری است؟
            │
       ┌────┴────┐
      Yes        No
       │          │
       ▼          ▼
 Aggregate    آیا Rule متعلق به
              یک Domain Object خاص است؟
                   │
              ┌────┴────┐
             Yes        No
              │          │
              ▼          ▼
       همان Object    Domain Service
       یا Policy
```

به صورت عملی:

### داخل Aggregate بماند اگر:

```text
فقط State خود AccountHeading کافی است.
```

مثال:

```text
Level 5 → Cannot create Child
```

### Policy مستقل مناسب است اگر:

```text
Business Decision پیچیده یا قابل‌استفاده مجدد داریم
و نمی‌خواهیم آن را به یک Aggregate خاص محدود کنیم.
```

### Domain Service مناسب است اگر:

```text
برای تصمیم‌گیری به Repository،
چند Object،
یا اطلاعات خارج از Aggregate نیاز داریم.
```

مثال فعلی:

```text
Code Uniqueness
```

---

# 12. Responsibility Matrix

| Rule                                         | Type                     | Owner / Enforcement                           |
| -------------------------------------------- | ------------------------ | --------------------------------------------- |
| Level بین 0 و 5                              | Domain Rule / Invariant  | AccountHeading                                |
| Root دارای Level صفر است                     | Invariant                | AccountHeading                                |
| Root Parent ندارد                            | Invariant                | AccountHeading                                |
| Non-Root دارای Parent است                    | Invariant                | AccountHeading                                |
| Child یک Level بعد از Parent است             | Invariant                | AccountHeading                                |
| Level 5 برابر Leaf است                       | Invariant                | AccountHeading                                |
| Leaf نمی‌تواند Child داشته باشد              | Invariant                | AccountHeading                                |
| Code خالی نیست                               | Invariant                | AccountHeading                                |
| Name خالی نیست                               | Invariant                | AccountHeading                                |
| Code تغییر نمی‌کند                           | Invariant                | AccountHeading                                |
| Parent تغییر نمی‌کند                         | Invariant                | AccountHeading                                |
| Nature از Parent به ارث می‌رسد               | Invariant                | AccountHeading                                |
| allowNegativeBalance از Parent به ارث می‌رسد | Invariant                | AccountHeading                                |
| ایجاد Root                                   | Behavior                 | AccountHeading                                |
| ایجاد First Level                            | Behavior                 | AccountHeading                                |
| ایجاد Child                                  | Behavior                 | AccountHeading                                |
| تغییر Name                                   | Behavior                 | AccountHeading                                |
| تشخیص Leaf                                   | Query / Derived Behavior | AccountHeading                                |
| تشخیص Root                                   | Query / Derived Behavior | AccountHeading                                |
| Code Unique در کل Headingها                  | Domain Rule / Policy     | AccountHeadingDomainService                   |
| حذف Heading دارای Child                      | Domain Rule              | خارج از Aggregate؛ نیازمند اطلاعات Repository |
| حذف Heading دارای Account                    | Domain Rule              | خارج از Aggregate؛ نیازمند اطلاعات Repository |
| ایجاد Account فقط زیر Leaf                   | Domain Rule / Policy     | منطق ایجاد Account با استفاده از `isLeaf()`   |

---

# 13. Aggregate Boundary

مرز Aggregate:

```text
             AccountHeading Aggregate

             ┌─────────────────────────┐
             │                         │
             │     AccountHeading      │
             │                         │
             │  ┌───────────────────┐  │
             │  │ Identity          │  │
             │  │ ParentId          │  │
             │  │ Code              │  │
             │  │ Name              │  │
             │  │ Nature            │  │
             │  │ AllowNegative...  │  │
             │  │ Level             │  │
             │  └───────────────────┘  │
             │                         │
             │  Behaviors              │
             │  ├─ createRoot          │
             │  ├─ createFirstLevel    │
             │  ├─ createChild         │
             │  └─ rename              │
             │                         │
             │  Queries                │
             │  ├─ isRoot              │
             │  └─ isLeaf              │
             │                         │
             │  Invariants             │
             │  └─ Internal Rules      │
             │                         │
             └─────────────────────────┘
                        │
                        │
              External Domain Information
                        │
            ┌───────────┼───────────┐
            ▼           ▼           ▼
        Repository    Policy    Domain Service
```

---

# 14. Current Implementation Notes

پیاده‌سازی فعلی `AccountHeading` بر مبنای **Immutable State** طراحی شده است.

خصوصیات State به صورت `final` هستند:

```java
private final AccountHeadingId parentId;
private final String code;
private final String name;
private final AccountNature nature;
private final boolean allowNegativeBalance;
private final int level;
```

`leaf` به عنوان State جداگانه ذخیره نمی‌شود و از `level` محاسبه می‌شود:

```java
public boolean isLeaf() {
    return level == MAX_LEVEL;
}
```

بنابراین از نگهداری دو State مستقل:

```text
level
leaf
```

که ممکن است با یکدیگر ناسازگار شوند جلوگیری شده است.

تغییر State نیز با ایجاد Instance جدید انجام می‌شود.

در حال حاضر `rename(...)` نیز از همین الگو استفاده می‌کند.

---

# 15. Explicit Domain Decisions

تصمیمات فعلی Domain:

1. حداکثر Level برابر `5` است.
2. Root در Level `0` قرار دارد.
3. Root فاقد Parent است.
4. Level `5` تنها سطح Leaf است.
5. Leaf نمی‌تواند Child داشته باشد.
6. Code بعد از ایجاد تغییر نمی‌کند.
7. Parent بعد از ایجاد تغییر نمی‌کند.
8. Name قابل تغییر است.
9. Nature در First Level تعیین می‌شود و سپس به Childها به ارث می‌رسد.
10. `allowNegativeBalance` در First Level تعیین می‌شود و سپس به Childها به ارث می‌رسد.
11. Root دارای Nature نیست.
12. Account فقط زیر Leaf قابل ایجاد است.
13. Unique بودن Code یک Domain Rule سراسری است.
14. بررسی Unique بودن Code به اطلاعات خارج از Aggregate نیاز دارد و فعلاً توسط `AccountHeadingDomainService` enforce می‌شود.
15. حذف Heading دارای Child یا Account یک Domain Rule است، اما enforcement آن نیازمند اطلاعات خارج از Aggregate است.
16. Delete فعلاً به عنوان Behavior داخل `AccountHeading` قرار نمی‌گیرد.
17. Policy الزاماً یک کلاس جدا نیست.
18. Policy می‌تواند داخل Aggregate enforce شود.
19. در صورت نیاز، Policy می‌تواند به صورت کلاس مستقل در Domain پیاده‌سازی شود.
20. اگر یک Rule برای تصمیم‌گیری به اطلاعات خارج از Aggregate نیاز داشته باشد، Domain Service می‌تواند مسئول enforcement آن باشد.
21. `AccountHeading` فقط `isLeaf()` را expose می‌کند.
22. Rule «Account فقط زیر Leaf ساخته می‌شود» در منطق ایجاد Account enforce می‌شود و نیازی به `canCreateAccount()` در `AccountHeading` نیست.
23. `AccountHeading` نباید مستقیماً به `AccountHeadingRepository` وابسته باشد.
24. Domain Service در جایی استفاده می‌شود که منطق Domain به اطلاعات خارج از Aggregate یا چند Domain Object نیاز دارد.

---

# 16. Domain Model Summary

```text
AccountHeading
│
├── Identity
│   └── AccountHeadingId
│
├── State
│   ├── parentId
│   ├── code
│   ├── name
│   ├── nature
│   ├── allowNegativeBalance
│   └── level
│
├── Invariants
│   ├── Level 0..5
│   ├── Root rules
│   ├── Parent rules
│   ├── Level progression
│   ├── Leaf rules
│   ├── Code validation
│   ├── Name validation
│   ├── Immutable code
│   ├── Immutable parent
│   ├── Nature inheritance
│   └── allowNegativeBalance inheritance
│
├── Behaviors
│   ├── createRoot(...)
│   ├── createFirstLevel(...)
│   ├── createChild(...)
│   └── rename(...)
│
└── Queries
    ├── isRoot()
    └── isLeaf()


AccountHeadingDomainService
│
└── Policies / Domain Rules
    └── Code Uniqueness
            │
            └── AccountHeadingRepository


External Domain Rules
│
├── Delete Heading
│   ├── no children
│   └── no accounts
│
└── Account Creation
    └── parent must be Leaf
```

---

# 17. Final Responsibility Principle

اصل تصمیم‌گیری در این مدل:

```text
اگر Rule فقط به State خود Aggregate نیاز دارد
        ↓
Aggregate
```

```text
اگر Rule یک Business Decision است
        ↓
Policy
```

```text
اگر Policy/Rule برای تصمیم‌گیری به
اطلاعات خارج از Aggregate نیاز دارد
        ↓
Domain Service / External Domain Logic
```

بنابراین در `AccountHeading`:

```text
Level / Parent / Leaf / Inheritance
        ↓
AccountHeading
```

```text
Code Uniqueness
        ↓
AccountHeadingDomainService
        ↓
AccountHeadingRepository
```

```text
Delete
        ↓
فعلاً خارج از Aggregate
        ↓
نیازمند بررسی Child / Account
```

```text
Account Creation
        ↓
Account Domain Logic
        ↓
AccountHeading.isLeaf()
```

اصل مهم این است:

> **Business Rule را بر اساس ماهیت Rule دسته‌بندی می‌کنیم، نه صرفاً بر اساس اینکه کد آن در چه کلاسی قرار گرفته است.**

و همچنین:

> **Policy الزاماً کلاس جدا نیست؛ محل پیاده‌سازی آن تابع Scope قانون و اطلاعات موردنیاز برای تصمیم‌گیری است.**
