package com.runnersz.reto2.android.ui.imagen

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.runnersz.reto2.android.api.RunnersZApi
import com.runnersz.reto2.android.databinding.FragmentImagenBinding
import com.runnersz.reto2.android.viewmodel.ImagenViewModel
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ImagenFragment : Fragment() {

    private var _binding: FragmentImagenBinding? = null
    private val binding get() = _binding!!

    private val imagenViewModel: ImagenViewModel by viewModels()
    private val gson: Gson = Gson()

    private val seleccionarArchivo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imagenViewModel.seleccionarArchivo(it, requireContext())
            binding.tvArchivoSeleccionado.text = it.lastPathSegment ?: it.toString()
            binding.tvMensajeImagen.text = "Archivo cargado / File loaded"
            binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))
            binding.ivResultadoImagen.setImageBitmap(it.toBitmap(requireContext()))
            binding.tvEstadoValor.text = requireContext().getString(R.string.imagen_estado_listo)
            ocultarReintentar()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagenBinding.inflate(inflater, container, false)
        val view = binding.root

        verificarArchivoCompartido()

        binding.btnSeleccionarArchivo.setOnClickListener {
            seleccionarArchivo.launch("*/*")
        }

        binding.btnDescifrarImagen.setOnClickListener {
            descifrarImagen()
        }

        binding.btnReintentarImagen.setOnClickListener {
            descifrarImagen()
        }

        // Observar ViewModel
        imagenViewModel.esProcesando.observe(viewLifecycleOwner) { procesando ->
            binding.progresoDescifrado.visibility = if (procesando) View.VISIBLE else View.GONE
            binding.btnDescifrarImagen.isEnabled = !procesando
            binding.btnReintentarImagen.isEnabled = !procesando
        }

        imagenViewModel.mensajeEstado.observe(viewLifecycleOwner) { estado ->
            binding.tvEstadoValor.text = estado
            binding.tvEstadoValor.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (imagenViewModel.esError.value == true) R.color.color_error else R.color.color_success
                )
            )
        }

        imagenViewModel.bitmapResultado.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.ivResultadoImagen.setImageBitmap(bitmap)
                binding.tvMensajeImagen.text = requireContext().getString(R.string.imagen_exito)
                binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))
                Toast.makeText(requireContext(), requireContext().getString(R.string.imagen_exito), Toast.LENGTH_SHORT).show()
            }
        }

        imagenViewModel.esError.observe(viewLifecycleOwner) { esError ->
            if (esError == true && imagenViewModel.mensajeEstado.value != "Listo / Ready") {
                Snackbar.make(view, imagenViewModel.mensajeEstado.value ?: "", Snackbar.LENGTH_LONG).show()
            }
        }

        imagenViewModel.esProcesando.observe(viewLifecycleOwner) { procesando ->
            binding.btnReintentarImagen.visibility = if (procesando || imagenViewModel.esError.value == true) View.VISIBLE else View.GONE
        }

        return view
    }

    private fun verificarArchivoCompartido() {
        val prefs = requireContext().getSharedPreferences("cipher_sprint_share", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("shared_uri", null)
        if (uriStr != null) {
            prefs.edit().remove("shared_uri").apply()
            Uri.parse(uriStr)?.let {
                imagenViewModel.seleccionarArchivo(it, requireContext())
                binding.tvArchivoSeleccionado.text = it.lastPathSegment ?: it.toString()
                binding.tvMensajeImagen.text = "Archivo recibido vía Compartir / Received via Share"
                binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))
            }
        }
    }

    private fun descifrarImagen() {
        val uri = imagenViewModel.archivoSeleccionado.value
        val password = binding.etClave.text?.toString()?.trim() ?: ""

        if (uri == null) {
            binding.tvMensajeImagen.text = requireContext().getString(R.string.imagen_error_seleccion)
            binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
            Snackbar.make(binding.root, getString(R.string.imagen_error_seleccion), Snackbar.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            binding.tvMensajeImagen.text = requireContext().getString(R.string.imagen_error_clave)
            binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
            Snackbar.make(binding.root, getString(R.string.imagen_error_clave), Snackbar.LENGTH_SHORT).show()
            return
        }

        imagenViewModel.descifrarImagen(uri, password, requireContext())
    }

    private fun Uri.toBitmap(context: Context): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(this)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun ocultarReintentar() {
        binding.btnReintentarImagen.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            val cacheDir = requireContext().cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith("_file")) {
                    borrarArchivoTemporal(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _binding = null
    }

    private fun borrarArchivoTemporal(file: File) {
        try {
            if (file.exists()) {
                val length = file.length()
                val fos = FileOutputStream(file)
                fos.write(ByteArray(length.toInt()))
                fos.close()
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            file.delete()
        }
    }
}
