# Plan de pruebas A · Servidor y escritorio

| ID | Grupo | Prueba | Resultado esperado | Responsable | Método | Resultado obtenido | Evidencia |
|---|---|---|---|---|---|---|---|
| PT-01 | A | Recuperar texto, binario y PNG por la API | Recuperación idéntica byte a byte (mismo SHA-256) y aviso «integridad verificada» con ficheros de 1 B, 1 MiB y 20 MiB | Alain | auto | pendiente | pendiente |
| PT-02 | A | Cifrar/descifrar un fichero de 1 byte | Éxito con salida idéntica | Alain | auto | pendiente | pendiente |
| PT-03 | A | Cifrar/descifrar un fichero de 20 MiB | Bloques procesados, memoria acotada, progreso visible y UI sin bloquear | Alain | auto | pendiente | pendiente |
| PT-04 | A | Enviar un fichero de 21 MiB con límite 20 MiB | 413 `FILE_TOO_LARGE` y no se genera salida | Alain | auto | pendiente | pendiente |
| PT-05 | A | Enviar un fichero de 0 B | 400 `EMPTY_FILE` con mensaje «El archivo está vacío» | Alain | auto | pendiente | pendiente |
| PT-06 | A | Descifrar con contraseña incorrecta | 422 `DECRYPT_FAILED`, sin `.part` y sin salida | Alain | auto | pendiente | pendiente |
| PT-07 | A | Alterar/truncar un `.enc` (1 byte, bloques reordenados, truncado) | 422 en cada caso | Alain | auto | pendiente | pendiente |
| PT-08 | A | Contraseña vacía, token incorrecto y servidor apagado | `EMPTY_PASSWORD`, `UNAUTHORIZED` y alerta con «Reintentar» | Alain | manual | pendiente | pendiente |
| PT-09 | A | Vigenère (mayúsc./minúsc., Ñ/tildes/diéresis, espacios y signos) | «Siya Xoadz, ¿khé tlf?» con LUNA descifra correctamente | Alain | auto | pendiente | pendiente |
| PT-10 | A | Validación de clave Vigenère (vacía, dígitos, Ñ) | 400 `INVALID_KEY` | Alain | auto | pendiente | pendiente |
| PT-11 | A | César: válidos 1 y 25; 0, 26, −3, «abc» | Válidos correctos; inválidos → `INVALID_SHIFT`; «Ataque al amanecer» +3 → «Dwdtxh do dpdqhfhu» | Alain | auto | pendiente | pendiente |
| PT-12 | A | Nonces por bloque | Nonces distintos por bloque; prefijo/sal distintos en dos cifrados del mismo fichero | Alain | auto | pendiente | pendiente |
| PT-13 | A | Rutas con/sin token y multipart sin `password` | 401 y 400 con `{code,message}` | Alain | auto | pendiente | pendiente |
