package de.jr.pixelremote;

import android.content.Context;

/** Resolve stored service feedback in the language currently selected on the phone. */
final class UiMessage {
    private final int resource;
    private final Object[] args;
    private UiMessage(int resource, Object... args) { this.resource = resource; this.args = args; }
    static UiMessage of(int resource, Object... args) { return new UiMessage(resource, args); }
    static UiMessage empty() { return new UiMessage(0); }
    boolean isEmpty() { return resource == 0; }
    String render(Context context) { return isEmpty() ? "" : Localized.context(context).getString(resource, args); }
}
