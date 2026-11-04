package com.runnersz.reto2.android.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.data.ApiClient
import com.runnersz.reto2.android.data.ApiResult
import java.io.File
import java.io.FileOutputStream

class ImageFragment : Fragment(R.layout.fragment_image) {

    private var selectedFile: File? = null
    private var isDemo = false

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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

        val path = arguments?.getString(ARG_PATH)
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                selectedFile = file
                isDemo = false
                view.findViewById<TextView>(R.id.tvFileName).text =
                    arguments?.getString(ARG_NAME) ?: file.name
                etClave.setText("")
            } else {
                showStatus(getString(R.string.image_no_file_error), R.color.red)
            }
        } else {
            prepareDemo(view)
        }

        button.setOnClickListener {
            val file = selectedFile
            val key = etClave.text.toString().trim()

            when {
                file == null || !file.exists() ->
                    showStatus(getString(R.string.image_no_file_error), R.color.red)
                key.isEmpty() ->
                    showStatus(getString(R.string.image_empty_key), R.color.red)
                else -> {
                    button.isEnabled = false
                    button.text = getString(R.string.image_processing)
                    showStatus(getString(R.string.image_decrypting), R.color.navy)

                    ApiClient().decryptFile(file, key) { result ->
                        if (!isAdded) return@decryptFile
                        val activeView = view ?: return@decryptFile
                        val activeButton = activeView.findViewById<MaterialButton>(R.id.btnDecryptImage)
                        activeButton.isEnabled = true
                        activeButton.text = getString(R.string.btn_decrypt_image)

                        when (result) {
                            is ApiResult.Success -> {
                                val bitmap = BitmapFactory.decodeByteArray(
                                    result.data,
                                    0,
                                    result.data.size,
                                )
                                if (bitmap == null) {
                                    showStatus(getString(R.string.image_corrupt), R.color.red)
                                } else {
                                    activeView.findViewById<ImageView>(R.id.ivResult)
                                        .setImageBitmap(bitmap)
                                    activeView.findViewById<View>(R.id.previewCard).visibility =
                                        View.VISIBLE
                                    showStatus(
                                        getString(R.string.image_integrity_ok),
                                        R.color.green,
                                    )
                                }
                            }
                            is ApiResult.Failure ->
                                showStatus(result.error.texto, R.color.red)
                        }
                    }
                }
            }
        }
    }

    private fun prepareDemo(view: View) {
        val destination = File(requireContext().cacheDir, "image.png.enc")
        try {
            if (!destination.exists()) {
                requireContext().assets.open(DEMO_ASSET).use { input ->
                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                }
            }
            selectedFile = destination
            isDemo = true
            view.findViewById<TextView>(R.id.tvFileName).text =
                getString(R.string.image_demo_file)
            view.findViewById<EditText>(R.id.etClave).setText(DEMO_KEY)
        } catch (_: Exception) {
            showStatus(getString(R.string.image_no_file_error), R.color.red)
        }
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

            selectedFile?.takeIf { !isDemo && it != destination }?.delete()
            selectedFile = destination
            isDemo = false
            view?.findViewById<TextView>(R.id.tvFileName)?.text = name
            view?.findViewById<EditText>(R.id.etClave)?.setText("")
            showStatus("", R.color.muted)
        } catch (_: Exception) {
            showStatus(getString(R.string.image_no_file_error), R.color.red)
        }
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

    private fun showStatus(text: String, color: Int) {
        val currentView = view ?: return
        val status = currentView.findViewById<TextView>(R.id.tvStatus)
        status.text = text
        status.setTextColor(ContextCompat.getColor(currentView.context, color))
    }

    override fun onDestroyView() {
        if (!isDemo) selectedFile?.delete()
        selectedFile = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_PATH = "path"
        private const val ARG_NAME = "name"
        private const val DEMO_ASSET = "demo/image.png.enc"
        private const val DEMO_KEY = "RunnersZ-Demo-2026"

        fun newInstance(path: String, name: String): ImageFragment =
            ImageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, path)
                    putString(ARG_NAME, name)
                }
            }
    }
}
