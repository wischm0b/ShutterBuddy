package de.jr.pixelremote;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;

public final class RemoteService extends Service {
    static volatile int state = R.string.status_stopped;
    static volatile UiMessage lastResult = UiMessage.empty();
    private static volatile boolean active;
    private static volatile long lastWatchSeen;
    static boolean isRunning() { return active; }
    static boolean hasWatchLink() { return active && lastWatchSeen > 0 && SystemClock.elapsedRealtime() - lastWatchSeen < 25000; }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final IQApp watchApp = new IQApp("b7e89053786045fc9a3c9d70dfd8a174");
    private final Set<Long> listening = new HashSet<>();
    private final Map<Long, CommandSession> sessions = new HashMap<>();
    private final Map<Long, IQDevice> peers = new HashMap<>();
    private final SharedPreferences.OnSharedPreferenceChangeListener settingsChanged = (preferences, key) -> handler.post(() -> {
        if (!this.running || !"revision".equals(key)) return;
        updateNotification();
        for (Map.Entry<Long, CommandSession> entry : sessions.entrySet()) {
            IQDevice device = peers.get(entry.getKey());
            if (device != null) send(device, Map.of("v", 2, "type", "settings", "session", entry.getValue().token));
        }
    });
    private ConnectIQ sdk;
    private boolean running, sdkReady, cameraPending;
    private int generation;
    private Runnable pendingShot;
    private TimedShot timedShot;
    private IQDevice pendingDevice;
    private CommandSession pendingSession;
    private int pendingSequence;
    private long pendingDeadline;
    interface CameraAction { void fire(IntConsumer done); }
    private CameraAction cameraAction = CameraAccessibilityService::trigger;
    private SeriesRun series;
    private Runnable seriesTask, seriesGuard;
    private final Runnable refresh = new Runnable() {
        @Override public void run() { if (running && sdkReady) loadDevices(); if (running) handler.postDelayed(this, 8000); }
    };

    @Override public void onCreate() {
        super.onCreate(); WatchSettings.prefs(this).registerOnSharedPreferenceChangeListener(settingsChanged);
    }
    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "remote").setContentTitle(Localized.get(this, R.string.notification_title))
                .setContentText(Localized.get(this, R.string.notification_body)).setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(open).setOngoing(true).build();
    }
    private void updateNotification() {
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel("remote", Localized.get(this, R.string.channel), NotificationManager.IMPORTANCE_LOW));
        notifications.notify(1, notification());
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (running) return START_NOT_STICKY;
        running = true; active = true; lastWatchSeen = 0;
        state = R.string.status_connecting; lastResult = UiMessage.empty();

        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel("remote", Localized.get(this, R.string.channel), NotificationManager.IMPORTANCE_LOW));
        startForeground(1, notification());
        try {
            sdk = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS);
            sdk.initialize(this, false, new ConnectIQ.ConnectIQListener() {
                @Override public void onSdkReady() { handler.post(() -> {
                    if (!running) return;
                    sdkReady = true;  handler.post(refresh);
                }); }
                @Override public void onInitializeError(ConnectIQ.IQSdkErrorStatus error) { handler.post(() -> {
                    if (!running) return;
                    state = R.string.sdk_unavailable;
                    lastResult = UiMessage.of(R.string.sdk_help);
                }); }
                @Override public void onSdkShutDown() { handler.post(() -> {
                    if (!running) return;
                    sdkReady = false; lastWatchSeen = 0; stopSeries(Protocol.CANCELLED); cancelCountdown(); sessions.clear(); state = R.string.sdk_closed;
                }); }
            });
        } catch (RuntimeException error) { state = R.string.sdk_failed; }
        return START_NOT_STICKY;
    }

    private void loadDevices() {
        try {
            List<IQDevice> devices = sdk.getKnownDevices();
            if (devices == null || devices.isEmpty()) { state = R.string.no_watch;  return; }
            for (IQDevice device : devices) {
                if (!listening.add(device.getDeviceIdentifier())) continue;
                try {
                    sdk.registerForDeviceEvents(device, (peer, status) -> handler.post(() -> {
                        if (!running) return;

                        if (status != IQDevice.IQDeviceStatus.CONNECTED) {
                            if (series != null && pendingDevice.getDeviceIdentifier() == peer.getDeviceIdentifier()) stopSeries(Protocol.CANCELLED);
                            if (pendingDevice != null && pendingDevice.getDeviceIdentifier() == peer.getDeviceIdentifier()) cancelCountdown();
                            sessions.remove(peer.getDeviceIdentifier());
                            if (sessions.isEmpty()) lastWatchSeen = 0;
                            state = R.string.watch_disconnected;
                        }
                    }));
                    sdk.registerForAppEvents(device, watchApp, (peer, app, messages, status) -> handler.post(() -> {
                        if (!running) return;

                        if (status != ConnectIQ.IQMessageStatus.SUCCESS || messages == null) return;
                        for (Object item : messages) if (item instanceof Map<?, ?> message) receive(peer, message);
                    }));

                    if (sessions.isEmpty()) state = R.string.watch_ready;
                } catch (Exception error) { listening.remove(device.getDeviceIdentifier()); state = R.string.watch_failed; }
            }
        } catch (Exception error) { state = R.string.sdk_no_reply;  }
    }

    private static String string(Map<?, ?> message, String key) { Object value = message.get(key); return value instanceof String ? (String) value : ""; }
    private static int number(Map<?, ?> message, String key) {
        Object value = message.get(key);
        if (!(value instanceof Number)) return -1;
        double numeric = ((Number) value).doubleValue();
        return numeric >= 0 && numeric <= Integer.MAX_VALUE && numeric == Math.rint(numeric) ? (int) numeric : -1;
    }
    private void receive(IQDevice device, Map<?, ?> message) {
        if (number(message, "v") != 2) {  return; }
        long deviceId = device.getDeviceIdentifier();
        long saved = getSharedPreferences("remote", MODE_PRIVATE).getLong("iqWatch", -1);
        if (saved != -1 && saved != deviceId) { lastResult = UiMessage.of(R.string.other_watch); return; }
        String type = string(message, "type");
        long now = SystemClock.elapsedRealtime();

        if ("hello".equals(type)) {
            String nonce = string(message, "nonce");
            if (nonce.isEmpty() || nonce.length() > 128 || cameraPending) return;
            CommandSession session = sessions.get(deviceId);
            if (session == null || !session.nonce.equals(nonce) || !session.touch(session.token, now)) {

                session = new CommandSession(nonce, UUID.randomUUID().toString(), now); sessions.put(deviceId, session);
            }
            getSharedPreferences("remote", MODE_PRIVATE).edit().putLong("iqWatch", deviceId).apply();
            lastWatchSeen = now;
            peers.put(deviceId, device);
            sendWithTicket(device, session, Map.of("v", 2, "type", "ready", "nonce", nonce, "session", session.token));
            state = R.string.watch_answered;
            return;
        }
        CommandSession session = sessions.get(deviceId);
        if (session == null || !session.touch(string(message, "session"), now)) {  return; }
        lastWatchSeen = now;
        if ("ping".equals(type)) {
            Map<String, Object> pong = new HashMap<>(Map.of("v", 2, "type", "pong", "session", session.token));
            if (message.get("rid") instanceof Number) pong.put("rid", message.get("rid"));
            sendWithTicket(device, session, pong); return;
        }
        int seq = number(message, "seq");
        if (seq <= 0) return;
        if ("cancel".equals(type)) {
            if (pendingSession == session && pendingSequence == seq && cameraPending) {
                if (series != null) { stopSeries(Protocol.CANCELLED); return; }
                if (pendingShot != null) cancelCountdown();
                // An already dispatched camera action cannot be recalled; return its actual result.
                return;
            }
            int cached = session.cached(seq);
            if (cached < 0) { session.cancelSequence(seq); cached = Protocol.CANCELLED; }
            result(device, session, seq, cached); return;
        }
        if ("prepare".equals(type)) {
            if (repeatPending(device, session, seq)) return;
            int cached = session.cached(seq);
            if (cached >= 0) { result(device, session, seq, cached); return; }
            String ticket = cameraPending ? null : session.arm(seq, UUID.randomUUID().toString(), now);
            if (ticket == null) { result(device, session, seq, Protocol.BUSY); return; }
            send(device, Map.of("v", 2, "type", "armed", "session", session.token, "seq", seq, "ticket", ticket));
        } else if ("shutter".equals(type)) {
            int delay = message.containsKey("delay") ? number(message, "delay") : 0;
            String mode = string(message, "mode");
            boolean repeat = "series".equals(mode);
            int interval = number(message, "interval");
            if ((!mode.isEmpty() && !"single".equals(mode) && !repeat)
                    || (repeat && (!SeriesRun.validInterval(interval) || delay != 0))) {
                result(device, session, seq, Protocol.FAILED); return;
            }
            if (!TimedShot.validDelay(delay)) { result(device, session, seq, Protocol.FAILED); return; }
            CommandSession.Decision decision = Boolean.TRUE.equals(message.get("fast"))
                    ? session.beginFast(seq, string(message, "ticket"), now) : session.begin(seq, string(message, "ticket"), now);
            if (decision == CommandSession.Decision.DUPLICATE) { result(device, session, seq, session.cached(seq)); return; }
            if (decision == CommandSession.Decision.BUSY && repeatPending(device, session, seq)) return;
            if (decision != CommandSession.Decision.EXECUTE) {
                result(device, session, seq, decision == CommandSession.Decision.BUSY ? Protocol.BUSY : Protocol.FAILED); return;
            }
            if (cameraPending) { session.finish(Protocol.BUSY); result(device, session, seq, Protocol.BUSY); return; }
            cameraPending = true;
            pendingDevice = device; pendingSession = session; pendingSequence = seq;
            if (repeat) { startSeries(device, session, seq, interval, now); return; }
            pendingDeadline = now + delay * 1000L;
            TimedShot shot = new TimedShot(delay, now); timedShot = shot;
            int currentGeneration = generation;
            pendingShot = () -> {
                pendingShot = null;
                if (!running || generation != currentGeneration) return;
                if (sessions.get(deviceId) != session || !shot.claim(SystemClock.elapsedRealtime())) {
                    finishShot(device, session, seq, Protocol.FAILED); return;
                }
                cameraAction.fire(code -> handler.post(() -> {
                    if (!running || generation != currentGeneration) return;

                    finishShot(device, session, seq, code);
                }));
            };
            if (delay > 0) {
                lastResult = UiMessage.of(R.string.timer_active, delay);
                sendScheduled(device, session, seq);
                handler.postDelayed(pendingShot, delay * 1000L);
            } else pendingShot.run();
        }
    }
    private boolean repeatPending(IQDevice device, CommandSession session, int seq) {
        if (!cameraPending || pendingSession != session || pendingSequence != seq) return false;
        if (series != null) { sendSeriesState(device, session, seq); return true; }
        if (pendingShot != null) sendScheduled(device, session, seq);
        return true;
    }
    private void startSeries(IQDevice device, CommandSession session, int seq, int interval, long now) {
        SeriesRun run = new SeriesRun(interval, now); series = run;
        int currentGeneration = generation;
        seriesTask = () -> {
            if (!running || generation != currentGeneration || series != run) return;
            long time = SystemClock.elapsedRealtime();
            if (sessions.get(device.getDeviceIdentifier()) != session || time - lastWatchSeen > 10000 || !run.claim(time)) {
                stopSeries(Protocol.FAILED); return;
            }
            cameraAction.fire(code -> handler.post(() -> {
                if (!running || generation != currentGeneration || series != run) return;
                if (code != Protocol.OK) { stopSeries(code); return; }
                if (!run.complete(SystemClock.elapsedRealtime())) return;
                sendSeriesState(device, session, seq);
                handler.postDelayed(seriesTask, interval * 1000L);
            }));
        };
        seriesGuard = new Runnable() {
            @Override public void run() {
                if (!running || generation != currentGeneration || series != run) return;
                long time = SystemClock.elapsedRealtime();
                if (time - lastWatchSeen > 10000 || run.stalled(time)) { stopSeries(Protocol.FAILED); return; }
                handler.postDelayed(this, 1000);
            }
        };
        sendSeriesState(device, session, seq);
        handler.postDelayed(seriesTask, interval * 1000L); handler.postDelayed(seriesGuard, 1000);
    }
    private void sendSeriesState(IQDevice device, CommandSession session, int seq) {
        if (series == null) return;
        lastResult = UiMessage.of(R.string.series_active, series.count(), series.interval);
        send(device, Map.of("v", 2, "type", "seriesState", "session", session.token, "seq", seq,
                "interval", series.interval, "count", series.count()));
    }
    private void stopSeries(int code) {
        if (series == null) return;
        series.stop(); series = null;
        if (seriesTask != null) handler.removeCallbacks(seriesTask);
        if (seriesGuard != null) handler.removeCallbacks(seriesGuard);
        seriesTask = null; seriesGuard = null;
        if (running) {
            finishShot(pendingDevice, pendingSession, pendingSequence, code);
            if (code == Protocol.CANCELLED) lastResult = UiMessage.of(R.string.series_stopped);
        }
        else {
            pendingSession.finish(code); cameraPending = false;
            pendingDevice = null; pendingSession = null;
        }
    }
    private void sendScheduled(IQDevice device, CommandSession session, int seq) {
        int millis = (int) Math.max(0, pendingDeadline - SystemClock.elapsedRealtime());
        send(device, Map.of("v", 2, "type", "scheduled", "session", session.token, "seq", seq,
                "remainingMs", millis, "delay", (millis + 999)/1000));
    }
    private void finishShot(IQDevice device, CommandSession session, int seq, int code) {
        cameraPending = false; pendingShot = null; timedShot = null; pendingDevice = null; pendingSession = null;
        session.finish(code); lastResult = Protocol.message(code);
        if (sessions.get(device.getDeviceIdentifier()) == session) result(device, session, seq, code);
    }
    private void cancelCountdown() {
        if (pendingShot == null || timedShot == null || !timedShot.cancel()) return;
        handler.removeCallbacks(pendingShot);
        finishShot(pendingDevice, pendingSession, pendingSequence, Protocol.CANCELLED);
    }
    private void result(IQDevice device, CommandSession session, int seq, int code) {
        sendWithTicket(device, session, Map.of("v", 2, "type", "result", "session", session.token, "seq", seq, "code", code));
    }
    private void sendWithTicket(IQDevice device, CommandSession session, Map<String, Object> message) {
        Map<String, Object> data = new HashMap<>(message);
        data.put("nextTicket", session.issueFast(UUID.randomUUID().toString(), SystemClock.elapsedRealtime()));
        data.put("ttl", CommandSession.FAST_TTL);
        data.put("timer", true);
        data.put("series", true);
        send(device, data);
    }
    private void send(IQDevice device, Map<String, Object> message) {
        try {
            Map<String, Object> data = new HashMap<>(message);
            data.put("settings", WatchSettings.snapshot(this));
            sdk.sendMessage(device, watchApp, data, (peer, app, status) -> handler.post(() -> {
                if (!running) return;

                if (status != ConnectIQ.IQMessageStatus.SUCCESS) state = R.string.reply_failed;
            }));
        } catch (Exception error) {  state = R.string.message_failed; }
    }
    @Override public void onDestroy() {
        WatchSettings.prefs(this).unregisterOnSharedPreferenceChangeListener(settingsChanged);
        running = false; active = false; lastWatchSeen = 0; generation++; stopSeries(Protocol.CANCELLED); handler.removeCallbacksAndMessages(null);
        if (sdk != null) { try { sdk.unregisterAllForEvents(); sdk.shutdown(this); } catch (Exception ignored) { } }
        sessions.clear(); peers.clear(); listening.clear(); state = R.string.status_stopped;  super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
