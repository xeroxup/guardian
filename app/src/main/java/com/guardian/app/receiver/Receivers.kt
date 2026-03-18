package com.guardian.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.guardian.app.GuardianApp
import com.guardian.app.R
import com.guardian.app.data.model.EventType
import com.guardian.app.data.repository.GuardianRepository
import com.guardian.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Anti-fraud keywords to detect in SMS
private val SCAM_KEYWORDS = listOf(
    "вы выиграли", "winner", "prize", "lottery", "миллион", "million",
    "банк", "карта", "блокировка", "suspended", "account",
    "пароль", "password", "код", "code", "подтверждение",
    "срочно", "urgent", "перевод", "перевести", "перечисление",
    "налоговая", "судебный", "пристав", "долг",
    "крипто", "bitcoin", "инвестиция", "удвоитель",
    "смс", "sms", "на ваш номер", "короткий номер",
    "звонок", "перезвоните", "пропущенный",
    "аккаунт", "восстановить", "взлом", "хакер"
)

// Suspicious URLs in SMS
private val SUSPICIOUS_URLS = listOf(
    "bit.ly", "goo.gl", "tinyurl", "t.co", "is.gd",
    "vk.cc", "tiny.cc", "cutt.ly", "shorturl",
    ".xyz", ".top", ".click", ".link", ".info"
)

// Global flag to prevent repeated notifications
private var lastUsbNotificationTime = 0L

private fun showNotification(context: Context, title: String, message: String) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, GuardianApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    } catch (e: Exception) {
        // Silently fail
    }
}

private fun logEvent(context: Context, title: String, message: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val repository = GuardianRepository(context)
            repository.addEvent(EventType.USB_ENABLED, title, message)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

private fun checkForScam(text: String): Boolean {
    val lowerText = text.lowercase()
    return SCAM_KEYWORDS.any { lowerText.contains(it.lowercase()) } ||
           SUSPICIOUS_URLS.any { lowerText.contains(it) }
}

private fun isUsbDebuggingEnabled(context: Context): Boolean {
    return try {
        val secure = android.provider.Settings.Secure::class.java
        val method = secure.getMethod("getInt", android.content.ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType)
        val result = method.invoke(null, context.contentResolver, "adb_enabled", 0) as Int
        result == 1
    } catch (e: Exception) {
        false
    }
}

private suspend fun isProtectionAndModuleEnabled(context: Context, module: String): Boolean {
    return try {
        val repository = GuardianRepository(context)
        val protectionEnabled = repository.isProtectionEnabled.first()
        if (!protectionEnabled) return false
        
        when (module) {
            "usb" -> repository.isUsbMonitorEnabled.first()
            "sms" -> repository.isSmsFilterEnabled.first()
            "call" -> repository.isCallFilterEnabled.first()
            "app" -> repository.isAppMonitorEnabled.first()
            else -> false
        }
    } catch (e: Exception) {
        false
    }
}

private fun getAppName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
}

// SMS Filter - blocks scam SMS
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isProtectionAndModuleEnabled(context, "sms")) return@launch
                
                val bundle = intent.extras ?: return@launch
                val pdus = bundle.get("pdus") as? Array<*> ?: return@launch
                
                for (pdu in pdus) {
                    val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                    val message = sms.messageBody ?: continue
                    val sender = sms.originatingAddress ?: "Unknown"
                    
                    // Check for scam
                    if (checkForScam(message)) {
                        logEvent(context, "⚠️ Scam SMS Detected", "From: $sender")
                        
                        showNotification(
                            context,
                            "⚠️ Suspicious SMS Blocked",
                            "Scam message from $sender blocked"
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
}

// Call Filter - blocks unwanted calls
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isProtectionAndModuleEnabled(context, "call")) return@launch
                
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state != TelephonyManager.EXTRA_STATE_RINGING) return@launch
                
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return@launch
                
                // Check for suspicious numbers
                val suspiciousPrefixes = listOf("+44", "+1-900", "8-800", "8495", "8800", "+7-900", "+7900")
                
                if (suspiciousPrefixes.any { number.startsWith(it) }) {
                    logEvent(context, "⚠️ Suspicious Call", "From: $number")
                    
                    showNotification(
                        context,
                        "⚠️ Suspicious Call Detected",
                        "Call from $number may be fraud"
                    )
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
}

// USB Debugging Monitor
class UsbMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    checkUsbDebugging(context)
                }
            }
            "android.hardware.usb.action.USB_STATE" -> {
                if (intent.getBooleanExtra("connected", false)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        checkUsbDebugging(context)
                    }
                }
            }
        }
    }
    
    private suspend fun checkUsbDebugging(context: Context) {
        try {
            if (!isProtectionAndModuleEnabled(context, "usb")) return
            
            // Rate limit: only show notification once per hour
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUsbNotificationTime < 3600000) return
            
            if (isUsbDebuggingEnabled(context)) {
                lastUsbNotificationTime = currentTime
                
                showNotification(
                    context,
                    "⚠️ Security Warning",
                    "USB Debugging is ENABLED! This is a major security risk. Disable in Developer Options."
                )
                
                val repository = GuardianRepository(context)
                repository.addEvent(
                    EventType.USB_ENABLED,
                    "⚠️ USB Debugging Detected",
                    "Security threat - USB debugging is enabled"
                )
            }
        } catch (e: Exception) {
            // Ignore errors
        }
    }
}

// App installation tracker
class AppMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isProtectionAndModuleEnabled(context, "app")) return@launch
                
                val repository = GuardianRepository(context)
                
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val appName = getAppName(context, packageName)
                        repository.addEvent(
                            EventType.APP_INSTALLED,
                            "📱 App Installed",
                            appName
                        )
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val appName = getAppName(context, packageName)
                        repository.addEvent(
                            EventType.APP_REMOVED,
                            "🗑️ App Removed",
                            appName
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
}
