package com.runnersz.reto2.android.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.data.ApiClient
import com.runnersz.reto2.android.data.ApiResult
import com.runnersz.reto2.android.domain.ShiftValidator
import java.io.File

/**
 * @author jairoben
 */
class OrderFragment : Fragment(R.layout.fragment_order) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTexto = view.findViewById<EditText>(R.id.etTexto)
        val etShift = view.findViewById<EditText>(R.id.etDesplazamiento)
        val button = view.findViewById<MaterialButton>(R.id.btnDecryptOrder)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            (activity as? MainActivity)?.goHome()
        }

        arguments?.getString(ARG_PATH)?.let { path ->
            val file = File(path)
            if (file.exists() && file.length() <= MAX_TEXT_BYTES) {
                Thread {
                    val content = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
                    activity?.runOnUiThread {
                        if (isAdded) {
                            if (content != null) {
                                etTexto.setText(content)
                            } else {
                                showResult(getString(R.string.order_empty_text), true)
                            }
                        }
                    }
                }.start()
            } else if (file.exists()) {
                showResult(getString(R.string.order_file_too_large), true)
            }
        }

        button.setOnClickListener {
            val text = etTexto.text.toString()
            val shift = etShift.text.toString().trim().toIntOrNull()

            when {
                text.isBlank() -> showResult(getString(R.string.order_empty_text), true)
                shift == null || !ShiftValidator.esValido(shift) ->
                    showResult(ShiftValidator.MENSAJE, true)
                else -> {
                    button.isEnabled = false
                    button.text = getString(R.string.order_decrypting)

                    ApiClient().caesarDecrypt(text, shift) { result ->
                        if (!isAdded) return@caesarDecrypt
                        val activeView = view ?: return@caesarDecrypt
                        val activeButton = activeView.findViewById<MaterialButton>(R.id.btnDecryptOrder)
                        activeButton.isEnabled = true
                        activeButton.text = getString(R.string.btn_decrypt_order)

                        when (result) {
                            is ApiResult.Success -> showResult(result.data, false)
                            is ApiResult.Failure -> showResult(result.error.texto, true)
                        }
                    }
                }
            }
        }
    }

    private fun showResult(text: String, isError: Boolean) {
        val currentView = view ?: return
        currentView.findViewById<View>(R.id.resultCard).visibility = View.VISIBLE
        val result = currentView.findViewById<TextView>(R.id.tvResult)
        result.text = text
        result.setTextColor(
            ContextCompat.getColor(
                currentView.context,
                if (isError) R.color.red else R.color.navy,
            ),
        )
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
