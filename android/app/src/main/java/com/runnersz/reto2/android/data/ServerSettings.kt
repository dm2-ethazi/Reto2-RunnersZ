package com.runnersz.reto2.android.data

import android.content.Context
import android.content.SharedPreferences
import com.runnersz.reto2.android.domain.AddressParser
import com.runnersz.reto2.android.domain.Direccion

/**
 * Persistencia de la dirección del servidor y del token (DJ3: `SharedPreferences` privadas).
 *
 * El token queda en claro dentro del sandbox de la app (`allowBackup="false"` en el manifest),
 * como decide DJ3; se documenta en el manual. La dirección guardada ya está normalizada
 * (`http://host:puerto`) por [AddressParser].
 */
class ServerSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(NOMBRE, Context.MODE_PRIVATE)

    /** Dirección normalizada guardada, o vacía si nunca se guardó. */
    var direccion: String
        get() = prefs.getString(CLAVE_DIRECCION, "") ?: ""
        set(valor) {
            prefs.edit().putString(CLAVE_DIRECCION, valor).apply()
        }

    /** Token guardado, o vacío si nunca se guardó. */
    var token: String
        get() = prefs.getString(CLAVE_TOKEN, "") ?: ""
        set(valor) {
            prefs.edit().putString(CLAVE_TOKEN, valor).apply()
        }

    /** `true` cuando falta dirección o token: la primera ejecución abre Ajustes. */
    fun estaIncompleto(): Boolean = direccion.isBlank() || token.isBlank()

    /**
     * Valora ambos campos con [AddressParser] y solo los guarda si los dos son válidos
     * («datos inválidos no se guardan», §5.2).
     *
     * @return la dirección normalizada si se guardó; el error si no se guardó nada.
     */
    fun guardar(direccionIntroducida: String, tokenIntroducido: String): ResultadoGuardado {
        val direccionLimpia = direccionIntroducida.trim()
        val tokenLimpio = tokenIntroducido.trim()

        if (tokenLimpio.isEmpty()) {
            return ResultadoGuardado.Invalido("El token no puede estar vacío")
        }
        return when (val analisis = AddressParser.parse(direccionLimpia)) {
            is Direccion.Valida -> {
                direccion = analisis.url
                token = tokenLimpio
                ResultadoGuardado.Guardado(analisis.url)
            }
            is Direccion.Invalida -> ResultadoGuardado.Invalido(analisis.motivo)
        }
    }

    /** Resultado de intentar guardar los ajustes. */
    sealed class ResultadoGuardado {
        data class Guardado(val direccion: String) : ResultadoGuardado()
        data class Invalido(val motivo: String) : ResultadoGuardado()
    }

    companion object {
        private const val NOMBRE = "runnersz_ajustes"
        private const val CLAVE_DIRECCION = "direccion"
        private const val CLAVE_TOKEN = "token"
    }
}
