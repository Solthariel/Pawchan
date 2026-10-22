package com.solthariel.pawchan.archive

import com.solthariel.pawchan.domain.archive.ArchivePhase
import com.solthariel.pawchan.domain.archive.ArchiveState
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import com.solthariel.pawchan.workspace.WorkspaceController
import com.solthariel.pawchan.workspace.readingPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream

/** App lifecycle owns operations. Durable snapshots are committed before external publication/downloads. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class ArchiveController(
    private val scope: CoroutineScope,
    private val workspace: WorkspaceController,
    val services: ArchiveServices,
    private val enqueueMedia: () -> Unit,
) {
    private val mutex = Mutex()
    val arhivach = ArhivachController(scope, workspace, services.external)
    private val mutableBusy = MutableStateFlow(false)
    private val mutableNotice = MutableStateFlow<String?>(null)
    val busy = mutableBusy.asStateFlow()
    val notice = mutableNotice.asStateFlow()
    private val mutableSaved = MutableStateFlow<ThreadAddress?>(null)
    val saved = mutableSaved.asStateFlow()

    fun consumeSaved() {
        mutableSaved.value = null
    }

    val display =
        workspace.state
            .mapLatest { state ->
                withContext(Dispatchers.Default) { state.copies.map(::prepareArchiveCopy) }
            }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())
    val mediaFiles =
        workspace.state
            .mapLatest { state ->
                withContext(Dispatchers.Default) {
                    state.copies.flatMap { it.archive.files }.associate { file ->
                        file.url to
                            services.media
                                .resolve(file)
                                ?.toURI()
                                ?.toString()
                                .orEmpty()
                    }
                }
            }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyMap())

    /** Итог операции показан пользователю: следующее такое же сообщение снова всплывёт. */
    fun consumeNotice() {
        mutableNotice.value = null
    }

    /** Общая настройка «Обновлять сохранённые копии»; выключена — фоновое обновление пропускается. */
    @Volatile var updatesEnabled: Boolean = true

    fun save(
        address: ThreadAddress,
        media: Boolean,
    ) = operation {
        val snapshot =
            workspace.reading.snapshot(address) ?: workspace.latestSnapshot?.takeIf { it.address == address }
                ?: workspace.state.value.copies
                    .firstOrNull { it.snapshot.address == address }
                    ?.snapshot
                ?: throw IOException("Нет загруженного треда")
        val reading = workspace.readingPosition(address)
        val state =
            workspace.repository.update { state ->
                val previous = state.copies.firstOrNull { it.snapshot.address == address }
                val copy =
                    ThreadCopy(
                        previous?.let {
                            com.solthariel.pawchan.domain.archive
                                .mergeArchivedThread(it.snapshot, snapshot)
                        } ?: snapshot,
                        System.currentTimeMillis(),
                        (previous?.archive ?: ArchiveState()).copy(
                            includeMedia = media,
                            media =
                                if (media) {
                                    previous?.archive?.media ?: com.solthariel.pawchan.domain.archive
                                        .ArchiveMediaState()
                                } else {
                                    com.solthariel.pawchan.domain.archive
                                        .ArchiveMediaState()
                                },
                        ),
                        reading?.let {
                            com.solthariel.pawchan.domain.workspace
                                .OpenThread(address, snapshot.subject, it.first, it.second)
                        }
                            ?: state.tabs.firstOrNull { it.address == address } ?: previous?.position,
                    )
                state.copy(copies = state.copies.filterNot { it.snapshot.address == address } + copy)
            }
        val copy = requireNotNull(state.copies.firstOrNull { it.snapshot.address == address })
        mutableNotice.value = if (copy.archive.includeMedia) "Копия сохранена, файлы загружаются" else "Копия сохранена"
        enqueueMedia()
        mutableSaved.value = address
    }

    fun update(
        address: ThreadAddress,
        externalOnly: Boolean = false,
    ) = operation {
        val copy =
            workspace.repository
                .load()
                .copies
                .firstOrNull { it.snapshot.address == address } ?: return@operation
        val success = services.synchronizer.update(copy, externalOnly)
        val updated =
            workspace.repository
                .load()
                .copies
                .firstOrNull { it.snapshot.address == address }
        if (updated?.archive?.includeMedia == true) enqueueMedia()
        mutableNotice.value = if (success) "Копия обновлена" else updated?.archive?.message
    }

    /** «Обновить» во вкладке копий: все живые треды дополняются новыми постами. */
    fun updateAll() =
        operation {
            val copies =
                workspace.repository
                    .load()
                    .copies
                    .filter { it.archive.phase != ArchivePhase.SOURCE_UNAVAILABLE }
            val failed = copies.count { !services.synchronizer.update(it, false) }
            if (copies.any { it.archive.includeMedia }) enqueueMedia()
            mutableNotice.value = if (failed == 0) "Копии обновлены" else "Не удалось обновить копий: $failed"
        }

    fun remove(address: ThreadAddress) =
        operation {
            workspace.repository.update { state ->
                state.copy(
                    copies =
                        state.copies.filterNot {
                            it.snapshot.address ==
                                address
                        },
                )
            }
            // Shared URL-addressed media can belong to other copies; collect only unreferenced files after mutation.
            enqueueMedia()
            mutableNotice.value = "Локальная копия удалена"
        }

    fun export(
        address: ThreadAddress,
        onSaved: () -> Unit = {},
        output: () -> OutputStream?,
    ) = operation {
        val copy =
            workspace.repository
                .load()
                .copies
                .firstOrNull { it.snapshot.address == address } ?: return@operation
        withContext(Dispatchers.IO) {
            val stream = output() ?: throw IOException("Не удалось открыть файл для экспорта")
            services.zip.write(copy, stream)
        }
        onSaved()
        mutableNotice.value = "ZIP сохранён; отсутствующие медиа остаются внешними ссылками"
    }

    suspend fun writeZip(
        address: ThreadAddress,
        output: OutputStream,
    ) = mutex.withLock {
        val copy =
            workspace.repository
                .load()
                .copies
                .firstOrNull { it.snapshot.address == address }
                ?: throw IOException("Сохранённая копия не найдена")
        withContext(Dispatchers.IO) { services.zip.write(copy, output) }
    }

    suspend fun synchronize() =
        mutex.withLock {
            if (!updatesEnabled) return@withLock
            try {
                services.synchronizer.run()
                if (workspace.state.value.copies
                        .any { it.archive.includeMedia }
                ) {
                    enqueueMedia()
                }
            } catch (failure: IOException) {
                mutableNotice.value = failure.message
            } finally {
                workspace.reload()
            }
        }

    private fun operation(block: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        scope.launch {
            try {
                mutex.withLock { block() }
            } catch (
                failure: IOException,
            ) {
                mutableNotice.value = failure.message
            } catch (_: SecurityException) {
                mutableNotice.value = "Доступ к файлу потерян. Выберите другое место сохранения."
            } finally {
                mutableBusy.value = false
                workspace.reload()
            }
        }
    }
}
