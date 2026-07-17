# واحد ۰۱: داستان و Override انسانی (Story & Override)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Story Wizard + Human Override
**وضعیت:** فعال

---

## بخش الف — Story Wizard

### تعریف
نقطه‌ی ورود کاربر به سیستم. بدون نیاز به دانش فنی، چارچوب مفهومی و احساسی پروژه را از طریق یک فرآیند گام‌به‌گام می‌سازد.

### مسئولیت
ایجاد `StoryContext` که حاوی تمام اطلاعات مفهومی پروژه است؛ راهنمایی کاربر با پیشنهادات هوشمند؛ اعتبارسنجی ورودی؛ انتقال داده به DNA Manager.

### مراحل ویزارد

| مرحله | سؤال | گزینه‌ها | الزامی |
|---|---|---|---|
| ۱. Story Type | پروژه چه نوع داستانی دارد؟ | Narrative، Conceptual، Visual-only، Experimental | بله |
| ۲. Genre | ژانر پروژه چیست؟ | Action، Drama، Sci-Fi، Fantasy، Horror، Romance، Documentary، Experimental، Hybrid (۲-۳ زیرژانر) | بله |
| ۳. Mood | حال‌وهوای کلی چیست؟ | Calm، Dark، Epic، Emotional، Mysterious، Tense، Hopeful، Melancholic (تا ۲ مورد: اصلی + فرعی) | بله |
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
  "mood": {
    "primary": "Dark",
    "secondary": "Mysterious"
  },
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
- **Rule 2 (Blocking):** اگر Story Type = Documentary، ژانر باید محدود به Documentary/Drama باشد.
- **Rule 3 (Warning، غیرمسدودکننده):** اگر Genre = Horror و Mood.primary = Hopeful، هشدار «ترکیب غیرمعمول» نمایش داده می‌شود؛ ادامه مجاز است.
- **Rule 4:** اگر همه‌ی فیلدهای الزامی پر شده باشند، `completion_status = "complete"`، در غیر این صورت `"partial"`.

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class StoryType { NARRATIVE, CONCEPTUAL, VISUAL_ONLY, EXPERIMENTAL }
enum class Genre { ACTION, DRAMA, SCI_FI, FANTASY, HORROR, ROMANCE, DOCUMENTARY, EXPERIMENTAL }
enum class Mood { CALM, DARK, EPIC, EMOTIONAL, MYSTERIOUS, TENSE, HOPEFUL, MELANCHOLIC }
enum class NarrativeIntensity { MINIMAL, MODERATE, HIGH, EXTREME }
enum class VisualIntent { REALISTIC, CINEMATIC, STYLIZED, ARTISTIC, ABSTRACT }

data class StoryContext(
    val storyType: StoryType,
    val genre: List<Genre>,
    val moodPrimary: Mood,
    val moodSecondary: Mood? = null,
    val narrativeIntensity: NarrativeIntensity = NarrativeIntensity.MODERATE,
    val visualIntent: VisualIntent = VisualIntent.CINEMATIC,
    val createdAt: String,
    val completionStatus: CompletionStatus
)

enum class CompletionStatus { COMPLETE, PARTIAL }

/**
 * بررسی ترکیبات غیرمعمول Genre+Mood — نتیجه فقط Warning است، هرگز Blocking.
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

fun validateStoryContext(context: StoryContext): ValidationResult {
    // فیلدهای الزامی در خودِ نوع داده (non-null) تضمین شده‌اند؛
    // اینجا فقط قوانین Cross-field (مثل محدودیت Documentary) بررسی می‌شود.
    if (context.storyType == StoryType.DOCUMENTARY_RESTRICTED_CHECK) {
        // نمونه‌ی مفهومی؛ منطق واقعی بر اساس Rule 2 پیاده می‌شود
    }
    return ValidationResult(valid = true, errors = emptyList())
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

خروجی Story Wizard (`StoryContext`) مستقیماً به واحد ۰۲ (DNA Manager) داده می‌شود تا DNA پیش‌فرض پروژه از روی آن ساخته شود. Human Override یک لایه‌ی عرضی (Cross-cutting) است که در تمام واحدهای دیگر (DNA، Scene، Shot، Camera و...) قابل استفاده است — تعریفش اینجاست، ولی کاربردش سراسری است.

---

## معیارهای موفقیت

- StoryContext معتبر و کامل تولید می‌شود.
- خطای Validation کمتر از حد قابل‌قبول (کیفیت راهنمایی، نه سرعت).
- تمام Override ها Scope خودکار ثبت‌شده دارند.
- قوانین Blocking هرگز قابل Override نیستند.
- Revoke بدون خطا و با بازگردانی کامل مقادیر اصلی کار می‌کند.
