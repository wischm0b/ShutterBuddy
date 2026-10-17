package de.jr.pixelremote;

import android.os.Looper;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class RemoteServiceTimerTest {
    private RemoteService service;
    private FakeSdk sdk;
    private final IQDevice device = new IQDevice(42, "Test watch");
    private Map<String, Object> shutter;
    @Before public void setup() throws Exception {
        service = Robolectric.buildService(RemoteService.class).create().get(); sdk = new FakeSdk();
        ReflectionHelpers.setField(service, "sdk", sdk); ReflectionHelpers.setField(service, "running", true);
        service.getSharedPreferences("remote", 0).edit().clear().commit();
        receive(Map.of("v", 2, "type", "hello", "nonce", "test"));
        Map<?, ?> ready = sdk.messages.get(0);
        shutter = new HashMap<>(Map.of("v", 2, "type", "shutter", "session", ready.get("session"),
                "seq", 1, "ticket", ready.get("nextTicket"), "fast", true, "delay", 3));
    }
    @After public void teardown() { service.onDestroy(); }
    private void receive(Map<?, ?> message) throws Exception {
        Method method = RemoteService.class.getDeclaredMethod("receive", IQDevice.class, Map.class);
        method.setAccessible(true); method.invoke(service, device, message);
    }
    private long results() { return sdk.messages.stream().filter(message -> "result".equals(message.get("type"))).count(); }
    private Map<?, ?> last() { return sdk.messages.get(sdk.messages.size() - 1); }
    @Test public void helloAndHeartbeatCarryLatestPhoneSettings() throws Exception {
        Map<?, ?> initial = (Map<?, ?>) sdk.messages.get(0).get("settings");
        assertEquals("en", initial.get("language")); assertEquals(false, initial.get("tone"));
        WatchSettings.saveLanguage(service, "de"); WatchSettings.save(service, "tone", true);
        receive(Map.of("v", 2, "type", "ping", "session", shutter.get("session"), "rid", 1));
        Map<?, ?> settings = (Map<?, ?>) last().get("settings");
        assertEquals("de", settings.get("language")); assertEquals(true, settings.get("tone"));
        assertEquals(2, settings.get("revision"));
    }
    @Test public void phoneChangePushesSettingsWithoutReconnectingOrStartingPhoto() {
        WatchSettings.save(service, "countdownVibrate", false);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("settings", last().get("type"));
        assertEquals(shutter.get("session"), last().get("session"));
        assertEquals(false, ((Map<?, ?>) last().get("settings")).get("countdownVibrate"));
        assertEquals(0, results()); assertFalse(ReflectionHelpers.getField(service, "cameraPending"));
    }
    private void series(int interval) {
        shutter.put("mode", "series"); shutter.put("delay", 0); shutter.put("interval", interval);
    }
    private void cancel() throws Exception {
        receive(Map.of("v", 2, "type", "cancel", "session", shutter.get("session"), "seq", 1));
    }
    @Test public void seriesStartsAfterIntervalAndDuplicateDoesNotStartAnotherLoop() throws Exception {
        int[] photos = {0};
        ReflectionHelpers.setField(service, "cameraAction", (RemoteService.CameraAction) done -> { photos[0]++; done.accept(Protocol.OK); });
        series(2); receive(shutter); receive(shutter);
        assertEquals("seriesState", last().get("type")); assertEquals(0, last().get("count"));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1999)); assertEquals(0, photos[0]);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1)); assertEquals(1, photos[0]);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2)); assertEquals(2, photos[0]);
        cancel(); assertEquals(Protocol.CANCELLED, last().get("code"));
        receive(shutter); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6)); assertEquals(2, photos[0]);
    }
    @Test public void stopBeforeFirstPhotoCancelsSeries() throws Exception {
        int[] photos = {0};
        ReflectionHelpers.setField(service, "cameraAction", (RemoteService.CameraAction) done -> { photos[0]++; done.accept(Protocol.OK); });
        series(3); receive(shutter); cancel();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6)); assertEquals(0, photos[0]);
    }
    @Test public void delayedCameraCompletionAfterStopCannotRestartSeries() throws Exception {
        java.util.concurrent.atomic.AtomicReference<java.util.function.IntConsumer> completion = new java.util.concurrent.atomic.AtomicReference<>();
        int[] photos = {0};
        ReflectionHelpers.setField(service, "cameraAction", (RemoteService.CameraAction) done -> { photos[0]++; completion.set(done); });
        series(1); receive(shutter); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        cancel(); completion.get().accept(Protocol.OK);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6)); assertEquals(1, photos[0]);
        assertEquals(Protocol.CANCELLED, last().get("code"));
    }
    @Test public void cameraFailureEndsSeriesImmediately() throws Exception {
        int[] photos = {0};
        ReflectionHelpers.setField(service, "cameraAction", (RemoteService.CameraAction) done -> { photos[0]++; done.accept(Protocol.NO_CAMERA); });
        series(1); receive(shutter); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));
        assertEquals(1, photos[0]); assertEquals(Protocol.NO_CAMERA, last().get("code"));
    }
    @Test public void missingWatchHeartbeatEndsSeries() throws Exception {
        int[] photos = {0};
        ReflectionHelpers.setField(service, "cameraAction", (RemoteService.CameraAction) done -> { photos[0]++; done.accept(Protocol.OK); });
        series(2); receive(shutter); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(15));
        assertEquals(5, photos[0]); assertEquals(Protocol.FAILED, last().get("code"));
    }
    @Test public void heartbeatKeepsLongSeriesAlive() throws Exception {
        int[] photos = {0};
        ReflectionHelpers.setField(service, "cameraAction", (RemoteService.CameraAction) done -> { photos[0]++; done.accept(Protocol.OK); });
        series(2); receive(shutter);
        for (int i = 0; i < 6; i++) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
            receive(Map.of("v", 2, "type", "ping", "session", shutter.get("session"), "rid", i));
        }
        assertEquals(9, photos[0]); assertEquals(0, results()); cancel();
    }
    @Test public void invalidIntervalDoesNotDispatchCamera() throws Exception {
        series(0); receive(shutter); assertEquals(Protocol.FAILED, last().get("code"));
        series(6); receive(shutter); assertEquals(Protocol.FAILED, last().get("code"));
        assertFalse(ReflectionHelpers.getField(service, "cameraPending"));
    }
    @Test public void duplicateCommandKeepsCountdownAndReturnsNoEarlyBusyResult() throws Exception {
        receive(shutter); receive(shutter);
        assertEquals("scheduled", last().get("type")); assertEquals(0, results());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2)); assertEquals(0, results());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(1, results());
        // No accessibility service in this test: camera action is attempted only at the deadline.
        assertEquals(Protocol.NO_ACCESS, last().get("code"));
    }
    @Test public void cancellationStopsScheduledAction() throws Exception {
        receive(shutter);
        receive(Map.of("v", 2, "type", "cancel", "session", shutter.get("session"), "seq", 1));
        assertEquals(Protocol.CANCELLED, last().get("code")); assertEquals(1, results());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5)); assertEquals(1, results());
    }
    @Test public void repeatedPrepareCannotTurnActiveCountdownIntoBusyFailure() throws Exception {
        receive(shutter);
        receive(Map.of("v", 2, "type", "prepare", "session", shutter.get("session"), "seq", 1));
        assertEquals("scheduled", last().get("type")); assertEquals(0, results());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3)); assertEquals(1, results());
    }
    @Test public void removedPreviewCommandCannotScheduleAPhotoEvenWithAValidTicket() throws Exception {
        Map<String, Object> oldCommand = new HashMap<>(shutter);
        oldCommand.put("type", "preview"); oldCommand.put("pid", 1); oldCommand.put("delay", 0);
        int messages = sdk.messages.size();
        receive(oldCommand);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));
        assertEquals(messages, sdk.messages.size());
        assertEquals(0, results());
        assertFalse(ReflectionHelpers.getField(service, "cameraPending"));
    }
    private static final class FakeSdk extends ConnectIQ {
        final List<Map<?, ?>> messages = new ArrayList<>();
        @Override public List<IQDevice> getConnectedDevices() { return List.of(); }
        @Override public List<IQDevice> getKnownDevices() { return List.of(); }
        @Override public IQDevice.IQDeviceStatus getDeviceStatus(IQDevice device) { return IQDevice.IQDeviceStatus.CONNECTED; }
        @Override public void sendMessage(IQDevice device, IQApp app, Object message, IQSendMessageListener listener) { messages.add(new HashMap<>((Map<?, ?>) message)); }
        @Override protected void sendMessageProtocol(IQDevice device, IQApp app, byte[] data, IQSendMessageListener listener, boolean flag) { throw new UnsupportedOperationException(); }
        @Override protected void sendImageProtocol(IQDevice device, IQApp app, byte[] data, IQSendImageListener listener) { throw new UnsupportedOperationException(); }
        @Override protected void registerForRemoteAppEvents(IQDevice device, IQApp app) { }
    }
}
