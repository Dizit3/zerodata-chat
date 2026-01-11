package com.zerodata.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val errorLog = intent.getStringExtra("error_log") ?: "Unknown error"

        setContent {
            CrashScreen(
                errorLog = errorLog,
                onCopy = { copyToClipboard(errorLog) },
                onShare = { shareError(errorLog) },
                onRestart = { restartApp() }
            )
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Error Log", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Текст ошибки скопирован", Toast.LENGTH_SHORT).show()
    }

    private fun shareError(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Отправить отчет"))
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun CrashScreen(errorLog: String, onCopy: () -> Unit, onShare: () -> Unit, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
    ) {
        Text(
            text = "Упс! Произошла ошибка :(",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Red
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Приложение столкнулось с проблемой. Пожалуйста, передайте этот текст разработчику.",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFEEEEEE))
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = errorLog,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onCopy) { Text("Копировать") }
            Button(onClick = onShare) { Text("Отправить") }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Перезапустить приложение")
        }
    }
}
