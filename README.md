# RunnersZ — Unity

## Autoría y alcance

- Author / responsable: Jairo (@jairoben).
- Revisor previsto: Alain; no implica revisión aprobada.
- Rama: feature/unity-v2. PR: #40.
- Referencia principal: Contrato_Tecnico_RunnersZ_v2.docx.
- Minijuego 2D autónomo en Unity 6.3; no consume la API del servidor.

## Contenido real

| Carpeta o componente | Responsabilidad |
| --- | --- |
| Assets/Scripts/Logic/ | César, normalización, respuestas, estados, dificultad y rondas. |
| HintScore, PhraseBank | Apoyo para pistas, puntuación y frases. |
| GameSession | Estados EnCurso, Victoria y Derrota; puntuación y reinicio lógico. |
| DoorLives | Contador de vidas y reinicio. |
| Assets/Scripts/Game/EnemyHopper.cs | Desplaza el enemigo a intervalos mediante Rigidbody2D. |
| Assets/Scripts/Game/Door.cs | Detecta enemigos, resta vidas, desactiva el objeto y emite Hit. |
| Assets/Scripts/Editor/SceneBuilder.cs | Menú RunnersZ/Build Game Scene para una escena inicial. |
| Logic/*Tests.cs | Casos de lógica sujetos a configuración del Test Framework. |

## Flujo de juego previsto

Mostrar un mensaje César, aceptar respuesta y validarla mientras el enemigo
avanza. La respuesta correcta debe conducir a victoria y el evento de puerta
debe conectarse al criterio de derrota del contrato. Reiniciar restablece la
sesión y la escena. Dificultad, rondas, pistas y puntuación deben enlazarse al HUD.

Esto describe el objetivo. Door actualmente descuenta vidas y emite un evento;
no invoca directamente GameSession.Lose. La unión escena-lógica sigue pendiente.

## Preparación del editor

1. Instalar Unity Hub y Unity 6.3 (6000.3.x) con licencia válida.
2. Crear el proyecto 2D conservando Assets/Scripts del repositorio.
3. Configurar Test Framework y Code Coverage; preparar los recursos de texto
   que requiera la interfaz elegida.
4. Crear Assets/Scenes y registrar los tags Enemy y Door antes del generador.
5. Revisar SceneBuilder antes de ejecutar RunnersZ/Build Game Scene.
6. Configurar cámara, posiciones, sprites, colliders, HUD y controles.
7. Conectar EnemyHopper, eventos de Door, validación, sesión y reinicio.

SceneBuilder solo crea objetos iniciales. No construye un juego terminado:
faltan conexiones de componentes, disposición visual y pruebas en el editor.
No se han comprobado compilación ni resolución de paquetes en Unity.

## Pruebas y aceptación

- EditMode: César, normalización, validación, vidas, estados, puntuación y rondas.
- PlayMode: movimiento, trigger, conexión de derrota, victoria y reinicio.
- Confirmar que no se admiten acciones de juego tras finalizar la sesión.
- Comprobar interfaz, dificultades y progresión en la plataforma de entrega.
- Medir cobertura conforme al contrato; registrar logs, versión y evidencias.

## Estado y entregables

No hay ejecución verificada de Unity 6.3, EditMode, PlayMode, cobertura ni build.
Escena, prefabs, configuración de proyecto y UI requieren trabajo en el editor.
La licencia no se guarda en Git. Solo tras pruebas y revisión se puede considerar
terminado el apartado. No se declara ningún APK ni ejecutable generado.

## Documentación

El README de unity/ contiene esta misma guía. Consulta también el manual Unity
de docs/. Esta documentación distingue código disponible de integración pendiente.
