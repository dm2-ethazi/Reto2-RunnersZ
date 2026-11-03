# Código JavaFX

Main y Launcher arrancan el cliente; I18n carga los catálogos y conserva el idioma. Ejecuta mvn javafx:run desde desktop. Los subpaquetes separan interfaz, servicios y modelo (contrato §5.4). I18n utiliza solo Java estándar y preferencias locales; el controlador enlaza los controles sin reconstruirlos.
