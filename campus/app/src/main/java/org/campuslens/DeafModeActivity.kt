package org.campuslens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class DeafModeActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader
    private var pendingWebPermission: PermissionRequest? = null
    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val request = pendingWebPermission
        pendingWebPermission = null
        if (granted) request?.grant(request.resources) else {
            request?.deny()
            Toast.makeText(this, "Camera permission is required for hand tracking.", Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "CampusLens · Deaf Mode"
        assetLoader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) = assetLoader.shouldInterceptRequest(request.url)
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    if (!request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) { request.deny(); return@runOnUiThread }
                    if (ContextCompat.checkSelfPermission(this@DeafModeActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) request.grant(request.resources)
                    else { pendingWebPermission = request; requestCamera.launch(Manifest.permission.CAMERA) }
                }
            }
        }
        setContentView(webView)
        webView.loadUrl("https://appassets.androidplatform.net/assets/deaf/index.html")
    }
    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() { if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else finish() }
    override fun onDestroy() {
        if (::webView.isInitialized) { webView.stopLoading(); webView.loadUrl("about:blank"); webView.destroy() }
        super.onDestroy()
    }
}
