package com.runnersz.reto2.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.data.ApiClient
import com.runnersz.reto2.android.data.ApiResult
import com.runnersz.reto2.android.domain.ShiftValidator
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pestaña Orden: descifrado César de 1 a 25 contra la API y copia del resultado al tocarlo.
 *
 * @author Jairo
 */
class OrderFragment : Fragment(R.layout.fragment_order) {

    private val viewModel: OrderViewModel by viewModels()

    /**
     * Conecta el formulario, restaura el último resultado y carga el archivo recibido.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTexto = view.findViewById<EditText>(R.id.etTexto)
        val etShift = view.findViewById<EditText>(R.id.etDesplazamiento)
        val button = view.findViewById<MaterialButton>(R.id.btnDecryptOrder)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            (activity as? MainActivity)?.goHome()
        }
        button.setOnClickListener { decrypt(etTexto, etShift, button) }

        viewModel.result?.let { showResult(it, viewModel.resultIsError, reveal = false) }
        if (viewModel.loading) paintLoading(button, true)

        if (savedInstanceState == null) {
            arguments?.getString(ARG_PATH)?.let { path -> loadFileText(etTexto, path) }
        }
    }

    /**
     * Carga en el campo el texto de un archivo recibido desde otra aplicación.
     *
     * @param etTexto campo donde se escribe
     * @param path ruta de la copia en el caché
     */
    private fun loadFileText(etTexto: EditText, path: String) {
        val file = File(path)
        when {
            !file.exists() -> showResult(getString(R.string.order_empty_text), true)
            file.length() > MAX_TEXT_BYTES ->
                showResult(getString(R.string.order_file_too_large), true)
            else -> lifecycleScope.launch {
                val content = withContext(Dispatchers.IO) {
                    runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
                }
                if (!isAdded) return@launch
                if (content != null) {
                    etTexto.setText(content)
                } else {
                    showResult(getString(R.string.order_empty_text), true)
                }
            }
        }
    }

    /**
     * Valida el texto y el desplazamiento y pide el descifrado a la API.
     */
    private fun decrypt(etTexto: EditText, etShift: EditText, button: MaterialButton) {
        if (viewModel.loading) return

        val text = etTexto.text.toString()
        val shift = etShift.text.toString().trim().toIntOrNull()

        when {
            text.isBlank() -> showResult(getString(R.string.order_empty_text), true)
            shift == null || !ShiftValidator.esValido(shift) -> showResult(ShiftValidator.MENSAJE, true)
            else -> {
                hideKeyboard()
                viewModel.loading = true
                viewModel.result = null
                paintLoading(button, true)

                ApiClient().caesarDecrypt(text, shift) { result ->
                    if (!isAdded) return@caesarDecrypt
                    viewModel.loading = false
                    val activeView = view ?: return@caesarDecrypt
                    paintLoading(activeView.findViewById(R.id.btnDecryptOrder), false)

                    when (result) {
                        is ApiResult.Success -> presentResult(result.data, false)
                        is ApiResult.Failure -> presentResult(result.error.texto, true)
                    }
                }
            }
        }
    }

    /**
     * Guarda el resultado en el ViewModel y lo muestra.
     *
     * @param text resultado o mensaje de error
     * @param isError `true` si el texto es un error
     */
    private fun presentResult(text: String, isError: Boolean) {
        viewModel.result = text
        viewModel.resultIsError = isError
        showResult(text, isError)
    }

    /**
     * Bloquea el botón y cambia su texto mientras se descifra.
     */
    private fun paintLoading(button: MaterialButton, loading: Boolean) {
        button.isEnabled = !loading
        button.text = getString(
            if (loading) R.string.order_decrypting else R.string.btn_decrypt_order,
        )
    }

    /**
     * Pinta la tarjeta de resultado y la colorea según sea error o no.
     *
     * @param text contenido a mostrar
     * @param isError `true` si es un error
     * @param reveal `true` para desplazar la tarjeta hasta la vista
     */
    private fun showResult(text: String, isError: Boolean, reveal: Boolean = true) {
        val currentView = view ?: return
        val card = currentView.findViewById<View>(R.id.resultCard)
        card.visibility = View.VISIBLE
        val result = currentView.findViewById<TextView>(R.id.tvResult)
        result.text = text
        result.setTextColor(
            ContextCompat.getColor(
                currentView.context,
                if (isError) R.color.red else R.color.navy,
            ),
        )
        card.isClickable = !isError
        card.isFocusable = !isError
        card.setOnClickListener { copyResult() }
        val scroll = currentView as? ScrollView ?: return
        if (!reveal) return
        scroll.post {
            val margin = resources.getDimensionPixelSize(R.dimen.screen_padding_vertical)
            val bottom = card.top + card.height + margin - scroll.height
            val target = bottom.coerceIn(0, maxOf(0, card.top))
            if (bottom > scroll.scrollY) scroll.smoothScrollTo(0, target)
        }
    }

    /**
     * Copia el resultado al portapapeles. Desde Android 13 el sistema ya muestra
     * su propio aviso, así que el Toast solo se usa en versiones anteriores.
     */
    private fun copyResult() {
        val currentView = view ?: return
        val text = currentView.findViewById<TextView>(R.id.tvResult).text?.toString().orEmpty()
        if (viewModel.resultIsError || text.isBlank()) return
        val clipboard = requireContext().getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(requireContext(), R.string.result_copied, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Oculta el teclado en pantalla antes de lanzar la petición.
     */
    private fun hideKeyboard() {
        val currentView = view ?: return
        val manager = requireContext().getSystemService<InputMethodManager>() ?: return
        manager.hideSoftInputFromWindow(currentView.windowToken, 0)
    }

    companion object {
        private const val ARG_PATH = "path"
        private const val MAX_TEXT_BYTES = 1024L * 1024L

        /**
         * Crea el fragmento con el archivo de texto recibido desde otra aplicación.
         *
         * @param path ruta de la copia en el caché
         * @param name nombre original del archivo
         */
        fun newInstance(path: String, name: String): OrderFragment =
            OrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, path)
                    putString("name", name)
                }
            }
    }
}
