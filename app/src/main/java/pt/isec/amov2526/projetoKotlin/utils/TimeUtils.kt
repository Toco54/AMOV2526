package pt.isec.amov2526.projetoKotlin.utils

import java.util.Calendar

data class StatusResult(
    val isActive: Boolean,
    val label: String, // "ACTIVE", "PAUSED", "SCHEDULED", "DISABLED"
    val subLabel: String?
)

object TimeUtils {

    fun calculateStatus(
        isEnabled: Boolean,
        pausedUntil: Long,
        isScheduleEnabled: Boolean,
        days: List<Int>, // 0=Sun, 1=Mon...
        start: String,
        end: String,
        now: Long = System.currentTimeMillis()
    ): StatusResult {
        
        // 1. Check Pause (Timer/Indefinite Priority over "Disabled" flag)
        if (pausedUntil > now) {
            val remaining = pausedUntil - now
            return StatusResult(false, "PAUSED", "Paused: ${formatDuration(remaining)}")
        }
        
        if (pausedUntil == -1L) {
            return StatusResult(false, "PAUSED", "Paused indefinitely")
        }

        if (!isEnabled) {
            return StatusResult(false, "DISABLED", "Disabled by user")
        }
        
        // 2. Check Schedule
        if (isScheduleEnabled) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            val currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY=1 -> 0
            
            val isTodayActive = days.contains(currentDay)
            val (startH, startM) = parseTime(start)
            val (endH, endM) = parseTime(end)
            
            val nowTime = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startTime = startH * 60 + startM
            val endTime = endH * 60 + endM
            
            val isActiveWindow = isTodayActive && (nowTime in startTime until endTime)
            
            if (isActiveWindow) {
                // User defined this time frame to be DISABLED
                return StatusResult(false, "SCHEDULED", "Scheduled off until $end")
            }
            
            // Outside disabled window -> Active
            // Optional: Show when next disabled window starts? For now just Active.
            return StatusResult(true, "ACTIVE", null)
        }
        
        return StatusResult(true, "ACTIVE", null)
    }

    fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        return if (parts.size == 2) {
            (parts[0].toIntOrNull() ?: 9) to (parts[1].toIntOrNull() ?: 0)
        } else 9 to 0
    }

    fun formatDuration(millis: Long): String {
        val minutes = millis / 1000 / 60
        val seconds = (millis / 1000) % 60
        if (minutes > 60) {
             val hours = minutes / 60
             val mins = minutes % 60
             return "${hours}h ${mins}m"
        }
        return "${minutes}m ${seconds}s"
    }

    fun formatSchedule(days: List<Int>, start: String, end: String): String {
        if (days.isEmpty() || start.isEmpty() || end.isEmpty()) return ""
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val activeDays = days.sorted().joinToString(", ") { dayNames[it] }
        return "🕒 $activeDays • $start - $end"
    }

    fun getNextActiveTime(days: List<Int>, startH: Int, startM: Int, now: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        var dayOffset = 0
        // Limit search to 7 days
        while (dayOffset < 8) {
            val checkDay = (cal.get(Calendar.DAY_OF_WEEK) - 1 + dayOffset) % 7
            if (days.contains(checkDay)) {
                // Found a potential day.
                // If it's today (offset 0), check if start time is in future.
                if (dayOffset == 0) {
                    val nowTime = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                    val targetTime = startH * 60 + startM
                    if (targetTime > nowTime) {
                        val diffMins = targetTime - nowTime
                        val h = diffMins / 60
                        val m = diffMins % 60
                        return "Active in ${h}h ${m}m"
                    }
                } else {
                    // Future day
                    // "Active Mon 09:00"
                    val dayName = when(checkDay) {
                        0 -> "Sun"; 1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"
                        else -> ""
                    }
                    return "Active $dayName ${String.format("%02d:%02d", startH, startM)}"
                }
            }
            dayOffset++
        }
        return "Schedule Inactive"
    }
}
