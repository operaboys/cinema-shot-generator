package com.operaboys.cinemashotgenerator.ui.dna

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.dna.ProjectDna
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.visualidentity.StyleInfluence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// اتصال Style Matrix — قدم ۱الف از ۱۰ زیرقدم (ADR-113): تست واحد مستقیم
// DnaViewModel.setSecondaryVisualStyle/setStyleInfluence — هم‌الگوی دقیق
// OutputDeliveryViewModelTest.kt (ساخت مستقیم ViewModel با
// ioScopeOverride=Dispatchers.Unconfined، بدون Compose/UI؛ database.close()
// عمداً حذف شده، طبق یافته‌ی مستندشده‌ی ADR-070/071 در همان فایل).
//
// یافته‌ی واقعی دیباگ این فایل: updateAndSave داخل DnaViewModel هر بار یک
// ذخیره‌ی Fire-and-Forget جدا (ioScope.launch { repository.saveProjectDna(...) })
// راه می‌اندازد؛ چون saveProjectDna یک تابع suspend واقعی Room است (نه صرفاً
// یک محاسبه‌ی درون‌حافظه)، دو فراخوانی پیاپی setSecondaryVisualStyle/
// setStyleInfluence دو Coroutine مستقل و بالقوه هم‌زمان می‌سازند — تکمیل آن‌ها
// به ترتیب فراخوانی تضمین‌شده نیست (حتی با Dispatchers.Unconfined، چون Room
// suspend Query واقعاً به Executor داخلی خودش سوییچ می‌کند). این یک ویژگی
// از قبل موجود Auto-Save این ViewModel است (در هر Setter دیگر هم همینطور)،
// نه باگ تازه‌ی این قدم — خارج از Scope این ADR است. برای رفع این مسابقه در
// خودِ تست (نه کد Production)، awaitRepositoryState هم‌الگوی awaitCondition
// موجود AiStoryBreakdownViewModelTest.kt (Poll با Timeout) استفاده شد.

private const val PROJECT_ID = "proj_dna_vm_test"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DnaViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ProjectDnaRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = ProjectDnaRepository(database.projectDnaDao())
        // ProjectDnaEntity یک ForeignKey واقعی به ProjectEntity دارد — بدون این،
        // saveProjectDna داخل updateAndSave بی‌صدا شکست می‌خورد (همان یافته‌ی
        // مستندشده‌ی OutputDeliveryViewModelTest.kt).
        runBlocking {
            ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID }).createProject("Dna VM Test").getOrThrow()
        }
    }

    private fun buildViewModel(): DnaViewModel = DnaViewModel(
        application = ApplicationProvider.getApplicationContext(),
        projectId = PROJECT_ID,
        repository = repository,
        idProvider = { "dna_vm_test" },
        ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
    )

    private fun awaitRepositoryState(timeoutMs: Long = 3000, predicate: (ProjectDna) -> Boolean): ProjectDna {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last: ProjectDna? = null
        while (System.currentTimeMillis() < deadline) {
            last = runBlocking { repository.loadProjectDna(PROJECT_ID).getOrThrow() }
            if (last != null && predicate(last)) return last
            Thread.sleep(5)
        }
        assertTrue("condition was never met within ${timeoutMs}ms, last value=$last", false)
        error("unreachable")
    }

    @Test
    fun `setSecondaryVisualStyle persists the selected style and setStyleInfluence persists the selected influence`() {
        val viewModel = buildViewModel()

        viewModel.setSecondaryVisualStyle(VisualStyle.FILM_NOIR)
        assertEquals(VisualStyle.FILM_NOIR, viewModel.dna.value.coreIdentity.secondaryStyle)
        awaitRepositoryState { it.coreIdentity.secondaryStyle == VisualStyle.FILM_NOIR }

        viewModel.setStyleInfluence(StyleInfluence.STRONG)
        assertEquals(StyleInfluence.STRONG, viewModel.dna.value.coreIdentity.influence)
        val reloaded = awaitRepositoryState { it.coreIdentity.influence == StyleInfluence.STRONG }

        assertEquals(VisualStyle.FILM_NOIR, reloaded.coreIdentity.secondaryStyle)
        assertEquals(StyleInfluence.STRONG, reloaded.coreIdentity.influence)
    }

    @Test
    fun `setting secondaryStyle to null also resets influence to null, both in state and after reload`() {
        val viewModel = buildViewModel()

        viewModel.setSecondaryVisualStyle(VisualStyle.CYBERPUNK)
        awaitRepositoryState { it.coreIdentity.secondaryStyle == VisualStyle.CYBERPUNK }
        viewModel.setStyleInfluence(StyleInfluence.MODERATE)
        awaitRepositoryState { it.coreIdentity.influence == StyleInfluence.MODERATE }
        assertEquals(VisualStyle.CYBERPUNK, viewModel.dna.value.coreIdentity.secondaryStyle)
        assertEquals(StyleInfluence.MODERATE, viewModel.dna.value.coreIdentity.influence)

        viewModel.setSecondaryVisualStyle(null)

        assertNull(viewModel.dna.value.coreIdentity.secondaryStyle)
        assertNull(viewModel.dna.value.coreIdentity.influence)

        val reloaded = awaitRepositoryState { it.coreIdentity.secondaryStyle == null }
        assertNull(reloaded.coreIdentity.secondaryStyle)
        assertNull(reloaded.coreIdentity.influence)
    }

    @Test
    fun `setStyleInfluence alone does not change secondaryStyle`() {
        val viewModel = buildViewModel()

        viewModel.setSecondaryVisualStyle(VisualStyle.ANIME)
        awaitRepositoryState { it.coreIdentity.secondaryStyle == VisualStyle.ANIME }
        viewModel.setStyleInfluence(StyleInfluence.SUBTLE)
        awaitRepositoryState { it.coreIdentity.influence == StyleInfluence.SUBTLE }
        assertEquals(VisualStyle.ANIME, viewModel.dna.value.coreIdentity.secondaryStyle)

        viewModel.setStyleInfluence(null)

        assertEquals(VisualStyle.ANIME, viewModel.dna.value.coreIdentity.secondaryStyle)
        assertNull(viewModel.dna.value.coreIdentity.influence)
        val reloaded = awaitRepositoryState { it.coreIdentity.influence == null }
        assertEquals(VisualStyle.ANIME, reloaded.coreIdentity.secondaryStyle)
    }
}
