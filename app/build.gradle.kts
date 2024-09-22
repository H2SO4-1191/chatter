import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}
val firebaseDbUrl: String = localProperties.getProperty("FIREBASE_DB_URL") ?: ""
val aiApiKey: String = localProperties.getProperty("AI_API_KEY") ?: ""
val chatterServerUrl: String = localProperties.getProperty("CHATTER_SERVER_URL") ?: ""
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.h2so4.chatter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.h2so4.chatter"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "FIREBASE_DB_URL", "\"$firebaseDbUrl\"")
        buildConfigField("String", "AI_API_KEY", "\"$aiApiKey\"")
        buildConfigField("String", "CHATTER_SERVER_URL", "\"$chatterServerUrl\"")
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.multidex)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.firebase.database)
    implementation(platform(libs.firebase.bom.v3210))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.moshi)
    implementation(libs.converter.scalars)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.json)
    implementation(libs.androidx.runtime)
    implementation(libs.generativeai)
    implementation (libs.okhttp)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}