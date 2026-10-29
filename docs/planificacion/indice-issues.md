# Índice de issues del backlog (I-NN ↔ #N)

Correspondencia entre los **identificadores internos del contrato** (Anexo I.3)
y los **números reales de GitHub**. Generado el 29-10-2026 desde la API de GitHub.

> Los issues **#1, #2 y #3** son ajenos a este backlog (preexistentes en el repo):
> «Aprobación de la propuesta», «Configurar GitHub» y «Generar codigo base con swager de la api».

| ID  | Issue | Título | Responsable | Horas | Sprint | Rama sugerida | Estado |
|-----|-------|--------|-------------|-------|--------|---------------|--------|
| I-01 | #4 | Repositorio, ramas y CI base | Alain | 3 h | Sprint 1 | `feature/i01-workflow` | open |
| I-02 | #5 | Servidor Docker + API con token | Alain | 4 h | Sprint 1 | `feature/i02-servidor-docker` | open |
| I-03 | #6 | Vigenere y Cesar en servidor | Alain | 3 h | Sprint 1 | `feature/i03-texto-cifrado` | open |
| I-04 | #7 | AES-GCM por bloques y formato .enc | Alain | 6 h | Sprint 1 | `feature/i04-aes-bloques` | open |
| I-05 | #8 | Endpoints multipart de cifrado y descifrado | Alain | 4 h | Sprint 1 | `feature/i05-endpoints-files` | open |
| I-06 | #9 | Android: esqueleto, ajustes de conexion y OkHttp | Jairo | 5 h | Sprint 1 | `feature/i06-android-skeleton` | open |
| I-07 | #10 | Android: descifrar imagen por la API | Jairo | 5 h | Sprint 1 | `feature/i07-android-decrypt-image` | open |
| I-08 | #11 | Unity: escena, enemigo, puerta y fisica 2D | Jairo | 6 h | Sprint 1 | `feature/i08-unity-scene-physics` | open |
| I-09 | #12 | Unity: frases, Cesar y validacion | Jairo | 4 h | Sprint 1 | `feature/i09-unity-phrases-caesar` | open |
| I-10 | #13 | JavaFX: cliente, ajustes y HttpClient | Alain | 7 h | Sprint 2 | `feature/i10-desktop-client` | open |
| I-11 | #14 | JavaFX: Vigenere, integridad y alertas | Alain | 4 h | Sprint 2 | `feature/i11-desktop-vigenere` | open |
| I-12 | #15 | Tests, cobertura 80 %, pre-push y act | Alain | 4 h | Sprint 2 | `feature/i12-calidad-ci` | open |
| I-13 | #16 | Pruebas del Anexo VI.4 con evidencias | Alain | 3 h | Sprint 2 | `feature/i13-pruebas-evidencias` | open |
| I-14 | #17 | Manual de instalacion, README y demo | Alain | 2 h | Sprint 2 | `feature/i14-manual-servidor` | open |
| I-15 | #18 | Android: Cesar, errores y accesibilidad | Jairo | 4 h | Sprint 2 | `feature/i15-android-caesar-a11y` | open |
| I-16 | #19 | Unity: dificultad, rondas, pistas y paneles final | Jairo | 6 h | Sprint 2 | `feature/i16-unity-difficulty-ui` | open |
| I-17 | #20 | APK de Android y de Unity | Jairo | 3 h | Sprint 2 | `feature/i17-apk-build` | open |
| I-18 | #21 | Manuales de uso de Android y Unity | Jairo | 3 h | Sprint 2 | `feature/i18-manuals` | open |
| I-19 | #22 | Pruebas en dispositivos y ensayo de la demo | Jairo | 4 h | Sprint 2 | `feature/i19-device-tests-demo` | open |

**Total: 19 issues · 80 h** (Sprint 1: 40 h · Sprint 2: 40 h).

## Cómo mover el tablero

```bash
python scripts/board.py status          # ver el tablero por columna
python scripts/board.py start 4         # I-01 (#4) → En curso
python scripts/board.py review 5        # I-02 (#5) → En revisión
python scripts/board.py done 5          # I-02 (#5) → Hecho
```

El tablero completo está en <https://github.com/orgs/dm2-ethazi/projects/14>.
