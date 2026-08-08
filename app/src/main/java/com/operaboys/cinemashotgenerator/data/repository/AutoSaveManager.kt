package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.ProjectDao
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import java.time.Instant

// واحد ۱۵ — تکمیل AutoSaveManager (طبق ADR-017، در قدم اول واحد ۱۵ این کلاس
// فقط به‌عنوان امضای TODO ثبت شده بود، بدون بدنه‌ی واقعی).
// منبع حقیقت: docs/blueprints/15-project-storage.md، بخش «Auto-Save و Backup».
//
// تصمیم طراحی (مستند در docs/adr/022-unit15-autosave-manager-deviations.md):
// منطق Timer/Debounce واقعی (فراخوانی خودکار هر ۳۰ ثانیه) در این لایه پیاده
// نشد. تصمیم معماری هر ۳۰ ثانیه یا بلافاصله بعد از تغییرات مهم به Lifecycle
// واقعی اپ (Activity/Compose Scope، واحد ۱۶ — هنوز در این پروژه وجود ندارد)
// وابسته است، نه به لایه‌ی Repository. این کلاس فقط منطق تصمیم (saveIfDirty)
// را پیاده می‌کند؛ فراخوانی دوره‌ای/رویدادمحور آن مسئولیت لایه‌ی بالاتر است.
// intervalSeconds همچنان به‌عنوان مقدار پیکربندی نگه داشته شده (public، نه
// private) تا لایه‌ی UI آینده بتواند برای زمان‌بندی واقعی Timer از همین مقدار
// بخواند — بدون این‌که خودِ این کلاس Timer را اجرا کند.
class AutoSaveManager(
    private val projectDao: ProjectDao,
    val intervalSeconds: Long = 30,
    private val clock: () -> String = ::nowIso8601
) {
    suspend fun saveIfDirty(project: ProjectEntity, isDirty: Boolean): Result<Boolean> {
        if (!isDirty) return Result.success(false)
        return runCatching {
            projectDao.saveProject(project.copy(lastModified = clock()))
            true
        }
    }

    // واحد ۱۶ فاز ۶ — قدم ۱: اتصال واقعی این کلاس به یک Timer دوره‌ای (طبق تصمیم
    // مستند docs/adr/058-unit16-phase6-step1-settings-autosave.md). ذخیره‌ی واقعیِ
    // فیلدهای هر Entity (Scene/Shot/DNA/Asset) از قبل و مستقل از این کلاس، بلافاصله
    // روی هر تغییر انجام می‌شود — این متد فقط `ProjectEntity.lastModified` را در
    // فاصله‌های زمانی منظم تازه نگه می‌دارد (شکافی که پیش از این قدم واقعاً وجود
    // داشت: `lastModified` فقط با تغییرنام/آرشیو/کپی پروژه به‌روزرسانی می‌شد، هرگز با
    // ویرایش واقعی محتوا). `saveIfDirty` موجود عمداً بدون تغییر ماند (تست‌های
    // AutoSaveManagerTest.kt موجود دست‌نخورده)؛ این یک متد Convenience تازه است که
    // خودش بارگذاری فعلیِ Project را هم انجام می‌دهد، تا لایه‌ی UI مجبور به حمل
    // ProjectEntity خام نباشد.
    suspend fun touch(projectId: String): Result<Boolean> {
        val project = projectDao.loadProject(projectId) ?: return Result.success(false)
        return saveIfDirty(project, isDirty = true)
    }
}

private fun nowIso8601(): String = Instant.now().toString()
