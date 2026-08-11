package com.example.surymeter.ui

import java.util.Locale

object Format {

    fun bytes(value: Long): String {
        var v = value.toDouble()
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var i = 0
        while (v >= 1024 && i < units.size - 1) {
            v /= 1024
            i++
        }
        return if (i == 0) {
            "${v.toLong()} ${units[i]}"
        } else {
            String.format(Locale.US, "%.1f %s", v, units[i])
        }
    }

    fun speed(bps: Long): String = "${bytes(bps)}/s"

    fun dateLabel(date: String): String = date.substringAfterLast('-')
}
