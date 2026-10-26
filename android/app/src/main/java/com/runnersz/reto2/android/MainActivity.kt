package com.runnersz.reto2.android

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.runnersz.reto2.android.core.AesFileDecryptor
import com.runnersz.reto2.android.core.BattleOrderService
import com.runnersz.reto2.android.core.EmptyAesPasswordException
import com.runnersz.reto2.android.core.SharedFileKind
import com.runnersz.reto2.android.core.SharedFileKinds
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val decryptor = AesFileDecryptor()
    private val battleOrders = BattleOrderService()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var imageScreen: View
    private lateinit var orderScreen: View
    private lateinit var navImage: Button
    private lateinit var navOrder: Button
    private lateinit var imagePickButton: Button
    private lateinit var imageDecryptButton: Button
    private lateinit var imageFileLabel: TextView
    private lateinit var aesPassword: EditText
    private lateinit var imageProgress: ProgressBar
    private lateinit var imageStatus: TextView
    private lateinit var imageView: ImageView
    private lateinit var orderPickButton: Button
    private lateinit var orderDecryptButton: Button
    private lateinit var orderFileLabel: TextView
    private lateinit var shiftInput: EditText
    private lateinit var orderProgress: ProgressBar
    private lateinit var orderStatus: TextView
    private lateinit var orderText: TextView

    private var imageUri: Uri? = null
    private var orderUri: Uri? = null
    private var imageTempOut: File? = null
    private var imageBusy = false
    private var orderBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        configureListeners()
        showImageScreen()
        handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun bindViews() {
        imageScreen = findViewById(R.id.imageScreen)
        orderScreen = findViewById(R.id.orderScreen)
        navImage = findViewById(R.id.navImage)
        navOrder = findViewById(R.id.navOrder)
        imagePickButton = findViewById(R.id.imagePickButton)
        imageDecryptButton = findViewById(R.id.imageDecryptButton)
        imageFileLabel = findViewById(R.id.imageFileLabel)
        aesPassword = findViewById(R.id.aesPassword)
        imageProgress = findViewById(R.id.imageProgress)
        imageStatus = findViewById(R.id.imageStatus)
        imageView = findViewById(R.id.imageView)
        orderPickButton = findViewById(R.id.orderPickButton)
        orderDecryptButton = findViewById(R.id.orderDecryptButton)
        orderFileLabel = findViewById(R.id.orderFileLabel)
        shiftInput = findViewById(R.id.shiftInput)
        orderProgress = findViewById(R.id.orderProgress)
        orderStatus = findViewById(R.id.orderStatus)
        orderText = findViewById(R.id.orderText)
    }

    private fun configureListeners() {
        navImage.setOnClickListener { showImageScreen() }
        navOrder.setOnClickListener { showOrderScreen() }
        imagePickButton.setOnClickListener { pickImage() }
        orderPickButton.setOnClickListener { pickOrder() }
        imageDecryptButton.setOnClickListener { processImage() }
        orderDecryptButton.setOnClickListener { processOrder() }
    }

    private fun showImageScreen() {
        imageScreen.visibility = View.VISIBLE
        orderScreen.visibility = View.GONE
        styleNavigation(navImage, selected = true)
        styleNavigation(navOrder, selected = false)
    }

    private fun showOrderScreen() {
        imageScreen.visibility = View.GONE
        orderScreen.visibility = View.VISIBLE
        styleNavigation(navImage, selected = false)
        styleNavigation(navOrder, selected = true)
    }

    private fun styleNavigation(button: Button, selected: Boolean) {
        button.setBackgroundColor(getColor(if (selected) R.color.gold else R.color.garnet))
        button.setTextColor(getColor(if (selected) R.color.ink else R.color.cream))
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("image/png", "application/octet-stream"),
            )
        }
        startActivityForResult(intent, REQUEST_IMAGE)
    }

    private fun pickOrder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("text/plain", "application/octet-stream"),
            )
        }
        startActivityForResult(intent, REQUEST_ORDER)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (exception: SecurityException) {
            Toast.makeText(this, "El fichero se leerá solo durante esta sesión.", Toast.LENGTH_SHORT)
                .show()
        }
        val name = queryDisplayName(uri)
        when (requestCode) {
            REQUEST_IMAGE -> acceptImage(uri, name)
            REQUEST_ORDER -> acceptOrder(uri, name)
        }
    }

    private fun handleIncoming(intent: Intent?) {
        if (intent == null) {
            return
        }
        val uri =
            when (intent.action) {
                Intent.ACTION_SEND -> sharedUri(intent)
                Intent.ACTION_VIEW -> intent.data
                else -> null
            } ?: return
        val name = queryDisplayName(uri)
        when (SharedFileKinds.detect(name, intent.type)) {
            SharedFileKind.ORDER -> acceptOrder(uri, name)
            SharedFileKind.IMAGE -> acceptImage(uri, name)
            SharedFileKind.UNKNOWN ->
                if (SharedFileKinds.isEncryptedName(name)) {
                    acceptImage(uri, name)
                } else {
                    Toast.makeText(
                        this,
                        "Formato no soportado. Se esperan .png, .png.enc, .txt o .txt.enc.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }

    private fun sharedUri(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    private fun acceptImage(uri: Uri, name: String) {
        imageUri = uri
        imageFileLabel.text = name
        imageTempOut?.delete()
        imageTempOut = null
        imageView.setImageDrawable(null)
        imageStatus.setTextColor(getColor(R.color.muted))
        imageStatus.text =
            if (SharedFileKinds.isEncryptedName(name)) {
                "Fichero cifrado listo. Introduce la clave AES y pulsa Mostrar / Descifrar."
            } else {
                "Fichero listo. Pulsa Mostrar / Descifrar."
            }
        showImageScreen()
    }

    private fun acceptOrder(uri: Uri, name: String) {
        orderUri = uri
        orderFileLabel.text = name
        orderText.text = ""
        orderStatus.setTextColor(getColor(R.color.muted))
        orderStatus.text = "Fichero listo. Introduce el desplazamiento y pulsa Descifrar orden."
        showOrderScreen()
    }

    private fun processImage() {
        if (imageBusy) {
            return
        }
        val uri = imageUri
        if (uri == null) {
            Toast.makeText(this, "Selecciona primero una imagen.", Toast.LENGTH_SHORT).show()
            return
        }
        val password = aesPassword.text.toString()
        imageBusy = true
        imageProgress.visibility = View.VISIBLE
        imageDecryptButton.isEnabled = false
        imageStatus.setTextColor(getColor(R.color.muted))
        imageStatus.text = "Procesando imagen…"

        executor.execute {
            var input: File? = null
            var output: File? = null
            try {
                input = copyToCache(uri, "reto2_image_in", ".bin")
                val encrypted = SharedFileKinds.hasEncMagic(input)
                val bitmap: Bitmap
                if (encrypted) {
                    if (password.isEmpty()) {
                        throw EmptyAesPasswordException()
                    }
                    output = File(cacheDir, "reto2_image_out_${java.util.UUID.randomUUID()}.png")
                    decryptor.decrypt(input, output, password)
                    bitmap =
                        BitmapFactory.decodeFile(output.absolutePath)
                            ?: throw IOException(
                                "El contenido descifrado no es una imagen PNG válida.",
                            )
                } else {
                    bitmap =
                        BitmapFactory.decodeFile(input.absolutePath)
                            ?: throw IOException("El fichero no es una imagen válida.")
                }
                val keptOutput = if (encrypted) output else null
                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        keptOutput?.delete()
                        return@runOnUiThread
                    }
                    imageTempOut?.delete()
                    imageTempOut = keptOutput
                    imageView.setImageBitmap(bitmap)
                    imageStatus.setTextColor(getColor(R.color.success))
                    imageStatus.text =
                        if (encrypted) {
                            "Imagen descifrada. Integridad SHA-256 verificada."
                        } else {
                            "Imagen cargada sin cifrado."
                        }
                    finishImageWork()
                }
            } catch (exception: Exception) {
                output?.delete()
                val message = exception.message ?: "No se pudo procesar la imagen."
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        imageStatus.setTextColor(getColor(R.color.error))
                        imageStatus.text = message
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        finishImageWork()
                    }
                }
            } finally {
                input?.delete()
            }
        }
    }

    private fun processOrder() {
        if (orderBusy) {
            return
        }
        val uri = orderUri
        if (uri == null) {
            Toast.makeText(this, "Selecciona primero una orden.", Toast.LENGTH_SHORT).show()
            return
        }
        val shift = shiftInput.text.toString().trim().toIntOrNull()
        if (shift == null || shift !in 1..25) {
            shiftInput.error = "Introduce un desplazamiento entre 1 y 25."
            Toast.makeText(this, "Desplazamiento no válido.", Toast.LENGTH_SHORT).show()
            return
        }
        orderBusy = true
        orderProgress.visibility = View.VISIBLE
        orderDecryptButton.isEnabled = false
        orderStatus.setTextColor(getColor(R.color.muted))
        orderStatus.text = "Descifrando orden…"

        executor.execute {
            var input: File? = null
            try {
                input = copyToCache(uri, "reto2_order_in", ".txt")
                val order = battleOrders.decryptFile(input, shift)
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        orderText.text = order
                        orderStatus.setTextColor(getColor(R.color.success))
                        orderStatus.text = "Orden recuperada con desplazamiento $shift."
                        finishOrderWork()
                    }
                }
            } catch (exception: Exception) {
                val message = exception.message ?: "No se pudo descifrar la orden."
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        orderStatus.setTextColor(getColor(R.color.error))
                        orderStatus.text = message
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        finishOrderWork()
                    }
                }
            } finally {
                input?.delete()
            }
        }
    }

    private fun finishImageWork() {
        imageBusy = false
        imageProgress.visibility = View.GONE
        imageDecryptButton.isEnabled = true
    }

    private fun finishOrderWork() {
        orderBusy = false
        orderProgress.visibility = View.GONE
        orderDecryptButton.isEnabled = true
    }

    private fun copyToCache(uri: Uri, prefix: String, suffix: String): File {
        val target = File.createTempFile(prefix, suffix, cacheDir)
        try {
            val source =
                contentResolver.openInputStream(uri)
                    ?: throw IOException("No se pudo abrir el fichero seleccionado.")
            source.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            return target
        } catch (exception: Exception) {
            target.delete()
            throw exception
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        try {
            contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            val value = cursor.getString(index)
                            if (!value.isNullOrBlank()) {
                                return value
                            }
                        }
                    }
                }
        } catch (exception: Exception) {
            Toast.makeText(this, "No se pudieron leer los metadatos del fichero.", Toast.LENGTH_SHORT)
                .show()
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "fichero"
    }

    override fun onDestroy() {
        executor.shutdownNow()
        imageTempOut?.delete()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_IMAGE = 1001
        private const val REQUEST_ORDER = 1002
    }
}
