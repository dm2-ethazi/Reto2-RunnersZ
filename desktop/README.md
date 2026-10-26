# Desktop — JavaFX 21

Cliente para ficheros AES y mensajes Vigenère, en capas controller/service/model.

## Preparar y compilar (desde la raíz)

Prepara antes Python con `py -3.12 scripts/prepare_python.py`. Configura JAVA_HOME
al JDK 21. Maven Wrapper fija Maven 3.9.9; versiones JavaFX/JUnit/JaCoCo/Shade
están en pom.xml, sin dependencias de pruebas GUI adicionales.

```powershell
.\desktop\mvnw.cmd -f desktop/pom.xml clean verify
java -jar desktop/target/reto2-desktop.jar
```

En Linux instala Xvfb para pruebas gráficas y ejecuta:

```sh
xvfb-run -a desktop/mvnw -f desktop/pom.xml -Djavafx.platform=linux clean verify
java -jar desktop/target/reto2-desktop.jar
```

Los tests JavaFX abren una ventana de prueba y cierran automáticamente sus
alertas. No requieren interacción. JaCoCo exige >=80 % de líneas del módulo
completo, incluida la interfaz; informe en target/site/jacoco/index.html.

## Arquitectura

- App/Launcher: ventana principal y ciclo de vida JavaFX.
- MainController: conexión, reintento, pestañas y apagado.
- AesController/VigenereController: formularios, tareas fuera del hilo gráfico,
  estado/progreso y errores controlados mediante UiAlerts.
- BackendService: HTTP/JSON local. BackendLauncher: puerto libre, proceso hijo,
  espera de health y cierre. BundledBackend: extracción del código incluido.
- FileTextService: lectura/escritura UTF-8 con cierre garantizado.
- AesResult: datos de resultado e integridad.

## Uso

AES: selecciona entrada y una salida NUEVA, escribe la contraseña oculta y pulsa
Cifrar/Descifrar. La barra indeterminada indica trabajo, no un porcentaje medido.
Al descifrar debe aparecer Integridad verificada. Nunca uses el original como salida.
Vigenère: escribe/carga texto UTF-8, clave solo A-Z y cifra/descifra; guarda el resultado.

## Distribución

El JAR incluye dependencias Java y fuentes/requisitos del backend, no Python.
Dentro del repo detecta backend/.venv. Para mover SOLO el JAR, prepara un Python
con backend/requirements.txt y define RETO2_PYTHON con su ruta absoluta.
RETO2_BACKEND_HOME permite elegir un backend externo; por defecto extrae el incluido
si no encuentra carpeta. No instala paquetes ni abre puertos públicos.

La exportación JAR incorpora nativos JavaFX específicos de plataforma. Windows
usa javafx.platform=win y Linux linux; no se promete un binario universal.
Si el backend no arranca, comprueba Python/dependencias y usa Reintentar.
La validación gráfica en Windows no sustituye probar una distribución Linux real.
