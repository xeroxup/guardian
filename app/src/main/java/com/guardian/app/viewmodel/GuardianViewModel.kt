package com.guardian.app.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.app.data.model.AppStats
import com.guardian.app.data.model.BlacklistedApp
import com.guardian.app.data.model.EventType
import com.guardian.app.data.model.SecurityEvent
import com.guardian.app.data.repository.GuardianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GuardianViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GuardianRepository(application)
    
    // UI State
    val isProtectionEnabled = repository.isProtectionEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isUsbDebugEnabled = repository.isUsbDebugEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val blacklist = repository.blacklist.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events = repository.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val logs = events // Alias for compatibility
    val stats = repository.stats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppStats())
    val isScanning = MutableStateFlow(false)
    val usbStatus = repository.isUsbDebugEnabled
    
    // Computed properties for UI compatibility
    val threats: Int get() = stats.value.threatsBlocked
    val blocks: Int get() = stats.value.threatsBlocked
    val checks: Int get() = stats.value.appsScanned
    
    // Actions
    fun toggleProtection() {
        viewModelScope.launch {
            val newValue = !isProtectionEnabled.value
            repository.setProtectionEnabled(newValue)
            
            if (newValue) {
                repository.addEvent(
                    EventType.PROTECTION_ENABLED,
                    "🛡️ Protection Enabled",
                    "Active monitoring started"
                )
            } else {
                repository.addEvent(
                    EventType.PROTECTION_DISABLED,
                    "⚠️ Protection Disabled",
                    "Device is vulnerable"
                )
            }
        }
    }
    
    fun toggleUsbDebug() {
        viewModelScope.launch {
            val newValue = !isUsbDebugEnabled.value
            repository.setUsbDebugEnabled(newValue)
            
            if (newValue) {
                repository.addEvent(
                    EventType.USB_ENABLED,
                    "🔌 USB Debug Enabled",
                    "Security risk detected"
                )
            } else {
                repository.addEvent(
                    EventType.USB_DISABLED,
                    "✅ USB Debug Disabled",
                    "Threat removed"
                )
            }
        }
    }
    
    fun addToBlacklist(name: String, packageName: String) {
        viewModelScope.launch {
            repository.addToBlacklist(BlacklistedApp(name = name, packageName = packageName))
            repository.addEvent(
                EventType.APP_BLOCKED,
                "🚫 App Blocked",
                "$name has been blocked",
                packageName
            )
            repository.incrementBlockedCount()
        }
    }
    
    fun removeFromBlacklist(id: String) {
        viewModelScope.launch {
            repository.removeFromBlacklist(id)
        }
    }
    
    suspend fun isPackageBlocked(packageName: String): Boolean {
        return repository.isPackageBlacklisted(packageName)
    }
    
    fun addEvent(type: EventType, title: String, description: String, packageName: String? = null) {
        viewModelScope.launch {
            repository.addEvent(type, title, description, packageName)
        }
    }
    
    fun resetStats() {
        viewModelScope.launch {
            repository.resetStats()
        }
    }
    
    fun startScan() {
        viewModelScope.launch {
            isScanning.value = true
            try {
                val app = getApplication<Application>()
                val pm = app.packageManager
                val packages = withContext(Dispatchers.IO) {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                }
                
                val blockedApps = blacklist.value
                var threatsFound = 0
                
                for (packageInfo in packages) {
                    // Check if app is on blacklist
                    if (blockedApps.any { it.packageName == packageInfo.packageName }) {
                        threatsFound++
                        repository.addEvent(
                            EventType.APP_BLOCKED,
                            "🚫 Threat Detected",
                            "${packageInfo.packageName} is blacklisted"
                        )
                    }
                }
                
                repository.updateScanStats(packages.size)
                
                if (threatsFound > 0) {
                    repository.addEvent(
                        EventType.SCAN_COMPLETED,
                        "⚠️ Scan Complete",
                        "Found $threatsFound threats"
                    )
                } else {
                    repository.addEvent(
                        EventType.SCAN_COMPLETED,
                        "✅ Scan Complete",
                        "Scanned ${packages.size} apps - no threats"
                    )
                }
            } catch (e: Exception) {
                repository.addEvent(
                    EventType.SCAN_COMPLETED,
                    "❌ Scan Failed",
                    e.message ?: "Unknown error"
                )
            } finally {
                isScanning.value = false
            }
        }
    }
}
