package pt.isec.amov2526.projetoKotlin.utils

import java.util.Calendar

/**
 * Resultado de uma verificação de estado temporal.
 * @param isActive Se o sistema deve estar ativo.
 * @param label Rótulo principal para a UI (ACTIVE, PAUSED, SCHEDULED, DISABLED).
 * @param subLabel Descrição secundária (ex: "Active until 18:00").
 */
data class StatusResult(
    val isActive: Boolean,
    val label: String,
    val subLabel: String?
)

/**
 * Utilitário para cálculos de agendamento e estado temporal.
 *
 * Encapsula a lógica complexa de determinar se uma regra ou o sistema global deve estar ativo
 * com base em:
 * 1. Pausas temporárias (`pausedUntil`).
 * 2. Agendamentos semanais (Dias da semana + Horário Início/Fim).
 * 3. Master Switch (`isEnabled`).
 */
object TimeUtils {

    /**
     * Calcula o estado efetivo de um componente (Regra ou Sistema) num dado instante.
     */
    fun calculateStatus(
        isEnabled: Boolean,
        pausedUntil: Long,
        isScheduleEnabled: Boolean,
        days: List<Int>,
        start: String,
        end: String,
        now: Long = System.currentTimeMillis()
    ): StatusResult {

        // 1. Verificação de Pausa
        if (pausedUntil > now) {
            val remaining = pausedUntil - now
            return StatusResult(false, "PAUSED", "Paused: ${formatDuration(remaining)}")
        }

        if (pausedUntil == -1L) {
            return StatusResult(false, "PAUSED", "Paused indefinitely")
        }

        // 2. Verificação de Master Switch
        if (!isEnabled) {
            return StatusResult(false, "DISABLED", "Disabled by user")
        }

        // 3. Verificação de Agendamento
        if (isScheduleEnabled) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            // Calendar.DAY_OF_WEEK: 1=Domingo .. 7=Sábado -> Normalizamos para 0=Dom .. 6=Sáb
            val currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1

            val (startH, startM) = parseTime(start)
            val (endH, endM) = parseTime(end)

            val nowTime = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val startTime = startH * 60 + startM
            val endTime = endH * 60 + endM

            val isDayActive = days.contains(currentDay)
            val isTimeActive = nowTime in startTime until endTime

            if (isDayActive && isTimeActive) {
                // Dentro do horário ativo.
                return StatusResult(true, "ACTIVE", "Active until $end")
            } else {
                // Fora do horário -> Inativo (Agendado).
                val msg = if (!isDayActive) "Schedule: Inactive Today"
                else if (nowTime < startTime) "Schedule: Starts at $start"
                else "Schedule: Ended at $end"

                return StatusResult(false, "SCHEDULED", msg)
            }
        }

        // Se não houver pausa, nem disable, nem agendamento -> Sempre Ativo.
        return StatusResult(true, "ACTIVE", null)
    }

    /**
     * Converte string "HH:mm" para par de Inteiros (Hora, Minuto).
     */
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

    /**
     * Calcula o próximo horário de ativação para exibição na UI quando o sistema está agendado mas inativo.
     */
    fun getNextActiveTime(days: List<Int>, startH: Int, startM: Int, now: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        var dayOffset = 0

        while (dayOffset < 8) {
            val checkDay = (cal.get(Calendar.DAY_OF_WEEK) - 1 + dayOffset) % 7
            if (days.contains(checkDay)) {

                // Se é hoje
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
                    // Dia futuro
                    val dayName = when (checkDay) {
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
