plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
    id("kotlin-kapt")

}

android {
    namespace = "com.example.fitkagehealth"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fitkagehealth"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "EXERCISE_API_KEY", "\"7a0dbad340mshdcec17747bd62bdp159b03jsn0afd3af6d28b\"")
        buildConfigField("String", "EXERCISE_API_HOST", "\"exercisedb.p.rapidapi.com\"")
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"d9b491b077c542b8ba09e8629a67a3b8\"")
        buildConfigField("String", "SPOONACULAR_BASE_URL", "\"https://api.spoonacular.com/\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- AndroidX / Material ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation("me.zhanghai.android.materialratingbar:library:1.4.0")
    implementation("com.github.Baseflow:PhotoView:2.3.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    implementation("com.google.android.material:material:1.9.0")
    // --- Room ---
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    implementation(libs.play.services.contextmanager)

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

    kapt("androidx.room:room-compiler:2.8.3")

    implementation("com.google.android.gms:play-services-location:21.0.1")
// Use BOM to manage Firebase versions from a single place
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))

// Firebase libraries (do NOT add versions; BOM controls those versions)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("androidx.cardview:cardview:1.0.0")
    // --- Gson ---
    implementation("com.google.code.gson:gson:2.10.1")

    // --- Firebase ---

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    kapt ("com.github.bumptech.glide:compiler:4.16.0")

    implementation("de.hdodenhof:circleimageview:3.1.0")


    implementation(libs.firebase.ai)
    implementation(libs.androidx.biometric)
    implementation("androidx.core:core-splashscreen:1.0.0")
    // --- Navigation ---
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // --- Google Sign-In ---
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // --- Jetpack Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.animation.core.android)
    implementation(libs.androidx.compose.ui)

    // --- Debug / Testing ---
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // --- Utilities ---
    implementation("javax.inject:javax.inject:1")
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // --- Glide ---
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Add this so the annotation processor can read new Kotlin metadata
    kapt("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.6.0")

}
