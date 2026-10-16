package de.jr.pixelremote;

final class Protocol {
    static final int OK = 0, NO_ACCESS = 1, NO_CAMERA = 2, NO_SHUTTER = 3, BUSY = 4, FAILED = 5, CANCELLED = 6;
    static String message(int result) {
        return switch (result) {
            case OK -> "Auslöser betätigt (Aufnahme bitte in der Kamera prüfen).";
            case NO_ACCESS -> "Bedienungshilfe ist nicht aktiv.";
            case NO_CAMERA -> "Pixel-Kamera muss sichtbar und das Handy entsperrt sein.";
            case NO_SHUTTER -> "Bitte Fotomodus wählen und die Pixel-Kamera erneut öffnen.";
            case BUSY -> "Auslöser ist noch beschäftigt.";
            case CANCELLED -> "Selbstauslöser abgebrochen.";
            default -> "Auslösen fehlgeschlagen.";
        };
    }
}
