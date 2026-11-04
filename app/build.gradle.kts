plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

android {
    namespace = "io.github.lkhanl.moneytracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.lkhanl.moneytracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Örnek veri yalnızca istenirse: ./gradlew installDebug -PdemoData=true
        // Gerçek telefona kurarken sahte işlemler gerçeklerine karışmasın.
        buildConfigField("boolean", "DEMO_DATA", (findProperty("demoData") == "true").toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.compose.ui:ui:1.12.2")
}
