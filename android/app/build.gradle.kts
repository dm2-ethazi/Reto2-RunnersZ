plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("jacoco")
}

android {
    namespace = "com.runnersz.reto2.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.runnersz.reto2.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.testLogging {
                    events("passed", "failed", "skipped")
                }
            }
        }
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources.excludes += "META-INF/LICENSE*"
    }
}

jacoco {
    toolVersion = "0.8.12"
}

dependencies {
    // DJ2: AppCompat + Material Components + Fragments (sin Compose)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // DJ4: OkHttp 4.12 como única librería de red
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // DJ10: pruebas
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation(kotlin("test"))
}

val debugClasses = files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/databinding/**")
    },
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/databinding/**")
    }
)
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(debugClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    reports { xml.required.set(true); html.required.set(true) }
}
tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("jacocoTestReport")
    classDirectories.setFrom(debugClasses)
    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    violationRules { rule { limit { counter = "LINE"; minimum = "0.80".toBigDecimal() } } }
}
