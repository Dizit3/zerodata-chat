package com.zerodata.chat.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import timber.log.Timber

object FirstRunOsPrompts {
    private const val PREFS = "zerodata_os_prompts"
    private const val KEY_BATTERY_ASKED = "battery_exemption_asked"

    fun shouldRequestBatteryExemption(alreadyIgnoring: Boolean, alreadyAsked: Boolean): Boolean {
        return !alreadyIgnoring && !alreadyAsked
    }

    fun requestIfNeeded(activity: Activity) {
        requestNotificationsIfNeeded(activity)
        requestBatteryExemptionIfNeeded(activity)
    }

    private fun requestNotificationsIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1101)
        }
    }

    private fun requestBatteryExemptionIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pm = activity.getSystemService(PowerManager::class.java) ?: return
        val ignoring = pm.isIgnoringBatteryOptimizations(activity.packageName)
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val asked = prefs.getBoolean(KEY_BATTERY_ASKED, false)
        if (!shouldRequestBatteryExemption(ignoring, asked)) return
        prefs.edit().putBoolean(KEY_BATTERY_ASKED, true).apply()
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Could not open battery-optimization dialog")
        }
    }
}
