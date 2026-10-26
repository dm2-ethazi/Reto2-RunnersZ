package com.runnersz.reto2.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(sdk = [26])
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityTest {
    private fun controller(): ActivityController<MainActivity> =
        Robolectric.buildActivity(MainActivity::class.java).setup()

    private fun click(activity: MainActivity, identifier: Int) {
        activity.findViewById<Button>(identifier).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun await(activity: MainActivity, identifier: Int) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (true) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (activity.findViewById<Button>(identifier).isEnabled) return
            if (System.nanoTime() > deadline) fail("Activity task did not finish")
            Thread.sleep(20)
        }
    }

    private fun incoming(controller: ActivityController<MainActivity>, name: String,
                         content: ByteArray, mime: String, share: Boolean = false) {
        val file = File(controller.get().cacheDir, name).apply { writeBytes(content) }
        val intent = if (share) Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        } else Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), mime)
        controller.newIntent(intent)
    }

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/vectors/$name")).use { it.readBytes() }

    @Test fun navigatesAndRecoversSharedCaesarOrder() {
        val controller = controller()
        try {
            val activity = controller.get()
            click(activity, R.id.navOrder)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.orderScreen).visibility)
            click(activity, R.id.orderDecryptButton)
            click(activity, R.id.navImage)
            click(activity, R.id.imageDecryptButton)
            incoming(controller, "orden.txt", "Dwdtxh Ñá!".toByteArray(), "text/plain", true)
            for (shift in listOf("", "0", "26", "no")) {
                activity.findViewById<EditText>(R.id.shiftInput).setText(shift)
                click(activity, R.id.orderDecryptButton)
                assertNotNull(activity.findViewById<EditText>(R.id.shiftInput).error)
            }
            activity.findViewById<EditText>(R.id.shiftInput).setText("3")
            click(activity, R.id.orderDecryptButton)
            await(activity, R.id.orderDecryptButton)
            assertEquals("Ataque Ñá!", activity.findViewById<TextView>(R.id.orderText).text.toString())
            assertFalse(activity.isFinishing)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun showsPlainAndBackendEncryptedPng() {
        val controller = controller()
        try {
            val activity = controller.get()
            incoming(controller, "image.png", fixture("image.bin"), "image/png")
            click(activity, R.id.imageDecryptButton)
            await(activity, R.id.imageDecryptButton)
            assertNotNull(activity.findViewById<ImageView>(R.id.imageView).drawable)
            incoming(controller, "image.png.enc", fixture("image.enc"), "application/octet-stream", true)
            activity.findViewById<EditText>(R.id.aesPassword).setText("RunnersZ-ñ-🔐")
            click(activity, R.id.imageDecryptButton)
            await(activity, R.id.imageDecryptButton)
            assertTrue(activity.findViewById<TextView>(R.id.imageStatus).text.contains("verificada"))
            assertNotNull(activity.findViewById<ImageView>(R.id.imageView).drawable)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun reportsEmptyWrongPasswordsAndCorruptImagesWithoutClosing() {
        val controller = controller()
        try {
            val activity = controller.get()
            for (password in listOf("", "incorrecta")) {
                incoming(controller, "bad.png.enc", fixture("image.enc"), "application/octet-stream")
                activity.findViewById<EditText>(R.id.aesPassword).setText(password)
                click(activity, R.id.imageDecryptButton)
                await(activity, R.id.imageDecryptButton)
                assertFalse(activity.findViewById<TextView>(R.id.imageStatus).text.contains("verificada"))
            }
            for (content in listOf(byteArrayOf(1,2,3,4), "HGAE".toByteArray())) {
                incoming(controller, "corrupt.png", content, "image/png")
                click(activity, R.id.imageDecryptButton)
                await(activity, R.id.imageDecryptButton)
                assertFalse(activity.isFinishing)
            }
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun launchesSelectorsAndHandlesTheirResults() {
        val controller = controller()
        try {
            val activity = controller.get()
            val shadow = Shadows.shadowOf(activity)
            click(activity, R.id.imagePickButton)
            val imageRequest = shadow.nextStartedActivityForResult
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, imageRequest.intent.action)
            shadow.receiveResult(imageRequest.intent, Activity.RESULT_CANCELED, Intent())
            click(activity, R.id.orderPickButton)
            val orderRequest = shadow.nextStartedActivityForResult
            val file = File(activity.cacheDir, "selector.txt").apply { writeText("B") }
            shadow.receiveResult(orderRequest.intent, Activity.RESULT_OK,
                Intent().setData(Uri.fromFile(file)))
            assertTrue(activity.findViewById<TextView>(R.id.orderFileLabel).text.contains("selector.txt"))
            click(activity, R.id.imagePickButton)
            val selectedImage = shadow.nextStartedActivityForResult
            val png = File(activity.cacheDir, "selector.png").apply { writeBytes(fixture("image.bin")) }
            shadow.receiveResult(selectedImage.intent, Activity.RESULT_OK,
                Intent().setData(Uri.fromFile(png)))
            assertTrue(activity.findViewById<TextView>(R.id.imageFileLabel).text.contains("selector.png"))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun rejectsUnsupportedIntentsAndHandlesMissingFiles() {
        val controller = controller()
        try {
            val activity = controller.get()
            incoming(controller, "document.pdf", byteArrayOf(1), "application/pdf")
            controller.newIntent(Intent(Intent.ACTION_SEND))
            controller.newIntent(Intent("unsupported.action"))
            val missing = File(activity.cacheDir, "missing.txt")
            controller.newIntent(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(missing), "text/plain"))
            activity.findViewById<EditText>(R.id.shiftInput).setText("1")
            click(activity, R.id.orderDecryptButton)
            await(activity, R.id.orderDecryptButton)
            assertFalse(activity.isFinishing)
            val image = File(activity.cacheDir, "missing.png")
            controller.newIntent(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(image), "image/png"))
            click(activity, R.id.imageDecryptButton)
            await(activity, R.id.imageDecryptButton)
            assertFalse(activity.isFinishing)
        } finally { controller.pause().stop().destroy() }
    }
}
