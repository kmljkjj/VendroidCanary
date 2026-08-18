package dev.vendicated.vencord;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final int FILECHOOSER_RESULTCODE = 8485;

    private WebView wv;
    private boolean wvInitialized = false;
    public ValueCallback<Uri[]> filePathCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        setContentView(R.layout.activity_main);
        setupWindow();

        wv = findViewById(R.id.webview);
        configureWebView(wv);

        wv.setWebViewClient(new VWebviewClient());
        wv.setWebChromeClient(new VChromeClient(this));
        wv.addJavascriptInterface(new VencordNative(this, wv), "VencordMobileNative");

        wv.setVisibility(View.INVISIBLE);

        HttpClient.fetchVencordAsync(
                this,
                this::loadDiscord,
                () -> Toast.makeText(
                                this,
                                "Impossible de télécharger Vencord. Réessaie.",
                                Toast.LENGTH_LONG)
                        .show());
    }

    private void setupWindow() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.parseColor("#1E1F22"));
        getWindow().setNavigationBarColor(Color.parseColor("#1E1F22"));
        // Keep screen responsive; Discord is heavy
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView webView) {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setOffscreenPreRaster(true);
        s.setSafeBrowsingEnabled(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setGeolocationEnabled(false);
        s.setTextZoom(100);

        // Critical: desktop UA → full Discord client (settings work)
        s.setUserAgentString(Constants.DESKTOP_USER_AGENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            s.setForceDark(WebSettings.FORCE_DARK_OFF);
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setNestedScrollingEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);
    }

    private void loadDiscord() {
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            handleUrl(intent.getData());
        } else {
            wv.loadUrl(Constants.DISCORD_APP_URL);
        }
        wvInitialized = true;
    }

    private void handleUrl(Uri data) {
        String host = data.getHost();
        if (Constants.isDiscordHost(host)) {
            wv.loadUrl(data.toString());
        } else {
            wv.loadUrl(Constants.DISCORD_APP_URL);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (wvInitialized && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            handleUrl(intent.getData());
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && wv != null) {
            wv.evaluateJavascript(
                    "(function(){try{if(window.VencordMobile&&VencordMobile.onBackPress){return!!VencordMobile.onBackPress();}return false;}catch(e){return false;}})()",
                    r -> {
                        if ("false".equals(r) || "null".equals(r)) {
                            if (wv.canGoBack()) wv.goBack();
                            else finish();
                        }
                    });
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && intent != null) {
                String dataString = intent.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                } else if (intent.getClipData() != null) {
                    final int count = intent.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = intent.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onPause() {
        if (wv != null) {
            CookieManager.getInstance().flush();
            wv.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wv != null) wv.onResume();
    }

    @Override
    protected void onDestroy() {
        if (wv != null) {
            wv.loadUrl("about:blank");
            wv.stopLoading();
            wv.destroy();
            wv = null;
        }
        super.onDestroy();
    }
}
