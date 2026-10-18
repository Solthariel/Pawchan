package com.solthariel.pawchan.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

internal class UpdateViewModel(
    private val client: UpdateClient,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = mutableState.asStateFlow()

    fun check(configuration: UpdateConfiguration) {
        if (configuration.manifestUrl.isBlank()) {
            mutableState.value = UpdateUiState.Error(UpdateError.NOT_CONFIGURED)
            return
        }
        viewModelScope.launch {
            mutableState.value = UpdateUiState.Checking
            mutableState.value =
                runCatching { client.check() }
                    .fold(
                        onSuccess = { result ->
                            if (result == null) {
                                UpdateUiState.UpToDate
                            } else {
                                UpdateUiState.Available(result.first, result.second)
                            }
                        },
                        onFailure = ::failureState,
                    )
        }
    }

    fun download(release: UpdatePackage) {
        viewModelScope.launch {
            mutableState.value = UpdateUiState.Downloading(release, 0)
            mutableState.value =
                runCatching {
                    client.download(release) { progress ->
                        mutableState.value = UpdateUiState.Downloading(release, progress)
                    }
                }.fold(
                    onSuccess = { file -> UpdateUiState.Ready(release, file.absolutePath) },
                    onFailure = ::failureState,
                )
        }
    }

    fun install(context: Context) {
        val ready = mutableState.value as? UpdateUiState.Ready ?: return
        val launched = UpdateInstaller.launch(context, File(ready.apkPath))
        if (!launched) {
            mutableState.value = ready.copy(permissionRequired = true)
        }
    }

    override fun onCleared() {
        client.close()
    }

    private fun failureState(error: Throwable): UpdateUiState.Error {
        if (error is CancellationException) throw error
        return UpdateUiState.Error(UpdateError.GENERAL)
    }

    companion object {
        fun factory(
            context: Context,
            configuration: UpdateConfiguration,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    UpdateViewModel(
                        UpdateClient(context, configuration),
                    )
                }
            }
    }
}
