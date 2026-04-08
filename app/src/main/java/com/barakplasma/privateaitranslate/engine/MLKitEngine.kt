package com.barakplasma.privateaitranslate.engine

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation as MLKitTranslation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import net.youapps.translation_engines.ApiKeyState
import net.youapps.translation_engines.EngineSettingsProvider
import net.youapps.translation_engines.Language
import net.youapps.translation_engines.Translation
import net.youapps.translation_engines.TranslationEngine
import java.util.Locale

class MLKitEngine(
    settingsProvider: EngineSettingsProvider
) : TranslationEngine(settingsProvider) {
    override val name = "Google ML Kit (On-Device)"
    override val defaultUrl = ""
    override val urlModifiable = false
    override val apiKeyState = ApiKeyState.DISABLED
    override val autoLanguageCode = null
    override val supportsAudio = false
    override val isOnDevice = true

    override fun createOrRecreate(): TranslationEngine = apply {
        // No global initialization needed; translator clients are created on demand.
    }

    override suspend fun getLanguages(): List<Language> {
        return TranslateLanguage.getAllLanguages().map { code ->
            Language(code, Locale(code).displayLanguage)
        }.sortedBy { it.name }
    }

    override suspend fun translate(query: String, source: String, target: String): Translation {
        if (source.isEmpty() || source == "auto") {
            throw IllegalArgumentException("Google ML Kit does not support automatic source language detection. Please select a specific source language.")
        }
        if (target.isEmpty() || target == "auto") {
            throw IllegalArgumentException("A specific target language must be selected.")
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
            
        val translator = MLKitTranslation.getClient(options)
        
        return try {
            val conditions = DownloadConditions.Builder().build()
            
            // Ensure the required translation model is downloaded before translating
            translator.downloadModelIfNeeded(conditions).await()
            
            val translatedText = translator.translate(query).await()
            Translation(translatedText)
        } finally {
            translator.close()
        }
    }
}
