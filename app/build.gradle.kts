import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Copy
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------
// 本地配置（local.properties 已被 .gitignore 排除，不进仓库）：
//   sdk.dir                      Android SDK 路径
//   release.storeFile            release 签名文件，相对项目根目录或绝对路径
//   release.storePassword        密钥库口令
//   release.keyAlias             密钥别名
//   release.keyPassword          密钥口令
//   dorm.roster.path             仓库外的真实名单 json（可选，构建时复制进 assets）
// 缺少签名配置时自动回退用 debug 签名，保证 assembleRelease 仍能产出可安装包。
// ---------------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val releaseKeystore: File? = localProperties.getProperty("release.storeFile")
    ?.takeIf { it.isNotBlank() }
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }

android {
    namespace = "com.example.dormcheck"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dormcheck"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = releaseKeystore
                storePassword = localProperties.getProperty("release.storePassword")
                keyAlias = localProperties.getProperty("release.keyAlias")
                keyPassword = localProperties.getProperty("release.keyPassword")
                // minSdk 24 起 APK Signature Scheme v2 已全覆盖，
                // AGP 对 minSdk >= 24 的包只签 v2，无需再开 v1（JAR signing）。
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            // 正式版：R8 代码压缩 + 资源裁剪，减小体积
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                // 没配签名时回退 debug 签名，保证产物可直接安装
                signingConfigs.getByName("debug")
            }
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ---------------------------------------------------------------------------
// 内置名单注入（可选）：把仓库外的真实名单文件复制进 assets，避免名单进入版本库。
// 配置方式：在 local.properties 里加一行
//     dorm.roster.path=/绝对路径/真实名单.json
// 未配置时不做任何事，assets 里有什么就用什么（没有则视为公开版，无内置数据）。
// ---------------------------------------------------------------------------
val rosterSourcePath = localProperties.getProperty("dorm.roster.path")
if (!rosterSourcePath.isNullOrBlank()) {
    val syncRosterAsset = tasks.register<Copy>("syncRosterAsset") {
        from(rosterSourcePath)
        into(layout.projectDirectory.dir("src/main/assets"))
        rename { "roster.json" }
    }
    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(syncRosterAsset)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    // DataStore：把点名结果保存在本机，重启后自动恢复
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Compose 相关版本显式锁定，保证 compileSdk 36 + AGP 8.13 可编译
    implementation("androidx.compose.ui:ui:1.9.4")
    implementation("androidx.compose.ui:ui-graphics:1.9.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.4")
    implementation("androidx.compose.foundation:foundation:1.9.4")
    implementation("androidx.compose.material3:material3:1.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling:1.9.4")

    testImplementation("junit:junit:4.13.2")
}
