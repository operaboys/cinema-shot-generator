package com.operaboys.cinemashotgenerator.ui.storybreakdown

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
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
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.story.STORY_TAB_AI_BREAKDOWN_BUTTON_TAG
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

// واحد ۱۶ فاز ۲ — قدم ۲: تست end-to-end واقعی صفحه‌ی AI Story Breakdown — اولین بار
// که کل زنجیره‌ی واحد ۰۱ب (PromptBuilder→ChunkCombiner→JsonDoctor→
// StoryToDomainMapper) از طریق UI واقعی صدا زده و نتیجه‌اش در AssetDao/SceneDao/
// ShotDao واقعی (نه Fake) ذخیره می‌شود. الگوی راه‌اندازی (MainScaffold + دیتابیس
// In-Memory + idProvider ثابت) عیناً از StoryTabFlowTest.kt/AppNavigationTest.kt
// گرفته شده. جزئیات کامل تصمیمات در docs/adr/046-unit16-phase2-step2-ai-story-breakdown.md.

private const val PROJECT_ID = "proj_breakdown_flow_test"

private const val VALID_JSON = """
{
  "characters": [ {"name":"Alice","description":"A brave explorer","role":"main","gender":"female"} ],
  "locations": [ {"name":"Forest","description":"A dark forest"} ],
  "objects": [],
  "shots": [
    {"sceneName":"Opening","shotNumber":1,"description":"Alice enters the forest","characterNames":["Alice"],"locationName":"Forest","objectNames":[]}
  ]
}
"""

private const val TRAILING_COMMA_JSON = """
{
  "characters": [ {"name":"Bob","description":"A quiet observer","role":"main","gender":"male"}, ],
  "locations": [ {"name":"Room","description":"A small room"} ],
  "objects": [],
  "shots": [
    {"sceneName":"Scene1","shotNumber":1,"description":"Bob looks around","characterNames":["Bob"],"locationName":"Room","objectNames":[]}
  ]
}
"""

private const val UNMATCHED_NAME_JSON = """
{
  "characters": [ {"name":"Dave","description":"A wanderer","role":"main","gender":"male"} ],
  "locations": [ {"name":"Street","description":"A busy street"} ],
  "objects": [],
  "shots": [
    {"sceneName":"Scene1","shotNumber":1,"description":"Dave and Eve walk","characterNames":["Dave","Eve"],"locationName":"Street","objectNames":[]}
  ]
}
"""

/** فصل دوم JSON معتبر بالا، به‌گونه‌ای شکسته شده که [CONTINUE] دقیقاً وسط آرایه‌ی shots بیفتد. */
private const val CHUNK_1 = """
{
  "characters": [ {"name":"Carol","description":"A curious cartographer","role":"main","gender":"female"} ],
  "locations": [ {"name":"Cave","description":"A deep cave"} ],
  "objects": [],
  "shots": [
    {"sceneName":"Scene1","shotNumber":1,"description":"Carol maps the cave","characterNames":["Carol"],"locationName":"Cave","objectNames":[]}
[CONTINUE]"""

private const val CHUNK_2 = """
  ]
}
"""

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiStoryBreakdownFlowTest {

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
        dataStoreFileName = "ai_breakdown_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
            repository = ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID })
        )

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    storyRepository = StoryRepository(database.storyDao(), database.storyBreakdownSessionDao()),
                    assetRepository = AssetRepository(database.assetDao()),
                    sceneRepository = SceneRepository(database.sceneDao()),
                    shotRepository = ShotRepository(database.shotDao())
                )
            }
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    /**
     * یافته‌ی تکرارشده (عیناً همان یافته‌ی مستندشده در StoryTabFlowTest.kt/
     * docs/adr/045-...md): `performClick()` (تزریق لمس مبتنی بر مختصات) روی
     * دکمه‌های این صفحه — که هرکدام داخل یک Column با `verticalScroll` تودرتو
     * قرار دارند — به‌طور تکرارپذیر «موفق» گزارش می‌شود اما وضعیت واقعی هرگز
     * تغییر نمی‌کند. `performSemanticsAction(SemanticsActions.OnClick)` این
     * مشکل را قطعی حل کرد؛ برای همه‌ی کلیک‌های این فایل استفاده شده.
     */
    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction {
        performScrollTo()
        performSemanticsAction(SemanticsActions.OnClick)
        return this
    }

    private fun createProjectAndNavigateToBreakdown(name: String) {
        // این دو کلیک اول (کارت «پروژه‌ی جدید» در Home و دکمه‌ی تأیید Dialog تغییر نام)
        // در هیچ Column با verticalScroll ای نیستند — طبق همان دلیل StoryTabFlowTest.kt،
        // performScrollTo() روی گره‌ی بدون والد Scrollable با AssertionError شکست
        // می‌خورد؛ پس اینجا عیناً از performClick() ساده (نه clickViaSemantics) استفاده شد.
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput(name)
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(STORY_TAB_AI_BREAKDOWN_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.storyLabel", Language.FA)),
            timeoutMillis = 5_000
        )
    }

    private fun goToPhase2AndPaste(json: String) {
        composeRule.onNodeWithTag(AI_BREAKDOWN_STORY_FIELD_TAG).performTextInput(
            "A very long story about a journey through a forest and beyond, full of discovery and wonder."
        )
        composeRule.onNodeWithTag(AI_BREAKDOWN_GENERATE_PROMPT_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.proceedToPhase2Button", Language.FA)),
            timeoutMillis = 5_000
        )
        composeRule.onNodeWithText(uiString("aiBreakdown.proceedToPhase2Button", Language.FA)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.pasteLabel", Language.FA)),
            timeoutMillis = 5_000
        )
        composeRule.onNodeWithTag(AI_BREAKDOWN_PASTE_FIELD_TAG).performTextInput(json)
    }

    @Test
    fun `end-to-end flow with a simple valid JSON reaches Phase 3 and confirm saves real Asset-Scene-Shot rows`() {
        createProjectAndNavigateToBreakdown("Breakdown Happy Path")
        goToPhase2AndPaste(VALID_JSON)

        composeRule.onNodeWithText(uiString("aiBreakdown.continueButton", Language.FA)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.reviewTitle", Language.FA)),
            timeoutMillis = 5_000
        )

        composeRule.onNodeWithText(uiString("aiBreakdown.confirmButton", Language.FA)).clickViaSemantics()

        // یافته‌ی واقعی: Poll کردن DAO های Flow-محور از داخل یک runBlocking تودرتوی
        // waitUntil (به‌جای یک سیگنال UI واقعی) با تعلیق/ازسرگیری coroutine واقعی
        // confirmAndSave (که خودش روی Dispatchers.Main.immediate اجرا می‌شود) تداخل
        // پیدا می‌کند و هرگز به شرط نمی‌رسد. راه‌حل واقعی: منتظر ماندن برای همان
        // سیگنال UI که خودِ confirmAndSave (بعد از تکمیل واقعی همه‌ی نوشتن‌ها) تولید
        // می‌کند — Navigation به Studio (`onConfirmedAndSaved`) — دقیقاً هم‌الگو با
        // انتظار روی گره‌های UI در StoryTabFlowTest.kt، نه Poll مستقیم دیتابیس.
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("story.titleLabel", Language.FA)),
            timeoutMillis = 5_000
        )

        val assets = runBlocking { database.assetDao().getAssetsForProject(PROJECT_ID).first() }
        val scenes = runBlocking { database.sceneDao().getScenesForProject(PROJECT_ID).first() }
        assertEquals(2, assets.size) // Alice (character) + Forest (location)
        assertEquals(1, scenes.size)
        val shots = runBlocking { database.shotDao().getShotsForScene(scenes.first().sceneId).first() }
        assertEquals(1, shots.size)
    }

    /**
     * یافته‌ی واقعی هنگام نوشتن این تست: `processAiResponse` → `repairJson` برای
     * انواع خطای `autoFixable=true` (مثل کاما اضافه) تعمیر را همان‌جا امتحان و در
     * صورت موفقیت مستقیماً `Success` برمی‌گرداند — یعنی `NeedsManualRepair` (و در
     * نتیجه Modal تعمیر دستی UI) اصلاً هرگز برای این نوع خطا Trigger نمی‌شود؛ Modal
     * فقط برای خطاهایی است که حتی بعد از تعمیر خودکار داخلی هم Parse نمی‌شوند. پس
     * «مسیر JsonDoctor + تعمیر خودکار» برای کاما اضافه با رسیدن مستقیم و بی‌صدا به
     * فاز ۳ اثبات می‌شود، نه با باز شدن Modal.
     */
    @Test
    fun `a trailing-comma JSON is silently auto-repaired by JsonDoctor and reaches Phase 3 directly`() {
        createProjectAndNavigateToBreakdown("Breakdown Trailing Comma")
        goToPhase2AndPaste(TRAILING_COMMA_JSON)

        composeRule.onNodeWithText(uiString("aiBreakdown.continueButton", Language.FA)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.reviewTitle", Language.FA)),
            timeoutMillis = 5_000
        )
    }

    @Test
    fun `two chunks ending with CONTINUE are combined correctly by the chunk combiner and reach Phase 3`() {
        createProjectAndNavigateToBreakdown("Breakdown Chunk Combiner")

        composeRule.onNodeWithTag(AI_BREAKDOWN_STORY_FIELD_TAG).performTextInput(
            "A very long story about a cartographer exploring a deep cave system full of secrets."
        )
        composeRule.onNodeWithTag(AI_BREAKDOWN_GENERATE_PROMPT_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.proceedToPhase2Button", Language.FA)),
            timeoutMillis = 5_000
        )
        composeRule.onNodeWithText(uiString("aiBreakdown.proceedToPhase2Button", Language.FA)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.pasteLabel", Language.FA)),
            timeoutMillis = 5_000
        )

        composeRule.onNodeWithTag(AI_BREAKDOWN_PASTE_FIELD_TAG).performTextInput(CHUNK_1)
        composeRule.onNodeWithText(uiString("aiBreakdown.newChunkButton", Language.FA)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiTemplateForCount(1)),
            timeoutMillis = 5_000
        )

        composeRule.onNodeWithTag(AI_BREAKDOWN_PASTE_FIELD_TAG).performTextInput(CHUNK_2)
        composeRule.onNodeWithText(uiString("aiBreakdown.continueButton", Language.FA)).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.reviewTitle", Language.FA)),
            timeoutMillis = 5_000
        )
    }

    @Test
    fun `an unmatched character name (Rule 9) is shown as a warning on the Phase 3 review`() {
        createProjectAndNavigateToBreakdown("Breakdown Unmatched Name")
        goToPhase2AndPaste(UNMATCHED_NAME_JSON)

        composeRule.onNodeWithText(uiString("aiBreakdown.continueButton", Language.FA)).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("aiBreakdown.warningsHeader", Language.FA)),
            timeoutMillis = 5_000
        )

        composeRule.onNodeWithText("Eve", substring = true).performScrollTo().assertIsDisplayed()
    }

    private fun uiTemplateForCount(count: Int): String =
        uiTemplate("aiBreakdown.chunksCountTemplate", Language.FA, "count" to count.toString())
}
