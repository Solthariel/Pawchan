package com.solthariel.pawchan

import android.app.Application
import androidx.work.Configuration
import com.solthariel.pawchan.backup.BackupRecovery
import com.solthariel.pawchan.data.preferences.DataStoreSettingsRepository
import com.solthariel.pawchan.watch.SettingsWorkerFactory
import com.solthariel.pawchan.watch.ThreadWatchNotifier
import com.solthariel.pawchan.watch.ThreadWatchScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PawchanApplication :
    Application(),
    Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    internal val backupRecovery by lazy {
        BackupRecovery(this, preferences, applicationScope) {
            ThreadWatchNotifier.createChannel(this)
            ThreadWatchScheduler.schedule(this)
        }
    }
    val preferences by lazy {
        DataStoreSettingsRepository
            .create(this)
    }
    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(
                    SettingsWorkerFactory(preferences),
                ).build()

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }

    override fun onCreate() {
        super.onCreate()
        backupRecovery.start()
    }
}
