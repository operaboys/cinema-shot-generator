# واحد ۱۳: پایان‌بخشی پرامپت (Prompt Finalization Pipeline)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**ادغام از:** Prompt Cleaner + Token Cost Calculator
**وضعیت:** فعال
**وابستگی:** Output Delivery System (واحد ۱۴) — این واحد **بعد از Rendering** اجرا می‌شود

---

## جایگاه در پایپ‌لاین

```
PromptBlueprint (خنثی، از واحد ۱۱)
        ↓
Renderer (واحد ۱۴) → متن خام مخصوص یک مدل خاص
        ↓
Prompt Cleaner (این واحد، بخش الف) → متن پاکسازی‌شده
        ↓
Token Cost Calculator (این واحد، بخش ب) → تخمین توکن + هشدار محدودیت
```

**نکته مهم:** این واحد آخرین مرحله‌ی داخل Output Delivery System است، نه یک مرحله‌ی جدا بین Prompt Engineering Core و خروجی نهایی. در این مرحله، برخلاف واحد ۱۱، دانستن `target_model` کاملاً معنادار است — چون متن قبلاً برای یک مدل مشخص Render شده.

---

## بخش الف — Prompt Cleaner (تمیزکاری)

### پنج فاز پردازش

```
Phase 1: Conflict Detection      (تشخیص تضاد کلمات: sunny+rainy)
Phase 2: Redundancy Removal      (حذف مترادف‌های تکراری: beautiful+pretty+gorgeous)
Phase 3: Stop Words Filtering    (حذف کلمات پرکننده: very very, a, that)
Phase 4: Token Optimization      (فشرده‌سازی جمله بدون از دست دادن معنی)
Phase 5: Final Polish            (Grammar، حروف بزرگ، نقطه‌گذاری)
```

### پیاده‌سازی مفهومی (Kotlin)

```kotlin
data class CleaningReport(
    val conflictsDetected: Int,
    val conflictsResolved: List<String>,
    val redundancyRemoved: Int,
    val stopWordsRemoved: Int,
    val originalLength: Int,
    val cleanedLength: Int,
    val compressionRatio: Float
)

data class CleaningOptions(
    val detectConflicts: Boolean = true,
    val removeRedundancy: Boolean = true,
    val filterStopWords: Boolean = true,
    val optimizeTokens: Boolean = true,
    val aggressiveMode: Boolean = false
)

val SYNONYM_GROUPS = listOf(
    listOf("beautiful", "pretty", "gorgeous", "stunning"),
    listOf("big", "large", "huge", "massive"),
    listOf("fast", "quick", "rapid", "swift")
)

val WEATHER_CONFLICTS = listOf("sunny" to "rainy", "clear" to "stormy")
val LOGICAL_CONTRADICTIONS = listOf(
    "standing" to "sitting", "open" to "closed", "moving" to "static", "alive" to "dead"
)

/** تشخیص تضادهای منطقی/آب‌وهوایی در متن رندرشده. */
fun detectConflicts(text: String): List<Pair<String, String>> {
    val found = mutableListOf<Pair<String, String>>()
    for ((a, b) in WEATHER_CONFLICTS + LOGICAL_CONTRADICTIONS) {
        if (text.contains(a, ignoreCase = true) && text.contains(b, ignoreCase = true)) {
            found += a to b
        }
    }
    return found
}

/** حذف مترادف‌های تکراری، فقط اولین رخداد هر گروه را نگه می‌دارد. */
fun removeSynonymDuplicates(text: String): String {
    var result = text
    for (group in SYNONYM_GROUPS) {
        val present = group.filter { result.contains(it, ignoreCase = true) }
        if (present.size > 1) {
            present.drop(1).forEach { word ->
                result = result.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), "")
            }
        }
    }
    return result.replace(Regex("\\s+"), " ").trim()
}

/**
 * تابع اصلی پایپ‌لاین تمیزکاری. ترتیب فازها ثابت است.
 * نکته: منطق فعلی (Synonym Groups، Stop Words) فقط برای متن انگلیسی
 * طراحی شده؛ معادل فارسی این قوانین هنوز تعریف نشده — این یک
 * محدودیت شناخته‌شده است که باید در پیاده‌سازی واقعی مشخص شود.
 */
fun cleanPrompt(renderedText: String, options: CleaningOptions = CleaningOptions()): Pair<String, CleaningReport> {
    var cleaned = renderedText
    val conflicts = if (options.detectConflicts) detectConflicts(cleaned) else emptyList()
    conflicts.forEach { (_, loser) -> cleaned = cleaned.replace(Regex("\\b$loser\\b", RegexOption.IGNORE_CASE), "") }

    val beforeRedundancy = cleaned
    if (options.removeRedundancy) cleaned = removeSynonymDuplicates(cleaned)

    // Phase 3-5: Stop words, compression, grammar (پیاده‌سازی کامل در کد واقعی)

    val report = CleaningReport(
        conflictsDetected = conflicts.size,
        conflictsResolved = conflicts.map { "${it.first} vs ${it.second}" },
        redundancyRemoved = if (beforeRedundancy != cleaned) 1 else 0,
        stopWordsRemoved = 0,
        originalLength = renderedText.length,
        cleanedLength = cleaned.length,
        compressionRatio = (renderedText.length - cleaned.length).toFloat() / renderedText.length
    )
    return cleaned to report
}
```

### قوانین

| Rule | شرح | Severity |
|---|---|---|
| تضاد آب‌وهوا/منطقی تشخیص داده شود | باید حل شود، نه فقط گزارش | **Blocking** (پردازشی، نه Validation کاربر) |
| کاهش طول کمتر از حداقل مورد انتظار (مثلاً ۳۰٪) | ممکن است تمیزکاری ناقص باشد | **Warning** |

---

## بخش ب — Token Cost Calculator

### سه روش تخمین توکن

| روش | دقت | سرعت | فرمول |
|---|---|---|---|
| Character-based | ±۲۰٪ | خیلی سریع | `کاراکتر / ۴` |
| Word-based | ±۱۰٪ | سریع | `کلمه × ۱.۳۳` |
| Accurate (Tokenizer) | دقیق | کندتر | بر اساس Tokenizer واقعی مدل |

```kotlin
fun estimateTokensFromCharacters(text: String): Int = (text.length / 4.0).let { Math.ceil(it).toInt() }
fun estimateTokensFromWords(text: String): Int {
    val wordCount = text.trim().split(Regex("\\s+")).size
    return Math.ceil(wordCount * 1.33).toInt()
}
```

### بازتعریف نقش Budget Tracking

چون این اپ فقط پرامپت تولید می‌کند (نه رسانه)، «هزینه‌ی واقعی تولید» خارج از کنترل مستقیم این اپ است:

- **Token Counting** → کاملاً کاربردی و اصلی: به کاربر می‌گوید آیا پرامپتش با محدودیت طول مدل انتخابی سازگار است.
- **Budget Tracking/Cost Estimation** → یک فیچر **اختیاری و کمکی**، نه بخش الزامی pipeline؛ این اپ خودش پولی رد و بدل نمی‌کند.

```kotlin
data class TokenCheckResult(val estimatedTokens: Int, val maxTokens: Int, val withinLimit: Boolean, val warning: String?)

/** بررسی نهایی محدودیت توکن — این تنها جایی است که این چک انجام می‌شود (بعد از Rendering، برای یک مدل مشخص). */
fun checkTokenLimit(cleanedText: String, modelProfile: ModelProfile): TokenCheckResult {
    val estimated = estimateTokensFromWords(cleanedText)
    val max = modelProfile.constraints.maxTokens
    return TokenCheckResult(
        estimatedTokens = estimated,
        maxTokens = max,
        withinLimit = estimated <= max,
        warning = if (estimated > max) "پرامپت (${estimated} توکن) از محدودیت این مدل (${max}) بیشتر است" else null
    )
}
```

### Model Pricing — Data-driven، نه هاردکد

اطلاعات قیمت‌گذاری هر مدل (در صورت وجود) باید در همان فایل JSON مربوط به Model Profile (واحد ۱۴) نگهداری شود، نه هاردکد در کد Kotlin — تا افزودن/تغییر مدل بدون Build مجدد اپ ممکن باشد.

---

## معیارهای موفقیت

- تضادهای متنی همیشه قبل از ارائه‌ی خروجی نهایی حل می‌شوند.
- تخمین توکن با دقت قابل‌قبول (حداقل روش Word-based، ±۱۰٪) انجام می‌شود.
- بررسی محدودیت طول همیشه *بعد* از Rendering برای یک مدل خاص انجام می‌شود، نه قبل از آن.
- Budget Tracking (در صورت پیاده‌سازی) صرفاً کمکی است، نه بخشی الزامی از مسیر تولید پرامپت.
