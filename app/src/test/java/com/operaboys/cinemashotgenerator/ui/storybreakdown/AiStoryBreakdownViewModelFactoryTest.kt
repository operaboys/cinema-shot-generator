package com.operaboys.cinemashotgenerator.ui.storybreakdown

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import com.operaboys.cinemashotgenerator.data.repository.StoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع G16 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/063-...):
// factory قبلاً با `if (args.size == 4)` رفتار همه‌یا‌هیچ داشت — اگر فقط ۱ تا ۳ از ۴
// Repository داده می‌شد، هر ۴ تا (حتی آن‌هایی که واقعاً تزریق شده بودند) بی‌صدا دور
// ریخته می‌شدند. این تست دقیقاً همان سناریو را بررسی می‌کند: فقط ۲ از ۴ (storyRepository،
// assetRepository) تزریق می‌شوند و باید واقعاً استفاده شوند — نه اینکه به‌خاطر نبودن
// sceneRepository/shotRepository، کل چهارتا نادیده گرفته شوند.
//
// روش اثبات: هم‌الگو با ProjectListViewModelTest.kt (G15) — یک دیتابیس In-Memory
// جدا (injectedDatabase) هرگز به Singleton سراسری AppDatabase.getInstance متصل
// نمی‌شود؛ اگر تزریق واقعاً کار کند، نتیجه‌ی confirmAndSave در injectedDatabase
// دیده می‌شود، نه در Singleton سراسری (که در این فرآیند تست خالی می‌ماند).
// sceneRepository/shotRepository عمداً null گذاشته می‌شوند (باید Fallback به
// Singleton کنند) — برای این‌که تزریق دو موردی بدون تداخل FK دو دیتابیس متفاوت
// (Scene باید در همان دیتابیسی باشد که Shot به آن رجوع می‌کند) بررسی شود، JSON این
// تست عمداً "shots": [] است — پس confirmAndSave حلقه‌ی scene/shot را اصلاً اجرا
// نمی‌کند و آن دو Repository (چه Fallback چه نه) هرگز لمس نمی‌شوند؛ فقط
// assetRepository (تزریقی) واقعاً محک زده می‌شود.

private const val PROJECT_ID = "proj_breakdown_factory_test"

private const val CHARACTERS_ONLY_JSON = """
{
  "characters": [ {"name":"Nora","description":"A cartographer","role":"main","gender":"female"} ],
  "locations": [ {"name":"Harbor","description":"A foggy harbor"} ],
  "objects": [],
  "shots": []
}
"""

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiStoryBreakdownViewModelFactoryTest {

    private lateinit var injectedDatabase: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    @Test
    fun `factory with only 2 of 4 repositories injected actually uses them instead of discarding all 4`() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val injectedStoryRepository = StoryRepository(injectedDatabase.storyDao(), injectedDatabase.storyBreakdownSessionDao())
        val injectedAssetRepository = AssetRepository(injectedDatabase.assetDao())

        val viewModel = AiStoryBreakdownViewModel.factory(
            application = application,
            projectId = PROJECT_ID,
            storyRepository = injectedStoryRepository,
            assetRepository = injectedAssetRepository,
            // sceneRepository/shotRepository عمداً تزریق نمی‌شوند — باید Fallback کنند.
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        ).create(AiStoryBreakdownViewModel::class.java)

        viewModel.setFreeformStory("A long story about a cartographer at a foggy harbor.")
        viewModel.setCurrentChunkInput(CHARACTERS_ONLY_JSON)
        viewModel.processResponse()

        assertEquals(
            "انتظار می‌رفت CHARACTERS_ONLY_JSON با موفقیت Parse شود و فاز به FINAL_REVIEW برود",
            BreakdownPhase.FINAL_REVIEW,
            viewModel.phase.value
        )

        viewModel.confirmAndSave().join()

        val savedAssets = injectedDatabase.assetDao().getAssetsForProject(PROJECT_ID).first()
        assertEquals(
            "اگر assetRepository تزریقی درست استفاده شود، Nora (character) و Harbor (location) باید در injectedDatabase ذخیره شده باشند",
            2,
            savedAssets.size
        )
    }
}
