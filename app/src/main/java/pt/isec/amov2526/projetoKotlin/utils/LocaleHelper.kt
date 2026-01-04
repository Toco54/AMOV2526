package pt.isec.amov2526.projetoKotlin.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    fun setLocale(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getSelectedLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales[0]?.language ?: Locale.getDefault().language
        } else {
            Locale.getDefault().language
        }
    }

    fun applyInitialLocale(context: Context) {
        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedLang = sharedPrefs.getString("language", null)
        
        if (savedLang != null) {
            setLocale(savedLang)
        } else {

            val deviceLang = Locale.getDefault().language
            if (deviceLang == "pt") {
                setLocale("pt")
            } else {
                setLocale("en")
            }
        }
    }

    fun saveAndSetLocale(context: Context, languageCode: String) {
        val sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("language", languageCode).apply()
        setLocale(languageCode)
    }
}
