package com.zerodata.chat.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import timber.log.Timber
import androidx.core.content.FileProvider
import com.zerodata.chat.BuildConfig
import com.zerodata.chat.network.GitHubApi
import com.zerodata.chat.network.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

class UpdateManager(private val context: Context) {

    private val gitHubApi: GitHubApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        gitHubApi = retrofit.create(GitHubApi::class.java)
    }

    suspend fun checkForUpdates(owner: String, repo: String): GitHubRelease? {
        return withContext(Dispatchers.IO) {
            try {
                // Using getReleases instead of getLatestRelease because 'latest' excludes pre-releases
                val releases = gitHubApi.getReleases(owner, repo)
                val release = releases.firstOrNull() ?: return@withContext null
                
                val currentVersion = BuildConfig.VERSION_NAME
                
                // Compare versions (simple string comparison for now, assuming semantic versioning like v1.0.0)
                // Normalize tags: remove 'v' prefix if present
                val remoteVersion = release.tagName.removePrefix("v")
                val localVersion = currentVersion.removePrefix("v")

                if (remoteVersion != localVersion) {
                    Timber.d("Update found: remote=%s, local=%s", remoteVersion, localVersion)
                    release
                } else {
                    Timber.d("No update needed. remote=%s, local=%s", remoteVersion, localVersion)
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Check for updates failed")
                null
            }
        }
    }

    suspend fun downloadUpdate(downloadUrl: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(downloadUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Timber.e("Download failed: HTTP %d", response.code)
                    return@withContext null
                }

                val file = File(context.externalCacheDir, "update.apk")
                val body = response.body ?: run {
                    Timber.e("Download failed: response body is null")
                    return@withContext null
                }
                
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                Timber.d("Update downloaded to: %s", file.absolutePath)
                file
            } catch (e: Exception) {
                Timber.e(e, "Download update failed")
                null
            }
        }
    }

    fun installUpdate(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            Timber.d("Starting installation for: %s", uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start installation intent")
        }
    }
}
