/*
 * Copyright (c) 2026 You Apps
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.barakplasma.privateaitranslate.engine

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.collect
import net.youapps.translation_engines.ApiKeyState
import net.youapps.translation_engines.EngineSettingsProvider
import net.youapps.translation_engines.Language
import net.youapps.translation_engines.Translation
import net.youapps.translation_engines.TranslationEngine
import java.io.File

/**
 * On-device translation engine using the TranslateGemma 4B INT4 model via LiteRT-LM.
 *
 * The model file must be downloaded first via [TranslateGemmaHelper.startDownload].
 * Requires Android 12 (API 31+).
 *
 * SDK integration note: The LiteRT-LM SDK (com.google.ai.edge.litertlm:litertlm-android)
 * is loaded via reflection at runtime to avoid a hard compile-time dependency on the
 * prebuilt AAR, which is not consistently available on Google Maven. The AAR can be
 * placed in app/libs/ to enable inference; if it is absent the engine throws
 * ModelNotAvailableException when translate() is called.
 *
 * Reflection call equivalent:
 *   val config = EngineConfig(modelPath, Backend.CPU())
 *   Engine(config).use { engine ->
 *     engine.initialize()
 *     engine.createConversation().use { conv ->
 *       conv.sendMessageAsync("<<<source>>>$src<<<target>>>$dst<<<text>>>$query")
 *         .collect { sb.append(it.toString()) }
 *     }
 *   }
 */
class TranslateGemmaEngine(
    settingsProvider: EngineSettingsProvider,
    private val appContext: Context
) : TranslationEngine(settingsProvider) {
    override val name = "TranslateGemma (On-Device)"
    override val defaultUrl = ""
    override val urlModifiable = false
    override val apiKeyState = ApiKeyState.DISABLED
    override val autoLanguageCode = "auto"
    override val supportsAudio = false
    override val isOnDevice = true

    // Holds the live Engine instance (class loaded via reflection).
    private var liveEngine: Any? = null
    private var engineClass: Class<*>? = null
    private var sdkAvailable: Boolean? = null

    override fun createOrRecreate(): TranslationEngine = apply {
        closeLiveEngine()
        sdkAvailable = null // re-probe on next call
    }

    private fun closeLiveEngine() {
        try {
            (liveEngine as? AutoCloseable)?.close()
        } catch (_: Exception) {}
        liveEngine = null
    }

    /** Returns true if the LiteRT-LM SDK classes are loadable. */
    private fun isSdkAvailable(): Boolean {
        sdkAvailable?.let { return it }
        return try {
            Class.forName("com.google.ai.edge.litertlm.Engine")
            true.also { sdkAvailable = it }
        } catch (_: ClassNotFoundException) {
            false.also { sdkAvailable = it }
        }
    }

    private fun getOrCreateEngine(): Any {
        liveEngine?.let { return it }

        val modelFile = getModelFile(appContext)
        check(modelFile.exists()) {
            "TranslateGemma model not downloaded. Open Settings → TranslateGemma to download it."
        }
        check(isSdkAvailable()) {
            "LiteRT-LM SDK not found. Add litertlm-android AAR to app/libs/ to enable this engine."
        }

        val engineConfigClass = Class.forName("com.google.ai.edge.litertlm.EngineConfig")
        val backendClass = Class.forName("com.google.ai.edge.litertlm.Backend")
        val cpuClass = Class.forName("com.google.ai.edge.litertlm.Backend\$CPU")
        val cpuInstance = cpuClass.getDeclaredConstructor().newInstance()
        val config = engineConfigClass.getDeclaredConstructor(
            String::class.java, backendClass,
            backendClass, backendClass,
            Int::class.javaObjectType, Int::class.javaObjectType,
            String::class.java
        ).newInstance(modelFile.absolutePath, cpuInstance, null, null, null, null, null)

        val engineClass = Class.forName("com.google.ai.edge.litertlm.Engine")
        val engine = engineClass.getDeclaredConstructor(engineConfigClass).newInstance(config)
        engineClass.getMethod("initialize").invoke(engine)
        liveEngine = engine
        this.engineClass = engineClass
        return engine
    }

    override suspend fun getLanguages(): List<Language> = SUPPORTED_LANGUAGES

    override suspend fun translate(query: String, source: String, target: String): Translation {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw IllegalStateException("TranslateGemma requires Android 12 (API 31) or higher")
        }

        val engine = getOrCreateEngine()
        val sourceLang = if (source.isEmpty() || source == autoLanguageCode) "auto" else source
        val prompt = "<<<source>>>$sourceLang<<<target>>>$target<<<text>>>$query"

        val sb = StringBuilder()
        val conversation = engineClass!!.getMethod("createConversation").invoke(engine)
        try {
            val convClass = Class.forName("com.google.ai.edge.litertlm.Conversation")
            val sendAsync = convClass.getMethod("sendMessageAsync", String::class.java)
            @Suppress("UNCHECKED_CAST")
            val flow = sendAsync.invoke(conversation, prompt) as kotlinx.coroutines.flow.Flow<Any>
            flow.collect { chunk -> sb.append(chunk.toString()) }
        } finally {
            (conversation as? AutoCloseable)?.close()
        }

        return Translation(translatedText = sb.toString().trim())
    }

    companion object {
        const val MODEL_FILENAME = "translategemma-4b-it-int4-generic.litertlm"
        const val MODEL_DIR = "translategemma"
        const val MODEL_SIZE_BYTES = 2_000_000_000L // ~2 GB
        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/barakplasma/translategemma-4b-it-android-task-quantized/resolve/main/artifacts/int4-generic/translategemma-4b-it-int4-generic.litertlm"

        fun getModelFile(context: Context): File =
            File(context.getExternalFilesDir(null), "$MODEL_DIR/$MODEL_FILENAME")

        private val SUPPORTED_LANGUAGES = listOf(
            Language("af", "Afrikaans"),
            Language("ar", "Arabic"),
            Language("be", "Belarusian"),
            Language("bg", "Bulgarian"),
            Language("bn", "Bengali"),
            Language("ca", "Catalan"),
            Language("cs", "Czech"),
            Language("cy", "Welsh"),
            Language("da", "Danish"),
            Language("de", "German"),
            Language("el", "Greek"),
            Language("en", "English"),
            Language("es", "Spanish"),
            Language("et", "Estonian"),
            Language("eu", "Basque"),
            Language("fa", "Persian"),
            Language("fi", "Finnish"),
            Language("fr", "French"),
            Language("ga", "Irish"),
            Language("gl", "Galician"),
            Language("gu", "Gujarati"),
            Language("he", "Hebrew"),
            Language("hi", "Hindi"),
            Language("hr", "Croatian"),
            Language("hu", "Hungarian"),
            Language("hy", "Armenian"),
            Language("id", "Indonesian"),
            Language("is", "Icelandic"),
            Language("it", "Italian"),
            Language("ja", "Japanese"),
            Language("ka", "Georgian"),
            Language("kk", "Kazakh"),
            Language("km", "Khmer"),
            Language("kn", "Kannada"),
            Language("ko", "Korean"),
            Language("lt", "Lithuanian"),
            Language("lv", "Latvian"),
            Language("mk", "Macedonian"),
            Language("ml", "Malayalam"),
            Language("mn", "Mongolian"),
            Language("mr", "Marathi"),
            Language("ms", "Malay"),
            Language("mt", "Maltese"),
            Language("my", "Burmese"),
            Language("nb", "Norwegian"),
            Language("nl", "Dutch"),
            Language("pl", "Polish"),
            Language("pt", "Portuguese"),
            Language("ro", "Romanian"),
            Language("ru", "Russian"),
            Language("sk", "Slovak"),
            Language("sl", "Slovenian"),
            Language("sq", "Albanian"),
            Language("sr", "Serbian"),
            Language("sv", "Swedish"),
            Language("sw", "Swahili"),
            Language("ta", "Tamil"),
            Language("te", "Telugu"),
            Language("th", "Thai"),
            Language("tr", "Turkish"),
            Language("uk", "Ukrainian"),
            Language("ur", "Urdu"),
            Language("uz", "Uzbek"),
            Language("vi", "Vietnamese"),
            Language("zh", "Chinese (Simplified)"),
            Language("zh-TW", "Chinese (Traditional)"),
        )
    }
}
