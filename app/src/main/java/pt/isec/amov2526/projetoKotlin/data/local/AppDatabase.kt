package pt.isec.amov2526.projetoKotlin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de Dados local Room para a aplicação SafetySec.
 *
 * Armazena persistentemente o perfil do utilizador logado para:
 * 1. Acesso rápido ao arranque (evita chamadas de rede bloqueantes).
 * 2. Funcionamento offline do modo "Protegido".
 *
 * @property entities Entidades geridas (apenas UserEntity nesta versão).
 * @property version Versão do esquema (incrementar em alterações estruturais).
 */
@Database(entities = [UserEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtém a instância singleton da base de dados.
         *
         * Utiliza double-checked locking para garantir thread-safety durante a criação.
         * Configurada com `fallbackToDestructiveMigration` para simplificar o desenvolvimento (limpa dados em mudanças de esquema).
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safetysec_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
