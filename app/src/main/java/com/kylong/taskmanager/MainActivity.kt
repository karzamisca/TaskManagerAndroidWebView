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
import java.net.URLDecoder

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
            val fileName = extractFileName(contentDisposition, url)
            download(Uri.parse(url), userAgent, mimeType, fileName)
        }

        // Load the initial URL
        webView.loadUrl("https://kylongtask.azurewebsites.net/login")
    }

    private fun extractFileName(contentDisposition: String?, url: String): String {
        return try {
            if (!contentDisposition.isNullOrEmpty() && contentDisposition.contains("filename")) {
                val fileNamePart = contentDisposition.split("filename*=")
                if (fileNamePart.size > 1) {
                    // Extract and decode the UTF-8 encoded filename
                    URLDecoder.decode(fileNamePart[1].substringAfter("''"), "UTF-8")
                } else {
                    // Fallback to the regular "filename" attribute
                    contentDisposition.split("filename=")[1].trim('"')
                }
            } else {
                // If Content-Disposition header is absent or invalid, use the last segment of the URL
                Uri.parse(url).lastPathSegment ?: "downloaded_file"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "downloaded_file"
        }
    }

    private fun download(downloadUri: Uri, userAgent: String, mimeType: String?, fileName: String) {
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

        Toast.makeText(this, "Download started: $fileName", Toast.LENGTH_SHORT).show()
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
