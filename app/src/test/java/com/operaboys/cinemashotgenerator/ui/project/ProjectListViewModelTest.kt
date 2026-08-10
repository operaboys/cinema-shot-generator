package com.operaboys.cinemashotgenerator.ui.project

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// رفع G15 ممیزی post-Unit16 (docs/audit/post-unit16-full-audit.md، docs/adr/063-...):
// تست مستقیم ProjectListViewModel (نه از طریق Compose) — این ViewModel تا این قدم
// اصلاً تست مستقلی نداشت (طبق یافته‌ی خودِ ممیزی). هدف اصلی: اثبات اینکه
// exportProject واقعاً از database تزریقی استفاده می‌کند، نه Singleton سراسری
// AppDatabase.getInstance — با seed کردن پروژه فقط در یک دیتابیس In-Memory جدا
// (نه در Singleton سراسری اپ)، و انتظار موفقیت export فقط اگر آن database
// تزریقی واقعاً خوانده شده باشد.

private const val PROJECT_ID = "proj_export_test"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectListViewModelTest {

    private lateinit var injectedDatabase: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        runBlocking {
            ProjectRepository(injectedDatabase.projectDao(), idProvider = { PROJECT_ID }).createProject("Export Test").getOrThrow()
        }
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    @Test
    fun `exportProject reads from the injected database, not the global Singleton`() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Application>()
        // پروژه‌ی seed‌شده در setUp فقط در injectedDatabase وجود دارد — هرگز در
        // Singleton سراسری AppDatabase.getInstance(application) ذخیره نشده. اگر
        // exportProject هنوز (طبق باگ قبلی) مستقیماً آن Singleton را می‌خواند،
        // serializeFullProject با «پروژه یافت نشد» شکست می‌خورد و
        // lastActionMessage پر می‌شود؛ اگر database تزریقی واقعاً استفاده شود،
        // export موفق است و lastActionMessage خالی می‌ماند.
        val viewModel = ProjectListViewModel(
            application = application,
            repository = ProjectRepository(injectedDatabase.projectDao()),
            database = injectedDatabase,
            // طبق یافته‌ی مستند OutputDeliveryViewModelTest، Dispatchers.Unconfined
            // به‌تنهایی کافی نیست چون DAO های Room به Executor داخلی خودشان hop
            // می‌کنند — به همین دلیل exportProject اکنون Job برمی‌گرداند و اینجا
            // .join() می‌شود تا واقعاً قبل از خواندن lastActionMessage کامل شود.
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )

        viewModel.exportProject(PROJECT_ID).join()

        assertNull(
            "اگر database تزریقی درست کار کند، export باید موفق شود و lastActionMessage خالی بماند",
            viewModel.lastActionMessage.value
        )
    }
}
