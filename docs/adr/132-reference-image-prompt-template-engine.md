# ADR-132: زیرقدم ۲ از ۵ — موتور Template و اعتبارسنجی «پرامپت ساخت عکس مرجع»

## وضعیت

پذیرفته‌شده

## زمینه

ادامه‌ی ADR-131 (زیرقدم ۱: مدل داده). این زیرقدم فقط منطق خالص دامنه
(Template + اعتبارسنجی) را اضافه می‌کند — بدون AI Connector، بدون
UI/ViewModel. طبق طراحی فیچر، کاملاً مستقل از موتور اصلی پرامپت ویدیو
(`PromptAssembly.kt`/`Renderer.kt`) است؛ خروجی این زیرقدم هرگز به آن مسیر
راه پیدا نمی‌کند — این پرامپت‌ها صرفاً به کاربر کمک می‌کنند خارج از این
اپ عکس مرجع یک Asset را بسازد.

### راستی‌آزمایی مستقل

پیش از نوشتن کد، `ProjectDna.kt`، `StyleMatrix.kt` و
`ui/dna/DnaTabContent.kt` کامل خوانده شدند:

- `ProjectDna.coreIdentity: CoreIdentity` تأیید شد — `dominantVisualStyle:
  VisualStyle` (غیر-nullable)، `secondaryStyle: VisualStyle?`،
  `influence: StyleInfluence?`.
- در `ui/dna/DnaTabContent.kt` خط ۱۷۲ (دقیقاً همان خطی که پیش‌بررسی
  گفته بود)، `checkStyleCompatibility(dna.coreIdentity.dominantVisualStyle,
  secondary)` تأیید کرد مسیر واقعی مستقیماً از `coreIdentity` است.
- `StyleMatrix.kt`: `data class StyleMatrix` تأیید شد جایی Instantiate
  نمی‌شود، اما `combineStyles`/`toStyleReference`/`checkStyleCompatibility`
  توابع Top-level مستقل‌اند و بدون نیاز به آن Struct قابل استفاده‌اند —
  دقیقاً طبق پیش‌بررسی.
- `VisualStyle.toStyleReference(): StyleReference` و
  `combineStyles(primary: StyleReference, secondary: StyleReference?,
  influence: StyleInfluence?): String` امضای دقیق‌شان تأیید شد.
- `Severity`/`ValidationIssue` (`domain/validation/ValidationEngine.kt`)
  تأیید شدند — `ValidationIssue(severity, field, message, suggestion)`.
- فیلدهای موجود هر نوع Asset (`PhysicalAppearance.toPromptString()`،
  `LocationAsset.environment/description/keyElements/basePrompt`،
  `ObjectAsset.size/materialAndColor/specialTrait/basePrompt`) کامل
  تأیید شدند.

هیچ مغایرتی با پیش‌بریفینگ پیدا نشد.

## تصمیم

### فایل‌های تازه

دو فایل جدید در `domain/asset/` (هم‌الگو با `AssetValidation.kt` موجود):

**`ImagePromptEngine.kt`** — چهار تابع خالص (بدون Side Effect/I/O)، خروجی
همیشه انگلیسی:

- `buildCharacterBaseImagePrompt(character, projectDna)`: پرامپت شخصیت
  پایه/خنثی — لباس/پوز خنثی ثابت (`"plain neutral clothing"`،
  `"neutral standing pose, front-facing, plain studio background"`) +
  `physicalAppearance.toPromptString()` (بازاستفاده، نه بازنویسی) +
  `defaultMood` در صورت وجود.
- `buildOutfitImagePrompt(outfit, character, projectDna)`: با عبارت صریح
  ارجاع به مرجع شخصیت پایه (`"matching the established character
  reference exactly"`) + `outfit.description` + `outfit.condition` (در
  صورت وجود).
- `buildLocationImagePrompt(location, projectDna)` /
  `buildObjectImagePrompt(objectAsset, projectDna)`: از فیلدهای موجود هر
  نوع.

یک تابع خصوصی مشترک `styleTokensOf(projectDna)` هر چهار تابع را از تکرار
فراخوانی `combineStyles(coreIdentity.dominantVisualStyle.toStyleReference(),
coreIdentity.secondaryStyle?.toStyleReference(), coreIdentity.influence)`
معاف می‌کند.

**`ImagePromptValidation.kt`** — چهار تابع (`validateCharacterImagePromptInputs`/
`validateOutfitImagePromptInputs`/`validateLocationImagePromptInputs`/
`validateObjectImagePromptInputs`)، هرکدام `List<ValidationIssue>`
برمی‌گردانند، همه `Severity.WARNING`:

- طول پرامپت تولیدشده (کوتاه‌تر از ۲۰ کاراکتر یا بلندتر از ۸۰۰).
- فیلد حداقلی خالی: `physicalAppearance.ageRange` (Character)،
  `description`/`environment.type` (Location)، `materialAndColor`
  (Object).
- تناقض کلیدواژه‌ای ساده بین سبک پروژه و متن آزاد کاربر — Heuristic
  کوچک، نه NLP: سبک انیمیشنی (۲D/۳D) + کلیدواژه‌ی «فوتورئال» در متن، **یا
  برعکس** (طبق دستور کار صریح): سبک `PHOTOREALISTIC` + کلیدواژه‌ی
  انیمیشنی (`anime`/`cartoon`/`animated`) در متن.

### چرا امضا شامل `generatedPrompt: String` است، نه فقط خودِ Asset

توابع اعتبارسنجی روی متن **تولیدشده** (خروجی واقعی `ImagePromptEngine.kt`)
کار می‌کنند، نه فقط ورودی خام — چون قانون طول پرامپت باید طول واقعی
خروجی نهایی را بسنجد، نه یک تخمین از روی فیلدهای ورودی. این هم‌الگو با
سبک این پروژه است (مثل `validateShotBeatTimeline` که خروجی واقعی را
می‌سنجد، نه صرفاً ورودی خام).

## پیامدها

- موتور اصلی پرامپت ویدیو دست‌نخورده ماند — این دو فایل هیچ‌جا از
  `PromptAssembly.kt`/`Renderer.kt` فراخوانی نمی‌شوند و هیچ‌جا آن‌ها را
  فراخوانی نمی‌کنند.
- تجمیع‌کننده‌ی اصلی (`ValidationAggregator.kt`) دست‌نخورده ماند — این
  قوانین Warning-only هرگز در آن مسیر Wire نشدند (طبق طراحی صریح: تصمیم
  نهایی پرامپت عکس همیشه با کاربر است، نه بخشی از Validation رسمی شات).
- ذخیره‌سازی خروجی این توابع در `imagePromptQuick`/... (ADR-131) هنوز کار
  زیرقدم‌های بعدی است — این زیرقدم فقط تابع تولیدکننده را می‌سازد.
- این فقط **زیرقدم ۲ از ۵** است. AI Connector (ترجمه‌ی فارسی/بهبود
  پرامپت با AI)، UI/ViewModel، و اتصال به فیلدهای ذخیره‌شده هنوز آغاز
  نشده‌اند — زیرقدم‌های ۳ تا ۵.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- `ImagePromptEngineTest.kt` (۶ تست تازه، یکی برای هر نوع Asset + یک تست
  ترکیب سبک اصلی/ثانویه): همه Pass.
- `ImagePromptValidationTest.kt` (۱۱ تست تازه — طول کوتاه/بلند/معتبر،
  فیلد حداقلی خالی برای هر سه نوع Asset، تناقض سبک هر دو جهت، سازگاری
  بدون هشدار، دو سناریوی «صفر Issue»): همه Pass.
- `domain.asset.*`/`domain.visualidentity.*` (شامل `AssetValidationTest`/
  `AssetModelsTest`/`StyleMatrixTest` موجود): همه Pass، بدون رگرسیون —
  فقط دو فایل تازه اضافه شدند، هیچ فایل موجودی تغییر نکرد.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۷۵ تست، ۹۷۳ Pass،
  ۲ Fail — `AssetFormFlowTest` و `OutputDeliveryFlowTest`، هر دو از پیش
  در فهرست کلاس‌های ناپایدار (Flaky) مستندشده‌ی این پروژه (بدون هیچ
  ارتباطی با این تغییرات — هیچ فایل UI لمس نشد). با اجرای مجدد و مجزا
  هر دو کامل Pass شدند — تأیید شد Flakiness شناخته‌شده است، نه رگرسیون.
- `./gradlew :app:assembleDebug`: موفق.
