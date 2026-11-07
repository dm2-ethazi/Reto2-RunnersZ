package com.runnersz.reto2.android.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.data.ApiClient
import com.runnersz.reto2.android.data.ApiResult

/**
 * @author jairoben
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnCheckApi).setOnClickListener { checkApi() }
        view.findViewById<View>(R.id.rowOrder).setOnClickListener {
            (activity as? MainActivity)?.goToOrder()
        }
        view.findViewById<View>(R.id.rowImage).setOnClickListener {
            (activity as? MainActivity)?.goToImage()
        }

        restoreStatus(view)
        if (viewModel.checking) {
            paintLoading(view, true)
        } else if (viewModel.statusText == null) {
            checkApi()
        }
    }

    private fun restoreStatus(view: View) {
        val text = viewModel.statusText ?: return
        val status = view.findViewById<TextView>(R.id.tvStatus)
        status.text = text
        status.setTextColor(ContextCompat.getColor(requireContext(), viewModel.statusColorRes))
    }

    private fun checkApi() {
        if (viewModel.checking) return
        val currentView = view ?: return

        viewModel.checking = true
        paintLoading(currentView, true)
        val status = currentView.findViewById<TextView>(R.id.tvStatus)
        status.text = getString(R.string.home_status_checking)
        status.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy))

        ApiClient().checkAuth { result ->
            if (!isAdded) return@checkAuth
            viewModel.checking = false
            val activeView = view ?: return@checkAuth
            paintLoading(activeView, false)

            when (result) {
                is ApiResult.Success -> {
                    viewModel.statusText = getString(R.string.home_status_ok)
                    viewModel.statusColorRes = R.color.green
                }
                is ApiResult.Failure -> {
                    viewModel.statusText = result.error.texto
                    viewModel.statusColorRes = R.color.red
                }
            }

            val activeStatus = activeView.findViewById<TextView>(R.id.tvStatus)
            activeStatus.text = viewModel.statusText
            activeStatus.setTextColor(
                ContextCompat.getColor(activeView.context, viewModel.statusColorRes),
            )
        }
    }

    private fun paintLoading(view: View, loading: Boolean) {
        val button = view.findViewById<MaterialButton>(R.id.btnCheckApi)
        button.isEnabled = !loading
        button.text = getString(
            if (loading) R.string.btn_test_api_loading else R.string.btn_test_api,
        )
    }
}
