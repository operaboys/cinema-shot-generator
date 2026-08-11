package com.operaboys.cinemashotgenerator.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.domain.story.OverrideScope
import com.operaboys.cinemashotgenerator.domain.story.OverrideType
import com.operaboys.cinemashotgenerator.domain.story.RuleSeverity
import com.operaboys.cinemashotgenerator.domain.story.createOverride
import com.operaboys.cinemashotgenerator.domain.story.revokeOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع G3 ممیزی post-Unit16 (اولویت ۳ بند ۸): تا این قدم هیچ پیاده‌سازی واقعی
// OverrideEventLogger وجود نداشت — هر Override انسانی واقعی هرگز ثبت نمی‌شد.
// این تست ثابت می‌کند یک Override واقعی (از طریق createOverride/revokeOverride
// دامنه‌ی موجود، نه یک Mock) با RoomOverrideEventLogger تزریقی، واقعاً یک ردیف
// در EventLogDao ثبت می‌کند.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomOverrideEventLoggerTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleScope() = OverrideScope(
        entityType = "shot",
        entityId = "shot_001",
        field = "lightingMotivation",
        originalValue = "sunlight",
        overrideValue = "candlelight"
    )

    @Test
    fun `createOverride with a real RoomOverrideEventLogger writes a real row to EventLogDao`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val logger = RoomOverrideEventLogger(database.eventLogDao(), scope)

        val result = createOverride(
            overrideType = OverrideType.ARTISTIC,
            scope = sampleScope(),
            targetRuleSeverity = RuleSeverity.WARNING,
            reason = "تست واقعی ثبت رویداد",
            logger = logger
        )
        assertTrue(result.isSuccess)
        val overrideId = result.getOrThrow().overrideId

        // منتظر تکمیل واقعی کار launch شده در RoomOverrideEventLogger.log می‌ماند —
        // بدون این، خواندن EventLogDao قبل از نوشتن واقعی رخ می‌دهد (طبق یافته‌ی
        // مستند این پروژه: DAO های Suspend به Executor داخلی خودشان hop می‌کنند).
        scope.coroutineContext[kotlinx.coroutines.Job]!!.children.toList().forEach { it.join() }

        val events = database.eventLogDao().getEventsForEntity(overrideId).first()
        assertEquals(1, events.size)
        assertEquals("override_created", events.first().eventType)
        assertEquals(overrideId, events.first().entityId)
    }

    @Test
    fun `revokeOverride with a real RoomOverrideEventLogger writes a real row to EventLogDao`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val logger = RoomOverrideEventLogger(database.eventLogDao(), scope)
        val created = createOverride(
            overrideType = OverrideType.ARTISTIC,
            scope = sampleScope(),
            targetRuleSeverity = RuleSeverity.WARNING,
            logger = logger
        ).getOrThrow()

        revokeOverride(created, reason = "تست لغو", logger = logger)

        scope.coroutineContext[kotlinx.coroutines.Job]!!.children.toList().forEach { it.join() }

        val events = database.eventLogDao().getEventsForEntity(created.overrideId).first()
        assertEquals(2, events.size)
        assertTrue(events.any { it.eventType == "override_revoked" })
    }
}
