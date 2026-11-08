package com.runnersz.reto2.android.ui

import androidx.lifecycle.ViewModel

/**
 * Evita repetir la comprobación de la API en cada giro cuando ya hay un resultado.
 *
 * @author Jairo
 */
class HomeViewModel : ViewModel() {
    var statusText: String? = null
    var statusColorRes: Int = 0
    var checking = false
}
