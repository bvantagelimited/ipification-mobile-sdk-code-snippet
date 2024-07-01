
import Foundation
import UIKit
import CoreTelephony

//deprecated
class DeviceInfo {
    
    public static func activeMNC() -> String {
        let activeCarrier = getActiveCarrier()
        return activeCarrier?.mobileNetworkCode ?? ""
    }
    
    public static func activeMCC() -> String {
        let activeCarrier = getActiveCarrier()
        return activeCarrier?.mobileCountryCode ?? ""
    }
    
    private static func getActiveCarrier() -> CTCarrier?{
        if #available(iOS 12.0, *) {
            let info: CTTelephonyNetworkInfo = CTTelephonyNetworkInfo()
            if let carriers = info.serviceSubscriberCellularProviders{
                if(carriers.count == 1){
                    // single SIM
                    let carrier: CTCarrier? = carriers.first?.value
                    return carrier
                }
                else if(carriers.count > 1){
                    // dual SIM
                    if #available(iOS 13.0, *) {
                        let dataServiceIdentifier = info.dataServiceIdentifier
                        if(dataServiceIdentifier != nil && info.serviceSubscriberCellularProviders != nil && info.serviceSubscriberCellularProviders?.index(forKey: dataServiceIdentifier!) != nil){
                            let currentProvider  = info.serviceSubscriberCellularProviders![dataServiceIdentifier!]
                            return currentProvider
                        }
                        return nil
                    } else {
                        // Fallback on earlier versions
                        return nil
                    }
                }
            }
        } else {
            // Fallback on earlier versions
            let info: CTTelephonyNetworkInfo = CTTelephonyNetworkInfo()
            if let carrier = info.subscriberCellularProvider{
                return carrier
            }
        }
        
        return nil
    }
    
    public static func getSIM1() -> CTCarrier?{
        if #available(iOS 12.0, *) {
            let info: CTTelephonyNetworkInfo = CTTelephonyNetworkInfo()
            if let carriers = info.serviceSubscriberCellularProviders{
                if(carriers.count == 1){
                    // single SIM
                    let carrier: CTCarrier? = carriers.first?.value
                    if(carrier != nil){
                        return carrier
                    }
                    else{
                        return nil
                    }
                }
                else if(carriers.count > 1){
                    let carrier = info.serviceSubscriberCellularProviders?.first?.value
                    return carrier
                }
            }
            
        } else {
            // Fallback on earlier versions
            let info: CTTelephonyNetworkInfo = CTTelephonyNetworkInfo()
            if let carrier = info.subscriberCellularProvider{
                return carrier
            }
        }
        return nil
    }
    
    
    private static func getSIM2() -> CTCarrier?{
        if #available(iOS 12.0, *) {
            let info: CTTelephonyNetworkInfo = CTTelephonyNetworkInfo()
            if let carriers = info.serviceSubscriberCellularProviders{
                if(carriers.count > 1){
                    var index = 0
                    for (_ , carrier) in info.serviceSubscriberCellularProviders ?? [:] {
                        if(index == 1){
                            return carrier
                        }
                        index += 1
                    }
                }
            }
            
        }
        return nil
    }
    
    private static func isDualSim() -> Bool{
        if #available(iOS 12.0, *) {
            let info: CTTelephonyNetworkInfo = CTTelephonyNetworkInfo()
            if let carriers = info.serviceSubscriberCellularProviders{
                if(carriers.count > 1){
                    return true
                }
            }
        }
        return false
    }
    
}
