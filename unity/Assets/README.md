# Assets

## Uso
Los PNG de `Art/` se cargan automáticamente al iniciar la escena `Game`.

## Cómo funciona
`background.png` se usa como fondo, `sheet.png` conserva la hoja de sprites, `enemy.png` se recorta en ocho fotogramas para los enemigos y `door.png` se escala para la puerta.

## Qué utiliza
Unity `Texture2D`, `Application.dataPath` y los assets PNG incluidos en esta carpeta.

## Diseño
Los recursos se mantienen fuera de la escena para poder sustituir arte sin rehacer el layout; responde al cambio visual solicitado para fondo, enemigo y puerta.
