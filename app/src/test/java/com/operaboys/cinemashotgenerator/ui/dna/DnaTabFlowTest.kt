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
import androidx.compose.ui.test.performTextReplacement
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
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.LocationType
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.domain.visualidentity.CinematicMode
import com.operaboys.cinemashotgenerator.domain.visualidentity.StyleInfluence
import com.operaboys.cinemashotgenerator.domain.visualidentity.resolveEffectiveCinematicMode
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_HOME_TAG
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.StudioTab
import com.operaboys.cinemashotgenerator.ui.navigation.studioTabTestTag
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
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
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده.
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

    // قدم مستقل بعد از فاز ۴ — رفع محدودیت dependentShotsCount + سه فیلد بدون UI
    // OutputConstraints (docs/adr/054-...md). تست‌های خودِ dependentShotsCount
    // (که DnaTabContent را مستقیماً، بدون MainScaffold، Mount می‌کنند) در فایل
    // جداگانه‌ی DnaSoftLockWarningTest.kt هستند — چون composeRule این فایل از
    // قبل در setUp() یک‌بار روی MainScaffold کامل `setContent` صدا زده و Compose
    // Test Rule اجازه‌ی دومین فراخوان `setContent` در همان تست را نمی‌دهد
    // (`IllegalStateException: ...has already set content`، یافته‌ی واقعی این قدم).

    // حذف مفهومی mandatoryElements (ADR-128): بخش «افزودن یک عنصر الزامی» این تست
    // (که قبلاً همراه forbidden element/max shot duration در یک تست ترکیبی بود)
    // حذف شد — خودِ فیلد/UI/ViewModel متناظرش کامل از پروژه حذف شده‌اند.

    @Test
    fun `adding a forbidden element and setting max shot duration round-trips after leaving and re-entering Studio`() {
        createProjectAndOpenDnaTab("DNA Output Constraints Test")

        composeRule.onNodeWithTag(DNA_MAX_SHOT_DURATION_FIELD_TAG).performScrollTo().performTextReplacement("15")

        composeRule.onNodeWithTag(DNA_FORBIDDEN_VALUE_FIELD_TAG).performScrollTo().performTextInput("dutch_angle")
        composeRule.onNodeWithTag(DNA_ADD_FORBIDDEN_ELEMENT_BUTTON_TAG).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(dnaForbiddenElementChipTag(0)), timeoutMillis = 5_000)

        // «بستن و بازکردن مجدد صفحه» — عیناً همان الگوی تست Round-Trip بالا (Visual Style).
        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).performClick()
        composeRule.onNodeWithText("DNA Output Constraints Test").clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(studioTabTestTag(StudioTab.STORY)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(studioTabTestTag(StudioTab.DNA)).performClick()

        composeRule.waitUntilExactlyOneExists(hasTestTag(DNA_MAX_SHOT_DURATION_FIELD_TAG), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(DNA_MAX_SHOT_DURATION_FIELD_TAG).performScrollTo().assertTextContains("15")
        // یافته‌ی واقعی: substring ای که از مرز متن فارسی (RTL) به انگلیسی (LTR) عبور
        // کند (مثلاً "دوربین: dutch_angle") در assertTextContains شکست می‌خورد — برای
        // پرهیز از این مرز، فقط بخش انگلیسی (بدون پیشوند فارسی دسته) بررسی می‌شود.
        composeRule.onNodeWithTag(dnaForbiddenElementChipTag(0)).performScrollTo().assertTextContains("dutch_angle", substring = true)
    }

    private fun moodLabelFor(mood: Mood): String = com.operaboys.cinemashotgenerator.ui.story.moodLabel(mood, Language.FA)

    // تکمیل Rule یتیم — قدم ۴ از ۴ (ADR-112، آخرین قدم کل فیچر): هم‌الگو دقیق با
    // «selecting a grouped Mood ...» بالا — برای فیلد تازه‌ی globalMode
    // (ProjectDna.cinematicLanguage.globalMode، سه گزینه‌ی ثابت و غیر-nullable).

    @Test
    fun `selecting the global Cinematic Mode saves the correct value`() {
        createProjectAndOpenDnaTab("DNA Cinematic Mode Test")

        composeRule.onNodeWithTag(DNA_CINEMATIC_MODE_FIELD_TAG).clickViaSemantics()
        val fastCutLabel = cinematicModeLabel(CinematicMode.FAST_CUT, Language.FA)
        composeRule.onNodeWithText(fastCutLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(fastCutLabel), timeoutMillis = 5_000)
    }

    /**
     * تست End-to-End واقعی، طبق الزام صریح دستور کار: اثبات می‌کند کل زنجیره‌ی
     * ADR-106 تا ADR-112 واقعاً به‌هم وصل است — نه فقط لایه‌های جدا. تنظیم
     * globalMode از طریق UI واقعی (نه یک `ProjectDna` دستی در تست دامنه) ذخیره
     * می‌شود؛ سپس همان `ProjectDna` از طریق `ProjectDnaRepository` واقعی
     * (همان دیتابیس Room این تست) دوباره بارگذاری و مستقیماً به
     * `resolveEffectiveCinematicMode` (domain.visualidentity) داده می‌شود —
     * برای یک Scene/Shot خنثی، بدون هیچ Override — تا ثابت شود مقدار انتخاب‌شده
     * در UI واقعاً به نتیجه‌ی نهایی این تابع دامنه‌ای می‌رسد.
     */
    @Test
    fun `changing the global Cinematic Mode from the DNA tab actually changes resolveEffectiveCinematicMode for a shot with no override`() {
        createProjectAndOpenDnaTab("DNA Cinematic Mode E2E Test")

        composeRule.onNodeWithTag(DNA_CINEMATIC_MODE_FIELD_TAG).clickViaSemantics()
        val longTakeLabel = cinematicModeLabel(CinematicMode.LONG_TAKE, Language.FA)
        composeRule.onNodeWithText(longTakeLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(longTakeLabel), timeoutMillis = 5_000)

        val projectDnaRepository = ProjectDnaRepository(database.projectDnaDao())
        val loadedDna = runBlocking {
            val projectId = database.projectDao().getAllProjects().first()
                .first { it.projectName == "DNA Cinematic Mode E2E Test" }.projectId
            projectDnaRepository.loadProjectDna(projectId).getOrThrow()!!
        }

        val neutralScene = Scene(
            sceneId = "scene_e2e",
            sceneNumber = 1,
            narrativeRole = NarrativeRole.DEVELOPMENT,
            location = SceneLocation(LocationType.MIXED, "a neutral location"),
            timeOfDay = TimeOfDay.AFTERNOON,
            atmospherePrimary = Atmosphere.CALM
        )
        val neutralShot = Shot(
            shotId = "shot_e2e",
            sceneId = "scene_e2e",
            shotNumber = 1,
            shotDescription = "a neutral shot with no cinematic mode override",
            shotGoal = ShotGoal.ESTABLISHING,
            shotType = ShotType.MEDIUM,
            durationSeconds = 4f,
            motionLevel = MotionLevel.SUBTLE,
            soundProfile = SoundProfile(enabled = false)
        )

        assertEquals(CinematicMode.LONG_TAKE, resolveEffectiveCinematicMode(loadedDna, neutralScene, neutralShot))
    }

    // اتصال کامل Style Matrix — قدم ۳ از ۸ زیرقدم (ADR-115): چهار تست تازه —
    // انتخاب Secondary Style + نمایش/عدم‌نمایش شرطی فیلد Influence، پاک‌شدن
    // همزمان Influence با انتخاب «بدون سبک ثانویه»، انتخاب Influence، و نمایش
    // زنده‌ی هشدار ناسازگاری (هم برای یک جفت ناسازگار HIGH→ندارد و یک جفت
    // LOW→دارد، طبق ماتریس واقعی ADR-114).

    @Test
    fun `selecting a Secondary Style saves it and reveals the Influence field, which is hidden before any secondary style is chosen`() {
        createProjectAndOpenDnaTab("Style Matrix Secondary Style Test")

        composeRule.onNodeWithTag(DNA_STYLE_INFLUENCE_FIELD_TAG).assertDoesNotExist()

        composeRule.onNodeWithTag(DNA_SECONDARY_STYLE_FIELD_TAG).clickViaSemantics()
        val filmNoirLabel = visualStyleLabel(VisualStyle.FILM_NOIR, Language.FA)
        composeRule.onNodeWithText(filmNoirLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(filmNoirLabel), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(DNA_STYLE_INFLUENCE_FIELD_TAG).performScrollTo().assertTextContains(
            uiString("dna.coreIdentity.influenceUnset", Language.FA)
        )
    }

    @Test
    fun `choosing no secondary style clears both secondaryStyle and influence, and hides the Influence field again`() {
        createProjectAndOpenDnaTab("Style Matrix Clear Secondary Style Test")

        composeRule.onNodeWithTag(DNA_SECONDARY_STYLE_FIELD_TAG).clickViaSemantics()
        val filmNoirLabel = visualStyleLabel(VisualStyle.FILM_NOIR, Language.FA)
        composeRule.onNodeWithText(filmNoirLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(filmNoirLabel), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(DNA_STYLE_INFLUENCE_FIELD_TAG).performScrollTo().clickViaSemantics()
        val strongLabel = styleInfluenceLabel(StyleInfluence.STRONG, Language.FA)
        composeRule.onNodeWithText(strongLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(strongLabel), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(DNA_SECONDARY_STYLE_FIELD_TAG).performScrollTo().clickViaSemantics()
        val noneLabel = uiString("dna.coreIdentity.secondaryStyleNone", Language.FA)
        composeRule.onNodeWithText(noneLabel).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(hasText(noneLabel), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(DNA_STYLE_INFLUENCE_FIELD_TAG).assertDoesNotExist()

        val reloaded = runBlocking {
            val projectDnaRepository = ProjectDnaRepository(database.projectDnaDao())
            val projectId = database.projectDao().getAllProjects().first()
                .first { it.projectName == "Style Matrix Clear Secondary Style Test" }.projectId
            projectDnaRepository.loadProjectDna(projectId).getOrThrow()!!
        }
        assertEquals(null, reloaded.coreIdentity.secondaryStyle)
        assertEquals(null, reloaded.coreIdentity.influence)
    }

    @Test
    fun `a HIGH-compatibility secondary style shows no warning, and a LOW-compatibility secondary style shows the live warning`() {
        createProjectAndOpenDnaTab("Style Matrix Warning Test")

        // پیش‌فرض dominantVisualStyle، CINEMATIC_STYLE است (defaultProjectDna).
        composeRule.onNodeWithTag(DNA_SECONDARY_STYLE_FIELD_TAG).clickViaSemantics()
        val filmNoirLabel = visualStyleLabel(VisualStyle.FILM_NOIR, Language.FA)
        composeRule.onNodeWithText(filmNoirLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(filmNoirLabel), timeoutMillis = 5_000)

        // CINEMATIC_STYLE × FILM_NOIR — هر دو CINEMATIC، طبق ماتریس Category = HIGH → بدون هشدار.
        val highWarningText = uiTemplate(
            "dna.coreIdentity.styleCompatibilityWarningTemplate",
            Language.FA,
            "primary" to visualStyleLabel(VisualStyle.CINEMATIC_STYLE, Language.FA),
            "secondary" to filmNoirLabel
        )
        composeRule.onNodeWithText(highWarningText).assertDoesNotExist()

        composeRule.onNodeWithTag(DNA_SECONDARY_STYLE_FIELD_TAG).performScrollTo().clickViaSemantics()
        val mangaLabel = visualStyleLabel(VisualStyle.MANGA, Language.FA)
        composeRule.onNodeWithText(mangaLabel).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(mangaLabel), timeoutMillis = 5_000)

        // CINEMATIC_STYLE (CINEMATIC) × MANGA (ANIMATION_2D) طبق ماتریس Category = LOW → هشدار زنده.
        val lowWarningText = uiTemplate(
            "dna.coreIdentity.styleCompatibilityWarningTemplate",
            Language.FA,
            "primary" to visualStyleLabel(VisualStyle.CINEMATIC_STYLE, Language.FA),
            "secondary" to mangaLabel
        )
        composeRule.waitUntilExactlyOneExists(hasText(lowWarningText), timeoutMillis = 5_000)
    }
}
