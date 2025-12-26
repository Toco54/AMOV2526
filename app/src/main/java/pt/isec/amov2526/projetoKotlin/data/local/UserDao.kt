package pt.isec.amov2526.projetoKotlin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    // Insere ou Atualiza (se já existir, substitui)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Vai buscar o utilizador (como só há 1 logado de cada vez, limitamos a 1)
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUser(): UserEntity?

    // Limpa a tabela (útil no Logout)
    @Query("DELETE FROM user_profile")
    suspend fun clearUser()
}