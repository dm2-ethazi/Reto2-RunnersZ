plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace="com.runnersz.reto2.android"; compileSdk=35
 defaultConfig { applicationId="com.runnersz.reto2.android"; minSdk=26; targetSdk=35; versionCode=1; versionName="2.0.0" }
}
dependencies { implementation("androidx.core:core-ktx:1.13.1"); implementation("androidx.appcompat:appcompat:1.7.0"); implementation("com.squareup.okhttp3:okhttp:4.12.0"); testImplementation("junit:junit:4.13.2") }