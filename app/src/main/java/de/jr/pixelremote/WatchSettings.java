package de.jr.pixelremote;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

final class WatchSettings {
    static SharedPreferences prefs(Context context) { return context.getSharedPreferences("watchSettings", Context.MODE_PRIVATE); }
    static String language(Context context) { return "de".equals(prefs(context).getString("language", "en")) ? "de" : "en"; }
    static boolean enabled(Context context, String key) { return prefs(context).getBoolean(key, !"tone".equals(key)); }
    static void save(Context context, String key, boolean value) {
        SharedPreferences p = prefs(context);
        p.edit().putBoolean(key, value).putInt("revision", p.getInt("revision", 0) + 1).apply();
    }
    static void saveLanguage(Context context, String language) {
        SharedPreferences p = prefs(context);
        p.edit().putString("language", "de".equals(language) ? "de" : "en").putInt("revision", p.getInt("revision", 0) + 1).apply();
    }
    static Map<String, Object> snapshot(Context context) {
        return Map.of("language", language(context), "vibrate", enabled(context, "vibrate"),
                "tone", enabled(context, "tone"), "countdownVibrate", enabled(context, "countdownVibrate"),
                "revision", prefs(context).getInt("revision", 0));
    }
}
