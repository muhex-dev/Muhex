package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * FontSettings: A premium Compose-based font picker and manager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSettings(
    prefs: SharedPreferences,
    targetKey: String, // "clock_font_family" or "date_font_family"
    title: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val uiContext = LocalContext.current
    val scope = rememberCoroutineScope()

    // States
    val defaultFont = if (targetKey == "clock_font_family") "sans-serif-condensed" else "sans-serif"
    var currentFont by remember {
        mutableStateOf(prefs.getString(targetKey, defaultFont) ?: defaultFont)
    }
    
    // Update current font when targetKey changes or becomes visible
    LaunchedEffect(targetKey, isVisible) {
        if (isVisible) {
            currentFont = prefs.getString(targetKey, defaultFont) ?: defaultFont
        }
    }

    val userFonts = remember { mutableStateListOf<File>() }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshFonts() {
        scope.launch {
            isRefreshing = true
            val fonts = withContext(Dispatchers.IO) { 
                val loaded = FontManager.loadUserFonts(context)
                delay(600)
                loaded
            }
            userFonts.clear()
            userFonts.addAll(fonts)
            isRefreshing = false
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val resultFile = withContext(Dispatchers.IO) { FontManager.importFont(context, it) }
                if (resultFile != null) {
                    refreshFonts()
                    Toast.makeText(uiContext, "Font imported", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(uiContext, "Import failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isRefreshing = true
                val count = withContext(Dispatchers.IO) { FontManager.importFontsFromFolder(context, it) }
                refreshFonts()
                if (count > 0) {
                    Toast.makeText(uiContext, "Imported $count fonts", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(uiContext, "No fonts found in folder", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val topLimitPx = with(density) { 60.dp.toPx() }

    val offset = remember { Animatable(screenHeightPx) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            refreshFonts()
            offset.animateTo(screenHeightPx * 0.1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
        } else {
            offset.animateTo(screenHeightPx, tween(400, easing = FastOutSlowInEasing))
            onDismissFinished()
        }
    }

    if (offset.value >= screenHeightPx && !isVisible) return

    val isDark = isSystemInDarkTheme()
    var sheetOpacity by remember { mutableFloatStateOf(prefs.getFloat("sheet_transparency", 1.0f)) }
    val backgroundColor = (if (isDark) Color(0xFF1C1C1E) else Color.White).copy(alpha = sheetOpacity)
    val contentColor = if (isDark) Color.White else Color.Black

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - (offset.value / screenHeightPx)).coerceIn(0f, 0.2f)))
                .clickable { onDismiss() }
        )

        Surface(
            modifier = Modifier
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .fillMaxWidth()
                .height(config.screenHeightDp.dp - with(density) { offset.value.toDp() }),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = backgroundColor,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Handle
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (offset.value > screenHeightPx * 0.8f) {
                                        scope.launch {
                                            offset.animateTo(screenHeightPx, tween(300))
                                            onDismiss()
                                        }
                                    }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    val newOffset = (offset.value + dragAmount).coerceIn(topLimitPx, screenHeightPx)
                                    scope.launch { offset.snapTo(newOffset) }
                                }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.2f))
                    )

                    // Transparency Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Opacity,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Slider(
                            value = sheetOpacity,
                            onValueChange = {
                                sheetOpacity = it
                                prefs.edit().putFloat("sheet_transparency", it).apply()
                            },
                            valueRange = 0.5f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF4CAF50),
                                activeTrackColor = Color(0xFF4CAF50),
                                inactiveTrackColor = contentColor.copy(alpha = 0.1f)
                            )
                        )
                        Text(
                            "${(sheetOpacity * 100).toInt()}%",
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp)
                        )
                    }
                }

                // Title bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = contentColor)
                    }
                    Text(
                        title,
                        color = contentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    
                    IconButton(onClick = { refreshFonts() }) {
                        val rotation = rememberInfiniteTransition().animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
                        )
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.rotate(if (isRefreshing) rotation.value else 0f)
                        )
                    }

                    IconButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Add Folder", tint = contentColor.copy(alpha = 0.7f))
                    }
                    
                    IconButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Font", tint = Color(0xFF4CAF50))
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item { FontSectionHeader("User Fonts") }
                    if (userFonts.isEmpty()) {
                        item { EmptyFontsMessage(contentColor) }
                    } else {
                        items(userFonts, key = { it.absolutePath }) { fontFile ->
                            val fontPath = "file://${fontFile.absolutePath}"
                            FontItemRow(
                                name = fontFile.nameWithoutExtension.substringBeforeLast("_"),
                                isSelected = currentFont == fontPath,
                                contentColor = contentColor,
                                typeface = try { Typeface.createFromFile(fontFile) } catch(e: Exception) { Typeface.DEFAULT },
                                onDelete = {
                                    if (FontManager.deleteUserFont(fontFile)) {
                                        refreshFonts()
                                        Toast.makeText(uiContext, "Deleted", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClick = {
                                    currentFont = fontPath
                                    prefs.edit { putString(targetKey, fontPath) }
                                }
                            )
                        }
                    }

                    item { FontSectionHeader("System Fonts") }
                    items(FontManager.systemFonts) { fontName ->
                        FontItemRow(
                            name = fontName,
                            isSelected = currentFont == fontName,
                            contentColor = contentColor,
                            typeface = Typeface.create(fontName, Typeface.NORMAL),
                            onClick = {
                                currentFont = fontName
                                prefs.edit { putString(targetKey, fontName) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFontsMessage(contentColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "No user fonts found.\nTap + or folder icon to import fonts",
            color = contentColor.copy(alpha = 0.4f),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun FontSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFF4CAF50),
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 12.dp)
    )
}

@Composable
fun FontItemRow(
    name: String,
    isSelected: Boolean,
    contentColor: Color,
    typeface: Typeface,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        color = if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.05f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayName = name.replace("_", " ").let {
                    if (it.isNotEmpty()) it.substring(0, 1).uppercase() + it.substring(1) else it
                }
                Text(
                    text = displayName,
                    color = if (isSelected) Color(0xFF4CAF50) else contentColor,
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily(typeface))
                )
                Text(
                    text = "The quick brown fox jumps over the lazy dog",
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    style = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily(typeface))
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

object FontManager {
    private const val TAG = "FontManager"
    private const val USER_FONTS_DIR = "user_fonts"

    // Thread-safe cache for resolved typefaces to improve performance
    private val typefaceCache = java.util.concurrent.ConcurrentHashMap<String, Typeface>()

    val systemFonts = listOf(
        "sans-serif",
        "sans-serif-medium",
        "sans-serif-condensed",
        "serif",
        "monospace",
        "casual"
    )

    /**
     * Loads all imported user fonts from the internal directory.
     */
    fun loadUserFonts(context: Context): List<File> {
        val dir = File(context.filesDir, USER_FONTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir.listFiles()?.filter { file ->
            file.isFile && (file.name.lowercase().endsWith(".ttf") || file.name.lowercase().endsWith(".otf"))
        }?.sortedBy { it.name } ?: emptyList()
    }

    /**
     * Deletes a user font and clears the cache to ensure the UI updates.
     */
    fun deleteUserFont(fontFile: File): Boolean {
        return try {
            if (fontFile.exists()) {
                val deleted = fontFile.delete()
                if (deleted) clearCache()
                deleted
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete font: ${fontFile.path}", e)
            false
        }
    }

    /**
     * Imports a font from a Uri (e.g., from a file picker) into internal storage.
     */
    fun importFont(context: Context, uri: Uri): File? {
        return try {
            val (safeName, _) = getSafeFileName(context, uri)
            val dir = File(context.filesDir, USER_FONTS_DIR)
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                clearCache()
                destFile
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            null
        }
    }

    /**
     * Imports all fonts from a selected folder (DocumentTree).
     */
    fun importFontsFromFolder(context: Context, treeUri: Uri): Int {
        var count = 0
        try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return 0
            val files = root.listFiles()
            files.forEach { file ->
                if (file.isFile) {
                    val name = file.name?.lowercase() ?: ""
                    if (name.endsWith(".ttf") || name.endsWith(".otf")) {
                        if (importFont(context, file.uri) != null) {
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Folder import failed", e)
        }
        return count
    }

    /**
     * Generates a safe filename for importing, preventing collisions and sanitizing names.
     */
    fun getSafeFileName(context: Context, uri: Uri): Pair<String, String> {
        var displayName = ""
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) displayName = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Query fail", e)
            }
        }
        if (displayName.isEmpty()) {
            displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "font_${System.currentTimeMillis()}.ttf"
        }

        val ext = displayName.substringAfterLast(".", "ttf").lowercase()
        val finalExt = if (ext == "ttf" || ext == "otf") ext else "ttf"
        // Sanitize base name: remove special chars
        val base = displayName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9]"), "_")

        return Pair("${base}_${System.currentTimeMillis()}.$finalExt", displayName)
    }

    /**
     * Resolves a font path or system font name into a Typeface.
     * Uses a cache to avoid expensive file/asset operations on every UI frame.
     */
    fun resolveTypeface(fontPath: String?, style: Int = Typeface.NORMAL): Typeface {
        if (fontPath.isNullOrEmpty()) return Typeface.create(Typeface.DEFAULT, style)
        
        val cacheKey = "$fontPath|$style"
        typefaceCache[cacheKey]?.let { return it }

        val typeface = try {
            val file = when {
                fontPath.startsWith("file://") -> File(fontPath.substring(7))
                fontPath.startsWith("/") || fontPath.contains(File.separator) -> File(fontPath)
                else -> null
            }

            if (file != null && file.exists()) {
                Typeface.create(Typeface.createFromFile(file), style)
            } else {
                // System font or resource-based font name
                Typeface.create(fontPath, style)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving typeface for $fontPath", e)
            Typeface.create(Typeface.DEFAULT, style)
        }

        typefaceCache[cacheKey] = typeface
        return typeface
    }

    /**
     * Forces a clear of the typeface cache.
     */
    fun clearCache() {
        typefaceCache.clear()
    }
}
