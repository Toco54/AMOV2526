package pt.isec.amov2526.projetoKotlin

import android.app.Application
import android.util.Log
import pt.isec.amov2526.projetoKotlin.data.local.AppDatabase

class SafetySecApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // O override aqui serve para seguir a indicação do slide P03:
        // monitorizar o ciclo de vida da App.
        Log.i("SafetySecApp", "A aplicação iniciou e o Firebase está pronto.")
    }
}