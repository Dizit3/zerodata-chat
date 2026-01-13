package com.zerodata.chat.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRelease
}

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("assets") val assets: List<GitHubAsset>,
    @SerializedName("body") val body: String
)

data class GitHubAsset(
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("name") val name: String,
    @SerializedName("content_type") val contentType: String
)
