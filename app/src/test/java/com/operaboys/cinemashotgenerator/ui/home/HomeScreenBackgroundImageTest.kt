package com.operaboys.cinemashotgenerator.ui.home

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// رفع G12 باقی‌مانده (دستور کار ۲۰۲۶-۰۸-۱۳، docs/adr/080-...): homeScreenImageUri
// قبلاً فقط Persist می‌شد، هیچ‌جای Home رندر نمی‌شد. این تست ثابت می‌کند وقتی مقدار
// واقعی است، یک تصویر واقعی (نه فقط گرادیان Placeholder) در Home دیکود و نمایش
// داده می‌شود — با یک URI واقعی file:// (نه Mock/Shadow، دقیقاً همان مسیر کد
// production که ContentResolver.openInputStream/BitmapFactory.decodeStream را
// روی مقدار واقعی صدا می‌زند).

private const val PROJECT_ID = "proj_home_bg_image_test"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenBackgroundImageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreFileName: String
    private lateinit var workflowViewModel: WorkflowViewModel
    private lateinit var database: AppDatabase
    private lateinit var projectListViewModel: ProjectListViewModel
    private lateinit var imageFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreFileName = "home_bg_image_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        if (::imageFile.isInitialized) imageFile.delete()
    }

    private fun renderHomeScreen() {
        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                HomeScreen(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    onOpenDrawer = {},
                    onOpenProject = {},
                    onViewAllProjects = {},
                    onShowMessage = {}
                )
            }
        }
    }

    @Test
    fun `homeScreenImageUri set to a real file renders an actual decoded background image`() {
        imageFile = File(context.cacheDir, "home_bg_test_${UUID.randomUUID()}.png")
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        FileOutputStream(imageFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        workflowViewModel.setHomeScreenImageUri(Uri.fromFile(imageFile).toString())

        renderHomeScreen()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HOME_BACKGROUND_IMAGE_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(HOME_BACKGROUND_IMAGE_TAG).assertIsDisplayed()
    }

    @Test
    fun `no homeScreenImageUri leaves the plain gradient background — no image node rendered`() {
        renderHomeScreen()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(HOME_BACKGROUND_IMAGE_TAG).assertDoesNotExist()
    }
}
