# ADR-048: واحد ۱۶ — فاز ۳، قدم ۱: صفحه‌ی Asset Library (لیست + فیلتر)

**تاریخ:** 2026-08-04
**وضعیت:** کامل شد. `gradle :app:assembleDebug :app:testDebugUnitTest` → `BUILD SUCCESSFUL` (۵۹۷ تست، ۰ شکست، ۰ خطا).

## Context

اولین قدم فاز ۳ واحد ۱۶ — صفحه‌ی Asset Library (لیست + فیلتر، بدون فرم ساخت/ویرایش
هنوز). منابع حقیقت: `docs/blueprints/16-user-workflow-v2.md` بخش «مرحله ۳» و
`docs/design/README.md` بخش «۸. Assets Library».

## تصمیم ۱ (یافته‌ی از‌پیش‌تأییدشده‌ی دستور کار): کمبود Repository رفع شد با یک Query عمومی پارامتری‌شده، نه سه متد جدا در DAO

طبق یافته‌ی صریح دستور کار، `AssetRepository` قبل از این قدم فقط `load*(ids: List<String>)` داشت. با grep تأیید شد جدول `assets` یک جدول عمومی واحد است (نه سه جدول جدا به‌ازای هر نوع) با ستون تفکیک‌کننده‌ی `assetType`. به‌جای سه متد تقریباً یکسان در `AssetDao` (`getAllCharactersForProject`/`getAllLocationsForProject`/`getAllObjectsForProject`)، یک `Query` پارامتری‌شده اضافه شد:
```kotlin
@Query("SELECT * FROM assets WHERE projectId = :projectId AND assetType = :assetType")
fun getAssetsForProjectByType(projectId: String, assetType: String): Flow<List<AssetEntity>>
```
تفکیک نوع‌محور واقعی (سه تابع Kotlin مجزا با نوع بازگشتی واقعی) در سطح `AssetRepository` انجام شد — `loadAllCharacterAssets`/`loadAllLocationAssets`/`loadAllObjectAssets(projectId): Flow<List<T>>` — دقیقاً همان API سطح‌بالایی که دستور کار خواسته بود، فقط با یک DAO Query مشترک زیرش (نه سه نسخه‌ی تکراری Room-generated).

## تصمیم ۲ (تناقض واقعی معماری، حل‌شده): `LocationType` روی `LocationAsset` وجود نداشت

هم `docs/design/README.md` («Sub-filter row ... INDOOR/OUTDOOR/MIXED for locations») و هم دستور کار این قدم («برای Locations → LocationType (INDOOR/OUTDOOR/MIXED/CUSTOM)») صریحاً یک فیلد نوع‌دار روی `LocationAsset` فرض کرده بودند — دقیقاً مثل `CharacterTier` روی `CharacterAsset` و `ObjectSubtype` روی `ObjectAsset`. با grep کامل `domain/asset/AssetModels.kt` تأیید شد **این فیلد اصلاً وجود نداشت**؛ تنها enum مشابه (`LocationType { INDOOR, OUTDOOR, MIXED, CUSTOM }`) در `domain.scene.SceneModels.kt` بود — مفهومی کاملاً متفاوت (دسته‌بندی یک Scene، نه یک LocationAsset دائمی در کتابخانه).

**تصمیم:** یک `enum class LocationType` تازه و مستقل در `domain.asset` اضافه شد (نه بازاستفاده از نسخه‌ی `domain.scene`) — هم‌راستا با اصل صریح مستندشده در `AssetContinuity.kt` («هرگز یک enum مشترک» برای مفاهیم Character/Location/Prop که فقط شباهت اسمی دارند؛ همین اصل اینجا برای Asset در برابر Scene هم اعمال شد). فیلد `locationType: LocationType = LocationType.CUSTOM` به `LocationAsset` اضافه شد — با مقدار پیش‌فرض (محافظه‌کارانه‌ترین گزینه)، کاملاً Backward Compatible: با grep هر ۴ محل ساخت واقعی این نوع تأیید شد هیچ‌کدام نمی‌شکنند (Named Arguments، مقدار پیش‌فرض جدید را بی‌صدا می‌گیرند). `LocationAssetDto`/`toDto()`/`toDomain()` هم به‌روزرسانی شدند (رشته‌ی نام Enum، هم‌الگو با `continuityLockLevel`).

این تصمیم به‌عنوان یک یافته‌ی معماری واقعی (نه فقط جزئیات محلی) در این گزارش برجسته شده — برای اطلاع معمار، نه صرفاً یک تصمیم محلی خاموش.

## تصمیم ۳: `Flow.stateIn(...)` در ViewModel، نه Collect دستی

`AssetLibraryViewModel` هم‌الگو با `ProjectListViewModel` (تنها ViewModel موجود پروژه که منبعش از قبل یک `Flow` واقعی Room بود) از `Flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())` استفاده می‌کند — نه یک `MutableStateFlow` دستی + `ioScope.launch{}` (الگوی `StoryViewModel`/`DnaViewModel`، که برای منابع suspend یک‌باره مناسب است، نه یک `Flow` مداوم). به همین دلیل `AssetLibraryViewModel` پارامتر `ioScopeOverride` ندارد — `ProjectListViewModel` هم ندارد، برای همان دلیل.

## تصمیم ۴: فیلتر زیرگروه (Sub-filter) در لایه‌ی UI اعمال می‌شود، نه در Repository/ViewModel

فهرست کامل هر نوع (بدون فیلتر) از Repository/ViewModel می‌آید؛ فیلتر بر اساس Tier/LocationType/ObjectSubtype انتخاب‌شده مستقیماً در Composable با `remember`-free `.filter{}` ساده روی لیست موجود اعمال می‌شود (تعداد آیتم‌ها در این مرحله از پروژه کوچک است، فیلتر در حافظه ارزان است) — به‌جای یک Query جدا به‌ازای هر Tier که یک ترکیب انفجاری Query/State غیرضروری می‌ساخت.

## تصمیم ۵: محدودیت بصری Opaque — پیاده‌سازی دقیق طبق دستورالعمل رسمی Implementation Notes

طبق `docs/design/README.md` بخش Implementation Notes (بند ۱): «solid/near-opaque fills (dark theme ~#212B4A / light theme white) + a visible 2dp border + drop shadow، selected/active = solid brand color with white text». این دقیقاً همان توکن‌های موجود `CinemaTheme.extendedColors.solidSurface`/`cardBorder` هستند (بدون `alpha` روی رنگ زمینه) — برای فیلتر Segmented بالا (۳ دکمه) و هر Chip زیرفیلتر/Badge سطح این صفحه اعمال شد؛ دقیقاً همان الگوی از‌پیش‌استفاده‌شده در `PhaseCircle` صفحه‌ی AI Story Breakdown (ADR-046).

## تصمیم ۶: Badge سطح، همیشه نارنجی Opaque — حتی برای Location/Object

سند طراحی «tier badge (orange)» را فقط برای Characters نام برده؛ برای یکدستی بصری (و چون خودِ سند صریحاً می‌گوید «همین قانون برای هر Badge/Chip وضعیت در این صفحه» اعمال شود)، همان کامپوننت Badge نارنجی Opaque برای زیرگروه Location/Object هم بازاستفاده شد (مقدار متن Badge = `LocationType`/`ObjectSubtype` متناظر، همان مقداری که برای زیرفیلتر هم استفاده می‌شود).

## تصمیم ۷: `projectId` این صفحه از `PLACEHOLDER_ACTIVE_PROJECT_ID` می‌آید

مسیر ریشه‌ی `Assets` (بدون آرگومان) از فاز ۰/۱ همین‌طور مانده — هیچ مفهوم «پروژه‌ی فعال/اخیر» واقعی هنوز وجود ندارد. `BottomNavBar.kt` از قبل دقیقاً همین محدودیت را برای ورودی Studio از نوار پایین با یک ثابت مستندشده (`PLACEHOLDER_ACTIVE_PROJECT_ID`) حل کرده بود؛ همان ثابت اینجا هم بازاستفاده شد (نه یک ثابت موازی جدید) — این صفحه دقیقاً همان محدودیت شناخته‌شده‌ی موجود Studio را به ارث می‌برد، نه یک محدودیت تازه.

## تصمیم ۸: دکمه‌ی «افزودن Asset جدید» فقط Snackbar «به‌زودی» است

طبق دستور کار صریح («فعلاً فقط Navigation، بدنه‌ی فرم در قدم بعدی»)، و چون مقصد واقعی (فرم ساخت) هنوز وجود ندارد، دکمه‌ی شناور از همان الگوی تثبیت‌شده‌ی `onComingSoon`/Snackbar (`NavDrawer.kt`/`MainScaffold.kt`) استفاده می‌کند — نه یک Navigation واقعی به یک مقصد ساختگی.

## تصمیم ۹: تست Contrast — تأیید کد، نه Assertion خودکار پیکسل

نسخه‌ی Compose UI Test این پروژه (تأییدشده با بررسی مستقیم API های موجود در جلسات قبلی همین گروه قدم‌ها) هیچ API استانداردی برای Assert کردن رنگ واقعی رندرشده‌ی یک پیکسل/پس‌زمینه ندارد. طبق اجازه‌ی صریح دستور کار («اگر ابزار تست پشتیبانی نمی‌کند، فقط به‌صورت دستی رعایت کن و در گزارش ذکر کن»)، این محدودیت به‌جای Assertion خودکار، با بررسی مستقیم کد رعایت شد: `OpaqueSegmentedButton`/`OpaqueChip`/`TierBadge` هرکدام صریحاً از `CinemaTheme.extendedColors.solidSurface`/`orange` (بدون `.copy(alpha=...)`) + `BorderStroke(2.dp, ...)` استفاده می‌کنند — قابل تأیید مستقیم با خواندن `AssetsScreen.kt`.

## تست

- `AssetRepositoryTest.kt` (۶ تست جدید): `loadAllCharacterAssets`/`loadAllLocationAssets`/`loadAllObjectAssets` — لیست خالی برای پروژه‌ی بدون Asset، لیست صحیح شامل همه‌ی Asset های ذخیره‌شده‌ی همان نوع/همان پروژه (نادیده‌گرفتن انواع دیگر و پروژه‌های دیگر).
- `AssetsScreenFlowTest.kt` (۳ تست End-to-End، `MainScaffold` کامل + Room واقعی In-Memory): فیلتر پیش‌فرض Characters + زیرفیلتر Tier صحیح؛ سوییچ به Locations نمایش زیرفیلتر LocationType؛ سوییچ به Objects نمایش زیرفیلتر ObjectSubtype. یافته‌ی تست: متن یک زیرفیلتر (مثلاً «کاراکتر اصلی») عمداً همان متن Badge روی کارت متناظرش هم هست (طبق طراحی) — هدف‌گیری با `testTag` به‌جای متن برای رفع ابهام لازم بود.

## Consequences

- کار آینده‌ی شناخته‌شده: فرم ساخت/ویرایش Asset (تصمیمات F5/F6) کار قدم بعدی فاز ۳ است.
- `LocationType` تازه‌اضافه‌شده هنوز در هیچ UI ویرایشی قابل تغییر نیست (فقط نمایش/فیلتر) — تنظیمش کار همان فرم ساخت/ویرایش آینده است.
