package com.zerodata.chat.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.zerodata.chat.CrashActivity
import kotlin.system.exitProcess

class GlobalExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            Log.e("Crash", "Captured a crash!", exception)
            
            val intent = Intent(context, CrashActivity::class.java).apply {
                putExtra("error_log", Log.getStackTraceString(exception))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            
            exitProcess(1)
        } catch (e: Exception) {
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    companion object {
        fun initialize(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(context))
        }
    }
}
