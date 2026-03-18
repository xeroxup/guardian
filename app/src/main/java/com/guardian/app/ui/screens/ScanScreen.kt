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
import androidx.compose.ui.unit.dp
import com.guardian.app.ui.theme.*
import com.guardian.app.viewmodel.GuardianViewModel

@Composable
fun ScanScreen(viewModel: GuardianViewModel) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val blacklist by viewModel.blacklist.collectAsState()
    var isScanning by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    
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
        
        // Scan Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(200.dp)
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
                    .size(140.dp)
                    .clickable(enabled = !isScanning) {
                        isScanning = true
                        scanResults = emptyList()
                        
                        // Perform scan
                        val pm = context.packageManager
                        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        val blacklistedPackages = blacklist.map { it.packageName }.toSet()
                        val threats = mutableListOf<Pair<String, String>>()
                        
                        for (app in installedApps) {
                            if (blacklistedPackages.contains(app.packageName)) {
                                val appName = pm.getApplicationLabel(app).toString()
                                threats.add(appName to app.packageName)
                            }
                        }
                        
                        scanResults = threats
                        isScanning = false
                        viewModel.startScan()
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
                        contentDescription = "Scan",
                        modifier = Modifier
                            .size(56.dp)
                            .then(if (isScanning) Modifier.rotate(rotation) else Modifier),
                        tint = if (isScanning) GuardianPrimary else GuardianGreen
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Scan Status
        Text(
            text = if (isScanning) "Scanning..." else "Tap to scan",
            style = MaterialTheme.typography.titleMedium,
            color = if (isScanning) GuardianPrimary else Color.Gray,
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
        if (scanResults.isNotEmpty()) {
            Text(
                text = "Threats Found (${scanResults.size})",
                style = MaterialTheme.typography.titleMedium,
                color = GuardianRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scanResults) { (appName, packageName) ->
                    ThreatItem(
                        appName = appName,
                        packageName = packageName
                    )
                }
            }
        } else if (!isScanning && stats.appsScanned > 0) {
            // Show last scan results
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
    packageName: String
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
                    .background(GuardianRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
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
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
