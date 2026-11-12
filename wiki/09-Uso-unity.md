# 09 · Uso del minijuego Unity

Juego de descifrado César: un enemigo avanza hacia la puerta mientras tú resuelves mensajes. **No usa la API**; funciona sin servidor.

---

## 1. Arrancar

- **Windows:** `unity\Builds\Windows\RunnersZ.exe` (comparte toda la carpeta).
- **Android:** instala `RunnersZ.apk` y abre **RunnersZ** en apaisado.
- **Editor:** abre `unity/` en Unity 6000.3.24f1, carga `Assets/Scenes/Game.unity` y pulsa **Play**.

✅ Aparece el menú con las tres dificultades.

---

## 2. Cómo se juega

1. Elige dificultad: **Fácil**, **Normal** o **Difícil**.
2. Aparece un mensaje cifrado con César (rotación A–Z).
3. Escribe el mensaje **descifrado** y pulsa **Validar** (o `Enter` en Windows).
4. Cada acierto suma puntos y derrota al enemigo de esa ronda.
5. Un fallo hace **avanzar al enemigo** hacia la puerta; si llega, pierdes una vida.
6. Resuelve tres mensajes para ganar.

Detalles del cifrado:

- Solo rota `A-Z`; **espacios y signos se conservan**.
- En **Fácil** se muestra el desplazamiento, así que puedes resolver sin calcularlo.
- La validación ignora diferencias de mayúsculas y espacios sobrantes (normalización del §10.5).

---

## 3. Reglas exactas

| Elemento | Valor |
|---|---|
| Rondas por partida | 3 |
| Vidas | 3 |
| Pistas por dificultad | 3 (Fácil) · 2 (Normal) · 1 (Difícil) |
| Puntos por acierto | 100 |
| Coste de cada pista | −50 (mínimo 0 de puntuación) |
| Progresión de enemigos | +2 enemigos por ronda y un 10 % menos de intervalo |
| Victoria | Resolver los tres mensajes |
| Derrota | Perder las tres vidas |

Reglas finas que conviene conocer:

- Las pistas **se reponen en cada ronda**; la puntuación **se acumula** durante la partida.
- **Reiniciar** restaura vidas, puntuación y ronda, y permite cambiar de dificultad.
- Una respuesta **vacía no cuenta como fallo** ni hace avanzar al enemigo: avisa y espera.
- Una respuesta **incorrecta** hace avanzar un salto al instante y reinicia el temporizador, sin penalización de puntos.

---

## 4. Controles

### Windows

| Acción | Cómo |
|---|---|
| Escribir la respuesta | Teclado, en el campo de texto |
| Validar | Botón **Validar** o `Enter` |
| Pista | Botón **Pista** |
| Reiniciar partida | Botón **Reiniciar** |
| Cambiar dificultad | Botón **Cambiar dificultad** (vuelve al menú) |
| Rueda de pares | Botón **Manuscrito** |

La interfaz y los enemigos son **formas básicas dibujadas por código** (`RunnerGame`), así que no depende de assets externos para funcionar.

### Android

La partida va **en apaisado a 60 fps** y todo se resuelve tocando:

| Acción | Cómo |
|---|---|
| Abrir la rueda de pares | Botón **Manuscrito** |
| Escribir | Toca el campo de respuesta: se abre el teclado del sistema |
| Validar / Pista / Reiniciar / Cambiar dificultad | Botones de **48 dp** |
| Cerrar el manuscrito | Barra atrás |
| Volver al menú | Barra atrás (otra vez) |
| Salir del juego | Barra atrás estando ya en el menú |

La interfaz se mide en **dp sobre el área segura**: llena la pantalla sin barras negras, respeta el recorte del notch y mantiene el campo de respuesta visible cuando aparece el teclado. Si el sistema no tiene Georgia ni Times New Roman, se usa una serif incluida en Unity.

---

## 5. Pantallas y estados

- **Menú**: título y tres dificultades; es el punto de partida y el retorno al cambiar de dificultad.
- **Partida**: mensaje cifrado, campo de respuesta, ronda, vidas, puntos, pistas disponibles y el pasillo con el enemigo avanzando.
- **Victoria**: mensaje de éxito con color, icono, texto y sonido propio.
- **Derrota**: mensaje de fallo con color, icono, texto y sonido distinto.

Los sonidos se sintetizan por código (`AudioClip.Create`), no son archivos: así el repositorio no guarda binarios.

---

## 6. Consejos para la demo

1. Empieza en **Fácil** para mostrar la mecánica con el desplazamiento visible.
2. Falla a propósito una vez para enseñar cómo avanza el enemigo.
3. Usa una pista para mostrar el descuento de 50 puntos.
4. Termina las tres rondas para llegar a la victoria.
5. En Windows, demuestra `Enter`; en Android, el botón **Manuscrito** y el teclado del sistema.

---

## 7. Limitaciones declaradas

- El APK **no se ha probado en un teléfono físico** por el equipo: teclado táctil y rendimiento real siguen sin verificar en hardware. Pruébalo antes de la entrega.
- No se ha medido cobertura de pruebas en Unity (sí hay 25 EditMode y 2 PlayMode).
- Los ejecutables viven en `unity/Builds/`, ignorado por Git: se regeneran con [05](05-Instalacion-unity.md).

---

## 8. Problemas típicos

| Síntoma | Solución |
|---|---|
| El juego no responde a la respuesta | Comprueba que el campo tiene foco; en móvil, toca el campo para abrir el teclado |
| Se ve en vertical en Android | Ejecuta `RunnersZ → Configure Android` y recompila |
| No hay sonido | Revisa el volumen del sistema; los clips se generan al arrancar |
| El enemigo no avanza | Es lo esperado tras una respuesta vacía |
| El texto se corta | Usa una pantalla compatible; la interfaz está probada en 9 perfiles |

Más casos: [11 · Problemas frecuentes](11-Problemas-frecuentes.md).

---

## Siguiente paso

- [10 · Verificación y demo](10-Verificacion-y-demo.md)
- [11 · Problemas frecuentes](11-Problemas-frecuentes.md)

[Volver al índice](Home.md)
