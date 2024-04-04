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

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        
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
