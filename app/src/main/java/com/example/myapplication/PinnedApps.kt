@file:OptIn(ExperimentalFoundationApi::class)

package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

private object NowAppsConfig {
    const val SCALE_STEP = 0.0f            
    val BORDER_COLOR = Color(0x4DFFFFFF) // Subtle border
    val BORDER_WIDTH = 0.8.dp
    
    // Physical spring for natural settling - Adjusted for a slower, more graceful glide
    val SETTLE_SPRING = spring<Float>(
        dampingRatio = 0.9f,  // Higher damping for a smoother, non-bouncy stop
        stiffness = 80f       // Lower stiffness for a slower movement
    )
}

@Composable
fun PinnedApps(
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel, android.view.View) -> Unit,
    onEmptyLongClick: () -> Unit = {},
    isEditMode: Boolean = false
) {
    val context = LocalContext.current
    val repository = remember { AppRepository.getInstance(context) }
    val prefs = remember { context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE) }
    val view = LocalView.current
    
    var cardBgColorInt by remember { mutableIntStateOf(prefs.getInt("now_apps_bg_color", android.graphics.Color.WHITE)) }
    var strokeColorInt by remember { mutableIntStateOf(prefs.getInt("now_apps_stroke_color", android.graphics.Color.WHITE)) }
    var textColorInt by remember { mutableIntStateOf(prefs.getInt("now_apps_text_color", android.graphics.Color.BLACK)) }
    var cardWidth by remember { mutableFloatStateOf(prefs.getFloat("now_apps_card_width", 220f)) }
    var cardHeight by remember { mutableFloatStateOf(prefs.getFloat("now_apps_card_height", 64f)) }
    var verticalOffset by remember { mutableFloatStateOf(prefs.getFloat("now_apps_vertical_offset", 0f)) }
    var horizontalOffset by remember { mutableFloatStateOf(prefs.getFloat("now_apps_horizontal_offset", 0f)) }
    var visibleCount by remember { mutableIntStateOf(prefs.getInt("now_apps_visible_count", 3)) }
    var opacity by remember { mutableFloatStateOf(prefs.getFloat("now_apps_opacity", 1.0f)) }
    var nowAppsTextSize by remember { mutableFloatStateOf(prefs.getFloat("now_apps_text_size", 15f)) }
    var displayMode by remember { mutableStateOf(prefs.getString("now_apps_display_mode", "both") ?: "both") }
    var cornerRadius by remember { mutableFloatStateOf(prefs.getFloat("now_apps_corner_radius", 28f)) }
    var iconSize by remember { mutableFloatStateOf(prefs.getFloat("now_apps_icon_size", 32f)) }
    var fontFamilyName by remember { mutableStateOf(prefs.getString("now_apps_font_family", "sans-serif-condensed") ?: "sans-serif-condensed") }

    val listener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            when (key) {
                "now_apps_bg_color" -> cardBgColorInt = p.getInt(key, android.graphics.Color.WHITE)
                "now_apps_stroke_color" -> strokeColorInt = p.getInt(key, android.graphics.Color.WHITE)
                "now_apps_text_color" -> textColorInt = p.getInt(key, android.graphics.Color.BLACK)
                "now_apps_card_width" -> cardWidth = p.getFloat(key, 220f)
                "now_apps_card_height" -> cardHeight = p.getFloat(key, 64f)
                "now_apps_vertical_offset" -> verticalOffset = p.getFloat(key, 0f)
                "now_apps_horizontal_offset" -> horizontalOffset = p.getFloat(key, 0f)
                "now_apps_visible_count" -> visibleCount = p.getInt(key, 3)
                "now_apps_opacity" -> opacity = p.getFloat(key, 1.0f)
                "now_apps_text_size" -> nowAppsTextSize = p.getFloat(key, 15f)
                "now_apps_display_mode" -> displayMode = p.getString(key, "both") ?: "both"
                "now_apps_corner_radius" -> cornerRadius = p.getFloat(key, 28f)
                "now_apps_icon_size" -> iconSize = p.getFloat(key, 32f)
                "now_apps_font_family" -> fontFamilyName = p.getString(key, "sans-serif-condensed") ?: "sans-serif-condensed"
            }
        }
    }

    DisposableEffect(prefs) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    if (apps.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (!isEditMode) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = horizontalOffset.dp.toPx()
                            translationY = (-verticalOffset).dp.toPx()
                        }
                        .padding(bottom = 60.dp)
                        .combinedClickable(
                            onLongClick = onEmptyLongClick,
                            onClick = {}
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = Color(textColorInt).copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No apps pinned.\nLong press to add apps.",
                        color = Color(textColorInt).copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    var currentList by remember(apps) { mutableStateOf(apps) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        currentList.asReversed().forEachIndexed { index, app ->
            val stackIndex = (currentList.size - 1) - index
            key(app.packageName + app.label) {
                NowAppCard(
                    app = app,
                    stackIndex = stackIndex,
                    totalCount = currentList.size,
                    visibleCount = visibleCount,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    cardBgColor = Color(cardBgColorInt).copy(alpha = opacity),
                    strokeColor = Color(strokeColorInt),
                    textColor = Color(textColorInt),
                    displayMode = displayMode,
                    cornerRadius = cornerRadius,
                    iconSize = iconSize,
                    textSize = nowAppsTextSize,
                    fontFamily = FontFamily(FontManager.resolveTypeface(fontFamilyName, android.graphics.Typeface.NORMAL)),
                    isEditMode = isEditMode,
                    // Pass individual offsets to the card
                    horizontalOffset = horizontalOffset,
                    verticalOffset = verticalOffset,
                    onRotate = {
                        val newList = currentList.toMutableList()
                        val first = newList.removeAt(0)
                        newList.add(first)
                        currentList = newList
                        repository.updatePinnedOrderByIds(newList.map { repository.getAppUniqueId(it) })
                    },
                    onClick = { onAppClick(app) },
                    onLongClick = { view -> onAppLongClick(app, view) }
                )
            }
        }
    }
}

@Composable
fun NowAppCard(
    app: AppModel,
    stackIndex: Int,
    totalCount: Int,
    visibleCount: Int,
    cardWidth: Float,
    cardHeight: Float,
    cardBgColor: Color,
    strokeColor: Color,
    textColor: Color,
    displayMode: String,
    cornerRadius: Float,
    iconSize: Float,
    textSize: Float,
    fontFamily: FontFamily,
    isEditMode: Boolean,
    horizontalOffset: Float,
    verticalOffset: Float,
    onRotate: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (android.view.View) -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    
    val swipeOffset = remember { Animatable(0f) }   
    val rotationZ = remember { Animatable(0f) }     

    val visualStackIndex = stackIndex.coerceAtMost(visibleCount - 1)
    val targetScale = 1f - (visualStackIndex * NowAppsConfig.SCALE_STEP)
    
    // PEEK LOGIC: Back cards are offset by 20% of the card height
    val peekAmountDp = cardHeight * 0.2f
    // Positive offset to move background cards DOWN (visible from the bottom)
    val targetYPx = with(density) { (visualStackIndex * peekAmountDp).dp.toPx() }

    // Add global vertical offset here (Negative because translationY negative is UP)
    val globalYOffsetPx = with(density) { (-verticalOffset).dp.toPx() }

    // Depth effect: background cards are solid but slightly dimmed for depth
    val dimFactor = if (stackIndex == 0) 1.0f else 0.95f
    val targetAlpha = if (stackIndex < visibleCount) 1.0f else 0f

    val animatedScale by animateFloatAsState(targetScale, NowAppsConfig.SETTLE_SPRING, label = "scale")
    val animatedStackY by animateFloatAsState(targetYPx, NowAppsConfig.SETTLE_SPRING, label = "stackY")
    val animatedAlpha by animateFloatAsState(targetAlpha, NowAppsConfig.SETTLE_SPRING, label = "alpha")

    val swipeThresholdPx = with(density) { -60.dp.toPx() }
    val cardShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius.dp) }
    val isTopCard = stackIndex == 0

    Box(
        modifier = Modifier
            .zIndex((100 - stackIndex).toFloat())
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                this.rotationZ = rotationZ.value
                // Combine global offsets with stack and swipe logic
                translationX = horizontalOffset.dp.toPx()
                translationY = swipeOffset.value + animatedStackY + globalYOffsetPx
                alpha = animatedAlpha
            }
            .shadow(if (isTopCard) 12.dp else 2.dp, cardShape, clip = false)
            .width(cardWidth.dp)
            .height(cardHeight.dp)
            .background(cardBgColor.copy(
                red = cardBgColor.red * dimFactor,
                green = cardBgColor.green * dimFactor,
                blue = cardBgColor.blue * dimFactor
            ), cardShape)
            .border(NowAppsConfig.BORDER_WIDTH, strokeColor.copy(alpha = 0.4f), cardShape)
            .clip(cardShape)
            .pointerInput(isTopCard, isEditMode) {
                if (!isTopCard || isEditMode) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        if (swipeOffset.value < swipeThresholdPx) {
                            scope.launch {
                                // 1. Calculate jump to back of visible stack
                                val backIndex = (totalCount - 1).coerceAtMost(visibleCount - 1)
                                val nextStackY = with(density) { (backIndex * peekAmountDp).dp.toPx() }
                                val jumpDistance = nextStackY - 0f 

                                // 2. Perform rotation
                                onRotate() 
                                
                                // 3. Instant compensation to maintain visual continuity
                                swipeOffset.snapTo(swipeOffset.value - jumpDistance)
                                
                                // 4. Smooth settling spring
                                launch { rotationZ.animateTo(0f, NowAppsConfig.SETTLE_SPRING) }
                                swipeOffset.animateTo(0f, NowAppsConfig.SETTLE_SPRING)
                            }
                        } else {
                            scope.launch {
                                launch { rotationZ.animateTo(0f, spring(stiffness = Spring.StiffnessLow)) }
                                swipeOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Dampen downward drag to feel "heavy" when cards peek from bottom
                        val dragY = if (dragAmount.y > 0) dragAmount.y * 0.35f else dragAmount.y
                        scope.launch {
                            swipeOffset.snapTo(swipeOffset.value + dragY)
                        }
                    }
                )
            }
            .combinedClickable(
                enabled = isTopCard && !isEditMode,
                onClick = onClick,
                onLongClick = { onLongClick(view) }
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (displayMode != "label") {
                Image(
                    painter = rememberAsyncImagePainter(app.icon), 
                    contentDescription = null, 
                    modifier = Modifier.size(iconSize.dp)
                )
            }
            if (displayMode == "both") Spacer(modifier = Modifier.width(16.dp))
            if (displayMode != "icon") {
                Text(
                    text = app.label,
                    color = textColor,
                    fontSize = textSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}
