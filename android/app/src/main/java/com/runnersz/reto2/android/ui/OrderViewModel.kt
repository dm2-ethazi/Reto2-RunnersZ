package com.runnersz.reto2.android.ui

import androidx.lifecycle.ViewModel

/**
 * Conserva el resultado de la orden mientras el usuario gira el móvil o cambia de
 * pestaña. El texto y el desplazamiento los restaura el propio [android.widget.EditText]
 * a partir del estado de la vista.
 *
 * @author Jairo
 */
class OrderViewModel : ViewModel() {
    var result: String? = null
    var resultIsError = false
    var loading = false
}
