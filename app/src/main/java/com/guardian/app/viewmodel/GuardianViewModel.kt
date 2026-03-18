package com.guardian.app.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.app.data.model.AppStats
import com.guardian.app.data.model.BlacklistedApp
import com.guardian.app.data.model.EventType
import com.guardian.app.data.model.SecurityEvent
import com.guardian.app.data.repository.GuardianRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GuardianViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GuardianRepository(application)
    
    // Main protection
    val isProtectionEnabled = repository.isProtectionEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    // Individual module toggles
    val isUsbMonitorEnabled = repository.isUsbMonitorEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isSmsFilterEnabled = repository.isSmsFilterEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isCallFilterEnabled = repository.isCallFilterEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val isAppMonitorEnabled = repository.isAppMonitorEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    // Data
    val blacklist = repository.blacklist.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val events = repository.events.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val stats = repository.stats.stateIn(viewModelScope, SharingStarted.Eagerly, AppStats())
    
    // Computed
    val threats get() = events.value.count { it.type == EventType.APP_BLOCKED || it.type == EventType.USB_ENABLED }
    val blocks get() = stats.value.threatsBlocked
    val checks get() = stats.value.appsScanned
    val lastScanTime get() = stats.value.lastScanTime
    
    // Setters
    fun setProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setProtectionEnabled(enabled)
            repository.addEvent(
                if (enabled) EventType.PROTECTION_ENABLED else EventType.PROTECTION_DISABLED,
                if (enabled) "🛡️ Protection Enabled" else "⏸️ Protection Disabled",
                "Security protection has been ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    fun setUsbMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUsbMonitorEnabled(enabled)
            repository.addEvent(
                EventType.SCAN_COMPLETED,
                "🔌 USB Monitor",
                "USB monitoring ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    fun setSmsFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSmsFilterEnabled(enabled)
            repository.addEvent(
                EventType.SCAN_COMPLETED,
                "📱 SMS Filter",
                "SMS filtering ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    fun setCallFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCallFilterEnabled(enabled)
            repository.addEvent(
                EventType.SCAN_COMPLETED,
                "📞 Call Filter",
                "Call filtering ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    fun setAppMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppMonitorEnabled(enabled)
            repository.addEvent(
                EventType.SCAN_COMPLETED,
                "📦 App Monitor",
                "App monitoring ${if (enabled) "enabled" else "disabled"}"
            )
        }
    }
    
    // Blacklist operations
    fun addToBlacklist(name: String, packageName: String) {
        viewModelScope.launch {
            repository.addToBlacklist(BlacklistedApp(name = name, packageName = packageName))
        }
    }
    
    fun removeFromBlacklist(id: String) {
        viewModelScope.launch {
            repository.removeFromBlacklist(id)
        }
    }
    
    // Scan
    fun startScan() {
        viewModelScope.launch {
            try {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val blacklisted = blacklist.value.map { it.packageName }.toSet()
                
                var threatsFound = 0
                for (packageInfo in packages) {
                    if (blacklisted.contains(packageInfo.packageName)) {
                        threatsFound++
                        repository.incrementBlockedCount()
                        repository.addEvent(
                            EventType.APP_BLOCKED,
                            "⚠️ Blacklisted App Detected",
                            pm.getApplicationLabel(packageInfo).toString(),
                            packageInfo.packageName
                        )
                    }
                }
                
                repository.updateScanStats(packages.size)
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    "🔍 Scan Completed",
                    "Scanned ${packages.size} apps, $threatsFound threats found"
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Reset stats
    fun resetStats() {
        viewModelScope.launch {
            repository.resetStats()
        }
    }
}
