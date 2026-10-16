module RemoteLayout {
    // Projected from the Fenix 8 47 mm SDK key centers toward the display center.
    const PLUS_X = 0.086;
    const PLUS_Y = 0.505;
    const MINUS_X = 0.143;
    const MINUS_Y = 0.710;
    const START_X = 0.859;
    const START_Y = 0.293;
    function hitsKey(x, y, w, h, nx, ny) {
        var dx = x - w*nx; var dy = y - h*ny;
        var radius = h*0.075;
        return dx*dx + dy*dy <= radius*radius;
    }
}

module DelaySelection {
    function nextIndex(current, direction, count) {
        var next = current + direction;
        if (next < 0) { return 0; }
        if (next >= count) { return count - 1; }
        return next;
    }
}
