package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DockBar(
    repository: AppRepository,
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel, View) -> Unit,
    onOrderChanged: (List<AppModel>) -> Unit = {},
    isEditMode: Boolean = false
) {
    if (apps.isEmpty()) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // Dock Configuration States
    var showDock by remember { mutableStateOf(prefs.getBoolean("show_dock_bar", true)) }
    var orientation by remember { mutableStateOf(prefs.getString("dock_orientation", "horizontal") ?: "horizontal") }
    var dockAlignment by remember { mutableStateOf(prefs.getString("dock_alignment", "left") ?: "left") }
    
    // Theming States
    var bgColor by remember { mutableIntStateOf(prefs.getInt("dock_bg_color", android.graphics.Color.WHITE)) }
    var textColor by remember { mutableIntStateOf(prefs.getInt("dock_text_color", android.graphics.Color.WHITE)) }
    var dockBgAlpha by remember { mutableFloatStateOf(prefs.getFloat("dock_bg_alpha", 0.15f)) }
    
    // Layout & Spacing States
    var dockVerticalPadding by remember { mutableFloatStateOf(prefs.getFloat("dock_vertical_padding", 12f)) }
    var dockBottomMargin by remember { mutableFloatStateOf(prefs.getFloat("dock_bottom_margin", 8f)) }
    var dockHorizontalMargin by remember { mutableFloatStateOf(prefs.getFloat("dock_horizontal_margin", 0f)) }
    var horizontalOffset by remember { mutableFloatStateOf(prefs.getFloat("dock_horizontal_offset", 0f)) }
    var spacing by remember { mutableFloatStateOf(prefs.getFloat("dock_spacing", 16f)) }
    var cornerRadius by remember { mutableFloatStateOf(prefs.getFloat("dock_corner_radius", 32f)) }
    
    // Content Appearance States
    var iconSize by remember { mutableFloatStateOf(prefs.getFloat("dock_icon_size", 48f)) }
    var displayMode by remember { mutableStateOf(prefs.getString("dock_display_mode", "icon") ?: "icon") }
    var dockTextSize by remember { mutableFloatStateOf(prefs.getFloat("dock_text_size", 10f)) }
    var fontFamilyName by remember { mutableStateOf(prefs.getString("dock_font_family", "sans-serif-condensed") ?: "sans-serif-condensed") }

    // Reorder State
    var currentApps by remember(apps) { mutableStateOf(apps) }
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "show_dock_bar" -> showDock = p.getBoolean(key, true)
                "dock_orientation" -> orientation = p.getString(key, "horizontal") ?: "horizontal"
                "dock_alignment" -> dockAlignment = p.getString(key, "left") ?: "left"
                "dock_bg_color" -> bgColor = p.getInt(key, android.graphics.Color.WHITE)
                "dock_text_color" -> textColor = p.getInt(key, android.graphics.Color.WHITE)
                "dock_bg_alpha" -> dockBgAlpha = p.getFloat(key, 0.15f)
                "dock_vertical_padding" -> dockVerticalPadding = p.getFloat(key, 12f)
                "dock_bottom_margin" -> dockBottomMargin = p.getFloat(key, 8f)
                "dock_horizontal_margin" -> dockHorizontalMargin = p.getFloat(key, 0f)
                "dock_horizontal_offset" -> horizontalOffset = p.getFloat(key, 0f)
                "dock_icon_size" -> iconSize = p.getFloat(key, 48f)
                "dock_spacing" -> spacing = p.getFloat(key, 16f)
                "dock_corner_radius" -> cornerRadius = p.getFloat(key, 32f)
                "dock_display_mode" -> displayMode = p.getString(key, "icon") ?: "icon"
                "dock_text_size" -> dockTextSize = p.getFloat(key, 10f)
                "dock_font_family" -> fontFamilyName = p.getString(key, "sans-serif-condensed") ?: "sans-serif-condensed"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    if (!showDock) return

    val isHorizontal = orientation == "horizontal"
    val dockShape = RoundedCornerShape(cornerRadius.dp)
    val dockColor = Color(bgColor).copy(alpha = dockBgAlpha)
    val dockFontFamily = FontFamily(FontManager.resolveTypeface(fontFamilyName, android.graphics.Typeface.NORMAL))
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(
                bottom = dockBottomMargin.dp,
                start = if (!isHorizontal && dockAlignment == "left") dockHorizontalMargin.dp else 0.dp,
                end = if (!isHorizontal && dockAlignment == "right") dockHorizontalMargin.dp else 0.dp
            ),
        contentAlignment = if (isHorizontal) {
            Alignment.BottomCenter
        } else {
            if (dockAlignment == "right") Alignment.BottomEnd else Alignment.BottomStart
        }
    ) {
        val contentModifier = Modifier
            .wrapContentSize()
            .offset(
                x = if (isHorizontal) horizontalOffset.dp else 0.dp,
                y = if (!isHorizontal) horizontalOffset.dp else 0.dp
            )
            .clip(dockShape)
            .background(dockColor)

        if (isHorizontal) {
            Row(
                modifier = contentModifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = (spacing / 2).dp, vertical = dockVerticalPadding.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentApps.forEachIndexed { index, app ->
                    val itemWidth = with(density) { (iconSize.dp + spacing.dp).toPx() }
                    
                    val animatedOffset by animateFloatAsState(
                        targetValue = when {
                            draggingIndex == -1 -> 0f
                            index == draggingIndex -> dragOffset
                            else -> {
                                val targetShift = (dragOffset / itemWidth).roundToInt()
                                when {
                                    targetShift > 0 && index > draggingIndex && index <= draggingIndex + targetShift -> -itemWidth
                                    targetShift < 0 && index < draggingIndex && index >= draggingIndex + targetShift -> itemWidth
                                    else -> 0f
                                }
                            }
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "reorder"
                    )

                    DockItem(
                        app = app,
                        iconSize = iconSize,
                        textColor = Color(textColor),
                        textSize = dockTextSize,
                        fontFamily = dockFontFamily,
                        displayMode = displayMode,
                        isHorizontal = true,
                        modifier = Modifier
                            .zIndex(if (index == draggingIndex) 1f else 0f)
                            .graphicsLayer { translationX = animatedOffset }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggingIndex = index },
                                    onDragEnd = {
                                        val targetIndex = (draggingIndex + (dragOffset / itemWidth).roundToInt())
                                            .coerceIn(0, currentApps.size - 1)
                                        if (targetIndex != draggingIndex) {
                                            val newList = currentApps.toMutableList()
                                            val item = newList.removeAt(draggingIndex)
                                            newList.add(targetIndex, item)
                                            currentApps = newList
                                            onOrderChanged(newList)
                                            repository.updateDockOrderByIds(newList.map { repository.getAppUniqueId(it) })
                                        }
                                        draggingIndex = -1
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.x
                                    }
                                )
                            },
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick
                    )
                }
            }
        } else {
            Column(
                modifier = contentModifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = (spacing / 2).dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.dp),
                horizontalAlignment = if (dockAlignment == "right") Alignment.End else Alignment.Start
            ) {
                currentApps.forEachIndexed { index, app ->
                    val itemHeight = with(density) { (iconSize.dp + spacing.dp).toPx() }
                    
                    val animatedOffset by animateFloatAsState(
                        targetValue = when {
                            draggingIndex == -1 -> 0f
                            index == draggingIndex -> dragOffset
                            else -> {
                                val targetShift = (dragOffset / itemHeight).roundToInt()
                                when {
                                    targetShift > 0 && index > draggingIndex && index <= draggingIndex + targetShift -> -itemHeight
                                    targetShift < 0 && index < draggingIndex && index >= draggingIndex + targetShift -> itemHeight
                                    else -> 0f
                                }
                            }
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "reorder_vertical"
                    )

                    DockItem(
                        app = app,
                        iconSize = iconSize,
                        textColor = Color(textColor),
                        textSize = dockTextSize,
                        fontFamily = dockFontFamily,
                        displayMode = displayMode,
                        isHorizontal = false,
                        dockAlignment = dockAlignment,
                        modifier = Modifier
                            .zIndex(if (index == draggingIndex) 1f else 0f)
                            .graphicsLayer { translationY = animatedOffset }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggingIndex = index },
                                    onDragEnd = {
                                        val targetIndex = (draggingIndex + (dragOffset / itemHeight).roundToInt())
                                            .coerceIn(0, currentApps.size - 1)
                                        if (targetIndex != draggingIndex) {
                                            val newList = currentApps.toMutableList()
                                            val item = newList.removeAt(draggingIndex)
                                            newList.add(targetIndex, item)
                                            currentApps = newList
                                            onOrderChanged(newList)
                                            repository.updateDockOrderByIds(newList.map { repository.getAppUniqueId(it) })
                                        }
                                        draggingIndex = -1
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                    }
                                )
                            },
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DockItem(
    modifier: Modifier = Modifier,
    app: AppModel,
    iconSize: Float,
    textColor: Color,
    textSize: Float,
    fontFamily: FontFamily,
    displayMode: String,
    isHorizontal: Boolean,
    dockAlignment: String = "left",
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel, View) -> Unit
) {
    val view = LocalView.current
    val clickableModifier = modifier
        .wrapContentSize()
        .combinedClickable(
            onClick = { onAppClick(app) },
            onLongClick = { onAppLongClick(app, view) }
        )

    if (isHorizontal) {
        Column(
            modifier = clickableModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DockItemContent(app, iconSize, textColor, textSize, fontFamily, displayMode, isHorizontal = true, dockAlignment)
        }
    } else {
        Row(
            modifier = clickableModifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (dockAlignment == "right") Arrangement.End else Arrangement.Start
        ) {
            DockItemContent(app, iconSize, textColor, textSize, fontFamily, displayMode, isHorizontal = false, dockAlignment)
        }
    }
}

@Composable
private fun DockItemContent(
    app: AppModel,
    iconSize: Float,
    textColor: Color,
    textSize: Float,
    fontFamily: FontFamily,
    displayMode: String,
    isHorizontal: Boolean,
    dockAlignment: String
) {
    val showIcon = displayMode != "label"
    val showLabel = displayMode != "icon"
    val isRightAligned = !isHorizontal && dockAlignment == "right"

    if (isRightAligned) {
        // Label on left, Icon on right
        if (showLabel) {
            Text(
                text = app.label,
                color = textColor,
                fontSize = textSize.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(end = if (showIcon) 12.dp else 0.dp)
            )
        }
        if (showIcon) {
            Image(
                painter = rememberAsyncImagePainter(app.icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize.dp)
            )
        }
    } else {
        // Standard: Icon on left/top, Label on right/bottom
        if (showIcon) {
            Image(
                painter = rememberAsyncImagePainter(app.icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize.dp)
            )
        }
        if (showLabel && showIcon) {
            Spacer(modifier = if (isHorizontal) Modifier.height(4.dp) else Modifier.width(12.dp))
        }
        if (showLabel) {
            Text(
                text = app.label,
                color = textColor,
                fontSize = textSize.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isHorizontal) TextAlign.Center else TextAlign.Start,
                modifier = if (isHorizontal) Modifier.widthIn(max = (iconSize * 1.5f).dp) else Modifier
            )
        }
    }
}
