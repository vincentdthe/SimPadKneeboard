package com.simpad.kneeboard.ui.inking

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.simpad.kneeboard.data.models.InkPoint
import com.simpad.kneeboard.data.models.InkStroke
import com.simpad.kneeboard.data.models.InkingTool
import java.util.UUID
import kotlin.math.hypot

class InkingView(context: Context) : View(context) {

    private val strokes = mutableListOf<InkStroke>()
    private var currentStrokePoints = mutableListOf<InkPoint>()
    private var activeTool: InkingTool = InkingTool.PEN
    private var activeColorArgb: Long = 0xFF00E5FFL // Default cyan
    private var activeStrokeWidth: Float = 3.5f

    private var onStrokeFinished: ((InkStroke) -> Unit)? = null
    private var onStrokesUpdated: ((List<InkStroke>) -> Unit)? = null
    private var onFingerSwipe: ((Float) -> Unit)? = null

    // Touch gesture tracking for finger (swiping / navigation)
    private var fingerStartX: Float = 0f
    private var fingerStartY: Float = 0f
    private var isFingerMoving: Boolean = false

    // Hardware paints
    private val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val highlighterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    private val currentPath = Path()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setTool(tool: InkingTool) {
        activeTool = tool
    }

    fun setColor(colorArgb: Long) {
        activeColorArgb = colorArgb
    }

    fun setStrokeWidth(width: Float) {
        activeStrokeWidth = width
    }

    fun setStrokes(newStrokes: List<InkStroke>) {
        strokes.clear()
        strokes.addAll(newStrokes)
        invalidate()
    }

    fun setOnStrokeFinishedListener(listener: (InkStroke) -> Unit) {
        onStrokeFinished = listener
    }

    fun setOnStrokesUpdatedListener(listener: (List<InkStroke>) -> Unit) {
        onStrokesUpdated = listener
    }

    fun setOnFingerSwipeListener(listener: (Float) -> Unit) {
        onFingerSwipe = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)

        // HARDWARE PALM REJECTION & S-PEN SEPARATION:
        // If the event comes from a STYLUS or ERASER hardware pen:
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            val isEraserTool = (toolType == MotionEvent.TOOL_TYPE_ERASER) || (activeTool == InkingTool.ERASER)
            handleStylusInput(event, isEraserTool)
            return true
        }

        // If the event comes from a FINGER (Touch):
        // Fingers are used for navigation / page swipes and are completely rejected from drawing lines!
        handleFingerInput(event)
        return true
    }

    private fun handleStylusInput(event: MotionEvent, isEraser: Boolean) {
        val x = event.x
        val y = event.y
        val pressure = event.pressure.coerceIn(0.1f, 1.0f)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isEraser) {
                    eraseStrokesNear(x, y, 30f)
                } else {
                    currentStrokePoints.clear()
                    currentStrokePoints.add(InkPoint(x, y, pressure, System.currentTimeMillis()))
                    currentPath.reset()
                    currentPath.moveTo(x, y)
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val historySize = event.historySize
                if (isEraser) {
                    for (i in 0 until historySize) {
                        eraseStrokesNear(event.getHistoricalX(i), event.getHistoricalY(i), 30f)
                    }
                    eraseStrokesNear(x, y, 30f)
                } else {
                    // Process high-frequency historical samples from S-Pen (240 Hz touch sampling)
                    for (i in 0 until historySize) {
                        val hX = event.getHistoricalX(i)
                        val hY = event.getHistoricalY(i)
                        val hP = event.getHistoricalPressure(i).coerceIn(0.1f, 1.0f)
                        currentStrokePoints.add(InkPoint(hX, hY, hP, event.getHistoricalEventTime(i)))
                        currentPath.lineTo(hX, hY)
                    }
                    currentStrokePoints.add(InkPoint(x, y, pressure, System.currentTimeMillis()))
                    currentPath.lineTo(x, y)
                }
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isEraser) {
                    eraseStrokesNear(x, y, 30f)
                    onStrokesUpdated?.invoke(strokes.toList())
                } else if (currentStrokePoints.size > 1) {
                    val stroke = InkStroke(
                        id = UUID.randomUUID().toString(),
                        colorArgb = activeColorArgb,
                        strokeWidth = activeStrokeWidth,
                        isHighlighter = (activeTool == InkingTool.HIGHLIGHTER),
                        points = currentStrokePoints.toList()
                    )
                    strokes.add(stroke)
                    onStrokeFinished?.invoke(stroke)
                }
                currentStrokePoints.clear()
                currentPath.reset()
                invalidate()
            }
        }
    }

    private fun handleFingerInput(event: MotionEvent) {
        // Finger gestures for page turning & navigation
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fingerStartX = event.x
                fingerStartY = event.y
                isFingerMoving = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - fingerStartX
                val dy = event.y - fingerStartY
                if (hypot(dx, dy) > 30f) {
                    isFingerMoving = true
                }
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - fingerStartX
                val dy = event.y - fingerStartY
                // If horizontal swipe is dominant and > 120 pixels
                if (isFingerMoving && Math.abs(dx) > 120f && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    onFingerSwipe?.invoke(dx)
                }
                isFingerMoving = false
            }
        }
    }

    private fun eraseStrokesNear(x: Float, y: Float, radius: Float) {
        val radiusSquared = radius * radius
        var modified = false

        val iterator = strokes.iterator()
        while (iterator.hasNext()) {
            val stroke = iterator.next()
            for (p in stroke.points) {
                val dx = p.x - x
                val dy = p.y - y
                if ((dx * dx + dy * dy) <= radiusSquared) {
                    iterator.remove()
                    modified = true
                    break
                }
            }
        }

        if (modified) {
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw committed strokes
        for (stroke in strokes) {
            drawStroke(canvas, stroke)
        }

        // 2. Draw live in-progress stroke
        if (currentStrokePoints.isNotEmpty()) {
            val isHighlighter = (activeTool == InkingTool.HIGHLIGHTER)
            val paint = if (isHighlighter) highlighterPaint else penPaint

            val color = if (isHighlighter) {
                // Alpha blend for highlighter
                (activeColorArgb and 0x00FFFFFFL) or 0x4D000000L
            } else {
                activeColorArgb
            }

            paint.color = color.toInt()
            paint.strokeWidth = activeStrokeWidth
            canvas.drawPath(currentPath, paint)
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: InkStroke) {
        if (stroke.points.isEmpty()) return

        val paint = if (stroke.isHighlighter) highlighterPaint else penPaint
        val color = if (stroke.isHighlighter) {
            (stroke.colorArgb and 0x00FFFFFFL) or 0x4D000000L
        } else {
            stroke.colorArgb
        }
        paint.color = color.toInt()
        paint.strokeWidth = stroke.strokeWidth

        val path = Path()
        val pts = stroke.points
        path.moveTo(pts[0].x, pts[0].y)

        for (i in 1 until pts.size) {
            // Smooth Bézier curve between midpoints
            val prev = pts[i - 1]
            val curr = pts[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadTo(prev.x, prev.y, midX, midY)
        }
        if (pts.size > 1) {
            path.lineTo(pts.last().x, pts.last().y)
        } else {
            // Single tap dot
            canvas.drawCircle(pts[0].x, pts[0].y, stroke.strokeWidth / 2f, paint)
            return
        }

        canvas.drawPath(path, paint)
    }
}
