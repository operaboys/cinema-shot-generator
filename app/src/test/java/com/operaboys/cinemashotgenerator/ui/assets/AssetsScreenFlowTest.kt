package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
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
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_ASSETS_TAG
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
import com.operaboys.cinemashotgenerator.ui.navigation.PLACEHOLDER_ACTIVE_PROJECT_ID
import com.operaboys.cinemashotgenerator.ui.project.ProjectListViewModel
import com.operaboys.cinemashotgenerator.ui.theme.CinemaShotGeneratorTheme
import com.operaboys.cinemashotgenerator.ui.workflow.WorkflowViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
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
// projectId این صفحه از PLACEHOLDER_ACTIVE_PROJECT_ID می‌آید (بدون نیاز به یک ردیف
// واقعی Project — جدول assets هیچ ForeignKey ای به projects ندارد، تأییدشده با
// خواندن AssetEntity.kt). جزئیات کامل تصمیمات در
// docs/adr/048-unit16-phase3-step1-asset-library.md.

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
        projectListViewModel = ProjectListViewModel(
            application = context.applicationContext as Application,
            repository = ProjectRepository(database.projectDao(), idProvider = { "proj_asset_library_test" })
        )

        val assetRepository = AssetRepository(database.assetDao())
        runBlocking {
            assetRepository.saveCharacterAsset(PLACEHOLDER_ACTIVE_PROJECT_ID, sampleCharacter())
            assetRepository.saveLocationAsset(PLACEHOLDER_ACTIVE_PROJECT_ID, sampleLocation())
            assetRepository.saveObjectAsset(PLACEHOLDER_ACTIVE_PROJECT_ID, sampleObject())
        }

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    assetRepository = assetRepository
                )
            }
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

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
}
