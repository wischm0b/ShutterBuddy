package de.jr.pixelremote;

import android.view.accessibility.AccessibilityNodeInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class CameraSafetyTest {
    @Test public void accessibilityConfigurationDoesNotRequestScreenshotAccess() throws Exception {
        try (android.content.res.XmlResourceParser parser = org.robolectric.RuntimeEnvironment.getApplication().getResources().getXml(R.xml.accessibility)) {
            while (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) parser.next();
            assertEquals("accessibility-service", parser.getName());
            assertFalse(parser.getAttributeBooleanValue("http://schemas.android.com/apk/res/android", "canTakeScreenshot", false));
        }
    }
    private AccessibilityNodeInfo node(String id, String description, boolean enabled) {
        AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
        node.setViewIdResourceName(id); node.setContentDescription(description);
        node.setVisibleToUser(true); node.setEnabled(enabled);
        return node;
    }
    @Test public void identifiesGermanAndEnglishPhotoControls() {
        assertNotNull(CameraAccessibilityService.findShutter(node(null, "Foto aufnehmen", true)));
        assertNotNull(CameraAccessibilityService.findShutter(node(null, "Take photo", true)));
        assertNotNull(CameraAccessibilityService.findShutter(node(CameraAccessibilityService.CAMERA + ":id/shutter_button", "Shutter", true)));
    }
    @Test public void rejectsGalleryLensAndDisabledControls() {
        assertNull(CameraAccessibilityService.findShutter(node(null, "Letztes Foto ansehen", true)));
        assertNull(CameraAccessibilityService.findShutter(node(null, "Switch camera", true)));
        assertNull(CameraAccessibilityService.findShutter(node(null, "Foto aufnehmen", false)));
    }
    @Test public void rejectsKnownVideoControlEvenWithReusedShutterId() {
        assertNull(CameraAccessibilityService.findShutter(node(CameraAccessibilityService.CAMERA + ":id/shutter_button", "Start video recording", true)));
        assertNull(CameraAccessibilityService.findShutter(node(null, "Video aufnehmen", true)));
    }
    @Test public void ignoresInvisibleShutter() {
        AccessibilityNodeInfo hidden = node(null, "Take photo", true); hidden.setVisibleToUser(false);
        assertNull(CameraAccessibilityService.findShutter(hidden));
    }
}
