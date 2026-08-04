package com.operaboys.cinemashotgenerator.ui.story

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
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
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import com.operaboys.cinemashotgenerator.domain.dna.Mood
import com.operaboys.cinemashotgenerator.domain.outputdelivery.Language
import com.operaboys.cinemashotgenerator.domain.story.Genre
import com.operaboys.cinemashotgenerator.ui.home.CREATE_PROJECT_NAME_FIELD_TAG
import com.operaboys.cinemashotgenerator.ui.i18n.uiString
import com.operaboys.cinemashotgenerator.ui.navigation.BOTTOM_NAV_HOME_TAG
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

// واحد ۱۶ فاز ۲ — قدم ۱: تست end-to-end واقعی Story Tab — طبق دستور کار:
// (۱) پر کردن فرم → بستن و بازکردن مجدد صفحه → داده باقی می‌ماند (Regression مستقیم
// برای همان کلاس باگ که در docs/adr/045-...md اشاره شد: StoryContext قبل از این قدم
// هیچ مسیر Persist ای نداشت)؛ (۲) اعتبارسنجی‌های StoryValidation.kt (حداقل یک Rule
// Blocking، یک Warning) در UI به‌درستی نمایش داده می‌شوند.
//
// «بستن و بازکردن مجدد صفحه» با خروج واقعی از Studio (نه فقط رفتن به یک Tab دیگر) و
// ورود دوباره از کارت پروژه شبیه‌سازی شده — چون این باعث می‌شود NavBackStackEntry
// (و در نتیجه StoryViewModel/ViewModelStore متناظرش) واقعاً از نو ساخته شود و
// StoryContext واقعاً از Room دوباره بارگذاری شود (نه صرفاً از یک State هنوز زنده در
// حافظه خوانده شود).
//
// یافته‌ی چهارم (تشخیص با dump کامل درخت Semantics قبل/بعد کلیک): `performClick()`
// (تزریق لمس واقعی با مختصات محاسبه‌شده از Layout) روی `FilterChip`/آیتم‌های
// `ExposedDropdownMenu` این صفحه به‌طور قابل‌تکرار «موفق» گزارش می‌شود اما وضعیت
// واقعی (Selected) هرگز تغییر نمی‌کند — با اینکه گره در محدوده‌ی Viewport و
// Actions=[OnClick] واقعی دارد. جایگزینی مستقیم با `performSemanticsAction`
// (فراخوانی مستقیم Action ثبت‌شده‌ی OnClick، بدون شبیه‌سازی لمس مبتنی بر مختصات) —
// یک API رسمی و واقعی Compose Testing، نه Workaround — این مشکل را قطعی و تکرارپذیر
// حل کرد؛ برای همه‌ی تعامل‌های این فایل با عناصر داخل Story Tab استفاده شده.
//
// یافته‌ی پنجم (باگ واقعی معماری، نه فقط تست — کشف‌شده دقیقاً با همین تست): تست اول
// این فایل، حتی بعد از رفع یافته‌ی چهارم، همچنان بعد از «بستن و بازکردن مجدد» Timeout
// می‌خورد. بررسی مستقیم دیتابیس (`database.storyDao().loadStoryContextForProject`)
// نشان داد نوشتن هرگز به دیتابیس In-Memory تست نرسیده بود — چون `StoryViewModel`
// (ساخته‌شده با `viewModel(factory=...)` داخل `StoryTabContent`) بدون پارامتر
// Repository تزریق‌شده، از `AppDatabase.getInstance(application)` (Singleton واقعی
// دستگاه) استفاده می‌کرد، نه دیتابیس In-Memory این تست — یعنی نوشتن‌ها بی‌صدا موفق
// می‌شدند اما به دیتابیس اشتباهی می‌رفتند. رفع شد با تزریق `storyRepository` از
// همین‌جا تا `MainScaffold`→`AppNavHost`→`StudioShell`→`StoryTabContent`→
// `StoryViewModel.factory` (هم‌الگو با `workflowViewModel`/`projectListViewModel`
// موجود). جزئیات کامل در docs/adr/045-unit16-phase2-step1-story-tab.md.

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoryTabFlowTest {

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
        dataStoreFileName = "story_flow_test_prefs_" + UUID.randomUUID().toString().replace("-", "")
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
            repository = ProjectRepository(database.projectDao(), idProvider = { "proj_story_flow_test" })
        )

        composeRule.setContent {
            CinemaShotGeneratorTheme(darkTheme = true, language = Language.FA) {
                MainScaffold(
                    workflowViewModel = workflowViewModel,
                    projectListViewModel = projectListViewModel,
                    storyRepository = StoryRepository(database.storyDao(), database.storyBreakdownSessionDao())
                )
            }
        }
    }

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(dataStoreFileName).delete()
        database.close()
    }

    private fun createProjectAndEnterStudio(name: String) {
        composeRule.onNodeWithText(uiString("home.newProjectTitle", Language.FA)).performClick()
        composeRule.onNodeWithTag(CREATE_PROJECT_NAME_FIELD_TAG).performTextInput(name)
        composeRule.onNodeWithText(uiString("project.rename.confirm", Language.FA)).performClick()
        // ایجاد سریع خودکار وارد Studio همان پروژه می‌شود (Tab «داستان» پیش‌فرض). حداقل
        // یک گره (نه دقیقاً یک گره) — چون هم عنوان Header استودیو و هم فیلد عنوان Tab
        // داستان، هر دو همین نام را نمایش می‌دهند (Ambiguous برای Exactly-One).
        composeRule.waitUntilAtLeastOneExists(hasText(name), timeoutMillis = 5_000)
    }

    /** طبق یافته‌ی چهارم بالا — فراخوانی مستقیم Action «OnClick» به‌جای تزریق لمس مبتنی بر مختصات. */
    private fun SemanticsNodeInteraction.clickViaSemantics(): SemanticsNodeInteraction {
        performScrollTo()
        performSemanticsAction(SemanticsActions.OnClick)
        return this
    }

    @Test
    fun `toggling a genre in the Story tab persists across leaving and re-entering Studio for the same project`() {
        createProjectAndEnterStudio("Story Persist Test")

        val actionLabel = genreLabel(Genre.ACTION, Language.FA)
        composeRule.onNodeWithText(actionLabel).clickViaSemantics()
        composeRule.onNodeWithText(actionLabel).assertIsSelected()

        // «بستن و بازکردن مجدد صفحه»: برگشت واقعی به Home و ورود دوباره از کارت پروژه
        // — یک NavBackStackEntry/StoryViewModel کاملاً تازه می‌سازد.
        composeRule.onNodeWithTag(BOTTOM_NAV_HOME_TAG).performClick()
        composeRule.onNodeWithText("Story Persist Test").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeRule.onNodeWithText(actionLabel).assertIsSelected() }.isSuccess
        }
    }

    @Test
    fun `Story tab shows a Blocking issue for empty genre and a Warning issue for an unusual genre-mood combination`() {
        createProjectAndEnterStudio("Story Validation Test")

        // Rule 1 (Blocking): ژانر پیش‌فرض خالی است — بدون هیچ تعاملی باید نمایش داده شود.
        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("story.validationBlockingHeader", Language.FA)),
            timeoutMillis = 5_000
        )

        // Rule 3 (Warning): ترکیب Horror + Hopeful غیرمعمول است.
        composeRule.onNodeWithText(genreLabel(Genre.HORROR, Language.FA)).clickViaSemantics()
        composeRule.onNodeWithText(uiString("story.moodPrimaryLabel", Language.FA)).clickViaSemantics()
        composeRule.onNodeWithText(moodLabel(Mood.HOPEFUL, Language.FA)).clickViaSemantics()

        composeRule.waitUntilExactlyOneExists(
            hasText(uiString("story.validationWarningHeader", Language.FA)),
            timeoutMillis = 5_000
        )
    }
}
