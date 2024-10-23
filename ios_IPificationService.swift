//
//  IPificationService.swift
//  IPificationSDK-Demo
//
//  Created by IPification on 29/02/2024.
//  Copyright © 2024 IPification. All rights reserved.
//

import Foundation
import Network

// Enum to define the type of request being made
enum RequestType {
    case coverage
    case auth
    case redirect
}

@available(iOS 12.0, *)
@available(iOSApplicationExtension 12.0, macOS 10.14 ,*)
class IPificationService {
    
    // Function to check coverage with provided credentials
    func performCheckCoverage(clientID: String, redirectUri: String, phoneNumber: String) {
        // Ensure all required parameters are provided
        guard !clientID.isEmpty, !redirectUri.isEmpty, !phoneNumber.isEmpty else {
            print("Missing credentials for coverage check.")
            //TODO handle error
            return
        }
        
        let coverageURLString = "https://api.ipification.com/auth/realms/ipification/coverage"

        // Create an instance of IPificationCoreService and connect to the coverage URL
        let ipService = IPificationCoreService(REDIRECT_URI: redirectUri)
        ipService.onSuccess = { (response) -> Void in
            print("IP COVERAGE - FINAL SUCCESS RESPONSE:", response)
            // Proceed to authentication upon successful coverage check
            self.performAuth(clientID: clientID, redirectUri: redirectUri, phoneNumber: phoneNumber)
        }
        ipService.onError = { (error) -> Void in
            print("IP COVERAGE - FINAL ERROR RESPONSE:", error)
            // TODO: Handle error, possibly by sending an SMS
        }
        ipService.connectTo(urlString: "\(coverageURLString)?client_id=\(clientID)&phone=\(phoneNumber)", requestType: .coverage)
    }
    
    // Function to perform authentication with provided credentials
    func performAuth(clientID: String, redirectUri: String, phoneNumber: String) {
        // Ensure all required parameters are provided
        guard !clientID.isEmpty, !redirectUri.isEmpty, !phoneNumber.isEmpty else {
            print("Missing credentials for authentication.")
            //TODO handle error
            return
        }
        let authURLString = "https://api.ipification.com/auth/realms/ipification/protocol/openid-connect/auth"

        // Generate a random state string for security
        // deprecated
        // let randomState = randomString(length: 16)
        
        // Build the URL components for the authentication request
        guard var urlComponents = URLComponents(string: authURLString) else {
            print("Invalid auth URL.")
            //TODO handle error
            return
        }

        urlComponents.queryItems = [
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "client_id", value: clientID),
            URLQueryItem(name: "redirect_uri", value: redirectUri),
            URLQueryItem(name: "scope", value: "openid ip:phone_verify"),
            // URLQueryItem(name: "state", value: randomState),
            URLQueryItem(name: "login_hint", value: phoneNumber)
        ]
        
        guard let urlAuthString = urlComponents.url?.absoluteString else {
            print("Failed to create URL from components.")
            //TODO handle error
            return
        }
        
        // Create an instance of IPificationCoreService and connect to the auth URL
        let ipService = IPificationCoreService(REDIRECT_URI: redirectUri)
        ipService.onSuccess = { (response) -> Void in
            print("IP AUTH - FINAL SUCCESS RESPONSE:", response)
            // Extract the authorization code from the response
            let code = self.getParamValue(key: "code", response: response)
            print("code", code ?? "empty")
        }
        ipService.onError = { (error) -> Void in
            print("IP AUTH - FINAL ERROR RESPONSE:", error)
            // TODO: Handle error, possibly by sending an SMS
        }
       
        ipService.connectTo(urlString: urlAuthString, requestType: .auth)
    }
    
    // Utility function to generate a random string of specified length
    func randomString(length: Int) -> String {
        let prefix = "ip-sdk-"
        let remainingLength = max(0, length - prefix.count)
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        let randomLetters = String((0..<remainingLength).map { _ in letters.randomElement()! })
        return prefix + randomLetters
    }

    // Utility function to extract a parameter value from a URL query string
    private func getParamValue(key: String, response: String) -> String? {
        // Parse the URL string
        if let url = URL(string: response) {
            // Extract query parameters
            if let queryItems = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems {
                // Search for the specified key in the query parameters
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

    var cookies : [HTTPCookie] = []

    var REDIRECT_URI = ""
    var isNetworkError = false
    var isConnectReady = 0
    var TIMEOUT : TimeInterval = 10000
    var receivedData = 0
    var previousByteLengh = 0
    var mData = Data()
    var connection: NWConnection!
    var requestType: RequestType = .auth
    var currentHost = ""

    public var onSuccess: ((_ response: String) -> Void)?
    public var onError: ((_ error: String) -> Void)?

    // Initialize the core service with the redirect URI
    init(REDIRECT_URI: String) {
        self.REDIRECT_URI = REDIRECT_URI
        self.mData = Data()
        self.isConnectReady = 0
        self.isNetworkError = false
        self.receivedData = 0
        self.previousByteLengh = 0
        self.cookies = []
    }
    
    // Function to connect to a specified URL
    public func connectTo(urlString: String, requestType: RequestType) {
        // Initialize data
        self.requestType = requestType
        
        // Ensure success and error callbacks are set
        if self.onSuccess == nil {
            print("Error: Success callback is not registered. Please register a success callback before proceeding.")
            return
        }
        if self.onError == nil {
            print("Error: Error callback is not registered. Please register an error callback before proceeding.")
            return
        }
        if REDIRECT_URI.isEmpty {
            print("Error: The REDIRECT_URI is invalid or empty. Please provide a valid redirect URI.")
            onError?("The REDIRECT_URI is invalid or empty. Please provide a valid redirect URI.")
            return
        }
        // Parse the URL
        guard let url = URL(string: urlString) else {
            // Handle invalid URL
            print("Error: The provided URL string is invalid. Please check the URL format and try again.")
            onError?("The provided URL string is invalid. Please check the URL format and try again.")
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
        
        currentHost = hostString
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
            case .waiting(let error):
                print("waiting error \(error.debugDescription)")
                self.isConnectReady = -1
                if error == .posix(POSIXErrorCode.ENETDOWN) && self.receivedData == -1 {
                    print("network error")
                    self.receivedData = 1
                    self.errorNetwork(error.debugDescription)
                } else {
                    self.didDisconnect(socket: self, error: error.debugDescription)
                }
                self.socketDisconnect(becauseOf: error)
            case .failed(let error):
                self.isConnectReady = -1
                print("failed \(error.debugDescription)")
                self.didDisconnect(socket: self, error: error.localizedDescription)
                self.socketDisconnect(becauseOf: error)
            case .cancelled:
                self.isConnectReady = -1
                print("cancelled")
            case .setup:
                print("setup")
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
            default:
                print("default")
            }
        }
        connection.start(queue: .main)
    }
    
    // Function called when the connection is successfully established
    private func didConnect(socket: IPificationCoreService, url: URL){
        print("didConnect!")
        print( "host" , url.host!)
        
        let host = url.host!
        let path = url.path != "" ? url.path : "/"
        print( "path" , path)
        let query = url.query != nil ? "?" + url.query! : ""
        print( "query" , query)
        
        let cookies = loadCookies(host: url.host!, path: path)

        // Format the HTTP request
        let body =  String(format: requestStrFrmt, path + query , "", cookies, host)
        print(body)
                
        socket.writeData(body.data(using: .utf8)!, withTag: 1)
        socket.readDataWithTag(1)
        
        // Set a timeout for the connection
        DispatchQueue.main.asyncAfter(deadline: .now() + TIMEOUT / 1000) {
            if(self.receivedData == 0 && self.isNetworkError == false){
                self.receivedData = -1
                // TODO: Handle error, maybe call a completion handler with an error
                self.onError?("Failed to connect - Timeout")
                socket.connection.cancel()
            }
        }
    }
    
    // Function to disconnect the socket due to an error
    private func socketDisconnect(becauseOf error: Error?) {
        print("disconnect \(error?.localizedDescription ?? "")")
        if connection != nil && connection.state != .cancelled {
            connection.cancel()
        }
    }
    
    // Function called when the socket is disconnected
    private func didDisconnect(socket sock: IPificationCoreService, error err: String?) {
        if(receivedData == 0 && isNetworkError == false){
            receivedData = -1
            onError?("Cannot connect to server")
        } else {
            print("socketDidDisconnect!")
        }
    }

    // Function to handle network errors
    func errorNetwork(_ error: String){
        isNetworkError = true
        onError?("CELLULAR NETWORK IS NOT ACTIVE (\(error))")
    }
    
    // Function to cancel the connection
    func cancelConnection() {
        self.connection.cancel()
    }
    
    // Function to send data over the connection
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
            } else {
                print("didWriteData")
            }
        }))
    }
    
    // Function to read data from the connection
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
    
    // Function called when data is successfully read
    func didReadData(_ data: Data, withTag: Int) {
        let str = String(decoding: data, as: UTF8.self)
        let components = str.components(separatedBy: "\r\n\r\n")

        // check and save cookie
        if(str.contains("set-cookie") || str.contains("Set-Cookie")){
            let array = str.components(separatedBy: "\r\n")
            for data in array{
                if(data.starts(with: "set-cookie:") || data.starts(with: "Set-Cookie:")){
                    let cookie = data.components(separatedBy: ": ")[1]
                    saveCookie(rawCookie: cookie)
                }
            }
        }
        if components.count > 1 {
            let headers = components[0]
            let body = components[1]
            // Extract status code from headers
            if let statusCodeRange = headers.range(of: "\\d{3}", options: .regularExpression) {
                let statusCodeString = headers[statusCodeRange]
                if let statusCode = Int(statusCodeString) {
                    switch statusCode {
                    case 300..<400:
                        // Handle redirect status (3xx)
                        let lines = headers.components(separatedBy: "\r\n")
                        var locationHeader: String?
                        for line in lines {
                            if line.starts(with: "Location:") || line.starts(with: "location:") {
                                locationHeader = line.components(separatedBy: ": ")[1]
                                break
                            }
                        }
                        if let locationHeader = locationHeader, locationHeader.starts(with: "http") && !locationHeader.starts(with: REDIRECT_URI) {
                            // Call Redirect Request with locationHeader
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
                                            } else {
                                                error = res
                                            }
                                        } else {
                                            error = str
                                        }
                                    } else {
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
                        // Handle success status (200)
                        if(requestType == .coverage){
                            let result = components.count > 1 ? components[1] : ""
                            if(result.contains("available") == true){
                                if(self.isCoverageAvailable(response: result) == true){
                                    onSuccess?(result)
                                } else {
                                    onError?("available = false. telco is not supported")
                                }
                            } else {
                                onError?(result)
                            }
                        } else {
                            // return success status 200 for other type
                            let result = components.count > 1 ? components[1] : ""
                            onSuccess?(result)
                        }
                    default:
                        // Handle other error statuses
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
    
    // Function to check if coverage is available in the response
    private func isCoverageAvailable(response: String) -> Bool{
        let data =  Data(response.utf8)
        do {
            // Ensure the JSON is in the expected format
            if let json = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String: Any] {
                return json["available"] as? Bool ?? false
            }
        } catch let error as NSError {
            print("Failed to parse data : \(error.localizedDescription)", response)
        }
        return false
    }
    
    // Function to extract the authorization code from the response URL
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
    
    // Function to continue the request with a new URL (e.g., after a redirect)
    public func continueRequest(url: String){
        // Clear data
        resetData()
        // Check and process URL
        var covertedUrl = url
        if(isEscaped(str: url) == false){
            covertedUrl = url.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? url
        }
        connectTo(urlString: covertedUrl, requestType: .redirect)
    }
    
    // Function to check if a string is percent-encoded
    func isEscaped(str: String) -> Bool {
        return str.removingPercentEncoding != str
    }
    
    // Function to reset connection data
    private func resetData(){
        self.mData = Data()
        self.isConnectReady = 0
        self.isNetworkError = false
        self.receivedData = 0
        self.previousByteLengh = 0
    }

    //support cookies
    //save cookie
    func saveCookie(rawCookie: String){
        let rawCookieParams = rawCookie.components(separatedBy: ";");
        let rawCookieNameAndValue = rawCookieParams[0].split(separator: "=", maxSplits: 1);
        if (rawCookieNameAndValue.count != 2) {
            return
        }
        let cookieName = rawCookieNameAndValue[0].trimmingCharacters(in: .whitespaces);
        let cookieValue = rawCookieNameAndValue[1].trimmingCharacters(in: .whitespaces);
        
        var isSecure = "FALSE"
        var domain = currentHost
        var path = "/"
        var httpOnly = false
        
        for i in 0..<rawCookieParams.count {
            let rawCookieParamNameAndValue = rawCookieParams[i].split(separator: "=", maxSplits: 1);
            
            let paramName = rawCookieParamNameAndValue[0].trimmingCharacters(in: .whitespaces);
            
            if (paramName == "Secure" || paramName == "secure") {
                isSecure = "TRUE"
            }
            else if (paramName == "HttpOnly") {
                httpOnly = true
            }
            else {
                if (rawCookieParamNameAndValue.count == 2) {
                    let paramValue = rawCookieParamNameAndValue[1].trimmingCharacters(in: .whitespaces);
                    if (paramName.caseInsensitiveCompare("domain") == .orderedSame ) {
                        domain = paramValue
                    } else if (paramName.caseInsensitiveCompare("path") == .orderedSame ) {
                        path = paramValue
                    }
                }else{
                   print("Invalid cookie: attribute not a flag or missing value. \(rawCookieParamNameAndValue)");
                }
            }
        }
        let cookie = saveCookie(name: cookieName, value: cookieValue, domain: domain, path: path, isSecure: isSecure, httpOnly: httpOnly)
        if(cookie != nil){
            cookies.append(cookie!)
        }
    }
    
    func saveCookie(name: String, value: String, domain: String, path: String, isSecure: String, httpOnly: Bool) -> HTTPCookie?{
        var cookieProps: [HTTPCookiePropertyKey : Any] = [
            HTTPCookiePropertyKey.name: name,
            HTTPCookiePropertyKey.value: value,
            HTTPCookiePropertyKey.domain: domain,
            HTTPCookiePropertyKey.path: path
        ]
        if(isSecure == "TRUE"){
            cookieProps = [
                HTTPCookiePropertyKey.name: name,
                HTTPCookiePropertyKey.value: value,
                HTTPCookiePropertyKey.secure: isSecure,
                HTTPCookiePropertyKey.domain: domain,
                HTTPCookiePropertyKey.path: path
            ]
        }

        let cookie = HTTPCookie(properties: cookieProps)
        return cookie
    }
    
    func loadCookies(host : String, path : String) -> String{
        var result = "Cookie: "
        // print("loadCookies: \(host) \(path)")
        var isExist = false
        for cookie in cookies {
            // print("host.contains(cookie.domain): \(host.contains(cookie.domain)) - path.starts(with: cookie.path): \(path.starts(with: cookie.path))")
            if(host.contains(cookie.domain) && path.starts(with: cookie.path)){
                result += "\(cookie.name)=\(cookie.value); "
                isExist = true
            }
        }
        result += "\r\n"
        if(isExist == false){
            result = ""
        }
        // print("loadCookies - result: \(result)")
        return result
    }
}


