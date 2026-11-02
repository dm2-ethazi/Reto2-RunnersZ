# RunnersZ â€” Unity 6.3

Autor: Jairo (`@jairoben`). Rama Ãºnica: `feature/unity-v2`. PR: #40.

`GameController` conecta el flujo 2D con la lÃ³gica pura: texto CÃ©sar, entrada, victoria, derrota por puerta y reinicio. `EnemyHopper` mueve el enemigo con `Rigidbody2D`; `Door` consume vidas mediante `Collider2D` trigger.

## Escena

1. Abrir con Unity 6.3 (`6000.3.x`).
2. Ejecutar `RunnersZ > Build Game Scene`.
3. Asignar en `GameController` la puerta, enemigo, textos, `InputField` y botones.
4. Confirmar tag `Enemy`, `Rigidbody2D`, colliders y trigger de la puerta.
5. Ejecutar: respuesta correcta = victoria; tres impactos = derrota; Reiniciar restablece todo.

EditMode cubre CÃ©sar, validaciÃ³n, estados, vidas, rondas, pistas y puntuaciÃ³n. PlayMode debe comprobar movimiento, trigger, victoria, derrota y reinicio. Unity serializa referencias de escena, por lo que la comprobaciÃ³n final requiere abrir el editor. No se versionan licencia, `Library/` ni artefactos generados.
