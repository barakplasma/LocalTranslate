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

package com.barakplasma.privateaitranslate.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.barakplasma.privateaitranslate.engine.TranslateGemmaEngine
import java.io.File

object TranslateGemmaHelper {

    /**
     * Starts a DownloadManager download for the TranslateGemma INT4 model.
     * Returns the DownloadManager download ID, which can be used to query progress.
     * Returns -1 if the model is already downloaded.
     */
    fun startDownload(context: Context): Long {
        val modelFile = TranslateGemmaEngine.getModelFile(context)
        if (modelFile.exists()) return -1L

        modelFile.parentFile?.mkdirs()

        val request = DownloadManager.Request(Uri.parse(TranslateGemmaEngine.MODEL_DOWNLOAD_URL))
            .setTitle("TranslateGemma model")
            .setDescription("Downloading on-device translation model (~2 GB)")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(modelFile))
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    /**
     * Queries the current download progress for a given DownloadManager download ID.
     * Returns a Float in 0..1 while in progress, 1f on success, -1f on failure, null if not found.
     */
    fun queryProgress(context: Context, downloadId: Long): Float? {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        dm.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val status = cursor.getInt(statusCol)
            val downloaded = cursor.getLong(bytesCol)
            val total = cursor.getLong(totalCol)
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> 1f
                DownloadManager.STATUS_FAILED -> -1f
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED ->
                    if (total > 0) (downloaded.toFloat() / total.toFloat()) else 0f
                else -> 0f
            }
        }
        return null
    }

    /** Cancels an in-progress download. */
    fun cancelDownload(context: Context, downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(downloadId)
    }

    /** Deletes the downloaded model file. Returns true on success. */
    fun deleteModel(context: Context): Boolean {
        return TranslateGemmaEngine.getModelFile(context).delete()
    }

    /** Returns the model file size in bytes, or 0 if not downloaded. */
    fun getModelFileSizeBytes(context: Context): Long {
        val file = TranslateGemmaEngine.getModelFile(context)
        return if (file.exists()) file.length() else 0L
    }

    /** Returns available storage in bytes on the external files directory. */
    fun getAvailableStorageBytes(context: Context): Long {
        val dir = context.getExternalFilesDir(null) ?: return 0L
        return dir.freeSpace
    }
}
