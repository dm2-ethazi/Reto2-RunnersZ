# Android — descifrado móvil

Kotlin, Android mínimo 26, compile/target SDK 35 y Gradle Wrapper 8.9.
La aplicación solo descifra; no requiere que el backend esté encendido.

## Compilar y probar desde la raíz

Configura JAVA_HOME al JDK 21 y ANDROID_HOME al SDK. Instala Android 35 y
Build Tools 34.0.0 desde Android Studio/SDK Manager. No uses Java 25 con este wrapper.

```powershell
.\android\gradlew.bat -p android clean testDebugUnitTest jacocoTestReport jacocoTestCoverageVerification assembleDebug
```

APK debug: app/build/outputs/apk/debug/app-debug.apk. No es un APK de producción
firmado para una tienda. Informe: app/build/reports/jacoco/jacocoTestReport/html.
JaCoCo mide todo el paquete de aplicación, incluida MainActivity, y exige >=80 %
de líneas. Las pruebas de UI se ejecutan en JVM con Android simulado API 26;
no equivalen a una ejecución en un móvil físico.

## Dependencias documentadas

- AGP 8.6.1, Kotlin 2.0.21, JUnit 4.13.2, declarados en Gradle.
- Robolectric 4.14.1, SOLO testImplementation: simula ciclo de vida, selectores,
  intents y errores de la Activity sin emulador. Artefacto comprobado en Maven
  Central; trae AndroidX Test de forma transitiva, por eso useAndroidX=true.
- La app productiva no añade dependencias AndroidX de interfaz; usa Activity
  y widgets Android, java.crypto y java.security disponibles desde API 26.

## Usar

Imagen: abre o comparte .png/.png.enc, o pulsa selector. Introduce la contraseña
AES si está cifrada y pulsa Mostrar/Descifrar. Solo muestra el bitmap después
 de verificar tags GCM y SHA-256. Un PNG sin cifrar también puede visualizarse.
Orden: abre/comparte .txt, introduce César 1..25 y pulsa Descifrar orden.
Los errores se muestran con Toast/estado; no deben cerrar la aplicación.

## Código y pruebas

MainActivity conecta intents, selector, vistas y trabajo en ExecutorService.
`core/` contiene descifrado AES en streaming, formato HGAE v1, César y detección
 de archivos, separados de Android y testeables en JVM.
MainActivityTest prueba navegación, compartir/abrir/seleccionar, clave vacía,
clave incorrecta, imagen corrupta, orden válida y desplazamientos inválidos.
AesInteropTest usa vectores reales del backend, incluido el caso en que el tag
GCM es válido pero la huella almacenada es incorrecta.

Vectores y contraseña pública SOLO de prueba en src/test/resources/vectors.
Se regeneran desde la raíz con el Python preparado y scripts/generate_vectors.py.
El temporal se crea en caché privada y se publica sin reemplazo mediante enlace
duro; si el sistema de archivos no lo admite, falla sin recurrir a sobrescritura.

Prueba también en móvil real las concesiones URI de otros proveedores de archivos,
rotación/ciclo de vida y compartir desde distintas apps. Los tests de JVM no
certifican por sí solos ese comportamiento ni todas las versiones Android.
