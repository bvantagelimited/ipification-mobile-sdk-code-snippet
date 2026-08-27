# Configure custom staging URLs

Use custom URLs only when IPification provides a staging API server for your integration.
Production should use the SDK's standard production environment and URLs.

Before initializing the SDK, provide:

- The staging API server supplied during onboarding, including `https://` and without a trailing
  slash.
- The client ID and redirect URI registered for the same environment.

Do not mix staging URLs or credentials with production credentials. Configure the SDK once,
before starting any Coverage or Authorization request.

The two staging endpoints are derived from the supplied server:

```text
{staging-api-server}/auth/realms/ipification/coverage
{staging-api-server}/auth/realms/ipification/protocol/openid-connect/auth
```

## iOS (Swift)

```swift
func configureIPification(isProduction: Bool) {
    let configuration = IPConfiguration.sharedInstance

    if isProduction {
        configuration.ENV = IPEnvironment.PRODUCTION
        configuration.customUrls = false

        // Use the production credentials registered during onboarding.
        configuration.CLIENT_ID = "<production-client-id>"
        configuration.REDIRECT_URL = "<production-redirect-uri>"
        return
    }

    // Replace this value with the staging server supplied by IPification.
    let stagingApiServer = "https://<staging-api-server>"

    configuration.ENV = IPEnvironment.SANDBOX
    configuration.customUrls = true
    configuration.COVERAGE_URL =
        "\(stagingApiServer)/auth/realms/ipification/coverage"
    configuration.AUTHORIZATION_URL =
        "\(stagingApiServer)/auth/realms/ipification/protocol/openid-connect/auth"

    // These values must be registered for the same staging environment.
    configuration.CLIENT_ID = "<staging-client-id>"
    configuration.REDIRECT_URL = "<staging-redirect-uri>"
}
```

## Android (Kotlin)

```kotlin
fun configureIPification(isProduction: Boolean) {
    val configuration = IPConfiguration.getInstance()

    if (isProduction) {
        configuration.ENV = IPEnvironment.PRODUCTION
        configuration.customUrls = false

        // Use the production credentials registered during onboarding.
        configuration.CLIENT_ID = "<production-client-id>"
        configuration.REDIRECT_URL = "<production-redirect-uri>"
        return
    }

    // Replace this value with the staging server supplied by IPification.
    val stagingApiServer = "https://<staging-api-server>"

    configuration.ENV = IPEnvironment.SANDBOX
    configuration.customUrls = true
    configuration.COVERAGE_URL = Uri.parse(
        "$stagingApiServer/auth/realms/ipification/coverage"
    )
    configuration.AUTHORIZATION_URL = Uri.parse(
        "$stagingApiServer/auth/realms/ipification/protocol/openid-connect/auth"
    )

    // These values must be registered for the same staging environment.
    configuration.CLIENT_ID = "<staging-client-id>"
    configuration.REDIRECT_URL = "<staging-redirect-uri>"
}
```

## Android (Java)

```java
private void configureIPification(boolean isProduction) {
    IPConfiguration configuration = IPConfiguration.getInstance();

    if (isProduction) {
        configuration.setENV(IPEnvironment.PRODUCTION);
        configuration.setCustomUrls(false);

        // Use the production credentials registered during onboarding.
        configuration.setCLIENT_ID("<production-client-id>");
        configuration.setREDIRECT_URI("<production-redirect-uri>");
        return;
    }

    // Replace this value with the staging server supplied by IPification.
    String stagingApiServer = "https://<staging-api-server>";

    configuration.setENV(IPEnvironment.SANDBOX);
    configuration.setCustomUrls(true);
    configuration.setCOVERAGE_URL(Uri.parse(
        stagingApiServer + "/auth/realms/ipification/coverage"
    ));
    configuration.setAUTHORIZATION_URL(Uri.parse(
        stagingApiServer + "/auth/realms/ipification/protocol/openid-connect/auth"
    ));

    // These values must be registered for the same staging environment.
    configuration.setCLIENT_ID("<staging-client-id>");
    configuration.setREDIRECT_URI("<staging-redirect-uri>");
}
```

## Verification checklist

Before testing, confirm that:

1. The API server uses HTTPS and exactly matches the server supplied by IPification.
2. `customUrls` is `true` only for the custom staging configuration.
3. The client ID and redirect URI belong to the selected environment.
4. Both Coverage and Authorization URLs are configured before the first SDK request.
5. A production build cannot accidentally select the staging configuration.

Treat the API server as configuration rather than embedding one shared staging hostname in the
application. For production applications, inject it through the build configuration or another
controlled configuration source; do not accept an arbitrary server from end-user input.
