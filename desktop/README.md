# Cliente de escritorio JavaFX

Cliente de RunnersZ para el reto 2. Java 21 y JavaFX 21; el servidor FastAPI mantiene toda la lógica criptográfica. El cliente envía el contenido de los archivos o los mensajes por HTTP a la API y presenta los resultados.

## Ejecución y empaquetado

Desde esta carpeta, con JDK 21 y Maven instalados:

```powershell
mvn javafx:run
mvn clean package
```

El paquete genera `target/runnersz-desktop.jar`. La compilación incorpora JavaFX, la interfaz, Inter y Open Sans, junto con los textos de licencia SIL OFL. Se debe generar el artefacto en el mismo sistema operativo de destino (Windows o Linux), conforme al contrato.

En Windows, ejecuta `run.bat` después de empaquetar. La aplicación abre una ventana principal con las pestañas AES y Vigenère; **Ajustes** abre el diálogo de conexión. En Linux, genera el JAR en ese sistema con Maven y ejecútalo con `java -jar target/runnersz-desktop.jar`.

## Uso

1. Abre **Ajustes**, introduce la dirección base del servidor (por ejemplo `http://192.168.1.10:8000`) y el token Bearer configurado en el backend. **Probar** consulta `/api/v1/auth/check`; **Guardar** almacena la configuración en las preferencias locales del usuario.
2. En **Ficheros · AES**, selecciona el archivo, el destino y la contraseña. El cliente envía el contenido multipart a `/api/v1/files/encrypt` o `/api/v1/files/decrypt`; para descifrar exige `X-Integrity: verified` antes de guardar. El resultado se escribe primero en un temporal junto al destino y luego se mueve al nombre elegido. El original nunca se usa como destino implícito; si el destino ya existe, se pide confirmación.
3. En **Mensajes · Vigenère**, escribe un mensaje o abre un `.txt` UTF-8, introduce una clave A–Z y elige cifrar o descifrar. El cliente envía JSON a `/api/v1/text/vigenere`; se puede guardar la respuesta como texto UTF-8.

## Organización y flujo

- `Main`: construye las vistas JavaFX, valida entradas, controla alertas y seleccionadores de archivos y ejecuta las tareas largas con `Task` en un ejecutor fuera del hilo gráfico.
- `Launcher`: punto de entrada JVM normal que llama a JavaFX; evita el error del lanzador de Java al usar directamente una clase principal que extiende `Application` desde un JAR.
- `SettingsStore`: normaliza la dirección y gestiona preferencias locales. La contraseña AES no se guarda. El token se oculta en el formulario.
- `ApiClient`: concentra las peticiones HTTP, cabeceras de autenticación, codificación multipart/JSON y presentación de errores del servidor. No implementa AES, Vigenère ni César.
- `runnersz.css` y `fonts/`: paleta azul noche `#1B2A41`, pergamino `#F4F0E6` y oro `#C9A96A`; texto Inter, ayudas Open Sans, foco visible y contraste del oro solo sobre azul noche.

Las operaciones largas muestran un indicador y no bloquean JavaFX. Los errores de lectura, red, autenticación, contraseña o integridad se comunican mediante alertas y no cierran la aplicación.

## Configuración del servicio

El servidor debe estar disponible desde este equipo, con el token correcto y estas rutas de API: `GET /api/v1/auth/check`, `POST /api/v1/files/encrypt`, `POST /api/v1/files/decrypt` y `POST /api/v1/text/vigenere`. El contrato acepta HTTP en la red local de demostración; en esa modalidad, el tráfico viaja sin TLS.
