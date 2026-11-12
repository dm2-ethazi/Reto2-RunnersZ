# 07 · Uso del cliente de escritorio

Guía de la aplicación JavaFX: qué hay en cada pestaña, cómo se cifra y descifra un fichero y qué hacer cuando algo falla.

---

## 1. Arrancar

```powershell
java -jar desktop\target\runnersz-desktop.jar
```

```bash
java -jar desktop/target/runnersz-desktop.jar
```

La primera vez aparece el chip **«Sin configurar»**. Configura la conexión antes de nada:

1. **Ajustes**.
2. Dirección base **sin** `/api/v1`: `http://localhost:8000` o `http://192.168.1.50:8000`.
3. Token Bearer del servidor.
4. **Probar** → debe responder correctamente (204).
5. **Guardar**.

Los ajustes quedan en las preferencias del sistema (nodo `com/runnersz`) y se conservan entre ejecuciones. El token nunca se imprime ni se registra en logs.

---

## 2. La ventana

- **Cabecera**: marca RunnersZ, selector de idioma (español, euskera, inglés), chip de estado de la conexión y botón **Ajustes**.
- **Tres pestañas**: **Ficheros · AES**, **Mensajes · Vigenère**, **Mensajes · César**.
- **Barra inferior**: estado de la operación y avisos. Los errores de red ofrecen **Reintentar** e **Ir a Ajustes**.
- Todo el contenido va dentro de un `ScrollPane`: si aumentas la fuente del sistema, nada se recorta.

---

## 3. Pestaña «Ficheros · AES»

Cifra o descifra un fichero contra la API usando una contraseña propia.

### Cifrar

1. Pulsa **Elegir…** o **arrastra** el fichero hasta la tarjeta.
2. El destino se propone solo: `nombre.enc`.
3. Escribe la contraseña (puedes verla con **Mostrar**).
4. Pulsa **Cifrar archivo** o `Alt+C`.

Durante la transferencia se muestran el **porcentaje real**, los **MiB enviados** y **Cancelar operación**.

✅ Al terminar, junto al original aparece `nombre.enc` y la barra inferior anuncia el resultado.

### Descifrar

1. Elige el `.enc`.
2. El destino se propone como `nombre.recuperado`.
3. Escribe la **misma** contraseña usada al cifrar.
4. Pulsa **Descifrar archivo** o `Alt+D`.

✅ El cliente exige la cabecera `X-Integrity: verified`. Si el servidor no la devuelve, **no publica el resultado** y muestra un error de integridad.

### Comportamiento seguro

- El **original nunca se modifica**.
- El resultado se escribe en un temporal `.runnersz-*.part` junto al destino y se mueve con `ATOMIC_MOVE`: si algo falla, no quedan ficheros a medias.
- La contraseña se borra del formulario al terminar y **no se guarda** en ningún sitio.
- Si el destino es una carpeta o no hay permisos, se avisa antes de transferir.

---

## 4. Pestaña «Mensajes · Vigenère»

1. Escribe el texto o cárgalo desde un `.txt`.
2. Indica una clave de letras **A–Z** (por ejemplo `LUNA`). Se repite sola.
3. Pulsa cifrar o descifrar.

Con el resultado puedes:

- **Copiar** al portapapeles.
- **Guardar** como `.txt`.
- **Intercambiar**: devuelve el resultado al cuadro de entrada para encadenar operaciones.

Tildes, Ñ, signos y espacios se conservan sin consumir clave. Clave inválida → error `INVALID_KEY`.

---

## 5. Pestaña «Mensajes · César»

Igual que Vigenère, pero con un **desplazamiento entre 1 y 25**.

- Un valor fuera de rango (0, 26, negativo, decimal) produce `INVALID_SHIFT` y el mensaje del contrato.
- El resultado se puede copiar, guardar o intercambiar igual que en Vigenère.

Ejemplo: `Ataque al amanecer` con desplazamiento 3 → `Dwdtxh do dpdqhfhu`.

---

## 6. Idiomas

El selector de la cabecera cambia entre **español, euskera e inglés** sin reiniciar. La vista se recarga con `FXMLLoader` y el `ResourceBundle` correspondiente; el formulario mantiene su estado (`UiState`).

---

## 7. Accesibilidad (§9.3)

- Todos los controles declaran rol (`setAccessibleRole`) y nombre (`setAccessibleText`); los botones de icono añaden descripción (`setAccessibleHelp`).
- Ayuda contextual al pasar el ratón o al enfocar con el teclado.
- Objetivos táctiles de 34–38 px de alto.
- Foco visible con borde de 2 px y orden de tabulación natural.
- Atajos: **`Alt+C`** cifrar, **`Alt+D`** descifrar.
- Estado y avisos se anuncian en la barra inferior y en el aviso no bloqueante.

---

## 8. Errores frecuentes y qué hacer

| Mensaje | Causa | Solución |
|---|---|---|
| «Configura la dirección del servidor y el token» | Ajustes vacíos | Ajustes → URL + token → Probar → Guardar |
| «El token falta o no es válido» | Token distinto al del servidor | Copia el valor de `backend/.env` |
| «No se pudo contactar con el servidor» | Servidor apagado, IP/puerto mal o cortafuegos | Arranca la API, revisa la IP, prueba `/health` en el navegador |
| «La API no confirmó la integridad» | Falta `X-Integrity: verified` | Revisa que el servidor sea el del proyecto |
| «Contraseña incorrecta o fichero dañado» | Contraseña distinta o `.enc` alterado | Usa la contraseña original; no edites el `.enc` |
| «El archivo está vacío» | 0 bytes | Elige un fichero con contenido |
| «El archivo supera el límite» | Más de 100 MiB | Sube `RZ_MAX_UPLOAD_MIB` o usa otro fichero |

Si el problema es de red y el servidor está en otro equipo: el cortafuegos debe permitir el puerto 8000 y la API debe escuchar en `0.0.0.0`, no solo en `127.0.0.1`.

---

## 9. Notas para quien mantiene el cliente

- **FXML real**: `desktop.fxml` se carga con `FXMLLoader`; los textos son claves `%...` que se resuelven desde `i18n/`.
- Los **iconos** de los botones los crea `DesktopController` con Ikonli al arrancar; en Scene Builder se ven sin icono.
- Para editar la vista: abre `desktop.fxml` en Scene Builder y añade `estilo/runnersz.css` en *Preview → Scene Style Sheets*. Las fuentes de `fonts/` se registran en tiempo de ejecución.
- **Streaming**: multipart en bloques de 64 KiB; la memoria no depende del tamaño del fichero.
- **Timeout**: 120 s en todas las peticiones.

El código está separado en capas (`controller`, `service`, `model`, `utiles`); el detalle de cada clase está en [03 · Instalación del escritorio](03-Instalacion-escritorio.md#estructura-del-cliente).

---

## 10. Ejecutar en Linux

El cliente es el mismo en Windows y Linux: solo cambian los **nativos de JavaFX** que van dentro del JAR. En Linux necesitas **JDK 21**, **GTK 3** y **fontconfig**.

### Paquetes y JDK

```bash
sudo apt update
sudo apt install -y libgtk-3-0 fontconfig
sudo apt install -y openjdk-21-jdk      # o temurin-21-jdk si tu distro no lo tiene
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

✅ Debe decir `openjdk version "21.x"`. Gradle y Maven no aceptan JDK 25 para este proyecto.

### Compilar, probar y abrir

```bash
cd ~/Reto2-RunnersZ/desktop
chmod +x mvnw
./mvnw clean verify
./mvnw -B package
java -jar target/runnersz-desktop-linux.jar
```

✅ `BUILD SUCCESS`, cobertura igual o superior al 80 % y se abre la ventana **RunnersZ**.

- El perfil de Maven elige solo la plataforma `linux`, así que el JAR lleva los nativos `.so` de JavaFX. Comprobación: `unzip -l target/runnersz-desktop-linux.jar | grep -c '\.so$'` → **10**.
- También puedes abrirlo desde el código con `./mvnw javafx:run`.
- El JAR se ejecuta con `java -jar` y JDK 21; no hace falta configurar la ruta de módulos de JavaFX.

### Generar el JAR de Windows desde Linux

```bash
./mvnw -B package -Djavafx.platform=win
```

Genera `target/runnersz-desktop-win.jar`. Ese artefacto se ejecuta en Windows, no en Linux.

### Sin entorno gráfico

```bash
sudo apt install -y xvfb
xvfb-run --auto-servernum ./mvnw verify
```

Así se ejecutan las pruebas (cargan el FXML real) en un servidor. La aplicación en sí necesita una sesión gráfica: por SSH sin X11 verás `Unable to open DISPLAY`.

### Conectar con la API

**Ajustes** → `http://localhost:8000` (o `http://<IP_DEL_PC>:8000`, sin `/api/v1`) → token → **Probar** → **Guardar**. Si el servidor está en otro equipo, arráncalo con `--host 0.0.0.0` y permite el puerto 8000 en el cortafuegos (`sudo ufw allow 8000/tcp`).

### Problemas típicos en Linux

| Síntoma | Causa | Solución |
|---|---|---|
| `./mvnw: Permission denied` | Sin permiso de ejecución | `chmod +x mvnw` |
| `libgtk-3.so.0: cannot open shared object file` | Falta GTK 3 | `sudo apt install libgtk-3-0` |
| `Unable to open DISPLAY` | Sin sesión gráfica | Abre la app en el escritorio o usa `xvfb-run` para pruebas |
| `java -version` no es 21 | JDK del sistema | Instala JDK 21 y exporta `JAVA_HOME` |
| `ExceptionInInitializerError` en JavaFX | JAR de otra plataforma | Recompila en Linux con `./mvnw -B package` |
| El JAR de Windows no arranca | Nativos de Windows | Ejecútalo en Windows, no en Linux |

---

## Siguiente paso

- [08 · Uso de Android](08-Uso-android.md)
- [10 · Verificación y demo](10-Verificacion-y-demo.md)

[Volver al índice](Home.md)
