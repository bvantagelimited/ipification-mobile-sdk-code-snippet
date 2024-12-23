# iOS and Android IP Configuration Update for New Staging Environment

## iOS - Swift

```swift
if (isProduction) {
    IPConfiguration.sharedInstance.ENV = IPEnvironment.PRODUCTION
    IPConfiguration.sharedInstance.customUrls = false

    // TODO: your client credential on live
    // IPConfiguration.sharedInstance.CLIENT_ID = 
    // IPConfiguration.sharedInstance.REDIRECT_URL = 
} else {
    IPConfiguration.sharedInstance.ENV = IPEnvironment.SANDBOX
    IPConfiguration.sharedInstance.customUrls = true
    IPConfiguration.sharedInstance.COVERAGE_URL = "https://api.stage.ipification.com/auth/realms/ipification/coverage"
    IPConfiguration.sharedInstance.AUTHORIZATION_URL = "https://api.stage.ipification.com/auth/realms/ipification/protocol/openid-connect/auth"
    
    // TODO: your client credential on stage
    // IPConfiguration.sharedInstance.CLIENT_ID = 
    // IPConfiguration.sharedInstance.REDIRECT_URL = 
}
```

## Android - Kotlin

```kotlin
if (isProduction) {
    IPConfiguration.getInstance().ENV = IPEnvironment.PRODUCTION
    IPConfiguration.getInstance().customUrls = false

    // TODO: your client credential on live
    // IPConfiguration.getInstance().CLIENT_ID = 
    // IPConfiguration.getInstance().REDIRECT_URL = 
} else {
    IPConfiguration.getInstance().ENV = IPEnvironment.SANDBOX
    IPConfiguration.getInstance().customUrls = true
    IPConfiguration.getInstance().COVERAGE_URL = Uri.parse("https://api.stage.ipification.com/auth/realms/ipification/coverage")
    IPConfiguration.getInstance().AUTHORIZATION_URL = Uri.parse("https://api.stage.ipification.com/auth/realms/ipification/protocol/openid-connect/auth")

    // TODO: your client credential on stage
    // IPConfiguration.getInstance().CLIENT_ID = 
    // IPConfiguration.getInstance().REDIRECT_URL = 
}
```

## Android - Java

```java
private void initIPification() {

    IPConfiguration.getInstance().setENV(IPEnvironment.SANDBOX);
    // for stage only - start
    IPConfiguration.getInstance().setCustomUrls(true);
    IPConfiguration.getInstance().setCOVERAGE_URL(Uri.parse("https://api.stage.ipification.com/auth/realms/ipification/coverage"));
    IPConfiguration.getInstance().setAUTHORIZATION_URL(Uri.parse("https://api.stage.ipification.com/auth/realms/ipification/protocol/openid-connect/auth"));
    // TODO: your client credential on stage
    // IPConfiguration.getInstance().setCLIENT_ID();
    // IPConfiguration.getInstance().setREDIRECT_URI();

}
