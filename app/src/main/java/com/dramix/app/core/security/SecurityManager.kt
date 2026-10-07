package com.dramix.app.core.security

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket

class SecurityManager {

    fun isDeviceCompromised(): Boolean {
        return isRooted() || isFridaDetected()
    }

    fun getDetectedThreats(): List<String> {
        val threats = mutableListOf<String>()
        if (checkSuBinaries()) threats.add("ROOT_SU_BINARY_FOUND")
        if (checkTestKeys()) threats.add("ROOT_TEST_KEYS_BUILD")
        if (checkFridaMaps()) threats.add("FRIDA_LIBRARY_MAPPED")
        if (checkFridaPort()) threats.add("FRIDA_SERVER_PORT_OPEN")
        return threats
    }

    private fun isRooted(): Boolean {
        return checkSuBinaries() || checkTestKeys()
    }

    private fun isFridaDetected(): Boolean {
        return checkFridaMaps() || checkFridaPort()
    }

    private fun checkSuBinaries(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/system/app/Superuser.apk"
        )
        for (path in paths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {
                // Ignore security exceptions on locked systems
            }
        }
        return false
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkFridaMaps(): Boolean {
        return try {
            val mapsFile = File("/proc/self/maps")
            if (!mapsFile.exists()) return false

            BufferedReader(FileReader(mapsFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val entry = line?.lowercase() ?: continue
                    if (entry.contains("frida-agent") ||
                        entry.contains("frida-gadget") ||
                        entry.contains("libfrida")
                    ) {
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun checkFridaPort(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", 27042), 50)
                true // Successfully connected to default Frida port
            }
        } catch (_: Exception) {
            false
        }
    }
}
