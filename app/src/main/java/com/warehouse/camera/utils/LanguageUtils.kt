package com.warehouse.camera.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import java.util.*

object LanguageUtils {
    private const val LANGUAGE_PREF = "language_pref"
    private const val LANGUAGE_KEY = "selected_language"
    
    // Available languages
    enum class Language(val code: String) {
        ENGLISH("en"),
        RUSSIAN("ru"),
        CHINESE("zh")
    }
    
    // Set language for the app
    fun setLanguage(activity: AppCompatActivity, language: Language) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        
        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)
        
        // Update configuration using the appropriate method based on API level
        updateResourcesLocale(activity, locale)
        
        // Save the selected language
        val sharedPrefs = activity.getSharedPreferences(LANGUAGE_PREF, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(LANGUAGE_KEY, language.code).apply()
        
        // Restart activity to apply changes
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
    }
    
    // Get currently selected language
    fun getSelectedLanguage(context: Context): Language {
        val sharedPrefs = context.getSharedPreferences(LANGUAGE_PREF, Context.MODE_PRIVATE)
        val languageCode = sharedPrefs.getString(LANGUAGE_KEY, Language.ENGLISH.code) ?: Language.ENGLISH.code
        
        return when (languageCode) {
            Language.RUSSIAN.code -> Language.RUSSIAN
            Language.CHINESE.code -> Language.CHINESE
            else -> Language.ENGLISH
        }
    }
    
    // Apply saved language to activity
    fun applyLanguage(activity: AppCompatActivity) {
        val language = getSelectedLanguage(activity)
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        
        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)
        
        // Update configuration using the appropriate method based on API level
        updateResourcesLocale(activity, locale)
    }
    
    // Create a context configured with the given language
    fun createConfigurationContext(context: Context, language: Language): Context {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.createConfigurationContext(config)
        } else {
            val resources = context.resources
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
            context
        }
    }
    
    // Helper method to update resources with locale based on API level
    private fun updateResourcesLocale(context: Context, locale: Locale) {
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            val updatedContext = context.createConfigurationContext(configuration)
            context.resources.updateConfiguration(configuration, resources.displayMetrics)
            context.applicationContext.resources.updateConfiguration(configuration, resources.displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }
}