package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.asset.CharacterContinuityLevel
import com.operaboys.cinemashotgenerator.domain.asset.CharacterTier
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.i18n.uiTemplate
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_ASSETS_TAG
import com.operaboys.cinemashotgenerator.ui.navigation.MainScaffold
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

// واحد ۱۶ فاز ۳ — قدم ۲ (آخرین قدم فاز ۳): تست‌های End-to-End واقعی سه فرم ساخت
// Asset — از دکمه‌ی شناور صفحه‌ی Assets تا ذخیره‌ی واقعی Room و بازگشت به لیست.
// الگوی راه‌اندازی عیناً از AssetsScreenFlowTest.kt گرفته شده. جزئیات کامل
// تصمیمات در docs/adr/049-unit16-phase3-step2-asset-forms.md.

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssetFormFlowTest {

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
        dataStoreFileName = "asset_form_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
            repository = ProjectRepository(database.projectDao(), idProvider = { "proj_asset_form_test" })
        )

        val assetRepository = AssetRepository(database.assetDao())

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

    private fun openAssetsScreen() {
        composeRule.onNodeWithTag(BOTTOM_NAV_ASSETS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
    }

    @Test
    fun `creating a character from the real form saves it and shows it back on the Asset Library list`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_LIBRARY_FAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("characterForm.title", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(CHARACTER_FORM_NAME_FIELD_TAG).performTextInput("Captain Amelia")
        composeRule.onNodeWithTag(CHARACTER_FORM_AGE_RANGE_FIELD_TAG).performTextInput("30-35")
        composeRule.onNodeWithTag(CHARACTER_FORM_SAVE_BUTTON_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.waitUntilExactlyOneExists(hasText("Captain Amelia"), timeoutMillis = 5_000)
    }

    @Test
    fun `creating a location from the real form saves it and shows it back on the Asset Library list`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_FILTER_LOCATIONS_TAG).performClick()
        composeRule.onNodeWithTag(ASSET_LIBRARY_FAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("locationForm.title", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(LOCATION_FORM_NAME_FIELD_TAG).performTextInput("Bridge of the Ship")
        composeRule.onNodeWithTag(LOCATION_FORM_DESCRIPTION_FIELD_TAG).performTextInput("the ship's command bridge")
        composeRule.onNodeWithTag(LOCATION_FORM_SAVE_BUTTON_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(ASSET_FILTER_LOCATIONS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Bridge of the Ship"), timeoutMillis = 5_000)
    }

    @Test
    fun `creating an object from the real form saves it and shows it back on the Asset Library list`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_FILTER_OBJECTS_TAG).performClick()
        composeRule.onNodeWithTag(ASSET_LIBRARY_FAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("objectForm.title", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(OBJECT_FORM_NAME_FIELD_TAG).performTextInput("Brass Compass")
        composeRule.onNodeWithTag(OBJECT_FORM_SIZE_FIELD_TAG).performTextInput("small")
        composeRule.onNodeWithTag(OBJECT_FORM_MATERIAL_FIELD_TAG).performTextInput("brass, gold")
        composeRule.onNodeWithTag(OBJECT_FORM_SAVE_BUTTON_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(ASSET_FILTER_OBJECTS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Brass Compass"), timeoutMillis = 5_000)
    }

    @Test
    fun `the object form shows a Blocking validation issue live when size is left empty`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_FILTER_OBJECTS_TAG).performClick()
        composeRule.onNodeWithTag(ASSET_LIBRARY_FAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("objectForm.title", Language.FA)), timeoutMillis = 5_000)

        // size/materialAndColor از ابتدا خالی‌اند، پس پیام Rule 10 (Blocking) باید
        // بدون هیچ تعاملی از همان لحظه‌ی باز شدن فرم در درخت UI ظاهر شود. assertExists
        // (نه assertIsDisplayed) عمداً استفاده شد — این ردیف در انتهای یک Column
        // معمولی با verticalScroll قرار دارد (نه LazyColumn)، پس صرف‌نظر از اندازه‌ی
        // Viewport پیش‌فرض Robolectric همیشه Compose می‌شود؛ چیزی که این تست واقعاً
        // باید اثبات کند «زنده بودن اعتبارسنجی» است، نه موقعیت اسکرول فعلی صفحه.
        composeRule.onNodeWithText("فیلد size برای Object/Prop الزامی است و نمی‌تواند خالی باشد").assertExists()
        composeRule.onNodeWithTag(OBJECT_FORM_SAVE_BUTTON_TAG).assertExists()
    }

    @Test
    fun `changing the character tier automatically updates the default continuity lock level shown`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_LIBRARY_FAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("characterForm.title", Language.FA)), timeoutMillis = 5_000)

        composeRule.onNodeWithText(
            uiTemplate("assetForm.continuityLockLevelFixedTemplate", Language.FA, "level" to characterContinuityLevelLabel(CharacterContinuityLevel.FULL, Language.FA))
        ).assertIsDisplayed()

        composeRule.onNodeWithTag(CHARACTER_FORM_TIER_FIELD_TAG).performClick()
        composeRule.onNodeWithText(characterTierLabel(CharacterTier.SECONDARY, Language.FA)).performClick()

        composeRule.onNodeWithText(
            uiTemplate("assetForm.continuityLockLevelFixedTemplate", Language.FA, "level" to characterContinuityLevelLabel(CharacterContinuityLevel.MEDIUM, Language.FA))
        ).assertIsDisplayed()
    }

    // رفع G7 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md): تا این قدم
    // هیچ AssetCard ای اصلاً onClick نداشت — این ۳ تست ثابت می‌کنند که (۱) لمس یک
    // کارت موجود فرم را با داده‌ی واقعی پیش‌پر می‌کند، (۲) ذخیره‌ی دوباره همان
    // شناسه‌ی موجود را به‌روزرسانی می‌کند (نه یک رکورد تازه) — با REPLACE بودن
    // OnConflictStrategy در AssetDao، اثبات «فقط یک رکورد باقی می‌ماند» دقیقاً همان
    // اثبات «شناسه تکرار نشد» است.

    @Test
    fun `clicking an existing character card opens the edit form pre-filled, and saving updates the same asset instead of creating a new one`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_LIBRARY_FAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("characterForm.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(CHARACTER_FORM_NAME_FIELD_TAG).performTextInput("Captain Amelia")
        composeRule.onNodeWithTag(CHARACTER_FORM_AGE_RANGE_FIELD_TAG).performTextInput("30-35")
        composeRule.onNodeWithTag(CHARACTER_FORM_SAVE_BUTTON_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Captain Amelia"), timeoutMillis = 5_000)

        composeRule.onNodeWithText("Captain Amelia").performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("characterForm.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(CHARACTER_FORM_NAME_FIELD_TAG).assertTextContains("Captain Amelia")
        composeRule.onNodeWithTag(CHARACTER_FORM_AGE_RANGE_FIELD_TAG).assertTextContains("30-35")

        composeRule.onNodeWithTag(CHARACTER_FORM_NAME_FIELD_TAG).performTextClearance()
        composeRule.onNodeWithTag(CHARACTER_FORM_NAME_FIELD_TAG).performTextInput("Captain Amelia Voss")
        composeRule.onNodeWithTag(CHARACTER_FORM_SAVE_BUTTON_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasText("Captain Amelia Voss"), timeoutMillis = 5_000)
        composeRule.onNodeWithText("Captain Amelia").assertDoesNotExist()
    }

    @Test
    fun `clicking an existing location card opens the edit form pre-filled, and saving updates the same asset instead of creating a new one`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_FILTER_LOCATIONS_TAG).performClick()
        composeRule.onNodeWithTag(ASSET_LIBRARY_FAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("locationForm.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(LOCATION_FORM_NAME_FIELD_TAG).performTextInput("Bridge of the Ship")
        composeRule.onNodeWithTag(LOCATION_FORM_DESCRIPTION_FIELD_TAG).performTextInput("the ship's command bridge")
        composeRule.onNodeWithTag(LOCATION_FORM_SAVE_BUTTON_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(ASSET_FILTER_LOCATIONS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Bridge of the Ship"), timeoutMillis = 5_000)

        composeRule.onNodeWithText("Bridge of the Ship").performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("locationForm.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(LOCATION_FORM_NAME_FIELD_TAG).assertTextContains("Bridge of the Ship")
        composeRule.onNodeWithTag(LOCATION_FORM_DESCRIPTION_FIELD_TAG).assertTextContains("the ship's command bridge")

        composeRule.onNodeWithTag(LOCATION_FORM_NAME_FIELD_TAG).performTextClearance()
        composeRule.onNodeWithTag(LOCATION_FORM_NAME_FIELD_TAG).performTextInput("Engine Room")
        composeRule.onNodeWithTag(LOCATION_FORM_SAVE_BUTTON_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(ASSET_FILTER_LOCATIONS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Engine Room"), timeoutMillis = 5_000)
        composeRule.onNodeWithText("Bridge of the Ship").assertDoesNotExist()
    }

    @Test
    fun `clicking an existing object card opens the edit form pre-filled, and saving updates the same asset instead of creating a new one`() {
        openAssetsScreen()
        composeRule.onNodeWithTag(ASSET_FILTER_OBJECTS_TAG).performClick()
        composeRule.onNodeWithTag(ASSET_LIBRARY_FAB_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("objectForm.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(OBJECT_FORM_NAME_FIELD_TAG).performTextInput("Brass Compass")
        composeRule.onNodeWithTag(OBJECT_FORM_SIZE_FIELD_TAG).performTextInput("small")
        composeRule.onNodeWithTag(OBJECT_FORM_MATERIAL_FIELD_TAG).performTextInput("brass, gold")
        composeRule.onNodeWithTag(OBJECT_FORM_SAVE_BUTTON_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(ASSET_FILTER_OBJECTS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Brass Compass"), timeoutMillis = 5_000)

        composeRule.onNodeWithText("Brass Compass").performClick()
        composeRule.waitUntilExactlyOneExists(hasText(uiString("objectForm.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(OBJECT_FORM_NAME_FIELD_TAG).assertTextContains("Brass Compass")
        composeRule.onNodeWithTag(OBJECT_FORM_SIZE_FIELD_TAG).assertTextContains("small")

        composeRule.onNodeWithTag(OBJECT_FORM_NAME_FIELD_TAG).performTextClearance()
        composeRule.onNodeWithTag(OBJECT_FORM_NAME_FIELD_TAG).performTextInput("Golden Compass")
        composeRule.onNodeWithTag(OBJECT_FORM_SAVE_BUTTON_TAG).performClick()

        composeRule.waitUntilExactlyOneExists(hasText(uiString("assetLibrary.title", Language.FA)), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(ASSET_FILTER_OBJECTS_TAG).performClick()
        composeRule.waitUntilExactlyOneExists(hasText("Golden Compass"), timeoutMillis = 5_000)
        composeRule.onNodeWithText("Brass Compass").assertDoesNotExist()
    }
}
