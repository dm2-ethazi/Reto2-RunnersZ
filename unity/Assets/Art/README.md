# Recursos de arte

## Correccion del recurso de puerta
Para verificarlo, inicia una partida en Assets/Scenes/Game.unity: al final del carril debe verse la puerta gotica. La imagen original de la puerta estaba almacenada en Assets/Art/sheet.png, mientras door.png contenia un enemigo. Se copio la imagen de la puerta sin modificar sus bytes a Art/door.png y Resources/door.png, que carga PixelFantasyArt mediante Resources.Load. Se conservan los metadatos y el filtrado punto. Esta correccion del objetivo visual corresponde al pliego 4.11 y al contrato 9.6.
