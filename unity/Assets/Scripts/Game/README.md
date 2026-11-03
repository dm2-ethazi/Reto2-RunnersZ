# Game

## Uso
Abre `Assets/Scenes/Game.unity` y ejecuta Play.

## Cómo funciona
`PixelFantasyArt` carga los PNG desde `Assets/Art` y mantiene texturas de respaldo procedural; `RunnerGame` dibuja el fondo, la puerta y el enemigo animado dentro del canvas del juego.

## Qué utiliza
Unity IMGUI, `Texture2D`, `Application.dataPath` y las clases de lógica de RunnersZ.

## Diseño
La carga por nombre conserva compatibilidad cuando faltan archivos y permite iterar el arte sin cambiar la lógica de rondas, cumpliendo el reemplazo visual pedido.


## Posicion del enemigo en el carril
En Play, inicia una partida: los ocho frames se dibujan dentro del carril inferior (y=610 a 680 del canvas). RunnerGame utiliza una ventana UV comun de 192x336 sobre enemy.png, omitiendo solo los margenes verticales vacios, y la escala proporcionalmente a 32x56. La base y=668 deja margen para la oscilacion de 2 unidades sin invadir los controles. Se conserva el filtrado punto y no se modifica el PNG. Este ajuste de presentacion responde al pliego 4.11 y al contrato 9.6; no cambia las reglas de partida.

## Escalado del fondo del carril
Para comprobarlo, inicia una partida en Game.unity. El fondo llena el carril 1104x70 con escala uniforme y recorte vertical alrededor del suelo (UV inferior 0.19). La altura UV se calcula desde las dimensiones reales de la textura; los arboles y la luna conservan sus proporciones. Por la forma panoramica del carril se muestra una franja del paisaje, no la imagen completa comprimida. Resources/background.png usa filtrado punto, sin mipmaps, compresion ni conversion a potencia de dos. Se conserva el PNG original. Ajuste visual conforme al pliego 4.11 y contrato 9.6.

## Puerta al final del carril
La puerta se carga desde Resources/door.png y se dibuja en el extremo derecho del recuadro, dentro del suelo del carril, en un rectangulo cuadrado de 120x120 para conservar la proporcion 1:1. Su base queda en y=680, alineada con el recorrido del enemigo; el objeto visual coincide con el objetivo de colision existente. Los importadores Art y Resources usan filtrado punto y no usan mipmaps, compresion ni conversion a potencia de dos.

## Visibilidad de la puerta
La puerta se dibuja despues de los enemigos en cada frame, por lo que el PNG de Resources/door.png permanece visible en el extremo final aunque un enemigo haya llegado al objetivo. El orden visual no altera la deteccion ni la perdida de vidas.

## Correccion del recurso de puerta
Para verificarlo, inicia una partida en Assets/Scenes/Game.unity: al final del carril debe verse la puerta gotica. La imagen original de la puerta estaba almacenada en Assets/Art/sheet.png, mientras door.png contenia un enemigo. Se copio la imagen de la puerta sin modificar sus bytes a Art/door.png y Resources/door.png, que carga PixelFantasyArt mediante Resources.Load. Se conservan los metadatos y el filtrado punto. Esta correccion del objetivo visual corresponde al pliego 4.11 y al contrato 9.6.

La puerta se amplia un 25% para distinguirla mejor: 120x120, centrada en x=1088 y apoyada en y=680. Conserva el PNG original y su proporcion cuadrada.

## Limpieza visual del carril
Se elimina la linea verde horizontal que cruzaba todo el rectangulo del carril. El fondo, la puerta, los enemigos y la etiqueta de llegada permanecen sin cambios.

## Ajuste de apoyo en el suelo
La base de la animacion baja de y=668 a y=674 para que los pies se apoyen visualmente en el terreno del carril. Se mantiene el recorte UV, la escala 32x56 y la oscilacion de 2 unidades.

## Ajuste final del apoyo
La base baja 3 unidades adicionales, de y=674 a y=677, para eliminar el espacio visual bajo los pies. Se mantienen el recorte, la escala y la animacion.

## Fondo ampliado y mensaje recolocado
El carril de paisaje se amplia hacia arriba: pasa de y=610/70 a y=570/110, manteniendo escala uniforme y recorte UV. El mensaje de estado blanco se mueve a y=392, antes del encabezado de respuesta, para no cubrir el paisaje ni los enemigos.

## Altura final del fondo
El fondo del carril se reduce a 90 px y se desplaza a y=590, manteniendo la base en y=680. Esto deja separacion visual respecto al boton de pista y conserva la escala uniforme.

## Mensaje sobre el paisaje
El mensaje de estado se presenta sobre el extremo derecho del recuadro: rectangulo (550,562,470,24), alineacion MiddleRight. Termina antes de la puerta (x=1028) y deja 4 unidades antes del paisaje (y=590). Su estilo se crea una sola vez como copia del estilo de cuerpo; no cambia las demas etiquetas. Para comprobarlo, inicia una partida y revisa los mensajes inicial y de golpe. Ajuste de interfaz conforme a pliego 4.11 y contrato 9.6.

## Mensaje debajo del paisaje
El mensaje de estado se coloca debajo del recuadro, alineado a la derecha: (650,682,502,24). Su borde derecho coincide con el del paisaje (x=1152); queda fuera del terreno, junto a la etiqueta de llegada y antes de los botones inferiores.
