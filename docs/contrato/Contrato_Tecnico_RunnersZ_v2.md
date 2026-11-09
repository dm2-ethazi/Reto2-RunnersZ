**RunnersZ · RETO 2 · FASE DISCOVERY**

**Contrato técnico**

Servidor Python (Docker) · Cliente JavaFX · App Android · Minijuego Unity

| Dato | Valor |
| :---- | :---- |
| **Cliente** | Hogwarts |
| **Proveedor** | RunnersZ |
| **Integrantes** | Alain y Jairo |
| **Repositorio** | https\://github.com/dm2-ethazi/Reto2-RunnersZ |
| **Versión** | 2.0 · corregida tras la revisión del profesorado |
| **Fecha** | 28 de octubre de 2026 (versión 1.0: 26 de octubre de 2026\) |

## **Cambios de esta versión respecto a la propuesta rechazada**

| Área | Observación recibida | Qué hemos cambiado |
| :---- | :---- | :---- |
| DEIN | Todas las llamadas por la API, también Android; añadir tipografía; incluir ARIA y A11Y. | Servidor en Docker Compose y un único sistema HTTP para todos los clientes ([3](#bookmark=id.acr11hbedr7o)). Tipografía Inter y Open Sans ([9.2](#bookmark=id.xyah9v7aelkm)). Tabla de accesibilidad equivalente a ARIA/A11Y ([9.3](#bookmark=id.sglwok1x7ykm)). |
| ADAT | [Anexo I](#bookmark=id.nkx9zat9ltw2) de planificación; Discovery \+ 2 sprints; pruebas del Anexo VI.4; compatibilidad .enc/Android; 0 bytes; nonces y autenticación por bloque. | [Anexo I](#bookmark=id.nkx9zat9ltw2) completo; Discovery de 3 días y 2 sprints ([12](#bookmark=id.42woavx4rqqd)); plan de pruebas con resultados esperados ([7.3](#bookmark=id.qpcn9q88sw52)); formato .enc único tratado solo en el servidor ([5.3](#bookmark=id.buarf4vgkgj3)); 0 B rechazado ([7.1](#bookmark=id.pdasqhjjelaz)); esquema de nonces y AAD por bloque ([6.2](#bookmark=id.hi11wmcbzmb7)). |
| UNITY | Estructura del juego poco definida. | Nuevo apartado [10](#bookmark=id.6okhli8yxgpv): estados, vidas de la puerta, penalizaciones, dificultad y rondas, física 2D, validación de mensajes y pistas. |
| PYTH | Plan de pruebas concreto; cobertura que falle; uso de act; hooks; pyproject.toml; defender opciones no usadas; ejecución completa. | Apartado [11](#bookmark=id.wa94psko59v6) reescrito: pruebas concretas, cobertura bloqueante, act integrado, pre-commit y pre-push diferenciados, protección de rama, pyproject.toml, opciones descartadas y evidencias. |

# **Índice**

[**1\. Objetivo**](#bookmark=id.lgh49ck7n4ye)	

[**2\. Qué vamos a hacer**](#bookmark=id.z7oztp2ktatl)	

[**3\. Cómo lo haremos: arquitectura, versiones y API**](#bookmark=id.acr11hbedr7o)	

[3.1 Tecnologías y versiones](#bookmark=id.u2cdiiv04u0e)	  
[3.2 Despliegue del servidor con Docker Compose](#bookmark=id.oo32mu124qo3)	  
[3.3 API REST](#bookmark=id.6ko960xlhftv)	  
[3.4 Seguridad](#bookmark=id.c2yp956gcxar)	

[**4\. Almacenamiento**](#bookmark=id.fgi1g18cg7eb)	

[**5\. Funcionamiento propuesto**](#bookmark=id.z6wo7ttp2gx2)	

[5.1 Recorrido de la información](#bookmark=id.wqsiisy1b84q)	  
[5.2 Operaciones principales](#bookmark=id.pjr4i2a3a7hi)	  
[5.3 Formato del fichero .enc](#bookmark=id.buarf4vgkgj3)	  
[5.4 Estructura del código JavaFX](#bookmark=id.40j2jrtwvyjh)	

[**6\. Especificación de los algoritmos**](#bookmark=id.cdc62vc2uc3y)	

[6.1 Vigenère (servidor)](#bookmark=id.wwuuy8otb1)	  
[6.2 AES-256-GCM por bloques: nonces y autenticación](#bookmark=id.hi11wmcbzmb7)	  
[6.3 César (servidor y minijuego Unity)](#bookmark=id.kw4jnxxztsts)	  
[6.4 Contraseña AES](#bookmark=id.80ot2ss2oz4q)	

[**7\. Límites, incidencias y plan de pruebas**](#bookmark=id.ixwtfo4cm29q)	

[7.1 Tamaños y recursos](#bookmark=id.pdasqhjjelaz)	  
[7.2 Errores y protección del original](#bookmark=id.xnvjgnf4f3pm)	  
[7.3 Plan de pruebas (Anexo VI.4 del pliego)](#bookmark=id.qpcn9q88sw52)	

[**8\. Diagramas de casos de uso**](#bookmark=id.4msa33ghhno)	

[8.1 Cliente de escritorio](#bookmark=id.seet2hktidxv)	  
[8.2 App Android (rol Auror desplegado)](#bookmark=id.cj34o3dwtbei)	  
[8.3 Minijuego Unity](#bookmark=id.6itnppizey3z)	  
[8.4 Descripción resumida](#bookmark=id.s3ie3oxulvbh)	

[**9\. Diseño y accesibilidad**](#bookmark=id.2hcju9n8q4rs)	

[9.1 Criterios de diseño](#bookmark=id.thpysjns1u3v)	  
[9.2 Tipografía](#bookmark=id.xyah9v7aelkm)	  
[9.3 Accesibilidad (ARIA y A11Y)](#bookmark=id.sglwok1x7ykm)	  
[9.4 Cliente de escritorio JavaFX y ajustes de conexión](#bookmark=id.2fvpqy97dbiu)	  
[9.5 App Android](#bookmark=id.lt6awnjt9f)	  
[9.6 Minijuego Unity 6.3](#bookmark=id.8dziebjilnzb)	

[**10\. Especificación del minijuego Unity**](#bookmark=id.6okhli8yxgpv)	

[10.1 Estados y cómo los reconoce el jugador](#bookmark=id.ljcdav5qdtak)	  
[10.2 Reglas de la partida](#bookmark=id.2tshr853l6kw)	  
[10.3 Dificultad y progresión](#bookmark=id.2b8ay1algkxt)	  
[10.4 Uso de la física 2D](#bookmark=id.yqi9a3lhvm0t)	  
[10.5 Validación de los mensajes](#bookmark=id.f8gntqqyarx1)	  
[10.6 Ayuda al jugador](#bookmark=id.27kzlkfu5311)	

[**11\. Calidad, control de versiones y CI/CD**](#bookmark=id.wa94psko59v6)	

[11.1 Control de versiones y trazabilidad](#bookmark=id.4xn2n86n4uck)	  
[11.2 Configuración centralizada: pyproject.toml](#bookmark=id.4hxrpzspye5k)	  
[11.3 Pipeline de CI/CD en GitHub Actions](#bookmark=id.566j1r5hnxit)	  
[11.4 Hooks: pre-commit y pre-push](#bookmark=id.p4optfyscgh6)	  
[11.5 Cómo usamos act en nuestro proceso](#bookmark=id.f01lfrobd689)	  
[11.6 Protección de ramas en GitHub](#bookmark=id.89bbgquxbfnp)	  
[11.7 Opciones vistas en clase que no usamos y por qué](#bookmark=id.chu4zona043o)	  
[11.8 Evidencias de ejecución completa del pipeline](#bookmark=id.7ldly2ht04c6)	

[**12\. Planificación ágil**](#bookmark=id.42woavx4rqqd)	

[**13\. Entregables y documentación final**](#bookmark=id.21f9qjypm20i)	

[**Anexo I. Planificación**](#bookmark=id.nkx9zat9ltw2)	

[I.1 Enlace al tablero](#bookmark=id.ic3upgobzm6y)	  
[I.2 Capacidad disponible del equipo](#bookmark=id.exlndo8jv37c)	  
[I.3 Backlog (Issues, responsables, prioridades, criterios y estimaciones)](#bookmark=id.3rf8zzstd55l)	  
[I.4 Objetivos de los sprints](#bookmark=id.mcpfmo9z9zrf)	  
[I.5 Acuerdos de trabajo](#bookmark=id.tgo0x6f9s500)	  
[I.6 Riesgos previstos](#bookmark=id.wypro5xq8uva)	

# **1\. Objetivo**

La solución consistirá en un software capaz de gestionar información cifrada, procesar ficheros, aplicar mecanismos de protección de datos y ofrecer interfaces funcionales (escritorio, móvil y juego), integradas en una única solución coherente con un repositorio, un pipeline de CI/CD y documentación técnica.

# **2\. Qué vamos a hacer**

| Componente | Qué se entregará |
| :---- | :---- |
| **Servidor Python (API REST)** | Servicio desplegado con Docker Compose en un PC del equipo (red local / demo). Expone la API con autenticación por token: cifrado Vigenère, César, cifrado/descifrado AES-256-GCM de ficheros por bloques, y gestión de errores. Es el único sitio donde vive la lógica de cifrado. |
| **Cliente de escritorio JavaFX** | Interfaz gráfica para elegir fichero de entrada y de salida, cifrar y descifrar a través de la API (sube el fichero y descarga el resultado), verificar la recuperación y avisar con alertas. Ejecutable en Windows y Linux (Debian/Ubuntu). |
| **App Android nativa** | Solo consulta/descifrado. Abre o recibe .txt y .png.enc, los envía a la API y muestra el resultado: imágenes descifradas con clave AES y órdenes descifradas con desplazamiento César. Gestiona errores sin cerrarse. |
| **Minijuego Unity 6.3** | Minijuego 2D autónomo (APK Android, sin servidor, como exige el pliego): oleada de enemigos que avanzan hacia una puerta; el jugador descifra mensajes César. Estados: en curso, victoria y derrota, con física 2D (apartado [10](#bookmark=id.6okhli8yxgpv)). |
| **Calidad y automatización** | Tests unitarios con cobertura mínima del 80 % que hace fallar el pipeline, CI/CD en cada push y PR, hooks pre-commit y pre-push, ejecución local del pipeline con act y configuración centralizada en pyproject.toml. |
| **Documentación y demo** | Manual de instalación, manual de uso, plan de pruebas con evidencias, wiki y demostración funcional. |

# **3\. Cómo lo haremos: arquitectura, versiones y API**

Hay un único servidor Python que concentra la lógica de cifrado y de ficheros. Los tres clientes que necesitan cifrar o descifrar (JavaFX y Android) usan el mismo sistema: peticiones HTTP a la API, enviando el contenido (no rutas) mediante multipart/form-data para ficheros y JSON para textos. No hay un segundo sistema local en ningún cliente. El minijuego Unity es autónomo, como exige el pliego, y no usa la API.

*(figura omitida en esta version Markdown: Figura 1. Arquitectura: clientes, servidor en Docker Compose y minijuego autónomo.; ver el .docx)*

*Figura 1\. Arquitectura: clientes, servidor en Docker Compose y minijuego autónomo.*

## **3.1 Tecnologías y versiones**

Las versiones son la referencia del proyecto y se fijan en los ficheros de dependencias (pyproject.toml y uv.lock, pom.xml, build.gradle.kts) para que cualquier integrante replique el entorno sin configuraciones ocultas.

| Área | Tecnología | Versión de referencia | Uso |
| :---- | :---- | :---- | :---- |
| Servidor | Python | 3.12 | Lógica de Vigenère, César, AES y ficheros |
|  | FastAPI \+ Uvicorn \+ python-multipart | 0.115.x / 0.30.x / 0.0.x | API REST y recepción multipart |
|  | cryptography | 43.x | AES-256-GCM y PBKDF2-HMAC-SHA256 |
|  | uv | 0.4.x o superior | Gestión de dependencias y entorno (uv.lock) |
|  | pytest \+ pytest-cov, Ruff, isort | 8.x / 5.x, 0.6.x, 5.13.x | Tests, cobertura, estilo y orden de imports |
| Despliegue | Docker Engine \+ Docker Compose | Docker 24 o superior, Compose v2 | Servidor reproducible en el PC del equipo |
| Escritorio | JDK (Temurin) | 21 LTS | Compilación y ejecución; java.net.http.HttpClient |
|  | JavaFX | 21.0.x | Interfaz gráfica |
|  | Maven \+ Shade Plugin | 3.9.x / 3.5.x | Fat JAR |
|  | JUnit 5 \+ JaCoCo | 5.10.x / 0.8.12 | Tests y cobertura (umbral 80 % bloqueante) |
| Android | Kotlin \+ Android Gradle Plugin | 2.0.x / 8.5.x | App nativa |
|  | OkHttp | 4.12.x | Cliente HTTP (multipart y JSON) |
|  | minSdk / targetSdk | 26 (Android 8\) / 35 | Compatibilidad |
| Juego | Unity | 6.3 (6000.3.x, exigido por el pliego) | Minijuego 2D exportado como APK |
| Diseño | Inter y Open Sans | Última estable (licencia OFL) | Tipografía de las tres aplicaciones |
| Automatización | Git \+ GitHub \+ GitHub Actions | Git 2.4x | Repositorio y CI/CD |
|  | act, pre-commit | Última estable, 3.x / 4.x | Pipeline en local y hooks |

## **3.2 Despliegue del servidor con Docker Compose**

El servidor se ejecuta en un PC del equipo conectado a la misma red local que los móviles de la demostración. El despliegue se resume en tres pasos, que se documentan en el manual de instalación:

1. Copiar .env.example a .env y definir RZ\_API\_TOKEN (token de la API) y, opcionalmente, RZ\_MAX\_UPLOAD\_MIB.

2. Ejecutar «docker compose up \-d \--build» en la carpeta del servidor.

3. Comprobar «http\://IP-del-PC:8000/api/v1/health» y abrir el puerto 8000 en el cortafuegos del PC si hace falta.

El contenedor se basa en python:3.12-slim, instala las dependencias con «uv sync \--locked», se ejecuta con un usuario sin privilegios, usa un volumen temporal efímero para los ficheros .part y declara un healthcheck sobre /api/v1/health. El fichero .env nunca se sube al repositorio (solo .env.example).

## **3.3 API REST**

Todas las rutas cuelgan de /api/v1. Salvo /health, exigen la cabecera «Authorization: Bearer \<token\>». La contraseña AES viaja en un campo del formulario multipart, nunca en la URL.

| Método | Ruta | Entrada | Respuesta correcta |
| :---- | :---- | :---- | :---- |
| GET | /health | — | 200 {estado, versión}. No requiere token. |
| GET | /auth/check | Token | 204 si el token es válido; 401 si no. |
| POST | /files/encrypt | multipart: file, password | 200 application/octet-stream con el .enc. Cabeceras: nombre sugerido y tamaño. |
| POST | /files/decrypt | multipart: file (.enc), password | 200 con los bytes originales, solo si la autenticación de bloques y la huella SHA-256 son correctas. Cabecera X-Integrity: verified. |
| POST | /text/vigenere | JSON: text, key, mode (encrypt\|decrypt) | 200 {result} |
| POST | /text/caesar | JSON: text, shift (1–25), mode | 200 {result} |

**Errores.** Todo error devuelve JSON {code, message} con el estado HTTP adecuado y nunca contiene la contraseña ni el token:

| code | HTTP | Cuándo ocurre |
| :---- | :---- | :---- |
| UNAUTHORIZED | 401 | Falta el token o es incorrecto. |
| EMPTY\_FILE | 400 | El fichero subido ocupa 0 bytes («El archivo está vacío»). |
| EMPTY\_PASSWORD / INVALID\_KEY / INVALID\_SHIFT | 400 | Contraseña vacía; clave Vigenère vacía o con caracteres no válidos; desplazamiento fuera de 1–25 o no numérico. |
| FILE\_TOO\_LARGE | 413 | Supera el máximo configurado (por defecto 100 MiB; 20 MiB siempre debe funcionar). |
| DECRYPT\_FAILED | 422 | Contraseña incorrecta, fichero .enc dañado, truncado o con huella distinta («clave incorrecta o fichero dañado»). |
| INTERNAL\_ERROR | 500 | Fallo inesperado o disco lleno; se eliminan los temporales. |

## **3.4 Seguridad**

* **Autenticación:** HTTP con token de API (decisión del equipo). El token se compara en tiempo constante, se guarda en .env en el servidor y se configura en cada cliente junto a la dirección (apartado [9.4](#bookmark=id.2fvpqy97dbiu)).

* **Limitación conocida y asumida:** al usar HTTP sin TLS, el contenido y la contraseña AES viajan sin cifrar por la red local. Se acepta para la demostración en una red local controlada; la contraseña no se registra ni se almacena. Mejora futura documentada: HTTPS con un proxy inverso y certificado.

* **Repositorio público:** cuando se haga público para la wiki, ningún secreto estará en él (.env ignorado en .gitignore; token de ejemplo ficticio).

# **4\. Almacenamiento**

* El servidor **no conserva ficheros de usuario**: guarda el contenido recibido solo como temporal (.part) en un volumen efímero y lo elimina al terminar la petición, con éxito o con error.

* Los resultados los guarda el cliente donde elija el usuario; por defecto, junto al original con la extensión .enc (cifrado) o con el sufijo .recuperado (descifrado). El original no se modifica ni se mueve. Si la ruta de salida coincide con la de entrada o ya existe, el cliente rechaza la operación o pide confirmación explícita para otro destino.

* En Android, los ficheros se leen mediante el selector del sistema o «Compartir»; la imagen descifrada se muestra en pantalla y la copia temporal de la caché se borra al salir de la pantalla.

* Para recuperar el contenido solo hace falta el fichero .enc y la contraseña: la sal, las iteraciones, el prefijo del nonce, el tamaño de bloque y la versión del formato viajan en la cabecera del propio fichero, y la huella SHA-256 del original viaja dentro del contenido cifrado. No hay ficheros auxiliares y la contraseña nunca se almacena.

# **5\. Funcionamiento propuesto**

## **5.1 Recorrido de la información**

| N.º | Paso |
| :---- | :---- |
| 1 | El usuario selecciona en el cliente el fichero de entrada, la ubicación de salida y la contraseña, y pulsa Cifrar o Descifrar. |
| 2 | El cliente (JavaFX o Android) sube el contenido a la API por HTTP multipart con el token. Si el servidor no responde, el cliente avisa con una alerta y permite reintentar. |
| 3 | El servidor valida token, tamaño y contraseña; rechaza 0 B; lee el contenido por bloques de 1 MiB, aplica AES-256-GCM y escribe en un .part; solo si todo es correcto lo envía como respuesta. Cualquier error elimina el temporal. |
| 4 | Los ficheros .enc se mueven entre dispositivos por almacenamiento, compartir o red. Android los abre y los sube a la API para descifrarlos (no implementa el cifrado). |
| 5 | El cliente recibe el resultado (bytes y estado de la integridad) o un error JSON y lo comunica al usuario mediante alertas; en escritorio lo guarda en la ruta de salida. |

## **5.2 Operaciones principales**

* **Lectura:** en el cliente JavaFX, los flujos se abren y cierran con bloques try / catch / finally: el cierre de recursos se garantiza en finally y el usuario recibe una alerta sin que la aplicación se cierre. En el servidor se usa try / except / finally y with.

* **Cifrado:** clave de 256 bits derivada de la contraseña con PBKDF2-HMAC-SHA256; cada bloque se cifra con AES-GCM (apartado [6.2](#bookmark=id.hi11wmcbzmb7)). La huella SHA-256 del original se añade al final del contenido cifrado.

* **Descifrado:** se lee la cabecera y se autentica cada bloque antes de escribir. Al terminar, el servidor compara la huella guardada con la del contenido recuperado. Una contraseña incorrecta, un fichero alterado o truncado o una huella distinta provocan un error controlado y no se entrega nada.

* **Escritura:** fichero temporal y renombrado atómico (servidor y cliente de escritorio).

* **Colaboración entre componentes:** los clientes solo orquestan (interfaz, alertas, progreso, comunicación); toda la lógica crítica está en el servidor, lo que permite probarla de forma aislada.

## **5.3 Formato del fichero .enc**

Un único formato, generado y leído solo por el servidor. Esto resuelve la compatibilidad con Android: la app recibe las imágenes cifradas (.png.enc) producidas por la aplicación de escritorio y se limita a subirlas a /files/decrypt, de modo que no existen dos implementaciones que mantener sincronizadas.

| Campo | Bytes | Contenido |
| :---- | :---- | :---- |
| **Magic** | 4 | Identificador «RZEN». |
| **Versión** | 1 | 1\. |
| **KDF** | 1 | 1 \= PBKDF2-HMAC-SHA256. |
| **Iteraciones** | 4 | 600 000 (big-endian). |
| **Sal** | 16 | Aleatoria por fichero. |
| **Prefijo de nonce** | 4 | Aleatorio por fichero. |
| **Tamaño de bloque** | 4 | 1 MiB (1 048 576\) en claro por bloque. |
| **Bloques** | variable | Cada bloque: texto cifrado \+ etiqueta GCM de 16 B. Los datos en claro son el original seguido de su SHA-256 (32 B). |

## **5.4 Estructura del código JavaFX**

El código se organiza en capas (controladores, servicios y modelo). Los controladores solo gestionan la interfaz; los servicios encapsulan la comunicación con la API (HttpClient, multipart, token, gestión de errores JSON) y el acceso a ficheros; el modelo contiene los datos de cada operación. Las operaciones largas se ejecutan en un Task fuera del hilo de la interfaz.

# **6\. Especificación de los algoritmos**

## **6.1 Vigenère (servidor)**

Cifra y descifra mensajes con una clave, sobre texto escrito por el usuario o leído de un fichero .txt (UTF-8). Comportamiento documentado respecto a caracteres:

| Elemento | Comportamiento |
| :---- | :---- |
| **Alfabeto** | 26 letras (A–Z). Decisión del equipo: es el alfabeto más habitual en este tipo de ejercicios y evita ambigüedades con la Ñ. |
| **Mayúsculas / minúsculas** | Se conservan en el resultado. |
| **Espacios, dígitos, signos y símbolos** | No se cifran y se mantienen en su posición. No avanzan la posición de la clave. |
| **Ñ y letras con tilde o diéresis** | No pertenecen al alfabeto: se conservan sin cifrar y no avanzan la posición de la clave. Se prioriza que la recuperación sea exacta; mejora futura: normalizar tildes. |
| **Clave** | Solo letras A–Z; se normalizan a mayúsculas. Una clave vacía o con caracteres no válidos produce el error INVALID\_KEY. |
| **Ejemplo (clave «LUNA»)** | «Hola Mundo, ¿qué tal?» se cifra como «Siya Xoadz, ¿khé tlf?». |

## **6.2 AES-256-GCM por bloques: nonces y autenticación**

* **Clave por fichero:** la sal de 16 B es aleatoria en cada cifrado, así que cada fichero usa una clave distinta aunque la contraseña se repita.

* **Nonce de 96 bits por bloque:** prefijo aleatorio de 4 B (en la cabecera) seguido de un contador de bloque de 8 B (0, 1, 2…). El contador no se repite dentro del fichero y la clave es única por fichero, por lo que nunca se reutiliza un par (clave, nonce).

* **Autenticación de cada bloque:** cada bloque lleva su etiqueta GCM de 16 B y se verifica antes de escribir nada. Como datos asociados (AAD) se autentican la cabecera completa, el índice del bloque y un indicador de «último bloque». Así se detectan bloques alterados, reordenados, eliminados o un fichero truncado.

* **Integridad extremo a extremo:** además, la huella SHA-256 del original va dentro del contenido cifrado y se compara tras descifrar.

## **6.3 César (servidor y minijuego Unity)**

Desplazamiento entero de 1 a 25 sobre el mismo alfabeto y con las mismas reglas de caracteres que Vigenère (César equivale a Vigenère con clave de una letra). El desplazamiento 0, los negativos, los mayores de 25 y los valores no numéricos se rechazan con el aviso correspondiente (en Android, un aviso nativo a partir del error INVALID\_SHIFT; la app valida también en local antes de enviar). Ejemplo: con desplazamiento 3, «Ataque al amanecer» se cifra como «Dwdtxh do dpdqhfhu». En el minijuego Unity se usa el mismo cifrado solo como mecánica lúdica, implementado dentro del juego.

## **6.4 Contraseña AES**

El sistema rechaza únicamente la contraseña vacía, con el error EMPTY\_PASSWORD. Cualquier otra contraseña se acepta (texto UTF-8 de cualquier longitud) y se deriva con PBKDF2. Limitación conocida: una contraseña corta ofrece menos protección frente a ataques de fuerza bruta.

# **7\. Límites, incidencias y plan de pruebas**

## **7.1 Tamaños y recursos**

* **1 B:** se acepta y se cifra con normalidad.

* **0 B:** se **rechaza** con el error EMPTY\_FILE y el mensaje «El archivo está vacío». No se genera ningún fichero (decisión que sustituye a la anterior, que era contradictoria).

* **1 MiB y 20 MiB o más:** procesado por bloques de 1 MiB con memoria acotada; el servidor admite hasta 100 MiB por defecto (configurable). El cliente muestra el progreso y la operación se ejecuta fuera del hilo de la interfaz.

* **Limitaciones conocidas:** el coste inicial de PBKDF2 añade una pequeña espera fija; la velocidad depende de la red local; no hay reanudación de operaciones interrumpidas; la capacidad real se acreditará con las pruebas de la entrega final.

## **7.2 Errores y protección del original**

| Situación de error | Respuesta prevista | Protección |
| :---- | :---- | :---- |
| Fichero de entrada inexistente o sin permisos (cliente) | Alerta clara; no se llama a la API. | No se crea ningún fichero de salida. |
| Fichero de 0 B | EMPTY\_FILE: «El archivo está vacío». | No se genera salida. |
| Contraseña incorrecta o .enc corrupto/truncado | DECRYPT\_FAILED: «clave incorrecta o fichero dañado». | Se elimina el .part; no se entrega un descifrado parcial. |
| Contraseña vacía | EMPTY\_PASSWORD antes de empezar (también se valida en el cliente). | No se lee ni se escribe ningún fichero. |
| Huella de integridad que no coincide | DECRYPT\_FAILED tras el descifrado. | Se elimina el .part; no se entrega como válido. |
| Salida igual a la entrada o ya existente (cliente) | Rechazo o confirmación explícita para otro destino. | El original nunca se sobrescribe. |
| Token ausente o incorrecto | UNAUTHORIZED: «Token no válido. Revisa los ajustes». | No se procesa ningún contenido. |
| Servidor no disponible o dirección errónea | El cliente avisa y permite reintentar o ir a Ajustes (tiempo máximo de espera de 120 s). | Sin cambios en ficheros. |
| Disco lleno o fallo de escritura | INTERNAL\_ERROR / alerta en el cliente. | Se eliminan los temporales; el original queda intacto. |
| Android: clave, desplazamiento o archivo inválidos | Excepción capturada y aviso (Toast, Snackbar o diálogo). | La app no se cierra; recursos liberados en finally. |

## **7.3 Plan de pruebas (Anexo VI.4 del pliego)**

Las pruebas se automatizan con pytest (servidor) y JUnit (cliente de escritorio) y se repiten a mano en la demostración con los dispositivos reales. Se prepararán ficheros de prueba de 0 B, 1 B, 1 MiB, 20 MiB y 21 MiB y una imagen PNG.

| Grupo | Prueba | Resultado esperado |
| :---- | :---- | :---- |
| Recuperación de ficheros | Cifrar y descifrar de vuelta ficheros de texto, binario y PNG (1 B, 1 MiB, 20 MiB). | El descifrado es idéntico byte a byte al original (mismo SHA-256) y el cliente muestra «integridad verificada». |
|  | Descifrar con la contraseña correcta un .enc generado en escritorio desde Android. | La imagen se muestra correctamente en la app. |
| Tamaños | Fichero de 1 B. | Se cifra y se recupera. |
|  | Fichero de 20 MiB. | Se procesa por bloques con memoria acotada, con progreso en el cliente y sin bloquear la interfaz. |
|  | Fichero de 21 MiB con límite configurado a 20 MiB. | 413 FILE\_TOO\_LARGE y no se crea salida. |
| Errores | Fichero de 0 B. | 400 EMPTY\_FILE, «El archivo está vacío». |
|  | Contraseña incorrecta. | 422 DECRYPT\_FAILED; no queda .part ni salida. |
|  | Fichero .enc con un byte alterado, bloques reordenados, o truncado. | 422 DECRYPT\_FAILED en cada caso; no se entrega nada. |
|  | Contraseña vacía, token incorrecto, servidor apagado. | EMPTY\_PASSWORD, UNAUTHORIZED y alerta de conexión con opción de reintentar. |
| Vigenère | Mayúsculas y minúsculas; Ñ, tildes y diéresis; espacios y signos; ejemplo de [6.1](#bookmark=id.wwuuy8otb1). | Se conservan y la recuperación es exacta; «Siya Xoadz, ¿khé tlf?» con clave LUNA. |
|  | Clave vacía, con dígitos o con Ñ. | 400 INVALID\_KEY. |
| César | Desplazamientos 1 y 25; 0, 26, −3 y «abc». | 1 y 25 correctos; el resto, INVALID\_SHIFT; «Ataque al amanecer» \+3 \= «Dwdtxh do dpdqhfhu». |
| AES (formato) | Nonces distintos por bloque y prefijo/sal distintos en dos cifrados del mismo fichero. | Salidas distintas; ningún nonce repetido. |
| API | Rutas con y sin token; multipart sin campo password. | 401 y 400 con JSON {code, message}. |

# **8\. Diagramas de casos de uso**

Se presenta un diagrama por cliente. Las flechas discontinuas indican relaciones include (el caso incluye obligatoriamente al otro) y extend (el caso amplía al otro solo en ciertas condiciones, como un error).

## **8.1 Cliente de escritorio**

*(figura omitida en esta version Markdown: cu81.png; ver el .docx)*

## **8.2 App Android (rol Auror desplegado)**

*(figura omitida en esta version Markdown: cu82.png; ver el .docx)*

## **8.3 Minijuego Unity**

*(figura omitida en esta version Markdown: Figura 2. Casos de uso del minijuego (actualizados).; ver el .docx)*

*Figura 2\. Casos de uso del minijuego (actualizados).*

## **8.4 Descripción resumida**

| ID | Caso de uso | Actor | Descripción y resultado |
| :---- | :---- | :---- | :---- |
| CU-01 | Seleccionar fichero de entrada y salida | Usuario | Elige origen y destino; el sistema valida que no coincidan. |
| CU-02 | Cifrar fichero (AES) | Usuario | Sube el fichero a la API y guarda el .enc sin modificar el original. |
| CU-03 | Descifrar fichero (AES) | Usuario | Recupera exactamente los bytes del original con la contraseña correcta. |
| CU-04 | Cifrar / descifrar mensaje (Vigenère) | Usuario | Transforma texto escrito o cargado desde .txt con una clave, mediante la API. |
| CU-05 | Verificar integridad | Usuario | El servidor comprueba la huella SHA-256 y el cliente informa en pantalla. |
| CU-06 | Mostrar alerta de error | Usuario | Informa de cualquier fallo (incluidos 0 B, token, red) sin perder datos. |
| CU-07 | Abrir o recibir archivo | Auror | Importa .txt o .png.enc desde almacenamiento o compartir. |
| CU-08 | Descifrar imagen mágica (AES) | Auror | Pide la clave, la envía con el fichero a la API y muestra la imagen. |
| CU-09 | Descifrar orden de batalla (César) | Auror | Pide el desplazamiento y muestra el texto plano devuelto por la API. |
| CU-10 | Notificar error (móvil) | Auror | Toast, Snackbar o diálogo ante clave, desplazamiento, archivo, token o red no válidos. |
| CU-11 | Elegir dificultad e iniciar / reiniciar partida | Jugador | Selecciona Fácil/Normal/Difícil y arranca el estado «en curso». |
| CU-12 | Leer texto cifrado e introducir respuesta | Jugador | Muestra el texto y recoge la respuesta. |
| CU-13 | Validar respuesta | Jugador | Normaliza y compara; si es correcta, el enemigo es derrotado; si no, avanza un salto. |
| CU-14 | Avanzar enemigo y restar vida de la puerta | Motor de juego | Si el enemigo entra en el trigger de la puerta, ésta pierde 1 vida y el enemigo desaparece. |
| CU-15 | Usar pista | Jugador | Revela una palabra y resta puntos; número limitado por partida. |
| CU-16 | Mostrar estado victoria / derrota | Motor de juego | Muestra el panel final con color, icono, texto y sonido. |
| CU-17 | Configurar conexión con el servidor | Usuario / Auror | Edita la dirección (IP:puerto) y el token, prueba la conexión y los guarda (JavaFX y Android; no figura en los diagramas 8.1 y 8.2). |

# **9\. Diseño y accesibilidad**

Los bocetos muestran el aspecto y la distribución propuestos; son prototipos.

| Nombre del color | Código HEX | Uso en las aplicaciones |
| :---- | :---- | :---- |
| **Azul Hogwarts** | \#1B2A41 | Fondo principal, navegación y pantallas de inicio. |
| **Pergamino** | \#E8DCC6 | Fondos claros, tarjetas y zonas de lectura. |
| **Oro mágico** | \#C9A96A | Botones principales (texto azul), iconos y elementos destacados. |
| **Piedra del castillo** | \#2E2E33 | Paneles, bordes y superficies secundarias. |
| **Madera antigua** | \#4A3520 | Menús especiales y detalles decorativos. |

## **9.1 Criterios de diseño**

* Seguridad de uso: contraseña y token ocultos por defecto y mensajes de error comprensibles.

* Uso táctico (móvil): controles grandes, pocas pantallas y navegación inferior entre «Imagen» y «Orden», más acceso a Ajustes.

* Coherencia entre las tres aplicaciones: misma paleta, tipografía y tono de los mensajes.

## **9.2 Tipografía**

Se usan **Inter** (títulos, botones y cuerpo) y **Open Sans** (textos largos y manuales). Son fuentes libres (licencia SIL OFL), legibles y disponibles de forma uniforme en Windows, Linux y Android porque se **empaquetan con cada aplicación** (recursos del JAR, res/font en Android y fuente TextMeshPro en Unity), sin depender de las fuentes del sistema. Respaldo: Segoe UI / Roboto / sans-serif. El texto cifrado del juego y de los resultados usa una fuente monoespaciada para distinguir bien las letras.

| Uso | Fuente | Tamaño mínimo |
| :---- | :---- | :---- |
| **Títulos** | Inter Bold | 20 pt escritorio · 22 sp móvil |
| **Cuerpo y botones** | Inter Regular / Medium | 12 pt escritorio · 16 sp móvil |
| **Textos largos y ayudas** | Open Sans Regular | 12 pt escritorio · 14 sp móvil |
| **Texto cifrado** | Monoespaciada | 14 pt · 18 sp móvil |

## **9.3 Accesibilidad (ARIA y A11Y)**

ARIA es el estándar web de roles, nombres, estados y regiones en vivo. Nuestras aplicaciones no son web, así que aplicamos el mismo modelo con la API de accesibilidad de cada plataforma:

| Concepto ARIA / A11Y | JavaFX | Android | Unity |
| :---- | :---- | :---- | :---- |
| Rol y nombre accesible (role, aria-label) | setAccessibleRole y setAccessibleText en cada control y botón de icono | contentDescription en imágenes e iconos; labelFor en campos | Etiquetas de texto visibles en todos los botones |
| Descripción y ayuda (aria-describedby) | setAccessibleHelp en contraseña, token y dirección | Texto de ayuda (hint) y stateDescription | Texto de la pista junto al botón |
| Región en vivo (aria-live) | Alertas modales con foco automático | accessibilityLiveRegion en el mensaje de error o estado | Mensaje de estado y panel de resultado visibles y con sonido |
| Foco y orden de lectura | Orden de tabulación lógico, foco visible, atajos (mnemónicos) en Cifrar/Descifrar | Orden compatible con TalkBack; navegación inferior | Foco por defecto en el campo de respuesta; Intro valida |
| Tamaño de objetivo | Botones ≥ 32 px de alto | Zonas táctiles ≥ 48 dp | Botones ≥ 48 px de alto |
| Contraste (WCAG 2.1, 4.5:1) | Se cumple | Se cumple | Se cumple |
| No depender solo del color | Estado correcto/error con icono y texto | Ídem | Victoria y derrota con icono, texto y sonido |

* **Contraste:** el oro \#C9A96A solo se usa como fondo de botón con texto azul \#1B2A41 (≈ 6:1) o como texto sobre azul; **nunca como texto sobre pergamino**, donde no alcanza el contraste mínimo.

* Escalado de texto: las pantallas funcionan con el tamaño de fuente del sistema aumentado.

* Comprobación: revisión con TalkBack en Android y navegación solo con teclado en escritorio durante el Sprint 2\.

## **9.4 Cliente de escritorio JavaFX y ajustes de conexión**

*(figura omitida en esta version Markdown: Figura 3. Pestañas de ficheros (AES) y mensajes (Vigenère).; ver el .docx)*

*Figura 3\. Pestañas de ficheros (AES) y mensajes (Vigenère).*

Los dos clientes incluyen una pantalla de **ajustes de conexión**: un campo editable «Dirección del servidor» (IP:puerto), el token de API, un botón «Probar» que llama a /auth/check y el guardado local. La dirección no está fija en el código.

*(figura omitida en esta version Markdown: Figura 4. Ajustes de conexión en escritorio y en Android.; ver el .docx)*

*Figura 4\. Ajustes de conexión en escritorio y en Android.*

## **9.5 App Android**

*(figura omitida en esta version Markdown: Figura 5. Pantallas Imagen, Orden y gestión de errores (la pantalla de Ajustes está en la Figura 4).; ver el .docx)*

*Figura 5\. Pantallas Imagen, Orden y gestión de errores (la pantalla de Ajustes está en la Figura 4).*

## **9.6 Minijuego Unity 6.3**

*(figura omitida en esta version Markdown: Figura 6. Pantalla de juego con ronda, enemigos restantes, puntos, vidas de la puerta y botón de pista.; ver el .docx)*

*Figura 6\. Pantalla de juego con ronda, enemigos restantes, puntos, vidas de la puerta y botón de pista.*

# **10\. Especificación del minijuego Unity**

## **10.1 Estados y cómo los reconoce el jugador**

*(figura omitida en esta version Markdown: Figura 7. Estados del juego.; ver el .docx)*

*Figura 7\. Estados del juego.*

* **En curso:** HUD con ronda, enemigos derrotados / total, puntos y vidas de la puerta (♥ ♥ ♥).

* **Victoria:** panel a pantalla completa verde con icono ✓, texto «¡Victoria\!», puntuación y sonido. Botones «Siguiente ronda» y «Menú».

* **Derrota:** panel rojo con icono ✗, texto «Derrota: la puerta ha caído», puntuación y sonido distinto. Botones «Reintentar» y «Menú».

## **10.2 Reglas de la partida**

| Elemento | Regla |
| :---- | :---- |
| **Partida** | Cada partida es una oleada de N enemigos, cada uno con su frase cifrada. Aparece **un enemigo cada vez**. |
| **Victoria** | Descifrar las N frases. |
| **Derrota** | La puerta llega a 0 vidas. |
| **Vidas de la puerta** | La puerta tiene 3 vidas. Cuando un enemigo llega a ella, quita **1 vida y desaparece** (no es derrota inmediata); si quedan vidas, aparece el siguiente enemigo. |
| **Penalización por fallo** | La respuesta incorrecta hace que el enemigo avance **un salto** hacia la puerta (pierdes distancia). No resta puntos. |
| **Avance natural** | El enemigo avanza un salto cada cierto intervalo; la distancia hasta la puerta es de 8 saltos. |
| **Pistas** | Un botón de pista, limitado por partida; cada uso revela la siguiente palabra en claro y resta 50 puntos. |
| **Puntuación** | \+100 por frase descifrada; −50 por pista; nunca baja de 0\. |

## **10.3 Dificultad y progresión**

Menú inicial con selector **Fácil / Normal / Difícil**. Tras una victoria se puede pasar a la **siguiente ronda** (máximo 3\) con la misma dificultad: cada ronda suma 2 enemigos y reduce un 10 % el intervalo de avance; las vidas de la puerta se restauran a 3\.

| Dificultad | Enemigos (N) | Intervalo de salto | Pistas | Desplazamiento César |
| :---- | :---- | :---- | :---- | :---- |
| **Fácil** | 5 | 10 s | 3 | 1–5 y se muestra en pantalla |
| **Normal** | 8 | 7 s | 2 | 1–25, no se muestra |
| **Difícil** | 12 | 5 s | 1 | 1–25, no se muestra; frases más largas |

## **10.4 Uso de la física 2D**

* **Enemigo:** Rigidbody2D de tipo Kinematic (sin gravedad) y Collider2D; se mueve con MovePosition en FixedUpdate, con una transición corta (≈ 0,25 s) en cada salto.

* **Puerta:** BoxCollider2D con «Is Trigger» activado y un script que implementa OnTriggerEnter2D.

* **Colisión frente a trigger:** el enemigo no rebota ni empuja la puerta; es un trigger. Al entrar un objeto con la etiqueta «Enemy», la puerta pierde 1 vida, el enemigo se desactiva y, si las vidas son 0, se declara la derrota.

* La matriz de capas permite solo la interacción Enemy–Door.

## **10.5 Validación de los mensajes**

Antes de comparar, tanto la respuesta como la frase correcta se normalizan igual: se recortan los espacios (trim) y se colapsan los espacios repetidos; se pasa a mayúsculas; se eliminan tildes y diéresis (descomposición Unicode) y la **Ñ se trata como N**; y se ignoran los signos de puntuación. Una respuesta vacía no cuenta como fallo: se muestra un aviso. El banco de frases está escrito en mayúsculas y sin tildes ni Ñ para que el César no altere ningún carácter.

## **10.6 Ayuda al jugador**

No hay tutorial: el único apoyo es el botón de pista (limitado y con coste en puntos), decisión del equipo para evitar la frustración sin regalar la solución.

# **11\. Calidad, control de versiones y CI/CD**

## **11.1 Control de versiones y trazabilidad**

* **Ramas:** main (estable), develop (integración) y ramas feature/\* por tarea; se usarán al menos dos ramas de desarrollo además de la principal.

* **Commits:** al menos tres con cambios reales por integrante durante el proyecto (Discovery y dos sprints), con mensajes descriptivos.

* **Pull Requests:** al menos una integrada, con descripción breve y revisión o comentario del otro integrante. Las tareas se integran siempre por PR hacia develop.

## **11.2 Configuración centralizada: pyproject.toml**

El servidor se gestiona con **uv**: dependencias, entorno y ejecución de herramientas viven en pyproject.toml y uv.lock, y se usa siempre «uv run» (sin activar el entorno a mano).

\[project\]  
name \= "runnersz-server"  
version \= "1.0.0"  
requires-python \= "\>=3.12"  
dependencies \= \["fastapi", "uvicorn", "python-multipart", "cryptography"\]  
   
\[dependency-groups\]  
dev \= \["pytest\>=8.0", "pytest-cov\>=5.0", "ruff\>=0.6", "isort\>=5.13", "pre-commit\>=3.8", "httpx"\]  
   
\[tool.pytest.ini\_options\]  
testpaths \= \["tests"\]  
pythonpath \= \["."\]  
addopts \= "-q \--cov=app \--cov-report=term-missing \--cov-report=xml \--cov-fail-under=80"  
   
\[tool.ruff\]  
line-length \= 88  
target-version \= "py312"  
   
\[tool.isort\]  
profile \= "black"  
line\_length \= 88

Con «--cov-fail-under=80» en la configuración, **pytest falla (código de salida distinto de 0\) si la cobertura baja del 80 %**, tanto en local como en el hook de pre-push y en el job de CI. En el cliente de escritorio, JaCoCo usa la regla «check» con mínimo 0,80 sobre las líneas de las clases de dominio y servicios.

## **11.3 Pipeline de CI/CD en GitHub Actions**

| Job | Se ejecuta en | Pasos y validación |
| :---- | :---- | :---- |
| backend | Cada push y cada PR (a main y develop) | checkout → setup-uv (Python 3.12) → «uv sync \--locked» → «uv run ruff check .» → «uv run isort . \--check-only» → «uv run pytest» (cobertura ≥ 80 % o el job falla); sube el informe de cobertura como artefacto. |
| desktop | Cada push y cada PR | checkout → JDK 21 → «mvn verify» (tests JUnit, JaCoCo con umbral del 80 % que hace fallar el build) → fat JAR como artefacto. |
| docker | Cada PR | «docker compose build» y arranque con comprobación de /health para validar que el servidor despliega. |

El resultado queda visible en GitHub Actions. Los checks backend y desktop serán requisito obligatorio para integrar una PR ([11.6](#bookmark=id.89bbgquxbfnp)).

## **11.4 Hooks: pre-commit y pre-push**

Un hook de pre-commit **sólo puede bloquear un commit**, no un push. Por eso separamos las dos etapas:

| Etapa | Qué comprueba | Para qué |
| :---- | :---- | :---- |
| pre-commit (al hacer commit) | Espacios finales, fin de fichero, YAML/TOML/JSON, conflictos de merge, ficheros grandes, ruff, ruff-format, isort, «uv run pytest \-q» (tests rápidos) y «no-commit-to-branch» para main y develop. | Impedir commits con errores simples y commits directos en ramas protegidas. |
| pre-push (al hacer push) | «uv run pytest» con cobertura mínima del 80 % y un hook local que **bloquea el push a main y develop** leyendo la variable PRE\_COMMIT\_REMOTE\_BRANCH. | Impedir publicar código sin cobertura y empujar directamente a ramas protegidas. |

Instalación obligatoria para el equipo: «uv run pre-commit install \--hook-type pre-commit \--hook-type pre-push».

## **11.5 Cómo usamos act en nuestro proceso**

act ejecuta los workflows de GitHub Actions en local con Docker (imagen catthehacker/ubuntu:act-latest, fijada en .actrc). Se integra en el día a día así:

4. Al modificar un workflow: «act \--validate» (sin Docker) para detectar errores de sintaxis.

5. Antes de cada push de una rama feature: «act push \-j backend» (y «-j desktop» si se ha tocado Java) para ver ruff, isort, tests y cobertura en un contenedor Linux limpio.

6. Si falla, se corrige en local sin ensuciar el historial; después se hace commit y push.

7. En la PR manda siempre GitHub Actions: act es una simulación y puede diferir (permisos, concurrencia, imagen del runner).

La orden y su uso quedan documentados en el README, y el resultado de una ejecución completa se incluye en las evidencias ([11.8](#bookmark=id.7ldly2ht04c6)).

## **11.6 Protección de ramas en GitHub**

El repositorio dm2-ethazi/Reto2-RunnersZ será privado dentro de la organización mientras se desarrolla (somos administradores) y se hará público para la wiki. Configuraremos reglas de protección (ruleset) para main y develop: PR obligatoria, checks backend y desktop en verde, al menos un comentario/revisión del otro integrante y prohibición de push directo y force-push. Dado que la aplicación de estas reglas en un repositorio privado de una organización depende del plan de GitHub, mientras sea privado la barrera efectiva son los hooks locales de 11.4 y, al hacerse público, la protección de GitHub es obligatoria y se verifica con una captura.

## **11.7 Opciones vistas en clase que no usamos y por qué**

| Opción | Por qué no la usamos |
| :---- | :---- |
| pip con requirements.txt y entorno manual | Usamos uv: crea el entorno, fija versiones en uv.lock y «uv sync \--locked» hace fallar el CI si el lock no está al día; así local y CI usan las mismas versiones. |
| Script propio de bloqueo de rama (shell) | Preferimos el hook estándar «no-commit-to-branch» y un hook de pre-push mínimo; evitan mantener scripts distintos en Windows y Linux. |
| Despliegue continuo (Continuous Deployment) | El proyecto es una demo local; el despliegue se hace a mano con Docker Compose. Hacemos integración continua y generamos artefactos (fat JAR, APK) pero no desplegamos automáticamente. |
| Commits directos en main o develop | Se prohíben en favor del flujo feature → PR → develop. |

## **11.8 Evidencias de ejecución completa del pipeline**

En la entrega final se aportarán capturas o registros de: (1) «uv run pre-commit run \--all-files»; (2) «act push» terminando en «Job succeeded», con ruff, isort, tests y cobertura; (3) la ejecución en GitHub Actions con los checks en verde en una PR; (4) un fallo provocado a propósito (por ejemplo cobertura por debajo del 80 %) que bloquea el job; y (5) el informe de cobertura publicado como artefacto.

# **12\. Planificación ágil**

La guía establece una fase de Discovery y **dos sprints** de desarrollo. El proyecto se entrega el **12 de noviembre de 2026**. Hay reuniones diarias de 5 minutos y revisión al final de cada sprint. El tablero se gestiona con GitHub Projects (backlog, en curso, en revisión, hecho). Una tarea se considera hecha cuando está integrada mediante PR y sus tests están en verde en el pipeline.

| Fase | Fechas | Objetivo | Entregables |
| :---- | :---- | :---- | :---- |
| **Discovery (3 días)** | 28–30 de octubre | Cerrar el contrato corregido y las decisiones técnicas. | Contrato v2.0, contrato de la API (rutas y errores), repositorio con ramas, tablero de GitHub Projects con el backlog, entorno Docker y CI base. |
| **Sprint 1** | 2–6 de noviembre | Servidor funcional y esqueletos de los clientes. | API con token, Vigenère y AES-GCM por bloques en Docker; Android con ajustes y descifrado de imagen; Unity con enemigo, puerta, física y frases. |
| **Sprint 2** | 9–12 de noviembre | Integración, calidad y cierre. | Cliente JavaFX completo, César en Android, juego completo (dificultad, rondas, pistas), cobertura ≥ 80 %, pruebas del Anexo VI.4, APK y fat JAR, manuales y demo. |

El detalle de tareas, responsables, horas, objetivos, acuerdos de trabajo y riesgos está en el [**Anexo I**](#bookmark=id.nkx9zat9ltw2).

# **13\. Entregables y documentación final**

* Repositorio de código fuente con historial de commits, ramas y Pull Requests.

* Servidor Python con Dockerfile, docker-compose.yml, .env.example y README para levantarlo en Windows y Linux.

* Aplicación de escritorio: fat JAR (misma aplicación en Linux Debian/Ubuntu con entorno gráfico y en Windows) que se conecta al servidor.

* Aplicación Android (APK) y minijuego Unity (APK Android).

* Manual de instalación y manual de uso de cada aplicación; wiki del repositorio.

* Plan de pruebas con resultados y evidencias del pipeline (apartados [7.3](#bookmark=id.qpcn9q88sw52) y [11.8](#bookmark=id.7ldly2ht04c6)).

* [Anexo I](#bookmark=id.nkx9zat9ltw2) de planificación con el enlace al tablero de GitHub Projects.

| Firma Cliente | RunnersZ |
| :---- | :---- |
| Firma y fecha: \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ | Alain y Jairo *(figura omitida en esta version Markdown: logo.png; ver el .docx)* |

# **Anexo I. Planificación**

## **I.1 Enlace al tablero**

**Tablero de GitHub Projects: [https\://github.com/orgs/dm2-ethazi/projects/6](https://github.com/orgs/dm2-ethazi/projects/6)**

## **I.2 Capacidad disponible del equipo**

| Integrante | Rol principal | Discovery | Sprint 1 | Sprint 2 |
| :---- | :---- | :---- | :---- | :---- |
| **Alain** | Servidor, JavaFX y CI | Fuera del cómputo | 20 h | 20 h |
| **Jairo** | Android y Unity | Fuera del cómputo | 20 h | 20 h |
| **Total** |  |  | **40 h** | **40 h** |

Capacidad total estimada: 80 horas-persona en los dos sprints (unas 20 h por integrante y sprint). El Discovery (3 días) se dedica a definición y preparación, y no se cuenta.

## **I.3 Backlog (Issues, responsables, prioridades, criterios y estimaciones)**

Prioridad: A \= alta, M \= media, B \= baja. Horas en horas-persona.

| Issue | Título | Resp. | Prio. | Criterios de aceptación | Horas |
| :---- | :---- | :---- | :---- | :---- | :---- |
| Sprint 1 |  |  |  |  |  |
| \#1 | Repositorio, ramas y CI base | Alain | A | Ramas main/develop; workflow que ejecuta ruff y pytest; hooks instalados. | 3 |
| \#2 | Servidor Docker \+ API con token | Alain | A | «docker compose up» levanta la API; /health y /auth/check responden; 401 sin token. | 4 |
| \#3 | Vigenère y César en servidor | Alain | A | Ejemplos de [6.1](#bookmark=id.wwuuy8otb1) y [6.3](#bookmark=id.kw4jnxxztsts) correctos; INVALID\_KEY e INVALID\_SHIFT con pruebas. | 3 |
| \#4 | AES-GCM por bloques y formato .enc | Alain | A | Nonces por bloque, AAD y huella según [6.2](#bookmark=id.hi11wmcbzmb7); recuperación idéntica; detecta alteración y truncado. | 6 |
| \#5 | Endpoints multipart de cifrado y descifrado | Alain | A | Cifrar y descifrar por HTTP con 1 B, 1 MiB y 20 MiB; EMPTY\_FILE y FILE\_TOO\_LARGE. | 4 |
| \#6 | Android: esqueleto, ajustes de conexión y OkHttp | Jairo | A | Campo IP:puerto y token guardados; «Probar» llama a /auth/check; navegación Imagen/Orden. | 5 |
| \#7 | Android: descifrar imagen por la API | Jairo | A | Abre .png.enc, envía clave y fichero, muestra la imagen; error de clave sin cerrar la app. | 5 |
| \#8 | Unity: escena, enemigo, puerta y física 2D | Jairo | A | Enemigo kinematic avanza por saltos; trigger de la puerta resta 1 vida y elimina al enemigo. | 6 |
| \#9 | Unity: frases, César y validación | Jairo | A | Normalización según [10.5](#bookmark=id.f8gntqqyarx1); banco de frases sin tildes; acierto derrota al enemigo. | 4 |
| Sprint 2 |  |  |  |  |  |
| \#10 | JavaFX: cliente, ajustes y HttpClient | Alain | A | Cifra y descifra ficheros por la API con progreso fuera del hilo de la UI; try/catch/finally. | 7 |
| \#11 | JavaFX: Vigenère, integridad y alertas | Alain | A | Pestaña Vigenère con carga/guardado .txt; alerta «integridad verificada»; accesibilidad según [9.3](#bookmark=id.sglwok1x7ykm). | 4 |
| \#12 | Tests, cobertura 80 %, pre-push y act | Alain | A | Pipeline falla bajo 80 %; hooks pre-commit y pre-push; «act push» en verde. | 4 |
| \#13 | Pruebas del Anexo VI.4 con evidencias | Alain | A | Tabla [7.3](#bookmark=id.qpcn9q88sw52) ejecutada con resultados esperados y capturas. | 3 |
| \#14 | Manual de instalación, README y demo (servidor y escritorio) | Alain | M | Pasos reproducibles en Windows y Linux; protección de ramas verificada. | 2 |
| \#15 | Android: César, errores y accesibilidad | Jairo | A | Orden descifrada por /text/caesar; Toast/Snackbar; TalkBack y 48 dp. | 4 |
| \#16 | Unity: dificultad, rondas, pistas y paneles final | Jairo | A | Tabla [10.3](#bookmark=id.2b8ay1algkxt) aplicada; victoria/derrota con color, icono, texto y sonido. | 6 |
| \#17 | APK de Android y de Unity | Jairo | A | APK instalables y probados en un móvil real conectado al servidor. | 3 |
| \#18 | Manuales de uso de Android y Unity | Jairo | M | Manuales con capturas. | 3 |
| \#19 | Pruebas en dispositivos y ensayo de la demo | Jairo | M | Demo completa de extremo a extremo sin fallos. | 4 |
| **Total** |  |  |  | **Sprint 1: 40 h · Sprint 2: 40 h** | **80** |

## **I.4 Objetivos de los sprints**

| Sprint | Objetivo |
| :---- | :---- |
| **Sprint 1 (2–6 nov.)** | Tener el servidor desplegable con Docker, con Vigenère, César y AES-GCM por bloques probados, y los primeros flujos de Android y Unity funcionando contra la API o de forma autónoma. |
| **Sprint 2 (9–12 nov.)** | Completar JavaFX, César en Android y el juego; alcanzar cobertura del 80 % con el pipeline en verde; ejecutar las pruebas del Anexo VI.4; generar APK y fat JAR; manuales y demostración. |

## **I.5 Acuerdos de trabajo**

* Reunión diaria de 5 minutos y revisión al final de cada sprint.

* Nadie hace commit ni push directo en main o develop: se trabaja en feature/\* y se integra por PR.

* Una PR se integra cuando el pipeline está en verde y el otro integrante la ha revisado o comentado.

* Cada Issue tiene responsable, criterios de aceptación y estimación en el tablero; se mueve entre columnas según su estado.

* Antes de cada push se ejecutan los hooks y, si se toca el pipeline, act.

* Mensajes de commit descriptivos y secretos (token, contraseñas) nunca en el repositorio.

## **I.6 Riesgos previstos**

| Riesgo | Impacto | Mitigación |
| :---- | :---- | :---- |
| Plazo corto (dos sprints) y un integrante por plataforma | Alto | Priorizar Issues de prioridad alta; hacer demo funcional mínima al final del Sprint 1\. |
| El móvil no alcanza el servidor (red, cortafuegos, IP cambiante) | Alto | Campo editable de dirección, botón «Probar», puerto 8000 abierto y ensayo en la red de la demo. |
| Tráfico HTTP sin cifrar | Medio | Token, red local controlada, contraseña no registrada; HTTPS como mejora futura. |
| Ficheros grandes (20 MiB) lentos o con fallos de memoria | Medio | Procesado por bloques y pruebas tempranas en el Sprint 1\. |
| La protección de rama no se aplica en repositorio privado | Medio | Hooks pre-commit y pre-push; activar la protección al hacer público el repositorio. |
| Diferencias entre act y GitHub Actions | Bajo | La validación final es siempre GitHub Actions. |
| Falta de experiencia con Unity o Android | Medio | Reservar horas de aprendizaje en el Discovery y simplificar el alcance si hace falta. |
| Ausencia o enfermedad de un integrante | Alto | Documentar cada tarea en el tablero y revisar el código del otro por PR. |
