# ADR-057: واحد ۱۶ — فاز ۵، قدم ۳ (آخرین قدم) — صفحه‌ی Output Delivery؛ فاز ۵ کامل شد

**تاریخ:** 2026-08-08
**وضعیت:** کامل شد. `gradle :app:testDebugUnitTest :app:assembleDebug` → نتیجه‌ی واقعی در پایین این سند.

## Context

آخرین قدم فاز ۵ — طبق تصمیم مستند ADR-056، این قدم هر دو وظیفه‌ی «تولید
Prompt» و «تحویل خروجی» را پوشش می‌دهد (هیچ صفحه‌ی مستقل «Prompt
Generation» ساخته نشد). منابع حقیقت: `docs/design/README.md` بخش «۱۰.
Output Delivery» و `docs/blueprints/16-user-workflow-v2.md` مراحل ۶-۸.

## تصمیم ۱ (توافق با پیش‌بررسی معمار): منبع «هزینه‌ی Token» — نه Option A، نه Option B، یک گزینه‌ی سوم بهتر

معمار پیش‌بررسی صریح کرده بود: `Renderer.kt`/`RenderedOutput` هیچ فیلد Token
Count واقعی ندارند و از او خواسته بود بین **Option A** (ساخت یک تخمین
UI-level کاراکتر→توکن) و **Option B** (تغییر برچسب به «شمار کاراکتر») یکی
را انتخاب کند (با ترجیح صریح خودش برای A).

**یافته‌ی تازه که هر دو گزینه را غیرضروری کرد:** grep مستقیم
`domain/outputdelivery/ModelProfileLibrary.kt` و `ModelProfiles.kt` نشان
داد `ModelConstraints.maxTokens: Int` از قبل روی هر ۱۴ پروفایل (۱۳ مدل
واقعی + `universal_default`) مقداردهی شده و دقیقاً با جدول «Model List»
سند طراحی مطابقت دارد (Veo=500، Kling=625، Seedance=500، HappyHorse=500،
Runway=250، Luma=375، Hailuo=500، Wan=500، HunyuanVideo=500، LTX=500،
Vidu=375، Midjourney=250، SD3=125، Universal=500).

**تصمیم نهایی:** به‌جای اختراع یک تخمین تازه (A) یا کنار گذاشتن مفهوم
Token (B)، همین فیلد از‌پیش‌موجود و دقیق مستقیماً هم روی چیپ مدل و هم در
خط هزینه‌ی Output Preview نمایش داده شد — به‌عنوان یک «هزینه‌ی ثابت هر
تولید» (منطبق با روش قیمت‌گذاری واقعی بسیاری از API های ویدیوی تجاری —
هزینه‌ی ثابت هر Generation، نه شمارش زنده‌ی توکن هر پرامپت)، **مستقل** از
هشدار Over-limit واقعی (که همچنان از `validatePromptLength` واقعی —
شمارش کاراکتر در برابر `maxPromptLength` — می‌آید، بدون تغییر). این دو
عدد دو منبع واقعی و مستقل‌اند، نه یکی از دیگری محاسبه‌شده. این گزینه‌ی
سوم به معمار گزارش می‌شود، نه یک سرپیچی خاموش از دستور کار.

## تصمیم ۲: هماهنگی Warnings با صفحه‌ی Validation (نه یک منبع کاملاً جدا)

بررسی `assemblePromptBlueprint` (`domain/promptengine/PromptAssembly.kt`)
نشان داد پارامتر `validationIssues: List<ValidationIssue>` از بیرون
تزریق می‌شود (نه داخلی محاسبه‌شده) و `summarizeConflictResolution` فقط
موارد WARNING را برای `PromptBlueprint.warnings` فیلتر می‌کند. پس دقیقاً
همان تجمیع‌کننده‌ی ADR-055 (`aggregateShotValidation`) این‌جا هم به‌عنوان
همان پارامتر تزریق شد — منبع واحد، نه دو منطق مستقل موازی. علاوه بر آن،
دو هشدار **واقعاً تازه** هم نمایش داده می‌شوند که ساختاری در تجمیع سطح-Shot
جایی نداشتند (چون به یک `profile` انتخاب‌شده نیاز دارند که صفحه‌ی
Validation اصلاً مفهومش را ندارد): `validatePromptLength` (Over-limit) و
`validateUnsupportedFeatureUsage` (Renderer.kt، هر دو واحد ۱۴). این صفحه
از هم Shot Composer و هم Validation قابل‌دسترس است، پس نمایش دوباره‌ی
هشدارهای سطح-Shot اینجا تکرار بی‌فایده نیست — ورودی مستقیم از Shot
Composer اصلاً هرگز این هشدارها را ندیده.

## تصمیم ۳: معنای دکمه‌ی «بازتولید» — محاسبه‌ی تازه، نه تصادفی‌سازی

بررسی `Renderer.kt` نشان داد فیلد `seed` روی `PromptBlueprint` (از
`manageSeed`، مقداری کاملاً Deterministic وابسته به `shotId`) هیچ‌جای
منطق واقعی رندر متن استفاده نمی‌شود. پس با داده‌ی یکسان، خروجی «بازتولید»
همیشه یکسان است — این دکمه در این پروژه یعنی «زنجیره را از صفر با
داده‌ی *فعلی* دوباره اجرا کن» (مفید اگر کاربر Shot را جای دیگری ویرایش
کرده و به این صفحه برگشته)، نه یک تولیدکننده‌ی تصادفی/نسخه‌ی جایگزین —
چون دامنه اصلاً چنین مکانیزمی ندارد.

## تصمیم ۴: محل ذخیره‌ی مدل انتخاب‌شده — `WorkflowState`، نه DataStore

سند طراحی «Selected output model» را در همان فهرست تخت
State Management کنار Language/Theme/`shotListViewMode` می‌آورد (یک
Preference سطح-UI برای طول یک نشست، نه داده‌ی دامنه‌ی پایدار). دقیقاً
همان الگوی موجود `WorkflowState.shotListViewMode`/
`WorkflowViewModel.setShotListViewMode` برای `selectedModelProfileId`/
`setSelectedModelProfileId` تکرار شد — طول یک نشست Studio، بدون نوشتن در
DataStore.

## تصمیم ۵: بازاستفاده از `OpaqueChip` موجود برای چیپ‌های مدل

سند طراحی چیپ‌های مدل را با «گرادیان بنفش برای حالت انتخاب‌شده» توصیف
می‌کند، اما هیچ چیپ دیگری در کل این کدبیس گرادیان واقعی ندارد (فقط رنگ
Solid). به‌جای ساخت یک نوع Chip تازه فقط برای این صفحه، `OpaqueChip`
موجود (`ui/assets/AssetsScreen.kt`) عیناً بازاستفاده شد — رنگ حالت
انتخاب‌شده‌اش (`MaterialTheme.colorScheme.primary`) از قبل کاملاً
Solid/Opaque است، دقیقاً همان الزام Contrast صریح سند طراحی، بدون نیاز
به یک کامپوننت Bespoke.

## تصمیم ۶: Grid چیپ Wrap‌شونده — تقسیم دستی به ردیف، نه `LazyVerticalGrid`/`FlowRow`

`LazyVerticalGrid(columns = GridCells.Adaptive(...))` تو در توی یک
`Column` با اسکرول عمودی از قبل موجود، نیازمند ارتفاع صریح/محاسبه‌شده
است — با `Adaptive` تعداد ستون واقعی فقط در Runtime معلوم می‌شود، پس هر
فرمول ثابتی برای ارتفاع اشتباه خواهد بود (یک منبع باگ شناخته‌شده‌ی
Compose). `FlowRow` هم تا این قدم هیچ‌جای این کدبیس استفاده نشده (بدون
سابقه‌ی اثبات‌شده در این نسخه‌ی Compose پروژه). به‌جای این دو، فهرست ۱۴
پروفایل با `chunked(2)` به ردیف‌های ثابت تقسیم شد — بدون هیچ ریسک
Nested-Scroll یا محاسبه‌ی ارتفاع.

## تصمیم ۷ (طراحی مسیر تست‌پذیری): `promptGenerationRepository` تا `MainScaffold` رشته شد

بر خلاف تصمیم اولیه‌ی این قدم (که فرض کرده بود این Repository همیشه
`null` باشد و ViewModel خودش از `AppDatabase.getInstance` بسازد)، بررسی
الگوی تست‌های موجود این واحد (`ValidationFlowTest`، `ShotsFlowTest` و
بقیه) نشان داد الگوی مستقر همیشه یک زنجیره‌ی End-to-End واقعی از طریق
`MainScaffold` با Repository های وصل به یک Room In-memory تست است، نه
مونتاژ مستقیم صفحه به‌تنهایی. برای هماهنگی با این الگو،
`promptGenerationRepository: PromptGenerationRepository? = null` دقیقاً
مثل پنج Repository دیگر تا `App.kt` → `MainScaffold.kt` → `AppNavHost.kt`
→ `OutputDeliveryScreen` رشته شد (در `App.kt` واقعی هم از همان `database`
مشترک ساخته و تزریق می‌شود، نه رها به پیش‌فرض).

## یافته‌ی واقعی دیباگ ۱: Race واقعی در `regenerate()` (باگ کد محصول، رفع شد)

هنگام نوشتن تست «عوض‌کردن مدل → هشدار Over-limit»، نتیجه بین اجراهای
مکرر ناسازگار بود (گاهی هشدار پیدا می‌شد، گاهی اصلاً پیدا نمی‌شد) — نشانه‌ی
کلاسیک یک Race، نه یک باگ Deterministic. علت واقعی: `regenerate()` هر بار
یک Coroutine تازه در `ioScope.launch` باز می‌کرد **بدون لغو** هر
Coroutine قبلی هنوز درحال‌اجرا. با انتخاب سریع دو مدل پشت‌سرهم (یا حتی
فقط انتخاب اولین مدل غیر-پیش‌فرض بلافاصله بعد از `init { regenerate() }`ی
پیش‌فرض)، اگر زنجیره‌ی قدیمی‌تر (کندتر) دیرتر از زنجیره‌ی تازه به پایان
برسد، آخرین نوشتن روی `_state.value` نتیجه‌ی قدیمی/نامرتبط را جایگزین
نتیجه‌ی تازه می‌کند — یک باگ واقعی در کد محصول (نه فقط تست)، چون کاربر
واقعی هم می‌تواند همین رفتار «انتخاب سریع چند مدل» را انجام دهد. رفع:
`OutputDeliveryViewModel` اکنون یک `private var regenerateJob: Job?` نگه
می‌دارد و هر فراخوان تازه‌ی `regenerate()` ابتدا `regenerateJob?.cancel()`
را صدا می‌زند، پیش از باز کردن Job تازه.

## یافته‌ی واقعی دیباگ ۲: باگ Fixture تست (نه باگ کد محصول)

هنگام نوشتن `OutputDeliveryFlowTest.kt`، فیکسچر Shot اولیه (کپی از
`ValidationFlowTest`، که فقط `lighting` را Override می‌کند) باعث شکست
مکرر با `ComposeTimeoutException` روی انتظار کارت پیش‌نمایش می‌شد. یک تست
تشخیصی موقت و مستقیم روی `PromptGenerationRepository.collectData` (بدون
Compose، حذف‌شده پس از دیباگ) دقیقاً علت را نشان داد:
`resolveCameraSettingsFor`/`resolveEnvironmentSettingsFor`
(`SettingsResolutionRepository`) همیشه `sceneDefault=null` عبور می‌دهند
(تصمیم مستند ADR-013)، پس اگر `camera`/`environment` خودِ Shot هم
Override نداشته باشند، `resolveSourcedSettings` با `IllegalStateException`
شکست می‌خورد. `ValidationFlowTest` هرگز این مسیر را فرا نمی‌خواند (فقط
`aggregateShotValidation` مستقیم)، پس این محدودیت آن‌جا هرگز آشکار
نمی‌شد — یک تفاوت واقعی و مستند بین دو صفحه، نه تناقض. رفع: فیکسچر
`OutputDeliveryFlowTest` اکنون هر سه (`camera`/`lighting`/`environment`)
را Override می‌کند (مقادیر نمونه از فیکسچر موجود و کارآمد
`PromptGenerationRepositoryTest.kt` گرفته شد).

## یافته‌ی واقعی دیباگ ۳: خواندن `ClipboardManager` از Context خودِ Activity میزبان (بهبود، نه علت اصلی)

در مسیر دیباگ، `ClipboardManager` تست از `ApplicationProvider.
getApplicationContext()` به `composeRule.activity` (پس از تغییر Rule به
`createAndroidComposeRule<ComponentActivity>()`) تغییر کرد — دقیقاً همان
Context ای که واقعاً پشت `LocalClipboardManager.current` داخل درخت
Compose است. این تغییر به‌خودی‌خود مشکل را حل نکرد (تست همچنان شکست
می‌خورد)، اما یک بهبود درست و ماندگار است (هم‌راستا با نحوه‌ی واقعی
دسترسی Compose به این سرویس)، پس نگه داشته شد. علت واقعی شکست جای دیگری
بود — یافته‌ی ۴ زیر.

## یافته‌ی واقعی دیباگ ۴ (علت اصلی): بدون `performScrollTo`، کلیک‌های Semantics روی گره‌های خارج از Viewport فعلی اثر واقعی ندارند

هم دکمه‌ی Copy و هم چیپ Stable Diffusion SD3 در انتهای یک `Column` بلند و
اسکرول‌شونده قرار دارند (۱۴ چیپ مدل + کارت پیش‌نمایش + بخش Warnings).
`performClick()`/`performSemanticsAction(OnClick)` ساده روی این گره‌ها
بدون هیچ Exception ای «موفق» گزارش می‌شد (گره پیدا می‌شد، `OnClick` هم در
`config` آن حضور داشت) اما Handler واقعی هرگز اجرا نمی‌شد — نه
`clipboardManager.setText`، نه حتی یک `println` تشخیصی داخل خودِ Lambda
تولید (که مستقیماً داخل کد محصول موقتاً اضافه و بعد حذف شد) هرگز چاپ
نشد. یک تست مستقل Compose (بدون Navigation/Drawer، فقط یک دکمه‌ی ساده)
با همان `LocalClipboardManager.setText` **درست** کار کرد — یعنی خودِ
مکانیزم Clipboard/Compose سالم بود؛ مشکل مختص گره‌های خارج از Viewport
این صفحه‌ی خاص (پشت اسکرول) بود. رفع: هم‌الگو با `clickViaSemantics()`ی
موجود در `DnaSoftLockWarningTest.kt` (که همین دقیقاً همین ترکیب را از
قبل استفاده می‌کرد)، `clickViaSemantics()` این فایل هم اکنون ابتدا
`performScrollTo()` را صدا می‌زند، سپس `performSemanticsAction(OnClick)`
را — برای هر پنج نقطه‌ی کلیک این تست (هر سه چیپ مدل + دکمه‌ی Copy).

## تست‌ها (`OutputDeliveryFlowTest.kt`، End-to-End، Robolectric)

- انتخاب چیپ Midjourney → خروجی واقعاً رندرشده با پیشوند واقعی
  `command_string` آن پروفایل («/imagine prompt:») شروع می‌شود — اثبات
  مستقیم اینکه `RenderedOutput` واقعاً متناظر مدل انتخاب‌شده است.
- Shot با محتوای طولانی + انتخاب Stable Diffusion SD3 (کوچک‌ترین
  `maxPromptLength`=۵۰۰) → متن دقیق هشدار واقعی `validatePromptLength`
  نمایش داده می‌شود.
- دکمه‌ی Copy → متن واقعی Clipboard سیستم (`android.content.ClipboardManager`)
  واقعاً حاوی متن رندرشده‌ی همین شات است.
- چیپ‌های مدل (انتخاب‌شده و انتخاب‌نشده) هر دو کاملاً قابل‌مشاهده‌اند —
  هم‌الگو با یافته‌ی Contrast مستندشده‌ی ADR-055.
- عوض‌کردن مدل هدف (Universal→Runway) → خط هزینه‌ی Token داخل کارت
  پیش‌نمایش واقعاً به `maxTokens` پروفایل تازه (۵۰۰→۲۵۰) به‌روز می‌شود.

## نتیجه‌ی نهایی Build

`gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`، ۶۴۰
تست (۶۳۵→۶۴۰، ۵ تست جدید)، ۰ Failure، ۰ Error.

## Consequences

- `Export` فعلاً فقط Copy to Clipboard با پیام تأیید متفاوت است (هیچ
  Infra نوشتن فایل/Share Intent واقعی در کل کدبیس وجود ندارد — هم‌الگو با
  محدودیت‌های ثبت‌شده‌ی مشابه قبلی این پروژه، مثل انتخاب‌گر تصویر ADR-051).
- **فاز ۵ واحد ۱۶ (Validation → Output Delivery) به‌طور کامل تکمیل شد.**
- قدم مستقل بعدی: فاز ۶ (رفتارهای سراسری + Settings/Backups).
