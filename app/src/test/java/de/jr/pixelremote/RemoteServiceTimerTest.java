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
