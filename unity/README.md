# RunnersZ - Unity

Minijuego autonomo de descifrado Cesar para Windows, con Unity 6000.3.24f1.
No consume la API del servidor. Responsable: Jairo (@jairoben).
Rama de trabajo: feature/unity-v2.

## Jugar

Abre `unity/Builds/Windows/RunnersZ.exe` en el ordenador de desarrollo.
Para compartirlo, conserva toda la carpeta Windows, incluidos los datos y DLL.
El ejecutable no se incluye en Git: se puede regenerar desde el proyecto.

En Unity Hub abre la carpeta `unity`, carga `Assets/Scenes/Game.unity`
y pulsa Play. El menu `RunnersZ/Build Game Scene` regenera la escena inicial.

Selecciona Facil, Normal o Dificil. Introduce el mensaje descifrado y pulsa
Validar o Enter. Cesar rota A-Z; conserva espacios y signos. Facil muestra el
desplazamiento. Hay tres rondas, tres vidas y 3/2/1 pistas segun dificultad.
Cada acierto suma 100 puntos. Cada pista resta 50, con minimo cero.
Un fallo acerca a los enemigos; cuando llegan a la puerta se pierde una vida.
Resolver tres mensajes produce victoria. Perder tres vidas produce derrota.
Reiniciar restaura vidas, puntuacion y ronda; puedes cambiar de dificultad.

La interfaz y los enemigos usan formas basicas dibujadas por RunnerGame.
Los scripts Door/EnemyHopper se conservan como componentes previos; la escena
entregada ejecuta RunnerGame y su simulacion de avance, sin depender de tags.

## Validacion del 2 de noviembre de 2026

- Generacion de escena y compilacion de scripts con Unity: correctas.
- EditMode: 10 pruebas aprobadas, 0 fallos.
- PlayMode: 2 pruebas aprobadas, 0 fallos.
- Windows x64: ejecutable generado; proceso iniciado y registro inspeccionado.
- Evidencias XML: `docs/evidencias/unity/`.
- Seguimiento y limitaciones: `docs/planificacion/unity-tareas.md`.

Las pruebas cubren Cesar, normalizacion, dificultad, rondas, puntuacion,
pistas, victoria, derrota por enemigos y reinicio. No se ha medido cobertura.
No se declara APK ni revision visual humana aprobada.

## Regenerar Windows

Desde la raiz del repositorio, con Unity instalado y licencia activa:

```powershell
& 'C:\Program Files\Unity\Hub\Editor\6000.3.24f1\Editor\Unity.exe' `
  -batchmode -nographics -projectPath "$PWD/unity" `
  -executeMethod RunnersZ.Editor.SceneBuilder.BuildWindows -quit `
  -logFile "$PWD/unity/windows-build.log"
```

Las pruebas se ejecutan desde Window > General > Test Runner: EditMode y
PlayMode. Los ensamblados de pruebas estan incluidos en el proyecto.
