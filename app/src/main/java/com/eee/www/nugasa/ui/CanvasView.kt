package com.eee.www.nugasa.ui

import android.content.Context
import android.graphics.Canvas
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.eee.www.nugasa.R
import com.eee.www.nugasa.model.FingerMap
import com.eee.www.nugasa.utils.*
import kotlin.properties.Delegates

class CanvasView : View, CanvasEventHandler.Callback, MediatedView {
    private object DelayedMillis {
        const val PICK = 3000L
        const val START_ANIM_BEFORE_PICK = 300L
        const val REPEAT_ANIM_BEFORE_PICK = 15L
        const val REPEAT_ANIM_AFTER_PICK = 30L
        const val RESET = 4000L
        const val SOUND_EFFECT = 1000L
        const val SNACKBAR = 2000L
    }

    override var mediator: Mediator? = null
    var fingerPicker: FingerPicker? = null
    var fingerCount = 1

    val fingerMap = FingerMap()

    private val eventHandler = CanvasEventHandler(this)

    private var shouldKeepDrawn by Delegates.notNull<Boolean>()

    private val soundEffector = SoundEffector(context)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    init {
        resetAll()
    }

    private fun resetAll() {
        mediator?.setPressed(false)
        fingerPicker?.reset(context)
        fingerMap.clear()
        shouldKeepDrawn = false
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (shouldKeepDrawn) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                Log.d(TAG, "onTouchEvent : ACTION_DOWN")
                addNewPoint(event)
                stopPressedJobs()
                triggerPressedJobs()
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                Log.d(TAG, "onTouchEvent : ACTION_MOVE")
                movePoint(event)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                Log.d(TAG, "onTouchEvent : ACTION_UP")
                removePoint(event)
                val reset = resetAllIfEmptyPoint()
                stopPressedJobs()
                if (!reset) {
                    triggerPressedJobs()
                }
                invalidate()
                return true
            }
        }
        return false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == INVISIBLE) {
            stopPressedJobs()
            resetAll()
        }
    }

    private fun addNewPoint(event: MotionEvent) {
        if (fingerMap.isFull()) {
            return
        }
        // first touch!
        if (fingerMap.isEmpty()) {
            mediator?.setPressed(true)
        }
        val pointerId = fingerMap.add(event)
        Log.d(TAG, "addNewPoint : $pointerId")
    }

    private fun stopPressedJobs() {
        stopSound()
        stopSnackbar()
        stopSelect()
        stopAnim()
    }

    private fun stopSound() {
        soundEffector.stop()
    }

    private fun stopSnackbar() {
        eventHandler.removeEventIfSent(CanvasEvent.SNACKBAR)
    }

    private fun stopSelect() {
        eventHandler.removeEventIfSent(CanvasEvent.PICK)
    }

    private fun stopAnim() {
        eventHandler.removeEventIfSent(CanvasEvent.ANIM_BEFORE_PICK)
    }

    private fun triggerPressedJobs() {
        if (canSelect()) {
            triggerSound()
            triggerSelect()
            triggerAnim()
        } else {
            triggerSnackbar()
        }
    }

    private fun canSelect(): Boolean {
        return fingerMap.size > fingerCount
    }

    private fun triggerSound() {
        soundEffector.playTriggerInMillis(DelayedMillis.SOUND_EFFECT)
    }

    private fun triggerSelect() {
        eventHandler.sendEventDelayed(CanvasEvent.PICK, DelayedMillis.PICK)
    }

    private fun triggerAnim() {
        eventHandler.sendEventDelayed(
            CanvasEvent.ANIM_BEFORE_PICK,
            DelayedMillis.START_ANIM_BEFORE_PICK
        )
    }

    private fun triggerSnackbar() {
        eventHandler.sendEventDelayed(CanvasEvent.SNACKBAR, DelayedMillis.SNACKBAR)
    }

    private fun movePoint(event: MotionEvent) {
        fingerMap.move(event)
        Log.d(TAG, "movePoint")
    }

    private fun removePoint(event: MotionEvent) {
        val pointerId = fingerMap.remove(event)
        Log.d(TAG, "removePoint : $pointerId")
    }

    private fun resetAllIfEmptyPoint(): Boolean {
        if (fingerMap.isEmpty()) {
            resetAll()
            return true
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (shouldKeepDrawn) {
            fingerPicker?.drawSelected(canvas)
        } else {
            fingerPicker?.draw(canvas)
        }
    }

    override fun handleMessage(event: CanvasEvent) {
        when (event) {
            CanvasEvent.PICK -> {
                doPick(fingerCount)

                playSelectSound()

                stopAnim()
                doPickAnim()
                keepDrawnAwhile()
                invalidate()

                doVibrate()
            }
            CanvasEvent.ANIM_BEFORE_PICK -> {
                doAnim()
                invalidate()
            }
            CanvasEvent.ANIM_AFTER_PICK -> {
                doPickAnim()
                invalidate()
            }
            CanvasEvent.RESET -> {
                resetAll()
                invalidate()
            }
            CanvasEvent.SNACKBAR -> {
                showMessage()
            }
        }
    }

    private fun doPick(fingerCount: Int) {
        fingerPicker?.pick(fingerCount)
    }

    private fun playSelectSound() {
        soundEffector.playSelect()
    }

    private fun keepDrawnAwhile() {
        shouldKeepDrawn = true

        eventHandler.removeEventIfSent(CanvasEvent.RESET)
        eventHandler.sendEventDelayed(CanvasEvent.RESET, DelayedMillis.RESET)
    }

    private fun doVibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(100, 100)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(100)
        }
    }

    private fun doAnim() {
        eventHandler.sendEventDelayedIfNotSent(
            CanvasEvent.ANIM_BEFORE_PICK,
            DelayedMillis.REPEAT_ANIM_BEFORE_PICK
        )
    }

    private fun doPickAnim() {
        eventHandler.sendEventDelayedIfNotSent(
            CanvasEvent.ANIM_AFTER_PICK,
            DelayedMillis.REPEAT_ANIM_AFTER_PICK
        )
    }

    private fun showMessage() {
        CenterSnackbar.showShort(this, R.string.pickImpossibleMessage)
    }

    fun destroy() {
        soundEffector.release()
    }
}