package com.operaboys.cinemashotgenerator.ui.workflow

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.workflow.AppTheme
import com.operaboys.cinemashotgenerator.domain.workflow.ComposerLayoutVariant
import com.operaboys.cinemashotgenerator.domain.workflow.HomeLayoutVariant
import com.operaboys.cinemashotgenerator.domain.workflow.WorkflowStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// واحد ۱۶ — فاز ۰: تست‌های WorkflowViewModel (docs/adr/042-unit16-phase0-shared-foundation.md).
//
// scope تزریق‌شده Dispatchers.Unconfined است (نه viewModelScope واقعی) — دلیل کامل
// در کامنت WorkflowViewModel.kt: از قفل‌شدگی واقعی runBlocking{job.join()} روی
// Looper اصلیِ PAUSED‌شده‌ی Robolectric جلوگیری می‌کند، بدون نیاز به هیچ وابستگی
// تست جدید (kotlinx-coroutines-test).

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkflowViewModelTest {

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "test_workflow_prefs_" + UUID.randomUUID().toString().replace("-", "")
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
    }

    private fun newViewModel(): WorkflowViewModel = WorkflowViewModel(
        application = context.applicationContext as Application,
        dataStore = dataStore,
        ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
    )

    // --- مقداردهی اولیه‌ی صحیح ---

    @Test
    fun `initial state uses the documented defaults when nothing is persisted yet`() {
        val viewModel = newViewModel()

        assertEquals(Language.FA, viewModel.language.value)
        assertEquals(AppTheme.DARK, viewModel.theme.value)
        assertEquals(HomeLayoutVariant.HERO, viewModel.homeLayoutVariant.value)
        assertEquals(ComposerLayoutVariant.TABS, viewModel.composerLayoutVariant.value)
        assertNull(viewModel.workflowState.value)
    }

    // --- اعمال فوری ---

    @Test
    fun `setLanguage applies instantly to the exposed StateFlow`() {
        val viewModel = newViewModel()
        viewModel.setLanguage(Language.EN)
        assertEquals(Language.EN, viewModel.language.value)
    }

    @Test
    fun `setTheme applies instantly to the exposed StateFlow, independently of language`() {
        val viewModel = newViewModel()
        viewModel.setTheme(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, viewModel.theme.value)
        assertEquals(Language.FA, viewModel.language.value) // تغییرنکرده — مستقل از تم
    }

    // --- Persistence واقعی (نه Mock) ---

    @Test
    fun `language, theme, and layout picks persist across a fresh ViewModel instance backed by the same DataStore`() = runBlocking {
        val viewModel1 = newViewModel()
        viewModel1.setLanguage(Language.EN).join()
        viewModel1.setTheme(AppTheme.LIGHT).join()
        viewModel1.setHomeLayoutVariant(HomeLayoutVariant.RESUME).join()
        viewModel1.setComposerLayoutVariant(ComposerLayoutVariant.ACCORDION).join()

        val viewModel2 = newViewModel()

        assertEquals(Language.EN, viewModel2.language.value)
        assertEquals(AppTheme.LIGHT, viewModel2.theme.value)
        assertEquals(HomeLayoutVariant.RESUME, viewModel2.homeLayoutVariant.value)
        assertEquals(ComposerLayoutVariant.ACCORDION, viewModel2.composerLayoutVariant.value)
    }

    // --- WorkflowState ---

    @Test
    fun `startWorkflowSession creates a real WorkflowState for the given project`() {
        val viewModel = newViewModel()
        viewModel.startWorkflowSession("proj_001")

        val state = viewModel.workflowState.value
        assertEquals("proj_001", state?.projectId)
        assertEquals(WorkflowStep.STORY_WIZARD, state?.currentStep)
    }
}
