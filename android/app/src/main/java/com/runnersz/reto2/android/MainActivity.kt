package com.runnersz.reto2.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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

class MainActivity : AppCompatActivity() {

    private enum class Tab { INICIO, ORDEN, IMAGEN }

    private var currentTab = Tab.INICIO

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
            showTab(Tab.INICIO)
            handleIncomingIntent(intent)
        } else {
            currentTab = Tab.entries.getOrElse(savedInstanceState.getInt(KEY_TAB)) { Tab.INICIO }
            paintNavigation(currentTab)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_TAB, currentTab.ordinal)
        super.onSaveInstanceState(outState)
    }

    fun goHome() = showTab(Tab.INICIO)

    fun goToOrder() = showTab(Tab.ORDEN)

    fun goToImage() = showTab(Tab.IMAGEN)

    private fun setupNavigation() {
        findViewById<View>(R.id.navInicio).setOnClickListener { showTab(Tab.INICIO) }
        findViewById<View>(R.id.navOrden).setOnClickListener { showTab(Tab.ORDEN) }
        findViewById<View>(R.id.navImagen).setOnClickListener { showTab(Tab.IMAGEN) }
    }

    private fun showTab(tab: Tab, fragment: Fragment = createFragment(tab)) {
        currentTab = tab
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment, tab.name)
            .commit()
        paintNavigation(tab)
    }

    private fun createFragment(tab: Tab): Fragment = when (tab) {
        Tab.INICIO -> HomeFragment()
        Tab.ORDEN -> OrderFragment()
        Tab.IMAGEN -> ImageFragment()
    }

    private fun paintNavigation(tab: Tab) {
        setNavItem(R.id.navInicio, R.id.navInicioIcon, R.id.navInicioLabel, tab == Tab.INICIO)
        setNavItem(R.id.navOrden, R.id.navOrdenIcon, R.id.navOrdenLabel, tab == Tab.ORDEN)
        setNavItem(R.id.navImagen, R.id.navImagenIcon, R.id.navImagenLabel, tab == Tab.IMAGEN)
    }

    private fun setNavItem(itemId: Int, iconId: Int, labelId: Int, active: Boolean) {
        val color = ContextCompat.getColor(
            this,
            if (active) R.color.gold else R.color.nav_inactive,
        )
        findViewById<ImageView>(iconId).setColorFilter(color)
        findViewById<TextView>(labelId).setTextColor(color)
        findViewById<View>(itemId).isSelected = active
    }

    private fun applyInsets() {
        val header = findViewById<View>(R.id.header)
        val bottomNav = findViewById<View>(R.id.bottomNav)

        ViewCompat.setOnApplyWindowInsetsListener(header) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(
                left = dp(20),
                top = bars.top + dp(16),
                right = dp(20),
                bottom = dp(16),
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri = when (intent?.action) {
            Intent.ACTION_SEND -> streamUri(intent)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        } ?: return

        val name = displayName(uri) ?: uri.lastPathSegment ?: "archivo"
        val mime = contentResolver.getType(uri).orEmpty()
        val local = copyToCache(uri, name) ?: return

        val isImage = mime.startsWith("image/") || name.endsWith(".enc", ignoreCase = true)
        if (isImage) {
            showTab(Tab.IMAGEN, ImageFragment.newInstance(local.absolutePath, name))
        } else {
            showTab(Tab.ORDEN, OrderFragment.newInstance(local.absolutePath, name))
        }
    }

    @Suppress("DEPRECATION")
    private fun streamUri(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val KEY_TAB = "tab_actual"
    }
}
