package de.jr.pixelremote;

import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;

final class Localized {
    static Context context(Context base) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(WatchSettings.language(base)));
        return base.createConfigurationContext(config);
    }
    static String get(Context base, int id) { return context(base).getString(id); }
}
