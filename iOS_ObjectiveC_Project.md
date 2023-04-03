
# iOS SDK Changes to work with Objective-C Project
-------------

This document describes how to update the IPification iOS SDK and use it in the  Project.
-------------
**Minimum iOS required** : iOS 12

Main Flow of iOS SDK : 
*   Prepare the `authorization request` with required parameters
*   Call Authorization API with `authorization request` ( GET ) through `Cellular Network` (use Network Framework - NetworkSocket.swift)
*   Receive a response with: 
    *   result directly via `redirect_uri` (1) or 
    *   redirection url (`301` or `302`) (2)
*   (1) -> Parser the response then return the result to client
*   (2) -> Perform all url(s) redirection until receive the result with `redirect_uri` (through `Cellular Network`)

**Note:** All requests need to be performed via `cellular network` interface.



## Guideline
To integrate IPification Swift SDK into an Objective-C project, follow these steps:

#### 1. Create a Bridging Header by Xcode
The first time you add a Swift file to an Objective-C project, this dialogue box will appear, asking if you would like to create a bridging header. Make sure you click Create Bridging Header, then Xcode will configure everything for you.

#### 2. Import the Header
At **Target > Build Settings > Swift Compiler — General > Objective-C Generated Interface Header Name**, you can find the ${SWIFT_MODULE_NAME}-Swift.h header file name, which will be used to import the Swift class in your Objective-C file.

Words before -Swift.h is your Product Module Name, able to find under **Target/Build Settings/All/Packaging**.



#### 3. Modify Swift Code 
```
By default, the generated header contains interfaces for Swift declarations marked with the `public` or `open` modifier. If your app target has an Objective-C bridging header, the generated header also includes interfaces marked with the internal modifier. Declarations marked with the `private` or `fileprivate` modifier don’t appear in the generated header, and aren’t exposed to the Objective-C runtime unless they are explicitly marked with a `@objc` attribute.
```
- Classes extends `NSObject` and that is annotated with `@objc`, satisfying all the requirements to be used in `Objective-C`.
- All usage functions need to be annotated with `@objc`


#### 3. Usage
Within the Objective-C code file where you want to access your Swift class, use this syntax to import it:

```
#import "productModuleName-Swift.h"
```

This header file name might not pop up like other available header files do when you are typing it, but once you write down this syntax, your Swift class will be able to access, too.


Now you can use the Swift classes and methods in your Objective-C code.

To call a Swift method from Objective-C, use the syntax **[<Swift class name> <method name>]**. 
For example:

```
   AuthorizationService *authorizationService = [[AuthorizationService alloc] init];
   [authorizationService setCallbackSuccess:^(AuthorizationResponse *response) {
      // Handle successful response here
      NSLog(@"Authorization success: %@", [response getCode]);
   }];


   AuthorizationRequestBuilder *authBuilder = [[AuthorizationRequestBuilder alloc] init];
   [authBuilder setScopeWithValue:@"openid ip:phone_verify"];
   [authBuilder addQueryParamWithKey:@"login_hint" value: @"381123456789"];
   AuthorizationRequest *authRequest = [authBuilder build];
   [authorizationService startAuthorization:authRequest];

```

That's it! With these steps, you should be able to integrate Swift classes into your Objective-C project.


Reference: https://developer.apple.com/documentation/swift/importing-swift-into-objective-c
