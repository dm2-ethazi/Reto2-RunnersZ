# Validación Android
Autor: Jairo. Fecha: 2026-10-30 (re-verificada 2026-10-31). Tarea #42, rama feature/android-v2.

## Ejecución local
Windows 11, JDK 21, Gradle Wrapper 8.9, SDK 35.
Comando: gradlew.bat clean testDebugUnitTest assembleDebug jacocoTestReport jacocoCoverageVerification lintDebug
Resultado: BUILD SUCCESSFUL; Android Lint sin errores bloqueantes.
Tests: 48. Fallos: 0. Errores: 0.
Líneas cubiertas: 306; sin cubrir: 19.
Cobertura global: 94.15 %. Umbral bloqueante: 80 %.
Solo se excluyen recursos, bindings y BuildConfig generados; UI incluida.
Se realizó una compilación limpia para retirar artefactos v1 anteriores.

Re-verificación 2026-10-31 desde cero (clean): mismos resultados con 48 tests.
Correcciones aplicadas ese día: codificación UTF-8 de Imagen/Orden y mensaje
DJ7 «El servidor no confirmó la integridad» distinto de «sin conexión».

## Emulador y entregable
Pixel 8a conectado mediante adb. Instalación APK y apertura MainActivity verificadas.
Capturas: evidencias/ajustes.png, evidencias/imagen.png y evidencias/orden.png.
APK debug: app/build/outputs/apk/debug/app-debug.apk.
Copia para probar: Escritorio/RunnersZ-Pruebas/runnersz-android-v2-debug.apk.
SHA-256: 17973E154EB4B845D5BDC828040C59CB88396D056652A334AF3FEF561EA84BD2

## Pruebas cubiertas
Configuración, autenticación HTTP 204/401/500, conexión y timeout, Bearer,
multipart y clave, X-Integrity, JSON César, desplazamientos, errores,
PNG válido/inválido, recepción TXT, intents, preferencias y pantallas.

## Límites
Tests HTTP contra MockWebServer; no equivalen a una prueba con el backend real.
Pendientes externos: móvil físico, TalkBack manual y demo con API del equipo.
GitHub Actions y revisión cruzada se verifican por separado tras publicar.
No se afirma integración en develop ni aprobación de Alain.