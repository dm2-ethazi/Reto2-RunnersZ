# Android - RunnersZ v2

## Identificacion y autoria

- Apartado: Android, trabajo asignado a Jairo.
- Rama: feature/android-v2.
- Responsable: Jairo.
- Revision: Alain.
- Contrato: Contrato_Tecnico_RunnersZ_v2.docx.
- Alcance: cliente Android de descifrado y comunicacion con la API.
- Estado: En revision. No es Hecho hasta tener CI verde, revision aprobada y
  evidencias reproducibles.

## Requisitos del contrato

- Kotlin y minSdk 26.
- Abrir o recibir .txt, .png y .png.enc mediante selector y compartir.
- Pantalla Imagen: elegir imagen cifrada, introducir clave AES, solicitar el
  descifrado al backend y mostrar el bitmap recibido.
- Pantalla Orden: enviar texto y desplazamiento Caesar valido de 1 a 25, y
  presentar el texto plano.
- Errores de clave, desplazamiento, archivo corrupto o API no disponible con
  Toast, Snackbar o dialogo sin cerrar la app.
- Logica de red separada de UI y cubierta por pruebas unitarias.
- Direccion, puerto y token configurables.
- Autenticacion Bearer conforme al backend v2.

## Estructura

- app/src/main/java/.../MainActivity.kt: UI y navegacion.
- ConnectionSettings.kt: direccion, puerto y token.
- ApiClient.kt: llamadas HTTP con OkHttp.
- ImageDecryptor.kt: validacion y conversion del resultado.
- app/src/test/: pruebas unitarias.
- build.gradle.kts: dependencias declaradas del modulo.

El servidor es la autoridad para el descifrado remoto de esta variante. No se
debe introducir otra implementacion local que contradiga el contrato API v2.

## Puesta en marcha

1. Abrir android/ en Android Studio.
2. Usar JDK y Android SDK compatibles.
3. Instalar una plataforma Android igual o superior a minSdk 26.
4. Ejecutar las pruebas de app/src/test/.
5. Configurar URL, puerto y token.
6. Arrancar el backend y probar salud, Imagen y Orden.
7. Generar APK debug tras completar la validacion Android.

No se versionan credenciales, tokens ni archivos locales del SDK.

## Validacion

Casos previstos: configuracion y URL, peticion autenticada, imagen valida,
respuesta corrupta, clave incorrecta, desplazamiento fuera de 1..25, backend
apagado, timeout, error HTTP, compartir y selector.

La ejecucion real de Gradle, APK y dispositivo requiere Android SDK, Gradle y
Android Studio. Hasta ejecutarlo, debe figurar como pendiente humano.

## Documentacion relacionada

- docs/manual-uso-android.md
- docs/README-i21.md
- docs/README-i22.md
- docs/evidencias/android-unity.md
