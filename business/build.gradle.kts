import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val versionPropsFile = file("version.properties")

fun getOrBumpVersion(): Pair<Int, String> {
    val props = Properties()
    if (!versionPropsFile.exists()) {
        props.setProperty("versionCode", "4")
        props.setProperty("versionName", "1.3")
        versionPropsFile.createNewFile()
        FileOutputStream(versionPropsFile).use { props.store(it, "Version properties") }
    } else {
        FileInputStream(versionPropsFile).use { props.load(it) }
    }

    var code = props.getProperty("versionCode", "4").toIntOrNull() ?: 4
    var name = props.getProperty("versionName", "1.3")

    val taskNames = gradle.startParameter.taskNames
    val isBuilding = taskNames.any { task ->
        val t = task.lowercase()
        t.contains("assemble") || t.contains("bundle") || t.contains("package") || t.contains("install") || t.contains("build")
    }

    if (isBuilding) {
        code += 1
        val parts = name.split(".")
        name = if (parts.size >= 2) {
            val major = parts[0]
            val minor = (parts[1].toIntOrNull() ?: 0) + 1
            "$major.$minor"
        } else {
            "1.$code"
        }
        props.setProperty("versionCode", code.toString())
        props.setProperty("versionName", name)
        FileOutputStream(versionPropsFile).use { props.store(it, "Auto-incremented during build") }
        println(">>> [Auto-Version] Bumped Sahibkar version -> Code: $code, Name: v$name")
    }

    return Pair(code, name)
}

val (appVersionCode, appVersionName) = getOrBumpVersion()

android {
    namespace = "com.example.business"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.emanager.business"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Retrofit 2 & Gson Converter
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp 3 & Logging Interceptor
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines & Lifecycle ViewModel
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Jetpack Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Firebase Auth & Firestore
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    doLast {
        val apkSource = file("build/outputs/apk/debug/business-debug.apk")
        if (apkSource.exists()) {
            val targetDir = rootProject.file("APK_FAYLLARI")
            targetDir.mkdirs()
            val targetApk = File(targetDir, "e-Manager_Sahibkar_v${appVersionName}.apk")
            apkSource.copyTo(targetApk, overwrite = true)
            println(">>> [Auto-Copy] Successfully copied APK to: ${targetApk.name}")
        }
    }
}

