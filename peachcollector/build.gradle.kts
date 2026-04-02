plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    `maven-publish`
}

val libraryVersionCode = 1L
val libraryVersionName = "2.0.0"

android {
    namespace = "ch.ebu.peachcollector"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("long", "VERSION_CODE", "$libraryVersionCode")
        buildConfigField("String", "VERSION_NAME", "\"$libraryVersionName\"")
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    lint {
        targetSdk = 35
    }

    testOptions {
        targetSdk = 35
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.media3.common)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lifecycle.process)
    implementation(libs.okhttp)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.ebu"
                artifactId = "peachcollector"
                version = libraryVersionName
            }
        }
    }
}
