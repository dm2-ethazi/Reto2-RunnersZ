# 05 · Instalación del minijuego Unity

Minijuego **autónomo** de descifrado César para **Windows y Android**. No consume la API del servidor: toda la lógica cabe en el propio juego.

---

## Antes de empezar

- **Unity Hub** y el editor **6000.3.24f1** con el módulo **Android Build Support** (SDK/NDK/OpenJDK) si vas a generar el APK ([01 · Requisitos](01-Requisitos.md#6-unity-hub-editor-y-módulo-android)).
- Licencia de Unity activa (Personal es suficiente).
- `adb` solo si vas a instalar el APK en un móvil.

> Los ejecutables **no están en Git** (`unity/Builds/` está ignorado). Hay que regenerarlos; es el objetivo de esta página.

---

## Paso 1 — Abrir el proyecto

1. Abre **Unity Hub** → **Projects → Add → Add project from disk**.
2. Selecciona la carpeta `unity/` del repositorio (la que contiene `Assets/`, `Packages/` y `ProjectSettings/`).
3. Ábrelo con la versión **6000.3.24f1**.

La primera apertura importa assets y puede tardar varios minutos. Si Unity avisa de que el proyecto se creó con otra versión, cancela y usa exactamente 6000.3.24f1: la escena y los ajustes están fijados a esa versión.

✅ En la ventana *Project* ves `Assets/Scenes/Game.unity` y, en la barra de menús, aparece **RunnersZ**.

---

## Paso 2 — Jugar dentro del editor

1. Abre `Assets/Scenes/Game.unity` (doble clic).
2. Pulsa **Play** ▶.
3. Elige **Fácil**, **Normal** o **Difícil** y juega.

✅ El juego entra en modo partida en apaisado (1920×1080 en la vista *Game*).

Si la escena no carga o se ha corrompido, regenérala con el menú:

**RunnersZ → Build Game Scene**

Ese comando crea de nuevo la escena inicial con cámara, lienzo y el objeto `RunnerGame`.

---

## Paso 3 — Menús del proyecto

| Menú | Qué hace |
|---|---|
| **RunnersZ → Build Game Scene** | Regenera `Assets/Scenes/Game.unity` |
| **RunnersZ → Configure Android** | Aplica paquete `com.runnersz.game`, apaisado, ARM64, IL2CPP, OpenGL ES 3 y minSdk 25 |
| **RunnersZ → Apply app icon** | Importa `Assets/Art/app-icon.png` y lo asigna a Android y Windows |
| **RunnersZ → Build Android APK** | Genera `Builds/Android/RunnersZ.apk` |
| **RunnersZ → Render Layout Previews** | Pinta en PNG los rectángulos de cada pantalla para revisar la geometría |

`Configure Android` es importante: la configuración **no** está garantizada solo por `ProjectSettings`; el script la reaplica antes de compilar para que el APK salga siempre igual.

---

## Paso 4 — Generar el ejecutable de Windows

### Desde el editor

*File → Build Settings…* → plataforma **Windows** → **Build**. Guarda en `Builds/Windows/RunnersZ.exe`.

### En modo batch (reproducible, sin abrir la interfaz)

Desde la **raíz del repositorio**, en PowerShell:

```powershell
& 'C:\Program Files\Unity\Hub\Editor\6000.3.24f1\Editor\Unity.exe' `
  -batchmode -nographics -projectPath "$PWD/unity" `
  -executeMethod RunnersZ.Editor.SceneBuilder.BuildWindows -quit `
  -logFile "$PWD/unity/windows-build.log"
```

✅ Se crea `unity/Builds/Windows/RunnersZ.exe`. El log queda en `unity/windows-build.log`.

> Revisa el log: `-quit` devuelve el control aunque la compilación falle. Busca `Build succeeded` y que no haya `Exception` al final.

Para ejecutarlo:

```powershell
.\unity\Builds\Windows\RunnersZ.exe
```

Si lo compartes, **comparte toda la carpeta `Builds/Windows/`** (incluye `RunnersZ_Data/` y las DLL); el `.exe` solo no funciona.

---

## Paso 5 — Generar el APK de Android

### Desde el editor

**RunnersZ → Configure Android** y después **RunnersZ → Build Android APK**.

### En modo batch

```powershell
& 'C:\Program Files\Unity\Hub\Editor\6000.3.24f1\Editor\Unity.exe' `
  -batchmode -nographics -projectPath "$PWD/unity" `
  -executeMethod RunnersZ.Editor.AndroidBuild.BuildBatch `
  -logFile "$PWD/unity/android-build.log"
```

✅ Se crea `unity/Builds/Android/RunnersZ.apk`.

Configuración aplicada por el script:

| Ajuste | Valor |
|---|---|
| Paquete | `com.runnersz.game` |
| Orientación | Apaisado (sin autorrotación vertical) |
| Arquitectura | `arm64-v8a` |
| Backend de scripts | IL2CPP |
| Gráficos | OpenGL ES 3 |
| `minSdk` | 25 |

Comprobación del APK (opcional, con las *build-tools* del SDK):

```powershell
aapt2 dump badging unity\Builds\Android\RunnersZ.apk | Select-String "package|native-code|landscape"
```

✅ Debe mostrar `package: name='com.runnersz.game'`, `native-code: 'arm64-v8a'` y la característica `android.hardware.screen.landscape`.

Instalar en un móvil con depuración USB:

```powershell
adb install -r unity\Builds\Android\RunnersZ.apk
```

---

## Paso 6 — Ejecutar las pruebas

*Window → General → Test Runner* → pestaña **EditMode** → **Run All**; después **PlayMode** → **Run All**.

✅ Resultados de referencia (5 de noviembre de 2026):

- **EditMode: 25 pruebas aprobadas, 0 fallos.** Cinco de ellas recorren nueve perfiles de pantalla (móviles 16:9 y 19.5:9 con notch, tablets 4:3, 21:9, escritorio y una densidad extrema) comprobando límites, área segura, solapes, tamaño táctil en dp y que el lienzo llena la pantalla.
- **PlayMode: 2 pruebas aprobadas, 0 fallos**, sin excepciones en ejecución.

Las pruebas cubren César, normalización, dificultad, rondas, puntuación, pistas, victoria, derrota por enemigos y reinicio. **No se ha medido cobertura** en Unity.

---

## Paso 7 — Revisar la geometría de la interfaz (opcional)

**RunnersZ → Render Layout Previews** genera PNG con los rectángulos reales de cada pantalla. Sirve para comprobar que nada se sale del área segura sin necesidad de un dispositivo.

✅ Los PNG aparecen en la carpeta que indique el log del comando.

---

## Estructura del proyecto

```text
unity/
  Assets/
    Scenes/Game.unity        escena jugable
    Scripts/Game/            RunnerGame (interfaz y simulación), MobileUi, GameAudio, Door, EnemyHopper
    Scripts/Logic/           Caesar, AnswerNormalizer, DifficultyConfig, GameState, HintScore, PhraseBank…
    Editor/                  SceneBuilder, AndroidBuild, LayoutPreview
    Art/                     background.png, sheet.png, enemy.png, door.png, app-icon.png
    Tests/                   EditMode y PlayMode
  Packages/
  ProjectSettings/           versión 6000.3.24f1
  Builds/                    salidas (ignoradas por Git)
```

Los scripts `Door` y `EnemyHopper` se conservan como componentes previos; la escena entregada ejecuta `RunnerGame` y su simulación de avance, sin depender de tags.

---

## Problemas típicos

| Síntoma | Solución |
|---|---|
| Unity pide actualizar el proyecto | Instala 6000.3.24f1; no actualices la escena |
| «Android module not installed» | Añade *Android Build Support* + SDK/NDK/OpenJDK al editor |
| `Build failed` sin detalle | Lee `unity/android-build.log` o `windows-build.log` |
| El `.exe` no arranca al copiarlo solo | Copia toda la carpeta `Builds/Windows/` |
| El APK no se instala | Activa "orígenes desconocidos" o usa `adb install -r` |
| El juego se ve en vertical | Ejecuta `RunnersZ → Configure Android` y recompila |

Lista completa: [11 · Problemas frecuentes](11-Problemas-frecuentes.md).

---

## Siguiente paso

- Reglas y controles del juego: [09 · Uso de Unity](09-Uso-unity.md)
- Demo completa: [10 · Verificación y demo](10-Verificacion-y-demo.md)

[Volver al índice](Home.md)
