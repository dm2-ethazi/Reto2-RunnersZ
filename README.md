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
| `backend/` | FastAPI + Docker Compose; el código vive en la rama `feature/api` y esta rama solo conserva los informes de cobertura |
| `desktop/` | Cliente de escritorio JavaFX (código vigente en `com.runnersz`) |
| `android/` | Reservado para la aplicación Android |
| `unity/` | Reservado para el minijuego Unity 6.3 |
| `scripts/` | Utilidades del equipo (`board.py`, comprobadores de rama y de mensaje de commit) |
| `.github/` | Propietarios, plantillas y workflow `desktop.yml` |

## Cliente de escritorio

- 10 clases en capas (`controller`, `service`, `model`, `utiles`), vista declarativa `desktop.fxml` y
  hoja de estilos con la paleta y las tipografías del contrato.
- API por HTTP con `java.net.http.HttpClient`, envío y recepción por bloques de 64 KiB, progreso real,
  cancelación y timeout de 120 s.
- JUnit 5 + JaCoCo con umbral del 80 % (`mvn verify` falla por debajo) y fat JAR en
  `desktop/target/runnersz-desktop.jar`.
- Idiomas: español, euskera e inglés. Accesibilidad según §9.3.

Detalles de uso, configuración de la API y pruebas en [desktop/README.md](desktop/README.md).

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

La rama de trabajo es `feature/desktop`. El servidor, Android y Unity no se presentan como
implementados en esta copia. El cliente de escritorio cubre los requisitos de los apartados 3, 4, 5,
7, 9.2, 9.3, 9.4 y 11.2–11.3 del contrato, y su manual está en `desktop/README.md`.

## Uso local

1. Instala JDK 21 (el wrapper descarga Maven 3.9 si hace falta).
2. `cd desktop && ./mvnw clean verify` compila, ejecuta las pruebas y aplica el umbral de cobertura.
3. `./mvnw javafx:run` o `java -jar target/runnersz-desktop.jar` para abrir la aplicación.
4. Configura la dirección y el token del servidor en **Ajustes**; no hay credenciales en el código.

## Equipo (roles del Anexo I.2)

- **Alain** (`@alain8123`): servidor, cliente JavaFX, CI/hooks y documentación de servidor/escritorio.
- **Jairo** (`@jbenavides`): Android, Unity y arranque del repositorio.
