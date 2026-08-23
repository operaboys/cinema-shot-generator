# ADR-139: آپلود عکس مرجع واقعی Asset — زیرقدم ۳ از ۳، پایانی: اتصال خودکار به پرامپت Shot

## وضعیت

پذیرفته‌شده

## زمینه

آخرین زیرقدم فیچر «آپلود عکس مرجع واقعی Asset» (ادامه‌ی ADR-137/138). طبق
تصمیم صریح معمار («باید خودکار وصل بشه»)، عکس‌های مرجع آپلودشده‌ی
Character/Location/Object اکنون به‌صورت خودکار به `imageReferences` نهایی
پرامپت هر Shot اضافه می‌شوند. با این قدم، فیچر «آپلود عکس مرجع واقعی
Asset» (ADR-137 تا ۱۳۹) کامل می‌شود.

### راستی‌آزمایی مستقل پیش از کدنویسی

- `ShotModels.kt`: `ImageReference(type: String, localFilePath: String,
  description: String)` — `type` طبق کامنت `"character" | "style" |
  "composition" | "lighting"` — دقیقاً طبق پیش‌بررسی.
- `PromptEngineModels.kt`: `PromptGenerationInput.characters/objects/locations`
  دقیقاً طبق پیش‌بررسی، بدون هیچ فیلد دیگری که به این قدم مربوط باشد.
- `PromptGenerationRepository.kt`: تأیید مستقیم —
  `assetRepository.loadCharacterAssets(shot.characterIds)`،
  `loadObjectAssets(shot.objectIds)`،
  `loadLocationAssets(resolveSceneLocation(shot, scene))` — یعنی
  `input.characters`/`objects`/`locations` از قبل دقیقاً به همان Asset
  هایی محدود هستند که این Shot خاص استفاده می‌کند؛ هیچ فیلتر اضافه‌ای در
  این زیرقدم لازم نبود.
- `PromptAssembly.kt`: `imageReferences = input.shot.imageReferences`
  دقیقاً در `assemblePromptBlueprint` (خط ۷۹ طبق پیش‌بررسی) — محل طبیعی
  ترکیب دو مسیر، دقیقاً طبق پیش‌بررسی.
- **یافته‌ی مهم (grep در سراسر پروژه، ورای پیش‌بررسی):**
  `ImageReference.type` امروز در هیچ‌جای مسیر Render واقعی خوانده نمی‌شود.
  تنها مصرف‌کننده‌ی واقعی `imageReferences` در مسیر Render،
  `buildReferenceImageInstruction` (`Renderer.kt`) است که فقط
  `imageReferences.isEmpty()`/`imageReferences.size` را می‌خواند (برای
  انتخاب بین جمله‌ی مفرد/جمع) — نه `.type`، نه `.localFilePath`، نه حتی
  `.description`. یعنی انتخاب `type` برای Location/Object در این قدم
  امروز **هیچ اثر رفتاری واقعی روی پرامپت نهایی ندارد** — فقط یک برچسب
  معنایی برای مصرف احتمالی آینده است.

هیچ مغایرت دیگری بین پیش‌بریفینگ و کد واقعی یافت نشد.

## تصمیم

### `collectAssetImageReferences` (فایل جدید `AssetImageReferenceCollector.kt`)

تابع خالص جدید:
```kotlin
fun collectAssetImageReferences(
    characters: List<CharacterAsset>,
    objects: List<ObjectAsset>,
    locations: List<LocationAsset>
): List<ImageReference>
```
هر `ReferenceImage` روی هر Asset را به یک `ImageReference` تبدیل می‌کند:
- Character → `type = "character"` (تطابق مستقیم و بدون ابهام با یکی از ۴
  مقدار مستند).
- Location → `type = "composition"` — نزدیک‌ترین معادل معنایی موجود
  (محیط/چیدمان صحنه).
- Object → **هیچ‌کدام از ۴ مقدار مستندشده واقعاً برای «شیء فیزیکی»
  مناسب نیست.** طبق دستور صریح («افزودن مقدار پنجم تصمیم طراحی بزرگ‌تری
  است، خودسرانه تصمیم نگیر»)، مقدار پنجمی اضافه نشد. به‌جای آن، همان
  `"composition"` (کم‌غلط‌ترین گزینه‌ی موجود در دسترس) به‌عنوان راه‌حل
  **موقت** انتخاب شد — این یک شکاف طراحی شناخته‌شده و آگاهانه است، نه یک
  تصمیم قطعی؛ چون طبق یافته‌ی بالا امروز `type` هیچ اثر عملکردی ندارد،
  هزینه‌ی این انتخاب موقت صفر است. اگر در آینده `type` واقعاً در Renderer
  مصرف شود، این نگاشت باید بازبینی شود (و شاید مقدار پنجمی مثل `"prop"`
  اضافه شود — تصمیمی برای آن لحظه، با معمار).

### اتصال در `assemblePromptBlueprint` (`PromptAssembly.kt`)

خط `imageReferences = input.shot.imageReferences` به این تغییر کرد:
```kotlin
imageReferences = (input.shot.imageReferences + collectAssetImageReferences(input.characters, input.objects, input.locations))
    .distinctBy { it.localFilePath }
```
رفرنس‌های دستی Shot Composer دست‌نخورده می‌مانند (مسیر قدیمی، بدون تغییر
رفتار)؛ رفرنس‌های خودکار Asset کنار آن‌ها اضافه می‌شوند.
`distinctBy(localFilePath)` از دستور تکراری در پرامپت نهایی جلوگیری
می‌کند اگر کاربر همان عکسی را که روی Asset هم آپلود کرده، دستی هم در Shot
Composer اضافه کرده باشد (رفرنس دستی، به‌خاطر ترتیب `+`، اولویت نگه‌داشتن
دارد چون `distinctBy` اولین رخداد را نگه می‌دارد).

## پیامدها

- `AssetModels.kt`/`AssetDto.kt`/`DnaAssetMappers.kt`/`ShotModels.kt`/
  `Renderer.kt`/هر Screen یا ViewModel فرم Asset دست‌نخورده ماندند —
  طبق فهرست فایل‌های مجاز این قدم.
- تست موجود `assemblePromptBlueprint fills every structured part from a
  full input` (که `blueprint.imageReferences.size == 1` را چک می‌کند)
  بدون تغییر Pass می‌ماند — `sampleCharacter()`/`sampleObjectAsset()`/
  `sampleLocationAsset()` در `PromptAssemblyTest.kt` هیچ‌کدام
  `referenceImages` نداشتند (پیش‌فرض `emptyList()` از ADR-137)، پس
  `collectAssetImageReferences` روی آن ورودی همیشه لیست خالی برمی‌گرداند
  — بدون شکستن هیچ تست موجود.
- با این قدم، فیچر «آپلود عکس مرجع واقعی Asset» (ADR-137 تا ۱۳۹) کامل
  شد: آپلود (زیرقدم ۱) → نمایش در کارت کتابخانه/فرم (زیرقدم ۲) → اتصال
  خودکار به پرامپت واقعی هر Shot (این زیرقدم).

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- ۵ تست تازه‌ی مستقیم روی `collectAssetImageReferences`
  (`AssetImageReferenceCollectorTest.kt`): لیست خالی وقتی هیچ Asset ای
  `referenceImages` ندارد؛ نگاشت صحیح `type` برای هر سه نوع؛ ترکیب چند
  Asset هم‌زمان.
- ۴ تست یکپارچگی تازه در `PromptAssemblyTest.kt`: عکس مرجع Character با
  `type="character"` در `blueprint.imageReferences` نهایی حاضر است (کنار
  رفرنس دستی موجود `sampleShot()`، بدون حذف آن)؛ عدم تکرار وقتی همان
  `localFilePath` هم دستی هم خودکار می‌آید؛ عکس مرجع Location/Object با
  `type="composition"`.
- `domain.promptengine.*`/`domain.outputdelivery.*`/`domain.asset.*`/
  `data.repository.*` (کل چهار پکیج): بدون رگرسیون.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۱۰۱۹ تست، ۲ Fail —
  `AppNavigationTest`/`OutputDeliveryFlowTest`، هر دو از پیش در فهرست
  کلاس‌های ناپایدار (Flaky) مستندشده‌ی این پروژه (بدون ارتباط با این
  قدم). با اجرای مجدد و مجزا هر دو کامل Pass شدند.
- `./gradlew :app:assembleDebug`: موفق.
