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

/**
 * Important: do NOT re-fetch the main document with HttpURLConnection.
 * That drops / desyncs WebView cookies and causes Discord errors like
 * "Impossible de charger l'objet".
 *
 * Vencord is injected via evaluateJavascript only.
 */
public class VWebviewClient extends WebViewClient {

    private String lastInjectedDocument = "";

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
        lastInjectedDocument = "";
        injectVencord(view, url, false);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        injectVencord(view, url, true);
        view.setVisibility(View.VISIBLE);
        CookieManager.getInstance().flush();
        super.onPageFinished(view, url);
    }

    private void injectVencord(WebView view, String url, boolean pageFinished) {
        if (HttpClient.VencordRuntime == null) return;
        if (url == null || url.startsWith("about:")) return;

        if (pageFinished) {
            if (url.equals(lastInjectedDocument)) return;
        } else if (!lastInjectedDocument.isEmpty()) {
            return;
        }

        String bootstrap =
                "(function(){"
                        + "if(window.__VendroidVencordInjected)return 'skip';"
                        + "window.__VendroidVencordInjected=1;"
                        + "return 'go';"
                        + "})()";

        view.evaluateJavascript(
                bootstrap,
                status -> {
                    if ("\"skip\"".equals(status) || "skip".equals(status)) {
                        lastInjectedDocument = url != null ? url : lastInjectedDocument;
                        return;
                    }
                    view.evaluateJavascript(HttpClient.VencordRuntime, null);
                    if (HttpClient.VencordMobileRuntime != null) {
                        view.evaluateJavascript(HttpClient.VencordMobileRuntime, null);
                    }
                    if (pageFinished && url != null) {
                        lastInjectedDocument = url;
                    }
                    Logger.d("Vencord inject status=" + status + " url=" + url);
                });
    }

    /**
     * Never intercept Discord traffic. Letting WebView handle requests keeps
     * cookies, HTTP/2, cache and CDN auth intact (attachments, emojis, settings).
     */
    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return null;
    }
}
