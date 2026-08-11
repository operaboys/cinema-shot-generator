package com.operaboys.cinemashotgenerator.domain.scene

import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.domain.stateversioning.validateStateTransition

// واحد ۱۶ فاز ۴ — قدم ۱: قانون واقعی State Machine (واحد ۱۲) روی عملیات «قفل صحنه»
// (Quick Action «Lock Scene» صفحه‌ی Scene Detail) — دقیقاً هم‌الگو با
// domain/project/ProjectLifecycle.kt (archiveProject، ADR-044). طبق
// ALLOWED_TRANSITIONS واقعی (StateMachine.kt)، DRAFT/REVIEW هر دو مستقیماً به LOCKED
// می‌روند؛ FINAL/ARCHIVED نمی‌توانند (نه در فهرست مقصدهای مجازشان). این تابع Rule
// واقعی را اجرا می‌کند، نه یک Bypass ساختگی — فراخوان (Repository/ViewModel) مسئول
// نمایش دلیل Blocking واقعی به کاربر است.
//
// MIGRATED (رفع بدهی فنی مستند در ADR-064/ADR-066): قبلاً مستقیماً canTransition
// خام را صدا می‌زد — هم‌الگو با تغییر ProjectLifecycle.kt، اکنون از
// validateStateTransition با customMessage عبور می‌کند، بدون افت کیفیت پیام.

fun lockScene(scene: Scene): Result<Scene> {
    val issue = validateStateTransition(
        current = scene.state,
        target = EntityState.LOCKED,
        customMessage = "صحنه در وضعیت ${scene.state} است — طبق قوانین State Machine واحد ۱۲، " +
            "این انتقال به Locked مجاز نیست"
    )
    if (issue != null) return Result.failure(IllegalStateException(issue.message))
    return Result.success(scene.copy(state = EntityState.LOCKED))
}
