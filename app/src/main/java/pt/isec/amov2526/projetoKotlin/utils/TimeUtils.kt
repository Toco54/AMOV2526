package pt.isec.amov2526.projetoKotlin.utils

import java.util.Calendar

data class StatusResult(
    val isActive: Boolean,
    val label: String, 
    val subLabel: String?
)

object TimeUtils {

    fun calculateStatus(
        isEnabled: Boolean,
        pausedUntil: Long,
        isScheduleEnabled: Boolean,
        days: List<Int>, 
        start: String,
        end: String,
        now: Long = System.currentTimeMillis()
    ): StatusResult {

        
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

        
        if (isScheduleEnabled) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            val currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1 

            val (startH, startM) = parseTime(start)
            val (endH, endM) = parseTime(end)

            val nowTime = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startTime = startH * 60 + startM
            val endTime = endH * 60 + endM

            
            val isDayActive = days.contains(currentDay)
            val isTimeActive = nowTime in startTime until endTime

            if (isDayActive && isTimeActive) {
                
                return StatusResult(true, "ACTIVE", "Active until $end")
            } else {
                
                val msg = if (!isDayActive) "Schedule: Inactive Today"
                else if (nowTime < startTime) "Schedule: Starts at $start"
                else "Schedule: Ended at $end"

                return StatusResult(false, "SCHEDULED", msg)
            }
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
        
        return "🕒 Active: $activeDays • $start - $end"
    }

    fun getNextActiveTime(days: List<Int>, startH: Int, startM: Int, now: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        var dayOffset = 0
        
        while (dayOffset < 8) {
            val checkDay = (cal.get(Calendar.DAY_OF_WEEK) - 1 + dayOffset) % 7
            if (days.contains(checkDay)) {
                
                
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

    fun getCurrentTime(): String {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return String.format("%02d:%02d", h, m)
    }
}