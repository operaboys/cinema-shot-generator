# ADR-113: اتصال کامل Style Matrix — قدم ۱الف از ۱۰ زیرقدم: افزودن Secondary Style + Influence به CoreIdentity (فقط مدل داده + DTO + Mapper + ViewModel، بدون UI)

## زمینه

این ADR اولین زیرقدم از یک برنامه‌ی ۱۰ زیرقدمی («اتصال کامل Style
Matrix») است. هدف این زیرقدم مشخص: افزودن مفهوم «سبک ثانویه + شدت
تأثیر» به مدل واقعی `CoreIdentity` (نه ساختار قدیمی و موازی
`StyleMatrix.kt`) — فقط لایه‌ی داده (دامنه + DTO + Mapper + ViewModel)،
بدون هیچ UI.

## بررسی مستقل — تأیید کامل پیش‌بررسی معمار، بدون هیچ تفاوتی

با خواندن کامل هر چهار فایل هدف پیش از نوشتن هر خط کد، تمام ادعاهای
پیش‌بریفینگ عیناً تأیید شد — بدون هیچ انحراف:

- `domain/visualidentity/StyleMatrix.kt`: `StyleMatrix`/`StyleReference`
  واقعاً هیچ‌جای دیگر کدبیس ساخته نمی‌شوند؛ `StyleInfluence` (enum
  `SUBTLE/MODERATE/STRONG`) دقیقاً همان‌جاست.
- `domain/dna/ProjectDna.kt` → `CoreIdentity`: دقیقاً یک فیلد
  `dominantVisualStyle: VisualStyle` — بدون هیچ مفهوم Secondary/Influence.
- `data/repository/ProjectDnaDto.kt` → `CoreIdentityDto`: دقیقاً همان
  ساختار پیش‌بینی‌شده (enum ها به‌صورت String).
- `data/repository/DnaAssetMappers.kt`: محل نگاشت `CoreIdentity`↔`CoreIdentityDto`
  دقیقاً در خطوط پیش‌بینی‌شده (۵۰-۵۵ toDomain، ۹۰-۹۵ toDto).
- `ui/dna/DnaViewModel.kt`: `updateAndSave` و `setPreferredLightingStyle`
  دقیقاً هم‌الگوی پیش‌بریفینگ.
- شماره‌ی بعدی ADR: با `ls docs/adr/` مستقل تأیید شد ADR-112 آخرین بود،
  پس این ADR شماره‌ی ۱۱۳ گرفت (نه فرض‌شده از پیام معمار).

## یافته‌ی جانبی مهم — سازگاری این قدم با تصمیم قبلی ADR-064 (نه تناقض)

هنگام به‌روزرسانی README، ممیزی G14 (`docs/adr/064-g14-orphaned-rules-decisions.md`،
تصمیم ۱۱) پیدا شد که صراحتاً `StyleMatrix.kt` را **«ماژول منسوخ (نه در
انتظار وصل)»** اعلام کرده بود — چون `CoreIdentity.dominantVisualStyle`
واقعی از قبل جایگزین آن شده و `checkStyleCompatibility` فقط یک
Placeholder همیشه-خنثی است. این می‌توانست در نگاه اول با عنوان «اتصال
کامل Style Matrix» این برنامه‌ی ۱۰ زیرقدمی در تناقض به‌نظر برسد.

با بررسی دقیق‌تر تأیید شد **هیچ تناقضی وجود ندارد**: خودِ دستور این
زیرقدم صریحاً می‌گوید «`StyleMatrix.kt` و `StyleReference` را دست نزن...
ساختار موازی قدیمی `StyleMatrix.kt` که در قدم‌های بعدی یا حذف یا با enum
جایگزین می‌شود» — یعنی معمار از قبل با تصمیم ADR-064 هم‌راستا عمل کرده:
این برنامه‌ی ۱۰ زیرقدمی مفهوم Style Matrix (سبک اصلی+ثانویه+شدت تأثیر)
را به مدل واقعاً استفاده‌شده (`CoreIdentity`) منتقل می‌کند، نه اینکه
ماژول منسوخ `StyleMatrix.kt` را دوباره زنده کند. تنها چیزی که از آن
ماژول بازاستفاده شد، خودِ enum کوچک و بی‌ضرر `StyleInfluence` بود (نه
`StyleReference`/`StyleMatrix`/`checkStyleCompatibility`). این یافته به‌عنوان
تأیید هم‌راستایی (نه انحراف) ثبت می‌شود.

## تصمیم ۱ — دو فیلد Nullable در `CoreIdentity`

```kotlin
data class CoreIdentity(
    val dominantVisualStyle: VisualStyle,
    val realismLevel: RealismLevel,
    val styleConsistency: StyleConsistency,
    val locked: Boolean = false,
    val secondaryStyle: VisualStyle? = null,
    val influence: StyleInfluence? = null
)
```

هر دو Nullable با پیش‌فرض `null` — Breaking-Change-کمینه، پروژه‌های
موجود بدون این دو فیلد صریح هم کار می‌کنند. `secondaryStyle` از همان
enum `VisualStyle` استفاده می‌کند (نه یک نوع تازه) — سبک ثانویه هم باید
از همان فهرست ۳۴‌مقداره‌ی سبک اصلی انتخاب شود، دقیقاً مطابق مفهوم اصلی
Style Matrix (اصلی+ثانویه هر دو از یک دامنه). `StyleInfluence` بازاستفاده
شد، نه بازتعریف.

## تصمیم ۲ — `setSecondaryVisualStyle(null)` همزمان `influence` را هم `null` می‌کند

```kotlin
fun setSecondaryVisualStyle(style: VisualStyle?) = updateAndSave {
    it.copy(
        coreIdentity = it.coreIdentity.copy(
            secondaryStyle = style,
            influence = if (style == null) null else it.coreIdentity.influence
        )
    )
}

fun setStyleInfluence(influence: StyleInfluence?) = updateAndSave {
    it.copy(coreIdentity = it.coreIdentity.copy(influence = influence))
}
```

توجیه: `Influence` بدون `Secondary` معنای منطقی ندارد — دقیقاً همان
قاعده‌ای که `combineStyles` موجود در `StyleMatrix.kt` از قبل رعایت
می‌کند (`if (secondary != null && influence != null)` — وقتی
`secondary=null`، `influence` هرچه باشد نادیده گرفته می‌شود). این قدم
همان قاعده را یک لایه بالاتر، در سطح خودِ State می‌آورد تا حالت
غیرممکن «Influence غیر-null بدون Secondary» اصلاً در دامنه ذخیره نشود
— نه فقط در محاسبه‌ی نهایی prompt نادیده گرفته شود.

## پیاده‌سازی DTO/Mapper

`CoreIdentityDto` دو فیلد `String?` تازه گرفت (هم‌الگوی
`LightingPreferenceDto.preferredStyle`)؛ `DnaAssetMappers.kt` هر دو
جهت را با `?.let { Enum.valueOf(it) }` / `?.name` نگاشت کرد — بدون
هیچ Room Migration (چون `dnaDataJson` یک ستون JSON خام است، نه ستون‌های
تفکیک‌شده).

## تست

**`DnaViewModelTest.kt` (فایل تازه، ۳ تست):** انتخاب `secondaryStyle`
و `influence` هرکدام واقعاً Persist می‌شوند (هم در State، هم پس از
Reload از Repository)؛ تنظیم `secondaryStyle=null` واقعاً `influence`
را هم به `null` برمی‌گرداند (هم در State، هم پس از Reload)؛ تنظیم
`influence=null` به‌تنهایی `secondaryStyle` را تغییر نمی‌دهد.

**یافته‌ی واقعی دیباگ حین نوشتن تست (نه باگ Production):** دو فراخوانی
پیاپی `setSecondaryVisualStyle`/`setStyleInfluence` در یک تست خام، دو
Coroutine مستقل Fire-and-Forget (`ioScope.launch { repository.saveProjectDna(...) }`)
راه می‌اندازند؛ چون `saveProjectDna` یک تابع suspend واقعی Room است
(نه صرفاً محاسبه‌ی درون‌حافظه)، تکمیل این دو Coroutine به ترتیب
فراخوانی تضمین نیست — حتی با `Dispatchers.Unconfined` (چون Room واقعاً
به Executor داخلی خودش سوییچ می‌کند، یک نقطه‌ی تعلیق واقعی). این یک
ویژگی از قبل موجود الگوی Auto-Save همین ViewModel است (در هر Setter
دیگر هم همینطور) — نه باگ تازه‌ی این قدم، و خارج از Scope این ADR
(تغییر معماری `updateAndSave` مجاز نبود). رفع فقط در خودِ تست: کمکی
`awaitRepositoryState` هم‌الگوی `awaitCondition` موجود
`AiStoryBreakdownViewModelTest.kt` (Poll با Timeout) بین فراخوانی‌های
پیاپی اضافه شد.

**`DtoMappersTest.kt` (۲ تست تازه):** چون `CoreIdentity` بر خلاف
`Shot`/`LightingSettings` تابع `toDto()`/`toDomain()` مستقل ندارد
(نگاشتش مستقیماً داخل `ProjectDna.toDto()`/`toDomain()` نوشته شده)،
Round-Trip از طریق کل `ProjectDna` تست شد: هر دو فیلد پر → برابر پس از
`toDto().toDomain()`؛ هر دو فیلد `null` → برابر. تست جداگانه‌ی سازگاری
عقب‌رو: دیکد یک `CoreIdentityDto` JSON قدیمی (بدون دو کلید تازه) با
موفقیت و پیش‌فرض `null` — هم‌الگوی دقیق تست مشابه موجود برای
`cinematicLanguage`/`cinematicModeOverride` (ADR-106).

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`DnaViewModelTest`) | ۳ تست، موفق |
| `gradle :app:testDebugUnitTest` (`DtoMappersTest`) | موفق (شامل ۲ تست تازه) |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۸۲ تست (۸۷۷+۵ تازه)، ۱ شکست نامرتبط (`OutputDeliveryFlowTest`) در یک اجرا و ۳ شکست نامرتبط دیگر (`AssetFormFlowTest`، `AppNavigationTest`، `OutputDeliveryFlowTest`) در اجرای دیگر — همگی در اجرای مجزا (`--tests`) موفق؛ هیچ‌کدام در مسیر وابستگی این قدم نیست؛ همان کلاس Flake محیطی از‌پیش‌مستند (ADR-044 تا ADR-112) |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این زیرقدم (عمداً)

- هیچ UI (نه در `DnaTabContent.kt`، نه هیچ‌جای دیگر) — زیرقدم بعدی.
- هیچ تغییری در `StyleMatrix.kt`/`StyleReference`/`checkStyleCompatibility` —
  طبق تصمیم صریح معمار، سرنوشت این ماژول منسوخ (حذف یا جایگزینی با enum)
  در زیرقدم‌های بعدی همین برنامه‌ی ۱۰ زیرقدمی تعیین می‌شود.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
