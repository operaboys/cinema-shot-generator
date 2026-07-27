# واحد ۰۱: داستان و Override انسانی (Story & Override)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Story Wizard + Human Override
**وضعیت:** فعال

**نسخه:** ۳ — بازنویسی برای رفع یک تناقض نامی واقعی: `enum Mood` محلی این بلوپرینت (۸ مقدار قدیمی) با `enum Mood` سراسری در `02-dna-manager-v2.md` (نسخه‌ی ۳، ۲۵+ مقدار در ۵ دسته) هم‌نام بود اما مقادیر متفاوت داشت — دو تعریف مستقل برای یک اسم. این نسخه `Mood` محلی را حذف و این بلوپرینت را به enum سراسری بلوپرینت ۰۲ ارجاع می‌دهد. همچنین جایگاه این بلوپرینت در توالی گردش کار (اکنون ۹ مرحله‌ای، نه ۸) اصلاح شد.

**نسخه:** ۴ — بازنویسی برای رفع یک ناسازگاری جزئی کشف‌شده در بازبینی معماری مستقل: JSON نمونه‌ی `StoryContext` هنوز فرمت تودرتوی قدیمی (`"mood": {"primary": ..., "secondary": ...}`) را نشان می‌داد، در حالی که `data class StoryContext` واقعی (طبق Migration نسخه‌ی ۳) دو فیلد مسطح و مستقل `moodPrimary`/`moodSecondary` دارد. JSON نمونه اکنون با ساختار Kotlin واقعی هماهنگ شد (`mood_primary`/`mood_secondary`، بدون تودرتو). این یک Migration آگاهانه بود، نه یک تناقض معماری، اما در یک نگاه گذرا ناسازگار به‌نظر می‌رسید — این نسخه آن ابهام را رفع می‌کند. **تغییرات با «🆕v4» علامت‌گذاری شده‌اند.**

---

## بخش الف — Story Wizard

### تعریف
نقطه‌ی ورود کاربر به سیستم. بدون نیاز به دانش فنی، چارچوب مفهومی و احساسی پروژه را از طریق یک فرآیند گام‌به‌گام می‌سازد.

### مسئولیت
ایجاد `StoryContext` که حاوی تمام اطلاعات مفهومی پروژه است؛ راهنمایی کاربر با پیشنهادات هوشمند؛ اعتبارسنجی ورودی.

🆕 **مقصد `StoryContext` (اصلاح مهم):** این خروجی مستقیماً به DNA Manager (واحد ۰۲) **نمی‌رود**. مسیر واقعی: `StoryContext` به همراه یک داستان کامل و آزاد که کاربر می‌نویسد، به واحد **۰۱ب (AI Story Breakdown + JSON Doctor)** سپرده می‌شود — آنجا این چارچوب مفهومی، همراه با متن داستان، به یک AI بیرونی (یا از طریق کپی/پیست دستی، یا از طریق یک AI Connector Profile مستقیم) سپرده می‌شود تا کاراکترها، مکان‌ها، و فهرست شات واقعی ساخته شوند. تنها بعد از تکمیل واحد ۰۱ب (که خروجی کاملی از Asset/Scene/Shot تولید می‌کند)، جریان به DNA Manager و مراحل بعدی گردش کار (واحد ۱۶) می‌رسد. این بلوپرینت (۰۱) فقط مسئول ساخت `StoryContext` است، نه تولید داستان یا Scene/Shot واقعی — آن مسئولیت به‌طور کامل به واحد ۰۱ب تعلق دارد.

### مراحل ویزارد

| مرحله | سؤال | گزینه‌ها | الزامی |
|---|---|---|---|
| ۱. Story Type | پروژه چه نوع داستانی دارد؟ | Narrative، Conceptual، Visual-only، Experimental | بله |
| ۲. Genre | ژانر پروژه چیست؟ | Action، Drama، Sci-Fi، Fantasy، Horror، Romance، Documentary، Experimental، Hybrid (۲-۳ زیرژانر) | بله |
| ۳. Mood | حال‌وهوای کلی چیست؟ | 🆕v3 از `enum Mood` سراسری (بلوپرینت ۰۲ نسخه ۳، ۲۵+ گزینه در ۵ دسته: High Energy/Positive/Emotional/Dark/Calm) — همان enum که `GlobalMoodBase.primaryEmotion` در بلوپرینت ۰۲ استفاده می‌کند (تا ۲ مورد: اصلی + فرعی) | بله |
| ۴. Narrative Intensity | شدت روایت چقدر باشد؟ | Minimal، Moderate (پیش‌فرض)، High، Extreme | بله |
| ۵. Visual Intent | هدف بصری چیست؟ | Realistic، Cinematic (پیش‌فرض)، Stylized، Artistic، Abstract | بله |

**پیشنهادهای هوشمند (غیرالزامی، فقط راهنما):**
- اگر Story Type = Experimental → پیشنهاد ژانرهای غیرمتعارف
- اگر Story Type = Documentary → محدود کردن گزینه‌ها به واقع‌گرایانه
- اگر Genre = Horror → پیشنهاد Mood: Dark, Tense, Mysterious
- اگر Genre = Romance → پیشنهاد Mood: Emotional, Hopeful, Calm

### ساختار خروجی: StoryContext

```json
{
  "story_type": "Narrative",
  "genre": ["Sci-Fi", "Drama"],
  "mood_primary": "Dark",
  "mood_secondary": "Mysterious",
  "narrative_intensity": "High",
  "visual_intent": "Cinematic",
  "metadata": {
    "created_at": "2026-01-26T10:30:00Z",
    "completion_status": "complete"
  }
}
```

### قوانین اعتبارسنجی

- **Rule 1 (Blocking):** اگر فیلد الزامی خالی باشد، عبور به مرحله‌ی بعد مسدود می‌شود.
- **Rule 2 (Blocking):** 🆕 اگر Genre شامل Documentary باشد، کل لیست ژانر باید زیرمجموعه‌ای از {Documentary, Drama} باشد (نه اینکه StoryType محدود شود — StoryType اصلاً مقدار Documentary ندارد).
- **Rule 3 (Warning، غیرمسدودکننده):** اگر Genre = Horror و Mood.primary = Hopeful، هشدار «ترکیب غیرمعمول» نمایش داده می‌شود؛ ادامه مجاز است.
- **Rule 4:** اگر همه‌ی فیلدهای الزامی پر شده باشند، `completion_status = "complete"`، در غیر این صورت `"partial"`.

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class StoryType { NARRATIVE, CONCEPTUAL, VISUAL_ONLY, EXPERIMENTAL }
enum class Genre { ACTION, DRAMA, SCI_FI, FANTASY, HORROR, ROMANCE, DOCUMENTARY, EXPERIMENTAL }
// 🆕v3 enum Mood محلی حذف شد — این بلوپرینت اکنون از Mood سراسری (import از domain.dna، بلوپرینت ۰۲) استفاده می‌کند.
// دلیل: enum محلی قبلی (۸ مقدار: Calm/Dark/Epic/Emotional/Mysterious/Tense/Hopeful/Melancholic) با enum
// سراسری DNA (۲۵+ مقدار در ۵ دسته) هم‌نام بود اما مقادیر متفاوت داشت — یک تناقض واقعی، نه صرفاً دو اسم شبیه.
enum class NarrativeIntensity { MINIMAL, MODERATE, HIGH, EXTREME }
enum class VisualIntent { REALISTIC, CINEMATIC, STYLIZED, ARTISTIC, ABSTRACT }

data class StoryContext(
    val storyType: StoryType,
    val genre: List<Genre>,
    val moodPrimary: Mood,          // 🆕v3 اکنون از domain.dna.Mood (بلوپرینت ۰۲) — نه enum محلی
    val moodSecondary: Mood? = null,
    val narrativeIntensity: NarrativeIntensity = NarrativeIntensity.MODERATE,
    val visualIntent: VisualIntent = VisualIntent.CINEMATIC,
    val createdAt: String,
    val completionStatus: CompletionStatus
)

enum class CompletionStatus { COMPLETE, PARTIAL }

/**
 * بررسی ترکیبات غیرمعمول Genre+Mood — نتیجه فقط Warning است، هرگز Blocking.
 * 🆕v3 mood اکنون از نوع domain.dna.Mood است؛ HOPEFUL و DARK هر دو در enum سراسری
 * جدید هم وجود دارند (به‌ترتیب در دسته‌های Positive و Dark)، پس این منطق بدون تغییر معنایی کار می‌کند.
 */
fun checkMoodGenreCompatibility(genre: Genre, mood: Mood): String? {
    return when {
        genre == Genre.HORROR && mood == Mood.HOPEFUL ->
            "ترکیب Horror + Hopeful غیرمعمول است — آیا مطمئن هستید؟"
        genre == Genre.ROMANCE && mood == Mood.DARK ->
            "ترکیب Romance + Dark غیرمعمول است (مگر Dark Romance مدنظر باشد)"
        else -> null
    }
}

/**
 * 🆕 اصلاح باگ: نسخه‌ی قبلی این تابع به یک مقدار enum غیرموجود
 * (StoryType.DOCUMENTARY_RESTRICTED_CHECK) ارجاع می‌داد. Rule 2 در واقع
 * درباره‌ی Genre است، نه StoryType — StoryType اصلاً مقدار Documentary ندارد
 * (فقط NARRATIVE/CONCEPTUAL/VISUAL_ONLY/EXPERIMENTAL)؛ Documentary یک مقدار Genre است.
 * این تفسیر توسط Claude Code در اولین اجرای واقعی این بلوپرینت کشف و تأیید شد (ADR-001).
 */
fun validateStoryContext(context: StoryContext): ValidationResult {
    // فیلدهای الزامی در خودِ نوع داده (non-null) تضمین شده‌اند؛
    // اینجا فقط قوانین Cross-field (مثل محدودیت Documentary) بررسی می‌شود.
    val errors = mutableListOf<String>()

    // Rule 2 (Blocking): اگر Genre شامل DOCUMENTARY باشد، کل لیست ژانر
    // باید زیرمجموعه‌ای از {DOCUMENTARY, DRAMA} باشد.
    if (Genre.DOCUMENTARY in context.genre) {
        val allowedWithDocumentary = setOf(Genre.DOCUMENTARY, Genre.DRAMA)
        if (!context.genre.all { it in allowedWithDocumentary }) {
            errors += "وقتی ژانر شامل Documentary است، فقط ترکیب با Drama مجاز است"
        }
    }

    return ValidationResult(valid = errors.isEmpty(), errors = errors)
}
```

### حالت Quick Mode
علاوه بر حالت گام‌به‌گام (Guided)، یک فرم تک‌صفحه‌ای با تمام فیلدها برای کاربران باتجربه ارائه می‌شود که همزمان Validate می‌شود.

---

## بخش ب — Human Override

### تعریف
لایه‌ای که امکان دخالت آگاهانه‌ی انسانی خارج از منطق سخت‌گیرانه‌ی سیستم را فراهم می‌کند. تصمیمات خلاقانه‌ی کاربر همیشه بر قوانین خودکار (غیرساختاری) اولویت دارد.

### مسئولیت
مکانیزم امن نقض موقت قوانین؛ ثبت خودکار Scope هر تغییر؛ حفظ تاریخچه؛ جلوگیری از Override روی قوانین Blocking.

### مدل مجوز: دو سطح (نه سلسله‌مراتب اولویت)

- **Blocking Rules:** غیرقابل Override (مثل Shot بدون Subject).
- **Warning Rules:** همیشه قابل Override با یک اقدام کاربر (مثل تضاد سبکی جزئی).

هیچ سطح اولویت جداگانه‌ای (Low/Medium/High) وجود ندارد — تصمیم فقط بر اساس severity خودِ قانون گرفته می‌شود.

### چهار نوع Override (دسته‌بندی موضوعی)

| نوع | هدف | مثال |
|---|---|---|
| Artistic | جلوه‌ی هنری خاص | Dutch Angle برای حس Unease |
| Narrative | نیاز روایی | تغییر موقت Continuity برای Flashback |
| Visual | تغییر فنی بصری | ترکیب نور غیرمعمول |
| Technical | حل مشکل سیستمی | تغییر محدودیت طول پرامپت |

### ساختار داده‌ی Override

```json
{
  "override_id": "ovr_001",
  "override_type": "artistic",
  "created_at": "2026-01-26T10:30:00Z",
  "active": true,
  "scope": {
    "entity_type": "shot",
    "entity_id": "shot_001",
    "field": "camera.lens_type",
    "original_value": "portrait",
    "override_value": "ultra_wide"
  },
  "justification": { "reason": null },
  "impact": { "warnings_suppressed": ["lens_distance_conflict"] },
  "metadata": {
    "usage_count": 1,
    "last_applied": "2026-01-26T10:35:00Z",
    "revoked": false,
    "revoked_at": null,
    "revoked_reason": null
  }
}
```

`justification.reason` اختیاری است. ثبت `scope` همیشه خودکار و الزامی است (بخشی از State & Versioning).

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class OverrideType { ARTISTIC, NARRATIVE, VISUAL, TECHNICAL }
enum class RuleSeverity { BLOCKING, WARNING }

data class OverrideScope(
    val entityType: String,
    val entityId: String,
    val field: String,
    val originalValue: String,
    val overrideValue: String
)

data class HumanOverride(
    val overrideId: String,
    val overrideType: OverrideType,
    val createdAt: String,
    val active: Boolean = true,
    val scope: OverrideScope,
    val reason: String? = null,   // اختیاری
    val usageCount: Int = 0,
    val lastApplied: String? = null,
    val revoked: Boolean = false,
    val revokedAt: String? = null,
    val revokedReason: String? = null
)

sealed class OverridePermission {
    data class Allowed(val reason: String) : OverridePermission()
    data class Denied(val reason: String) : OverridePermission()
}

/** فقط بر اساس severity خودِ قانون تصمیم می‌گیرد، نه سطح اولویت Override. */
fun checkOverridePermission(ruleSeverity: RuleSeverity): OverridePermission {
    return when (ruleSeverity) {
        RuleSeverity.BLOCKING -> OverridePermission.Denied(
            "این قانون ساختاری است؛ بدون آن پرامپت معنی ندارد"
        )
        RuleSeverity.WARNING -> OverridePermission.Allowed(
            "Warning rule — کاربر تصمیم‌گیرنده‌ی نهایی است"
        )
    }
}

/**
 * ایجاد و اعمال یک Override. اگر قانون هدف Blocking باشد، خطا می‌دهد.
 */
suspend fun createOverride(
    overrideType: OverrideType,
    scope: OverrideScope,
    targetRuleSeverity: RuleSeverity,
    reason: String? = null
): Result<HumanOverride> {
    val permission = checkOverridePermission(targetRuleSeverity)
    if (permission is OverridePermission.Denied) {
        return Result.failure(IllegalStateException(permission.reason))
    }

    val override = HumanOverride(
        overrideId = generateOverrideId(),
        overrideType = overrideType,
        createdAt = nowIso8601(),
        scope = scope,
        reason = reason
    )

    // ثبت خودکار Scope در State & Versioning (الزامی، جدا از reason)
    logOverrideEvent(type = "override_created", overrideId = override.overrideId, scope = scope)

    return Result.success(override)
}

/** Override هرگز حذف نمی‌شود، فقط Revoke (غیرفعال) می‌شود و مقادیر اصلی بازمی‌گردد. */
suspend fun revokeOverride(overrideId: String, reason: String?): Result<Unit> {
    // بازیابی rollback data، بازگرداندن original_value، علامت‌گذاری revoked=true
    logOverrideEvent(type = "override_revoked", overrideId = overrideId, scope = null)
    return Result.success(Unit)
}
```

### قوانین

- **Rule 1 (Blocking):** قوانین Blocking هرگز قابل Override نیستند.
- **Rule 2:** هر Override با Scope خودکار ثبت می‌شود؛ دلیل اختیاری است.
- **Rule 3 (Blocking):** Override قابل حذف نیست، فقط قابل Revoke.
- **Rule 4 (Warning):** اگر Scope در سطح کل پروژه باشد، هشدار «این Override روی کل پروژه تأثیر می‌گذارد» نمایش داده می‌شود.

### هشدارهای سیستم (جزئیات کامل در `docs/governance/override-policy.md`)
Continuity Break، DNA Violation، استفاده‌ی زیاد از Override در بازه‌ی کوتاه، Override طولانی‌مدت بدون بازبینی.

---

## یکپارچگی بین دو بخش

🆕 خروجی Story Wizard (`StoryContext`) به همراه داستان آزاد کاربر، به واحد **۰۱ب (AI Story Breakdown)** داده می‌شود (نه مستقیماً به DNA Manager — طبق اصلاح بالا در بخش «مسئولیت»)؛ فقط بعد از تکمیل آن واحد، جریان به DNA Manager (واحد ۰۲) و بقیه‌ی مراحل گردش کار (واحد ۱۶) می‌رسد. Human Override یک لایه‌ی عرضی (Cross-cutting) است که در تمام واحدهای دیگر (DNA، Scene، Shot، Camera و...) قابل استفاده است — تعریفش اینجاست، ولی کاربردش سراسری است.

---

## معیارهای موفقیت

- StoryContext معتبر و کامل تولید می‌شود.
- خطای Validation کمتر از حد قابل‌قبول (کیفیت راهنمایی، نه سرعت).
- تمام Override ها Scope خودکار ثبت‌شده دارند.
- قوانین Blocking هرگز قابل Override نیستند.
- Revoke بدون خطا و با بازگردانی کامل مقادیر اصلی کار می‌کند.

---

## یادداشت پیاده‌سازی (برای Claude Code، هنگام اجرای این بلوپرینت به‌روزشده)

این یک قدم **Migration مستندسازی** روی واحد ۰۱ موجود است (که در `domain/story/` از قبل پیاده‌سازی و تست شده) — نه یک تغییر منطقی واقعی در کد:

- 🆕 با grep بررسی کن که آیا کد واقعی `domain/story/StoryValidation.kt` از قبل منطق درست (بر مبنای Genre) را دارد یا نه — طبق ADR-001، احتمال زیاد کد **از قبل درست است** و فقط متن این بلوپرینت (که همین الان اصلاح شد) عقب مانده بود. اگر کد واقعی هم هنوز نسخه‌ی قدیمی/باگ‌دار را دارد (بعید، ولی چک کن، حدس نزن)، آن را هم مطابق این بلوپرینت اصلاح کن.
- هیچ فیلد یا تابع جدیدی در این قدم لازم نیست — این صرفاً هم‌راستاسازی مستندات با کد موجود و کد آینده (واحد ۰۱ب) است.
- اگر واحد ۰۱ب (`01b-ai-story-breakdown.md`) هنوز پیاده‌سازی نشده، همین اصلاح مستندسازی به‌تنهایی قابل اجرا و commit است؛ منتظر واحد ۰۱ب نمان.
- 🆕v3 **این Migration وابسته به بلوپرینت ۰۲ (نسخه ۳) است:** با grep بررسی کن که `domain/story/StoryModels.kt` (یا فایل مشابه) از قبل `enum Mood` محلی داشته یا از `domain.dna.Mood` استفاده می‌کرده. اگر `enum Mood` محلی در کد موجود بود، باید حذف شود و تمام ارجاعات (`StoryContext.moodPrimary`, `checkMoodGenreCompatibility`) به `import domain.dna.Mood` تغییر کنند — این باید **بعد از** اجرای Migration بلوپرینت ۰۲ (که خودِ enum سراسری `Mood` را می‌سازد) انجام شود، نه قبلش.
- 🆕v3 جایگاه این بلوپرینت در توالی گردش کار (واحد ۱۶) اکنون «۹ مرحله‌ای» است (نه ۸، طبق افزودن مرحله‌ی ۰۱ب) — این فقط یک اصلاح عددی در متن است، تأثیری بر منطق کد ندارد.
- 🆕v4 این نسخه فقط ساختار JSON نمونه را با `data class` واقعی هماهنگ کرد (فیلدهای مسطح `mood_primary`/`mood_secondary` به‌جای آبجکت تودرتوی `mood: {primary, secondary}`) — هیچ تغییر واقعی در کد Kotlin لازم نیست، چون `data class StoryContext` از قبل (طبق نسخه‌ی ۳) درست بود؛ فقط JSON نمونه‌ی نمایشی عقب مانده بود. اگر جایی از کد واقعی (مثلاً سریالایزر یا Parser JSON در واحد ۰۱ب) از فرمت تودرتوی قدیمی پیروی می‌کند، آن را با grep پیدا کن و به فرمت مسطح جدید هماهنگ کن.
