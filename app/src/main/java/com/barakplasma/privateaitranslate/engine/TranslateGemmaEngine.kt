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
import com.barakplasma.privateaitranslate.util.CrashLogger
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.flow.collect
import net.youapps.translation_engines.ApiKeyState
import net.youapps.translation_engines.EngineSettingsProvider
import net.youapps.translation_engines.Language
import net.youapps.translation_engines.Translation
import net.youapps.translation_engines.TranslationEngine
import java.io.File

private const val TAG = "TranslateGemma"

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
    override val supportedModels = listOf("CPU", "GPU")

    private var liveEngine: Engine? = null

    override fun createOrRecreate(): TranslationEngine = apply {
        closeLiveEngine()
    }

    private fun closeLiveEngine() {
        try {
            liveEngine?.close()
        } catch (e: Exception) {
            CrashLogger.e(TAG, "Failed to close engine: ${e.message}", e)
        }
        liveEngine = null
    }

    private fun getOrCreateEngine(): Engine {
        liveEngine?.let { return it }

        val modelFile = getModelFile(appContext)
        check(modelFile.exists()) {
            "TranslateGemma model not downloaded. Open Settings → TranslateGemma to download or import it."
        }

        return try {
            val selectedBackend = getSelectedModel() ?: "CPU"
            val backend = when (selectedBackend) {
                "GPU" -> Backend.GPU()
                else -> Backend.CPU()
            }
            val config = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = backend
            )
            CrashLogger.i(TAG, "Initializing engine ($selectedBackend) with model: ${modelFile.absolutePath} (${modelFile.length()} bytes)")
            val engine = Engine(config)
            engine.initialize()
            liveEngine = engine
            CrashLogger.i(TAG, "Engine initialized successfully ($selectedBackend)")
            engine
        } catch (e: Exception) {
            CrashLogger.e(TAG, "Failed to initialize engine: ${e.message}", e)
            throw IllegalStateException("TranslateGemma engine initialization failed: ${e.message}", e)
        }
    }

    override suspend fun getLanguages(): List<Language> = SUPPORTED_LANGUAGES

    override suspend fun translate(query: String, source: String, target: String): Translation {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            throw IllegalStateException("TranslateGemma requires Android 12 (API 31) or higher")
        }

        val engine = getOrCreateEngine()
        val sourceLang = if (source.isEmpty() || source == autoLanguageCode) "auto" else source
        val prompt = "<src>$sourceLang</src><dst>$target</dst><text>$query</text>"

        CrashLogger.i(TAG, "About to translate: source=$sourceLang target=$target query='$query' backend=${getSelectedModel() ?: "CPU"}")

        return try {
            val sb = StringBuilder()
            CrashLogger.i(TAG, "Creating conversation...")
            engine.createConversation().use { conversation ->
                CrashLogger.i(TAG, "Sending message async...")
                conversation.sendMessageAsync(prompt).collect { chunk ->
                    try {
                        sb.append(chunk.toString())
                    } catch (e: Exception) {
                        CrashLogger.w(TAG, "Failed to append chunk: ${e.message}", e)
                    }
                }
            }
            CrashLogger.i(TAG, "Translation complete: '${sb.toString().take(100)}'")
            Translation(translatedText = sb.toString().trim())
        } catch (e: Exception) {
            CrashLogger.e(TAG, "Translation failed: ${e.message}", e)
            throw IllegalStateException("Translation failed: ${e.message}", e)
        }
    }

    companion object {
        const val MODEL_FILENAME = "translategemma-4b-it-int4-generic.litertlm"
        const val MODEL_DIR = "translategemma"
        const val MODEL_SIZE_BYTES = 2_000_000_000L
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
