import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.operaboys.cinemashotgenerator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.operaboys.cinemashotgenerator"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // یافته‌ی ۳ appendix ADR-081 (ADR-083): رشته‌ی نسخه‌ی واقعی (کارت درباره،
        // Settings) به BuildConfig.VERSION_NAME نیاز دارد — AGP جدید این را
        // به‌صورت پیش‌فرض خاموش می‌کند.
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // material-icons-extended (نه -core): -core فقط ~۲۴ آیکون اصلی دارد و آیکون‌های
    // لازم برای نوار پایین (Folder/Movie/Apps) را ندارد. افزایش حجم APK ناشی از این
    // کتابخانه (چند هزار Vector Icon) شناخته‌شده است؛ isMinifyEnabled این پروژه فعلاً
    // false است (build.gradle.kts:۲۵)، پس این افزایش حجم فعلاً در Release هم اعمال
    // می‌شود — فعال‌کردن R8/Minify (که خودش خارج از Scope این فاز است) در آینده این
    // اندازه را به آیکون‌های واقعاً استفاده‌شده کوچک خواهد کرد.
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
