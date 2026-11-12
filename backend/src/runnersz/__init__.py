"""Servidor RunnersZ: API FastAPI de cifrado AES-256-GCM y cifrados clásicos.

El paquete separa composición (main), HTTP (routes), seguridad (auth), ajustes
(config), algoritmos (crypto y text), modelos de entrada (models) y errores
(errors), de modo que cada pieza se pueda probar por separado.
"""
