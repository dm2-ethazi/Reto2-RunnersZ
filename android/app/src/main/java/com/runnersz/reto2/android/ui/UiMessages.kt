package com.runnersz.reto2.android.ui

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Avisos a la persona usuaria (Toast/Snackbar), centralizados para mantener un tono común
 * y para poder sustituirlos en I-15 (diálogos y accesibilidad) sin tocar el resto.
 */
object UiMessages {

    /** Aviso breve y no bloqueante. */
    fun toast(frag: Fragment, texto: String) {
        Toast.makeText(frag.requireContext(), texto, Toast.LENGTH_SHORT).show()
    }

    /**
     * Aviso con acción opcional (p. ej. «Ir a Ajustes» o «Reintentar»), según §5.1.
     */
    fun aviso(frag: Fragment, texto: String, accion: String? = null, alPulsar: (() -> Unit)? = null) {
        val vista = frag.view ?: return
        val barra = Snackbar.make(vista, texto, if (accion == null) Snackbar.LENGTH_LONG else Snackbar.LENGTH_INDEFINITE)
        if (accion != null && alPulsar != null) {
            barra.setAction(accion) { alPulsar() }
        }
        barra.show()
    }
}
