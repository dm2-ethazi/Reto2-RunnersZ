package com.runnersz.reto2.android.ui.ajustes

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.google.android.material.snackbar.Snackbar
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.api.ApiClient
import com.runnersz.reto2.android.api.RunnersZApi
import com.runnersz.reto2.android.api.TokenManager
import com.runnersz.reto2.android.databinding.FragmentAjustesBinding
import com.runnersz.reto2.android.viewmodel.AjustesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AjustesFragment : Fragment() {

    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!

    private val ajustesViewModel: AjustesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAjustesBinding.inflate(inflater, container, false)
        val view = binding.root

        // Cargar configuración existente
        val tokenGuardado = TokenManager.getToken()
        val servidorGuardado = TokenManager.getServerUrl()

        if (tokenGuardado.isNotEmpty()) {
            binding.etToken.setText(tokenGuardado)
        }
        if (servidorGuardado.isNotEmpty()) {
            binding.etDireccion.setText(servidorGuardado)
        }

        actualizarEstadoConfig(TokenManager.isConfigured())

        // Observar ViewModel
        ajustesViewModel.mensajeEstado.observe(viewLifecycleOwner) { estado ->
            binding.tvConfigEstado.text = estado
            binding.tvConfigEstado.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (ajustesViewModel.esError.value == true) R.color.color_error else R.color.color_success
                )
            )
        }

        ajustesViewModel.esProcesandoConexion.observe(viewLifecycleOwner) { procesando ->
            binding.btnProbarConexion.isEnabled = !procesando
            binding.btnGuardarConfig.isEnabled = !procesando
        }

        ajustesViewModel.esError.observe(viewLifecycleOwner) { esError ->
            if (esError == true && ajustesViewModel.mensajeEstado.value != "Configuración no guardada / Config not saved") {
                Snackbar.make(view, ajustesViewModel.mensajeEstado.value ?: "", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.btnGuardarConfig.setOnClickListener {
            guardarConfiguracion()
        }

        binding.btnProbarConexion.setOnClickListener {
            probarConexion()
        }

        return view
    }

    private fun guardarConfiguracion() {
        val direccion = binding.etDireccion.text?.toString()?.trim()
        val token = binding.etToken.text?.toString()?.trim()

        if (direccion.isNullOrEmpty()) {
            binding.tvConfigEstado.text = "Ingresá la dirección del servidor"
            binding.tvConfigEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
            Snackbar.make(binding.root, "Ingresá la dirección del servidor", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (!esUrlValida(direccion)) {
            binding.tvConfigEstado.text = "URL inválida. Formato: IP:puerto o https://host"
            binding.tvConfigEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
            Snackbar.make(binding.root, "URL inválida", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (token.isNullOrEmpty()) {
            binding.tvConfigEstado.text = "Ingresá el token Bearer"
            binding.tvConfigEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
            Snackbar.make(binding.root, "Ingresá el token Bearer", Snackbar.LENGTH_SHORT).show()
            return
        }

        val direccionValida = direccion!!
        val tokenValido = token!!
        val serverUrl = if (!direccionValida.endsWith("/")) direccionValida + "/" else direccionValida

        ajustesViewModel.guardarConfiguracion(serverUrl, tokenValido, requireContext())
    }

    private fun esUrlValida(url: String): Boolean {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            Patterns.WEB_URL.matcher(url).matches()
        } else {
            url.contains(":") && !url.contains(" ") && url.length > 3
        }
    }

    private fun probarConexion() {
        binding.tvApiEstado.text = requireContext().getString(R.string.msg_cargando)
        binding.tvApiEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))

        CoroutineScope(Dispatchers.Main).launch {
            try {
                TokenManager.applyConfigToApi()
                val response = ApiClient.api.checkToken()

                withContext(Dispatchers.Main) {
                    when (response.code()) {
                        200, 204 -> {
                            binding.tvApiEstado.text = requireContext().getString(R.string.ajustes_api_ok)
                            binding.tvApiEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))
                        }
                        401 -> {
                            binding.tvApiEstado.text = requireContext().getString(R.string.ajustes_token_invalido)
                            binding.tvApiEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
                        }
                        else -> {
                            binding.tvApiEstado.text = requireContext().getString(R.string.ajustes_api_error)
                            binding.tvApiEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvApiEstado.text = "${requireContext().getString(R.string.ajustes_api_error)}: ${e.message}"
                    binding.tvApiEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
                    e.printStackTrace()
                }
            }
        }
    }

    private fun actualizarEstadoConfig(configurada: Boolean) {
        if (configurada) {
            binding.tvConfigEstado.text = requireContext().getString(R.string.ajustes_config_guardada)
            binding.tvConfigEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))
        } else {
            binding.tvConfigEstado.text = requireContext().getString(R.string.ajustes_config_no_guardada)
            binding.tvConfigEstado.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
