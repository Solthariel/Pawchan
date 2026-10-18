package com.solthariel.pawchan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import com.solthariel.pawchan.navigation.PawchanApp
import com.solthariel.pawchan.thread.PostSelectionHost

class MainActivity : ComponentActivity() {
    private val appLock: com.solthariel.pawchan.security.AppLockViewModel by viewModels {
        androidx.lifecycle.viewmodel.viewModelFactory {
            initializer {
                com.solthariel.pawchan.security
                    .AppLockViewModel(applicationContext)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.foreground()
    }

    override fun onStop() {
        if (!isChangingConfigurations) appLock.background()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.solthariel.pawchan.infrastructure
            .NotificationPermissionController(
                this,
                (application as PawchanApplication).preferences.notificationPrompt,
            ).requestOnce()
        enableEdgeToEdge()
        setContent {
            com.solthariel.pawchan.security.AppLockHost(this, appLock) {
                com.solthariel.pawchan.backup.BackupRecoveryHost((application as PawchanApplication).backupRecovery) {
                    PostSelectionHost { PawchanApp() }
                }
            }
        }
    }
}
