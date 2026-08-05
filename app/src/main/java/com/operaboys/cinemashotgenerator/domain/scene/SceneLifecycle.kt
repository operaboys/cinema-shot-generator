package com.operaboys.cinemashotgenerator.domain.scene

import com.operaboys.cinemashotgenerator.domain.stateversioning.EntityState
import com.operaboys.cinemashotgenerator.domain.stateversioning.canTransition

// واحد ۱۶ فاز ۴ — قدم ۱: قانون واقعی State Machine (واحد ۱۲) روی عملیات «قفل صحنه»
// (Quick Action «Lock Scene» صفحه‌ی Scene Detail) — دقیقاً هم‌الگو با
// domain/project/ProjectLifecycle.kt (archiveProject، ADR-044). طبق
// ALLOWED_TRANSITIONS واقعی (StateMachine.kt)، DRAFT/REVIEW هر دو مستقیماً به LOCKED
// می‌روند؛ FINAL/ARCHIVED نمی‌توانند (نه در فهرست مقصدهای مجازشان). این تابع Rule
// واقعی را اجرا می‌کند، نه یک Bypass ساختگی — فراخوان (Repository/ViewModel) مسئول
// نمایش دلیل Blocking واقعی به کاربر است.

fun lockScene(scene: Scene): Result<Scene> {
    if (!canTransition(scene.state, EntityState.LOCKED)) {
        return Result.failure(
            IllegalStateException(
                "صحنه در وضعیت ${scene.state} است — طبق قوانین State Machine واحد ۱۲، " +
                    "این انتقال به Locked مجاز نیست"
            )
        )
    }
    return Result.success(scene.copy(state = EntityState.LOCKED))
}
