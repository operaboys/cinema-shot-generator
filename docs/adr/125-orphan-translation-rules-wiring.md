# ADR-125: اتصال دو Rule یتیم سیستم ترجمه‌ی UI — validateLanguageSupported و validateTranslationKeyFound

## وضعیت

پذیرفته‌شده

## زمینه

`domain/outputdelivery/Bilingual.kt` سه Rule دارد. یکی (`validateTranslationCoverage`)
از قبل واقعاً وصل بود — `ui/i18n/UiStringsTest.kt` روی `faStrings.keys`/
`enStrings.keys` واقعی صدا می‌زند. دو تای دیگر (`validateLanguageSupported`،
`validateTranslationKeyFound`) نوشته و تست‌شده بودند اما هیچ‌جای UI/ViewModel
واقعی صدا زده نمی‌شدند — یتیم.

### راستی‌آزمایی مستقل (قبل از کدنویسی)

- `Bilingual.kt` کامل خوانده شد: تأیید شد `validateLanguageSupported` یک
  رشته‌ی خام را با `Language.entries` (case-insensitive) مقایسه می‌کند و
  Rule `validateTranslationKeyFound` یک `WARNING` برمی‌گرداند وقتی کلید در
  هیچ‌کدام از دو زبان نیست.
- `WorkflowViewModel.kt` کامل خوانده شد: تأیید شد خط ۶۹-۷۱ (تعریف `_language`)
  دقیقاً همان الگوی توصیف‌شده بود — `raw?.let { Language.entries.find { it.name == raw } } ?: Language.FA`،
  بدون هیچ Log/Rule. با `grep` گسترده روی کل `app/src/main` تأیید شد این تنها
  نقطه‌ی واقعی است که یک رشته‌ی خام زبان از منبع بیرونی (DataStore) پردازش
  می‌شود — صفر استفاده از `Locale` سیستم‌عامل در کل پروژه.
- `WorkflowViewModelTest.kt` کامل خوانده شد — تست‌های موجود فقط رفتار
  `defaults`/`setLanguage`/Persistence را چک می‌کردند، هیچ تستی برای مقدار
  نامعتبر نداشتند؛ افزودن Rule خطر شکستن هیچ‌کدام را نداشت.
- `UiStringsTest.kt` کامل خوانده شد — تست سوم موجود فقط کلیدهای *تعریف‌شده*
  در `faStrings` را چک می‌کند (شکاف پوشش دقیقاً همان‌طور که در پرامپت این
  قدم توصیف شده بود: یک تایپوی کامل کلید که در هیچ‌کدام از دو Map نیست هرگز
  دیده نمی‌شود).
- `grep` گسترده برای `android.util.Log`/`Timber` در `app/src/main`: صفر
  نتیجه — تأیید شد هیچ Logging Framework ای در کدبیس نیست.
- **راستی‌آزمایی تجربی مستقیم (نه فرض)**: قبل از نوشتن تست نهایی، یک تست
  Probe موقت نوشته و اجرا شد که ثابت کرد از داخل یک تست JVM/Robolectric این
  پروژه (`./gradlew :app:testDebugUnitTest`)، Working Directory واقعی همان
  دایرکتوری ماژول (`app/`) است و `File("src/main/java/...")` واقعاً در
  دسترس است (۲۰۰ فایل `.kt` پیدا شد، `system-out` تست Probe). این تست Probe
  بعد از تأیید حذف شد — کد نهایی مستقیماً از این یافته استفاده می‌کند.

هیچ مغایرتی با پیش‌بریفینگ معمار پیدا نشد.

## تصمیم

### چرا `validateLanguageSupported` در `WorkflowViewModel` وصل شد (نه در `UiStrings.kt`)

`UiStrings.kt` هیچ نقطه‌ی «رشته‌ی خام زبان از منبع بیرونی» ندارد — `uiString`/`t`
همیشه یک مقدار enum `Language` معتبر از‌قبل می‌گیرند (طبق Type System). این Rule
دقیقاً برای لحظه‌ی «تبدیل رشته‌ی خام به enum» طراحی شده (طبق کامنت خودِ
`Bilingual.kt`) — و آن لحظه فقط یک‌بار در کل کدبیس رخ می‌دهد: خواندن اولیه‌ی
DataStore در `WorkflowViewModel`. وصل‌کردنش جای دیگری معنای واقعی نداشت.

### چرا `validateTranslationKeyFound` به‌عنوان تست وصل شد (نه Runtime check در `uiString`)

`uiString(key, language)` میان‌بر مستقیم `t()`/`Bilingual.kt` است — در صدها
جای کدبیس، در هر رندر UI صدا زده می‌شود. افزودن این Rule مستقیم داخل آن
یعنی اجرای یک چک اضافه روی هر تک رندر — یک Overhead بی‌دلیل برای چیزی که
صرفاً یک ابزار توسعه‌دهنده است (پیدا کردن تایپوی کلید قبل از انتشار)، نه
رفتار مصرف‌کننده‌ی نهایی. `t()` خودش هم از قبل fallback امن دارد (خودِ کلید
نمایش داده می‌شود) — پس هیچ خطر Runtime واقعی‌ای هم برای رفع فوری وجود
نداشت.

به‌جایش، هم‌الگو دقیق با نحوه‌ی اتصال Rule خواهرش (`validateTranslationCoverage`
در `UiStringsTest.kt`، خط ۱۸)، یک تست خودکار جامع نوشته شد: با یک Regex
(`\buiString\(\s*"([^"]+)"`) تمام رشته‌های کلید واقعی که در سراسر
`app/src/main` با `uiString("...")` فراخوانی می‌شوند استخراج می‌شوند، و برای
هر کدام `validateTranslationKeyFound` روی `uiTranslations` واقعی صدا زده
می‌شود. این رویکرد (نه یک لیست دستی نماینده) انتخاب شد چون راستی‌آزمایی
تجربی نشان داد کاملاً ممکن است — یک تست جامع‌تر و دقیق‌تر از یک نمونه‌ی
دستی محدود.

### پیاده‌سازی

**۱. `WorkflowViewModel.kt`**: تعریف `_language` به یک تابع خصوصی
`resolveInitialLanguage(raw: String?)` منتقل شد:

```kotlin
private fun resolveInitialLanguage(raw: String?): Language {
    if (raw != null) {
        validateLanguageSupported(raw)?.let { issue ->
            Log.w("WorkflowViewModel", "persisted language '$raw' is invalid, falling back to FA: ${issue.message}")
        }
    }
    return raw?.let { r -> Language.entries.find { it.name == r } } ?: Language.FA
}
```

منطق واقعی Fallback (`Language.entries.find { it.name == raw } ?: Language.FA`)
عمداً دست‌نخورده ماند — تنها افزوده، یک `Log.w` قبل از آن، فقط وقتی
`validateLanguageSupported` واقعاً `ValidationIssue` برمی‌گرداند. رفتار
ظاهری اپ برای کاربر بدون هیچ تغییری باقی می‌ماند.

**۲. `UiStringsTest.kt`**: دو تست تازه — یکی که کل کدبیس را Scan می‌کند و
هر کلید واقعی را با `validateTranslationKeyFound` تأیید می‌کند، و یکی که
با یک کلید عمداً جعلی ثابت می‌کند خودِ Rule واقعاً کار می‌کند (نه همیشه
`null`).

**۳. `WorkflowViewModelTest.kt`**: دو تست تازه با `ShadowLog` (زیرساخت
استاندارد Robolectric برای گرفتن Log های واقعی در تست، بدون هیچ وابستگی
تازه) — یکی ثابت می‌کند مقدار نامعتبر همچنان به FA Fallback می‌شود *و* یک
هشدار واقعی Log می‌شود؛ دیگری ثابت می‌کند مقدار معتبر هیچ هشداری تولید
نمی‌کند.

## پیامدها

- هیچ رفتار مشاهده‌پذیر برای کاربر نهایی تغییر نکرد.
- هیچ وابستگی جدیدی اضافه نشد (`android.util.Log` پلتفرم استاندارد،
  `ShadowLog` از‌قبل بخشی از Robolectric نصب‌شده‌ی پروژه).
- هر سه Rule واقعی `Bilingual.kt` اکنون واقعاً متصل‌اند —
  `validateTranslationCoverage` (از قبل)، `validateLanguageSupported`
  (Runtime، `WorkflowViewModel`)، `validateTranslationKeyFound` (تست جامع
  خودکار، `UiStringsTest.kt`).

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق
  (یک تلاش اول با نام تست حاوی `(...)` رد شد — Kotlin نام تابع Backtick‌دار
  را نپذیرفت؛ نام اصلاح شد).
- `WorkflowViewModelTest.kt`: ۷ تست (۵ قبلی + ۲ تازه)، همه Pass.
- `UiStringsTest.kt`: ۵ تست (۳ قبلی + ۲ تازه)، همه Pass — تست جامع واقعاً
  چند صد کلید واقعی کدبیس را Scan و تأیید کرد.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۵۰ تست، ۲ شکست — هر دو از
  کلاس‌های از‌قبل‌مستندشده‌ی ناپایدار فقط در اجرای کامل (`AssetFormFlowTest`،
  `OutputDeliveryFlowTest` — مستندشده از ADR-044، ریشه‌یابی‌شده ADR-070/071)؛
  هر دو در اجرای ایزوله دوباره Pass شدند، بدون هیچ شکست واقعی مرتبط با این
  قدم.
- `./gradlew :app:assembleDebug`: موفق.
