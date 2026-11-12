# 10 · Verificación y guion de la demo

Checklist para comprobar que **todo el proyecto funciona de extremo a extremo** antes de entregar o demostrar. Cada punto tiene el comando, el resultado esperado y un hueco para anotar la evidencia.

> Regla de honestidad del equipo (C.1-4): **no escribas «pasa» sin haber ejecutado el comando y visto la salida**. Si algo no se puede probar (sin móvil, sin licencia, sin red), se anota `PENDIENTE-HUMANO`, no «verde».

---

## 0. Preparación (5 min)

| # | Comprobación | Comando | Esperado |
|---|---|---|---|
| 0.1 | Herramientas | `git --version` · `uv --version` · `java -version` | Git 2.x, uv 0.x, **Java 21** |
| 0.2 | Repositorio limpio | `git status -sb` | Sin cambios inesperados |
| 0.3 | API pública viva | abrir `https://runnersz-api.onrender.com/api/v1/health` | `{"status":"ok","version":"2.0.0"}` |

---

## 1. Servidor (10 min)

| # | Comprobación | Comando | Esperado |
|---|---|---|---|
| 1.1 | Dependencias | `cd backend && uv sync --locked` | Termina sin errores |
| 1.2 | Pruebas y cobertura | `uv run pytest` | `62 passed`, cobertura **97,71 %** (umbral 80 %) |
| 1.3 | Estilo | `uv run ruff check .` y `uv run isort . --check-only` | Sin avisos |
| 1.4 | Arranque | `uv run --python 3.12 uvicorn runnersz.main:app --app-dir src --env-file .env --host 127.0.0.1 --port 8000 --reload` | `Application startup complete` |
| 1.5 | Salud | `curl http://127.0.0.1:8000/api/v1/health` | `{"status":"ok","version":"2.0.0"}` |
| 1.6 | Auth correcta | `curl -i .../api/v1/auth/check -H "Authorization: Bearer <TU_TOKEN>"` | `204` |
| 1.7 | Auth sin token | `curl -i .../api/v1/auth/check` | `401 UNAUTHORIZED` |
| 1.8 | César | POST `text/caesar` con `Ataque al amanecer`, 3, encrypt | `{"result":"Dwdtxh do dpdqhfhu"}` |
| 1.9 | Vigenère | POST `text/vigenere` con `Hola Mundo, ¿qué tal?`, `LUNA` | `{"result":"Siya Xoadz, ¿khé tlf?"}` |
| 1.10 | Error controlado | POST `text/caesar` con `shift: 0` | `400 INVALID_SHIFT` |

---

## 2. Ficheros y compatibilidad entre clientes (10 min)

Esta es la prueba que demuestra que **todos los clientes hablan el mismo formato**.

1. Crea un fichero de pruebas:

```powershell
"RunnersZ: prueba de integridad" | Out-File -Encoding utf8 prueba.txt
```

2. **Cifra con el escritorio**: pestaña *Ficheros · AES*, elige `prueba.txt`, contraseña `clave123`, **Cifrar archivo**. → se crea `prueba.txt.enc`.

3. **Descifra con el escritorio**: mismo `.enc`, misma contraseña. → se crea `prueba.txt.recuperado` con el mismo contenido.

4. **Descifra con la API** (demuestra el formato):

```powershell
curl.exe -s -X POST http://127.0.0.1:8000/api/v1/files/decrypt `
  -H "Authorization: Bearer <TU_TOKEN>" `
  -F "file=@prueba.txt.enc" -F "password=clave123" `
  -o prueba-api.txt -D cabeceras.txt
```

✅ `cabeceras.txt` contiene `x-integrity: verified` y `prueba-api.txt` coincide con `prueba.txt`.

5. **Prueba negativa**: repite el descifrado con `password=otra` → `422 DECRYPT_FAILED`.

6. **Cifra una imagen** con el escritorio y guárdala como `.enc`: la usarás para probar la pestaña *Imagen* de Android.

| # | Comprobación | Esperado |
|---|---|---|
| 2.1 | `.enc` generado por el escritorio se descifra por la API | Contenido idéntico + `X-Integrity` |
| 2.2 | Contraseña incorrecta | `422 DECRYPT_FAILED` |
| 2.3 | Original intacto tras cifrar | `prueba.txt` sigue ahí y sin cambios |

---

## 3. Escritorio (5 min)

| # | Comprobación | Esperado |
|---|---|---|
| 3.1 | Build con pruebas y umbral | `.\mvnw.cmd clean verify` → `BUILD SUCCESS`, cobertura ≥ 80 % |
| 3.2 | Arranque del JAR | `java -jar target\runnersz-desktop.jar` abre la ventana |
| 3.3 | Ajustes | **Probar** responde correctamente (204) |
| 3.4 | Cifrar/descifrar | Ver §2, pasos 2 y 3 |
| 3.5 | Vigenère y César | Resultados iguales a los de la API |
| 3.6 | Idiomas | Cambiar el selector y comprobar que se traduce la interfaz |

---

## 4. Android (10 min)

| # | Comprobación | Esperado |
|---|---|---|
| 4.1 | Pruebas y build | `.\gradlew.bat testDebugUnitTest assembleDebug` → `BUILD SUCCESSFUL`, 19 pruebas |
| 4.2 | Instalación | `adb install -r app\build\outputs\apk\release\app-release.apk` → `Success` |
| 4.3 | Inicio | Comprobación automática correcta; estado conectado |
| 4.4 | Orden | Texto César + desplazamiento → resultado correcto y copiable al tocar |
| 4.5 | Imagen | `.enc` de imagen de §2 + clave → vista previa con integridad verificada |
| 4.6 | Rotación | Girar el dispositivo y comprobar que no se pierden formulario ni resultado |
| 4.7 | Error de red | Activar modo avión y pulsar **Probar API** → mensaje claro, app abierta |

> 4.2–4.7 requieren un dispositivo físico o emulador. Si no hay, márcalo `PENDIENTE-HUMANO`.

---

## 5. Unity (10 min)

| # | Comprobación | Esperado |
|---|---|---|
| 5.1 | Abrir escena | `Assets/Scenes/Game.unity` carga sin errores |
| 5.2 | EditMode | Test Runner → EditMode → Run All → 25 aprobadas, 0 fallos |
| 5.3 | PlayMode | Test Runner → PlayMode → Run All → 2 aprobadas, 0 fallos |
| 5.4 | Partida Fácil | Resolver los tres mensajes → victoria |
| 5.5 | Fallo y vidas | Fallar tres veces → derrota con sonido e icono distintos |
| 5.6 | Pista y puntuación | Usar una pista → −50 puntos |
| 5.7 | Windows | `unity\Builds\Windows\RunnersZ.exe` arranca |
| 5.8 | Android | APK instalado en apaisado, controles táctiles y botón **Manuscrito** |

---

## 6. Calidad del repositorio (5 min)

| # | Comprobación | Comando | Esperado |
|---|---|---|---|
| 6.1 | Hooks instalados | `uv run --directory backend pre-commit run --all-files` | Sin fallos (ver nota en [13](13-Flujo-de-trabajo-y-CI.md#6-aviso-conocido-sobre-los-hooks)) |
| 6.2 | Rama protegida | `python scripts/check_branch.py` | Falla a propósito si estás en `main`/`develop` |
| 6.3 | Mensaje de commit | `python scripts/check_commit_message.py mensaje.txt` | Valida `TIPO: asunto` |
| 6.4 | Pruebas de herramientas | `uv run --python 3.12 --with pytest python -m pytest scripts/tests -q` | 43 pruebas aprobadas |
| 6.5 | CI | Pestaña *Actions* del PR | Jobs `backend` y `desktop` en verde |

---

## 7. Guion de la demo (10 minutos)

Orden pensado para contar una historia y no repetir pasos:

1. **Minuto 0–1 · Contexto.** La criptografía vive en el servidor; escritorio y Android son clientes finos; Unity es autónomo.
2. **Minuto 1–3 · API.** Abre `/docs`, autoriza y muestra `auth/check` (204). Cifra un archivo y descífralo con `X-Integrity: verified`.
3. **Minuto 3–5 · Escritorio.** Cifra `prueba.txt` y descífralo. Muestra Vigenère con el ejemplo del contrato y el cambio de idioma.
4. **Minuto 5–7 · Android.** Inicio conectado, una orden César descifrada y una imagen `.enc` recuperada desde el móvil.
5. **Minuto 7–9 · Unity.** Una ronda Fácil, una pista, un fallo para ver avanzar al enemigo y la victoria.
6. **Minuto 9–10 · Cierre.** Enseña los números: 62 pruebas backend (97,71 %), 64 escritorio (82 %), 19 Android, 25+2 Unity, y el CI en verde.

Antes de la demo: deja la API despierta (ábrela un par de minutos antes), el JAR compilado, el móvil con el APK instalado y el juego abierto en el menú.

---

## 8. Plantilla de evidencias

Para cada punto ejecutado, adjunta la evidencia al PR (comando, salida y fecha) con este formato:

```text
PT-04 · Descifrado de 2 MiB
Comando: curl.exe -X POST .../files/decrypt -F file=@foto.png.enc -F password=clave123
Salida: HTTP 200 · x-integrity: verified · 2 097 152 bytes
Fecha: 2026-11-12
Equipo: Windows 11 · JDK 21 · uv 0.12
```

Si el equipo mantiene un plan de pruebas formal, conserva sus identificadores (PT-01…PT-19 y UN-xx) en la evidencia. Esta página es la versión práctica para replicar el proyecto.

---

## Siguiente paso

- Si algo falla: [11 · Problemas frecuentes](11-Problemas-frecuentes.md)
- Antes de publicar APK o servidor: [12 · Personalizar el backend y el token](12-Personalizar-backend-y-token.md)

[Volver al índice](Home.md)
