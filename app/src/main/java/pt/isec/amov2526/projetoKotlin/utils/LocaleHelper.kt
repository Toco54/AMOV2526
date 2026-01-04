package pt.isec.amov2526.projetoKotlin.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Utilitário estático para gestão de Localização (Idioma) da aplicação.
 *
 * Utiliza o `AppCompatDelegate` para mudar o idioma em tempo de execução sem reiniciar a Activity manualmente.
 * Gere a persistência da escolha do utilizador em `SharedPreferences`.
 */
object LocaleHelper {
    /**
     * Aplica um novo locale à aplicação.
     * @param languageCode Código ISO da língua (ex: "pt", "en").
     */
    fun setLocale(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Retorna o idioma atualmente ativo na aplicação.
     */
    fun getSelectedLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales[0]?.language ?: Locale.getDefault().language
        } else {
            Locale.getDefault().language
        }
    }

    /**
     * Inicializa o locale no arranque da aplicação.
     * Verifica se existe uma preferência salva; caso contrário, aplica a lógica de fallback (PT ou EN).
     */
    fun applyInitialLocale(context: Context) {
        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedLang = sharedPrefs.getString("language", null)

        if (savedLang != null) {
            setLocale(savedLang)
        } else {
            // Deteção automática baseada no sistema.
            val deviceLang = Locale.getDefault().language
            if (deviceLang == "pt") {
                setLocale("pt")
            } else {
                setLocale("en")
            }
        }
    }

    /**
     * Guarda a preferência do utilizador e aplica a mudança imediatamente.
     */
    fun saveAndSetLocale(context: Context, languageCode: String) {
        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("language", languageCode).apply()
        setLocale(languageCode)
    }
}