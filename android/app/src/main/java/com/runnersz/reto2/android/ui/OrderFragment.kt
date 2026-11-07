package com.runnersz.reto2.android.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
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
 * @author jairoben
 */
class OrderFragment : Fragment(R.layout.fragment_order) {

    private val viewModel: OrderViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTexto = view.findViewById<EditText>(R.id.etTexto)
        val etShift = view.findViewById<EditText>(R.id.etDesplazamiento)
        val button = view.findViewById<MaterialButton>(R.id.btnDecryptOrder)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            (activity as? MainActivity)?.goHome()
        }
        button.setOnClickListener { decrypt(etTexto, etShift, button) }

        viewModel.result?.let { showResult(it, viewModel.resultIsError) }
        if (viewModel.loading) paintLoading(button, true)

        if (savedInstanceState == null) {
            arguments?.getString(ARG_PATH)?.let { path -> loadFileText(etTexto, path) }
        }
    }

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

    private fun presentResult(text: String, isError: Boolean) {
        viewModel.result = text
        viewModel.resultIsError = isError
        showResult(text, isError)
    }

    private fun paintLoading(button: MaterialButton, loading: Boolean) {
        button.isEnabled = !loading
        button.text = getString(
            if (loading) R.string.order_decrypting else R.string.btn_decrypt_order,
        )
    }

    private fun showResult(text: String, isError: Boolean) {
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
        (currentView as? ScrollView)?.post {
            (currentView as? ScrollView)?.smoothScrollTo(0, card.top)
        }
    }

    private fun hideKeyboard() {
        val currentView = view ?: return
        val manager = requireContext().getSystemService<InputMethodManager>() ?: return
        manager.hideSoftInputFromWindow(currentView.windowToken, 0)
    }

    companion object {
        private const val ARG_PATH = "path"
        private const val MAX_TEXT_BYTES = 1024L * 1024L

        fun newInstance(path: String, name: String): OrderFragment =
            OrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, path)
                    putString("name", name)
                }
            }
    }
}
