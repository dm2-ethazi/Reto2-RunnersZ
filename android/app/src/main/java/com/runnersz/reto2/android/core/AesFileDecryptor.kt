package com.runnersz.reto2.android.core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.security.GeneralSecurityException
import java.security.MessageDigest
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AesFileDecryptor {
    fun decrypt(inputFile: File, outputFile: File, password: String) {
        if (password.isEmpty()) {
            throw EmptyAesPasswordException()
        }
        if (!inputFile.isFile) {
            throw IOException("No existe el fichero de entrada.")
        }
        if (inputFile.canonicalFile == outputFile.canonicalFile) {
            throw IOException("La salida no puede ser el mismo fichero que la entrada.")
        }
        if (outputFile.exists()) {
            throw IOException("El destino ya existe.")
        }
        val totalSize = inputFile.length()
        if (totalSize < EncFormat.HEADER_SIZE + EncFormat.DIGEST_SIZE + EncFormat.TAG_SIZE) {
            throw AesFormatException("El fichero .enc es demasiado pequeño.")
        }

        val part = File(outputFile.absoluteFile.parentFile, outputFile.name + ".part")
        outputFile.absoluteFile.parentFile?.mkdirs()
        if (!part.createNewFile()) {
            throw IOException("El fichero temporal ya existe.")
        }

        var completed = false
        try {
            FileInputStream(inputFile).use { source ->
                val headerBytes = readExactly(source, EncFormat.HEADER_SIZE)
                val header = EncFormat.parse(headerBytes)
                val payloadSize = totalSize - EncFormat.HEADER_SIZE
                val (blockCount, lastLength) =
                    EncFormat.blockCountAndLastLength(payloadSize, header.blockSize)
                val keyBytes = deriveKey(header, password)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val hasher = MessageDigest.getInstance("SHA-256")
                var expectedDigest = ByteArray(0)

                FileOutputStream(part).use { target ->
                    for (counter in 0 until blockCount) {
                        val isLast = counter == blockCount - 1
                        val ciphertextLength =
                            if (isLast) {
                                lastLength + EncFormat.DIGEST_SIZE + EncFormat.TAG_SIZE
                            } else {
                                header.blockSize + EncFormat.TAG_SIZE
                            }
                        val ciphertext = readExactly(source, ciphertextLength)
                        cipher.init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(keyBytes, "AES"),
                            GCMParameterSpec(128, EncFormat.nonce(header.noncePrefix, counter)),
                        )
                        cipher.updateAAD(EncFormat.aad(headerBytes, isLast))
                        val plaintext =
                            try {
                                cipher.doFinal(ciphertext)
                            } catch (exception: AEADBadTagException) {
                                throw AesAuthenticationException()
                            } catch (exception: GeneralSecurityException) {
                                throw AesFormatException(
                                    "No se pudo descifrar el bloque $counter.",
                                    exception,
                                )
                            }

                        val content =
                            if (isLast) {
                                if (plaintext.size != lastLength + EncFormat.DIGEST_SIZE) {
                                    throw AesFormatException(
                                        "El último bloque no contiene la huella esperada.",
                                    )
                                }
                                expectedDigest = plaintext.copyOfRange(lastLength, plaintext.size)
                                plaintext.copyOfRange(0, lastLength)
                            } else {
                                if (plaintext.size != header.blockSize) {
                                    throw AesFormatException(
                                        "Un bloque intermedio tiene un tamaño incorrecto.",
                                    )
                                }
                                plaintext
                            }
                        target.write(content)
                        hasher.update(content)
                    }
                    if (source.read() != -1) {
                        throw AesFormatException("El fichero .enc contiene datos sobrantes.")
                    }
                    target.flush()
                    target.fd.sync()
                }

                if (!MessageDigest.isEqual(expectedDigest, hasher.digest())) {
                    throw AesIntegrityException()
                }
            }

            Files.createLink(outputFile.toPath(), part.toPath())
            completed = true
            Files.delete(part.toPath())
        } finally {
            if (!completed) {
                part.delete()
            }
        }
    }

    private fun deriveKey(header: EncHeader, password: String): ByteArray {
        val specification = PBEKeySpec(password.toCharArray(), header.salt, header.iterations, 256)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(specification).encoded
        } catch (exception: GeneralSecurityException) {
            throw AesFormatException("No se pudo derivar la clave AES.", exception)
        } finally {
            specification.clearPassword()
        }
    }

    private fun readExactly(source: InputStream, length: Int): ByteArray {
        val data = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = source.read(data, offset, length - offset)
            if (read < 0) {
                throw AesFormatException("El fichero .enc está truncado.")
            }
            offset += read
        }
        return data
    }
}
