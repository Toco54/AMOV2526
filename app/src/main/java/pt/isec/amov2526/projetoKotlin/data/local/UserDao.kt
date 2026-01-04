package pt.isec.amov2526.projetoKotlin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)


    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUser(): UserEntity?


    @Query("DELETE FROM user_profile")
    suspend fun clearUser()
}