package com.example.markdown_editor.ui.util

import android.text.format.DateFormat
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock

class AndroidDateFormatter : DateFormatter {

    override fun formatRelativeTime(timeMillis: Long, maxTimeMillis: Long?): String {
        if (maxTimeMillis != null) {
            val currentMillis = Clock.System.now().toEpochMilliseconds()
            if ((currentMillis - timeMillis) > maxTimeMillis) {
                return formatDateTime(timeMillis)
            }
        }
        val timeString = DateUtils.getRelativeTimeSpanString(
            timeMillis,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        )
        return timeString?.toString() ?: formatDateTime(timeMillis)
    }

    override fun formatDateTime(timeMillis: Long, skeleton: String): String {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeMillis))
    }

    override fun formatDateOnly(timeMillis: Long, skeleton: String): String {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeMillis))
    }

    override fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }
}