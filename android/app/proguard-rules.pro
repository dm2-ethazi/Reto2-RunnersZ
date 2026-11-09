# CipherSprint: ofuscación de release con R8.
# El token no se guarda como literal: se reconstruye desde fragmentos enmascarados.

-repackageclasses 'com.runnersz.reto2.android.a'
-allowaccessmodification
-overloadaggressively
-renamesourcefileattribute SourceFile

# Elimina trazas de depuración del binario de release.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Componentes declarados en el manifiesto.
-keep public class com.runnersz.reto2.android.SplashActivity { *; }
-keep public class com.runnersz.reto2.android.MainActivity { *; }

# OkHttp y plataforma.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
