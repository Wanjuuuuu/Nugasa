package com.eee.www.nugasa.utils

import android.os.Handler
import android.os.Looper
import android.os.Message

enum class CanvasEvent {
    PICK,
    ANIM_BEFORE_PICK,
    ANIM_AFTER_PICK,
    RESET,
    SNACKBAR;
}

class CanvasEventHandler(private val callback: Callback) : Handler.Callback {

    interface Callback {
        fun handleMessage(event: CanvasEvent)
    }

    private val eventHandler = Handler(Looper.getMainLooper(), this)

    fun sendEventDelayed(event: CanvasEvent, delayedMillis: Long) {
        eventHandler.sendEmptyMessageDelayed(event.ordinal, delayedMillis)
    }

    fun sendEventDelayedIfNotSent(event: CanvasEvent, delayedMillis: Long) {
        if (!eventHandler.hasMessages(event.ordinal)) {
            eventHandler.sendEmptyMessageDelayed(event.ordinal, delayedMillis)
        }
    }

    fun removeEventIfSent(event: CanvasEvent) {
        if (eventHandler.hasMessages(event.ordinal)) {
            eventHandler.removeMessages(event.ordinal)
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        return try {
            callback.handleMessage(toCanvasEvent(msg.what))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun toCanvasEvent(ordinal: Int): CanvasEvent {
        if (ordinal >= CanvasEvent.values().size) {
            throw Exception("Wrong ordinal : $ordinal")
        }
        return CanvasEvent.values()[ordinal]
    }
}