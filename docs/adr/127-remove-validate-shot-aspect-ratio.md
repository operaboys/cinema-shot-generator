# ADR-127: حذف Rule یتیم و بی‌معنای معماری validateShotAspectRatio

## وضعیت

پذیرفته‌شده

## زمینه

**این یک حذف است، نه یک اتصال** — برخلاف ADR-125/ADR-126 (که Rule های
یتیم مشابه را واقعاً به یک نقطه‌ی واقعی کد وصل کردند)، این قدم تصمیم
گرفت `validateShotAspectRatio` را کامل از کدبیس حذف کند.

### راستی‌آزمایی مستقل (قبل از حذف)

- `DnaValidation.kt` کامل خوانده شد: `validateShotAspectRatio` (خط ۹۱-۹۹
  + کامنت مستندسازی بالای آن، خط ۸۷-۹۹ عملاً) یک تابع خودبسنده بود —
  بدون هیچ فراخوان دیگری داخل همان فایل، بدون Side effect.
- `DnaValidationTest.kt` کامل خوانده شد: دو تست
  (`validateShotAspectRatio matching is valid`/`mismatch blocks`، خط
  ۱۲۷-۱۳۹) هم دقیقاً همان مرزها را داشتند — بدون اشتراک با تست‌های دیگر
  (`sampleDna()` helper خودش دست‌نخورده ماند، چون `aspectRatio` پارامتر
  آن برای ساخت `OutputConstraints.aspectRatio` واقعی — یک فیلد سطح DNA
  که در تست‌های دیگر همین فایل هم استفاده می‌شود — لازم است).
- `grep` مستقل روی کل `app/src/main` و `app/src/test` تأیید کرد
  `validateShotAspectRatio` هیچ فراخوان‌کننده‌ی واقعی دیگری نداشت — فقط
  تعریف تابع + دو تست + چند اشاره‌ی تاریخی در کامنت‌های Migration (ADR-010،
  ADR-027) که دست‌نخورده ماندند (رکورد تاریخی صحیح، نه ادعای نادرست
  درباره‌ی کد فعلی).
- `AspectRatio` enum و `ProjectDna.OutputConstraints.aspectRatio` مستقل
  تأیید شدند که در `DnaViewModel.kt`/`DnaTabContent.kt`/`DnaLabels.kt`
  (فرم DNA سطح پروژه) کاملاً مستقل از `validateShotAspectRatio` به کار
  خود ادامه می‌دهند — بدون هیچ وابستگی به تابع حذف‌شده.

**یافته‌ی مهم اضافی (تأییدشده مستقل، در دستور کار این قدم ذکر نشده بود):**
این یتیمی قبلاً هم دیده و بررسی شده بود — `docs/adr/064-g14-orphaned-rules-decisions.md`
(تصمیم ۱۰) دقیقاً همین تحلیل معماری را مستقل رسیده بود: «`Shot`/
`CameraSettings` هیچ فیلد `aspectRatio` مستقلی ندارد... پس چیزی برای
مقایسه‌ی aspectRatio شات با DNA وجود ندارد» — اما آن تصمیم وقت «بدون
اقدام» بود، **مشروط**: «مگر اگر یک فاز آینده per-Shot aspect ratio
override اضافه کند». این قدم آن شرط را قطعی می‌بندد — طبق تصمیم صریح
تازه‌ی کاربر پروژه، چنین Overrideـی هرگز اضافه نخواهد شد (طراحی درست
پروژه: همه‌ی شات‌های یک پروژه همیشه یک aspectRatio واحد از DNA دارند) —
پس شرط ADR-064 دیگر هرگز محقق نمی‌شود و ادامه‌ی نگهداری این Rule بی‌معنا
است. این ADR آن تصمیم قدیمی‌تر را می‌بندد، نه نقض می‌کند.

## تصمیم

**حذف کامل، نه اتصال.** دلیل معماری دقیق (برای این‌که در آینده کسی این
Rule را «یتیم فراموش‌شده» تصور نکند و اشتباهاً بخواهد وصلش کند):

`domain/shot/ShotModels.kt` → `data class Shot` **هرگز** فیلد `aspectRatio`
مستقلی نداشته و طبق تصمیم صریح کاربر پروژه **هرگز نباید داشته باشد** —
نسبت تصویر همیشه و فقط یک مقدار سراسری سطح پروژه است
(`ProjectDna.outputConstraints.aspectRatio`)، بدون هیچ مکانیزم Override
سطح شات. چون هیچ‌جای ساختار داده چیزی برای «مقایسه با DNA» وجود ندارد،
`validateShotAspectRatio` **از نظر ساختاری هرگز نمی‌تواند در یک سناریوی
واقعی معنادار فراخوانی شود** — نه این‌که هنوز به یک نقطه‌ی UI وصل نشده
(مثل ADR-125/۱۲۶)، بلکه اصلاً چیزی برای وصل‌کردن به آن وجود ندارد. این یک
محافظ بی‌فایده است، نه یک Rule ناقص.

⚠️ **برای خواننده‌ی آینده**: اگر روزی این پروژه تصمیم گرفت شات‌ها بتوانند
یک `aspectRatio` مستقل از DNA پروژه داشته باشند (Override سطح شات)، آن‌
موقع یک Rule مشابه دوباره لازم می‌شود — اما باید از صفر با امضای تازه
(`Shot`، نه `AspectRatio` خام) نوشته شود، نه این‌که این تابع حذف‌شده
دوباره اضافه شود؛ طراحی فعلی پروژه صراحتاً این سناریو را رد کرده است.

### تغییرات

- `domain/dna/DnaValidation.kt`: تابع `validateShotAspectRatio` (شامل
  کامنت مستندسازی بالای آن) کامل حذف شد.
- `app/src/test/.../domain/dna/DnaValidationTest.kt`: دو تست
  `validateShotAspectRatio matching is valid`/`mismatch blocks` کامل
  حذف شدند.
- هیچ فایل دیگری تغییر نکرد — `AspectRatio` enum،
  `ProjectDna.OutputConstraints.aspectRatio`، و هر فایل UI مرتبط با DNA
  دست‌نخورده ماندند (عمداً؛ این یتیمی مستقل و جدا از فیچر «mandatoryElements
  سطح شات» است که در یک قدم کاملاً مجزا بررسی می‌شود).

## پیامدها

- کاهش سطح کد بدون هیچ تغییر رفتاری — این تابع هرگز فراخوانی نمی‌شد.
- Rule های واقعی/متصل خواهر آن (`validateShotAgainstDna`،
  `validateShotDuration`، `validateColorPalette`) و Rule مستنداً یتیم
  دیگر (`checkMandatoryElementsPresent`، هم‌دلیل ADR-055/ADR-064) بدون
  تغییر باقی ماندند.
- `docs/adr/064-g14-orphaned-rules-decisions.md` (تصمیم ۱۰) اکنون یک
  تصمیم بسته‌شده است — این ADR آن را ارجاع می‌دهد، نه بازنویسی می‌کند.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۵۱ تست (۲ تست کمتر از
  قدم قبلی، دقیقاً هم‌اندازه‌ی دو تست حذف‌شده)، ۱ شکست — از کلاس ناپایدار
  از‌قبل‌مستندشده‌ی `OutputDeliveryFlowTest` (مستندشده از ADR-044،
  ریشه‌یابی‌شده ADR-070/071)؛ در اجرای ایزوله دوباره Pass شد. هیچ فایل
  دیگری به‌طور غیرمنتظره به تابع حذف‌شده ارجاع نمی‌داد — `DnaValidationTest.kt`
  با ۱۴ تست باقی‌مانده کامل Pass شد.
- `./gradlew :app:assembleDebug`: موفق.
