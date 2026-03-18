package com.guardian.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardian.app.data.model.EventType
import com.guardian.app.data.model.SecurityEvent
import com.guardian.app.ui.theme.*
import com.guardian.app.viewmodel.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventsScreen(viewModel: GuardianViewModel) {
    val events by viewModel.events.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    
    val backgroundColor = if (isDarkTheme) GuardianBackground else GuardianBackgroundLight
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    val surfaceColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = "Журнал событий",
            style = MaterialTheme.typography.headlineLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "История защиты и угроз",
            style = MaterialTheme.typography.bodyMedium,
            color = grayText
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColor
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isDarkTheme) 0.dp else 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = if (isDarkTheme) Color(0xFF1A1C26) else Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Журнал пуст",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "События безопасности появятся здесь",
                            style = MaterialTheme.typography.bodySmall,
                            color = grayText
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventItem(event = event, isDarkTheme = isDarkTheme)
                }
            }
        }
    }
}

@Composable
private fun EventItem(
    event: SecurityEvent,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    val cardBackground = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight
    
    // Theme-aware icon colors
    val blueColor = if (isDarkTheme) GuardianBlue else GuardianBlueLight
    val redColor = if (isDarkTheme) GuardianRed else GuardianRedLight
    val yellowColor = if (isDarkTheme) GuardianYellow else GuardianYellowLight
    val pinkColor = if (isDarkTheme) GuardianPink else GuardianPinkLight
    val greenColor = if (isDarkTheme) GuardianGreen else GuardianGreenLight
    val primaryColor = if (isDarkTheme) GuardianPrimary else GuardianPrimaryLight
    
    val (icon, iconColor, bgColor) = when (event.type) {
        EventType.USB_ENABLED, EventType.USB_DISABLED -> Triple(Icons.Default.Usb, blueColor, blueColor.copy(alpha = 0.1f))
        EventType.APP_BLOCKED -> Triple(Icons.Default.Block, redColor, redColor.copy(alpha = 0.1f))
        EventType.APP_INSTALLED -> Triple(Icons.Default.Download, yellowColor, yellowColor.copy(alpha = 0.1f))
        EventType.APP_REMOVED -> Triple(Icons.Default.Delete, pinkColor, pinkColor.copy(alpha = 0.1f))
        EventType.PROTECTION_ENABLED -> Triple(Icons.Default.Shield, greenColor, greenColor.copy(alpha = 0.1f))
        EventType.PROTECTION_DISABLED -> Triple(Icons.Default.ShieldMoon, redColor, redColor.copy(alpha = 0.1f))
        EventType.SCAN_COMPLETED -> Triple(Icons.Default.CheckCircle, primaryColor, primaryColor.copy(alpha = 0.1f))
        EventType.SMS_BLOCKED -> Triple(Icons.Default.Message, redColor, redColor.copy(alpha = 0.1f))
        EventType.CALL_BLOCKED -> Triple(Icons.Default.Phone, redColor, redColor.copy(alpha = 0.1f))
    }
    
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val time = dateFormat.format(Date(event.timestamp))
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 2.dp)
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
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title.replace(Regex("[📱🔍✅⚠️🦠⏳❌🔌📞]"), "").trim(),
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = grayText
                )
            }
            
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = grayText
            )
        }
    }
}
