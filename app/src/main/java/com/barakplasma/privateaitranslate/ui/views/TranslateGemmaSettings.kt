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

package com.barakplasma.privateaitranslate.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.barakplasma.privateaitranslate.R
import com.barakplasma.privateaitranslate.ui.components.DialogButton
import com.barakplasma.privateaitranslate.ui.components.StyledIconButton
import com.barakplasma.privateaitranslate.ui.dialogs.FullscreenDialog
import com.barakplasma.privateaitranslate.ui.models.DownloadState
import com.barakplasma.privateaitranslate.ui.models.TranslateGemmaModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateGemmaSettings(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val model: TranslateGemmaModel = viewModel()

    LaunchedEffect(Unit) {
        model.init(context)
    }

    FullscreenDialog(
        onDismissRequest = onDismissRequest,
        topBar = {
            com.barakplasma.privateaitranslate.ui.components.SearchAppBar(
                title = stringResource(R.string.translategemma_title),
                value = "",
                onValueChange = {},
                navigationIcon = {
                    StyledIconButton(
                        imageVector = Icons.Default.ArrowBack,
                        onClick = onDismissRequest
                    )
                },
                actions = {}
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    item {
                        SelectionContainer(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(text = stringResource(R.string.translategemma_summary))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        when (model.downloadState) {
                            DownloadState.IDLE -> {
                                if (model.isModelDownloaded) {
                                    ModelStatusRow(
                                        label = stringResource(R.string.translategemma_model_downloaded),
                                        sizeBytes = model.modelFileSizeBytes
                                    ) {
                                        StyledIconButton(imageVector = Icons.Default.Delete) {
                                            model.deleteModel(context)
                                        }
                                    }
                                } else {
                                    NotDownloadedRow {
                                        model.startDownload(context)
                                    }
                                }
                            }

                            DownloadState.DOWNLOADING -> {
                                DownloadingRow(
                                    progress = model.downloadProgress,
                                    onCancel = { model.cancelDownload(context) }
                                )
                            }

                            DownloadState.DONE -> {
                                ModelStatusRow(
                                    label = stringResource(R.string.translategemma_model_downloaded),
                                    sizeBytes = model.modelFileSizeBytes
                                ) {
                                    StyledIconButton(imageVector = Icons.Default.Delete) {
                                        model.deleteModel(context)
                                    }
                                }
                            }

                            DownloadState.ERROR -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.translategemma_download_error),
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    StyledIconButton(
                                        imageVector = Icons.Default.Error,
                                        tint = MaterialTheme.colorScheme.error
                                    ) {
                                        model.resetError()
                                    }
                                }
                            }
                        }
                    }
                }

                DialogButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(16.dp),
                    text = stringResource(R.string.okay)
                ) {
                    onDismissRequest.invoke()
                }
            }
        }
    )
}

@Composable
private fun NotDownloadedRow(onDownload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.translategemma_model_not_downloaded),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        StyledIconButton(imageVector = Icons.Default.Download, onClick = onDownload)
    }
}

@Composable
private fun DownloadingRow(progress: Float, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.translategemma_downloading, (progress * 100).toInt()),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        CircularProgressIndicator(
            modifier = Modifier
                .padding(8.dp)
                .requiredSize(27.dp),
            strokeWidth = 3.dp,
            progress = { progress }
        )
        Button(
            modifier = Modifier.padding(start = 8.dp),
            onClick = onCancel
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun ModelStatusRow(
    label: String,
    sizeBytes: Long,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (sizeBytes > 0) {
                Text(
                    text = "%.1f GB".format(sizeBytes.toFloat() / 1_000_000_000f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action()
    }
}
