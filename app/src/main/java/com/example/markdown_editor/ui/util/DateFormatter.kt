package com.example.markdown_editor.ui.util

interface DateFormatter {
    fun formatRelativeTime(timeMillis: Long, maxTimeMillis: Long? = null): String
    fun formatDateTime(timeMillis: Long, skeleton: String = "MMMdHHmm"): String
    fun formatDateOnly(timeMillis: Long, skeleton: String = "MMMMd"): String
    fun isSameDay(t1: Long, t2: Long): Boolean

    companion object {
        const val MINUTE_MILLIS = 60_000L
        const val HOUR_MILLIS = 3_600_000L
    }
}