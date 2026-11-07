# Evidencias de validación del cliente JavaFX

Todo lo que hay aquí se puede repetir desde `desktop/`. Los números son los de la última ejecución
(`./mvnw -B verify`, 7 de noviembre de 2026, JDK 21 en Windows 11).

## 1. Pruebas automáticas

```bash
cd desktop
./mvnw -B verify        # compila, ejecuta JUnit 5 y aplica el umbral JaCoCo
```

Resultado: **64 pruebas, 0 fallos, 0 errores, 0 omitidas** (`BUILD SUCCESS`).

| Clase | Pruebas | Qué evidencia |
|---|---:|---|
| `ApiClientTest` | 14 | rutas de la API, token, multipart por bloques, acentos en el nombre, `X-Integrity`, 401/413/500, red caída, contraseña vacía |
| `FileOperationServiceTest` | 6 | 0 B rechazado, salida que es carpeta, protección del original, publicación y limpieza de temporales |
| `FileServiceTest` | 4 | temporales junto al destino, reemplazo atómico, textos UTF-8 |
| `SettingsStoreTest` | 8 | sin credenciales por defecto, normalización de `/api/v1`, validación de URL y token, migración |
| `I18nTest` | 9 | paridad de claves en los cuatro catálogos y claves usadas por el FXML |
| `DesktopControllerTest` | 22 | roles y nombres accesibles, estado del formulario, selección de ficheros, desplazamiento, avisos con su título, diálogo de Ajustes (probar, guardar y token rechazado), confirmación de reemplazo, menú de edición, portapapeles y un cifrado completo contra la API local |
| `MainTest` | 1 | arranque real de la ventana, título, fuentes empaquetadas y parada sin ventanas ni tareas vivas |

`DesktopControllerTest` carga el `desktop.fxml` real en el hilo de JavaFX. Para no bloquearse, un
vigilante cierra los diálogos modales y anota su título, así que cada prueba comprueba también **qué**
aviso aparece; cuando la prueba necesita pulsar dentro de un diálogo (Ajustes, confirmar reemplazo),
el vigilante se desactiva y el test localiza el `DialogPane` por su texto. Si el entorno no puede
arrancar JavaFX, las clases de vista se omiten en lugar de fallar; en integración continua se
ejecutan con `xvfb-run` (pantalla virtual).

## 2. Cobertura

```bash
./mvnw -B verify
# informe: target/site/jacoco/index.html y target/site/jacoco/jacoco.csv
```

| Paquete | Cobertura de líneas |
|---|---:|
| `com.runnersz.model` | 100,0 % (2/2) |
| `com.runnersz.utiles` | 93,3 % (42/45) |
| `com.runnersz.controller` | 82,0 % (559/682) |
| `com.runnersz` (`Main`, `Launcher`) | 81,1 % (30/37) |
| `com.runnersz.service` | 80,6 % (303/376) |
| **Total del proyecto** | **82,0 % (936/1142)** |

El `check` de JaCoCo **no lleva filtros de paquete**: mide el proyecto completo y falla por debajo
del 80 %, que es lo que pide el pliego. Cada capa supera también ese 80 % por separado, así que no
hay ninguna zona «sin medir».

**Qué queda fuera y por qué (el 18 % restante).** Las líneas sin cubrir son:

- los cuatro manejadores que abren el **selector de ficheros nativo** (`handleChooseInput`,
  `handleChooseOutput`, `handleOpenText`, `handleSaveText`): el diálogo lo dibuja el sistema
  operativo, no JavaFX, así que no es automatizable desde JUnit;
- los manejadores de **arrastrar y soltar** (`configureDropTarget`), que necesitan un `Dragboard`
  real del sistema;
- `Launcher.main` y `Main.main`, que llaman a `Application.launch` y arrancan un bucle de eventos
  nuevo;
- ramas de error poco frecuentes de los `Flow.Subscriber` de `ApiClient` (cancelación a mitad de
  bloque, error de lectura del canal).

Todo lo demás —incluidas las dos ramas del diálogo de Ajustes y la cancelación del reemplazo— está
cubierto por pruebas que se ejecutan en cada `mvn verify`.

## 3. Empaquetado

```bash
./mvnw -B package
java -jar target/runnersz-desktop.jar
```

- `target/runnersz-desktop.jar` ≈ 11,2 MB, arranca en Windows y en Linux con JDK 21.
- Sin rutas absolutas de Windows ni lecturas de `os.name` en el código (`grep -rn "C:\\\\" src/main`).
- La ventana se abre con el título «RunnersZ · Cliente de escritorio».

## 4. Pruebas manuales del Anexo VI.4 (tamaños)

Estas pruebas necesitan el servidor levantado (`docker compose up` en el repositorio del servidor) y
un token válido en Ajustes.

Preparación:

```bash
mkdir -p /tmp/evidencias && cd /tmp/evidencias
: > vacio.bin                       # 0 B
printf '\x41' > uno.bin             # 1 B
head -c 1048576 /dev/urandom > un_mib.bin
head -c 20971520 /dev/urandom > veinte_mib.bin
```

En la aplicación: Ajustes → dirección y token → **Probar** → **Guardar**. Después, para cada fichero:

| # | Fichero | Acción | Resultado esperado | Evidencia a guardar |
|---|---|---|---|---|
| 1 | `vacio.bin` (0 B) | Cifrar | Aviso «El archivo está vacío (EMPTY_FILE)»; **no** se crea salida | captura del aviso + `ls -l` sin `.enc` |
| 2 | `uno.bin` (1 B) | Cifrar y descifrar | `.enc` creado y original recuperado byte a byte | `cmp uno.bin uno.bin.recuperado` sin salida |
| 3 | `un_mib.bin` (1 MiB) | Cifrar | Progreso real 0–100 %, 1,0 MiB enviados, `X-Integrity: verified` al descifrar | captura durante la transferencia + `ls -l` |
| 4 | `veinte_mib.bin` (20 MiB) | Cifrar y descifrar | Progreso por bloques de 64 KiB, sin subir el uso de memoria, resultado íntegro | `cmp` + captura del progreso |
| 5 | `veinte_mib.bin` | Cancelar a mitad | «Operación cancelada.», sin `.part` ni salida parcial | captura + `ls -la` del directorio |

Comprobación de integridad y protección del original:

```bash
cmp veinte_mib.bin veinte_mib.bin.recuperado     # sin salida = idénticos
ls -l veinte_mib.bin.*                           # no debe quedar ningún .runnersz-*.part
```

Prueba negativa de integridad (servidor que no manda `X-Integrity`): el cliente muestra «La API no
confirmó la integridad del fichero…» y **no** publica el resultado. Esta rama ya está cubierta de
forma automática en `DesktopControllerTest.descifrarSinIntegridadNoPublicaElResultado`.

## 5. Accesibilidad y tipografía (§9.2 y §9.3)

Comprobable sin servidor:

```bash
cd desktop
./mvnw -B test -Dtest=DesktopControllerTest    # roles, nombres y ayudas de todos los controles
java -cp "target/classes;target/test-classes;$(cat target/cp.txt)" com.runnersz.support.UiPreview
```

`UiPreview` abre la vista real, comprueba que **todos** los controles tienen ayuda emergente y guarda
una captura de cada pestaña en `target/ui-1.png` … `target/ui-3.png`.

| Requisito del contrato | Dónde se cumple | Cómo comprobarlo |
|---|---|---|
| Rol y nombre accesible en cada control | `DesktopController.installAccessibility()` | `DesktopControllerTest.cadaControlDeclaraSuRol` y `cadaBotonTieneRolNombreAyuda` |
| Ayuda contextual (`aria-describedby`) | `installTooltips()` + `accessibleHelp` del FXML | `UiPreview`: «tooltips: todos los controles tienen ayuda» |
| Región en vivo | barra de estado y aviso no bloqueante con `setAccessibleText` | `DesktopControllerTest.mostrarLaContrasena…` y avisos |
| Foco y orden de lectura | borde de 2 px, `Alt+C`/`Alt+D`, menú de edición traducido | `elMenuDeEdicionEstaTraducidoYRespetaLaSeleccion` |
| Objetivo ≥ 32 px | `.button` 34 px, botones de operación 38 px | `runnersz.css` |
| Contraste | `#1B2A41` sobre `#DDD0B0` ≈ 9:1; oro solo como fondo con texto azul | `runnersz.css` |
| Base relativa en `em` | `.root` 16 px (12 pt) y el resto en `em` sobre esa base | `DesktopControllerTest.laTipografiaUsaEmSobreUnaBaseDe16Px` |
| Títulos 20 pt | `.section-title` 1,6875 em = 27 px ≈ 20,25 pt; `.brand` 1,875 em = 30 px ≈ 22,5 pt | `runnersz.css` |
| Cuerpo y botones 12 pt | `.root` 16 px = 12 pt, `.button` 1 em | `runnersz.css` |
| Textos largos y ayudas 12 pt | `.muted`, `.helper`, `.drop-hint`, `.status-text` 1 em; `.tooltip` 16 px | `runnersz.css` |
| Texto cifrado 14 pt monoespaciada | `.mono` y `.shift-value` 1,1875 em = 19 px ≈ 14,25 pt | `runnersz.css` |
| Tamaños en píxeles enteros (sin pixelado) | 16/19/27/30 px: ningún tamaño cae en fracciones | `DesktopControllerTest.laTipografiaUsaEmSobreUnaBaseDe16Px` |

## 6. Registro de la aplicación (log)

| Requisito | Dónde se cumple | Cómo comprobarlo |
|---|---|---|
| El registro nunca se versiona | `.gitignore`: `*.log`, `*.log.gz`, `logs/` | `git status --short` después de usar la aplicación |
| Fuera del repositorio y del JAR | `Logs.directory()` usa la carpeta de registros del usuario | arrancar y leer la línea «Registro en …» |
| Rotación y retención | `RollingFileAppender`: 5 MB o un día, 7 días, 50 MB | `LogsTest.elRegistroEscribeNivelFechaYMensaje` |
| Sin secretos en el fichero | `Logs.redact` enmascara `token`, `password`, `clave` y `Bearer` | `LogsTest.losSecretosSeEnmascaran` y `elErrorSeRegistraConSuTraza` |
| Ruta propia de cada sistema | `Logs.defaultDirectory` | `LogsTest.laCarpetaPorDefectoDependeDelSistemaOperativo` |
| El usuario puede verlo | Ajustes → Ver registro | `DesktopControllerTest.elVisorDelRegistroSeAbreConSuRuta` |

En este equipo (Windows) la ruta por omisión es `%LOCALAPPDATA%\RunnersZ\logs\runnersz.log`, es decir
`C:\Users\alain\AppData\Local\RunnersZ\logs\runnersz.log`. En las pruebas se escribe en
`desktop/target/logs/`, que también queda ignorado.
