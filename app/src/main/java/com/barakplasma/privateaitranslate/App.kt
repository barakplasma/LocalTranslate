/*
 * Copyright (c) 2023 You Apps
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

package com.barakplasma.privateaitranslate

import android.app.Application
import com.barakplasma.privateaitranslate.engine.GeminiNanoEngine
import com.barakplasma.privateaitranslate.engine.MLKitEngine
import com.barakplasma.privateaitranslate.engine.TranslateGemmaEngine
import com.barakplasma.privateaitranslate.util.CrashLogger
import com.barakplasma.privateaitranslate.util.EnginePreferencesProviderImpl
import com.barakplasma.privateaitranslate.util.Preferences
import com.barakplasma.privateaitranslate.util.SpeechHelper
import net.youapps.translation_engines.TranslationEngine
import net.youapps.translation_engines.TranslationEngines

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        CrashLogger.init(this)

        Preferences.initialize(
            this
        )

        DatabaseHolder().initDb(
            this
        )

        SpeechHelper.initTTS(this)

        val settingsProvider = EnginePreferencesProviderImpl()
        translationEngines = if (BuildConfig.ON_DEVICE_ONLY) {
            listOf(
                GeminiNanoEngine(settingsProvider),
                MLKitEngine(settingsProvider),
                TranslateGemmaEngine(settingsProvider, this)
            )
        } else {
            listOf(
                GeminiNanoEngine(settingsProvider),
                MLKitEngine(settingsProvider),
                TranslateGemmaEngine(settingsProvider, this)
            ) + TranslationEngines.getAllEngines(settingsProvider)
        }

        // initialize all translation engines
        updateAllTranslationEngines()
    }

    companion object {
        lateinit var translationEngines: List<TranslationEngine>

        fun updateAllTranslationEngines() {
            for (engine in translationEngines) engine.createOrRecreate()
        }

        fun getAvailableEngines(): List<TranslationEngine> {
            if (BuildConfig.ON_DEVICE_ONLY) return translationEngines.filter { it.isOnDevice }
            val highSecurityMode = Preferences.get(Preferences.highSecurityModeKey, false)
            return if (highSecurityMode) translationEngines.filter { it.isOnDevice } else translationEngines
        }
    }
}
