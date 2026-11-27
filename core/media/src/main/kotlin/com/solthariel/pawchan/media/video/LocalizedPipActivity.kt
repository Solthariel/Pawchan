package com.solthariel.pawchan.media.video

import android.content.Context
import androidx.activity.ComponentActivity

abstract class LocalizedPipActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(base.localizedForMedia())
}
