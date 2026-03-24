plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "app.hub"
    compileSdk = 36

    useLibrary("org.apache.http.legacy")

    defaultConfig {
        applicationId = "app.hub"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google Maps API Key from gradle.properties
        val mapsKey = project.findProperty("MAPS_API_KEY") as? String ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
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
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Firebase products
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // AndroidX & UI
    implementation(libs.androidx.core)
    implementation(libs.core.splashscreen)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.coordinatorlayout)
    implementation(libs.mediarouter)
    implementation(libs.fragment)
    implementation(libs.activity)
    implementation(libs.gridlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.cardview)
    implementation(libs.recyclerview)
    // SwipeRefreshLayout for pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore")
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Google Plus Codes (Open Location Code) - Official Library
    implementation("com.google.openlocationcode:openlocationcode:1.0.4")
    // Picasso for image loading
    implementation("com.squareup.picasso:picasso:2.8")
    // Glide for GIF support
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.fragment:fragment-testing:1.6.2")
    testImplementation("androidx.constraintlayout:constraintlayout:2.2.0")
    // jqwik for property-based testing
    testImplementation("net.jqwik:jqwik:1.8.2")
    testRuntimeOnly("net.jqwik:jqwik-engine:1.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}