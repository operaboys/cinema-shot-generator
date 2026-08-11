package com.operaboys.cinemashotgenerator.data.repository

import com.operaboys.cinemashotgenerator.data.dao.EventLogDao
import com.operaboys.cinemashotgenerator.data.entity.EventLogEntity
import com.operaboys.cinemashotgenerator.domain.story.OverrideEvent
import com.operaboys.cinemashotgenerator.domain.story.OverrideEventLogger
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// رفع G3 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، اولویت ۳ بند ۸):
// پیاده‌سازی واقعی OverrideEventLogger (domain/story/OverrideActions.kt) — قبلاً
// فقط NoOp وجود داشت، پس هیچ Override انسانی واقعی هرگز ثبت نمی‌شد. از همان
// EventLogEntity/EventLogDao موجود (واحد ۱۵، ساخته‌شده برای StateVersioningEventLogger)
// بازاستفاده می‌کند — بدون نیاز به Schema/Entity تازه.
//
// OverrideEventLogger.log(event) یک تابع synchronous است (نه suspend) — دقیقاً طبق
// امضای موجود در OverrideActions.kt که این قدم عمداً تغییرش نداد (خارج از دستور کار).
// چون EventLogDao.logEvent واقعاً suspend است (نوشتن Room)، این کلاس یک CoroutineScope
// تزریقی می‌گیرد و نوشتن را روی آن launch می‌کند — هم‌الگو با ioScopeOverride سراسری
// این پروژه، نه بلاک‌کردن Thread فراخوان (که می‌تواند Main Thread باشد).
class RoomOverrideEventLogger(
    private val eventLogDao: EventLogDao,
    private val scope: CoroutineScope,
    private val clock: () -> String = ::defaultClock
) : OverrideEventLogger {

    override fun log(event: OverrideEvent) {
        scope.launch {
            eventLogDao.logEvent(
                EventLogEntity(
                    eventType = event.type,
                    entityId = event.overrideId,
                    timestamp = clock()
                )
            )
        }
    }
}

private fun defaultClock(): String = Instant.now().toString()
