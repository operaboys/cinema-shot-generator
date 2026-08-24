# ADR-145: بستن یافته‌ی کوچک‌تر چکاپ نهایی — اتصال checkTotalSoundLayerCount

## وضعیت

پذیرفته‌شده

## زمینه

چکاپ جامع نهایی (ADR-143) یک یافته‌ی کوچک‌تر مرتبط با یافته‌ی حیاتی Hard
Lock (ADR-144) هم ثبت کرده بود: `checkTotalSoundLayerCount`
(`domain/audio/AudioValidation.kt`، Rule 1 واحد ۱۰، Warning-only) هرگز
از `aggregateShotValidation` (واحد ۰۷) فراخوانی نمی‌شد، چون آن تابع
اصلاً پارامتر `AudioContext` نداشت. این قدم دقیقاً همان شکاف را
می‌بندد.

## راستی‌آزمایی مستقل (پیش از نوشتن کد)

- امضای دقیق `checkTotalSoundLayerCount(audioContext: AudioContext):
  ValidationIssue?` و منطق آن (مجموع سه لیست > ۸ → Warning) با خواندن
  کامل `AudioValidation.kt` تأیید شد — بدون نیاز به تغییر (طبق دستور،
  فقط خوانده شد).
- هر چهار فراخوان‌کننده‌ی `aggregateShotValidation` با Grep سراسری پیدا
  و کامل خوانده شدند.
- **دو یافته‌ی واقعی که با پیش‌بریفینگ فرق داشت** (طبق دستور صریح، اینجا
  ثبت می‌شود):
  1. `OutputDeliveryViewModel` از قبل به `AudioContextRepository` **مستقیم**
     دسترسی نداشت، اما به `PromptGenerationInput` (از
     `promptGenerationRepository.collectData`) دسترسی دارد که خودش از
     قبل یک فیلد `audioContext: AudioContext?` کامل‌پرشده دارد
     (`PromptGenerationRepository.collectData` همین حالا
     `audioContextRepository.loadAudioContext` را صدا می‌زند و نتیجه
     را داخل `PromptGenerationInput` می‌گذارد). پس نیازی به تزریق
     Repository جدید یا فراخوان I/O دوم نبود — فقط `input.audioContext`
     مستقیماً پاس داده شد.
  2. در `ShotComposerViewModel`، `refreshValidationSummary()` (که
     `aggregateShotValidation` را صدا می‌زند) عمداً یک تابع **خالص و
     بدون I/O** طراحی شده (طبق کامنت مستند خودِ تابع — روی هر
     کلیدفشاری اجرا می‌شود، بدون Debounce، دقیقاً چون هیچ I/O ای ندارد).
     افزودن یک فراخوان `suspend` مستقیم داخل آن این طراحی را می‌شکست.
     رفع: `AudioContext` مثل `cachedScene`/`cachedDna`/
     `cachedCharacterAssets` **یک‌بار** در `init` بارگذاری و Cache شد
     (`cachedAudioContext`) — چون این ViewModel هیچ مسیر ویرایش برای
     خودِ `AudioContext` ندارد (Tab «صدا» فیلدهای جداگانه‌ی
     `Shot.soundProfile` را ویرایش می‌کند، نه `AudioContext`)، بارگذاری
     یک‌باره صحیح و کافی است.

## تصمیم

1. `ValidationAggregator.kt`: پارامتر `audioContext: AudioContext? = null`
   (Nullable، پیش‌فرض null — Backward Compatible) به
   `aggregateShotValidation` اضافه شد. در سطح **LOGICAL_CONSISTENCY**
   (نه DATA_COMPLETENESS یا CONTINUITY_AND_DEPENDENCY) وایر شد — دلیل:
   این Rule فقط فیلدهای خودِ همین `AudioContext` را با یک آستانه‌ی ثابت
   می‌سنجد (نه کامل‌بودن داده‌ی الزامی شات، که Rule 2 همین واحد
   `validateActionSoundTimeline` است و در Level 1 قرار دارد؛ نه
   وابستگی به DNA/Scene/Asset)، دقیقاً هم‌الگو با
   `checkSlowMotionInDialogue`/`checkCalmAtmosphereInClimax` موجود در
   همان سطح.
2. `OutputDeliveryViewModel.kt`: `input.audioContext` موجود مستقیماً
   پاس داده شد — **بدون تزریق Repository جدید** (طبق یافته‌ی بالا).
3. `ShotComposerViewModel.kt`: `AudioContextRepository` به‌عنوان
   وابستگی جدید تزریق‌پذیر اضافه شد؛ `cachedAudioContext` یک‌بار در
   `init` بارگذاری می‌شود (هم‌الگو با فیلدهای Cache موجود).
4. `ValidationViewModel.kt`/`StudioOutputViewModel.kt`: هر دو
   `AudioContextRepository` را به‌عنوان وابستگی جدید گرفتند و در همان
   نقطه‌ی بارگذاری Shot/Scene/DNA/Asset موجود، `AudioContext` را هم
   بارگذاری و به `aggregateShotValidation` پاس دادند — هم‌الگو دقیق با
   بقیه‌ی Repository های این دو فایل.
5. `ValidationViewModel.factory`/`StudioOutputViewModel.factory`:
   پارامتر `audioContextRepository` اضافه شد. در `ValidationViewModel`
   این پارامتر عمداً **بعد از `database`** (نه در ترتیب طبیعی‌اش کنار
   بقیه‌ی Repository ها) قرار گرفت — یافته‌ی دیباگ واقعی: فراخوان
   موقعیتی موجود در `ValidationScreen.kt` با قرار گرفتن آن قبل از
   `database` می‌شکست (تست کامپایل این را فوراً آشکار کرد).

## پیامدها

- فایل‌های تغییریافته: `ValidationAggregator.kt`،
  `ShotComposerViewModel.kt`، `OutputDeliveryViewModel.kt`،
  `ValidationViewModel.kt`، `StudioOutputViewModel.kt`،
  `ValidationAggregatorTest.kt`. **`AudioValidation.kt` دست‌نخورده
  ماند** — طبق دستور صریح، فقط خوانده شد.
- کاربر اکنون هنگام Preview/Validation/Output Delivery/Studio Dashboard
  یک شات با بیش از ۸ لایه‌ی صوتی، یک هشدار واقعی می‌بیند — قبلاً این
  Rule هیچ‌جا اجرا نمی‌شد.
- `OutputDeliveryViewModel` هیچ وابستگی تازه‌ای نگرفت (فقط از داده‌ی
  از‌قبل‌بارگذاری‌شده استفاده کرد).

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` →
  `BUILD SUCCESSFUL` (یک بار FAILED به‌خاطر شکستن فراخوان موقعیتی
  `ValidationScreen.kt` — بلافاصله با جابه‌جایی ترتیب پارامتر رفع و
  دوباره تأیید شد).
- تست‌های هدفمند `ValidationAggregatorTest` (سه تست جدید: بیش از ۸ →
  Warning واقعی؛ دقیقاً ۸ → بدون Warning؛ `audioContext` حذف‌شده →
  رفتار قبلی بدون تغییر): **۲۷ تست، ۰ شکست**.
- تست‌های هدفمند چهار پکیج ViewModel فراخوان‌کننده
  (`ui.shots`/`ui.outputdelivery`/`ui.validation`/`ui.studio`):
  **۵۱ تست، ۰ شکست**.
- اجرای کامل `./gradlew :app:testDebugUnitTest` و
  `./gradlew :app:assembleDebug` در ادامه‌ی همین قدم، در گزارش نهایی
  با شمار واقعی مستند شده‌اند.
