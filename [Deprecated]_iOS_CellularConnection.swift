
----------
Sample Code: NetworkSocket.swift
----------
@available(iOS 12.0, *)
@available(iOSApplicationExtension 12.0, macOS 10.14 ,*)
internal class NetworkSocket {
    var previousByteLengh = 0

    var host = ""
    var port:UInt16 = 443
    var receivedData: Bool = false
    var endpoint: URLComponents? = nil;
    
    public var callbackSuccess: ((_ response: ResponseProtocol) -> Void)?
    public var callbackFailed: ((_ response: CellularException) -> Void)?
    
    init(endpoint : URLComponents) {
        self.endpoint = endpoint
        self.delegate = NetworkDelegate(endpoint: endpoint, cellularCallback: self)
    }
   
    
    var delegate: NetworkDelegate?
    
   
    var connection:NWConnection!
   
    
    internal func performRequest(){
        
        host = endpoint!.host!
        port = endpoint!.port ?? (endpoint!.scheme == "http" ? 80 : 443)
        do{
            try connectTo(host, port: port, enableTLS: port == 443, tlsSettings: nil)
        }catch{
            self.delegate?.didDisconnect(socket: self, error: error)
        }
        
    }
        
    func connectTo(_ host: String, port: UInt16, enableTLS: Bool, tlsSettings: [NSObject : AnyObject]?) throws {
        receivedData = false
        let h = NWEndpoint.Host.init(host)
        let p =  NWEndpoint.Port.init(rawValue: port)

        let options = NWProtocolTLS.Options()
        
        let tcpOptions = NWProtocolTCP.Options()
        tcpOptions.connectionTimeout = Int(cellularRequest!.connectTimeout / 1000)

        let params = NWParameters(tls: enableTLS ? options : nil, tcp: tcpOptions)
        
        params.requiredInterfaceType = .cellular
        
        self.connection =  NWConnection.init(host:  h  , port: p!, using: params)
                
        connection.stateUpdateHandler = { (newState) in
            print("TCP state change to: \(newState)")
            switch newState {
            case .ready:
                print("ready")
                self.delegate!.didConnect(socket: self)
                break
            case .waiting(let error):
                print("waiting error \(error.debugDescription ?? "")")
                if error == .posix(POSIXErrorCode.ENETDOWN) && self.receivedData == false {
                    print("network error")
                    self.receivedData = true
                    self.delegate?.errorNetwork(error.debugDescription)
                    self.disconnect(becauseOf: error)
                }else{
                    self.delegate?.didDisconnect(socket: self, error: error.debugDescription)
                    self.disconnect(becauseOf: error)
                }
                break
            
            case .failed(let error):
                print("failed \(error.debugDescription ?? "")")
                self.delegate?.didDisconnect(socket: self, error: error)
                self.disconnect(becauseOf: error)
                break
            case .cancelled:
                print("cancelled" )
                break
            default:
                print("default")
                break
            }
        }
        
        connection.start(queue: .main)
        
    }
    
    func disconnect(becauseOf error: Error?) {
        print("disconnect \(error?.localizedDescription ?? "")")
        if connection != nil && connection.state != .cancelled {
            connection.cancel()
        }
    }
    func forceDisconnect(_ sessionID: UInt32) {
        print("forceDisconnect--")
        if connection != nil && connection.state != .cancelled {
            connection.cancel()
        }
    }
    func forceDisconnect(becauseOf error: Error?) {
        print("forceDisconnect \(error?.localizedDescription ?? "")")
        if connection != nil && connection.state != .cancelled {
            connection.cancel()
        }
    }
    
    func writeData(_ data: Data, withTag: Int) {
        print("writeData")
        connection.send(content: data, completion: .contentProcessed({[weak self] (sendError) in
            guard let self = self else {return}
            guard let delegate = self.delegate else {return}
            if let sendError = sendError {
                print("write error")
                self.connection!.cancel()
                delegate.didDisconnect(socket: self, error: sendError)
            }else {
                print("didWriteData")
                delegate.didWriteData(data, withTag: withTag, from: self)
            }
        }))
    }
    
    func readDataWithTag(_ tag: Int) {
        print("readDataWithTag")
        connection.receive(minimumIncompleteLength: 1, maximumLength: 4096) {[weak self]  (data, ctx, isComplete, error) in
            guard let self = self else {return}
            guard let delegate = self.delegate else {return}
            if let error = error {
                // Handle error in reading
                self.connection!.cancel()
                delegate.didDisconnect(socket: self, error: error)
            } else {
                // Parse out body length
                if datalength < self.previousByteLengh || datalength < 4096 {
//                        NSLog("did receive, EOF")
                    self.delegate?.didReadData(self.mData, withTag: tag, sock: self)
                    self.stop()
                    return
                }
                self.previousByteLengh = datalength
                self.readDataWithTag(tag)
            }
            
        }
    }
    
    func readDataToLength(_ length: Int, withTag tag: Int) {
        print("readDataToLength")
    }
    
    func readDataToData(_ data: Data, withTag tag: Int) {
        print("readDataToData")
    }
    
    func readDataToData(_ data: Data, withTag tag: Int, maxLength: Int) {
        print("readDataToData")
    }
    deinit {
        if connection != nil && connection.state != .cancelled {
            connection.cancel()
        }
        print("NetworkSocket deinit")
    }
    
}



--------------
Sample Code: NetworkDelegate.swift
--------------

@available(iOS 12.0, *)
class NetworkDelegate{
    
    var endpoint: URLComponents;
    let requestStrFrmt =  "GET %@ HTTP/1.1\r\n%@%@Host: %@\r\n\r\n";
    
    var receivedData: Bool? = nil
    var isNetworkError = false
    var cellularCallback: CallbackProtocol
    
    var currentHost = ""
    
    init(endpoint: URLComponents,
        cellularCallback: CallbackProtocol) {
        self.endpoint = endpoint
        self.cellularCallback = cellularCallback
    }
    
    public func didConnect(socket: NetworkSocket){
        Log.d("didConnect!");
        currentHost = endpoint.host!
        var path = endpoint.path != "" ? endpoint.path : "/"
        var query = endpoint.query != nil ? "?" + endpoint.query! : ""
        
        let cookies = loadCookies(host: endpoint.host!, path: path)
        
        let body =  String(format: requestStrFrmt, path + query , loadHeaders(),
                           cookies, endpoint.host!)
        socket.writeData(body.data(using: .utf8)!, withTag: 1)
        socket.readDataWithTag(1)
        // timeout checking manually
        DispatchQueue.main.asyncAfter(deadline: .now() + readTimeout / 1000) {
            if(self.receivedData == nil && self.isNetworkError == false){
                self.receivedData = false
                let error = IPificationException(IPificationError.cannot_connect, "Failed to connect - Timeout \(self.readTimeout/1000)");
                self.cellularCallback.onError(error: error)
                socket.connection.cancel()
            }
        }
    }
    
    public func didDisconnect(socket sock: NetworkSocket, error err: Error?) {
        print("disconnecct" , err?.localizedDescription ?? "error")
        if(receivedData == nil && isNetworkError == false){
            receivedData = false
            let error = IPificationException(IPificationError.cannot_connect, err?.localizedDescription ?? "Cannot connect to server");
            cellularCallback.onError(error: error)
        }else{
            Log.d("socketDidDisconnect!")
        }
    }
  
    public func didDisconnect(socket sock: NetworkSocket, error err: String?) {
        if(receivedData == nil && isNetworkError == false){
            receivedData = false
            let error = IPificationException(IPificationError.cannot_connect, err ?? "Cannot connect to server");
            cellularCallback.onError(error: error)
        }else{
            Log.d("socketDidDisconnect!")
        }
    }
    
    func didReadData(_ data: Data, withTag: Int, sock: NetworkSocket){
        Log.d("didReadData")
        let str = String(decoding: data, as: UTF8.self)
        var array = str.components(separatedBy: "\r\n\r\n")
        receivedData = true
        var result = array.count > 1 ? array[1] : ""
        cellularCallback.onSuccess(result)
        }
    }
    
    
    func didWriteData(_ data: Data?, withTag: Int, from: NetworkSocket){
        Log.d("didwrite")
    }
    func errorNetwork(_ error: String){
        isNetworkError = true
        cellularCallback.onError(error: IPificationException(IPificationError.notActive, "CELLULAR_NOT_ACTIVE (\(error))"))
    }
    
    
}
