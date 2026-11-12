# RunnersZ · Cliente de escritorio (JavaFX)

Cliente de escritorio del Reto 2. Cifra y descifra ficheros a través de la API HTTP del servidor
RunnersZ y ofrece las pestañas de Vigenère y César. **El cliente no implementa criptografía**: toda
la lógica vive en el servidor (§3 del contrato).

## Requisitos

- JDK 21 (Temurin 21.0.x probado).
- Maven 3.9 o el wrapper incluido (`./mvnw`).
- Sin dependencias nativas que instalar aparte: el fat JAR lleva dentro las de JavaFX de Windows y
  de Linux, así que **el mismo fichero** se ejecuta en los dos sistemas con JDK 21.

## Compilación y pruebas

```bash
cd desktop
./mvnw clean verify        # compila, ejecuta JUnit 5 y aplica el umbral JaCoCo del 80 %
./mvnw javafx:run          # abre la aplicación desde el código fuente
./mvnw package             # genera el fat JAR único para Windows y Linux
java -jar target/runnersz-desktop.jar         # Windows o Linux (Debian/Ubuntu)
```

### Exportación: un solo fat JAR para Windows y Linux

El pliego (§4.8) pide **la misma aplicación para Windows y para Linux, cambiando solo la
exportación**: el código es idéntico porque JavaFX lleva sus nativos dentro del JAR. El `pom.xml`
mete en un único artefacto los nativos de los dos sistemas, de modo que el mismo fichero arranca en
ambos:

| Comando | Artefacto |
|---|---|
| `./mvnw -B package` (en cualquier sistema) | `target/runnersz-desktop.jar` |

Para comprobar que lleva los nativos de las dos plataformas:

```bash
unzip -l target/runnersz-desktop.jar | grep -c '\.dll$'   # 54 (Windows)
unzip -l target/runnersz-desktop.jar | grep -c '\.so$'    # 10 (Linux)
```

Las clases comunes de los dos clasificadores de JavaFX son idénticas byte a byte; cada plataforma
solo añade lo suyo (en Windows `glass/ui/win` y `prism/d3d`, en Linux `glass/ui/gtk` y `prism/es2` con
sus shaders), y el `pom.xml` copia del JAR de Linux únicamente esos paquetes y los `.so`, sin
duplicar entradas. JavaFX carga en tiempo de ejecución los nativos de su sistema.

En Linux hace falta un entorno gráfico con GTK 3 (`libgtk-3-0`) y fontconfig, que es lo que el
contrato llama «Linux Debian/Ubuntu con entorno gráfico». El JDK 21 no trae JavaFX: por eso el JAR
incluye clases y nativos, y la entrada `Launcher` arranca sin configurar la ruta de módulos.

`mvn verify` falla si la cobertura de líneas **del proyecto** baja del 80 %, el mínimo que pide el
pliego (JaCoCo `check` sin filtros de paquete). El informe queda en `target/site/jacoco/index.html`.

Medición actual: **82,0 %** (936 de 1142 líneas), también por encima del 80 % en cada capa. El
desglose por paquete y el procedimiento para repetirlo están en
[`docs/evidencias.md`](docs/evidencias.md).

## Configuración de la conexión

La aplicación **no lleva ninguna dirección ni token escritos en el código**. En la primera ejecución
aparece el chip «Sin configurar»:

1. Pulsa **Ajustes**.
2. Escribe la dirección base del servidor (por ejemplo `https://servidor:8000` o
   `http://localhost:8000`). No incluyas `/api/v1`: el cliente lo añade.
3. Escribe el token Bearer que usa el servidor.
4. **Probar** llama a `GET /api/v1/auth/check` y muestra el resultado dentro del propio diálogo.
   **Guardar** conserva los ajustes en las preferencias locales del usuario.

Los ajustes se guardan en el nodo `com/runnersz` de las preferencias del sistema y se migran
automáticamente desde el nodo heredado `/org/example`. El token nunca se registra en los mensajes de
error ni se imprime en la interfaz.

## Uso

### Ficheros · AES

1. **Elegir…** o arrastra un fichero hasta la tarjeta.
2. El destino se propone solo: `nombre.enc` al cifrar y `nombre.recuperado` al descifrar.
3. Escribe la contraseña (puedes mostrarla con **Mostrar**).
4. **Cifrar archivo** (`Alt+C`) o **Descifrar archivo** (`Alt+D`).

Durante la transferencia aparecen el porcentaje real, los MiB enviados y **Cancelar operación**. Al
descifrar, el cliente exige la cabecera `X-Integrity: verified`; si falta, no publica el resultado.
La contraseña se borra del formulario al terminar y nunca se guarda.

### Mensajes · Vigenère

Carga o escribe el texto, indica una clave de letras A–Z y cifra o descifra. El resultado se puede
copiar, guardar como `.txt` o devolver al cuadro de entrada con **Intercambiar**.

### Mensajes · César

Igual que Vigenère, con un desplazamiento entre 1 y 25. El servidor responde `INVALID_SHIFT` si el
valor se sale del rango.

## Accesibilidad (§9.3)

- Todos los controles declaran su rol (`setAccessibleRole`) y su nombre (`setAccessibleText`); los
  botones de icono llevan además la descripción (`setAccessibleHelp`).
- Todos tienen ayuda contextual: ayuda emergente al pasar el ratón o al enfocar con el teclado.
- Objetivos táctiles de 34–38 px de alto.
- Foco visible con borde de 2 px y orden de tabulación natural.
- Atajos: `Alt+C` cifrar, `Alt+D` descifrar, menú contextual de edición traducido.
- El estado y los avisos se anuncian en la barra inferior y en el aviso no bloqueante.
- Las tres pestañas y el pie viven dentro de `ScrollPane`, así que el contenido no se recorta al
  aumentar la fuente del sistema.

## Estructura

```
src/main/java/com/runnersz/
  Launcher.java              entrada del fat JAR
  Main.java                  carga FXML, registra fuentes y recarga al cambiar de idioma
  controller/DesktopController.java   controlador de la vista
  controller/UiState.java             estado del formulario al recargar la vista
  service/ApiClient.java              HTTP, multipart por bloques, JSON y errores
  service/FileOperationService.java   validar → transferir a temporal → publicar
  service/FileService.java            temporales, movimiento atómico y textos
  service/SettingsStore.java          ajustes validados en preferencias
  model/ConnectionSettings.java       instantánea inmutable
  utiles/I18n.java                    catálogos y errores traducidos
src/main/resources/com/runnersz/
  desktop.fxml               vista declarativa completa
  estilo/runnersz.css        paleta y tipografías del contrato
  fonts/                     Inter y Open Sans (OFL) empaquetadas
  i18n/messages*.properties  es, eu, en
```

Decisiones relevantes:

- **FXML real**: la ventana se carga con `FXMLLoader` y `ResourceBundle`; no hay recursos muertos.
- **Tipografía del §9.2 en puntos**: marca 22 pt, títulos 20 pt, cuerpo, ayudas y botones 12 pt, y
  resultados monoespaciados 14 pt. Inter y Open Sans van empaquetadas en el JAR.
- **Paleta**: el `#DDD0B0` de los botones secundarios no es un color nuevo, es un tono derivado del
  pergamino `#E8DCC6`. Con `#1B2A41` encima supera 9:1 de contraste y separa el botón del fondo.
- **Streaming**: el cuerpo multipart se emite en bloques de 64 KiB y la respuesta se escribe
  directamente en un temporal, así que la memoria no depende del tamaño del fichero (§7.1).
- **Timeout de 120 s** en todas las peticiones (§7.2) y botones **Reintentar** / **Ir a Ajustes** en
  los errores de red.
- **Publicación segura**: el resultado se escribe en `.runnersz-*.part` junto al destino y se mueve
  con `ATOMIC_MOVE`; un fallo no deja ficheros a medias.
- **Errores del contrato**: `UNAUTHORIZED`, `EMPTY_FILE`, `EMPTY_PASSWORD`, `INVALID_KEY`,
  `INVALID_SHIFT`, `FILE_TOO_LARGE`, `DECRYPT_FAILED` e `INTERNAL_ERROR` se traducen a los tres
  idiomas.

## Pruebas

`./mvnw verify` ejecuta 83 pruebas JUnit 5 contra un servidor HTTP local (`StubApi`), sin Docker ni
red externa:

| Clase | Qué comprueba |
|---|---|
| `ApiClientTest` | rutas, token, multipart por bloques, acentos en el nombre, `X-Integrity`, 401/413/500, red caída, contraseña vacía o con saltos de línea |
| `FileOperationServiceTest` | fichero vacío, salida que es carpeta, protección del original, publicación y limpieza de temporales |
| `FileServiceTest` | temporales junto al destino, reemplazo atómico, textos UTF-8 |
| `SettingsStoreTest` | sin credenciales por defecto, normalización de `/api/v1`, validación de URL y token, migración |
| `DesktopControllerTest` | roles y nombres accesibles, estado del formulario, selección de ficheros, desplazamiento, avisos, diálogo de Ajustes, menú de edición, portapapeles y un cifrado completo (carga el FXML real) |
| `MainTest` | arranque de la ventana, título, fuentes empaquetadas y parada sin ventanas ni tareas vivas |
| `I18nTest` | paridad de claves en los cuatro catálogos, claves del FXML, formato por idioma |

Las pruebas usan preferencias en memoria (`MemoryPreferencesFactory`), de modo que no tocan la
configuración real del usuario.

`DesktopControllerTest` carga `desktop.fxml` en el hilo de JavaFX y cierra los diálogos modales desde
un vigilante, así que comprueba también qué aviso aparece sin necesidad de interacción. Si el
entorno no puede arrancar el motor gráfico, la clase se omite; en integración continua se ejecuta
bajo Xvfb.

## Integración continua

`.github/workflows/desktop.yml` ejecuta en cada push y PR: JDK 21, `mvn --batch-mode verify`, informe
JaCoCo y fat JAR como artefactos. En local, `act push -j desktop` reproduce el job con la imagen
fijada en `.actrc`.

## Editar la vista en Scene Builder

`desktop.fxml` usa solo controles estándar de JavaFX, así que se abre en cualquier versión de Scene
Builder sin instalar librerías ni tocar su classpath. Los candados de los botones los crea
`DesktopController` con Ikonli al arrancar: en el editor los botones se ven sin icono, y es lo
esperado, porque al ejecutar la aplicación sí aparecen.

- Abre `src/main/resources/com/runnersz/desktop.fxml` con **File → Open**. Arrastrar el fichero sobre
  el icono de la aplicación, o abrirlo por línea de órdenes, también funciona; en versiones antiguas
  la carga desde la línea de órdenes se hacía antes de registrar librerías y fallaba.
- Los textos aparecen como `%files.title`, `%caesar.shift`…: las traducciones las inyecta
  `FXMLLoader` desde `i18n/` al ejecutar, y Scene Builder lee el FXML en crudo.
- Para ver colores y tipografías reales: **Preview → Scene Style Sheets → Add a Style Sheet…** y elige
  `estilo/runnersz.css`. Las fuentes de `fonts/` las registra la aplicación en tiempo de ejecución, así
  que el editor usará la fuente del sistema.
- El controlador no está en el classpath del editor: el panel *Controller* sale vacío y los `onAction`
  se conservan como texto en el FXML.

## Documentación

- [`docs/codigo.md`](docs/codigo.md): qué hace cada capa del cliente, cómo se reparten los hilos y
  por qué está separada así.
- [`docs/evidencias.md`](docs/evidencias.md): pruebas, cobertura por paquete, empaquetado y el
  procedimiento de las pruebas manuales del Anexo VI.4 (0 B, 1 B, 1 MiB, 20 MiB).

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| «Configura la dirección del servidor y el token» | Falta configuración: abre Ajustes y pulsa Probar. |
| «El token falta o no es válido» | El token del servidor no coincide con el guardado. |
| «No se pudo contactar con el servidor» | Servidor apagado, IP/puerto incorrectos o cortafuegos. |
| «La API no confirmó la integridad» | El servidor no devolvió `X-Integrity: verified`; el fichero no se guardó. |
| «Contraseña incorrecta o fichero dañado» | La clave AES no corresponde al `.enc` o el fichero está alterado. |
