# Cliente de escritorio JavaFX

Cliente de RunnersZ para el reto 2. Java 21 y JavaFX 21; el servidor FastAPI mantiene toda la lógica criptográfica. El cliente envía el contenido de los archivos o los mensajes por HTTP a la API y presenta los resultados.

## Ejecución y empaquetado

Desde esta carpeta, con JDK 21 y Maven instalados:

```powershell
mvn javafx:run
mvn clean package
```

El paquete genera `target/runnersz-desktop.jar`. La compilación incorpora JavaFX y la interfaz. Se debe generar el artefacto en el mismo sistema operativo de destino (Windows o Linux), conforme al contrato.

Ejecuta el JAR con `java -jar target/runnersz-desktop.jar`. La aplicación abre una ventana principal con las pestañas AES y Vigenère; **Ajustes** abre el diálogo de conexión. Genera el JAR en el sistema operativo de destino.

## Uso

1. Abre **Ajustes**, introduce la dirección base del servidor (por ejemplo `http://192.168.1.10:8000`) y el token Bearer configurado en el backend. **Probar** consulta `/api/v1/auth/check` y guarda la configuración si la autenticación es correcta. **Guardar** también permite almacenar la configuración sin probarla.
2. En **Ficheros · AES**, selecciona el archivo, el destino y la contraseña. El cliente envía el contenido multipart a `/api/v1/files/encrypt` o `/api/v1/files/decrypt`; para descifrar exige `X-Integrity: verified` antes de guardar. El resultado se escribe primero en un temporal junto al destino y luego se mueve al nombre elegido. El original nunca se usa como destino implícito; si el destino ya existe, se pide confirmación.
3. En **Mensajes · Vigenère**, escribe un mensaje o abre un `.txt` UTF-8, introduce una clave A–Z y elige cifrar o descifrar. El cliente envía JSON a `/api/v1/text/vigenere`; se puede guardar la respuesta como texto UTF-8.

## Organización y flujo

- `com.runnersz.Main`: ciclo de vida de JavaFX; crea y cierra el controlador.
- `com.runnersz.Launcher`: entrada del fat JAR, sin heredar de Application.
- `controller/DesktopController`: controles, eventos, validación de entradas, alertas y tareas de fondo.
- `service/ApiClient`: HTTP, multipart, JSON, token y errores. Exige `X-Integrity: verified` al descifrar.
- `service/FileOperationService`: coordina lectura → API → escritura; impide escribir sobre el original, incluidos enlaces al mismo archivo.
- `service/FileService`: lectura binaria/UTF-8 y escritura mediante temporal, movimiento y limpieza en `finally`.
- `service/SettingsStore`: preferencias locales y validación de ajustes. Conserva el nodo histórico `/org/example` para no perder la configuración al reorganizar paquetes.
- `model/ConnectionSettings` y `model/FileResult`: datos de conexión y respuesta. Cada operación recibe una copia inmutable de los ajustes.
- `runnersz.css`: paleta azul noche `#1B2A41`, pergamino `#F4F0E6` y oro `#C9A96A`; usa Segoe UI y suavizado LCD en Windows, con foco visible y contraste del oro solo sobre azul noche.

Las operaciones largas muestran un indicador y no bloquean JavaFX. Los errores de lectura, red, autenticación, contraseña o integridad se comunican mediante alertas y no cierran la aplicación.

## Configuración del servicio

El servidor debe estar disponible desde este equipo, con el token correcto y estas rutas de API: `GET /api/v1/auth/check`, `POST /api/v1/files/encrypt`, `POST /api/v1/files/decrypt` y `POST /api/v1/text/vigenere`. El contrato acepta HTTP en la red local de demostración; en esa modalidad, el tráfico viaja sin TLS.

## Estructura y decisiones

```text
src/main/java/
  com/runnersz/desktop/
    Main.java
    Launcher.java
    controller/DesktopController.java
    model/ConnectionSettings.java
    model/FileResult.java
    service/ApiClient.java
    service/FileOperationService.java
    service/FileService.java
    service/SettingsStore.java
src/main/resources/com/runnersz/desktop/runnersz.css
```

La separación responde al contrato §5.4: los servicios no importan JavaFX y el
controlador delega red/disco. Las llamadas a la API se ejecutan en `Task`; sus
callbacks actualizan la interfaz en el hilo JavaFX. Se conservan el diseño,
las rutas HTTP y las dependencias existentes (JavaFX y las API estándar de Java).
Los comentarios explican responsabilidades y decisiones, sin repetir cada instrucción.

Las referencias normativas se mantienen **fuera del repositorio**:
`C:\Users\dm2\ia\reglas\docs\pliego\pliego.pdf` y
`C:\Users\dm2\ia\reglas\docs\contrato\contrato.pdf`.
Aunque terminan en .pdf, los archivos facilitados contienen documentos Word
(Open XML). No se crea ninguna carpeta `docs` en el repositorio.

## Trazabilidad de esta reorganización

| Requisito | Origen | Estado de esta tarea | Dónde se cumple |
|---|---|---|---|
| Capas controlador, servicios y modelo | Contrato §5.4 | Implementado | Paquetes controller, service, model |
| Cliente HTTP sin cifrado local | Contrato §3 y §5.2 | Conservado | ApiClient |
| Trabajo de red fuera del hilo gráfico | Contrato §5.4 | Conservado | DesktopController.beginOperation |
| Conservar original y limpiar temporales | Pliego §4.5, contrato §4 y §7.2 | Verificado con servicio HTTP simulado | FileOperationService y FileService |
| Explicación y mantenibilidad | Pliego §4.8–4.9, contrato §13 | Comentarios y guía actualizados; defensa pública pendiente | Código y README |
| Fuentes Inter/Open Sans empaquetadas | Contrato §9.2 | Pendiente previo a esta tarea | CSS actual conserva Segoe UI |
| JUnit y JaCoCo ≥80 % | Contrato §11.2–11.3 | Pendiente previo a esta tarea | pom.xml aún no los configura |
| Pruebas reales AES 1 B, 1 MiB y 20 MiB | Pliego §4.5, contrato §7.3 | Pendiente con servidor real | Backend no disponible en esta revisión |

La implementación existente mantiene los cuerpos HTTP y los ficheros completos
en memoria; **no acredita memoria acotada por bloques en el cliente** (§7.1).
La lectura y el guardado de mensajes UTF-8 continúan en el hilo gráfico.
Se conserva el plazo HTTP de 10 minutos para ficheros, que debe contrastarse
con el límite de 120 segundos descrito en §7.2. Estas limitaciones no se resuelven
añadiendo funcionalidades en una reorganización.

## Comprobación

1. Desde `desktop`, ejecuta `mvn clean verify` para compilar y empaquetar.
   Actualmente no hay suite JUnit: un build correcto no demuestra cobertura.
2. Comprueba manualmente `mvn javafx:run`, ajustes, AES, Vigenère y teclado.
3. Con el servidor real, ejecuta los casos de §7.3 y conserva sus evidencias
   fuera del repositorio según la organización indicada por el usuario.

Durante la reorganización se comprueban servicios con un servidor HTTP simulado:
autenticación, JSON con escapes/Unicode, multipart, integridad ausente, error 422,
rechazo de archivo vacío, protección del original y enlaces, UTF-8 y eliminación
de temporales tras un fallo. Esto no sustituye las pruebas criptográficas reales,
la revisión visual ni la cobertura contractual.

## Resultado de la validación de esta tarea

- En pc_clase, `mvn -B -ntp clean verify`: **BUILD SUCCESS**.
  Maven 3.8.5 y JDK 25.0.2; el compilador utiliza `--release 21`.
- Generado `target/runnersz-desktop.jar` para Windows. Se verificaron la
  entrada `com.runnersz.Launcher`, las clases de arranque/controlador,
  y el recurso CSS en el JAR. Los adaptadores se retiraron en la limpieza posterior.
- Pasaron 20 comprobaciones de servicios en Windows con un servidor HTTP
  simulado, incluida la independencia de la instantánea de ajustes, las
  operaciones sobre enlaces al mismo archivo y la limpieza ante fallo de escritura.
  El comprobador temporal se retiró después de ejecutarlo; no es una suite JUnit.
- El código completo también compiló con JDK 21 y JavaFX 21.0.5 en Linux.
- `git diff --check -- desktop` terminó con código 0. Se comprobaron contenido,
  HEAD e índice, y la conservación de cambios previos ajenos al módulo.
- No se abrió la interfaz gráfica ni se ejecutaron operaciones contra un
  servidor criptográfico real. Quedan pendientes la revisión visual/teclado,
  las pruebas reales de §7.3 y los requisitos señalados arriba.


## Limpieza para el repositorio

Los únicos puntos de entrada vigentes son `com.runnersz.Main`
(ejecución JavaFX) y `com.runnersz.Launcher` (fat JAR).
Se retiraron `src/main/java/org/example` y `src/main/resources/org/example`.
Si una configuración local del IDE utilizaba `org.example.Main`, selecciona
`com.runnersz.Main`; Maven ya utiliza la entrada actual.

El nodo de preferencias `/org/example` permanece de forma intencionada:
es la ubicación de los ajustes locales ya guardados, no una dependencia de
las clases eliminadas. Retirarlo haría perder la configuración del usuario.
`target/` y `.idea/` son archivos locales ignorados; no forman parte de la subida.

### Validación posterior a la limpieza

`mvn -B -ntp clean verify` vuelve a terminar con BUILD SUCCESS tras retirar
los lanzadores de compatibilidad. El JAR se revisa para confirmar que conserva
la entrada y los recursos actuales y no empaqueta las clases de org.example.
Las pruebas de scripts de la raíz suman 41 casos superados; no equivalen a
cobertura JUnit del cliente. Se mantiene pendiente la revisión gráfica y con servidor real.
