package com.guardian.app.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guardian.app.data.api.VirusTotalResult
import com.guardian.app.ui.theme.*
import com.guardian.app.viewmodel.GuardianViewModel
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(viewModel: GuardianViewModel) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val blacklist by viewModel.blacklist.collectAsState()
    val isVirusTotalScanning by viewModel.isVirusTotalScanning.collectAsState()
    val virusTotalProgress by viewModel.virusTotalProgress.collectAsState()
    val virusTotalResults by viewModel.virusTotalResults.collectAsState()
    
    var isScanning by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showVirusTotalDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuardianBackground)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Scan",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Check installed applications",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Scan Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Scan Button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    if (isScanning) GuardianPrimary.copy(alpha = 0.3f) else GuardianGreen.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Scan Icon
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable(enabled = !isScanning && !isVirusTotalScanning) {
                            isScanning = true
                            scanResults = emptyList()
                            
                            scope.launch {
                                val pm = context.packageManager
                                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                                val blacklistedPackages = blacklist.map { it.packageName }.toSet()
                                
                                val dangerousPatterns = listOf(
                                    "com.svidersk", "com.zmzpass", "com.crypt",
                                    "com.malware", "com.virus", "com.trojan",
                                    "com.hack", "com.keylog", "com.spy",
                                    "com.remote", "com.admin", "com.root",
                                    "com.system", "com.advanced", "com.powerful",
                                    ".hacker", ".cracker", ".bypass"
                                )
                                
                                val dangerousPerms = listOf(
                                    "android.permission.READ_SMS",
                                    "android.permission.RECEIVE_SMS", 
                                    "android.permission.SEND_SMS",
                                    "android.permission.READ_CALL_LOG",
                                    "android.permission.READ_CONTACTS",
                                    "android.permission.CAMERA",
                                    "android.permission.RECORD_AUDIO"
                                )
                                
                                val threats = mutableListOf<Pair<String, String>>()
                                
                                for (app in installedApps) {
                                    val packageName = app.packageName.lowercase()
                                    val appName = pm.getApplicationLabel(app).toString()
                                    
                                    if (blacklistedPackages.contains(app.packageName)) {
                                        threats.add(appName to "Blacklisted")
                                        continue
                                    }
                                    
                                    val isDangerousPattern = dangerousPatterns.any { packageName.contains(it) }
                                    
                                    val hasSuspiciousPerms = try {
                                        val pi = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                                        val perms = pi.requestedPermissions?.toList() ?: emptyList()
                                        perms.count { dangerousPerms.contains(it) } >= 3
                                    } catch (e: Exception) { false }
                                    
                                    val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                                    
                                    if ((isDangerousPattern || hasSuspiciousPerms) && !isSystemApp) {
                                        val reason = when {
                                            isDangerousPattern -> "Suspicious name"
                                            hasSuspiciousPerms -> "Dangerous permissions"
                                            else -> "Unknown threat"
                                        }
                                        threats.add(appName to reason)
                                    }
                                    
                                    kotlinx.coroutines.delay(2)
                                }
                                
                                scanResults = threats
                                isScanning = false
                                viewModel.startScan()
                            }
                        },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = GuardianSurface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.Security,
                            contentDescription = "Quick Scan",
                            modifier = Modifier
                                .size(48.dp)
                                .then(if (isScanning) Modifier.rotate(rotation) else Modifier),
                            tint = if (isScanning) GuardianPrimary else GuardianGreen
                        )
                    }
                }
            }
            
            // VirusTotal Button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    if (isVirusTotalScanning) GuardianYellow.copy(alpha = 0.3f) else GuardianBlue.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable(enabled = !isScanning && !isVirusTotalScanning) {
                            showVirusTotalDialog = true
                        },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = GuardianSurface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVirusTotalScanning) Icons.Default.Sync else Icons.Default.BugReport,
                            contentDescription = "VirusTotal Scan",
                            modifier = Modifier
                                .size(48.dp)
                                .then(if (isVirusTotalScanning) Modifier.rotate(rotation) else Modifier),
                            tint = if (isVirusTotalScanning) GuardianYellow else GuardianBlue
                        )
                    }
                }
            }
        }
        
        // Scan Status
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                isScanning -> "Scanning..."
                isVirusTotalScanning -> "VirusTotal: ${virusTotalProgress.first}/${virusTotalProgress.second}"
                else -> "Tap to scan"
            },
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isScanning -> GuardianPrimary
                isVirusTotalScanning -> GuardianYellow
                else -> Color.Gray
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                iconTint = GuardianGreen,
                title = "Scanned",
                value = stats.appsScanned.toString()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Block,
                iconTint = GuardianRed,
                title = "Threats",
                value = stats.threatsBlocked.toString()
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                iconTint = GuardianYellow,
                title = "In List",
                value = blacklist.size.toString()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Scan Results
        if (scanResults.isNotEmpty() || virusTotalResults.isNotEmpty()) {
            val totalThreats = scanResults.size + virusTotalResults.values.count { it.isInfected }
            Text(
                text = "Threats Found ($totalThreats)",
                style = MaterialTheme.typography.titleMedium,
                color = GuardianRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Local scan results
                items(scanResults) { (appName, reason) ->
                    ThreatItem(
                        appName = appName,
                        reason = reason,
                        icon = Icons.Default.Warning,
                        iconTint = GuardianRed
                    )
                }
                
                // VirusTotal results
                items(virusTotalResults.filter { it.value.isInfected }.toList()) { (packageName, result) ->
                    VirusTotalThreatItem(
                        packageName = packageName,
                        result = result
                    )
                }
            }
        } else if (!isScanning && !isVirusTotalScanning && stats.appsScanned > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = GuardianGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No threats found",
                        style = MaterialTheme.typography.titleMedium,
                        color = GuardianGreen
                    )
                    if (stats.lastScanTime > 0) {
                        Text(
                            text = "Last scan: ${formatTime(stats.lastScanTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
    
    // VirusTotal Dialog
    if (showVirusTotalDialog) {
        AlertDialog(
            onDismissRequest = { showVirusTotalDialog = false },
            containerColor = GuardianSurface,
            title = { 
                Text("VirusTotal Scan", color = Color.White, fontWeight = FontWeight.Bold) 
            },
            text = {
                Column {
                    Text(
                        text = "This will scan all installed apps using the VirusTotal database.",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ Note: Free API limit is 4 requests per minute. Full scan may take a while.",
                        color = GuardianYellow,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!viewModel.isVirusTotalApiKeyConfigured()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ API Key not configured. Please add it in the code.",
                            color = GuardianRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVirusTotalDialog = false
                        viewModel.startVirusTotalScan()
                    }
                ) {
                    Text("Start Scan", color = GuardianBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVirusTotalDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ThreatItem(
    appName: String,
    reason: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = iconTint
                )
            }
        }
    }
}

@Composable
private fun VirusTotalThreatItem(
    packageName: String,
    result: VirusTotalResult
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pm = context.packageManager
    val appName = remember(packageName) {
        try {
            pm.getApplicationLabel(
                pm.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GuardianRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = GuardianRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${result.malwareName ?: "Detected"} (${result.detectedBy}/${result.totalScanners})",
                    style = MaterialTheme.typography.bodySmall,
                    color = GuardianRed
                )
                result.scanDate?.let { date ->
                    Text(
                        text = "Scanned: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
