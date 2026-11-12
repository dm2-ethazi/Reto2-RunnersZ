# 14 · Ejecutar todo en Linux

Comandos completos para instalar y arrancar cada apartado en **Ubuntu/Debian**. Si usas otra distribución, cambia el gestor de paquetes; el resto de comandos es igual.

> Verificado sobre la documentación del repositorio y sus requisitos reales (`pom.xml`, `build.gradle`, `ProjectVersion.txt`, `Dockerfile`). En la máquina donde se escribió esta wiki no había Linux para ejecutarlos, así que si algún paquete tiene otro nombre en tu distribución, usa el equivalente.

---

## 0. Resumen

| Apartado | Comando clave | Resultado |
|---|---|---|
| Servidor | `uv run --python 3.12 uvicorn runnersz.main:app --app-dir src --env-file .env` | API en `:8000` |
| Servidor (Docker) | `docker compose up -d --build` | API en `:8000` |
| Escritorio | `./mvnw -B package` + `java -jar target/runnersz-desktop-linux.jar` | Ventana JavaFX |
| Android | `./gradlew assembleRelease` + `adb install` | APK en el móvil |
| Unity | `Unity -batchmode -executeMethod RunnersZ.Editor.AndroidBuild.BuildBatch` | APK del juego |

---

## 1. Paquetes base

```bash
sudo apt update
sudo apt install -y git curl unzip ca-certificates fontconfig libgtk-3-0
```

Comprobación:

```bash
git --version && curl --version | head -1
```

✅ Deben responder sin error. `libgtk-3-0` y `fontconfig` son necesarios para abrir el cliente JavaFX.

---

## 2. JDK 21

El `pom.xml` compila con `release 21` y Gradle 8.9 no admite JDK 25. **No uses el JDK del sistema si es otro.**

Ubuntu 24.04 y Debian con repositorios actualizados:

```bash
sudo apt install -y openjdk-21-jdk
```

Si tu distribución no lo tiene, instala Temurin:

```bash
sudo apt install -y wget gpg apt-transport-https
wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo $VERSION_CODENAME) main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update && sudo apt install -y temurin-21-jdk
```

Ruta de `JAVA_HOME` según lo que hayas instalado:

| Instalación | `JAVA_HOME` |
|---|---|
| `openjdk-21-jdk` | `/usr/lib/jvm/java-21-openjdk-amd64` |
| `temurin-21-jdk` | `/usr/lib/jvm/temurin-21-jdk-amd64` |

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

✅ Debe decir `openjdk version "21.x"` (o `21.0.x` en Temurin).

Para que persista, añádelo a `~/.bashrc`:

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc
```

---

## 3. uv (servidor)

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
source "$HOME/.local/bin/env"
uv --version
```

✅ `uv 0.x.y`. Si `source` falla porque ese archivo no existe:

```bash
export PATH="$HOME/.local/bin:$PATH"
```

`uv` descarga Python 3.12 por su cuenta: no hace falta tocar el Python del sistema.

---

## 4. Docker (opcional, para desplegar)

```bash
sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
newgrp docker
docker run --rm hello-world
```

✅ El `hello-world` termina con `This message shows that your installation appears to be working correctly`.

> Sin `newgrp docker` (o sin cerrar y volver a abrir la sesión) verás `permission denied while trying to connect to the Docker daemon`.

---

## 5. Android SDK

Solo si vas a compilar la app Android o el APK de Unity.

1. Descarga **Command line tools only** desde <https://developer.android.com/studio#command-tools> (es un `.zip`).
2. Instálalo:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
unzip -q commandlinetools-linux-*.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
adb version
```

✅ `sdkmanager` acepta las licencias y `adb version` responde.

Persistencia en `~/.bashrc`:

```bash
cat >> ~/.bashrc <<'EOF'
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
EOF
source ~/.bashrc
```

Alternativa: instala Android Studio y usa su SDK Manager; el SDK queda en `~/Android/Sdk` por defecto.

---

## 6. Servidor con uv (recomendado)

```bash
cd ~/Reto2-RunnersZ/backend
uv sync --locked
cat > .env <<'EOF'
RZ_API_TOKEN=demo-runnersz-2026-4f7a
RZ_MAX_UPLOAD_MIB=100
EOF
unset RZ_API_TOKEN        # si la tuvieras exportada en la sesión
uv run --python 3.12 uvicorn runnersz.main:app --app-dir src --env-file .env --host 0.0.0.0 --port 8000
```

- `0.0.0.0` permite probar desde el móvil o desde otro PC de la red.
- Abre el puerto si tienes cortafuegos: `sudo ufw allow 8000/tcp`.
- Tu IP en la red: `hostname -I`.

Comprobaciones (en otra terminal):

```bash
curl -s http://127.0.0.1:8000/api/v1/health
curl -i -H "Authorization: Bearer demo-runnersz-2026-4f7a" http://127.0.0.1:8000/api/v1/auth/check
```

✅ `{"status":"ok","version":"2.0.0"}` y `HTTP/1.1 204 No Content`.

Pruebas y estilo:

```bash
cd ~/Reto2-RunnersZ/backend
uv run pytest
uv run ruff check .
uv run isort . --check-only
```

✅ `62 passed` y cobertura `97.71 %` (umbral 80 %). Para parar el servidor: **Ctrl+C**.

---

## 7. Servidor con Docker

```bash
cd ~/Reto2-RunnersZ/backend
cp .env.example .env
sed -i 's/^RZ_API_TOKEN=.*/RZ_API_TOKEN=demo-runnersz-2026-4f7a/' .env
docker compose up -d --build
docker compose ps
curl -s http://127.0.0.1:8000/api/v1/health
```

✅ El servicio `server` aparece `running` / `healthy`.

```bash
docker compose logs -f server     # ver registros (Ctrl+C para salir)
docker compose down               # parar y eliminar el contenedor
```

> `.env` es opcional: sin él se usa el token de reserva `change-me`. Para la demo define siempre el tuyo.

---

## 8. Cliente de escritorio

```bash
sudo apt install -y libgtk-3-0 fontconfig    # si no lo hiciste en el paso 1
cd ~/Reto2-RunnersZ/desktop
chmod +x mvnw
./mvnw clean verify
./mvnw -B package
java -jar target/runnersz-desktop-linux.jar
```

✅ `BUILD SUCCESS`, cobertura ≥ 80 % y se abre la ventana **RunnersZ**.

- El JAR de Linux se elige solo por el perfil de Maven; lleva dentro los nativos de JavaFX (10 archivos `.so`).
- Para generar el JAR de Windows desde Linux: `./mvnw -B package -Djavafx.platform=win` → `target/runnersz-desktop-win.jar` (ese sí necesita Windows para ejecutarse).
- En un servidor sin pantalla, ejecuta solo las pruebas con pantalla virtual:

```bash
sudo apt install -y xvfb
xvfb-run --auto-servernum ./mvnw verify
```

- Si sale `Unable to open DISPLAY`, no hay sesión gráfica: abre la aplicación en el escritorio Linux, no por SSH sin X11.

Configuración de la conexión: **Ajustes** → `http://localhost:8000` o `http://<IP_DEL_PC>:8000` (sin `/api/v1`) → token → **Probar** → **Guardar**.

---

## 9. App Android

```bash
cd ~/Reto2-RunnersZ/android
chmod +x gradlew
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew testDebugUnitTest assembleRelease
ls -lh app/build/outputs/apk/release/app-release.apk
```

✅ `BUILD SUCCESSFUL` y el APK en `app/build/outputs/apk/release/app-release.apk`.

Instalar en el móvil:

```bash
adb devices
adb install -r app/build/outputs/apk/release/app-release.apk
```

✅ `Success`. Si aparece `no permissions` o `unauthorized`:

```bash
sudo usermod -aG plugdev "$USER"
echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"' \
  | sudo tee /etc/udev/rules.d/51-android.rules
sudo udevadm control --reload-rules
sudo udevadm trigger
```

`18d1` es Google; cambia el `idVendor` según la marca (`lsusb` lo muestra). Cierra sesión y vuelve a entrar para aplicar el grupo `plugdev`, y acepta la huella RSA en el móvil.

> Recuerda que la app apunta a `https://runnersz-api.onrender.com` y solo habla HTTPS. Para usar tu servidor local hay que recompilarla: [12 · Personalizar el backend y el token](12-Personalizar-backend-y-token.md).

---

## 10. Minijuego Unity

Instala **Unity Hub** (AppImage o paquete `.deb` desde <https://unity.com/download>), entra con tu cuenta y añade el editor **6000.3.24f1** con el módulo **Android Build Support** (SDK/NDK/OpenJDK incluidos).

Ruta habitual del editor:

```bash
~/Unity/Hub/Editor/6000.3.24f1/Editor/Unity -version
```

✅ Imprime `6000.3.24f1`.

### APK de Android (funciona en Linux)

```bash
cd ~/Reto2-RunnersZ
~/Unity/Hub/Editor/6000.3.24f1/Editor/Unity \
  -batchmode -nographics -projectPath "$PWD/unity" \
  -executeMethod RunnersZ.Editor.AndroidBuild.BuildBatch \
  -logFile "$PWD/unity/android-build.log"
```

✅ Se crea `unity/Builds/Android/RunnersZ.apk`. Instálalo con `adb install -r unity/Builds/Android/RunnersZ.apk`.

### Jugador de Linux y de Windows

- **Linux**: abre el proyecto en el editor y usa *File → Build Settings → Linux* (x86_64). El repositorio no trae un método batch para Linux.
- **Windows**: `RunnersZ.Editor.SceneBuilder.BuildWindows` compila para Windows, y **Unity no puede generar un ejecutable de Windows desde Linux**. Genera el `.exe` en una máquina Windows con el comando de [05](05-Instalacion-unity.md).
- La escena se puede regenerar igual: menú **RunnersZ → Build Game Scene**.

### Pruebas

Abre *Window → General → Test Runner*, pestañas **EditMode** y **PlayMode**, y pulsa **Run All**.

✅ 25 EditMode y 2 PlayMode, 0 fallos.

> Los builds en batch necesitan licencia activa. Inicia sesión en Unity Hub antes de lanzarlos; si no, el log dirá que no hay licencia válida.

---

## 11. Demo rápida de extremo a extremo

Con el servidor arrancado en el paso 6:

```bash
cd /tmp
head -c 20971520 /dev/urandom > grande.bin
TOKEN=demo-runnersz-2026-4f7a

curl -s -X POST http://127.0.0.1:8000/api/v1/files/encrypt \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@grande.bin" -F "password=clave123" \
  -o grande.bin.enc -D cabeceras-enc.txt

curl -s -X POST http://127.0.0.1:8000/api/v1/files/decrypt \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@grande.bin.enc" -F "password=clave123" \
  -o grande.recuperado -D cabeceras-dec.txt

cmp grande.bin grande.recuperado && echo "idénticos"
grep -i x-integrity cabeceras-dec.txt
ls -lh grande.bin grande.bin.enc
```

✅ `idénticos`, `x-integrity: verified` y el `.enc` algo mayor que el original.

Texto, en una línea:

```bash
curl -s -X POST http://127.0.0.1:8000/api/v1/text/caesar \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"text":"Ataque al amanecer","shift":3,"mode":"encrypt"}'
```

✅ `{"result":"Dwdtxh do dpdqhfhu"}`.

---

## 12. Problemas típicos en Linux

| Síntoma | Causa | Solución |
|---|---|---|
| `java -version` no es 21 | JDK del sistema | Instala JDK 21 y exporta `JAVA_HOME` |
| `./mvnw: Permission denied` | Sin permiso de ejecución | `chmod +x mvnw` (y `chmod +x gradlew`) |
| `Unable to open DISPLAY` | Sin sesión gráfica | Ejecuta en el escritorio o usa `xvfb-run` para pruebas |
| `error while loading shared libraries: libgtk-3.so.0` | Falta GTK 3 | `sudo apt install libgtk-3-0` |
| `uv: command not found` | `PATH` sin `~/.local/bin` | `source "$HOME/.local/bin/env"` |
| `docker: permission denied` | Usuario fuera del grupo `docker` | `sudo usermod -aG docker $USER`, `newgrp docker` |
| `sdkmanager: command not found` | Falta en el `PATH` | Exporta `cmdline-tools/latest/bin` |
| `SDK location not found` | Sin `local.properties` ni `ANDROID_HOME` | `echo "sdk.dir=$ANDROID_HOME" > android/local.properties` |
| `adb devices` vacío o `unauthorized` | Reglas udev / huella RSA | Grupo `plugdev`, regla udev y aceptar en el móvil |
| `Address already in use` en 8000 | Otro proceso | `ss -tlnp | grep 8000` y ciérralo, o usa `--port 8001` |
| Unity: `No valid Unity Editor license` | Sin sesión en el Hub | Inicia sesión con Unity Hub |
| Unity: `Build failed` al compilar Windows | Objetivo no soportado en Linux | Compila Windows en Windows; en Linux usa Android o Linux |
| `xvfb-run: command not found` | Falta Xvfb | `sudo apt install xvfb` |

Más casos generales: [11 · Problemas frecuentes](11-Problemas-frecuentes.md).

---

## 13. WSL y contenedores

- **WSL2** sirve para el servidor (uv/Docker) y para compilar Android, pero **JavaFX necesita WSLg** (Windows 11) o un servidor X. Para la demo del escritorio, usa un Linux con escritorio.
- **Contenedor sin GUI**: ejecuta el servidor dentro y el cliente JavaFX fuera, apuntando a `http://<IP>:8000`.
- **Rutas**: el repositorio no debe vivir en `/mnt/c` si compilas Unity o Android: es mucho más lento y puede romper permisos. Clónalo en `~/`.
- **Permisos de línea**: `.gitattributes` fija LF para scripts; si clonaste en Windows y copiaste a Linux, revisa `file gradlew mvnw` y ejecuta `git config core.autocrlf input` antes de clonar de nuevo.

---

## 14. Siguiente paso

- Servidor y API: [02 · Instalación del servidor](02-Instalacion-servidor.md) · [06 · Uso de la API](06-Uso-servidor.md)
- Escritorio: [03](03-Instalacion-escritorio.md) · [07 · Uso](07-Uso-escritorio.md)
- Android: [04](04-Instalacion-android.md) · [08 · Uso](08-Uso-android.md)
- Unity: [05](05-Instalacion-unity.md) · [09 · Uso](09-Uso-unity.md)
- Demo completa: [10 · Verificación y demo](10-Verificacion-y-demo.md)

[Volver al índice](Home.md)
