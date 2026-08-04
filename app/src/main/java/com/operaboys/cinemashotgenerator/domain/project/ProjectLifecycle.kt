package com.operaboys.cinemashotgenerator.domain.project

import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.domain.stateversioning.canTransition

// واحد ۱۶ — فاز ۱: قانون واقعی State Machine (واحد ۱۲) روی عملیات Archive منوی
// Overflow کارت پروژه.
//
// یافته‌ی مهم (مستند در docs/adr/044-...md): طبق ALLOWED_TRANSITIONS واقعی واحد ۱۲
// (StateMachine.kt)، ARCHIVED فقط از FINAL قابل‌دسترسی است — نه از هر وضعیتی. یعنی
// «آرشیو» یک پروژه‌ی تازه‌ساخته‌شده (که همیشه با DRAFT شروع می‌شود) طبق قانون واقعی
// مجاز نیست، حتی اگر ظاهر منوی Overflow آن را یک عملیات همیشه-در-دسترس نشان دهد.
// این تابع این Rule واقعی را اجرا می‌کند (نه یک Bypass ساختگی) — فراخوان (Repository/
// ViewModel) مسئول نمایش دلیل Blocking واقعی به کاربر است، نه نادیده‌گرفتنش.

fun archiveProject(project: Project): Result<Project> {
    if (!canTransition(project.state, EntityState.ARCHIVED)) {
        return Result.failure(
            IllegalStateException(
                "پروژه در وضعیت ${project.state} است — طبق قوانین State Machine واحد ۱۲، " +
                    "فقط پروژه‌های FINAL قابل آرشیو شدن‌اند"
            )
        )
    }
    return Result.success(project.copy(state = EntityState.ARCHIVED))
}
