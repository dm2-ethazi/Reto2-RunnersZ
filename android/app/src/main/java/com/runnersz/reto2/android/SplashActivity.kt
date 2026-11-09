package com.runnersz.reto2.android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla de arranque: muestra el logotipo brevemente y cede el paso a [MainActivity].
 *
 * @author Jairo
 */
class SplashActivity : AppCompatActivity() {

    /**
     * Muestra el logotipo y abre [MainActivity] en cuanto pasa el retardo.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }, 900L)
    }
}
