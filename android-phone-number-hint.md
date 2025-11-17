# 📱 Phone Number Retrieval Implementation Guide (Android)

This document explains how to retrieve a user's SIM-based phone number on Android devices using two approaches:

1. **Google Phone Number Hint API** — No permission required  
2. **Permission-based fallback** using `TelephonyManager` and `SubscriptionManager`  
---

## Overview

| Method | Permissions | Works Without Play Services |
|---------|--------------|-----------------------------|
| **Option 1: Phone Number Hint API** | ❌ No | ❌ No | 
| **Option 2: TelephonyManager Fallback** | ✅ Yes | ✅ Yes |

---

## Option 1: Google Phone Number Hint API (No Permissions Required)

<img src="https://github.com/user-attachments/assets/ab967ffb-5fdd-452c-99cc-191a399f9686" alt="Screenshot_20241224_113312" width="300"/>

### 🔍 Description

The [Phone Number Hint API](https://developers.google.com/identity/phone-number-hint/android) displays a **secure, system dialog** listing SIM phone numbers for the user to choose from.  
It doesn’t require any dangerous permissions and is privacy-safe.

---

### Step 1: Add Dependencies

```groovy
// app/build.gradle
implementation 'com.google.android.gms:play-services-auth:21.3.0'

// Optional: for parsing/formatting phone numbers
implementation("com.googlecode.libphonenumber:libphonenumber:8.13.24")
```

---

### Step 2: Create a Request and Launch the Hint Picker

```kotlin
val request = GetPhoneNumberHintIntentRequest.builder().build()
val signInClient = Identity.getSignInClient(this)

signInClient.getPhoneNumberHintIntent(request)
    .addOnSuccessListener { result: PendingIntent ->
        try {
            phoneNumberHintIntentResultLauncher.launch(
                IntentSenderRequest.Builder(result).build()
            )
        } catch (e: Exception) {
            Log.e("PhoneHint", "Launching PendingIntent failed", e)
        }
    }
    .addOnFailureListener { e ->
        Log.e("PhoneHint", "Phone Number Hint failed", e)
    }
```

---

### 🧾 Step 3: Handle the Result

```kotlin
private val phoneNumberHintIntentResultLauncher =
    registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val phoneNumber = Identity.getSignInClient(requireActivity())
                    .getPhoneNumberFromIntent(result.data)

                val phone = detectCountryAndExtractNationalNumber(phoneNumber)
                binding.phoneCodeEditText.setText(phone.second)

                Log.d("PhoneHint", "Retrieved phone number: $phoneNumber")
            } catch (e: Exception) {
                Log.e("PhoneHint", "Failed to retrieve phone number: ${e.message}")
                Toast.makeText(requireContext(), "Failed to retrieve phone number", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("PhoneHint", "Hint cancelled or no result.")
            Toast.makeText(requireContext(), "Phone number hint cancelled", Toast.LENGTH_SHORT).show()
        }
    }
```

---

### Optional: Normalize with libphonenumber

```kotlin
fun detectCountryAndExtractNationalNumber(phoneNumber: String): Pair<String?, String?> {
    val phoneUtil = PhoneNumberUtil.getInstance()
    return try {
        val parsed = phoneUtil.parse(phoneNumber, null)
        val region = phoneUtil.getRegionCodeForNumber(parsed)
        val national = parsed.nationalNumber.toString()
        Pair(region, national)
    } catch (e: NumberParseException) {
        Log.e("PhoneHint", "Number parse failed: ${e.message}")
        Pair(null, null)
    }
}
```

---

### ✅ Advantages

- Zero runtime permissions required  
- Secure and privacy-friendly flow  
- Automatically handles multiple SIMs  
- Compliant with Android Play policy  

---

### ⚠️ Limitations

- Requires Google Play Services  
- Returns `null` if unsupported or unavailable or user cancels  

---

## Option 2: TelephonyManager Fallback (READ_PHONE_STATE + READ_PHONE_NUMBERS)


---

### Manifest Permissions

```xml
<!--
  Required to read basic telephony information such as the device's network state
  and active subscriptions.
  This permission is needed to call:
    - TelephonyManager.getLine1Number()      → retrieves the device's phone number (MSISDN)
    - SubscriptionManager.getActiveSubscriptionInfoList() → lists SIMs/subscriptions
-->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!--
  Required on Android 13 (API 33 / Tiramisu) and above to access the user's phone number
  via TelephonyManager.getPhoneNumber(int subId).
  Without this permission, even READ_PHONE_STATE cannot return the MSISDN anymore.
-->
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />
```
<img src="https://github.com/user-attachments/assets/5a0f2f7b-7717-455b-856c-63e392e32fa8" alt="Screenshot_20241224_113312" width="300"/>
<img src="https://github.com/user-attachments/assets/dfd04470-07e1-484f-9d9e-b49685ea8da4" alt="Screenshot_20241224_113312" width="300"/>
---

### Kotlin Implementation

```kotlin
val PHONE_PERMS = arrayOf(
  Manifest.permission.READ_PHONE_STATE,
  Manifest.permission.READ_PHONE_NUMBERS
)

fun requestPermsThenFetch() {
  if (hasAllPhonePerms(context)) {
    fetchPhoneNumberNow(context) { msisdn ->
      if (!msisdn.isNullOrBlank()) {
        viewModel.onPhoneNumberFromHint(msisdn)
        Log.d("PhoneFetch", "Perm-accepted; fetched: $msisdn")
      } else {
        Log.d("PhoneFetch", "Cannot fetch the number")
      }
    }
  } else {
    permLauncher.launch(PHONE_PERMS)
  }
}

val permLauncher = rememberLauncherForActivityResult(
  ActivityResultContracts.RequestMultiplePermissions()
) { grants ->
  val granted = PHONE_PERMS.all { grants[it] == true }
  if (granted) {
    fetchPhoneNumberNow(context) { msisdn ->
      if (!msisdn.isNullOrBlank()) {
        viewModel.onPhoneNumberFromHint(msisdn)
      } else {
        val dial = Util.getSystemDialCode(context)
        viewModel.onCountryCodeFromHint(dial)
      }
    }
  } else {
    val dial = Util.getSystemDialCode(context)
    viewModel.onCountryCodeFromHint(dial)
  }
}

private fun hasAllPhonePerms(ctx: Context): Boolean =
  ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
  ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED

private fun logPhoneFetch(message: String, level: String = "d") {
  when (level) {
    "e" -> Log.e("PhoneFetch", message)
    "w" -> Log.w("PhoneFetch", message)
    "i" -> Log.i("PhoneFetch", message)
    else -> Log.d("PhoneFetch", message)
  }
}
```

---

### Fetch Logic (Telephony + SubscriptionManager)

```kotlin

private fun fetchPhoneNumberNow(
  context: Context,
  onNumber: (String?, String?) -> Unit,  // (phoneNumber, countryISO)
) {
  // Guard: satisfy lint and avoid SecurityException
  if (!hasAllPhonePerms(context)) {
    logPhoneFetch("No permission granted — aborting fetch.", level = "w")
    onNumber(null, null)
    return
  }

  val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
  val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

  // Get system country code as fallback
  val systemCountryISO = Locale.getDefault().country.uppercase()

  logPhoneFetch("📱 Starting phone number fetch...")
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      logPhoneFetch("DEFAULT_SUBSCRIPTION_ID  ${SubscriptionManager.DEFAULT_SUBSCRIPTION_ID}...")
  }
  // function from IPification SDK
  val activeSubId = DeviceUtils.getInstance(context).activeSimOperator().getSubID()
  logPhoneFetch("activeSubId  ${activeSubId}...")
  // Android 13+ path — requires READ_PHONE_STATE and READ_PHONE_NUMBERS
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    logPhoneFetch("Running on Android 13+ (TIRAMISU). Trying SubscriptionManager.getPhoneNumber()...")
    try {
      val subs = sm.activeSubscriptionInfoList
      logPhoneFetch("Active subscriptions count: ${subs?.size ?: 0}")

      if (!subs.isNullOrEmpty()) {
        // Try to get active SIM phone number first
        val activeMsisdn = try {
          sm.getPhoneNumber(activeSubId)
        } catch (se: SecurityException) {
          logPhoneFetch("SecurityException on active SIM getPhoneNumber(): ${se.message}", level = "e")
          null
        }

        if (!activeMsisdn.isNullOrBlank()) {
          // Get country ISO for active SIM
          val activeCountryIso = subs.find { it.subscriptionId == activeSubId }?.countryIso?.uppercase() ?: systemCountryISO
          logPhoneFetch("Got phone number from active SIM (subId=$activeSubId): $activeMsisdn, countryISO=$activeCountryIso", level = "i")
          onNumber(activeMsisdn, activeCountryIso)
          return
        }
      } else {
        val msisdn = sm.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
        logPhoneFetch("No active subscriptions found. get DEFAULT_SUBSCRIPTION_ID: $msisdn, countryISO=$systemCountryISO", level = "e")
        onNumber(msisdn, systemCountryISO)
        return
      }
    } catch (se: SecurityException) {
      logPhoneFetch("SecurityException reading activeSubscriptionInfoList: ${se.message}", level = "e")
    }
  } else {
    logPhoneFetch("Running on Android <13 — skipping SubscriptionManager.getPhoneNumber().")
  }
  var tmm = tm.createForSubscriptionId(activeSubId)
  // Get country ISO from active SIM for line1Number result
  val fallbackCountryISO = try {
    val subs = sm.activeSubscriptionInfoList
    subs?.find { it.subscriptionId == activeSubId }?.countryIso?.uppercase() ?: systemCountryISO
  } catch (e: Exception) {
    logPhoneFetch("Could not get country ISO from active SIM, using system default", level = "w")
    systemCountryISO
  }
  // Use line1Number: primary method for Android <13, fallback for Android 13+
  val fallback = try {
    tmm.line1Number
  } catch (se: SecurityException) {
    logPhoneFetch("SecurityException reading line1Number: ${se.message}", level = "e")
    null
  }

  if (!fallback.isNullOrBlank()) {
    logPhoneFetch("Got phone number from TelephonyManager: $fallback, countryISO=$fallbackCountryISO", level = "i")
    onNumber(fallback, fallbackCountryISO)
    return
  } else {
    logPhoneFetch("TelephonyManager.line1Number returned null/blank", level = "e")
  }

  onNumber(fallback, fallbackCountryISO)
}
```

---

### ✅ Advantages

- Works on most of Android versions 
- Provides direct access to carrier/MSISDN info  
- Can enumerate multiple SIMs  
---

### ⚠️ Limitations

- Needs explicit permissions  
- May return `null` depending on carrier  
---

