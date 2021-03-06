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

import Foundation

class AppManager {
    private static var appManager: AppManager?;
    
    /** The internal message */
    static let MSG_TYPE_INTERNAL = 1;
    /** The internal return message. */
    static let MSG_TYPE_IN_RETURN = 2;
    /** The external launcher message */
    static let MSG_TYPE_EXTERNAL_LAUNCHER = 3;
    /** The external install message */
    static let MSG_TYPE_EXTERNAL_INSTALL = 4;
    /** The external return message. */
    static let MSG_TYPE_EX_RETURN = 5;
    
    let mainViewController: MainViewController;
    var viewControllers = [String: TrinityViewController]();
    
    let appsPath: String;
    let dataPath: String;
    let tempPath: String;
    
    var curController: TrinityViewController?;
    
    let dbAdapter: ManagerDBAdapter;
    
    var appList: [AppInfo];
    var appInfos = [String: AppInfo]();
    var lastList = [String]();
    let installer: AppInstaller;
    
    private var launcherInfo: AppInfo? = nil;
    
    var installUriList = [String]();
    var launcherReady = false;
    
    init(_ mainViewController: MainViewController) {
        self.mainViewController = mainViewController;
        appsPath = NSHomeDirectory() + "/Documents/apps/";
        dataPath = NSHomeDirectory() + "/Documents/data/";
        tempPath = NSHomeDirectory() + "/Documents/temp/";
        
        let fileManager = FileManager.default
        var first = false;
        if (!fileManager.fileExists(atPath: appsPath)) {
            do {
                try fileManager.createDirectory(atPath: appsPath, withIntermediateDirectories: true, attributes: nil)
                first = true;
            }
            catch let error {
                print("Make appsPath error: \(error)");
            }     
        }
        
        if (!fileManager.fileExists(atPath: dataPath)) {
            do {
                try fileManager.createDirectory(atPath: dataPath, withIntermediateDirectories: true, attributes: nil)
            }
            catch let error {
                print("Make dataPath error: \(error)");
            }
        }
        
        if (!fileManager.fileExists(atPath: tempPath)) {
            do {
                try fileManager.createDirectory(atPath: tempPath, withIntermediateDirectories: true, attributes: nil)
            }
            catch let error {
                print("Make tempPath error: \(error)");
            }
        }
        
        dbAdapter = ManagerDBAdapter(dataPath);
        installer = AppInstaller(appsPath, dataPath, tempPath, dbAdapter);
        appList = try! dbAdapter.getAppInfos();
        
        if first {
            saveLauncherInfo();
        }
        
        saveBuiltInAppInfos();
        refreashInfos();
        
        AppManager.appManager = self;
    }
    
    static func getShareInstance() -> AppManager {
        return AppManager.appManager!;
    }
    
    func saveLauncherInfo() {
        let path = getAbsolutePath("www/launcher");
        do {
            let info = try installer.parseManifest(path + "/manifest.json", true)!;
            try installer.copyAssetsFolder(path, appsPath + info.app_id);
            info.built_in = true;
            try dbAdapter.addAppInfo(info);
        }
        catch let error {
            print("Copy launcher error: \(error)");
        }
    }
    
    func getLauncherInfo() -> AppInfo {
        if launcherInfo == nil {
            launcherInfo = try! dbAdapter.getLauncherInfo();
        }
        return launcherInfo!;
    }
    
    func refreashInfos() {
        appList = try! dbAdapter.getAppInfos();
        appInfos = [String: AppInfo]();
        for info in appList {
            appInfos[info.app_id] = info;
        }
    }
    
    func getAppInfo(_ id: String) -> AppInfo? {
        if (id == "launcher") {
            return getLauncherInfo();
        }
        else {
            return appInfos[id];
        }
    }
    
    func getAppInfos() -> [String: AppInfo] {
        return appInfos;
    }
    
    func getStartPath(_ info: AppInfo) -> String {
        if (!info.remote) {
            return getAppUrl(info) + info.start_url;
        }
        else {
            return info.start_url;
        }
    }
    
    func getAppPath(_ info: AppInfo) -> String {
        if (!info.remote) {
            return appsPath + info.app_id + "/";
        }
        else {
            let index = info.start_url.range(of: "/", options: .backwards)?.lowerBound
            return String(info.start_url[info.start_url.startIndex ..< index!]);
        }
    }
    
    func getAppUrl(_ info: AppInfo) -> String {
        var url = getAppPath(info);
        if (!info.remote) {
            url = "file://" + url;
        }
        return url;
    }
    
    func getDataPath(_ id: String) -> String {
        var appId = id;
        if (id == "launcher") {
            appId = getLauncherInfo().app_id;
        }
        return dataPath + appId + "/";
    }
    
    func getDataUrl(_ id: String) -> String {
        return "file://" + getDataPath(id);
    }
    
    func getTempPath(_ id: String) -> String {
        var appId = id;
        if (id == "launcher") {
            appId = getLauncherInfo().app_id;
        }
        return tempPath + appId + "/";
    }
    
    func getTempUrl(_ id: String) -> String {
        return "file://" + getTempPath(id);
    }
    
    func saveBuiltInAppInfos() {
        let path = getAbsolutePath("www/built-in") + "/";
        
        let fileManager = FileManager.default;
        let dirs = try? fileManager.contentsOfDirectory(atPath: path);
        guard dirs != nil else {
            return;
        }
        
        do {
            for dir in dirs! {
                var needInstall = true;
                for info in appList {
                    if info.app_id == dir {
                        needInstall = false;
                        break;
                    }
                }
                
                if (needInstall) {
                    try installer.copyAssetsFolder(path +  dir, appsPath + dir);
                    let info = try installer.parseManifest(appsPath +  dir + "/manifest.json")
                    guard (info != nil || info!.app_id != "") else {
                        return;
                    }
                    
                    info!.built_in = true;
                    try dbAdapter.addAppInfo(info!);
                }
            }
        } catch AppError.error(let err) {
            print(err);
        } catch let error {
            print(error.localizedDescription);
        }
    }

    func install(_ url: String) throws -> AppInfo? {
        let info = try! installer.install(self, url);
        if (info != nil) {
            refreashInfos();
        }
        return info;
    }
    
    func unInstall(_ id: String) throws {
        try close(id);
        try installer.unInstall(appInfos[id]);
        refreashInfos();
    }
    
    func removeLastlistItem(_ id: String) {
        for (index, item) in lastList.enumerated() {
            if item == id {
                lastList.remove(at: index);
                break;
            }
        }
    }
    
    func switchContent(_ to: TrinityViewController, _ id: String) {
        mainViewController.switchController(from: curController!, to: to)
        removeLastlistItem(id);
        lastList.insert(id, at: 0);
    }

    func start(_ id: String) throws {
        var viewController = viewControllers[id]
        if viewController == nil {
            if (id == "launcher") {
                viewController = LauncherViewController();
            }
            else {
                let appInfo = appInfos[id];
                guard appInfo != nil else {
                    throw AppError.error("No such app!");
                }
                let appViewController = AppViewController();
                appViewController.setInfo(id, appInfo!);
                viewController = appViewController;
//                viewController?.startPage = "built-in/org.elastos.trinity.demo1/demo1.html";
            }
            
            mainViewController.add(viewController!)
            viewControllers[id] = viewController;
            lastList.insert(id, at: 0);
        }
        else {
            if (curController != viewController) {
                switchContent(viewController!, id);
            }
        }
        
        curController = viewController
    }
    
    func close(_ id: String) throws {
        if (id == "launcher") {
            throw AppError.error("Launcher can't close!");
        }
        
        let info = appInfos[id];
        if (info == nil) {
            throw AppError.error("No such app!");
        }
    
        let viewController = viewControllers[id]
        if (viewController == nil) {
            return;
        }
    
        if (viewController == curController) {
            let id2 = lastList[1];
            let viewController2 = viewControllers[id2]
            if (viewController2 == nil) {
                throw AppError.error("RT inner error!");
            }
            switchContent(viewController2!, id2);
        }
        
        removeLastlistItem(id);
        viewControllers[id] = nil;
        viewController!.remove();
    }
    
    
    func loadLauncher() throws {
        try start("launcher");
    }
    
    func setInstallUri(_ uri: String) {
        if launcherReady {
            try? self.sendMessage("launcher", AppManager.MSG_TYPE_EXTERNAL_INSTALL, uri, "system");
        }
        else {
            installUriList.append(uri);
        }
    }
    
    func isLauncherReady() -> Bool {
        return launcherReady;
    }
    
    func setLauncherReady() {
        launcherReady = true;
    
        for uri in installUriList {
            try? self.sendMessage("launcher", AppManager.MSG_TYPE_EXTERNAL_INSTALL, uri, "system");
        }
    }

    func sendMessage(_ toId: String, _ type: Int, _ msg: String, _ fromId: String) throws {
        let viewController = viewControllers[toId]
        if (viewController != nil) {
            viewController!.basePlugin!.onReceive(msg, type, fromId);
        }
        else {
            throw AppError.error(toId + " isn't running!");
        }
    }
    
    func getPluginAuthority(_ id: String, _ plugin: String) -> Int {
        let info = appInfos[id];
        if (info != nil) {
            for pluginAuth in info!.plugins {
                if (pluginAuth.plugin == plugin) {
                    return pluginAuth.authority;
                }
            }
        }
        return AppInfo.AUTHORITY_NOEXIST;
    }
    
    func getUrlAuthority(_ id: String, _ url: String) -> Int {
        let info = appInfos[id];
        if (info != nil) {
            for urlAuth in info!.urls {
                if (urlAuth.url == url) {
                    return urlAuth.authority;
                }
            }
        }
        return AppInfo.AUTHORITY_NOEXIST;
    }
    
    func setPluginAuthority(_ id: String, _ plugin: String, _ authority: Int) throws {
        let info = appInfos[id];
        guard (info != nil) else {
            throw AppError.error("No such app!");
        }
        
        for pluginAuth in info!.plugins {
            if (pluginAuth.plugin == plugin) {
                try dbAdapter.updatePluginAuth(pluginAuth, authority);
                pluginAuth.authority = authority;
                return;
            }
        }
        throw AppError.error("The plugin isn't in list!");
    }
    
    func setUrlAuthority(_ id: String, _ url: String, _ authority: Int) throws {
        let info = appInfos[id];
        guard (info != nil) else {
            throw AppError.error("No such app!");
        }
        
        for urlAuth in info!.urls {
            if (urlAuth.url == url) {
                try dbAdapter.updateUrlAuth(urlAuth, authority);
                urlAuth.authority = authority;
                return;
            }
        }
        throw AppError.error("The url isn't in list!");
    }

    func runAlertPluginAuth(_ info: AppInfo, _ pluginName: String,
                            _ plugin: TrinityPlugin,
                            _ command: CDVInvokedUrlCommand) {

        func doAllowHandler(alerAction:UIAlertAction) {
            try? setPluginAuthority(info.app_id, pluginName, AppInfo.AUTHORITY_ALLOW);
            plugin.execute(command);
            let result = CDVPluginResult(status: CDVCommandStatus_NO_RESULT);
            result?.setKeepCallbackAs(false);
            plugin.commandDelegate?.send(result, callbackId:command.callbackId);
        }
        
        func doRefuseHandler(alerAction:UIAlertAction) {
            try? setPluginAuthority(info.app_id, pluginName, AppInfo.AUTHORITY_DENY);
            let result = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                         messageAs: "Plugin:'" + pluginName + "' have not run authority.");
            result?.setKeepCallbackAs(false);
            plugin.commandDelegate.send(result, callbackId: command.callbackId)
        }
        
        let alertController = UIAlertController(title: "Plugin authority request",
                message: "App:'" + info.name + "' request plugin:'" + pluginName + "' access authority.",
                preferredStyle: UIAlertController.Style.alert)
        let cancelAlertAction = UIAlertAction(title: "Refuse", style: UIAlertAction.Style.cancel, handler: doRefuseHandler)
        alertController.addAction(cancelAlertAction)
        let sureAlertAction = UIAlertAction(title: "Allow", style: UIAlertAction.Style.default, handler: doAllowHandler)
        alertController.addAction(sureAlertAction)
        
        self.mainViewController.present(alertController, animated: true, completion: nil)
    }
    
    func runAlertUrlAuth(_ info: AppInfo, _ url: String) {
        func doAllowHandler(alerAction:UIAlertAction) {
            try? setUrlAuthority(info.app_id, url, AppInfo.AUTHORITY_ALLOW);
        }
        
        func doRefuseHandler(alerAction:UIAlertAction) {
            try? setUrlAuthority(info.app_id, url, AppInfo.AUTHORITY_DENY);
        }
        
        let alertController = UIAlertController(title: "Url authority request",
                                                message: "App:'" + info.name + "' request url:'" + url + "' access authority.",
                                                preferredStyle: UIAlertController.Style.alert)
        let cancelAlertAction = UIAlertAction(title: "Refuse", style: UIAlertAction.Style.cancel, handler: doRefuseHandler)
        alertController.addAction(cancelAlertAction)
        let sureAlertAction = UIAlertAction(title: "Allow", style: UIAlertAction.Style.default, handler: doAllowHandler)
        alertController.addAction(sureAlertAction)
        self.mainViewController.present(alertController, animated: true, completion: nil)
    }
    
    func getRunningList() -> [String] {
        var ret = [String]();
        for id in viewControllers.keys {
            ret.append(id);
        }
        return ret;
    }
    
    func getAppList() -> [String] {
        var ret = [String]();
        for info in appList {
            ret.append(info.app_id);
        }
        return ret;
    }
    
    func getLastList() -> [String] {
        return lastList;
    }
    
}
