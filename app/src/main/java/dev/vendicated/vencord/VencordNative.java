package dev.vendicated.vencord;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

public class VencordNative {
    private final Activity activity;
    private final WebView webView;

    public VencordNative(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public void goBack() {
        activity.runOnUiThread(() -> {
            if (webView.canGoBack()) webView.goBack();
            else activity.finish();
        });
    }
}
