# Unity - RunnersZ 6.3

## Identificacion y autoria

- Apartado: Unity, trabajo asignado a Jairo.
- Rama: feature/unity-v2.
- Responsable: Jairo.
- Revision: Alain.
- Contrato: Contrato_Tecnico_RunnersZ_v2.docx.
- Alcance: juego 2D, logica pura testeable y guia de escena.
- Estado: En revision. No es Hecho hasta tener CI verde, revision aprobada y
  evidencias de Unity 6.3.

## Requisitos del contrato

- Crear manualmente un proyecto 2D con Unity 6.3, version 6000.3.x.
- Enemigo con Rigidbody2D y Collider2D que avanza hacia la puerta.
- Puerta con Collider2D configurado como trigger.
- OnTriggerEnter2D en la puerta produce derrota.
- Mostrar un texto cifrado con Caesar.
- Recibir una respuesta, validarla y mostrar victoria si es correcta.
- Mantener estados en curso, victoria y derrota.
- Permitir reinicio de partida.
- Separar la logica C# de la escena y hacerla testeable.
- Incorporar dificultad, rondas, pistas, vidas y puntuacion conforme al alcance
  de Jairo y a los criterios del contrato.

## Estructura

- Assets/Scripts/Logic/: Caesar, validacion, estados, rondas, dificultad,
  pistas, puntuacion y banco de frases.
- Assets/Scripts/Game/: enemigo, puerta y configuracion de fisica.
- Assets/Scripts/Editor/SceneBuilder.cs: apoyo para construir la escena.
- Assets/Scripts/Logic/*Tests.cs: pruebas de logica pura.
- README.md: esta guia de implementacion y validacion.

La carpeta contiene codigo y guia. La escena, los prefabs y los recursos
generados por el editor deben crearse y revisarse en Unity 6.3.

## Puesta en marcha

1. Instalar Unity Hub y Unity 6.3 (6000.3.x).
2. Crear un proyecto 2D en esta carpeta, conservando el contenido versionado.
3. Instalar Test Framework, Code Coverage y TextMeshPro desde Package Manager.
4. Crear la escena con enemigo, puerta trigger, camara y HUD.
5. Configurar Rigidbody2D y Collider2D del enemigo.
6. Configurar Collider2D trigger y OnTriggerEnter2D de la puerta.
7. Conectar texto cifrado, entrada, boton validar, estados y reinicio.
8. Ejecutar EditMode y PlayMode.
9. Revisar cobertura y documentar evidencias antes de generar el entregable.

La licencia de Unity y el editor son requisitos del entorno de desarrollo. No se
guardan licencias, credenciales ni artefactos privados en el repositorio.

## Criterios de aceptacion

- El enemigo avanza hacia la puerta en estado En curso.
- El contacto con la puerta cambia a Derrota.
- Una respuesta Caesar correcta cambia a Victoria.
- Una respuesta incorrecta conserva la partida y muestra un error.
- Reiniciar devuelve la partida al estado inicial.
- Cambio de dificultad y ronda altera los parametros previstos.
- Pistas y puntuacion se actualizan sin romper la validacion.
- La logica se puede probar sin depender de GameObjects.
- La escena funciona en PlayMode y los resultados quedan documentados.

## Validacion pendiente

La ejecucion de EditMode, PlayMode, Code Coverage, creacion de escena y
generacion de APK requieren Unity 6.3 y licencia. Hasta disponer de ese entorno
deben aparecer como PENDIENTE-HUMANO, nunca como pruebas pasadas.

## Documentacion relacionada

- docs/manual-uso-unity.md
- docs/evidencias/android-unity.md
