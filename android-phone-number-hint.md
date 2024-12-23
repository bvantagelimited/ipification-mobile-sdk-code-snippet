
# 📱 Phone Number Hint API Implementation Guide

Follow the official documentation here: [Phone Number Hint API](https://developers.google.com/identity/phone-number-hint/android)

The **Phone Number Hint API** provides a seamless way to retrieve a user’s SIM-based phone numbers **without requiring additional permissions** or **manual input**.

---

## 🚀 **Implementation Steps**

### 1. **Add Dependency**

Include the following in your `app/build.gradle` file:

```groovy
implementation 'com.google.android.gms:play-services-auth:21.3.0'
```

---

### 2. **Create a `GetPhoneNumberHintIntentRequest` Object**

Set up the phone number hint request:

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
            Log.e("PhoneNumberHint", "Launching the PendingIntent failed", e)
        }
    }
    .addOnFailureListener { e ->
        Log.e("PhoneNumberHint", "Phone Number Hint failed", e)
    }
```

---

### 3. **Handle the Result**

Register an `ActivityResultLauncher` to process the result:

```kotlin
private val phoneNumberHintIntentResultLauncher =
    registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val phoneNumber = Identity.getSignInClient(this)
                .getPhoneNumberFromIntent(result.data)
            // Use the retrieved phone number
        } else {
            Log.e("PhoneNumberHint", "User canceled or an error occurred")
        }
    }
```

---

## 🛡️ **Key Notes**

- The API works without requiring **READ_PHONE_STATE** permission.
- Ensure you handle possible exceptions gracefully.
- Test on a physical device with an active SIM card.

For more details, refer to the [official documentation](https://developers.google.com/identity/phone-number-hint/android). 🚀


## **Complete example**:

```kotlin
import android.app.PendingIntent
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity

class PhoneNumberHintFragment : Fragment() {

    private lateinit var phoneNumberHintIntentResultLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ActivityResultLauncher once
        phoneNumberHintIntentResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    try {
                        val phoneNumber = Identity.getSignInClient(requireActivity())
                            .getPhoneNumberFromIntent(result.data)
                        
                        val phone = detectCountryAndExtractNationalNumber(phoneNumber)
                        binding.phoneCodeEditText.setText(phone.second)

                        Log.d("PhoneHint", "Phone number: $phoneNumber")
                    } catch (e: Exception) {
                        Log.e("PhoneHint", "Failed to retrieve phone number: ${e.message}")
                        Toast.makeText(requireContext(), "Failed to retrieve phone number", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("PhoneHint", "Phone number hint was cancelled or failed")
                    Toast.makeText(requireContext(), "Phone number hint cancelled", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun showPhoneNumberHint() {
        try {
            val request: GetPhoneNumberHintIntentRequest = GetPhoneNumberHintIntentRequest.builder().build()

            Identity.getSignInClient(requireActivity())
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener { result: PendingIntent ->
                    try {
                        phoneNumberHintIntentResultLauncher.launch(
                            IntentSenderRequest.Builder(result).build()
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e("PhoneHint", "Launching the PendingIntent failed: ${e.message}")
                        Toast.makeText(requireContext(), "Failed to launch phone number hint", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("PhoneHint", "Phone Number Hint failed: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to retrieve phone number hint", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e("PhoneHint", "Unexpected error: ${e.message}")
            Toast.makeText(requireContext(), "An unexpected error occurred", Toast.LENGTH_LONG).show()
        }
    }
}
```
