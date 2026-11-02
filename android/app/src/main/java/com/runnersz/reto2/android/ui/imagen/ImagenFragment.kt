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
import com.google.gson.Gson
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.api.ApiClient
import com.runnersz.reto2.android.api.ApiError
import com.runnersz.reto2.android.api.RunnersZApi
import com.runnersz.reto2.android.databinding.FragmentImagenBinding
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

    private lateinit var api: RunnersZApi
    private val gson: Gson = Gson()
    private var archivoSeleccionado: Uri? = null

    private val seleccionarArchivo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        archivoSeleccionado = uri
        uri?.let {
            binding.tvArchivoSeleccionado.text = it.lastPathSegment ?: it.toString()
            binding.tvMensajeImagen.text = "Archivo cargado / File loaded"
            binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))
            binding.ivResultadoImagen.setImageBitmap(null)
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

        api = ApiClient.api

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

        return view
    }

    private fun verificarArchivoCompartido() {
        val prefs = requireContext().getSharedPreferences("cipher_sprint_share", Context.MODE_PRIVATE)
        val uriStr = prefs.getString("shared_uri", null)
        if (uriStr != null) {
            prefs.edit().remove("shared_uri").apply()
            Uri.parse(uriStr)?.let {
                archivoSeleccionado = it
                binding.tvArchivoSeleccionado.text = it.lastPathSegment ?: it.toString()
                binding.tvMensajeImagen.text = "Archivo recibido vía Compartir / Received via Share"
                binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))
            }
        }
    }

    private fun descifrarImagen() {
        val uri = archivoSeleccionado
        val password = binding.etClave.text?.toString().trim()

        if (uri == null) {
            binding.tvMensajeImagen.text = requireContext().getString(R.string.imagen_error_seleccion)
            binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
            return
        }

        if (TextUtils.isEmpty(password)) {
            binding.tvMensajeImagen.text = requireContext().getString(R.string.imagen_error_clave)
            binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
            return
        }

        binding.tvEstadoValor.text = requireContext().getString(R.string.imagen_estado_procesando)
        binding.tvEstadoValor.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))
        binding.progresoDescifrado.visibility = View.VISIBLE
        ocultarReintentar()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val file = uriToFile(requireContext(), uri)
                if (file == null) {
                    withContext(Dispatchers.Main) {
                        binding.progresoDescifrado.visibility = View.GONE
                        binding.tvMensajeImagen.text = requireContext().getString(R.string.imagen_error_seleccion)
                        binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
                        mostrarReintentar()
                    }
                    return@launch
                }

                val requestFile = RequestBody.create(
                    file,
                    MediaType.parse("application/octet-stream")
                )
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val passwordBody = RequestBody.create(password, MediaType.parse("text/plain"))

                val response: retrofit2.Response<kotlin.Unit> = api.decryptFile(body, passwordBody)

                withContext(Dispatchers.Main) {
                    binding.progresoDescifrado.visibility = View.GONE
                    if (response.isSuccessful) {
                        val integrityHeader = response.headers().get("X-Integrity")
                        val verificada = "verified".equals(integrityHeader, ignoreCase = true)

                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            binding.ivResultadoImagen.setImageBitmap(bitmap)
                        }

                        binding.tvEstadoValor.text = if (verificada) {
                            requireContext().getString(R.string.imagen_integridad_verificada)
                        } else {
                            requireContext().getString(R.string.imagen_integridad_fallida)
                        }
                        binding.tvEstadoValor.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))

                        binding.tvMensajeImagen.text = requireContext().getString(R.string.imagen_exito)
                        binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_success))

                        Toast.makeText(requireContext(), requireContext().getString(R.string.imagen_exito), Toast.LENGTH_SHORT).show()

                        // Borrado robusto de caché temporal (Contrato 5.1)
                        borrarArchivoTemporal(file)
                    } else {
                        manejarErrorApi(response)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progresoDescifrado.visibility = View.GONE
                    binding.tvEstadoValor.text = requireContext().getString(R.string.imagen_error_api)
                    binding.tvEstadoValor.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
                    binding.tvMensajeImagen.text = "${e.message}"
                    binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
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
                    "EMPTY_FILE" -> getString(R.string.error_empty_file)
                    "EMPTY_PASSWORD" -> getString(R.string.error_empty_password)
                    else -> apiError?.message ?: getString(R.string.imagen_error_api)
                }
            }
            413 -> {
                getString(R.string.error_file_too_large)
            }
            422 -> {
                val apiError = parseErrorBody(response)
                when (apiError?.code) {
                    "DECRYPT_FAILED" -> getString(R.string.error_decrypt_failed)
                    else -> apiError?.message ?: getString(R.string.imagen_error_api)
                }
            }
            else -> getString(R.string.imagen_error_api) + " (HTTP ${response.code()})"
        }
        binding.tvEstadoValor.text = errorMessage
        binding.tvEstadoValor.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
        binding.tvMensajeImagen.text = errorMessage
        binding.tvMensajeImagen.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_error))
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

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, System.currentTimeMillis().toString() + "_file")
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mostrarReintentar() {
        binding.btnReintentarImagen.visibility = View.VISIBLE
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
}
