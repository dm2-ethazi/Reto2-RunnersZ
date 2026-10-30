# RunnersZ Android

Author: Jairo (@jairoben). Revisión: Alain. Rama: feature/android-v2.
Contrato principal: Contrato_Tecnico_RunnersZ_v2.docx (28/10/2026).
Seguimiento: issue #42, PR #39.

## Qué hace

Cliente nativo Kotlin, minSdk 26 / targetSdk 35. No cifra localmente:
Imagen sube un .png.enc y la contraseña a la API multipart; solo acepta
resultados con X-Integrity: verified y muestra el bitmap escalado.
Orden abre TXT UTF-8 o admite texto escrito; valida desplazamiento 1..25,
envía JSON a César y muestra el campo result.
Ajustes guarda dirección y token en preferencias privadas y prueba autenticación.
Recibe archivos por Abrir con o Compartir; la navegación conserva acceso a Ajustes.

## Estructura

- app/src/main/.../data: ApiClient asíncrono OkHttp, resultados y preferencias.
- app/src/main/.../domain: dirección, errores y desplazamiento.
- app/src/main/.../ui: Imagen, Orden y Ajustes.
- MainActivity: navegación y recepción de intents.
- app/src/main/res: navegación, estilos y tipografías Inter/Open Sans.
- app/src/test: JUnit, Robolectric y servidor HTTP de pruebas.
- licenses: licencias OFL de las fuentes.
- gradle/wrapper: Gradle 8.9, sin instalación global necesaria.

## Compilar y probar

Desde android/, con JDK 21 y SDK Android 35:

    gradlew.bat testDebugUnitTest assembleDebug
    gradlew.bat jacocoTestReport jacocoCoverageVerification

Linux: sustituir gradlew.bat por ./gradlew.
Definir ANDROID_HOME o sdk.dir en local.properties (archivo local, no subir).
APK: app/build/outputs/apk/debug/app-debug.apk.
Informe: app/build/reports/jacoco/jacocoTestReport/.
El umbral JaCoCo es 80%; no se rebaja para dar un resultado positivo.

## Uso

1. Abrir Ajustes, indicar IP:puerto del servidor y token.
2. Guardar y Probar; el servidor debe responder 204 en /api/v1/auth/check.
3. Imagen: seleccionar archivo, introducir contraseña y pulsar Descifrar.
4. Orden: escribir o abrir TXT, indicar desplazamiento y Descifrar orden.
5. Un error conserva la aplicación abierta para corregir y reintentar.

En emulador, 10.0.2.2 accede al PC anfitrión. En móvil real usar su IP LAN.
El servidor es una dependencia externa del módulo: debe ofrecer los endpoints
v2. No se incluye ni se modifica el backend en esta rama.

## Protección y límites

- El original se lee sin modificar. Subida desde copia privada con buffer 64 KiB.
- La copia temporal se borra al salir de Imagen. La contraseña no se persiste.
- Máximo de entrada Imagen: 100 MiB; TXT: 1 MiB. Bitmap reducido a 2048 px.
- Token en preferencias privadas con backup desactivado, sin cifrado adicional.
- HTTP LAN permitido por contrato: usar solamente red de demo controlada.
- Solicitudes OkHttp fuera del hilo UI, timeout 120 s, sin reintentos automáticos.
- Respuestas y pantallas no registran token ni contraseña en logs.
- TalkBack, 48 dp, fuentes escalables y estados textuales; revisar en dispositivo.

## Validación

Consultar docs/validacion-android.md para resultados realmente ejecutados.
Una compilación local no equivale a revisión aprobada ni CI verde.

## Capturas del emulador
- [Ajustes](android/docs/evidencias/ajustes.png)
- [Imagen](android/docs/evidencias/imagen.png)
- [Orden](android/docs/evidencias/orden.png)
