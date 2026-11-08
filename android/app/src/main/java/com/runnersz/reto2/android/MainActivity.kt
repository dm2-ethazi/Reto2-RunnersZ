package com.runnersz.reto2.android

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.runnersz.reto2.android.data.ApiConfig
import com.runnersz.reto2.android.ui.HomeFragment
import com.runnersz.reto2.android.ui.ImageFragment
import com.runnersz.reto2.android.ui.OrderFragment
import java.io.File
import java.io.FileOutputStream

/** Actividad única de la app: aloja las tres pestañas, pinta la barra inferior y
 * reparte los archivos que llegan por *Abrir con* o *Compartir*.
 *
 * @author Jairo
 */
class MainActivity : AppCompatActivity() {

    private enum class Tab { INICIO, ORDEN, IMAGEN }

    private var currentTab = Tab.INICIO

    /**
     * Prepara la barra inferior, los márgenes del sistema y la pestaña inicial.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        applyInsets()
        setupNavigation()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            /**
             * Vuelve a Inicio antes de dejar que el sistema cierre la actividad.
             */
            override fun handleOnBackPressed() {
                if (currentTab != Tab.INICIO) {
                    goHome()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState == null) {
            cleanTemporaryFiles()
            showTab(Tab.INICIO)
            handleIncomingIntent(intent)
        } else {
            currentTab = Tab.entries.getOrElse(savedInstanceState.getInt(KEY_TAB)) { Tab.INICIO }
            restoreTabs()
        }
    }

    /**
     * Atiende un archivo que llega mientras la app ya está abierta.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Guarda la pestaña activa para restaurarla tras un giro o un cierre del proceso.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_TAB, currentTab.ordinal)
        super.onSaveInstanceState(outState)
    }

/**
 * Muestra la pestaña Inicio.
 */
    fun goHome() = showTab(Tab.INICIO)

/**
 * Muestra la pestaña Orden.
 */
    fun goToOrder() = showTab(Tab.ORDEN)

/**
 * Muestra la pestaña Imagen.
 */
    fun goToImage() = showTab(Tab.IMAGEN)

/**
 * Conecta los tres botones de la barra inferior con su pestaña.
 */
    private fun setupNavigation() {
        findViewById<View>(R.id.navInicio).setOnClickListener { showTab(Tab.INICIO) }
        findViewById<View>(R.id.navOrden).setOnClickListener { showTab(Tab.ORDEN) }
        findViewById<View>(R.id.navImagen).setOnClickListener { showTab(Tab.IMAGEN) }
    }

    /**
     * Mantiene una única instancia por pestaña con [androidx.fragment.app.FragmentTransaction.show]
     * y [androidx.fragment.app.FragmentTransaction.hide], de forma que el texto escrito, el
     * resultado de una orden y la imagen descifrada sobrevivan al cambio de pestaña.
     */
    private fun showTab(tab: Tab, incoming: Fragment? = null) {
        val manager = supportFragmentManager
        val existing = manager.findFragmentByTag(tab.name)

        if (incoming == null && tab == currentTab && existing != null) {
            paintNavigation(tab)
            return
        }

        currentTab = tab
        val transaction = manager.beginTransaction()
        Tab.entries.forEach { other ->
            if (other != tab) manager.findFragmentByTag(other.name)?.let(transaction::hide)
        }

        when {
            incoming != null -> {
                existing?.let(transaction::remove)
                transaction.add(R.id.contenedor, incoming, tab.name)
            }
            existing != null -> transaction.show(existing)
            else -> transaction.add(R.id.contenedor, createFragment(tab), tab.name)
        }

        transaction.commit()
        paintNavigation(tab)
    }

    /**
     * Tras un giro o una recreación, el FragmentManager devuelve las pestañas ya creadas:
     * solo hay que dejar visible la que estaba activa.
     */
    private fun restoreTabs() {
        val manager = supportFragmentManager
        var restored = false
        manager.beginTransaction().apply {
            Tab.entries.forEach { tab ->
                val fragment = manager.findFragmentByTag(tab.name) ?: return@forEach
                restored = true
                if (tab == currentTab) show(fragment) else hide(fragment)
            }
            commitNow()
        }

        if (!restored) {
            showTab(currentTab)
        } else {
            paintNavigation(currentTab)
        }
    }

/**
 * Crea el fragmento que corresponde a la pestaña indicada.
 *
 *  * @param tab pestaña que se quiere mostrar
 */
    private fun createFragment(tab: Tab): Fragment = when (tab) {
        Tab.INICIO -> HomeFragment()
        Tab.ORDEN -> OrderFragment()
        Tab.IMAGEN -> ImageFragment()
    }

/**
 * Colorea la barra inferior según la pestaña activa.
 */
    private fun paintNavigation(tab: Tab) {
        setNavItem(R.id.navInicio, R.id.navInicioIcon, R.id.navInicioLabel, tab == Tab.INICIO)
        setNavItem(R.id.navOrden, R.id.navOrdenIcon, R.id.navOrdenLabel, tab == Tab.ORDEN)
        setNavItem(R.id.navImagen, R.id.navImagenIcon, R.id.navImagenLabel, tab == Tab.IMAGEN)
    }

/**
 * Marca un elemento de la barra inferior como activo o inactivo.
 *
 *  * @param itemId contenedor pulsable del elemento
 *  * @param iconId icono que cambia de color
 *  * @param labelId texto que cambia de color
 *  * @param active `true` si la pestaña está visible
 */
    private fun setNavItem(itemId: Int, iconId: Int, labelId: Int, active: Boolean) {
        val color = ContextCompat.getColor(
            this,
            if (active) R.color.gold else R.color.nav_inactive,
        )
        findViewById<ImageView>(iconId).setColorFilter(color)
        findViewById<TextView>(labelId).setTextColor(color)
        findViewById<View>(itemId).isSelected = active
    }

/**
 * Aplica los márgenes de las barras del sistema y del recorte de pantalla a la cabecera,
 *  * el contenido y la navegación inferior. En horizontal oculta el subtítulo para dejar
 *  * más sitio al contenido.
 */
    private fun applyInsets() {
        val header = findViewById<View>(R.id.header)
        val bottomNav = findViewById<View>(R.id.bottomNav)
        val content = findViewById<View>(R.id.contenedor)
        val subtitle = findViewById<View>(R.id.headerSubtitle)
        val horizontal = resources.getDimensionPixelSize(R.dimen.header_padding_horizontal)
        val vertical = resources.getDimensionPixelSize(R.dimen.header_padding_vertical)

        subtitle.visibility = if (
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        ) {
            View.GONE
        } else {
            View.VISIBLE
        }

        ViewCompat.setOnApplyWindowInsetsListener(header) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout(),
            )
            view.updatePadding(
                left = horizontal + bars.left,
                top = vertical + bars.top,
                right = horizontal + bars.right,
                bottom = vertical,
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout(),
            )
            view.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = bars.bottom,
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout(),
            )
            view.updatePadding(left = bars.left, right = bars.right)
            insets
        }
    }

/**
 * Reparte un archivo recibido desde otra aplicación: lo copia al caché y abre la
 *  * pestaña de imagen u orden según su tipo.
 */
    private fun handleIncomingIntent(intent: Intent?) {
        val uri = when (intent?.action) {
            Intent.ACTION_SEND -> streamUri(intent)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        } ?: return

        val name = displayName(uri) ?: uri.lastPathSegment ?: "archivo"
        val mime = contentResolver.getType(uri).orEmpty()
        val local = copyToCache(uri, name)
        if (local == null) {
            Toast.makeText(this, R.string.share_copy_failed, Toast.LENGTH_LONG).show()
            return
        }

        val isImage = mime.startsWith("image/") || name.endsWith(".enc", ignoreCase = true)
        if (isImage) {
            showTab(Tab.IMAGEN, ImageFragment.newInstance(local.absolutePath, name))
        } else {
            showTab(Tab.ORDEN, OrderFragment.newInstance(local.absolutePath, name))
        }
    }

/**
 * Lee el `Uri` del archivo adjunto, con la API moderna desde Android 13.
 */
    @Suppress("DEPRECATION")
    private fun streamUri(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

/**
 * Consulta el nombre visible del archivo en el proveedor de contenido.
 *
 *  * @return el nombre, o `null` si el proveedor no lo expone
 */
    private fun displayName(uri: Uri): String? = try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
    } catch (_: Exception) {
        null
    }

/**
 * Copia al caché el archivo recibido y descarta los que superan el límite de subida.
 *
 *  * @param uri origen del archivo
 *  * @param name nombre original, que se conserva en la copia
 *  * @return la copia local, o `null` si no se pudo leer o es demasiado grande
 */
    private fun copyToCache(uri: Uri, name: String): File? {
        return try {
            val safeName = File(name).name.ifBlank { "archivo" }
            val destination = File(cacheDir, "in-${System.currentTimeMillis()}-$safeName")
            val stream = contentResolver.openInputStream(uri) ?: return null
            stream.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }

            if (destination.length() > ApiConfig.MAX_UPLOAD_BYTES) {
                destination.delete()
                null
            } else {
                destination
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Borra las copias temporales de archivos compartidos que quedaron de sesiones
     * anteriores. Solo se ejecuta con un arranque limpio, nunca tras un giro, para no
     * eliminar el archivo que una pestaña restaurada todavía está usando.
     */
    private fun cleanTemporaryFiles() {
        val stale = cacheDir.listFiles() ?: return
        stale.forEach { file ->
            if (file.name.startsWith("in-") || file.name.startsWith("upload-")) {
                file.delete()
            }
        }
    }

    companion object {
        private const val KEY_TAB = "tab_actual"
    }
}
