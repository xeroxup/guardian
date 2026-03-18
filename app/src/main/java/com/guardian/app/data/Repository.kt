package com.guardian.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guardian_prefs")

class Repository(private val context: Context) {
    
    companion object {
        private val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        private val USB_STATUS = booleanPreferencesKey("usb_status")
        private val BLACKLIST = stringPreferencesKey("blacklist")
        private val LOGS = stringPreferencesKey("logs")
        private val STATS = stringPreferencesKey("stats")
    }
    
    // Protection State
    val protectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PROTECTION_ENABLED] ?: true
    }
    
    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PROTECTION_ENABLED] = enabled
        }
    }
    
    // USB Status
    val usbStatus: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USB_STATUS] ?: false
    }
    
    suspend fun setUsbStatus(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USB_STATUS] = enabled
        }
    }
    
    // Blacklist
    val blacklist: Flow<List<BlacklistedApp>> = context.dataStore.data.map { prefs ->
        val json = prefs[BLACKLIST] ?: "[]"
        parseBlacklist(json)
    }
    
    suspend fun addToBlacklist(app: BlacklistedApp) {
        context.dataStore.edit { prefs ->
            val current = parseBlacklist(prefs[BLACKLIST] ?: "[]").toMutableList()
            current.add(app)
            prefs[BLACKLIST] = blacklistToJson(current)
        }
    }
    
    suspend fun removeFromBlacklist(id: String) {
        context.dataStore.edit { prefs ->
            val current = parseBlacklist(prefs[BLACKLIST] ?: "[]").toMutableList()
            current.removeAll { it.id == id }
            prefs[BLACKLIST] = blacklistToJson(current)
        }
    }
    
    // Logs
    val logs: Flow<List<LogEntry>> = context.dataStore.data.map { prefs ->
        val json = prefs[LOGS] ?: "[]"
        parseLogs(json)
    }
    
    suspend fun addLog(type: LogType, title: String, desc: String) {
        context.dataStore.edit { prefs ->
            val current = parseLogs(prefs[LOGS] ?: "[]").toMutableList()
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val entry = LogEntry(
                type = type,
                title = title,
                desc = desc,
                time = timeFormat.format(Date())
            )
            current.add(0, entry)
            // Keep only last 50 entries
            val trimmed = current.take(50)
            prefs[LOGS] = logsToJson(trimmed)
        }
    }
    
    // Stats
    val stats: Flow<Stats> = context.dataStore.data.map { prefs ->
        val json = prefs[STATS] ?: "{}"
        parseStats(json)
    }
    
    suspend fun updateStats(update: (Stats) -> Stats) {
        context.dataStore.edit { prefs ->
            val current = parseStats(prefs[STATS] ?: "{}")
            val updated = update(current)
            prefs[STATS] = statsToJson(updated)
        }
    }
    
    suspend fun resetStats() {
        context.dataStore.edit { prefs ->
            prefs[STATS] = statsToJson(Stats())
        }
    }
    
    // JSON Serialization Helpers
    private fun parseBlacklist(json: String): List<BlacklistedApp> {
        return try {
            if (json == "[]" || json.isEmpty()) return emptyList()
            val list = mutableListOf<BlacklistedApp>()
            // Simple JSON parsing without external library
            val items = json.removePrefix("[").removeSuffix("]").split("},{")
            items.forEach { item ->
                val clean = item.removePrefix("{").removeSuffix("}")
                val id = clean.extract("\"id\"")
                val name = clean.extract("\"name\"")
                val packageName = clean.extract("\"packageName\"")
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    list.add(BlacklistedApp(id, name, packageName))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun blacklistToJson(list: List<BlacklistedApp>): String {
        if (list.isEmpty()) return "[]"
        return "[" + list.joinToString(",") { app ->
            """{"id":"${app.id}","name":"${app.name}","packageName":"${app.packageName}"}"""
        } + "]"
    }
    
    private fun parseLogs(json: String): List<LogEntry> {
        return try {
            if (json == "[]" || json.isEmpty()) return emptyList()
            val list = mutableListOf<LogEntry>()
            val items = json.removePrefix("[").removeSuffix("]").split("},{")
            items.forEach { item ->
                val clean = item.removePrefix("{").removeSuffix("}")
                val id = clean.extract("\"id\"")
                val typeStr = clean.extract("\"type\"")
                val title = clean.extract("\"title\"")
                val desc = clean.extract("\"desc\"")
                val time = clean.extract("\"time\"")
                if (id.isNotEmpty() && title.isNotEmpty()) {
                    val type = when (typeStr.uppercase()) {
                        "BLOCK" -> LogType.BLOCK
                        "THREAT" -> LogType.THREAT
                        else -> LogType.CHECK
                    }
                    list.add(LogEntry(id, type, title, desc, time))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun logsToJson(list: List<LogEntry>): String {
        if (list.isEmpty()) return "[]"
        return "[" + list.joinToString(",") { entry ->
            """{"id":"${entry.id}","type":"${entry.type.name}","title":"${entry.title}","desc":"${entry.desc}","time":"${entry.time}"}"""
        } + "]"
    }
    
    private fun parseStats(json: String): Stats {
        return try {
            if (json == "{}" || json.isEmpty()) return Stats()
            Stats(
                threats = json.extract("\"threats\"").toIntOrNull() ?: 0,
                blocks = json.extract("\"blocks\"").toIntOrNull() ?: 0,
                checks = json.extract("\"checks\"").toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            Stats()
        }
    }
    
    private fun statsToJson(stats: Stats): String {
        return """{"threats":${stats.threats},"blocks":${stats.blocks},"checks":${stats.checks}}"""
    }
    
    private fun String.extract(key: String): String {
        val pattern = "$key\":\""
        val start = this.indexOf(pattern)
        if (start == -1) {
            val pattern2 = "$key\":"
            val start2 = this.indexOf(pattern2)
            if (start2 == -1) return ""
            val valueStart = start2 + pattern2.length
            var valueEnd = this.indexOf(",", valueStart)
            if (valueEnd == -1) valueEnd = this.indexOf("}", valueStart)
            if (valueEnd == -1) return ""
            return this.substring(valueStart, valueEnd).trim('"', ' ')
        }
        val valueStart = start + pattern.length
        var valueEnd = this.indexOf("\"", valueStart)
        if (valueEnd == -1) valueEnd = this.length
        return this.substring(valueStart, valueEnd)
    }
}
