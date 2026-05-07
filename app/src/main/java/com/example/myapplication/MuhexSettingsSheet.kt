package com.example.myapplication

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun MuhexSettingsSheet(
    prefs: SharedPreferences,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF0F0F0F) else Color(0xFFF8F9FA)
    val contentColor = if (isDark) Color.White else Color.Black

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    
    val offset = remember { Animatable(screenHeightPx) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            offset.animateTo(screenHeightPx * 0.05f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
        } else {
            offset.animateTo(screenHeightPx, tween(400))
        }
    }

    if (offset.value >= screenHeightPx && !isVisible) return

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - (offset.value / screenHeightPx)).coerceIn(0f, 0.7f)))
                .clickable { onDismiss() }
        )

        Surface(
            modifier = Modifier
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .fillMaxWidth()
                .fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = backgroundColor,
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .background(contentColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    )
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 8.dp, end = 24.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = contentColor)
                    }
                    Text(
                        text = "Muhex Pro",
                        color = contentColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "PREMIUM",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item { MuhexSectionHeader("Look & Feel", contentColor) }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Palette,
                            title = "Icon Pack",
                            subtitle = "Adaptive, themed, or custom icons",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Style,
                            title = "Theme Engine",
                            subtitle = "Material You, Glassmorphism, or OLED Black",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.FontDownload,
                            title = "Typography",
                            subtitle = "Custom fonts for labels and clock",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.BlurCircular,
                            title = "Blur & Transparency",
                            subtitle = "Control UI depth and glass strength",
                            contentColor = contentColor
                        )
                    }

                    item { MuhexSectionHeader("Home & Layout", contentColor) }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.GridOn,
                            title = "Desktop Grid",
                            subtitle = "Columns, rows, and subgrid positioning",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.VerticalAlignBottom,
                            title = "Advanced Dock",
                            subtitle = "Infinite scroll, 2nd row, and hidden dock",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.FolderOpen,
                            title = "Folder Style",
                            subtitle = "Immersive, windowed, or pixel style",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Search,
                            title = "Search Bar",
                            subtitle = "Providers, styles, and animation",
                            contentColor = contentColor
                        )
                    }

                    item { MuhexSectionHeader("Interactions", contentColor) }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Gesture,
                            title = "Gestures",
                            subtitle = "Swipes, pinches, and double taps",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "Notification Badges",
                            subtitle = "Dynamic dots, numeric counts, and colors",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.AdsClick,
                            title = "Popup Menus",
                            subtitle = "Custom shortcuts and app info styles",
                            contentColor = contentColor
                        )
                    }

                    item { MuhexSectionHeader("App Drawer", contentColor) }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Apps,
                            title = "Drawer Layout",
                            subtitle = "Vertical, Horizontal, or Fast Scroll",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.AutoMirrored.Filled.Label,
                            title = "Tabs & Categories",
                            subtitle = "Auto-organize or manual groups",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.History,
                            title = "Recent Apps",
                            subtitle = "Top row predictions and history",
                            contentColor = contentColor
                        )
                    }

                    item { MuhexSectionHeader("Privacy & Security", contentColor) }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.VisibilityOff,
                            title = "Hidden Apps",
                            subtitle = "Encrypt and hide from all menus",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Lock,
                            title = "App Locker",
                            subtitle = "Biometric or Pattern protection",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Security,
                            title = "Stealth Mode",
                            subtitle = "Disguise launcher as a calculator",
                            contentColor = contentColor
                        )
                    }

                    item { MuhexSectionHeader("Advanced", contentColor) }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.AutoFixHigh,
                            title = "Animation Speed",
                            subtitle = "Relaxed, Stock, or Ludicrous speed",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.CloudSync,
                            title = "Cloud Backup",
                            subtitle = "Sync layout across devices",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Memory,
                            title = "Performance",
                            subtitle = "RAM optimization and frame limiting",
                            contentColor = contentColor
                        )
                    }
                    item {
                        MuhexSettingItem(
                            icon = Icons.Default.Science,
                            title = "Labs",
                            subtitle = "Experimental beta features",
                            contentColor = contentColor
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Muhex Launcher Pro",
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Build 2024.12.01-PRO • v1.4.0",
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Thank you for supporting Muhex development!",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MuhexSectionHeader(text: String, contentColor: Color) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 12.dp)
    )
}

@Composable
fun MuhexSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle action */ }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = contentColor.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = title,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.2f),
            modifier = Modifier.size(20.dp)
        )
    }
}
