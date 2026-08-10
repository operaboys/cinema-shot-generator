package com.operaboys.cinemashotgenerator.ui.storybreakdown

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع G14 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، بخش الف،
// docs/adr/064-g14-orphaned-rules-decisions.md): validateTargetShotCountRange
// (Rule 2، PromptBuilder.kt) قبلاً هرگز صدا زده نمی‌شد — setTargetShotCount با
// coerceIn(1,150) مقدار خارج از محدوده را بی‌صدا کوتاه می‌کرد. این تست اثبات
// می‌کند اکنون: (۱) مقدار خام کاربر نگه داشته می‌شود (نه کوتاه‌سازی خاموش)، (۲)
// خطای واقعی این Rule در targetShotCountError ظاهر می‌شود، (۳) دکمه‌ی «تولید
// Prompt» (از طریق generatePrompt) تا وقتی خطا برطرف نشود مسدود است. مسیر
// «ورودی معتبر → تولید واقعی Prompt» از قبل در
// AiStoryBreakdownFlowTest.kt («happy path») پوشش داده شده — اینجا فقط رفتار
// تازه (بخش الف) بدون نیاز به Compose/ioScopeOverride تست می‌شود، چون
// setTargetShotCount/generatePrompt's guard هر دو synchronous هستند.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiStoryBreakdownViewModelTest {

    private lateinit var injectedDatabase: AppDatabase
    private lateinit var viewModel: AiStoryBreakdownViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
        viewModel = AiStoryBreakdownViewModel(
            application = application,
            projectId = "proj_target_shot_count_test",
            storyRepository = StoryRepository(injectedDatabase.storyDao(), injectedDatabase.storyBreakdownSessionDao())
        )
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    @Test
    fun `setTargetShotCount above 150 keeps the raw value and shows a real blocking error, not a silent clamp`() {
        viewModel.setTargetShotCount(200)

        assertEquals(200, viewModel.targetShotCount.value)
        assertNotNull(
            "طبق رفع G14، مقدار خارج از محدوده باید یک خطای واقعی نشان دهد، نه کوتاه‌سازی خاموش",
            viewModel.targetShotCountError.value
        )
    }

    @Test
    fun `setTargetShotCount below 1 keeps the raw value and shows a real blocking error, not a silent clamp`() {
        viewModel.setTargetShotCount(0)

        assertEquals(0, viewModel.targetShotCount.value)
        assertNotNull(viewModel.targetShotCountError.value)
    }

    @Test
    fun `setTargetShotCount within 1-150 clears the error, matching previous valid-input behavior`() {
        viewModel.setTargetShotCount(200)
        assertNotNull(viewModel.targetShotCountError.value)

        viewModel.setTargetShotCount(50)

        assertEquals(50, viewModel.targetShotCount.value)
        assertNull(viewModel.targetShotCountError.value)
    }

    @Test
    fun `generatePrompt is blocked while targetShotCountError is set`() {
        viewModel.setTargetShotCount(200)

        viewModel.generatePrompt()

        assertNull(
            "generatePrompt باید قبل از هرگونه تولید متن، به‌خاطر خطای Rule 2، بی‌اثر برگردد",
            viewModel.generatedPrompt.value
        )
    }
}
