package com.example.facebooklite

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.mediaPlaybackRequiresUserGesture = true
        ws.setGeolocationEnabled(false)
        ws.allowFileAccess = false
        ws.allowContentAccess = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectHideAndRestrictJS()
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                // basic ad/tracker block list (expand as needed)
                val blocked = listOf("doubleclick.net","googlesyndication","adsystem","adservice","facebook.com/ads")
                if(blocked.any { url.contains(it, ignoreCase = true) }) {
                    return WebResourceResponse("text/plain","utf-8", null)
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                // Deny all permission requests (camera, mic, location)
                request?.deny()
            }
        }

        // Optional: reduce fingerprinting by modifying UA
        ws.userAgentString = ws.userAgentString + " FacebookLiteClient/1.0"

        // Load facebook
        webView.loadUrl("https://www.facebook.com")
    }

    private fun injectHideAndRestrictJS() {
        // This JS tries to hide comment boxes, reply buttons, ad containers.
        val js = """
        (function(){
          try {
            const adSel = ['iframe[src*="ads"]', '[id^="pagelet_"]', '.ads', '[data-pagelet*="FeedUnit"]'];
            const hideSel = ['[aria-label="Write a comment"]', 'div[role="textbox"]', 'button[aria-label*="Reply"]', '.UFICommentBody', '[data-testid="UFI2Composer"]'];
            function hideAll(arr){
              arr.forEach(sel=>{
                document.querySelectorAll(sel).forEach(el=>el.style.display='none');
              });
            }
            hideAll(adSel); hideAll(hideSel);
            const ob = new MutationObserver(muts=>{
              muts.forEach(m=>{
                m.addedNodes.forEach(n=>{
                  if(n.querySelectorAll){
                    hideAll(adSel); hideAll(hideSel);
                  }
                });
              });
            });
            ob.observe(document.body,{childList:true,subtree:true});
            console.log('FB Lite JS injected');
          } catch(e){ console.log('inject err', e); }
        })();
        """
        webView.evaluateJavascript(js, null)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
