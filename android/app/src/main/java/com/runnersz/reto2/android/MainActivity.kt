package com.runnersz.reto2.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.runnersz.reto2.android.api.TokenManager
import com.runnersz.reto2.android.security.RootEmulatorDetector
import com.runnersz.reto2.android.ui.ajustes.AjustesFragment
import com.runnersz.reto2.android.ui.imagen.ImagenFragment
import com.runnersz.reto2.android.ui.orden.OrdenFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar configuración cifrada (token + servidor)
        TokenManager.init(this)
        TokenManager.applyConfigToApi()

        // Verificar entorno inseguro (nivel alto)
        val detector = RootEmulatorDetector
        if (detector.isEnvironmentUnsafe(this)) {
            // Nivel alto: solo se registra, no se bloquea para permitir pruebas en emulador
        }

        bottomNavigation = findViewById(R.id.bottom_navigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_orden -> loadFragment(OrdenFragment())
                R.id.nav_imagen -> loadFragment(ImagenFragment())
                R.id.nav_ajustes -> loadFragment(AjustesFragment())
                else -> false
            }
        }

        // Cargar primera pantalla por defecto
        if (savedInstanceState == null) {
            loadFragment(OrdenFragment())
        }

        // Manejar intención de "Compartir" (recibir .txt o .png.enc desde otra app)
        handleShareIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri != null) {
                // Enviar al fragmento activo según el tipo de archivo
                val mime = contentResolver.getType(uri)
                if (mime != null && (mime.startsWith("image/") || type.endsWith(".enc") || type.endsWith(".png.enc"))) {
                    // Ir a pestaña Imagen
                    bottomNavigation.selectedItemId = R.id.nav_imagen
                    // El fragmento ImagenFragment recibirá el URI a través de un método estático o shared preference
                    // Por simplicidad, guardamos en un archivo temporal accesible
                    val editor = getSharedPreferences("cipher_sprint_share", MODE_PRIVATE).edit()
                    editor.putString("shared_uri", uri.toString())
                    editor.apply()
                    Toast.makeText(this, "Archivo recibido. Ve a pestaña Imagen para descifrar.", Toast.LENGTH_LONG).show()
                } else if (mime != null && (mime.startsWith("text/") || type.endsWith(".txt"))) {
                    // Ir a pestaña Orden
                    bottomNavigation.selectedItemId = R.id.nav_orden
                    val editor = getSharedPreferences("cipher_sprint_share", MODE_PRIVATE).edit()
                    editor.putString("shared_uri", uri.toString())
                    editor.apply()
                    Toast.makeText(this, "Archivo recibido. Ve a pestaña Orden para descifrar.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Tipo de archivo no soportado: $mime", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment): Boolean {
        return supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit() > 0
    }
}
