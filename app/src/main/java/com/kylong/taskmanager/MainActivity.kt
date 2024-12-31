package com.kylong.taskmanager

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.CookieManager
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webview)

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // WebView client to intercept URL loading
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (Uri.parse(url).host != "kylongtask.azurewebsites.net") {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true
                }
                return false
            }
        }

        // Set up the WebView to listen for file downloads
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val downloadUri = Uri.parse(url)
            download(downloadUri, userAgent, mimeType)
        }

        // Load the initial URL
        webView.loadUrl("https://kylongtask.azurewebsites.net/login")
    }

    private fun download(downloadUri: Uri, userAgent: String, mimeType: String?) {
        // Get the file name from the URL
        val path = downloadUri.path?.split("/")?.toTypedArray()
        var fileName = path?.lastOrNull() ?: "downloaded_file"

        // Check if a file extension is provided, otherwise, use the MIME type to determine the extension
        if (!fileName.contains(".")) {
            mimeType?.let {
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                if (extension != null) {
                    fileName += ".$extension"
                }
            }
        }

        // Get the cookies for the download request
        val cookies = CookieManager.getInstance().getCookie(downloadUri.toString())

        // If cookies are null, display an alert and return
        if (cookies == null) {
            AlertDialog.Builder(this)
                .setMessage("The file cannot be downloaded, try updating your WebView.")
                .setPositiveButton("Okay", null)
                .show()
            return
        }

        // Create the DownloadManager request
        val request = DownloadManager.Request(downloadUri)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        // Add the cookies and user-agent to the request header
        request.addRequestHeader("cookie", cookies)
        request.addRequestHeader("User-Agent", userAgent)

        // Set download visibility
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        // Get the DownloadManager system service and enqueue the request
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webview)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
