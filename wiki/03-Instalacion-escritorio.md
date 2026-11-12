# 03 · Instalación del cliente de escritorio

Aplicación **Java 21 + JavaFX** que cifra y descifra ficheros a través de la API, y ofrece las pestañas de Vigenère y César. **No implementa criptografía**: todo ocurre en el servidor.

> 🐧 **¿Linux?** Los comandos equivalentes están en [14 · Ejecutar todo en Linux](14-Linux.md).

---

## Antes de empezar

- JDK **21** instalado y en el `PATH` ([01 · Requisitos](01-Requisitos.md#4-escritorio-y-android-jdk-21)).
- No hace falta instalar Maven: el repositorio trae el wrapper `mvnw`.
- La API arrancada y con token a mano ([02](02-Instalacion-servidor.md)).

Comprobación:

```powershell
java -version
```

✅ Debe decir `21.x`.

---

## Paso 1 — Compilar, probar y medir cobertura

Windows (PowerShell):

```powershell
cd <ruta-del-repositorio>\desktop
.\mvnw.cmd clean verify
```

Linux / macOS:

```bash
cd <ruta-del-repositorio>/desktop
./mvnw clean verify
```

En Git Bash también funciona `./mvnw`. Si en Linux da "permiso denegado": `chmod +x mvnw`.

Qué hace `verify`:

1. Compila con `release 21` y `-Xlint:all`.
2. Ejecuta **64 pruebas JUnit 5** contra un servidor HTTP local de pruebas (`StubApi` falso).
3. Aplica el umbral **JaCoCo del 80 % de líneas del proyecto**: si baja, el build **falla**.

✅ Debes ver `BUILD SUCCESS` y, al final, el chequeo de cobertura:

```text
All coverage checks have been met.
```

El informe navegable queda en `desktop/target/site/jacoco/index.html`.

> Medición de referencia del repositorio: **82,0 %** (936 de 1142 líneas).

---

## Paso 2 — Generar el ejecutable (fat JAR)

```powershell
.\mvnw.cmd package
```

✅ Se genera **un solo JAR** que arranca igual en Windows y en Linux:

| Sistema donde compilas | Comando | Artefacto |
|---|---|---|
| Windows | `.\mvnw.cmd -B package` | `target/runnersz-desktop.jar` |
| Linux | `./mvnw -B package` | `target/runnersz-desktop.jar` |

El JAR lleva dentro los **nativos de JavaFX de los dos sistemas** (54 `.dll` y 10 `.so`); JavaFX carga los de su plataforma al arrancar. Comprobación opcional:

```bash
unzip -l target/runnersz-desktop.jar | grep -c '\.dll$'   # 54
unzip -l target/runnersz-desktop.jar | grep -c '\.so$'    # 10
```

---

## Paso 3 — Abrir la aplicación

Desde el código fuente:

```powershell
.\mvnw.cmd javafx:run
```

Desde el JAR (recomendado para la demo, no necesita Maven):

```powershell
java -jar target\runnersz-desktop.jar
```

```bash
java -jar target/runnersz-desktop.jar
```

✅ Se abre la ventana **RunnersZ** con el chip **«Sin configurar»** la primera vez.

En Linux hace falta un entorno gráfico con **GTK 3** (`libgtk-3-0`) y `fontconfig`. En un servidor sin pantalla, usa Xvfb (`xvfb-run`) o ejecútalo en el escritorio.

---

## Paso 4 — Configurar la conexión (obligatorio)

La aplicación **no lleva dirección ni token escritos en el código**.

1. Pulsa **Ajustes**.
2. Escribe la dirección base del servidor, **sin** `/api/v1` (el cliente lo añade solo):
   - Mismo PC: `http://localhost:8000`
   - Otro equipo de la red: `http://192.168.1.50:8000`
3. Escribe el **token Bearer** de `backend/.env`.
4. Pulsa **Probar**: llama a `GET /api/v1/auth/check`.
5. Pulsa **Guardar**.

✅ La prueba debe indicar éxito (`204`); el chip de la barra deja de decir «Sin configurar».

Los ajustes se guardan en las **preferencias del sistema**, nodo `com/runnersz` (se migran automáticamente desde el nodo antiguo `/org/example`). El token nunca se registra en logs ni se muestra en pantalla.

> ¿Qué pasa si la dirección lleva `/api/v1`? El cliente lo normaliza y no lo duplica. Aun así, lo limpio es escribir solo `http://host:puerto`.

---

## Paso 5 — Primera prueba de extremo a extremo

1. Pestaña **Ficheros · AES**.
2. **Elegir…** un archivo pequeño (`nota.txt`).
3. Escribe una contraseña (por ejemplo `clave123`).
4. **Cifrar archivo** (`Alt+C`).
5. Cuando termine, el resultado queda como `nota.txt.enc` junto al original.
6. Cambia a **Descifrar archivo** (`Alt+D`) con el mismo `.enc` y la misma contraseña.
7. Se genera `nota.txt.recuperado` y debe coincidir con el original.

✅ La barra inferior anuncia la operación completada y el resultado aparece en la carpeta elegida.

Uso detallado de cada pestaña: [07 · Uso del escritorio](07-Uso-escritorio.md).

---

## Estructura del cliente

```text
desktop/src/main/java/com/runnersz/
  Launcher.java              entrada del fat JAR
  Main.java                  carga FXML, registra fuentes, recarga al cambiar idioma
  controller/DesktopController.java   controlador de la vista
  controller/UiState.java             estado del formulario al recargar
  service/ApiClient.java              HTTP, multipart por bloques, JSON y errores
  service/FileOperationService.java   validar → transferir a temporal → publicar
  service/FileService.java            temporales, movimiento atómico y textos
  service/SettingsStore.java          ajustes validados en preferencias
  model/ConnectionSettings.java       instantánea inmutable
  utiles/I18n.java                    catálogos y errores traducidos
desktop/src/main/resources/com/runnersz/
  desktop.fxml               vista declarativa completa
  estilo/runnersz.css        paleta y tipografías del contrato
  fonts/                     Inter y Open Sans (OFL) empaquetadas
  i18n/messages*.properties  es, eu, en
```

Decisiones que conviene conocer:

- **Streaming**: el multipart se emite en bloques de 64 KiB y la respuesta se escribe directamente a un temporal, así que la memoria no depende del tamaño del fichero (§7.1).
- **Timeout de 120 s** en todas las peticiones (§7.2).
- **Publicación segura**: el resultado se escribe en `.runnersz-*.part` junto al destino y se mueve con `ATOMIC_MOVE`; un fallo no deja ficheros a medias y **el original nunca se modifica**.
- **Idiomas**: español, euskera e inglés, con el selector de la cabecera; al cambiar se recarga la vista.
- **Accesibilidad (§9.3)**: roles y nombres accesibles, ayuda contextual, foco visible de 2 px, objetivos de 34–38 px y atajos `Alt+C` / `Alt+D`.

---

## Ejecutar solo las pruebas o regenerar la cobertura

```powershell
.\mvnw.cmd test                        # solo pruebas
.\mvnw.cmd verify                      # pruebas + umbral
start target\site\jacoco\index.html    # abrir el informe (Windows)
```

En entornos sin pantalla, las pruebas del controlador necesitan un motor gráfico; en el CI se ejecutan con `xvfb-run --auto-servernum mvn --batch-mode verify`.

---

## Problemas típicos

| Síntoma | Solución |
|---|---|
| `UnsupportedClassVersionError` o `release version 21 not supported` | Estás con un JDK distinto de 21: ajusta `JAVA_HOME` |
| `mvnw: Permission denied` en Linux | `chmod +x mvnw` |
| La ventana no abre en Linux | Falta GTK 3 o no hay entorno gráfico |
| «Configura la dirección del servidor y el token» | Abre **Ajustes**, escribe URL y token, pulsa **Probar** |
| El build falla por cobertura | Alguna prueba no cubre código nuevo: revisa `target/site/jacoco` |

Lista completa: [11 · Problemas frecuentes](11-Problemas-frecuentes.md).

---

## Siguiente paso

- Aprender a usar cada pestaña: [07 · Uso del escritorio](07-Uso-escritorio.md)
- Probar la app Android contra el mismo servidor: [04 · Instalación de Android](04-Instalacion-android.md)

[Volver al índice](Home.md)
