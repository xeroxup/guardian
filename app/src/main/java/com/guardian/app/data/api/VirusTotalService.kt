package com.guardian.app.data.api

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.guardian.app.data.model.VirusTotalResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class VirusTotalService(private val context: Context) {
    
    companion object {
        private const val TAG = "VirusTotalService"
        private const val BASE_URL = "https://www.virustotal.com/"
        // Note: In production, store this securely (e.g., encrypted in SharedPreferences)
        // Get your free API key from https://www.virustotal.com/gui/join-us
        private const val API_KEY = "YOUR_VIRUSTOTAL_API_KEY"
    }
    
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val api: VirusTotalApi by lazy {
        retrofit.create(VirusTotalApi::class.java)
    }
    
    /**
     * Scans an installed app using VirusTotal API
     * Returns scan result with detection statistics
     */
    suspend fun scanApp(packageName: String): ScanResult = withContext(Dispatchers.IO) {
        try {
            // Get APK file path
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val apkPath = appInfo.sourceDir
            
            // Calculate SHA-256 hash of the APK
            val sha256Hash = calculateFileHash(apkPath)
            if (sha256Hash == null) {
                return@withContext ScanResult.Error("Could not calculate file hash")
            }
            
            // Query VirusTotal API
            val response = api.getFileReport(API_KEY, sha256Hash)
            
            if (response.error != null) {
                return@withContext when {
                    response.error.contains("quota", ignoreCase = true) -> ScanResult.RateLimited
                    response.error.contains("not found", ignoreCase = true) -> ScanResult.NotFound
                    else -> ScanResult.Error(response.error)
                }
            }
            
            // Parse the response
            val data = response.data
            if (data == null) {
                return@withContext ScanResult.NotFound
            }
            
            val attributes = data.attributes
            val stats = attributes?.lastAnalysisStats
            
            if (stats == null) {
                return@withContext ScanResult.NotFound
            }
            
            val detectedCount = (stats.malicious ?: 0) + (stats.suspicious ?: 0)
            val totalScanners = (stats.malicious ?: 0) + (stats.suspicious ?: 0) + 
                              (stats.undetected ?: 0) + (stats.unchecked ?: 0) + 
                              (stats.typeUnsupported ?: 0) + (stats.timeout ?: 0)
            
            // Get malware name if detected
            val malwareName = if (detectedCount > 0) {
                attributes.lastAnalysisResults?.values
                    ?.find { it.category == "malicious" }
                    ?.result ?: "Unknown malware"
            } else null
            
            val result = VirusTotalResult(
                isInfected = detectedCount > 0,
                detectedBy = detectedCount,
                totalScanners = totalScanners,
                malwareName = malwareName,
                scanDate = attributes.lastAnalysisDate?.let {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(it * 1000))
                },
                permalink = "https://www.virustotal.com/gui/file/$sha256Hash"
            )
            
            ScanResult.Success(result)
            
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP Error: ${e.code()} - ${e.message()}")
            when (e.code()) {
                429 -> ScanResult.RateLimited
                401 -> ScanResult.Error("Invalid API key")
                404 -> ScanResult.NotFound
                else -> ScanResult.Error("HTTP Error: ${e.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning app: ${e.message}", e)
            ScanResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Scan multiple apps and return results
     */
    suspend fun scanApps(
        packageNames: List<String>, 
        onProgress: (Int, Int, String) -> Unit
    ): List<Pair<String, ScanResult>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, ScanResult>>()
        
        packageNames.forEachIndexed { index, packageName ->
            onProgress(index + 1, packageNames.size, packageName)
            
            val result = scanApp(packageName)
            results.add(packageName to result)
            
            // Rate limiting - VirusTotal free tier allows 4 requests per minute
            if (index < packageNames.size - 1) {
                kotlinx.coroutines.delay(16000) // 16 seconds between requests
            }
        }
        
        results
    }
    
    /**
     * Get list of installed apps with their package info
     */
    fun getInstalledApps(): List<Pair<String, String>> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return packages.map { appInfo ->
            val appName = pm.getApplicationLabel(appInfo).toString()
            appName to appInfo.packageName
        }.sortedBy { it.first }
    }
    
    /**
     * Calculate SHA-256 hash of a file
     */
    private fun calculateFileHash(filePath: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val file = java.io.File(filePath)
            val inputStream = java.io.FileInputStream(file)
            
            val buffer = ByteArray(8192)
            var read: Int
            
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            
            inputStream.close()
            
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating hash: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if API key is configured
     */
    fun isApiKeyConfigured(): Boolean = API_KEY != "YOUR_VIRUSTOTAL_API_KEY" && API_KEY.isNotBlank()
}
