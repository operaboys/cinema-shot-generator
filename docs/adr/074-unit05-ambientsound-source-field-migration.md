# ADR-074: Migration نسخه ۳ واحد ۰۵ — افزودن `source` به `AmbientSound`

## زمینه

بلوپرینت `05-shot-engine-v2.md` (نسخه ۳) و `10-audio-context.md` (نسخه
۲) هر دو ادعا می‌کنند `AmbientSound` این دو واحد هماهنگ است — هر دو
باید ۴ فیلد داشته باشند (`type`، `intensity`، `description`، `source`)
با پیش‌فرض‌های متفاوت به‌ازای زمینه‌ی متفاوت. بازبینی مستقل قبل از هر
تغییر کد این ادعا را تأیید کرد: `domain/audio/AudioModels.kt` واقعاً
۴ فیلد داشت، اما `domain/shot/ShotModels.kt` فقط ۳ فیلد داشت — دقیقاً
همان‌طور که در دستور این قدم آمده بود.

## بازبینی Blast Radius (قبل از هر تغییر)

`grep` سراسری `AmbientSound(` روی `app/src/main` و `app/src/test` دو
فراخوان واقعی امضای ۳ پارامتری واحد ۰۵ پیدا کرد — دقیقاً همان دو محلی
که در دستور کار آمده بود، بدون فراخوان دیگری که دیده نشده باشد:
- `ui/shots/ShotComposerViewModel.kt` (`generateAmbientSounds`)
- `data/repository/DtoMappers.kt` (هر دو جهت `toDomain`/`toDto`)

و یک فراخوان تست (`ShotRepositoryTest.kt`). همه‌ی فراخوان‌های دیگر
`AmbientSound(...)` در کدبیس، `domain.audio.AmbientSound` (۴ پارامتری،
از قبل درست) بودند.

`AmbientSoundSuggestion` (واحد ۰۸) فیلد معادل `source` **ندارد** —
فقط `type`/`intensity`/`description`. `generateAmbientSounds` تنها راه
ساخت `AmbientSound` در `ShotComposerViewModel` است؛ هیچ مسیر
افزودن/ویرایش دستی برای `AmbientSound` در این ViewModel یا
`AudioTabContent.kt` وجود ندارد (برخلاف `ActionSound` که `addActionSound`
دارد) — پس `source` همیشه `"auto_generated"` است، بدون نیاز به یک
مسیر `"user_edited"` جداگانه.

`AmbientSoundDto` (`data/repository/ShotDto.kt`) هم فقط ۳ فیلد داشت —
یک تغییر DTO/Schema واقعی. `ShotEntity.shotDataJson` یک JSON Blob است
(نه ستون‌های مجزای Room)؛ چون فیلد جدید پیش‌فرض دارد، رکوردهای قدیمی
بدون این کلید هم با `kotlinx.serialization` به‌درستی decode می‌شوند —
Room Migration لازم نیست.

## تغییر

- `domain/shot/ShotModels.kt`: `AmbientSound` فیلد
  `source: String = "auto_generated"` گرفت.
- `ui/shots/ShotComposerViewModel.kt`: `generateAmbientSounds` صریحاً
  `source = "auto_generated"` ست می‌کند.
- `data/repository/ShotDto.kt`: `AmbientSoundDto` همان فیلد را با همان
  پیش‌فرض گرفت.
- `data/repository/DtoMappers.kt`: هر دو جهت نگاشت (`toDomain`/`toDto`)
  فیلد جدید را منتقل می‌کنند.
- کامنت‌های هدر `ShotModels.kt` (کنار `AmbientSound`) و
  `AudioModels.kt` (که صریحاً این عدم‌هماهنگی را به‌عنوان یک تفاوت
  «واقعی و دائمی» توصیف کرده بود) به‌روزرسانی شدند تا این Migration را
  منعکس کنند.
- `ShotRepositoryTest.kt`: فراخوان تست به `source` صریح به‌روزرسانی
  شد. `ShotsFlowTest.kt`: یک Assertion جدید اضافه شد که تأیید می‌کند
  `source` واقعاً روی Room ذخیره/بازیابی می‌شود (نه فقط در Memory
  ViewModel).

## راستی‌آزمایی

- `gradle :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` →
  موفق.
- `gradle :app:testDebugUnitTest` روی `domain/shot`، `domain/audio`،
  `data/repository`، و `ui.shots.ShotsFlowTest` → ۱۳۵ تست، ۰ شکست.
- `gradle :app:assembleDebug` (چون این یک تغییر DTO واقعی بود) →
  موفق.

## Skills استفاده‌شده

`zero-hallucination-coder` (بازبینی مستقل هر سه ادعای پیش‌بررسی —
تعداد/نام فیلد، Blast Radius کامل فراخوان‌ها، و وجود/عدم‌وجود فیلد
معادل در `AmbientSoundSuggestion` — با `grep` روی کد واقعی، پیش از
نوشتن هر خط کد).
