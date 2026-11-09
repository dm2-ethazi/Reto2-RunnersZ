# RunnersZ · Reto 2

Proyecto del Reto 2 para el cliente **Hogwarts**. Fecha de entrega: **12 de noviembre de 2026**.

Arquitectura del contrato técnico v2.0. El pliego, el contrato y las normas se mantienen fuera del
repositorio, en `C:\Users\dm2\ia\reglas`, por indicación del responsable:

- **Servidor** Python (FastAPI) en **Docker Compose**: concentra toda la lógica de cifrado y expone
  la API HTTP (rama `feature/api`).
- **Cliente de escritorio** JavaFX: usa la API solo por HTTP.
- **App Android**: usa la API solo por HTTP.
- **Minijuego Unity 6.3**: autónomo (no usa la API).

## Estructura del monorepo

| Carpeta | Contenido |
|---|---|
| `backend/` | FastAPI + Docker Compose; la API del contrato, con su manual en `backend/README.md` |
| `desktop/` | Cliente de escritorio JavaFX (código vigente en `com.runnersz`) |
| `android/` | Reservado para la aplicación Android |
| `unity/` | Minijuego Unity 6.3 (descripción, uso y compilación más abajo) |
| `scripts/` | Utilidades del equipo (`board.py`, comprobadores de rama y de mensaje de commit) |
| `.github/` | Propietarios, plantillas y workflow `desktop.yml` |

## Cliente de escritorio

- 10 clases en capas (`controller`, `service`, `model`, `utiles`), vista declarativa `desktop.fxml` y
  hoja de estilos con la paleta y las tipografías del contrato.
- API por HTTP con `java.net.http.HttpClient`, envío y recepción por bloques de 64 KiB, progreso real,
  cancelación y timeout de 120 s.
- JUnit 5 + JaCoCo con umbral del 80 % (`mvn verify` falla por debajo) y fat JAR ejecutable en
  `desktop/target/runnersz-desktop-win.jar` y `desktop/target/runnersz-desktop-linux.jar`.
- Idiomas: español, euskera e inglés. Accesibilidad según §9.3.

Detalles de uso, configuración de la API y pruebas en [desktop/README.md](desktop/README.md).

## Minijuego Unity

Minijuego autónomo de descifrado César para Windows y Android, con Unity 6000.3.24f1.
No consume la API del servidor. Responsable: Jairo (@jairoben).
Rama de trabajo: `feature/unity-v2`.

### Jugar

Abre `unity/Builds/Windows/RunnersZ.exe` en el ordenador de desarrollo.
Para compartirlo, conserva toda la carpeta Windows, incluidos los datos y DLL.
El ejecutable no se incluye en Git: se puede regenerar desde el proyecto.

En Unity Hub abre la carpeta `unity`, carga `Assets/Scenes/Game.unity`
y pulsa Play. El menú `RunnersZ/Build Game Scene` regenera la escena inicial.

Selecciona Fácil, Normal o Difícil. Introduce el mensaje descifrado y pulsa
Validar o Enter. César rota A-Z; conserva espacios y signos. Fácil muestra el
desplazamiento. Hay tres rondas, tres vidas y 3/2/1 pistas según dificultad.
Cada acierto suma 100 puntos. Cada pista resta 50, con mínimo cero.
Un fallo acerca a los enemigos; cuando llegan a la puerta se pierde una vida.
Resolver tres mensajes produce victoria. Perder tres vidas produce derrota.
Reiniciar restaura vidas, puntuación y ronda; puedes cambiar de dificultad.

La interfaz y los enemigos usan formas básicas dibujadas por RunnerGame.
Los scripts Door/EnemyHopper se conservan como componentes previos; la escena
entregada ejecuta RunnerGame y su simulación de avance, sin depender de tags.

### Jugar en Android

El APK se genera en `unity/Builds/Android/RunnersZ.apk` (arm64-v8a, IL2CPP,
OpenGL ES 3, minSdk 25) y no se incluye en Git. Para instalarlo en un teléfono
con depuración USB activa:

```powershell
adb install -r unity/Builds/Android/RunnersZ.apk
```

La partida va en apaisado y a 60 fps. Todo se resuelve tocando la pantalla:
el botón Manuscrito abre la rueda de pares, el campo de respuesta despliega el
teclado del sistema, y Validar, Pista, Reiniciar y Cambiar dificultad son
botones de 48 dp. La barra atrás de Android cierra el manuscrito, vuelve al
menú y, si ya estás en el menú, sale del juego.

La interfaz se mide en dp sobre el área segura del dispositivo: el lienzo llena
la pantalla sin barras negras, respeta el recorte del notch y mantiene el campo
de respuesta visible cuando aparece el teclado. Si el sistema no tiene Georgia
ni Times New Roman, el texto serif usa la fuente incluida en Unity.

### Validación del 5 de noviembre de 2026

- EditMode: 25 pruebas aprobadas, 0 fallos. Cinco son nuevas y recorren nueve
  perfiles de pantalla (móviles 16:9 y 19.5:9 con notch, tablets 4:3, 21:9,
  escritorio y una densidad extrema) comprobando límites, área segura, solapes,
  tamaño táctil en dp y que el lienzo llena la pantalla.
- PlayMode: 2 pruebas aprobadas, 0 fallos, sin excepciones en ejecución.
- APK Android: generado con 0 errores; `aapt2` confirma paquete
  `com.runnersz.game`, `targetSdkVersion 36`, `native-code arm64-v8a` y la
  característica `android.hardware.screen.landscape`.
- Geometría revisada a ojo con `RunnersZ/Render Layout Previews`, que pinta los
  rectángulos reales de cada pantalla en PNG.
- Limitación: no se ha instalado ni probado el APK en un teléfono físico, así
  que el teclado táctil y el rendimiento real siguen sin verificar en hardware.

### Validación del 2 de noviembre de 2026

- Generación de escena y compilación de scripts con Unity: correctas.
- EditMode: 10 pruebas aprobadas, 0 fallos.
- PlayMode: 2 pruebas aprobadas, 0 fallos.
- Windows x64: ejecutable generado; proceso iniciado y registro inspeccionado.
- Evidencias XML: `docs/evidencias/unity/`.
- Seguimiento y limitaciones: `docs/planificacion/unity-tareas.md`.

Las pruebas cubren César, normalización, dificultad, rondas, puntuación,
pistas, victoria, derrota por enemigos y reinicio. No se ha medido cobertura.

### Regenerar Windows

Desde la raíz del repositorio, con Unity instalado y licencia activa:

```powershell
& 'C:\Program Files\Unity\Hub\Editor\6000.3.24f1\Editor\Unity.exe' `
  -batchmode -nographics -projectPath "$PWD/unity" `
  -executeMethod RunnersZ.Editor.SceneBuilder.BuildWindows -quit `
  -logFile "$PWD/unity/windows-build.log"
```

### Regenerar Android

El módulo Android del editor debe estar instalado. La configuración (apaisado,
ARM64, IL2CPP, OpenGL ES 3, paquete `com.runnersz.game`) la aplica el propio
script antes de compilar:

```powershell
& 'C:\Program Files\Unity\Hub\Editor\6000.3.24f1\Editor\Unity.exe' `
  -batchmode -nographics -projectPath "$PWD/unity" `
  -executeMethod RunnersZ.Editor.AndroidBuild.BuildBatch `
  -logFile "$PWD/unity/android-build.log"
```

Las pruebas se ejecutan desde Window > General > Test Runner: EditMode y
PlayMode. Los ensamblados de pruebas están incluidos en el proyecto.

## Cómo trabajamos

- **Normas y referencias**: `C:\Users\dm2\ia\reglas\AGENTS.md` y el pliego/contrato de `reglas\docs`.
  No se incluye una carpeta `docs` en este repositorio.
- **Tablero kanban**: <https://github.com/orgs/dm2-ethazi/projects/14> (Proyecto 14 «RunnersZ»).
  Consultar o moverlo con `python scripts/board.py status|start|review|done <n>`.
- **Backlog**: requisitos y planificación en el Anexo I del contrato externo.
- **Reglas de commit**: `TIPO: descripción` en imperativo, con el asunto en 50 caracteres o menos y
  el cuerpo en 72; `scripts/check_commit_message.py` valida el formato.
- **Revisión**: los hooks conservan la protección de `main` y `develop`; las tareas se integran por PR.

## Estado

La rama de trabajo es `feature/desktop`. La API del servidor vive en `backend/`, con su manual en
[backend/README.md](backend/README.md); el minijuego Unity se documenta en la sección anterior. El
cliente de escritorio cubre los requisitos de los apartados 3, 4, 5, 7, 9.2, 9.3, 9.4 y 11.2–11.3 del
contrato, y su manual está en `desktop/README.md`.

## Uso local

1. Instala JDK 21 (el wrapper descarga Maven 3.9 si hace falta).
2. `cd desktop && ./mvnw clean verify` compila, ejecuta las pruebas y aplica el umbral de cobertura.
3. `./mvnw javafx:run` o `java -jar target/runnersz-desktop-win.jar` (en Linux,
   `java -jar target/runnersz-desktop-linux.jar`) para abrir la aplicación.
4. Configura la dirección y el token del servidor en **Ajustes**; no hay credenciales en el código.

## Equipo (roles del Anexo I.2)

- **Alain** (`@alain8123`): servidor, cliente JavaFX, CI/hooks y documentación de servidor/escritorio.
- **Jairo** (`@jbenavides`): Android, Unity y arranque del repositorio.
