# Vectores AES interoperables

Datos públicos generados por scripts/generate_vectors.py desde la raíz.
La contraseña pública es `RunnersZ-ñ-🔐`; no la uses en producción.

manifest.json contiene los archivos y huellas del original.
Android descifra los .enc y compara bytes/huellas: entrada vacía,
límites de bloque, texto UTF-8 y PNG de 1x1 válido.
Python verifica los CRC del PNG y las huellas del manifiesto.
Sales y nonces aleatorios hacen que regenerar cambie los .enc.

No edites un .enc manualmente. Regenera con Python 3.12, ejecuta todas
las pruebas y revisa el diff antes del commit.