# 11 · Problemas frecuentes

Síntoma → causa → solución. Empieza por la sección del apartado que falla.

---

## 1. Entorno y versiones

| Síntoma | Causa | Solución |
|---|---|---|
| `java -version` dice 25 o 17 | JDK distinto del requerido | Instala JDK **21** y apunta `JAVA_HOME` (ver [01](01-Requisitos.md#4-escritorio-y-android-jdk-21)) |
| `release version 21 not supported` | El compilador usa un JDK anterior | Igual que arriba |
| `Unsupported class file major version 69` | Gradle 8.9 con JDK 25 | Usa JDK 21 |
| `uv: command not found` | El instalador no actualizó el `PATH` | Cierra y reabre la terminal; en Windows añade `%USERPROFILE%\.local\bin` al `PATH` |
| `adb: command not found` | Platform-Tools no está en el `PATH` | Usa la ruta completa de `platform-tools` o añádela al `PATH` |
| Unity no encuentra módulos Android | Faltan módulos del editor | Añade *Android Build Support* + SDK/NDK/OpenJDK |

---

## 2. Servidor

| Síntoma | Causa | Solución |
|---|---|---|
| `uv sync --locked` falla diciendo que el lock no está al día | `pyproject.toml` cambió y `uv.lock` no | `uv lock` y vuelve a `uv sync` (es un cambio de dependencias: avisa al responsable) |
| `Address already in use` al arrancar | Otro proceso en el puerto 8000 | `netstat -ano \| findstr :8000` y cierra el proceso, o usa `--port 8001` |
| El servidor arranca pero todo devuelve 401 | `RZ_API_TOKEN` vacío o distinto | Revisa `backend/.env` y reinicia (si cambiaste el archivo, reinicia el proceso) |
| `uvicorn: error: unrecognized arguments: --env-file` | Orden de argumentos incorrecto | Copia el comando de [02](02-Instalacion-servidor.md#paso-3--arrancar-el-servidor) tal cual |
| Docker arranca pero la API devuelve 401 | No hay `.env` y se usa el token de reserva `change-me` | Crea `.env` con tu token o define `RZ_API_TOKEN` en el entorno |
| Swagger devuelve 401 tras autorizar | Se añadió `Bearer` a mano o se cacheó el token viejo | Escribe **solo** el token, recarga con **Ctrl+F5** y autoriza otra vez |
| `act` no reproduce el CI | Faltan Docker o la imagen de `.actrc` | Instala Docker y ejecuta `act push -j backend`; no es sustituto del CI real |
| Docker: `docker compose up` falla al leer `.env` | Línea no válida o comillas raras | Deja solo `RZ_API_TOKEN=...` y `RZ_MAX_UPLOAD_MIB=...` |
| Docker: contenedor `unhealthy` | La app no responde en `/api/v1/health` | `docker compose logs server`; suele ser el puerto o el token mal formado |

---

## 3. Cliente de escritorio

| Síntoma | Causa | Solución |
|---|---|---|
| `mvnw: Permission denied` | Falta permiso de ejecución | `chmod +x mvnw` |
| La ventana no abre en Linux | Sin entorno gráfico o sin GTK 3 | Instala `libgtk-3-0` y `fontconfig`; usa sesión gráfica |
| `mvn verify` falla por JaCoCo | Cobertura < 80 % | Añade pruebas al código nuevo; no bajes el umbral |
| «Configura la dirección del servidor y el token» | Ajustes vacíos | Ajustes → URL + token → **Probar** → Guardar |
| «No se pudo contactar con el servidor» | API parada, IP/puerto mal, cortafuegos | Comprueba `/health` en el navegador del mismo PC |
| «No se pudo contactar» desde otro PC de la red | El servidor escucha solo en `127.0.0.1` | Arranca con `--host 0.0.0.0` y abre el puerto en el cortafuegos |
| «La API no confirmó la integridad» | Servidor ajeno al contrato | Usa la API del repositorio |
| «Contraseña incorrecta o fichero dañado» | Clave distinta o `.enc` alterado | Usa la contraseña original; no edites el `.enc` |

---

## 4. Android

| Síntoma | Causa | Solución |
|---|---|---|
| `SDK location not found` | Falta `local.properties` o `ANDROID_HOME` | Crea `android/local.properties` con `sdk.dir=...` |
| `failed to find target with hash string 'android-35'` | Falta *SDK Platform 35* | Instálalo en el SDK Manager |
| Gradle descarga eternamente la primera vez | Baja Gradle 8.9 y dependencias | Espera; después queda en caché |
| `adb devices` vacío | Depuración USB apagada o cable solo de carga | Activa depuración, usa cable de datos |
| `unauthorized` | No aceptaste la huella RSA | Acepta el aviso en el móvil o revoca autorizaciones y reconecta |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Firma distinta a la instalada | `adb uninstall com.runnersz.reto2.android` y reinstala |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | Emulador x86 | Usa emulador con imágenes ARM o un móvil real |
| Inicio muestra «sin conexión» | API dormida o sin internet | Espera hasta 50 s y pulsa **Probar API** |
| «Token no válido» tras cambiar el servidor | El token va dentro del APK | Recompila con el token nuevo ([12](12-Personalizar-backend-y-token.md)) |
| No conecta con un servidor HTTP local | La app es solo HTTPS | Publica el backend con HTTPS o modifica `network_security_config.xml` (no recomendado para entrega) |

---

## 5. Unity

| Síntoma | Causa | Solución |
|---|---|---|
| Unity pide actualizar el proyecto | Versión distinta de 6000.3.24f1 | Instala la versión exacta |
| «Android module not installed» | Faltan módulos del editor | Añádelos desde Unity Hub |
| `Build failed` sin detalle | Error en el log batch | Abre `unity/android-build.log` o `windows-build.log` y busca `error CS` |
| La escena `Game` está vacía | Se perdió o se regeneró mal | **RunnersZ → Build Game Scene** |
| El `.exe` no arranca al copiarlo | Falta `RunnersZ_Data/` y DLL | Copia toda la carpeta `Builds/Windows/` |
| El APK va en vertical | Configuración de orientación no aplicada | **RunnersZ → Configure Android** y recompila |
| `adb install` falla con el APK de Unity | Versión previa con otra firma | Desinstala `com.runnersz.game` y reinstala |

---

## 6. Git, hooks y commits

| Síntoma | Causa | Solución |
|---|---|---|
| El hook bloquea el commit en `main` o `develop` | Protección de ramas (`scripts/check_branch.py`) | Crea una rama `feature/...`; no uses `--no-verify` |
| El hook rechaza el mensaje | Formato institucional | `TIPO: asunto` en ≤ 50 caracteres, cuerpo ≤ 72 (ver [13](13-Flujo-de-trabajo-y-CI.md)) |
| `pre-commit` no está instalado | Faltan los hooks locales | `uv sync --locked` y `uv run --directory backend pre-commit install --hook-type pre-commit --hook-type pre-push --hook-type commit-msg` |
| `pre-commit run --all-files` solo ejecuta 3 hooks | El YAML declara `repos:` dos veces | Aviso conocido, explicado en [13](13-Flujo-de-trabajo-y-CI.md#6-aviso-conocido-sobre-los-hooks) |
| `board.py` falla con error de permisos | La CLI de GitHub no tiene los scopes | `gh auth refresh -s repo,project,read:org,workflow` |

---

## 7. Cómo pedir ayuda con un fallo

Incluye siempre:

1. El comando exacto y la salida completa (sin token ni contraseñas).
2. Versiones: `java -version`, `uv --version`, `adb version`, versión de Unity.
3. Sistema operativo y si es móvil físico o emulador.
4. Qué esperabas y qué ocurrió.

Nunca pegues el contenido de `backend/.env` ni el token de la API.

---

[Volver al índice](Home.md)
