package com.example.myapplication

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Helper to map app labels/packages to Material Design Icons.
 */
object  IconMapper {
    fun getMaterialIcon(label: String, packageName: String): ImageVector {
        val l = label.lowercase()
        val p = packageName.lowercase()

        return when {
            l.contains("phone") || p.contains("telecom") || p.contains("dialer") -> Icons.Default.Phone
            l.contains("camera") -> Icons.Default.CameraAlt
            l.contains("message") || l.contains("sms") || p.contains("messaging") -> Icons.AutoMirrored.Filled.Message
            l.contains("chat") || l.contains("whatsapp") || l.contains("telegram") -> Icons.AutoMirrored.Filled.Chat
            l.contains("setting") || p.contains("settings") -> Icons.Default.Settings
            l.contains("browser") || l.contains("chrome") || p.contains("browser") -> Icons.Default.Language
            l.contains("photo") || l.contains("gallery") || p.contains("gallery") -> Icons.Default.Image
            l.contains("music") || l.contains("player") || p.contains("audio") -> Icons.Default.MusicNote
            l.contains("video") || l.contains("youtube") || l.contains("netflix") -> Icons.Default.PlayCircle
            l.contains("calc") -> Icons.Default.Calculate
            l.contains("calen") -> Icons.Default.CalendarMonth
            l.contains("clock") || l.contains("alarm") -> Icons.Default.Schedule
            l.contains("map") -> Icons.Default.Place
            l.contains("mail") || l.contains("gmail") -> Icons.Default.Email
            l.contains("store") || l.contains("play") -> Icons.Default.ShoppingBag
            l.contains("file") || l.contains("folder") || l.contains("manager") -> Icons.Default.Folder
            l.contains("contact") -> Icons.Default.Contacts
            l.contains("note") -> Icons.Default.Description
            l.contains("tool") -> Icons.Default.Build
            l.contains("search") || l.contains("google") -> Icons.Default.Search
            else -> Icons.Default.Apps // Generic fallback
        }
    }
}
