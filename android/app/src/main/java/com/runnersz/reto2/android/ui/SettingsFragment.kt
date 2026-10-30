package com.runnersz.reto2.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.data.ApiClient
import com.runnersz.reto2.android.data.ApiResult
import com.runnersz.reto2.android.data.ServerSettings
import com.runnersz.reto2.android.databinding.FragmentAjustesBinding
import com.runnersz.reto2.android.domain.AddressParser
import com.runnersz.reto2.android.domain.Direccion

/**
 * Ajustes de conexión (CU-17, §9.4).
 *
 * - Campo «Dirección del servidor» con ayuda (§5.2).
 * - Token oculto por defecto con botón mostrar (`app:endIconMode="password_toggle"`).
 * - **«Probar»** → `GET /api/v1/auth/check`: **204** ⇒ «Conexión correcta» (icono ✓ + texto);
 *   **401** ⇒ «Token no válido. Revisa los ajustes»; sin respuesta ⇒ aviso de conexión.
 * - **«Guardar»** → persistencia local (DJ3). Los datos inválidos **no se guardan**.
 *
 * La red se ejecuta fuera del hilo UI (OkHttp `enqueue`) y el resultado vuelve al hilo
 * principal; la app nunca se cierra por un fallo (§7.2).
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAjustesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(vista: View, estado: Bundle?) {
        super.onViewCreated(vista, estado)
        cargarGuardados()

        binding.botonProbar.setOnClickListener { probarConexion() }
        binding.botonGuardar.setOnClickListener { guardar() }
    }

    /** Rellena los campos con lo que ya estuviera guardado (sin mostrar el token). */
    private fun cargarGuardados() {
        val ajustes = ServerSettings(requireContext())
        binding.direccion.setText(ajustes.direccion)
        binding.token.setText(ajustes.token)
    }

    /**
     * «Probar»: valida la dirección en local y llama a `checkAuth` (§5.2).
     *
     * 204 ⇒ «Conexión correcta»; 401 ⇒ «Token no válido»; sin respuesta ⇒ «No se pudo
     * conectar». Mientras responde, deshabilita el botón para no duplicar peticiones.
     */
    private fun probarConexion() {
        val direccion = binding.direccion.text.toString()
        val token = binding.token.text.toString()

        val url = when (val analisis = AddressParser.parse(direccion)) {
            is Direccion.Invalida -> {
                mostrarResultado(ok = false, texto = analisis.motivo)
                return
            }
            is Direccion.Valida -> analisis.url
        }

        if (token.isBlank()) {
            mostrarResultado(ok = false, texto = "El token no puede estar vacío")
            return
        }

        binding.botonProbar.isEnabled = false
        ApiClient(url, token).checkAuth { resultado ->
            if (!isAdded || _binding == null) return@checkAuth
            binding.botonProbar.isEnabled = true
            when (resultado) {
                is ApiResult.Success ->
                    mostrarResultado(ok = true, texto = getString(com.runnersz.reto2.android.R.string.conexion_correcta))
                is ApiResult.Failure ->
                    mostrarResultado(ok = false, texto = resultado.error.texto)
            }
        }
    }

    /** «Guardar»: solo persiste si los dos campos son válidos (§5.2, DJ3). */
    private fun guardar() {
        val ajustes = ServerSettings(requireContext())
        when (val resultado = ajustes.guardar(
            binding.direccion.text.toString(),
            binding.token.text.toString(),
        )) {
            is ServerSettings.ResultadoGuardado.Guardado ->
                UiMessages.toast(this, "Ajustes guardados")
            is ServerSettings.ResultadoGuardado.Invalido -> {
                mostrarResultado(ok = false, texto = resultado.motivo)
                UiMessages.aviso(this, resultado.motivo)
            }
        }
    }

    /** Muestra el icono y el texto del resultado de la conexión (§5.2). */
    private fun mostrarResultado(ok: Boolean, texto: String) {
        binding.resultadoConexion.visibility = View.VISIBLE
        binding.iconoResultado.setImageResource(
            if (ok) com.runnersz.reto2.android.R.drawable.ic_check else com.runnersz.reto2.android.R.drawable.ic_error,
        )
        binding.textoResultado.text = texto
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
