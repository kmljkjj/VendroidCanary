package dev.vendicated.vencord;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.CookieManager;
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

/**
 * Restored first-build behavior (upstream Vendroid style):
 * - inject Vencord on page start
 * - strip CSP on main frame + CSS so injection works
 * - forward WebView cookies on intercepted requests (fix for session)
 */
public class VWebviewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        var url = request.getUrl();
        if ("canary.discord.com".equals(url.getHost())
                || "discord.com".equals(url.getHost())
                || Constants.isDiscordHost(url.getHost())
                || "about:blank".equals(url.toString())) {
            return false;
        }
        try {
            view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, url));
        } catch (Exception e) {
            Logger.e("open url", e);
        }
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // Same as original Vendroid: inject both runtimes at start
        if (HttpClient.VencordRuntime != null) {
            view.evaluateJavascript(HttpClient.VencordRuntime, null);
        }
        if (HttpClient.VencordMobileRuntime != null) {
            view.evaluateJavascript(HttpClient.VencordMobileRuntime, null);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        view.setVisibility(View.VISIBLE);
        CookieManager.getInstance().flush();
        super.onPageFinished(view, url);
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
        String path = req.getUrl().getPath();
        if (req.isForMainFrame() || (path != null && path.endsWith(".css"))) {
            try {
                return doFetch(req);
            } catch (IOException ex) {
                Logger.e("shouldInterceptRequest", ex);
            }
        }
        return null;
    }

    private WebResourceResponse doFetch(WebResourceRequest req) throws IOException {
        String url = req.getUrl().toString();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(45000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod(req.getMethod() != null ? req.getMethod() : "GET");

        // Critical: send the same cookies as the WebView (session / "object" loads)
        String cookie = CookieManager.getInstance().getCookie(url);
        if (cookie != null && !cookie.isEmpty()) {
            conn.setRequestProperty("Cookie", cookie);
        }

        Map<String, String> reqHeaders = req.getRequestHeaders();
        if (reqHeaders != null) {
            for (Map.Entry<String, String> h : reqHeaders.entrySet()) {
                if ("Cookie".equalsIgnoreCase(h.getKey())) continue;
                if ("Accept-Encoding".equalsIgnoreCase(h.getKey())) continue;
                conn.setRequestProperty(h.getKey(), h.getValue());
            }
        }

        int code = conn.getResponseCode();
        if (code < 100) code = 200;
        String msg = conn.getResponseMessage() != null ? conn.getResponseMessage() : "OK";

        // Store Set-Cookie back into WebView jar
        Map<String, List<String>> headerFields = conn.getHeaderFields();
        if (headerFields != null) {
            List<String> setCookies = headerFields.get("Set-Cookie");
            if (setCookies == null) setCookies = headerFields.get("set-cookie");
            if (setCookies != null) {
                CookieManager cm = CookieManager.getInstance();
                for (String sc : setCookies) {
                    cm.setCookie(url, sc);
                }
            }
        }

        Map<String, String> modified = new HashMap<>();
        if (headerFields != null) {
            for (Map.Entry<String, List<String>> header : headerFields.entrySet()) {
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
        if (stream == null) stream = new ByteArrayInputStream(new byte[0]);

        return new WebResourceResponse(mime, encoding, code, msg, modified, stream);
    }
}
