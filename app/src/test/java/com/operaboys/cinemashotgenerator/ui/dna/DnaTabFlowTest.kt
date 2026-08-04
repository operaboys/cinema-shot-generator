package com.operaboys.cinemashotgenerator.ui.dna

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectDnaRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.dna.LightingStyle
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.dna.VisualStyle
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_HOME_TAG
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// واحد ۱۶ فاز ۲ — قدم ۳ (آخرین قدم فاز ۲): تست end-to-end واقعی Tab «DNA» —
// طبق دستور کار: (۱) Round-Trip واقعی (تغییر DNA → بستن/بازکردن Studio → مقادیر
// باقی می‌مانند)؛ (۲) اعتبارسنجی زنده (Hex نامعتبر → Blocking فوری، بدون جلوگیری
// از ادامه‌ی تایپ در فیلدهای دیگر)؛ (۳) انتخاب از فهرست گروه‌بندی‌شده (VisualStyle/
// Mood/LightingStyle) و ذخیره‌ی صحیح. الگوی راه‌اندازی عیناً از
// StoryTabFlowTest.kt/AiStoryBreakdownFlowTest.kt گرفته شده. جزئیات کامل تصمیمات
// در docs/adr/047-unit16-phase2-step3-dna-tab.md.

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DnaTabFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "dna_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
        workflowViewModel = WorkflowViewModel(
            application = context.applicationContext as Application,
            dataStore = dataStore,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        projectListViewModel = ProjectListViewModel(
            application = context.applicationContext as Application,
            repository = ProjectRepository(database.projectDao(), idProvider = { "proj_dna_flow_test" })
        )

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    projectDnaRepository = ProjectDnaRepository(database.projectDnaDao())
                )
            }
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    /** طبق یافته‌ی مستندشده در StoryTabFlowTest.kt: `performClick()` روی FilterChip/DropdownMenuItem این نوع صفحات غیرقابل‌اعتماد است. */
    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction {
        performScrollTo()
        performSemanticsAction(SemanticsActions.OnClick)
        return this
    }

    private fun createProjectAndOpenDnaTab(name: String) {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput(name)
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.DNA)).performClick()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("dna.group.coreIdentity", Language.FA)),
            timeoutMillis = 5_000
        )
    }

    @Test
    fun `changing the Visual Style then leaving and re-entering Studio persists the new value (round-trip)`() {
        createProjectAndOpenDnaTab("DNA Persist Test")

        composeRule.onNodeWithTag(DNA_VISUAL_STYLE_FIELD_TAG).clickViaSemantics()
        val cyberpunkLabel = visualStyleLabel(VisualStyle.CYBERPUNK, Language.FA)
        composeRule.onNodeWithText(cyberpunkLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(cyberpunkLabel), timeoutMillis = 5_000)

        // «بستن و بازکردن مجدد صفحه»: برگشت واقعی به Home و ورود دوباره از کارت پروژه.
        //
        // یافته‌ی واقعی (با دیباگ مستقیم شمارش گره‌ها، دو یافته‌ی جدا):
        // (۱) انتظار روی hasText(name) پس از کلیک کارت، سیگنال قابل‌اعتمادی برای
        // «واقعاً وارد Studio شدیم» نیست — چون کارت همان پروژه از قبل، همین حالا هم
        // روی خودِ صفحه‌ی Home (فهرست «پروژه‌های اخیر») با همین متن قابل‌مشاهده است.
        // سیگنال واقعی و مختص Studio، وجود Tab «داستان» است.
        // (۲) یافته‌ی مهم‌تر: `performClick()` (لمس مبتنی بر مختصات) روی خودِ کارت
        // پروژه (ProjectCard.kt) در این سناریوی خاص (بازگشت به Home از عمق Tab «DNA»
        // استودیو) هیچ ناوبری‌ای Trigger نمی‌کند — تأییدشده با شمارش مستقیم گره‌ها:
        // بعد از کلیک، صفحه هنوز دقیقاً همان Home است (۰ گره Tab «داستان»، ۱ گره متن
        // Home). این عیناً همان کلاس مشکل تثبیت‌شده در ADR-045 برای FilterChip/
        // DropdownMenuItem است، فقط این‌بار روی یک Card؛ رفع با `clickViaSemantics()`.
        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).performClick()
        composeRule.onNodeWithText("DNA Persist Test").clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.STORY)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.DNA)).performClick()

        composeRule.waitUntilExactlyOneExists(hasText(cyberpunkLabel), timeoutMillis = 5_000)
    }

    @Test
    fun `an invalid Hex color shows a Blocking warning immediately without preventing further typing in other fields`() {
        createProjectAndOpenDnaTab("DNA Validation Test")

        composeRule.onNodeWithTag(dnaSwatchFieldTag(0)).performScrollTo().performTextInput("notahex")

        // پیام Blocking واقعی از validateColorPalette («مقادیر رنگ نامعتبر: ...») حاوی «نامعتبر» است.
        composeRule.waitUntilExactlyOneExists(hasText("نامعتبر", substring = true), timeoutMillis = 5_000)

        // فیلد دیگر همچنان قابل‌تایپ است — Soft Lock: هیچ‌چیز Block نمی‌شود.
        composeRule.onNodeWithTag(DNA_GRADING_PRESET_FIELD_TAG).performScrollTo().performTextInput("cinematic teal-orange")
        composeRule.onNodeWithTag(DNA_GRADING_PRESET_FIELD_TAG).assertTextContains("cinematic teal-orange")
    }

    @Test
    fun `selecting a grouped Mood and a grouped Lighting Style saves the correct values`() {
        createProjectAndOpenDnaTab("DNA Grouped Selection Test")

        composeRule.onNodeWithTag(DNA_MOOD_FIELD_TAG).clickViaSemantics()
        val hopefulLabel = moodLabelFor(Mood.HOPEFUL)
        composeRule.onNodeWithText(hopefulLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(hopefulLabel), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(DNA_LIGHTING_STYLE_FIELD_TAG).clickViaSemantics()
        val goldenHourLabel = lightingStyleLabel(LightingStyle.GOLDEN_HOUR, Language.FA)
        composeRule.onNodeWithText(goldenHourLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(goldenHourLabel), timeoutMillis = 5_000)
    }

    private fun moodLabelFor(mood: Mood): String = com.operaboys.cinemashotgenerator.ui.story.moodLabel(mood, Language.FA)
}
