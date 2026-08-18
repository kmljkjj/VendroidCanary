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
import java.util.List;
import java.util.Map;

public class VWebviewClient extends WebViewClient {

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
        injectVencord(view);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        // Re-inject in case SPA navigation dropped scripts
        injectVencord(view);
        view.setVisibility(View.VISIBLE);
        super.onPageFinished(view, url);
    }

    private void injectVencord(WebView view) {
        if (HttpClient.VencordRuntime != null) {
            view.evaluateJavascript(HttpClient.VencordRuntime, null);
        }
        if (HttpClient.VencordMobileRuntime != null) {
            view.evaluateJavascript(HttpClient.VencordMobileRuntime, null);
        }
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
        // Strip CSP on main document + CSS so Vencord inject works
        if (req.isForMainFrame() || (req.getUrl().getPath() != null && req.getUrl().getPath().endsWith(".css"))) {
            try {
                return doFetch(req);
            } catch (IOException ex) {
                Logger.e("shouldInterceptRequest", ex);
            }
        }
        return null;
    }

    private WebResourceResponse doFetch(WebResourceRequest req) throws IOException {
        var url = req.getUrl().toString();
        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestMethod(req.getMethod());
        Map<String, String> reqHeaders = req.getRequestHeaders();
        if (reqHeaders != null) {
            for (var h : reqHeaders.entrySet()) {
                conn.setRequestProperty(h.getKey(), h.getValue());
            }
        }

        int code = conn.getResponseCode();
        String msg = conn.getResponseMessage() != null ? conn.getResponseMessage() : "OK";

        Map<String, List<String>> headers = conn.getHeaderFields();
        Map<String, String> modified = new HashMap<>();
        if (headers != null) {
            for (var header : headers.entrySet()) {
                if (header.getKey() == null) continue;
                if ("Content-Security-Policy".equalsIgnoreCase(header.getKey())) continue;
                if ("Content-Security-Policy-Report-Only".equalsIgnoreCase(header.getKey())) continue;
                if (header.getValue() != null && !header.getValue().isEmpty()) {
                    modified.put(header.getKey(), header.getValue().get(0));
                }
            }
        }
        if (url.endsWith(".css")) modified.put("Content-Type", "text/css");

        String mime = modified.getOrDefault("Content-Type", "text/html");
        if (mime.contains(";")) mime = mime.substring(0, mime.indexOf(';')).trim();

        return new WebResourceResponse(
                mime,
                "utf-8",
                code,
                msg,
                modified,
                code >= 400 ? conn.getErrorStream() : conn.getInputStream());
    }
}
