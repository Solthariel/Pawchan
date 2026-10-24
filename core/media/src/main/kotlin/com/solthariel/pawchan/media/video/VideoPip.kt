package com.solthariel.pawchan.media.video

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Rational
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.media.MediaAttachmentUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Что передаёт просмотрщик окну PiP: саму играющую сессию (без переоткрытия и перемотки).
 * [onShown] закрывает просмотрщик, когда окно PiP уже на экране.
 */
internal class VideoPipRequest(
    val attachment: MediaAttachmentUi,
    val session: VideoPlaybackSession,
    val dimensions: MediaDimensions?,
    val onShown: () -> Unit,
    val access: VideoPrivacyAccess? = null,
)

/** Где продолжить файл после PiP: позиция, игрался ли он и скорость. */
internal data class VideoPipResume(
    val position: Long,
    val playing: Boolean,
    val speed: VideoSpeed,
)

/** Развёрнутое окно PiP: просмотрщик снова открывается на этом файле. */
internal data class VideoPipReturn(
    val attachment: MediaAttachmentUi,
    val atMillis: Long = SystemClock.elapsedRealtime(),
) {
    val fresh: Boolean
        get() = SystemClock.elapsedRealtime() - atMillis < RETURN_EXPIRY_MS
}

/** Передача между просмотрщиком и окном PiP внутри одного процесса. */
internal object VideoPipHandoff {
    @Volatile var pending: VideoPipRequest? = null

    /** Позиция, на которой закрыли PiP: просмотрщик того же файла продолжит с неё. */
    @Volatile private var resume: Pair<String, VideoPipResume>? = null

    val returning = MutableStateFlow<VideoPipReturn?>(null)

    private val main = Handler(Looper.getMainLooper())
    private var parked: VideoPlaybackSession? = null
    private val expire =
        Runnable {
            parked?.let { player ->
                remember(player.url, VideoPipResume(player.status.value.position, false, player.sound.value.speed))
                player.close()
            }
            parked = null
        }

    /** Развёрнутое окно PiP оставляет сессию просмотрщику; не подхваченная вовремя закрывается. */
    fun park(session: VideoPlaybackSession) {
        main.removeCallbacks(expire)
        parked?.takeIf { it !== session }?.close()
        parked = session
        main.postDelayed(expire, RETURN_EXPIRY_MS)
    }

    fun adopt(url: String): VideoPlaybackSession? {
        val session = parked?.takeIf { it.url == url } ?: return null
        main.removeCallbacks(expire)
        parked = null
        session.inPip = false
        return session
    }

    fun remember(
        url: String,
        state: VideoPipResume,
    ) {
        resume = url to state
    }

    fun takeResume(url: String): VideoPipResume? {
        val saved = resume?.takeIf { it.first == url } ?: return null
        resume = null
        return saved.second
    }
}

/**
 * Хост просмотрщика текущего экрана подхватывает возврат из PiP: открывает тот же файл
 * (из списка экрана, если он там есть, иначе один этот файл).
 */
@Composable
internal fun BindVideoPipReturn(
    items: List<MediaAttachmentUi>,
    onSelect: (MediaAttachmentUi) -> Unit,
) {
    val currentItems by rememberUpdatedState(items)
    val currentSelect by rememberUpdatedState(onSelect)
    val access = LocalVideoPrivacyAccess.current
    LaunchedEffect(access) {
        combine(VideoPipHandoff.returning, access?.changes ?: flowOf(true)) { request, allowed -> request to allowed }
            .collect { (request, allowed) ->
                if (!allowed || access?.isAllowed() == false || request == null) return@collect
                if (VideoPipHandoff.returning.compareAndSet(request, null) && request.fresh) {
                    val attachment = request.attachment
                    currentSelect(currentItems.firstOrNull { it.originalUrl == attachment.originalUrl } ?: attachment)
                }
            }
    }
}

internal fun Context.canUsePip(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

internal fun Context.launchVideoPip(request: VideoPipRequest) {
    VideoPipHandoff.pending = request
    startActivity(
        Intent(this, VideoPipActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
    )
}

/** Android принимает пропорции окна PiP только в диапазоне 1:2.39 … 2.39:1. */
internal fun MediaDimensions?.pipAspect(): Rational {
    val width = this?.width ?: DEFAULT_WIDTH
    val height = this?.height ?: DEFAULT_HEIGHT
    val ratio = (width.toFloat() / height).coerceIn(1f / MAX_ASPECT, MAX_ASPECT)
    return Rational((ratio * ASPECT_SCALE).toInt(), ASPECT_SCALE)
}

private const val DEFAULT_WIDTH = 16
private const val DEFAULT_HEIGHT = 9
private const val MAX_ASPECT = 2.35f
private const val ASPECT_SCALE = 1000
private const val RETURN_EXPIRY_MS = 30_000L

/** The permission is checked both while binding UI and again when the user clicks. */
@Composable
internal fun rememberVideoPip(
    video: MediaAttachmentUi,
    player: VideoPlaybackSession,
    onClose: () -> Unit,
): (() -> Unit)? {
    val context = LocalContext.current
    val access = LocalVideoPrivacyAccess.current
    val pipAllowed = LocalVideoPipAllowed.current
    val currentClose by rememberUpdatedState(onClose)
    return remember(video, player, context, pipAllowed, access) {
        if (pipAllowed && access?.isAllowed() != false && context.canUsePip()) {
            {
                if (access?.isAllowed() != false) {
                    player.inPip = true
                    context.launchVideoPip(VideoPipRequest(video, player, video.dimensions, { currentClose() }, access))
                }
            }
        } else {
            null
        }
    }
}
