# Validación Android
Autor: Jairo. Fecha: 2026-10-30 (re-verificada 2026-11-02). Tarea #42, rama feature/android-v2.

## Ejecución local
Windows 11, JDK 21, Gradle Wrapper 8.9, SDK 35.
Comando: gradlew.bat clean testDebugUnitTest assembleDebug jacocoTestReport jacocoCoverageVerification lintDebug
Resultado: BUILD SUCCESSFUL; Android Lint sin errores bloqueantes.
Tests: 51. Fallos: 0. Errores: 0.
Líneas cubiertas: 306; sin cubrir: 19.
Cobertura global: 94.15 %. Umbral bloqueante: 80 %.
Solo se excluyen recursos, bindings y BuildConfig generados; UI incluida.
Se realizó una compilación limpia para retirar artefactos v1 anteriores.

Re-verificación 2026-10-31 desde cero (clean): 48 tests. Correcciones ese día:
codificación UTF-8 de Imagen/Orden y mensaje DJ7 «El servidor no confirmó la
integridad» distinto de «sin conexión».
Re-verificación 2026-11-02 desde cero (clean): 51 tests. Añade los tests §5.2
de imagen: errores 400/413/500/401 en decrypt, Authorization en multipart,
subida de 20 MiB en streaming y borrado de la caché al salir de la pantalla.

## Emulador y entregable
Pixel 8a conectado mediante adb. Instalación APK y apertura MainActivity verificadas.
Capturas: evidencias/ajustes.png, evidencias/imagen.png y evidencias/orden.png.
APK debug: app/build/outputs/apk/debug/app-debug.apk.
Copia para probar: Escritorio/RunnersZ-Pruebas/runnersz-android-v2-debug.apk.
SHA-256: DDBE90460DCADD8427E70BF7AABFD7AF2EEFB97A230982956EE25D3276378311

## Pruebas cubiertas
Configuración, autenticación HTTP 204/401/500, conexión y timeout, Bearer,
multipart con Authorization y clave, X-Integrity (DJ7), JSON César,
desplazamientos, errores 400/413/422/500, PNG válido/inválido, subida de
20 MiB en streaming, borrado de caché al salir, recepción TXT, intents,
preferencias y pantallas.

## Límites
Tests HTTP contra MockWebServer; no equivalen a una prueba con el backend real.
Pendiente la integración opcional contra el servidor real con
docs/vectors/image.png.enc y la contraseña de manifest.json, cuando Alain
publique el comentario handoff en I-07 (previsto 4-5 nov).
Pendientes externos: móvil físico, TalkBack manual y demo con API del equipo.
GitHub Actions y revisión cruzada se verifican por separado tras publicar.
No se afirma integración en develop ni aprobación de Alain.