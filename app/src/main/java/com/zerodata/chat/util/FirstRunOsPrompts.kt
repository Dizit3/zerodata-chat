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
    private const val KEY_DATA_SAVER_TOGGLE_ASKED = "data_saver_toggle_asked"
    private const val KEY_DATA_SAVER_EXCEPTION_ASKED = "data_saver_exception_asked"

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

    fun shouldPromptSaverToggle(
        status: DataSaverPromptStatus,
        toggleAsked: Boolean,
    ): Boolean = !toggleAsked && status == DataSaverPromptStatus.SAVER_OFF

    fun shouldPromptException(
        status: DataSaverPromptStatus,
        toggleAsked: Boolean,
        exceptionAsked: Boolean,
    ): Boolean {
        if (exceptionAsked || status == DataSaverPromptStatus.WHITELISTED) return false
        return toggleAsked || status == DataSaverPromptStatus.RESTRICTED
    }

    fun requestIfNeeded(activity: Activity) {
        requestNotificationsIfNeeded(activity)
        val openedBattery = requestBatteryExemptionIfNeeded(activity)
        if (!openedBattery) {
            requestDataSaverStepsIfNeeded(activity)
        }
    }

    fun requestDataSaverStepsIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        if (activity.isFinishing || activity.isDestroyed) return
        val status = currentDataSaverStatus(activity) ?: return
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val toggleAsked = prefs.getBoolean(KEY_DATA_SAVER_TOGGLE_ASKED, false)
        val exceptionAsked = prefs.getBoolean(KEY_DATA_SAVER_EXCEPTION_ASKED, false)

        if (shouldPromptSaverToggle(status, toggleAsked)) {
            prefs.edit().putBoolean(KEY_DATA_SAVER_TOGGLE_ASKED, true).apply()
            AlertDialog.Builder(activity)
                .setTitle("Экономия трафика")
                .setMessage(
                    "На 2G другие приложения могут съесть канал. Сначала включите экономию трафика."
                )
                .setPositiveButton("Открыть") { _, _ ->
                    openDataSaverSettings(activity)
                }
                .setNegativeButton("Позже") { _, _ ->
                    requestDataSaverStepsIfNeeded(activity)
                }
                .show()
            return
        }

        if (shouldPromptException(status, toggleAsked, exceptionAsked)) {
            prefs.edit().putBoolean(KEY_DATA_SAVER_EXCEPTION_ASKED, true).apply()
            AlertDialog.Builder(activity)
                .setTitle("Исключение ZeroData")
                .setMessage(
                    "Разрешите ZeroData без ограничений при экономии трафика, иначе MQTT в фоне могут обрезать."
                )
                .setPositiveButton("Открыть") { _, _ ->
                    openUnrestrictedDataSettings(activity)
                }
                .setNegativeButton("Позже", null)
                .show()
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

    fun openDataSaverSettings(context: Context) {
        try {
            context.startActivity(Intent("android.settings.DATA_SAVER_SETTINGS"))
        } catch (e: Exception) {
            Timber.e(e, "Could not open Data Saver settings")
        }
    }

    fun openUnrestrictedDataSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Could not open unrestricted-data settings")
            openDataSaverSettings(context)
        }
    }
}
