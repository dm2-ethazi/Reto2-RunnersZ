package com.runnersz.reto2.android.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.data.ApiClient
import com.runnersz.reto2.android.data.ApiResult
import com.runnersz.reto2.android.domain.FileValidator
import com.runnersz.reto2.android.domain.ImageDecoder
import com.runnersz.reto2.android.domain.ProblemaArchivo
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author jairoben
 */
class ImageFragment : Fragment(R.layout.fragment_image) {

    private val viewModel: ImageViewModel by viewModels()

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) copySelection(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etClave = view.findViewById<EditText>(R.id.etClave)
        val button = view.findViewById<MaterialButton>(R.id.btnDecryptImage)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            (activity as? MainActivity)?.goHome()
        }
        view.findViewById<View>(R.id.fileCard).setOnClickListener {
            filePicker.launch(arrayOf("*/*"))
        }

        etClave.doAfterTextChanged { viewModel.key = it?.toString().orEmpty() }
        button.setOnClickListener { decrypt() }

        if (viewModel.file == null) {
            showEmpty(view)
        } else {
            restore(view)
        }
    }

    private fun restore(view: View) {
        val file = viewModel.file
        if (file == null || !file.exists()) {
            showEmpty(view)
            return
        }

        view.findViewById<TextView>(R.id.tvFileName).text =
            viewModel.displayName ?: file.name
        view.findViewById<EditText>(R.id.etClave).setText(viewModel.key)
        viewModel.bitmap?.let { bitmap ->
            view.findViewById<ImageView>(R.id.ivResult).setImageBitmap(bitmap)
            view.findViewById<View>(R.id.previewCard).visibility = View.VISIBLE
        }
        viewModel.status?.let { text ->
            view.findViewById<TextView>(R.id.tvStatus).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(requireContext(), viewModel.statusColorRes))
            }
        }
        if (viewModel.loading) {
            paintLoading(view, true)
        }
    }

    /** Estado inicial: sin archivo y sin clave, cada usuario elige el suyo. */
    private fun showEmpty(view: View) {
        viewModel.file = null
        viewModel.displayName = null
        viewModel.key = ""
        viewModel.status = null
        viewModel.bitmap = null

        view.findViewById<TextView>(R.id.tvFileName).text = getString(R.string.image_no_file)
        view.findViewById<EditText>(R.id.etClave).setText("")
        showStatus("", R.color.muted)
    }

    private fun copySelection(uri: Uri) {
        try {
            val name = displayName(uri) ?: uri.lastPathSegment ?: "archivo.enc"
            val safeName = File(name).name.ifBlank { "archivo.enc" }
            val destination = File(
                requireContext().cacheDir,
                "upload-${System.currentTimeMillis()}-$safeName",
            )
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("No se pudo abrir el archivo")

            val problem = FileValidator.problema(safeName, destination.length())
            if (problem != null) {
                destination.delete()
                viewModel.bitmap?.recycle()
                viewModel.bitmap = null
                view?.findViewById<View>(R.id.previewCard)?.visibility = View.GONE
                showStatus(problemText(problem), R.color.red)
                return
            }

            viewModel.file?.takeIf { it != destination }?.delete()
            viewModel.bitmap?.recycle()
            viewModel.bitmap = null

            viewModel.file = destination
            viewModel.displayName = name
            viewModel.key = ""
            viewModel.status = null

            view?.findViewById<TextView>(R.id.tvFileName)?.text = name
            view?.findViewById<EditText>(R.id.etClave)?.setText("")
            view?.findViewById<View>(R.id.previewCard)?.visibility = View.GONE
            showStatus("", R.color.muted)
        } catch (_: Exception) {
            showStatus(getString(R.string.image_no_file_error), R.color.red)
        }
    }

    private fun decrypt() {
        if (viewModel.loading) return
        val currentView = view ?: return
        val file = viewModel.file
        val key = currentView.findViewById<EditText>(R.id.etClave).text.toString().trim()

        when {
            file == null || !file.exists() ->
                showStatus(getString(R.string.image_no_file_error), R.color.red)
            key.isEmpty() ->
                showStatus(getString(R.string.image_empty_key), R.color.red)
            else -> {
                val problem = FileValidator.problema(file.name, file.length())
                if (problem != null) {
                    showStatus(problemText(problem), R.color.red)
                    return
                }

                viewModel.loading = true
                viewModel.bitmap?.recycle()
                viewModel.bitmap = null
                currentView.findViewById<View>(R.id.previewCard).visibility = View.GONE
                paintLoading(currentView, true)
                showStatus(getString(R.string.image_decrypting), R.color.navy)

                ApiClient().decryptFile(file, key) { result ->
                    if (!isAdded) return@decryptFile
                    when (result) {
                        is ApiResult.Success -> decodeAsync(result.data)
                        is ApiResult.Failure -> {
                            viewModel.loading = false
                            val activeView = view ?: return@decryptFile
                            paintLoading(activeView, false)
                            showStatus(result.error.texto, R.color.red)
                        }
                    }
                }
            }
        }
    }

    /**
     * Descifra en segundo plano: una imagen grande no debe bloquear la interfaz.
     */
    private fun decodeAsync(bytes: ByteArray) {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { ImageDecoder.decode(bytes) }
            viewModel.loading = false

            val activeView = view
            if (activeView == null) {
                bitmap?.recycle()
                return@launch
            }

            paintLoading(activeView, false)
            if (bitmap == null) {
                showStatus(getString(R.string.image_corrupt), R.color.red)
                return@launch
            }

            viewModel.bitmap = bitmap
            activeView.findViewById<ImageView>(R.id.ivResult).setImageBitmap(bitmap)
            val preview = activeView.findViewById<View>(R.id.previewCard)
            preview.visibility = View.VISIBLE
            showStatus(getString(R.string.image_integrity_ok), R.color.green)
            reveal(activeView, preview)
        }
    }

    /**
     * Desplaza lo justo para que la vista previa quede a la vista, sin perder la cabecera.
     */
    private fun reveal(view: View, target: View) {
        val scroll = view as? ScrollView ?: return
        scroll.post {
            val margin = resources.getDimensionPixelSize(R.dimen.screen_padding_vertical)
            val bottom = target.top + target.height + margin - scroll.height
            val limit = bottom.coerceIn(0, maxOf(0, target.top))
            if (bottom > scroll.scrollY) scroll.smoothScrollTo(0, limit)
        }
    }

    private fun problemText(problem: ProblemaArchivo): String = when (problem) {
        ProblemaArchivo.VACIO -> getString(R.string.image_empty_file)
        ProblemaArchivo.DEMASIADO_GRANDE -> getString(R.string.image_too_large)
        ProblemaArchivo.EXTENSION -> getString(R.string.image_wrong_extension)
    }

    private fun paintLoading(view: View, loading: Boolean) {
        val button = view.findViewById<MaterialButton>(R.id.btnDecryptImage)
        button.isEnabled = !loading
        button.text = getString(
            if (loading) R.string.image_processing else R.string.btn_decrypt_image,
        )
    }

    private fun displayName(uri: Uri): String? = try {
        requireContext().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
    } catch (_: Exception) {
        null
    }

    private fun showStatus(text: String, colorRes: Int) {
        viewModel.status = text
        viewModel.statusColorRes = colorRes
        val currentView = view ?: return
        val status = currentView.findViewById<TextView>(R.id.tvStatus)
        status.text = text
        status.setTextColor(ContextCompat.getColor(currentView.context, colorRes))
    }

    companion object {
        private const val ARG_PATH = "path"
        private const val ARG_NAME = "name"

        fun newInstance(path: String, name: String): ImageFragment =
            ImageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, path)
                    putString(ARG_NAME, name)
                }
            }
    }
}
