package com.solthariel.pawchan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.initializer
import com.solthariel.pawchan.navigation.IncomingLinksViewModel
import com.solthariel.pawchan.navigation.LocalIncomingLinks
import com.solthariel.pawchan.navigation.PawchanApp
import com.solthariel.pawchan.thread.PostSelectionHost

class MainActivity : ComponentActivity() {
    private val incomingLinks: IncomingLinksViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLinks.accept(intent.action, intent.dataString)
    }

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
        if (savedInstanceState == null) incomingLinks.accept(intent.action, intent.dataString)
        com.solthariel.pawchan.infrastructure
            .NotificationPermissionController(
                this,
                (application as PawchanApplication).preferences.notificationPrompt,
            ).requestOnce()
        enableEdgeToEdge()
        setContent {
            com.solthariel.pawchan.security.AppLockHost(this, appLock) {
                com.solthariel.pawchan.backup.BackupRecoveryHost((application as PawchanApplication).backupRecovery) {
                    CompositionLocalProvider(LocalIncomingLinks provides incomingLinks) {
                        PostSelectionHost { PawchanApp() }
                    }
                }
            }
        }
    }
}
