# ADR-087: دکمه‌های سریع زبان/تم روی هدر هر صفحه‌ی داخلی (یافته‌ی #۱۶ appendix ADR-081)

## زمینه

طبق appendix ADR-081 (یافته‌ی #۱۶)، فقط `HomeScreen.kt` (`HomeHeader`) دو
دکمه‌ی میان‌بر Translate/تم داشت؛ همه‌ی صفحات داخلی دیگر پروژه فقط دکمه‌ی
برگشت + عنوان داشتند — یعنی تغییر زبان/تم از هر صفحه‌ی دیگر جز Home ممکن
نبود، مگر با برگشت کامل به Home یا رفتن به Settings.

## تصمیم قطعی (از پیش با کاربر گرفته‌شده)

این دو دکمه به **هر** صفحه‌ی داخلی پروژه اضافه شد، اما **بدون** ساختن یک
Composable هدر مشترک تازه یا بازنویسی ساختاری هدرهای موجود — فقط تزریق دو
`IconButton` به هدر موجود هر صفحه، دقیقاً هم‌الگو با منطق شرطی
`HomeHeader` (آیکون `Translate` برای زبان؛ `LightMode`/`DarkMode` بسته به
`AppTheme` فعلی برای تم). دلیل: ریسک باگ پنهان ناشی از یک Refactor بزرگ
روی ۱۲+ صفحه پذیرفته نشد؛ تکرار کد کوچک (چند خط `IconButton` در هر فایل)
قابل‌قبول‌تر از آن ریسک بود.

## مسیر پیاده‌سازی — دو دسته‌ی صفحه

### دسته ۱ — ۸ صفحه‌ی مصرف‌کننده‌ی `AssetFormHeader` مشترک (`ui/assets/AssetFormSupport.kt`)

`AssetFormHeader` (که از قبل توسط ۸ صفحه بازاستفاده می‌شد) گسترش یافت:
پارامترهای تازه‌ی `language`/`theme`/`onToggleLanguage`/`onToggleTheme` +
دو `toggleLanguageTestTag`/`toggleThemeTestTag` اختیاری. چون این پارامترها
الزامی‌اند (نه Optional با پیش‌فرض)، هر ۸ فراخوان (نه فقط تعریف) باید
به‌روزرسانی می‌شد — دقیقاً هم‌قاعده با `language` که در تمام صفحات این
پروژه از قبل الزامی بوده. صفحات:
- `ShotComposerScreen.kt`
- `ui/settings/SettingsScreen.kt`
- `ui/backups/BackupsScreen.kt`
- `ui/outputdelivery/OutputDeliveryScreen.kt`
- `ui/validation/ValidationScreen.kt`
- `ui/assets/ObjectAssetFormScreen.kt`
- `ui/assets/LocationAssetFormScreen.kt`
- `ui/assets/CharacterAssetFormScreen.kt`

`SettingsScreen.kt` استثنای جزئی است: چون `language`/`theme` از قبل
مستقیماً از `workflowViewModel` (پارامتر موجود این صفحه) خوانده می‌شوند،
نیازی به پارامتر تازه در امضای تابع نبود — فقط
`workflowViewModel::setLanguage`/`setTheme` مستقیماً در محل فراخوانی
`AssetFormHeader` وصل شد (همان الگوی از پیش موجود این فایل برای
`LanguageThemeCard`).

سه فرم Asset (`Character`/`Location`/`Object`) قبلاً `backTestTag` را به
`AssetFormHeader` پاس نمی‌دادند (دکمه‌ی برگشت تگ نداشت) — یک
`*_FORM_BACK_BUTTON_TAG` تازه هم برای هرکدام اضافه شد، هم‌قدم با همین کار،
چون بدون تگ دکمه‌ی برگشت، تست‌پذیری این صفحات ناقص می‌ماند.

### دسته ۲ — ۳ صفحه‌ی هدر خصوصی/محلی

هرکدام هدر Composable مستقل خودشان دارند (نه `AssetFormHeader`)؛ همان دو
`IconButton` مستقیماً به بدنه‌ی همان Composable خصوصی اضافه شد:
- **`SceneDetailScreen.kt`** (`SceneDetailHeader`) — `theme: AppTheme`
  پارامتر تازه به امضای صفحه اضافه شد (قبلاً فقط `language` داشت)؛
  `AppNavHost.kt` آن را از `workflowViewModel.theme` تزریق می‌کند.
- **`StudioShell.kt`** (`StudioHeader`) — کم‌ریسک‌ترین مورد: چون
  `workflowViewModel` از قبل پارامتر این صفحه بود و همان‌جا
  `language`/`setLanguage`/`setTheme` در دسترس‌اند، فقط یک خط
  `collectAsStateWithLifecycle()` تازه برای `theme` اضافه شد — بدون هیچ
  تغییر در امضای عمومی `StudioShell` یا در `AppNavHost.kt`.
- **`AiStoryBreakdownScreen.kt`** (`BreakdownHeader`) — مثل SceneDetail،
  `theme: AppTheme` پارامتر تازه؛ `AppNavHost.kt` از `workflowViewModel`
  تزریق می‌کند. دکمه‌ی برگشت این صفحه هم قبلاً تگ نداشت —
  `AI_BREAKDOWN_BACK_BUTTON_TAG` تازه اضافه شد.

## استثنا — هیچ صفحه‌ای واقعاً موکول نشد

هر ۱۲ فایل فهرست‌شده در دستور کار بررسی و کامل شد؛ هیچ‌کدام آنقدر
تودرتو/پیچیده نبود که تزریق ساده‌ی دو دکمه واقعاً نیازمند بازنویسی
ساختاری بزرگ‌تر باشد — حتی `StudioShell.kt` (هدر مشترک بین ۴ Tab) و
`SceneDetailScreen.kt` (هدر با منوی Overflow موجود) هر دو فقط به افزودن
دو `IconButton` به همان `Row` موجود نیاز داشتند.

## یافته‌ی واقعی رفع رگرسیون تست

ابتدا تست‌های دکمه‌ی تازه برای `BackupsScreen`/`ValidationScreen` با
`performScrollTo()` قبل از کلیک نوشته شدند (کپی از الگوی دکمه‌های دیگرِ
پایین‌تر همان صفحات که واقعاً نیاز به Scroll دارند) — و با خطای واقعی
`Semantic Node has no parent layout with a Scroll SemanticsAction` شکست
خوردند. یافته‌ی واقعی: هدر (شامل این دو دکمه‌ی تازه) همیشه در بالای صفحه و
بیرون از هر `verticalScroll` است، پس از ابتدا کاملاً دیده‌شونده است —
`performScrollTo()` غیرلازم و باعث خطا بود. رفع: حذف `performScrollTo()`،
فقط `clickViaSemantics()` مستقیم.

## پیاده‌سازی — خلاصه‌ی فایل‌ها

- **`ui/assets/AssetFormSupport.kt`**: `AssetFormHeader` گسترش‌یافته.
- ۸ فایل دسته ۱ (بالا) + ۳ فایل دسته ۲ (بالا).
- **`ui/navigation/AppNavHost.kt`**: تزریق `theme`/`onToggleLanguage`/
  `onToggleTheme` به هر ۹ مقصدی که این پارامترها را نیاز داشتند (`Settings`/
  `StudioShell` از `workflowViewModel` داخلی خودشان استفاده می‌کنند، نیازی
  به تغییر در AppNavHost نداشتند).
- کلیدهای ترجمه‌ی تازه اضافه نشد — همان دو کلید موجود
  (`home.toggleLanguageButton`/`home.toggleThemeButton`) بازاستفاده شدند،
  چون این دو رشته از قبل عمومی بودند، نه مخصوص Home.

## تست

- رفع فایل تست موجود `ValidationOverrideFlowTest.kt` (تنها فراخوان مستقیم
  `ValidationScreen(...)` بیرون از `AppNavHost`) — پارامتر تازه‌ی `theme`
  اضافه شد.
- ۳ تست End-to-End واقعی تازه (طبق دستور کار، برای نمونه‌ی معقول، نه هر
  ۱۲ صفحه) — هرکدام ثابت می‌کند کلیک واقعاً `WorkflowViewModel.language`/
  `.theme` را عوض می‌کند، نه فقط این‌که دکمه‌ها رندر می‌شوند:
  - `BackupsFlowTest.kt`
  - `ValidationFlowTest.kt`
  - `AssetFormFlowTest.kt` (فرم Character)

## راستی‌آزمایی نهایی

| بررسی | نتیجه |
|---|---|
| `gradle :app:compileDebugKotlin` + `:app:compileDebugUnitTestKotlin` | موفق |
| `gradle :app:testDebugUnitTest` (کل Suite) | **۷۵۳ تست، ۰ شکست** |
| `gradle :app:assembleDebug` | موفق |

## قدم بعدی پیشنهادی

باقی‌مانده‌ی appendix ADR-081: نبود هدر سراسری یکسان بین صفحات (تصمیم
معماری بزرگ‌تر، عمداً خارج از دامنه‌ی این قدم — طبق تصمیم قطعی کاربر، فقط
تزریق دکمه بدون یکسان‌سازی ساختاری هدرها).

## Skills استفاده‌شده

`zero-hallucination-coder` — فهرست ۱۲ فایل قبل از شروع مستقلاً با
`grep "onBack: () -> Unit"` در `ui/` تأیید شد (دقیقاً همان ۱۲ فایل).
`decision-record` — استثنای `SettingsScreen`/`StudioShell` (بدون پارامتر
تازه، از `workflowViewModel` موجود استفاده) و رفع رگرسیون
`performScrollTo` با دلیل صریح در همین سند ثبت شدند. `proportional-effort`
— طبق تصمیم قطعی کاربر، هیچ Composable هدر مشترک تازه‌ای ساخته نشد؛
تکرار کوچک کد به‌جای Refactor بزرگ انتخاب شد.
