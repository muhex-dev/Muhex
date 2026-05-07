package com.example.myapplication

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.appwidget.AppWidgetManager
import android.app.role.RoleManager
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val check: (Context) -> Boolean,
    val request: (Activity) -> Unit
)

@Composable
fun PermissionCheckerPopup(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    val permissions = remember {
        listOf(
            PermissionItem(
                "accessibility",
                "Accessibility Service",
                "Needed for locking the screen with a gesture.",
                Icons.Default.AccessibilityNew,
                { ctx -> isAccessibilityServiceEnabled(ctx, ScreenLockService::class.java) },
                { act ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    act.startActivity(intent)
                }
            ),
            PermissionItem(
                "storage",
                "Storage Access",
                "Needed to access and set your custom wallpapers.",
                Icons.Default.Storage,
                { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    } else {
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    }
                },
                { act ->
                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                    
                    if (!androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(act, perm) && 
                        androidx.core.content.ContextCompat.checkSelfPermission(act, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", act.packageName, null)
                        }
                        act.startActivity(intent)
                    } else {
                        androidx.core.app.ActivityCompat.requestPermissions(act, arrayOf(perm), 1001)
                    }
                }
            ),
            PermissionItem(
                "launcher",
                "Default Launcher",
                "Set as default to use all features smoothly.",
                Icons.Default.Home,
                { ctx -> isDefaultLauncher(ctx) },
                { act -> requestDefaultLauncher(act) }
            )
        )
    }

    var pendingPermissions by remember { mutableStateOf(permissions.filter { !it.check(context) }) }
    var dismissedItems by remember { mutableStateOf(setOf<String>()) }

    // Re-check permissions when activity resumes
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val currentPending = permissions.filter { !it.check(context) }
            pendingPermissions = currentPending
            if (currentPending.isEmpty()) {
                onAllGranted()
                break
            }
        }
    }

    // Filter out items the user has chosen to skip for now
    val visiblePending = pendingPermissions.filter { it.id !in dismissedItems }

    if (visiblePending.isEmpty()) {
        // If all remaining permissions were dismissed, we consider it "done" for this session
        if (pendingPermissions.isNotEmpty()) {
             // We don't call onAllGranted() here because they aren't actually granted,
             // but the popup will disappear.
        }
        return
    }

    val current = visiblePending.first()

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(current.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(current.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    current.description,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { dismissedItems = dismissedItems + current.id },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Later")
                    }
                    
                    Button(
                        onClick = { current.request(activity) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Grant")
                    }
                }

                if (visiblePending.size > 1) {
                    Text(
                        "${visiblePending.size} permissions remaining",
                        modifier = Modifier.padding(top = 12.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (enabledService in enabledServices) {
        val enabledServiceInfo = enabledService.resolveInfo.serviceInfo
        if (enabledServiceInfo.packageName == context.packageName && enabledServiceInfo.name == service.name) {
            return true
        }
    }
    return false
}

fun isDefaultLauncher(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    } else {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }
}

fun requestDefaultLauncher(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        activity.startActivityForResult(intent, 1002)
    } else {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
    }
}
