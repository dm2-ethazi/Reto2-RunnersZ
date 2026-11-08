package com.runnersz.reto2.android.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import java.io.File

/**
 * Conserva el archivo elegido, la clave introducida, el estado y la imagen descifrada
 * para que nada se pierda al girar o al cambiar de pestaña.
 *
 * La clave se mantiene solo en memoria: nunca se guarda en el [android.os.Bundle], para
 * que no acabe escrita en el estado persistido del proceso.
 *
 * @author Jairo
 */
class ImageViewModel : ViewModel() {

    var file: File? = null
    var displayName: String? = null
    var key: String = ""
    var status: String? = null
    var statusColorRes: Int = 0
    var bitmap: Bitmap? = null
    var loading = false

    /**
     * Libera la imagen descifrada y borra la copia temporal del archivo.
     */
    override fun onCleared() {
        bitmap?.recycle()
        bitmap = null
        file?.delete()
        file = null
        super.onCleared()
    }
}
