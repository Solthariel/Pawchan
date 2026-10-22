# Границы модулей и владельцы

Обязательное продолжение [AGENTS.md](../../AGENTS.md), не исторический отчёт.

## Границы ответственности и модуляризация

- Сначала устанавливается ответственность, владелец состояния и направление
  зависимостей; затем граница закрепляется Gradle-модулем. Число модулей,
  файлов и соответствие структурным лимитам сами по себе не доказывают качество.
- Граф production-модулей ацикличен. Двусторонние связи между пакетами будущих
  независимых фич устраняются до их выноса.
- Фича не зависит от app или внутренних ViewModel, контроллеров и состояния
  соседней фичи. Межфичевые входы и результаты имеют узкие типизированные
  контракты и связываются в composition root. Глобальный event bus не используется
  как обход явных зависимостей.
- UI-события, навигационные callbacks и Compose-типы не помещаются в domain
  только ради устранения Gradle-зависимости. API-модуль не создаётся автоматически
  для каждого класса; у него должна быть реальная необходимость и потребитель.
- Сессия, изменяемое состояние и ресурс имеют явного владельца: создание,
  время жизни, scope, отмена, восстановление и освобождение описаны и проверяемы.
  Перенос кода не меняет эти гарантии неявно.
- Composition root собирает зависимости, но не становится хранилищем
  бизнес-правил, всех экранных состояний или универсальным посредником.
- Общий UI выделяется для нескольких реальных потребителей с общей
  ответственностью. Он не импортирует фичи и состояния конкретных экранов.
  Преобразования экранного состояния остаются у соответствующей фичи.
- `:core:ui` — presentation-only leaf: без project-зависимостей на app, фичи,
  domain/data или media и без HTTP/Room/DataStore/Coil/Media3. В нём только общий
  UI, ресурсы и его состояние взаимодействия. Экранные преобразования и
  app-host (навигационные inset, theme/media lifecycle) остаются у владельца.
  Геометрия, контроллеры и анимационная реализация закрыты; тесты внутренних
  деталей живут в самом модуле, не требуют публикации internals для app.
- Media не импортирует thread/composer и не принимает их UiState/ViewModel.
  Экран передаёт подготовленные media-модели и callbacks; порядок и принадлежность
  вложений задаёт caller. Галерея/раскладка принадлежат media, экранные post/thread
  операции — фиче. Меню media не использует визуальные компоненты из thread.
  Граница закреплена в `:core:media`: разрешены project-зависимости только на
  domain, core UI, data/network и video-ffmpeg. App, storage/workspace и фичи
  недоступны; HTTP/cache/native внутри capability не объявляются чистым domain.
- App предоставляет media сохранение через `MediaSaveQueue`, callbacks меню,
  `MediaViewerResources` и scoped `MediaPresentation`. Media не закрывает взятые
  у app loaders/HTTP client и не извлекает workspace через service locator.
  AppNetworkStack владеет shared resources; viewer — только своей UI-сессией.
  FileProvider, WorkManager и избранное остаются в app-host. Toolbar drawing
  находится в core UI, а unread/workspace-преобразование — в app-адаптере.
- Видео-сессия, переданная в PiP, принадлежит окну PiP (`inPip`); просмотрщик
  её не ставит на паузу, не закрывает и не перехватывает поверхность. Возврат —
  через `VideoPipHandoff.park/adopt`, непринятая сессия закрывается по таймауту.
- Архивач и сохранённые копии принадлежат app (`ArchiveController`,
  `ArhivachController`); thread видит только `ThreadArchiveHost`. Ссылки на
  Архивач — `WorkspaceState.marks`, а не состояние копии.
- Тесты media internals принадлежат core/media; app integration tests потребляют
  реальный публичный API. Public ради доступа из старого тестового пакета запрещён.
  Source policy и Gradle dependency guard не заменяют отдельную компиляцию API.
- `:feature:composer` не знает app/workspace/thread/catalog и не принимает их
  состояние. Публичные входы — экраны, модели/actions, ViewModel factories и
  создание draft coordinator. Конструкторы controller/ViewModel и операции
  persist/restore/session, captcha и attachment processing закрыты.
- Composer получает PostingRepository, draft repository и
  borrowed ImageLoader извне. Route ViewModelStore владеет сессией; app storage
  scope — записью черновиков, переживающей закрытие route. Уход владельца отменяет
  запросы, поздний результат не доставляется в чужой экран. App хранит own posts,
  связывает результат отправки с thread и сохраняет navigation/lifecycle wiring.
- Composer имеет project-зависимости только на domain, core UI/media и текущий
  data/network utility. Унаследованный PostingMediaBytes используется только
  внутри implementation; это не разрешение создавать concrete HTTP repositories
  в feature. Его окончательное размещение — отдельное решение ответственности,
  не перенос data/native и не новая общая модель сайта в этом батче.
- Тесты internals composer живут в feature/composer. Смешанные app integration
  тесты используют реальные public factories; shared test fixtures не образуют
  production-зависимость на app и не требуют публикации частных renderer-функций.
- Thread-сессия не получает WorkspaceController или workspace state. Долговечные
  эффекты observe/storeOwn передаются через ThreadWorkspaceEvents с domain payload;
  app-адаптер не создаёт новый scope и не меняет порядок сохранения.
- Preview ViewModel получает готовый ThreadRepository. Offline/cache/workspace
  wrapper создаётся host-слоем, не в ViewModel. UI-host и repository/events должны
  ссылаться на одного app-owned workspace владельца на время route-сессии.
- Foreground watch controller/poll/status/lifecycle принадлежат thread presentation,
  а Worker, scheduler, notifier, notification permission — app. ViewModel scope
  владеет foreground loop; он не запускает background scheduling через свой state.
- Thread presentation, foreground watch, card/preview/selection/export session и
  их ресурсы принадлежат `:feature:thread`. Production project dependencies — только
  `:domain`, `:core:ui`, `:core:media`; feature не зависит от app или других фич.
  ThreadSession — ViewModelStore-owned public entry, не второй state owner/обёртка
  над скрытой ViewModel. App использует public factory и ThreadFeatureScreen;
  UiState/ViewModel/tools/controllers/renderers остаются internal.
- App отвечает за route/store keys, composer, permissions, platform adapters,
  Workers и composition root. Lifecycle foreground watcher и сбор screen state
  принадлежат thread screen; app не подписывается на приватное состояние фичи.
  Узкие public host slots/DTO не публикуются дальше без реального потребителя.
- Feature unit/instrumented tests лежат в своём модуле и получают friend access
  только к нему. App-интеграции используют реальные public factories/ports;
  app не получает friend access к feature ради сохранения старого теста.
  Перенос теста не разрешает терять assertions или заменять production lifecycle
  тестовым callback. Source policy покрывает весь production нового модуля.
- Thread screen получает borrowed media-ресурсы и app-host slots явно через
  ThreadScreenHost. Slots принимают адреса, immutable rendering input и callbacks,
  не ThreadToolsState/ThreadScreenMemory. Reading session не создаёт scope или
  дублирующее workspace-хранилище: UI владеет сбором позиции, app — памятью и записью.
  Immediate visible update, distinct и debounce 300 мс сохраняются; уход владельца
  отменяет pending save, подмена владельца не оставляет старый collector.
- Post-menu storage наблюдается только в открытом меню; весь route не подписывается
  на workspace ради одного признака favorite. Thread использует полный PostAddress;
  app не обновляет own storage после отказа preferences. Сессия и menu semantics
  preview принадлежат фиче; prepared repository, pending reply, navigation и
  сохранение поста — app-host. Preview window получает thumbnail loader явно
  и не извлекает LocalWorkspace; порт host использует domain payloads, не UiState.
- Общие платформенные возможности могут зависеть от HTTP, Bitmap и native
  библиотек; они не зависят от фич. UI потребляет узкий API, а не конкретные
  data-реализации и детали транспорта. Текущие нарушения исправляются явно.
- Запрещены common/core/utils без определённой ответственности, несвязанные
  контейнеры зависимостей и forwarding-обёртки без собственной роли.
- Реализация модуля закрыта по умолчанию. Публичны только необходимые входы,
  результаты и контракты. Массовая замена internal на public ради сборки запрещена.
- Новый feature-модуль имеет связную ответственность, production-код,
  потребителя, определённый lifecycle, небольшой API и тестируемую границу.
- Перед переносом перечисляются входящие/исходящие зависимости, ресурсы,
  тесты, R/BuildConfig, платформенные адаптеры и сохраняемые контракты.
  Source policy, lint и compiler options обновляются для новых путей и модулей
  без потери покрытия, исключений или ослабления требований.
- Проверка полноты feature dependencies использует classpath, разрешённый из
  собственных api/implementation и api зависимых проектов, не полный app classpath.
  Учитываются и imports, и fully-qualified вызовы. Общий набор third-party jars
  может подтвердить типизацию, но не независимость зависимостей новой фичи.
  Обязательна реальная debug/release compilation модуля при доступном Android SDK.
- Ресурсные владельцы проверяются после переноса production и тестов. Ссылки R
  сопоставляются по полному имени символа, не по префиксу: thread_closed_short
  не использует thread_closed. Старые app-копии ресурса не сохраняются ради уже
  перенесённого теста; тест получает R своего модуля. Lint не отключается.
- Domain/data сохраняются по умолчанию, но не объявляются неприкосновенными.
  Необходимое изменение ответственности или контракта обосновывается и тестируется;
  оно не разрешает неявно менять Room, drafts, URI и имена entrypoint-классов.
- Архитектурный батч описывает устранённую связь, нового владельца,
  разрешённые/удалённые зависимости, сохранённое поведение и проверки.
  «Стало чище», «стало модульнее» и «идеальная архитектура» недостаточны.

- Thread toolbar не импортирует catalog controls. Refresh animation принадлежит
  core UI и не знает workspace; unread projection остаётся app-owned composable
  callback и наблюдается в toolbar-кнопках, не в root/списке. Нельзя переносить
  подписку на весь route только ради одного navigation indicator.
- Сайтоспецифичный URL/protocol не находится в thread renderer. Scoped PostLinkBindings
  предоставляет resolver, borrowed save queue и navigation callback; он не отдаёт
  workspace/repositories и не создаёт state owner/service locator. App регистрирует
  только реализованные ForumWebLinks; unknown SiteId не получает ссылку 2ch.
  Copy/share без поддержанного URL не выдаёт успех и не создаёт фиктивный адрес.
  Capability в меню вычисляется явно; отсутствие реализации не означает поддержку.

- Экспорт треда разделяет session и platform. Feature владеет выбором постов,
  захваченным стилем, CreateDocument launcher, единственной активной операцией,
  сообщениями результата и отменой screen scope. App предоставляет ThreadExportTarget
  для выбранного URI и владеет ContentResolver stream/HTML/PNG rendering.
- Export adapter получает только ThreadExportDocument/ThreadExportPost/ThreadShotStyle,
  не ThreadUiState/ThreadPostUi/ThreadToolsState/ThreadScreenMemory или workspace.
  Подготовка snapshot выполняется вне композиции на Default, I/O — на IO.
  Borrowed ImageLoader не закрывается адаптером; stream/bitmap закрываются локально.
  URI, MIME, filenames, HTML escaping и PNG layout не меняются при переносе.
- Clipboard/Intent для post menus реализует app-owned PostMenuTransfer. Thread
  потребляет переданный scoped transfer, не создаёт Android adapter и не извлекает
  Context ради этих действий. Отсутствие binding не создаёт фиктивный service locator.

- Saved-post consumer получает opaque SavedPostPresentation, resources и callbacks,
  не ThreadUiState/ThreadPostUi/PostPreviewTarget/Bindings или renderer composables.
  Workspace владеет membership/unread; thread — card/preview/media interaction.
  Session key — полный PostAddress, не голый numeric id. Favorite slot получает
  только close callback, не mutable feature state. Подготовка остаётся вне композиции.
- Gallery session не уничтожается открытием viewer: window может скрываться,
  filter/selection/list anchor живут у caller. Back закрывает верхний слой сначала.
  Direct post viewer не создаёт фиктивную gallery. Borrowed resources не закрываются.
- Icon-only toggle имеет48dp target, понятный tooltip/accessibility и различимое
  active состояние; click переключает независимо, без лишнего toast. Скрытие
  selected captcha preview не удаляет protocol/state/token. Scrollbar navigation
  mode выбирает экран явно; новый default для всех списков не вводится незаметно.

- Catalog не импортирует thread и не обходит границу fully-qualified references.
  Shared subject/stats helpers принадлежат core UI только при нескольких реальных
  callers; компонент с одним catalog caller остаётся catalog, не уезжает в common.
- Выбор группы вложений в каталоге и общая галерея треда — разные сценарии/owners.
  Перед исправлением установить точный экран/вход/возврат. Viewer скрывает chooser
  window, не уничтожая choices; явный dismiss очищает группу. Регрессия проверяет
  реальный viewer+Back, не тестовую кнопку вместо production close path.
- Тонкий gallery scrollbar рисуется2dp без изменения сетки; input hit area шире
  и consumes gesture без открытия/выделения tile. При отсутствии overflow нужно
  убрать input overlay тоже, не только прозрачный рисунок.
- Visual OP/sage glyphs не удаляют текстовые metadata из copy/export/search;
  имена не чистятся substring replace. Accepted captcha steps могут заменять
  idle label, но не ошибки/loading/sending/solved status или protocol/token.
