# ADR-126: اتصال Rule یتیم validateProfileAvailability در OutputDeliveryViewModel

## وضعیت

پذیرفته‌شده

## زمینه

`domain/outputdelivery/ModelProfileLibrary.kt` یک Rule نوشته‌شده اما یتیم
دارد — `validateProfileAvailability`. مصرف‌کننده‌ی واقعی همیشه `selectProfile`
بود (که خودش Fallback می‌کند)، نه این Rule.

### راستی‌آزمایی مستقل (قبل از کدنویسی)

- `ModelProfileLibrary.kt` کامل خوانده شد: تأیید شد `validateProfileAvailability`
  پارامتر اولش `targetPlatform` است، نه `profileId` — و فقط وقتی `null`
  برنمی‌گرداند که هیچ‌کدام از `hasExactMatch`/`hasUniversalFallback` برقرار
  نباشد.
- `ModelProfiles.kt` کامل خوانده شد: تأیید شد `platform`/`profileId` واقعاً
  دو مفهوم متفاوتند (مثلاً `veoProfile`: `platform="veo"`،
  `profileId="veo_3_1"`)، و `ALL_MODEL_PROFILES` یک `List` ثابت Kotlin است
  که همیشه `universalDefaultProfile` را در خود دارد (خط ۳۱۰) — این یک
  تصمیم معماری آگاهانه‌ی مستندشده (نه یک محدودیت پنهان): طبق کامنت خودِ
  `ModelProfiles.kt`، مهاجرت این کتابخانه از `val` های ثابت Kotlin به یک
  فایل JSON خارجی/قابل‌ویرایش (که بلوپرینت ۱۴ اصلی به‌عنوان معماری
  نهایی وعده داده بود) **در این قدم و فعلاً در دستور کار نیست** — یک تصمیم
  آگاهانه‌ی مدیریت ریسک، ثبت‌شده از قبل در ADR-021 به‌عنوان بدهی مستند، نه
  چیزی که این قدم نادیده گرفته باشد.
- `OutputDeliveryViewModel.kt` کامل خوانده شد: تأیید شد خط ۸۷/۱۲۷-۱۳۰ دقیقاً
  همان الگوی توصیف‌شده بود — `initialModelProfileId: String?` تنها نقطه‌ی
  واقعی کدبیس که یک مقدار خارجی به یک `ModelProfile` واقعی تبدیل می‌شود.
- `WorkflowViewModel.kt` (`resolveInitialLanguage`، ADR-125) برای الگوی
  دقیق `Log.w` خوانده شد.

هیچ مغایرتی در این چهار فایل با پیش‌بریفینگ پیدا نشد.

## تصمیم

### چرا `platform`/`profileId` دو مفهوم متفاوتند

`platform` یک شناسه‌ی کوتاه‌تر و عمومی‌تر است (مثلاً `"veo"`) — همان چیزی
که `selectProfile`/`validateProfileAvailability` برای «کدام سرویس» به کار
می‌برند. `profileId` شناسه‌ی دقیق یک نسخه‌ی مشخص از آن پلتفرم است (مثلاً
`"veo_3_1"`) — همان چیزی که UI/Persistence/`initialModelProfileId` واقعاً
ذخیره و رد‌وبدل می‌کنند. این پروژه هیچ‌جای دیگری این دو را با هم اشتباه
نمی‌گیرد؛ این ADR هم عمداً این تفاوت را حفظ می‌کند.

### چرا این Rule در این استفاده‌ی خاص عملاً فقط برای «even universal_default هم نیست» طراحی شده

`resolveInitialProfileId` فقط وقتی `validateProfileAvailability` را صدا
می‌زند که `initialModelProfileId` غیر-null باشد ولی هیچ پروفایلی با آن
دقیقاً همان `profileId` در `ALL_MODEL_PROFILES` پیدا نشود — در این حالت،
همان رشته‌ی نامعتبر به‌عنوان `targetPlatform` به Rule داده می‌شود. چون این
رشته (یک `profileId` نامعتبر) تقریباً هرگز با هیچ `platform` واقعی یکی
نیست، شرط اول Rule (`hasExactMatch`) همیشه `false` است — پس عملاً فقط شرط
دوم (`hasUniversalFallback`) باقی می‌ماند: آیا `universal_default` همچنان
در `ALL_MODEL_PROFILES` هست؟ چون `ALL_MODEL_PROFILES` یک لیست ثابت Kotlin
است که همیشه `universalDefaultProfile` را دارد، این Rule **در پیکربندی
فعلی همیشه `null` برمی‌گرداند** — یک محافظ ساختاری برای سناریوی فاجعه‌بار
(«حتی universal_default هم حذف شده»)، نه ابزار تشخیص هر `profileId`
نامعتبر به‌تنهایی (که خودِ `resolveInitialProfileId`، مستقل از این Rule،
با `firstOrNull == null` تشخیص می‌دهد و همیشه Log می‌کند).

### پیاده‌سازی

```kotlin
private fun resolveInitialProfileId(initialModelProfileId: String?): String {
    val matched = ALL_MODEL_PROFILES.firstOrNull { it.profileId == initialModelProfileId }
    if (matched != null) return matched.profileId
    if (initialModelProfileId != null) {
        validateProfileAvailability(initialModelProfileId, ALL_MODEL_PROFILES)?.let { issue ->
            Log.w("OutputDeliveryViewModel", "persisted model profile '$initialModelProfileId' is invalid, falling back: ${issue.message}")
        }
    }
    return ALL_MODEL_PROFILES.first().profileId
}
```

رفتار ظاهری عمداً بدون تغییر ماند — `Fallback` به `ALL_MODEL_PROFILES.first()`
دقیقاً همان‌طور که قبلاً بود باقی می‌ماند. هم‌الگو دقیق با
`resolveInitialLanguage` (ADR-125): `android.util.Log` پلتفرم استاندارد،
بدون هیچ وابستگی Logging تازه.

## انحراف از دستور کار این قدم (افشا‌شده، طبق قانون صریح)

دستور کار این قدم در بخش تحلیل فنی خودش صریحاً پیش‌بینی کرده بود که این
Rule «عملاً یک سناریوی فاجعه‌بار که فعلاً هرگز رخ نمی‌دهد» را می‌سنجد — اما
در فهرست تست‌های الزامی همان دستور کار، تست دوم انتظار داشت «با یک
`initialModelProfileId` نامعتبر... یک Log هشدار واقعاً ثبت می‌شود». این دو
بخش دستور کار با هم ناسازگار بودند: چون `ALL_MODEL_PROFILES` همیشه
`universal_default` را دارد، `validateProfileAvailability` برای **هیچ**
رشته‌ی نامعتبری (حتی یک `profileId` کاملاً جعلی) واقعاً `ValidationIssue`
برنمی‌گرداند — پس هیچ هشداری هم واقعاً Log نمی‌شود.

تست دوم با رفتار واقعی/صحیح کد هماهنگ شد (بدون هشدار برای این سناریو، با
توضیح کامل چرا) — طبق راستی‌آزمایی مستقل، نه فرض دستور کار. اثبات این‌که
خودِ Rule وقتی واقعاً `universal_default` هم موجود نباشد کار می‌کند از قبل
در `ModelProfileLibraryTest.kt`
(`validateProfileAvailability is blocking when nothing is available at all`)
پوشش داده شده بود — تکرارش با یک `profiles` list ساختگی در سطح ViewModel
چیز تازه‌ای اثبات نمی‌کرد.

## پیامدها

- هیچ رفتار مشاهده‌پذیر برای کاربر نهایی تغییر نکرد.
- هیچ وابستگی جدیدی اضافه نشد.
- در پیکربندی فعلی (`ALL_MODEL_PROFILES` ثابت، همیشه شامل `universal_default`)،
  مسیر `Log.w` این Rule عملاً هرگز اجرا نمی‌شود — اما `resolveInitialProfileId`
  خودش، مستقل از این Rule، هر `profileId` نامعتبری را Log می‌کند؟ **نه** —
  طبق تصمیم صریح این ADR، `Log.w` فقط داخل شرط موفقیت این Rule قرار دارد
  (نه یک `Log.w` جدا و بی‌قید‌وشرط برای هر `profileId` نامعتبر) — چون
  دستور کار این قدم صراحتاً خواسته بود Log فقط «اگر ValidationIssue
  برگرداند» ثبت شود، نه هر بار که `firstOrNull` تهی باشد. این یعنی این
  Rule فعلاً یک محافظ خاموش برای آینده است (اگر معماری این کتابخانه به
  JSON خارجی مهاجرت کند و `universal_default` دیگر تضمین‌شده نباشد،
  همان‌جا این Rule واقعاً فعال می‌شود) — نه یک ابزار Debug فعال امروز.

## راستی‌آزمایی

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin`: موفق.
- `OutputDeliveryViewModelTest.kt`: ۱۲ تست (۹ قبلی + ۳ تازه)، همه Pass —
  شامل تصحیح تست دوم پس از یافتن ناسازگاری دستور کار.
- `./gradlew :app:testDebugUnitTest` (کل مجموعه): ۹۵۳ تست، ۱ شکست — از
  کلاس ناپایدار از‌قبل‌مستندشده‌ی `OutputDeliveryFlowTest` (مستندشده از
  ADR-044، ریشه‌یابی‌شده ADR-070/071)؛ در اجرای ایزوله دوباره Pass شد،
  بدون هیچ شکست واقعی مرتبط با این قدم.
- `./gradlew :app:assembleDebug`: موفق.
