# RunnersZ / CipherSprint — ProGuard rules (Contrato Técnico v2.0)
# Ofuscación para dificultar ingeniería inversa (nivel alto de seguridad)

-keep class com.runnersz.reto2.android.** { *; }
-keepnames class com.runnersz.reto2.android.** { *; }
-keepmembernames class com.runnersz.reto2.android.** { *; }

# Retrofit + Gson + OkHttp
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.** { *; }
-keep class com.squareup.retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# AndroidX Security (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }

# AndroidX
-keep class androidx.** { *; }

# Preserve entry points
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Preserve Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# No ofuscar modelos de datos de la API
-keep class com.runnersz.reto2.android.api.{ *; }

-dontwarn androidx.security.**
-dontwarn com.squareup.okhttp3.**
-dontwarn com.squareup.retrofit2.**
-dontwarn okio.**
