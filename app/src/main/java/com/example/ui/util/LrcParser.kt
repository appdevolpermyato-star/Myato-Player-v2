package com.example.ui.util

import android.util.Log

data class LrcLine(
    val timeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(content: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        
        content.lines().forEach { rawLine ->
            regex.find(rawLine)?.let { match ->
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val text = match.groupValues[4].trim()
                
                val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                lines.add(LrcLine(totalMs, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
