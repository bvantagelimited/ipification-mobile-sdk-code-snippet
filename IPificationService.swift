//
//  IPificationService.swift
//  IPificationSDK-Demo
//
//  Created by IPification on 29/02/2024.
//  Copyright © 2024 IPification. All rights reserved.
//

import Foundation
import Network

enum RequestType {
    case coverage
    case auth
    case redirect
}


class IPificationService {
    
    func performCheckCoverage() {
        // TODO: update as your credential
        let clientID = ""
        let redirectUri = ""
        let phoneNumber = "381123456789"
        
        
        // Create an instance of IPificationCoreService and connect to the coverage URL
        let ipservice = IPificationCoreService(REDIRECT_URI: redirectUri)
        ipservice.onSuccess = { (response) -> Void in
            print("IP COVERAGE - FINAL SUCCESS RESPONSE:", response)
            self.performAuth()
        }
        ipservice.onError = { (error) -> Void in
            print("IP COVERAGE - FINAL ERROR RESPONSE:", error)
            self.performAuth()
        }
        ipservice.connectTo(urlString: "https://api.ipification.com/auth/realms/ipification/coverage?client_id=\(clientID)&phone=\(phoneNumber)", requestType: .coverage)
    }
    
    func performAuth(){
        // TODO: update as your credential
        let clientID = ""
        let redirectUri = ""
        let phoneNumber = "381123456789"
        let randomState = randomString(length: 16)
        
        
        // Create an instance of IPificationCoreService and connect to the auth URL
        let ipservice = IPificationCoreService(REDIRECT_URI: redirectUri)
        ipservice.onSuccess = { (response) -> Void in
            print("IP AUTH - FINAL SUCCESS RESPONSE:", response)
            let code = self.getParamValue(key: "code", response: response)
            print("code", code ?? "empty")
        }
        ipservice.onError = { (error) -> Void in
            print("IP AUTH - FINAL ERROR RESPONSE:", error)
        }
        ipservice.connectTo(urlString: "https://stage.ipification.com/auth/realms/ipification/protocol/openid-connect/auth?response_type=code&client_id=\(clientID)&redirect_uri=\(redirectUri)&scope=openid ip:phone_verify&state=\(randomState)&login_hint=\(phoneNumber)"
, requestType: .auth)
    }
    
    //util
    func randomString(length: Int) -> String {
        let prefix = "ip-sdk-"
        let remainingLength = max(0, length - prefix.count)
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        let randomLetters = String((0..<remainingLength).map { _ in letters.randomElement()! })
        return prefix + randomLetters

    }
    // util
    private func getParamValue(key: String, response: String) -> String? {
        // Parse the URL string
        if let url = URL(string: response) {
            // Extract query parameters
            if let queryItems = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems {
                // Search for the "code" parameter
                for queryItem in queryItems {
                    if queryItem.name == key {
                        return queryItem.value
                    }
                }
            }
        }
        return nil
    }
}

class IPificationCoreService {
    let requestStrFrmt =  "GET %@ HTTP/1.1\r\n%@%@Host: %@\r\n\r\n";
    
    var REDIRECT_URI = ""
    var isNetworkError = false
    var isConnectReady = 0
    var TIMEOUT : TimeInterval = 10000
    var receivedData = 0
    var previousByteLengh = 0
    var mData = Data()
    var connection: NWConnection!
    var requestType: RequestType = .auth
    
    public var onSuccess: ((_ response: String) -> Void)?
    public var onError: ((_ error: String) -> Void)?

    
    init(REDIRECT_URI: String) {
        self.REDIRECT_URI = REDIRECT_URI
        self.mData = Data()
        self.isConnectReady = 0
        self.isNetworkError = false
        self.receivedData = 0
        self.previousByteLengh = 0
    }
    
    public func connectTo(urlString: String, requestType: RequestType) {
        // init data
        self.requestType = requestType
        
        if(self.onSuccess == nil){
            print("please register callback")
            return
        }
        if(self.onError == nil){
            print("please register callback")
            return
        }
        if(REDIRECT_URI.isEmpty){
            print("please register callback")
            // TODO: Handle error, maybe call a completion handler with an error
            onError?("redirect_uri is invalid")
            return
        }
        // Parse the URL
        guard let url = URL(string: urlString) else {
            // Handle invalid URL
            print("Invalid URL")
            // TODO: Handle error, maybe call a completion handler with an error
            onError?("url is invalid")
            return
        }

        // Extract host and port
        guard let hostString = url.host else {
            // Handle missing host
            print("Missing host in URL")
            // TODO: Handle error, maybe call a completion handler with an error
            onError?("url is invalid [host]")
            return
        }
        

        let host = NWEndpoint.Host(hostString)
        let port = NWEndpoint.Port(rawValue: url.scheme == "http" ? 80 : 443)
        
        // Create TCP options
        let tcpOptions = NWProtocolTCP.Options()
        // Set connection timeout in milliseconds
        tcpOptions.connectionTimeout = Int(TIMEOUT / 1000) // Convert seconds to milliseconds
        tcpOptions.noDelay = true // Enable TCP no delay

        // Create TLS options (empty for now)
        let tlsOptions = NWProtocolTLS.Options()

        // Create parameters with TCP and TLS options
        let params = NWParameters(tls: url.scheme == "https" ? tlsOptions : nil, tcp: tcpOptions)

        // Set required interface is cellular
        params.requiredInterfaceType = .cellular

        // Create NWConnection with host, port, and parameters
        connection = NWConnection(host: host, port: port!, using: params)
        connection.stateUpdateHandler = { (newState) in
            print("TCP state change to: \(newState)")
            switch newState {
            case .ready:
                print("ready")
                self.isConnectReady = 1
                self.didConnect(socket: self, url: url)
                break
            case .waiting(let error):
                print("waiting error \(error.debugDescription)")
                self.isConnectReady = -1
                if error == .posix(POSIXErrorCode.ENETDOWN) && self.receivedData == -1 {
                    print("network error")
                    self.receivedData = 1
                    self.errorNetwork(error.debugDescription)
                }else{
                    self.didDisconnect(socket: self, error: error.debugDescription)
                }
                self.socketDisconnect(becauseOf: error)
                break
            
            case .failed(let error):
                self.isConnectReady = -1
                print("failed \(error.debugDescription)")
                self.didDisconnect(socket: self, error: error.localizedDescription)
                self.socketDisconnect(becauseOf: error)
                break
            case .cancelled:
                self.isConnectReady = -1
                print("cancelled" )
                break
            case .setup:
                print("setup")
                break
            case .preparing:
                print("preparing")
                DispatchQueue.main.asyncAfter(deadline: .now() + (self.TIMEOUT / 1000)) {
                    if(self.isConnectReady == 0){
                        self.isConnectReady = -1
                        print("timeout")
                        self.didDisconnect(socket: self, error: "Failed to connect - Timeout Error")
                        self.cancelConnection()
                    }
                }
                break
            default:
                print("default")
                break
            }
        }
        connection.start(queue: .main)
    }
    
    
    private func didConnect(socket: IPificationCoreService, url: URL){
        print("didConnect!");
        
        print( "host" , url.host!)
        let host = url.host!
        let path = url.path != "" ? url.path : "/"
        print( "path" , path)
        let query = url.query != nil ? "?" + url.query! : ""
        print( "query" , query)
        
        let body =  String(format: requestStrFrmt, path + query , "", "", host);
        print(body)
                
        socket.writeData(body.data(using: .utf8)!, withTag: 1)
        socket.readDataWithTag(1)
        

        DispatchQueue.main.asyncAfter(deadline: .now() + TIMEOUT / 1000) {
            if(self.receivedData == 0 && self.isNetworkError == false){
                self.receivedData = -1
                // TODO: Handle error, maybe call a completion handler with an error
                self.onError?("Failed to connect - Timeout")
                socket.connection.cancel()
            }
        }

    }
    
    private func socketDisconnect(becauseOf error: Error?) {
        print("disconnect \(error?.localizedDescription ?? "")")
        if connection != nil && connection.state != .cancelled {
            connection.cancel()
        }
    }
    
    private func didDisconnect(socket sock: IPificationCoreService, error err: String?) {
        
        if(receivedData == 0 && isNetworkError == false){
            receivedData = -1
            onError?("Cannot connect to server")
        }else{
            print("socketDidDisconnect!")
        }
        
    }
    func errorNetwork(_ error: String){
        isNetworkError = true
        onError?("CELLULAR IS NOT ACTIVE (\(error))")

    }
    
    func cancelConnection() {
        self.connection.cancel()
    }
    
    func writeData(_ data: Data, withTag: Int) {
        print("writeData")
        connection.send(content: data, completion: .contentProcessed({[weak self] (sendError) in
            guard let self = self else {return}
            if let sendError = sendError {
                print("write error")
                if connection != nil && connection.state != .cancelled {
                    connection.cancel()
                }
                // TODO: Handle error, maybe call a completion handler with an error
                self.didDisconnect(socket: self, error: sendError.localizedDescription)
            }else {
                print("didWriteData")
            }
        }))
    }
    
    func readDataWithTag(_ tag: Int) {
        print("readDataWithTag")
        connection.receive(minimumIncompleteLength: 1, maximumLength: 4096) {(data, contentContext, isComplete, error) in
            var datalength = 0
            if let error = error {
                print(error)
                if self.connection != nil && self.connection.state != .cancelled {
                    self.connection.cancel()
                }
                self.didDisconnect(socket: self, error: error.localizedDescription)
                return
            } else {
                // Parse out body length
                if let d = data {
                    datalength = d.count
                    self.mData.append(d)
                    self.receivedData = 1
                    
                    print("dataLength: received: \(datalength) - previous: \(self.previousByteLengh)")
                    if datalength < self.previousByteLengh || datalength < 4096 {
                        self.didReadData(self.mData, withTag: tag)
                        return
                    }
                    self.previousByteLengh = datalength
                    self.readDataWithTag(tag)

                }
            }
        }
    }
    
    func didReadData(_ data: Data, withTag: Int) {
        let str = String(decoding: data, as: UTF8.self)
        let components = str.components(separatedBy: "\r\n\r\n")
        
        if components.count > 1 {
            let headers = components[0]
            let body = components[1]
            // Extract status code from headers
            if let statusCodeRange = headers.range(of: "\\d{3}", options: .regularExpression) {
                let statusCodeString = headers[statusCodeRange]
                if let statusCode = Int(statusCodeString) {
                    switch statusCode {
                    case 300..<400:
                        // Redirect status 3xx
                        let lines = headers.components(separatedBy: "\r\n")
                        var locationHeader: String?
                        for line in lines {
                            if line.starts(with: "Location:") || line.starts(with: "location:") {
                                locationHeader = line.components(separatedBy: ": ")[1]
                                break
                            }
                        }
                        if let locationHeader = locationHeader, locationHeader.starts(with: "http") && !locationHeader.starts(with: REDIRECT_URI) {
                            // TODO: Call redirect request with locationHeader
                            continueRequest(url: locationHeader)
                        }
                        else if let locationHeader = locationHeader, locationHeader.starts(with: REDIRECT_URI) {
                            let result = locationHeader
                            if(requestType == .auth || requestType == .redirect){
                                if(getCode(response: result) == nil || getCode(response: result) == ""){
                                    print("getCode error")
                                    var error = ""
                                    if(result.isEmpty){
                                        let array = str.components(separatedBy: "\r\n")
                                        if(array.count > 1){
                                            let res = array[0].replacingOccurrences(of:"HTTP/1.1", with: "")
                                            if(res.isEmpty){
                                                error = array[1]
                                            }else{
                                                error = res
                                            }
                                        }else{
                                            error = str
                                        }
                                    } else{
                                        error = result
                                    }
                                    onError?(error)
                                    return
                                }
                            }
                            onSuccess?(result)
                        }
                        else if let locationHeader = locationHeader {
                            onSuccess?(locationHeader)
                        }
                        else {
                            onError?("\(statusCode ) but Invalid or missing redirect URL \(headers)")
                        }
                    case 200:
                        if(requestType == .coverage){
                            let result = components.count > 1 ? components[1] : ""
                            if(result.contains("available") == true){
                                if(self.isCoverageAvailable(response: result) == true){
                                    onSuccess?(result)
                                }
                                else{
                                    onError?("available = false. telco is not supported")
                                }
                            } else{
                                onError?(result)
                            }
                        }else{
                            // Success status 200
                            let result = components.count > 1 ? components[1] : ""
                            onSuccess?(result)
                        }
                    default:
                        // Error status
                        let result = components.count > 1 ? components[1] : ""
                        var error = "something went wrong"
                        if(result.isEmpty) {
                            let array = str.components(separatedBy: "\r\n")
                            
                            if(array.count > 1) {
                                let res = array[0].replacingOccurrences(of:"HTTP/1.1", with: "")
                                if(res.isEmpty) {
                                    error = array[1]
                                } else {
                                    error = res
                                }
                            } else {
                                error = str
                            }
                        } else {
                            error = result
                        }
                        onError?("Received error status: \(statusCode) with error \(error)")
                    }
                } else {
                    onError?("Failed to parse status code. Error: \(body)")
                }
            } else {
                onError?("Status code not found in headers : \(str)")
            }
        } else {
            onError?("Incomplete response : \(str)")
        }
    }
    
    private func isCoverageAvailable(response: String) -> Bool{
        let data =  Data(response.utf8)
        
        do {
            // make sure this JSON is in the format we expect
            if let json = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String: Any] {
                return json["available"] as? Bool ?? false
            }
        } catch let error as NSError {
            print("Failed to parse data : \(error.localizedDescription)", response)
        }
        return false
    }
    
    public func getCode(response: String) -> String? {
        // Parse the URL string
        if let url = URL(string: response) {
            // Extract query parameters
            if let queryItems = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems {
                // Search for the "code" parameter
                for queryItem in queryItems {
                    if queryItem.name == "code" {
                        return queryItem.value
                    }
                }
            }
        }
        return nil
    }
    
    public func continueRequest(url: String){
        // clear Data
        resetData()
        // check and process URL
        var covertedUrl = url
        if(isEscaped(str: url) == false){
            covertedUrl = url.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? url
        }
        connectTo(urlString: covertedUrl, requestType: .redirect)
    }
    
    func isEscaped(str: String) -> Bool {
        return str.removingPercentEncoding != str
    }
    private func resetData(){
        self.mData = Data()
        self.isConnectReady = 0
        self.isNetworkError = false
        self.receivedData = 0
        self.previousByteLengh = 0
    }
    
}
