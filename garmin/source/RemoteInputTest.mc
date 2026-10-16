import Toybox.Test;
import Toybox.WatchUi;

(:test)
class InputTestRemote {
    var photos = 0;
    var delays = 0;
    function select() { photos += 1; }
    function changeDelay(direction) { delays += direction; }
}

(:test)
function physicalStartStillTakesOnePhoto(logger) {
    var remote = new InputTestRemote();
    var input = new RemoteInput(remote, new RemoteView(remote));
    if (!input.onSelect()) { input.handleKey(WatchUi.KEY_ENTER); }
    Test.assertEqual(remote.photos, 1);
    return true;
}

(:test)
function onlyShutterCircleTakesTouchPhoto(logger) {
    var remote = new InputTestRemote();
    var input = new RemoteInput(remote, new RemoteView(remote));
    input.handleTap(227, 90);
    input.handleTap(20, 140);
    Test.assertEqual(remote.photos, 0);
    input.handleTap(227, 245);
    Test.assertEqual(remote.photos, 1);
    return true;
}

(:test)
function tappingStatusDoesNotTakePhoto(logger) {
    var remote = new InputTestRemote();
    var input = new RemoteInput(remote, new RemoteView(remote));
    if (!input.onSelect()) { input.handleTap(227, 65); }
    Test.assertEqual(remote.photos, 0);
    Test.assertEqual(remote.delays, 0);
    return true;
}

(:test)
function tappingDelayDoesNotTakePhoto(logger) {
    var remote = new InputTestRemote();
    var input = new RemoteInput(remote, new RemoteView(remote));
    if (!input.onSelect()) { input.handleTap(227, 401); }
    Test.assertEqual(remote.photos, 0);
    Test.assertEqual(remote.delays, 1);
    return true;
}

(:test)
function sideDelayKeysAndTouchMarkersAgree(logger) {
    var remote = new InputTestRemote();
    var input = new RemoteInput(remote, new RemoteView(remote));
    input.onPreviousPage(); Test.assertEqual(remote.delays, 1);
    input.onNextPage(); Test.assertEqual(remote.delays, 0);
    input.handleTap(39, 229); Test.assertEqual(remote.delays, 1);
    input.handleTap(65, 322); Test.assertEqual(remote.delays, 0);
    Test.assertEqual(remote.photos, 0);
    return true;
}

(:test)
function tappingMarkedStartTakesOnePhoto(logger) {
    var remote = new InputTestRemote();
    var input = new RemoteInput(remote, new RemoteView(remote));
    if (!input.onSelect()) { input.handleTap(390, 133); }
    Test.assertEqual(remote.photos, 1);
    Test.assertEqual(remote.delays, 0);
    return true;
}

(:test)
function delaySelectionStopsAtBothEnds(logger) {
    Test.assertEqual(DelaySelection.nextIndex(0, -1, 4), 0);
    Test.assertEqual(DelaySelection.nextIndex(3, 1, 4), 3);
    var low = 0; var high = 3;
    for (var i = 0; i < 20; i++) {
        low = DelaySelection.nextIndex(low, -1, 4);
        high = DelaySelection.nextIndex(high, 1, 4);
    }
    Test.assertEqual(low, 0); Test.assertEqual(high, 3);
    return true;
}

(:test)
function delaySelectionCanReturnFromBothLimits(logger) {
    var index = 0;
    for (var expected = 1; expected <= 3; expected++) {
        index = DelaySelection.nextIndex(index, 1, 4);
        Test.assertEqual(index, expected);
    }
    for (var expected = 2; expected >= 0; expected--) {
        index = DelaySelection.nextIndex(index, -1, 4);
        Test.assertEqual(index, expected);
    }
    return true;
}
