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

import android.app.AlertDialog;
//import android.app.FragmentManager;
//import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import org.apache.cordova.PluginManager;
import org.elastos.trinity.runtime.R;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class AppManager {

    /** The internal message */
    public static final int MSG_TYPE_INTERNAL = 1;
    /** The internal return message. */
    public static final int MSG_TYPE_IN_RETURN = 2;
    /** The external launcher message */
    public static final int MSG_TYPE_EXTERNAL_LAUNCHER = 3;
    /** The external install message */
    public static final int MSG_TYPE_EXTERNAL_INSTALL = 4;
    /** The external return message. */
    public static final int MSG_TYPE_EX_RETURN = 5;

    private static AppManager appManager;
    public WebViewActivity activity;
    public WebViewFragment curFragment = null;
    ManagerDBAdapter dbAdapter = null;

    public String appsPath = null;
    public String dataPath = null;
    public String tempPath = null;

    private AppInstaller installer;

    protected LinkedHashMap<String, AppInfo> appInfos;
    private ArrayList<String> lastList = new ArrayList<String>();
    public AppInfo[] appList;

    private AppInfo launcherInfo;

    private ArrayList<String>  installUriList = new ArrayList<String>();
    private boolean launcherReady = false;

    AppManager(WebViewActivity activity) {
        AppManager.appManager = this;
        this.activity = activity;

        appsPath = activity.getFilesDir() + "/apps/";
        dataPath = activity.getFilesDir() + "/data/";
        tempPath = activity.getFilesDir() + "/temp/";

        Boolean first = false;
        File destDir = new File(appsPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
            first = true;
        }
        destDir = new File(dataPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        destDir = new File(tempPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        destDir = new File(dataPath + "launcher");
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        dbAdapter = new ManagerDBAdapter(activity);
//        dbAdapter.clean();

        installer = new AppInstaller();
        installer.init(activity, dbAdapter, appsPath, dataPath, tempPath);

        if (first) {
            saveLauncherInfo();
        }

        appList = dbAdapter.getAppInfos();
        saveBuiltInAppInfos();

        refreashInfos();
    }

    public static AppManager getShareInstance() {
        return AppManager.appManager;
    }

    private void saveLauncherInfo() {
        AssetManager manager = activity.getAssets();
        try {
            InputStream input = manager.open("www/launcher/manifest.json");
            AppInfo info = installer.parseManifest(input, 1);
            installer.copyAssetsFolder("www/launcher", appsPath + info.app_id);
            info.built_in = 1;
            dbAdapter.addAppInfo(info);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AppInfo getLauncherInfo() {
        if (launcherInfo == null) {
            launcherInfo = dbAdapter.getLauncherInfo();
        }
        return launcherInfo;
    }

    private void refreashInfos() {
        appList = dbAdapter.getAppInfos();
        appInfos = new LinkedHashMap();
        for (int i = 0; i < appList.length; i++) {
            appInfos.put(appList[i].app_id, appList[i]);
        }
    }

    public AppInfo getAppInfo(String id) {
        if (id == "launcher") {
            return getLauncherInfo();
        }
        else {
            return appInfos.get(id);
        }
    }

    public HashMap<String, AppInfo> getAppInfos() {
        return appInfos;
    }

    public String getStartPath(AppInfo info) {
        if (info.remote == 0) {
            return getAppUrl(info) + info.start_url;
        }
        else {
            return info.start_url;
        }
    }

    public String getAppPath(AppInfo info) {
        if (info.remote == 0) {
            return appsPath + info.app_id + "/";
        }
        else {
            return info.start_url.substring(0, info.start_url.lastIndexOf("/") + 1);
        }
    }

    public String getAppUrl(AppInfo info) {
        String url = getAppPath(info);
        if (info.remote == 0) {
            url = "file://" + url;
        }
        return url;
    }

    public String getDataPath(String id) {
        if (id == "launcher") {
            id = getLauncherInfo().app_id;
        }
        return dataPath + id + "/";
    }

    public String getDataUrl(String id) {
        return "file://" + getDataPath(id);
    }


    public String getTempPath(String id) {
        if (id == "launcher") {
            id = getLauncherInfo().app_id;
        }
        return tempPath + id + "/";
    }

    public String getTempUrl(String id) {
        return "file://" + getTempPath(id);
    }

    public String resetPath(String dir, String origin) {
        if (origin.indexOf("http://") != 0 && origin.indexOf("https://") != 0
                && origin.indexOf("file:///") != 0) {
            while (origin.startsWith("/")) {
                origin = origin.substring(1);
            }
            origin = dir + origin;
        }
        return origin;
    }

    public void saveBuiltInAppInfos(){
        AssetManager manager = activity.getAssets();
        try {
            String[] appdirs= manager.list("www/built-in");
            for (String appdir : appdirs) {
                boolean needInstall = true;
                for (int i = 0; i < appList.length; i++) {
                    if (appdir.equals(appList[i].app_id)) {
                        needInstall = false;
                        break;
                    }
                }

                if (needInstall) {
                    installer.copyAssetsFolder("www/built-in/" + appdir, appsPath + appdir);
                    InputStream input = new FileInputStream(appsPath + appdir + "/manifest.json");
                    AppInfo info = installer.parseManifest(input, 0);

                    info.built_in = 1;
                    dbAdapter.addAppInfo(info);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AppInfo install(String url) throws Exception  {
        AppInfo info = installer.install(this, url);
        if (info != null) {
            refreashInfos();
        }
        return info;
    }

    public void unInstall(String id) throws Exception {
        close(id);
        installer.unInstall(appInfos.get(id));
        refreashInfos();
    }

    private WebViewFragment findFragmentById(String id) {
        FragmentManager manager = activity.getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();
        for (int i = 0; i < fragments.size(); i++) {
            WebViewFragment fragment = (WebViewFragment)fragments.get(i);
            if ((fragment != null) && (fragment.id.equals(id))) {
                return fragment;
            }
        }
        return null;
    }

    public void switchContent(WebViewFragment fragment, String id) {
        FragmentManager manager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if ((curFragment != null) && (curFragment != fragment)) {
            transaction.hide(curFragment);
//            if (!curFragment.id.equals("launcher")) {
//                curFragment.onPause();
//            }
        }
        if (curFragment != fragment) {
            if (!fragment.isAdded()) {
                transaction.add(R.id.content, fragment, id);
            } else if (curFragment != fragment) {
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(fragment);
            }
//            transaction.addToBackStack(null);
            transaction.commit();
//            if (!id.equals("launcher")) {
//                fragment.onResume();
//            }
            curFragment = fragment;
        }
        lastList.remove(id);
        lastList.add(0, id);
    }

    public boolean doBackPressed() {
        if (curFragment.id.equals("launcher")) {
            return true;
        }
        else {
            switchContent(findFragmentById("launcher"), "launcher");
            return false;
        }
    }

    public void start(String id) throws Exception {
        WebViewFragment fragment = findFragmentById(id);
        if (fragment == null) {
            if (id.equals("launcher")) {
                fragment = LauncherViewFragment.newInstance();
            }
            else {
                AppInfo info = appInfos.get(id);
                if (info == null) {
                    throw new Exception("No such app!");
                }
                fragment = AppViewFragment.newInstance(id);
            }
        }
//        else if (!id.equals("launcher")) {
//            fragment.onResume();
//        }
        switchContent(fragment, id);
    }

    public void close(String id) throws Exception {
        if (id.equals("launcher")) {
            throw new Exception("Launcher can't close!");
        }

        AppInfo info = appInfos.get(id);
        if (info == null) {
            throw new Exception("No such app!");
        }

        FragmentManager manager = activity.getSupportFragmentManager();
        WebViewFragment fragment = findFragmentById(id);
        if (fragment == null) {
            return;
        }

        if (fragment == curFragment) {
            String id2 = lastList.get(1);
            WebViewFragment fragment2 = findFragmentById(id2);
            if (fragment2 == null) {
                throw new Exception("RT inner error!");
            }
            switchContent(fragment2, id2);
        }

//        if (fragment.appView != null) {
//            fragment.appView.handleDestroy();
//        }

        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(fragment);
        transaction.commit();
        lastList.remove(id);
    }

    public void loadLauncher() throws Exception {
        start("launcher");
    }

    public void setInstallUri(String uri) {
        if (uri == null) return;

        if (launcherReady) {
            try {
                sendMessage("launcher", MSG_TYPE_EXTERNAL_INSTALL, uri, "system");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            installUriList.add(uri);
        }
    }

    public boolean isLauncherReady() {
        return launcherReady;
    }

    public void setLauncherReady() {
        launcherReady = true;

        for (int i = 0; i < installUriList.size(); i++) {
            String uri = installUriList.get(i);
            try {
                sendMessage("launcher", MSG_TYPE_EXTERNAL_INSTALL, uri, "system");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void sendMessage(String toId, int type, String msg, String fromId) throws Exception {
        FragmentManager manager = activity.getSupportFragmentManager();
        WebViewFragment fragment = (WebViewFragment)manager.findFragmentByTag(toId);
        if (fragment != null) {
            fragment.basePlugin.onReceive(msg, type, fromId);
        }
        else {
            throw new Exception(toId + " isn't running!");
        }
    }

    public int getPluginAuthority(String id, String plugin) {
        AppInfo info = appInfos.get(id);
        if (info != null) {
            for (AppInfo.PluginAuth pluginAuth : info.plugins) {
                if (pluginAuth.plugin.equals(plugin)) {
                    return pluginAuth.authority;
                }
            }
        }
        return AppInfo.AUTHORITY_NOEXIST;
    }

    public int getUrlAuthority(String id, String url) {
        AppInfo info = appInfos.get(id);
        if (info != null) {
            for (AppInfo.UrlAuth urlAuth : info.urls) {
                if (urlAuth.url.equals(url)) {
                    return urlAuth.authority;
                }
            }
        }
        return AppInfo.AUTHORITY_NOEXIST;
    }

    public void setPluginAuthority(String id, String plugin, int authority) throws Exception {
        AppInfo info = appInfos.get(id);
        if (info == null) {
            throw new Exception("No such app!");
        }

        dbAdapter.updatePluginAuth(info.tid, plugin, authority);
        for (AppInfo.PluginAuth pluginAuth : info.plugins) {
            if (pluginAuth.plugin.equals(plugin)) {
                pluginAuth.authority = authority;
                return;
            }
        }
        throw new Exception("The plugin isn't in list!");
    }

    public void setUrlAuthority(String id, String url, int authority)  throws Exception {
        AppInfo info = appInfos.get(id);
        if (info == null) {
            throw new Exception("No such app!");
        }

        int count = dbAdapter.updateURLAuth(info.tid, url, authority);
        if (count > 0) {
            for (AppInfo.UrlAuth urlAuth : info.urls) {
                if (urlAuth.url.equals(url)) {
                    urlAuth.authority = authority;
                    return ;
                }
            }
        }
        throw new Exception("The plugin isn't in list!");
    }

    private static void print(String msg) {
        String name = Thread.currentThread().getName();
        System.out.println(name + ": " + msg);
    }


    private class LockObj {
        int authority = AppInfo.AUTHORITY_NOINIT;
    }

    public synchronized int runAlertPluginAuth(AppInfo info, String plugin, int originAuthority) {
        LockObj lock = new LockObj();
        lock.authority = originAuthority;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertPluginAuth(info, plugin, lock);
            }
        });
        try {
            synchronized (lock) {
                if (lock.authority == originAuthority) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return originAuthority;
        }
        return lock.authority;
    }

    public void alertPluginAuth(AppInfo info, String plugin, LockObj lock) {
        AlertDialog.Builder ab = new AlertDialog.Builder(activity);
        ab.setTitle("Plugin authority request");
        ab.setMessage("App:'" + info.name + "' request plugin:'" + plugin + "' access authority.");
        ab.setIcon(android.R.drawable.ic_dialog_info);


        ab.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    setPluginAuthority(info.app_id, plugin, AppInfo.AUTHORITY_ALLOW);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (lock) {
                    lock.authority = AppInfo.AUTHORITY_ALLOW;
                    lock.notify();
                }
            }
        });
        ab.setNegativeButton("Refuse", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    setPluginAuthority(info.app_id, plugin, AppInfo.AUTHORITY_DENY);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (lock) {
                    lock.authority = AppInfo.AUTHORITY_DENY;
                    lock.notify();
                }
            }
        });
        ab.show();
    }

    public synchronized int runAlertUrlAuth(AppInfo info, String url, int originAuthority) {
        LockObj lock = new LockObj();
        lock.authority = originAuthority;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertUrlAuth(info, url, lock);
            }
        });
        try {
            synchronized (lock) {
                if (lock.authority == originAuthority) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return originAuthority;
        }
        return lock.authority;
    }

    public void alertUrlAuth(AppInfo info, String url, LockObj lock) {
        AlertDialog.Builder ab = new AlertDialog.Builder(activity);
        ab.setTitle("Url authority request");
        ab.setMessage("App:'" + info.name + "' request url:'" + url + "' access authority.");
        ab.setIcon(android.R.drawable.ic_dialog_info);

        ab.setPositiveButton("Allow", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    setUrlAuthority(info.app_id, url, AppInfo.AUTHORITY_ALLOW);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (lock) {
                    lock.authority = AppInfo.AUTHORITY_ALLOW;
                    lock.notify();
                }
            }
        });
        ab.setNegativeButton("Refuse", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    setUrlAuthority(info.app_id, url, AppInfo.AUTHORITY_DENY);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (lock) {
                    lock.authority = AppInfo.AUTHORITY_DENY;
                    lock.notify();
                }
            }
        });
        ab.show();
    }

    public String[] getAppList() {
        String[] ids = new String[appList.length];
        for (int i = 0; i < appList.length; i++) {
            ids[i] = appList[i].app_id;
        }
        return ids;
    }

    public String[] getRunningList() {
        FragmentManager manager = activity.getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();
        String[] ids = new String[fragments.size()];

        for (int i = 0; i < fragments.size(); i++) {
            WebViewFragment fragment = (WebViewFragment)fragments.get(i);
            ids[i] = fragment.id;
        }
        return ids;
    }

    public String[] getLastList() {
        String[] ids = new String[lastList.size()];
        return lastList.toArray(ids);
    }


    public void flingTheme() {
        if (curFragment.id.equals("launcher")) {
            return;
        }

        AppViewFragment fragment = (AppViewFragment) curFragment;
        if (fragment.titlebar.getVisibility() == View.VISIBLE) {
            fragment.titlebar.setVisibility(View.GONE);
        } else {
            fragment.titlebar.bringToFront();//for qrscanner
            fragment.titlebar.setVisibility(View.VISIBLE);
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        FragmentManager manager = activity.getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();

        for (int i = 0; i < fragments.size(); i++) {
            WebViewFragment fragment = (WebViewFragment)fragments.get(i);
            if (fragment != null && fragment.appView != null) {
                PluginManager pm = fragment.appView.getPluginManager();
                if (pm != null) {
                    pm.onConfigurationChanged(newConfig);
                }
            }
        }
    }

    public void onRequestPermissionResult(int requestCode, String permissions[],
                                           int[] grantResults) throws JSONException {
        FragmentManager manager = activity.getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();

        for (int i = 0; i < fragments.size(); i++) {
            WebViewFragment fragment = (WebViewFragment)fragments.get(i);
            if (fragment != null) {
                fragment.onRequestPermissionResult(requestCode, permissions, grantResults);
            }
        }
    }
}
