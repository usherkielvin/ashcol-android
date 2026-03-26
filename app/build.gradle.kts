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
        targetSdk = 36
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
    implementation(libs.swiperefreshlayout)
    // Google Sign-In
    implementation(libs.play.services.auth)
    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    // Google Plus Codes (Open Location Code) - Official Library
    implementation(libs.openlocationcode)
    // Picasso for image loading
    implementation(libs.picasso)
    // Glide for GIF support
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.shimmer)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.constraintlayout)
    // jqwik for property-based testing
    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.jqwik.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    debugImplementation(libs.fragment.testing)
    debugImplementation(libs.fragment.testing.manifest)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
