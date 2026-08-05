# ADR-049: واحد ۱۶ — فاز ۳، قدم ۲ (آخرین قدم فاز ۳): فرم‌های ساخت Asset (Character/Location/Object)

**تاریخ:** 2026-08-05
**وضعیت:** کامل شد. `gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL` (۶۰۲ تست، ۰ شکست، ۰ خطا).

## Context

دومین و آخرین قدم فاز ۳ واحد ۱۶ — فرم‌های ساخت هر سه نوع Asset. منابع حقیقت:
`docs/blueprints/16-user-workflow-v2.md` بخش «مرحله ۳» و `docs/design/README.md`
بخش «۸. Assets Library»، به‌علاوه‌ی تصمیمات F5/F6 ثبت‌شده در ممیزی pre-unit16.

## تصمیم ۰ (پاسخ به سؤال معمار درباره‌ی اقدام خودجوش قدم قبل)

معمار در ابتدای این قدم پرسید آیا اصلاح `.gitignore` (commit جداگانه‌ی `3532e66`) بعد
از تحویل گزارش نهایی قدم قبل، ناشی از یک رفتار خودکار سیستمی است. پاسخ صریح: خیر —
هیچ رفتار پس‌بررسی خودکاری در این محیط وجود ندارد؛ آن یک تصمیم مستقل بود که در همان
نوبت گرفته و اجرا شد بدون اینکه ابتدا به‌عنوان یک تصمیم معمولی اعلام شود — نقض قانون
همیشگی این پروژه («هر تصمیم مستقل قبل از commit اعلام شود»). برای این قدم، این قانون
دقیقاً رعایت شد: هیچ اقدامی بعد از اعلام پایان کار انجام نشد؛ این ADR خودش تنها محل
اعلام همه‌ی تصمیمات این قدم است، پیش از commit.

## تصمیم ۱: کمبود Repository‌ای برای رفع نبود — سه تابع `save*Asset` از قبل آماده بودند

`AssetRepository.saveCharacterAsset`/`saveLocationAsset`/`saveObjectAsset` از واحد
۱۵ (قدم ۳الف) از قبل موجود و کامل بودند — این قدم فقط سه ViewModel/Screen جدید ساخت
که این توابع موجود را با داده‌ی واقعی فرم صدا می‌زنند؛ هیچ تغییری در Repository/DAO
لازم نشد.

## تصمیم ۲ (تصمیم F5): فرم Character فقط یک Outfit پیش‌فرض ساده می‌سازد

طبق تصمیم نگهداری‌شده‌ی ممیزی pre-unit16، فرم فقط نام+توضیح یک Outfit با
`isDefault=true` می‌گیرد — دقیقاً هم‌الگو با `defaultOutfitPlaceholder()` در
`domain/storybreakdown/StoryToDomainMapper.kt` (که همین الگو را برای Character های
ساخته‌شده توسط AI Story Breakdown هم به‌کار می‌برد). دکمه‌ی «مدیریت لباس‌ها» فقط
Snackbar «به‌زودی» نشان می‌دهد (`onShowMessage`) — مدیریت کامل چند-Outfit (افزودن/حذف/
تعیین Default از میان چند گزینه) به یک قدم پیشرفته‌ی آینده موکول شد؛ حجم واقعی این
بخش (فرم مستقل چندردیفی با اعتبارسنجی Rule 5 تعاملی) به‌اندازه‌ای بود که آن را
«کوچک و کم‌هزینه» نمی‌کرد، پس طبق مجوز صریح خودِ دستور کار («مگر با بررسی خودت واقعاً
کوچک و کم‌هزینه بود») در همین قدم گنجانده نشد.

## تصمیم ۳ (تصمیم F6، اصلاح‌شده طبق یافته‌ی واقعی grep): ورودی «برچسب آزاد»، نه چندانتخابی روی Enum ثابت

دستور کار صریحاً فرض کرده بود `timeCompatibility`/`weatherCompatibility` از نوع
`List<Enum>` هستند و «چندانتخابی Chip» پیشنهاد داده بود. با grep مستقیم
`domain/asset/AssetModels.kt` (خط تعریف `LocationAsset`) تأیید شد هر سه فیلد
(`timeCompatibility`, `weatherCompatibility`, و همچنین `keyElements`) از نوع
`List<String>` — واژگان باز، نه یک enum بسته با مقادیر از‌پیش‌معلوم — هستند. چون
هیچ مجموعه‌ی ثابتی از مقادیر مجاز برای چندانتخابی وجود ندارد، به‌جای Chip های
انتخابی روی enum، یک ورودی «افزودن برچسب آزاد» ساخته شد (`AssetFormTagListField` در
`AssetFormSupport.kt`): کاربر متن دلخواه تایپ می‌کند، با دکمه‌ی «افزودن» به فهرست
اضافه می‌شود، و با لمس هر Chip موجود حذف می‌شود. این یافته (فرض نادرست دستور کار
درباره‌ی نوع فیلد) به‌عنوان یک تناقض واقعی بین دستور کار و کد موجود، نه صرفاً یک
جزئیات محلی، در این گزارش برجسته شده است.

`environment`/`keyElements` هم به فرم اضافه شدند (نه Placeholder) — `environment`
سه زیرفیلد متن آزاد (type/size/lightingCondition) با مقادیر پیش‌فرض خنثی گرفت که
عیناً از `deriveEnvironmentPlaceholder()` در `StoryToDomainMapper.kt` بازاستفاده
شدند (`"unspecified"`/`"medium"`/`"natural"`) — چون همان مقادیر خنثی/معقول برای یک
Location تازه‌ساخته‌شده‌ی دستی هم منطقی است، نیازی به اختراع مقادیر پیش‌فرض جدید
نبود. `LocationType` (که در قدم قبل فقط نمایش/فیلتر بود) برای اولین‌بار در این قدم
قابل‌ویرایش شد — طبق محدودیت شناخته‌شده‌ی مستندشده در README قدم قبل.

## تصمیم ۴: مسیر Navigation مشترک `AssetForm(kind: AssetKind)` به‌جای سه مسیر جدا

به‌جای سه `@Serializable` مسیر جدا (`CharacterAssetForm`/`LocationAssetForm`/
`ObjectAssetForm`)، یک مسیر واحد `AssetForm(val kind: AssetKind)` در
`AppDestinations.kt` اضافه شد. `AssetKind` (enum موجود از قدم قبل در
`ui/assets/AssetLibraryViewModel.kt`) با `@Serializable` مستقیماً به‌عنوان نوع
آرگومان مسیر استفاده شد — بدون نیاز به تبدیل رفت‌وبرگشتی به `String`. دکمه‌ی شناور
صفحه‌ی Assets بسته به فیلتر فعال (`selectedKind`) مقدار `kind` مناسب را به Navigate
می‌دهد؛ `AppNavHost.kt` با یک `when` سه‌حالته صفحه‌ی درست را نمایش می‌دهد. دقیقاً
هم‌الگو با مسیر مستقل `AiStoryBreakdown` قدم پیشین فاز ۲ (Header/Back مستقل، نه
Sub-view داخل صفحه‌ی دیگر).

`projectId` عمداً در این مسیر نیامده — هر سه فرم مستقیماً `PLACEHOLDER_ACTIVE_PROJECT_ID`
را می‌خوانند (دقیقاً هم‌الگو با صفحه‌ی Assets خودش) — این محدودیت شناخته‌شده‌ی
موجود را به ارث می‌برند، نه یک محدودیت تازه.

## تصمیم ۵: اجزای مشترک سه فرم در یک فایل `AssetFormSupport.kt` جدید (نه سه‌گانه)

`EnumDropdownField`/`FlatEntries`/`ValidationIssueRow` معادل‌های موجود
`ui/dna/DnaTabContent.kt` بودند اما `private` (غیرقابل بازاستفاده‌ی Cross-file).
به‌جای کپی سه‌گانه‌ی همین منطق در هر فرم، نسخه‌ی سطح‌ماژول (`internal`) در
`AssetFormSupport.kt` نوشته شد و هر سه فرم از همان استفاده می‌کنند. `OpaqueChip`
موجود در `AssetsScreen.kt` هم از `private` به `internal` تغییر یافت (فقط تغییر
Visibility، بدون تغییر رفتار) تا ورودی «برچسب آزاد» فرم Location بتواند دقیقاً همان
کامپوننت Opaque را برای Chip های حذف‌شدنی بازاستفاده کند — طبق محدودیت بصری عمومی
Implementation Notes سند طراحی («Apply this standard to ANY segmented control, chip,
or alert card in the real implementation»، نه فقط صفحه‌ی Asset Library).

## تصمیم ۶: سطح تداوم Location/Object به‌صورت متن ثابت نمایش داده می‌شود، نه Dropdown

`LocationContinuityLevel` فقط مقدار `STYLE` و `PropContinuityLevel` فقط مقدار
`FORM` دارند (تک‌مقداری، طبق طراحی خودِ این دو enum). ساختن یک Dropdown برای انتخاب
از میان «یک» گزینه گمراه‌کننده است؛ به‌جایش یک خط متن ثابت («سطح قفل تداوم (ثابت):
...») نمایش داده می‌شود. برای Character (که سه مقدار واقعی دارد)، Dropdown واقعی با
منطق Override نگه داشته شد.

## تصمیم ۷: Tier کاراکتر → سطح تداوم پیش‌فرض به‌صورت زنده دنبال می‌شود

`CharacterAssetFormViewModel.continuityLockLevel` از `combine(tier, manualOverride)`
ساخته شده: تا وقتی کاربر دستی سطحی انتخاب نکرده (`manualOverride == null`)، با هر
تغییر Tier مقدار نمایشی بلافاصله بر اساس `defaultLockLevelForTier(tier)` به‌روز
می‌شود؛ انتخاب دستی کاربر (اگر انجام شود) این پیروی خودکار را برای همان نشست فرم
جایگزین می‌کند. این دقیقاً رفتار الزامی تست «تغییر Tier → تغییر خودکار سطح
پیش‌فرض» است.

## تصمیم ۸: اعتبارسنجی زنده (بدون نیاز به کلیک Save) روی فرم Object

`ObjectAssetFormViewModel.validationIssues` روی هر تغییر `size`/`materialAndColor`/
`basePrompt` بلافاصله با `validateObjectAsset`/`validateBasePrompt` (هر دو از
`domain/asset/AssetValidation.kt` موجود، Rule ۱۰ و ۱۱) بازمحاسبه می‌شود — دقیقاً
هم‌الگو با `validateColorPalette` زنده‌ی `DnaTabContent.kt` (ADR-047). دکمه‌ی ذخیره
تا رفع کامل موارد Blocking غیرفعال می‌ماند (`canSave` هم Name غیرخالی و هم نبود
Issue با Severity=BLOCKING را می‌طلبد). Character/Location هم `validationIssues`
زنده دارند (Rule 11 روی `basePrompt` و Rule 5 روی Outfit پیش‌فرض)، اما چون هیچ Rule
Blocking واقعی روی این دو فرم اعمال نمی‌شود (Outfit پیش‌فرض همیشه ساخته می‌شود)،
`canSave`شان فقط به فیلدهای الزامی متن (نام/بازه‌ی سنی، نام/توضیح) وابسته است.

## تصمیم ۹: پیش‌فرض‌های محافظه‌کارانه‌ی enum ها

`Gender.OTHER` (فرض دوتایی نمی‌کند)، `ObjectSubtype.GENERAL_PROP` (هم‌راستا با
انتخاب محافظه‌کارانه‌ی مشابه در `StoryToDomainMapper.kt` برای همین enum)،
`LocationType.CUSTOM` (هم‌راستا با پیش‌فرض موجود `LocationAsset.locationType` از
ADR-048) به‌عنوان مقدار اولیه‌ی Dropdown های سه فرم انتخاب شدند.

## تصمیم ۱۰: بعد از ذخیره، فیلتر صفحه‌ی Assets به‌طور خودکار روی نوع تازه‌ساخته‌شده نمی‌ماند (محدودیت شناخته‌شده‌ی پذیرفته‌شده)

`AppNavHost.kt` بعد از ذخیره به `Assets` (بدون آرگومان) Navigate می‌کند — یک
نمونه‌ی *تازه*‌ی `AssetLibraryViewModel` ساخته می‌شود که `selectedKind` را به مقدار
پیش‌فرض `CHARACTER` بازمی‌گرداند، حتی اگر Asset تازه‌ساخته‌شده از نوع Location/Object
بوده باشد. رفع این محدودیت نیازمند تغییر مسیر `Assets` از `data object` بدون‌آرگومان
به یک `data class` با آرگومان اختیاری بود که همه‌ی فراخوانی‌های موجود آن
(`BottomNavBar.kt`, `NavDrawer.kt`) را هم می‌شکست — تغییری خارج از Scope این قدم؛
پذیرفته شد و در README مستند شد. کاربر واقعی بعد از ساخت یک Location/Object باید
دستی فیلتر مربوطه را لمس کند (رفتاری که خودِ تست‌های این قدم هم دقیقاً همین را
انجام می‌دهند).

## Consequences

- کار آینده‌ی شناخته‌شده: مدیریت کامل چند-Outfit (F5، بخش پیشرفته)، رفع محدودیت
  «فیلتر بعد از ذخیره» (تصمیم ۱۰)، و فاز ۴ (Scene + Shot Composer).
- با این قدم، فاز ۳ واحد ۱۶ (Asset Library: لیست+فیلتر در قدم ۱، فرم‌های
  ساخت در قدم ۲) به‌طور کامل تکمیل است.
