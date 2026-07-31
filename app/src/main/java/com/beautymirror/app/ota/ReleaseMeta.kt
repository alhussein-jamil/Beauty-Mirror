package com.beautymirror.app.ota

import org.json.JSONObject

data class ReleaseMeta(
    val version: String,
    val buildNumber: Int,
    val sha256: String,
    val size: Long,
    val apkAssetName: String,
) {
    companion object {
        fun parse(json: String): ReleaseMeta? = runCatching {
            val obj = JSONObject(json)
            val build = obj.optInt("buildNumber", -1)
            val sha = obj.optString("sha256").trim().lowercase()
            val size = obj.optLong("size", -1L)
            val apk = obj.optString("apkAssetName").trim()
            if (build <= 0 || sha.isEmpty() || size <= 0L || apk.isEmpty()) return null
            ReleaseMeta(
                version = obj.optString("version").ifBlank { build.toString() },
                buildNumber = build,
                sha256 = sha,
                size = size,
                apkAssetName = apk,
            )
        }.getOrNull()

        fun isNewer(remoteBuild: Int, localBuild: Int): Boolean = remoteBuild > localBuild
    }
}
