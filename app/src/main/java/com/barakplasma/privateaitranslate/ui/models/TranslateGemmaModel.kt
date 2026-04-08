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

package com.barakplasma.privateaitranslate.ui.models

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barakplasma.privateaitranslate.engine.TranslateGemmaEngine
import com.barakplasma.privateaitranslate.util.TranslateGemmaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Download states: null = not started, 0..1 = in progress, 1f = done, -1f = error */
enum class DownloadState { IDLE, DOWNLOADING, DONE, ERROR }

class TranslateGemmaModel : ViewModel() {
    var isModelDownloaded by mutableStateOf(false)
    var downloadProgress by mutableFloatStateOf(0f)
    var downloadState by mutableStateOf(DownloadState.IDLE)
    var modelFileSizeBytes by mutableStateOf(0L)

    private var activeDownloadId = -1L
    private var pollJob: Job? = null

    fun init(context: Context) {
        isModelDownloaded = TranslateGemmaEngine.getModelFile(context).exists()
        modelFileSizeBytes = TranslateGemmaHelper.getModelFileSizeBytes(context)
    }

    fun startDownload(context: Context) {
        if (downloadState == DownloadState.DOWNLOADING) return
        downloadState = DownloadState.DOWNLOADING
        downloadProgress = 0f

        viewModelScope.launch(Dispatchers.IO) {
            val downloadId = TranslateGemmaHelper.startDownload(context)
            if (downloadId == -1L) {
                // Already downloaded
                isModelDownloaded = true
                modelFileSizeBytes = TranslateGemmaHelper.getModelFileSizeBytes(context)
                downloadState = DownloadState.DONE
                return@launch
            }
            activeDownloadId = downloadId
            pollDownloadProgress(context, downloadId)
        }
    }

    private fun pollDownloadProgress(context: Context, downloadId: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val progress = TranslateGemmaHelper.queryProgress(context, downloadId)
                when (progress) {
                    null, -1f -> {
                        downloadState = DownloadState.ERROR
                        break
                    }
                    1f -> {
                        downloadProgress = 1f
                        isModelDownloaded = TranslateGemmaEngine.getModelFile(context).exists()
                        modelFileSizeBytes = TranslateGemmaHelper.getModelFileSizeBytes(context)
                        downloadState = DownloadState.DONE
                        break
                    }
                    else -> {
                        downloadProgress = progress
                        delay(500)
                    }
                }
            }
        }
    }

    fun cancelDownload(context: Context) {
        pollJob?.cancel()
        if (activeDownloadId != -1L) {
            TranslateGemmaHelper.cancelDownload(context, activeDownloadId)
            activeDownloadId = -1L
        }
        downloadState = DownloadState.IDLE
        downloadProgress = 0f
    }

    fun deleteModel(context: Context) {
        TranslateGemmaHelper.deleteModel(context)
        isModelDownloaded = false
        modelFileSizeBytes = 0L
        downloadState = DownloadState.IDLE
    }

    fun resetError() {
        downloadState = DownloadState.IDLE
    }
}
