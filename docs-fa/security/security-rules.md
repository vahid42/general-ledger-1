# قوانین امنیت — Ultra Compact

## هدف

تعریف قوانین قابل‌اعمال امنیتی برای بررسی انسانی، خودکار و AI.

این قوانین حوزه‌های Authentication، Authorization، Access Control، Input Validation، Data Protection، Secrets، API، Injection، External Calls، Resource Access، Logging، Cryptography، Dependencies، Configuration و Transaction/Concurrencyهای حساس از نظر امنیت را پوشش می‌دهند.

## قوانین اصلی

| ID    | Rule                                                                                                              |
| ----- | ----------------------------------------------------------------------------------------------------------------- |
| S-001 | منابع محافظت‌شده باید نیازمند Authentication باشند.                                                               |
| S-002 | Authorization باید در مرز مناسب اعمال شود؛ Authentication به‌تنهایی کافی نیست.                                    |
| S-003 | اصل Least Privilege برای کاربران، سرویس‌ها، نقش‌ها، Credentialها و منابع رعایت شود.                               |
| S-004 | Secretها، Credentialها، Tokenها و Private Keyها هرگز Hard-code نشوند.                                             |
| S-005 | Secret یا داده حساس هرگز در Log، Error، Response یا Debug Output افشا نشود.                                       |
| S-006 | تمام ورودی‌های غیرقابل‌اعتماد در مرز سیستم Validate و محدود شوند.                                                 |
| S-007 | از Injection جلوگیری شود؛ از Queryهای Parameterized و APIهای امن استفاده شود.                                     |
| S-008 | Identity، Role، Permission یا فیلدهای امنیتی ارسال‌شده توسط Client بدون اعتبارسنجی سمت Server قابل‌اعتماد نباشند. |
| S-009 | داده‌های حساس متناسب با الزاماتشان در Transit و At Rest محافظت شوند.                                              |
| S-010 | فقط از Cryptography تأییدشده و مدیریت امن Key استفاده شود؛ Cryptography سفارشی پیاده‌سازی نشود.                   |
| S-011 | از Deserialization ناامن داده‌های غیرقابل‌اعتماد یا با سطح اعتماد ناکافی جلوگیری شود.                             |
| S-012 | فراخوانی سرویس‌های خارجی باید دارای Authentication، Authorization، Validation و Failure Handling مناسب باشد.      |
| S-013 | دسترسی به File، Path، Resource و Network فقط به اهداف مجاز محدود شود.                                             |
| S-014 | Errorها باید امن باشند؛ Stack Trace، اطلاعات داخلی، Credential یا جزئیات حساس پیاده‌سازی افشا نشود.               |
| S-015 | کنترل‌های امنیتی در مسیرهای دسترسی معادل به‌صورت یکسان اعمال شوند و مسیر Bypass ایجاد نشود.                       |
| S-016 | کنترل امنیتی بدون تصمیم تأییدشده امنیتی/معماری تضعیف یا دور زده نشود.                                             |
| S-017 | Dependencyها و Configuration نباید ریسک امنیتی شناخته‌شده یا قابل‌اجتناب ایجاد کنند.                              |
| S-018 | عملیات حساس امنیتی باید Auditability و Traceability موردنیاز را حفظ کنند.                                         |

## الزامات امنیتی

الزامات پروژه‌ای یا عددی باید جداگانه تعریف شوند.

نمونه‌ها: Authentication، Token Lifetime، Rate Limit، Secret Policy، Encryption، Audit، Data Retention و Maximum Authentication Attempts.

**Security Agent نباید Security Requirement جدیدی اختراع کند.**

## بررسی امنیتی

Security Agent باید:

1. Diff و Security Context مرتبط را بررسی کند.
2. Security Requirements، Rules، ADRها و Business Rules مرتبط را بررسی کند.
3. آسیب‌پذیری‌ها و مسیرهای Bypass امنیتی را شناسایی کند.
4. برای هر Finding از Diff، Evidence ارائه دهد.
5. Finding را به `CONFIRMED`، `POTENTIAL` یا `SUGGESTION` طبقه‌بندی کند.
6. Severity و Confidence تعیین کند.
7. Risk را توضیح داده و در صورت امکان Remediation پیشنهاد دهد.

## طبقه‌بندی Finding

* `CONFIRMED`: نقض Rule یا Requirement با Evidence موجود اثبات شده است.
* `POTENTIAL`: الگوی دارای ریسک امنیتی وجود دارد، اما Evidence کافی از Runtime یا Configuration موجود نیست.
* `SUGGESTION`: پیشنهاد Hardening بدون اثبات وجود نقض امنیتی.

## Severity

* `BLOCKER`: آسیب‌پذیری بحرانی یا نقض شدید Security Requirement.
* `HIGH`: ریسک قابل‌توجه Unauthorized Access، Data Exposure، Injection، Credential Compromise یا Security-Control Bypass.
* `MEDIUM`: ضعف امنیتی با ریسک قابل‌توجه در شرایط خاص.
* `LOW`: ضعف جزئی یا فرصت برای Security Hardening.

## محدودیت‌ها

Remediation امنیتی نباید این موارد را نقض کند:

* Business Rules
* Domain Invariants
* Architecture Rules
* Data Consistency
* Transaction Semantics
* Availability Requirements

اهداف امنیتی:

`Confidentiality + Integrity + Availability + Least Privilege + Auditability`

## منبع حقیقت

اولویت منابع:

1. Security Requirements صریح
2. Security/Architecture ADRهای تأییدشده
3. Security Rules اختصاصی پروژه
4. Security Rules عمومی
5. AI Suggestions

**AI-generated Suggestion به‌تنهایی نباید باعث `REQUEST_CHANGES` شود.**

## قرارداد Rule

تمام Security Ruleها دارای ID یکتا با قالب `S-XXX` هستند.

قوانین فعلی: `S-001` تا `S-018`.
