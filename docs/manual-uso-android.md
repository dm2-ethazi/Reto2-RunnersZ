# Manual de Android v2

1. Arranca el servidor Docker y confirma GET /api/v1/health.
2. En Ajustes introduce URL, puerto y token y pulsa Probar.
3. Comparte un fichero PNG.ENCRYPTED o selecciónalo desde el sistema.
4. Introduce la contraseña y pulsa Descifrar. La app envía el fichero a /api/v1/files/decrypt y muestra la imagen recibida.
5. Para órdenes, abre un TXT, introduce un desplazamiento entero de 1 a 25 y valida. La petición usa /api/v1/text/caesar.

Los errores de red, token, contraseña, archivo corrupto o desplazamiento se muestran sin cerrar la aplicación. Las pruebas de móvil real y TalkBack quedan PENDIENTE-HUMANO hasta disponer de dispositivo.