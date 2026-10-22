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
        appLock.lifecycle.foreground()
    }

    override fun onStop() {
        if (!isChangingConfigurations) appLock.lifecycle.background()
        super.onStop()
    }

    private val screenOffReceiver =
        object : android.content.BroadcastReceiver() {
            override fun onReceive(
                context: android.content.Context,
                intent: Intent,
            ) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) appLock.lifecycle.screenOff()
            }
        }

    override fun onPostResume() {
        super.onPostResume()
        appLock.lifecycle.finishForeground()
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            android.content.IntentFilter(Intent.ACTION_SCREEN_OFF),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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
