package pt.isec.amov2526.projetoKotlin

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.local.AppDatabase
import pt.isec.amov2526.projetoKotlin.utils.LocaleHelper

/**
 * Ponto de entrada da aplicação Android.
 *
 * Responsável pela configuração global:
 * 1. Inicialização da base de dados local (Room).
 * 2. Aplicação das preferências de idioma (Locale).
 * 3. Mecanismo de segurança "Fail-Safe": Garante que o estado do sistema no backend é resetado para "OFFLINE"
 *    no arranque, prevenindo inconsistências se a app tiver crashado anteriormente enquanto estava "ACTIVE".
 */
class SafetySecApp : Application() {

    // Inicialização preguiçosa da base de dados local.
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // Restaura o idioma preferido do utilizador.
        LocaleHelper.applyInitialLocale(this)


        // Logging de ciclo de vida para diagnóstico.
        Log.i("SafetySecApp", "A aplicação iniciou e o Firebase está pronto.")

        // Lógica Fail-Safe: Se o utilizador já estiver logado, garante que o estado no servidor reflete o arranque a frio (OFFLINE).
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)
                        .update("systemStatus", "OFFLINE")
                        .addOnSuccessListener {
                            Log.i(
                                "SafetySecApp",
                                "Estado do sistema resetado para OFFLINE no arranque."
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e(
                                "SafetySecApp",
                                "Falha ao resetar estado para OFFLINE: ${e.message}"
                            )
                        }
                } catch (e: Exception) {
                    Log.e("SafetySecApp", "Erro excecional ao resetar estado: ${e.message}")
                }
            }
        }
    }
}
