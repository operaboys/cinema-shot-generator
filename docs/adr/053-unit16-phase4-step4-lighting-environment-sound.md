# ADR-053: واحد ۱۶ — فاز ۴، قدم ۴ (آخرین قدم) — Tab «نور و محیط» + Tab «صدا»

**تاریخ:** 2026-08-06
**وضعیت:** کامل شد. `gradle :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL` (۶۲۵ تست، ۰ شکست، ۰ خطا). **فاز ۴ واحد ۱۶ به‌طور کامل تکمیل شد.**

## Context

آخرین قدم فاز ۴ واحد ۱۶ — Tab «نور و محیط» (`LightingSettings`/`EnvironmentSettings`،
واحد ۰۸) و Tab «صدا» (`SoundProfile`، واحد ۰۵ + Environment-to-Sound Mapping
واحد ۰۸). منابع حقیقت: `docs/blueprints/08-scene-conditions.md` و
`docs/design/README.md` بخش «۷. Shot Composer».

## تصمیم ۱: دو سوییچ منبع/Override مستقل، نه یک سوییچ ترکیبی برای کل Tab

`Shot.lighting: SourcedSettings<LightingSettings>` و
`Shot.environment: SourcedSettings<EnvironmentSettings>` دو فیلد کاملاً مستقل
واحد ۰۵ هستند — یک شات می‌تواند نور را Override کند اما محیط را از صحنه ارث
ببرد (یا برعکس). دستور کار «سوییچ منبع/Override (هم‌الگو دقیق با Tab دوربین)»
را بدون تصریح صریح «یک سوییچ یا دو سوییچ» خواسته بود؛ طبق ساختار واقعی دامنه،
**دو** سوییچ مستقل (یکی برای بخش نورپردازی، یکی برای بخش محیط) پیاده شد — نه
یک سوییچ مشترک که هردو را هم‌زمان تغییر دهد. تست
`switching lighting and environment sources independently...` صریحاً همین
استقلال را اثبات می‌کند (سوییچ نور، منبع محیط را دست‌نخورده نگه می‌دارد).

## تصمیم ۲: بازاستفاده‌ی `lightingStyleLabel` موجود؛ کلید جدید برای `ColorTemperature` این واحد

`LightingSettings.style` از `LightingStyle` واحد ۰۲/DNA می‌آید (۲۲ مقدار) —
تابع `lightingStyleLabel` و همه‌ی کلیدهای ترجمه‌ی `lightingStyle.*` از قبل در
`ui.dna.DnaLabels`/`UiStrings.kt` (فاز ۲) موجود بودند؛ به‌جای بازتعریف، مستقیماً
Import و بازاستفاده شدند. در مقابل، `ColorTemperature` این واحد (۰۸:
WARM/NEUTRAL/COLD/MIXED) — با اینکه هم‌نام `ColorTemperature` واحد ۰۲/DNA
(WARM/COOL/NEUTRAL) است — طبق کامنت صریح `LightingModels.kt` عمداً یک enum
جداست (واژگان متفاوت: Cold به‌جای Cool + مقدار اضافه‌ی Mixed). کلیدهای ترجمه‌ی
این فایل با پیشوند متفاوت `sceneConditionsColorTemperature.*` نوشته شدند (نه
بازاستفاده از کلیدهای موجود `colorTemperature.*`) تا هیچ اشتباه معنایی/تداخلی
رخ ندهد.

## تصمیم ۳: محدودیت ۳موردی `environmentalMotion` — پیاده‌سازی به‌صورت Chip غیرفعال، نه مسدودسازی با Exception

طبق تصریح دستور کار («غیرفعال‌کردن گزینه‌های بیشتر بعد از انتخاب ۳تا») — هر
Chip یک نوع Environmental Motion، وقتی لیست فعلی ۳ مورد دارد و آن مورد
انتخاب‌نشده است، `onClick=null` می‌گیرد (به‌جای فراخوان واقعی) — طبق امضای
موجود `OpaqueChip(onClick: (() -> Unit)?)`. `toggleEnvironmentalMotion` در
ViewModel هم به‌صورت تدافعی همین سقف را دوباره بررسی می‌کند (نه فقط به UI
اعتماد کامل) — دو لایه‌ی محافظت، هم‌راستا با کامنت خودِ `EnvironmentModels.kt`
(«حداکثر ۳ مورد»).

## تصمیم ۴ (Rule 5): دکمه‌ی صریح «تولید صداهای محیط»، بدون هیچ Effect خودکار

طبق تصریح دقیق سند طراحی («manual only / never auto-generated (Rule 5) —
only on explicit user action»): `generateAmbientSounds()` در ViewModel فقط
از کلیک مستقیم دکمه فراخوانی می‌شود — هیچ `LaunchedEffect`/فراخوان در
`init{}` وجود ندارد. مستقیماً از `mapEnvironmentToSound` (واحد ۰۸، بدون
تغییر منطق) استفاده می‌کند؛ چون امضای آن تابع `String` (نه enum) و
lowercase می‌گیرد، مقادیر enum فعلی (`weatherType`/`weatherIntensity`/
`windStrength`) با `.name.lowercase()` تبدیل می‌شوند. **لیست موجود جایگزین
می‌شود، نه Append** — دکمه معنای «بازتولید طبق آب‌وهوای فعلی» دارد، نه
«افزودن یک نسخه‌ی دیگر». تست
`opening the Sound tab never auto-generates ambient sounds...` هر دو نیمه‌ی
Rule 5 را اثبات می‌کند: نیمه‌ی منفی (باز کردن Tab هیچ صدایی تولید نمی‌کند) و
نیمه‌ی مثبت (کلیک صریح واقعاً تولید می‌کند، با آب‌وهوای بارانی برای اطمینان
از خروجی غیرخالی — پیش‌فرض CLEAR+بدون‌باد چیزی تولید نمی‌کند).

## تصمیم ۵: `SoundProfile.ambientAutoGenerate` — بدون کنترل UI در این قدم

`SoundProfile.ambientAutoGenerate: Boolean` در دامنه موجود است اما دستور کار
هیچ کنترل UI صریحی برایش نخواست. مقدار بارگذاری‌شده از Shot موجود بدون تغییر
دوباره ذخیره می‌شود (`loadedAmbientAutoGenerate`) — الزام واقعی Rule 5 (عدم
تولید خودکار) کاملاً مستقل از این پرچم توسط خودِ ViewModel (نبود هیچ فراخوان
خودکار) تضمین می‌شود، نه توسط این پرچم.

## تصمیم ۶: `actionSounds`/`characterSounds` — فرم افزودن دستی ساده، بدون انتخاب‌گر کاراکتر

طبق تصریح دستور کار («فرم افزودن دستی ساده، هم‌الگو با Attached References»):
هر دو بخش سه فیلد متنی + دکمه‌ی افزودن دارند. `characterSound.characterId`
یک فیلد متنی آزاد است (نه Dropdown انتخاب از `Shot.characterIds` یا کتابخانه‌ی
Asset) — دقیقاً هم‌الگو با تصمیم Attached References قدم قبل (ADR-052):
ساخت یک Picker واقعی کاراکتر خارج از Scope این قدم بود.

## تصمیم ۷ (بدون Validation زنده — انحراف آگاهانه از الگوی قدم قبل)

بخش الف/ب واحد ۰۸ جمعاً ۱۳ Rule اعتبارسنجی دارند (۵ Lighting + ۸ Environment).
برخلاف Tab دوربین (قدم قبل، ADR-052) که ۴ Rule خودبسنده را زنده وایر کرد،
این قدم **هیچ‌کدام** از این ۱۳ Rule را زنده وایر نکرد — تصمیمی آگاهانه، نه
فراموشی:

- دو Rule نورپردازی (`checkSunlightAtNight`/`checkMoonlightAtNoon`) و یک Rule
  محیط (`checkFireInRainOutdoors`) به داده‌ی خارج از این دو Tab نیاز دارند
  (`TimeOfDay`/`locationType` از Scene) — ShotComposerViewModel فعلاً هیچ
  وابستگی به `SceneRepository` ندارد؛ افزودنش فقط برای این Rule‌ها یک
  وابستگی تازه و گسترش Scope غیرضروری بود.
- وایر کردن فقط زیرمجموعه‌ای از ۱۳ Rule (آن‌هایی که خودبسنده‌اند) بدون هیچ
  نشانه‌ی بصری «این لیست کامل نیست» می‌توانست کاربر را به اشتباه به این باور
  برساند که همه‌ی قوانین بررسی می‌شوند — ناقص اما بی‌نشانه، بدتر از هیچ.
- این قدم (آخرین قدم فاز ۴، دو Tab هم‌زمان) از قبل بزرگ‌ترین قدم این فاز بود؛
  دستور کار صریحاً Validation را در فهرست تست‌های الزامی این قدم نخواسته بود
  (برخلاف قدم قبل که Round-Trip/محدودیت/Rule 5/سوییچ منبع خواسته بود).

بدهی ثبت‌شده برای آینده: وایر کردن کامل هر ۱۳ Rule نیاز به یک قدم مستقل دارد
که `ShotComposerViewModel` را به `SceneRepository` (برای `TimeOfDay`/
`locationType`) وصل کند.

## Consequences

- `ShotComposerScreen.kt` اکنون هر ۴ Tab را دارد: MAIN (فیلدهای سطح‌بالا،
  قدم ۲)، CAMERA (قدم ۳)، LIGHTING («نور و محیط»، این قدم)، AUDIO («صدا»،
  این قدم) — **فاز ۴ واحد ۱۶ به‌طور کامل تکمیل شد.**
- کار مستقل بعدی طبق تصمیم قبلی معمار (نه ادامه‌ی فاز ۴): رفع دو محدودیت
  ثبت‌شده در `unit16-execution-plan.md` — `dependentShotsCount` در Tab DNA
  (ADR-047) و سه فیلد بدون UI در `OutputConstraints` (ADR-047).
