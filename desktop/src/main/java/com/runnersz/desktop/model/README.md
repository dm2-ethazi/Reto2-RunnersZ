# Modelo de conexión

## Uso
Crea ServerConfig con la URL base HTTP/HTTPS y el token; utiliza endpoint("/api/v1/auth/check") para construir una ruta.

## Funcionamiento
Valida servidor, puerto y ausencia de credenciales, rutas, consultas o fragmentos. Normaliza la barra final y oculta el token en toString.

## Dependencias
Solo java.net.URI y clases del JDK 21.

## Decisiones
Contrato 3.3 y 9.4: una API común y dirección editable. Contrato 3.4: no mostrar secretos. La validación evita enviar Authorization a una URL accidentalmente mal configurada.
