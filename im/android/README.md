
# Using IPification API for IM Authentication

This guide demonstrates how to use the IPification AUTH API for IM authentication in your Android application.

## Introduction

The provided code snippet demonstrates how to initiate the authentication process using the IPification AUTH API and handle the authentication result callbacks.

## Code Explanation

The code snippet initializes the IPification AUTH API with the required configuration and starts the authentication process. It also defines callbacks to handle authentication success, failure, and when the user opens the IM app.

## Instructions for Use
### Manifest Configuration

In your `AndroidManifest.xml` file, make sure to add the following attributes to the activity declaration:

```xml
<activity
    android:name=".YourIMLoginActivity"
    android:launchMode="singleInstance"
    android:windowSoftInputMode="adjustPan">
    <!-- Other activity attributes and configurations -->
</activity>
```
Adding `android:launchMode="singleInstance"` ensures that the activity is launched as a single instance, while `android:windowSoftInputMode="adjustPan"` adjusts the pan of the window to make room for the input method (soft keyboard).

Additionally, include the following `<queries>` section to declare the packages required for your app to query IM apps:

```xml
<queries>
    <package android:name="org.telegram.messenger" />
    <package android:name="org.telegram.messenger.web" />
    <package android:name="com.whatsapp" />
    <package android:name="com.viber.voip" />
</queries>
```


1. **Set Up IPification Configuration**:
    - AUTOMODE IS ON (as always)
    - Set the IPification environment to `SANDBOX` or `PRODUCTION`.
    - Set your `CLIENT_ID`.
    - Define your `REDIRECT_URI`.
    - [Optional] Generate a state using `generateState()` method.
    - Define the priority app list for IM authentication.

3. **Define Authentication Callbacks**:
   - Define callbacks to handle authentication success, failure, and when the user opens the IM app.

4. **Start Authentication**:
   - Call `startAuthentication()` method from `IMServices` with the provided callback.

## Usage Example

```kotlin
val callback = object : VerifyCompleteListener {
    override fun onSuccess(state: String) {
        Log.d("DemoActivity", "state:$state")
        // TODO: Call your backend with {state} to check the auth result
    }

    override fun onFail(error: String) {
        Log.e("DemoActivity", "" + error)
        // TODO: Fall back to OTP
    }

    override fun onOpenedLink() {
        // TODO: Hide loading
        Log.d("DemoActivity", "opened IM app" )
    }
}

IPConfiguration.getInstance().ENV = IPEnvironment.SANDBOX
IPConfiguration.getInstance().CLIENT_ID = "your-client-id"
IPConfiguration.getInstance().REDIRECT_URI = Uri.parse("your-redirect-uri")

IPConfiguration.getInstance().currentState = IPConfiguration.getInstance().generateState()
IPConfiguration.getInstance().IM_PRIORITY_APP_LIST = arrayOf("wa", "telegram", "viber")

IMServices.startAuthentication(this@activity, callback)

```

## Handling Authentication Result

To properly handle the authentication result in your activity, ensure you override the `onNewIntent()` and `onResume()` methods and call `IMServices.checkAndFinishSession()` within them.

Override the `onNewIntent()` and `onResume()` methods in your activity. 

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    // Set flag indicating new intent received
    onNewIntent = true
    // Call method to handle session check
    IMServices.checkAndFinishSession()
}

override fun onResume() {
    super.onResume()
    // Check if activity was launched from a new intent
    if (!onNewIntent) {
        // If not, call method to handle session check
        IMServices.checkAndFinishSession()
    }
    // Reset flag indicating new intent
    onNewIntent = false
}
```

