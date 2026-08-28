# Domain Layer Structure

اگر بخواهیم نام پوشه‌ها دقیقاً بر اساس **Aggregate Root** باشند، ساختار Domain به شکل زیر خواهد بود:

```text
domain
├── common
├── account
├── account-heading
├── journal
└── shared
```

## نقش هر پوشه

| پوشه | مسئولیت |
|------|---------|
| **account** | شامل Aggregate مربوط به **Account** (Aggregate Root، Entityها، Value Objectها، Domain Serviceهای اختصاصی، Policyها و Repository Interface مربوط به Account) |
| **account-heading** | شامل Aggregate مربوط به **AccountHeading** و تمام اجزای داخلی آن |
| **journal** | شامل Aggregate مربوط به **Journal** و اجزای وابسته به آن |
| **common** | شامل کلاس‌های پایه و عمومی Domain که به هیچ Aggregate خاصی تعلق ندارند؛ مانند `AggregateRoot`، `Entity`، `ValueObject`، `Identifier`، `DomainEvent` و سایر Base Classها |
| **shared** | شامل مفاهیم مشترک دامنه که بین چند Aggregate استفاده می‌شوند اما خودشان Aggregate نیستند؛ مانند `Money`، `Currency`، `FiscalPeriodId`، `Percentage` و سایر Value Objectهای مشترک |

---

## اصل طراحی

- هر **Aggregate Root** یک پوشه مستقل دارد.
- تمام اجزای داخلی Aggregate در همان پوشه قرار می‌گیرند.
- کلاس‌های پایه در `common` قرار می‌گیرند.
- مفاهیم مشترک دامنه که متعلق به Aggregate خاصی نیستند در `shared` قرار می‌گیرند.
- هیچ Aggregate نباید به ساختار داخلی Aggregate دیگر دسترسی مستقیم داشته باشد و ارتباط تنها از طریق شناسه (Identity) یا قراردادهای Domain انجام می‌شود.