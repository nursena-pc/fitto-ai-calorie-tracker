package com.example.fitto.data.local

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateKeyFormatter {
    fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
