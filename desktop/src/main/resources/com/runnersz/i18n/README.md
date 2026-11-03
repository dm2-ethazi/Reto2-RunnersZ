# Catálogos de idioma

Para cambiar un texto, edita la misma clave en messages_es.properties, messages_eu.properties y messages_en.properties. Mantén messages.properties igual al catálogo español de respaldo. Usa UTF-8 y conserva parámetros {0}, {1} y escapes de salto de línea. Ejecuta mvn clean verify desde desktop para empaquetarlos. ResourceBundle y MessageFormat pertenecen al JDK: no hay dependencias nuevas. Las claves estables permiten volver de un idioma a otro sin perder datos y cubren etiquetas, ayudas y errores conforme a la petición y a la accesibilidad del contrato §9.3.
