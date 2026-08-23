# ADR-142: بازبینی نهایی پیش از تکمیل — دسته‌ی اول (حذف کد مرده + اصلاح مستندات یتیم)

## وضعیت

پذیرفته‌شده (بخش اول از یک بازبینی چندبخشی — بخش‌های بعدی در ADRهای بعدی مستند می‌شوند)

## زمینه

طبق درخواست صریح مالک پروژه («پروژه از دید مالک به مرحله‌ی تکمیل نزدیک
است»)، یک بازبینی جامع و باز (نه فیچر جدید) روی کل کدبیس انجام شد تا
باگ، ناهماهنگی، Flake، بدهی فنی، مغایرت مستند/کد، و Rule/کد یتیم
باقی‌مانده پیدا و رفع شود. طبق دستور صریح، استفاده‌ی فعال از این
Skillها الزامی بود: `systematic-debugging`، `kill-dead-code`،
`flaky-hunter`، `tech-debt-tracker`، `readme-audit`، `adversarial-reviewer`،
`verification-before-completion`.

این ADR فقط **بخش اول** (یافته‌های قطعی و رفع‌شده تا این لحظه) را
مستند می‌کند — چهار بخش تحقیقاتی دیگر (تکمیل readme-audit، بازبینی
خصمانه‌ی واحد ۱۹، بازبینی UI/Compose، بازبینی امنیتی) به‌صورت
Subagent موازی در پس‌زمینه در حال اجرا بودند و نتایجشان بعد از این ADR
می‌رسد — طبق دستور صریح («چند ADR جداگانه مجاز است»)، هر بخش با ADR
جداگانه‌ی خودش بسته می‌شود، نه یک ADR غول‌پیکر واحد.

### یافته‌های `flaky-hunter` (بدون رفع لازم)

اجرای کامل `./gradlew :app:testDebugUnitTest` (Task ID `bmuh899q4`،
خروجی واقعی این Session): **۱۰۱۰ تست کامل، ۱ Fail**
(`OutputDeliveryFlowTest > the Export button starts a real
ACTION_SEND_MULTIPLE chooser...`) — دقیقاً همان کلاس تست و همان الگوی
شکست (`ComposeTimeoutException`) که در ADR-070/071 قبلاً ریشه‌یابی و
مستند شده (تزاحم منابع تحت اجرای کامل موازی، نه یک باگ واقعی). هیچ
کلاس Flaky سومی پیدا نشد.

### یافته‌های `kill-dead-code` (رفع‌شده در این ADR)

با Grep مستقل روی کل `domain/` و `data/` (فراتر از سه نمونه‌ی
قبلی ADR-127/128/140)، دو خوشه‌ی کد مرده‌ی جدید پیدا شد:

1. **`EvolutionEntry`/`EvolutionTimeline`** (`domain/asset/AssetModels.kt`):
   تأیید با Grep — این دو type فقط در تعریف خودشان و یک کامنت توضیحی
   یتیم در `AssetDto.kt` ارجاع داشتند؛ صفر پوشش تست، صفر مصرف‌کننده‌ی
   واقعی. منطق واقعی Evolution Timeline از قبل به واحد ۱۲
   (State & Versioning) موکول شده بود و هرگز پیاده نشد.
2. **خوشه‌ی `StyleMatrix.kt`** (`CustomStyle`، `StyleMatrix`،
   `StyleUpdateResult`، `validatePrimaryStyleUpdate`،
   `checkStyleMatrixCompatibility`): با خواندن کامل فایل (۲۴۰ خط) و
   Grep مستقل، تأیید شد این خوشه دقیقاً همان چیزی است که **ADR-064
   (تصمیم ۱۱) صریحاً برای حذف/ادغام علامت‌گذاری کرده بود** («منسوخ توسط
   طراحی واقعی DNA Tab، نه در انتظار وصل‌شدن») اما هیچ‌گاه اجرا نشده
   بود. مرز دقیق زنده/مرده با دقت رعایت شد — `StyleReference`،
   `toStyleReference()`، `combineStyles`، `checkStyleCompatibility`،
   `categoryCompatibility`، `explicitStyleOverrides` (که در ADR-114/116
   بازنویسی و به `PromptAssembly.kt` وصل شدند) **دست‌نخورده ماندند**؛
   فقط دقیقاً همان زیرمجموعه‌ای که ADR-064 برای حذف علامت زده بود حذف
   شد.

**بررسی شد اما رفع نشد** — `domain/storybreakdown/JsonDoctor.kt`ی
`validateJsonRepairResult`: ابتدا کاندید حذف مشابه StyleMatrix تصور
می‌شد، اما با خواندن دقیق متن ADR-064 (تصمیم ۱۲، Rule 7) روشن شد
تصمیم واقعی «بدون نیاز به وصل» بود، نه «علامت‌گذاری‌شده برای حذف» —
تفاوت دقیقی با خوشه‌ی StyleMatrix. تأیید مستقل با Grep نشان داد
`JsonRepairDialog` (در `AiStoryBreakdownScreen.kt`) هنوز دقیقاً همان
نقشی را ایفا می‌کند که ADR-064 توصیف کرده بود. **تصمیم: بدون تغییر —
همان طبقه‌بندی «یتیم عمدی، قبلاً تصمیم‌گیری‌شده» مثل توابع
`domain/storage/StorageValidation.kt`/`ReferentialIntegrity.kt`/
`domain/stateversioning/LockMechanism.kt`/`StateMachine.kt` که در
ADR-066/076 قبلاً بررسی و بدون‌تغییر رها شده بودند.**

### یافته‌ی اصلی و **حیاتی** (گزارش‌شده، نه رفع‌شده — نیازمند تصمیم معمار)

مهم‌ترین یافته‌ی این بازبینی: **مکانیزم Hard Lock تداوم کاراکتر —
یکی از دو اصل معماری بنیادین کل پروژه طبق مستندات — عملاً در اپ در
حال اجرا اعمال نمی‌شود.** سه تابع `validateCharacterUpdate`/
`validateLocationUpdate`/`validatePropUpdate`
(`domain/asset/AssetContinuity.kt`) هرگز از هیچ Screen/ViewModel واقعی
صدا زده نمی‌شوند — با Grep مستقل روی کل `ui/assets/` تأیید شد؛ تنها
مصرف‌کننده‌شان تست واحد خودشان است. یعنی کاربر امروز می‌تواند
نام/ظاهر‌فیزیکی/بازه‌سنی یک کاراکتر MAIN (سطح `FULL`، که طبق
`ContinuityRules()` باید Blocking و بدون‌استثنا باشد) را آزادانه در
`CharacterAssetFormScreen` تغییر داده و ذخیره کند — بدون آنکه هیچ
بررسی Blocking واقعی اجرا شود.

این یافته **عمداً رفع نشد** — طبق دستور صریح مالک پروژه («اگر رفع یک
مشکل نیاز به یک تصمیم بزرگ معماری دارد، آن را رفع نکن»). رفع واقعی
نیازمند یک تصمیم معماری است که این قدم مجاز به گرفتنش نیست: چگونه
باید قفل سطح-فیلد روی یک فرم Compose آزاد-متن (که امروز اصلاً مفهوم
«کدام فیلد تغییر کرد» ندارد) اعمال شود؟ غیرفعال/خاکستری‌کردن فیلدهای
قفل‌شده؟ مقایسه‌ی مقدار قدیم/جدید در لحظه‌ی ذخیره و مسدودکردن؟ راه
دیگر؟ **این یافته باید مستقیماً به معمار گزارش شود و منتظر تصمیم او
بماند.**

یافته‌ی مرتبط و کوچک‌تر: Rule 1 واحد ۱۰ (`checkTotalSoundLayerCount`،
`domain/audio/AudioValidation.kt`) هم یتیم است — برخلاف Rule خواهرش
(`validateActionSoundTimeline`، که در `ValidationAggregator.kt:119`
وصل است)، این Rule هرگز صدا زده نمی‌شود. برخلاف تصور اولیه، این یک
اصلاح کوچک محلی نیست: امضای عمومی
`aggregateShotValidation(shot, scene, dna, characterAssets,
objectAssets, locationAssets)` اصلاً پارامتر `AudioContext` ندارد (با
Grep مستقل تأیید شد) — وصل‌کردن این Rule نیازمند تغییر امضای عمومی این
تابع + به‌روزرسانی همه‌ی نقاط فراخوانی واقعی است، نه یک تغییر محلی.
این یافته هم به‌جای رفع یک‌جانبه، همراه یافته‌ی Hard Lock بالا گزارش
می‌شود.

### یافته‌های مستندسازی یتیم (رفع‌شده در این ADR)

1. **`docs/governance/ai-coding-guidelines.md` خط ۱۳۲:** «۱۷ واحد از
   مجموع ۱۸ واحد» — این دقیقاً همان شکاف بود که خودِ ADR-141 (بخش
   یافته‌های جانبی) به‌عنوان خارج از Scope آن قدم علامت زده بود؛ اکنون
   با افزودن واحد ۱۹ (که کد اجرایی واقعی هم دارد)، به «۱۸ از ۱۹»
   اصلاح شد.
2. **`README.md` خط ~۲۶۸۸:** ادعای «فرم‌های Asset فقط Ú¯Ø³Ø§Ø®Øª دارند، نه
   ویرایش» در جدول ساختار پوشه‌ها — این ادعا مستقیماً با بند دیگری در
   همان فایل (خط ۲۷۳۳، که با ~~خط‌خورده~~ + «رفع شد» طبق ADR-061 ثبت
   شده) در تناقض بود؛ یعنی خودِ README یک تناقض داخلی داشت. اصلاح شد
   تا با ADR-061 هم‌راستا باشد.
3. **`README.md` خط ~۲۸۱۳-۲۸۱۷:** ادعای «آپلود/انتخاب تصویر واقعی
   هیچ‌جای اپ پیاده نشده» — با Grep مستقل تأیید شد این ادعا **از دو
   جهت** قدیمی بود: (۱) Home Screen Image تنظیمات از قبل با
   `chooseImageLauncher`/`OpenDocument()` رفع شده بود (ADR-075، مدت‌ها
   قبل از این بازبینی — این بند هرگز به‌روز نشده بود)، (۲) عکس مرجع
   Asset هم اکنون آپلود واقعی دارد (`OpenDocument()`، هم‌الگو، واحد ۱۹
   بخش ب، ADR-137 تا ۱۴۰). تنها مورد واقعاً باقی‌مانده: Attached
   References شات (Shot Composer) که هنوز `localFilePath` خالی دارد.
   بند اصلاح شد تا این وضعیت دقیق را منعکس کند.

### راستی‌آزمایی مستقل

- خواندن کامل `AssetContinuity.kt` (۶۶ خط) + Grep روی `ui/assets/` —
  صفر مصرف‌کننده‌ی واقعی سه تابع Lock.
- خواندن کامل `StyleMatrix.kt` (۲۴۰ خط، قبل و بعد از حذف) + Grep
  مستقل روی کل `app/` برای هر نماد حذف‌شده — صفر ارجاع باقی‌مانده
  به‌جز کامنت‌های توضیحی که فقط نام فایل را ذکر می‌کنند (نه نوع).
- خواندن کامل `StyleMatrixTest.kt` (۲۳۹ خط) — تأیید ۵ تست حذف‌شده
  دقیقاً متناظر خوشه‌ی مرده‌اند و ۱۱ تست باقی‌مانده دقیقاً متناظر بخش
  زنده‌اند؛ `assertNotNull`/`assertNull` فقط در ۵ تست حذف‌شده مصرف
  می‌شدند.
- Grep مستقل روی متن دقیق `docs/adr/064-g14-orphaned-rules-decisions.md`
  برای تصمیم ۱۱ (StyleMatrix، حذف) در برابر تصمیم ۱۲ Rule 7
  (JsonDoctor، بدون‌نیاز‌به‌وصل) — تأیید این دو حکم متفاوتند.
- Grep مستقل `AudioContext`/`audioContext` در `ValidationAggregator.kt`
  — صفر رخداد، تأیید عدم وجود پارامتر.
- Grep مستقل روی `AssetFormFlowTest.kt` (خطوط ۲۳۴/۲۵۷/۲۸۵) برای اثبات
  ادعای ویرایش Asset.
- Grep مستقل `chooseImageLauncher`/`OpenDocument`/`takePersistableUriPermission`
  در `SettingsScreen.kt` و فرم‌های سه‌گانه‌ی Asset.

## تصمیم

1. حذف `EvolutionEntry`/`EvolutionTimeline` از `AssetModels.kt` + حذف
   کامنت توضیحی یتیم متناظر در `AssetDto.kt`.
2. حذف خوشه‌ی مرده‌ی `StyleMatrix.kt` (`CustomStyle`، `StyleMatrix`،
   `StyleUpdateResult`، `validatePrimaryStyleUpdate`،
   `checkStyleMatrixCompatibility`) طبق تصمیم ۱۱ ADR-064؛ حذف ۵ تست
   متناظر + ایمپورت‌های یتیم‌شده از `StyleMatrixTest.kt`.
3. بدون تغییر در `JsonDoctor.kt` — تصمیم ADR-064 برای این مورد «بدون
   نیاز به وصل» بود، نه حذف.
4. بدون تغییر در `AssetContinuity.kt`/`ValidationAggregator.kt` —
   یافته‌ی Hard Lock و Rule صدا نیازمند تصمیم معمار، طبق دستور صریح.
5. اصلاح `ai-coding-guidelines.md` خط ۱۳۲ به «۱۸ از ۱۹».
6. اصلاح دو بند قدیمی `README.md` (فرم‌های Asset، آپلود تصویر) طبق
   بالا.

## پیامدها

- کامپایل کامل (`compileDebugKotlin` + `compileDebugUnitTestKotlin`)
  موفق — بدون خطا.
- اجرای هدفمند تست‌های `domain.asset.*`/`domain.visualidentity.*`/
  `data.repository.*` بعد از این تغییرات: **۲۳۷ تست، ۰ Fail، ۰
  Ignored** (این Session).
- یافته‌ی Hard Lock باید در گزارش نهایی این بازبینی به‌عنوان مهم‌ترین
  یافته به معمار ارائه شود — بدون تصمیم او، بدون اقدام بیشتر.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` →
  `BUILD SUCCESSFUL`.
- `./gradlew :app:testDebugUnitTest --tests
  "com.operaboys.cinemashotgenerator.domain.asset.*" --tests
  "com.operaboys.cinemashotgenerator.domain.visualidentity.*" --tests
  "com.operaboys.cinemashotgenerator.data.repository.*"` →
  `BUILD SUCCESSFUL`، گزارش HTML: ۲۳۷ تست/۰ شکست/۰ نادیده‌گرفته‌شده.
- اجرای کامل Suite (پیش از این تغییرات، برای پایه‌ی flaky-hunter):
  ۱۰۱۰ تست، ۱ شکست (`OutputDeliveryFlowTest`، شناخته‌شده، ADR-070/071).
  اجرای کامل نهایی Suite (بعد از تمام تغییرات این بازبینی، شامل
  بخش‌های بعدی) در ADR بعدی این زنجیره گزارش می‌شود.
