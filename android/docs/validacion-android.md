# Validación Android
Autor: Jairo. Fecha: 2026-10-30. Tarea #42, rama feature/android-v2.

## Ejecución local
Windows 11, JDK 21, Gradle Wrapper 8.9, SDK 35.
Comando: gradlew.bat testDebugUnitTest assembleDebug jacocoTestReport jacocoCoverageVerification
Resultado: BUILD SUCCESSFUL.
Tests: 47. Fallos: 0. Errores: 0.
Líneas cubiertas: 301; sin cubrir: 17.
Cobertura global: 94.65 %. Umbral bloqueante: 80 %.
Solo se excluyen recursos, bindings y BuildConfig generados; UI incluida.
Se realizó una compilación limpia para retirar artefactos v1 anteriores.

## Emulador y entregable
Pixel 8a conectado mediante adb. Instalación APK y apertura MainActivity verificadas.
Capturas: evidencias/ajustes.png y evidencias/orden.png.
APK debug: app/build/outputs/apk/debug/app-debug.apk.
Copia para probar: Escritorio/RunnersZ-Pruebas/runnersz-android-v2-debug.apk.
SHA-256: 5AC6DAB7C775509A37BD260ED98018E5F23DB253EF94296A1DA42D3E8260DE0F

## Pruebas cubiertas
Configuración, autenticación HTTP 204/401/500, conexión y timeout, Bearer,
multipart y clave, X-Integrity, JSON César, desplazamientos, errores,
PNG válido/inválido, recepción TXT, intents, preferencias y pantallas.

## Límites
Tests HTTP contra MockWebServer; no equivalen a una prueba con el backend real.
Pendientes externos: móvil físico, TalkBack manual y demo con API del equipo.
GitHub Actions y revisión cruzada se verifican por separado tras publicar.
No se afirma integración en develop ni aprobación de Alain.