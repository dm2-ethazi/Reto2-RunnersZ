# 12 · Personalizar el servidor y el token de la app

Todo funciona tal cual con la configuración de entrega. Esta página es para cuando necesitas **tu propio servidor** o **rotar el token**.

---

## 1. Dónde vive cada cosa

| Elemento | Dónde | ¿Se versiona? |
|---|---|---|
| Token del servidor | `backend/.env` (`RZ_API_TOKEN`) | No (`.gitignore`) |
| Ejemplo de variables | `backend/.env.example` | Sí, con valores ficticios |
| Token del escritorio | Preferencias del sistema, nodo `com/runnersz` | No |
| Token de Android | Tres fragmentos enmascarados en `security/Credential*.kt` | Sí (ofuscado) |
| URL de Android | `data/ApiConfig.kt` | Sí |
| URL del escritorio | **Ajustes** de la aplicación | No |

---

## 2. Cambiar el token del servidor

### En local (uv)

1. Edita `backend/.env`:

```dotenv
RZ_API_TOKEN=mi-token-nuevo-2026
RZ_MAX_UPLOAD_MIB=100
```

2. Para el servidor (**Ctrl+C**) y vuelve a arrancarlo con el mismo comando:

```powershell
uv run --python 3.12 uvicorn runnersz.main:app --app-dir src --env-file .env --host 127.0.0.1 --port 8000 --reload
```

3. Comprueba:

```powershell
curl.exe -i http://127.0.0.1:8000/api/v1/auth/check -H "Authorization: Bearer mi-token-nuevo-2026"
```

✅ `204`. Con el token anterior debe dar `401`.

> La configuración se relee en cada petición, pero `--env-file` solo se lee al arrancar: por eso hay que reiniciar.

### En Docker

1. Cambia `backend/.env`.
2. Recrea el contenedor:

```bash
docker compose up -d --force-recreate
```

### En un host público (Render y similares)

1. Cambia la variable `RZ_API_TOKEN` en el panel del host (nunca en el repositorio).
2. Redespliega y comprueba `https://<tu-dominio>/api/v1/health`.
3. Si cambias el token, **el APK de Android deja de autenticarse** hasta que lo recompiles (§4).

---

## 3. Escritorio: solo hay que escribir el token nuevo

1. **Ajustes**.
2. Pega el token nuevo.
3. **Probar** → **Guardar**.

No hay que recompilar nada.

---

## 4. Android: URL y token van dentro del APK

La app no permite configurar el servidor: la URL y el token están fijados para que el token no pueda enviarse a otro host. Para usar tu backend debes **recompilar**.

### 4.1 Cambiar la URL

Edita `android/app/src/main/java/com/runnersz/reto2/android/data/ApiConfig.kt`:

```kotlin
object ApiConfig {
    const val BASE_URL = "https://tu-servidor.example.com"   // sin /api/v1
    const val MAX_UPLOAD_BYTES = 100L * 1024L * 1024L
}
```

El cliente añade `/api/v1` a cada ruta.

> ⚠️ **Debe ser HTTPS.** El manifest lleva `usesCleartextTraffic="false"` y `network_security_config.xml` solo confía en las CA del sistema. Un servidor `http://192.168.x.x:8000` **no funcionará** desde la app.

### 4.2 (Solo demo en red local) Permitir HTTP

Si necesitas apuntar a un servidor HTTP de la red local para una demo controlada:

1. En `android/app/src/main/res/xml/network_security_config.xml`, añade tu host con `cleartextTrafficPermitted="true"`.
2. En `AndroidManifest.xml`, pon `android:usesCleartextTraffic="true"`.
3. Recompila y **no distribuyas** ese APK.

Esto contradice la postura de seguridad del cliente (§3.4 limita HTTP a una red controlada). Para la entrega, deja HTTPS.

### 4.3 Regenerar los fragmentos del token

`SecureTokenProvider` reconstruye el token de **20 caracteres** a partir de tres fragmentos enmascarados:

- `CredentialA.DATA` → posiciones 0–6
- `CredentialB.DATA` → posiciones 7–13
- `CredentialC.DATA` → posiciones 14–19
- Máscara: `byte[i] = ord(token[i]) XOR ((i * 73 + 19) AND 0xFF)`

Guarda este script como `generar_token.py` **fuera del repositorio** (o bórralo después) y ejecútalo con tu token:

```python
TOKEN = "demo-runnersz-2026-1"   # exactamente 20 caracteres

def fragmento(inicio, fin):
    return [ord(TOKEN[i]) ^ ((i * 73 + 19) & 0xFF) for i in range(inicio, fin)]

def kotlin(nombre, valores):
    cuerpo = ", ".join(str(v) for v in valores)
    return f"internal object {nombre} {{\n    val DATA = intArrayOf({cuerpo})\n}}\n"

print(kotlin("CredentialA", fragmento(0, 7)))
print(kotlin("CredentialB", fragmento(7, 14)))
print(kotlin("CredentialC", fragmento(14, len(TOKEN))))
```

Salida esperada (tres objetos Kotlin):

```text
internal object CredentialA {
    val DATA = intArrayOf(...)
}

internal object CredentialB {
    val DATA = intArrayOf(...)
}

internal object CredentialC {
    val DATA = intArrayOf(...)
}
```

Pega cada `DATA` en su archivo:

- `android/app/src/main/java/com/runnersz/reto2/android/security/CredentialA.kt`
- `.../CredentialB.kt`
- `.../CredentialC.kt`

Si tu token **no mide 20 caracteres**, ajusta también en `SecureTokenProvider.kt`:

```kotlin
private const val TOKEN_LENGTH = 20   // pon la longitud real
```

Los desplazamientos `0`, `7` y `14` pueden quedarse así siempre que el token tenga al menos 15 caracteres; el fragmento C cubre el resto.

### 4.4 Recompilar y reinstalar

```powershell
cd android
.\gradlew.bat testDebugUnitTest assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

✅ Inicio debe volver a conectar (`auth/check` → 204). Las pruebas `ApiClientTest`, `ApiConfigTest` y `SecureTokenProviderTest` validan la URL y la reconstrucción.

### 4.5 Advertencia de seguridad

- Cualquier token dentro de un APK es **extraíble** con suficiente esfuerzo (instrumentación, memoria, reconstrucción de fragmentos). La ofuscación y R8 solo elevan el coste.
- Si el APK se distribuye, **considera el token comprometido** y rótalo antes de la demo.
- No subas a Git un APK con un token real, ni captures de pantalla con el token.
- La solución fuerte de producción es no distribuir el token: un proxy propio con login y credenciales efímeras.

---

## 5. Checklist después de cambiar credenciales

| # | Comprobación | Esperado |
|---|---|---|
| 1 | `curl -i .../auth/check` con el token nuevo | `204` |
| 2 | `curl -i .../auth/check` con el token viejo | `401` |
| 3 | Escritorio: Ajustes → Probar | Correcto |
| 4 | Escritorio: cifrar/descifrar un fichero | Funciona |
| 5 | Android: Inicio | Conectado |
| 6 | Android: orden e imagen | Funcionan |
| 7 | Unity | No depende de la API: no hay que tocar nada |

---

## 6. Qué NO debes cambiar

- El prefijo `/api/v1`, los nombres de campo (`text`, `key`, `shift`, `mode`, `file`, `password`) ni los códigos de error: están fijados por el contrato y los tres clientes dependen de ellos.
- El formato `.enc` (`RZEN`, PBKDF2 600 000, bloques de 1 MiB, SHA-256 final): cambiarlo rompe la compatibilidad entre clientes y las pruebas del contrato.
- El umbral de cobertura (80 %) ni las reglas de ramas: son requisitos de calidad.

Cualquier cambio de API o de formato se acuerda como `contract-change` entre los dos responsables del proyecto, con aprobación explícita de ambos.

---

[Volver al índice](Home.md)
