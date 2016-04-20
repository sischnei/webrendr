"use strict";
var page = require('webpage').create(),
    system = require('system'),
    selector = 'html',
    address, output;

if (system.args.length < 3 || system.args.length > 4) {
    console.log('Usage: rasterize.js URL filename [selector]');
    phantom.exit(1);
} else {
    address = system.args[1];
    output = system.args[2];
    page.viewportSize = { width: 600, height: 600 };
    if (system.args.length >= 3) {
        selector = system.args[3];
    }

    page.open(address, function (status) {

        if (status !== 'success') {
            console.log('Unable to load the address!');
            phantom.exit(1);
        } else {
            window.setTimeout(function () {
                var clipRect = page.evaluate(function(selector){
                    var selector = document.querySelector(selector);
                    if (selector !== null) {
                        return selector.getBoundingClientRect();
                    } else {
                        return document.querySelector('html').getBoundingClientRect();
                    }
                }, selector);
                page.clipRect = {
                    top:    clipRect.top,
                    left:   clipRect.left,
                    width:  clipRect.width,
                    height: clipRect.height
                };

                page.render(output);
                phantom.exit();
            }, 200);
        }
    });
}