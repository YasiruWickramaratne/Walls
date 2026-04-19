package com.example.walls.ui.fullscreen

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.OnStateChangedListener
import kotlin.math.abs

/**
 * SubsamplingScaleImageView that intercepts left/right swipes at minimum zoom
 * and delegates to navigation callbacks instead of panning.
 * Detection uses only displacement (not velocity) so slow or "pause then lift"
 * swipes are still recognised.
 */
class SwipeableScaleImageView(context: Context) : SubsamplingScaleImageView(context) {
    companion object {
        private const val TAG = "SwipeableScaleImageView"
    }

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onZoomStateChanged: ((Boolean) -> Unit)? = null

    private var startX = 0f
    private var startY = 0f
    private var isMultiTouch = false
    private var swipeHandled = false
    private var maxDx = 0f
    private var maxDy = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        setOnStateChangedListener(object : OnStateChangedListener {
            override fun onScaleChanged(newScale: Float, origin: Int) {
                val ms = minScale
                val isZoomed = isReady && !ms.isNaN() && ms > 0f && newScale > ms * 1.05f
                onZoomStateChanged?.invoke(isZoomed)
            }

            override fun onCenterChanged(newCenter: android.graphics.PointF?, origin: Int) = Unit
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                isMultiTouch = false
                swipeHandled = false
                maxDx = 0f
                maxDy = 0f
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                isMultiTouch = true
                swipeHandled = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (shouldHandleSwipe(event, source = "move")) {
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (shouldHandleSwipe(event, source = "up")) {
                    return true
                }
                swipeHandled = false
                isMultiTouch = false
            }

            MotionEvent.ACTION_CANCEL -> {
                swipeHandled = false
                isMultiTouch = false
            }
        }
        if (swipeHandled) {
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun shouldHandleSwipe(event: MotionEvent, source: String): Boolean {
        if (isMultiTouch || swipeHandled) return false

        val dx = event.rawX - startX
        val dy = event.rawY - startY
        if (abs(dx) > abs(maxDx)) {
            maxDx = dx
        }
        if (abs(dy) > abs(maxDy)) {
            maxDy = dy
        }
        val ms = minScale
        val notZoomed = !isReady || ms.isNaN() || ms <= 0f || scale <= ms * 1.1f
        val effectiveDx = maxDx
        val effectiveDy = maxDy
        val minDistancePx = maxOf(8f * resources.displayMetrics.density, touchSlop * 0.75f)
        val clearlyHorizontal = abs(effectiveDx) > minDistancePx && abs(effectiveDx) > abs(effectiveDy) * 1.05f
        val clearlyVertical = abs(effectiveDy) > minDistancePx && abs(effectiveDy) > abs(effectiveDx) * 1.15f

        if (!notZoomed || (!clearlyHorizontal && !clearlyVertical)) {
            Log.d(
                TAG,
                "ignored source=$source dx=$dx dy=$dy effectiveDx=$effectiveDx effectiveDy=$effectiveDy " +
                    "scale=$scale minScale=$minScale isReady=$isReady notZoomed=$notZoomed " +
                    "clearlyHorizontal=$clearlyHorizontal clearlyVertical=$clearlyVertical threshold=$minDistancePx"
            )
            return false
        }

        swipeHandled = true
        parent?.requestDisallowInterceptTouchEvent(true)
        Log.d(
            TAG,
            "accepted source=$source dx=$dx dy=$dy effectiveDx=$effectiveDx effectiveDy=$effectiveDy " +
                "scale=$scale minScale=$minScale direction=${
                    when {
                        clearlyVertical && effectiveDy < 0 -> "up"
                        clearlyVertical -> "down"
                        effectiveDx < 0 -> "left"
                        else -> "right"
                    }
                }"
        )
        when {
            clearlyVertical && effectiveDy < 0 -> onSwipeUp?.invoke()
            clearlyVertical -> onSwipeDown?.invoke()
            effectiveDx < 0 -> onSwipeLeft?.invoke()
            else -> onSwipeRight?.invoke()
        }
        return true
    }
}
