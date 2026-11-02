# Cliente desktop RunnersZ

Cliente JavaFX 21, organizado en modelo, servicios y controladores según contrato 5.4. Todas las operaciones criptográficas corresponden a la API Python.

## Uso
1. Instala JDK 21 y Maven 3.9.
2. Desde desktop ejecuta mvn verify.
3. El fat JAR se genera en target. Las ventanas funcionales y el empaquetado multiplataforma siguen pendientes; el código inicial no constituye una entrega completa.
4. El servidor requiere URL base HTTP/HTTPS y token; no incluyas secretos en Git.

## Funcionamiento
model/ServerConfig valida dirección y token y oculta este último en diagnósticos. service/ConnectionService verifica /api/v1/auth/check con espera acotada; debe ejecutarse desde un Task.
Main y ApiClient todavía son la base previa y se completarán en bloques posteriores.

## Dependencias
JDK 21, JavaFX 21.0.5, Maven, JUnit 5 y servidor RunnersZ. La comprobación de conexión usa java.net.http.HttpClient sin librerías adicionales.

## Decisiones
Contrato 3.3, 5.4 y 9.4: cliente HTTP común, capas y ajustes editables. Paleta firmada: azul Hogwarts, pergamino, oro, piedra y madera; se descarta la propuesta morada anterior. Inter y Open Sans se empaquetarán, como exige 9.2.
Consulta docs/REQUISITOS.md para requisitos y pendientes.

## Git
Se trabaja en feature/desktop. Antes de cada commit o push se muestra su contenido y se espera confirmación. Solo se incluyen rutas desktop; las PR las gestiona el usuario.
