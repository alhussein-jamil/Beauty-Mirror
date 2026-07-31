package com.beautymirror.app.ota

import android.content.Context
import android.content.pm.PackageManager
import com.beautymirror.app.BuildConfig
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class UpdateStatus {
    data object Idle : UpdateStatus()
    data object UpToDate : UpdateStatus()
    data object Checking : UpdateStatus()
    data class Downloading(val percent: Int) : UpdateStatus()
    data object Installing : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
    data class Available(val build: Int, val version: String) : UpdateStatus()
}

class UpdateService(
    private val context: Context,
    private val client: GithubReleaseClient = GithubReleaseClient(BuildConfig.OTA_GITHUB_REPO),
) {
    private val mutex = Mutex()

    @Volatile
    var status: UpdateStatus = UpdateStatus.Idle
        private set

    var onStatus: ((UpdateStatus) -> Unit)? = null

    fun localVersionCode(): Int = try {
        val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    } catch (_: Exception) {
        BuildConfig.VERSION_CODE
    }

    suspend fun checkForUpdate(force: Boolean = false): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!BuildConfig.OTA_ENABLED) {
                publish(UpdateStatus.Error("Updates disabled in this build"))
                return@withLock
            }
            if (!force && !OtaPreferences.isAutoUpdateEnabled(context)) return@withLock
            if (!force && OtaPreferences.cooldownActive(context)) return@withLock

            publish(UpdateStatus.Checking)
            val (release, meta) = client.lookupLatestWithMeta()
            if (release == null || meta == null) {
                publish(UpdateStatus.Error("Could not reach GitHub releases"))
                return@withLock
            }
            val local = localVersionCode()
            if (!ReleaseMeta.isNewer(meta.buildNumber, local)) {
                publish(UpdateStatus.UpToDate)
                return@withLock
            }
            publish(UpdateStatus.Available(meta.buildNumber, meta.version))

            val asset = release.findAsset(meta.apkAssetName)
                ?: release.assets.firstOrNull {
                    it.name.endsWith(".apk", ignoreCase = true) &&
                        !it.name.contains("unsigned", ignoreCase = true)
                }
            if (asset == null) {
                publish(UpdateStatus.Error("Release APK asset missing"))
                return@withLock
            }
            val apk = File(context.cacheDir, "beauty-mirror-ota.apk")
            val ok = client.downloadAsset(asset, apk) { received, total ->
                val percent = if (total != null && total > 0L) {
                    ((received * 100L) / total).toInt().coerceIn(0, 99)
                } else {
                    0
                }
                publish(UpdateStatus.Downloading(percent))
            }
            if (!ok || !apk.isFile) {
                OtaPreferences.markApplyFailed(context)
                publish(UpdateStatus.Error("Download failed"))
                return@withLock
            }
            if (apk.length() != meta.size) {
                apk.delete()
                OtaPreferences.markApplyFailed(context)
                publish(UpdateStatus.Error("Size mismatch"))
                return@withLock
            }
            if (sha256(apk) != meta.sha256) {
                apk.delete()
                OtaPreferences.markApplyFailed(context)
                publish(UpdateStatus.Error("Checksum mismatch"))
                return@withLock
            }
            publish(UpdateStatus.Installing)
            try {
                ApkInstaller.install(context, apk)
                OtaPreferences.clearCooldown(context)
            } catch (error: Exception) {
                OtaPreferences.markApplyFailed(context)
                publish(UpdateStatus.Error(error.message ?: "Install failed"))
            }
        }
    }

    private fun publish(next: UpdateStatus) {
        status = next
        onStatus?.invoke(next)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
