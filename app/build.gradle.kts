import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    id("com.google.devtools.ksp")
}

val versionPropsFile = file("version.properties")

fun getOrBumpVersion(): Pair<Int, String> {
    val props = Properties()
    if (!versionPropsFile.exists()) {
        props.setProperty("versionCode", "24")
        props.setProperty("versionName", "1.24")
        versionPropsFile.createNewFile()
        FileOutputStream(versionPropsFile).use { props.store(it, "Version properties") }
    } else {
        FileInputStream(versionPropsFile).use { props.load(it) }
    }

    var code = props.getProperty("versionCode", "24").toIntOrNull() ?: 24
    var name = props.getProperty("versionName", "1.24")

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
        println(">>> [Auto-Version] Bumped POS version -> Code: $code, Name: v$name")
    }

    return Pair(code, name)
}

val (appVersionCode, appVersionName) = getOrBumpVersion()

android {
    namespace = "com.example.myapplication"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.emanager.app"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val rootKeystore = file("${rootDir}/debug.keystore")
    if (rootKeystore.exists()) {
        signingConfigs {
            create("debugConfig") {
                storeFile = rootKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            if (rootKeystore.exists()) {
                signingConfig = signingConfigs.getByName("debugConfig")
            }
        }
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

    // 1. Retrofit 2 & Gson Converter
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // 2. OkHttp 3 & Logging Interceptor
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 3. Room Database (with KSP)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // 4. Coroutines & Lifecycle ViewModel
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // 5. CameraX & Google ML Kit Barcode Scanning
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // 6. Jetpack Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 7. Firebase Auth, Firestore & Google Sign-In
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

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
        val apkSource = file("build/outputs/apk/debug/app-debug.apk")
        if (apkSource.exists()) {
            val targetDir = rootProject.file("APK_FAYLLARI")
            targetDir.mkdirs()

            // Automatically remove previous POS APK files so only the latest version remains
            targetDir.listFiles()?.filter { it.isFile && it.name.startsWith("e-Manager_POS_") && it.name.endsWith(".apk") }?.forEach { oldApk ->
                val deleted = oldApk.delete()
                println(">>> [Auto-Clean] Deleted previous POS APK: ${oldApk.name} (success: $deleted)")
            }

            val targetApk = File(targetDir, "e-Manager_POS_v${appVersionName}.apk")
            apkSource.copyTo(targetApk, overwrite = true)
            println(">>> [Auto-Copy] Successfully copied latest POS APK to: ${targetApk.name}")

            val updatesJsonFile = rootProject.file("app_updates.json")
            if (updatesJsonFile.exists()) {
                val content = updatesJsonFile.readText()
                val updated = content.replace(
                    Regex("\"pos\":\\s*\\{[^\\}]*\\}", RegexOption.DOT_MATCHES_ALL),
                    """
                    "pos": {
                        "versionCode": $appVersionCode,
                        "versionName": "$appVersionName",
                        "apkName": "${targetApk.name}",
                        "downloadUrl": "https://raw.githubusercontent.com/rbagrv/emanapps/main/APK_FAYLLARI/${targetApk.name}",
                        "releaseNotes": "POS Kassa üçün yeni versiya, təkmilləşdirilmiş çap və avtomatik yenilənmə sistemi.",
                        "forceUpdate": false
                      }
                    """.trimIndent().trim()
                )
                updatesJsonFile.writeText(updated)
                println(">>> [Auto-Update] Updated app_updates.json for POS v$appVersionName")
            }
        }
    }
}
