package app.capgo.plugin.health

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Activity that displays the app's privacy policy for Health Connect permissions.
 *
 * This activity is launched by Health Connect when the user wants to see why the app
 * needs health data access. It displays a WebView with the privacy policy.
 *
 * The privacy policy URL can be customized by defining a string resource named
 * "health_connect_privacy_policy_url" in your app's res/values/strings.xml:
 *
 * ```xml
 * <resources>
 *     <string name="health_connect_privacy_policy_url">https://yourapp.com/privacy</string>
 * </resources>
 * ```
 *
 * Alternatively, you can place an HTML file at www/privacypolicy.html in your assets.
 */
class PermissionsRationaleActivity : Activity() {

    private val defaultUrl = "file:///android_asset/public/privacypolicy.html"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(applicationContext)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = false
        setContentView(webView)

        val url = getPrivacyPolicyUrl()
        webView.loadUrl(url)
    }

    private fun getPrivacyPolicyUrl(): String {
        return try {
            val resId = resources.getIdentifier(
                "health_connect_privacy_policy_url",
                "string",
                packageName
            )
            if (resId != 0) {
                getString(resId)
            } else {
                defaultUrl
            }
        } catch (e: Exception) {
            defaultUrl
        }
    }
}
