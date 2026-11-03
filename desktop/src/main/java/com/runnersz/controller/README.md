# Controlador de escritorio

Ejecuta mvn javafx:run desde desktop y cambia el selector Español/Euskara/English. DesktopController enlaza texto, prompts y accesibilidad a claves de I18n. Las tareas de fondo conservan el contenido del formulario; los diálogos traducen sus botones explícitamente. Utiliza JavaFX 21 y los servicios existentes. Mantiene las responsabilidades y accesibilidad del contrato §5.4 y §9.3.

## Cambios consecutivos de idioma

La función `words` lee `languageCode` en cada cálculo del binding. Esta lectura
revalida la propiedad JavaFX y permite seguir recibiendo invalidaciones después
del primer cambio. El selector conserva el contenido del formulario y usa
`com.runnersz.utiles.I18n`; los catálogos permanecen en recursos `com/runnersz/i18n`.
La prueba `LanguageSwitchRegression` recorre los tres idiomas repetidamente sin
abrir ventanas. Consulta su README bajo `src/test/java/com/runnersz/controller`.
