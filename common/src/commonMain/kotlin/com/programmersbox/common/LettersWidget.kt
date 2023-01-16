package com.programmersbox.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun <T : Any> PatternInput(
    options: List<T>,
    modifier: Modifier = Modifier,
    optionToString: (T) -> String = { it.toString() },
    colors: PatternColors = PatternInputDefaults.defaultColors(),
    dotsSize: Float = 50f,
    dotCircleSize: Float = dotsSize * 2,
    sensitivity: Float = dotsSize,
    linesStroke: Float,
    circleStroke: Stroke = Stroke(width = linesStroke),
    animationDuration: Int = 200,
    animationDelay: Long = 100,
    onStart: (Dot<T>) -> Unit = {},
    onDotRemoved: (Dot<T>) -> Unit = {},
    onDotConnected: (Dot<T>) -> Unit = {},
    onResult: (List<Dot<T>>) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val dotsList = remember(options) { mutableListOf<Dot<T>>() }
    var previewLine by remember { mutableStateOf(Line(Offset(0f, 0f), Offset(0f, 0f))) }
    val connectedLines = remember { mutableListOf<Line>() }
    val connectedDots = remember { mutableListOf<Dot<T>>() }
    var removableDot: Dot<T>? = remember { null }

    Canvas(
        modifier
            .pointerInput(options) {
                forEachGesture {
                    awaitPointerEventScope {
                        //Pressed----------------------------------------
                        val down = awaitFirstDown(requireUnconsumed = false)
                        dotsList.find { dots ->
                            down.position.x in
                                    (dots.offset.x - sensitivity)..(dots.offset.x + sensitivity) &&
                                    down.position.y in (dots.offset.y - sensitivity)..(dots.offset.y + sensitivity)
                        }?.let { dots ->
                            connectedDots.add(dots)
                            onStart(dots)
                            scope.launch {
                                dots.size.animateTo(
                                    (dotsSize * 1.8).toFloat(),
                                    tween(animationDuration)
                                )
                                delay(animationDelay)
                                dots.size.animateTo(dotsSize, tween(animationDuration))
                            }
                            previewLine = previewLine.copy(start = Offset(dots.offset.x, dots.offset.y))
                        }

                        //Moved--------------------------------------
                        val isSuccess = drag(down.id) { change ->
                            previewLine = previewLine.copy(end = Offset(change.position.x, change.position.y))
                            //Connecting and adding
                            dotsList.find { dots ->
                                change.position.x in
                                        (dots.offset.x - sensitivity)..(dots.offset.x + sensitivity) &&
                                        change.position.y in (dots.offset.y - sensitivity)..(dots.offset.y + sensitivity)
                            }
                                ?.let { dots ->
                                    if (dots !in connectedDots) {
                                        if (previewLine.start != Offset(0f, 0f)) {
                                            connectedLines.add(
                                                Line(
                                                    start = previewLine.start,
                                                    end = dots.offset
                                                )
                                            )
                                        }
                                        connectedDots.add(dots)
                                        onDotConnected(dots)
                                        scope.launch {
                                            dots.size.animateTo(
                                                (dotsSize * 1.8).toFloat(),
                                                tween(animationDuration)
                                            )
                                            delay(animationDelay)
                                            dots.size.animateTo(dotsSize, tween(animationDuration))
                                        }
                                        previewLine = previewLine.copy(start = dots.offset)
                                    } else if (dots in connectedDots) {
                                        if (removableDot == null)
                                            removableDot = connectedDots.getOrNull(connectedDots.indexOf(dots) - 1)
                                    }
                                }

                            //Checking if the last one should be removed
                            if (removableDot != null && connectedDots.size >= 2) {
                                if (
                                    change.position.x in
                                    (removableDot!!.offset.x - sensitivity)..(removableDot!!.offset.x + sensitivity) &&
                                    change.position.y in (removableDot!!.offset.y - sensitivity..removableDot!!.offset.y + sensitivity)
                                ) {
                                    connectedLines.removeLastOrNull()
                                    connectedDots.removeLastOrNull()?.let(onDotRemoved)
                                    removableDot = null
                                    connectedDots.lastOrNull()
                                        ?.let { previewLine = previewLine.copy(start = it.offset) }
                                }
                            }
                        }

                        //Released--------------------------------------
                        if (isSuccess) {
                            previewLine = previewLine.copy(start = Offset(0f, 0f), end = Offset(0f, 0f))
                            onResult(connectedDots)
                            connectedLines.clear()
                            connectedDots.clear()
                        }
                    }
                }
            }
    ) {
        drawCircle(
            color = colors.dotsColor,
            radius = size.width / 2 - circleStroke.width,
            style = circleStroke,
            center = center
        )

        val radius = (size.width / 2) - (dotsSize * 2) - circleStroke.width

        if (dotsList.size < options.size) {
            options.forEachIndexed { index, t ->
                val angleInDegrees = ((index.toFloat() / options.size.toFloat()) * 360.0) + 50.0
                val x = -(radius * sin(toRadians(angleInDegrees))).toFloat() + (size.width / 2)
                val y = (radius * cos(toRadians(angleInDegrees))).toFloat() + (size.height / 2)

                dotsList.add(
                    Dot(
                        id = t,
                        offset = Offset(x = x, y = y),
                        size = Animatable(dotsSize),
                    )
                )
            }
        }
        if (previewLine.start != Offset(0f, 0f) && previewLine.end != Offset(0f, 0f)) {
            drawLine(
                color = colors.linesColor,
                start = previewLine.start,
                end = previewLine.end,
                strokeWidth = linesStroke,
                cap = StrokeCap.Round
            )
        }
        for (dots in dotsList) {
            drawCircle(
                color = colors.dotsColor,
                radius = dotCircleSize,
                style = Stroke(width = 2.dp.value),
                center = dots.offset
            )
            drawIntoCanvas {
                it.nativeCanvas.drawTextLine(
                    TextLine.make(optionToString(dots.id), Font(typeface = null, size = dots.size.value)),
                    dots.offset.x,
                    dots.offset.y + (dots.size.value / 3),
                    NativePaint().apply {//.asFrameworkPaint().apply {
                        color = colors.letterColor.toArgb()
                        //textAlign = Paint.Align.CENTER
                    }
                )
            }
        }
        for (line in connectedLines) {
            drawLine(
                color = colors.linesColor,
                start = line.start,
                end = line.end,
                strokeWidth = linesStroke,
                cap = StrokeCap.Round
            )
        }

    }
}

internal data class Dot<T : Any>(
    val id: T,
    val offset: Offset,
    val size: Animatable<Float, AnimationVector1D>
)

private data class Line(
    val start: Offset,
    val end: Offset
)

@Immutable
internal data class PatternColors(
    val dotsColor: Color,
    val linesColor: Color,
    val letterColor: Color,
)

internal object PatternInputDefaults {
    @Composable
    internal fun defaultColors(
        dotsColor: Color = Color.White,
        linesColor: Color = Color.White,
        letterColor: Color = Color.White,
    ) = PatternColors(
        dotsColor = dotsColor,
        linesColor = linesColor,
        letterColor = letterColor,
    )
}

private fun toRadians(degrees: Double) = (degrees * PI) / 180

private const val PI = 3.141592653589793