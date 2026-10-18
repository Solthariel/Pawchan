package com.solthariel.pawchan.infrastructure

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.PawchanApplication
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Borrows the Application-owned preferences without creating a second DataStore. */
internal class AppPreferencesViewModel(
    val settings: DataStoreSettingsRepository,
) : ViewModel() {
    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AppPreferencesViewModel((context.applicationContext as PawchanApplication).preferences) }
            }
    }
}
