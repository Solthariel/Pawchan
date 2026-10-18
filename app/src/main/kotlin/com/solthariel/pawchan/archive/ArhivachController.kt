package com.solthariel.pawchan.archive

import com.solthariel.pawchan.domain.archive.ArchiveSubmission
import com.solthariel.pawchan.domain.archive.ExternalThreadArchive
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.workspace.withArhivachLink
import com.solthariel.pawchan.workspace.WorkspaceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Архивач из приложения: доступность сайта (проверка не чаще раза в несколько минут), капча,
 * отправка треда и ссылки на отправленные копии (хранятся в рабочем пространстве).
 */
internal class ArhivachController(
    private val scope: CoroutineScope,
    private val workspace: WorkspaceController,
    private val external: ExternalThreadArchive,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutableAvailable = MutableStateFlow(true)
    private var checkedAt: Long? = null

    @Volatile private var checking = false
    val available: StateFlow<Boolean> = mutableAvailable.asStateFlow()
    val links: StateFlow<Map<ThreadAddress, String>> =
        workspace.state
            .map { it.marks.arhivach }
            .stateIn(scope, SharingStarted.Eagerly, workspace.state.value.marks.arhivach)

    /**
     * Удачная проверка живёт несколько минут; после неудачной — проверяем снова при каждом открытии меню
     * (включили VPN — пункты оживут сразу). Новая проверка не идёт параллельно с прошлой.
     */
    fun refreshAvailability(force: Boolean = false) {
        val time = now()
        val last = checkedAt
        val fresh = last != null && time - last < AVAILABILITY_TTL_MILLIS && mutableAvailable.value
        if (checking || (fresh && !force)) return
        checking = true
        checkedAt = time
        scope.launch {
            try {
                mutableAvailable.value = external.available()
            } finally {
                checking = false
            }
        }
    }

    suspend fun captcha(): DomainOutcome<ByteArray> = external.captcha()

    suspend fun submit(
        address: ThreadAddress,
        code: String,
    ): ArchiveSubmission {
        val result = external.add(address, code)
        if (result is ArchiveSubmission.Accepted) {
            mutableAvailable.value = true
            workspace.change { it.withArhivachLink(address, result.url) }
        }
        return result
    }

    suspend fun load(
        address: ThreadAddress,
        link: String,
    ): DomainOutcome<ThreadSnapshot> = external.load(address, link)
}

private const val AVAILABILITY_TTL_MILLIS = 5 * 60_000L
