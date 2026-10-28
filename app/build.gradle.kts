import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// İmza bilgileri: CI'da ortam değişkenlerinden (GitHub Secrets), yerelde ise
// git'e girmeyen keystore.properties dosyasından okunur. Kodda sır bulunmaz.
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun gizli(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(prop)

// Sürüm kodu CI'daki çalıştırma numarasından gelir; yerel derlemelerde 1'dir.
val surumKodu = (System.getenv("YTU_VERSION_CODE") ?: "1").toInt()
val githubRepo = providers.gradleProperty("ytumenu.githubRepo").get()

android {
    namespace = "com.kampuslezzet.ytumenu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kampuslezzet.ytumenu"
        minSdk = 26
        targetSdk = 35
        versionCode = surumKodu
        versionName = "1.0.$surumKodu"
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
    }

    val keystoreYolu = gizli("YTU_KEYSTORE_PATH", "storeFile")
    signingConfigs {
        if (keystoreYolu != null) {
            create("release") {
                storeFile = file(keystoreYolu)
                storePassword = gizli("YTU_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = gizli("YTU_KEY_ALIAS", "keyAlias")
                keyPassword = gizli("YTU_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)

    testImplementation(libs.junit)
}
