package com.kylong.taskmanager

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.webkit.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.net.URLDecoder

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    // Register for Activity Result API
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = result.data?.data
            fileChooserCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        } else {
            fileChooserCallback?.onReceiveValue(null)
        }
        fileChooserCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webview)

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webSettings.setSupportMultipleWindows(true) // Allow pop-ups
        webSettings.saveFormData = true // Enable saving form data (like username & password)

        // Handle file uploads and pop-ups
        webView.webChromeClient = object : WebChromeClient() {
            // Handling creation of a new window (for links with target="_blank")
            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?
            ): Boolean {
                val result = view?.hitTestResult
                val data = result?.extra
                if (data != null) {
                    // Create an intent to open the link in the browser
                    val context: Context = view.context
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data))
                    context.startActivity(browserIntent)
                    return true
                }
                return false
            }

            // Handle file chooser for <input type="file">
            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback = filePathCallback
                val intent = fileChooserParams?.createIntent()
                if (intent != null) {
                    fileChooserLauncher.launch(intent)
                    return true
                }
                return false
            }

            override fun onJsAlert(
                view: WebView?, url: String?, message: String?, result: JsResult?
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result?.confirm() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView?, url: String?, message: String?, result: JsResult?
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result?.confirm() }
                    .setNegativeButton("Cancel") { _, _ -> result?.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsPrompt(
                view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ -> result?.confirm(defaultValue) }
                    .setNegativeButton("Cancel") { _, _ -> result?.cancel() }
                    .show()
                return true
            }
        }

        // Handle URL loading
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (Uri.parse(url).host != "kylongtask.azurewebsites.net") {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    return true
                }
                return false
            }
        }

        // Handle file downloads
        webView.setDownloadListener { url, userAgent, contentDisposition, _, _ ->
            val fileName = extractFileName(contentDisposition, url)
            download(Uri.parse(url), userAgent, fileName)
        }

        // Load the initial URL
        webView.loadUrl("https://kylongtask.azurewebsites.net/login")

        // Handle back navigation
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
    }

    private fun extractFileName(contentDisposition: String?, url: String): String {
        return try {
            if (!contentDisposition.isNullOrEmpty() && contentDisposition.contains("filename")) {
                val fileNamePart = contentDisposition.split("filename*=")
                if (fileNamePart.size > 1) {
                    URLDecoder.decode(fileNamePart[1].substringAfter("''"), "UTF-8")
                } else {
                    contentDisposition.split("filename=")[1].trim('"')
                }
            } else {
                Uri.parse(url).lastPathSegment ?: "downloaded_file"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "downloaded_file"
        }
    }

    private fun download(downloadUri: Uri, userAgent: String, fileName: String) {
        val cookies = CookieManager.getInstance().getCookie(downloadUri.toString())
        if (cookies == null) {
            AlertDialog.Builder(this)
                .setMessage("The file cannot be downloaded, try updating your WebView.")
                .setPositiveButton("Okay", null)
                .show()
            return
        }

        val request = DownloadManager.Request(downloadUri)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.addRequestHeader("cookie", cookies)
        request.addRequestHeader("User-Agent", userAgent)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(this, "Download started: $fileName", Toast.LENGTH_SHORT).show()
    }
}
