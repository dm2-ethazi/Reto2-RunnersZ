# Plan de Jairo: ejecución secuencial

Autor: Jairo (@jairoben). Contrato principal: v2, 28/10/2026.
Ramas únicas: feature/android-v2 y feature/unity-v2.
PR #39 y #40 son trabajo incompleto; no acreditan integración ni CI.

## Orden
1. #9 Android: proyecto, ajustes, autenticación y navegación.
2. #10 Android: imagen por API.
3. Issue específico de órdenes César y accesibilidad, según Anexo I #15.
4. #11 Unity: escena, enemigo, puerta y física.
5. #12 Unity: frases, César y validación.
6. #16 Unity: dificultad, rondas, pistas y paneles.
7. #17 APK instalables.
8. #18 manuales con capturas y #19 dispositivo/demo.
9. #20 índice de entrega, #21 revisión y #22 evidencias de pipeline.

Una sola tarea En curso. Las demás esperan en Backlog mientras no cumplan
aceptación. Cada tarea incorpora sus pruebas y documentación durante el trabajo.
En revisión requiere un cambio comprobable listo para revisar. Hecho requiere
integración en develop, revisión cruzada y checks verdes. Nunca autoaprobar.

## Inicio de #9
La Activity actual no contiene interfaz y el cliente omite el token.
El manifest referenciaba AppTheme sin recurso versionado; se usa ahora el
tema Material del sistema y nombre completo de Activity. Se habilita HTTP
para la demo LAN del contrato: no usar con datos sensibles fuera de esa red.
Pendientes: Gradle reproducible, SDK, ajustes persistidos, /auth/check,
navegación, accesibilidad y pruebas. No se ha compilado ni generado APK.

## Responsabilidades
Jairo: Android, Unity, APK y evidencias correspondientes al Anexo I.
Alain: API, JavaFX y CI. Coordinación de checks mediante PR, sin modificar sus
ramas. La asignación errónea de #16 y #17 se corrige conforme al contrato.