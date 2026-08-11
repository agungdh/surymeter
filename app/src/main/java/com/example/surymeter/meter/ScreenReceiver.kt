package com.example.surymeter.meter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> MeterState.update { it.copy(screenOn = true) }
            Intent.ACTION_SCREEN_OFF -> MeterState.update { it.copy(screenOn = false) }
        }
    }
}
