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
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.api.ApiClient
import com.runnersz.reto2.android.api.ApiError
import com.runnersz.reto2.android.api.CaesarRequest
import com.runnersz.reto2.android.api.CaesarResponse
import com.runnersz.reto2.android.api.RunnersZApi
import com.runnersz.reto2.android.databinding.FragmentOrdenBinding
import com.runnersz.reto2.android.viewmodel.OrdenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class OrdenFragment : Fragment() {

    private var _binding: FragmentOrdenBinding? = null
    private val binding get() = _binding!!

    private val ordenViewModel: OrdenViewModel by viewModels()
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

        // Observar ViewModel
        ordenViewModel.esProcesando.observe(viewLifecycleOwner) { procesando ->
            binding.btnDescifrarOrden.isEnabled = !procesando
            binding.btnReintentar.isEnabled = !procesando
        }

        ordenViewModel.resultadoDescifrado.observe(viewLifecycleOwner) { resultado ->
            if (resultado.isNotEmpty()) {
                binding.etResultado.setText(resultado)
                mostrarMensaje(getString(R.string.orden_exito), false)
                Toast.makeText(requireContext(), getString(R.string.orden_exito), Toast.LENGTH_SHORT).show()
            }
        }

        ordenViewModel.mensajeError.observe(viewLifecycleOwner) { error ->
            if (error != null && ordenViewModel.esError.value == true) {
                mostrarMensaje(error, true)
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }

        ordenViewModel.esProcesando.observe(viewLifecycleOwner) { procesando ->
            binding.btnReintentar.visibility = if (procesando || ordenViewModel.mensajeError.value != null) View.VISIBLE else View.GONE
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
                ordenViewModel.cargarTexto(texto)
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
            Snackbar.make(binding.root, getString(R.string.orden_error_texto), Snackbar.LENGTH_SHORT).show()
            return
        }

        if (shiftStr.isNullOrEmpty()) {
            mostrarMensaje(getString(R.string.orden_error_shift), true)
            Snackbar.make(binding.root, getString(R.string.orden_error_shift), Snackbar.LENGTH_SHORT).show()
            return
        }

        val shift = shiftStr?.toIntOrNull()
        if (shift == null || shift < 1 || shift > 25) {
            mostrarMensaje(getString(R.string.orden_error_shift), true)
            Snackbar.make(binding.root, getString(R.string.orden_error_shift), Snackbar.LENGTH_SHORT).show()
            return
        }

        ordenViewModel.descifrarOrden(texto, shift)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
