# واحد ۱۹: عکس مرجع (Reference Image)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** پرامپت ساخت عکس مرجع (ADR-131 تا ۱۳۶) + آپلود عکس مرجع واقعی Asset (ADR-137 تا ۱۴۰)
**وضعیت:** فعال — کاملاً پیاده‌سازی و commit‌شده (طبق «یادداشت پیاده‌سازی» پایین)
**نسخه:** ۱

---

## زمینه

این واحد دو فیچر مستقل را که مستقیماً در گفتگو با معمار طراحی و اجرا
شدند — بدون یک بلوپرینت اولیه در `docs/blueprints/` — در یک سند واحد
گرد می‌آورد (طبق تصمیم معمار: هر دو درباره‌ی «عکس مرجع یک Asset»اند،
فقط با دو مسیر کاملاً متفاوت). جزئیات کامل چرایی این شکاف مستندسازی و
چرایی گردآوری هر دو زیر یک واحد مستقل (نه ضمیمه‌ی واحد ۰۶) در
`docs/adr/141-blueprint-19-reference-image-documentation.md` آمده.

**دو بخش این واحد در دو جهت کاملاً متفاوت جریان دارند:**

```
بخش الف (پرامپت ساخت عکس مرجع)
  فقط متن پرامپت — کاملاً موازی و مستقل از موتور اصلی پرامپت ویدیو
  هرگز وارد PromptAssembly.kt/Renderer.kt نمی‌شود
  خروجی: کمک به کاربر برای ساخت تصویر خارج از این اپ (Midjourney/SD/...)

بخش ب (آپلود عکس مرجع واقعی)
  خودِ فایل تصویر (به‌صورت Uri) — مستقیماً وارد PromptBlueprint.imageReferences
  از آنجا به buildReferenceImageInstruction (واحد ۱۴، Renderer.kt) می‌رسد
  خروجی: یک جمله‌ی دستوری صریح در پرامپت نهایی («use the attached reference image»)
```

این دو مسیر **هرگز نباید با هم قاطی شوند** — یکی صرفاً یک ابزار کمکی
بیرون از اپ است، دیگری مستقیماً بخشی از پرامپت نهایی تولیدشده توسط
خودِ اپ.

---

## بخش الف — پرامپت ساخت عکس مرجع

### تعریف
مسیری کاملاً مستقل که به کاربر کمک می‌کند متن پرامپت لازم برای ساخت
یک عکس مرجع (شخصیت پایه، هر Outfit، مکان، یا شیء) را در یک ابزار
تولید تصویر **بیرون از این اپ** (مثل Midjourney، Stable Diffusion) خودش
بسازد. این بخش هرگز خودِ عکس را تولید یا ذخیره نمی‌کند.

### مسئولیت
ساخت پرامپت Template فوری (بدون AI، بدون I/O) برای هر یک از چهار نوع
محتوا؛ غنی‌سازی اختیاری همان پرامپت با یک AI Connector Profile موجود
(هم‌زمان با ترجمه‌ی فارسی برای مرور کاربر)؛ اعتبارسنجی کیفی (فقط
Warning، هرگز Blocking) روی متن نهایی تولیدشده؛ ذخیره‌ی نتیجه (متن، نه
عکس) روی خودِ Asset برای مرور بعدی و تشخیص «قدیمی‌شدن».

### پیاده‌سازی مفهومی (Kotlin)

`domain/asset/ImagePromptEngine.kt` — مسیر Template (فوری، بدون I/O):

```kotlin
fun buildCharacterBaseImagePrompt(character: CharacterAsset, projectDna: ProjectDna): String
fun buildOutfitImagePrompt(outfit: Outfit, character: CharacterAsset, projectDna: ProjectDna): String
fun buildLocationImagePrompt(location: LocationAsset, projectDna: ProjectDna): String
fun buildObjectImagePrompt(objectAsset: ObjectAsset, projectDna: ProjectDna): String
```

هر چهار تابع با `styleTokensOf(projectDna)` شروع می‌شوند — دقیقاً همان
زنجیره‌ی واقعی `combineStyles`/`toStyleReference` (واحد ۰۳، Visual
Identity) که موتور اصلی پرامپت ویدیو هم استفاده می‌کند؛ خروجی نهایی این
دو مسیر (متن پرامپت عکس در برابر `PromptBlueprint.structuredParts`) اما
کاملاً مستقل و بدون هیچ اتصال به هم می‌مانند.

`domain/asset/ImagePromptValidation.kt` — اعتبارسنجی کیفی، فقط Warning:

```kotlin
fun validateCharacterImagePromptInputs(character: CharacterAsset, projectDna: ProjectDna, generatedPrompt: String): List<ValidationIssue>
fun validateOutfitImagePromptInputs(outfit: Outfit, projectDna: ProjectDna, generatedPrompt: String): List<ValidationIssue>
fun validateLocationImagePromptInputs(location: LocationAsset, projectDna: ProjectDna, generatedPrompt: String): List<ValidationIssue>
fun validateObjectImagePromptInputs(objectAsset: ObjectAsset, projectDna: ProjectDna, generatedPrompt: String): List<ValidationIssue>
```

هر چهار تابع طول پرامپت (خیلی کوتاه/خیلی بلند) و تناقض کلیدواژه‌ای سبک
(مثلاً کلیدواژه‌ی «photorealistic» در متن آزاد وقتی سبک پروژه انیمیشنی
است، یا برعکس) را بررسی می‌کنند، به‌علاوه‌ی یک بررسی اختصاصی هر نوع
(مثلاً خالی‌بودن `physicalAppearance.ageRange` برای Character). این
قوانین **کاملاً مستقل از تجمیع‌کننده‌ی اصلی** (`ValidationAggregator.kt`،
واحد ۰۷) هستند و هرگز آنجا فراخوانی نمی‌شوند، چون این پرامپت هرگز وارد
موتور اصلی نمی‌شود و تصمیم نهایی همیشه با کاربر است.

`domain/asset/ImagePromptAiConnector.kt` — مسیر اختیاری/دوم (AI):

```kotlin
@Serializable
data class ImagePromptAiResponse(val imagePromptEn: String, val imagePromptFa: String)

fun buildImagePromptAiRequest(templatePrompt: String, styleTokens: String): String
fun parseImagePromptAiResponse(rawResponseText: String): Result<ImagePromptAiResponse>
suspend fun generateImagePromptWithAi(
    templatePrompt: String,
    styleTokens: String,
    profile: AiConnectorProfile,
    apiKey: String,
    engine: HttpClientEngine = OkHttp.create()
): Result<ImagePromptAiResponse>
```

پرامپت Template زیرقدم بالا را با کمک همان `sendToAiConnector` عمومی
موجود (واحد ۰۱ب) به یک AI متنی بیرونی می‌فرستد تا نسخه‌ی حرفه‌ای‌تر
انگلیسی + ترجمه‌ی فارسی برگرداند — بدون هیچ تغییر در خودِ
`sendToAiConnector`.

### فیلدهای ذخیره‌سازی

هر چهار نوع محتوا (شخصیت پایه روی `CharacterAsset`، هر `Outfit`،
`LocationAsset`، `ObjectAsset`) چهار فیلد یکسان دارند:

```kotlin
val imagePromptQuick: String? = null           // نتیجه‌ی مسیر Template
val imagePromptAi: String? = null               // نتیجه‌ی مسیر AI (انگلیسی)
val imagePromptFaPreview: String? = null        // ترجمه‌ی فارسی مسیر AI
val imagePromptGeneratedAt: Long? = null        // Timestamp آخرین تولید
```

`CharacterAsset`/`LocationAsset`/`ObjectAsset` (نه `Outfit`) یک فیلد
پنجم هم دارند: `updatedAt: Long?` — زمان آخرین `save()` واقعی Asset.
تشخیص «قدیمی‌شدن پرامپت عکس» با مقایسه‌ی این دو انجام می‌شود:

```kotlin
fun isImagePromptStale(generatedAt: Long?): Boolean =
    generatedAt != null && (loadedUpdatedAt ?: 0) > generatedAt
```

اگر Asset بعد از تولید آخرین پرامپت عکس دوباره ویرایش/ذخیره شده باشد،
یک هشدار «این پرامپت ممکن است قدیمی شده باشد» در UI نمایش داده می‌شود
(هنوز Warning، نه Blocking — تصمیم نهایی تولید دوباره یا نه با کاربر
است).

### قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ۱ | طول پرامپت تولیدشده باید بین ۲۰ تا ۸۰۰ کاراکتر باشد | **Warning** |
| ۲ | تناقض کلیدواژه‌ای سبک (فوتورئال در برابر انیمیشنی، هر دو جهت) بین سبک بصری پروژه و متن آزاد کاربر | **Warning** |
| ۳ | `Character.physicalAppearance.ageRange` خالی | **Warning** |
| ۴ | `Location.description`/`environment.type` خالی | **Warning** |
| ۵ | `Object.materialAndColor` خالی | **Warning** |

هر ۵ قانون **همیشه فقط Warning هستند** — هیچ‌کدام هرگز Blocking
نمی‌شوند، چون این مسیر صرفاً کمک‌کننده است، نه یک گلوگاه اجباری تولید.

---

## بخش ب — آپلود عکس مرجع واقعی

### تعریف
امکان پیوست یک عکس مرجع **واقعی** (فایل تصویری موجود روی دستگاه کاربر
— نه یک عکس ساخته‌شده در خودِ اپ) به هر Asset، از طریق چارچوب رسمی
انتخاب فایل اندروید (Storage Access Framework)، به‌همراه نمایش آن در
کارت‌های کتابخانه و فرم ویرایش، و اتصال خودکار به پرامپت نهایی هر Shot.

### مسئولیت
انتخاب فایل با `ActivityResultContracts.OpenDocument()` +
`takePersistableUriPermission` (بدون کپی فایل، بدون اعتبارسنجی
فرمت/حجم)؛ ذخیره‌ی رشته‌ی `Uri` (نه یک کپی فایل روی دیسک) روی
`referenceImages` هر Asset؛ نمایش Thumbnail کوچک در کارت‌های لیست
کتابخانه + پیش‌نمایش بزرگ‌تر در فرم ویرایش؛ اتصال خودکار این عکس‌ها به
`imageReferences` نهایی هر Shot که از همان Asset استفاده می‌کند.

### پیاده‌سازی مفهومی (Kotlin)

`domain/asset/AssetModels.kt`:

```kotlin
data class ReferenceImage(val localFilePath: String, val description: String)
```

روی هر سه نوع Asset:

```kotlin
val referenceImages: List<ReferenceImage> = emptyList()
```

`localFilePath` در عمل یک رشته‌ی `Uri` پایدار است (مثل
`content://media/external/images/...`)، نه یک مسیر فایل کپی‌شده — این
یک انحراف عمدی از نام فیلد است (نام از نسخه‌ی قدیمی این مفهوم باقی
مانده)، ثبت‌شده در ADR-137.

انتخاب فایل (هر سه Screen فرم Asset — الگوی یکسان، از قبل اثبات‌شده در
`SettingsScreen.kt`):

```kotlin
val chooseReferenceImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    uri?.let {
        runCatching {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModel.addReferenceImage(it.toString())
    }
}
```

مدیریت لیست در ViewModel (هر سه ViewModel فرم Asset):

```kotlin
fun addReferenceImage(uri: String) {
    _referenceImages.value = _referenceImages.value + ReferenceImage(localFilePath = uri, description = "")
}

fun removeReferenceImage(index: Int) {
    val current = _referenceImages.value
    if (index !in current.indices) return
    _referenceImages.value = current.filterIndexed { i, _ -> i != index }
}
```

`description` هر عکس عمداً کاربر-وارد-نمی‌کند و در لحظه‌ی افزودن ثابت
نمی‌شود — در لحظه‌ی `save()` از فیلدهای زنده‌ی فرم بازمحاسبه می‌شود
(Derived، نه Fixed):

```kotlin
val referenceImageDescription = "${name} — ${physicalAppearance.toPromptString()}"  // Character
// Location: "${name} — ${description}"
// Object:   "${name} — ${materialAndColor}"
val referenceImages = _referenceImages.value.map { it.copy(description = referenceImageDescription) }
```

نمایش عکس (کارت کتابخانه، ۵۶dp؛ پیش‌نمایش فرم، ۱۴۰dp) با یک Composable
بومی و بدون کتابخانه‌ی خارجی (`ui/home/HomeScreen.kt`، از پیش موجود
برای تصویر پس‌زمینه‌ی صفحه‌ی اصلی، `internal` پس در کل ماژول
قابل‌استفاده‌ی مجدد است):

```kotlin
@Composable
internal fun DecodedContentImage(uriString: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop)
```

`content://` را با `BitmapFactory.decodeStream` روی `Dispatchers.IO`
Decode می‌کند.

اتصال خودکار به پرامپت Shot — `domain/promptengine/AssetImageReferenceCollector.kt`:

```kotlin
fun collectAssetImageReferences(
    characters: List<CharacterAsset>,
    objects: List<ObjectAsset>,
    locations: List<LocationAsset>
): List<ImageReference>
```

هر `ReferenceImage` روی هر Asset به یک `ImageReference` (واحد ۰۵،
`domain/shot/ShotModels.kt`) تبدیل می‌شود — Character با
`type = "character"`؛ Location/Object با `type = "composition"`
(نزدیک‌ترین معادل موجود، نه یک تطابق کامل — جزئیات کامل این تصمیم در
ADR-139). ورودی‌های این تابع (`characters`/`objects`/`locations`) از
`PromptGenerationInput` می‌آیند که از قبل دقیقاً به همان Asset هایی
فیلترشده‌اند که همین Shot استفاده می‌کند
(`PromptGenerationRepository.kt`) — هیچ فیلتر اضافه‌ای لازم نیست.

اتصال در `assemblePromptBlueprint` (واحد ۱۱، `PromptAssembly.kt`):

```kotlin
imageReferences = (input.shot.imageReferences + collectAssetImageReferences(input.characters, input.objects, input.locations))
    .distinctBy { it.localFilePath }
```

رفرنس‌های دستی Shot Composer (`Shot.imageReferences` خام، مسیر
از‌قبل‌موجود) و رفرنس‌های خودکار Asset (این بخش) کنار هم قرار می‌گیرند؛
`distinctBy(localFilePath)` مانع دستور تکراری در پرامپت نهایی می‌شود
اگر کاربر همان عکس را هم دستی هم روی Asset اضافه کرده باشد.

### قوانین اعتبارسنجی

**بدون Rule.** سه تابع اعتبارسنجی که قرار بود این مسیر را پوشش دهند
(`validateImageFile`/`validateReferenceImageFile` در
`domain/asset/AssetValidation.kt`، و `validateImageReferenceFile` در
`domain/shot/ShotValidation.kt` — برای سناریوی آپلود مستقیم فایل با
اعتبارسنجی فرمت/حجم/سلامت و وجود فایل روی دیسک) **کامل حذف شدند**
(ADR-140)، چون آن سناریو با معماری واقعی این بخش (Uri مستقیم، بدون کپی
فایل) هرگز هم‌راستا نبود. آپلود امروز بدون هیچ محدودیت فرمت/حجم/سلامت
Blocking یا Warning انجام می‌شود — فقط انتخابگر سیستمی اندروید
(`OpenDocument()`، محدود به `arrayOf("image/*")` در سطح Intent) نوع
فایل را فیلتر می‌کند.

---

## یکپارچگی بین دو بخش

هر دو بخش روی همان سه نوع Asset (Character/Location/Object) و همان
مفهوم کلی «عکس مرجع» کار می‌کنند، اما به‌طور کامل مستقل پیاده شدند و
هیچ داده‌ای بین‌شان رد‌وبدل نمی‌شود:

- بخش الف **متن** تولید می‌کند (`imagePromptQuick`/`imagePromptAi`/...)
  که هرگز به بخش ب یا به موتور اصلی پرامپت ویدیو راه پیدا نمی‌کند —
  فقط برای کمک به کاربر جهت ساخت تصویر **بیرون از این اپ**.
- بخش ب **خودِ عکس** (به‌صورت Uri) را نگه می‌دارد
  (`referenceImages`) که مستقیماً وارد `PromptBlueprint.imageReferences`
  و از آنجا به دستور صریح `buildReferenceImageInstruction` (واحد ۱۴)
  می‌شود.
- نوع داده‌ی هر بخش هم عمداً متفاوت است: بخش الف از هیچ نوع مشترکی
  استفاده نمی‌کند (فقط `String?` روی خودِ Asset)؛ بخش ب از `ReferenceImage`
  (بدون `type`) استفاده می‌کند که در لحظه‌ی اتصال به Shot به
  `ImageReference` (با `type`، واحد ۰۵) تبدیل می‌شود — این دو نوع
  هم‌نام-مانند اما عمداً متفاوت‌اند، دقیقاً طبق `docs/reference/type-registry.md`.
- کاربری که می‌خواهد فقط از بخش الف استفاده کند (فقط متن پرامپت، بدون
  آپلود عکس واقعی) کاملاً می‌تواند — بخش ب اختیاری و کاملاً جداست.

---

## معیارهای موفقیت

- بخش الف: پرامپت Template فوری و بدون I/O برای هر ۴ نوع محتوا در
  دسترس است؛ مسیر AI اختیاری بدون کلید ذخیره‌شده هرگز درخواست HTTP
  واقعی نمی‌زند؛ هیچ خروجی این بخش هرگز وارد `PromptAssembly.kt`/
  `Renderer.kt` نمی‌شود.
- بخش ب: انتخاب فایل با `OpenDocument()` بدون کپی فایل کار می‌کند؛
  Uri انتخاب‌شده بعد از بستن اپ هم معتبر می‌ماند
  (`takePersistableUriPermission`)؛ عکس در کارت کتابخانه و فرم نمایش
  داده می‌شود؛ عکس‌های آپلودشده بدون هیچ اقدام دستی اضافه در پرامپت
  نهایی هر Shot مرتبط حاضرند، بدون تکرار.

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام خواندن این بلوپرینت)

این سند یک **Migration مستندسازی محض** روی دو فیچر کاملاً
پیاده‌سازی‌شده و commit‌شده است — **نه یک دستور کار برای نوشتن کد
جدید**. تمام فایل‌ها/توابع/فیلدهای ذکرشده در این سند از قبل در شاخه‌ی
`claude/cinema-shot-generator-init-m6pqzh` وجود دارند و تست دارند. اگر
هنگام خواندن این بلوپرینت برای کار آینده متوجه مغایرتی بین این سند و
کد واقعی شدید، همیشه به کد واقعی اعتماد کنید (با grep تأیید کنید)، نه
این متن — و مغایرت را گزارش دهید تا این بلوپرینت هم اصلاح شود.

جزئیات کامل هر زیرقدم (تصمیمات مستقل، راستی‌آزمایی، نتایج تست) در
ADR-131 تا ADR-141 آمده:

| زیرقدم | ADR | موضوع |
|---|---|---|
| بخش الف، ۱ از ۵ | ۱۳۱ | مدل داده (فیلدهای `imagePromptQuick`/...) |
| بخش الف، ۲ از ۵ | ۱۳۲ | موتور Template + اعتبارسنجی |
| بخش الف، ۳ از ۵ | ۱۳۳ | مسیر اختیاری AI |
| بخش الف، ۴ از ۵ | ۱۳۴ | اتصال به فرم Character/Outfit |
| بخش الف، ۵ از ۵ | ۱۳۵ | اتصال به فرم Location/Object |
| بخش الف (تکمیلی) | ۱۳۶ | اتصال Rule های اعتبارسنجی یتیم‌مانده |
| بخش ب، ۱ از ۳ | ۱۳۷ | مدل داده + انتخاب/ذخیره‌ی Uri |
| بخش ب، ۲ از ۳ | ۱۳۸ | نمایش در کارت کتابخانه + فرم |
| بخش ب، ۳ از ۳ | ۱۳۹ | اتصال خودکار به پرامپت Shot |
| پاک‌سازی | ۱۴۰ | حذف Rule های یتیم اعتبارسنجی فایل محلی (سناریوی کنارگذاشته‌شده) |
| این سند | ۱۴۱ | خودِ Migration مستندسازی (این بلوپرینت + هماهنگی اسناد مرجع) |
