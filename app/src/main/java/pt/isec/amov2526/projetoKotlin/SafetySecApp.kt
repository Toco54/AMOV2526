package pt.isec.amov2526.projetoKotlin

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.local.AppDatabase

class SafetySecApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()


        pt.isec.amov2526.projetoKotlin.utils.LocaleHelper.applyInitialLocale(this)



        Log.i("SafetySecApp", "A aplicação iniciou e o Firebase está pronto.")


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