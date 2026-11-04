plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    // Android tarafı JVM çıktısını kullanır; iOS için yerel derlenir.
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            implementation("org.kotlincrypto.hash:sha2:0.8.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
