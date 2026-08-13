# ADR-075: رفع G10 (انتخاب تصویر Settings) + تصحیح G11 (یافته‌ی نادرست ممیزی)

## بخش الف — رفع G10

`SettingsScreen.kt`ی `onChooseImage` فقط پیام `comingSoonMessage` نشان
می‌داد. زیرساخت لازم از قبل در دو جای دیگر آماده بود: الگوی File Picker
واقعی (`ActivityResultContracts.GetContent()` در `ProjectsScreen.kt`،
برای Import Project) و مسیر ذخیره‌سازی (`WorkflowViewModel.setHomeScreenImageUri`،
از قبل به `SettingsScreen` وصل).

**تصمیم مستقل — `OpenDocument()` به‌جای کپی کامل `GetContent()`:** دستور
کار صریحاً خواست بررسی شود آیا Persistable URI Permission لازم است، نه
صرفاً الگوی مرجع کپی شود. با بررسی مستقل تأیید شد: `importProject`
(`ProjectListViewModel.kt`) محتوای Uri را **بلافاصله و یک‌بار** با
`contentResolver.openInputStream` می‌خواند و خودِ Uri را نگه نمی‌دارد —
برای این مورد، عدم‌نیاز `GetContent()` (پشت `ACTION_GET_CONTENT`) به
Persistable Permission بی‌اثر است. اما `homeScreenImageUri` برخلاف آن،
در DataStore Persist می‌شود و قرار است در اجراهای بعدی اپ (بعد از
بستن/باز کردن مجدد) دوباره خوانده شود — طبق مستندات پلتفرم اندروید،
`ACTION_GET_CONTENT` تضمین معتبر ماندن Uri بین اجراها را نمی‌دهد؛ فقط
`ACTION_OPEN_DOCUMENT` (Storage Access Framework) این تضمین را با
`takePersistableUriPermission` می‌دهد. به همین دلیل از
`ActivityResultContracts.OpenDocument()` استفاده شد، نه یک کپی مستقیم
از الگوی `ProjectsScreen.kt`.

پیاده‌سازی: `chooseImageLauncher` (`OpenDocument()`) با MIME Type
`"image/*"`؛ در Callback، `contentResolver.takePersistableUriPermission(uri,
Intent.FLAG_GRANT_READ_URI_PERMISSION)` (در `runCatching`، چون همه‌ی
Content Provider ها این را پشتیبانی نمی‌کنند) و سپس
`workflowViewModel.setHomeScreenImageUri(uri.toString())` — دقیقاً همان
متد موجود، بدون تغییر امضا.

**پاک‌سازی:** `comingSoonMessage`/`settings.comingSoonFeature` (fa+en)
دیگر جایی استفاده نمی‌شدند (تأییدشده با `grep`) — حذف شدند.

**مرز دامنه (طبق دستور صریح):** رندر واقعی این تصویر روی `HomeScreen.kt`
جزو این قدم نیست — همان یافته‌ی G12 (که `homeScreenImageUri` هرگز در
`HomeScreen.kt` خوانده نمی‌شود) دست‌نخورده باقی می‌ماند؛ این قدم فقط
مسیر «انتخاب و ذخیره‌ی URI» را تکمیل کرد.

## بخش ب — تصحیح G11 (یافته‌ی اصلی ممیزی نادرست بود)

بازبینی مستقل (بدون اعتماد به توضیح دستور کار) دو ادعا را تأیید کرد:

1. `buildReferenceImageInstruction` (`domain/outputdelivery/Renderer.kt:36`)
   واقعاً پیاده‌سازی شده و در `renderBlueprintToText` (خط ۶۵) فراخوانی
   می‌شود — تأییدشده با `MIGRATED (docs/adr/030-...)` در همان فایل.
2. `ImageReference` (واحد ۰۵) و `ReferenceImage` (واحد ۰۶) هر دو فقط
   فیلد محلی/متنی دارند (`type/localFilePath/description` و
   `localFilePath/description`)؛ خودِ بلوپرینت ۱۴ (خط ۳۸) صریحاً می‌گوید
   ارجاع به تصویر باید «یک جمله‌ی دستوری کلی» باشد، نه نام/مسیر فایل —
   «دقیقاً هماهنگ با تصمیم بنیادی قبلی این پروژه (`ReferenceImage`، واحد
   ۰۶) که هرگز نام/مسیر فایل را در متن پرامپت درج نمی‌کند».

**نتیجه:** یافته‌ی اصلی G11 («Attached References فقط توضیح متنی است»)
یک توصیف نادرست از یک طراحی عمدی و کامل بوده، نه یک Gap. `docs/audit/post-unit16-full-audit.md`
اصلاح شد (فقط بخش «وضعیت نهایی» که خودش قبلاً افزودنی بود؛ متن اصلی
گزارش دست‌نخورده ماند).

**یافته‌ی جانبی (نه دلیلی برای رد تصحیح بالا):** `validateImageReferenceFile`
(`domain/shot/ShotValidation.kt`) و `validateReferenceImageFile`
(`domain/asset/AssetValidation.kt`) واقعاً انتظار یک فایل واقعی روی
دیسک را دارند (`fileExists: (String) -> Boolean`) — اما این‌ها همان
Rule های یتیم G14 هستند که در ADR-064 قبلاً «Post-MVP، بدون Infra»
تصمیم‌گیری و مستند شده‌اند؛ وجودشان نشان می‌دهد یک زیرساخت فایل واقعی
یک روز ممکن است ساخته شود، اما رفتار فعلی و وایرشده‌ی اپ (که همیشه
`localFilePath=""` است و هرگز این Rule ها را صدا نمی‌زند) دقیقاً همان
طراحی متن‌محور عمدی بالا را دنبال می‌کند.

## راستی‌آزمایی

- `gradle :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` →
  موفق.
- `gradle :app:testDebugUnitTest` روی `ui/settings` و `ui/i18n` → ۱۱
  تست، ۰ شکست (یک تست تازه: کلیک دکمه‌ی «انتخاب تصویر» بدون Crash اجرا
  می‌شود و `homeScreenImageUri` بدون نتیجه‌ی واقعی Picker دست‌نخورده
  می‌ماند — شبیه‌سازی واقعی نتیجه‌ی Picker در این کدبیس هیچ Precedent ی
  ندارد، حتی برای `importLauncher` مشابه در `ProjectsScreen.kt`).

## Skills استفاده‌شده

`zero-hallucination-coder` — هر دو بخش این قدم صریحاً خواستند بررسی
مستقل به‌جای اعتماد به توضیح؛ بخش الف با خواندن مستقیم رفتار
`ACTION_GET_CONTENT` در برابر `ACTION_OPEN_DOCUMENT` (نه کپی کورکورانه‌ی
الگوی مرجع)، بخش ب با خواندن مستقیم بلوپرینت‌های ۰۶/۱۴ و کد واقعی
`Renderer.kt` (نه فقط تکرار توضیح دستور کار).
