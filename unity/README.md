# RunnersZ - Unity

Minijuego autonomo de descifrado Cesar para Windows y Android, con Unity 6000.3.24f1.
No consume la API del servidor. Responsable: Jairo (@jairoben).
Rama de trabajo: feature/unity-v2.

## Jugar

Abre `unity/Builds/Windows/RunnersZ.exe` en el ordenador de desarrollo.
Para compartirlo, conserva toda la carpeta Windows, incluidos los datos y DLL.
El ejecutable de Windows no se versiona: se regenera desde el proyecto. El APK de la entrega
(release **v1.0.0**, `bundleVersion 1.0`) sí se publica en la página de releases:
[descarga del APK](https://github.com/dm2-ethazi/Reto2-RunnersZ/releases/download/v1.0.0/RunnersZ-unity_v1.0.0.apk).

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

## Jugar en Android

El APK se genera en `unity/Builds/Android/RunnersZ.apk` (arm64-v8a, IL2CPP,
OpenGL ES 3, minSdk 25) y no se versiona; la copia de la entrega es la del release
(página de releases del repositorio). Para instalarlo en un telefono
con depuracion USB activa:

```powershell
adb install -r unity/Builds/Android/RunnersZ.apk
```

La partida va en apaisado y a 60 fps. Todo se resuelve tocando la pantalla:
el boton Manuscrito abre la rueda de pares, el campo de respuesta despliega el
teclado del sistema, y Validar, Pista, Reiniciar y Cambiar dificultad son
botones de 48 dp. La barra atras de Android cierra el manuscrito, vuelve al
menu y, si ya estas en el menu, sale del juego.

La interfaz se mide en dp sobre el area segura del dispositivo: el lienzo llena
la pantalla sin barras negras, respeta el recorte del notch y mantiene el campo
de respuesta visible cuando aparece el teclado. Si el sistema no tiene Georgia
ni Times New Roman, el texto serif usa la fuente incluida en Unity.

## Validacion del 5 de noviembre de 2026

- EditMode: 24 pruebas aprobadas, 0 fallos. Cinco son nuevas y recorren nueve
  perfiles de pantalla (moviles 16:9 y 19.5:9 con notch, tablets 4:3, 21:9,
  escritorio y una densidad extrema) comprobando limites, area segura, solapes,
  tamano tactil en dp y que el lienzo llena la pantalla.
- PlayMode: 2 pruebas aprobadas, 0 fallos, sin excepciones en ejecucion.
- APK Android: generado con 0 errores; `aapt2` confirma paquete
  `com.runnersz.game`, `targetSdkVersion 36`, `native-code arm64-v8a` y la
  caracteristica `android.hardware.screen.landscape`.
- Geometria revisada a ojo con `RunnersZ/Render Layout Previews`, que pinta los
  rectangulos reales de cada pantalla en PNG.
- Limitacion: no se ha instalado ni probado el APK en un telefono fisico, asi
  que el teclado tactil y el rendimiento real siguen sin verificar en hardware.

## Validacion del 2 de noviembre de 2026

- Generacion de escena y compilacion de scripts con Unity: correctas.
- EditMode: 10 pruebas aprobadas, 0 fallos.
- PlayMode: 2 pruebas aprobadas, 0 fallos.
- Windows x64: ejecutable generado; proceso iniciado y registro inspeccionado.
- Evidencias XML: resultados del Test Runner (no se conservan en el repositorio).

Las pruebas cubren Cesar, normalizacion, dificultad, rondas, puntuacion,
pistas, victoria, derrota por enemigos y reinicio. No se ha medido cobertura.

## Regenerar Windows

Desde la raiz del repositorio, con Unity instalado y licencia activa:

```powershell
& 'C:\Program Files\Unity\Hub\Editor\6000.3.24f1\Editor\Unity.exe' `
  -batchmode -nographics -projectPath "$PWD/unity" `
  -executeMethod RunnersZ.Editor.SceneBuilder.BuildWindows -quit `
  -logFile "$PWD/unity/windows-build.log"
```

## Regenerar Android

El modulo Android del editor debe estar instalado. La configuracion (apaisado,
ARM64, IL2CPP, OpenGL ES 3, paquete `com.runnersz.game`) la aplica el propio
script antes de compilar:

```powershell
& 'C:\Program Files\Unity\Hub\Editor\6000.3.24f1\Editor\Unity.exe' `
  -batchmode -nographics -projectPath "$PWD/unity" `
  -executeMethod RunnersZ.Editor.AndroidBuild.BuildBatch `
  -logFile "$PWD/unity/android-build.log"
```

Las pruebas se ejecutan desde Window > General > Test Runner: EditMode y
PlayMode. Los ensamblados de pruebas estan incluidos en el proyecto.
