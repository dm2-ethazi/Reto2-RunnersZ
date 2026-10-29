# RunnersZ — Android

## Autoría y alcance

- Author / responsable: Jairo (@jairoben).
- Revisor previsto: Alain; no implica revisión aprobada.
- Rama: feature/android-v2. PR: #39.
- Referencia principal: Contrato_Tecnico_RunnersZ_v2.docx.
- Este apartado contiene el cliente Kotlin que delega el descifrado en la API.

## Contenido real

| Archivo | Responsabilidad actual |
| --- | --- |
| MainActivity.kt | Activity de arranque; onCreate todavía no crea una interfaz. |
| ConnectionSettings.kt | Valida URL HTTP/HTTPS y token no vacío; elimina la barra final. |
| ApiClient.kt | Define peticiones de descifrado multipart y César JSON con OkHttp. |
| ImageDecryptor.kt | Convierte la respuesta del servidor en bitmap; rechaza imagen inválida. |
| app/src/test/ | Pruebas de configuración y conversión de imagen. |

El código Kotlin está en android/app/src/main/java/com/runnersz/reto2/android/.

## Comunicación prevista

- POST /api/v1/files/decrypt: multipart con file y password.
- POST /api/v1/text/caesar: JSON con text, shift y mode=decrypt.
- El cliente debe enviar Authorization: Bearer seguido del token configurado.
- Actualmente ApiClient envía únicamente el prefijo Bearer: la autenticación
  todavía necesita corregirse. Las peticiones no se han validado de extremo a extremo.

## Entorno y dependencias declaradas

Kotlin, compileSdk/targetSdk 35, minSdk 26. El módulo declara AndroidX Core
1.13.1, AppCompat 1.7.0, OkHttp 4.12.0 y JUnit 4.13.2.
Consulta android/build.gradle.kts y android/app/build.gradle.kts antes de
preparar el entorno. No se incluye Gradle Wrapper.

1. Abrir android/ en Android Studio y preparar SDK 35.
2. Sincronizar Gradle con una versión compatible con los plugins declarados.
3. Ejecutar las pruebas unitarias desde el IDE.
4. Completar la UI y autenticación antes de intentar la prueba funcional.
5. Con Gradle compatible instalado, desde android/: gradle testDebugUnitTest.
6. Para construir: gradle assembleDebug. APK esperado en
   app/build/outputs/apk/debug/app-debug.apk; su generación no está verificada.

## Requisitos y trabajo pendiente

El contrato exige pantallas Imagen y Orden, ajustes de conexión, selector,
recepción de archivos compartidos, operaciones fuera del hilo UI, gestión de
errores sin cerrar la app y pruebas reproducibles. Estos requisitos no deben
confundirse con funciones terminadas: la Activity está vacía, falta conectar
la UI a los servicios y comprobar autenticación, errores HTTP y recursos.
También faltan validación en dispositivo, cobertura exigida y APK comprobado.

## Cómo validar

Probar configuración válida e inválida, token incorrecto, backend inaccesible,
imagen válida/corrupta, clave errónea, César 1 y 25, desplazamientos inválidos,
selector y compartir. Registrar comando, entorno, resultado y evidencia.
No hay resultados nuevos de Gradle ni cobertura verificada en esta actualización.

## Documentación

El README de android/ contiene esta misma guía para quien abra el módulo.
Manuales y evidencias complementarios están en docs/. La rama queda pendiente
de validación y revisión; este README no certifica el cumplimiento del contrato.
