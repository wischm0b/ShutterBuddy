import Toybox.Application;
import Toybox.Attention;
import Toybox.Lang;

// Phone-owned preferences. Keep the last confirmed configuration across app starts.
class WatchFeedback {
    var language = "en";
    var vibrate = true;
    var tone = false;
    var countdownVibrate = true;
    var revision = -1;
    var lastSecond = -1;
    function initialize() {
        var saved = Application.Storage.getValue("feedbackSettings");
        apply(saved);
        revision = -1;
    }
    function resetSession() { revision = -1; }
    function apply(data) {
        if (!(data instanceof Lang.Dictionary) || !(data["revision"] instanceof Lang.Number)
            || data["revision"] < 0 || data["revision"] <= revision
            || !(data["vibrate"] instanceof Lang.Boolean) || !(data["tone"] instanceof Lang.Boolean)
            || !(data["countdownVibrate"] instanceof Lang.Boolean)
            || !(data["language"] instanceof Lang.String)
            || (!data["language"].equals("en") && !data["language"].equals("de"))) { return false; }
        language = data["language"]; vibrate = data["vibrate"]; tone = data["tone"];
        countdownVibrate = data["countdownVibrate"]; revision = data["revision"];
        Application.Storage.setValue("feedbackSettings", data);
        return true;
    }
    function resetCountdown() { lastSecond = -1; }
    function countdown(second) {
        var pulse = CountdownPulse.shouldPulse(lastSecond, second);
        lastSecond = second;
        if (countdownVibrate && pulse) { buzz(25, 60); }
    }
    function shutter() {
        if (vibrate) { buzz(70, 160); }
        if (tone && (Attention has :playTone)) {
            try { Attention.playTone(Attention.TONE_KEY); } catch (e) { }
        }
    }
    function buzz(strength, duration) {
        if (Attention has :vibrate) {
            try { Attention.vibrate([new Attention.VibeProfile(strength, duration)]); } catch (e) { }
        }
    }
}

module CountdownPulse {
    // Never replay missed seconds, or buzz at zero / repeatedly on a 250 ms UI tick.
    function shouldPulse(previous, current) { return current > 0 && (previous < 0 || current < previous); }
}

(:test)
class FeedbackProbe extends WatchFeedback {
    var pulses = 0;
    var lastStrength = 0;
    function initialize() { WatchFeedback.initialize(); resetSession(); }
    function buzz(strength, duration) { pulses += 1; lastStrength = strength; }
}

(:test)
function phonePreferencesRejectStaleUpdatesAndRespectIndependentSwitches(logger) {
    var feedback = new FeedbackProbe();
    var config = {"revision" => 3, "language" => "de", "vibrate" => true, "tone" => false, "countdownVibrate" => true};
    Toybox.Test.assertEqual(feedback.apply(config), true);
    feedback.resetCountdown(); feedback.countdown(3); feedback.countdown(3); feedback.countdown(2);
    Toybox.Test.assertEqual(feedback.pulses, 2); Toybox.Test.assertEqual(feedback.lastStrength, 25);
    feedback.shutter(); Toybox.Test.assertEqual(feedback.pulses, 3); Toybox.Test.assertEqual(feedback.lastStrength, 70);
    config["revision"] = 2; config["vibrate"] = false;
    Toybox.Test.assertEqual(feedback.apply(config), false); Toybox.Test.assertEqual(feedback.vibrate, true);
    config["revision"] = 4; config["countdownVibrate"] = false;
    Toybox.Test.assertEqual(feedback.apply(config), true);
    feedback.countdown(1); feedback.shutter(); Toybox.Test.assertEqual(feedback.pulses, 3);
    feedback.resetSession(); config["revision"] = 0; config["language"] = "en";
    Toybox.Test.assertEqual(feedback.apply(config), true); Toybox.Test.assertEqual(feedback.language, "en");
    config["revision"] = 1; config["language"] = "invalid";
    Toybox.Test.assertEqual(feedback.apply(config), false);
    return true;
}

(:test)
function countdownFeedbackOnlyPulsesOnNewPositiveSeconds(logger) {
    Toybox.Test.assertEqual(CountdownPulse.shouldPulse(-1, 3), true);
    Toybox.Test.assertEqual(CountdownPulse.shouldPulse(3, 3), false);
    Toybox.Test.assertEqual(CountdownPulse.shouldPulse(3, 2), true);
    Toybox.Test.assertEqual(CountdownPulse.shouldPulse(3, 1), true);
    Toybox.Test.assertEqual(CountdownPulse.shouldPulse(1, 0), false);
    Toybox.Test.assertEqual(CountdownPulse.shouldPulse(2, 3), false);
    return true;
}
