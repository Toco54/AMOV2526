package pt.isec.amov2526.projetoKotlin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val role: String,
    val pin: String? = null,
    val mfaEnabled: Boolean = false
)