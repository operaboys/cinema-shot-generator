# واحد ۱۶: گردش کار کاربر (User Workflow)

**نقش:** Blueprint — نقشه‌راه کامل تعامل کاربر با سیستم (نه یک ماژول اجرایی مستقل)
**وضعیت:** فعال
**وابستگی:** تمام واحدهای دیگر (این سند نحوه‌ی به‌کارگیری آن‌ها را در توالی درست نشان می‌دهد)

---

## توالی ۸ مرحله‌ای

```
شروع
  ↓
۱. Story Wizard (واحد ۰۱)         → نطفه‌ی مفهومی پروژه
  ↓
۲. DNA Configuration (واحد ۰۲)    → قوانین سراسری پروژه
  ↓
۳. Asset Library (واحد ۰۶)        → کاراکترها، مکان‌ها، اشیا
  ↓
۴. Scene Creation (واحد ۰۴)       → ساختار صحنه
  ↓
۵. Shot Creation (واحد ۰۵)        → جزئیات اجرایی
  ↓
۶. Validation (واحد ۰۷)           → کنترل کیفیت
  ↓
۷. Prompt Generation (واحد ۱۱)    → تولید PromptBlueprint خنثی
  ↓
۸. Output Delivery (واحد ۱۴)      → انتخاب مدل، Render، تحویل نهایی
  ↓
پایان (ذخیره و Export)
```

---

## جزئیات هر مرحله

### مرحله ۱: Story Wizard
**ورودی:** هیچ (شروع پروژه‌ی جدید) | **خروجی:** `StoryContext` معتبر
**اقدام کاربر:** پاسخ به ۵ سؤال (Story Type، Genre، Mood، Narrative Intensity، Visual Intent)
**مسیر جایگزین:** Quick Mode (فرم تک‌صفحه‌ای برای کاربر باتجربه)

### مرحله ۲: DNA Configuration
**ورودی:** `StoryContext` | **خروجی:** `ProjectDna` با Core Identity در حالت Soft Lock
**اقدام کاربر:** بررسی/اصلاح DNA پیشنهادی سیستم
**نکته:** فعال‌سازی «قفل» یعنی Soft Lock (هشدار در تغییر بعدی)، نه بلاک کامل

### مرحله ۳: Asset Library
**ورودی:** `ProjectDna` | **خروجی:** کاراکترها/مکان‌ها/اشیا با Continuity Rules
**اقدام کاربر:** تعریف کاراکترهای اصلی (ظاهر، Outfit، Continuity Lock)، مکان‌ها، اشیا

### مرحله ۴: Scene Creation
**ورودی:** Asset Library | **خروجی:** Scene با تنظیمات سراسری (Location، Time، Atmosphere)

### مرحله ۵: Shot Creation
**ورودی:** Scene | **خروجی:** Shot با تمام جزئیات (Camera، Lighting، Beat Sheet، Sound Profile)

### مرحله ۶: Validation
**ورودی:** Shot(s) | **خروجی:** گزارش Validation (Blocking Errors / Warnings)
**قانون:** پیشروی فقط وقتی مجاز است که هیچ Blocking Error باقی نمانده (یا با Override رفع شده)

⚠️ **نکته‌ی تکمیلی (از سند اصلی، جا افتاده بود در نسخه‌ی قبلی):** این مرحله فقط یک بار اجرا نمی‌شود — Validation در دو نقطه‌ی جدا از پایپ‌لاین رخ می‌دهد:
- **Pre-Prompt Validation:** همین‌جا، روی داده‌ی ساختاریافته (DNA، Scene، Shot) قبل از تولید Prompt.
- **Post-Prompt Validation:** بعد از Prompt Finalization (واحد ۱۳)، روی متن نهایی رندرشده — بررسی می‌کند که تمیزکاری/فشرده‌سازی معنای اصلی را از بین نبرده باشد.

### مرحله ۷: Prompt Generation
**ورودی:** Shot معتبرشده | **خروجی:** `PromptBlueprint` خنثی (بدون انتخاب مدل هدف در این مرحله)
**نکته‌ی مهم:** انتخاب Target Model **در این مرحله نیست** — Prompt Engineering Core باید کاملاً کور نسبت به مدل بماند.

### مرحله ۸: Output Delivery
**ورودی:** `PromptBlueprint` | **خروجی:** یک یا چند `RenderedOutput` + هر دو نسخه‌ی زبانی
**اقدام کاربر:** انتخاب مدل هدف (یا چند مدل)، بررسی خروجی، Copy/Export

---

## پیاده‌سازی مفهومی وضعیت گردش کار (Kotlin)

```kotlin
enum class WorkflowStep {
    STORY_WIZARD, DNA_CONFIG, ASSET_LIBRARY, SCENE_CREATION,
    SHOT_CREATION, VALIDATION, PROMPT_GENERATION, OUTPUT_DELIVERY
}

enum class StepStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

data class WorkflowState(
    val sessionId: String,
    val projectId: String,
    val currentStep: WorkflowStep,
    val stepStatus: Map<WorkflowStep, StepStatus>,
    val startedAt: String,
    val lastActionAt: String
) {
    val progressPercentage: Int
        get() = (stepStatus.values.count { it == StepStatus.COMPLETED } * 100) / WorkflowStep.entries.size
}

/** پیشروی به مرحله‌ی بعد فقط با هشدار مجاز است اگر مراحل قبلی کامل نباشند (نه Blocking). */
fun canJumpToStep(state: WorkflowState, target: WorkflowStep): Pair<Boolean, String?> {
    val previousSteps = WorkflowStep.entries.filter { it.ordinal < target.ordinal }
    val allCompleted = previousSteps.all { state.stepStatus[it] == StepStatus.COMPLETED }
    return if (allCompleted) true to null
    else true to "برخی مراحل قبلی کامل نشده‌اند — ادامه با احتیاط"
}
```

---

## قوانین گردش کار

| Rule | شرح | Severity |
|---|---|---|
| پرش به مرحله‌ای که پیش‌نیازش کامل نیست | فقط هشدار، نه بلاک | **Warning** |
| وجود Blocking Error در Validation | جلوی پیشروی به Prompt Generation را می‌گیرد | **Blocking** |
| ذخیره‌ی خودکار بعد از تکمیل مراحل کلیدی (DNA، Asset، Scene) | Auto-Save trigger | — |

---

## مسیرهای جایگزین

- **Express Workflow:** Story Wizard (Skip) → DNA (Template) → Asset (Import) → Scene (Bulk Create) → Shot (Preset) → Validate (Auto-Fix) → Generate
- **Iterative Workflow:** ساخت یک Scene → یک Shot → تولید → بازبینی → اصلاح → افزودن Shot بیشتر → تکرار

⚠️ «Collaborative Workflow» (چند کاربر همزمان) از دامنه‌ی فعلی حذف شده — اپ تک‌کاربره و بدون سرور است.

---

## معیارهای موفقیت (بدون KPI عددی سرعت‌محور)

✅ کیفیت پرامپت نهایی، معیار اصلی موفقیت است — نه سرعت رسیدن به آن
✅ هیچ مرحله‌ای گیج‌کننده نیست؛ Entry/Exit Condition هر مرحله شفاف است
✅ Validation خطاهای رایج را قبل از تولید Prompt تشخیص می‌دهد
✅ Auto-Save از دست‌رفتن داده را در تمام مراحل کلیدی جلوگیری می‌کند

⚠️ معیارهای قدیمی «کمتر از ۲۰ دقیقه» و «نرخ ترک کاربر» (Abandonment Rate) به‌طور کامل حذف شدند — اولی چون فلسفه‌ی پروژه «کیفیت مهم‌تر از سرعت» است، دومی چون به تحلیل رفتار جمعیتی چند کاربر وابسته است و برای اپ تک‌کاربره بی‌معناست.

---

## سنجش کیفیت (Quality Rubric) — ابزار عملی برای «کیفیت مهم‌تر از سرعت»

طبق سند اصلی KPI پروژه، صرف گفتنِ «کیفیت مهم است» بدون یک ابزار قابل‌اندازه‌گیری، عملاً غیرقابل پیگیری است. این Rubric (نه یک KPI سرعت‌محور، بلکه ابزاری برای خودارزیابی کیفیت خروجی) می‌تواند به‌صورت اختیاری، هم توسط کاربر و هم به‌عنوان یک قابلیت داخلی اپ (خودارزیابی Prompt قبل از نمایش نهایی) استفاده شود.

### پنج محور سنجش (هرکدام ۰ تا ۲۰، جمعاً از ۱۰۰)

| محور | سؤال کلیدی |
|---|---|
| **وضوح سوژه** | آیا سوژه‌ی اصلی Shot واضح و قابل‌تصویرسازی است، بدون ابهام؟ |
| **وضوح سینمایی** | آیا پارامترهای دوربین (زاویه، فاصله، حرکت) مشخص و قابل‌اجرا هستند؟ |
| **دقت بصری** | آیا جزئیات بصری (نور، محیط) کافی و شفاف‌اند؟ |
| **انسجام سبکی** | آیا سبک با ژانر/Mood پروژه سازگار است؟ |
| **ایجاز پرامپت** | آیا پرامپت بدون تکرار زائد و به‌اندازه‌ی کافی فشرده است؟ |

```kotlin
data class QualityScore(
    val subjectClarity: Int,      // 0-20
    val cinematicClarity: Int,    // 0-20
    val visualSpecificity: Int,   // 0-20
    val styleCoherence: Int,      // 0-20
    val conciseness: Int          // 0-20
) {
    val total: Int get() = subjectClarity + cinematicClarity + visualSpecificity + styleCoherence + conciseness
}

/**
 * این تابع یک ابزار کمکی/اختیاری است — نه بخشی الزامی از Validation
 * (که در واحد ۰۷ تعریف شده). هدف: کمک به کاربر برای دیدن نقاط ضعف
 * احتمالی پرامپت قبل از ارسال به مدل AI، نه بلاک‌کردن جریان کار.
 */
fun evaluatePromptQuality(renderedOutput: RenderedOutput, blueprint: PromptBlueprint): QualityScore {
    // پیاده‌سازی واقعی بر اساس بررسی وجود/کیفیت هر بخش از structuredParts
    // نمونه‌ی مفهومی؛ جزئیات دقیق الگوریتم در زمان پیاده‌سازی مشخص می‌شود
    TODO("پیاده‌سازی واقعی در فاز کدنویسی")
}
```

**نکته‌ی مهم — تفاوت با KPI قدیمی:** بر خلاف نسخه‌ی اولیه‌ی این Rubric (که بخشی از یک سیستم Test Dataset ثابت با ۲۰ ورودی از پیش تعریف‌شده برای مقایسه‌ی نسخه‌های مختلف موتور بود)، در این معماری این ابزار صرفاً برای **بازخورد لحظه‌ای به خودِ کاربر** استفاده می‌شود — نه برای مقایسه‌ی رسمی Sprint به Sprint یا معیار پذیرش یک نسخه. اگر در آینده نیاز به تست رگرسیون کیفیت بین نسخه‌های مختلف کد احساس شد، می‌توان یک Test Dataset مشابه (متناسب با ۱۸ واحد فعلی) طراحی کرد.

### اصل ضدفریب (Anti-Gaming Principle)

هرگونه بهینه‌سازی (مثلاً کوتاه‌سازی برای کاهش Token در واحد ۱۳) که باعث افت این Quality Score شود، یک بهینه‌سازی نامعتبر است:

```
❌ نادرست: "astronaut space dark" (کوتاه ولی فاقد جزئیات)
✅ درست: "lone astronaut in dark, atmospheric space" (کوتاه‌تر شده، ولی بدون از‌دست‌رفتن معنا)
```

این اصل مستقیماً مکمل قانون Token Optimization در واحد ۱۳ (Prompt Finalization) است — فشرده‌سازی هرگز نباید به قیمت افت کیفیت باشد.
