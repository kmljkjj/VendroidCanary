package dev.vendicated.vencord;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/** Vendroid + canary + inject desktop spoof before Vencord */
public class VWebviewClient extends WebViewClient {

    /** Spoof desktop environment early (before Discord / Vencord). */
    private static final String DESKTOP_SPOOF_JS =
            "(function(){"
                    + "if(window.__VendroidDesktopSpoof)return;"
                    + "window.__VendroidDesktopSpoof=1;"
                    + "try{"
                    + "var ua='" + Constants.DESKTOP_USER_AGENT.replace("'", "\\'") + "';"
                    + "try{Object.defineProperty(Navigator.prototype,'userAgent',{get:function(){return ua},configurable:true});}catch(e){}"
                    + "try{Object.defineProperty(Navigator.prototype,'platform',{get:function(){return 'Win32'},configurable:true});}catch(e){}"
                    + "try{Object.defineProperty(Navigator.prototype,'maxTouchPoints',{get:function(){return 0},configurable:true});}catch(e){}"
                    + "try{Object.defineProperty(Navigator.prototype,'vendor',{get:function(){return 'Google Inc.'},configurable:true});}catch(e){}"
                    // Minimal DiscordNative so some desktop-gated checks pass (no real native modules)
                    + "if(!window.DiscordNative){"
                    + "window.DiscordNative={"
                    + "isRenderer:true,"
                    + "process:{platform:'win32',arch:'x64',env:{}},"
                    + "app:{getReleaseChannel:function(){return 'canary'},getVersion:function(){return '1.0.9199'},reload:function(){location.reload()},getPath:function(){return ''}},"
                    + "nativeModules:{canSubmitCrash:function(){return false},ensureModule:function(){return Promise.resolve()},requireModule:function(){return {}}},"
                    + "clipboard:{copy:function(t){try{navigator.clipboard.writeText(t)}catch(e){}}},"
                    + "ipc:{send:function(){},on:function(){},invoke:function(){return Promise.resolve()}},"
                    + "window:{FOCUS:function(){},blur:function(){},fullscreen:function(){}},"
                    + "os:{release:function(){return '10.0.0'}}"
                    + "};"
                    + "}"
                    + "console.log('[VendroidCanary] desktop spoof applied');"
                    + "}catch(e){console.warn('[VendroidCanary] desktop spoof failed',e);}"
                    + "})();";

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        var url = request.getUrl();
        String authority = url.getAuthority();
        if ("canary.discord.com".equals(authority)
                || "discord.com".equals(authority)
                || "about:blank".equals(url.toString())) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        view.getContext().startActivity(intent);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // 1) desktop spoof first
        view.evaluateJavascript(DESKTOP_SPOOF_JS, null);
        // 2) Vencord
        if (HttpClient.VencordRuntime != null) {
            view.evaluateJavascript(HttpClient.VencordRuntime, null);
        }
        if (HttpClient.VencordMobileRuntime != null) {
            view.evaluateJavascript(HttpClient.VencordMobileRuntime, null);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // Re-apply spoof after navigation (SPA)
        view.evaluateJavascript(DESKTOP_SPOOF_JS, null);
        view.setVisibility(View.VISIBLE);
        super.onPageFinished(view, url);
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
        if (req.isForMainFrame() || (req.getUrl().getPath() != null && req.getUrl().getPath().endsWith(".css"))) {
            try {
                return doFetch(req);
            } catch (IOException ex) {
                Logger.e("Error during shouldInterceptRequest", ex);
            }
        }
        return null;
    }

    private WebResourceResponse doFetch(WebResourceRequest req) throws IOException {
        var url = req.getUrl().toString();
        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(req.getMethod());
        for (var h : req.getRequestHeaders().entrySet()) {
            conn.setRequestProperty(h.getKey(), h.getValue());
        }
        // Same desktop UA on intercepted document
        conn.setRequestProperty("User-Agent", Constants.DESKTOP_USER_AGENT);

        var code = conn.getResponseCode();
        var msg = conn.getResponseMessage();

        var headers = conn.getHeaderFields();
        var modifiedHeaders = new HashMap<String, String>();
        for (var header : headers.entrySet()) {
            if (header.getKey() != null && !"Content-Security-Policy".equalsIgnoreCase(header.getKey())) {
                if (header.getValue() != null && !header.getValue().isEmpty()) {
                    modifiedHeaders.put(header.getKey(), header.getValue().get(0));
                }
            }
        }
        if (url.endsWith(".css")) modifiedHeaders.put("Content-Type", "text/css");

        return new WebResourceResponse(
                modifiedHeaders.getOrDefault("Content-Type", "application/octet-stream"),
                "utf-8",
                code,
                msg != null ? msg : "OK",
                modifiedHeaders,
                conn.getInputStream());
    }
}
