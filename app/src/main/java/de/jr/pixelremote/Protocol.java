package de.jr.pixelremote;

final class Protocol {
    static final int OK = 0, NO_ACCESS = 1, NO_CAMERA = 2, NO_SHUTTER = 3, BUSY = 4, FAILED = 5, CANCELLED = 6;
    static UiMessage message(int result) {
        return switch (result) {
            case OK -> UiMessage.of(R.string.photo_ok);
            case NO_ACCESS -> UiMessage.of(R.string.photo_access);
            case NO_CAMERA -> UiMessage.of(R.string.photo_camera);
            case NO_SHUTTER -> UiMessage.of(R.string.photo_mode);
            case BUSY -> UiMessage.of(R.string.photo_busy);
            case CANCELLED -> UiMessage.of(R.string.photo_cancelled);
            default -> UiMessage.of(R.string.photo_failed);
        };
    }
}
