# RunnersZ · Reto 2

Proyecto del Reto 2 para el cliente **Hogwarts**. Fecha de entrega: **12 de noviembre de 2026**.

Arquitectura del contrato técnico v2.0. El pliego, el contrato y las normas se mantienen fuera del repositorio, en `C:\Users\dm2\ia\reglas`, por indicación del responsable:

- **Servidor** Python (FastAPI) en **Docker Compose**: concentra toda la lógica de cifrado y expone la API HTTP.
- **Cliente de escritorio** JavaFX: usa la API solo por HTTP.
- **App Android**: usa la API solo por HTTP.
- **Minijuego Unity 6.3**: autónomo (no usa la API).

## Estructura del monorepo

| Carpeta | Contenido |
|---|---|
| `backend/` | Reservado para FastAPI + Docker Compose; sin implementación en esta copia |
| `desktop/` | Cliente de escritorio JavaFX |
| `android/` | Reservado para la aplicación Android; contiene un marcador |
| `unity/` | Reservado para el minijuego Unity 6.3; contiene un marcador |
| `scripts/` | Utilidades del equipo (p. ej. `board.py` del tablero) |
| `.github/` | Propietarios de rutas y plantillas de colaboración |

## Cómo trabajamos

- **Normas y referencias**: `C:\Users\dm2\ia\reglas\AGENTS.md` y el pliego/contrato de `reglas\docs`. No se incluye una carpeta `docs` en este repositorio.
- **Tablero kanban**: <https://github.com/orgs/dm2-ethazi/projects/14> (Proyecto 14 «RunnersZ»).
  Consultar o moverlo con `python scripts/board.py status|start|review|done <n>`.
- **Backlog**: requisitos y planificación en el Anexo I del contrato externo. Los identificadores se contrastan con los issues del repositorio antes de actuar sobre el tablero.
- **Reglas de commit**: seguir `reglas/AGENTS.md`: `tipo(ámbito opcional): descripción`, en imperativo y con un máximo de 72 caracteres. La preparación actual queda sin commit ni push hasta el aviso del responsable.
- **Revisión**: los hooks existentes conservan la protección de `main` y `develop`. El comprobador `scripts/check_commit_message.py` valida el formato de `reglas/AGENTS.md`; la limpieza retira la validación antigua en mayúsculas y 50 caracteres. Comprobar un archivo de mensaje no crea un commit.

## Estado

El cliente `desktop/` contiene el código vigente en `com.runnersz.desktop`,
organizado en controlador, servicios y modelo. Su guía está en
[desktop/README.md](desktop/README.md). Se retiraron los lanzadores de
compatibilidad de `org.example` y los recursos antiguos sin uso.

La rama de trabajo observada es `feature/desktop`. Los módulos de servidor,
Android y Unity no se presentan como implementados en esta copia.
No se acredita la aceptación completa del contrato: siguen pendientes las fuentes
empaquetadas, JUnit/JaCoCo con cobertura mínima del 80 %, las pruebas con servidor
real, los módulos restantes y la validación del pipeline.

## Equipo (roles del Anexo I.2)

- **Alain** (`@alain8123`): servidor, cliente JavaFX, CI/hooks y documentación de servidor/escritorio.
- **Jairo** (`@jairoben`): Android, Unity y arranque del repositorio.

El código de `scripts/board.py` conserva su configuración del Project 14.
No se ha consultado ni modificado el tablero durante la limpieza. La carpeta
`.github/workflows` contiene un marcador: no hay un pipeline ejecutable en esta copia.


## Uso local

1. Instala JDK 21 y Maven para trabajar en el cliente.
2. Entra en `desktop` y ejecuta `mvn clean verify`.
3. Usa `mvn javafx:run` o el JAR generado, siguiendo la configuración HTTP descrita en el README del cliente.
4. Para revisar cambios locales utiliza `git status --short` y `git diff --check`. Los binarios de `target/` y las preferencias de `.idea/` se ignoran.

## Cómo funciona y qué utiliza

El cliente JavaFX delega la criptografía al servidor por HTTP, mediante
`java.net.http.HttpClient`; guarda resultados en disco con servicios propios
y conserva dirección/token en las preferencias locales del usuario.
JavaFX 21.0.5, Maven y el JDK son las dependencias actuales del cliente.
Los scripts usan Python; los hooks invocan también uv/pytest, y el tablero usa
la CLI de GitHub. La limpieza no añade dependencias ni servicios.

## Decisiones de organización

La separación en capas cumple el contrato §5.4 y facilita la explicación pública
del código requerida por el pliego §4.8. Mantener código, recursos y configuración
en sus carpetas estándar facilita la replicación del entorno (§4.13).
Las plantillas y propietarios siguen disponibles para el trabajo en GitHub
(§4.7), con referencias externas al contrato y sin documentos duplicados.
La limpieza física conserva las eliminaciones locales previas de backend y
documentación; no altera el historial ni prepara archivos en el índice de Git.

## Validación de la limpieza

- `uv run --python 3.12 --with pytest python -m pytest scripts/tests -q`: 41 pruebas superadas.
- `mvn -B -ntp clean verify` en desktop: compilación y fat JAR correctos.
- `git diff --check`: sin errores.
- Los entornos privados, `desktop/target` y `desktop/.idea` quedan ignorados;
  los archivos de ejemplo `.env.example` siguen permitidos.
- La limpieza se mantiene en el árbol de trabajo: sin commits, push ni cambios
  en el índice. Los requisitos funcionales pendientes se indican en Estado y en el README del cliente.
