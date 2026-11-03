# Auditoría v1 → v2 · 29 de octubre de 2026

Auditoría de arranque ejecutada por el agente de Jairo sobre el estado real del repositorio
y del entorno, previa a la Fase 0. Comandos y salidas reales.

## 1. Estado del repositorio

El prompt de arranque asumía un remoto con `main`, `integration/reto2` y varias `feature/*`.
**La comprobación real desmiente esa premisa**: el remoto estaba **vacío**.

```bash
git fetch --all --prune
# Todas las ramas remotas aparecen como [gone]:
#   [deleted] (none) -> origin/main, origin/integration/reto2, origin/feature/*, chore/*, docs/*
git ls-remote --heads origin
# (sin salida: el remoto no tenía ninguna referencia)
```

Local sí existía el histórico completo en `integration/reto2` (`c728f45`), con 35 commits de
`jairoben`, sin `.venv`, `target/`, `.gradle`, logs ni `.env` versionados:

```bash
git ls-files | Select-String '(^|/)(\.venv|target|\.gradle|build|__pycache__)/|\.log$|\.env$'
# OK: sin basura versionada
```

### Decisión (DJ0)

Ante el remoto vacío, el humano decidió **arrancar de cero**: un `main` con la estructura
oficial vacía y una rama de prototipo (`prototype/runnersz`) de la que el agente de Alain
abre sus PR. El histórico v1 **no** se restaura en el remoto; se conserva en local como
material reutilizable (el contrato lo trata como «v1 rechazada»).

## 2. Entorno de esta máquina (Windows)

| Herramienta | Mín. requerido | Encontrado | Estado |
|---|---|---|---|
| git | 2.40 | 2.55.0.windows.5 | ✅ |
| gh CLI | 2.50 | 2.103.3 (instalado en esta sesión con winget) | ✅ |
| JDK Temurin | 21 | 21.0.12.101 (instalado en esta sesión); había 25.0.4 (prohibido para Gradle) | ✅ |
| Android SDK | plat. 35, build-tools 34.0.0 | plataformas 35/36, build-tools 34.0.0/35.0.0, `adb` 37.0.1 | ✅ |
| Unity Hub + Editor | 6000.3.x | 6000.3.24f1 | ✅ |
| Unity Android Build Support | requerido | **ausente** (solo `windowsstandalonesupport`) | ⚠️ PENDIENTE-HUMANO |
| Docker + Compose v2 | 24 | 29.7.2 + Compose v5.5.1 | ✅ |
| uv | 0.4 | 0.12.18 | ✅ |
| Python | 3.12 | 3.12 | ✅ |

Notas: `JAVA_HOME` apuntaba a JDK 25 (el contrato prohíbe usarlo con el wrapper Gradle 8.9); se
instaló Temurin 21. `ANDROID_HOME` no estaba definido y `adb` no estaba en el PATH (el SDK está en
`%LOCALAPPDATA%\Android\Sdk`).

## 3. Contrato y prompts

`docs/contrato/` no existía en el repo. El contrato v2 se aportó fuera del repo
(`Contrato_Tecnico_RunnersZ_v2.docx.md`, 376 KB con 10 imágenes base64). Se limpió con el script
del prompt (quitar `[imageN]: <data:…>` y sustituir las referencias por nota) y el resultado
(`docs/contrato/Contrato_Tecnico_RunnersZ_v2.md`, **53 KB, sin base64**) quedó por debajo del
límite de 80 KB. El `.docx` original (656 KB, < 1 MiB) se versiona también en `docs/contrato/`.

## 4. GitHub

El CLI `gh` no estaba instalado ni autenticado; se instaló con `winget`. La autenticación y la
creación de etiquetas, hitos, issues y el Project 14 quedan registradas en la sección siguiente
cuando se ejecutaron (ver `docs/planificacion/indice-issues.md`).
