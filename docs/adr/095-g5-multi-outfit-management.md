# ADR-095: پیاده‌سازی G5 — مدیریت چند-Outfit شرطی برای Character Asset

## زمینه

طبق ADR-067 بخش ه («G5: چند-Outfit — تصمیم مستقل، موکول شد»)، این کار از
قدم‌های قبلی عمداً جدا نگه داشته شده بود: `CharacterAssetFormViewModel.kt`
فقط دو `MutableStateFlow<String>` مجزا (`_outfitName`/`_outfitDescription`)
داشت، `save()` همیشه دقیقاً یک `Outfit` می‌ساخت، و دکمه‌ی «مدیریت لباس‌ها»
فقط یک Snackbar Placeholder بود. دامنه (`Outfit`/`OutfitCondition`،
`selectOutfitForScene`، `enforceCharacterContinuity`) از قبل کامل و صحیح
آماده بود — طبق دستور این قدم، **هیچ‌کدام تغییر نکردند**؛ این قدم فقط UI را
به آن‌ها وصل می‌کند.

## بررسی مستقل (پیش از نوشتن کد)

خواندن کامل سه فایل دامنه، دقیقاً همان‌طور که پیش‌بررسی گفته بود:
- `AssetModels.kt`: `Outfit(id, name, description, isDefault, condition: OutfitCondition?)`، `OutfitCondition(weather, timeOfDay, locationType)` — هر سه فیلد شرط `String?` (نه enum) در سطح دامنه.
- `AssetSelection.kt`: `selectOutfitForScene(outfits, sceneWeather, manualOverrideId)` — فقط `condition.weather` را مقایسه می‌کند (`firstOrNull { it.condition?.weather == sceneWeather }`)؛ `timeOfDay`/`locationType` عملاً هنوز در هیچ منطق تطبیقی خوانده نمی‌شوند (محدودیت شناخته‌شده‌ی دامنه‌ی موجود، نه چیزی که این قدم باید حل کند).
- `CharacterContinuity.kt`: `enforceCharacterContinuity` واقعاً `selectOutfitForScene` را صدا می‌زند.

## تصمیم ۱ — سه فیلد condition: enum بسته، نه رشته‌ی آزاد

با grep در دامنه مشخص شد سه enum بسته‌ی از‌قبل‌موجود دقیقاً همین سه مفهوم را
پوشش می‌دهند:
- `weather` → `WeatherType` (`domain/sceneconditions/EnvironmentModels.kt`) — همان enumی که `sceneWeather` واقعی در `PromptAssembly.kt` از آن می‌سازد (`weatherType.name.lowercase()`).
- `timeOfDay` → `TimeOfDay` (`domain/scene/SceneModels.kt`، واحد ۰۴).
- `locationType` → `LocationType` (`domain/scene/SceneModels.kt`) — **نه** `domain.asset.LocationType` (که هم ۴ مقدار یکسان دارد اما طبق کامنت صریح خودِ `AssetModels.kt` عمداً یک مفهوم جدا است: دسته‌بندی فیلتر کتابخانه‌ی Asset، نه نوع مکان یک Scene).

هر سه فیلد در ViewModel با `.name.lowercase()` ذخیره می‌شوند — هم‌قرارداد با
`sceneWeather` واقعی، تا اگر matching آینده به `timeOfDay`/`locationType` هم
گسترش یابد، بدون نیاز به تغییر Casing کار کند. توابع برچسب (`weatherTypeLabel`،
`timeOfDayLabel`، `sceneLocationTypeLabel`) هر سه از قبل در پکیج‌های دیگر
(`ui/shots`، `ui/scenes`) عمومی تعریف شده بودند — بازاستفاده شدند، تکرار
نشدند.

## تصمیم ۲ — رفتار مرزی «حذف Outfit پیش‌فرض/آخرین Outfit»

با grep هیچ الگوی مشابه موجودی در پروژه (حذف از یک لیست با دقیقاً یک عضو
الزامی) پیدا نشد — این اولین مورد است. دو زیرتصمیم:

1. **حذف تنها Outfit باقی‌مانده مسدود می‌شود (no-op ساختاری در ViewModel،
   نه فقط یک ValidationIssue قابل‌نادیده‌گرفتن).** دلیل: `selectOutfitForScene`
   /`selectOutfitForShot` دقیقاً `outfits.first { it.isDefault }` را روی یک
   لیست خالی صدا می‌زنند — `NoSuchElementException` واقعی در زمان اجرا (نه
   صرفاً نقض Rule 5). تضمین ساختاری در سطح ViewModel («لیست هرگز خالی
   نمی‌شود») امن‌تر از تکیه‌ی صرف بر Validation/canSave است — هم‌راستا با
   الگوی «تضمین ساختاری، نه Runtime» که قبلاً در ADR-006/ADR-093 دیده شده.
2. **حذف Outfit پیش‌فرض (وقتی حداقل یک عضو دیگر باقی می‌ماند)** اولین عضو
   باقی‌مانده را خودکار پیش‌فرض می‌کند — Rule 5 (`validateDefaultOutfitExists`،
   از قبل در `ValidationAggregator` Level 3 وایر) هرگز نقض نمی‌شود.

دفاع دوم (نه اول): `canSave` اکنون علاوه‌بر `name`/`ageRange`، `outfits.isNotEmpty()`
را هم بررسی می‌کند — پوشش حالت مرزی «ویرایش یک Character موجود که مستقیم
(نه از طریق این فرم) با `outfits = emptyList()` ساخته شده بود» (چنین حالتی
در تست‌های E2E موجود پروژه واقعاً وجود دارد،
`AssetFormFlowTest.kt`).

`_outfits` همیشه با دقیقاً یک Outfit پیش‌فرض («Default» + توضیح خالی) شروع
می‌شود — همان مقدار پیش‌فرض قدیمی (`_outfitName = MutableStateFlow("Default")`)
— تا یک Character تازه‌ساز بدون هیچ تعامل کاربر هم Rule 5 را ارضا کند (رفتار
قبلی، بدون تغییر).

## تصمیم ۳ — UI: Card ردیفی، نه Chip

هم‌الگو با `ActionSoundsSection` (`ui/shots/AudioTabContent.kt`): empty-state
وقتی لیست خالی است، فرم «افزودن» ثابت پایین (State محلی، پاک‌شونده بعد از
افزودن). برخلاف Sound Chips (فقط نمایش+حذف با یک برچسب تخت)، هر ردیف Outfit
به تعامل بیشتری نیاز دارد (پیش‌فرض‌کردن، سه Dropdown شرط) — به‌جای `OpaqueChip`
از یک `Card` ردیفی استفاده شد (هم‌الگو با `LinkedAssetCard` در
`ui/scenes/SceneDetailScreen.kt`). سه Dropdown شرط از یک نسخه‌ی محلی
`NullableEnumDropdownField` استفاده می‌کنند — نسخه‌ی اصلی همین الگو در
`ui/shots/LightingEnvironmentTabContent.kt` `private` است (غیرقابل import از
این پکیج)؛ تکرار محلی همان تصمیم طراحی خودِ آن فایل است (کوچک، تک‌فایلی)، نه
یک اشتباه DRY.

## تست

- `CharacterAssetFormViewModelTest.kt` (تازه — اولین تست ViewModel-level این
  کلاس): ۸ تست — شروع خودکار با یک Outfit پیش‌فرض، افزودن با شرط مستقل،
  عدم-پیش‌فرض‌بودن Outfit تازه، حذف غیرپیش‌فرض، حذف پیش‌فرض (ترفیع خودکار)،
  حذف تنها Outfit باقی‌مانده (مسدود)، `setOutfitAsDefault` (خودکار غیرفعال‌کردن
  قبلی)، رفت‌وبرگشت کامل سه فیلد condition (null↔enum).
- یک تست عمداً نوشته **نشد**: `validationIssues` (Rule 5) — این Flow از
  `combine(...).stateIn(WhileSubscribed)` ساخته می‌شود؛ بدون Collector واقعی
  یا `ioScopeOverride = Dispatchers.Unconfined` (که این فایل هم‌الگو با
  `AiStoryBreakdownViewModelTest.kt` عمداً ندارد)، `.value` فقط مقدار اولیه
  را برمی‌گرداند — تستی آنجا چیزی واقعی اثبات نمی‌کرد.
- `AssetFormFlowTest.kt`: ۱ تست E2E تازه — افزودن یک Outfit دوم از فرم واقعی
  (نمایش در لیست)، سپس حذف آن (ناپدید شدن از لیست).

## راستی‌آزمایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` | موفق |
| `gradle :app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (`CharacterAssetFormViewModelTest`، `AssetFormFlowTest`) | موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | ۳ شکست نامرتبط (`AssetFormFlowTest` یک تست قدیمی، `OutputDeliveryFlowTest`، `ValidationFlowTest`) — flaky شناخته‌شده‌ی محیط Sandbox؛ هر سه در اجرای مجزا موفق |
| `gradle :app:assembleDebug` | موفق |

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد — این یک پیاده‌سازی کد مستقیم
بود (خواندن دامنه‌ی موجود + پیروی از الگوی UI مرجع مشخص‌شده در دستور کار)،
بدون نیاز به هیچ‌کدام از Skillهای تخصصی UI/معماری/تست موجود در این محیط.
