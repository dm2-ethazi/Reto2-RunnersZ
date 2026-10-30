# Índice de issues del backlog (I-NN ↔ #N)

Correspondencia entre los **identificadores internos del contrato** (Anexo I.3)
y los **números reales de GitHub**. Verificado el 30-10-2026 leyendo los
cuerpos de los issues: **no se deduce del número de GitHub** (la reestructuración
del 29-10-2026 rompió la correspondencia secuencial).

## Cómo se ha contrastado

| Método | Issues | Evidencia |
|--------|--------|-----------|
| «Anexo I #NN» explícito en el cuerpo | #9, #10, #11, #12, #16, #17, #18, #19, #41 | p. ej. #41: «Contrato v2: Anexo I #15» |
| Título + área + hito contra el Anexo I.3 | #4–#8, #13, #14, #15 | p. ej. #15 «Tests, cobertura 80 %, pre-push y act» = I-12 |

> Los issues **#1, #2 y #3** son ajenos a este backlog (aprobación de la
> propuesta, configuración de GitHub y contrato OpenAPI).
> Los issues **#20, #21 y #22** son tareas complementarias de Jairo (índice de
> evidencias, revisión de criterios y pipeline) **sin I-NN**: no pertenecen al
> Anexo I.3 y no se les asigna identificador.

| ID  | Issue | Título (backlog actual) | Responsable | Horas contrato | Sprint | Rama sugerida | Estado |
|-----|-------|-------------------------|-------------|----------------|--------|---------------|--------|
| I-01 | #4  | Repositorio, ramas y CI base | Alain | 3 h | Sprint 1 | `feature/i01-workflow` | open |
| I-02 | #5  | Servidor Docker Compose y API con token | Alain | 4 h | Sprint 1 | `feature/i02-api-base` | open |
| I-03 | #6  | Vigenère y César en servidor | Alain | 3 h | Sprint 1 | `feature/i03-texto-cifrado` | open |
| I-04 | #7  | AES-GCM por bloques y formato .enc | Alain | 6 h | Sprint 1 | `feature/i04-aes-bloques` | open |
| I-05 | #8  | Endpoints multipart de cifrado y descifrado | Alain | 4 h | Sprint 1 | `feature/i05-endpoints-files` | open |
| I-06 | #9  | Android: base, ajustes y navegación | Jairo | 5 h | Sprint 1 | `feature/i06-android-skeleton` | open |
| I-07 | #10 | Android: imagen AES mediante API | Jairo | 5 h | Sprint 1 | `feature/i07-android-decrypt-image` | open |
| I-08 | #11 | Unity: escena, enemigo y puerta | Jairo | 6 h | Sprint 1 | `feature/i08-unity-scene-physics` | open |
| I-09 | #12 | Unity: frases, César y respuestas | Jairo | 4 h | Sprint 1 | `feature/i09-unity-phrases-caesar` | open |
| I-10 | —   | JavaFX: cliente, ajustes y HttpClient | Alain | 7 h | Sprint 2 | `feature/i10-desktop-client` | **sin issue** |
| I-11 | —   | JavaFX: Vigenère, integridad y alertas | Alain | 4 h | Sprint 2 | `feature/i11-desktop-vigenere` | **sin issue** |
| I-12 | #15 | Tests, cobertura 80 %, pre-push y act | Alain | 4 h | Sprint 2 | `feature/i12-calidad-ci` | open |
| I-13 | #13 | Pruebas del Anexo VI.4 con evidencias | Alain | 3 h | Sprint 2 | `feature/i13-pruebas-evidencias` | open |
| I-14 | #14 | Manual de instalación, README y demo | Alain | 2 h | Sprint 2 | `feature/i14-manual-servidor` | open |
| I-15 | #41 | Android: órdenes César y accesibilidad | Jairo | 4 h | Sprint 2 | `feature/android-v2` | open |
| I-16 | #16 | Unity: dificultad, rondas y paneles | Jairo | 6 h | Sprint 2 | `feature/unity-v2` | open |
| I-17 | #17 | Entrega: APK Android y Unity | Jairo | 3 h | Sprint 2 | `feature/android-v2`, `feature/unity-v2` | open |
| I-18 | #18 | Documentación: manuales Android y Unity | Jairo | 3 h | Sprint 2 | `feature/android-v2`, `feature/unity-v2` | open |
| I-19 | #19 | Validación: dispositivo y ensayo de demo | Jairo | 4 h | Sprint 2 | `feature/android-v2`, `feature/unity-v2` | open |

**Total del Anexo I.3: 19 tareas · 80 h** (Sprint 1: 40 h · Sprint 2: 40 h).
**Con issue en el backlog: 17** · **Sin issue: I-10 e I-11** (el cliente JavaFX
no tiene tarjeta desde la reestructuración del 29-10-2026; crearla o reasignar
el alcance es **decisión de Alain**, no de este índice).

## Cómo mover el tablero

```bash
python scripts/board.py status          # ver el tablero por columna
python scripts/board.py start 9         # I-06 (#9) → En curso
python scripts/board.py review 10       # I-07 (#10) → En revisión
python scripts/board.py done 10         # I-07 (#10) → Hecho
```

`board.py` resuelve el I-NN **desde esta tabla** y contrasta que el título del
issue sigue coincidiendo; si el título cambia, lo marca con `(título≠índice)`.

El tablero completo está en <https://github.com/orgs/dm2-ethazi/projects/14>.
