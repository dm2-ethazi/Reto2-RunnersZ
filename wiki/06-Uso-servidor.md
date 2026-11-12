# 06 · Uso de la API (servidor)

Referencia de las rutas, ejemplos reales y errores del servidor. Todos los ejemplos están comprobados contra el código del repositorio.

---

## 1. Conceptos básicos

- Prefijo de **todas** las rutas: `/api/v1`.
- Autenticación: cabecera `Authorization: Bearer <TU_TOKEN>` en todas las rutas **excepto** `health`.
- Cuerpos de error: siempre `{"code": "...", "message": "..."}`. Los `422` de validación de FastAPI se convierten a `400` con la misma forma.
- La **contraseña del fichero** y el **token de acceso** son cosas distintas: el token abre la API, la contraseña cifra el `.enc`.
- Timeout recomendado en los clientes: **120 s** (§7.2).

---

## 2. Rutas

| Método | Ruta | Entrada | Respuesta correcta |
|---|---|---|---|
| GET | `/api/v1/health` | Ninguna | `200` con `status` y `version` |
| GET | `/api/v1/auth/check` | Token Bearer | `204` sin cuerpo |
| POST | `/api/v1/text/vigenere` | JSON: `text`, `key`, `mode` | `200` con `result` |
| POST | `/api/v1/text/caesar` | JSON: `text`, `shift`, `mode` | `200` con `result` |
| POST | `/api/v1/files/encrypt` | Multipart: `file`, `password` | Bytes cifrados + nombre sugerido + `Content-Length` |
| POST | `/api/v1/files/decrypt` | Multipart: `file`, `password` | Bytes recuperados + `X-Integrity: verified` + nombre sugerido |

`mode` solo admite `encrypt` o `decrypt`; si se omite, se asume `encrypt`.

---

## 3. Salud y autenticación

```bash
curl -s http://127.0.0.1:8000/api/v1/health
```

```json
{"status":"ok","version":"2.0.0"}
```

```bash
curl -i http://127.0.0.1:8000/api/v1/auth/check -H "Authorization: Bearer <TU_TOKEN>"
```

✅ `HTTP/1.1 204 No Content`, sin cuerpo. Sin token: `401 UNAUTHORIZED`.

---

## 4. César

### Cifrar

PowerShell:

```powershell
$cuerpo = @{ text = "Ataque al amanecer"; shift = 3; mode = "encrypt" } | ConvertTo-Json
curl.exe -s -X POST http://127.0.0.1:8000/api/v1/text/caesar `
  -H "Authorization: Bearer <TU_TOKEN>" -H "Content-Type: application/json" -d $cuerpo
```

Linux/macOS:

```bash
curl -s -X POST http://127.0.0.1:8000/api/v1/text/caesar \
  -H "Authorization: Bearer <TU_TOKEN>" -H "Content-Type: application/json" \
  -d '{"text":"Ataque al amanecer","shift":3,"mode":"encrypt"}'
```

✅ Respuesta verificada:

```json
{"result":"Dwdtxh do dpdqhfhu"}
```

Para descifrar, envía ese texto con `"mode":"decrypt"` y el mismo `shift`.

### Reglas

- `shift` admite enteros de **1 a 25**, también enviados como texto (`"3"`).
- `0`, `26`, negativos y valores no enteros devuelven **`INVALID_SHIFT`** (HTTP 400):

```json
{"code":"INVALID_SHIFT","message":"El desplazamiento debe ser un entero entre 1 y 25"}
```

- Rota solo `A-Z` / `a-z`; **conserva espacios, signos, tildes y la Ñ**.

---

## 5. Vigenère

```json
{"text":"Hola Mundo, ¿qué tal?","key":"LUNA","mode":"encrypt"}
```

✅ Respuesta verificada (ejemplo del contrato §6.1):

```json
{"result":"Siya Xoadz, ¿khé tlf?"}
```

Reglas:

- La clave debe tener **letras ASCII A-Z** (se ignoran mayúsculas/minúsculas). Se repite sobre el texto.
- Las letras con tilde, la Ñ, los signos y los espacios **se conservan sin avanzar la clave**.
- Clave inválida → `INVALID_KEY` (HTTP 400).

---

## 6. Ficheros: cifrar y descifrar

### Cifrar un fichero

```powershell
curl.exe -s -X POST http://127.0.0.1:8000/api/v1/files/encrypt `
  -H "Authorization: Bearer <TU_TOKEN>" `
  -F "file=@nota.txt" -F "password=clave123" `
  -o nota.txt.enc -D cabeceras.txt
```

Linux/macOS: igual, con `\` al final de línea.

Comprobaciones:

```powershell
Get-Content cabeceras.txt
```

✅ Debe incluir:

```text
content-disposition: attachment; filename="nota.txt.enc"
content-length: <tamaño>
```

El tamaño crece respecto al original por la cabecera `RZEN`, la etiqueta GCM de 16 B por bloque y el SHA-256 final de 32 B (ver §8).

### Descifrar un fichero

```powershell
curl.exe -s -X POST http://127.0.0.1:8000/api/v1/files/decrypt `
  -H "Authorization: Bearer <TU_TOKEN>" `
  -F "file=@nota.txt.enc" -F "password=clave123" `
  -o nota-recuperada.txt -D cabeceras-dec.txt
```

✅ Respuesta verificada: `200`, cabecera `x-integrity: verified`, `content-disposition: attachment; filename="nota.txt"` y el contenido original byte a byte.

### Reglas y límites

- Fichero vacío → `EMPTY_FILE`; contraseña vacía → `EMPTY_PASSWORD`.
- Más de `RZ_MAX_UPLOAD_MIB` (100 MiB por defecto) → `FILE_TOO_LARGE` (HTTP 413).
- Contraseña incorrecta, bloque alterado, reordenado, truncado o huella que no cuadra → `DECRYPT_FAILED` (HTTP 422):

```json
{"code":"DECRYPT_FAILED","message":"clave incorrecta o fichero dañado"}
```

- **La cabecera `X-Integrity: verified` es obligatoria en los clientes**: si falta, el resultado no debe publicarse.
- El original **nunca se modifica**. El servidor escribe en un `.part` y renombra de forma atómica; los temporales se borran tanto al terminar bien como al fallar.

---

## 7. Tabla de errores

| `code` | HTTP | Mensaje | Cuándo |
|---|---|---|---|
| `UNAUTHORIZED` | 401 | «Token no válido. Revisa los ajustes» | Falta el token, no coincide o el servidor no tiene `RZ_API_TOKEN` |
| `EMPTY_FILE` | 400 | «El archivo está vacío» | Fichero de 0 bytes |
| `EMPTY_PASSWORD` | 400 | «La contraseña está vacía» | `password` vacío o ausente |
| `INVALID_KEY` | 400 | «La clave no es válida: usa solo letras A-Z» | Vigenère con clave no ASCII o vacía |
| `INVALID_SHIFT` | 400 | «El desplazamiento debe ser un entero entre 1 y 25» | `shift` fuera de rango |
| `INVALID_REQUEST` | 400 | Cuerpo o formulario incompletos | Falta `text`, `file` o `password`, o `mode` no es `encrypt`/`decrypt` |
| `FILE_TOO_LARGE` | 413 | Supera el límite | Más de `RZ_MAX_UPLOAD_MIB` |
| `DECRYPT_FAILED` | 422 | «clave incorrecta o fichero dañado» | Contraseña distinta o fichero alterado |
| `INTERNAL_ERROR` | 500 | Fallo inesperado | Error no controlado; se borran los temporales |

Ningún error incluye la contraseña ni el token.

---

## 8. Formato del fichero `.enc` (§5.3 y §6.2)

| Campo | Bytes | Contenido |
|---|---|---|
| Magic | 4 | `RZEN` |
| Versión | 1 | `1` |
| KDF | 1 | `1` = PBKDF2-HMAC-SHA256 |
| Iteraciones | 4 | 600 000, big-endian |
| Sal | 16 | Aleatoria por fichero |
| Prefijo de nonce | 4 | Aleatorio por fichero |
| Tamaño de bloque | 4 | 1 048 576 B (1 MiB en claro) |
| Bloques | variable | Texto cifrado + etiqueta GCM de 16 B; el último incluye el SHA-256 del original (32 B) |

- Clave de 256 bits por fichero: `PBKDF2-HMAC-SHA256(contraseña, sal, 600 000)`.
- Nonce de 96 bits: prefijo de 4 B + contador de bloque de 8 B, siempre distinto.
- AAD de cada bloque: cabecera completa + índice + indicador de último bloque. Reordenar, borrar o truncar bloques **rompe la autenticación**.
- El número de bloques se deduce del tamaño total: un fichero múltiplo exacto de 1 MiB (1 MiB, 20 MiB…) se recupera sin ambigüedad.

Consecuencia práctica: **no edites un `.enc` a mano** ni lo pases por herramientas que cambien bytes; el descifrado fallará aunque la contraseña sea correcta.

---

## 9. Swagger UI

<http://127.0.0.1:8000/docs>

1. **Authorize** → escribe solo el token (sin `Bearer`).
2. `GET /auth/check` → 204.
3. `POST /files/encrypt` → **Try it out**, elige archivo y escribe contraseña → **Execute**.
4. Para descifrar, descarga la respuesta (**Download file**), súbela en `/files/decrypt` con la misma contraseña y comprueba `X-Integrity`.

Si cambias el token, recarga con **Ctrl+F5** y vuelve a autorizar (Swagger cachea).

---

## 10. Clientes que consumen esta API

| Cliente | Cómo configura la conexión | Qué usa |
|---|---|---|
| Escritorio JavaFX | **Ajustes**: URL base + token | Las seis rutas |
| Android (CipherSprint) | Fija en código (URL y token) | `auth/check`, `text/caesar`, `files/decrypt` |
| Unity | No usa la API | — |

---

## Siguiente paso

- Cliente de escritorio: [07 · Uso del escritorio](07-Uso-escritorio.md)
- App Android: [08 · Uso de Android](08-Uso-android.md)

[Volver al índice](Home.md)
