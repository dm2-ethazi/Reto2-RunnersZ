package com.runnersz.reto2.android.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.data.ApiClient
import com.runnersz.reto2.android.data.ApiConfig
import com.runnersz.reto2.android.data.ApiResult

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var checking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvServer).text = ApiConfig.BASE_URL
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            (activity as? MainActivity)?.goHome()
        }

        val button = view.findViewById<MaterialButton>(R.id.btnTestConnection)
        button.setOnClickListener {
            if (checking) return@setOnClickListener
            val activeView = view ?: return@setOnClickListener
            val status = activeView.findViewById<TextView>(R.id.tvStatus)
            val activeContext = activeView.context

            checking = true
            button.isEnabled = false
            status.text = getString(R.string.settings_testing)
            status.setTextColor(ContextCompat.getColor(activeContext, R.color.navy))

            ApiClient().checkAuth { result ->
                checking = false
                if (!isAdded) return@checkAuth
                val currentView = view ?: return@checkAuth
                currentView.findViewById<MaterialButton>(R.id.btnTestConnection).isEnabled = true
                val currentStatus = currentView.findViewById<TextView>(R.id.tvStatus)
                val currentContext = currentView.context

                when (result) {
                    is ApiResult.Success -> {
                        currentStatus.text = getString(R.string.settings_ok)
                        currentStatus.setTextColor(
                            ContextCompat.getColor(currentContext, R.color.green),
                        )
                    }
                    is ApiResult.Failure -> {
                        currentStatus.text = result.error.texto
                        currentStatus.setTextColor(
                            ContextCompat.getColor(currentContext, R.color.red),
                        )
                    }
                }
            }
        }
    }
}
