package com.example.surymeter.data

import android.content.Context
import android.content.SharedPreferences

enum class NotifStyle(val value: Int) {
    SPEED_ONLY(0),
    UP_DOWN(1),
    NETWORKS(2),
    FULL(3);

    companion object {
        fun from(value: Int): NotifStyle = entries.firstOrNull { it.value == value } ?: FULL
    }
}

object Settings {

    private const val PREFS = "settings"
    private const val KEY_NOTIF_STYLE = "notif_style"
    private const val KEY_USE_BITS = "use_bits"
    private const val KEY_SHOW_ON_LOCKSCREEN = "show_on_lockscreen"
    private const val KEY_SHOW_SIGNAL = "show_signal"
    private const val KEY_PAUSE_SCREEN_OFF = "pause_screen_off"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    private fun p(context: Context): SharedPreferences {
        init(context)
        return prefs!!
    }

    var notifStyle: NotifStyle
        get() = NotifStyle.from(prefs?.getInt(KEY_NOTIF_STYLE, NotifStyle.FULL.value) ?: NotifStyle.FULL.value)
        set(value) { prefs?.edit()?.putInt(KEY_NOTIF_STYLE, value.value)?.apply() }

    var useBits: Boolean
        get() = prefs?.getBoolean(KEY_USE_BITS, false) ?: false
        set(value) { prefs?.edit()?.putBoolean(KEY_USE_BITS, value)?.apply() }

    var showOnLockscreen: Boolean
        get() = prefs?.getBoolean(KEY_SHOW_ON_LOCKSCREEN, true) ?: true
        set(value) { prefs?.edit()?.putBoolean(KEY_SHOW_ON_LOCKSCREEN, value)?.apply() }

    var showSignal: Boolean
        get() = prefs?.getBoolean(KEY_SHOW_SIGNAL, false) ?: false
        set(value) { prefs?.edit()?.putBoolean(KEY_SHOW_SIGNAL, value)?.apply() }

    var pauseWhenScreenOff: Boolean
        get() = prefs?.getBoolean(KEY_PAUSE_SCREEN_OFF, true) ?: true
        set(value) { prefs?.edit()?.putBoolean(KEY_PAUSE_SCREEN_OFF, value)?.apply() }

    fun interfaceVersion(): Int = 1
}
