package com.guardian.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = "События",
            style = MaterialTheme.typography.headlineLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Журнал активности",
            style = MaterialTheme.typography.bodyMedium,
            color = grayText
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = GuardianSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Нет событий",
                        style = MaterialTheme.typography.titleMedium,
                        color = grayText
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventItem(event = event)
                }
            }
        }
    }
}

@Composable
private fun EventItem(event: SecurityEvent) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val grayText = if (isDarkTheme) Color.Gray else Color(0xFF64748B)
    
    val (icon, iconColor, bgColor) = when (event.type) {
        EventType.USB_ENABLED, EventType.USB_DISABLED -> Triple(Icons.Default.Usb, GuardianBlue, GuardianBlue.copy(alpha = 0.1f))
        EventType.APP_BLOCKED -> Triple(Icons.Default.Block, GuardianRed, GuardianRed.copy(alpha = 0.1f))
        EventType.APP_INSTALLED -> Triple(Icons.Default.Download, GuardianYellow, GuardianYellow.copy(alpha = 0.1f))
        EventType.APP_REMOVED -> Triple(Icons.Default.Delete, GuardianPink, GuardianPink.copy(alpha = 0.1f))
        EventType.PROTECTION_ENABLED -> Triple(Icons.Default.Shield, GuardianGreen, GuardianGreen.copy(alpha = 0.1f))
        EventType.PROTECTION_DISABLED -> Triple(Icons.Default.ShieldMoon, GuardianRed, GuardianRed.copy(alpha = 0.1f))
        EventType.SCAN_COMPLETED -> Triple(Icons.Default.CheckCircle, GuardianPrimary, GuardianPrimary.copy(alpha = 0.1f))
        EventType.SMS_BLOCKED -> Triple(Icons.Default.Message, GuardianRed, GuardianRed.copy(alpha = 0.1f))
        EventType.CALL_BLOCKED -> Triple(Icons.Default.Phone, GuardianRed, GuardianRed.copy(alpha = 0.1f))
    }
    
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val time = dateFormat.format(Date(event.timestamp))
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) GuardianSurface else GuardianSurfaceLight),
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
