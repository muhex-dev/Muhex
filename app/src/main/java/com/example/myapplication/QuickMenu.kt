package com.example.myapplication

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.drawable.toBitmap
import androidx.activity.compose.BackHandler
import java.text.SimpleDateFormat
import java.util.*

sealed class QuickMenuAction {
    object Bluetooth : QuickMenuAction()
    object Wifi : QuickMenuAction()
    object Flashlight : QuickMenuAction()
    object Battery : QuickMenuAction()
    object Settings : QuickMenuAction()
    object Calculator : QuickMenuAction()
    object Calendar : QuickMenuAction()
    object Apps : QuickMenuAction()
    object Search : QuickMenuAction()
    object Files : QuickMenuAction()
    object Browser : QuickMenuAction()
    object Dismiss : QuickMenuAction()
}

@Composable
fun QuickMenu(
    isVisible: Boolean,
    apps: List<AppModel>,
    recentApps: List<AppModel> = emptyList(),
    onAction: (QuickMenuAction) -> Unit,
    onAppClick: (AppModel) -> Unit = {},
    onDismissFinished: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color.Black else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val secondaryContentColor = contentColor.copy(alpha = 0.6f)
    val itemBgColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)

    val revealProgress = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            revealProgress.animateTo(1f, tween(durationMillis = 500, easing = FastOutSlowInEasing))
        } else {
            revealProgress.animateTo(0f, tween(durationMillis = 400, easing = FastOutSlowInEasing))
            onDismissFinished()
        }
    }

    if (!isVisible && revealProgress.value <= 0f) return

    BackHandler(enabled = isVisible) {
        onAction(QuickMenuAction.Dismiss)
    }

    // Time update effect
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = Calendar.getInstance().time
            kotlinx.coroutines.delay(10000) // Update every 10s
        }
    }
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable { onAction(QuickMenuAction.Dismiss) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Set width to 90%
                .fillMaxHeight(0.52f)
                .graphicsLayer {
                    val maxRadius = Math.hypot(size.width.toDouble(), size.height.toDouble()).toFloat() * 1.2f
                    val currentRadius = maxRadius * revealProgress.value
                    
                    clip = true
                    shape = object : Shape {
                        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                            val path = Path().apply {
                                addOval(Rect(center = Offset(size.width, size.height), radius = currentRadius))
                            }
                            return Outline.Generic(path)
                        }
                    }
                    scaleX = 0.95f + (0.05f * revealProgress.value)
                    scaleY = 0.95f + (0.05f * revealProgress.value)
                    alpha = 1f
                }
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                            if (Math.abs(totalDrag) > 150) { // Threshold for swipe dismissal
                                onAction(QuickMenuAction.Dismiss)
                            }
                        }
                    )
                }
                .clickable(enabled = false) {}
                .clip(RoundedCornerShape(28.dp))
                .background(backgroundColor)
                .padding(16.dp) // Reduced main padding from 20dp
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Circle, null, tint = Color(0xFF30C1F4), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Muhex Launcher", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.Close, "Close", tint = secondaryContentColor, modifier = Modifier.size(20.dp).clickable { onAction(QuickMenuAction.Dismiss) })
            }

            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer from 12dp

            Row(modifier = Modifier.weight(1f)) {
                // Left Column: App List
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    val sortedApps = apps.sortedBy { it.label.lowercase() }
                    var lastLetter = ""
                    sortedApps.forEach { app ->
                        val currentLetter = app.label.take(1).uppercase()
                        val showLetter = currentLetter != lastLetter
                        lastLetter = currentLetter
                        AppRealItem(if (showLetter) currentLetter else "", app, contentColor) { 
                            onAppClick(app)
                            onAction(QuickMenuAction.Dismiss)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Column: Categories & Recents
                Column(modifier = Modifier.weight(0.55f)) {
                    Text("Productivity", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(itemBgColor).padding(6.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                GridItem(Icons.Default.Wifi, "Wi-Fi", Color(0xFF4285F4), contentColor) { onAction(QuickMenuAction.Wifi) }
                                GridItem(Icons.Default.Bluetooth, "BT", Color(0xFF34A853), contentColor) { onAction(QuickMenuAction.Bluetooth) }
                                GridItem(Icons.Default.FlashlightOn, "Flash", Color(0xFFFBBC05), contentColor) { onAction(QuickMenuAction.Flashlight) }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                GridItem(Icons.Default.Calculate, "Calc", Color(0xFFEA4335), contentColor) { onAction(QuickMenuAction.Calculator) }
                                GridItem(Icons.Default.CalendarMonth, "Cal", Color(0xFF9C27B0), contentColor) { onAction(QuickMenuAction.Calendar) }
                                GridItem(Icons.AutoMirrored.Filled.Send, "Share", Color(0xFF0F9D58), contentColor) { /* Share action */ }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

                    Text("Recent Apps", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(itemBgColor).padding(6.dp)) {
                        if (recentApps.isEmpty()) {
                            Text("No recent apps", color = secondaryContentColor, fontSize = 10.sp, modifier = Modifier.padding(8.dp))
                        } else {
                            Column {
                                recentApps.chunked(3).take(2).forEach { rowApps ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        rowApps.forEach { app ->
                                            val iconBitmap = remember(app.icon) { app.icon.toBitmap().asImageBitmap() }
                                            Column(
                                                modifier = Modifier.weight(1f).clickable { onAppClick(app); onAction(QuickMenuAction.Dismiss) }.padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(22.dp)) // Reduced icon size from 24dp
                                                Text(app.label, color = contentColor, fontSize = 8.sp, maxLines = 1, textAlign = TextAlign.Center)
                                            }
                                        }
                                        repeat(3 - rowApps.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

            // Bottom Stats / Taskbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), // Changed from fixed 48dp height
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = "Apps",
                        tint = Color(0xFF30C1F4),
                        modifier = Modifier.size(22.dp).clickable { onAction(QuickMenuAction.Apps) } // Reduced from 24dp
                    )
                    Spacer(modifier = Modifier.width(12.dp)) // Reduced from 16dp
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = secondaryContentColor,
                        modifier = Modifier.size(20.dp).clickable { onAction(QuickMenuAction.Search) } // Reduced from 22dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Files",
                        tint = Color(0xFFFBBC05),
                        modifier = Modifier.size(20.dp).clickable { onAction(QuickMenuAction.Files) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Web",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(20.dp).clickable { onAction(QuickMenuAction.Browser) }
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeFormatter.format(currentTime),
                        color = contentColor,
                        fontSize = 12.sp, // Reduced from 13.sp
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormatter.format(currentTime),
                        color = secondaryContentColor,
                        fontSize = 9.sp // Reduced from 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRealItem(letter: String, app: AppModel, color: Color, onClick: () -> Unit) {
    val iconBitmap = remember(app.icon) { app.icon.toBitmap().asImageBitmap() }
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { // Reduced vertical padding
        Text(letter, color = color.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(20.dp)) // Reduced from 22dp
        Spacer(modifier = Modifier.width(10.dp))
        Text(app.label, color = color, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun RowScope.GridItem(icon: ImageVector, label: String, iconColor: Color, labelColor: Color, onClick: () -> Unit = {}) {
    Column(modifier = Modifier.weight(1f).clickable { onClick() }.padding(2.dp), horizontalAlignment = Alignment.CenterHorizontally) { // Reduced padding
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { // Reduced from 32dp
            Icon(icon, label, tint = iconColor, modifier = Modifier.size(18.dp)) // Reduced from 20dp
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = labelColor, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1) // Reduced from 9sp
    }
}
