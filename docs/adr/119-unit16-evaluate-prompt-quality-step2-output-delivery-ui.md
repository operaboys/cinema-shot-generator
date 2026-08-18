# ADR-119: هوشمندسازی evaluatePromptQuality — قدم ۲ از ۳ زیرقدم: اتصال به UI — نمایش «شاخص کیفیت نوشتاری پرامپت (سریع نه دقیق)» در OutputDeliveryScreen (فقط مسیر ۱، بدون AI)

## زمینه

ADR-118 (قدم ۱) `evaluatePromptQuality` را با دو سیگنال تحقیق‌شده
(کلمات مبهم، MATTR) هوشمندتر کرد، اما تا این قدم هرگز از هیچ‌جای UI
فراخوانی نمی‌شد — کاربر هیچ راهی برای دیدن این محاسبه نداشت. این قدم
آن را به مسیر ۱ (بدون AI) `OutputDeliveryScreen` وصل می‌کند؛ مسیر
۲/AI اختیاری (اگر لازم شود) قدم ۳ این برنامه است.

**عنوان دقیق کارت («شاخص کیفیت نوشتاری پرامپت (سریع نه دقیق)» فارسی،
"Quick Prompt Quality Index (fast, not precise)" انگلیسی) یک تصمیم
محتوایی/متنی مستقیم از معمار و کاربر پروژه است — نه پیشنهاد Claude
Code. متن عیناً همان‌طور که در دستور کار این قدم آمده، بدون هیچ تغییری
پیاده‌سازی شد.**

## بررسی مستقل — تأیید کامل پیش‌بریفینگ، بدون هیچ تفاوتی

با خواندن کامل `OutputDeliveryViewModel.kt` (خصوصاً `regenerate()` و
`OutputDeliveryState.Ready`)، `OutputDeliveryScreen.kt` کامل (خصوصاً
`OutputPreviewCard` و محل صدا زدنش)، `QualityScore`/`evaluatePromptQuality`
در `WorkflowModels.kt`، و `OutputDeliveryViewModelTest.kt` پیش از
نوشتن هر خط کد: `blueprint` واقعاً یک `val` محلی در `regenerate()` بود
(خط ۱۶۹)؛ `finalRenderedOutput` واقعاً در خط ۲۱۰ ساخته می‌شد؛
`OutputDeliveryState.Ready` دقیقاً همان ۵ فیلد پیش‌بینی‌شده را داشت؛
`OutputPreviewCard` دقیقاً در خطوط ۱۴۸-۱۶۶ صدا زده می‌شد،
`OUTPUT_DELIVERY_PREVIEW_CARD_TAG` دقیقاً در خط ۶۰. هیچ‌جای کدبیس
`OutputDeliveryState.Ready(...)` را جز خودِ `regenerate()` نمی‌سازد
(تأییدشده با `grep`) — پس افزودن فیلد تازه هیچ تستی را نمی‌شکست.
**بدون هیچ انحراف واقعی.**

## پیاده‌سازی

**۱. `OutputDeliveryViewModel.kt`:** `OutputDeliveryState.Ready` یک
فیلد تازه گرفت: `val qualityScore: QualityScore`. در `regenerate()`،
بلافاصله بعد از `finalRenderedOutput`:
```kotlin
val qualityScore = evaluatePromptQuality(finalRenderedOutput, blueprint)
```
طبق تصمیم صریح دستور کار، فقط `QualityScore` نهایی نگه داشته می‌شود
— نه کل `blueprint` (نگه‌داشتن کل blueprint در state برای این یک
مصرف — نمایش یک کارت اطلاعاتی — بی‌دلیل بزرگ‌تر از لازم بود).

**۲. `OutputDeliveryScreen.kt`:** `PromptQualityCard(score, language)`
تازه — هم‌الگوی دقیق `OutputPreviewCard` (همان `Card`/رنگ‌بندی
`CinemaTheme`). عنوان از `uiString` (نه هاردکد)؛ امتیاز کل برجسته
(`${score.total}/100`)؛ پنج زیرمعیار هرکدام یک ردیف کوچک با برچسب
فارسی/انگلیسی متناظر با معنای همان محور (طبق کامنت `QualityScore` در
`WorkflowModels.kt`) و مقدار از ۲۰. **هیچ رنگ خطا/Blocking برای امتیاز
پایین** — کاملاً اطلاعاتی، هم‌راستا با کامنت خودِ
`evaluatePromptQuality` («کمک به کاربر، نه بلاک‌کردن جریان کار»).

**۳. محل فراخوانی:** بلافاصله بعد از `OutputPreviewCard` و پیش از
`WarningsSection`. توجیه ترتیب: این کارت مستقیماً درباره‌ی همان متنی
است که در `OutputPreviewCard` دیده شد (نه یک بخش مستقل جدا)، پس
منطقی است بلافاصله زیرش بیاید؛ `WarningsSection` (که می‌تواند به
دلایل کاملاً متفاوتی مثل تضاد Validation باشد، نه فقط کیفیت نوشتاری)
بعد از آن می‌آید.

**۴. testTag:** `OUTPUT_DELIVERY_QUALITY_CARD_TAG` — هم‌الگوی
`OUTPUT_DELIVERY_PREVIEW_CARD_TAG`.

## تست

**`OutputDeliveryViewModelTest.kt` (۱ تست تازه):** اثبات اتصال
End-to-End — نه تکرار تست مقدار عددی دقیق (که در `WorkflowModelsTest.kt`،
ADR-118 پوشش داده شده)، بلکه اثبات اینکه `regenerate()` واقعی این
ViewModel واقعاً `evaluatePromptQuality` را صدا می‌زند: بعد از
`regenerate().join()`، `state.qualityScore.total` در بازه‌ی معتبر
۰ تا ۱۰۰ است، و برابر مجموع پنج زیرمعیار است (سازگاری داخلی
`QualityScore.total`).

**`UiStringsTest.kt`:** بدون شکست — این‌بار (برخلاف ADR-115) هر دو
نقشه (فارسی/انگلیسی) از همان ابتدا با هم اضافه شدند.

**`OutputDeliveryFlowTest.kt`:** بدون هیچ تغییر کد، در اجرای مجزا سبز
ماند — کارت تازه چیدمان موجود صفحه را نمی‌شکند.

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`OutputDeliveryViewModelTest`، مجزا) | موفق (شامل ۱ تست تازه) |
| `gradle :app:testDebugUnitTest` (`OutputDeliveryFlowTest`، مجزا) | موفق، بدون تغییر |
| `gradle :app:testDebugUnitTest` (`UiStringsTest`، مجزا) | ۳ تست، موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۹۰۵ تست (۹۰۴+۱ تازه)، ۲ شکست نامرتبط (`AssetFormFlowTest`، `OutputDeliveryFlowTest`) — هر دو در اجرای مجزا موفق؛ همان کلاس Flake محیطی مستندشده از ADR-044 تا کنون |
| `gradle :app:assembleDebug` | موفق |

## خارج از Scope این قدم (عمداً)

- منطق `evaluatePromptQuality`/`scoreTextRichness` (ADR-118) دست‌نخورده.
- معماری AI Connector/`SecureKeyRepository` — مسیر ۲/AI اختیاری قدم ۳
  این برنامه است، اینجا فقط مسیر ۱ (بدون AI) وصل شد.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
