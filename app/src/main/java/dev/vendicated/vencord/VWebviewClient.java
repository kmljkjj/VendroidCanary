package dev.vendicated.vencord;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VWebviewClient extends WebViewClient {

    /** Avoid injecting the full Vencord bundle repeatedly (breaks React / settings). */
    private String lastInjectedUrl = "";

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        var url = request.getUrl();
        String host = url.getHost();
        if (host == null || Constants.isDiscordHost(host) || "about:blank".equals(url.toString())) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, url);
            view.getContext().startActivity(intent);
        } catch (Exception e) {
            Logger.e("Cannot open external url", e);
        }
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // Reset inject guard on full document load
        lastInjectedUrl = "";
        injectVencordOnce(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        injectVencordOnce(view, url);
        view.setVisibility(View.VISIBLE);
        // Light recovery if Discord shows its crash screen
        view.evaluateJavascript(
                "(function(){try{"
                        + "var t=document.body&&document.body.innerText||'';"
                        + "if(/cessé de fonctionner|stopped working|unexpectedly/i.test(t)){"
                        + "console.warn('[Vendroid] Discord crash UI detected');"
                        + "}"
                        + "}catch(e){}})();",
                null);
        super.onPageFinished(view, url);
    }

    private void injectVencordOnce(WebView view, String url) {
        if (url == null) return;
        // Same document — don't re-run multi‑MB browser.js
        if (url.equals(lastInjectedUrl)) return;
        if (HttpClient.VencordRuntime == null) return;

        // Guard inside the page too (SPA navigations)
        String guard =
                "(function(){if(window.__VendroidVencordInjected)return true;window.__VendroidVencordInjected=1;return false;})()";
        view.evaluateJavascript(
                guard,
                already -> {
                    if ("true".equals(already)) {
                        lastInjectedUrl = url;
                        return;
                    }
                    view.evaluateJavascript(HttpClient.VencordRuntime, null);
                    if (HttpClient.VencordMobileRuntime != null) {
                        view.evaluateJavascript(HttpClient.VencordMobileRuntime, null);
                    }
                    lastInjectedUrl = url;
                    Logger.d("Vencord injected for " + url);
                });
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
        // Only strip CSP on main HTML. Intercepting every CSS is slow and causes lag.
        if (!req.isForMainFrame()) return null;
        String path = req.getUrl().getPath();
        if (path != null && (path.endsWith(".js") || path.endsWith(".json") || path.endsWith(".woff2"))) {
            return null;
        }
        try {
            return doFetchStripCsp(req);
        } catch (Exception ex) {
            Logger.e("shouldInterceptRequest", ex);
            return null; // fall back to WebView default
        }
    }

    private WebResourceResponse doFetchStripCsp(WebResourceRequest req) throws IOException {
        var url = req.getUrl().toString();
        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(45000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod(req.getMethod() != null ? req.getMethod() : "GET");

        Map<String, String> reqHeaders = req.getRequestHeaders();
        if (reqHeaders != null) {
            for (var h : reqHeaders.entrySet()) {
                // Let HttpURLConnection manage compression
                if ("accept-encoding".equalsIgnoreCase(h.getKey())) continue;
                conn.setRequestProperty(h.getKey(), h.getValue());
            }
        }
        // Match desktop client
        conn.setRequestProperty("User-Agent", Constants.DESKTOP_USER_AGENT);

        int code = conn.getResponseCode();
        String msg = conn.getResponseMessage() != null ? conn.getResponseMessage() : "OK";
        if (code < 100) code = 200;

        Map<String, List<String>> headers = conn.getHeaderFields();
        Map<String, String> modified = new HashMap<>();
        if (headers != null) {
            for (var header : headers.entrySet()) {
                if (header.getKey() == null) continue;
                String key = header.getKey();
                if ("Content-Security-Policy".equalsIgnoreCase(key)) continue;
                if ("Content-Security-Policy-Report-Only".equalsIgnoreCase(key)) continue;
                // Avoid conflicting length after any transform
                if ("Content-Length".equalsIgnoreCase(key)) continue;
                if (header.getValue() != null && !header.getValue().isEmpty()) {
                    modified.put(key, header.getValue().get(0));
                }
            }
        }

        String mime = modified.getOrDefault("Content-Type", "text/html");
        String encoding = "utf-8";
        if (mime.contains(";")) {
            String[] parts = mime.split(";");
            mime = parts[0].trim();
            for (String p : parts) {
                p = p.trim();
                if (p.toLowerCase().startsWith("charset=")) {
                    encoding = p.substring(8).trim().replace("\"", "");
                }
            }
        }

        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            stream = new ByteArrayInputStream(new byte[0]);
        }

        return new WebResourceResponse(mime, encoding, code, msg, modified, stream);
    }
}
