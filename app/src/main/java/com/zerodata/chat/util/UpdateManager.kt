package com.zerodata.chat.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
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
                val release = gitHubApi.getLatestRelease(owner, repo)
                val currentVersion = BuildConfig.VERSION_NAME
                
                // Compare versions (simple string comparison for now, assuming semantic versioning like v1.0.0)
                // Normalize tags: remove 'v' prefix if present
                val remoteVersion = release.tagName.removePrefix("v")
                val localVersion = currentVersion.removePrefix("v")

                if (remoteVersion != localVersion) {
                    // Primitive check: just simple inequality assumes "new" means "update".
                    // Ideally, use a SemVer parser. For now, assuming standard Git flow: new release > old.
                    Log.d("UpdateManager", "Update found: $remoteVersion > $localVersion")
                    release
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Check failed", e)
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

                if (!response.isSuccessful) return@withContext null

                val file = File(context.externalCacheDir, "update.apk")
                val fos = FileOutputStream(file)
                fos.write(response.body?.bytes() ?: return@withContext null)
                fos.close()
                file
            } catch (e: Exception) {
                Log.e("UpdateManager", "Download failed", e)
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
        context.startActivity(intent)
    }
}
