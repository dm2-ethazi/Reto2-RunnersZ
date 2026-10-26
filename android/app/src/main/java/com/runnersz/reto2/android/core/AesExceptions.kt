package com.runnersz.reto2.android.core

open class AesDecryptException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class EmptyAesPasswordException :
    AesDecryptException("La clave AES no puede estar vacía.")

class AesFormatException(message: String, cause: Throwable? = null) :
    AesDecryptException(message, cause)

class AesAuthenticationException :
    AesDecryptException("Clave incorrecta o fichero dañado: falló la autenticación AES-GCM.")

class AesIntegrityException :
    AesDecryptException("La huella SHA-256 del contenido recuperado no coincide.")
