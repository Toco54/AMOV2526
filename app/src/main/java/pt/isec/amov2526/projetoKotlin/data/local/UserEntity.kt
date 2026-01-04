package pt.isec.amov2526.projetoKotlin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room que representa o perfil do utilizador local.
 *
 * Mapeia a tabela `user_profile` na base de dados SQLite local.
 * Contém dados essenciais para o funcionamento offline e cache de UI.
 *
 * @property uid Identificador único do Firebase Auth (Primary Key).
 * @property name Nome de exibição.
 * @property email Email de contacto.
 * @property role Role do utilizador (MONITOR, PROTECTED, BOTH).
 * @property pin PIN de segurança (hash ou plain, dependendo da implementação) para desarmar alertas.
 * @property mfaEnabled Se a autenticação de dois fatores está ativa.
 */
@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val role: String,
    val pin: String? = null,
    val mfaEnabled: Boolean = false
)
