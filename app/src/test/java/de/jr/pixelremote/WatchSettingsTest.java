package de.jr.pixelremote;

import android.content.Context;
import android.content.res.Configuration;
import org.robolectric.RuntimeEnvironment;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WatchSettingsTest {
    private Context context;
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication(); WatchSettings.prefs(context).edit().clear().commit();
    }
    @Test public void englishIsDefaultEvenOnGermanPhone() {
        Configuration config = new Configuration(context.getResources().getConfiguration()); config.setLocale(Locale.GERMAN);
        Context germanPhone = context.createConfigurationContext(config);
        assertEquals("Settings", Localized.get(germanPhone, R.string.settings));
        assertEquals("en", WatchSettings.snapshot(context).get("language"));
    }
    @Test public void changingLanguageRendersExistingServiceMessagesImmediately() {
        UiMessage message = Protocol.message(Protocol.NO_ACCESS);
        assertEquals("Accessibility is not enabled.", message.render(context));
        WatchSettings.saveLanguage(context, "de");
        assertEquals("Bedienungshilfe ist nicht aktiv.", message.render(context));
        WatchSettings.saveLanguage(context, "en");
        assertEquals("Accessibility is not enabled.", message.render(context));
    }
    @Test public void preferencesAreIndependentAndSnapshotHasRevision() {
        assertEquals(true, WatchSettings.snapshot(context).get("vibrate"));
        assertEquals(false, WatchSettings.snapshot(context).get("tone"));
        assertEquals(true, WatchSettings.snapshot(context).get("countdownVibrate"));
        WatchSettings.save(context, "tone", true); WatchSettings.save(context, "vibrate", false);
        assertEquals(false, WatchSettings.snapshot(context).get("vibrate"));
        assertEquals(true, WatchSettings.snapshot(context).get("tone"));
        assertEquals(true, WatchSettings.snapshot(context).get("countdownVibrate"));
        assertEquals(2, WatchSettings.snapshot(context).get("revision"));
    }
    @Test public void activityUsesSelectedLanguageAndSettingsAreAccessible() {
        MainActivity english = Robolectric.buildActivity(MainActivity.class).create().get();
        assertEquals("Settings", english.getString(R.string.settings));
        WatchSettings.saveLanguage(context, "de");
        MainActivity german = Robolectric.buildActivity(MainActivity.class).create().get();
        assertEquals("Einstellungen", german.getString(R.string.settings));
    }
}
