package com.solthariel.pawchan.ffmpeg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.SystemClock;
import android.util.Pair;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VideoPlayer implements AutoCloseable {
	private static final long SEEK_STALL_REPORT_DELAY = 5000L;
	private static final long THUMBNAIL_MIN_POSITION = 250L;
	private static final long THUMBNAIL_MAX_POSITION = 2000L;
	private static final long THUMBNAIL_END_MARGIN = 250L;

	private static boolean loaded = false;
	private static HolderInterface holder;
	private static boolean playbackSpeedSupported = true;

	private static final boolean BUNDLED_ATEMPO = true;

	public static Pair<Boolean, String> loadLibraries(Context context) {
		synchronized (VideoPlayer.class) {
			if (loaded) return new Pair<>(true, null);
			try {
				holder = new Holder();
				loaded = true;
				return new Pair<>(true, null);
			} catch (LinkageError error) {
				return new Pair<>(false, "FFmpeg native libraries: " + error.getMessage());
			}
		}
	}

	private static String joinArray(String[] values) {
		return String.join(", ", values);
	}

	public static boolean isLoaded() {
		synchronized (VideoPlayer.class) {
			return loaded;
		}
	}

	static void startDiagnosticCapture() {
		synchronized (VideoPlayer.class) {
			if (holder != null) {
				try {
					if (VideoDiagnostics.isExtendedRecording()) holder.startExtendedDiagnostics();
					else holder.startDiagnostics();
				} catch (RuntimeException | LinkageError ignored) {
					try { holder.startDiagnostics(); } catch (RuntimeException | LinkageError unavailable) {}
				}
			}
		}
	}

	static String stopDiagnosticCapture() {
		synchronized (VideoPlayer.class) {
			if (holder != null) {
				try {
					return holder.stopDiagnostics();
				} catch (RuntimeException | LinkageError ignored) {
					return "native_diagnostics_unavailable=true\n";
				}
			}
			return null;
		}
	}

	static boolean sampleDiagnosticCapture() {
		synchronized (VideoPlayer.class) {
			if (holder == null) return true;
			try { holder.sampleDiagnostics(); return true; }
			catch (RuntimeException | LinkageError ignored) { return false; }
		}
	}

	static int finishDiagnosticCapture() {
		synchronized (VideoPlayer.class) {
			if (holder == null) return 0;
			try { return holder.finishDiagnostics(); }
			catch (RuntimeException | LinkageError ignored) { return -1; }
		}
	}

	static byte[] readDiagnosticChunk(int offset, int length) {
		synchronized (VideoPlayer.class) {
			return holder != null ? holder.readDiagnosticsChunk(offset, length) : null;
		}
	}

	static void releaseDiagnosticCapture() {
		synchronized (VideoPlayer.class) {
			if (holder != null) {
				try { holder.releaseDiagnostics(); } catch (RuntimeException | LinkageError ignored) {}
			}
		}
	}

	public interface Listener {
		void onComplete(VideoPlayer player);
		void onBusyStateChange(VideoPlayer player, boolean busy);
		void onDimensionChange(VideoPlayer player);
		default void onDurationChange(VideoPlayer player, long duration) {}
		/** A fresh view surface replaced the previous output: it stays empty until its first frame. */
		default void onVideoOutputReset(VideoPlayer player) {}
	}

	public interface RangeCallback {
		// Called from the initialization thread once download ranges can be applied.
		default void onReadReady() {}
		void requestPartFromPosition(long start);
	}

	private static class SessionData {
		public final long pointer;
		public final RangeCallback rangeCallback;

		public long request;
		public long size;
		public long total = -1;
		public long partStart;
		public long partEnd;

		public SessionData(long pointer, RangeCallback rangeCallback) {
			this.pointer = pointer;
			this.rangeCallback = rangeCallback;
		}
	}

	private boolean hardwareEnabled = true;
	private final java.util.concurrent.ExecutorService releaseExecutor =
			java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "FFmpegRelease"));

	public void interruptRead() {
		synchronized (this) {
			SessionData active = getInitOrSessionDataLocked();
			if (!consumed && active != null) holder.destroy(active.pointer, true);
		}
	}

	public void setHardwareEnabled(boolean enabled) {
		synchronized (this) {
			if (initData != null || sessionData != null) throw new IllegalStateException("Set decoder before init");
			hardwareEnabled = enabled;
		}
	}

	private volatile Listener listener;
	private final boolean seekAnyFrame;
	private final boolean audioEnabled;

	private SessionData sessionData;
	private SessionData initData;

	private volatile boolean consumed = false;
	private boolean playing = false;
	private int playbackSpeed = 1000;
	private long nextSurfaceGeneration;
	private long latestSurfaceGeneration;
	private long latestAppliedSurfaceGeneration;
	private long latestRenderedSurfaceGeneration;
	// A seek requested while no surface is attached must win over the position the native side
	// reports after recreating the decoder: that position predates the seek (e.g. a gallery page
	// returning to 0:00 would otherwise jump back to where it was left).
	private long seekRequestAtSurfaceLoss = -1L;
	private long lastSeekTarget;
	// Surface.release() invalidates the Java wrapper even if JNI still has a global reference.
	// Keep each wrapper alive until the video thread confirms that its generation was handled.
	private final ArrayList<SurfaceRequest> surfaceRequests = new ArrayList<>();
	// TextureView destroys its SurfaceTexture as soon as the listener returns true. During a
	// PiP hand-off MediaCodec can still own the matching native window while the replacement is
	// being installed. While playing, keep detached textures alive until the new Surface renders
	// an actual frame. A paused decoder can release them once the replacement is applied.
	private final ArrayList<DeferredSurfaceTexture> deferredSurfaceTextures = new ArrayList<>();
	// Only explicit transfers of this player may reuse a detached, already-rendered surface.
	// Deferred surfaces remain player-owned until the new TextureView actually attaches.
	private long transferableSurfaceGeneration;
	private long surfaceTransferStartedMs;

	private boolean lastSeeking = false;
	private volatile boolean lastBuffering = false;
	// Distinct from seek buffering and from the user's play/pause choice.
	private final NetworkBufferingState networkBuffering = new NetworkBufferingState();

	public VideoPlayer(Listener listener, boolean seekAnyFrame) {
		this(listener, seekAnyFrame, true);
	}

	private VideoPlayer(Listener listener, boolean seekAnyFrame, boolean audioEnabled) {
		this.listener = listener;
		this.seekAnyFrame = seekAnyFrame;
		this.audioEnabled = audioEnabled;
	}

	public void setListener(Listener listener) {
		synchronized (this) {
			if (consumed) return;
			this.listener = listener;
			handler.post(() -> {
				if (!consumed && this.listener == listener) notifyBusyState();
			});
		}
	}

	public static Bitmap createThumbnail(File file) throws IOException, InterruptedException {
		if (!isLoaded()) {
			return null;
		}
		VideoPlayer player = new VideoPlayer(null, true, false);
		SurfaceTexture surfaceTexture = null;
		Surface surface = null;
		try {
			player.init(file, null);
			Point dimensions = player.getDimensions();
			long duration = player.getDuration();
			long targetPosition = calculateThumbnailPosition(duration);
			surfaceTexture = new SurfaceTexture(false);
			surfaceTexture.setDefaultBufferSize(Math.max(dimensions.x, 1), Math.max(dimensions.y, 1));
			surface = new Surface(surfaceTexture);
			player.setSurface(surface, Math.max(dimensions.x, 1), Math.max(dimensions.y, 1));
			if (targetPosition > 0L) {
				player.setPosition(targetPosition);
			}
			player.setPlaying(true);
			Bitmap bitmap = waitForThumbnailFrame(player, targetPosition > 0L ? 2000L : 3000L);
			if (bitmap == null && targetPosition > 0L) {
				player.setPosition(0L);
				bitmap = waitForThumbnailFrame(player, 1500L);
			}
			return bitmap;
		} finally {
			player.setPlaying(false);
			player.destroy();
			if (surfaceTexture != null) {
				surfaceTexture.release();
			}
		}
	}

	private static Bitmap waitForThumbnailFrame(VideoPlayer player, long timeoutMs) throws InterruptedException {
		long timeout = System.currentTimeMillis() + timeoutMs;
		while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeout) {
			Bitmap bitmap = player.getCurrentFrame();
			if (bitmap != null) {
				return bitmap;
			}
			Thread.sleep(40L);
		}
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}
		return null;
	}

	private static long calculateThumbnailPosition(long duration) {
		if (duration <= 2L * THUMBNAIL_END_MARGIN) {
			return 0L;
		}
		long position = Math.max(THUMBNAIL_MIN_POSITION, duration / 10L);
		position = Math.min(position, THUMBNAIL_MAX_POSITION);
		return Math.min(position, duration - THUMBNAIL_END_MARGIN);
	}

	public void init(File file, RangeCallback rangeCallback) throws IOException {
		int priority = Process.getThreadPriority(Process.myTid());
		Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
		try {
			SessionData initData = null;
			synchronized (this) {
				if (sessionData == null && initData == null && !consumed) {
					long initPointer;
					try (ParcelFileDescriptor descriptor = ParcelFileDescriptor
							.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
						int fd = descriptor.detachFd();
						initPointer = holder.preInit(fd);
					}
					if (rangeCallback == null) {
						long length = file.length();
						holder.setRange(initPointer, 0, length, length);
					}
					initData = new SessionData(initPointer, rangeCallback);
					this.initData = initData;
					if (!audioEnabled) {
						holder.setAudioEnabled(initPointer, false);
					}
					holder.setHardwareAcceleration(initPointer,
							audioEnabled && hardwareEnabled);
				}
			}
			if (initData != null) {
				if (rangeCallback != null) rangeCallback.onReadReady();
				holder.init(initData.pointer, new NativeBridge(this), seekAnyFrame);
				synchronized (this) {
					this.initData = null;
					if (consumed) {
						holder.destroy(initData.pointer, false);
					} else {
						int errorCode = holder.getErrorCode(initData.pointer);
						if (errorCode != 0) {
							destroyInternal(initData);
							throw new InitializationException(errorCode);
						}
						sessionData = initData;
						applyPlaybackSpeedLocked();
						seekerThread.start();
					}
				}
			}
		} finally {
			Process.setThreadPriority(priority);
		}
	}

	public static class InitializationException extends IOException {
		private InitializationException(int errorCode) {
			super("Can't initialize player: CODE=" + errorCode);
		}
	}

	private SessionData getInitOrSessionDataLocked() {
		return sessionData != null ? sessionData : initData;
	}

	public void setDownloadRange(long size, long total) {
		synchronized (this) {
			SessionData sessionData = getInitOrSessionDataLocked();
			if (sessionData != null) {
				sessionData.size = size;
				sessionData.total = total;
				if (size > sessionData.request || sessionData.request <= 0) {
					if (sessionData.request > 0) {
						sessionData.request = 0;
						sessionData.rangeCallback.requestPartFromPosition(0);
					}
					setRangeLocked(0, size, total);
				}
			}
		}
	}

	public void setPartRange(long start, long end) {
		synchronized (this) {
			SessionData sessionData = getInitOrSessionDataLocked();
			if (sessionData != null && sessionData.rangeCallback != null &&
					sessionData.request >= start && sessionData.request <= end) {
				sessionData.partStart = start;
				sessionData.partEnd = end;
				// Shift request position for higher priority
				sessionData.request = end;
				setRangeLocked(start, end, sessionData.total);
			}
		}
	}

	private void requestRange(long start) {
		synchronized (this) {
			SessionData sessionData = getInitOrSessionDataLocked();
			if (!consumed && sessionData != null && sessionData.rangeCallback != null) {
				int awaitLength = 200000;
				boolean request = true;
				if (start > 0 && sessionData.request > 0 && (start == sessionData.request ||
						start >= sessionData.partStart && start <= sessionData.partEnd + awaitLength)) {
					// Use current part range request
					request = false;
				}
				if (request && start <= sessionData.size + awaitLength) {
					// Use download range
					request = false;
					if (sessionData.request > 0) {
						// Cancel part range request
						sessionData.request = 0;
						sessionData.rangeCallback.requestPartFromPosition(0);
					}
					setRangeLocked(0, sessionData.size, sessionData.total);
				}
				if (request) {
					sessionData.request = start;
					sessionData.partStart = 0;
					sessionData.partEnd = 0;
					sessionData.rangeCallback.requestPartFromPosition(start);
				}
			}
		}
	}

	private void setRangeLocked(long start, long end, long total) {
		if (!consumed) {
			SessionData sessionData = getInitOrSessionDataLocked();
			if (sessionData != null) {
				holder.setRange(sessionData.pointer, start, end, total);
			}
		}
	}

	private boolean isInitialized() {
		return !consumed && sessionData != null;
	}

	private final int[] summaryOutput = new int[3];

	private boolean obtainSummary() {
		if (isInitialized()) {
			holder.getSummary(sessionData.pointer, summaryOutput);
			return true;
		}
		return false;
	}

	public synchronized Point getDimensions() {
		return obtainSummary() ? new Point(summaryOutput[0], summaryOutput[1]) : null;
	}

	public synchronized boolean isAudioPresent() {
		return obtainSummary() && summaryOutput[2] != 0;
	}

	public synchronized long getDuration() {
		return isInitialized() ? holder.getDuration(sessionData.pointer) : 0;
	}

	public synchronized long getPosition() {
		SeekToPosition seekToPosition = this.seekToPosition;
		return isInitialized() ? seekToPosition != null ? seekToPosition.position
				: holder.getPosition(sessionData.pointer) : 0;
	}

	private void cancelSetPositionLocked(boolean wait) {
		boolean locked = seekerMutex.tryAcquire();
		if (!locked) {
			VideoDiagnostics.recordUi("seek_cancel requested wait=" + wait);
			holder.setCancelSeek(sessionData.pointer, true);
			if (wait) {
				seekerMutex.acquireUninterruptibly();
				holder.setCancelSeek(sessionData.pointer, false);
				locked = true;
			}
		}
		if (locked) {
			seekerMutex.release();
		}
	}

	public void setPosition(long position) {
		synchronized (this) {
			if (isInitialized()) {
				long duration = holder.getDuration(sessionData.pointer);
				position = Math.max(0L, duration > 0L ? Math.min(position, duration) : position);
				cancelSetPositionLocked(false);
				synchronized (seekerThread) {
					long requestId = ++nextSeekRequestId;
					handler.removeMessages(Message.PLAYBACK_COMPLETE.ordinal());
					seekToPosition = new SeekToPosition(requestId, position);
					lastSeekTarget = position;
					VideoDiagnostics.recordUi("seek_request id=" + requestId + " position=" + position
							+ " playing=" + playing);
					seekerThread.notifyAll();
				}
			}
		}
	}

	private void onComplete() {
		Listener listener = this.listener;
		if (listener != null && !consumed) {
			listener.onComplete(this);
		}
	}

	private void handlePlaybackComplete(PlaybackCompletion completion) {
		synchronized (this) {
			if (!isInitialized() || consumed) return;
			long generation = holder.getPlaybackGeneration(sessionData.pointer);
			boolean current = completion != null && completion.isCurrent(generation,
					nextSeekRequestId, seekToPosition != null);
			VideoDiagnostics.recordUi("playback_complete " + (current ? "accepted" : "discarded")
					+ " event_generation=" + (completion != null ? completion.generation : -1)
					+ " generation=" + generation + " request=" + nextSeekRequestId
					+ " seek_pending=" + (seekToPosition != null));
			if (current) onComplete();
		}
	}

	private void onDimensionChange() {
		if (videoView != null) {
			videoView.requestLayout();
		}
		Listener listener = this.listener;
		if (listener != null) {
			listener.onDimensionChange(this);
		}
	}

	@SuppressLint("ViewConstructor")
	private static class PlayerTextureView extends TextureView implements TextureView.SurfaceTextureListener {
		private final WeakReference<VideoPlayer> player;
		private final int diagnosticsId = System.identityHashCode(this);
		private Surface playerSurface;
		private SurfaceTexture playerSurfaceTexture;
		private long playerSurfaceGeneration;
		private int surfaceUpdates;
		private boolean surfaceOutputConfirmed;
		private final Runnable retryVideoOutput = this::retryPendingVideoOutput;
		private long transferredSurfaceStartedMs = -1L;
		private final Runnable recordSettledGeometry = () ->
				VideoDiagnostics.recordViewGeometry("texture_layout_settled", this);

		public PlayerTextureView(Context context, VideoPlayer player) {
			super(context);
			this.player = new WeakReference<>(player);
			setSurfaceTextureListener(this);
			setClickable(false);
			setFocusable(false);
			addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
				if (VideoDiagnostics.isExtendedRecording() && (l != oldL || t != oldT || r != oldR || b != oldB)) {
					VideoDiagnostics.recordViewGeometry("texture_layout", this);
					removeCallbacks(recordSettledGeometry);
					postDelayed(recordSettledGeometry, 350L);
				}
			});
			VideoDiagnostics.recordUi("texture=" + diagnosticsId + " created native="
					+ System.identityHashCode(player));
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			VideoPlayer player = this.player.get();
			if (player == null) {
				return;
			}
			Point dimensions = player.getDimensions();
			if (dimensions != null && dimensions.x > 0 && dimensions.y > 0) {
				int width = getMeasuredWidth();
				int height = getMeasuredHeight();
				if (dimensions.x * height > dimensions.y * width) {
					height = dimensions.y * width / dimensions.x;
				} else {
					width = dimensions.x * height / dimensions.y;
				}
				setMeasuredDimension(width, height);
			}
		}

		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
			VideoPlayer player = this.player.get();
			if (player == null) {
				return;
			}
			if (player.restoreTransferredSurface(this, width, height)) {
				return;
			}
			playerSurface = new Surface(surface);
			playerSurfaceTexture = surface;
			surfaceUpdates = 0;
			surfaceOutputConfirmed = false;
			removeCallbacks(retryVideoOutput);
			VideoDiagnostics.surfaceAvailable(diagnosticsId);
			VideoDiagnostics.recordUi("texture=" + diagnosticsId + " surface_available"
					+ " surface_size=" + width + "x" + height
					+ " view_size=" + getWidth() + "x" + getHeight()
					+ " visibility=" + getVisibility() + " alpha=" + getAlpha());
			playerSurfaceGeneration = player.setSurface(playerSurface, width, height);
			VideoDiagnostics.recordViewGeometry("texture_available", this);
			if (playerSurfaceGeneration != 0L) {
				// Called on the main thread before this texture can receive its first frame.
				Listener listener = player.listener;
				if (listener != null) listener.onVideoOutputReset(player);
			}
			if (playerSurfaceGeneration == 0L) {
				playerSurface = null;
				playerSurfaceTexture = null;
			}
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
			removeCallbacks(retryVideoOutput);
			surfaceOutputConfirmed = false;
			VideoDiagnostics.surfaceDestroyed(diagnosticsId);
			VideoDiagnostics.recordUi("texture=" + diagnosticsId + " surface_destroyed"
					+ " updates=" + surfaceUpdates);
			boolean retained = releasePlayerSurface(surface);
			VideoDiagnostics.recordUi("texture=" + diagnosticsId + " surface_destroyed"
					+ " retained_until_replacement=" + retained);
			// Returning false transfers SurfaceTexture ownership to VideoPlayer. It releases the
			// texture only after the replacement is safe or the player is destroyed.
			return !retained;
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
			VideoPlayer player = this.player.get();
			if (player != null) {
				VideoDiagnostics.recordUi("texture=" + diagnosticsId + " surface_size_changed"
						+ " surface_size=" + width + "x" + height);
				player.setSurfaceSize(width, height);
				VideoDiagnostics.recordViewGeometry("texture_size_changed", this);
			}
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			surfaceUpdates++;
			if (surfaceUpdates == 1 && transferredSurfaceStartedMs >= 0L) {
				VideoDiagnostics.recordUi("surface_transfer_first_update generation=" + playerSurfaceGeneration
						+ " elapsed_ms=" + (SystemClock.elapsedRealtime() - transferredSurfaceStartedMs));
			}
			VideoDiagnostics.surfaceUpdated(diagnosticsId);
			VideoPlayer player = this.player.get();
			removeCallbacks(retryVideoOutput);
			if (!confirmVideoOutput() && player != null && player.isVideoView(this)
					&& playerSurfaceGeneration > 0L) {
				// The UI update can race native's post-success publication, including a paused frame.
				postOnAnimation(retryVideoOutput);
			}
			if (surfaceUpdates == 1 || surfaceUpdates == 30 || surfaceUpdates % 120 == 0) {
				VideoDiagnostics.recordViewGeometry("texture_frame", this);
				VideoDiagnostics.recordUi("texture=" + diagnosticsId + " surface_updated"
						+ " count=" + surfaceUpdates + " view_size=" + getWidth() + "x" + getHeight()
						+ " visibility=" + getVisibility() + " alpha=" + getAlpha()
						+ " generation=" + playerSurfaceGeneration
						+ " native=" + System.identityHashCode(player));
			}
		}

		private void retryPendingVideoOutput() {
			if (isAvailable() && surfaceUpdates > 0 && !confirmVideoOutput()) {
				VideoPlayer current = player.get();
				if (current != null) {
					synchronized (current) {
						if (current.isInitialized() && current.isVideoView(this) && playerSurfaceGeneration > 0L) {
							postOnAnimation(retryVideoOutput);
						}
					}
				}
			}
		}

		private boolean confirmVideoOutput() {
			VideoPlayer current = player.get();
			if (current == null || !current.isVideoView(this) || playerSurfaceGeneration <= 0L) return false;
			if (!surfaceOutputConfirmed) {
				if (!current.isSurfaceOutputConfirmed(playerSurfaceGeneration)) return false;
				surfaceOutputConfirmed = true;
				current.onSurfaceFrameRendered(playerSurfaceGeneration);
			}
			current.notifyVideoViewFrameUpdated();
			return true;
		}

		@Override
		protected void onAttachedToWindow() {
			super.onAttachedToWindow();
			VideoDiagnostics.recordUi("texture=" + diagnosticsId + " attached"
					+ " visibility=" + getVisibility());
		}

		@Override
		protected void onDetachedFromWindow() {
			removeCallbacks(retryVideoOutput);
			removeCallbacks(recordSettledGeometry);
			VideoDiagnostics.recordUi("texture=" + diagnosticsId + " detached"
					+ " updates=" + surfaceUpdates);
			super.onDetachedFromWindow();
		}

		@Override
		protected void onVisibilityChanged(View changedView, int visibility) {
			super.onVisibilityChanged(changedView, visibility);
			if (changedView == this) {
				VideoDiagnostics.recordUi("texture=" + diagnosticsId
						+ " visibility=" + visibility + " updates=" + surfaceUpdates);
			}
		}

		private boolean releasePlayerSurface(SurfaceTexture surface) {
			if (playerSurfaceTexture != surface || playerSurfaceGeneration == 0L) {
				return false;
			}
			VideoPlayer player = this.player.get();
			long generation = playerSurfaceGeneration;
			playerSurface = null;
			playerSurfaceTexture = null;
			playerSurfaceGeneration = 0L;
			boolean retained = player != null && player.deferSurfaceTextureRelease(generation, surface);
			VideoDiagnostics.recordUi("texture=" + diagnosticsId + " surface_handed_off"
					+ " generation=" + generation + " retained=" + retained);
			return retained;
		}

		public Bitmap getSnapshot(int maxEdge) {
			if (isAvailable() && surfaceOutputConfirmed && surfaceUpdates > 0
					&& getWidth() > 0 && getHeight() > 0 && maxEdge > 0) {
				float scale = Math.min(1f, (float) maxEdge / Math.max(getWidth(), getHeight()));
				int width = Math.max(1, Math.round(getWidth() * scale));
				int height = Math.max(1, Math.round(getHeight() * scale));
				try {
					Bitmap bitmap = getBitmap(width, height);
					VideoDiagnostics.recordUi("texture=" + diagnosticsId + " poster_snapshot="
							+ width + "x" + height + " available=" + (bitmap != null));
					return bitmap;
				} catch (RuntimeException e) {
					VideoDiagnostics.recordUi("texture=" + diagnosticsId
							+ " poster_snapshot_failed=" + e.getClass().getSimpleName());
				}
			}
			return null;
		}

		public Bitmap getCurrentFrame() {
			if (isAvailable() && surfaceUpdates > 0 && getWidth() > 0 && getHeight() > 0) {
				try {
					Bitmap bitmap = getBitmap();
					if (bitmap != null) {
						VideoDiagnostics.recordUi("texture=" + diagnosticsId
								+ " snapshot=" + bitmap.getWidth() + "x" + bitmap.getHeight());
					}
					return bitmap;
				} catch (RuntimeException e) {
					VideoDiagnostics.recordUi("texture=" + diagnosticsId
							+ " snapshot_failed=" + e.getClass().getSimpleName());
				}
			}
			return null;
		}
	}

	private View videoView;
	private Runnable videoViewFrameCallback;

	public void setVideoViewFrameCallback(Runnable callback) {
		videoViewFrameCallback = callback;
	}

	private void notifyVideoViewFrameUpdated() {
		Runnable callback = videoViewFrameCallback;
		if (callback != null) {
			videoViewFrameCallback = null;
			callback.run();
		}
	}

	public View getVideoView(Context context) {
		if (videoView == null) {
			videoView = new PlayerTextureView(context, this);
		}
		return videoView;
	}

	/** UI-thread only: copy the existing rendered texture, never create a view or read native frames. */
	public Bitmap getVideoViewSnapshot(int maxEdge) {
		View view = videoView;
		return view instanceof PlayerTextureView ? ((PlayerTextureView) view).getSnapshot(maxEdge) : null;
	}

	// Identity check without creating a view or changing its owner during a transfer.
	public boolean isVideoView(View view) {
		return view != null && view == videoView;
	}

	public void releaseVideoView() {
		View videoView = this.videoView;
		this.videoView = null;
		if (videoView != null && videoView.getParent() instanceof ViewGroup) {
			((ViewGroup) videoView.getParent()).removeView(videoView);
		}
	}

	/** Reserve the current output for a window transfer without detaching the visible view yet. */
	public void prepareVideoViewForTransfer() {
		synchronized (this) {
			if (videoView instanceof PlayerTextureView) {
				PlayerTextureView texture = (PlayerTextureView) videoView;
				long generation = texture.playerSurfaceGeneration;
				// Android may have detached the old window while the destination was being laid out.
				// Keep its already-reserved generation; its texture is now in deferredSurfaceTextures.
				if (generation == 0L && transferableSurfaceGeneration != 0L) return;
				surfaceTransferStartedMs = SystemClock.elapsedRealtime();
				transferableSurfaceGeneration = isInitialized() && generation > 0L
						&& generation == latestSurfaceGeneration
						&& generation == latestAppliedSurfaceGeneration
						&& generation == latestRenderedSurfaceGeneration ? generation : 0L;
				VideoDiagnostics.recordUi("surface_transfer_prepare generation=" + generation
						+ " reusable=" + (transferableSurfaceGeneration != 0L));
			}
		}
	}

	/** Detach for a same-player window transfer, without requiring a new decoder output. */
	public void releaseVideoViewForTransfer() {
		prepareVideoViewForTransfer();
		// onSurfaceTextureDestroyed retains the texture using the existing deferred ownership.
		releaseVideoView();
	}

	private boolean restoreTransferredSurface(PlayerTextureView target, int width, int height) {
		synchronized (this) {
			long generation = transferableSurfaceGeneration;
			transferableSurfaceGeneration = 0L;
			if (!isInitialized() || generation == 0L || generation != latestSurfaceGeneration
					|| generation != latestAppliedSurfaceGeneration) {
				return false;
			}
			for (int i = 0; i < deferredSurfaceTextures.size(); i++) {
				DeferredSurfaceTexture deferred = deferredSurfaceTextures.get(i);
				if (deferred.generation != generation || deferred.surfaceTexture.isReleased()) continue;
				try {
					// The old TextureView has detached from its GL context. This replaces only
					// the new view's unused texture, not MediaCodec's existing output surface.
					// setSurfaceTexture does not invoke onSurfaceTextureAvailable for it.
					target.setSurfaceTexture(deferred.surfaceTexture);
				} catch (RuntimeException e) {
					VideoDiagnostics.recordUi("surface_transfer_fallback generation=" + generation
							+ " error=" + e.getClass().getSimpleName());
					return false;
				}
				deferredSurfaceTextures.remove(i); // Ownership now belongs to the attached view.
				seekRequestAtSurfaceLoss = -1L; // The decoder keeps its output: no recovery seek follows.
				target.playerSurfaceTexture = deferred.surfaceTexture;
				target.playerSurfaceGeneration = generation;
				target.surfaceUpdates = 0;
				target.surfaceOutputConfirmed = false;
				target.removeCallbacks(target.retryVideoOutput);
				target.transferredSurfaceStartedMs = surfaceTransferStartedMs;
				setSurfaceSize(width, height);
				VideoDiagnostics.surfaceAvailable(target.diagnosticsId);
				VideoDiagnostics.recordUi("surface_transfer_reused generation=" + generation
						+ " texture=" + target.diagnosticsId + " size=" + width + "x" + height);
				return true;
			}
			VideoDiagnostics.recordUi("surface_transfer_fallback generation=" + generation
					+ " reason=texture_not_retained");
			return false;
		}
	}

	public void releaseVideoViewAndDestroyAsync() {
		VideoDiagnostics.recordUi("player_release_and_destroy scheduled");
		setPlaying(false);
		stopAudioImmediately();
		releaseVideoView();
		releaseExecutor.execute(() -> {
			VideoDiagnostics.recordUi("player_release_and_destroy native_destroy_started");
			destroy();
		});
	}

	private long setSurface(Surface surface, int width, int height) {
		synchronized (this) {
			if (isInitialized()) {
				SeekToPosition pendingSeek = seekToPosition;
				cancelSetPositionLocked(false);
				long generation = ++nextSurfaceGeneration;
				latestSurfaceGeneration = generation;
				surfaceRequests.add(new SurfaceRequest(generation, surface));
				VideoDiagnostics.recordUi("surface_request generation=" + generation
						+ " valid=" + surface.isValid() + " size=" + width + "x" + height);
				holder.requestSurface(sessionData.pointer, surface, generation, width, height);
				// Surface-only changes may not reset the codec or trigger a recovery seek.
				// Never abandon a cancelled transaction after its decoder was flushed.
				if (pendingSeek != null) setPosition(pendingSeek.position);
				return generation;
			} else {
				surface.release();
				return 0L;
			}
		}
	}

	private void setSurfaceSize(int width, int height) {
		synchronized (this) {
			if (isInitialized() && width > 0 && height > 0) {
				holder.setSurfaceSize(sessionData.pointer, width, height);
			}
		}
	}

	private void onSurfaceApplied(SurfaceApplied surfaceApplied) {
		synchronized (this) {
			releaseSurfaceRequestsLocked(surfaceApplied.generation);
			latestAppliedSurfaceGeneration = Math.max(latestAppliedSurfaceGeneration,
					surfaceApplied.generation);
			if (!playing) {
				releaseDeferredSurfaceTexturesLocked(surfaceApplied.generation, false);
			}
			if (!isInitialized() || surfaceApplied.generation != latestSurfaceGeneration) {
				VideoDiagnostics.recordUi("surface_applied_stale generation=" + surfaceApplied.generation
						+ " latest=" + latestSurfaceGeneration);
				return;
			}
			VideoDiagnostics.recordUi("surface_applied generation=" + surfaceApplied.generation
					+ " position=" + surfaceApplied.position
					+ " decoder_reset=" + surfaceApplied.decoderReset
					+ " resume=" + playing);
			if (surfaceApplied.decoderReset) {
				boolean seekedWithoutSurface = seekRequestAtSurfaceLoss >= 0L
						&& nextSeekRequestId > seekRequestAtSurfaceLoss;
				long position = seekedWithoutSurface ? lastSeekTarget : surfaceApplied.position;
				if (seekedWithoutSurface) {
					VideoDiagnostics.recordUi("surface_applied_seek_override reported=" + surfaceApplied.position
							+ " target=" + position);
				}
				setPosition(position);
			}
			seekRequestAtSurfaceLoss = -1L;
			if (playing) {
				holder.setPlaying(sessionData.pointer, networkBuffering.shouldPlay(playing));
			}
		}
	}

	private void releaseSurfaceRequestsLocked(long appliedGeneration) {
		for (int i = surfaceRequests.size() - 1; i >= 0; i--) {
			SurfaceRequest request = surfaceRequests.get(i);
			if (request.generation <= appliedGeneration) {
				request.surface.release();
				surfaceRequests.remove(i);
			}
		}
	}

	private void releaseAllSurfaceRequestsLocked() {
		for (SurfaceRequest request : surfaceRequests) {
			request.surface.release();
		}
		surfaceRequests.clear();
	}

	private boolean deferSurfaceTextureRelease(long generation, SurfaceTexture surfaceTexture) {
		synchronized (this) {
			if (!isInitialized() || generation <= 0L) {
				return false;
			}
			if (seekRequestAtSurfaceLoss < 0L) seekRequestAtSurfaceLoss = nextSeekRequestId;
			for (DeferredSurfaceTexture deferred : deferredSurfaceTextures) {
				if (deferred.surfaceTexture == surfaceTexture) {
					return true;
				}
			}
			long safeReplacementGeneration = playing
					? latestRenderedSurfaceGeneration : latestAppliedSurfaceGeneration;
			if (generation < safeReplacementGeneration) {
				surfaceTexture.release();
				VideoDiagnostics.recordUi("surface_texture_released generation=" + generation
						+ " reason=replacement_already_safe");
				return true;
			}
			deferredSurfaceTextures.add(new DeferredSurfaceTexture(generation, surfaceTexture));
			VideoDiagnostics.recordUi("surface_texture_deferred generation=" + generation
					+ " latest_applied=" + latestAppliedSurfaceGeneration
					+ " latest_rendered=" + latestRenderedSurfaceGeneration);
			return true;
		}
	}

	private boolean isSurfaceOutputConfirmed(long generation) {
		synchronized (this) {
			return isInitialized() && VideoSurfaceFrameGate.isConfirmed(generation,
					holder.getRenderedSurfaceGeneration(sessionData.pointer));
		}
	}

	private void onSurfaceFrameRendered(long generation) {
		synchronized (this) {
			if (!isInitialized() || generation <= latestRenderedSurfaceGeneration) {
				return;
			}
			latestRenderedSurfaceGeneration = generation;
			releaseDeferredSurfaceTexturesLocked(generation, false);
			VideoDiagnostics.recordUi("surface_first_frame generation=" + generation
					+ " latest_applied=" + latestAppliedSurfaceGeneration);
		}
	}

	private void releaseDeferredSurfaceTexturesLocked(long renderedGeneration, boolean releaseAll) {
		for (int i = deferredSurfaceTextures.size() - 1; i >= 0; i--) {
			DeferredSurfaceTexture deferred = deferredSurfaceTextures.get(i);
			if (releaseAll || deferred.generation < renderedGeneration) {
				deferred.surfaceTexture.release();
				deferredSurfaceTextures.remove(i);
				VideoDiagnostics.recordUi("surface_texture_released generation=" + deferred.generation
						+ " reason=" + (releaseAll ? "player_destroyed" : "replacement_safe"));
			}
		}
	}

	public void setPlaying(boolean playing) {
		synchronized (this) {
			if (isInitialized() && this.playing != playing) {
				SeekToPosition seekToPosition = this.seekToPosition;
				// Resuming an in-flight seek is safe: native commit reads the current
				// play intent. Cancelling here used to turn every Replay into two seeks.
				// A pause still reissues the target to guarantee a paused preview frame.
				if (!playing) cancelSetPositionLocked(false);
				VideoDiagnostics.recordUi("playback_intent playing=" + playing
						+ " seek_pending=" + (seekToPosition != null) + " request=" + nextSeekRequestId);
				this.playing = playing;
				holder.setPlaying(sessionData.pointer, networkBuffering.shouldPlay(playing));
				if (!playing && seekToPosition != null) {
					// Reissue a seek cancelled by the playback-state transition. When pausing,
					// the native player decodes exactly one preview frame without resuming audio.
					setPosition(seekToPosition.position);
				}
				handler.post(this::notifyBusyState);
			}
		}
	}

	private void stopAudioImmediately() {
		synchronized (this) {
			if (isInitialized()) {
				VideoDiagnostics.recordUi("player_audio_stop_immediate");
				holder.stopAudio(sessionData.pointer);
			}
		}
	}

	public void setPlaybackSpeed(int speed) {
		if (speed < 10) {
			speed = 10;
		} else if (speed > 10000) {
			speed = 10000;
		}
		synchronized (this) {
			VideoDiagnostics.recordUi("playback_speed_requested previous_milli=" + playbackSpeed
					+ " speed_milli=" + speed + " initialized=" + isInitialized());
			playbackSpeed = speed;
			applyPlaybackSpeedLocked();
		}
	}

	public boolean setVolume(int volume, int boostDb) {
		synchronized (this) {
			if (isInitialized()) {
				try {
					return holder.setVolume(sessionData.pointer, Math.max(0, Math.min(volume, 100)),
							Math.max(0, Math.min(boostDb, 12)));
				} catch (RuntimeException | LinkageError e) {
					return false;
				}
			}
			return false;
		}
	}

	public boolean setMuted(boolean muted) {
		synchronized (this) {
			if (isInitialized()) {
				try {
					return holder.setMuted(sessionData.pointer, muted);
				} catch (RuntimeException | LinkageError e) {
					return false;
				}
			}
			return false;
		}
	}

	private void applyPlaybackSpeedLocked() {
		if (isInitialized() && playbackSpeedSupported) {
			try {
				holder.setPlaybackSpeed(sessionData.pointer, playbackSpeed);
				VideoDiagnostics.recordUi("playback_speed_applied speed_milli=" + playbackSpeed);
			} catch (RuntimeException | LinkageError e) {
				playbackSpeedSupported = false;
				VideoDiagnostics.recordUi("playback_speed_unavailable requested_milli=" + playbackSpeed);
			}
		}
	}

	public boolean isPlaying() {
		synchronized (this) {
			return isInitialized() && playing;
		}
	}

	private final int[] dimensionsOutput = new int[2];

	public Bitmap getCurrentFrame() {
		synchronized (this) {
			if (isInitialized()) {
				View videoView = this.videoView;
				if (videoView instanceof PlayerTextureView) {
					Bitmap bitmap = ((PlayerTextureView) videoView).getCurrentFrame();
					if (bitmap != null) {
						return bitmap;
					}
				}
				int[] dimensions = dimensionsOutput;
				int[] frame = holder.getCurrentFrame(sessionData.pointer, dimensions);
				if (frame != null) {
					try {
						return Bitmap.createBitmap(frame, dimensions[0], dimensions[1], Bitmap.Config.ARGB_8888);
					} catch (Exception e) {
						// Ignore exception
					}
				}
			}
			return null;
		}
	}

	public Map<String, String> getMetadata() {
		synchronized (this) {
			if (isInitialized()) {
				String[] array = holder.getMetadata(sessionData.pointer);
				if (array != null) {
					HashMap<String, String> result = new HashMap<>();
					for (int i = 0; i < array.length; i += 2) {
						String key = array[i];
						String value = array[i + 1];
						if (key != null && value != null) {
							result.put(key, value);
						}
					}
					result.put("player_build", "ffmpeg8"
							+ " / " + joinArray(Build.SUPPORTED_ABIS));
					result.put("device_abis", joinArray(Build.SUPPORTED_ABIS));
					return result;
				}
			}
			return Collections.emptyMap();
		}
	}

	public void destroy() {
		close();
	}

	/** The current gallery/PiP owner closes the player; a transfer must not close it. */
	@Override
	public void close() {
		destroyInternal(null);
	}

	public void destroyAsync() {
		setPlaying(false);
		stopAudioImmediately();
		releaseExecutor.execute(this::destroy);
	}

	private void destroyInternal(SessionData preInitSessionData) {
		synchronized (this) {
			if (!consumed) {
				consumed = true;
				releaseExecutor.shutdown();
				listener = null;
				try {
					if (initData != null) {
						holder.destroy(initData.pointer, true);
					}
					SessionData sessionData = preInitSessionData != null ? preInitSessionData : this.sessionData;
					if (sessionData != null) {
						cancelSetPositionLocked(true);
						synchronized (seekerThread) {
							seekerThread.notifyAll();
						}
						holder.destroy(sessionData.pointer, false);
					}
				} finally {
					handler.removeCallbacksAndMessages(null);
					transferableSurfaceGeneration = 0L;
					releaseAllSurfaceRequestsLocked();
					releaseDeferredSurfaceTexturesLocked(Long.MAX_VALUE, true);
				}
			}
		}
	}

	private void onSeekingBufferingStateChange(boolean seekingChange, boolean bufferingChange) {
		if (seekingChange && lastBuffering) {
			return;
		}
		if (bufferingChange && lastSeeking) {
			return;
		}
		notifyBusyState();
	}

	private void notifyBusyState() {
		Listener listener;
		boolean busy;
		synchronized (this) {
			listener = this.listener;
			busy = lastSeeking || lastBuffering || networkBuffering.shouldShowProgress(playing);
		}
		if (listener != null && !consumed) {
			listener.onBusyStateChange(this, busy);
		}
	}

	private enum Message {PLAYBACK_COMPLETE, SIZE_CHANGED, START_SEEKING, END_SEEKING, START_BUFFERING, END_BUFFERING,
		REPORT_STALLED_SEEK, SURFACE_APPLIED, REQUEST_RANGE, DURATION_CHANGED, NETWORK_BUFFERING}

	private final Handler handler = new Handler(Looper.getMainLooper(), msg -> {
		if (consumed) return true;
		switch (Message.values()[msg.what]) {
			case NETWORK_BUFFERING: {
				long serial = (long) msg.obj;
				synchronized (this) {
					boolean buffering = msg.arg1 != 0;
					if (consumed || !networkBuffering.apply(serial, buffering)) return true;
					VideoDiagnostics.recordUi("network_buffering state=" + buffering
							+ " serial=" + serial + " user_playing=" + playing
							+ " speed_milli=" + playbackSpeed + " target_viewing_ms=3000");
					if (isInitialized()) {
						// Buffering must neither overwrite user intent nor cancel/reissue a seek.
						holder.setPlaying(sessionData.pointer, networkBuffering.shouldPlay(playing));
					}
				}
				notifyBusyState();
				return true;
			}
			case PLAYBACK_COMPLETE: {
				handlePlaybackComplete((PlaybackCompletion) msg.obj);
				return true;
			}
			case SIZE_CHANGED: {
				onDimensionChange();
				return true;
			}
			case START_SEEKING:
			case END_SEEKING: {
				boolean seeking = msg.what == Message.START_SEEKING.ordinal();
				if (lastSeeking != seeking) {
					lastSeeking = seeking;
					onSeekingBufferingStateChange(true, false);
				}
				return true;
			}
			case START_BUFFERING:
			case END_BUFFERING: {
				boolean buffering = msg.what == Message.START_BUFFERING.ordinal();
				if (lastBuffering != buffering) {
					lastBuffering = buffering;
					onSeekingBufferingStateChange(false, true);
				}
				return true;
			}
			case REPORT_STALLED_SEEK: {
				SeekToPosition seekToPosition = (SeekToPosition) msg.obj;
				if (this.seekToPosition == seekToPosition) {
					VideoDiagnostics.recordUi("seek_stalled id=" + seekToPosition.requestId
							+ " position=" + seekToPosition.position
							+ " elapsed_ms=" + SEEK_STALL_REPORT_DELAY);
				}
				return true;
			}
			case SURFACE_APPLIED: {
				onSurfaceApplied((SurfaceApplied) msg.obj);
				return true;
			}
			case REQUEST_RANGE: {
				long position = (long) msg.obj;
				requestRange(position);
				return true;
			}
			case DURATION_CHANGED: {
				long duration = (long) msg.obj;
				synchronized (this) {
					SeekToPosition seekToPosition = this.seekToPosition;
					if (duration > 0L && seekToPosition != null && seekToPosition.position > duration) {
						setPosition(duration);
					}
				}
				Listener listener = this.listener;
				if (listener != null && !consumed) {
					listener.onDurationChange(this, duration);
				}
				return true;
			}
		}
		return false;
	});

	private static class SeekToPosition {
		public final long requestId;
		public final long position;

		public SeekToPosition(long requestId, long position) {
			this.requestId = requestId;
			this.position = position;
		}
	}

	private static class SurfaceApplied {
		public final long generation;
		public final long position;
		public final boolean decoderReset;

		public SurfaceApplied(long generation, long position, boolean decoderReset) {
			this.generation = generation;
			this.position = position;
			this.decoderReset = decoderReset;
		}
	}

	private static class SurfaceRequest {
		public final long generation;
		public final Surface surface;

		public SurfaceRequest(long generation, Surface surface) {
			this.generation = generation;
			this.surface = surface;
		}
	}

	private static class DeferredSurfaceTexture {
		public final long generation;
		public final SurfaceTexture surfaceTexture;

		public DeferredSurfaceTexture(long generation, SurfaceTexture surfaceTexture) {
			this.generation = generation;
			this.surfaceTexture = surfaceTexture;
		}
	}

	private volatile SeekToPosition seekToPosition = null;
	private volatile long nextSeekRequestId;
	private final Semaphore seekerMutex = new Semaphore(1);
	private final Thread seekerThread = new Thread(this::seekerThread);

	private void seekerThread() {
		Thread seekerThread = Thread.currentThread();
		while (true) {
			SeekToPosition workSeekToPosition;
			synchronized (seekerThread) {
				while (true) {
					workSeekToPosition = seekToPosition;
					if (workSeekToPosition == null && !consumed) {
						try {
							seekerThread.wait();
						} catch (InterruptedException e) {
							// Uninterruptible wait
						}
					} else {
						break;
					}
				}
				if (consumed) {
					return;
				}
				if (sessionData != null) {
					seekerMutex.acquireUninterruptibly();
					holder.setCancelSeek(sessionData.pointer, false);
				} else {
					continue;
				}
			}
			try {
				long position = workSeekToPosition.position;
				VideoDiagnostics.recordUi("seek_started id=" + workSeekToPosition.requestId
						+ " position=" + position);
				handler.removeMessages(Message.END_BUFFERING.ordinal());
				handler.sendEmptyMessageDelayed(Message.START_BUFFERING.ordinal(), 200);
				handler.sendMessageDelayed(handler.obtainMessage
						(Message.REPORT_STALLED_SEEK.ordinal(), workSeekToPosition), SEEK_STALL_REPORT_DELAY);
				holder.setPosition(sessionData.pointer, position);
				VideoDiagnostics.recordUi("seek_native_finished id=" + workSeekToPosition.requestId);
				handler.removeMessages(Message.START_BUFFERING.ordinal());
				handler.sendEmptyMessageDelayed(Message.END_BUFFERING.ordinal(), 100);
			} finally {
				handler.removeMessages(Message.REPORT_STALLED_SEEK.ordinal(), workSeekToPosition);
				synchronized (seekerThread) {
					if (seekToPosition == workSeekToPosition) {
						seekToPosition = null;
					}
				}
				holder.setCancelSeek(sessionData.pointer, false);
				seekerMutex.release();
			}
		}
	}

	@SuppressWarnings("unused")
	private static class NativeBridge {
		private static final int BRIDGE_MESSAGE_PLAYBACK_COMPLETE = 1;
		private static final int BRIDGE_MESSAGE_SIZE_CHANGED = 2;
		private static final int BRIDGE_MESSAGE_START_SEEKING = 3;
		private static final int BRIDGE_MESSAGE_END_SEEKING = 4;

		private final WeakReference<VideoPlayer> player;

		public NativeBridge(VideoPlayer player) {
			this.player = new WeakReference<>(player);
		}

		public void onPlaybackComplete(long generation) {
			VideoPlayer player = this.player.get();
			if (player != null) {
				// Do not acquire the player monitor from a native callback: native EOF
				// holds finishMutex, while UI play/pause may hold that monitor.
				PlaybackCompletion completion = new PlaybackCompletion(generation, player.nextSeekRequestId);
				VideoDiagnostics.recordUi("playback_complete queued generation=" + generation
						+ " request=" + completion.requestId);
				player.handler.sendMessageDelayed(player.handler.obtainMessage
						(Message.PLAYBACK_COMPLETE.ordinal(), completion), 200);
			}
		}

		public void onSeek(long position) {
			VideoPlayer player = this.player.get();
			if (player != null) {
				player.handler.removeMessages(Message.REQUEST_RANGE.ordinal());
				player.handler.obtainMessage(Message.REQUEST_RANGE.ordinal(), position).sendToTarget();
			}
		}

		public void onDurationChanged(long duration) {
			VideoPlayer player = this.player.get();
			if (player != null) {
				player.handler.obtainMessage(Message.DURATION_CHANGED.ordinal(), duration).sendToTarget();
			}
		}

		public void onNetworkBuffering(long serial, boolean buffering) {
			VideoPlayer player = this.player.get();
			if (player != null) {
				player.handler.obtainMessage(Message.NETWORK_BUFFERING.ordinal(),
						buffering ? 1 : 0, 0, Long.valueOf(serial)).sendToTarget();
			}
		}

		public void onSurfaceApplied(long generation, long position, boolean decoderReset) {
			VideoPlayer player = this.player.get();
			if (player != null) {
				player.handler.obtainMessage(Message.SURFACE_APPLIED.ordinal(),
						new SurfaceApplied(generation, position, decoderReset)).sendToTarget();
			}
		}

		public String findHardwareVideoDecoder(String mimeType, int width, int height, float frameRate,
				int codecProfile, int codecLevel, int bitDepth, int colorTransfer) {
			return VideoDecoderCapabilities.findHardwareDecoder(mimeType, width, height, frameRate,
					codecProfile, codecLevel, bitDepth, colorTransfer);
		}

		public void onMessage(int what) {
			VideoPlayer player = this.player.get();
			if (player != null) {
				switch (what) {
					case BRIDGE_MESSAGE_PLAYBACK_COMPLETE: {
						// Current bundled engines deliver a generation through onPlaybackComplete.
						VideoDiagnostics.recordUi("playback_complete discarded reason=missing_generation");
						break;
					}
					case BRIDGE_MESSAGE_SIZE_CHANGED: {
						player.handler.sendEmptyMessage(Message.SIZE_CHANGED.ordinal());
						break;
					}
					case BRIDGE_MESSAGE_START_SEEKING: {
						player.handler.removeMessages(Message.END_SEEKING.ordinal());
						if (player.lastBuffering) {
							player.handler.sendEmptyMessage(Message.START_SEEKING.ordinal());
						} else {
							player.handler.sendEmptyMessageDelayed(Message.START_SEEKING.ordinal(), 500);
						}
						break;
					}
					case BRIDGE_MESSAGE_END_SEEKING: {
						player.handler.removeMessages(Message.START_SEEKING.ordinal());
						player.handler.sendEmptyMessage(Message.END_SEEKING.ordinal());
						break;
					}
				}
			}
		}
	}

	private interface HolderInterface {
		long preInit(int fd);
		void init(long pointer, Object nativeBridge, boolean seekAnyFrame);
		void destroy(long pointer, boolean initOnly);

		int getErrorCode(long pointer);
		void getSummary(long pointer, int[] output);

		long getDuration(long pointer);
		long getPosition(long pointer);
		long getPlaybackGeneration(long pointer);
		long getRenderedSurfaceGeneration(long pointer);
		void setPosition(long pointer, long position);

		void setRange(long pointer, long start, long end, long total);
		void setCancelSeek(long pointer, boolean cancelSeek);

		void setAudioEnabled(long pointer, boolean audioEnabled);
		void setHardwareAcceleration(long pointer, boolean hardwareAcceleration);
		void setPlaybackSpeed(long pointer, int speed);
		boolean setVolume(long pointer, int volume, int boostDb);
		boolean setMuted(long pointer, boolean muted);
		void stopAudio(long pointer);
		void requestSurface(long pointer, Surface surface, long generation, int width, int height);
		void setSurfaceSize(long pointer, int width, int height);
		void setPlaying(long pointer, boolean playing);

		int[] getCurrentFrame(long pointer, int[] dimensions);
		String[] getMetadata(long pointer);
		void startDiagnostics();
		String stopDiagnostics();
		void startExtendedDiagnostics();
		void sampleDiagnostics();
		int finishDiagnostics();
		byte[] readDiagnosticsChunk(int offset, int length);
		void releaseDiagnostics();
	}

	private static class Holder implements HolderInterface {
		@Override public native long preInit(int fd);
		@Override public native void init(long pointer, Object nativeBridge, boolean seekAnyFrame);
		@Override public native void destroy(long pointer, boolean initOnly);

		@Override public native int getErrorCode(long pointer);
		@Override public native void getSummary(long pointer, int[] output);

		@Override public native long getDuration(long pointer);
		@Override public native long getPosition(long pointer);
		@Override public native long getPlaybackGeneration(long pointer);
		@Override public native long getRenderedSurfaceGeneration(long pointer);
		@Override public native void setPosition(long pointer, long position);

		@Override public native void setRange(long pointer, long start, long end, long total);
		@Override public native void setCancelSeek(long pointer, boolean busy);

		@Override public native void setAudioEnabled(long pointer, boolean audioEnabled);
		@Override public native void setHardwareAcceleration(long pointer, boolean hardwareAcceleration);
		@Override public native void setPlaybackSpeed(long pointer, int speed);
		@Override public native boolean setVolume(long pointer, int volume, int boostDb);
		@Override public native boolean setMuted(long pointer, boolean muted);
		@Override public native void stopAudio(long pointer);
		@Override public native void requestSurface(long pointer, Surface surface, long generation,
				int width, int height);
		@Override public native void setSurfaceSize(long pointer, int width, int height);
		@Override public native void setPlaying(long pointer, boolean playing);

		@Override public native int[] getCurrentFrame(long pointer, int[] dimensions);
		@Override public native String[] getMetadata(long pointer);
		@Override public native void startDiagnostics();
		@Override public native String stopDiagnostics();
		@Override public native void startExtendedDiagnostics();
		@Override public native void sampleDiagnostics();
		@Override public native int finishDiagnostics();
		@Override public native byte[] readDiagnosticsChunk(int offset, int length);
		@Override public native void releaseDiagnostics();

		static {
			try {
				System.loadLibrary("dav1d");
			} catch (UnsatisfiedLinkError ignored) {
				// Optional in older WebM library builds.
			}
			System.loadLibrary("avutil");
			System.loadLibrary("swresample");
			System.loadLibrary("swscale");
			System.loadLibrary("avcodec");
			System.loadLibrary("avformat");
			if (BUNDLED_ATEMPO) {
				System.loadLibrary("avfilter");
			}
			System.loadLibrary("player");
		}
	}

}
