# ADR-115: اتصال کامل Style Matrix — قدم ۳ از ۸ زیرقدم: UI انتخاب Secondary Style + Influence + نمایش زنده‌ی Warning ناسازگاری در فرم DNA

## زمینه

ADR-113 (قدم ۱) `secondaryStyle`/`influence` را به `CoreIdentity` اضافه
کرد؛ ADR-114 (قدم ۲) `checkStyleCompatibility` را با ماتریس واقعی
سازگاری وصل کرد. تا این قدم، هیچ‌کدام در UI تب DNA قابل‌دیدن یا
قابل‌تنظیم نبودند. این قدم دو Dropdown تازه (Secondary Style، Influence)
و نمایش زنده‌ی هشدار ناسازگاری را به گروه «هویت اصلی» اضافه می‌کند.

## بررسی مستقل — تأیید کامل پیش‌بریفینگ، بدون هیچ تفاوتی

با خواندن کامل `DnaTabContent.kt`، `DnaLabels.kt`، هر دو نقشه‌ی
`UiStrings.kt`، متدهای قدم ۱ در `DnaViewModel.kt`، و `StyleMatrix.kt`
فعلی پیش از نوشتن هر خط کد: `DnaGroup("dna.group.coreIdentity",
fieldCount=3)` دقیقاً در خط ۱۶۰ بود؛ الگوی Dropdown nullable
(«Lighting Preference») دقیقاً در خطوط ۲۶۶-۲۸۷؛ `visualStyleLabel`/
`visualStyleCategoryLabel` از قبل در `DnaLabels.kt` موجود بودند؛
`colorPaletteIssue`/`ValidationIssueRow` دقیقاً همان الگوی توصیف‌شده؛
`setSecondaryVisualStyle`/`setStyleInfluence` دقیقاً در خطوط ۱۶۰-۱۷۱
`DnaViewModel.kt`؛ هر دو نقشه‌ی `UiStrings.kt` (فارسی/انگلیسی) دقیقاً
هم‌ساختار توصیف‌شده. **بدون هیچ انحراف واقعی از پیش‌بریفینگ.**

## کار انجام‌شده

**۱. `DnaLabels.kt`:** `styleInfluenceLabel(influence, language)` تازه
— هم‌الگوی دقیق `cinematicModeLabel`.

**۲. `UiStrings.kt` (هر دو نقشه):** کلیدهای تازه —
`dna.coreIdentity.secondaryStyleLabel`، `dna.coreIdentity.secondaryStyleNone`،
`dna.coreIdentity.influenceLabel`، `dna.coreIdentity.influenceUnset`،
`dna.coreIdentity.styleCompatibilityWarningTemplate` (با `{primary}`/
`{secondary}`، طبق الگوی موجود `uiTemplate`)، و سه مقدار
`styleInfluence.subtle/moderate/strong` (کنار `cinematicMode.*`).

**۳. `DnaTabContent.kt`:** `DnaGroup` گروه «هویت اصلی» از `fieldCount=3`
به `fieldCount=5` تغییر کرد. دو فیلد تازه اضافه شدند:
- **Secondary Style** (nullable): دقیقاً هم‌الگوی «Lighting Preference» —
  `DropdownMenuItem` اول «بدون سبک ثانویه» (`setSecondaryVisualStyle(null)`)،
  سپس `GroupedEntries` روی `VisualStyle.entries`.
- **Influence**: فقط وقتی `dna.coreIdentity.secondaryStyle != null` باشد
  رندر می‌شود (نه فقط غیرفعال — اصلاً در درخت Compose نیست، طبق تصمیم
  قدم ۱ که Influence بدون Secondary بی‌معناست). سه گزینه‌ی ثابت
  (`FlatEntries` روی `StyleInfluence.entries`).
- نمایش زنده‌ی هشدار: `secondaryStyleIssue` (`remember` روی
  `dominantVisualStyle`+`secondaryStyle`+`language`) وقتی `secondaryStyle`
  ست باشد `checkStyleCompatibility` را صدا می‌زند و به `ValidationIssue`
  تبدیل می‌کند؛ نمایش با `ValidationIssueRow` موجود، دقیقاً زیر دو
  Dropdown تازه.

**۴. testTag های تازه:** `DNA_SECONDARY_STYLE_FIELD_TAG`،
`DNA_STYLE_INFLUENCE_FIELD_TAG`.

**۵. رفع یافته‌ی ADR-114:** `validatePrimaryStyleUpdate` از
`StyleReference?` به `VisualStyle?` تغییر کرد (منطق داخلی بدون تغییر).

## تصمیم — محل تابع تبدیل `CompatibilityResult` → `ValidationIssue`

`toValidationIssueOrNull` (extension خصوصی روی `CompatibilityResult`) در
خودِ `DnaTabContent.kt` (لایه‌ی UI) تعریف شد، **نه** در
`domain/visualidentity/StyleMatrix.kt`. دلیل: این تابع به `Language`،
`uiTemplate`، و `visualStyleLabel` (لایه‌ی i18n/UI) نیاز دارد؛ افزودن
این وابستگی به یک فایل دامنه‌ای خالص، همان انضباط جداسازی «domain ←
data ممنوع» (که `DnaAssetMappers.kt` هم رعایت می‌کند) را برای «domain ←
UI» هم می‌شکست. `CompatibilityResult` خودش کاملاً خالص و بدون‌متن
می‌ماند (فقط `level`/`warning`)؛ متن‌سازی محلی‌سازی‌شده کاملاً در لایه‌ی
UI انجام می‌شود — این هم دقیق‌تر از الگوی موجود بعضی پیام‌های Warning
دامنه‌ای این پروژه است (که فارسی خام هاردکد شده‌اند، مثلاً پیام
`validatePrimaryStyleUpdate` خودش) چون واقعاً هر دو زبان را پشتیبانی
می‌کند.

## باگ واقعی پیداشده و رفع‌شده — کلید ترجمه‌ی فراموش‌شده در نقشه‌ی انگلیسی

اولین اجرای `gradle :app:testDebugUnitTest` (کل Suite) با یک شکست واقعی
مواجه شد: `UiStringsTest > fa and en string maps have exactly the same
set of keys` — سه کلید تازه (`styleInfluence.subtle/moderate/strong`)
را فقط به نقشه‌ی فارسی اضافه کرده بودم، نقشه‌ی انگلیسی را فراموش کرده
بودم (دقیقاً همان خطری که خودِ پیش‌بریفینگ صریحاً هشدار داده بود: «هر
کلید ترجمه‌ی جدید باید در هر دو اضافه شود، وگرنه در یکی از دو زبان
خالی/شکسته می‌ماند»). یک تست خودکار موجود (`UiStringsTest.kt`) این را
بلافاصله و دقیق تشخیص داد. رفع شد؛ اجرای مجدد همان تست موفق (هر ۳ تست
`UiStringsTest`).

## تست

**`DnaTabFlowTest.kt` (۴ تست تازه):** انتخاب Secondary Style واقعاً
Persist می‌شود و فیلد Influence (که پیش از آن اصلاً در درخت نبود) ظاهر
می‌شود؛ انتخاب دوباره‌ی «بدون سبک ثانویه» هم `secondaryStyle` و هم
`influence` را واقعاً به `null` برمی‌گرداند (تأییدشده هم در UI هم پس از
Reload از Repository واقعی) و فیلد Influence دوباره ناپدید می‌شود؛ یک
جفت سازگار (`CINEMATIC_STYLE`+`FILM_NOIR`، هر دو `CINEMATIC` → HIGH طبق
ماتریس ADR-114) هیچ هشداری نشان نمی‌دهد؛ یک جفت ناسازگار
(`CINEMATIC_STYLE`+`MANGA`، `CINEMATIC`×`ANIMATION_2D` → LOW) متن هشدار
واقعی را زنده نشان می‌دهد.

**`StyleMatrixTest.kt`:** دو تست `rule3` به‌روزشده برای امضای تازه‌ی
`validatePrimaryStyleUpdate(VisualStyle?)` — بدون تغییر تعداد کل تست
(۱۷ تست، هم‌الگوی قبلی).

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`DnaTabFlowTest`، مجزا) | ۹ تست (۵+۴ تازه)، موفق |
| `gradle :app:testDebugUnitTest` (`StyleMatrixTest`، مجزا) | ۱۷ تست، موفق |
| `gradle :app:testDebugUnitTest` (`UiStringsTest`، مجزا) | ۳ تست، موفق (پس از رفع باگ کلید فراموش‌شده) |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۸۹۲ تست (۸۸۹+۳ تازه)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest`) — هر دو در اجرای مجزا موفق؛ همان کلاس Flake محیطی مستندشده از ADR-044 تا کنون |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (عمداً)

`PromptAssembly.kt` (اتصال به پرامپت خروجی نهایی) — طبق دستور صریح،
قدم بعدی است.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
