package com.guardian.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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

class PackageMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            val repository = GuardianRepository(context)
            val isProtectionEnabled = repository.isProtectionEnabled.first()
            
            if (!isProtectionEnabled) return@launch
            
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    val isBlocked = repository.isPackageBlacklisted(packageName)
                    val appName = getAppName(context, packageName)
                    
                    if (isBlocked) {
                        // Try to uninstall the app
                        tryUninstallApp(context, packageName)
                        
                        repository.addEvent(
                            EventType.APP_BLOCKED,
                            "🚫 App Blocked",
                            "$appName ($packageName) was blocked"
                        )
                        
                        showNotification(
                            context,
                            "App Blocked",
                            "$appName was blocked from installation"
                        )
                    } else {
                        repository.addEvent(
                            EventType.APP_INSTALLED,
                            "📱 App Installed",
                            "$appName was installed"
                        )
                    }
                    
                    repository.updateScanStats(repository.stats.first().appsScanned + 1)
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    val appName = getAppName(context, packageName)
                    repository.addEvent(
                        EventType.APP_REMOVED,
                        "🗑️ App Removed",
                        "$appName was removed"
                    )
                }
            }
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
    
    private fun tryUninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = android.net.Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Cannot uninstall - try to disable
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val pm = context.packageManager
                    pm.setApplicationEnabledSetting(
                        packageName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        0
                    )
                }
            } catch (e: Exception) {
                // Cannot disable either
            }
        }
    }
    
    private fun showNotification(context: Context, title: String, message: String) {
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
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // In a real app, would start a foreground service for monitoring
            // For now, just log the boot event
            CoroutineScope(Dispatchers.IO).launch {
                val repository = GuardianRepository(context)
                repository.addEvent(
                    EventType.PROTECTION_ENABLED,
                    "📱 Device Boot",
                    "Guardian started after device boot"
                )
            }
        }
    }
}

class UsbDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.hardware.usb.action.USB_STATE") {
            val connected = intent.getBooleanExtra("connected", false)
            
            CoroutineScope(Dispatchers.IO).launch {
                val repository = GuardianRepository(context)
                val isProtectionEnabled = repository.isProtectionEnabled.first()
                
                if (connected && isProtectionEnabled) {
                    repository.addEvent(
                        EventType.USB_ENABLED,
                        "🔌 USB Connected",
                        "USB debugging may be enabled"
                    )
                    
                    showNotification(
                        context,
                        "USB Warning",
                        "USB connected - ensure debugging is disabled"
                    )
                }
            }
        }
    }
}
