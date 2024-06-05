//
//  MainViewController.swift
//  OurGrid
//
//  Created by Michael Rademaker on 11/10/2021.
//

import UIKit
import ORLib

class MainViewController: ORViewcontroller {
    
    lazy var queryParameters: String = {
        return "consolePlatform=iOS \(UIDevice.current.systemVersion)&consoleName=ourGrid&consoleVersion=\(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "N/A")&consoleProviders=push storage"
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        let url = Bundle.main.object(forInfoDictionaryKey: "BaseURL") as! String
        self.baseUrl =  "\(url)/cityselector/?\(queryParameters)"
        if let encodedUrl = self.baseUrl!.addingPercentEncoding(withAllowedCharacters: .urlFragmentAllowed){
            loadURL(url: URL(string: encodedUrl)!)
        }
    }
}
