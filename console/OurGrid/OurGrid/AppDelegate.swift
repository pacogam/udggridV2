//
//  AppDelegate.swift
//  OurGrid
//
//  Created by Michael Rademaker on 11/10/2021.
//

import UIKit
import Firebase
import ORLib

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var fcmToken: String?
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        if let bundleIdentifier = Bundle.main.bundleIdentifier {
            DefaultsKey.groupEntitlement = "group.\(bundleIdentifier)"
        }

        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        
        return true
    }

    // MARK: UISceneSession Lifecycle

    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        // Called when a new scene session is being created.
        // Use this method to select a configuration to create the new scene with.
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
        // Called when the user discards a scene session.
        // If any sessions were discarded while the application was not running, this will be called shortly after application:didFinishLaunchingWithOptions.
        // Use this method to release any resources that were specific to the discarded scenes, as they will not return.
    }
}

extension AppDelegate : UNUserNotificationCenterDelegate {

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo
        var notificationId : Int64? = nil

        if let notificationIdString = userInfo[ActionType.notificationId] as? String{
            notificationId = Int64(notificationIdString)
        }
        if let notiId = notificationId, let defaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement), let consoleId = defaults.string(forKey: GeofenceProvider.consoleIdKey) {
            ORNotificationResource.sharedInstance.notificationDelivered(notificationId: notiId, targetId: consoleId)
        }

        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {

        let userInfo = response.notification.request.content.userInfo
        var notificationId : Int64? = nil
        var consoleId : String?
        var project: ProjectConfig?

        if let notificationIdString = userInfo[ActionType.notificationId] as? String{
            notificationId = Int64(notificationIdString)
        }
        
        if let userDefaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement) {
            consoleId = userDefaults.string(forKey: GeofenceProvider.consoleIdKey) // TODO: geofence provider should also be adapted to store "per project"
            
            let selectedProjectId = userDefaults.string(forKey: DefaultsKey.projectKey)
            if let projectsData = userDefaults.data(forKey: DefaultsKey.projectsConfigurationKey) {
                let projects = (try? JSONDecoder().decode([ProjectConfig].self, from: projectsData)) ?? []
                project = projects.first(where:{ $0.id == selectedProjectId })
            }
        }

        NSLog("%@", "Action chosen: \(response.actionIdentifier)")

        switch response.actionIdentifier {
        case UNNotificationDefaultActionIdentifier:
            if let urlTo = userInfo[ActionType.appUrl] as? String, !urlTo.isEmpty {
                var urlRequest: URL?
                if urlTo.hasPrefix("http") || urlTo.hasPrefix("https") {
                    urlRequest = URL(string:urlTo)
                } else {
                    if let url = project?.baseURL {
                        urlRequest = URL(string: "\(url)/console/\(urlTo)")
                    }
                }
                if let url = urlRequest{
                    if let InBrowser = userInfo[ActionType.openInBrowser] as? Bool, InBrowser {
                        NSLog("%@", " in browser: \(url)")
                        UIApplication.shared.open(url)
                    } else {
                        NSLog("%@", " in app: \(url)")
                        (self.window?.topController as? ORViewcontroller)?.loadURL(url:url)
                    }
                }
            }
        case UNNotificationDismissActionIdentifier,
             "declineAction":
            if let notiId = notificationId, let conId = consoleId {
                ORNotificationResource.sharedInstance.notificationAcknowledged(notificationId: notiId, targetId: conId, acknowledgement: response.actionIdentifier)
            }
        default :
            if let notiId = notificationId, let conId = consoleId {
                ORNotificationResource.sharedInstance.notificationAcknowledged(notificationId: notiId, targetId: conId, acknowledgement: response.actionIdentifier)
            }
            if let buttonsString = userInfo[DefaultsKey.buttonsKey] as? String {
                if let buttonsData = buttonsString.data(using: .utf8) {
                    if let buttons = try? JSONDecoder().decode([ORPushNotificationButton].self, from: buttonsData) {
                        for button in buttons {
                            if button.title == response.actionIdentifier {
                                if let action = button.action {
                                    var urlRequest: URL?
                                    if action.url.hasPrefix("http") || action.url.hasPrefix("https") {
                                        urlRequest = URL(string:action.url)
                                    } else {
                                        if let url = project?.baseURL {
                                            urlRequest = URL(string: "\(url)/console/\(action.url)")
                                        }
                                    }
                                    if let url = urlRequest {
                                        if action.silent {
                                            let request = NSMutableURLRequest(url: url)
                                            request.httpMethod = action.httpMethod ?? "GET"
                                            if let body = action.data {
                                                request.httpBody = body.data(using: .utf8)
                                                request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                                            }
                                            let session = URLSession(configuration: URLSessionConfiguration.default, delegate: nil, delegateQueue : nil)
                                            let reqDataTask = session.dataTask(with: request as URLRequest, completionHandler:{ data, response, error in
                                                if (error != nil) {
                                                    NSLog("error %@", (error! as NSError).localizedDescription)
                                                }
                                            })
                                            reqDataTask.resume()
                                        } else if action.openInBrowser {
                                            NSLog("%@", " in browser: \(url)")
                                            UIApplication.shared.open(url)
                                        } else {
                                            NSLog("%@", " in app: \(url)")
                                            (self.window?.topController as? ORViewcontroller)?.loadURL(url:url)
                                        }
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
        completionHandler()
    }
}

extension AppDelegate : MessagingDelegate {
    func messaging(_ messaging: Messaging, didRefreshRegistrationToken fcmToken: String) {
        print("Firebase registration token: \(fcmToken)")
        if let defaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement){
            defaults.set(fcmToken, forKey: DefaultsKey.fcmTokenKey)
            defaults.synchronize()
        }
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let token = fcmToken {
            print("Firebase registration token: \(token)")
            if let defaults = UserDefaults(suiteName: DefaultsKey.groupEntitlement) {
                defaults.set(token, forKey: DefaultsKey.fcmTokenKey)
                defaults.synchronize()
            }
        } else {
            print("No fcm token")
        }
    }
}
