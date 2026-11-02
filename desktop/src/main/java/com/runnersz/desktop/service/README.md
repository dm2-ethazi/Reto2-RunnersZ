# Servicios del cliente

## Uso
ConnectionService.check(config) comprueba GET /api/v1/auth/check. Debe invocarse en un Task, fuera del hilo JavaFX. ApiClient conserva las operaciones multipart existentes, pendientes de completar en el siguiente bloque.

## Funcionamiento
La comprobación exige HTTP 204, identifica HTTP 401 y rechaza el resto. Aplica límite de conexión de 10 segundos y de petición de 120 segundos. No registra token ni cuerpo de respuesta.

## Dependencias
HttpClient del JDK 21 y ServerConfig. No incluye cifrado local.

## Decisiones
Contrato 3.3, 5.4, 7.2 y 9.4: API autenticada, separación de responsabilidades y espera acotada.
