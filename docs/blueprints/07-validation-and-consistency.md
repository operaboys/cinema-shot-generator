# واحد ۰۷: اعتبارسنجی و سازگاری (Validation & Consistency Engine)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Validation Engine + Logic Conflict Checker + Dependency Resolver
**وضعیت:** فعال

هر سه بخش این واحد انواعی از «قانون‌سنجی» هستند — با severity های مشترک (Blocking/Warning) و هدف مشترک: جلوگیری از تولید پرامپت ناقص یا متناقض.

---

## بخش الف — Validation Engine (اعتبارسنجی داده)

### سه سطح بررسی

```
Level 1: کامل بودن داده (فیلد الزامی، نوع داده، محدوده‌ی مقدار)
Level 2: سازگاری منطقی (تضاد بین تنظیمات مختلف Shot)
Level 3: کیفیت (Seed Consistency، Weighted Tags Balance)
```

### دو سطح Severity (نه سه)

**Hint در بلوپرینت اصلی صرفاً زیرمجموعه‌ی نرم‌تر Warning است، نه یک سطح مستقل.** در عمل فقط دو رفتار واقعی وجود دارد:

- **Blocking:** فرآیند متوقف می‌شود؛ کاربر باید رفع کند تا ادامه دهد.
- **Warning (شامل Hint):** نمایش داده می‌شود، کاربر می‌تواند نادیده بگیرد و ادامه دهد.

```kotlin
enum class Severity { BLOCKING, WARNING }

data class ValidationIssue(
    val severity: Severity,
    val field: String? = null,
    val message: String,
    val suggestion: String? = null
)

data class ValidationReport(
    val targetId: String,
    val blockingErrors: List<ValidationIssue>,
    val warnings: List<ValidationIssue>  // Hint های قدیمی هم اینجا قرار می‌گیرند
) {
    val isValid: Boolean get() = blockingErrors.isEmpty()
}
```

### قوانین کلیدی (نمونه‌ها)

| Rule | شرح | Severity |
|---|---|---|
| فیلد الزامی خالی (مثل `shot_description`) | داده‌ی ساختاری ناقص | **Blocking** |
| نوع داده نامعتبر یا خارج از محدوده‌ی مجاز | داده‌ی ساختاری نامعتبر | **Blocking** |
| باران + نور آتش + فضای بیرونی | ناسازگاری منطقی محیط/نور | **Warning** |
| لنز Ultra-wide + Close-up | اعوجاج چهره (فنی، نه ساختاری) | **Warning** |
| Weighted Tags همه بالای ۱.۵ | تأکید روی همه یعنی تأکید روی هیچ | **Warning** |
| Shot بدون هیچ Subject | ممکن است خالی به نظر برسد (مجاز برای Landscape) | **Warning** |
| Beat timestamp خارج از محدوده‌ی Duration شات | داده‌ی زمان‌بندی نامعتبر | **Blocking** |
| فایل `image_references[].local_file_path` موجود نیست | رفرنس شکسته | **Blocking** |
| Character Continuity Lock نقض شده | طبق واحد ۰۶ (Hard Lock) | **Blocking** |
| Seed شات با baseline صحنه ناهمخوان | صرفاً پیشنهاد کیفی | **Warning** |

**نکته مهم:** بررسی محدودیت طول/توکن پرامپت **در این واحد انجام نمی‌شود** — چون در این مرحله هنوز مدل هدف مشخص نیست (طبق معماری PromptBlueprint خنثی). این بررسی در Prompt Finalization Pipeline (واحد ۱۳)، بعد از Rendering برای یک مدل خاص، انجام می‌شود.

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
fun validateDataCompleteness(shot: Shot): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    if (shot.shotDescription.length < 10) {
        issues += ValidationIssue(Severity.BLOCKING, "shot_description", "توضیح شات الزامی است (حداقل ۱۰ کاراکتر)")
    }
    if (shot.characterIds.isEmpty() && shot.objectIds.isEmpty()) {
        issues += ValidationIssue(Severity.WARNING, "subjects", "شات بدون سوژه ممکن است خالی به نظر برسد")
    }
    return issues
}

fun validateLogicConsistency(shot: Shot, environment: EnvironmentSettings, lighting: LightingSettings): List<ValidationIssue> {
    val issues = mutableListOf<ValidationIssue>()
    if (environment.weather == "rain" && lighting.motivation == "fire" && environment.locationType == "outdoor") {
        issues += ValidationIssue(
            Severity.WARNING,
            message = "آتش در باران بیرون از ساختمان غیرمنطقی است",
            suggestion = "Location را به Indoor تغییر دهید یا Lighting Motivation را عوض کنید"
        )
    }
    return issues
}

fun validateBeatSheetTimeline(beats: List<Beat>, durationSeconds: Float): List<ValidationIssue> {
    return beats.filter { it.timestampSeconds < 0f || it.timestampSeconds > durationSeconds }
        .map {
            ValidationIssue(
                Severity.BLOCKING,
                message = "Beat timestamp ${it.timestampSeconds} خارج از محدوده‌ی شات (0-$durationSeconds) است"
            )
        }
}
```

---

## بخش ب — Logic Conflict Checker (تشخیص تناقض کارگردانی)

بررسی ترکیبات غیرمنطقی یا غیرحرفه‌ای بین تنظیمات مختلف یک Shot — مکمل Level 2 بالا، با تمرکز روی تضادهای سینمایی/کارگردانی (نه فقط داده‌ای):

```kotlin
/** حرکت سریع در حالت Long-take (که ذاتاً آرام است) ناسازگار است. */
fun checkFastMotionLongTake(motionLevel: MotionLevel, cinematicMode: CinematicMode): ValidationIssue? {
    val isFast = motionLevel in listOf(MotionLevel.DYNAMIC, MotionLevel.EXTREME)
    if (isFast && cinematicMode == CinematicMode.LONG_TAKE) {
        return ValidationIssue(
            Severity.WARNING,
            message = "حرکت سریع با حالت Long-take ناسازگار است",
            suggestion = "Cinematic Language را به Fast-cut تغییر دهید یا Motion Level را کاهش دهید"
        )
    }
    return null
}

/** دوربین ثابت در یک صحنه‌ی تعقیب، انرژی لازم را ندارد. */
fun checkStaticCameraInChase(cameraMovementType: String, shotDescription: String): ValidationIssue? {
    val isChase = listOf("chase", "running", "pursuit").any { shotDescription.contains(it, ignoreCase = true) }
    if (cameraMovementType == "static" && isChase) {
        return ValidationIssue(
            Severity.WARNING,
            message = "صحنه‌ی تعقیب با دوربین ثابت انرژی لازم را ندارد",
            suggestion = "از Tracking Shot یا Handheld استفاده کنید"
        )
    }
    return null
}
```

---

## بخش ج — Dependency Resolver (مدیریت وابستگی)

### سه نوع وابستگی

| نوع | تعریف | مثال | واکنش |
|---|---|---|---|
| **Strong** | تغییر Source حتماً Target را تحت‌تأثیر می‌گذارد | DNA → Scene، Scene → Shot، Character → Shot های استفاده‌کننده | **Invalidate** (نیازمند بازبینی، نه بلاک) |
| **Weak** | تغییر Source ممکن است Target را تحت‌تأثیر بگذارد | Style Reference → Shot | **Warn** |
| **Reference** | Target ارجاع می‌دهد اما تغییر معمولاً بی‌اهمیت است | Shot → Object (پس‌زمینه) | **Notify** |

### نکته‌ی مهم: Invalidate ≠ Block

«Invalidate» یعنی «این Entity نیازمند بازبینی/بازتولید است»، نه این‌که کاربر از تغییر منع شود. این کاملاً با Soft Lock در DNA Manager سازگار است — تغییر DNA همیشه مجاز است (فقط هشدار)، و Shot های وابسته به‌عنوان «نیازمند بازبینی» علامت می‌خورند.

**تنها استثنا:** Character Appearance/Identity/Age Lock — طبق واحد ۰۶، این‌ها Hard Lock هستند و واقعاً **Block** می‌کنند، نه فقط Invalidate.

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
enum class DependencyType { STRONG, WEAK, REFERENCE }

data class DependencyEdge(val sourceId: String, val targetId: String, val type: DependencyType)

data class ImpactReport(
    val directlyAffected: List<String>,
    val transitivelyAffected: List<String>,
    val toInvalidate: List<String>,
    val toWarn: List<String>
)

/** تحلیل اثر یک تغییر با پیمایش BFS روی گراف وابستگی. */
fun analyzeImpact(changedNodeId: String, edges: List<DependencyEdge>): ImpactReport {
    val direct = edges.filter { it.sourceId == changedNodeId }
    val toInvalidate = direct.filter { it.type == DependencyType.STRONG }.map { it.targetId }
    val toWarn = direct.filter { it.type == DependencyType.WEAK }.map { it.targetId }
    return ImpactReport(
        directlyAffected = direct.map { it.targetId },
        transitivelyAffected = emptyList(),
        toInvalidate = toInvalidate,
        toWarn = toWarn
    )
}

/** حذف یک Asset در حال استفاده مجاز نیست. */
fun canDeleteAsset(assetId: String, usageCount: Int): Result<Unit> {
    if (usageCount > 0) {
        return Result.failure(IllegalStateException("این Asset در $usageCount شات استفاده شده است"))
    }
    return Result.success(Unit)
}

/** تشخیص وابستگی دایره‌ای قبل از افزودن یک Edge جدید. */
fun wouldCreateCycle(source: String, target: String, existingEdges: List<DependencyEdge>): Boolean {
    val visited = mutableSetOf<String>()
    fun dfs(node: String): Boolean {
        if (node == source) return true
        if (!visited.add(node)) return false
        return existingEdges.filter { it.sourceId == node }.any { dfs(it.targetId) }
    }
    return dfs(target)
}
```

### روابط اصلی وابستگی در این پروژه

```
DNA Manager (Strong) → Scene → (Strong) → Shot → (Strong) → PromptBlueprint
Character Asset (Strong) → Shot های استفاده‌کننده از آن کاراکتر
Visual Identity (Weak) → Shot
```

---

## معیارهای موفقیت

- گراف وابستگی همیشه بدون Circular Dependency است.
- تحلیل اثر (Impact Analysis) سریع و کامل انجام می‌شود.
- Asset در حال استفاده قابل حذف نیست.
- Invalidate هرگز به معنای بلاک نیست، مگر برای Character Continuity Lock.
- خطاهای Blocking واقعاً جلوی تولید Prompt را می‌گیرند؛ Warning ها فقط اطلاع‌رسانی می‌کنند.
