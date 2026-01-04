package pt.isec.amov2526.projetoKotlin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (DAO) para a entidade UserEntity.
 *
 * Define as operações de base de dados para o perfil do utilizador local.
 * Desenhado para um cenário de "Single User" (apenas um utilizador logado por dispositivo de cada vez).
 */
@Dao
interface UserDao {

    /**
     * Insere ou atualiza o perfil do utilizador.
     * A estratégia REPLACE garante que mantemos apenas a versão mais recente dos dados.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Obtém o perfil do utilizador atual.
     * Como a tabela é limpa no logout, assume-se que o único registo existente é o do utilizador ativo.
     */
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUser(): UserEntity?

    /**
     * Limpa todos os dados locais do utilizador.
     * Executado durante o logout para garantir segurança.
     */
    @Query("DELETE FROM user_profile")
    suspend fun clearUser()
}
