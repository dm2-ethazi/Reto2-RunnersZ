# Decisiones · Android y Unity

Registro de decisiones del agente de Jairo (C.1-8): qué, por qué y alternativa descartada.
Cuando una decisión afecta al servidor o al escritorio de Alain, se avisa en su issue.

## DJ0 · Arranque del repositorio (Fase 0, 29-oct-2026)

- **Qué:** El remoto `dm2-ethazi/Reto2-RunnersZ` estaba **vacío** (todas las ramas `[gone]`).
  Ante esto, se arranca de cero: `main` con la estructura oficial vacía + README v2.0, y una rama
  de integración `prototype/runnersz` con la base de la Fase 0 (contrato, WORKFLOW, prompts,
  CODEOWNERS, plantillas, `board.py`, plan de pruebas). El Project de tablero es el **14**, no el 6.
- **Por qué:** El prompt asumía un remoto con `integration/reto2` publicada; la realidad no
  coincidía. Restaurar el histórico v1 completo habría contaminado `main` con código rechazado.
- **Alternativa descartada:** Restaurar `origin/integration/reto2` y crear `develop` desde su tip
  (lo preveía el prompt §4.5). Se descartó por decisión del humano: se prefiere un arranque limpio.
- **Efecto en I-01:** I-01 sigue asignado a **Alain** (su parte técnica: `uv`, hooks, CI, ruleset,
  `docs/api-contract.md`). La Fase 0 física la ejecuta J porque su máquina tenía el proyecto.

## DJ1 · HTTP en claro en Android 9+

- **Qué:** Permiso `INTERNET` + `res/xml/network_security_config.xml` con
  `cleartextTrafficPermitted="true"` (referenciado desde el manifest).
- **Por qué:** §3.4 asume HTTP en red local; la IP del PC cambia y no se puede enumerar.
- **Alternativa descartada:** HTTPS con certificado autofirmado (más segura, pero bloquea la demo
  por el manejo de confianza en el cliente). Mejora futura posible.

## DJ2 · UI Android sin Jetpack Compose

- **Qué:** Vistas + AppCompat + Material Components + Fragments. Destinos **Imagen** y **Orden**
  (navegación inferior) y **Ajustes** (barra superior; destino inicial si no hay configuración).
- **Por qué:** Continuidad con el código v1 (que ya usaba `navImage`/`navOrder`) y pocas
  dependencias (§9.1).
- **Alternativa descartada:** Jetpack Compose (más moderno, pero añade dependencias y curva de
  revisión innecesarias en la Discovery).

## DJ3 · Persistencia de dirección y token

- **Qué:** `SharedPreferences` privadas; el token en claro en el sandbox de la app
  (`allowBackup=false`); documentado en el manual.
- **Por qué:** §9.4 «guardado local»; sin librerías de seguridad extra.
- **Alternativa descartada:** Encapsulado con Android Keystore (más robusto, fuera del alcance).

## DJ4 · OkHttp como única librería de red

- **Qué:** OkHttp 4.12.x; JSON con `org.json` del SDK; `callTimeout` 120 s (§7.2),
  `connectTimeout` 10 s; sin reintentos automáticos (reintenta el usuario).
- **Por qué:** §3.1 y §7.2; una sola librería reduce la superficie de fallo.
- **Alternativa descartada:** Retrofit (añade anotaciones/procesamiento sin necesidad).

## DJ5 · Ficheros de hasta 20 MiB con memoria acotada

- **Qué:** Copiar el `Uri` a caché privada **por streaming**, subir con `RequestBody` desde
  fichero (nunca `readBytes()` entero), guardar la respuesta en `.part` y decodificar con
  `inSampleSize`; borrar la caché al salir de la pantalla (§4).
- **Por qué:** Un fichero de 20 MiB cargado entero en memoria haría `OutOfMemoryError`.
- **Alternativa descartada:** Lectura completa en un `ByteArray` (más simple, pero inviable).

## DJ6 · Resultado que no decodifica como imagen

- **Qué:** «El archivo descifrado no es una imagen válida» (sin cierre de la app).
- **Por qué:** Robustez §7.2.
- **Alternativa descartada:** Tratarlo como `DECRYPT_FAILED` (confundiría con clave incorrecta).

## DJ7 · Falta de `X-Integrity: verified` en un 200

- **Qué:** Se trata como error: «El servidor no confirmó la integridad» y no se muestra la imagen.
- **Por qué:** §3.3: solo se entrega el contenido si la huella es correcta.
- **Alternativa descartada:** Mostrar la imagen igualmente (rompería la garantía de integridad).

## DJ8 · Formato de la dirección

- **Qué:** Igual que el escritorio: exige `host:puerto` (puerto 1–65535), recorta espacios,
  acepta prefijo `http://`, rechaza `https://` con aviso («El servidor usa HTTP») y guarda
  `http://host:puerto`.
- **Por qué:** Un solo comportamiento en los dos clientes.
- **Alternativa descartada:** Formato libre en Android (generaría divergencias con el escritorio).

## DJ9 · Paleta y tipografías Android

- **Qué:** Paleta de §9 (Azul Hogwarts `#1B2A41`, Pergamino `#E8DCC6`, Oro `#C9A96A`, Piedra
  `#2E2E33`, Madera `#4A3520`) sustituyendo a la granate/oro v1; Inter y Open Sans en `res/font`
  (OFL); tamaños 22/16/14/18 sp; **oro nunca como texto sobre pergamino** (§9.3).
- **Por qué:** §9 y §9.2.
- **Alternativa descartada:** Mantener la paleta v1 (rechazada por el contrato).

## DJ10 · Cobertura Android

- **Qué:** JUnit4 + Robolectric + `mockwebserver:4.12.x` (solo `testImplementation`); JaCoCo ≥ 0,80
  sobre `data/` y `domain/` (y UI mientras se cumpla). **No se baja el umbral.**
- **Por qué:** Calidad §11.
- **Alternativa descartada:** Excluir la UI de forma permanente sin justificación.

## DJ11 · Pistas y puntuación entre rondas (Unity)

- **Qué:** Las pistas se repone **en cada ronda**; la puntuación **se acumula** entre rondas de una
  misma sesión; «Reintentar» repite la ronda con la puntuación de al empezarla; «Menú» cierra.
- **Por qué:** §10.2 dice «por partida».
- **Alternativa descartada:** Puntuación que se reinicia en cada ronda.

## DJ12 · Progresión de dificultad (Unity)

- **Qué:** `N(r) = N0 + 2·(r−1)`; `intervalo(r) = intervalo0 · 0,9^(r−1)` (compuesto); máximo 3
  rondas; vidas de la puerta a 3; misma dificultad.
- **Por qué:** §10.3 («cada ronda suma 2 enemigos y reduce un 10 % el intervalo»).
- **Alternativa descartada:** Intervalo con decremento simple (−10 % fijo del inicial).

## DJ13 · Respuesta vacía / fallo (Unity)

- **Qué:** Vacía ⇒ aviso, **no** cuenta como fallo ni avanza el enemigo. Incorrecta ⇒ avanza un
  salto ya (transición ≈ 0,25 s) y se **reinicia** el temporizador de salto. Sin penalización de
  puntos.
- **Por qué:** §10.2, §10.5.
- **Alternativa descartada:** Contar la respuesta vacía como fallo.

## DJ14 · Sonidos e iconos (Unity)

- **Qué:** Sonidos de victoria y derrota **distintos y sintetizados por código**
  (`AudioClip.Create`); iconos ✓ ✗ ♥ como sprites procedurales o de TextMeshPro con glifos
  comprobados; no se versionan binarios ni se dependen de licencias.
- **Por qué:** Regla C.1-12 (sin binarios en el repo).
- **Alternativa descartada:** Assets de sonido/iconos versionados.

## DJ15 · Orientación (Unity)

- **Qué:** Horizontal 1920×1080 (`Canvas Scaler` *Scale With Screen Size*, match 0,5), salvo que
  la Figura 6 del `.docx` muestre vertical (preguntar al humano si puede aportarla).
- **Por qué:** El enemigo recorre 8 saltos hacia la puerta.
- **Alternativa descartada:** Vertical (estrecha el pasillo de 8 saltos).

## DJ16 · Fuente monoespaciada

- **Qué:** Una OFL (p. ej. JetBrains Mono o Noto Sans Mono) para texto cifrado y resultados (§9.2).
- **Por qué:** §9.2 no fija cuál.
- **Alternativa descartada:** Fuente del sistema (no garantiza licencia ni monoespaciado uniforme).
