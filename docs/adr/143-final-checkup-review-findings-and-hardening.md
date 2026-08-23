# ADR-143: بازبینی نهایی پیش از تکمیل — دسته‌ی دوم (یافته‌های بازبینی‌های موازی + سخت‌سازی امنیتی)

## وضعیت

پذیرفته‌شده (بخش دوم و پایانی زنجیره‌ی ADR-142)

## زمینه

طبق ADR-142، چهار بخش تحقیقاتی باقی‌مانده‌ی بازبینی نهایی (تکمیل
`readme-audit`، بازبینی خصمانه‌ی `adversarial-reviewer` روی واحد ۱۹،
بازبینی UI/Compose، بازبینی امنیتی) به‌صورت موازی در Subagentهای
پس‌زمینه اجرا شدند. این ADR یافته‌های واقعی آن‌ها و اصلاحات محلی
(غیرمعماری) اعمال‌شده را مستند می‌کند.

### یافته‌های `readme-audit` (تکمیل، رفع‌شده)

Subagent مستقل با خواندن کامل بقیه‌ی `README.md` سه ناهماهنگی دیگر پیدا
کرد که با Grep مستقل تأیید شدند:

1. **بند تکراری «Choose Image» در محدودیت‌های Settings (خط ~۲۷۸۹):**
   همان ادعای قدیمی («هیچ File Picker ای نیست») که در بند دیگری از همین
   فایل (خط ۲۸۱۳، اصلاح‌شده در ADR-142) قبلاً اصلاح شده بود، یک نسخه‌ی
   تکراری در بند دیگری (محدودیت‌های Settings) داشت که جا مانده بود.
   اصلاح شد + ارجاع متقابل به بند دیگر اضافه شد.
2. **شمار Room Entity قدیمی (خط ۲۶۵۵):** «۱۳ Room Entity» — با
   `ls`ی مستقیم پوشه‌ی `data/entity/` تأیید شد ۱۴ فایل واقعی وجود دارد؛
   `StoryBreakdownSessionEntity` (ADR-121، `version = 2` با Migration
   واقعی در `AppDatabase.kt`) از فهرست جا افتاده بود. اصلاح شد.
3. **پوشه‌ی `ui/common/` در درخت ساختار (بین `i18n/` و بقیه):** با
   `ls` تأیید شد این پوشه از ADR-124 وجود دارد (`RetranslateButton.kt`)
   اما هرگز به درخت ساختار خلاصه اضافه نشده بود. اضافه شد.
4. **یادداشت روشن‌کننده (نه یک باگ، صرفاً وضوح بیشتر):** بند محدودیت
   «ترجمه‌ی واقعی فارسی پرامپت» می‌توانست با ویژگی جداگانه‌ی
   `translateToFarsi` (ADR-121 تا ۱۲۴، برای Preview فیلدهای Story
   Breakdown) اشتباه گرفته شود — یک جمله‌ی توضیحی برای تفکیک این دو
   مسیر اضافه شد.
5. **کامنت کد قدیمی در `SettingsScreen.kt:366-372`:** هنوز وضعیت
   «به‌زودی» (پیش از ADR-075) را توصیف می‌کرد، درحالی‌که همان فایل از
   ADR-075 یک File Picker واقعی دارد (`chooseImageLauncher`، خط ۱۵۳).
   این خارج از خودِ `README.md` بود اما یک اصلاح محلی مستقیم و
   بدون‌ریسک است — کامنت به‌روزرسانی شد.

### یافته‌های `adversarial-reviewer` روی واحد ۱۹ (رفع‌شده)

بازبینی خصمانه‌ی Subagent روی کل زنجیره‌ی «آپلود عکس مرجع واقعی» +
«پرامپت ساخت عکس مرجع» (ADR-131 تا ۱۴۱) دو یافته‌ی واقعی پیدا کرد
(علاوه بر چند مورد بررسی‌شده و **تأیید‌شده به‌عنوان غیرباگ** — نگاه
کنید به بخش راستی‌آزمایی):

1. **شکست بی‌صدای `takePersistableUriPermission` (هر سه فرم Asset):**
   `CharacterAssetFormScreen.kt`/`LocationAssetFormScreen.kt`/
   `ObjectAssetFormScreen.kt` هر سه دقیقاً همان الگو را دارند — نتیجه‌ی
   `runCatching { takePersistableUriPermission(...) }` نادیده گرفته
   می‌شد و `addReferenceImage` بدون قید و شرط اجرا می‌شد. اگر این فراخوان
   شکست بخورد (مثلاً یک Content Provider که Grant پایدار پشتیبانی
   نمی‌کند)، عکس مرجع در همان Session نمایش داده می‌شود اما بعد از
   بستن اپ/ریبوت گوشی خاموش‌شدن دسترسی بی‌صدا اتفاق می‌افتد — بدون هیچ
   نشانه‌ای برای کاربر یا در Log. این یافته مستقل هم توسط بازبینی
   امنیتی (پایین‌تر) تأیید شد.
   **این یک اصلاح محلی و غیرمعماری است** (فقط افزودن یک `Log.w` روی
   شکست، هم‌الگو دقیق با پیشینه‌ی `resolveInitialProfileId`/ADR-126) —
   رفتار موجود تغییر نکرد، فقط دیگر بی‌صدا نیست.
2. **کاما دوتایی در پرامپت با description خالی:** در
   `ImagePromptEngine.kt`، `buildOutfitImagePrompt`/
   `buildLocationImagePrompt`/`buildObjectImagePrompt` فیلد
   `description` (غیر-nullable `String`) را بدون محافظت
   `takeIf(isNotBlank)` اضافه می‌کردند — برخلاف فیلدهای اختیاری خواهر
   در همان توابع (`basePrompt`/`specialTrait`/`defaultMood`) که این
   محافظت را داشتند. `AssetValidation.kt` هیچ Rule Blocking ای روی
   description خالی ندارد (فقط Warning)، پس یک description خالی واقعاً
   می‌توانست به تولید شود و به یک کاما دوتایی قابل‌مشاهده در متن پرامپت
   نهایی (`"... , , ..."`) منتهی شود. **اصلاح محلی**: همان الگوی
   `takeIf(isNotBlank)` به هر سه اضافه شد — هیچ رفتار دیگری تغییر نکرد.

**بررسی‌شده و تأیید‌شده به‌عنوان غیرباگ** (توسط همین بازبینی): امکان
Crash با `Uri.parse` نامعتبر (هر دو نقطه‌ی نمایش با `runCatching`
محافظت می‌شوند)، انفجار طول پرامپت با تعداد نامحدود عکس مرجع (`Renderer.kt`
فقط `size`/`isEmpty()` را می‌خواند، نه محتوا)، خرابی Serialize در DTO/Room
(یک ستون JSON، نه رشته‌ی جداشونده با کاما)، مدیریت خطای AI Connector
(تمام مسیرهای خطا صریح Catch و Result.failure می‌شوند).

### یافته‌های بازبینی UI/Compose (بدون نیاز به رفع)

بازبینی کد-محور (بدون Emulator — طبق محدودیت این محیط، صریحاً افشا
شده) روی فرم‌های سه‌گانه‌ی Asset و `HomeScreen.kt` هیچ باگ Crash-محور،
هیچ رنگ Hardcode-شده‌ی ناقض Theme، و هیچ رشته‌ی انگلیسی Hardcode‌شده
پیدا نکرد — همه‌ی برچسب‌ها از `uiString()`/`uiTemplate()` با هر دو
کلید fa/en عبور می‌کنند؛ آیکون Back از `Icons.AutoMirrored` استفاده
می‌کند (RTL-سازگار). **یک مشاهده‌ی جزئی، غیرباگ:** `DecodedContentImage`
(مصرف‌شده در هر سه فرم Asset) هیچ Placeholder/Loading برای Uri
نامعتبر/در‌حال‌Decode ندارد — طبق کامنت صریح خودِ کد («اگر Uri دیگر
معتبر نیست... هیچ‌چیز رندر نمی‌شود، بدون Crash») این یک تصمیم عمدی
موجود است، نه یک نظارت جدید — بدون نیاز به رفع در این قدم.

### یافته‌های بازبینی امنیتی (یک مورد رفع‌شده، یک مورد گزارش‌شده)

بازبینی مستقل `SecureKeyRepository`/`AiConnector` تأیید کرد: کلید API
واقعاً با `EncryptedSharedPreferences` (`MasterKey` متصل به Android
Keystore) ذخیره می‌شود؛ هیچ‌جا Log نمی‌شود (Grep کل کدبیس روی
`Log.d/w/e/i/v`)؛ هیچ Interceptor لاگ‌گیری روی HTTP Client‌های
`AiConnector.kt`/`ImagePromptAiConnector.kt` نصب نیست؛ کلید هرگز وارد
Room یا داده‌ی Export/Backup نمی‌شود.

**یافته‌ی واقعی و رفع‌شده:** `AndroidManifest.xml` با
`android:allowBackup="true"` بدون هیچ Exclusion صریحی، یعنی فایل
`secure_api_keys.xml` (کلید رمزنگاری‌شده) به‌صورت پیش‌فرض در
Auto Backup/Device Transfer اندروید کپی می‌شود. رمزنگاری با
Android Keystore عملاً روی دستگاه دیگر غیرقابل‌بازیابی است، اما این
یک استثنای صریح دفاع-در-عمق است. **رفع شد** — طبق دستورالعمل رسمی
اندروید برای هر دو مسیر (API 31+ و قدیمی‌تر، چون `minSdk` این پروژه
۲۶ است): `res/xml/data_extraction_rules.xml` (`cloud-backup` +
`device-transfer`) و `res/xml/full_backup_content.xml` هر دو
`secure_api_keys.xml` را Exclude می‌کنند؛ `AndroidManifest.xml` با
`android:dataExtractionRules`/`android:fullBackupContent` به آن‌ها
وصل شد.

**یافته‌ی گزارش‌شده (بدون رفع در این قدم):** عکس‌های مرجع Asset
(`content://...`) وارد فایل Export پروژه می‌شوند
(`AssetEntityDto.assetDataJson` → `ReferenceImageDto.localFilePath`)
و از طریق `BackupsScreen.kt`ی `ACTION_SEND` قابل‌اشتراک‌اند — این
Uriها برای گیرنده (دستگاه/اپ دیگر) بی‌معنی‌اند و بسته به Provider
منبع می‌توانند نام مسیر محلی را افشا کنند (افشای جزئی مسیر، نه محتوای
حساس). این یک تصمیم محصول است (آیا Export پروژه اصلاً باید عکس مرجع
را همراه ببرد؟ باید هشدار بدهد؟) — **طبق دستور صریح («تصمیم بزرگ
معماری را خودسرانه نگیر»)، این یافته فقط گزارش می‌شود، رفع نمی‌شود.**

## تصمیم

1. سه بند/یادداشت جدید در `README.md` طبق یافته‌های `readme-audit`
   اصلاح شدند (بند تکراری Choose Image، شمار Entity، پوشه‌ی
   `ui/common/`) + یک جمله‌ی توضیحی برای ابهام ترجمه‌ی فارسی.
2. کامنت قدیمی `SettingsScreen.kt:366-372` به‌روز شد.
3. هر سه فرم Asset (`CharacterAssetFormScreen.kt`/
   `LocationAssetFormScreen.kt`/`ObjectAssetFormScreen.kt`): شکست
   `takePersistableUriPermission` اکنون با `Log.w` گزارش می‌شود (رفتار
   موجود بدون تغییر).
4. `ImagePromptEngine.kt`: `description` در هر سه تابع
   (`buildOutfitImagePrompt`/`buildLocationImagePrompt`/
   `buildObjectImagePrompt`) با `takeIf(isNotBlank)` محافظت شد؛ ۳ تست
   جدید در `ImagePromptEngineTest.kt` این رفتار را اثبات می‌کنند.
5. `res/xml/data_extraction_rules.xml` و `res/xml/full_backup_content.xml`
   ساخته شدند؛ `AndroidManifest.xml` به هر دو وصل شد.
6. یافته‌ی Export/اشتراک‌گذاری Uri محلی — فقط گزارش شد.
7. بدون تغییر در `AssetContinuity.kt`/`ValidationAggregator.kt` — طبق
   ADR-142، منتظر تصمیم معمار.

## پیامدها

- کامپایل کامل + `assembleDebug` بعد از تمام تغییرات این ADR: موفق.
- تست هدفمند `domain.asset.*` بعد از اضافه‌شدن ۳ تست جدید: **۷۷ تست،
  ۰ شکست**.
- اجرای کامل Suite نهایی (بعد از تمام تغییرات دو ADR این زنجیره):
  **۱۰۰۸ تست، ۱ شکست، ۰ نادیده‌گرفته‌شده** — دقیقاً همان یک شکست
  شناخته‌شده‌ی `OutputDeliveryFlowTest` (ADR-070/071، همان خط/همان
  Exception). شمار ۱۰۰۸ دقیقاً منطبق است: ۱۰۱۰ (پایه) − ۵ (تست‌های
  StyleMatrix حذف‌شده، ADR-142) + ۳ (تست‌های description خالی جدید) =
  ۱۰۰۸.
- هیچ کد `*.kt` مربوط به یافته‌ی Hard Lock/`checkTotalSoundLayerCount`
  (ADR-142) تغییر نکرد — هنوز منتظر تصمیم معمار.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` →
  `BUILD SUCCESSFUL`.
- `./gradlew :app:testDebugUnitTest --tests "...domain.asset.*"` →
  `BUILD SUCCESSFUL`، ۷۷ تست/۰ شکست (شامل ۳ تست جدید description خالی).
- `./gradlew :app:processDebugMainManifest` → `BUILD SUCCESSFUL` (تأیید
  صحت XML جدید Backup Rules).
- `./gradlew :app:assembleDebug` (دوبار — یک‌بار قبل و یک‌بار بعد از
  اتصال Manifest) → هر دو `BUILD SUCCESSFUL`.
- `./gradlew :app:testDebugUnitTest` (کامل، بدون فیلتر) →
  ۱۰۰۸ تست، ۱ شکست (شناخته‌شده)، ۰ نادیده‌گرفته‌شده.
- Grep مستقل تأیید کرد الگوی `takePersistableUriPermission` در هر سه
  فرم یکسان بود (نسخه‌برداری، نه انحراف) — اصلاح هر سه هم‌الگو اعمال
  شد.
- Grep مستقل `data/entity/` (۱۴ فایل) و `ui/common/` (وجود
  `RetranslateButton.kt`) برای تأیید یافته‌های `readme-audit` پیش از
  اعمال.
