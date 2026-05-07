package com.example.myapplication

import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ─── Muhex Premium Design Tokens ───────────────────────────────────────────
private val MuhexGreen        = Color(0xFF00E676)
private val MuhexGreenDim     = Color(0xFF00C853)
private val MuhexGreenGlow    = Color(0xFF00E676).copy(alpha = 0.18f)
private val MuhexSurface      = Color(0xFF0D0D0F)
private val MuhexSurface2     = Color(0xFF141418)
private val MuhexSurface3     = Color(0xFF1C1C22)
private val MuhexBorder       = Color(0xFF2A2A35)
private val MuhexBorderBright = Color(0xFF3A3A48)
private val MuhexText         = Color(0xFFF0F0F8)
private val MuhexTextMid      = Color(0xFF8888AA)
private val MuhexTextDim      = Color(0xFF44445A)
private val MuhexAccentBlue   = Color(0xFF448AFF)
private val MuhexAccentPurple = Color(0xFFBB86FC)

private val colorPalette = listOf(
    Color.White, Color(0xFFF0F0F8), Color(0xFF8888AA), Color(0xFF44445A),
    Color(0xFF0D0D0F), Color.Black,
    Color(0xFF00E676), Color(0xFF00C853), Color(0xFF69F0AE),
    Color(0xFF448AFF), Color(0xFF82B1FF), Color(0xFF2979FF),
    Color(0xFFBB86FC), Color(0xFFCF6679), Color(0xFFFF6D00), Color(0xFFFFD740)
)

// ─── Entry Point ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSettingsSheet(
    repository: AppRepository,
    prefs: SharedPreferences,
    isVisible: Boolean,
    initialTab: Int = 0,
    onOpenFontPicker: (key: String, title: String) -> Unit,
    onOpenClockSettings: () -> Unit = {},
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit = {}
) {
    val config   = LocalConfiguration.current
    val density  = LocalDensity.current
    val scope    = rememberCoroutineScope()

    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val topLimitPx     = with(density) { 48.dp.toPx() }

    var sheetOpacity by remember { mutableFloatStateOf(prefs.getFloat("sheet_transparency", 1f)) }
    val offset = remember { Animatable(screenHeightPx) }

    // Pref-backed toggles (drive tab visibility)
    var isClockViewEnabled by remember { mutableStateOf(prefs.getBoolean("clock_view_enabled",  true)) }
    var showNowApps        by remember { mutableStateOf(prefs.getBoolean("show_now_apps",        true)) }
    var showDockBar        by remember { mutableStateOf(prefs.getBoolean("show_dock_bar",         true)) }
    var gesturesEnabled    by remember { mutableStateOf(prefs.getBoolean("gestures_enabled",     true)) }
    var quotesEnabled      by remember { mutableStateOf(prefs.getBoolean("quotes_enabled",       true)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "clock_view_enabled" -> isClockViewEnabled = p.getBoolean(key, true)
                "show_now_apps"      -> showNowApps        = p.getBoolean(key, true)
                "show_dock_bar"      -> showDockBar        = p.getBoolean(key, true)
                "gestures_enabled"   -> gesturesEnabled    = p.getBoolean(key, true)
                "quotes_enabled"     -> quotesEnabled      = p.getBoolean(key, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Sheet animation
    LaunchedEffect(isVisible) {
        if (isVisible) {
            offset.animateTo(
                screenHeightPx * 0.04f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        } else {
            offset.animateTo(screenHeightPx, tween(380, easing = FastOutSlowInEasing))
            onDismissFinished()
        }
    }

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    LaunchedEffect(initialTab) { selectedTab = initialTab }

    if (offset.value >= screenHeightPx && !isVisible) return
    BackHandler { onDismiss() }

    // Scrim
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ((1f - offset.value / screenHeightPx) * 0.6f).coerceIn(0f, 0.6f)))
                .clickable { onDismiss() }
        )

        // Sheet surface
        val sheetBg = MuhexSurface.copy(alpha = sheetOpacity)
        Box(
            modifier = Modifier
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(sheetBg)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(MuhexBorderBright, Color.Transparent)),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (offset.value > screenHeightPx * 0.75f) {
                                scope.launch {
                                    offset.animateTo(screenHeightPx, tween(320))
                                    onDismiss()
                                }
                            } else {
                                scope.launch {
                                    offset.animateTo(
                                        screenHeightPx * 0.04f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            val newOff = (offset.value + dragAmount).coerceIn(topLimitPx, screenHeightPx)
                            scope.launch { offset.snapTo(newOff) }
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Drag handle + top glow
                MuhexSheetHeader(
                    sheetOpacity = sheetOpacity,
                    onOpacityChange = {
                        sheetOpacity = it
                        prefs.edit().putFloat("sheet_transparency", it).apply()
                    },
                    onDismiss = onDismiss
                )

                // ── Tab bar
                val tabs = buildList {
                    add("Home"     to Icons.Default.HomeWork)
                    add("Layouts"  to Icons.Default.Dashboard)
                    if (isClockViewEnabled) add("Clock"    to Icons.Default.WatchLater)
                    if (showNowApps)        add("Pinned"   to Icons.Default.PushPin)
                    add("Drawer"   to Icons.Default.GridView)
                    if (gesturesEnabled)    add("Gestures" to Icons.Default.TouchApp)
                    if (showDockBar)        add("Dock"     to Icons.Default.ViewAgenda)
                    if (quotesEnabled)      add("Quotes"   to Icons.Default.FormatQuote)
                    add("About"    to Icons.Default.Info)
                }

                LaunchedEffect(tabs.size) {
                    if (selectedTab >= tabs.size) selectedTab = 0
                }

                MuhexTabRow(
                    tabs       = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                // ── Content
                Box(modifier = Modifier.weight(1f)) {
                    when (tabs.getOrNull(selectedTab)?.first) {
                        "Home"     -> HomeTab(prefs, repository)
                        "Layouts"  -> LayoutsTab(prefs)
                        "Clock"    -> ClockTab(prefs, onOpenClockSettings)
                        "Pinned"   -> PinnedAppsTab(repository, prefs, onOpenFontPicker)
                        "Drawer"   -> DrawerTab(prefs, onOpenFontPicker)
                        "Gestures" -> GesturesTab(repository, prefs)
                        "Dock"     -> DockTab(repository, prefs, onOpenFontPicker)
                        "Quotes"   -> QuotesTab(prefs)
                        "About"    -> AboutTab()
                    }
                }
            }
        }
    }
}

// ─── Header ────────────────────────────────────────────────────────────────
@Composable
private fun MuhexSheetHeader(
    sheetOpacity: Float,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top glow line
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(48.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, MuhexGreen, Color.Transparent))
                )
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo mark
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MuhexGreenGlow, CircleShape)
                    .border(1.dp, MuhexGreen.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("M", color = MuhexGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "MUHEX",
                    color = MuhexText,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    letterSpacing = 3.sp
                )
                Text(
                    "Launcher Settings",
                    color = MuhexTextMid,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }

            // Opacity control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Opacity, null,
                    tint = MuhexTextDim,
                    modifier = Modifier.size(14.dp)
                )
                Slider(
                    value = sheetOpacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.4f..1f,
                    modifier = Modifier.width(80.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MuhexGreen,
                        activeTrackColor = MuhexGreen,
                        inactiveTrackColor = MuhexBorder
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Divider with glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, MuhexBorderBright, MuhexGreen.copy(alpha = 0.3f), MuhexBorderBright, Color.Transparent)
                    )
                )
        )
    }
}

// ─── Tab Row ───────────────────────────────────────────────────────────────
@Composable
private fun MuhexTabRow(
    tabs: List<Pair<String, ImageVector>>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor   = Color.Transparent,
        contentColor     = MuhexGreen,
        edgePadding      = 16.dp,
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, MuhexGreen, Color.Transparent)
                            )
                        )
                )
            }
        },
        divider = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MuhexBorder)
            )
        }
    ) {
        tabs.forEachIndexed { index, (title, icon) ->
            val selected = selectedTab == index
            val color by animateColorAsState(
                targetValue = if (selected) MuhexGreen else MuhexTextDim,
                animationSpec = tween(200)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(index) }
                    .background(if (selected) MuhexGreenGlow else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
                    if (selected) {
                        Text(
                            title,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── HOME TAB — Dashboard overview ─────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun HomeTab(prefs: SharedPreferences, repository: AppRepository) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MuhexSurface3, Color(0xFF0A1F0A), MuhexSurface3)
                        )
                    )
                    .border(1.dp, MuhexGreen.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Decorative circles
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = MuhexGreen.copy(alpha = 0.05f),
                        radius = size.width * 0.4f,
                        center = Offset(size.width * 0.85f, size.height * 0.5f)
                    )
                    drawCircle(
                        color = MuhexGreen.copy(alpha = 0.08f),
                        radius = size.width * 0.2f,
                        center = Offset(size.width * 0.1f, size.height * 0.2f)
                    )
                }
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "MUHEX LAUNCHER",
                        color = MuhexGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Premium • Customizable • Yours",
                        color = MuhexTextMid,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        item {
            Text(
                "QUICK CONTROLS",
                color = MuhexTextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Quick toggle grid
        val quickToggles = listOf(
            Triple("Clock",    "clock_view_enabled",  Icons.Default.WatchLater),
            Triple("Pinned",   "show_now_apps",        Icons.Default.PushPin),
            Triple("Dock",     "show_dock_bar",         Icons.Default.ViewAgenda),
            Triple("Gestures", "gestures_enabled",     Icons.Default.TouchApp),
            Triple("Quotes",   "quotes_enabled",       Icons.Default.FormatQuote),
            Triple("Haptic",   "haptic_enabled",       Icons.Default.Vibration)
        )

        item {
            val columns = 3
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quickToggles.chunked(columns).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { (label, prefKey, icon) ->
                            var enabled by remember { mutableStateOf(prefs.getBoolean(prefKey, true)) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (enabled) MuhexGreenGlow else MuhexSurface3)
                                    .border(
                                        1.dp,
                                        if (enabled) MuhexGreen.copy(alpha = 0.5f) else MuhexBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        enabled = !enabled
                                        prefs.edit().putBoolean(prefKey, enabled).apply()
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        icon, null,
                                        tint = if (enabled) MuhexGreen else MuhexTextDim,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        label,
                                        color = if (enabled) MuhexText else MuhexTextDim,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        // fill remainder
                        repeat(columns - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item { MuhexDivider() }

        item {
            Text(
                "SYSTEM INFO",
                color = MuhexTextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Version",      "Muhex 1.0")
                InfoRow("Package",      "com.example.myapplication")
                InfoRow("Build",        "Release")
                InfoRow("Theme Engine", "Jetpack Compose")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MuhexSurface3)
            .border(1.dp, MuhexBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MuhexTextMid, fontSize = 13.sp)
        Text(value, color = MuhexText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── LAYOUTS TAB ───────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun LayoutsTab(prefs: SharedPreferences) {
    val layouts = listOf(
        UnifiedHomeItem("Clock View",      "Stylized time display on home",   Icons.Default.WatchLater,    "clock_view_enabled"),
        UnifiedHomeItem("Pinned Stack",    "Samsung-style app cards",         Icons.Default.PushPin,       "show_now_apps"),
        UnifiedHomeItem("Fixed Dock",      "Quick access bottom bar",         Icons.Default.ViewAgenda,    "show_dock_bar"),
        UnifiedHomeItem("Gestures",        "System-wide gesture control",     Icons.Default.TouchApp,      "gestures_enabled"),
        UnifiedHomeItem("Quotes",          "Motivational home screen quotes", Icons.Default.FormatQuote,   "quotes_enabled"),
        UnifiedHomeItem("Haptic Feedback", "Touch vibration responses",       Icons.Default.Vibration,     "haptic_enabled"),
        UnifiedHomeItem("Status Bar",      "Show transparent status bar",     Icons.Default.PhoneAndroid,  "status_bar_transparent"),
        UnifiedHomeItem("Nav Gestures",    "Full-screen gesture navigation",  Icons.Default.SwipeUp,       "nav_gesture_enabled")
    )

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionLabel("LAYOUT MODULES")
            Spacer(Modifier.height(4.dp))
            Text(
                "Enable or disable each module of your launcher experience.",
                color = MuhexTextMid,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(12.dp))
        }

        items(layouts) { item ->
            var isEnabled by remember { mutableStateOf(prefs.getBoolean(item.prefKey, true)) }
            MuhexToggleCard(
                icon        = item.icon,
                title       = item.name,
                subtitle    = item.description,
                checked     = isEnabled,
                onToggle    = {
                    isEnabled = it
                    prefs.edit().putBoolean(item.prefKey, it).apply()
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── CLOCK TAB ─────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ClockTab(prefs: SharedPreferences, onOpenClockSettings: () -> Unit) {
    val clockStyles = listOf("Rotating", "Classic", "Stack", "Info", "Greeting", "Numeric", "Modern")
    var selectedIndex     by remember { mutableIntStateOf(prefs.getInt("clock_index", 0)) }
    var addedClocksOrder  by remember {
        val ordered = prefs.getString("added_clocks_ordered", null)
        val list    = ordered?.split(",")?.filter { it.isNotEmpty() }
            ?: prefs.getStringSet("added_clocks", setOf("0"))?.toList()
            ?: listOf("0")
        mutableStateOf(list)
    }

    val allStyles    = clockStyles.mapIndexed { i, n -> i to n }
    val addedList    = addedClocksOrder.mapNotNull { id -> allStyles.find { it.first.toString() == id } }
    val availableList = allStyles.filter { (i, _) -> !addedClocksOrder.contains(i.toString()) }

    val onToggleAdded: (Int) -> Unit = { index ->
        val newList = addedClocksOrder.toMutableList()
        val id = index.toString()
        if (newList.contains(id)) { if (newList.size > 1) newList.remove(id) }
        else newList.add(id)
        addedClocksOrder = newList
        prefs.edit()
            .putStringSet("added_clocks", newList.toSet())
            .putString("added_clocks_ordered", newList.joinToString(","))
            .apply()
    }

    val onMove: (Int, Int) -> Unit = { from, to ->
        val newList = addedClocksOrder.toMutableList()
        if (from in newList.indices && to in newList.indices) {
            val item = newList.removeAt(from); newList.add(to, item)
            addedClocksOrder = newList
            prefs.edit()
                .putStringSet("added_clocks", newList.toSet())
                .putString("added_clocks_ordered", newList.joinToString(","))
                .apply()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionLabel("CLOCK STYLE") }

        if (addedList.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DragHandle, null, tint = MuhexTextDim, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ACTIVE — DRAG TO REORDER", color = MuhexTextDim, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
            }
            item {
                ReorderableClockGrid(
                    items        = addedList,
                    selectedIndex = selectedIndex,
                    onSelect     = { selectedIndex = it; prefs.edit().putInt("clock_index", it).apply() },
                    onToggleAdded = onToggleAdded,
                    onMove       = onMove
                )
            }
        }

        if (availableList.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("AVAILABLE", color = MuhexTextDim, fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
            items(availableList.chunked(2)) { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { (index, name) ->
                        Box(modifier = Modifier.weight(1f)) {
                            ClockGridItem(index, name, selectedIndex == index, false,
                                onSelect = { selectedIndex = index; prefs.edit().putInt("clock_index", index).apply() },
                                onToggleAdded = { onToggleAdded(index) }
                            )
                        }
                    }
                    if (rowItems.size < 2) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            MuhexDivider()
            Spacer(Modifier.height(4.dp))
            Text(
                "Active: ${clockStyles.getOrNull(selectedIndex) ?: "—"}",
                color = MuhexText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(10.dp))
            MuhexActionButton(
                icon  = Icons.Default.Tune,
                label = "Customize Clock — Colors, Fonts & Scale",
                onClick = onOpenClockSettings
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── DRAWER TAB ────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun DrawerTab(prefs: SharedPreferences, onOpenFontPicker: (String, String) -> Unit) {
    var expandedItem by remember { mutableStateOf<String?>(null) }

    val animOptions = mapOf(
        "fade"       to "Fade",
        "slide_up"   to "Slide Up",
        "slide_down" to "Slide Down",
        "scale"      to "Scale",
        "circle"     to "Circle Reveal",
        "none"       to "None"
    )
    val displayModeOptions = mapOf("both" to "Icon & Label", "icon" to "Icon Only", "label" to "Label Only")

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionLabel("APP DRAWER") }

        item {
            MuhexExpandCard(
                icon  = Icons.Default.GridView,
                title = "Layout",
                value = "${prefs.getInt("drawer_columns", 4)} columns",
                expanded = expandedItem == "layout",
                onClick  = { expandedItem = if (expandedItem == "layout") null else "layout" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Grid Columns", color = MuhexTextMid, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(2, 3, 4, 5).forEach { col ->
                            val sel = prefs.getInt("drawer_columns", 4) == col
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) MuhexGreenGlow else MuhexSurface)
                                    .border(1.dp, if (sel) MuhexGreen else MuhexBorder, RoundedCornerShape(10.dp))
                                    .clickable { prefs.edit().putInt("drawer_columns", col).apply() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("$col", color = if (sel) MuhexGreen else MuhexTextMid, fontWeight = FontWeight.Bold) }
                        }
                    }
                    MuhexSlider("Background Opacity", prefs.getInt("drawer_opacity", 85).toFloat(), 0f..100f) {
                        prefs.edit().putInt("drawer_opacity", it.toInt()).apply()
                    }
                    MuhexSlider("Item Transparency", prefs.getInt("drawer_item_opacity", 100).toFloat(), 10f..100f) {
                        prefs.edit().putInt("drawer_item_opacity", it.toInt()).apply()
                    }
                }
            }
        }

        item {
            MuhexExpandCard(
                icon  = Icons.Default.TextFields,
                title = "Icon & Label",
                value = "Sizes and display",
                expanded = expandedItem == "label",
                onClick  = { expandedItem = if (expandedItem == "label") null else "label" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MuhexDropdown("Display Mode", prefs.getString("drawer_display_mode", "both") ?: "both", displayModeOptions) {
                        prefs.edit().putString("drawer_display_mode", it).apply()
                    }
                    MuhexSlider("Icon Size", prefs.getFloat("drawer_icon_size", 48f), 24f..72f) {
                        prefs.edit().putFloat("drawer_icon_size", it).apply()
                    }
                    MuhexSlider("Label Size", prefs.getFloat("drawer_label_size", 12f), 8f..24f) {
                        prefs.edit().putFloat("drawer_label_size", it).apply()
                    }
                    MuhexActionButton(Icons.Default.FontDownload, "Select Drawer Font") {
                        onOpenFontPicker("drawer_font_family", "Drawer Font")
                    }
                }
            }
        }

        item {
            MuhexExpandCard(
                icon  = Icons.Default.AutoMode,
                title = "Animations",
                value = "Open & close effects",
                expanded = expandedItem == "anim",
                onClick  = { expandedItem = if (expandedItem == "anim") null else "anim" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MuhexDropdown("Open Animation", prefs.getString("drawer_open_anim", "fade") ?: "fade", animOptions) {
                        prefs.edit().putString("drawer_open_anim", it).apply()
                    }
                    MuhexDropdown("Close Animation", prefs.getString("drawer_close_anim", "fade") ?: "fade", animOptions) {
                        prefs.edit().putString("drawer_close_anim", it).apply()
                    }
                    MuhexSlider("Animation Speed", prefs.getInt("drawer_anim_speed", 300).toFloat(), 100f..800f) {
                        prefs.edit().putInt("drawer_anim_speed", it.toInt()).apply()
                    }
                }
            }
        }

        item {
            MuhexExpandCard(
                icon  = Icons.Default.SortByAlpha,
                title = "Scroller",
                value = "Muhex alphabetic scroller",
                expanded = expandedItem == "scroller",
                onClick  = { expandedItem = if (expandedItem == "scroller") null else "scroller" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MuhexSlider("Edge Offset",   prefs.getFloat("scroller_padding",      0.12f), 0.05f..0.4f)   { prefs.edit().putFloat("scroller_padding", it).apply() }
                    MuhexSlider("Bending",        prefs.getFloat("scroller_bending",      300f),  0f..800f)      { prefs.edit().putFloat("scroller_bending", it).apply() }
                    MuhexSlider("Curve Spread",   prefs.getFloat("scroller_spread",       7.5f),  1f..25f)       { prefs.edit().putFloat("scroller_spread", it).apply() }
                    MuhexSlider("Font Size",      prefs.getFloat("scroller_text_size",    28f),   8f..64f)       { prefs.edit().putFloat("scroller_text_size", it).apply() }
                    MuhexSlider("Active Scale",   prefs.getFloat("scroller_scale",        2.4f),  1f..5f)        { prefs.edit().putFloat("scroller_scale", it).apply() }
                    MuhexSlider("Line Opacity",   prefs.getInt("scroller_line_alpha",     90).toFloat(), 0f..255f) { prefs.edit().putInt("scroller_line_alpha", it.toInt()).apply() }
                    MuhexSlider("Idle Opacity",   prefs.getInt("scroller_base_alpha",    130).toFloat(), 0f..255f) { prefs.edit().putInt("scroller_base_alpha", it.toInt()).apply() }
                    MuhexSlider("Anim Speed",     prefs.getInt("scroller_anim_duration", 250).toFloat(), 50f..1000f) { prefs.edit().putInt("scroller_anim_duration", it.toInt()).apply() }
                    MuhexSlider("Sensitivity",    prefs.getFloat("scroller_touch_slop",   80f),  20f..300f)     { prefs.edit().putFloat("scroller_touch_slop", it).apply() }
                    MuhexInlineToggle("Haptic Feedback", prefs.getBoolean("scroller_haptic", true)) { prefs.edit().putBoolean("scroller_haptic", it).apply() }
                }
            }
        }

        item {
            MuhexExpandCard(
                icon  = Icons.Default.Palette,
                title = "Theming",
                value = "Drawer colors",
                expanded = expandedItem == "theme",
                onClick  = { expandedItem = if (expandedItem == "theme") null else "theme" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MuhexColorPicker("Border Color", prefs.getInt("drawer_border_color", AndroidColor.WHITE)) {
                        prefs.edit().putInt("drawer_border_color", it.toArgb()).apply()
                    }
                    MuhexColorPicker("Item Border", prefs.getInt("drawer_item_border_color", AndroidColor.WHITE)) {
                        prefs.edit().putInt("drawer_item_border_color", it.toArgb()).apply()
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── DOCK TAB ──────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun DockTab(repository: AppRepository, prefs: SharedPreferences, onOpenFontPicker: (String, String) -> Unit) {
    var expandedItem by remember { mutableStateOf<String?>(null) }
    var dockedIds    by remember { mutableStateOf(repository.getDockIds()) }
    var orientation  by remember { mutableStateOf(prefs.getString("dock_orientation", "horizontal") ?: "horizontal") }
    var dockAlignment by remember { mutableStateOf(prefs.getString("dock_alignment", "left") ?: "left") }
    val allApps = remember { mutableStateOf<List<AppModel>>(emptyList()) }

    val displayModeOptions  = mapOf("both" to "Icon & Label", "icon" to "Icon Only", "label" to "Label Only")
    val orientationOptions  = mapOf("horizontal" to "Horizontal", "vertical" to "Vertical")
    val alignmentOptions    = mapOf("left" to "Left Side", "right" to "Right Side")

    LaunchedEffect(Unit) { allApps.value = repository.getAllApps() }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionLabel("DOCK BAR") }

        item {
            MuhexExpandCard(Icons.Default.Apps, "Apps", "${dockedIds.size} apps selected", expandedItem == "apps",
                onClick = { expandedItem = if (expandedItem == "apps") null else "apps" }
            ) {
                Column {
                    Text("Select up to 8 apps. 5 is recommended.", color = MuhexTextMid, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    AppSelectionContent(allApps.value, dockedIds, repository, MuhexText, MuhexSurface) { dockedIds = it }
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.Settings, "Layout", orientationOptions[orientation] ?: "Horizontal", expandedItem == "layout",
                onClick = { expandedItem = if (expandedItem == "layout") null else "layout" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MuhexDropdown("Orientation", orientation, orientationOptions) {
                        orientation = it
                        prefs.edit().putString("dock_orientation", it)
                            .putString("dock_content_alignment", if (it == "vertical") "row" else "column").apply()
                    }
                    if (orientation == "vertical") {
                        MuhexDropdown("Side Alignment", dockAlignment, alignmentOptions) {
                            dockAlignment = it; prefs.edit().putString("dock_alignment", it).apply()
                        }
                    }
                    MuhexSlider("Bottom Margin", prefs.getFloat("dock_bottom_margin", 8f), 0f..200f) { prefs.edit().putFloat("dock_bottom_margin", it).apply() }
                    if (orientation == "vertical") MuhexSlider("Side Margin", prefs.getFloat("dock_horizontal_margin", 0f), 0f..200f) { prefs.edit().putFloat("dock_horizontal_margin", it).apply() }
                    MuhexSlider("Item Spacing", prefs.getFloat("dock_spacing", 16f), 0f..100f) { prefs.edit().putFloat("dock_spacing", it).apply() }
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.TextFields, "Content", "Text and icons", expandedItem == "content",
                onClick = { expandedItem = if (expandedItem == "content") null else "content" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MuhexDropdown("Display Mode", prefs.getString("dock_display_mode", "icon") ?: "icon", displayModeOptions) { prefs.edit().putString("dock_display_mode", it).apply() }
                    MuhexSlider("Icon Size", prefs.getFloat("dock_icon_size", 48f), 24f..96f) { prefs.edit().putFloat("dock_icon_size", it).apply() }
                    MuhexSlider("Text Size", prefs.getFloat("dock_text_size", 10f), 8f..20f) { prefs.edit().putFloat("dock_text_size", it).apply() }
                    MuhexActionButton(Icons.Default.FontDownload, "Select Dock Font") { onOpenFontPicker("dock_font_family", "Dock Font") }
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.Palette, "Theming", "Colors and transparency", expandedItem == "theme",
                onClick = { expandedItem = if (expandedItem == "theme") null else "theme" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MuhexColorPicker("Background Color", prefs.getInt("dock_bg_color", AndroidColor.WHITE)) { prefs.edit().putInt("dock_bg_color", it.toArgb()).apply() }
                    MuhexSlider("Background Alpha", prefs.getFloat("dock_bg_alpha", 0.15f), 0f..1f) { prefs.edit().putFloat("dock_bg_alpha", it).apply() }
                    MuhexColorPicker("Text Color", prefs.getInt("dock_text_color", AndroidColor.WHITE)) { prefs.edit().putInt("dock_text_color", it.toArgb()).apply() }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── PINNED APPS TAB ───────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun PinnedAppsTab(repository: AppRepository, prefs: SharedPreferences, onOpenFontPicker: (String, String) -> Unit) {
    var expandedItem by remember { mutableStateOf<String?>("apps") }
    var pinnedIds    by remember { mutableStateOf(repository.getPinnedIds()) }
    val allApps      = remember { mutableStateOf<List<AppModel>>(emptyList()) }
    val displayModeOptions = mapOf("both" to "Icon & Label", "icon" to "Icon Only", "label" to "Label Only")

    LaunchedEffect(Unit) { allApps.value = repository.getAllApps() }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionLabel("PINNED STACK") }

        item {
            MuhexExpandCard(Icons.Default.PushPin, "Manage Apps", "${pinnedIds.size} Apps Pinned", expandedItem == "apps",
                onClick = { expandedItem = if (expandedItem == "apps") null else "apps" }
            ) {
                Column {
                    Text("Tap to pin. First app is always on top.", color = MuhexTextMid, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
                    AppSelectionContent(allApps.value, pinnedIds, repository, MuhexText, MuhexSurface, isPinnedApps = true) { pinnedIds = it }
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.Layers, "Stacking & Position", "${prefs.getInt("now_apps_visible_count", 3)} Visible", expandedItem == "stack",
                onClick = { expandedItem = if (expandedItem == "stack") null else "stack" }
            ) {
                var visCount by remember { mutableIntStateOf(prefs.getInt("now_apps_visible_count", 3)) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Visible Cards", color = MuhexTextMid, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4, 5).forEach { n ->
                            val sel = visCount == n
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) MuhexGreenGlow else MuhexSurface)
                                    .border(1.dp, if (sel) MuhexGreen else MuhexBorder, RoundedCornerShape(10.dp))
                                    .clickable { visCount = n; prefs.edit().putInt("now_apps_visible_count", n).apply() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("$n", color = if (sel) MuhexGreen else MuhexTextMid, fontWeight = FontWeight.Bold) }
                        }
                    }
                    MuhexSlider("Card Offset", prefs.getFloat("now_apps_offset", 18f), 0f..60f) { prefs.edit().putFloat("now_apps_offset", it).apply() }
                    MuhexSlider("Card Scale", prefs.getFloat("now_apps_scale", 0.93f), 0.7f..1f) { prefs.edit().putFloat("now_apps_scale", it).apply() }
                    MuhexSlider("Corner Radius", prefs.getFloat("now_apps_radius", 24f), 0f..48f) { prefs.edit().putFloat("now_apps_radius", it).apply() }
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.TextFields, "Label & Font", "Text settings", expandedItem == "label",
                onClick = { expandedItem = if (expandedItem == "label") null else "label" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MuhexDropdown("Display Mode", prefs.getString("now_apps_display_mode", "both") ?: "both", displayModeOptions) { prefs.edit().putString("now_apps_display_mode", it).apply() }
                    MuhexSlider("Label Size", prefs.getFloat("now_apps_label_size", 12f), 8f..20f) { prefs.edit().putFloat("now_apps_label_size", it).apply() }
                    MuhexActionButton(Icons.Default.FontDownload, "Select Pinned Font") { onOpenFontPicker("now_apps_font_family", "Pinned Font") }
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.Palette, "Theming", "Card colors", expandedItem == "theme",
                onClick = { expandedItem = if (expandedItem == "theme") null else "theme" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MuhexColorPicker("Card Background", prefs.getInt("now_apps_card_color", AndroidColor.WHITE)) { prefs.edit().putInt("now_apps_card_color", it.toArgb()).apply() }
                    MuhexSlider("Card Alpha", prefs.getFloat("now_apps_card_alpha", 0.12f), 0f..1f) { prefs.edit().putFloat("now_apps_card_alpha", it).apply() }
                    MuhexColorPicker("Label Color", prefs.getInt("now_apps_label_color", AndroidColor.WHITE)) { prefs.edit().putInt("now_apps_label_color", it.toArgb()).apply() }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── GESTURES TAB ──────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun GesturesTab(repository: AppRepository, prefs: SharedPreferences) {
    val gestureOptions = mapOf(
        "none"          to "— None —",
        "open_drawer"   to "Open App Drawer",
        "go_home"       to "Go Home",
        "open_recents"  to "Open Recents",
        "open_settings" to "Open Settings",
        "lock_screen"   to "Lock Screen",
        "expand_notif"  to "Expand Notifications",
        "search"        to "Open Search"
    )

    val gestures = listOf(
        Triple(Icons.Default.SwipeUp,    "Swipe Up",          "gesture_swipe_up"),
        Triple(Icons.Default.SwipeDown,  "Swipe Down",        "gesture_swipe_down"),
        Triple(Icons.Default.SwipeLeft,  "Swipe Left",        "gesture_swipe_left"),
        Triple(Icons.Default.SwipeRight, "Swipe Right",       "gesture_swipe_right"),
        Triple(Icons.Default.TouchApp,   "Double Tap",        "gesture_double_tap"),
        Triple(Icons.Default.PanTool,    "Long Press",        "gesture_long_press"),
        Triple(Icons.Default.Pinch,      "Pinch In",          "gesture_pinch_in"),
        Triple(Icons.Default.ZoomOut,    "Pinch Out",         "gesture_pinch_out")
    )

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionLabel("GESTURE MAPPING")
            Spacer(Modifier.height(4.dp))
            Text("Assign actions to each gesture on the home screen.", color = MuhexTextMid, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
        }

        items(gestures) { (icon, label, prefKey) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MuhexSurface3)
                    .border(1.dp, MuhexBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MuhexGreenGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = MuhexGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = MuhexText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    val current = prefs.getString(prefKey, "none") ?: "none"
                    Text(gestureOptions[current] ?: "—", color = MuhexGreen, fontSize = 11.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = MuhexTextDim, modifier = Modifier.size(18.dp))
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            MuhexDivider()
            Spacer(Modifier.height(12.dp))
            SectionLabel("SENSITIVITY")
            Spacer(Modifier.height(8.dp))
            MuhexSlider("Swipe Threshold", prefs.getFloat("gesture_swipe_threshold", 100f), 40f..300f) {
                prefs.edit().putFloat("gesture_swipe_threshold", it).apply()
            }
            Spacer(Modifier.height(8.dp))
            MuhexSlider("Long Press Duration (ms)", prefs.getInt("gesture_long_press_ms", 500).toFloat(), 200f..1500f) {
                prefs.edit().putInt("gesture_long_press_ms", it.toInt()).apply()
            }
            Spacer(Modifier.height(8.dp))
            MuhexInlineToggle("Haptic on Gesture", prefs.getBoolean("gesture_haptic", true)) {
                prefs.edit().putBoolean("gesture_haptic", it).apply()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── QUOTES TAB ────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun QuotesTab(prefs: SharedPreferences) {
    var expandedItem by remember { mutableStateOf<String?>(null) }
    val positionOptions  = mapOf("top" to "Top", "center" to "Center", "bottom" to "Bottom")
    val intervalOptions  = mapOf("always" to "Always Show", "daily" to "Daily", "hourly" to "Hourly", "session" to "Per Session")
    val categoryOptions  = mapOf("all" to "All", "motivation" to "Motivation", "philosophy" to "Philosophy", "stoic" to "Stoicism", "custom" to "Custom Only")

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionLabel("INSPIRATION") }

        item {
            // Quote preview card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MuhexSurface3)
                    .border(1.dp, MuhexGreen.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Icon(Icons.Default.FormatQuote, null, tint = MuhexGreen.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The impediment to action advances action. What stands in the way becomes the way.",
                        color = MuhexText,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("— Marcus Aurelius", color = MuhexGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.Category, "Category", prefs.getString("quote_category", "all") ?: "all", expandedItem == "cat",
                onClick = { expandedItem = if (expandedItem == "cat") null else "cat" }
            ) {
                MuhexDropdown("Quote Category", prefs.getString("quote_category", "all") ?: "all", categoryOptions) {
                    prefs.edit().putString("quote_category", it).apply()
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.Schedule, "Interval", prefs.getString("quote_interval", "always") ?: "always", expandedItem == "interval",
                onClick = { expandedItem = if (expandedItem == "interval") null else "interval" }
            ) {
                MuhexDropdown("Show Quote", prefs.getString("quote_interval", "always") ?: "always", intervalOptions) {
                    prefs.edit().putString("quote_interval", it).apply()
                }
            }
        }

        item {
            MuhexExpandCard(Icons.Default.Tune, "Appearance", "Font and position", expandedItem == "appear",
                onClick = { expandedItem = if (expandedItem == "appear") null else "appear" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MuhexDropdown("Position", prefs.getString("quote_position", "bottom") ?: "bottom", positionOptions) { prefs.edit().putString("quote_position", it).apply() }
                    MuhexSlider("Font Size", prefs.getFloat("quote_font_size", 14f), 8f..24f) { prefs.edit().putFloat("quote_font_size", it).apply() }
                    MuhexSlider("Opacity", prefs.getFloat("quote_opacity", 0.9f), 0.1f..1f) { prefs.edit().putFloat("quote_opacity", it).apply() }
                    MuhexColorPicker("Text Color", prefs.getInt("quote_text_color", AndroidColor.WHITE)) { prefs.edit().putInt("quote_text_color", it.toArgb()).apply() }
                    MuhexInlineToggle("Show Author", prefs.getBoolean("quote_show_author", true)) { prefs.edit().putBoolean("quote_show_author", it).apply() }
                    MuhexInlineToggle("Italic Style", prefs.getBoolean("quote_italic", true)) { prefs.edit().putBoolean("quote_italic", it).apply() }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── ABOUT TAB ─────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AboutTab() {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF0A1F0A), MuhexSurface3, Color(0xFF0A1F0A)))
                    )
                    .border(1.dp, MuhexGreen.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MuhexGreenGlow, CircleShape)
                            .border(2.dp, MuhexGreen.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("M", color = MuhexGreen, fontWeight = FontWeight.Black, fontSize = 36.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("MUHEX", color = MuhexText, fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 6.sp)
                    Text("Premium Android Launcher", color = MuhexTextMid, fontSize = 13.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MuhexGreenGlow)
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text("v1.0.0", color = MuhexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { SectionLabel("BUILD INFO") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Version",      "1.0.0")
                InfoRow("Build",        "Release")
                InfoRow("Min SDK",      "Android 8.0 (API 26)")
                InfoRow("Target SDK",   "Android 14 (API 34)")
                InfoRow("UI Engine",    "Jetpack Compose")
                InfoRow("Architecture","MVVM + Repository")
                InfoRow("Developer",   "muhex-dev")
            }
        }

        item { SectionLabel("OPEN SOURCE") }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MuhexSurface3)
                    .border(1.dp, MuhexBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Jetpack Compose", "Coil", "Kotlin Coroutines", "Material 3").forEach { lib ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(MuhexGreen, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text(lib, color = MuhexTextMid, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── CLOCK GRID COMPONENTS ─────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ClockPreview(index: Int, modifier: Modifier = Modifier) {
    val imageRes = when (index) {
        0 -> R.drawable.rotating; 1 -> R.drawable.classic; 2 -> R.drawable.stack
        3 -> R.drawable.info; 4 -> R.drawable.greeting; 5 -> R.drawable.numeric
        6 -> R.drawable.modern; else -> null
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MuhexSurface)
    ) {
        if (imageRes != null) {
            Image(
                painter = rememberAsyncImagePainter(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Schedule, null, tint = MuhexTextDim, modifier = Modifier.size(32.dp).align(Alignment.Center))
        }
    }
}

@Composable
private fun ReorderableClockGrid(
    items: List<Pair<Int, String>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onToggleAdded: (Int) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val haptic  = LocalHapticFeedback.current
    val density = LocalDensity.current
    val spacing = 12.dp
    val columns = 2

    var draggingIndex  by remember { mutableStateOf<Int?>(null) }
    var dragOffset     by remember { mutableStateOf(Offset.Zero) }
    var listForDisplay by remember(items) { mutableStateOf(items) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gridWidth    = maxWidth
        val itemSize     = (gridWidth - spacing) / columns
        val itemHeight   = itemSize / 1.35f
        val itemWidthPx  = with(density) { itemSize.toPx() }
        val itemHeightPx = with(density) { itemHeight.toPx() }
        val spacingPx    = with(density) { spacing.toPx() }

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            listForDisplay.chunked(columns).forEachIndexed { rowIndex, rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    rowItems.forEachIndexed { colIndex, (index, name) ->
                        val globalIndex = rowIndex * columns + colIndex
                        val isDragging  = draggingIndex == globalIndex
                        val scale by animateFloatAsState(if (isDragging) 1.12f else 1f, spring(stiffness = Spring.StiffnessLow))
                        Box(
                            modifier = Modifier
                                .width(itemSize)
                                .zIndex(if (isDragging) 10f else 0f)
                                .graphicsLayer {
                                    scaleX = scale; scaleY = scale
                                    if (isDragging) { translationX = dragOffset.x; translationY = dragOffset.y; shadowElevation = 24.dp.toPx() }
                                }
                                .pointerInput(globalIndex, items) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingIndex = globalIndex; dragOffset = Offset.Zero; haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                        onDragEnd   = { draggingIndex = null; dragOffset = Offset.Zero },
                                        onDragCancel = { draggingIndex = null; dragOffset = Offset.Zero },
                                        onDrag = { change, dragAmount ->
                                            change.consume(); dragOffset += dragAmount
                                            val movedCols  = (dragOffset.x / (itemWidthPx + spacingPx)).roundToInt()
                                            val movedRows  = (dragOffset.y / (itemHeightPx + spacingPx)).roundToInt()
                                            val targetIndex = (globalIndex + movedRows * columns + movedCols).coerceIn(0, listForDisplay.size - 1)
                                            if (targetIndex != draggingIndex && draggingIndex != null) {
                                                val newList = listForDisplay.toMutableList()
                                                val item = newList.removeAt(draggingIndex!!)
                                                newList.add(targetIndex, item)
                                                listForDisplay = newList
                                                val colDiff = (targetIndex % columns) - (globalIndex % columns)
                                                val rowDiff = (targetIndex / columns) - (globalIndex / columns)
                                                dragOffset = Offset(dragOffset.x - colDiff * (itemWidthPx + spacingPx), dragOffset.y - rowDiff * (itemHeightPx + spacingPx))
                                                draggingIndex = targetIndex
                                                onMove(globalIndex, targetIndex)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    )
                                }
                        ) {
                            ClockGridItem(index, name, selectedIndex == index, true, onSelect = { onSelect(index) }, onToggleAdded = { onToggleAdded(index) })
                        }
                    }
                    if (rowItems.size < columns) Spacer(modifier = Modifier.width(itemSize))
                }
            }
        }
    }
}

@Composable
private fun ClockGridItem(
    index: Int, name: String, isSelected: Boolean, isAdded: Boolean,
    onSelect: () -> Unit, onToggleAdded: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(20.dp))
            .background(MuhexSurface3)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MuhexGreen else MuhexBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ClockPreview(index = index)
                if (isAdded) {
                    IconButton(
                        onClick = onToggleAdded,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) MuhexGreen.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name,
                    color = if (isSelected) MuhexGreen else MuhexTextMid,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isAdded) {
                    Icon(Icons.Default.AddCircle, null, tint = MuhexTextDim, modifier = Modifier.size(16.dp).clickable { onToggleAdded() })
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ─── SHARED PRIMITIVE COMPONENTS ───────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MuhexToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (checked) MuhexGreenGlow else MuhexSurface3)
            .border(1.dp, if (checked) MuhexGreen.copy(alpha = 0.4f) else MuhexBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(if (checked) MuhexGreen.copy(alpha = 0.2f) else MuhexSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (checked) MuhexGreen else MuhexTextDim, modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, color = MuhexText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = MuhexTextMid, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor   = MuhexGreen,
                checkedThumbColor   = Color.Black,
                uncheckedTrackColor = MuhexBorder,
                uncheckedThumbColor = MuhexTextDim
            )
        )
    }
}

@Composable
private fun MuhexExpandCard(
    icon: ImageVector,
    title: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MuhexSurface3)
            .border(1.dp, if (expanded) MuhexGreen.copy(alpha = 0.35f) else MuhexBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(if (expanded) MuhexGreenGlow else MuhexSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (expanded) MuhexGreen else MuhexTextMid, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, color = MuhexText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(value, color = if (expanded) MuhexGreen else MuhexTextMid, fontSize = 11.sp)
            }
            val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(200))
            Icon(
                Icons.Default.KeyboardArrowDown, null,
                tint = MuhexTextMid,
                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation }
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit    = shrinkVertically(tween(180)) + fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MuhexSurface2)
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MuhexBorder))
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun MuhexSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    var current by remember { mutableFloatStateOf(value) }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MuhexTextMid, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                if (range.endInclusive <= 1f) "%.2f".format(current) else "${current.toInt()}",
                color = MuhexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it; onValueChange(it) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor          = MuhexGreen,
                activeTrackColor    = MuhexGreen,
                inactiveTrackColor  = MuhexBorder
            )
        )
    }
}

@Composable
private fun MuhexInlineToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    var state by remember { mutableStateOf(checked) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MuhexTextMid, fontSize = 13.sp)
        Switch(
            checked = state,
            onCheckedChange = { state = it; onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedTrackColor   = MuhexGreen,
                checkedThumbColor   = Color.Black,
                uncheckedTrackColor = MuhexBorder,
                uncheckedThumbColor = MuhexTextDim
            )
        )
    }
}

@Composable
private fun MuhexDropdown(label: String, current: String, options: Map<String, String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = MuhexTextMid, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MuhexSurface)
                .border(1.dp, if (expanded) MuhexGreen.copy(alpha = 0.5f) else MuhexBorder, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(options[current] ?: current, color = MuhexText, fontSize = 13.sp)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MuhexTextDim, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MuhexSurface2).border(1.dp, MuhexBorder, RoundedCornerShape(12.dp))
            ) {
                options.forEach { (key, display) ->
                    DropdownMenuItem(
                        text = { Text(display, color = if (key == current) MuhexGreen else MuhexText, fontSize = 13.sp) },
                        onClick = { onSelect(key); expanded = false },
                        modifier = Modifier.background(if (key == current) MuhexGreenGlow else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun MuhexColorPicker(label: String, currentArgb: Int, onColorSelected: (Color) -> Unit) {
    var selected by remember { mutableIntStateOf(currentArgb) }
    Column {
        Text(label, color = MuhexTextMid, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(colorPalette) { color ->
                val isSelected = selected == color.toArgb()
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MuhexGreen else MuhexBorder, CircleShape)
                        .clickable { selected = color.toArgb(); onColorSelected(color) }
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, null, tint = if (color == Color.White || color == Color(0xFFF0F0F8)) Color.Black else Color.White, modifier = Modifier.size(16.dp).align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
private fun MuhexActionButton(icon: ImageVector = Icons.Default.ArrowForward, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MuhexGreenGlow)
            .border(1.dp, MuhexGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = MuhexGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = MuhexGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun MuhexDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(listOf(Color.Transparent, MuhexBorderBright, Color.Transparent))
            )
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MuhexTextDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
    )
}

// ─── Stubs for externally-defined types (already exist in your project) ────
// AppRepository  — already defined
// AppModel       — already defined
// UnifiedHomeItem— already defined
// AppSelectionContent — already defined
// SliderSettingFloat, SettingToggle, SettingItem, ColorSection, DropdownSetting — can be replaced by the Muhex* versions above
