package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.SceneRepository
import com.operaboys.cinemashotgenerator.data.repository.ShotRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterAsset
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.asset.ContinuityRules
import com.operaboys.cinemashotgenerator.domain.asset.Environment
import com.operaboys.cinemashotgenerator.domain.asset.Gender
import com.operaboys.cinemashotgenerator.domain.asset.LocationAsset
import com.operaboys.cinemashotgenerator.domain.asset.LocationType
import com.operaboys.cinemashotgenerator.domain.asset.ObjectAsset
import com.operaboys.cinemashotgenerator.domain.asset.ObjectSubtype
import com.operaboys.cinemashotgenerator.domain.asset.Outfit
import com.operaboys.cinemashotgenerator.domain.asset.PhysicalAppearance
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.scene.Atmosphere
import com.operaboys.cinemashotgenerator.domain.scene.NarrativeRole
import com.operaboys.cinemashotgenerator.domain.scene.Scene
import com.operaboys.cinemashotgenerator.domain.scene.SceneLocation
import com.operaboys.cinemashotgenerator.domain.scene.TimeOfDay
import com.operaboys.cinemashotgenerator.domain.scene.LocationType as SceneLocationType
import com.operaboys.cinemashotgenerator.domain.shot.MotionLevel
import com.operaboys.cinemashotgenerator.domain.shot.Shot
import com.operaboys.cinemashotgenerator.domain.shot.ShotGoal
import com.operaboys.cinemashotgenerator.domain.shot.ShotType
import com.operaboys.cinemashotgenerator.domain.shot.SoundProfile
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_ASSETS_TAG
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

// واحد ۱۶ فاز ۳ — قدم ۱: تست end-to-end واقعی صفحه‌ی Asset Library — طبق دستور
// کار: جابه‌جایی بین سه نوع فیلتر Segmented و نمایش زیرفیلتر متناظر. الگوی
// راه‌اندازی عیناً از DnaTabFlowTest.kt/AiStoryBreakdownFlowTest.kt گرفته شده.
// جزئیات کامل تصمیمات در docs/adr/048-unit16-phase3-step1-asset-library.md.
//
// MIGRATED (رفع G6 ممیزی post-Unit16، docs/adr/062-...): دیگر از
// PLACEHOLDER_ACTIVE_PROJECT_ID استفاده نمی‌شود (کاملاً حذف شد). AssetsScreen
// اکنون واقعاً به resolveActiveOrRecentProjectId متکی است که یک ردیف Project
// واقعی در projectSummaries لازم دارد — پس این تست یک پروژه‌ی واقعی می‌سازد
// (بدون نیاز به ورود به Studio) و Asset ها را زیر همان projectId واقعی ذخیره
// می‌کند، نه یک رشته‌ی جعلی.

private const val PROJECT_ID = "proj_asset_library_test"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssetsScreenFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var shotRepository: ShotRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "asset_library_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(dataStoreFileName) }
        )
        workflowViewModel = WorkflowViewModel(
            application = context.applicationContext as Application,
            dataStore = dataStore,
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val projectRepository = ProjectRepository(database.projectDao(), idProvider = { PROJECT_ID })
        projectListViewModel = ProjectListViewModel(
            application = context.applicationContext as Application,
            repository = projectRepository
        )

        val assetRepository = AssetRepository(database.assetDao())
        val sceneRepository = SceneRepository(database.sceneDao())
        shotRepository = ShotRepository(database.shotDao())
        runBlocking {
            projectRepository.createProject("Asset Library Test").getOrThrow()
            assetRepository.saveCharacterAsset(PROJECT_ID, sampleCharacter())
            assetRepository.saveLocationAsset(PROJECT_ID, sampleLocation())
            assetRepository.saveObjectAsset(PROJECT_ID, sampleObject())
            // رفع G22 ممیزی post-Unit16: یک Scene+Shot واقعی که به char_lib_001
            // ارجاع می‌دهد — برای اثبات Rule واقعی «Asset در حال استفاده قابل حذف
            // نیست» (validateAssetDeletion). loc_lib_001/obj_lib_001 عمداً بدون
            // ارجاع می‌مانند تا مسیر موفق حذف هم قابل‌تست باشد.
            sceneRepository.saveScene(PROJECT_ID, sampleScene())
            shotRepository.saveShot(sampleShotUsingCharacter())
        }

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    assetRepository = assetRepository,
                    shotRepository = shotRepository
                )
            }
        }
    }

    @After
    fun tearDown() {
        composeRule.waitForIdle()
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        // database.close() عمداً حذف شد — ریشه‌ی واقعی Flake تاریخی این Suite
        // (docs/adr/070-flake-root-cause-investigation.md، رفع در ADR-071):
        // Room.inMemoryDatabaseBuilder نیازی به Close صریح ندارد (بدون فایل روی
        // دیسک، GC آن را با نابودی نمونه‌ی این کلاس تست جمع می‌کند)؛ این خط قبلاً
        // با Coroutine های ناتمام viewModelScope روی Executor داخلی Room مسابقه
        // می‌داد — نه یک نشتی حافظه‌ی فراموش‌شده. waitForIdle() بالا همچنان برای
        // Idling خودِ Compose مفید است، فقط دیگر ایمنی close() را تضمین نمی‌کند.
    }

    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    private fun sampleScene() = Scene(
        sceneId = "scene_asset_lib_test",
        sceneTitle = "Bridge Scene",
        sceneNumber = 1,
        narrativeRole = NarrativeRole.INTRODUCTION,
        location = SceneLocation(type = SceneLocationType.INDOOR, description = "the bridge"),
        timeOfDay = TimeOfDay.DAWN,
        atmospherePrimary = Atmosphere.CALM
    )

    private fun sampleShotUsingCharacter() = Shot(
        shotId = "shot_asset_lib_test",
        sceneId = "scene_asset_lib_test",
        shotNumber = 1,
        shotTitle = "Captain gives orders",
        shotDescription = "the captain addresses the crew",
        shotGoal = ShotGoal.ESTABLISHING,
        shotType = ShotType.WIDE,
        durationSeconds = 5f,
        motionLevel = MotionLevel.STATIC,
        soundProfile = SoundProfile(enabled = false),
        characterIds = listOf("char_lib_001")
    )

    private fun sampleCharacter() = CharacterAsset(
        assetId = "char_lib_001",
        characterTier = CharacterTier.MAIN,
        name = "Captain Amelia",
        physicalAppearance = PhysicalAppearance(ageRange = "30-35", gender = Gender.FEMALE),
        outfits = listOf(Outfit("outfit_01", "Uniform", "navy captain's uniform", isDefault = true)),
        basePrompt = "a confident sea captain",
        continuityRules = ContinuityRules()
    )

    private fun sampleLocation() = LocationAsset(
        assetId = "loc_lib_001",
        name = "Bridge of the Ship",
        description = "the ship's command bridge",
        environment = Environment(type = "indoor", size = "medium", lightingCondition = "bright"),
        locationType = LocationType.INDOOR,
        basePrompt = "a busy ship bridge"
    )

    private fun sampleObject() = ObjectAsset(
        assetId = "obj_lib_001",
        name = "Brass Compass",
        description = "an antique brass compass",
        subtype = ObjectSubtype.PERSONAL_PROP,
        size = "small",
        materialAndColor = "brass, gold",
        basePrompt = "an antique brass compass"
    )

    private fun openAssetsScreen() {
        composeRule.onNodeWithTag(BOTTOM_NAV_ASSETS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
    }

    @Test
    fun `Assets screen defaults to the Characters filter and shows the saved character with its tier sub-filter`() {
        openAssetsScreen()

        composeRule.waitUntilExactlyOneExists(hasText("Captain Amelia"), timeoutMillis = 5_000)
        // متن زیرفیلتر «کاراکتر اصلی» عمداً همان متن Badge سطح روی کارت متناظرش هم
        // هست (طبق طراحی)، پس هدف‌گیری با testTag (نه متن، که Ambiguous است).
        composeRule.onNodeWithTag("assetLibrary.subfilter.characterTier.MAIN").assertIsDisplayed()
    }

    @Test
    fun `switching the segmented filter to Locations shows the location sub-filter and the saved location`() {
        openAssetsScreen()

        composeRule.onNodeWithTag(ASSET_FILTER_LOCATIONS_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasText("Bridge of the Ship"), timeoutMillis = 5_000)
        composeRule.onNodeWithTag("assetLibrary.subfilter.locationType.INDOOR").assertIsDisplayed()
        composeRule.onNodeWithText(locationTypeLabel(LocationType.OUTDOOR, Language.FA)).assertIsDisplayed()
    }

    @Test
    fun `switching the segmented filter to Objects shows the object sub-filter and the saved object`() {
        openAssetsScreen()

        composeRule.onNodeWithTag(ASSET_FILTER_OBJECTS_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasText("Brass Compass"), timeoutMillis = 5_000)
        composeRule.onNodeWithTag("assetLibrary.subfilter.objectSubtype.PERSONAL_PROP").assertIsDisplayed()
    }

    // رفع G22 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/062-...):
    // تا این قدم Delete Asset اصلاً از UI قابل‌دسترس نبود.

    @Test
    fun `deleting a character that is used in a real shot is blocked with a real message, the character stays in the list`() {
        openAssetsScreen()
        composeRule.waitUntilExactlyOneExists(hasText("Captain Amelia"), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(assetCardMenuButtonTag("char_lib_001")).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(assetCardDeleteMenuItemTag("char_lib_001")), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(assetCardDeleteMenuItemTag("char_lib_001")).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.delete.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(ASSET_LIBRARY_DELETE_CONFIRM_BUTTON_TAG).performClick()

        // Blocking واقعی (validateAssetDeletion، AssetValidation.kt:57) — متن دقیق
        // پیام واقعی، نه یک رشته‌ی جعلی تست.
        composeRule.waitUntilExactlyOneExists(hasText("قابل حذف نیست", substring = true), timeoutMillis = 5_000)
        composeRule.onNodeWithText("Captain Amelia").assertExists()
        // اثبات مستقیم دیتابیس: Shot استفاده‌کننده هنوز دست‌نخورده است — یعنی Rule
        // واقعاً جلوی حذف را گرفت، نه فقط پیام نمایش داد.
        assertEquals(listOf("char_lib_001"), runBlocking { shotRepository.loadShot("shot_asset_lib_test").getOrNull()?.characterIds })
    }

    @Test
    fun `cancelling deletion of an unused location leaves it untouched, confirming actually removes it from the list`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_FILTER_LOCATIONS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Bridge of the Ship"), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(assetCardMenuButtonTag("loc_lib_001")).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(assetCardDeleteMenuItemTag("loc_lib_001")), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(assetCardDeleteMenuItemTag("loc_lib_001")).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.delete.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithText(uiString("assetLibrary.delete.cancel", Language.FA)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bridge of the Ship").assertExists()

        composeRule.onNodeWithTag(assetCardMenuButtonTag("loc_lib_001")).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(assetCardDeleteMenuItemTag("loc_lib_001")), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(assetCardDeleteMenuItemTag("loc_lib_001")).clickViaSemantics()
        composeRule.waitUntilExactlyOneExists(hasTestTag(ASSET_LIBRARY_DELETE_CONFIRM_BUTTON_TAG), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(ASSET_LIBRARY_DELETE_CONFIRM_BUTTON_TAG).performClick()

        composeRule.waitUntilDoesNotExist(hasText("Bridge of the Ship"), timeoutMillis = 5_000)
    }
}
