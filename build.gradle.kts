import com.correla.waypoint.WaypointBuildType
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.waypoint.android.application)
    alias(libs.plugins.waypoint.android.application.compose)
    alias(libs.plugins.waypoint.android.application.flavors)
    alias(libs.plugins.waypoint.android.application.jacoco)
    alias(libs.plugins.waypoint.android.hilt)
    alias(libs.plugins.waypoint.android.application.firebase)
    id("com.google.android.gms.oss-licenses-plugin")
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.sentry)
    alias(libs.plugins.secrets)
}

val keyStorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keyStorePropertiesFile.exists()) {
        load(FileInputStream(keyStorePropertiesFile))
    }
}

android {
    namespace = "com.correla.waypoint"

    signingConfigs {
        val storeFilePath =
            keystoreProperties["STORE_FILE"] as String? ?: System.getenv("STORE_FILE")
        val storePassword =
            keystoreProperties["STORE_PASSWORD"] as String? ?: System.getenv("STORE_PASSWORD")
        val keyAlias =
            keystoreProperties["STORE_KEY_ALIAS"] as String? ?: System.getenv("STORE_KEY_ALIAS")
        val keyPassword = keystoreProperties["STORE_KEY_PASSWORD"] as String?
            ?: System.getenv("STORE_KEY_PASSWORD")

        if (!storeFilePath.isNullOrEmpty() &&
            !storePassword.isNullOrEmpty() &&
            !keyAlias.isNullOrEmpty() &&
            !keyPassword.isNullOrEmpty()
        ) {

            create("release") {
                storeFile = file(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        } else {
            println("Warning: Signing configuration for release is not created because some properties are missing.")
        }
    }

    defaultConfig {
        applicationId = "com.correla.waypoint"
        versionCode = 129
        versionName = "0.0.6"

        testInstrumentationRunner = "com.correla.waypoint.core.testing.WaypointTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = WaypointBuildType.DEBUG.applicationIdSuffix
        }

        val release by getting {
            isMinifyEnabled = true
            applicationIdSuffix = WaypointBuildType.RELEASE.applicationIdSuffix
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            try {
                signingConfig = signingConfigs.getByName("release")
            } catch (e: UnknownDomainObjectException) {
                println("Warning: Signing configuration for release is not found.")
            }
            // Ensure Baseline Profile is fresh for release builds.
            baselineProfile.automaticGenerationDuringBuild = true
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.feature.jobs)
    implementation(projects.feature.inventory)
    implementation(projects.feature.addasset)
    implementation(projects.feature.reportdefectiveasset)
    implementation(projects.feature.sendassettoengineer)
    implementation(projects.feature.returnasset)
    implementation(projects.feature.collectionorders)
    implementation(projects.feature.vanaudit)
    implementation(projects.feature.profile)
    implementation(projects.feature.login)
    implementation(projects.feature.barcode)
    implementation(projects.feature.jobdetails)
    implementation(projects.feature.search)
    implementation(projects.feature.shared)
    implementation(projects.feature.barcodeinventory)
    implementation(projects.feature.notifications)

    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.analytics)
    implementation(projects.core.notifications)

    androidTestImplementation(projects.core.testing)
    androidTestImplementation(projects.core.datastoreTest)
    androidTestImplementation(projects.core.dataTest)
    androidTestImplementation(projects.core.network)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.accompanist.testharness)
    androidTestImplementation(kotlin("test"))
    debugImplementation(libs.androidx.compose.ui.testManifest)
    debugImplementation(projects.uiTestHiltManifest)

    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.window.manager)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.coil.kt)

    baselineProfile(projects.benchmarks)
}

baselineProfile {
    // Don't build on every iteration of a full assemble.
    // Instead enable generation directly for the release build variant.
    automaticGenerationDuringBuild = false
}

// androidx.test is forcing JUnit, 4.12. This forces it to use 4.13
configurations.configureEach {
    resolutionStrategy {
        force(libs.junit4)
        // Temporary workaround for https://issuetracker.google.com/174733673
        force("org.objenesis:objenesis:2.6")
    }
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

// Function to retrieve the Sentry Auth Token
fun getSentryAuthToken(): String {
    // Attempt to get the API key from CI/CD environment variables
    val ciApiKey: String? = System.getenv("SENTRY_AUTH_TOKEN")

    // Return the CI/CD environment variable if found
    ciApiKey?.let {
        return it
    }

    // Fall back to local.properties if environment variable is not found
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val localProperties = Properties()
        localProperties.load(FileInputStream(localPropertiesFile))
        return localProperties.getProperty("SENTRY_AUTH_TOKEN", "")
    }

    // Return an empty string if neither source has the token
    return ""
}


sentry {
    org.set("cloud-knowledge-base")
    projectName.set("waypoint-app")
    authToken.set(getSentryAuthToken())
}
