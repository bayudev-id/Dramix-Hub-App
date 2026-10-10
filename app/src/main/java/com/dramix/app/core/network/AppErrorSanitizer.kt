package com.dramix.app.core.network

import android.util.Log
import androidx.media3.common.PlaybackException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * AppErrorSanitizer
 *
 * Mengonversi seluruh exception jaringan, server, database, dan pemutar video ke dalam
 * kode error terstandarisasi ([ERR_XXX_000]) dan pesan aman untuk antarmuka pengguna.
 *
 * Mencegah kebocoran:
 * - URL mentah (http://..., https://...)
 * - IP server / Host internal / Port (:8090, :6101-6107)
 * - Domain gateway / Cloudflare endpoint
 * - Kunci otentikasi / query params (?api_key=..., token)
 * - Stack trace internal
 */
object AppErrorSanitizer {

    private const val TAG = "DramixError"

    private val URL_REGEX = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
    private val IP_REGEX = Regex("""\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?)(?::\d+)?\b""")
    private val DOMAIN_REGEX = Regex("""\b[a-zA-Z0-9.-]+\.(?:com|web\.id|net|org|io|ph|care|me|tv|app|do)(?::\d+)?\b""", RegexOption.IGNORE_CASE)
    private val QUERY_PARAM_REGEX = Regex("""\?[^&\s"'<>]+(?:&[^&\s"'<>]+)*""")
    private val ENDPOINT_PATH_REGEX = Regex("""/(?:api|vod|modelles|license)[^\s"'<>]*""", RegexOption.IGNORE_CASE)

    /**
     * Membersihkan teks dari URL, IP, endpoint path, domain, atau token.
     */
    fun sanitize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .replace(URL_REGEX, "")
            .replace(QUERY_PARAM_REGEX, "")
            .replace(ENDPOINT_PATH_REGEX, "")
            .replace(IP_REGEX, "")
            .replace(DOMAIN_REGEX, "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Format Throwable umum dengan pemetaan kode error standar.
     */
    fun format(
        throwable: Throwable?,
        defaultCode: String = "ERR_SYS_001",
        defaultMessage: String? = null
    ): String {
        if (throwable != null) {
            try {
                Log.e(TAG, "[$defaultCode] Exception caught: ${throwable.javaClass.simpleName} - ${throwable.message}", throwable)
            } catch (_: Throwable) {
                // Ignore in unmocked Android environment during JVM tests
            }
        }

        if (throwable == null) {
            return "[$defaultCode] ${defaultMessage ?: "Terjadi kesalahan sistem"}"
        }

        // 1. Masalah Koneksi Internet / Jaringan
        when (throwable) {
            is UnknownHostException -> {
                return "[ERR_NET_002] Periksa koneksi internet Anda"
            }
            is SocketTimeoutException -> {
                return "[ERR_NET_001] Batas waktu koneksi berakhir"
            }
            is ConnectException -> {
                return "[ERR_NET_003] Gagal terhubung ke server"
            }
        }

        // 2. Respon HTTP Gateway / Server
        if (throwable is HttpException) {
            return when (val code = throwable.code()) {
                400 -> "[ERR_SRV_400] Permintaan tidak valid"
                401 -> "[ERR_SRV_401] Autentikasi diperlukan"
                403 -> "[ERR_SRV_403] Akses layanan dibatasi"
                404 -> "[ERR_SRV_404] Konten tidak ditemukan"
                500 -> "[ERR_SRV_500] Gangguan server internal"
                502 -> "[ERR_SRV_502] Layanan upstream tidak merespons"
                503 -> "[ERR_SRV_503] Layanan dalam pemeliharaan"
                else -> "[ERR_SRV_$code] Respon server tidak sesuai ($code)"
            }
        }

        // 3. ExoPlayer / Media3 PlaybackException
        if (throwable is PlaybackException) {
            return formatPlaybackException(throwable)
        }

        // 4. Periksa pesan teks kustom yang mungkin sudah bersih atau memiliki kode
        val rawMsg = throwable.message ?: throwable.localizedMessage
        if (!rawMsg.isNullOrBlank()) {
            // Jika pesan sudah berformat kode ([ERR_...])
            if (rawMsg.startsWith("[ERR_")) {
                return sanitize(rawMsg)
            }

            // Keyword matching untuk kasus bisnis tertentu
            val lower = rawMsg.lowercase()
            when {
                lower.contains("terikat") || lower.contains("device") || lower.contains("bound") -> {
                    return "[ERR_LIC_003] Lisensi sudah terikat ke perangkat lain"
                }
                lower.contains("kadaluwarsa") || lower.contains("expired") || lower.contains("berakhir") -> {
                    return "[ERR_LIC_002] Masa aktif lisensi telah berakhir"
                }
                lower.contains("tidak valid") || lower.contains("invalid key") -> {
                    return "[ERR_LIC_001] Kode lisensi tidak valid"
                }
                lower.contains("siaran terputus") -> {
                    return "[ERR_PLY_004] Sinyal siaran terputus"
                }
            }

            val cleaned = sanitize(rawMsg)
            if (cleaned.isNotBlank() && !cleaned.contains("Exception") && cleaned.length < 50) {
                return "[$defaultCode] $cleaned"
            }
        }

        return "[$defaultCode] ${defaultMessage ?: "Terjadi kesalahan sistem"}"
    }

    private fun formatPlaybackException(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                "[ERR_PLY_002] Gangguan koneksi pemutaran video"
            }
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> {
                "[ERR_PLY_003] Akses streaming video ditolak"
            }
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> {
                "[ERR_PLY_001] Format media tidak didukung perangkat"
            }
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> {
                "[ERR_PLY_001] Berkas manifest tayangan rusak"
            }
            else -> {
                "[ERR_PLY_${error.errorCode}] Gangguan pemutaran media (${error.errorCodeName})"
            }
        }
    }

    // Helper semantik per modul:
    fun formatCatalog(throwable: Throwable?, defaultCode: String = "ERR_CAT_001"): String {
        return format(throwable, defaultCode = defaultCode, defaultMessage = "Gagal memuat katalog konten")
    }

    fun formatDetail(throwable: Throwable?, defaultCode: String = "ERR_DET_001"): String {
        return format(throwable, defaultCode = defaultCode, defaultMessage = "Gagal memuat detail konten")
    }

    fun formatSource(
        throwable: Throwable?,
        defaultCode: String = "ERR_STR_001",
        defaultMessage: String = "Gagal memuat sumber video"
    ): String {
        return format(throwable, defaultCode = defaultCode, defaultMessage = defaultMessage)
    }

    fun formatPlayback(
        throwable: Throwable?,
        defaultCode: String = "ERR_PLY_002"
    ): String {
        return format(throwable, defaultCode = defaultCode, defaultMessage = "Gangguan koneksi pemutaran video")
    }

    fun formatSearch(throwable: Throwable?, defaultCode: String = "ERR_SRC_001"): String {
        return format(throwable, defaultCode = defaultCode, defaultMessage = "Pencarian gagal diproses")
    }

    fun formatLicense(throwable: Throwable?, defaultCode: String = "ERR_LIC_004"): String {
        return format(throwable, defaultCode = defaultCode, defaultMessage = "Gagal memproses lisensi")
    }
}
