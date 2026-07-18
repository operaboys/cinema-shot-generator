# واحد ۱۲: وضعیت و نسخه‌بندی (State & Versioning)

**نقش:** Blueprint — منبع حقیقت برای پیاده‌سازی این واحد
**وضعیت:** فعال
**وابستگی:** Project Storage System (واحد ۱۵)، Validation & Consistency Engine (واحد ۰۷)

---

## تعریف

مدیریت چرخه‌ی حیات، وضعیت، و نسخه‌بندی تمام اجزای پروژه (Project، Scene، Shot، Output) — تضمین این‌که تغییرات قابل پیگیری، بازگشت‌پذیر، و کنترل‌شده باشند.

## State Machine (۵ حالت)

```
[New] → Draft → Review → Locked → Final → Archived
         ↑        ↓        ↓
         └────────┴────────┘
           (Reject / Override)
```

| حالت | ویرایش | حذف | استفاده در تولید |
|---|---|---|---|
| **Draft** | آزاد | بله | خیر |
| **Review** | محدود | خیر | خیر |
| **Locked** | فقط با Override | خیر | خیر |
| **Final** | خیر | خیر | بله |
| **Archived** | خیر (فقط خواندنی) | — | بازیابی با تأیید (Restore → Draft جدید) |

```kotlin
enum class EntityState { DRAFT, REVIEW, LOCKED, FINAL, ARCHIVED }

val ALLOWED_TRANSITIONS: Map<EntityState, List<EntityState>> = mapOf(
    EntityState.DRAFT to listOf(EntityState.REVIEW, EntityState.LOCKED),
    EntityState.REVIEW to listOf(EntityState.DRAFT, EntityState.LOCKED),
    EntityState.LOCKED to listOf(EntityState.FINAL, EntityState.DRAFT),
    EntityState.FINAL to listOf(EntityState.ARCHIVED),
    EntityState.ARCHIVED to emptyList()
)

fun canTransition(current: EntityState, target: EntityState): Boolean =
    target in (ALLOWED_TRANSITIONS[current] ?: emptyList())
```

### قوانین انتقال وضعیت

| Rule | شرح | Severity |
|---|---|---|
| انتقال خارج از مسیر مجاز (مثل Draft→Final مستقیم) | باید از Review→Locked عبور کند | **Blocking** |
| انتقال به Final بدون عبور از Locked | ترتیب اجباری | **Blocking** |
| انتقال به Review با فیلدهای الزامی ناقص | باید کامل باشد | **Blocking** |
| انتقال به Final با وابستگی‌های غیر-Final | تمام وابستگی‌ها باید Final باشند | **Blocking** |

---

## Lock Mechanism — نسخه‌ی ساده‌شده تک‌کاربره

**مهم:** این پروژه تک‌کاربره است، پس Lock فقط یک **flag ساده** برای جلوگیری از ویرایش تصادفی است — بدون نیاز به شناسایی «کاربر دیگر» یا هماهنگی بین‌کاربری.

```kotlin
data class EntityLock(
    val locked: Boolean = false,
    val lockedAt: String? = null,
    val lockReason: String? = null
)

fun setLock(entity: Entity, locked: Boolean, reason: String? = null): Entity {
    return entity.copy(lock = EntityLock(locked, if (locked) nowIso8601() else null, reason))
}

/** معادل Human Override برای باز کردن یک Entity قفل‌شده — همان الگوی Soft Lock در سرتاسر پروژه. */
fun unlockWithOverride(entity: Entity): Entity {
    logEvent("lock_override", entity.id)  // ثبت خودکار در تاریخچه
    return entity.copy(lock = EntityLock(locked = false))
}
```

---

## نسخه‌بندی (Versioning)

### دو نوع نسخه

| نوع | تعریف | مثال | Risk | Version Increment |
|---|---|---|---|---|
| **Safe** | فقط محتوا تغییر می‌کند | متن توضیحات، تنظیمات دوربین | پایین | Patch (v1.2.3 → v1.2.4) |
| **Risky** | ساختار یا وابستگی تغییر می‌کند | حذف Asset، تغییر DNA، حذف Character | بالا | Minor/Major |

```kotlin
enum class VersionType { SAFE, RISKY }

data class EntityVersion(
    val versionId: String,               // v1.2.3
    val entityId: String,
    val versionType: VersionType,
    val createdAt: String,
    val changeSummary: String,
    val modifiedFields: List<String>,
    val snapshotData: String,            // JSON کامل موجودیت در این نسخه
    val parentVersionId: String?
)

/** تعیین نوع نسخه بر اساس این‌که آیا فیلدهای تغییریافته روی وابستگی‌ها اثر دارند. */
fun determineVersionType(modifiedFields: List<String>): VersionType {
    val riskyFields = listOf("scene_id", "asset_references", "core_identity")
    return if (modifiedFields.any { it in riskyFields }) VersionType.RISKY else VersionType.SAFE
}
```

### Rollback

```kotlin
/** بازگشت به یک نسخه‌ی قبلی: خودِ Rollback هم یک نسخه‌ی جدید ثبت می‌کند (نه جایگزینی مستقیم تاریخچه). */
fun rollbackToVersion(entityId: String, targetVersionId: String): Result<EntityVersion> {
    val targetVersion = loadVersion(targetVersionId)
        ?: return Result.failure(IllegalArgumentException("نسخه‌ی هدف یافت نشد"))

    val newVersion = createVersionFromSnapshot(
        entityId = entityId,
        snapshotData = targetVersion.snapshotData,
        changeSummary = "Rollback به نسخه‌ی $targetVersionId"
    )
    logEvent("rollback_performed", entityId)
    return Result.success(newVersion)
}
```

---

## Impact Analysis

تحلیل اثر یک تغییر — این واحد از منطق Dependency Resolver (واحد ۰۷) استفاده می‌کند تا مشخص کند چه چیزهایی تحت‌تأثیر قرار می‌گیرند:

```kotlin
data class ImpactResult(
    val affectedEntityIds: List<String>,
    val dependencyChanges: Boolean,
    val breakingChanges: Boolean,
    val riskLevel: String  // "low" | "high" | "critical"
)

fun analyzeVersionImpact(entity: Entity, modifiedFields: List<String>): ImpactResult {
    val dependencyFields = listOf("scene_id", "asset_references")
    val hasDependencyChange = modifiedFields.any { it in dependencyFields }
    val riskLevel = when {
        entity.type == "project_dna" -> "critical"  // اثر روی کل پروژه
        hasDependencyChange -> "high"
        else -> "low"
    }
    return ImpactResult(
        affectedEntityIds = if (hasDependencyChange) findDependents(entity.id) else listOf(entity.id),
        dependencyChanges = hasDependencyChange,
        breakingChanges = false,
        riskLevel = riskLevel
    )
}
```

---

## قوانین اعتبارسنجی

| Rule | شرح | Severity |
|---|---|---|
| ویرایش Entity در حالت Locked/Final بدون Override | نقض قفل | **Blocking** |
| انتقال وضعیت خارج از مسیر مجاز State Machine | ترتیب غلط | **Blocking** |

**نکته مهم:** بخش‌های زیر که در نسخه‌ی اولیه‌ی این سند برای محیط چندکاربره طراحی شده بودند، از این اپ **حذف شده‌اند** (نه فقط پرچم‌گذاری، بلکه واقعاً کنار گذاشته شدند): «Concurrent Changes Management» و کلاس `EntityLock` چندکاربره (با `locked_by`, `forceUnlock`). اگر در آینده حالت تیمی/اشتراکی اضافه شود، این‌ها باید دوباره طراحی شوند.

---

## معیارهای موفقیت

- تمام تغییرات نسخه‌بندی می‌شوند (بدون استثنا).
- Rollback همیشه یک نسخه‌ی جدید ثبت می‌کند، نه جایگزینی مستقیم تاریخچه.
- انتقال وضعیت‌ها همیشه از مسیر مجاز State Machine عبور می‌کند.
- Impact Analysis سریع و دقیق است.
- Lock یک مکانیزم ساده‌ی تک‌کاربره است؛ بدون پیچیدگی هماهنگی چندکاربره.
