# CipherSprint Android

Autor: Jairo (@jairoben). Revisión: Alain. Rama: `feature/android`.
Cliente Android del Reto 2 RunnersZ. Esta versión sustituye la suite anterior
por la aplicación CipherSprint de tres pestañas.

## Qué hace

Aplicación nativa Kotlin, `minSdk 26`, `targetSdk 35`, `versionName 2.0.0`.
No descifra localmente: las operaciones se delegan en la API oficial por HTTPS.

- **Inicio**: bienvenida, botón **Probar API**, estado de conexión y accesos a
  Orden e Imagen. La comprobación se lanza automáticamente al abrir.
- **Orden**: descifrado César con campos vacíos y pistas. El usuario introduce
  el texto y un desplazamiento válido de 1 a 25. Acepta texto escrito o un TXT
  UTF-8 de hasta 1 MiB recibido por *Abrir con* / *Compartir*.
- **Imagen**: descifrado AES multipart. Incluye el vector de prueba
  `image.png.enc` y la clave de demostración precargada. Permite seleccionar
  otro `.enc`, exige `X-Integrity: verified` y muestra la vista previa.
- **Navegación**: barra inferior Orden / Imagen / Inicio y filas de
  acceso rápido en Inicio. Recibe archivos mediante `ACTION_SEND` y
  `ACTION_VIEW` (texto, imagen u octet-stream).
- **Interfaz adaptativa**: variantes vertical y horizontal, cabecera compacta
  al girar y márgenes seguros para notch y barra de navegación.

## Estructura

- `app/src/main/java/.../MainActivity.kt`: navegación, insets e intents.
- `app/src/main/java/.../SplashActivity.kt`: splash breve de arranque.
- `app/src/main/java/.../data`: cliente OkHttp, configuración y resultados.
- `app/src/main/java/.../domain`: validación César y mensajes de error.
- `app/src/main/java/.../security`: reconstrucción del token por fragmentos.
- `app/src/main/java/.../ui`: Inicio, Orden e Imagen.
- `app/src/main/assets/demo/image.png.enc`: vector AES de demostración.
- `app/src/main/res`: cabecera, layouts, navegación, colores y textos.
- `app/src/test`: 4 suites JUnit/Robolectric con 10 pruebas.
- `gradle/wrapper`: Gradle 8.9; no requiere instalación global.

## API utilizada

Base fija: `https://runnersz-api.onrender.com/api/v1`.

- `GET /auth/check`: valida el Bearer; respuesta correcta `204`.
- `POST /text/caesar`: JSON `{text, shift, mode:"decrypt"}`; devuelve `result`.
- `POST /files/decrypt`: multipart `file` + `password`; exige
  `X-Integrity: verified` y devuelve la imagen descifrada.
- Los errores del backend usan `{"code":"...","message":"..."}`.

El cliente usa OkHttp con timeout de llamada de 120 s, sin reintentos
automáticos. Las peticiones se ejecutan fuera del hilo de UI.

## Seguridad

- El token no aparece como literal en el código: se reconstruye en memoria a
  partir de tres fragmentos enmascarados con XOR (`CredentialA/B/C`).
- Solo se usa para la cabecera `Authorization`; no se guarda en preferencias,
  ni en backups, ni se muestra en la UI o en logs.
- La app no muestra el token en ninguna pantalla ni permite editarlo.
- Release con R8 full mode, `minifyEnabled`, `shrinkResources`, reempaquetado de
  clases y eliminación de llamadas a `Log`.
- `allowBackup=false`, reglas de extracción que excluyen preferencias, archivos
  y bases de datos, y `usesCleartextTraffic=false`.
- El servidor está fijado en código; no puede cambiarse desde la app para
  enviar el token a otro host.
- La red solo confía en las CA del sistema. **No se implementó certificate
  pinning**: un dispositivo comprometido con una CA de usuario instalada podría
  interceptar el tráfico.

### Límite importante

Ningún secreto distribuido dentro de un APK es 100 % indescifrable. Un atacante
que controle el dispositivo puede instrumentar el proceso, leer la memoria en
tiempo de ejecución o reconstruir los fragmentos; la ofuscación y R8 elevan el
coste, pero no convierten el token en matemáticamente imposible de extraer.

La solución fuerte para producción es no distribuir el token: usar un proxy
propio que autentique al usuario y emita credenciales efímeras, o un flujo de
login con tokens de corta duración y revocación. Este cliente mantiene el token
integrado porque es un requisito de la demo, no una garantía absoluta.

El APK de entrega se firma con la **clave debug** para poder instalarlo. Antes
de publicar en una tienda hay que generar un keystore de producción y custodiarlo
fuera del repositorio; el manifest release no es debuggable.

## Compilar y probar

Desde `android/`, con JDK 21 y SDK Android 35:

```bash
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" \
  sh ./gradlew --no-daemon testDebugUnitTest assembleRelease
```

En Windows también puede usarse `gradlew.bat`; en Linux, `./gradlew`.
El JDK del sistema debe ser 21: Gradle 8.9 no admite JDK 25.

- APK generado: `app/build/outputs/apk/release/app-release.apk`.
- Copia de entrega: `C:\Users\dm2\Desktop\CipherSprint.apk`.
- SHA-256 de la copia: `96cd2cc1688371250cdf3bef8dd4ef736effe63178778cd28381d42dba16e409`.
- Tamaño: 2.515.286 bytes.

## Validación

Consultar [`docs/validacion-android.md`](docs/validacion-android.md) para los
resultados realmente ejecutados, las capturas y los límites.

Resumen: `BUILD SUCCESSFUL`, 10 tests en 4 suites, 0 fallos, y verificación
manual en el emulador `emulator-5556` contra la API real desplegada.

## Capturas del emulador

- [Inicio](docs/evidencias/01_inicio.png)
- [Orden](docs/evidencias/02_orden.png)
- [Orden con resultado](docs/evidencias/03_orden_resultado.png)
- [Imagen](docs/evidencias/04_imagen.png)
- [Imagen con integridad verificada](docs/evidencias/05_imagen_resultado.png)
- [Orden en horizontal](docs/evidencias/06_horizontal_orden.png)
- [Imagen en horizontal](docs/evidencias/07_horizontal_imagen.png)
