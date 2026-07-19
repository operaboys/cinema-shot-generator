# ADR-003: تصمیمات پیاده‌سازی واحد ۰۶ (Asset & Continuity) — لایه‌ی دامنه

**تاریخ:** 2026-07-19
**وضعیت:** تأییدشده (توسط کاربر/معمار در جریان تأیید این قدم اجرایی)

## Context

پیاده‌سازی لایه‌ی دامنه‌ی خالص واحد ۰۶ طبق `docs/blueprints/06-asset-and-continuity.md`، با تأکید ویژه‌ی این قدم روی **Hard Lock مطلق** (برخلاف Soft Lock واحد ۰۲) — طبق `docs/governance/override-policy.md`، «سطح Character» عمداً از سیاست Override حذف شده چون مستقیماً با این تصمیم بنیادی در تضاد بود.

## تصمیمات

### ۱. افزودن condition + isDefault به Expression (تأییدشده توسط کاربر)
**مشکل:** بلوپرینت می‌گوید «همین منطق [selectOutfitForScene] برای expressions هم قابل استفاده است»، اما نمونه‌ی JSON برای Expression فقط `id/name/description/emotion` دارد — بدون `condition` یا `is_default` (برخلاف Outfit). بدون این دو فیلد، `selectExpressionForScene` معنای واقعی ندارد.
**گزینه‌ها:** (الف) افزودن این دو فیلد به Expression؛ (ب) عدم پیاده‌سازی selectExpressionForScene واقعی؛ (ج) رویکرد دیگر.
**تصمیم (تأییدشده):** گزینه‌ی الف — `Expression` اکنون `isDefault: Boolean` و `condition: OutfitCondition?` دارد (نوع `OutfitCondition` عیناً بازاستفاده شد، نه کپی با نام جدید، چون شکل داده یکسان است).

### ۲. selectExpressionForScene به‌صورت تابع مستقل، نه Generic مشترک
تنها دو محل مصرف (Outfit و Expression) وجود دارد؛ ساخت یک Interface/Generic مشترک در این مرحله انتزاع زودهنگام بود. طبق قانون ۱۰ ai-governance («وضوح بر اختصار»)، یک تابع مستقل و ساده انتخاب شد.

### ۳. LocationAsset پوشش‌دهنده‌ی هر دو AssetType.LOCATION و AssetType.OBJECT
بلوپرینت این بخش را «Location / Object Asset» با یک ساختار JSON مشترک عنوان کرده؛ به‌جای دو کلاس تقریباً یکسان، یک `LocationAsset` با فیلد `assetType` ساخته شد.

### ۴. AssetType به‌صورت enum (نه String)
`enum class AssetType { CHARACTER, LOCATION, OBJECT }` — واژگان محدود و ثابت است (per بلوپرینت)، enum ایمنی زمان کامپایل می‌دهد؛ مشابه رویکرد واحدهای ۰۱/۰۲ برای واژگان محدود مشابه.

### ۵. selectOutfitForScene/selectExpressionForScene با `.first{}` خام (نه Result-محور)
پیاده‌سازی عیناً طبق کد مفهومی بلوپرینت نگه داشته شد — از جمله رفتار پرتاب `NoSuchElementException` اگر `manualOverrideId` یا هیچ Outfit/Expression پیش‌فرضی یافت نشود. این با قانون کلی «خطاها Result/sealed class باشند، نه Exception خام» (ai-coding-guidelines) در تنش است، اما دستور کار صریحاً «دقیقاً طبق کد مفهومی بلوپرینت» خواسته بود. مشابه ADR-001 §۶، این تصمیم مستند و به‌تعویق‌افتاده است: کنترل خطای واقعی (مثلاً بسته‌بندی در `Result`) وقتی معنا پیدا می‌کند که واحد ۰۵ (Shot Engine) این توابع را واقعاً فراخوانی کند.

### ۶. ValidationResult محلی واحد ۰۶ — هم‌شکل با واحد ۰۲، در پکیج مجزا
`ValidationResult` این واحد (`Valid/Warning/Blocking`) دقیقاً هم‌شکل با نوع محلی واحد ۰۲ است اما در پکیج `domain.asset` مستقل تعریف شده — هر دو موقتی تا واحد ۰۷. **توجه:** Rule 4 (Hard Lock) از این نوع استفاده نمی‌کند؛ `UpdateResult` (Allowed/Blocked، بدون Warning) مخصوص آن است تا در سطح Type System هم امکان «فقط هشدار» برای قفل‌های Hard Lock وجود نداشته باشد.

### ۷. Rule 2 بدون تابع Runtime — تضمین در سطح Type System
`CharacterAsset.physicalAppearance: PhysicalAppearance` غیر-nullable است؛ چون Kotlin از قبل این الزام را در زمان کامپایل تضمین می‌کند، تابع validate جداگانه برای Rule 2 اضافه نشد (طبق اجازه‌ی صریح دستور کار).

### ۸. الگوریتم Rule 7 (نام مشابه): Levenshtein Distance با آستانه‌ی نسبی
پس از نرمال‌سازی (`trim` + `lowercase`)، دو نام «مشابه» تلقی می‌شوند اگر دقیقاً یکسان باشند یا فاصله‌ی ویرایشی‌شان حداکثر ۲۰٪ طول بلندتر نام (حداقل ۱) باشد. این الگوریتم بدون وابستگی به کتابخانه‌ی خارجی fuzzy-matching، غلط‌های تایپی و شباهت نزدیک را می‌گیرد و برای نام‌های کاملاً متفاوت False Positive نمی‌دهد.

### ۹. Evolution Timeline — فقط ساختار داده، بدون منطق
`EvolutionEntry`/`EvolutionTimeline` اضافه شدند (فیلد `changes: Map<String, List<String>>` مطابق نمونه‌ی JSON). منطق اعمال Evolution پیاده نشد — طبق بلوپرینت به واحد ۱۲ (State & Versioning) برای ثبت رسمی وابسته است و دستور کار صراحتاً این را خارج از Scope اعلام کرده بود.

### ۱۰. توابع non-suspend
مشابه ADR-001 §۵ و ADR-002 §۵ — منطق خالص فعلاً نقطه‌ی تعلیق ندارد.

## Consequences

- **آسان می‌شود:** پوشش کامل انتخاب خودکار Outfit و Expression با منطق یکسان و قابل‌تست؛ Hard Lock با تضمین Type-System-level که هرگز قابل نرم‌شدن نیست.
- **بدهی ثبت‌شده:** مهاجرت آینده‌ی ValidationResult به واحد ۰۷؛ سخت‌گیری روی خطاهای `.first{}` وقتی واحد ۰۵ متصل شود؛ اتصال واقعی Evolution Timeline به واحد ۱۲؛ Rule 6 نیاز به پیاده‌سازی واقعی `fileExists` در لایه‌ی Data (واحد ۱۵) دارد.
