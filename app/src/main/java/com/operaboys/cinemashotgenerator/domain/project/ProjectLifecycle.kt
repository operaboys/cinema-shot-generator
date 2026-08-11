package com.operaboys.cinemashotgenerator.domain.project

import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.domain.stateversioning.validateStateTransition

// واحد ۱۶ — فاز ۱: قانون واقعی State Machine (واحد ۱۲) روی عملیات Archive منوی
// Overflow کارت پروژه.
//
// یافته‌ی مهم (مستند در docs/adr/044-...md): طبق ALLOWED_TRANSITIONS واقعی واحد ۱۲
// (StateMachine.kt)، ARCHIVED فقط از FINAL قابل‌دسترسی است — نه از هر وضعیتی. یعنی
// «آرشیو» یک پروژه‌ی تازه‌ساخته‌شده (که همیشه با DRAFT شروع می‌شود) طبق قانون واقعی
// مجاز نیست، حتی اگر ظاهر منوی Overflow آن را یک عملیات همیشه-در-دسترس نشان دهد.
// این تابع این Rule واقعی را اجرا می‌کند (نه یک Bypass ساختگی) — فراخوان (Repository/
// ViewModel) مسئول نمایش دلیل Blocking واقعی به کاربر است، نه نادیده‌گرفتنش.
//
// MIGRATED (رفع بدهی فنی مستند در ADR-064/ADR-066): قبلاً مستقیماً canTransition
// خام را صدا می‌زد (تکرار همان بررسی‌ای که validateStateTransition — لایه‌ی
// ValidationIssue-برگردان واحد ۱۲ — دقیقاً برای همین منظور ساخته شده بود). اکنون
// از validateStateTransition با customMessage عبور می‌کند — یک منبع مشترک برای
// «آیا این انتقال مجاز است»، با حفظ دقیق پیام سفارشی موجود (بدون افت کیفیت پیام).

fun archiveProject(project: Project): Result<Project> {
    val issue = validateStateTransition(
        current = project.state,
        target = EntityState.ARCHIVED,
        customMessage = "پروژه در وضعیت ${project.state} است — طبق قوانین State Machine واحد ۱۲، " +
            "فقط پروژه‌های FINAL قابل آرشیو شدن‌اند"
    )
    if (issue != null) return Result.failure(IllegalStateException(issue.message))
    return Result.success(project.copy(state = EntityState.ARCHIVED))
}
