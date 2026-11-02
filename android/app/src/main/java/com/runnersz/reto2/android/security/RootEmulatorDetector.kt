package com.runnersz.reto2.android.security

import android.content.Context
import android.os.Build

object RootEmulatorDetector {

    fun isEmulator(context: Context): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk", ignoreCase = true)
            || Build.MODEL.contains("Emulator", ignoreCase = true)
            || Build.MODEL.contains("Android SDK built for", ignoreCase = true)
            || Build.HARDWARE.contains("goldfish", ignoreCase = true)
            || Build.HARDWARE.contains("ranchu", ignoreCase = true)
            || Build.PRODUCT.contains("sdk", ignoreCase = true)
            || Build.PRODUCT.contains("google_sdk", ignoreCase = true)
            || Build.BOARD.startsWith("goldfish")
            || Build.BOARD.startsWith("ranchu")
    }

    fun isRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    private fun checkRootMethod1(): Boolean {
        val checkRoots = arrayOf(
            "/system/app/Superuser.apk",
            "/system/xbin/daemonsu",
            "/system/xbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/system/xbin/supolicy",
            "/system/xbin/su.pie",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/data/local/su.pie",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/system/bin/su.ext",
            "/su/bin/su"
        )
        return checkRoots.any { java.io.File(it).exists() }
    }

    private fun checkRootMethod2(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val stream = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            stream.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRootMethod3(): Boolean {
        val paths = arrayOf("/system/bin/", "/system/xbin/", "/system/sbin/", "/sbin/", "/vendor/bin/")
        return paths.any { java.io.File(it, "su").exists() }
    }

    fun isEnvironmentUnsafe(context: Context): Boolean {
        return isEmulator(context) || isRooted()
    }
}
