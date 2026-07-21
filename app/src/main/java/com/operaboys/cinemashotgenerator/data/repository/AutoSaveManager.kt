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
}

private fun nowIso8601(): String = Instant.now().toString()
