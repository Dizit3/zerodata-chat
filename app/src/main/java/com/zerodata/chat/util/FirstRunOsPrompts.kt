package com.zerodata.chat.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import timber.log.Timber

object FirstRunOsPrompts {
    private const val PREFS = "zerodata_os_prompts"
    private const val KEY_BATTERY_ASKED = "battery_exemption_asked"
    private const val KEY_DATA_SAVER_ASKED = "data_saver_asked"

    enum class DataSaverPromptStatus {
        /** Data Saver off — other apps can still eat 2G. */
        SAVER_OFF,
        /** Saver on, this app is restricted. */
        RESTRICTED,
        /** Saver on and this app is unrestricted. */
        WHITELISTED,
    }

    fun shouldRequestBatteryExemption(alreadyIgnoring: Boolean, alreadyAsked: Boolean): Boolean {
        return !alreadyIgnoring && !alreadyAsked
    }

    fun shouldPromptDataSaver(status: DataSaverPromptStatus, alreadyAsked: Boolean): Boolean {
        return !alreadyAsked && status != DataSaverPromptStatus.WHITELISTED
    }

    fun requestIfNeeded(activity: Activity) {
        requestNotificationsIfNeeded(activity)
        val openedBattery = requestBatteryExemptionIfNeeded(activity)
        if (!openedBattery) {
            requestDataSaverIfNeeded(activity)
        }
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

    private fun requestBatteryExemptionIfNeeded(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = activity.getSystemService(PowerManager::class.java) ?: return false
        val ignoring = pm.isIgnoringBatteryOptimizations(activity.packageName)
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val asked = prefs.getBoolean(KEY_BATTERY_ASKED, false)
        if (!shouldRequestBatteryExemption(ignoring, asked)) return false
        prefs.edit().putBoolean(KEY_BATTERY_ASKED, true).apply()
        return try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.e(e, "Could not open battery-optimization dialog")
            false
        }
    }

    private fun requestDataSaverIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val status = currentDataSaverStatus(activity) ?: return
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val asked = prefs.getBoolean(KEY_DATA_SAVER_ASKED, false)
        if (!shouldPromptDataSaver(status, asked)) return
        prefs.edit().putBoolean(KEY_DATA_SAVER_ASKED, true).apply()
        AlertDialog.Builder(activity)
            .setTitle("Экономия трафика")
            .setMessage(
                "На 2G другие приложения могут съесть канал. Включите экономию трафика " +
                    "и разрешите ZeroData без ограничений."
            )
            .setPositiveButton("Исключение ZeroData") { _, _ ->
                openUnrestrictedDataSettings(activity)
            }
            .setNeutralButton("Экономия трафика") { _, _ ->
                openDataSaverSettings(activity)
            }
            .setNegativeButton("Позже", null)
            .show()
    }

    fun currentDataSaverStatus(context: Context): DataSaverPromptStatus? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        return when (cm.restrictBackgroundStatus) {
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED ->
                DataSaverPromptStatus.SAVER_OFF
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED ->
                DataSaverPromptStatus.WHITELISTED
            else -> DataSaverPromptStatus.RESTRICTED
        }
    }

    fun openDataSaverSettings(activity: Activity) {
        try {
            activity.startActivity(Intent("android.settings.DATA_SAVER_SETTINGS"))
        } catch (e: Exception) {
            Timber.e(e, "Could not open Data Saver settings")
        }
    }

    fun openUnrestrictedDataSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Could not open unrestricted-data settings")
            openDataSaverSettings(activity)
        }
    }
}
