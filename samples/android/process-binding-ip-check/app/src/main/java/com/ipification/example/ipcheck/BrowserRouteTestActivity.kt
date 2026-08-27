package com.ipification.example.ipcheck

import android.app.Activity
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent

class BrowserRouteTestActivity : Activity() {
    private lateinit var cellularBinding: ProcessCellularBinding
    private lateinit var statusView: TextView
    private lateinit var progressBar: ProgressBar

    private var mode = MODE_WEBVIEW
    private var contentStarted = false
    private var customTabLaunched = false
    private var customTabPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_WEBVIEW
        cellularBinding = ProcessCellularBinding(applicationContext)
        setContentView(createLoadingView())

        try {
            cellularBinding.bind(
                onBound = {
                    if (contentStarted) return@bind
                    contentStarted = true
                    when (mode) {
                        MODE_CUSTOM_TAB -> launchCustomTab()
                        else -> showWebView()
                    }
                },
                onFailure = { message -> showFailure(message) },
                onLost = { showFailure("The cellular network was lost") },
            )
        } catch (error: RuntimeException) {
            showFailure(error.message ?: "Could not request a cellular network")
        }
    }

    private fun showWebView() {
        statusView.text =
            "This WebView belongs to the app. The process is bound while the page loads."
        progressBar.visibility = View.GONE

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = false
            clearCache(true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = false
            }
            loadUrl(IP_CHECK_URL)
        }

        val closeButton = Button(this).apply {
            text = "Close and restore default network"
            setOnClickListener { finish() }
        }
        (statusView.parent as LinearLayout).apply {
            addView(
                webView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ),
            )
            addView(closeButton)
        }
    }

    private fun launchCustomTab() {
        statusView.text =
            "Opening a Custom Tab. It runs in the browser app, so this app's process binding " +
                "cannot force the tab to cellular."
        progressBar.visibility = View.GONE

        try {
            customTabLaunched = true
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, Uri.parse(IP_CHECK_URL))
        } catch (error: RuntimeException) {
            customTabLaunched = false
            showFailure(error.message ?: "No Custom Tab provider is available")
        }
    }

    private fun showFailure(message: String) {
        cellularBinding.unbind()
        progressBar.visibility = View.GONE
        statusView.text = message
    }

    override fun onPause() {
        super.onPause()
        if (customTabLaunched) customTabPaused = true
    }

    override fun onResume() {
        super.onResume()
        if (customTabLaunched && customTabPaused) finish()
    }

    override fun onDestroy() {
        cellularBinding.unbind()
        super.onDestroy()
    }

    private fun createLoadingView(): View {
        val padding = (24 * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)

            addView(TextView(this@BrowserRouteTestActivity).apply {
                text = if (mode == MODE_CUSTOM_TAB) "Custom Tab test" else "WebView test"
                textSize = 24f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(this@BrowserRouteTestActivity).also { statusView = it }.apply {
                text = "Waiting for a cellular network…"
                textSize = 16f
                setPadding(0, padding / 2, 0, padding / 2)
            })
            addView(ProgressBar(this@BrowserRouteTestActivity).also { progressBar = it })
        }
    }

    companion object {
        const val EXTRA_MODE = "test_mode"
        const val MODE_WEBVIEW = "webview"
        const val MODE_CUSTOM_TAB = "custom_tab"
        private const val IP_CHECK_URL = "https://api.ipify.org?format=json"
    }
}
