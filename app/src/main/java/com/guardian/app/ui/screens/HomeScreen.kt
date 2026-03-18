package com.guardian.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardian.app.ui.theme.*
import com.guardian.app.viewmodel.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(viewModel: GuardianViewModel) {
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsState()
    val isUsbMonitorEnabled by viewModel.isUsbMonitorEnabled.collectAsState()
    val stats by viewModel.stats.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuardianBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Guardian",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isProtectionEnabled) "Protection Active" else "Protection Disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isProtectionEnabled) GuardianGreen else GuardianRed
                )
            }
            
            // Main Protection Toggle
            Switch(
                checked = isProtectionEnabled,
                onCheckedChange = { viewModel.setProtectionEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GuardianGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = GuardianSurfaceVariant
                )
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main Shield Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isProtectionEnabled) GuardianPrimary.copy(alpha = 0.3f) else GuardianRed.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Shield Icon
            Card(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(24.dp, CircleShape),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = GuardianSurfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isProtectionEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                        contentDescription = "Shield",
                        modifier = Modifier.size(72.dp),
                        tint = if (isProtectionEnabled) GuardianPrimary else GuardianRed
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Usb,
                iconTint = if (isUsbMonitorEnabled) GuardianGreen else GuardianSurfaceVariant,
                title = "USB",
                value = if (isUsbMonitorEnabled) "ON" else "OFF",
                onClick = { viewModel.setUsbMonitorEnabled(!isUsbMonitorEnabled) }
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Block,
                iconTint = GuardianPink,
                title = "Blocked",
                value = stats.threatsBlocked.toString(),
                onClick = { }
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                iconTint = GuardianYellow,
                title = "Scanned",
                value = stats.appsScanned.toString(),
                onClick = { }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
                title = "Quick Scan",
                subtitle = "Scan now",
                iconTint = GuardianGreen,
                onClick = { viewModel.startScan() }
            )
            
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Apps,
                title = "Apps",
                subtitle = "Blacklist",
                iconTint = GuardianBlue,
                onClick = { /* Navigate to blacklist */ }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Individual Module Toggles
        Text(
            text = "Protection Modules",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        ModuleToggleCard(
            icon = Icons.Default.Usb,
            title = "USB Debug Monitor",
            subtitle = "Alert if USB debugging is enabled",
            isEnabled = isUsbMonitorEnabled,
            onToggle = { viewModel.setUsbMonitorEnabled(it) },
            enabledColor = GuardianGreen
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ModuleToggleCard(
            icon = Icons.Default.Sms,
            title = "SMS Filter",
            subtitle = "Block scam and fraud SMS",
            isEnabled = viewModel.isSmsFilterEnabled.collectAsState().value,
            onToggle = { viewModel.setSmsFilterEnabled(it) },
            enabledColor = GuardianBlue
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ModuleToggleCard(
            icon = Icons.Default.Phone,
            title = "Call Filter",
            subtitle = "Block suspicious calls",
            isEnabled = viewModel.isCallFilterEnabled.collectAsState().value,
            onToggle = { viewModel.setCallFilterEnabled(it) },
            enabledColor = GuardianPink
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ModuleToggleCard(
            icon = Icons.Default.Install,
            title = "App Monitor",
            subtitle = "Track app installations",
            isEnabled = viewModel.isAppMonitorEnabled.collectAsState().value,
            onToggle = { viewModel.setAppMonitorEnabled(it) },
            enabledColor = GuardianYellow
        )
    }
}

@Composable
private fun ModuleToggleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    enabledColor: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isEnabled) enabledColor.copy(alpha = 0.2f) else GuardianSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isEnabled) enabledColor else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = enabledColor,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = GuardianSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
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
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GuardianSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
