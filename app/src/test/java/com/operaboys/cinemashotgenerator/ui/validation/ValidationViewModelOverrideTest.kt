package com.operaboys.cinemashotgenerator.ui.validation

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.story.OverrideType
import com.operaboys.cinemashotgenerator.domain.validation.Severity
import com.operaboys.cinemashotgenerator.domain.validation.ValidationIssue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع یافته‌ی معماری «Human Override هرگز به هیچ اقدام واقعی کاربر وصل نشده»
// (G3/ADR-067، ADR-068) — تست مستقیم ValidationViewModel (نه از طریق کل Compose
// Navigation Tree، هم‌الگو با OutputDeliveryViewModelTest.kt): زنجیره‌ی واقعی
// createOverrideForIssue/revokeOverrideAction → domain.story.createOverride/
// revokeOverride → HumanOverrideRepository (Room واقعی، نه Mock) → EventLogDao
// (Room واقعی) را می‌سنجد.

private const val PROJECT_ID = "proj_validation_override_vm_test"
private const val SCENE_ID = "scene_validation_override_vm_test"
private const val SHOT_ID = "shot_validation_override_vm_test"

private val warningIssue = ValidationIssue(
    severity = Severity.WARNING,
    field = "negative_prompt_override",
    message = "negative_prompt_override فقط فاصله‌ی خالی است"
)

private val blockingIssue = ValidationIssue(
    severity = Severity.BLOCKING,
    field = "shot_description",
    message = "توضیح شات خیلی کوتاه است"
)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ValidationViewModelOverrideTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده.
    }

    private fun buildViewModel(): ValidationViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        return ValidationViewModel(
            application = application,
            projectId = PROJECT_ID,
            sceneId = SCENE_ID,
            shotId = SHOT_ID,
            shotRepository = ShotRepository(database.shotDao()),
            sceneRepository = SceneRepository(database.sceneDao()),
            projectDnaRepository = ProjectDnaRepository(database.projectDnaDao()),
            assetRepository = AssetRepository(database.assetDao()),
            database = database,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `createOverrideForIssue on a WARNING issue round-trips through real Room storage and is visible after reload`() = runBlocking {
        val viewModel = buildViewModel()

        assertTrue(viewModel.canOverride(warningIssue))
        assertFalse(viewModel.isOverridden(warningIssue))

        viewModel.createOverrideForIssue(warningIssue, OverrideType.ARTISTIC, "تصمیم آگاهانه‌ی کارگردان").join()

        assertTrue(viewModel.isOverridden(warningIssue))
        val stored = viewModel.activeOverrideFor(warningIssue)
        assertTrue(stored != null)
        assertEquals(OverrideType.ARTISTIC, stored!!.overrideType)
        assertEquals("تصمیم آگاهانه‌ی کارگردان", stored.reason)
        assertTrue(stored.active)

        // یافته‌ی واقعی دیباگ این قدم: ساخت یک ViewModel دوم برای شبیه‌سازی «باز
        // کردن دوباره‌ی صفحه» غیرقابل‌اعتماد بود — init داخلی خودش هم یک
        // ioScope.launch مستقل و un-joinable است (بدون بازگشت Job از سازنده)، پس
        // خواندن بلافاصله‌ی isOverridden روی نمونه‌ی تازه با بارگذاری Async آن
        // مسابقه می‌داد. اثبات مطمئن‌تر و مستقیم‌تر «داده واقعاً روی Room نشسته،
        // نه فقط در حافظه»: خودِ OverrideDao (کاملاً مستقل از هر ViewModel) را در
        // همین تست runBlocking می‌خوانیم.
        val rows = database.overrideDao().getOverridesForEntity(SHOT_ID).first()
        assertEquals(1, rows.size)
        assertTrue(rows.first().active)
    }

    @Test
    fun `createOverrideForIssue on a BLOCKING issue is denied by Rule 1 and never saved`() = runBlocking {
        val viewModel = buildViewModel()

        assertFalse(
            "طبق Rule 1 دامنه، دکمه‌ی Override هرگز نباید برای یک Issue با شدت BLOCKING نمایش داده شود",
            viewModel.canOverride(blockingIssue)
        )

        // حتی اگر UI به‌اشتباه صدا بزند: checkOverridePermission داخل createOverride
        // دامنه باید مستقل Denied برگرداند و چیزی ذخیره نشود (دفاع دوم، طبق کامنت
        // خودِ ValidationViewModel.createOverrideForIssue).
        viewModel.createOverrideForIssue(blockingIssue, OverrideType.TECHNICAL, null).join()

        assertFalse(viewModel.isOverridden(blockingIssue))
    }

    @Test
    fun `createOverrideForIssue and revokeOverrideAction both write real EventLog rows through the full ViewModel path`() = runBlocking {
        val viewModel = buildViewModel()

        viewModel.createOverrideForIssue(warningIssue, OverrideType.NARRATIVE, "دلیل تست").join()
        val override = viewModel.activeOverrideFor(warningIssue)
        assertTrue(override != null)

        val createdEvents = database.eventLogDao().getEventsForEntity(override!!.overrideId).first()
        assertEquals(1, createdEvents.size)
        assertEquals("override_created", createdEvents.first().eventType)

        viewModel.revokeOverrideAction(override, "دیگر لازم نیست").join()

        val allEvents = database.eventLogDao().getEventsForEntity(override.overrideId).first()
        assertEquals(2, allEvents.size)
        assertTrue(allEvents.any { it.eventType == "override_revoked" })
        assertFalse("بعد از Revoke، دیگر نباید در activeOverrides دیده شود", viewModel.isOverridden(warningIssue))
    }
}
