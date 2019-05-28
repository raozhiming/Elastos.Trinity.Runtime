/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

// these tests are meant to be executed by Cordova ParaMedic Appium runner
// you can find it here: https://github.com/apache/cordova-paramedic/
// it is not necessary to do a full CI setup to run these tests
// Run:
//      node cordova-paramedic/main.js --platform android --plugin cordova-plugin-camera --skipMainTests --target <emulator name>
// Please note only Android 5.1 and 4.4 are supported at this point.

'use strict';

var wdHelper = global.WD_HELPER;
var screenshotHelper = global.SCREENSHOT_HELPER;
var wd = wdHelper.getWD();

var MINUTE = 60 * 1000;
var BACK_BUTTON = 4;
var DEFAULT_SCREEN_WIDTH = 360;
var DEFAULT_SCREEN_HEIGHT = 567;
var DEFAULT_WEBVIEW_CONTEXT = 'WEBVIEW';
var PROMISE_PREFIX = 'appium_camera_promise_';
var CONTEXT_NATIVE_APP = 'NATIVE_APP';

describe('Camera tests Android.', function () {
    var driver;
    // the name of webview context, it will be changed to match needed context if there are named ones:
    var webviewContext = DEFAULT_WEBVIEW_CONTEXT;
    // this indicates that the device library has the test picture:
    var isTestPictureSaved = false;
    // we need to know the screen width and height to properly click on an image in the gallery:
    var screenWidth = DEFAULT_SCREEN_WIDTH;
    var screenHeight = DEFAULT_SCREEN_HEIGHT;
    // promise count to use in promise ID
    var promiseCount = 0;
    // determine if Appium session is created successfully
    var appiumSessionStarted = false;
    // determine if camera is present on the device/emulator
    var cameraAvailable = false;
    // determine if emulator is within a range of acceptable resolutions able to run these tests
    var isResolutionBad = true;
    // a path to the image we add to the gallery before test run
    var fillerImagePath;
    var isAndroid7 = getIsAndroid7();

    function getIsAndroid7() {
        if (global.USE_SAUCE) {
            return global.SAUCE_CAPS && (parseFloat(global.SAUCE_CAPS.platformVersion) >= 7);
        } else {
            // this is most likely null, meaning we cannot determine if it is Android 7 or not
            // paramedic needs to be modified to receive and pass the platform version when testing locally
            return global.PLATFORM_VERSION && (parseFloat(global.PLATFORM_VERSION) >= 7);
        }
    }


    function getDriver() {
        driver = wdHelper.getDriver('Android');
        return driver.getWebviewContext()
            .then(function(context) {
                webviewContext = context;
                return driver.context(webviewContext);
            })
            .waitForDeviceReady()
            .injectLibraries()
            .then(function () {
                return driver;
            })
            .then(function () {
                // doing it inside a function because otherwise
                // it would not hook up to the webviewContext var change
                // in the first methods of this chain
                return driver.context(webviewContext);
            });
    }

    afterAll(function (done) {
        driver
            .quit()
            .done(done);
    }, MINUTE);

    it('camera.ui.util configuring driver and starting a session', function (done) {
        // retry up to 3 times
        getDriver()
            .fail(function () {
                return getDriver()
                    .fail(function () {
                        return getDriver()
                            .fail(fail);
                    });
            })
            .then(function () {
                appiumSessionStarted = true;
            })
            .done(done);
    }, 30 * MINUTE);

});

