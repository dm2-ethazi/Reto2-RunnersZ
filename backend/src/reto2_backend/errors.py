class Reto2Error(Exception):
    def __init__(self, code: str, message: str, status_code: int = 400) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


class InvalidKeyError(Reto2Error):
    def __init__(
        self, message: str = "La clave debe contener solo letras A-Z y no puede estar vacía."
    ) -> None:
        super().__init__("CLAVE_INVALIDA", message, 422)


class InvalidShiftError(Reto2Error):
    def __init__(self, message: str = "El desplazamiento debe ser un entero entre 1 y 25.") -> None:
        super().__init__("DESPLAZAMIENTO_INVALIDO", message, 422)


class InvalidModeError(Reto2Error):
    def __init__(self, message: str = "El modo debe ser 'cifrar' o 'descifrar'.") -> None:
        super().__init__("MODO_INVALIDO", message, 422)


class EmptyPasswordError(Reto2Error):
    def __init__(self) -> None:
        super().__init__("CONTRASENA_VACIA", "La contraseña no puede estar vacía.", 422)


class InvalidPasswordError(Reto2Error):
    def __init__(self) -> None:
        super().__init__("CONTRASENA_INVALIDA", "La contraseña no es texto UTF-8 válido.", 422)


class InputNotFoundError(Reto2Error):
    def __init__(self, path: str) -> None:
        super().__init__("ENTRADA_NO_EXISTE", f"No existe el fichero de entrada: {path}", 404)


class InputNotFileError(Reto2Error):
    def __init__(self, path: str) -> None:
        super().__init__("ENTRADA_NO_ES_FICHERO", f"La entrada no es un fichero: {path}", 422)


class OutputExistsError(Reto2Error):
    def __init__(self, path: str) -> None:
        super().__init__("SALIDA_EXISTE", f"La salida ya existe y no se sobrescribirá: {path}", 409)


class OutputSameAsInputError(Reto2Error):
    def __init__(self) -> None:
        super().__init__(
            "SALIDA_IGUAL_ENTRADA", "La salida no puede ser el mismo fichero que la entrada.", 409
        )


class OutputParentError(Reto2Error):
    def __init__(self, path: str) -> None:
        super().__init__("RUTA_SALIDA_INVALIDA", f"No existe la carpeta de destino: {path}", 422)


class FormatError(Reto2Error):
    def __init__(self, message: str = "El fichero no tiene un formato .enc v1 válido.") -> None:
        super().__init__("FORMATO_INVALIDO", message, 422)


class AuthenticationError(Reto2Error):
    def __init__(self) -> None:
        super().__init__(
            "CLAVE_INCORRECTA_O_DANADO",
            "Clave incorrecta o fichero dañado: falló la autenticación AES-GCM.",
            422,
        )


class IntegrityError(Reto2Error):
    def __init__(self) -> None:
        super().__init__(
            "INTEGRIDAD_FALLIDA",
            "La huella SHA-256 del contenido recuperado no coincide.",
            422,
        )


class StorageError(Reto2Error):
    def __init__(
        self, message: str = "No se pudo leer o escribir el fichero.", status_code: int = 500
    ) -> None:
        super().__init__("ERROR_ALMACENAMIENTO", message, status_code)
