package com.operaboys.cinemashotgenerator.ui.dna

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.entity.ProjectEntity
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// قدم مستقل بعد از فاز ۴ — رفع محدودیت dependentShotsCount ثبت‌شده در
// unit16-execution-plan.md (ADR-047/ADR-051) و سه فیلد بدون UI OutputConstraints.
// جزئیات کامل تصمیمات در docs/adr/054-unit16-dependent-shots-count-output-constraints-ui.md.
//
// چرا یک فایل تست جداگانه (نه افزودن به DnaTabFlowTest.kt): composeRule آن فایل
// از قبل در setUp() یک‌بار `MainScaffold` کامل را `setContent` می‌کند؛ Compose Test
// Rule اجازه‌ی دومین فراخوان `setContent` در همان تست را نمی‌دهد
// (`IllegalStateException: ...has already set content`، یافته‌ی واقعی این قدم). چون
// این تست‌ها فقط رفتار مستقیم DnaTabContent+DnaViewModel با یک dependentShotsCount
// معین را بررسی می‌کنند (نه ناوبری کامل Studio)، Mount مستقیم DnaTabContent (بدون
// MainScaffold/StudioShell) هم دقیق‌تر و هم بدون این تداخل است. خودِ شمارش تجمعی
// صحیح («چند Scene و چند Shot») از قبل با تست موجود
// `ProjectRepositoryTest.observeProjectSummaries counts scenes and shots correctly
// through the scene-shot join` پوشش داده شده — این قدم دقیقاً همان Query موجود
// (ProjectDao.getProjectWithCounts) را از طریق StudioShell بازاستفاده می‌کند، نه یک
// Query تازه؛ چیزی که واقعاً تازه است این‌جا آزموده می‌شود: DnaViewModel این عدد را
// واقعاً به updateCoreIdentity می‌دهد و هشدار واقعی را نمایش می‌دهد.

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DnaSoftLockWarningTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction {
        performScrollTo()
        performSemanticsAction(SemanticsActions.OnClick)
        return this
    }

    private fun mountDnaTab(projectId: String, dependentShotsCount: Int, onShowMessage: (String) -> Unit) {
        runBlocking {
            database.projectDao().saveProject(
                ProjectEntity(projectId, "Test", "2026-08-06T10:00:00Z", "2026-08-06T10:00:00Z")
            )
        }
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                DnaTabContent(
                    projectId = projectId,
                    language = Language.FA,
                    projectDnaRepository = ProjectDnaRepository(database.projectDnaDao()),
                    dependentShotsCount = dependentShotsCount,
                    onShowMessage = onShowMessage
                )
            }
        }
        composeRule.waitUntilExactlyOneExists(hasTestTag(DNA_VISUAL_STYLE_FIELD_TAG), timeoutMillis = 5_000)
    }

    @Test
    fun `changing the Visual Style with a nonzero dependentShotsCount shows a real warning, not always silent`() {
        var lastMessage: String? = null
        mountDnaTab("proj_warning_test", dependentShotsCount = 4, onShowMessage = { lastMessage = it })

        composeRule.onNodeWithTag(DNA_VISUAL_STYLE_FIELD_TAG).clickViaSemantics()
        composeRule.onNodeWithText(visualStyleLabel(VisualStyle.CYBERPUNK, Language.FA)).clickViaSemantics()

        composeRule.waitUntil(timeoutMillis = 5_000) { lastMessage != null }
        assertTrue(lastMessage!!.contains("4"))
    }

    @Test
    fun `changing the Visual Style with zero dependentShotsCount applies the change without any warning`() {
        var lastMessage: String? = null
        mountDnaTab("proj_no_warning_test", dependentShotsCount = 0, onShowMessage = { lastMessage = it })

        composeRule.onNodeWithTag(DNA_VISUAL_STYLE_FIELD_TAG).clickViaSemantics()
        val cyberpunkLabel = visualStyleLabel(VisualStyle.CYBERPUNK, Language.FA)
        composeRule.onNodeWithText(cyberpunkLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(cyberpunkLabel), timeoutMillis = 5_000)

        composeRule.waitForIdle()
        assertTrue(lastMessage == null)
    }
}
