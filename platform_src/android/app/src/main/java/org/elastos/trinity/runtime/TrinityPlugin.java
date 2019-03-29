/*
 * Copyright (c) 2018 Elastos Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.elastos.trinity.runtime;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebView;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TrinityPlugin extends CordovaPlugin {
    private AppWhitelistPlugin whitelistPlugin;
    public String dataPath = null;
    public String appPath = null;
    private AppInfo appInfo = null;

    public void setWhitelistPlugin(AppWhitelistPlugin appWhitelistPlugin) {
        this.whitelistPlugin = appWhitelistPlugin;
    }

    public void setInfo(AppInfo info) {
        this.appInfo = info;
        this.dataPath = AppManager.appManager.getDataPath(info.app_id);
        this.appPath = AppManager.appManager.getAppPath(info);
    }

    public String getDataPath() {
        return dataPath;
    }

    public String getAppPath() {
        return appPath;
    }

    public Boolean isAllowAccess(String url) {
        return whitelistPlugin.shouldAllowNavigation(url);
    }

    private String getCanonicalDir(String path, String header) throws Exception {
        File file = new File(path);
        path = file.getCanonicalPath();
        if (!header.startsWith("/")) {
            path = path.substring(1);
        }
        if (!path.startsWith(header)) {
            throw new Exception("Dir is invalid!");
        }
        String dir = path.substring(header.length());
        return dir;
    }

    public String getCanonicalPath(String path) throws Exception {
        String ret = null;
        if (path.startsWith("assets/")) {
            String dir = getCanonicalDir(path, "assets/");
            ret = appPath + dir;
        }
        else if (path.startsWith("data/")) {
            String dir = getCanonicalDir(path, "data/");
            ret = dataPath + dir;
        }
        else if (!(path.startsWith("/")) && !(path.startsWith("assets://")) && (path.indexOf("://") != -1)) {
            if (whitelistPlugin.shouldAllowNavigation(path)) {
                ret = path;
            }
        }
        if (ret == null) {
            throw new Exception("Dir is invalid!");
        }
        return ret;
    }

    public String getRelativePath(String path) throws Exception {
        String ret = null;
        if (path.startsWith(appPath)) {
            String header = appPath;
            if (appInfo.built_in == 1) {
                path = path.substring(8);
                header = header.substring(8);
            }
            String dir = getCanonicalDir(path, header);
            ret = "asserts/" + dir;
        }
        else if (path.startsWith(dataPath)) {
            ret = "data/" + getCanonicalDir(path, dataPath);
        }
        else if (!(path.startsWith("/")) && !(path.startsWith("assets://")) && (path.indexOf("://") != -1)) {
            if (whitelistPlugin.shouldAllowNavigation(path)) {
                ret = path;
            }
        }
        if (ret == null) {
            throw new Exception("Dir is invalid!");
        }
        return ret;
    }

    @Override
    public final void privateInitialize(String serviceName, CordovaInterface cordova,
                                        CordovaWebView webView, CordovaPreferences preferences) {
        super.privateInitialize(serviceName, cordova, webView, preferences);
    }
}