# ADR-144: بستن شکاف حیاتی چکاپ نهایی — اتصال واقعی Hard/Soft Lock تداوم به فرم‌های Asset

## وضعیت

پذیرفته‌شده

## زمینه

چکاپ جامع نهایی (ADR-142/143) مهم‌ترین یافته‌ی خودش را کشف کرد: تضمین
سرلوحه‌ی پروژه — «سطح FULL همیشه Blocking، بدون استثنا» (مستند در
`README.md`، `docs/blueprints/06-asset-and-continuity-v2.md`،
`docs/blueprints/00-master-overview-v2.md`) — عملاً در اپ اجرا
نمی‌شد. `validateCharacterUpdate`/`validateLocationUpdate`/
`validatePropUpdate` (`domain/asset/AssetContinuity.kt`) از هیچ
Screen/ViewModel واقعی صدا زده نمی‌شدند؛ فقط تست واحد خودشان
(`AssetContinuityTest.kt`) آن‌ها را می‌خواند. یک کاربر می‌توانست
ظاهر/نام یک کاراکتر MAIN (سطح FULL) را آزادانه تغییر دهد و ذخیره کند.

طبق دستور صریح، منطق این قدم **از قبل کاملاً طراحی‌شده** بود
(`docs/blueprints/06-asset-and-continuity-v2.md`، امضای دقیق سه تابع)؛
این قدم فقط آن طراحی را با UI موجود وصل می‌کند.

## راستی‌آزمایی مستقل (پیش از نوشتن کد)

- `AssetContinuity.kt` کامل خوانده شد: `validateCharacterUpdate(level,
  rules, fieldBeingChanged)` رشته‌های دقیق `"name"`, `"asset_id"`,
  `"physical_appearance"`, `"age_range"` را می‌خواند — تأییدشده با
  `AssetContinuityTest.kt` موجود. `validateLocationUpdate`/
  `validatePropUpdate` هر دو پارامتر `fieldBeingChanged: String` را
  عملاً نادیده می‌گیرند — فقط `isStyleField`/`isFormField` نتیجه را
  تعیین می‌کند (تأییدشده با خواندن مستقیم بدنه‌ی هر دو تابع).
- Grep سراسری `UpdateResult` در `app/src/main/java/` تأیید کرد **هیچ
  `when` غیر-Exhaustive موجودی** روی این نوع وجود ندارد — هشدار صریح
  بلوپرینت («قبل از افزودن `Warned`...») از یک Migration قدیمی‌تر
  (ADR-029) بود که همان‌جا حل شده بود؛ این قدم چیزی برای اصلاح در این
  بخش نداشت.
- `CharacterAsset.continuityRules` همیشه `ContinuityRules()` پیش‌فرض
  است (هیچ UI برای سفارشی‌کردنش وجود ندارد) — تأییدشده با خواندن
  `CharacterAssetFormViewModel.save()` موجود.

## تصمیم

### بخش الف — Character (Hard/Soft Lock واقعی)

`CharacterAssetFormViewModel`: یک تصویر کامل `CharacterAsset` بارگذاری‌شده
(`loadedCharacterAsset`) نگه‌داری می‌شود (نه فقط `loadedUpdatedAt`
موجود). در `save()`، پیش از ذخیره، اگر Asset موجودی در حال ویرایش است
(نه ساخت جدید)، فیلدهای واقعاً تغییریافته با مقدار قدیمی مقایسه
می‌شوند:

- `name` مقایسه‌ی مستقیم → `"name"`.
- `physicalAppearance.ageRange` مستقل مقایسه می‌شود → `"age_range"`.
- بقیه‌ی `physicalAppearance` (با خنثی‌کردن `ageRange` پیش از مقایسه)
  → `"physical_appearance"`.

این تفکیک عمدی است: `appearanceLock`/`ageLock` دو قفل کاملاً مستقل‌اند
(طبق امضای سه‌آرگومانی تابع) — یک تغییر فقط در سن نباید قفل ظاهر را هم
فعال کند. `asset_id` بررسی نمی‌شود چون هیچ فیلد فرمی برای تغییرش وجود
ندارد (شناسه در طول ویرایش ثابت است).

اگر هر فیلد تغییریافته `Blocked` برگرداند، `save()` **واقعاً متوقف
می‌شود** — هیچ نوشتن روی دیسک اتفاق نمی‌افتد. `Warned` هرگز مانع
نمی‌شود (طبق تعریف پروژه‌ای Warning). نتیجه در یک StateFlow تازه
(`continuityIssues: List<ValidationIssue>`) قرار می‌گیرد و در
`CharacterAssetFormScreen` با کامپوننت موجود
`AssetFormValidationIssueRow` نمایش داده می‌شود (بدون کامپوننت جدید —
دقیقاً همان الگوی رنگ/آیکون Blocking در برابر Warning که برای Rule
های دیگر همین فرم از قبل استفاده می‌شد).

### بخش ب — Location/Object (Soft Lock واقعی، فقط Warning)

هر دو تابع دامنه هرگز `Blocked` برنمی‌گردانند (سطح STYLE/FORM). این
قدم فقط تعیین می‌کند **کدام فیلد** معادل «سبک بصری»/«فرم ظاهری» است —
تصمیمی که در خودِ بلوپرینت مشخص نشده بود (فقط مثال تست با نام رشته‌ای
دلخواه `"visual_style"`/`"size"` بود، بدون اتصال به فیلد واقعی دامنه):

- **Location:** `environment` (سه زیرفیلد `type`/`size`/
  `lightingCondition`) به‌عنوان معادل «سبک بصری مکان» (Rule ۸) انتخاب
  شد — مستقیم‌ترین فیلد موجود که واقعاً ظاهر/نورپردازی مکان را توصیف
  می‌کند. بقیه‌ی فیلدها (`name`/`description`/`locationType`/
  `timeCompatibility`/`weatherCompatibility`/`keyElements`/
  `basePrompt`) سبک بصری محسوب نشدند.
- **Object:** `size`/`materialAndColor` به‌عنوان معادل «فرم ظاهری شیء»
  (Rule ۹) انتخاب شدند — دقیقاً همان دو فیلدی که Rule ۱۰ (Blocking
  موجود) هم الزامی می‌داند، و مستقیماً فرم فیزیکی شیء را توصیف می‌کنند.
  بقیه‌ی فیلدها (`name`/`description`/`subtype`/`specialTrait`/
  `basePrompt`) فرم ظاهری محسوب نشدند.

این یک تصمیم طراحی واقعی این قدم است (نه صرفاً «وصل‌کردن») — چون
پیامدش فقط یک پیام Warning است (هرگز Blocking)، ریسک اشتباه پایین است؛
اگر معمار تشخیص دهد فیلد دیگری هم باید Warning بدهد، افزودنش یک تغییر
محلی کوچک در `checkContinuityBeforeSave` هر ViewModel است.

### بخش ج — پیامد صریح روی Character Evolution

بلوپرینت ۰۶ به یک مسیر رسمی «Evolution» اشاره می‌کند که تغییر آگاهانه‌ی
ظاهر یک کاراکتر FULL را (بیرون از مسیر ویرایش مستقیم فرم) امکان‌پذیر
می‌کرد. این مکانیزم هرگز پیاده نشده بود و در همین چکاپ (ADR-142) به‌عنوان
کد کاملاً مرده (`EvolutionEntry`/`EvolutionTimeline`) حذف شد. **طبق
دستور صریح، ساخت این مسیر در دامنه‌ی این قدم نبود.** پیامد واقعی: از
این پس، کاربری با یک کاراکتر FULL **هیچ راه رسمی** برای تغییر ظاهرش
ندارد — نه از فرم (اکنون Blocked)، نه از Evolution (وجود ندارد) — تا
وقتی این فیچر جدا طراحی و ساخته شود. این یک محدودیت شناخته‌شده‌ی جدید
است، نه یک باگ — باید در README مستند شود.

## پیامدها

- فایل‌های تغییریافته: `CharacterAssetFormViewModel.kt`,
  `LocationAssetFormViewModel.kt`, `ObjectAssetFormViewModel.kt` (منطق
  اتصال)؛ `CharacterAssetFormScreen.kt`, `LocationAssetFormScreen.kt`,
  `ObjectAssetFormScreen.kt` (نمایش `continuityIssues`)؛
  `CharacterAssetFormViewModelTest.kt`,
  `LocationAssetFormViewModelTest.kt`,
  `ObjectAssetFormViewModelTest.kt` (تست‌های جدید).
  **`AssetContinuity.kt` دست‌نخورده ماند** — طبق راستی‌آزمایی بالا، هیچ
  `when` غیر-Exhaustive ای برای اصلاح پیدا نشد.
- کاراکتر MAIN/FULL اکنون واقعاً محافظت می‌شود — تلاش برای ذخیره‌ی
  تغییر نام/ظاهر/سن روی دیسک بی‌اثر می‌ماند و یک پیام Blocking واضح
  نشان داده می‌شود.
- کاراکتر SECONDARY/MEDIUM، Location، و Object اکنون هشدارهای واقعی
  (نه فقط تست‌شده در انزوا) هنگام تغییر فیلدهای حساس نشان می‌دهند —
  بدون مسدودکردن ذخیره.
- یک محدودیت جدید (بخش ج بالا) باید به `README.md` اضافه شود.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` →
  `BUILD SUCCESSFUL`.
- تست‌های هدفمند
  (`CharacterAssetFormViewModelTest`/`LocationAssetFormViewModelTest`/
  `ObjectAssetFormViewModelTest`/`AssetContinuityTest`) با
  `--rerun-tasks`: **۶۵ تست، ۰ شکست**.
- `CharacterAssetFormViewModelTest` به‌تنهایی ۵ بار با `--rerun-tasks`
  اجرا شد (بررسی Flakiness طبق یافته‌ی دیباگ زیر) — هر ۵ بار
  `BUILD SUCCESSFUL`.
- یافته‌ی دیباگ واقعی (`systematic-debugging`، مستند در کامنت‌های
  تست): دو تست جدید (`FULL`/`MEDIUM`) در اولین اجرا شکست خوردند —
  علت اول (رفع‌شده): `continuityLockLevel` هم مثل `canSave` یک
  `combine(...).stateIn(WhileSubscribed)` روی `viewModelScope` واقعی
  است؛ بدون یک Collector واقعی، مقدارش روی پیش‌فرض اولیه (`FULL`) گیر
  می‌کند. علت دوم (رفع‌شده): مثل تمام تست‌های `save()`ی دیگر همین فایل،
  یک `shadowOf(Looper.getMainLooper()).idle()` صریح پیش از `save()`
  لازم بود — Robolectric خودش این را با پیام «Main looper has queued
  unexecuted runnables» تأیید کرده بود.
- اجرای کامل `./gradlew :app:testDebugUnitTest` و
  `./gradlew :app:assembleDebug` بعد از این تغییرات در قدم پایانی
  گزارش نهایی مستند شده‌اند.
