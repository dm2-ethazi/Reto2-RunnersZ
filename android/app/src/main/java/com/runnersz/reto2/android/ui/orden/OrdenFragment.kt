package com.runnersz.reto2.android.ui.orden

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.api.ApiClient
import com.runnersz.reto2.android.api.ApiError
import com.runnersz.reto2.android.api.CaesarRequest
import com.runnersz.reto2.android.api.CaesarResponse
import com.runnersz.reto2.android.api.RunnersZApi
import com.runnersz.reto2.android.databinding.FragmentOrdenBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class OrdenFragment : Fragment() {

    private var _binding: FragmentOrdenBinding? = null
    private val binding get() = _binding!!

    private lateinit var api: RunnersZApi
    private val gson: Gson = Gson()

    private val cargarArchivo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { leerTextoDesdeUri(requireContext(), it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdenBinding.inflate(inflater, container, false)
        val view = binding.root

        api = ApiClient.api

        verificarArchivoCompartido()

        binding.btnCargarTxt.setOnClickListener {
            cargarArchivo.launch("text/*")
        }

        binding.btnDescifrarOrden.setOnClickListener {
            descifrarOrden()
        }

        binding.btnReintentar.setOnClickListener {
            descifrarOrden()
        }

        return view
    }

    private fun verificarArchivoCompartido() {
        val prefs = requireContext().getSharedPreferences("cipher_sprint_share", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("shared_uri", null)
        if (uriStr != null) {
            prefs.edit().remove("shared_uri").apply()
            Uri.parse(uriStr)?.let { leerTextoDesdeUri(requireContext(), it) }
        }
    }

    private fun leerTextoDesdeUri(context: Context, uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val texto = reader.readText()
                binding.etTexto.setText(texto)
                mostrarMensaje("Archivo cargado / File loaded", false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mostrarMensaje("No se pudo leer el archivo / Could not read file", true)
        }
    }

    private fun descifrarOrden() {
        val texto = binding.etTexto.text?.toString()?.trim()
        val shiftStr = binding.etDesplazamiento.text?.toString()?.trim()

        if (texto.isNullOrEmpty()) {
            mostrarMensaje(getString(R.string.orden_error_texto), true)
            return
        }

        if (shiftStr.isNullOrEmpty()) {
            mostrarMensaje(getString(R.string.orden_error_shift), true)
            return
        }

        val shift = shiftStr?.toIntOrNull()
        if (shift == null || shift < 1 || shift > 25) {
            mostrarMensaje(getString(R.string.orden_error_shift), true)
            return
        }

        ocultarReintentar()
        mostrarMensaje(getString(R.string.msg_cargando), false)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response: retrofit2.Response<CaesarResponse> = api.caesar(
                    CaesarRequest(text = texto, shift = shift, mode = "decrypt")
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val resultado = response.body()?.result ?: "Sin resultado"
                        binding.etResultado.setText(resultado)
                        mostrarMensaje(getString(R.string.orden_exito), false)
                        Toast.makeText(requireContext(), getString(R.string.orden_exito), Toast.LENGTH_SHORT).show()
                    } else {
                        manejarErrorApi(response)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarMensaje("${getString(R.string.orden_error_api)}: ${e.message}", true)
                    mostrarReintentar()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun manejarErrorApi(response: retrofit2.Response<*>) {
        val errorMessage = when (response.code()) {
            401 -> {
                val apiError = parseErrorBody(response)
                getString(R.string.error_unauthorized, apiError?.message ?: "Token no válido")
            }
            400 -> {
                val apiError = parseErrorBody(response)
                when (apiError?.code) {
                    "INVALID_SHIFT" -> getString(R.string.orden_error_shift)
                    "EMPTY_PASSWORD" -> getString(R.string.error_empty_password)
                    else -> apiError?.message ?: getString(R.string.orden_error_api)
                }
            }
            422 -> {
                val apiError = parseErrorBody(response)
                apiError?.message ?: getString(R.string.orden_error_api)
            }
            else -> getString(R.string.orden_error_api) + " (HTTP ${response.code()})"
        }
        mostrarMensaje(errorMessage, true)
        mostrarReintentar()
    }

    private fun parseErrorBody(response: retrofit2.Response<*>): ApiError? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                gson.fromJson(errorBody, ApiError::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mostrarMensaje(mensaje: String, esError: Boolean) {
        binding.tvMensaje.text = mensaje
        binding.tvMensaje.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (esError) R.color.color_error else R.color.color_success
            )
        )
    }

    private fun mostrarReintentar() {
        binding.btnReintentar.visibility = View.VISIBLE
    }

    private fun ocultarReintentar() {
        binding.btnReintentar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
