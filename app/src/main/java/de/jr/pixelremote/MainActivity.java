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

public final class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView badge, connection, access, feedback;
    private Button primary, stop;
    private LinearLayout settings;
    private boolean settingsExpanded;
    private int page, surface, soft, ink, muted, accent, onAccent, outline;

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        String appearance = base.getSharedPreferences("appearance", MODE_PRIVATE).getString("theme", "dark");
        if (!"system".equals(appearance)) {
            Configuration config = new Configuration();
            config.uiMode = (base.getResources().getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                    | ("light".equals(appearance) ? Configuration.UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES);
            applyOverrideConfiguration(config);
        }
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
        TextView heading = text("Deine Kamera.\nAm Handgelenk.", 34, ink);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); heading.setLineSpacing(dp(2), 1); add(content, heading, 18);
        TextView subtitle = text("Pixel 8 Pro + Fenix 8", 15, muted); add(content, subtitle, 12);

        LinearLayout card = column(); card.setPadding(dp(22), dp(22), dp(22), dp(22)); card.setBackground(round(surface, 24, outline)); add(content, card, 28);
        badge = text("●  BEREIT ZUM START", 12, accent); badge.setLetterSpacing(.08f); badge.setTypeface(null, Typeface.BOLD); card.addView(badge);
        TextView cardTitle = text("Verbunden fotografieren", 23, ink); cardTitle.setTypeface(null, Typeface.BOLD); add(card, cardTitle, 18);
        connection = text("", 14, muted); add(card, connection, 12);
        access = text("", 14, muted); add(card, access, 8);
        primary = button(card, "Fernbedienung starten", true, v -> {
            if (CameraAccessibilityService.instance == null) openAccessSettings();
            else if (RemoteService.isRunning()) openCamera();
            else startRemote();
        });
        stop = button(card, "Fernbedienung beenden", false, v -> endRemote(false));
        feedback = text("", 14, accent); feedback.setTextIsSelectable(true); add(card, feedback, 14);

        TextView usage = text("So geht’s", 18, ink); usage.setTypeface(null, Typeface.BOLD); add(content, usage, 28);
        add(content, text("01   Fernbedienung starten\n02   Shutter Buddy auf der Uhr öffnen\n03   Pixel-Kamera öffnen · START drücken", 15, muted), 12);
        TextView hint = text("Fotomodus wählen. Handy entsperrt und Display eingeschaltet lassen.", 13, muted); add(content, hint, 12);

        button(content, "Einstellungen", false, v -> {
            settingsExpanded = !settingsExpanded; settings.setVisibility(settingsExpanded ? View.VISIBLE : View.GONE);
        });
        settings = column(); settings.setVisibility(settingsExpanded ? View.VISIBLE : View.GONE); add(content, settings, 14);
        settings.setPadding(dp(18), dp(18), dp(18), dp(18)); settings.setBackground(round(surface, 20, outline));
        addAppearance(settings);
        TextView privacy = text("Bedienungshilfe", 18, ink); privacy.setTypeface(null, Typeface.BOLD); add(settings, privacy, 24);
        add(settings, text("Zum Auslösen in der Pixel-Kamera nötig. Ohne Befehl liest der Dienst keine Bedienelemente aus.", 13, muted), 8);
        Switch disable = new Switch(this); disable.setText("Beim Beenden deaktivieren"); disable.setTextSize(14); disable.setTextColor(ink);
        disable.setChecked(getSharedPreferences("appearance", MODE_PRIVATE).getBoolean("disableOnStop", false));
        disable.setOnCheckedChangeListener((view, checked) -> getSharedPreferences("appearance", MODE_PRIVATE).edit().putBoolean("disableOnStop", checked).apply()); add(settings, disable, 16);
        add(settings, text("Danach musst du sie vor der nächsten Nutzung in Android selbst wieder einschalten.", 12, muted), 8);
        button(settings, "Bedienungshilfe verwalten", false, v -> openAccessSettings());
        button(settings, "Jetzt vollständig deaktivieren", false, v -> endRemote(true));
        button(settings, "Andere Uhr verwenden", false, v -> {
            stopService(new Intent(this, RemoteService.class));
            getSharedPreferences("remote", MODE_PRIVATE).edit().remove("iqWatch").apply();
            RemoteService.lastResult = "Bereit für eine andere Uhr. Fernbedienung erneut starten.";
        });
        TextView version = text("SHUTTER BUDDY  /  0.8.1", 10, muted); version.setLetterSpacing(.12f); version.setGravity(Gravity.CENTER); add(content, version, 28);
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
        TextView title = text("Darstellung", 18, ink); title.setTypeface(null, Typeface.BOLD); parent.addView(title);
        RadioGroup modes = new RadioGroup(this); modes.setOrientation(RadioGroup.HORIZONTAL);
        String[] values = {"dark", "light", "system"}; String[] labels = {"Dunkel", "Hell", "System"};
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
        badge.setText(!enabled ? "●  EINRICHTUNG NÖTIG" : linked ? "●  UHR ERREICHBAR" : running ? "●  WARTE AUF DIE UHR" : "●  BEREIT ZUM START");
        connection.setText(running ? RemoteService.state : "Garmin Connect verbindet Handy und Uhr.");
        access.setText(enabled ? (running ? "Kamera-Bedienung ist freigegeben." : "Bedienungshilfe freigegeben · wartet auf einen Befehl.") : "Bedienungshilfe ist ausgeschaltet.");
        primary.setText(!enabled ? "Bedienungshilfe einrichten" : running ? "Pixel-Kamera öffnen" : "Fernbedienung starten");
        stop.setVisibility(running ? View.VISIBLE : View.GONE);
        feedback.setText(RemoteService.lastResult); feedback.setVisibility(RemoteService.lastResult.isEmpty() ? View.GONE : View.VISIBLE);
    }
    private void endRemote(boolean forceDisable) {
        stopService(new Intent(this, RemoteService.class));
        boolean disable = forceDisable || getSharedPreferences("appearance", MODE_PRIVATE).getBoolean("disableOnStop", false);
        if (disable) CameraAccessibilityService.turnOff();
        RemoteService.lastResult = disable ? "Beendet. Bedienungshilfe ist ausgeschaltet." : "Fernbedienung beendet."; update();
    }
    private void openAccessSettings() { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
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
        if (intent == null) { RemoteService.lastResult = "Pixel-Kamera nicht gefunden."; update(); return; }
        startActivity(intent);
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("settings", settingsExpanded); super.onSaveInstanceState(state); }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
