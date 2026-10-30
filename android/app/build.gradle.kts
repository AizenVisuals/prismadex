plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "dev.bloody.prismadex"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.bloody.prismadex"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.8.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    // Release signing: the upload key lives OUTSIDE the repo, in
    // local.properties (gitignored) or environment variables. Without a
    // complete configuration, release builds stay unsigned, as before.
    // Keys: prismadex.release.storeFile / .storePassword / .keyAlias /
    // .keyPassword (env: PRISMADEX_RELEASE_STORE_FILE / _STORE_PASSWORD /
    // _KEY_ALIAS / _KEY_PASSWORD). Values are read literally (no escape
    // processing); prefer environment variables for exotic passwords.
    // Never commit a keystore or passwords. Setup steps are documented
    // in the expansion final report.
    val releaseKeystoreProps: Map<String, String> = rootProject.file("local.properties")
        .takeIf { it.exists() }?.readLines().orEmpty()
        .mapNotNull { raw ->
            val line = raw.substringBefore("#").trim()
            if ("=" !in line) null
            else line.split("=", limit = 2).let { it[0].trim() to it[1].trim() }
        }.toMap()
    fun releaseSigningValue(prop: String, env: String): String? =
        releaseKeystoreProps[prop] ?: System.getenv(env)
    val releaseKeystoreFile = releaseSigningValue(
        "prismadex.release.storeFile", "PRISMADEX_RELEASE_STORE_FILE"
    )?.let { file(it) }?.takeIf { it.exists() }
    val releaseSigningReady = releaseKeystoreFile != null &&
        releaseSigningValue("prismadex.release.storePassword", "PRISMADEX_RELEASE_STORE_PASSWORD") != null &&
        releaseSigningValue("prismadex.release.keyAlias", "PRISMADEX_RELEASE_KEY_ALIAS") != null &&
        releaseSigningValue("prismadex.release.keyPassword", "PRISMADEX_RELEASE_KEY_PASSWORD") != null
    if (releaseSigningReady) {
        signingConfigs {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseSigningValue(
                    "prismadex.release.storePassword", "PRISMADEX_RELEASE_STORE_PASSWORD"
                )
                keyAlias = releaseSigningValue(
                    "prismadex.release.keyAlias", "PRISMADEX_RELEASE_KEY_ALIAS"
                )
                keyPassword = releaseSigningValue(
                    "prismadex.release.keyPassword", "PRISMADEX_RELEASE_KEY_PASSWORD"
                )
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("release")
        }
        create("preview") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            versionNameSuffix = "-preview"
            matchingFallbacks += "release"
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.mozilla:rhino:1.8.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
}
