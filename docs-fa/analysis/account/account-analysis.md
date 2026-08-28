# Account — Domain Analysis

## 1. تعریف

**Account (حساب)** موجودیتی است که زیر یک **سرفصل نهایی (Leaf AccountHeading)** ایجاد می‌شود و محل نگهداری مانده مالی مربوط به یک شعبه و یک ارز است.

هر حساب دارای یک کد یکتا و Generated است و پس از ایجاد، مشخصات اصلی آن قابل تغییر نیست.

---

## 2. ساختار Account

هر Account شامل موارد زیر است:

* `AccountId`
* `AccountCode`
* `Name`
* `AccountHeadingId`
* `BranchId`
* `Currency`
* `Balance`
* `Status`

وضعیت حساب:

```text
OPEN
CLOSED
```

---

## 3. ارتباط با AccountHeading

هر Account دقیقاً به یک `AccountHeading` تعلق دارد.

Account فقط می‌تواند زیر **آخرین سطح سرفصل (Leaf)** ایجاد شود.

بنابراین ایجاد حساب زیر یک سرفصل غیرنهایی مجاز نیست.

```text
AccountHeading
    |
    └── Leaf AccountHeading
            |
            ├── Account
            ├── Account
            └── Account
```

قوانین مربوط به **ماهیت حساب، بدهکار/بستانکار و امکان منفی شدن** از سرفصل نهایی به Account منتقل می‌شوند.

Account نباید این قوانین را مجدداً مستقل تعریف کند.

---

## 4. Account Code

کد حساب توسط سیستم تولید می‌شود و کاربر آن را تعیین نمی‌کند.

کد حساب از ترکیب موارد زیر تشکیل می‌شود:

```text
Branch + Currency + AccountHeading + AccountSequence
```

مثال:

```text
Branch        = 14
Currency      = 01
AccountHeading = 101010102
Sequence      = 000001
```

Account Code:

```text
1401101010102000001
```

بنابراین ساختار کلی:

```text
14 | 01 | 101010102 | 000001
```

است.

---

## 5. تولید Sequence

برای هر ترکیب زیر، Sequence حساب‌ها به صورت ترتیبی تولید می‌شود:

```text
Branch + Currency + AccountHeading
```

مثال:

```text
1401101010102000001
1401101010102000002
1401101010102000003
```

حساب بعدی:

```text
1401101010102000004
```

خواهد بود.

در نتیجه برای ایجاد Account جدید، سیستم باید آخرین Sequence استفاده‌شده برای همان ترکیب را شناسایی کرده و Sequence بعدی را تولید کند.

---

## 6. Account-Payable Heading

حساب فقط زمانی قابل ایجاد است که AccountHeading مربوطه قابلیت پذیرش حساب داشته باشد.

بنابراین:

```text
AccountHeading.isAccountAcceptable() == true
```

پیش‌شرط ایجاد Account است.

اگر سرفصل قابلیت پذیرش حساب نداشته باشد:

```text
Create Account → Reject
```

---

## 7. Identity

`AccountId` هویت داخلی Account است.

`AccountCode` نیز یک شناسه کسب‌وکاری Generated و یکتا برای حساب است.

تغییر هیچ‌کدام پس از ایجاد Account مجاز نیست.

---

## 8. تغییرات Account

پس از ایجاد حساب، فقط **نام حساب** قابل تغییر است.

مقادیر زیر قابل تغییر نیستند:

* AccountCode
* Branch
* Currency
* AccountHeading
* AccountId

Behavior مربوط به تغییر نام:

```text
Account.rename(newName)
```

---

## 9. حذف Account

Account قابلیت حذف ندارد.

```text
DELETE Account → Not Allowed
```

به جای حذف، حساب می‌تواند بسته شود.

---

## 10. بسته شدن Account

Account دارای وضعیت است:

```text
OPEN
CLOSED
```

حساب فقط زمانی قابل بسته‌شدن است که مانده آن صفر باشد.

### Invariant

```text
Account can be closed only when Balance == 0.
```

بنابراین:

```text
Balance = 0
    ↓
Account.close()
    ↓
CLOSED
```

اما:

```text
Balance != 0
    ↓
Account.close()
    ↓
Reject
```

---

## 11. رفتار Account پس از بسته شدن

پس از بسته‌شدن حساب:

* ثبت تراکنش جدید روی حساب مجاز نیست.
* تغییر Balance مجاز نیست.
* Account مجدداً قابل استفاده برای عملیات مالی نیست.

حساب حذف نمی‌شود و سابقه آن حفظ می‌شود.

---

## 12. Balance

هر Account مانده مالی خودش را نگهداری می‌کند.

`Balance` بخشی از State مربوط به Account است.

مقدار Balance نباید مستقیماً توسط Application یا Client تنظیم شود.

عملیات زیر مجاز نیست:

```text
account.setBalance(...)
```

تغییر Balance باید نتیجه یک **عملیات مالی معتبر** باشد.

---

## 13. تغییر Balance

تغییر Balance در نتیجه عملیات مالی مانند Debit/Credit انجام می‌شود.

Account مسئول نگهداری State و اعمال قوانین مربوط به خودش است، اما عملیات مالی و اعتبارسنجی تراکنش در سطح Transaction/Financial Operation مدیریت خواهد شد.

اصل مهم:

```text
Transaction
    ↓
Financial Operation
    ↓
Account
    ↓
Balance Change
```

بنابراین Account مالک `Balance` است، اما Account مستقیماً مسئول ایجاد Transaction نیست.

---

## 14. Account و Closed State

حساب بسته‌شده نباید در عملیات مالی جدید شرکت کند.

```text
CLOSED Account
      ↓
Financial Operation
      ↓
Reject
```

در نتیجه تغییر Balance برای حساب بسته‌شده ممنوع است.

---

## 15. منفی شدن Balance

قابلیت منفی شدن Account به صورت مستقل در Account تعریف نمی‌شود.

این Rule از `AccountHeading` مربوط به Account به ارث می‌رسد.

```text
AccountHeading
    |
    ├── Account Nature
    ├── Debit/Credit Nature
    └── Allow Negative
             ↓
          Account
```

بنابراین Account باید هنگام تغییر Balance قوانین سرفصل نهایی خود را رعایت کند.

مثال:

```text
Balance = 1000
Debit   = 1500
Result  = -500
```

اگر سرفصل اجازه منفی شدن نداشته باشد:

```text
Reject
```

و اگر اجازه منفی شدن داشته باشد:

```text
Accept
```

---

# Domain Invariants

## INV-01 — Account must belong to a Leaf Heading

هر Account فقط باید زیر یک سرفصل نهایی ایجاد شود.

---

## INV-02 — Heading must accept Account

اگر AccountHeading قابلیت پذیرش حساب نداشته باشد، ایجاد Account مجاز نیست.

---

## INV-03 — Account Code is generated

AccountCode توسط سیستم تولید می‌شود و کاربر نمی‌تواند آن را تعیین یا تغییر دهد.

---

## INV-04 — Account Code is unique

کد Account باید یکتا باشد.

---

## INV-05 — Sequence is generated

Sequence حساب برای ترکیب:

```text
Branch + Currency + AccountHeading
```

به صورت ترتیبی تولید می‌شود.

---

## INV-06 — Only Name is mutable

پس از ایجاد Account فقط Name قابل تغییر است.

---

## INV-07 — Account cannot be deleted

Account حذف نمی‌شود.

---

## INV-08 — Account can close only with zero balance

```text
Balance == 0
```

پیش‌شرط بسته‌شدن Account است.

---

## INV-09 — Closed Account cannot participate in financial operations

Account با وضعیت `CLOSED` نمی‌تواند در عملیات مالی جدید شرکت کند.

---

## INV-10 — Balance cannot be directly assigned

Balance فقط از طریق یک عملیات مالی معتبر تغییر می‌کند.

---

## INV-11 — Balance changes are prohibited for closed accounts

Account بسته‌شده نمی‌تواند Balance خود را تغییر دهد.

---

## INV-12 — Negative Balance follows AccountHeading rule

قانون امکان منفی شدن Balance از AccountHeading مربوطه تبعیت می‌کند.

---

# Domain Behaviors

رفتارهای اصلی Account:

```text
create()
rename()
close()
applyDebit()
applyCredit()
```

نکته:

`applyDebit()` و `applyCredit()` در مرحله طراحی Transaction باید دقیق‌تر مشخص شوند و نباید بدون تعریف کامل عملیات مالی نهایی شوند.

---

# Domain Responsibilities

## Account مسئول است برای:

* حفظ Identity
* حفظ AccountCode
* حفظ Name
* حفظ Branch و Currency
* نگهداری Balance
* حفظ Status
* جلوگیری از تغییر اطلاعات Immutable
* جلوگیری از عملیات مالی روی حساب بسته‌شده
* رعایت قانون صفر بودن Balance هنگام Close
* رعایت قانون منفی شدن برگرفته از AccountHeading

## Account مسئول نیست برای:

* ایجاد Transaction
* تعیین اعتبار کلی Transaction
* مدیریت چند Account در یک Transaction
* تولید سند حسابداری
* مدیریت فرآیند مالی در سطح سیستم

این موارد در Domainهای مربوط به Transaction/Financial Operation مدیریت خواهند شد.

---

# Summary

Account یک موجودیت مالی است که زیر یک سرفصل نهایی قرار می‌گیرد و بر اساس ترکیب:

```text
Branch + Currency + AccountHeading + Sequence
```

دارای کد یکتای Generated می‌شود.

بعد از ایجاد، تنها Name قابل تغییر است و Account هرگز حذف نمی‌شود.

Account می‌تواند در وضعیت `OPEN` یا `CLOSED` باشد و فقط زمانی قابل بسته‌شدن است که Balance آن صفر باشد.

Balance متعلق به Account است، اما تغییر آن فقط در نتیجه یک عملیات مالی معتبر انجام می‌شود. حساب بسته‌شده نمی‌تواند در عملیات مالی جدید شرکت کند.

قوانین مربوط به ماهیت بدهکار/بستانکار و امکان منفی شدن Balance از AccountHeading نهایی به Account منتقل می‌شوند.
