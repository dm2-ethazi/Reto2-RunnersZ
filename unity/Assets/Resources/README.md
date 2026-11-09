# Recursos del juego


## Fondo del carril
Inicia Game.unity y una partida para visualizar background.png. PixelFantasyArt lo carga con Resources.Load<Texture2D>("background"); RunnerGame lo encuadra con escala uniforme y recorte vertical para llenar 1104x70. Depende de Unity IMGUI y del PNG original. El importador conserva las dimensiones, sin compresion ni mipmaps y con filtrado punto, para preservar el pixel art (pliego 4.11; contrato 9.6). La franja panoramica requiere recortar el paisaje para evitar deformarlo.

## Puerta al final del carril
La puerta se carga desde Resources/door.png y se dibuja en el extremo derecho del recuadro, dentro del suelo del carril, en un rectangulo cuadrado de 96x96 para conservar la proporcion 1:1. Su base queda en y=680, alineada con el recorrido del enemigo; el objeto visual coincide con el objetivo de colision existente. Los importadores Art y Resources usan filtrado punto y no usan mipmaps, compresion ni conversion a potencia de dos.

## Correccion del recurso de puerta
Para verificarlo, inicia una partida en Assets/Scenes/Game.unity: al final del carril debe verse la puerta gotica. La imagen original de la puerta estaba almacenada en Assets/Art/sheet.png, mientras door.png contenia un enemigo. Se copio la imagen de la puerta sin modificar sus bytes a Art/door.png y Resources/door.png, que carga PixelFantasyArt mediante Resources.Load. Se conservan los metadatos y el filtrado punto. Esta correccion del objetivo visual corresponde al pliego 4.11 y al contrato 9.6.
