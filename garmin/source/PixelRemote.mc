import Toybox.Application;
import Toybox.Communications;
import Toybox.Graphics;
import Toybox.Lang;
import Toybox.Math;
import Toybox.System;
import Toybox.Timer;
import Toybox.WatchUi;

class PixelRemoteApp extends Application.AppBase {
    var remote;
    function initialize() { AppBase.initialize(); }
    function onStart(state) { remote = new PixelRemoteLink(); remote.start(); }
    function onStop(state) { remote.stop(); }
    function getInitialView() { var view = new RemoteView(remote); return [view, new RemoteInput(remote, view)]; }
}

class RemoteInput extends WatchUi.InputDelegate {
    var remote;
    var view;
    var downSince = null;
    var downHandled = false;
    function initialize(model, screen) { InputDelegate.initialize(); remote = model; view = screen; }
    // Raw events distinguish a held DOWN key from its short action, without
    // changing the interval on the initial press or handling release twice.
    function onSelect() { return false; }
    function onKeyPressed(event) {
        if (event.getKey() == WatchUi.KEY_DOWN) { return pressDown(System.getTimer()); }
        return false;
    }
    function onKeyReleased(event) {
        if (event.getKey() == WatchUi.KEY_DOWN) { return releaseDown(System.getTimer()); }
        return false;
    }
    function pressDown(now) {
        if (downSince == null) { downSince = now; downHandled = false; }
        return true;
    }
    function releaseDown(now) {
        if (downSince != null && !downHandled) {
            var held = now - downSince; downSince = null; downHandled = true;
            if (held >= 800) { remote.toggleMode(); } else { remote.changeDelay(-1); }
        }
        return true;
    }
    function onKey(event) {
        return handleKey(event.getKey());
    }
    function handleKey(key) {
        if (key == WatchUi.KEY_ENTER) { remote.select(); return true; }
        if (key == WatchUi.KEY_UP) { remote.changeDelay(1); return true; }
        if (key == WatchUi.KEY_DOWN) {
            if (!downHandled) {
                if (downSince != null) { releaseDown(System.getTimer()); }
                else { remote.changeDelay(-1); downHandled = true; }
            }
            return true;
        }
        if (key == WatchUi.KEY_ESC) { return onBack(); }
        return false;
    }
    function onTap(event) {
        var coordinates = event.getCoordinates();
        return handleTap(coordinates[0], coordinates[1]);
    }
    function onHold(event) {
        var p = event.getCoordinates();
        if (RemoteLayout.hitsKey(p[0], p[1], view.width, view.height, RemoteLayout.MINUS_X, RemoteLayout.MINUS_Y)) { remote.toggleMode(); }
        return true;
    }
    function onSwipe(event) {
        if (event.getDirection() == WatchUi.SWIPE_UP) { return onNextPage(); }
        if (event.getDirection() == WatchUi.SWIPE_DOWN) { return onPreviousPage(); }
        if (event.getDirection() == WatchUi.SWIPE_RIGHT) { return onBack(); }
        return true;
    }
    function handleTap(x, y) {
        if (RemoteLayout.hitsKey(x, y, view.width, view.height, RemoteLayout.PLUS_X, RemoteLayout.PLUS_Y)) { remote.changeDelay(1); }
        else if (RemoteLayout.hitsKey(x, y, view.width, view.height, RemoteLayout.MINUS_X, RemoteLayout.MINUS_Y)) { remote.changeDelay(-1); }
        else if (RemoteLayout.hitsKey(x, y, view.width, view.height, RemoteLayout.START_X, RemoteLayout.START_Y)) { remote.select(); }
        else if (x > view.width*0.33 && x < view.width*0.67 && y > view.height*0.82 && y < view.height*0.96) { remote.changeDelay(1); }
        else {
            // The large logo and the marked START area are the only touch shutters.
            var dx = x - view.width/2; var dy = y - view.height*0.49;
            var radius = view.height*0.255;
            if (dx*dx + dy*dy <= radius*radius) { remote.select(); }
        }
        return true;
    }
    function onMenu() { return true; }
    function onNextPage() { remote.changeDelay(-1); return true; }
    function onPreviousPage() { remote.changeDelay(1); return true; }
    function onBack() { if (!remote.cancelAction()) { WatchUi.popView(WatchUi.SLIDE_IMMEDIATE); } return true; }
}
class RemoteTransmit extends Communications.ConnectionListener {
    var remote;
    var request;
    function initialize(model, id) { ConnectionListener.initialize(); remote = model; request = id; }
    function onComplete() { remote.delivered(request); }
    function onError() { remote.failed(request); }
}
class PixelRemoteLink {
    var ready = false;
    var message = "Connect Garmin Connect";
    var stopped = false;
    var waiting = "";
    var nonce;
    var session = null;
    var sequence = 0;
    var request = 0;
    var transportAccepted = false;
    var timeout;
    var heartbeat;
    var countdown;
    var delays as Lang.Array<Lang.Number> = [0, 3, 5, 10];
    var delayIndex as Lang.Number = 0;
    var remaining = 0;
    var countdownEnds = 0;
    var fastTicket = null;
    var fastUntil = 0;
    var sentAt = 0;
    var pingSentAt = 0;
    var pingId = 0;
    var timerSupported = false;
    var feedbackCode = -1;
    var apertureFrame = -1;
    var spinnerFrame = 0;
    var nextReconnect = 0;
    var seriesMode = false;
    var seriesInterval = 3;
    var seriesSupported = false;
    var seriesActive = false;
    var seriesCount = 0;
    var feedback;
    function initialize() {
        feedback = new WatchFeedback();
        nonce = System.getTimer().toString() + ":" + Math.rand().toString();
        timeout = new Timer.Timer(); heartbeat = new Timer.Timer(); countdown = new Timer.Timer();
        var savedDelay = Application.Storage.getValue("delaySeconds");
        for (var i = 0; i < delays.size(); i++) { if (delays[i] == savedDelay) { delayIndex = i; } }
        seriesMode = Application.Storage.getValue("seriesMode") == true;
        var savedInterval = Application.Storage.getValue("seriesInterval");
        if (savedInterval instanceof Lang.Number && savedInterval >= 1 && savedInterval <= 5) { seriesInterval = savedInterval; }
    }
    function show(text) { message = text; WatchUi.requestUpdate(); }
    function tr(en, de) { return feedback.language.equals("de") ? de : en; }
    function setReady(value) {
        ready = value;
        heartbeat.stop();
        if (!stopped) { heartbeat.start(method(:tick), value ? 3000 : 100, true); }
    }
    function start() {
        Communications.registerForPhoneAppMessages(method(:onPhone));
        setReady(false);
        nextReconnect = System.getTimer() + 3000;
        hello();
    }
    function stop() {
        if (session != null && (waiting.equals("timer") || waiting.equals("shutter") || waiting.equals("series") || waiting.equals("cancel"))) {
            try { Communications.transmit({"v" => 2, "type" => "cancel", "session" => session, "seq" => sequence}, null, new RemoteTransmit(self, request + 1)); } catch (e) { }
        }
        stopped = true; ready = false; timeout.stop(); heartbeat.stop(); countdown.stop(); apertureFrame = -1;
        Communications.registerForPhoneAppMessages(null);
    }
    function send(data, phase, milliseconds) {
        if (stopped) { return; }
        request += 1; waiting = phase; transportAccepted = false;
        sentAt = System.getTimer();
        timeout.start(method(:timedOut), milliseconds, false);
        try { Communications.transmit(data, null, new RemoteTransmit(self, request)); }
        catch (e) { failed(request); }
    }
    function hello() {
        if (stopped || !waiting.equals("")) { return; }
        setReady(false);
        if (!System.getDeviceSettings().phoneConnected) {
            ready = false; show(tr("Connect Garmin Connect\nPhone disconnected", "Garmin Connect verbinden\nKeine Handy-Verbindung")); return;
        }
        show(tr("Connecting to phone…\nStart the remote", "Verbinde mit Handy ...\nFernbedienung starten"));
        send({"v" => 2, "type" => "hello", "nonce" => nonce}, "hello", 12000);
    }
    function tick() {
        if (stopped) { return; }
        if (!ready) { spinnerFrame = (spinnerFrame + 1) % 8; WatchUi.requestUpdate(); }
        if (waiting.equals("series")) {
            pingSentAt = System.getTimer(); pingId += 1;
            try { Communications.transmit({"v" => 2, "type" => "ping", "session" => session, "rid" => pingId}, null, new RemoteTransmit(self, -1)); } catch (e) { }
            return;
        }
        if (!waiting.equals("")) { return; }
        if (!ready || session == null) {
            if (System.getTimer() >= nextReconnect) { nextReconnect = System.getTimer() + 3000; hello(); }
            return;
        }
        pingSentAt = System.getTimer(); pingId += 1;
        send({"v" => 2, "type" => "ping", "session" => session, "rid" => pingId}, "ping", 6000);
    }
    function failed(id) {
        if (stopped || id != request || waiting.equals("")) { return; }
        var wasCancel = waiting.equals("cancel");
        var wasShutter = waiting.equals("shutter") || waiting.equals("timer") || waiting.equals("series") || wasCancel;
        seriesActive = false;
        timeout.stop(); countdown.stop(); apertureFrame = -1; feedbackCode = -2; remaining = 0; waiting = ""; setReady(false); session = null; fastTicket = null;
        show(wasCancel ? tr("Stop not confirmed\nCapture may still fire", "Abbruch nicht bestaetigt\nTimer kann noch ausloesen") : (wasShutter ? tr("No reply\nCheck photo on Pixel", "Keine Rueckmeldung\nFoto am Pixel pruefen") : tr("Could not send\nCheck Garmin Connect", "Garmin-Senden fehlgeschlagen\nGarmin Connect pruefen")));
    }
    function delivered(id) {
        if (stopped || id != request || waiting.equals("")) { return; }
        transportAccepted = true;
        if (waiting.equals("hello")) { show(tr("Connecting to Pixel…\nPlease wait", "Verbinde mit Pixel ...\nKurz warten")); }
    }
    function timedOut() {
        var wasCancel = waiting.equals("cancel");
        var wasShutter = waiting.equals("shutter") || waiting.equals("timer") || waiting.equals("series") || wasCancel;
        seriesActive = false;
        countdown.stop(); apertureFrame = -1; feedbackCode = -2; remaining = 0; waiting = ""; setReady(false); session = null; fastTicket = null;
        show(wasCancel ? tr("Stop not confirmed\nCapture may still fire", "Abbruch nicht bestaetigt\nTimer kann noch ausloesen") : (wasShutter ? tr("No reply\nCheck photo on Pixel", "Keine Rueckmeldung\nFoto am Pixel pruefen") : (transportAccepted ? tr("Phone not responding\nOpen Shutter Buddy", "Handy antwortet nicht\nShutter Buddy oeffnen") : tr("Garmin not responding\nCheck Garmin Connect", "Garmin antwortet nicht\nGarmin Connect pruefen"))));
    }
    function select() {
        if (stopped) { return; }
        if (waiting.equals("timer") || waiting.equals("shutter") || waiting.equals("series")) { cancelAction(); return; }
        if (!waiting.equals("") && !waiting.equals("ping")) { return; }
        if (waiting.equals("ping")) { timeout.stop(); waiting = ""; }
        countdown.stop(); apertureFrame = -1; feedbackCode = -1;
        if (!ready || session == null) { hello(); return; }
        if ((seriesMode && !seriesSupported) || (!seriesMode && delaySeconds() > 0 && !timerSupported)) { feedbackCode = -2; show(tr("Update the phone app", "Handy-App aktualisieren")); return; }
        seriesCount = 0; seriesActive = false;
        feedback.resetCountdown();
        sequence += 1;
        if (fastTicket != null && System.getTimer() < fastUntil) {
            var ticket = fastTicket; fastTicket = null;
            show(delaySeconds() == 0 ? tr("Releasing shutter…", "Loese aus ...") : tr("Starting timer…", "Starte Selbstausloeser ..."));
            send(shutterData(ticket, true), "shutter", (seriesMode ? 0 : delaySeconds())*1000 + 8000);
            return;
        }
        show(tr("Releasing shutter…", "Loese aus ..."));
        send({"v" => 2, "type" => "prepare", "session" => session, "seq" => sequence}, "prepare", 8000);
    }
    function shutterData(ticket, fast) {
        return {"v" => 2, "type" => "shutter", "session" => session, "seq" => sequence, "ticket" => ticket,
            "fast" => fast, "delay" => seriesMode ? 0 : delays[delayIndex],
            "mode" => seriesMode ? "series" : "single", "interval" => seriesMode ? seriesInterval : 0};
    }
    function delaySeconds() { return seriesMode ? seriesInterval : delays[delayIndex]; }
    function toggleMode() {
        if (stopped || (!waiting.equals("") && !waiting.equals("ping"))) { return; }
        countdown.stop(); apertureFrame = -1; feedbackCode = -1;
        seriesMode = !seriesMode;
        Application.Storage.setValue("seriesMode", seriesMode);
        WatchUi.requestUpdate();
    }
    function animateAperture() {
        apertureFrame += 1;
        if (stopped || apertureFrame > 12) { countdown.stop(); apertureFrame = -1; }
        WatchUi.requestUpdate();
    }
    function changeDelay(direction) {
        if (!waiting.equals("") && !waiting.equals("ping")) { return; }
        var current = seriesMode ? seriesInterval - 1 : delayIndex;
        var next = DelaySelection.nextIndex(current, direction, seriesMode ? 5 : delays.size());
        if (next == current) { return; }
        countdown.stop(); apertureFrame = -1; feedbackCode = -1;
        if (seriesMode) { seriesInterval = next + 1; Application.Storage.setValue("seriesInterval", seriesInterval); }
        else { delayIndex = next; Application.Storage.setValue("delaySeconds", delays[delayIndex]); }
        show(tr("Delay: ", "Vorlauf: ") + (delaySeconds() == 0 ? tr("off", "aus") : delaySeconds().toString() + tr(" seconds", " Sekunden")) + tr("\nSTART: take photo", "\nSTART: Foto aufnehmen"));
    }
    function rememberTicket(data as Lang.Dictionary, started as Lang.Number) {
        if (!(data["nextTicket"] instanceof Lang.String) || !(data["ttl"] instanceof Lang.Number)) { return; }
        var elapsed = System.getTimer() - started;
        var validFor = data["ttl"] - elapsed;
        // Subtract the full round trip: conservative expiry without clock synchronisation.
        if (elapsed < 0 || validFor <= 0 || data["ttl"] > 5000) { return; }
        fastTicket = data["nextTicket"]; fastUntil = System.getTimer() + validFor;
    }
    function cancelAction() {
        if (waiting.equals("cancel")) { return true; }
        if (waiting.equals("prepare")) {
            timeout.stop(); waiting = ""; feedbackCode = 6; show(tr("Cancelled", "Abgebrochen")); return true;
        }
        if (session == null || (!waiting.equals("timer") && !waiting.equals("shutter") && !waiting.equals("series"))) { return false; }
        countdown.stop(); apertureFrame = -1; remaining = 0; show(tr("Stopping…\nWaiting for Pixel", "Breche ab ...\nWarte auf Pixel"));
        send({"v" => 2, "type" => "cancel", "session" => session, "seq" => sequence}, "cancel", 6000);
        return true;
    }
    function updateCountdown() {
        var millis = countdownEnds - System.getTimer();
        remaining = millis > 0 ? ((millis + 999)/1000).toNumber() : 0;
        feedback.countdown(remaining);
        if (remaining == 0) { countdown.stop(); show(tr("Waiting for shutter…", "Warte auf Ausloesung ...")); }
        else { show(tr("Photo in ", "Foto in ") + remaining + tr(" seconds\nSTART / BACK: cancel", " Sekunden\nSTART / BACK: abbrechen")); }
    }
    function onPhone(msg as Communications.PhoneAppMessage) as Void {
        if (stopped) { return; }
        var data = msg.data;
        if (!(data instanceof Lang.Dictionary) || data["v"] != 2) { return; }
        var type = data["type"];
        if (type == null) { return; }
        if (type.equals("ready")) {
            if (!waiting.equals("hello") || data["nonce"] == null || !data["nonce"].equals(nonce) || !(data["session"] instanceof Lang.String)) { return; }
            feedback.resetSession(); feedback.apply(data["settings"]);
            rememberTicket(data, sentAt);
            timerSupported = data["timer"] == true;
            seriesSupported = data["series"] == true;
            session = data["session"]; setReady(true); waiting = ""; timeout.stop();
            show(tr("Ready for your photo\nOpen Pixel Camera", "Bereit fuer dein Foto\nPixel-Kamera oeffnen")); return;
        }
        if (session == null || data["session"] == null || !data["session"].equals(session)) { return; }
        if (feedback.apply(data["settings"])) { WatchUi.requestUpdate(); }
        if (type.equals("settings")) { return; }
        if (type.equals("pong")) {
            if (data["rid"] == pingId) { rememberTicket(data, pingSentAt); }
            if (waiting.equals("ping")) { waiting = ""; timeout.stop(); ready = true; }
            return;
        }
        if (data["seq"] != sequence) { return; }
        if (type.equals("seriesState") && (waiting.equals("shutter") || waiting.equals("series"))) {
            var count = data["count"];
            if (!(count instanceof Lang.Number) || count < seriesCount || !seriesMode) { return; }
            timeout.stop(); waiting = "series"; seriesActive = true;
            timeout.start(method(:timedOut), 18000, false);
            if (count > seriesCount) {
                feedback.shutter();
                seriesCount = count; feedbackCode = 0; countdown.stop(); apertureFrame = 0;
                countdown.start(method(:animateAperture), 50, true);
            }
            WatchUi.requestUpdate(); return;
        }
        if (type.equals("armed") && waiting.equals("prepare") && data["ticket"] instanceof Lang.String) {
            timeout.stop(); show(tr("Releasing shutter…", "Loese aus ..."));
            send(shutterData(data["ticket"], false), "shutter", (seriesMode ? 0 : delaySeconds())*1000 + 8000);
            return;
        }
        if (type.equals("scheduled") && waiting.equals("shutter") && data["delay"] instanceof Lang.Number && data["delay"] > 0 && data["delay"] <= 10) {
            waiting = "timer";
            var millis = data["remainingMs"] instanceof Lang.Number ? data["remainingMs"] : data["delay"]*1000;
            countdownEnds = System.getTimer() + millis - (System.getTimer() - sentAt)/2;
            updateCountdown();
            if (remaining > 0) { countdown.start(method(:updateCountdown), 250, true); }
            return;
        }
        if (type.equals("result") && (waiting.equals("prepare") || waiting.equals("shutter") || waiting.equals("timer") || waiting.equals("series") || waiting.equals("cancel"))) {
            seriesActive = false;
            rememberTicket(data, sentAt);
            timeout.stop(); countdown.stop(); remaining = 0; waiting = "";
            var messages = [tr("Shutter pressed\nCheck photo on Pixel", "Ausloeser betaetigt\nFoto am Pixel pruefen"), tr("Enable accessibility\non your Pixel", "Bedienungshilfe\nam Pixel aktivieren"), tr("Unlock Pixel\nOpen Camera", "Pixel entsperren\nKamera oeffnen"), tr("Check Photo mode\nReopen Pixel Camera", "Fotomodus pruefen\nPixel-Kamera neu oeffnen"), tr("Camera busy\nPlease wait", "Kamera beschaeftigt\nKurz warten"), tr("Shutter failed\nSTART: try again", "Ausloesen fehlgeschlagen\nSTART: erneut versuchen"), tr("Capture cancelled", "Selbstausloeser abgebrochen")];
            var code = data["code"];
            feedbackCode = code instanceof Lang.Number && code >= 0 && code < messages.size() ? code : -2;
            apertureFrame = -1;
            if (feedbackCode == 0) {
                feedback.shutter();
                apertureFrame = 0;
                countdown.start(method(:animateAperture), 50, true);
            }
            show(code instanceof Lang.Number && code >= 0 && code < messages.size() ? messages[code] : tr("Unknown response", "Unbekannte Rueckmeldung"));
        }
    }
}
