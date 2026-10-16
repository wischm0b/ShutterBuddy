package de.jr.pixelremote;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;
import java.util.function.IntConsumer;

public final class CameraAccessibilityService extends AccessibilityService {
    static final String CAMERA = "com.google.android.GoogleCamera";
    static volatile CameraAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean busy;
    private long lastTrigger;
    @Override protected void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() { instance = null; super.onDestroy(); }

    static void turnOff() {
        CameraAccessibilityService service = instance;
        if (service == null) return;
        service.disableSelf();
        instance = null;
    }

    static void trigger(IntConsumer done) {
        CameraAccessibilityService service = instance;
        if (service == null) { done.accept(Protocol.NO_ACCESS); return; }
        service.handler.post(() -> service.fire(done));
    }

    private void fire(IntConsumer done) {
        if (busy || SystemClock.elapsedRealtime() - lastTrigger < 900) { done.accept(Protocol.BUSY); return; }
        KeyguardManager lock = getSystemService(KeyguardManager.class);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (lock.isKeyguardLocked() || root == null || root.getPackageName() == null || !CAMERA.contentEquals(root.getPackageName())) {
            done.accept(Protocol.NO_CAMERA); return;
        }
        AccessibilityNodeInfo shutter = findShutter(root);
        if (shutter == null) { done.accept(Protocol.NO_SHUTTER); return; }
        lastTrigger = SystemClock.elapsedRealtime();
        if (shutter.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { done.accept(Protocol.OK); return; }
        // Tap only the bounds of a positively identified shutter; never guess screen coordinates.
        Rect bounds = new Rect();
        shutter.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) { done.accept(Protocol.FAILED); return; }
        Path path = new Path(); path.moveTo(bounds.exactCenterX(), bounds.exactCenterY());
        busy = true;
        boolean accepted = dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 60)).build(),
                new GestureResultCallback() {
                    @Override public void onCompleted(GestureDescription gesture) { busy = false; done.accept(Protocol.OK); }
                    @Override public void onCancelled(GestureDescription gesture) { busy = false; done.accept(Protocol.FAILED); }
                }, handler);
        if (!accepted) { busy = false; done.accept(Protocol.FAILED); }
    }

    static AccessibilityNodeInfo findShutter(AccessibilityNodeInfo node) {
        if (node == null) return null;
        String id = node.getViewIdResourceName();
        String desc = node.getContentDescription() == null ? "" : node.getContentDescription().toString().trim().toLowerCase(Locale.ROOT);
        // The camera may reuse the shutter resource ID in video mode.
        boolean video = desc.contains("video") || desc.contains("record") || desc.contains("aufnahme stoppen");
        boolean exactId = (CAMERA + ":id/shutter_button").equals(id) || (CAMERA + ":id/shutter").equals(id);
        boolean exactDesc = desc.equals("shutter") || desc.equals("shutter button") || desc.equals("take photo")
                || desc.equals("take picture") || desc.equals("capture photo") || desc.equals("auslöser")
                || desc.equals("foto aufnehmen") || desc.equals("bild aufnehmen");
        if (node.isVisibleToUser() && node.isEnabled() && !video && (exactId || exactDesc)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findShutter(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

}
