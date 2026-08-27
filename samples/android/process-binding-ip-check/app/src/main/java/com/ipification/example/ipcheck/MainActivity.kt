package com.ipification.example.ipcheck

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val apiUrl = URL("https://api.ipify.org?format=json")
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var cellularBinding: ProcessCellularBinding
    private lateinit var runButton: Button
    private lateinit var statusView: TextView
    private lateinit var beforeView: TextView
    private lateinit var cellularView: TextView
    private lateinit var afterView: TextView

    private var runId = 0
    private var cellularCheckStartedForRun: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cellularBinding = ProcessCellularBinding(applicationContext)
        setContentView(createContentView())
        runButton.setOnClickListener { runVerification() }
    }

    private fun runVerification() {
        val currentRun = ++runId
        runButton.isEnabled = false
        beforeView.text = "Before forcing: checking…"
        cellularView.text = "While forced to cellular: waiting…"
        afterView.text = "After restoring default: waiting…"
        statusView.text = "Step 1 of 3: using Android's default network"

        executor.execute {
            val before = fetchPublicIp()
            mainHandler.post {
                if (currentRun != runId) return@post
                before.fold(
                    onSuccess = {
                        beforeView.text = "Before forcing: $it"
                        requestAndBindCellular(currentRun)
                    },
                    onFailure = { finishWithFailure(currentRun, "Before request failed", it) },
                )
            }
        }
    }

    private fun requestAndBindCellular(currentRun: Int) {
        statusView.text = "Step 2 of 3: requesting and binding the process to cellular"

        try {
            cellularBinding.bind(
                timeoutMillis = CELLULAR_TIMEOUT_MS,
                onBound = {
                    if (currentRun != runId || cellularCheckStartedForRun == currentRun) {
                        return@bind
                    }
                    cellularCheckStartedForRun = currentRun
                    statusView.text = "Step 2 of 3: process bound; checking the cellular IP"

                    executor.execute {
                        val cellular = fetchPublicIp()
                        mainHandler.post {
                            if (currentRun != runId) return@post
                            cellular.fold(
                                onSuccess = {
                                    cellularView.text = "While forced to cellular: $it"
                                    restoreDefaultAndCheck(currentRun)
                                },
                                onFailure = {
                                    finishWithFailure(currentRun, "Cellular request failed", it)
                                },
                            )
                        }
                    }
                },
                onFailure = { message ->
                    finishWithFailure(currentRun, message, null)
                },
                onLost = {
                    finishWithFailure(currentRun, "The cellular network was lost", null)
                },
            )
        } catch (error: RuntimeException) {
            finishWithFailure(currentRun, "Could not request a cellular network", error)
        }
    }

    private fun restoreDefaultAndCheck(currentRun: Int) {
        releaseCellularBinding()
        statusView.text = "Step 3 of 3: default network restored; checking again"

        executor.execute {
            val after = fetchPublicIp()
            mainHandler.post {
                if (currentRun != runId) return@post
                after.fold(
                    onSuccess = {
                        afterView.text = "After restoring default: $it"
                        statusView.text = comparisonMessage()
                        runButton.isEnabled = true
                    },
                    onFailure = { finishWithFailure(currentRun, "After request failed", it) },
                )
            }
        }
    }

    private fun fetchPublicIp(): Result<String> = runCatching {
        val connection = (apiUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")

            // Binding affects only new sockets. Do not reuse a connection from another step.
            setRequestProperty("Connection", "close")
        }

        try {
            val statusCode = connection.responseCode
            check(statusCode in 200..299) { "HTTP $statusCode" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).getString("ip")
        } finally {
            connection.disconnect()
        }
    }

    private fun finishWithFailure(currentRun: Int, message: String, error: Throwable?) {
        mainHandler.post {
            if (currentRun != runId) return@post
            ++runId // Ignore any HTTP result or network callback already queued for this run.
            releaseCellularBinding()
            statusView.text = buildString {
                append(message)
                error?.message?.let { append(": ").append(it) }
            }
            runButton.isEnabled = true
        }
    }

    private fun releaseCellularBinding() {
        cellularBinding.unbind()
        cellularCheckStartedForRun = null
    }

    private fun comparisonMessage(): String {
        val before = beforeView.text.toString().substringAfterLast(": ")
        val cellular = cellularView.text.toString().substringAfterLast(": ")
        val after = afterView.text.toString().substringAfterLast(": ")

        return if (before != cellular && before == after) {
            "Confirmed: cellular IP differs, and the original default-network IP returned."
        } else {
            "Completed. Compare the values; carrier NAT or VPNs may hide a route change."
        }
    }

    override fun onDestroy() {
        ++runId
        releaseCellularBinding()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun createContentView(): View {
        val padding = (24 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        content.addView(TextView(this).apply {
            text = "Whole-app cellular routing check"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, padding, 0, 0)
        })
        content.addView(TextView(this).apply {
            text = "Keep Wi-Fi and mobile data enabled, then run this test on a real device."
            textSize = 16f
            setPadding(0, padding / 2, 0, padding)
        })

        runButton = Button(this).apply { text = "Run 3-step IP check" }
        statusView = resultText("Ready")
        beforeView = resultText("Before forcing: —")
        cellularView = resultText("While forced to cellular: —")
        afterView = resultText("After restoring default: —")

        content.addView(runButton)
        content.addView(statusView)
        content.addView(beforeView)
        content.addView(cellularView)
        content.addView(afterView)
        content.addView(Button(this).apply {
            text = "Test WebView while forced"
            setOnClickListener {
                startBrowserRouteTest(BrowserRouteTestActivity.MODE_WEBVIEW)
            }
        })
        content.addView(Button(this).apply {
            text = "Test Custom Tab limitation"
            setOnClickListener {
                startBrowserRouteTest(BrowserRouteTestActivity.MODE_CUSTOM_TAB)
            }
        })

        return ScrollView(this).apply { addView(content) }
    }

    private fun startBrowserRouteTest(mode: String) {
        if (!runButton.isEnabled) return
        startActivity(
            Intent(this, BrowserRouteTestActivity::class.java)
                .putExtra(BrowserRouteTestActivity.EXTRA_MODE, mode),
        )
    }

    private fun resultText(initialText: String) = TextView(this).apply {
        text = initialText
        textSize = 16f
        setTextIsSelectable(true)
        val spacing = (12 * resources.displayMetrics.density).toInt()
        setPadding(0, spacing, 0, spacing)
    }

    private companion object {
        const val CELLULAR_TIMEOUT_MS = 15_000L
        const val HTTP_TIMEOUT_MS = 10_000
    }
}
