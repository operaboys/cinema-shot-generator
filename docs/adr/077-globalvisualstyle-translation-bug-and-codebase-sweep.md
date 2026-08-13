# ADR-077: رفع باگ ترجمه‌ی G8 (نمایش نام خام Enum) + بررسی سراسری همین کلاس باگ

## زمینه

`SceneDetailScreen.kt` خط ۳۴۵ (اضافه‌شده در ADR-076، رفع G8) عمداً خط
نمایش `globalVisualStyle` را دست‌نخورده گذاشته بود، طبق دستور صریح آن
قدم — نتیجه: بعد از انتخاب یک سبک از Dropdown تازه، Overview نام خام
Enum (مثلاً `"FILM_NOIR"`) را نشان می‌دهد، نه برچسب فارسی
`visualStyleLabel`. این قدم آن را به‌عنوان یک باگ واقعی (نه یک محدودیت
پذیرفته‌شده) رفع می‌کند، طبق قانون بنیادی پروژه: هر متنی که در خودِ
رابط کاربری نمایش داده می‌شود باید کامل ترجمه شود — فقط متن پرامپت
خروجی نهایی (برای مدل‌های AI) مجاز است انگلیسی بماند.

## بخش الف — رفع خط مشخص

`SceneDetailScreen.kt`ی `InfoRow` مربوط به `globalVisualStyleLabel`
اکنون `scene.globalVisualStyle.override` را (اگر غیر-null) با
`VisualStyle.valueOf(it)` به enum تبدیل و از `visualStyleLabel(...,
language)` عبور می‌دهد — دقیقاً هم‌الگو با نحوه‌ی مصرف همین تابع در
Dropdown همان فایل (که در ADR-076 درست اضافه شده بود). تست
`ScenesFlowTest.kt` (که در ADR-076 عمداً همین رفتار غلط را
Assert کرده بود) برعکس شد — اکنون برچسب فارسی را Assert می‌کند،
نه نام خام.

## بخش ب — بررسی سراسری همین کلاس باگ

با `grep` مستقل روی کل `ui/` (سه الگو طبق دستور کار: `.name`/`.toString()`
مستقیم در Text/Label، فیلدهای `String?` که مقدار `.name` یک enum را
ذخیره می‌کنند، و مقادیر `.override`/`.rawValue`-مانند بدون تابع Label
میانی) فهرست کامل زیر به دست آمد.

### موارد واقعی — رفع شدند (۲ مورد، جدا از بخش الف)

| فایل:خط | قبل | بعد |
|---|---|---|
| `ui/home/HomeScreen.kt:215` | `Icon(Icons.Filled.Translate, contentDescription = language.name)` | `contentDescription = uiString("home.toggleLanguageButton", language)` |
| `ui/home/HomeScreen.kt:220` | `Icon(..., contentDescription = theme.name)` | `contentDescription = uiString("home.toggleThemeButton", language)` |

این دو `contentDescription` (متن صفحه‌خوان/Screen Reader دکمه‌های
سوییچ زبان/تم در Home) مقدار خام `AppLanguage.name`/`AppTheme.name`
(مثلاً `"FA"`/`"DARK"`) را نشان می‌دادند — تنها دو استثنای این الگو در
کل کدبیس؛ **هر** `contentDescription` دیگر در پروژه (۱۳ مورد دیگر
بررسی‌شده) از قبل `uiString(...)` واقعی دارد. دو کلید تازه اضافه شدند
(`home.toggleLanguageButton`، `home.toggleThemeButton`، fa+en) —
هیچ‌جای دیگر کدبیس/تستی این متن قبلی را استفاده نمی‌کرد.

**یادداشت صادقانه:** برخلاف `Text`/`InfoRow` که در دستور کار صریح نام
برده شده بود، این دو مورد `contentDescription` روی `Icon` هستند —
از نظر فنی صفحه‌خوان (Accessibility) است، نه متن بصری روی صفحه. اما
همان اصل («هر متن UI باید ترجمه شود») دقیقاً همین‌جا هم صادق است —
مقایسه‌ی مستقیم با ۱۳ `contentDescription` دیگر پروژه (همه با
`uiString`) این را قطعی تأیید کرد، نه فرض.

### موارد بررسی‌شده و رد‌شده — False Positive (با دلیل دقیق)

| فایل:خط | الگو | چرا False Positive است |
|---|---|---|
| `ui/shots/ShotComposerViewModel.kt:479-481` | `_weatherType.value.name.lowercase()` و مشابه | پارامتر یک تابع دامنه (`mapEnvironmentToSound`)، نه متن UI — هم‌الگو با محدودیت مستند `checkStaticCameraInChase` (G14) |
| `ui/navigation/MainScaffold.kt:106`، `AppNavHost.kt:220` | `SceneDetailTab.SHOTS.name` | کلید داخلی Navigation Route (`initialTab: String`)، هرگز نمایش داده نمی‌شود |
| `ui/scenes/SceneDetailScreen.kt:112,137` | `SceneDetailTab.OVERVIEW.name` / `it.name == initialTab` | بازسازی State داخلی از همان کلید Route بالا، نه نمایش |
| `ui/studio/StudioShell.kt:167` | `StudioTab.entries.find { it.name == initialTab }` | همان الگو، بازسازی Tab انتخابی از کلید داخلی |
| `ui/workflow/WorkflowViewModel.kt` (۶ محل) | `language.name`/`theme.name`/... در `dataStore.edit` | ذخیره‌سازی DataStore (سریال‌سازی Preference)، نه نمایش — مقدار خوانده‌شده همیشه از طریق `*Label`/Radio با متن ترجمه‌شده نمایش داده می‌شود (تأییدشده در قدم‌های قبلی همین Suite) |
| `ui/validation/ValidationViewModel.kt:141` | `RuleSeverity.valueOf(issue.severity.name)` | پل داخلی بین دو enum دامنه (`Severity`↔`RuleSeverity`)، هرگز در UI نمایش داده نمی‌شود |
| `ui/assets/AssetsScreen.kt:457,513,569`، `SceneDetailScreen.kt:226,328,424`، `*FormViewModel.kt` (`_name.value = asset.name` و مشابه) | `.name` روی `CharacterAsset`/`LocationAsset`/`ObjectAsset`/`Outfit` | این‌ها فیلد `name: String` **آزاد و کاربر-وارد‌شده** (مثل «پل فرماندهی کشتی»)اند، نه `Enum.name` — هیچ enum ای درکار نیست |
| `ui/shots/ShotListScreen.kt:203`، `ShotComposerViewModel.kt:308,319,330`، `CameraTabContent.kt:122,128` | `camera.source`/`.source == "override"` | مقدار خام `SourcedSettings.source` (`"scene"`/`"override"`) فقط برای انتخاب/شرط منطقی مصرف می‌شود؛ برچسب‌های واقعاً نمایش‌داده‌شده (`cameraTab.sourceSceneLabel`/`sourceOverrideLabel`) از قبل `uiString` دارند |
| `ui/shots/CameraTabContent.kt:282,289,305`، `ShotComposerViewModel.kt:304`، `SettingsScreen.kt:248`، `ui/dna/DnaViewModel.kt:74`، `ValidationScreen.kt:260`، `AiStoryBreakdownScreen.kt:485` | `someNumber.toString()` (`Int`/`Float`) | مقادیر عددی (کانون دوربین، مدت‌زمان، تعداد) — نه enum، نیازی به ترجمه ندارند |

### پوشش تکمیلی (تأیید مثبت، نه یافته‌ی تازه)

با `grep` سراسری همه‌ی ۲۵ محل فراخوان `AssetFormEnumDropdownField` در کل
پروژه بازبینی شدند — **هر ۲۵ مورد دیگر** (به‌جز همان یک مورد رفع‌شده در
ADR-076/بخش الف همین ADR) از یک تابع `*Label(...)` واقعی برای
`selectedLabel` استفاده می‌کنند. همچنین هر دو `InfoRow`-مانند Composable
کل پروژه (`SceneDetailScreen.InfoRow`، `SettingsScreen.PrivacyInfoRow`)
بررسی شدند — `PrivacyInfoRow` هر سه فراخوانش را با `uiString` واقعی
پر می‌کند.

## راستی‌آزمایی

- `gradle :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` →
  موفق.
- `gradle :app:testDebugUnitTest` روی `ui/scenes`، `ui/i18n`،
  `ui.HomeProjectsStudioFlowTest` (اجرای ترکیبی) → ۱۱ تست، ۲ شکست — هر
  دو در `HomeProjectsStudioFlowTest` (Timeout/NavBackStackEntry
  Lifecycle)، بی‌ربط به `git diff` این قدم (فقط دو خط `contentDescription`
  تغییر کردند). اجرای مجزای همین فایل → **۴ تست، ۰ شکست** — تأیید شد
  این دو شکست، همان کلاس Flake محیطی از‌پیش‌مستند این پروژه هستند (نه
  Regression این قدم).
- `ScenesFlowTest`/`UiStringsTest` (شامل تست برعکس‌شده‌ی بخش الف) →
  ۷ تست، ۰ شکست.

## Skills استفاده‌شده

`zero-hallucination-coder` — بررسی سراسری با `grep` واقعی روی کل `ui/`
(نه حدس)، هر مورد پیداشده جداگانه بررسی و با دلیل مشخص یا رفع شد یا
False Positive علامت خورد؛ برای `contentDescription` (که فراتر از
الگوی دقیق دستور کار بود) با مقایسه‌ی مستقیم با ۱۳ نمونه‌ی دیگر پروژه
تصمیم گرفته شد، نه با حدس.
