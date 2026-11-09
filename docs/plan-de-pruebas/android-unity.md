# Plan de pruebas B · Android y Unity

Las pruebas **PT-14…PT-19** dependen de ficheros del servidor (los aporta Alain). Las filas **UN-xx** son adicionales al §7.3 del contrato, derivadas del §10 (juego), y las completa Jairo en I-08/I-09/I-16.

| ID | Grupo | Prueba | Resultado esperado | Responsable | Método | Resultado obtenido | Evidencia |
|---|---|---|---|---|---|---|---|
| PT-14 | B | Descifrar con la clave correcta un `.png.enc` generado en escritorio | La imagen se muestra (Alain aporta el fichero) | Jairo | manual | pendiente | pendiente |
| PT-15 | B | Android con fichero de 0 B | «El archivo está vacío» sin cierre de la app | Jairo | manual | pendiente | pendiente |
| PT-16 | B | Android con clave incorrecta | «clave incorrecta o fichero dañado» sin cierre de la app | Jairo | manual | pendiente | pendiente |
| PT-17 | B | Android: clave vacía, token incorrecto y servidor apagado | Aviso y acción «Reintentar»/«Ir a Ajustes» | Jairo | manual | pendiente | pendiente |
| PT-18 | B | César en Android (válidos 1 y 25; 0, 26, −3 y «abc») | Válidos correctos; inválidos rechazados en local y por `INVALID_SHIFT`; «Ataque al amanecer» +3 | Jairo | manual | pendiente | pendiente |
| PT-19 | B | `.enc` alterado o truncado en Android | `DECRYPT_FAILED` mostrado sin cierre de la app | Jairo | manual | pendiente | pendiente |
| UN-01 | B | El enemigo avanza un salto por intervalo (kinematic, `MovePosition`) | Transición ≈ 0,25 s por salto hacia la puerta | Jairo | auto | pendiente | pendiente |
| UN-02 | B | El enemigo entra en el trigger de la puerta | Resta 1 vida y desactiva al enemigo | Jairo | auto | pendiente | pendiente |
| UN-03 | B | La puerta llega a 0 vidas | Derrota con color + icono + texto + sonido | Jairo | manual | pendiente | pendiente |
| UN-04 | B | Acierto de frase (César + normalización §10.5) | Enemigo derrotado, +100 y aparece el siguiente | Jairo | auto | pendiente | pendiente |
| UN-05 | B | Respuesta incorrecta / vacía | Incorrecta: el enemigo avanza un salto. Vacía: aviso, sin fallo | Jairo | auto | pendiente | pendiente |
| UN-06 | B | Dificultad (Fácil/Normal/Difícil) y progresión de rondas | Tabla §10.3 aplicada; +2 enemigos y −10 % de intervalo por ronda (máx. 3) | Jairo | auto | pendiente | pendiente |
| UN-07 | B | Pistas | Revelan la siguiente palabra y restan 50 (suelo 0); al agotarse se desactivan | Jairo | auto | pendiente | pendiente |
| UN-08 | B | Accesibilidad (Unity): foco, Intro valida, botones ≥ 48 px, contraste | Cumple §9.3 columna Unity | Jairo | manual | pendiente | pendiente |
