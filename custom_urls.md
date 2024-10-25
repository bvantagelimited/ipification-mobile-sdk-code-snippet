# iOS and Android IP Configuration

## iOS - Swift

```swift
if (isProduction) {
    IPConfiguration.sharedInstance.ENV = IPEnvironment.PRODUCTION
    IPConfiguration.sharedInstance.customUrls = false
    // IPConfiguration.sharedInstance.COVERAGE_URL = "https://api.ipification.com/auth/realms/ipification/coverage"
    // IPConfiguration.sharedInstance.AUTHORIZATION_URL = "https://api.ipification.com/auth/realms/ipification/protocol/openid-connect/auth"
    // TODO: your client credential on live
    // IPConfiguration.sharedInstance.CLIENT_ID = ""
    // IPConfiguration.sharedInstance.REDIRECT_URL = ""
} else {
    IPConfiguration.sharedInstance.ENV = IPEnvironment.SANDBOX
    IPConfiguration.sharedInstance.customUrls = true
    IPConfiguration.sharedInstance.COVERAGE_URL = "https://api.stage.ipification.com/auth/realms/ipification/coverage"
    IPConfiguration.sharedInstance.AUTHORIZATION_URL = "https://api.stage.ipification.com/auth/realms/ipification/protocol/openid-connect/auth"
    
    // TODO: your client credential on stage
    // IPConfiguration.sharedInstance.CLIENT_ID = ""
    // IPConfiguration.sharedInstance.REDIRECT_URL = ""
}
```

## Android - Kotline

```kotlin
if (isProduction) {
    IPConfiguration.getInstance().ENV = IPEnvironment.PRODUCTION
    IPConfiguration.getInstance().customUrls = false
    // IPConfiguration.getInstance().COVERAGE_URL = Uri.parse("https://api.ipification.com/auth/realms/ipification/coverage")
    // IPConfiguration.getInstance().AUTHORIZATION_URL = Uri.parse("https://api.ipification.com/auth/realms/ipification/protocol/openid-connect/auth")
    // TODO: your client credential on live
    // IPConfiguration.getInstance().CLIENT_ID = ""
    // IPConfiguration.getInstance().REDIRECT_URL = ""
} else {
    IPConfiguration.getInstance().ENV = IPEnvironment.SANDBOX
    IPConfiguration.getInstance().customUrls = true
    IPConfiguration.getInstance().COVERAGE_URL = Uri.parse("https://api.stage.ipification.com/auth/realms/ipification/coverage")
    IPConfiguration.getInstance().AUTHORIZATION_URL = Uri.parse("https://api.stage.ipification.com/auth/realms/ipification/protocol/openid-connect/auth")

    // TODO: your client credential on stage
    // IPConfiguration.getInstance().CLIENT_ID = ""
    // IPConfiguration.getInstance().REDIRECT_URL = ""
}
```
