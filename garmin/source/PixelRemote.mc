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

class RemoteInput extends WatchUi.BehaviorDelegate {
    var remote;
    var view;
    function initialize(model, screen) { BehaviorDelegate.initialize(); remote = model; view = screen; }
    // Garmin dispatches the Selection behavior BEFORE onTap/onKey. Let it fall
    // through so touch coordinates and the physical START key stay distinct.
    function onSelect() { return false; }
    function onKey(event) {
        return handleKey(event.getKey());
    }
    function handleKey(key) {
        if (key == WatchUi.KEY_ENTER) { remote.select(); return true; }
        return false;
    }
    function onTap(event) {
        var coordinates = event.getCoordinates();
        return handleTap(coordinates[0], coordinates[1]);
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
    var message = "Garmin Connect verbinden";
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
    function initialize() {
        nonce = System.getTimer().toString() + ":" + Math.rand().toString();
        timeout = new Timer.Timer(); heartbeat = new Timer.Timer(); countdown = new Timer.Timer();
        var savedDelay = Application.Storage.getValue("delaySeconds");
        for (var i = 0; i < delays.size(); i++) { if (delays[i] == savedDelay) { delayIndex = i; } }
    }
    function show(text) { message = text; WatchUi.requestUpdate(); }
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
        if (session != null && (waiting.equals("timer") || waiting.equals("shutter") || waiting.equals("cancel"))) {
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
            ready = false; show("Garmin Connect verbinden\nKeine Handy-Verbindung"); return;
        }
        show("Verbinde mit Handy ...\nFernbedienung starten");
        send({"v" => 2, "type" => "hello", "nonce" => nonce}, "hello", 12000);
    }
    function tick() {
        if (stopped) { return; }
        if (!ready) { spinnerFrame = (spinnerFrame + 1) % 8; WatchUi.requestUpdate(); }
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
        var wasShutter = waiting.equals("shutter") || waiting.equals("timer") || wasCancel;
        timeout.stop(); countdown.stop(); apertureFrame = -1; feedbackCode = -2; remaining = 0; waiting = ""; setReady(false); session = null; fastTicket = null;
        show(wasCancel ? "Abbruch nicht bestaetigt\nTimer kann noch ausloesen" : (wasShutter ? "Keine Rueckmeldung\nFoto am Pixel pruefen" : "Garmin-Senden fehlgeschlagen\nGarmin Connect pruefen"));
    }
    function delivered(id) {
        if (stopped || id != request || waiting.equals("")) { return; }
        transportAccepted = true;
        if (waiting.equals("hello")) { show("Verbinde mit Pixel ...\nKurz warten"); }
    }
    function timedOut() {
        var wasCancel = waiting.equals("cancel");
        var wasShutter = waiting.equals("shutter") || waiting.equals("timer") || wasCancel;
        countdown.stop(); apertureFrame = -1; feedbackCode = -2; remaining = 0; waiting = ""; setReady(false); session = null; fastTicket = null;
        show(wasCancel ? "Abbruch nicht bestaetigt\nTimer kann noch ausloesen" : (wasShutter ? "Keine Rueckmeldung\nFoto am Pixel pruefen" : (transportAccepted ? "Handy antwortet nicht\nShutter Buddy oeffnen" : "Garmin antwortet nicht\nGarmin Connect pruefen")));
    }
    function select() {
        if (stopped) { return; }
        if (waiting.equals("timer") || waiting.equals("shutter")) { cancelAction(); return; }
        if (!waiting.equals("") && !waiting.equals("ping")) { return; }
        if (waiting.equals("ping")) { timeout.stop(); waiting = ""; }
        countdown.stop(); apertureFrame = -1; feedbackCode = -1;
        if (!ready || session == null) { hello(); return; }
        if (delaySeconds() > 0 && !timerSupported) { feedbackCode = -2; show("Handy-App aktualisieren\nVorlauf nicht unterstuetzt"); return; }
        sequence += 1;
        if (fastTicket != null && System.getTimer() < fastUntil) {
            var ticket = fastTicket; fastTicket = null;
            show(delaySeconds() == 0 ? "Loese aus ..." : "Starte Selbstausloeser ...");
            send({"v" => 2, "type" => "shutter", "session" => session, "seq" => sequence, "ticket" => ticket, "fast" => true, "delay" => delaySeconds()}, "shutter", delaySeconds()*1000 + 8000);
            return;
        }
        show("Loese aus ...");
        send({"v" => 2, "type" => "prepare", "session" => session, "seq" => sequence}, "prepare", 8000);
    }
    function delaySeconds() { return delays[delayIndex]; }
    function animateAperture() {
        apertureFrame += 1;
        if (stopped || apertureFrame > 12) { countdown.stop(); apertureFrame = -1; }
        WatchUi.requestUpdate();
    }
    function changeDelay(direction) {
        if (!waiting.equals("") && !waiting.equals("ping")) { return; }
        var next = DelaySelection.nextIndex(delayIndex, direction, delays.size());
        if (next == delayIndex) { return; }
        countdown.stop(); apertureFrame = -1; feedbackCode = -1;
        delayIndex = next;
        Application.Storage.setValue("delaySeconds", delays[delayIndex]);
        show("Vorlauf: " + (delaySeconds() == 0 ? "aus" : delaySeconds().toString() + " Sekunden") + "\nSTART: Foto aufnehmen");
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
            timeout.stop(); waiting = ""; feedbackCode = 6; show("Abgebrochen"); return true;
        }
        if (session == null || (!waiting.equals("timer") && !waiting.equals("shutter"))) { return false; }
        countdown.stop(); apertureFrame = -1; remaining = 0; show("Breche ab ...\nWarte auf Pixel");
        send({"v" => 2, "type" => "cancel", "session" => session, "seq" => sequence}, "cancel", 6000);
        return true;
    }
    function updateCountdown() {
        var millis = countdownEnds - System.getTimer();
        remaining = millis > 0 ? ((millis + 999)/1000).toNumber() : 0;
        if (remaining == 0) { countdown.stop(); show("Warte auf Ausloesung ..."); }
        else { show("Foto in " + remaining + " Sekunden\nSTART / BACK: abbrechen"); }
    }
    function onPhone(msg as Communications.PhoneAppMessage) as Void {
        if (stopped) { return; }
        var data = msg.data;
        if (!(data instanceof Lang.Dictionary) || data["v"] != 2) { return; }
        var type = data["type"];
        if (type == null) { return; }
        if (type.equals("ready")) {
            if (!waiting.equals("hello") || data["nonce"] == null || !data["nonce"].equals(nonce) || !(data["session"] instanceof Lang.String)) { return; }
            rememberTicket(data, sentAt);
            timerSupported = data["timer"] == true;
            session = data["session"]; setReady(true); waiting = ""; timeout.stop();
            show("Bereit fuer dein Foto\nPixel-Kamera oeffnen"); return;
        }
        if (session == null || data["session"] == null || !data["session"].equals(session)) { return; }
        if (type.equals("pong")) {
            if (data["rid"] == pingId) { rememberTicket(data, pingSentAt); }
            if (waiting.equals("ping")) { waiting = ""; timeout.stop(); ready = true; }
            return;
        }
        if (data["seq"] != sequence) { return; }
        if (type.equals("armed") && waiting.equals("prepare") && data["ticket"] instanceof Lang.String) {
            timeout.stop(); show("Loese aus ...");
            send({"v" => 2, "type" => "shutter", "session" => session, "seq" => sequence, "ticket" => data["ticket"], "delay" => delaySeconds()}, "shutter", delaySeconds()*1000 + 8000);
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
        if (type.equals("result") && (waiting.equals("prepare") || waiting.equals("shutter") || waiting.equals("timer") || waiting.equals("cancel"))) {
            rememberTicket(data, sentAt);
            timeout.stop(); countdown.stop(); remaining = 0; waiting = "";
            var messages = ["Ausloeser betaetigt\nFoto am Pixel pruefen", "Bedienungshilfe\nam Pixel aktivieren", "Pixel entsperren\nKamera oeffnen", "Fotomodus pruefen\nPixel-Kamera neu oeffnen", "Kamera beschaeftigt\nKurz warten", "Ausloesen fehlgeschlagen\nSTART: erneut versuchen", "Selbstausloeser abgebrochen"];
            var code = data["code"];
            feedbackCode = code instanceof Lang.Number && code >= 0 && code < messages.size() ? code : -2;
            apertureFrame = -1;
            if (feedbackCode == 0) {
                apertureFrame = 0;
                countdown.start(method(:animateAperture), 50, true);
            }
            show(code instanceof Lang.Number && code >= 0 && code < messages.size() ? messages[code] : "Unbekannte Rueckmeldung");
        }
    }
}
