package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private var webView: WebView? = null

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          webView?.let { wv ->
            wv.evaluateJavascript("window.handleAndroidBack ? window.handleAndroidBack() : false") { result ->
              if (result != "true") {
                if (wv.canGoBack()) {
                  wv.goBack()
                } else {
                  isEnabled = false
                  onBackPressedDispatcher.onBackPressed()
                }
              }
            }
          } ?: run {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
          }
        }
      }
    )

    setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
        ) {
          PosWebView(
            onWebViewCreated = { wv -> webView = wv },
            onPrintRequested = { printWebView() }
          )
        }
      }
    }
  }

  fun printWebView() {
    val wv = webView ?: return
    val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val printAdapter = wv.createPrintDocumentAdapter("GoodsManage_Invoice")
    val jobName = getString(R.string.app_name) + " Invoice Document"
    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
  }
}

class WebAppInterface(private val activity: MainActivity) {
  @JavascriptInterface
  fun printInvoice() {
    activity.runOnUiThread {
      activity.printWebView()
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PosWebView(
  onWebViewCreated: (WebView) -> Unit,
  onPrintRequested: () -> Unit
) {
  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      WebView(ctx).apply {
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          databaseEnabled = true
          allowFileAccess = true
          loadWithOverviewMode = true
          useWideViewPort = true
          cacheMode = WebSettings.LOAD_DEFAULT
        }
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {}
        if (ctx is MainActivity) {
          addJavascriptInterface(WebAppInterface(ctx), "AndroidBridge")
        }
        onWebViewCreated(this)
        loadUrl("file:///android_asset/index.html")
      }
    }
  )
}
