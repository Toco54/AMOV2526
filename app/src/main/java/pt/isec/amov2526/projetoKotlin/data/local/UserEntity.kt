package pt.isec.amov2526.projetoKotlin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val uid: String, // O ID do Firebase será a nossa chave única
    val name: String,
    val email: String,
    val role: String, // "MONITOR" ou "PROTECTED"
    val pin: String? = null // Pode ser nulo se for Monitor
)