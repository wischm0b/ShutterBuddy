import Toybox.Graphics;
import Toybox.Lang;
import Toybox.Math;
import Toybox.WatchUi;

class RemoteView extends WatchUi.View {
    var remote;
    var logo;
    var width = 454;
    var height = 454;
    const BG = 0x0C1210;
    const MINT = 0x9EE8C2;
    const DIM = 0x657B6D;
    const AMBER = 0xF4C66A;
    const RED = 0xF28B82;
    function initialize(model) {
        View.initialize(); remote = model;
        logo = WatchUi.loadResource(Rez.Drawables.ShutterLogo);
    }
    function onUpdate(dc) {
        var w = dc.getWidth(); var h = dc.getHeight(); width = w; height = h;
        var cx = w/2; var cy = h*0.49;
        dc.setColor(MINT, BG); dc.clear();
        // Exact selected artwork; no transformed or offscreen animation bitmap.
        dc.drawBitmap(cx - logo.getWidth()/2, cy - logo.getHeight()/2, logo);
        if (remote.apertureFrame >= 0) { drawIris(dc, cx, cy - 4, remote.apertureFrame); }
        if (remote.remaining > 0) {
            var number = remote.remaining.toString();
            var font = Graphics.FONT_NUMBER_MEDIUM;
            if (dc.getTextWidthInPixels(number, font) > 64 || dc.getFontHeight(font) > 68) { font = Graphics.FONT_NUMBER_MILD; }
            if (dc.getTextWidthInPixels(number, font) > 64 || dc.getFontHeight(font) > 68) { font = Graphics.FONT_MEDIUM; }
            dc.setColor(BG, BG); dc.fillCircle(cx, cy - 4, 42);
            dc.setColor(MINT, Graphics.COLOR_TRANSPARENT);
            dc.drawText(cx, cy - 4 - dc.getFontHeight(font)/2, font, number, Graphics.TEXT_JUSTIFY_CENTER);
        }
        drawStatus(dc, cx, h*0.14);
        drawKeyHints(dc, w, h);
        var value = remote.delaySeconds().toString();
        var textWidth = dc.getTextWidthInPixels(value, Graphics.FONT_SMALL);
        var iconWidth = 26; var gap = 16;
        var left = cx - (iconWidth + gap + textWidth)/2;
        drawTimer(dc, left + iconWidth/2, h*0.86);
        dc.setColor(remote.delaySeconds() > 0 ? MINT : DIM, Graphics.COLOR_TRANSPARENT);
        dc.drawText(left + iconWidth + gap, h*0.86 - dc.getFontHeight(Graphics.FONT_SMALL)/2,
            Graphics.FONT_SMALL, value, Graphics.TEXT_JUSTIFY_LEFT);
        dc.setPenWidth(1);
    }
    function drawStatus(dc, x, y) {
        var feedback = remote.feedbackCode != -1 || remote.waiting.equals("prepare") || remote.waiting.equals("shutter") || remote.waiting.equals("timer") || remote.waiting.equals("cancel");
        var linkX = feedback ? x - 28 : x;
        if (remote.ready) { drawLink(dc, linkX, y, true); }
        else { drawSpinner(dc, linkX, y); }
        if (feedback) { drawFeedback(dc, x + 28, y); }
    }
    function drawSpinner(dc, x, y) {
        var colors = [0x333B32, 0x4E503B, 0x716447, 0x948052, 0xB79B5C, 0xD5AF63, 0xE8BD68, AMBER];
        for (var i = 0; i < 8; i++) {
            var dot = point(x, y, 13, (i + remote.spinnerFrame)*45);
            dc.setColor(colors[i], Graphics.COLOR_TRANSPARENT); dc.fillCircle(dot[0], dot[1], 3);
        }
    }
    function drawLink(dc, x, y, linked) {
        dc.setPenWidth(3);
        dc.setColor(linked ? MINT : AMBER, Graphics.COLOR_TRANSPARENT);
        dc.drawRoundedRectangle(x - 18, y - 8, 24, 16, 7);
        dc.drawRoundedRectangle(x - 6, y - 8, 24, 16, 7);
        if (linked) { dc.drawLine(x - 6, y, x + 6, y); }
        else { dc.setColor(BG, BG); dc.fillRectangle(x - 3, y - 12, 6, 24); }
    }
    function drawKeyHints(dc, w, h) {
        dc.setPenWidth(4);
        dc.setColor(remote.delaySeconds() < 10 ? MINT : DIM, Graphics.COLOR_TRANSPARENT);
        var px = w*RemoteLayout.PLUS_X; var py = h*RemoteLayout.PLUS_Y;
        dc.drawLine(px - 12, py, px + 12, py); dc.drawLine(px, py - 12, px, py + 12);
        dc.setColor(remote.delaySeconds() > 0 ? MINT : DIM, Graphics.COLOR_TRANSPARENT);
        var mx = w*RemoteLayout.MINUS_X; var my = h*RemoteLayout.MINUS_Y;
        dc.drawLine(mx - 12, my, mx + 12, my);
        dc.setColor(remote.ready ? MINT : DIM, Graphics.COLOR_TRANSPARENT);
        var sx = w*RemoteLayout.START_X; var sy = h*RemoteLayout.START_Y;
        dc.drawCircle(sx, sy, 13); dc.fillCircle(sx, sy, 5);
    }
    function drawTimer(dc, x, y) {
        dc.setPenWidth(2);
        dc.setColor(remote.delaySeconds() > 0 ? MINT : DIM, Graphics.COLOR_TRANSPARENT);
        dc.drawCircle(x, y + 2, 11); dc.drawLine(x - 4, y - 13, x + 4, y - 13);
        dc.drawLine(x, y - 13, x, y - 9); dc.drawLine(x, y + 2, x, y - 5);
        dc.drawLine(x, y + 2, x + 5, y + 5);
        if (remote.delaySeconds() == 0) { dc.drawLine(x - 11, y + 13, x + 11, y - 9); }
    }
    function drawFeedback(dc, x, y) {
        dc.setPenWidth(3);
        if (remote.waiting.equals("prepare") || remote.waiting.equals("shutter") || remote.waiting.equals("timer") || remote.waiting.equals("cancel")) {
            dc.setColor(AMBER, Graphics.COLOR_TRANSPARENT);
            dc.drawLine(x - 8, y - 10, x + 8, y - 10); dc.drawLine(x - 8, y + 10, x + 8, y + 10);
            dc.drawLine(x - 7, y - 9, x + 7, y + 9); dc.drawLine(x + 7, y - 9, x - 7, y + 9);
        } else if (remote.feedbackCode == 0) {
            dc.setColor(MINT, Graphics.COLOR_TRANSPARENT);
            dc.drawLine(x - 9, y, x - 2, y + 7); dc.drawLine(x - 2, y + 7, x + 11, y - 8);
        } else if (remote.feedbackCode == 6) {
            dc.setColor(DIM, Graphics.COLOR_TRANSPARENT);
            dc.drawCircle(x, y, 11); dc.drawLine(x - 7, y + 7, x + 7, y - 7);
        } else if (remote.feedbackCode != -1) {
            dc.setColor(RED, Graphics.COLOR_TRANSPARENT);
            dc.drawLine(x, y - 12, x - 12, y + 10); dc.drawLine(x - 12, y + 10, x + 12, y + 10); dc.drawLine(x + 12, y + 10, x, y - 12);
            dc.drawLine(x, y - 4, x, y + 2); dc.fillCircle(x, y + 6, 1);
        }
    }
    function point(x, y, radius, degrees) as Graphics.Point2D {
        var angle = degrees*Math.PI/180.0;
        return [(x + radius*Math.cos(angle)).toNumber(), (y + radius*Math.sin(angle)).toNumber()];
    }
    function drawIris(dc, x, y, frame) {
        var radius = 63;
        var closing = frame <= 6 ? frame : 12 - frame;
        var hole = radius*(0.48 - closing*0.07);
        dc.setColor(BG, BG); dc.fillCircle(x, y, radius + 1);
        dc.setPenWidth(2);
        for (var blade = 0; blade < 6; blade++) {
            var angle = blade*60 - 30;
            var first = point(x, y, radius, angle);
            var inner = point(x, y, hole, angle + 30);
            var polygon = [first, point(x, y, radius, angle + 20), point(x, y, radius, angle + 40),
                point(x, y, radius, angle + 60), point(x, y, hole, angle + 90), inner];
            dc.setColor(MINT, Graphics.COLOR_TRANSPARENT); dc.fillPolygon(polygon);
            dc.setColor(BG, Graphics.COLOR_TRANSPARENT); dc.drawLine(first[0], first[1], inner[0], inner[1]);
        }
    }
}
