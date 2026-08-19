package com.operaboys.cinemashotgenerator.ui.assets

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.operaboys.cinemashotgenerator.data.AppDatabase
import com.operaboys.cinemashotgenerator.data.repository.AssetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// سیستم Preview دوزبانه‌ی پرامپت — قدم ۳ از ۳ زیرقدم، پایانی (ADR-123): اولین
// تست ViewModel-level این کلاس — فقط برای فیلد تازه‌ی descriptionFaPreview،
// هم‌الگو دقیق با CharacterAssetFormViewModelTest.kt/LocationAssetFormViewModelTest.kt.

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ObjectAssetFormViewModelTest {

    private lateinit var injectedDatabase: AppDatabase

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        injectedDatabase = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        injectedDatabase.close()
    }

    private fun <T> awaitCondition(flow: StateFlow<T>, timeoutMs: Long = 10_000, predicate: (T) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate(flow.value) && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
        assertTrue("condition was never met within ${timeoutMs}ms, last value=${flow.value}", predicate(flow.value))
    }

    @Test
    fun `save persists a non-null descriptionFaPreview exactly as entered`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = ObjectAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_object_fa_preview_test",
            repository = repository,
            idProvider = { "obj_fa_preview_set" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        // هم‌الگو دقیق با یافته‌ی دیباگ CharacterAssetFormViewModelTest.kt: canSave
        // (که خودش validationIssues را هم Combine می‌کند، Rule 10 Blocking) روی
        // viewModelScope واقعی ساخته می‌شود — بدون Collector واقعی + idle صریح،
        // save() بی‌صدا no-op می‌ماند.
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        vm.setName("Service Pistol")
        vm.setSize("small")
        vm.setMaterialAndColor("worn black metal")
        vm.setDescriptionFaPreview("یک هفت‌تیر کهنه")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted) { it }

        val loaded = repository.loadObjectAssets(listOf("obj_fa_preview_set")).getOrThrow().single()
        assertEquals("یک هفت‌تیر کهنه", loaded.descriptionFaPreview)
    }

    @Test
    fun `save leaves descriptionFaPreview null when the field was never filled in`() = runBlocking {
        val repository = AssetRepository(injectedDatabase.assetDao())
        val vm = ObjectAssetFormViewModel(
            application = ApplicationProvider.getApplicationContext(),
            projectId = "proj_object_fa_preview_test",
            repository = repository,
            idProvider = { "obj_fa_preview_null" },
            ioScopeOverride = CoroutineScope(Dispatchers.Unconfined)
        )
        CoroutineScope(Dispatchers.Unconfined).launch { vm.canSave.collect {} }
        vm.setName("Service Pistol")
        vm.setSize("small")
        vm.setMaterialAndColor("worn black metal")
        shadowOf(Looper.getMainLooper()).idle()

        vm.save()
        awaitCondition(vm.saveCompleted) { it }

        val loaded = repository.loadObjectAssets(listOf("obj_fa_preview_null")).getOrThrow().single()
        assertNull(loaded.descriptionFaPreview)
    }
}
