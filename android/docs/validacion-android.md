# Validación Android — CipherSprint

Autor: Jairo. Fecha: 2026-11-04. Rama: `feature/android`.
Paquete: `com.runnersz.reto2.android`. Versión: `2.0.0` (`versionCode 2`).

## Entorno de ejecución

- Windows 11.
- JDK 21.0.12 (`Eclipse Adoptium`).
- Gradle Wrapper 8.9, Android Gradle Plugin 8.6.1, Kotlin 1.9.24.
- SDK Android 35; `minSdk 26`, `targetSdk 35`.
- Emulador Pixel 8a, `emulator-5556`, 1080 × 2424.

El `JAVA_HOME` global apunta a JDK 25 y no sirve para este build
(`Unsupported class file major version 69`). Todas las comprobaciones se
ejecutaron forzando el JDK 21.

## Build y tests

Comando ejecutado desde `android/`:

```bash
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" \
  sh ./gradlew --no-daemon assembleRelease testDebugUnitTest
```

Resultado: **BUILD SUCCESSFUL**. `assembleRelease` incluye
`lintVitalRelease`, que no reportó errores bloqueantes.

| Suite | Tests | Fallos | Errores |
| --- | ---: | ---: | ---: |
| `data.ApiClientTest` | 5 | 0 | 0 |
| `data.ApiConfigTest` | 2 | 0 | 0 |
| `domain.ShiftValidatorTest` | 2 | 0 | 0 |
| `security.SecureTokenProviderTest` | 1 | 0 | 0 |
| **Total** | **10** | **0** | **0** |

No se ejecutaron instrumented tests, cobertura ni un análisis Lint completo
fuera del `lintVitalRelease` del build. No se presenta cobertura como si se
hubiera medido.

## APK entregado

- Ruta de build: `android/app/build/outputs/apk/release/app-release.apk`.
- Copia en el Escritorio: `C:\Users\dm2\Desktop\CipherSprint.apk`.
- Tamaño: 2.518.310 bytes.
- SHA-256: `f3865aff25f3a2696727c84f74030545de60a47e705a7194f7206a169b7cf6c6`.
- Release con R8 full mode, recursos reducidos y sin `debuggable`.
- Firmado con la clave **debug** para permitir la instalación de demo; no es
  una firma de producción.

## Verificación contra la API real

Servidor: `https://runnersz-api.onrender.com/api/v1`. Token integrado en la app.

- `GET /auth/check`: `204`, mostrado como «API responde correctamente» en Inicio
  y «Conexión correcta con la API» en Ajustes.
- `POST /text/caesar`: entrada `Dwdtxh Ñá! 2026`, desplazamiento `3`; resultado
  mostrado `Ataque Ñá! 2026`.
- `POST /files/decrypt`: vector `image.png.enc` con clave de demostración;
  respuesta con `X-Integrity: verified` y vista previa correcta.

Las capturas se tomaron con la app en primer plano. Se comprobó el foco
(`topResumedActivity`) antes de cada captura para descartar que el emulador
estuviera mostrando otra aplicación.

## Evidencias

- `evidencias/01_inicio.png`: Inicio con estado correcto.
- `evidencias/02_orden.png`: formulario César con valores por defecto.
- `evidencias/03_orden_resultado.png`: «Ataque Ñá! 2026».
- `evidencias/04_imagen.png`: vector de prueba y clave AES.
- `evidencias/05_imagen_resultado.png`: «Integridad verificada» y vista previa.
- `evidencias/06_ajustes.png`: servidor fijo y token enmascarado.
- `evidencias/07_ajustes_ok.png`: «Conexión correcta con la API».
- `evidencias/08_inicio_final.png`: Inicio final con la API disponible.

## Comprobaciones de seguridad realizadas

- Búsqueda del literal del token en el código fuente: sin coincidencias.
- El token se reconstruye en memoria desde tres arrays enmascarados y solo se
  usa en la cabecera `Authorization`.
- Ajustes muestra una máscara; no hay preferencias, backup ni extracción del
  token.
- `usesCleartextTraffic=false` y `network_security_config` solo permiten HTTPS
  con CA del sistema.
- Host de API fijado en `ApiConfig.BASE_URL`; no editable desde la UI.
- Release con R8 full mode y eliminación de `Log`; el manifest release no es
  debuggable.

## Límites que no se ocultan

- Un token incluido en un APK no es 100 % indescifrable. La ofuscación eleva el
  coste, pero un atacante con control del dispositivo puede instrumentar el
  proceso y recuperarlo. La alternativa robusta es un proxy o login con tokens
  efímeros y revocables.
- No se implementó certificate pinning; se confía en las CA del sistema.
- La firma del APK es debug, no de producción.
- La verificación se hizo en emulador, no en un móvil físico.
- No se probaron TalkBack manual, rotación, modo oscuro ni dispositivos de gama
  baja.
- La disponibilidad y el formato de la API dependen del backend desplegado; si
  cambia el contrato o se rota el token, hay que actualizar el cliente.
- No se hizo push ni se abrió PR en esta fase.
