package com.guardian.app.data.api

import com.guardian.app.data.model.VirusTotalResponse
import retrofit2.http.*

interface VirusTotalApi {
    
    @GET("api/v3/files/{hash}")
    suspend fun getFileReport(
        @Header("x-apikey") apiKey: String,
        @Path("hash") hash: String
    ): VirusTotalResponse
    
    @FormUrlEncoded
    @POST("api/v3/files")
    suspend fun uploadFile(
        @Header("x-apikey") apiKey: String,
        @Field("file") fileContent: String
    ): VirusTotalResponse
}

data class VirusTotalResult(
    val isInfected: Boolean,
    val detectedBy: Int,
    val totalScanners: Int,
    val malwareName: String?,
    val scanDate: String?,
    val permalink: String?
)

sealed class ScanResult {
    data class Success(val result: VirusTotalResult) : ScanResult()
    data class Error(val message: String) : ScanResult()
    object NotFound : ScanResult()
    object RateLimited : ScanResult()
}
