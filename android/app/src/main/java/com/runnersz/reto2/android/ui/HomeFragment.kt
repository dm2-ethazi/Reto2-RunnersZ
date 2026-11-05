package com.runnersz.reto2.android.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.runnersz.reto2.android.MainActivity
import com.runnersz.reto2.android.R
import com.runnersz.reto2.android.data.ApiClient
import com.runnersz.reto2.android.data.ApiResult

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var checking = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnCheckApi).setOnClickListener { checkApi() }
        view.findViewById<View>(R.id.rowOrder).setOnClickListener {
            (activity as? MainActivity)?.goToOrder()
        }
        view.findViewById<View>(R.id.rowImage).setOnClickListener {
            (activity as? MainActivity)?.goToImage()
        }

        checkApi()
    }

    private fun checkApi() {
        if (checking) return
        val currentView = view ?: return
        val context = context ?: return
        val status = currentView.findViewById<TextView>(R.id.tvStatus)
        val button = currentView.findViewById<View>(R.id.btnCheckApi)

        checking = true
        button.isEnabled = false
        status.text = getString(R.string.home_status_checking)
        status.setTextColor(ContextCompat.getColor(context, R.color.navy))

        ApiClient().checkAuth { result ->
            checking = false
            if (!isAdded) return@checkAuth
            val activeView = view ?: return@checkAuth
            activeView.findViewById<View>(R.id.btnCheckApi).isEnabled = true
            val activeStatus = activeView.findViewById<TextView>(R.id.tvStatus)
            val activeContext = activeView.context

            when (result) {
                is ApiResult.Success -> {
                    activeStatus.text = getString(R.string.home_status_ok)
                    activeStatus.setTextColor(ContextCompat.getColor(activeContext, R.color.green))
                }
                is ApiResult.Failure -> {
                    activeStatus.text = result.error.texto
                    activeStatus.setTextColor(ContextCompat.getColor(activeContext, R.color.red))
                }
            }
        }
    }
}
