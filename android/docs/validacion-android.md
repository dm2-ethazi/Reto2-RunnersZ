# Validación Android — CipherSprint

Autor: Jairo. Fecha: 2026-11-07. Rama: `feature/android`.
Paquete: `com.runnersz.reto2.android`. Versión: `2.0.0` (`versionCode 2`).

## Entorno de ejecución

- Windows 11.
- JDK 21.0.12 (`Eclipse Adoptium`).
- Gradle Wrapper 8.9, Android Gradle Plugin 8.6.1, Kotlin 1.9.24.
- SDK Android 35; `minSdk 26`, `targetSdk 35`.
- Emulador Pixel 8a, `emulator-5554`, Android 17 (API 37), 1080 × 2400,
  densidad 420.

El `JAVA_HOME` global apunta a JDK 25 y no sirve para este build
(`Unsupported class file major version 69`). Todas las comprobaciones se
ejecutaron forzando el JDK 21.

## Build y tests

Comando ejecutado desde `android/`:

```bash
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" \
  sh ./gradlew --no-daemon testDebugUnitTest assembleRelease
```

Resultado: **BUILD SUCCESSFUL**. `assembleRelease` incluye
`lintVitalRelease`, que no reportó errores bloqueantes.

| Suite | Tests | Fallos | Errores |
| --- | ---: | ---: | ---: |
| `data.ApiClientTest` | 7 | 0 | 0 |
| `data.ApiConfigTest` | 2 | 0 | 0 |
| `domain.FileValidatorTest` | 4 | 0 | 0 |
| `domain.ImageSamplerTest` | 3 | 0 | 0 |
| `domain.ShiftValidatorTest` | 2 | 0 | 0 |
| `security.SecureTokenProviderTest` | 1 | 0 | 0 |
| **Total** | **19** | **0** | **0** |

No se ejecutaron instrumented tests, cobertura ni un análisis Lint completo
fuera del `lintVitalRelease` del build. No se presenta cobertura como si se
hubiera medido.

## Pruebas de usuario en el emulador

Las pruebas se hicieron con la app instalada en release y contra la API real
(`https://runnersz-api.onrender.com/api/v1`), manejando la interfaz con
`adb`/`uiautomator` como un usuario normal: toques, texto escrito, rotación y
botones del sistema.

### Inicio

| Caso | Resultado esperado | Observado |
| --- | --- | --- |
| Abrir la app | Comprobación automática de la API | «API responde correctamente» |
| Pulsar **Probar API** | Repite la comprobación | Estado actualizado sin duplicar mensajes |
| Cambiar de pestaña y volver | No repite la petición ya hecha | Se conserva el estado |
| Sin red | Mensaje claro, sin cuelgue | «No se pudo conectar con el servidor» |
| Botón atrás | Sale de la app | Sale al launcher |

### Orden (César)

| Caso | Resultado esperado | Observado |
| --- | --- | --- |
| `Dwdtxh` con desplazamiento 3 | `Ataque` | Correcto |
| Desplazamiento 25 | Rotación del alfabeto correcta | `B` para `A`… correcto |
| Desplazamiento 0 o 26 | Aviso de rango y sin petición | Aviso mostrado |
| Campos vacíos | Aviso y sin petición | Aviso mostrado |
| Texto de 300 caracteres | Se envía y se muestra el resultado | Correcto |
| Sin red | Error de conexión, no de validación | «No se pudo conectar con el servidor» |
| Girar el móvil tras descifrar | Se conserva texto, desplazamiento y resultado | Correcto |
| Cambiar de pestaña y volver | Se conserva el formulario y el resultado | Correcto |
| Re-tocar la pestaña activa | No borra nada | Correcto |

### Imagen (AES)

| Caso | Resultado esperado | Observado |
| --- | --- | --- |
| Vector `image.png.enc` con la clave de demo | Vista previa e «Integridad verificada» | Correcto |
| Clave incorrecta | Mensaje de clave o archivo dañado | «Clave incorrecta o archivo dañado» |
| Clave vacía | Aviso y sin subida | Aviso mostrado |
| Archivo `.txt` elegido | Rechazo antes de subir y sin perder el vector | «El archivo debe tener extensión .enc» |
| Abrir selector de archivos | Filtra y permite elegir `.enc` | Correcto |
| Girar tras descifrar | Se conserva archivo y vista previa | Correcto |
| Cambiar de pestaña y volver | Se conserva archivo y vista previa | Correcto |
| Botón atrás | Vuelve a Inicio | Correcto |

### Accesibilidad e interfaz

| Caso | Resultado esperado | Observado |
| --- | --- | --- |
| Fuente del sistema al 1.4× | Sin textos cortados | Sin desbordes en las tres pantallas |
| Descripción del icono de la cabecera | Una sola etiqueta para TalkBack | Solo «Conexión protegida» |
| Margen horizontal | Contenido sin aire excesivo | Corregido en vertical y horizontal |
| Rotación en Orden | Cabecera, «Volver» y resultado utilizables | Correcto tras el ajuste de scroll |
| Rotación en Imagen | Vista previa completa | Correcto tras el ajuste de scroll |

## Errores encontrados y corregidos

1. **Resultado de Orden perdido al girar.** Los `Fragment` se recreaban sin
   estado. Se añadió `OrderViewModel`.
2. **Formulario borrado al cambiar de pestaña o re-tocar la pestaña activa.**
   Las pestañas se sustituían con `replace`. Ahora se usan instancias vivas con
   `add`/`show`/`hide` y se restauran al volver del proceso.
3. **Vista previa y archivo de Imagen perdidos al girar.** Se añadió
   `ImageViewModel`, que libera bitmap y archivo en `onCleared()`.
4. **Archivos que no eran `.enc` aceptados.** Se subía y fallaba en el servidor.
   Ahora `FileValidator` lo rechaza en el dispositivo con un mensaje claro y sin
   gastar la subida.
5. **Decodificación de la imagen en el hilo de UI.** Riesgo de bloqueo y de
   `OutOfMemoryError` con capturas grandes. Ahora se decodifica en
   `Dispatchers.IO` con muestreo (`inSampleSize`).
6. **Respuesta JSON inválida tratada como fallo de red.** Se captura
   `JSONException` y se muestra un error de respuesta del servidor.
7. **Etiquetas duplicadas para TalkBack y márgenes horizontales excesivos.**
   Simplificados los `content-desc` y los márgenes de los layouts.
8. **Horizontal: el resultado tapaba la cabecera y «Volver».**
   El desplazamiento automático arrastraba la pantalla y el rechazo de archivo
   dejaba visible la vista previa anterior. Ahora el desplazamiento es mínimo,
   el rechazo limpia la vista previa y el ScrollView no restaura su posición.

Cada apartado se cerró en su propio commit sobre `feature/android`:

- `FIX: Conserva el estado entre giros y pestañas`
- `FIX: Valida el .enc y decodifica sin bloquear UI`
- `FIX: Distingue respuesta inválida de fallo de red`
- `FEAT: Accesibilidad y horizontal más útil`
- `FIX: Mantén la cabecera al mostrar el resultado`

## APK entregado

- Ruta de build: `android/app/build/outputs/apk/release/app-release.apk`.
- Copia en el Escritorio: `C:\Users\dm2\Desktop\CipherSprint.apk`.
- Tamaño: 2.552.264 bytes.
- SHA-256: `7251e61c1e68923bd81096c979a9e5adedc8f309dd25f344268ed704b6e39393`.
- Release con R8 full mode, recursos reducidos y sin `debuggable`.
- Firmado con la clave **debug** para permitir la instalación de demo; no es
  una firma de producción.

## Evidencias

- `evidencias/01_inicio.png`: Inicio con estado correcto y tres pestañas.
- `evidencias/02_orden.png`: formulario César vacío, con pistas.
- `evidencias/03_orden_resultado.png`: `Dwdtxh` y 3 dan `Ataque`.
- `evidencias/04_imagen.png`: vector de prueba y clave AES precargados.
- `evidencias/05_imagen_resultado.png`: «Integridad verificada» y vista previa.
- `evidencias/06_horizontal_orden.png`: horizontal con cabecera, «Volver» y
  resultado visibles.
- `evidencias/07_horizontal_imagen.png`: horizontal con la vista previa
  completa tras descifrar.
- `evidencias/08_extension_invalida.png`: `.txt` rechazado por su extensión.

Las capturas se tomaron con la app en primer plano. Se comprobó el foco
(`topResumedActivity`) antes de cada captura para descartar que el emulador
estuviera mostrando otra aplicación.

## Comprobaciones de seguridad realizadas

- Búsqueda del literal del token en el código fuente: sin coincidencias.
- El token se reconstruye en memoria desde tres arrays enmascarados y solo se
  usa en la cabecera `Authorization`.
- La app no muestra el token en ninguna pantalla; no hay preferencias, backup
  ni extracción del token.
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
- No se probó TalkBack con voz real, ni modo oscuro, ni dispositivos de gama
  baja; la accesibilidad se limitó a etiquetas, regiones vivas y tamaños.
- En horizontal con fuente muy grande el contenido sigue siendo desplazable:
  se ve la cabecera al entrar, pero un resultado largo obliga a desplazar.
- La disponibilidad y el formato de la API dependen del backend desplegado; si
  cambia el contrato o se rota el token, hay que actualizar el cliente.
- Los cambios están publicados en `origin/feature/android`; no se abrió PR.
