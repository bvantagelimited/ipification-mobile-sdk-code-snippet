
# Using IPification API for IM Authentication

This guide demonstrates how to use the IPification AUTH API for IM authentication in your Android application.

## Introduction

The provided code snippet demonstrates how to initiate the authentication process using the IPification AUTH API and handle the authentication result callbacks.

## Code Explanation

The code snippet initializes the IPification AUTH API with the required configuration and starts the authentication process. It also defines callbacks to handle authentication success, failure, and when the user opens the IM app.

## Instructions for Use

1. **Set Up IPification Configuration**:
   - Set the IPification environment to `SANDBOX`.
   - Set your `CLIENT_ID`.
   - Define your `REDIRECT_URI`.
   - Generate a state using `generateState()` method.
   - Define the priority app list for IM authentication.
   - AUTOMODE IS ON (as always)

2. **Define Authentication Callbacks**:
   - Define callbacks to handle authentication success, failure, and when the user opens the IM app.

3. **Start Authentication**:
   - Call `startAuthentication()` method from `IMServices` with the provided callback.

## Usage Example

```kotlin
val callback = object : VerifyCompleteListener {
    override fun onSuccess(state: String) {
        Log.d("DemoActivity", "state:$state")
        // TODO: Call your backend with state to check the auth result
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
IPConfiguration.getInstance().CLIENT_ID = "6f2026a683bc439ebb414a03f9012f27"
IPConfiguration.getInstance().REDIRECT_URI = Uri.parse("https://test.ipification.com/auth")
IPConfiguration.getInstance().currentState = IPConfiguration.getInstance().generateState()
IPConfiguration.getInstance().IM_PRIORITY_APP_LIST = arrayOf("wa", "telegram")
IMServices.startAuthentication(this, callback)
