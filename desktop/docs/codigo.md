# El código del cliente JavaFX, explicado

Este documento explica cómo está construido el cliente de escritorio de RunnersZ: qué hace cada
pieza, por qué se separó así y dónde mirar cuando algo falla. Sirve para defender el código sin
tener que leerlo entero.

## 1. Qué hace la aplicación

La ventana tiene tres pestañas y un pie de estado:

| Pestaña | Operación | Quién hace el trabajo |
|---|---|---|
| **Ficheros · AES** | Sube un fichero, recibe el `.enc` o recupera el original | Servidor (la app solo transporta) |
| **Mensajes · Vigenère** | Cifra o descifra texto con una clave de letras | Servidor |
| **Mensajes · César** | Cifra o descifra texto con un desplazamiento de 1 a 25 | Servidor |

El cliente **no implementa criptografía**: valida, envía, muestra el progreso y publica la respuesta
(§3 y §5.3 del contrato). Esa decisión evita duplicar el algoritmo en Java y en Python y garantiza
que el formato `.enc` es exactamente el mismo para todos los clientes.

## 2. Arranque y capas

```
Launcher  →  Main  →  FXMLLoader(desktop.fxml + bundle)  →  DesktopController
                                                              ├── service/ApiClient
                                                              ├── service/FileOperationService
                                                              ├── service/FileService
                                                              └── service/SettingsStore
```

- **`Launcher`** existe porque un fat JAR que arranca directamente una clase `Application` falla
  con «JavaFX runtime components are missing»: el lanzador no extiende `Application` y arranca el
  motor con `Application.launch`.
- **`Main`** registra las fuentes empaquetadas (Inter y Open Sans), carga el FXML con el catálogo de
  idioma y vuelve a cargar la vista cuando se cambia de idioma, restaurando el formulario con
  `UiState`.
- **`DesktopController`** solo traduce eventos en llamadas a servicios y pinta el resultado. No abre
  sockets ni escribe ficheros.
- **`service/`** contiene toda la lógica comprobable sin interfaz: HTTP, ficheros y preferencias.
- **`utiles/I18n`** resuelve los catálogos y traduce los errores, de modo que un fallo se guarda
  como clave y se muestra en el idioma activo en el momento de pintarlo.

Esta separación es la que permite que las pruebas cubran dominio y servicios sin arrancar ventanas.

## 3. La vista es FXML de verdad

`desktop.fxml` declara la ventana completa con controles estándar de JavaFX y textos `%clave`. El
`FXMLLoader` inyecta esos textos desde `i18n/messages*.properties`, así que no hay cadenas quemadas
en el XML ni recursos muertos. Los `fx:id` coinciden con los campos del controlador y los `onAction`
con sus métodos `@FXML`.

Los iconos (candados y engranaje) **no** están en el FXML: los crea el controlador con Ikonli al
arrancar. En Scene Builder los botones se ven sin icono, y es lo esperado: el editor no necesita
Ikonli en su classpath y el FXML se abre en cualquier versión.

## 4. Hilos: una regla y tres mecanismos

Regla: **todo lo que toca la interfaz ocurre en el hilo de JavaFX**; la red y el disco, nunca.

1. `Task` para cada operación. Se ejecuta en un `ExecutorService` con hilos *daemon* llamados
   `runnersz-worker`, y sus callbacks (`setOnSucceeded`, `setOnFailed`, `setOnCancelled`) vuelven
   solos al hilo de JavaFX.
2. `progressProperty` y `messageProperty` se **enlazan** a la barra y a la etiqueta de progreso, así
   que el porcentaje real de transferencia se pinta sin código de sincronización.
3. Un `PauseTransition` apaga el aviso no bloqueante a los 5 s.

Mientras hay una operación, `running` no es nulo: los botones se deshabilitan, se muestra
«Cancelar operación» y un segundo clic no puede lanzar dos transferencias a la vez.

## 5. Transferencia por bloques

`ApiClient` construye el cuerpo `multipart/form-data` a mano y lo publica como
`StreamBodyPublisher`:

- el fichero se lee en bloques de **64 KiB** y cada bloque se emite como `ByteBuffer`;
- la respuesta se escribe directamente en un fichero temporal, no en memoria;
- por eso el consumo de memoria no depende del tamaño del fichero (§7.1) y se puede cifrar un
  fichero de 20 MiB con el mismo consumo que uno de 1 MiB;
- todas las peticiones usan un **timeout de 120 s** (§7.2).

Al descifrar, el cliente exige la cabecera `X-Integrity: verified`. Si falta, lanza un error y **no
publica** el resultado: un `.enc` dañado no puede sobrescribir un original.

## 6. Publicación segura del resultado

`FileOperationService` valida, transfiere y publica:

1. Rechaza ficheros de 0 bytes y salidas que son carpetas, y protege el fichero original.
2. Escribe el resultado en `.runnersz-*.part` **en el mismo directorio** que el destino.
3. Lo mueve con `ATOMIC_MOVE` (con reserva si el sistema de archivos no lo admite).

Si algo falla a mitad, el `finally` borra el temporal: no quedan ficheros a medias ni se pierde el
original. La contraseña se borra del formulario al terminar y nunca se guarda en disco.

## 7. Errores que se entienden

`ApiClient` traduce los códigos del servidor (`UNAUTHORIZED`, `EMPTY_FILE`, `EMPTY_PASSWORD`,
`INVALID_KEY`, `INVALID_SHIFT`, `FILE_TOO_LARGE`, `DECRYPT_FAILED`, `INTERNAL_ERROR`) a claves de los
cuatro catálogos. `I18n.errorMessage` recorre la cadena de causas y **nunca** muestra el mensaje
nativo del sistema operativo, que podría incluir rutas o datos del usuario.

Los diálogos de error ofrecen **Reintentar** e **Ir a Ajustes**; el aviso del pie no bloquea.

## 8. Accesibilidad (§9.3)

- Cada control declara su rol con `setAccessibleRole` (botón, campo, área de texto, pestañas, barra
  de desplazamiento, indicador de progreso, texto) y su nombre con `setAccessibleText`.
- Los botones de icono reciben además su descripción con `setAccessibleHelp`, y el nombre accesible
  no incluye el guion bajo del atajo.
- El valor del desplazamiento, el estado y el aviso se anuncian al cambiar (`setAccessibleText`).
- Foco visible de 2 px, orden de tabulación natural, atajos `Alt+C` y `Alt+D`, menú contextual de
  edición traducido y botones de al menos 34 px de alto.
- El estado nunca se comunica solo con color: el chip lleva texto y los errores, icono y mensaje.

## 9. Tipografía (§9.2)

Inter para títulos y botones, Open Sans para textos largos y una monoespaciada para resultados. Las
dos primeras van **empaquetadas** en el JAR, así que la aplicación se ve igual en Windows y Linux.
Los tamaños son **relativos**: `.root` fija la base en 16 px —que son exactamente 12 pt— y el resto
va en `em` sobre esa base, elegidos para caer en píxeles enteros: marca `1.875em` (30 px ≈ 22,5 pt),
títulos `1.6875em` (27 px ≈ 20,25 pt), cuerpo, ayudas y botones `1em` (16 px = 12 pt) y resultados
monoespaciados `1.1875em` (19 px ≈ 14,25 pt). Así se cumplen con holgura los mínimos del contrato
(20 pt, 12 pt y 14 pt), toda la interfaz se reescala cambiando un solo valor y ninguna letra cae en
posiciones fraccionarias: 20 pt son 26,67 px y 14 pt son 18,67 px, y esos decimales son los que se
ven pixelados en pantallas con escalado. La ayuda emergente usa 16 px absolutos porque vive en una
ventana aparte y no hereda la base del `.root`. `DesktopControllerTest.laTipografiaUsaEmSobreUnaBaseDe16Px`
comprueba los tamaños realmente calculados por el motor de CSS.

## 10. Pruebas

| Tipo | Clase | Qué demuestra |
|---|---|---|
| HTTP real contra un servidor local | `ApiClientTest` | rutas, token, multipart por bloques, cabeceras y errores |
| Dominio | `FileOperationServiceTest`, `FileServiceTest` | validaciones, temporales, movimiento atómico |
| Preferencias | `SettingsStoreTest` | validación, normalización, migración |
| Catálogos | `I18nTest` | paridad de claves y claves usadas por el FXML |
| Vista real | `DesktopControllerTest` | roles accesibles, nombres, estado del formulario, avisos, diálogo de Ajustes y portapapeles |
| Ciclo de vida | `MainTest` | arranque de la ventana, fuentes, título y parada limpia |

Las pruebas de interfaz cargan el FXML auténtico en el hilo de JavaFX y cierran los diálogos modales
desde un vigilante, de modo que no necesitan interacción ni pantalla visible (en integración continua
se ejecutan bajo Xvfb). Se omiten solas si el entorno no puede arrancar JavaFX.

## 11. Por qué así

- **Sin criptografía en el cliente**: el servidor es la única fuente de verdad del formato `.enc`.
- **Un servicio por responsabilidad**: HTTP, ficheros y preferencias se prueban por separado.
- **FXML + controlador delgado**: la vista se edita sin recompilar y el controlador no mezcla
  transporte con presentación.
- **Todo traducido por clave**: añadir un idioma es añadir un `.properties`, no tocar Java.
- **Fallar sin dañar**: temporal + `ATOMIC_MOVE` + comprobación de integridad antes de publicar.

## 12. Registro (log)

La aplicación escribe un registro propio con SLF4J y Logback, sin fichero de configuración estático:
`utiles/Logs` monta el `RollingFileAppender` en código y `utiles/Log` es la fachada que usa todo el
programa, de modo que ningún mensaje puede saltarse el enmascarado. El token, la contraseña y la
clave se sustituyen por `***` antes de escribir (`Logs.redact`).

La carpeta se elige en este orden: `-Drunnersz.logs.dir`, la variable `RUNNERSZ_LOGS_DIR` y, si no hay
ninguna, la carpeta de registros del usuario: `%LOCALAPPDATA%\RunnersZ\logs` en Windows,
`~/Library/Logs/RunnersZ` en macOS y `$XDG_STATE_HOME/runnersz/logs` (o `~/.local/state/runnersz/logs`)
en Linux. Nunca se escribe en el directorio de trabajo ni junto al JAR: instalar la aplicación en una
carpeta de solo lectura no rompe el registro, y el fichero no puede acabar en el repositorio porque
`.gitignore` excluye `*.log`, `*.log.gz` y `logs/`.

`runnersz.log` rota cada 5 MB o cada día a `runnersz.AAAA-MM-DD.n.log.gz`, conserva siete días y no
pasa de 50 MB. El nivel por omisión es `INFO`; con `-Drunnersz.log.level=DEBUG` se añade el detalle
fino y `-Drunnersz.logs.console=false` silencia la copia por consola. Si la carpeta no se puede crear,
el registro cae en una carpeta temporal del sistema y deja un aviso, sin impedir el arranque.

Se registran el arranque y el cierre, las peticiones HTTP (método, ruta, estado, milisegundos y
bytes), el resultado de cifrar o descifrar, la falta de `X-Integrity` y los fallos que la interfaz
muestra en un diálogo, con la pila completa que el diálogo no enseña. Las excepciones no capturadas de
cualquier hilo también se guardan en el manejador por omisión.

Desde Ajustes → **Ver registro** se abre un visor con las últimas 200 líneas, la ruta completa y los
botones para abrir la carpeta o copiar la ruta, traducido a los cuatro idiomas.
