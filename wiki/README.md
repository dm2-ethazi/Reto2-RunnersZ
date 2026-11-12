# Wiki de RunnersZ · cómo se usa y cómo se publica

Esta carpeta contiene la **wiki del proyecto** en Markdown. Está pensada para que cualquier persona (cliente, equipo nuevo, evaluador) pueda instalar, arrancar y usar cada apartado siguiendo los pasos en orden.

## Contenido

| Archivo | Página |
|---|---|
| `Home.md` | Portada: qué es el proyecto, ruta recomendada e índice |
| `01-Requisitos.md` | Herramientas, versiones y comprobaciones |
| `02-Instalacion-servidor.md` | API con `uv` y con Docker |
| `03-Instalacion-escritorio.md` | Cliente JavaFX: compilar, JAR y configuración |
| `04-Instalacion-android.md` | CipherSprint: SDK, APK e instalación |
| `05-Instalacion-unity.md` | Minijuego: escena, builds y pruebas |
| `06-Uso-servidor.md` | Rutas, ejemplos y errores de la API |
| `07-Uso-escritorio.md` | Uso de las tres pestañas |
| `08-Uso-android.md` | Uso de las tres pestañas de la app |
| `09-Uso-unity.md` | Reglas, controles y puntuación del juego |
| `10-Verificacion-y-demo.md` | Checklist de extremo a extremo y guion de demo |
| `11-Problemas-frecuentes.md` | Síntoma → causa → solución |
| `12-Personalizar-backend-y-token.md` | Rotar el token y recompilar Android |
| `13-Flujo-de-trabajo-y-CI.md` | Ramas, commits, hooks y GitHub Actions |

## Cómo leerla

- **Si vienes de cero:** `Home.md` → `01` → `02` → el apartado que te interese.
- **Si solo quieres la demo:** `Home.md` §2 y `10-Verificacion-y-demo.md`.
- **Si algo falla:** `11-Problemas-frecuentes.md`.

Los enlaces son relativos y terminan en `.md`, así que funcionan en GitHub, en VS Code y en Obsidian sin plugins. No sustituyen a los manuales oficiales de cada carpeta (`README.md` de `backend/`, `desktop/`, `android/` y `unity/`): los resumen y los ordenan.

## Publicar en la pestaña *Wiki* de GitHub

La wiki de GitHub es un repositorio aparte (`Reto2-RunnersZ.wiki.git`). Publicarla es una acción visible: necesita la autorización del responsable del repositorio.

1. Activa la wiki en **Settings → Features → Wikis** (una sola vez).
2. Clona el repositorio de la wiki:

```bash
git clone https://github.com/dm2-ethazi/Reto2-RunnersZ.wiki.git
```

3. Copia el contenido de esta carpeta, incluidas las capturas:

```powershell
Copy-Item wiki\*.md, wiki\img ..\Reto2-RunnersZ.wiki\ -Recurse -Force
```

```bash
cp -r wiki/*.md wiki/img ../Reto2-RunnersZ.wiki/
```

4. **Ajusta los enlaces**: GitHub Wiki resuelve los enlaces internos **sin** extensión `.md`.

```powershell
cd ..\Reto2-RunnersZ.wiki
Get-ChildItem *.md | ForEach-Object {
  (Get-Content $_ -Raw) -replace '\]\(([^)]+)\.md\)', ']($1)' | Set-Content $_ -NoNewline -Encoding utf8
}
```

```bash
sed -i -E 's/\]\(([^)]+)\.md\)/](\1)/g' *.md
```

5. Publica:

```bash
git add -A
git commit -m "docs: publica la wiki del proyecto"
git push
```

`Home.md` es la portada de la wiki de GitHub por convención. Si prefieres mantener los enlaces con `.md` (por ejemplo para leerla desde el repositorio), no hagas el paso 4 y publica solo si aceptas que los enlaces internos no resuelvan allí.

## Mantenerla

- Cuando cambie un comando, una ruta o un requisito, actualiza la página afectada **en el mismo commit** del cambio.
- No incluyas tokens, contraseñas, IP privadas reales ni capturas con credenciales.
- Si una página y el manual de su carpeta se contradicen, manda el manual; corrige la wiki.
