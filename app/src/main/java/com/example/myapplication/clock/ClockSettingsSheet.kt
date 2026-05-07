package com.example.myapplication.clock

import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val colorPalette = listOf(
    Color.White, Color.Black, Color.Gray, Color.Red, Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
    Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFFEB3B), Color(0xFFFFC107)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSettingsSheet(
    prefs: SharedPreferences,
    isVisible: Boolean,
    onOpenFontPicker: (key: String, title: String) -> Unit,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var sheetOpacity by remember { mutableFloatStateOf(prefs.getFloat("sheet_transparency", 1.0f)) }
    val backgroundColor = (if (isDark) Color(0xFF1C1C1E) else Color.White).copy(alpha = sheetOpacity)
    val contentColor = if (isDark) Color.White else Color.Black

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val topLimitPx = with(density) { 60.dp.toPx() }
    
    val offset = remember { Animatable(screenHeightPx) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isVisible) {
        if (isVisible) {
            offset.animateTo(screenHeightPx * 0.05f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
        } else {
            offset.animateTo(screenHeightPx, tween(400, easing = FastOutSlowInEasing))
            onDismissFinished()
        }
    }

    if (offset.value >= screenHeightPx && !isVisible) return

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
                HeaderHandle(
                    contentColor = contentColor,
                    sheetOpacity = sheetOpacity,
                    onOpacityChange = {
                        sheetOpacity = it
                        prefs.edit().putFloat("sheet_transparency", it).apply()
                    },
                    onDrag = { dragAmount ->
                        val newOffset = (offset.value + dragAmount).coerceIn(topLimitPx, screenHeightPx)
                        scope.launch { offset.snapTo(newOffset) }
                    },
                    onDragEnd = {
                        if (offset.value > screenHeightPx * 0.8f) {
                            scope.launch {
                                offset.animateTo(screenHeightPx, tween(300))
                                onDismiss()
                            }
                        }
                    }
                )

                Box(modifier = Modifier.weight(1f)) {
                    ClockSettingsContent(prefs, contentColor, onOpenFontPicker, onDismiss)
                }
            }
        }
    }
}

@Composable
private fun ClockSettingsContent(prefs: SharedPreferences, contentColor: Color, onOpenFontPicker: (String, String) -> Unit, onDismiss: () -> Unit) {
    var expandedItem by remember { mutableStateOf<String?>(null) }
    val clockIndex = prefs.getInt("clock_index", 0)
    
    // Check if current clock is Rotating for specific settings
    val isRotating = clockIndex == 0

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = contentColor.copy(alpha = 0.5f))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Configuring: ${getClockName(clockIndex)}",
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            val visSub = if (isRotating) "Main, Seconds, Date, Rings" else "Show/Hide Elements"
            SettingItem(Icons.Default.Visibility, "Visibility", visSub, contentColor, expandedItem == "vis", onClick = { expandedItem = if (expandedItem == "vis") null else "vis" }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingToggle("Show Clock", prefs.getBoolean("clock_show_main", true), contentColor) { prefs.edit().putBoolean("clock_show_main", it).apply() }
                    if (isRotating) {
                        SettingToggle("Seconds Ring", prefs.getBoolean("clock_show_seconds", true), contentColor) { prefs.edit().putBoolean("clock_show_seconds", it).apply() }
                        SettingToggle("Ring Numbers", prefs.getBoolean("clock_show_ring_nums", true), contentColor) { prefs.edit().putBoolean("clock_show_ring_nums", it).apply() }
                        SettingToggle("Center Disc", prefs.getBoolean("clock_show_center_disc", true), contentColor) { prefs.edit().putBoolean("clock_show_center_disc", it).apply() }
                        SettingToggle("Aura Effect", prefs.getBoolean("clock_show_aura", true), contentColor) { prefs.edit().putBoolean("clock_show_aura", it).apply() }
                    }
                    SettingToggle("Show Date", prefs.getBoolean("clock_show_date", true), contentColor) { prefs.edit().putBoolean("clock_show_date", it).apply() }
                }
            }
        }

        item {
            SettingItem(Icons.Default.Settings, "Layout & Scale", "Position and sizing", contentColor, expandedItem == "layout", onClick = { expandedItem = if (expandedItem == "layout") null else "layout" }) {
                val config = LocalConfiguration.current
                val screenWidth = config.screenWidthDp.toFloat()
                val screenHeight = config.screenHeightDp.toFloat()
                Column {
                    SliderSettingFloat("Global Scale", prefs.getFloat("clock_scale", 1.0f), 0.5f..3.0f, contentColor) { prefs.edit().putFloat("clock_scale", it).apply() }
                    
                    if (isRotating) {
                        SliderSettingFloat("Pill Angle", prefs.getFloat("clock_pill_angle", 0f), 0f..360f, contentColor) { prefs.edit().putFloat("clock_pill_angle", it).apply() }
                        SliderSettingFloat("Pill Size", prefs.getFloat("clock_pill_size", 48f), 20f..150f, contentColor) { prefs.edit().putFloat("clock_pill_size", it).apply() }
                    }
                    
                    SliderSettingFloat("Horizontal Offset", prefs.getFloat("clock_horizontal", 0f), -screenWidth..screenWidth, contentColor) { prefs.edit().putFloat("clock_horizontal", it).apply() }
                    SliderSettingFloat("Vertical Offset", prefs.getFloat("clock_vertical", 0f), -screenHeight..screenHeight, contentColor) { prefs.edit().putFloat("clock_vertical", it).apply() }
                    
                    Divider(Modifier.padding(vertical = 12.dp), color = contentColor.copy(alpha = 0.1f))

                    SliderSettingFloat("Date Horizontal Offset", prefs.getFloat("clock_date_horizontal", 0f), -screenWidth..screenWidth, contentColor) { prefs.edit().putFloat("clock_date_horizontal", it).apply() }
                    SliderSettingFloat("Date Vertical Offset", prefs.getFloat("clock_date_vertical", 0f), -screenHeight..screenHeight, contentColor) { prefs.edit().putFloat("clock_date_vertical", it).apply() }

                    if (isRotating) {
                        Divider(Modifier.padding(vertical = 12.dp), color = contentColor.copy(alpha = 0.1f))
                        SliderSettingFloat("Ring Thickness", prefs.getFloat("clock_ring_thickness", 3f), 1f..20f, contentColor) { prefs.edit().putFloat("clock_ring_thickness", it).apply() }
                        SliderSettingFloat("Pill Thickness", prefs.getFloat("clock_pill_thickness", 2f), 1f..20f, contentColor) { prefs.edit().putFloat("clock_pill_thickness", it).apply() }
                    }
                    
                    Divider(Modifier.padding(vertical = 12.dp), color = contentColor.copy(alpha = 0.1f))
                    
                    SliderSettingFloat("Hour Font Size", prefs.getFloat("clock_hour_size", 120f), 20f..500f, contentColor) { prefs.edit().putFloat("clock_hour_size", it).apply() }
                    SliderSettingFloat("Date Font Size", prefs.getFloat("clock_date_size", 34f), 10f..200f, contentColor) { prefs.edit().putFloat("clock_date_size", it).apply() }
                }
            }
        }

        item {
            SettingItem(Icons.Default.Palette, "Colors & Theme", "Clock, Rings and Accents", contentColor, expandedItem == "color", onClick = { expandedItem = if (expandedItem == "color") null else "color" }) {
                Column {
                    ColorSection("Main Clock Color", colorPalette, prefs.getInt("clock_color", AndroidColor.WHITE)) { prefs.edit().putInt("clock_color", it.toArgb()).apply() }
                    ColorSection("Date & Info Color", colorPalette, prefs.getInt("clock_date_color", AndroidColor.WHITE)) { prefs.edit().putInt("clock_date_color", it.toArgb()).apply() }
                    if (isRotating) {
                        ColorSection("Ring Color", colorPalette, prefs.getInt("clock_ring_color", AndroidColor.WHITE)) { prefs.edit().putInt("clock_ring_color", it.toArgb()).apply() }
                        ColorSection("Ring Numbers Color", colorPalette, prefs.getInt("clock_ring_num_color", AndroidColor.WHITE)) { prefs.edit().putInt("clock_ring_num_color", it.toArgb()).apply() }
                        ColorSection("Center Disc Color", colorPalette, prefs.getInt("clock_center_disc_color", AndroidColor.parseColor("#10FFFFFF"))) { prefs.edit().putInt("clock_center_disc_color", it.toArgb()).apply() }
                        ColorSection("Aura Color", colorPalette, prefs.getInt("clock_aura_color", AndroidColor.parseColor("#20FFFFFF"))) { prefs.edit().putInt("clock_aura_color", it.toArgb()).apply() }
                    }
                }
            }
        }
        
        item {
            SettingItem(Icons.Default.TextFields, "Custom Text", "Personalized Label", contentColor, expandedItem == "custom_text", onClick = { expandedItem = if (expandedItem == "custom_text") null else "custom_text" }) {
                Column {
                    var text by remember { mutableStateOf(prefs.getString("clock_custom_text", "") ?: "") }
                    TextField(
                        value = text,
                        onValueChange = { text = it; prefs.edit().putString("clock_custom_text", it).apply() },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        placeholder = { Text("Enter custom text...", color = contentColor.copy(alpha = 0.4f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = contentColor.copy(alpha = 0.05f),
                            unfocusedContainerColor = contentColor.copy(alpha = 0.05f),
                            focusedTextColor = contentColor,
                            unfocusedTextColor = contentColor
                        )
                    )
                    SettingToggle("Show Custom Text", prefs.getBoolean("clock_show_custom_text", false), contentColor) { prefs.edit().putBoolean("clock_show_custom_text", it).apply() }
                    SliderSettingFloat("Text Size", prefs.getFloat("clock_custom_text_size", 34f), 10f..250f, contentColor) { prefs.edit().putFloat("clock_custom_text_size", it).apply() }
                    val config = LocalConfiguration.current
                    SliderSettingFloat("Horizontal Position", prefs.getFloat("clock_custom_text_horizontal", 0f), -config.screenWidthDp.toFloat()..config.screenWidthDp.toFloat(), contentColor) { prefs.edit().putFloat("clock_custom_text_horizontal", it).apply() }
                    SliderSettingFloat("Vertical Position", prefs.getFloat("clock_custom_text_vertical", 400f), -config.screenHeightDp.toFloat()..config.screenHeightDp.toFloat(), contentColor) { prefs.edit().putFloat("clock_custom_text_vertical", it).apply() }
                    ColorSection("Text Color", colorPalette, prefs.getInt("clock_custom_text_color", AndroidColor.WHITE)) { prefs.edit().putInt("clock_custom_text_color", it.toArgb()).apply() }
                }
            }
        }

        item {
            SettingItem(Icons.Default.FormatBold, "Typography", "Fonts and Styles", contentColor, expandedItem == "font_style", onClick = { expandedItem = if (expandedItem == "font_style") null else "font_style" }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            SettingToggle("Bold", prefs.getBoolean("clock_font_bold", true), contentColor) { prefs.edit().putBoolean("clock_font_bold", it).apply() }
                        }
                        Box(Modifier.weight(1f)) {
                            SettingToggle("Italic", prefs.getBoolean("clock_font_italic", false), contentColor) { prefs.edit().putBoolean("clock_font_italic", it).apply() }
                        }
                    }
                    
                    Button(
                        onClick = { onOpenFontPicker("clock_font_family", "Clock Font") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.FontDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Clock Font")
                    }
                    Button(
                        onClick = { onOpenFontPicker("date_font_family", "Date Font") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.FontDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Date Font")
                    }
                    Button(
                        onClick = { onOpenFontPicker("custom_text_font_family", "Custom Text Font") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.FontDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Custom Text Font")
                    }
                }
            }
        }
    }
}

private fun getClockName(index: Int): String {
    val names = listOf(
        "Rotating", "Classic", "Stack", "Info", 
        "Greeting", "Numeric", "Modern"
    )
    return if (index in names.indices) names[index] else "Unknown"
}

@Composable
private fun HeaderHandle(
    contentColor: Color,
    sheetOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = onDragEnd,
                    onVerticalDrag = { _, dragAmount -> onDrag(dragAmount) }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(30.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.2f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Opacity, null, tint = contentColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            Slider(
                value = sheetOpacity,
                onValueChange = onOpacityChange,
                valueRange = 0.5f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color(0xFF4CAF50), activeTrackColor = Color(0xFF4CAF50))
            )
            Text("${(sheetOpacity * 100).toInt()}%", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    contentColor: Color,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val borderColor = contentColor.copy(alpha = 0.08f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(contentColor.copy(alpha = 0.04f))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, color = contentColor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.5.sp)
                Text(value, color = Color(0xFF4CAF50).copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null,
                tint = contentColor.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingToggle(label: String, initialValue: Boolean, contentColor: Color, onCheckedChange: (Boolean) -> Unit) {
    var checked by remember(initialValue) { mutableStateOf(initialValue) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = contentColor, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50))
        )
    }
}

@Composable
private fun SliderSettingFloat(label: String, initialValue: Float, range: ClosedFloatingPointRange<Float>, color: Color, onValueChange: (Float) -> Unit) {
    var value by remember(initialValue) { mutableFloatStateOf(initialValue) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color, fontSize = 13.sp)
            Text("%.1f".format(value), color = color.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Slider(
            value = value,
            onValueChange = {
                value = it
                onValueChange(it)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4CAF50),
                activeTrackColor = Color(0xFF4CAF50),
                inactiveTrackColor = color.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun ColorSection(label: String, palette: List<Color>, selectedArgb: Int, onColorSelected: (Color) -> Unit) {
    var currentSelected by remember(selectedArgb) { mutableIntStateOf(selectedArgb) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            palette.take(8).forEach { color ->
                val isSelected = color.toArgb() == currentSelected
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color, CircleShape)
                        .border(2.dp, if (isSelected) Color(0xFF4CAF50) else Color.Transparent, CircleShape)
                        .clickable {
                            currentSelected = color.toArgb()
                            onColorSelected(color)
                        }
                )
            }
        }
    }
}