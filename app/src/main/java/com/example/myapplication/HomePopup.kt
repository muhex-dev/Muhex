package com.example.myapplication

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Actions that can be performed from the Home Popup
 */
sealed class HomePopupAction {
    object Wallpaper : HomePopupAction()
    object Muhex : HomePopupAction()
    object Settings : HomePopupAction()
    object Dock : HomePopupAction()
    object PinnedApps : HomePopupAction()
    object Gestures : HomePopupAction()
    object Dismiss : HomePopupAction()
}

/**
 * HomePopup: A premium Compose-based customization menu.
 * Handles UI rendering and notifies the host of actions.
 */
@Composable
fun HomePopup(
    isVisible: Boolean,
    onAction: (HomePopupAction) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
    val contentColor = if (isDark) Color.White else Color.Black
    val iconBackground = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable { onAction(HomePopupAction.Dismiss) },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 120.dp)
                    .clickable(enabled = false) {} 
                    .fillMaxWidth(0.9f) 
                    .clip(RoundedCornerShape(32.dp))
                    .background(backgroundColor)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Home Screen",
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CustomizationItem(
                        icon = Icons.Default.Image,
                        label = "Wallpaper",
                        contentColor = contentColor,
                        iconBackground = iconBackground,
                        onClick = { onAction(HomePopupAction.Wallpaper) }
                    )
                    CustomizationItem(
                        icon = Icons.Default.Add,
                        label = "Muhex",
                        contentColor = contentColor,
                        iconBackground = iconBackground,
                        onClick = { onAction(HomePopupAction.Muhex) }
                    )
                    CustomizationItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        contentColor = contentColor,
                        iconBackground = iconBackground,
                        onClick = { onAction(HomePopupAction.Settings) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CustomizationItem(
    icon: ImageVector,
    label: String,
    contentColor: Color,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp) // Reverted to a more standard size since we have more room in 2 rows
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp) // Standard icon size
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
    }
}
