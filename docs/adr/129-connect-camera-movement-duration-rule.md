# ADR-129: قدم ۱ از ۲ — اتصال Rule یتیم validateCameraMovementDuration

## وضعیت

پذیرفته‌شده

## زمینه

`domain/camera/CameraValidation.kt` یک Rule یتیم داشت —
`validateCameraMovementDuration(movementDurationSeconds, shotDurationSeconds)`.
با `grep` مستقل تأیید شد هیچ فراخوان‌کننده‌ی واقعی نداشت.

### این وضعیت آگاهانه و از قبل مستند بود — ADR-008

`docs/adr/008-unit09-camera-motion-deviations.md` (تصمیم ۱، ۲۰۲۶-۰۷-۱۹) این
وضعیت را از ابتدا پیش‌بینی کرده بود: چون `CameraMovement` (شش Variant:
Basic/Orbit/DronePath/DollyZoom/HandheldShake/Compound) هیچ فیلد `duration`
نداشت، این Rule پارامتر خارجی مستقل گرفت — با یادداشت صریح: «اگر در آینده
واحدهای بالاتر واقعاً مدت حرکت را محاسبه کنند، افزودن فیلد duration دوباره
مطرح می‌شود». **این ADR دقیقاً همان «آینده» است.**

### راستی‌آزمایی مستقل

- `CameraModels.kt`/`CameraValidation.kt` کامل خوانده شدند — امضای
  `validateCameraMovementDuration` دقیقاً همان‌طور که توصیف شده بود.
- `ValidationAggregator.kt` کامل خوانده شد — بلوک
  `shot.camera.overrideValue?.let { camera -> ... }` دقیقاً محل درست برای
  این Rule بود (همان الگوی ۴ Rule دوربین موجود).
- `CameraTabContent.kt`/`ShotComposerViewModel.kt` کامل خوانده شدند — الگوی
  فیلد متنی Float اختیاری (`_durationSecondsText`/`toFloatOrNull()`) از قبل
  در همین فایل برای `Shot.durationSeconds` وجود داشت؛ همان الگو برای این
  فیلد تازه (با تفاوت: پیش‌فرض خالی، نه `"4"`، چون این فیلد اختیاری است)
  بازاستفاده شد.

هیچ مغایرتی با پیش‌بریفینگ پیدا نشد.

## تصمیم

### چرا `movementDurationSeconds` در `CameraSettings`، نه در هرکدام از شش Variant

این مفهوم («این حرکت چقدر طول می‌کشد») برای هر شش نوع حرکت یکسان و مستقل از
نوع خاص حرکت معنا دارد. افزودنش به داخل `sealed class CameraMovement` یعنی
تکرار همان فیلد در شش `data class` جدا، بدون فایده. `Nullable` با پیش‌فرض
`null` — پروژه‌ها/شات‌های موجود کاربر (که این مقدار را نداشتند) بدون تغییر
رفتار باقی می‌مانند؛ وقتی `null` است، Rule اصلاً صدا زده نمی‌شود (چون
مقداری برای مقایسه نیست، نه این‌که به‌اشتباه Blocking بدهد).

### پیاده‌سازی

**۱. دامنه** (`CameraModels.kt`): `CameraSettings.movementDurationSeconds: Float? = null`.

**۲. DTO/Mapper**: `CameraSettingsDto.movementDurationSeconds: Float? = null`
(`Json { ignoreUnknownKeys = true }` موجود یعنی بدون Migration، شات‌های
موجود کاربر بدون خطا Decode می‌شوند)؛ `DtoMappers.kt`
(`CameraSettingsDto.toDomain()`/`CameraSettings.toDto()`) به‌روزرسانی شد.

**۳. `ValidationAggregator.kt`**:

```kotlin
camera.movementDurationSeconds?.let { duration ->
    add(l2, validateCameraMovementDuration(duration, shot.durationSeconds))
}
```

دقیقاً هم‌الگو با بقیه‌ی Rule های همان بلوک — فقط وقتی کاربر واقعاً مقدار
وارد کرده باشد صدا زده می‌شود.

**۴. UI/ViewModel**: `ShotComposerViewModel.kt` یک `_movementDurationSecondsText`
تازه (String-backed، هم‌الگو دقیق با `_durationSecondsText`/`toFloatOrNull()`
موجود همان فایل) + `setMovementDurationSecondsText`. `CameraTabContent.kt`
(`MovementSection`) یک `OutlinedTextField` تازه — مستقل از نوع Variant
انتخابی، بعد از انتخاب Tier و قبل از جزئیات خاص هر Variant — به‌همراه یک
متن راهنمای کوچک که توضیح می‌دهد این فیلد اختیاری است.

## پیامدها

- هیچ رفتار مشاهده‌پذیر برای پروژه‌های موجود کاربر تغییر نکرد — فیلد جدید
  `null` پیش‌فرض دارد و Rule فقط با مقدار واقعی صدا زده می‌شود.
- کامنت «فهرست Rule های عمداً وایرنشده» در `ValidationAggregator.kt`
  (که قبلاً `validateCameraMovementDuration` را نام می‌برد) به‌روزرسانی شد
  تا وضعیت جدید (وصل‌شده) را منعکس کند — همان انضباط ADR-128.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- `ValidationAggregatorTest.kt`: ۱۸ تست (۱۵ قبلی + ۳ تازه)، همه Pass —
  `null`→بدون Issue، مقدار بیش از مدت شات→`BLOCKING`، مقدار معتبر→بدون Issue.
- `CameraValidationTest.kt` (۱۳ تست موجود، بدون تغییر امضا) و `UiStringsTest.kt`
  (شامل تست جامع Scan کلیدهای `uiString`): همه Pass.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۵۲ تست، ۹۵۱ Pass، ۱ Fail —
  `OutputDeliveryFlowTest` («the Export button starts a real ACTION_SEND_MULTIPLE
  chooser...»)، یکی از کلاس‌های ناپایدار (Flaky) از پیش مستندشده‌ی این پروژه
  (بدون ارتباط با تغییرات این ADR). با اجرای مجدد و مجزا
  (`--tests "...OutputDeliveryFlowTest"`) هر ۶ تست این کلاس Pass شدند —
  تأیید شد Flakiness شناخته‌شده است، نه رگرسیون واقعی.
- `./gradlew :app:assembleDebug`: موفق.
