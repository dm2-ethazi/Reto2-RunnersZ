# 01 · Requisitos y comprobación del entorno

Antes de instalar nada, comprueba qué tienes. No necesitas todas las herramientas si solo vas a usar un apartado: la tabla indica qué hace falta para cada uno.

---

## 1. Resumen por apartado

| Herramienta | Servidor | Escritorio | Android | Unity |
|---|:---:|:---:|:---:|:---:|
| Git | ✅ | ✅ | ✅ | ✅ |
| [uv](https://docs.astral.sh/uv/) (gestiona Python 3.12) | ✅ | — | — | — |
| Docker Desktop / Engine *(opcional, para el despliegue)* | ✅ | — | — | — |
| JDK **21** (Temurin, Microsoft o el de Android Studio) | — | ✅ | ✅ | — |
| Android SDK **35** + `adb` | — | — | ✅ | ✅ (solo APK) |
| Unity Hub + editor **6000.3.24f1** + módulo Android | — | — | — | ✅ |
| Navegador | ✅ (Swagger) | — | — | — |

**No hacen falta** Maven, Gradle ni Python instalados a mano: el repositorio trae `mvnw`, el wrapper de Gradle y `uv` resuelve Python 3.12.

---

## 2. Instalar Git

- Windows: <https://git-scm.com/download/win>
- Linux: `sudo apt install git`
- macOS: `brew install git` (o las Command Line Tools)

Comprobación:

```bash
git --version
```

✅ Debe imprimir algo como `git version 2.x`.

Clona el repositorio y sitúate dentro:

```powershell
git clone https://github.com/dm2-ethazi/Reto2-RunnersZ.git
cd Reto2-RunnersZ
```

> Si ya tienes la carpeta del proyecto, basta con abrir una terminal **en la raíz del repositorio** (donde está `README.md`).

---

## 3. Servidor: uv (y Docker si quieres desplegar)

`uv` instala y ejecuta Python 3.12 sin tocar el Python del sistema. Es lo que usan el README y el CI.

Windows (PowerShell):

```powershell
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

Linux / macOS:

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

Comprobación:

```bash
uv --version
```

✅ Debe imprimir `uv 0.x.y`. Si la terminal no lo encuentra, cierra y vuelve a abrirla.

Docker es **opcional**: solo lo necesitas para la variante con `docker compose` de la página [02](02-Instalacion-servidor.md). Comprobación:

```bash
docker --version
docker compose version
```

> En esta máquina de desarrollo no hay motor de Docker, así que el despliegue con contenedores está marcado como **pendiente de probar** en `backend/README.md`. La variante con `uv` sí está verificada.

---

## 4. Escritorio y Android: JDK 21

El `pom.xml` compila con `release 21` y Gradle 8.9 **no admite JDK 25**. Instala un JDK 21 real:

- Temurin 21: <https://adoptium.net/temurin/releases/?version=21>
- O usa el que trae Android Studio: `C:\Program Files\Android\Android Studio\jbr`

Comprobación (la versión debe empezar por **21**):

```powershell
java -version
```

```text
openjdk version "21.0.x" ...
```

Si tu `java -version` dice 25 (como en la máquina donde se escribió esta wiki), apunta `JAVA_HOME` al JDK 21 antes de compilar:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version   # ahora debe decir 21
```

Linux/macOS:

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

> Este paso es la causa número uno de fallos de compilación en escritorio y Android. Ver [11 · Problemas frecuentes](11-Problemas-frecuentes.md).

---

## 5. Android: SDK 35 y adb

Instala **Android Studio** (<https://developer.android.com/studio>) y, en el asistente *SDK Manager*, marca:

- **Android SDK Platform 35** (el proyecto compila con `compileSdk 35`).
- **Android SDK Platform-Tools** (incluye `adb`).
- Un emulador si no vas a usar un móvil físico (por ejemplo Pixel 8a, API 35).

El proyecto localiza el SDK de una de estas dos formas:

1. Variable de entorno `ANDROID_HOME` (o `ANDROID_SDK_ROOT`).
2. Archivo `android/local.properties` (está en `.gitignore`, no se sube):

```properties
sdk.dir=C\:\\Users\\<TU_USUARIO>\\AppData\\Local\\Android\\Sdk
```

Comprobación:

```powershell
adb devices
```

✅ Debe listar al menos un dispositivo (`device`, no `unauthorized`) o ninguno si aún no has conectado el móvil.

Para el móvil: *Ajustes → Información del teléfono → toca 7 veces "Número de compilación"* y activa **Depuración USB**. Al conectarlo, acepta el aviso de huella RSA.

---

## 6. Unity: Hub, editor y módulo Android

1. Instala **Unity Hub**: <https://unity.com/download>.
2. En *Installs → Install Editor* elige la versión **6000.3.24f1** (Unity 6.3). El proyecto está fijado a esa versión en `unity/ProjectSettings/ProjectVersion.txt`; otra versión avisará y pedirá actualizar.
3. En los módulos del editor marca:
   - **Android Build Support**
   - **Android SDK & NDK Tools**
   - **OpenJDK**
4. Inicia sesión con una cuenta con licencia (Personal vale). Sin licencia, el editor no compila.

Comprobación rápida: Unity Hub muestra la versión instalada y, al abrir `unity/`, no aparece el aviso de "módulo Android no instalado" al ejecutar `RunnersZ/Configure Android`.

> Los módulos Android del editor son independientes del SDK de Android Studio: Unity usa los suyos.

---

## 7. Comprobación en bloque

Copia y pega según tu sistema. Lo que no uses puede fallar sin problema; lo importante es que **las herramientas de los apartados que vas a usar** respondan.

PowerShell:

```powershell
git --version
uv --version
java -version
docker --version
adb devices
Get-ChildItem "C:\Program Files\Unity\Hub\Editor"   # debe listar 6000.3.24f1
```

Linux/macOS:

```bash
git --version
uv --version
java -version
docker --version 2>/dev/null || true
adb devices 2>/dev/null || true
ls ~/Unity/Hub/Editor 2>/dev/null || true
```

---

## 8. Espacio y tiempo

| Concepto | Tamaño aproximado |
|---|---|
| Repositorio clonado | < 100 MB |
| Dependencias del backend (`.venv`) | ~150 MB |
| Dependencias Maven del escritorio (`~/.m2`) | ~120 MB |
| Android SDK 35 + platform-tools | ~2 GB |
| Unity 6.3 + módulo Android | ~10–15 GB |

Tiempos de primera compilación: backend 1 min, escritorio 2–4 min (descarga Maven y dependencias), Android 3–6 min, Unity 5–15 min (importa assets).

---

## 9. Siguiente paso

Con las herramientas listas, continúa por el apartado que vayas a usar:

- [02 · Instalación del servidor](02-Instalacion-servidor.md) — **empieza aquí**: sin API, el escritorio y Android no tienen nada que consultar.
- [03 · Instalación del escritorio](03-Instalacion-escritorio.md)
- [04 · Instalación de Android](04-Instalacion-android.md)
- [05 · Instalación de Unity](05-Instalacion-unity.md)

[Volver al índice](Home.md)
