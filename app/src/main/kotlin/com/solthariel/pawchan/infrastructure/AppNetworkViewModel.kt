package com.solthariel.pawchan.infrastructure

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

internal class AppNetworkViewModel private constructor(
    val stack: AppNetworkStack,
) : ViewModel() {
    override fun onCleared() {
        stack.close()
    }

    companion object {
        fun factory(
            context: Context,
            settings: com.solthariel.pawchan.domain.settings.AppSettingsRepository,
            cacheMb: Int = com.solthariel.pawchan.domain.settings.StoragePreferences.DEFAULT_MB,
            mirrorHost: String = "2ch.su",
            githubDns: com.solthariel.pawchan.domain.settings.ServiceDns =
                com.solthariel.pawchan.domain.settings
                    .ServiceDns(),
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AppNetworkViewModel(
                        AppNetworkStack.create(context.applicationContext, settings, cacheMb, mirrorHost, githubDns),
                    )
                }
            }
    }
}
