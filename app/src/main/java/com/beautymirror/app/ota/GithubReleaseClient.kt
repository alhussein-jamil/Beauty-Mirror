package com.beautymirror.app.ota

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class GithubAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
)

data class GithubRelease(
    val tagName: String,
    val assets: List<GithubAsset>,
) {
    fun findAsset(name: String): GithubAsset? =
        assets.firstOrNull { it.name.equals(name, ignoreCase = true) }
}

/**
 * Public-repo GitHub Releases client (no PAT). Uses browser_download_url for assets.
 */
class GithubReleaseClient(
    private val repo: String,
    private val userAgent: String = "BeautyMirror-OTA",
) {
    fun lookupLatestWithMeta(): Pair<GithubRelease?, ReleaseMeta?> {
        val releases = listReleases() ?: return null to null
        var fallback: GithubRelease? = null
        for (release in releases) {
            if (release.findAsset("version.json") != null) {
                val meta = fetchMeta(release) ?: continue
                return release to meta
            }
            if (fallback == null) fallback = release
        }
        val release = fallback ?: return null to null
        return release to fetchMeta(release)
    }

    fun downloadAsset(
        asset: GithubAsset,
        destination: File,
        onProgress: ((received: Long, total: Long?) -> Unit)? = null,
    ): Boolean {
        destination.parentFile?.mkdirs()
        val part = File(destination.path + ".part")
        if (part.exists()) part.delete()
        val connection = open(asset.downloadUrl) ?: return false
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return false
            val total = connection.contentLengthLong.takeIf { it > 0L }
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var received = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        onProgress?.invoke(received, total)
                    }
                }
            }
            if (destination.exists()) destination.delete()
            if (!part.renameTo(destination)) {
                part.copyTo(destination, overwrite = true)
                part.delete()
            }
            true
        } catch (_: Exception) {
            part.delete()
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun listReleases(): List<GithubRelease>? {
        val connection = open("https://api.github.com/repos/$repo/releases?per_page=10") ?: return null
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.readBytes().toString(StandardCharsets.UTF_8)
            val array = JSONArray(body)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.optBoolean("draft", false)) continue
                    add(parseRelease(obj))
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchMeta(release: GithubRelease): ReleaseMeta? {
        val asset = release.findAsset("version.json") ?: return null
        val connection = open(asset.downloadUrl) ?: return null
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = connection.inputStream.readBytes().toString(StandardCharsets.UTF_8)
            ReleaseMeta.parse(json)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(obj: JSONObject): GithubRelease {
        val assetsJson = obj.optJSONArray("assets") ?: JSONArray()
        val assets = buildList {
            for (i in 0 until assetsJson.length()) {
                val a = assetsJson.getJSONObject(i)
                val url = a.optString("browser_download_url").ifBlank {
                    a.optString("url")
                }
                if (url.isBlank()) continue
                add(
                    GithubAsset(
                        name = a.optString("name"),
                        downloadUrl = url,
                        size = a.optLong("size", 0L),
                    ),
                )
            }
        }
        return GithubRelease(tagName = obj.optString("tag_name"), assets = assets)
    }

    private fun open(url: String): HttpURLConnection? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream, */*")
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
    }.getOrNull()
}
