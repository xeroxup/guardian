package com.guardian.app.data.model

import com.google.gson.annotations.SerializedName

data class VirusTotalResponse(
    @SerializedName("data")
    val data: VirusTotalData? = null,
    
    @SerializedName("error")
    val error: String? = null
)

data class VirusTotalData(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("type")
    val type: String? = null,
    
    @SerializedName("attributes")
    val attributes: VirusTotalAttributes? = null
)

data class VirusTotalAttributes(
    @SerializedName("last_analysis_stats")
    val lastAnalysisStats: AnalysisStats? = null,
    
    @SerializedName("last_analysis_date")
    val lastAnalysisDate: Long? = null,
    
    @SerializedName("last_analysis_results")
    val lastAnalysisResults: Map<String, AnalysisResult>? = null,
    
    @SerializedName("meaningful_name")
    val meaningfulName: String? = null,
    
    @SerializedName("names")
    val names: List<String>? = null,
    
    @SerializedName("size")
    val size: Long? = null,
    
    @SerializedName("sha256")
    val sha256: String? = null
)

data class AnalysisStats(
    @SerializedName("malicious")
    val malicious: Int? = null,
    
    @SerializedName("suspicious")
    val suspicious: Int? = null,
    
    @SerializedName("undetected")
    val undetected: Int? = null,
    
    @SerializedName("harmless")
    val harmless: Int? = null,
    
    @SerializedName("timeout")
    val timeout: Int? = null,
    
    @SerializedName("confirmed-timeout")
    val confirmedTimeout: Int? = null,
    
    @SerializedName("failure")
    val failure: Int? = null,
    
    @SerializedName("type-unsupported")
    val typeUnsupported: Int? = null,
    
    @SerializedName("unchecked")
    val unchecked: Int? = null
)

data class AnalysisResult(
    @SerializedName("category")
    val category: String? = null,
    
    @SerializedName("engine_name")
    val engineName: String? = null,
    
    @SerializedName("engine_version")
    val engineVersion: String? = null,
    
    @SerializedName("result")
    val result: String? = null,
    
    @SerializedName("method")
    val method: String? = null,
    
    @SerializedName("engine_update")
    val engineUpdate: String? = null
)
