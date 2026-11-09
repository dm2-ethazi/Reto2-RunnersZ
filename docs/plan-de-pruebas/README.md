# Plan de pruebas · RunnersZ · Reto 2

Cómo rellenar este plan y reglas de honestidad. Ficheros:

- [`servidor-escritorio.md`](servidor-escritorio.md) — pruebas **A** (PT-01…PT-13): servidor y cliente de escritorio (responsable: **Alain**).
- [`android-unity.md`](android-unity.md) — pruebas **B** (PT-14…PT-19 y UN-xx): Android y Unity (responsable: **Jairo**).

## Reglas de honestidad (C.1-4)

1. **Resultado obtenido** y **Evidencia** solo se rellenan tras **ejecutar** la prueba y ver su salida. Nunca se anticipan.
2. Un fallo real se anota como **fallo** y se abre un issue `bug`. **Prohibido** maquillar resultados.
3. Cada evidencia lleva el comando exacto, la fecha y la salida o captura (en `docs/evidencias/`), **sin secretos**.
4. Si algo no se pudo probar (sin móvil, sin licencia, sin red), se deja `PENDIENTE-HUMANO`, no «pasa».

## Columnas (idénticas en los dos ficheros)

| Columna | Contenido |
|---|---|
| ID | PT-nn o UN-nn |
| Grupo | A (servidor/escritorio) o B (Android/Unity) |
| Prueba | Descripción concreta |
| Resultado esperado | Qué debe ocurrir (contrato §) |
| Responsable | Alain o Jairo |
| Método | auto (script/test) o manual |
| Resultado obtenido | `pendiente` hasta ejecutarla |
| Evidencia | `pendiente` hasta guardar la prueba |

Los resultados de **Android/Unity** se completan en I-17/I-18/I-19 y los de **servidor/escritorio** en I-13.
