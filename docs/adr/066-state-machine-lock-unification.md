# ADR-066: یکسان‌سازی معماری State Machine/Lock — بدهی فنی مستند در ADR-064

## زمینه

ADR-064 (رفع G14، تصمیم ۳) این بدهی فنی را کشف و برای تصمیم مشترک
علامت‌گذاری کرده بود: `domain/project/ProjectLifecycle.kt`
(`archiveProject`) و `domain/scene/SceneLifecycle.kt` (`lockScene`)
به‌جای `validateStateTransition` (لایه‌ی `ValidationIssue`-برگردان
واحد ۱۲ که دقیقاً برای همین منظور ساخته شده بود)، مستقیماً از
`canTransition` خام استفاده می‌کردند. این قدم آن را رفع می‌کند.

## بازبینی مستقل پیش‌بررسی‌های دستور کار (طبق دستور صریح)

- `validateStateTransition` پیام عمومی «انتقال از X به Y مجاز نیست»
  برمی‌گرداند — تأیید شد (`StateMachine.kt:30-36`، پیش از این قدم).
- `archiveProject` پیام سفارشی «پروژه در وضعیت X است — طبق قوانین State
  Machine واحد ۱۲، فقط پروژه‌های FINAL قابل آرشیو شدن‌اند» دارد —
  تأیید شد.
- **ادعای پیش‌بررسی («بررسی کن آیا SceneLifecycle.kt هم پیام سفارشی
  مشابهی دارد») تأیید شد**: `lockScene` پیام سفارشی «صحنه در وضعیت X
  است — طبق قوانین State Machine واحد ۱۲، این انتقال به Locked مجاز
  نیست» دارد — ساختار کاملاً یکسان با `archiveProject`
  (`if (!canTransition(...)) return Result.failure(IllegalStateException(...))`).
- فراخوان‌کننده‌های واقعی هر دو تابع بررسی شدند: `ProjectRepository.archiveProject`
  (از طریق نام مستعار `archiveProject as applyArchiveTransition`) و
  `SceneDetailViewModel.lockScene()` (از طریق `lockScene as applyLockTransition`)
  — هر دو مستقیماً `Result.failure(...).message`/`.exceptionOrNull()?.message`
  را بدون تغییر به کاربر نشان می‌دهند؛ یعنی حفظ دقیق متن پیام برای عدم
  تغییر رفتار حیاتی است.

## تصمیم معماری: گزینه‌ی ب — `customMessage: String?` روی `validateStateTransition`

از سه گزینه‌ی مطرح‌شده در دستور کار:

- **گزینه‌ی الف** (فقط تضمین غیرمستقیم یک منبع حقیقت) رد شد — طبق
  استدلال خودِ دستور کار، این عملاً وضعیت فعلی است و چیزی را واقعاً
  تغییر نمی‌دهد؛ `canTransition` هنوز مستقیماً در دو فایل Lifecycle
  فراخوانی می‌شود، نه از طریق لایه‌ی مشترک `ValidationIssue`.
- **گزینه‌ی ب** انتخاب شد: `validateStateTransition` یک پارامتر
  اختیاری `customMessage: String? = null` گرفت. هر دو تابع Lifecycle
  اکنون این تابع را صدا می‌زنند و پیام سفارشی خودشان را پاس می‌دهند؛
  اگر `issue != null`، همان `issue.message` (که دقیقاً `customMessage`
  است) داخل `IllegalStateException` قرار می‌گیرد.
- گزینه‌ی جایگزین دیگری (مثلاً یک `sealed class TransitionResult`
  تازه، یا انتقال کامل هر دو ViewModel به مصرف مستقیم `ValidationIssue`
  به‌جای `Result<T>`) بررسی و رد شد — هر دو نیازمند تغییر امضای
  عمومی `archiveProject`/`lockScene` (که چند فایل مصرف‌کننده دارند)
  بودند، بدون فایده‌ی اضافه‌ای فراتر از چیزی که گزینه‌ی ب می‌دهد.

**چرا گزینه‌ی ب واقعاً «یک منبع حقیقت» می‌سازد:** بعد از این تغییر،
`canTransition` (تنها منطق خام بررسی مجاز/غیرمجاز) **فقط از داخل
`validateStateTransition`** فراخوانی می‌شود — با `grep -rn
"canTransition" app/src/main/` تأیید شد هیچ فایل دیگری (نه
`ProjectLifecycle.kt`، نه `SceneLifecycle.kt`) دیگر مستقیماً آن را صدا
نمی‌زند. `archiveProject`/`lockScene` هر دو اکنون از `validateStateTransition`
عبور می‌کنند، فقط `customMessage` متفاوت پاس می‌دهند — دقیقاً همان
دوگانگی «منطق مشترک + پیام‌های خاص هر Context» که دستور کار خواسته بود.

**بدون تغییر رفتار:** امضا/نوع بازگشتی `archiveProject`/`lockScene`
(`Result<Project>`/`Result<Scene>`، `IllegalStateException`) و متن دقیق
پیام‌های سفارشی هیچ‌کدام تغییر نکردند — فقط منبع تولید همان پیام از
یک `if` مستقیم به عبور از `validateStateTransition` جابه‌جا شد.
`ProjectRepository`/`SceneDetailViewModel` بدون هیچ تغییری کار
می‌کنند (تأیید با بازبینی کد + تست‌های موجود بدون تغییر سبز ماندند).

## تست‌ها

- `StateMachineTest.kt`: ۳ تست تازه برای `customMessage` (پیش‌فرض
  عمومی وقتی داده نشده؛ استفاده‌ی واقعی وقتی داده شده؛ نادیده‌گرفته‌شدن
  وقتی انتقال اصلاً مجاز است).
- `ProjectLifecycleTest.kt`: ۱ تست تازه — تأیید صریح که پیام شکست
  دقیقاً همان متن سفارشی قبلی است (نه پیام عمومی `validateStateTransition`).
  ۴ تست موجود بدون تغییر سبز ماندند.
- `SceneLifecycleTest.kt` (**تازه، این تابع تا این قدم هیچ تست مستقلی
  نداشت** — فقط غیرمستقیم از طریق UI/`SceneDetailViewModel`): ۵ تست —
  هر مسیر مجاز (DRAFT/REVIEW→LOCKED) و غیرمجاز (LOCKED/FINAL/ARCHIVED)،
  به‌علاوه‌ی تأیید صریح حفظ پیام سفارشی.
- تأیید صریح (`grep -rn "canTransition" app/src/main/`): `canTransition`
  دیگر مستقیماً در هیچ فایل Lifecycle فراخوانی نمی‌شود، فقط داخل
  `validateStateTransition`.

## Skills استفاده‌شده

هیچ Skill نصب‌شده‌ای در این قدم فراخوانی نشد.
