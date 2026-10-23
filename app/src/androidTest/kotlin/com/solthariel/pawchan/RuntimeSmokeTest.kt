package com.solthariel.pawchan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import com.solthariel.pawchan.catalog.CatalogActions
import com.solthariel.pawchan.catalog.CatalogContentActions
import com.solthariel.pawchan.catalog.CatalogNavigationActions
import com.solthariel.pawchan.catalog.CatalogPresentation
import com.solthariel.pawchan.catalog.CatalogScreen
import com.solthariel.pawchan.catalog.CatalogThreadContentUi
import com.solthariel.pawchan.catalog.CatalogThreadFlagsUi
import com.solthariel.pawchan.catalog.CatalogThreadStatisticsUi
import com.solthariel.pawchan.catalog.CatalogThreadTextUi
import com.solthariel.pawchan.catalog.CatalogThreadUi
import com.solthariel.pawchan.catalog.CatalogUiState
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFieldActions
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.NewThreadActions
import com.solthariel.pawchan.composer.NewThreadCaptchaActions
import com.solthariel.pawchan.composer.NewThreadScreen
import com.solthariel.pawchan.composer.NewThreadUiState
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.infrastructure.MediaLoaders
import com.solthariel.pawchan.infrastructure.media.MediaDownloadQueue
import com.solthariel.pawchan.infrastructure.thread.rememberAppThreadScreenHost
import com.solthariel.pawchan.library.HistoryScreen
import com.solthariel.pawchan.library.LibraryEntryUi
import com.solthariel.pawchan.library.LibraryUiState
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.navigation.threadAddress
import com.solthariel.pawchan.ui.PawchanTheme
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuntimeSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var imageLoader: ImageLoader
    private lateinit var thumbnailLoader: ThumbnailLoader
    private lateinit var thumbnailHttpClient: OkHttpClient

    @Before
    fun createImageLoader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        imageLoader = ImageLoader.Builder(context).build()
        thumbnailHttpClient = OkHttpClient()
        thumbnailLoader = ThumbnailLoader(context, thumbnailHttpClient)
    }

    @After
    fun closeImageLoader() {
        compose.runOnIdle { thumbnailLoader.close() }
        thumbnailHttpClient.dispatcher.executorService.shutdown()
        thumbnailHttpClient.connectionPool.evictAll()
        imageLoader.shutdown()
        java.io
            .File(
                InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "catalog-return-fixture.png",
            ).delete()
    }

    @Test
    fun catalogAcceptsSaveableThreadKeys() {
        val thread = catalogThread("b", 42L)
        compose.setContent {
            PawchanTheme(preference = ThemePreference.SYSTEM) {
                CatalogScreen(
                    presentation =
                        CatalogPresentation(
                            defaultBoardAddress(),
                            CatalogUiState.Content("Бред", 136, listOf(thread)),
                            imageLoader,
                            thumbnailLoader,
                        ),
                    actions = catalogActions(),
                    bottomActions = bottomActions(),
                    menu = testBoardMenu(),
                )
            }
        }

        compose.onNodeWithText("Тестовый тред").assertIsDisplayed()
        compose.onNodeWithText("136 сообщений в час").assertIsDisplayed()
        compose.onNodeWithContentDescription("Поиск по тредам").performClick()
        compose.onNodeWithText("Поиск…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Закрыть поиск").performClick()
        compose.onNodeWithText("/b/ — Бред").assertIsDisplayed()
        compose.onNodeWithContentDescription("закреплён").assertIsDisplayed()
        compose.onNodeWithContentDescription("бесконечный").assertIsDisplayed()
    }

    @Test
    fun libraryAcceptsSameThreadNumberFromDifferentBoards() {
        val entries =
            listOf(
                libraryEntry("b", 42L),
                libraryEntry("po", 42L),
            )
        compose.setContent {
            PawchanTheme(preference = ThemePreference.SYSTEM) {
                HistoryScreen(
                    state = LibraryUiState.Content(entries),
                    onBack = {},
                    onThreadClick = {},
                    bottomActions = bottomActions(),
                )
            }
        }

        compose.onNodeWithText("Тред /b/").assertIsDisplayed()
        compose.onNodeWithText("Тред /po/").assertIsDisplayed()
    }

    @Test
    fun threadRendersRichContentWithoutMedia() {
        val owner = androidx.lifecycle.ViewModelStore()
        val session =
            androidx.lifecycle.ViewModelProvider(
                owner,
                com.solthariel.pawchan.thread.ThreadSession.factory(
                    requireNotNull(threadAddress("dvach", "b", 42L)),
                    SmokeThreadRepository(),
                    smokeThreadPersistence(),
                ),
            )[com.solthariel.pawchan.thread.ThreadSession::class.java]
        compose.setContent {
            androidx.compose.runtime.DisposableEffect(session) { onDispose { owner.clear() } }
            PawchanTheme(preference = ThemePreference.SYSTEM) {
                com.solthariel.pawchan.thread.ThreadFeatureScreen(
                    session = session,
                    host =
                        rememberAppThreadScreenHost(
                            MediaLoaders(
                                imageLoader,
                                thumbnailLoader,
                                MediaDownloadQueue(
                                    InstrumentationRegistry.getInstrumentation().targetContext,
                                    (
                                        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                                            as PawchanApplication
                                    ).preferences,
                                ),
                                OkHttpClient(),
                            ),
                            null,
                        ),
                    actions =
                        com.solthariel.pawchan.thread
                            .ThreadFeatureActions({}, {}, {}),
                )
            }
        }
        compose.waitUntil(timeoutMillis = 5000L) {
            compose.onAllNodesWithText("Ответ").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ответ").assertIsDisplayed()
        compose.onNodeWithText("№43").assertIsDisplayed()
        compose.onNodeWithText("Автообновление выключено").assertIsDisplayed()
        compose.onNodeWithText("Следить").assertIsDisplayed()
    }

    @Test
    fun newThreadRendersPostingContract() {
        compose.setContent {
            PawchanTheme(preference = ThemePreference.SYSTEM) {
                NewThreadScreen(
                    board = requireNotNull(BoardId.parse("b")),
                    state =
                        NewThreadUiState(
                            draft =
                                ComposerDraftState(
                                    comment = "Первый пост",
                                    attachments =
                                        listOf(
                                            ComposerAttachment(
                                                ReplyAttachment(
                                                    fileName = "cat.png",
                                                    mimeType = "image/png",
                                                    content = byteArrayOf(1),
                                                ),
                                            ),
                                        ),
                                ),
                            rules =
                                com.solthariel.pawchan.domain.posting
                                    .PostingRules(),
                            captchaState = ComposerCaptchaState(satisfied = true),
                            progress = ComposerProgressState(),
                        ),
                    imageLoader = imageLoader,
                    actions =
                        NewThreadActions(
                            onBack = {},
                            fields = ComposerFieldActions({}, {}),
                            captcha = NewThreadCaptchaActions({}, {}),
                            onSubmit = {},
                        ),
                )
            }
        }

        compose.onNodeWithText("Новый тред в /b/").assertIsDisplayed()
        compose.onNodeWithText("Создать тред").assertIsDisplayed()
    }

    @Test fun catalogAttachmentChooserReturnsAfterViewerToolbarBack() {
        showCatalogViewerFixture()
        openSecondAttachment()
        compose.onAllNodesWithContentDescription("Назад").onLast().performClick()
        compose.onNodeWithContentDescription("four.png").assertIsDisplayed()
    }

    @Test fun catalogAttachmentChooserReturnsAfterSystemBack() {
        showCatalogViewerFixture()
        openSecondAttachment()
        androidx.test.espresso.Espresso
            .pressBack()
        compose.onNodeWithContentDescription("four.png").assertIsDisplayed()
    }

    private fun openSecondAttachment() {
        compose.onNodeWithContentDescription("one.png").performClick()
        compose.onNodeWithContentDescription("four.png").assertIsDisplayed()
        compose.onNodeWithContentDescription("two.png").performClick()
        // Просмотрщик открыт в отдельном окне; выбор вложений остаётся под ним.
        compose.onNodeWithContentDescription("four.png").assertExists()
    }

    private fun showCatalogViewerFixture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = java.io.File(context.cacheDir, "catalog-return-fixture.png")
        val bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
        try {
            image.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
        val imageUrl =
            android.net.Uri
                .fromFile(image)
                .toString()
        val attachments =
            listOf("one", "two", "three", "four").map { name ->
                com.solthariel.pawchan.media.MediaAttachmentUi(
                    com.solthariel.pawchan.media
                        .MediaAttachmentSource(name, "$name.png", imageUrl, imageUrl),
                    null,
                    com.solthariel.pawchan.media.AttachmentViewer.IMAGE,
                    com.solthariel.pawchan.domain.model.MediaKind.IMAGE,
                )
            }
        val thread = catalogThread("b", 42).copy(attachments = attachments)
        compose.setContent {
            PawchanTheme(preference = ThemePreference.SYSTEM) {
                var opened by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf<com.solthariel.pawchan.media.MediaAttachmentUi?>(null)
                }
                val close = { opened = null }
                androidx.activity.compose.BackHandler(opened != null, close)
                val save =
                    com.solthariel.pawchan.media
                        .rememberMediaSaveActions(
                            com.solthariel.pawchan.media
                                .MediaSaveQueue { _, _ -> false },
                        )
                com.solthariel.pawchan.media.MediaContentHost(
                    com.solthariel.pawchan.media
                        .MediaViewerSelection(opened, attachments) { opened = it },
                    com.solthariel.pawchan.media
                        .MediaViewerResources(imageLoader, thumbnailHttpClient),
                    save,
                    null,
                    close,
                ) {
                    val actions = catalogActions()
                    CatalogScreen(
                        CatalogPresentation(
                            defaultBoardAddress(),
                            CatalogUiState.Content("Бред", 136, listOf(thread)),
                            imageLoader,
                            thumbnailLoader,
                        ),
                        actions.copy(content = actions.content.copy(onMediaClick = { opened = it })),
                        bottomActions(),
                        testBoardMenu(),
                    )
                }
            }
        }
    }

    private fun catalogThread(
        board: String,
        threadId: Long,
    ): CatalogThreadUi =
        CatalogThreadUi(
            address = requireNotNull(threadAddress("dvach", board, threadId)),
            content =
                CatalogThreadContentUi(
                    subject = "Тестовый тред",
                    text = CatalogThreadTextUi("Первый пост"),
                    author = "Аноним",
                    date = null,
                    sage = false,
                ),
            attachments = emptyList(),
            flags = CatalogThreadFlagsUi(pinned = true, closed = false, endless = true),
            statistics =
                CatalogThreadStatisticsUi(
                    replies = 1,
                    files = 0,
                    knownMediaBytes = 0L,
                    mediaBytesComplete = true,
                ),
        )

    private fun libraryEntry(
        board: String,
        threadId: Long,
    ): LibraryEntryUi =
        LibraryEntryUi(
            address = requireNotNull(threadAddress("dvach", board, threadId)),
            subject = "Тред /$board/",
            lastVisitedAtEpochMillis = 1L,
            favorite = true,
        )

    private fun catalogActions(): CatalogActions =
        CatalogActions(
            content = CatalogContentActions({}, {}, {}, {}, {}),
            navigation = CatalogNavigationActions({}, {}, {}, {}),
        )

    private fun bottomActions(): AppBottomActions =
        AppBottomActions(
            onBoardsClick = {},
            onCatalogClick = {},
            onLibraryClick = {},
            onSettingsClick = {},
        )
}
