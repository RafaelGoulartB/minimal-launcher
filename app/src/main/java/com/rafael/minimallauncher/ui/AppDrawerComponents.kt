@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rafael.minimallauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafael.minimallauncher.data.AppItem
import com.rafael.minimallauncher.data.AppListControlsPosition
import com.rafael.minimallauncher.data.FolderItem
import com.rafael.minimallauncher.data.HomeItemRef
import com.rafael.minimallauncher.data.LauncherItem
import com.rafael.minimallauncher.data.LauncherApp
import com.rafael.minimallauncher.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun SearchGlyph() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val stroke = 2.4.dp.toPx()
        drawCircle(
            color = Color(0xFFBEBEBE),
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.42f, size.height * 0.4f),
            style = Stroke(stroke),
        )
        drawLine(
            color = Color(0xFFBEBEBE),
            start = Offset(size.width * 0.61f, size.height * 0.6f),
            end = Offset(size.width * 0.84f, size.height * 0.83f),
            strokeWidth = stroke,
        )
    }
}

@Composable
private fun ClearSearchButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = "Clear search" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val stroke = 2.4.dp.toPx()
            drawLine(
                color = Color(0xFFBEBEBE),
                start = Offset(size.width * 0.2f, size.height * 0.2f),
                end = Offset(size.width * 0.8f, size.height * 0.8f),
                strokeWidth = stroke,
            )
            drawLine(
                color = Color(0xFFBEBEBE),
                start = Offset(size.width * 0.8f, size.height * 0.2f),
                end = Offset(size.width * 0.2f, size.height * 0.8f),
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
private fun GearButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .semantics { contentDescription = "Settings" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension * 0.37f
            val innerRadius = size.minDimension * 0.14f
            val stroke = 2.8.dp.toPx()
            drawCircle(Color.White, outerRadius * 0.72f, center, style = Stroke(stroke))
            drawCircle(Color.White, innerRadius, center, style = Stroke(stroke))
            repeat(8) { index ->
                val angle = index * PI.toFloat() / 4f
                drawLine(
                    color = Color.White,
                    start = Offset(
                        center.x + cos(angle) * outerRadius * 0.68f,
                        center.y + sin(angle) * outerRadius * 0.68f,
                    ),
                    end = Offset(
                        center.x + cos(angle) * outerRadius,
                        center.y + sin(angle) * outerRadius,
                    ),
                    strokeWidth = stroke,
                )
            }
        }
    }
}

@Composable
internal fun AlphabetRail(
    sections: List<Pair<String, Int>>,
    activeLabel: String?,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var tipLabel by remember { mutableStateOf<String?>(null) }
    var displayedTipLabel by remember { mutableStateOf<String?>(null) }
    var tipSlot by remember { mutableIntStateOf(0) }
    var railHeightPx by remember { mutableFloatStateOf(0f) }
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current

    LaunchedEffect(tipLabel) {
        if (tipLabel != null) displayedTipLabel = tipLabel
    }

    LaunchedEffect(scrubbing, tipLabel) {
        if (!scrubbing && tipLabel != null) {
            delay(480)
            if (!scrubbing) tipLabel = null
        }
    }

    val tipVisible = tipLabel != null
    val tipAlpha by animateFloatAsState(
        targetValue = if (tipVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (tipVisible) 90 else 160),
        label = "alphabetTipAlpha",
    )
    val tipScale by animateFloatAsState(
        targetValue = if (tipVisible) 1f else 0.88f,
        animationSpec = tween(durationMillis = if (tipVisible) 90 else 160),
        label = "alphabetTipScale",
    )
    val highlightLabel = tipLabel ?: activeLabel

    fun selectSlot(slot: Int, fromScrub: Boolean) {
        if (slot !in sections.indices) return
        val (label, index) = sections[slot]
        tipLabel = label
        tipSlot = slot
        if (!fromScrub || index != lastSelectedIndex) {
            lastSelectedIndex = index
            onSelect(index)
        }
    }

    fun selectAt(y: Float, height: Float) {
        if (height <= 0f || sections.isEmpty()) return
        val slot = (y / height * sections.size).toInt().coerceIn(sections.indices)
        selectSlot(slot, fromScrub = true)
    }

    Box(modifier = modifier.width(76.dp)) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(24.dp)
                .onSizeChanged { railHeightPx = it.height.toFloat() }
                .pointerInput(sections) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            scrubbing = true
                            selectAt(it.y, size.height.toFloat())
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            selectAt(change.position.y, size.height.toFloat())
                        },
                        onDragEnd = { scrubbing = false },
                        onDragCancel = { scrubbing = false },
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            sections.forEachIndexed { slot, (label, index) ->
                val isActive = label == highlightLabel
                Text(
                    label,
                    modifier = Modifier
                        .clickable {
                            scrubbing = false
                            selectSlot(slot, fromScrub = false)
                        }
                        .padding(vertical = 1.dp),
                    color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF6E6E6E),
                    fontSize = launcherSp(if (isActive) 14.sp else 13.sp),
                    lineHeight = launcherSp(15.sp),
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        val tipText = displayedTipLabel
        if (tipText != null && tipAlpha > 0.01f) {
            val tipSize = 54.dp
            val slotHeight = if (sections.isNotEmpty() && railHeightPx > 0f) {
                railHeightPx / sections.size
            } else {
                0f
            }
            val yOffset = with(density) {
                (slotHeight * tipSlot + slotHeight / 2f - tipSize.toPx() / 2f)
                    .coerceAtLeast(0f)
                    .toDp()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 30.dp)
                    .offset(y = yOffset)
                    .size(tipSize)
                    .graphicsLayer {
                        alpha = tipAlpha
                        scaleX = tipScale
                        scaleY = tipScale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C2C2C)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tipText,
                    color = Color.White,
                    fontSize = launcherSp(28.sp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun FolderExpandGlyph(expanded: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .semantics {
                contentDescription = if (expanded) "Collapse folder" else "Expand folder"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val stroke = 2.dp.toPx()
            val color = Color(0xFF9A9A9A)
            val midY = size.height / 2f
            val tipY = if (expanded) 3.dp.toPx() else size.height - 3.dp.toPx()
            drawLine(color, Offset(2.dp.toPx(), midY), Offset(size.width / 2f, tipY), strokeWidth = stroke)
            drawLine(color, Offset(size.width / 2f, tipY), Offset(size.width - 2.dp.toPx(), midY), strokeWidth = stroke)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DrawerFolderRow(
    item: FolderItem,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val appCount = item.apps.size
    val interactionModifier = if (onClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick ?: {},
        )
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(interactionModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            item.label,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = launcherSp(21.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (appCount == 1) "1 app" else "$appCount apps",
            color = Color(0xFF8F8F8F),
            fontSize = launcherSp(14.sp),
            maxLines = 1,
        )
        Spacer(Modifier.width(4.dp))
        trailingContent?.invoke()
        FolderExpandGlyph(expanded = expanded)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DrawerAppRow(
    item: AppItem,
    isFolderChild: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionModifier = if (onClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick ?: {},
        )
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(start = if (isFolderChild) 14.dp else 0.dp)
            .then(interactionModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isFolderChild) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFF3A3A3A)),
            )
            Spacer(Modifier.width(14.dp))
        }
        Text(
            item.label,
            color = Color.White,
            fontSize = launcherSp(21.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ScrollToTopButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(52.dp)
            .background(Color.White, CircleShape)
            .semantics { contentDescription = "Scroll to top" }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(25.dp)) {
            val stroke = 2.4.dp.toPx()
            val centerX = size.width / 2f
            val top = 3.dp.toPx()
            val bottom = size.height - 3.dp.toPx()
            drawLine(Color.Black, Offset(centerX, bottom), Offset(centerX, top), strokeWidth = stroke)
            drawLine(Color.Black, Offset(centerX, top), Offset(5.dp.toPx(), 9.dp.toPx()), strokeWidth = stroke)
            drawLine(Color.Black, Offset(centerX, top), Offset(size.width - 5.dp.toPx(), 9.dp.toPx()), strokeWidth = stroke)
        }
    }
}

@Composable
internal fun AppListControls(
    searchFieldValue: TextFieldValue,
    searchFocusRequester: FocusRequester,
    onSearchChange: (TextFieldValue) -> Unit,
    onClearSearch: () -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        BasicTextField(
            value = searchFieldValue,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .focusRequester(searchFocusRequester)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF1B1B1B)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = launcherSp(20.sp),
                fontFamily = LocalLauncherAppearance.current.fontFamily,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchGlyph()
                    Spacer(Modifier.width(15.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchFieldValue.text.isEmpty()) {
                            Text("Search apps", color = Color(0xFF9A9A9A), fontSize = launcherSp(20.sp))
                        }
                        innerTextField()
                    }
                    if (searchFieldValue.text.isNotEmpty()) {
                        ClearSearchButton(onClick = onClearSearch)
                    }
                }
            },
        )
        GearButton(onClick = onOpenSettings)
    }
}
