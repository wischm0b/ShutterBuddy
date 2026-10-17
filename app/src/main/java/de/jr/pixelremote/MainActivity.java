package de.jr.pixelremote;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView badge, connection, access, feedback;
    private Button primary, stop;
    private LinearLayout settings;
    private boolean settingsExpanded;
    private int page, surface, soft, ink, muted, accent, onAccent, outline;

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(Localized.context(base));
        String appearance = base.getSharedPreferences("appearance", MODE_PRIVATE).getString("theme", "dark");
        Configuration config = new Configuration();
        config.setLocale(Locale.forLanguageTag(WatchSettings.language(base)));
        if (!"system".equals(appearance)) {
            config.uiMode = (base.getResources().getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                    | ("light".equals(appearance) ? Configuration.UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES);
        }
        applyOverrideConfiguration(config);
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        page = getColor(R.color.page); surface = getColor(R.color.surface); soft = getColor(R.color.surface_soft);
        ink = getColor(R.color.ink); muted = getColor(R.color.ink_muted); accent = getColor(R.color.accent);
        onAccent = getColor(R.color.on_accent); outline = getColor(R.color.outline);
        settingsExpanded = saved != null && saved.getBoolean("settings");
        LinearLayout content = column(); content.setPadding(dp(24), dp(22), dp(24), dp(28));
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(page); scroll.setFillViewport(true);
        scroll.addView(content); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return android.view.WindowInsets.CONSUMED;
        });
        scroll.requestApplyInsets();

        LinearLayout brandRow = column(); brandRow.setOrientation(LinearLayout.HORIZONTAL); brandRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.brand_logo); logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(round(0xFF0C1210, 16, 0)); logo.setClipToOutline(true); logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        brandRow.addView(logo, new LinearLayout.LayoutParams(dp(64), dp(64)));
        TextView brand = text("SHUTTER BUDDY", 12, accent); brand.setLetterSpacing(.18f); brand.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams brandLayout = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        brandLayout.leftMargin = dp(18); brandRow.addView(brand, brandLayout); content.addView(brandRow);
        TextView heading = text(getString(R.string.hero), 34, ink);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); heading.setLineSpacing(dp(2), 1); add(content, heading, 18);
        TextView subtitle = text("Pixel 8 Pro + Fenix 8", 15, muted); add(content, subtitle, 12);

        LinearLayout card = column(); card.setPadding(dp(22), dp(22), dp(22), dp(22)); card.setBackground(round(surface, 24, outline)); add(content, card, 28);
        badge = text(getString(R.string.ready_badge), 12, accent); badge.setLetterSpacing(.08f); badge.setTypeface(null, Typeface.BOLD); card.addView(badge);
        TextView cardTitle = text(getString(R.string.card_title), 23, ink); cardTitle.setTypeface(null, Typeface.BOLD); add(card, cardTitle, 18);
        connection = text("", 14, muted); add(card, connection, 12);
        access = text("", 14, muted); add(card, access, 8);
        primary = button(card, getString(R.string.start_remote), true, v -> {
            if (CameraAccessibilityService.instance == null) openAccessSettings();
            else if (RemoteService.isRunning()) openCamera();
            else startRemote();
        });
        stop = button(card, getString(R.string.stop_remote), false, v -> endRemote(false));
        feedback = text("", 14, accent); feedback.setTextIsSelectable(true); add(card, feedback, 14);

        TextView usage = text(getString(R.string.how_to), 18, ink); usage.setTypeface(null, Typeface.BOLD); add(content, usage, 28);
        add(content, text(getString(R.string.usage), 15, muted), 12);
        TextView hint = text(getString(R.string.camera_hint), 13, muted); add(content, hint, 12);

        button(content, getString(R.string.settings), false, v -> {
            settingsExpanded = !settingsExpanded; settings.setVisibility(settingsExpanded ? View.VISIBLE : View.GONE);
        });
        settings = column(); settings.setVisibility(settingsExpanded ? View.VISIBLE : View.GONE); add(content, settings, 14);
        settings.setPadding(dp(18), dp(18), dp(18), dp(18)); settings.setBackground(round(surface, 20, outline));
        addAppearance(settings);
        addWatchSettings(settings);
        TextView privacy = text(getString(R.string.access_title), 18, ink); privacy.setTypeface(null, Typeface.BOLD); add(settings, privacy, 24);
        add(settings, text(getString(R.string.access_help), 13, muted), 8);
        Switch disable = new Switch(this); disable.setText(getString(R.string.disable_stop)); disable.setTextSize(14); disable.setTextColor(ink);
        disable.setChecked(getSharedPreferences("appearance", MODE_PRIVATE).getBoolean("disableOnStop", false));
        disable.setOnCheckedChangeListener((view, checked) -> getSharedPreferences("appearance", MODE_PRIVATE).edit().putBoolean("disableOnStop", checked).apply()); add(settings, disable, 16);
        add(settings, text(getString(R.string.access_again), 12, muted), 8);
        button(settings, getString(R.string.manage_access), false, v -> openAccessSettings());
        button(settings, getString(R.string.fully_disable), false, v -> endRemote(true));
        button(settings, getString(R.string.switch_watch), false, v -> {
            stopService(new Intent(this, RemoteService.class));
            getSharedPreferences("remote", MODE_PRIVATE).edit().remove("iqWatch").apply();
            RemoteService.lastResult = UiMessage.of(R.string.switch_watch_hint);
        });
        TextView version = text("SHUTTER BUDDY  /  0.11.0", 10, muted); version.setLetterSpacing(.12f); version.setGravity(Gravity.CENTER); add(content, version, 28);
        update();
    }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setLineSpacing(dp(3), 1); return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke); return drawable;
    }
    private void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.topMargin = dp(top); parent.addView(child, layout);
    }
    private Button button(LinearLayout parent, String label, boolean prominent, View.OnClickListener click) {
        Button view = new Button(this); view.setText(label); view.setTextSize(15); view.setAllCaps(false);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); view.setTextColor(prominent ? onAccent : ink);
        view.setMinHeight(dp(52)); view.setPadding(dp(14), dp(13), dp(14), dp(13));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3090CDAA), round(prominent ? accent : soft, 16, 0), null));
        view.setOnClickListener(click); add(parent, view, prominent ? 22 : 10); return view;
    }
    private void addAppearance(LinearLayout parent) {
        TextView title = text(getString(R.string.appearance), 18, ink); title.setTypeface(null, Typeface.BOLD); parent.addView(title);
        RadioGroup modes = new RadioGroup(this); modes.setOrientation(RadioGroup.HORIZONTAL);
        String[] values = {"dark", "light", "system"}; String[] labels = {getString(R.string.dark), getString(R.string.light), getString(R.string.system)};
        String selected = getSharedPreferences("appearance", MODE_PRIVATE).getString("theme", "dark");
        for (int i = 0; i < values.length; i++) {
            RadioButton option = new RadioButton(this); option.setId(View.generateViewId()); option.setText(labels[i]);
            option.setTextColor(ink); option.setTextSize(13); option.setTag(values[i]);
            modes.addView(option, new RadioGroup.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            if (values[i].equals(selected)) modes.check(option.getId());
        }
        modes.setOnCheckedChangeListener((group, id) -> {
            View selectedView = group.findViewById(id); if (selectedView == null) return;
            String choice = (String) selectedView.getTag();
            if (choice.equals(getSharedPreferences("appearance", MODE_PRIVATE).getString("theme", "dark"))) return;
            getSharedPreferences("appearance", MODE_PRIVATE).edit().putString("theme", choice).apply(); recreate();
        }); add(parent, modes, 12);
    }
    private final Runnable refresh = new Runnable() { @Override public void run() { update(); handler.postDelayed(this, 1000); } };
    private void update() {
        boolean enabled = CameraAccessibilityService.instance != null;
        boolean running = RemoteService.isRunning(); boolean linked = RemoteService.hasWatchLink();
        badge.setText(!enabled ? getString(R.string.setup_badge) : linked ? getString(R.string.linked_badge) : running ? getString(R.string.waiting_badge) : getString(R.string.ready_badge));
        connection.setText(running ? getString(RemoteService.state) : getString(R.string.connect_help));
        access.setText(enabled ? (running ? getString(R.string.access_active) : getString(R.string.access_idle)) : getString(R.string.access_off));
        primary.setText(!enabled ? getString(R.string.setup_access) : running ? getString(R.string.open_camera) : getString(R.string.start_remote));
        stop.setVisibility(running ? View.VISIBLE : View.GONE);
        feedback.setText(RemoteService.lastResult.render(this)); feedback.setVisibility(RemoteService.lastResult.isEmpty() ? View.GONE : View.VISIBLE);
    }
    private void endRemote(boolean forceDisable) {
        stopService(new Intent(this, RemoteService.class));
        boolean disable = forceDisable || getSharedPreferences("appearance", MODE_PRIVATE).getBoolean("disableOnStop", false);
        if (disable) CameraAccessibilityService.turnOff();
        RemoteService.lastResult = UiMessage.of(disable ? R.string.stopped_access : R.string.remote_stopped); update();
    }
    private void openAccessSettings() { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
    private void addWatchSettings(LinearLayout parent) {
        TextView language = text(getString(R.string.language), 18, ink); language.setTypeface(null, Typeface.BOLD); add(parent, language, 24);
        RadioGroup languages = new RadioGroup(this); languages.setOrientation(RadioGroup.HORIZONTAL);
        for (String value : new String[]{"en", "de"}) {
            RadioButton option = new RadioButton(this); option.setId(View.generateViewId()); option.setTag(value);
            option.setText("de".equals(value) ? "Deutsch" : "English"); option.setTextColor(ink);
            languages.addView(option, new RadioGroup.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            if (value.equals(WatchSettings.language(this))) languages.check(option.getId());
        }
        languages.setOnCheckedChangeListener((group, id) -> {
            View selected = group.findViewById(id); if (selected == null) return;
            String value = (String) selected.getTag();
            if (!value.equals(WatchSettings.language(this))) { WatchSettings.saveLanguage(this, value); recreate(); }
        }); add(parent, languages, 12);
        TextView title = text(getString(R.string.watch_feedback), 18, ink); title.setTypeface(null, Typeface.BOLD); add(parent, title, 24);
        addWatchSwitch(parent, R.string.shutter_vibration, "vibrate");
        addWatchSwitch(parent, R.string.shutter_tone, "tone");
        addWatchSwitch(parent, R.string.countdown_vibration, "countdownVibrate");
        add(parent, text(getString(R.string.feedback_help), 12, muted), 12);
        add(parent, text(getString(R.string.sync_help), 12, muted), 8);
    }
    private void addWatchSwitch(LinearLayout parent, int label, String key) {
        Switch option = new Switch(this); option.setText(label); option.setTextSize(14); option.setTextColor(ink);
        option.setMinHeight(dp(48)); option.setChecked(WatchSettings.enabled(this, key));
        option.setOnCheckedChangeListener((view, checked) -> WatchSettings.save(this, key, checked)); add(parent, option, 12);
    }
    private void startRemote() {
        ArrayList<String> permissions = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!permissions.isEmpty()) { requestPermissions(permissions.toArray(new String[0]), 1); return; }
        startForegroundService(new Intent(this, RemoteService.class));
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 1 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) startForegroundService(new Intent(this, RemoteService.class));
    }
    private void openCamera() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(CameraAccessibilityService.CAMERA);
        if (intent == null) { RemoteService.lastResult = UiMessage.of(R.string.camera_missing); update(); return; }
        startActivity(intent);
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("settings", settingsExpanded); super.onSaveInstanceState(state); }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
