package dev.vendicated.vencord;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * Close to the first VendroidCanary build that worked for you.
 * + Canary URL, async Vencord download, basic WebView settings.
 */
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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.parseColor("#1E1F22"));
        getWindow().setNavigationBarColor(Color.parseColor("#1E1F22"));

        wv = findViewById(R.id.webview);

        // First-build style settings (no forced desktop UA — that broke loading for you)
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        wv.setWebViewClient(new VWebviewClient());
        wv.setWebChromeClient(new VChromeClient(this));
        wv.addJavascriptInterface(new VencordNative(this, wv), "VencordMobileNative");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);

        wv.setVisibility(View.INVISIBLE);

        HttpClient.fetchVencordAsync(
                this,
                this::loadDiscord,
                () -> Toast.makeText(this, "Failed to download Vencord", Toast.LENGTH_LONG).show());
    }

    private void loadDiscord() {
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri data = intent.getData();
            if (Constants.isDiscordHost(data.getHost())) {
                wv.loadUrl(data.toString());
            } else {
                wv.loadUrl(Constants.DISCORD_APP_URL);
            }
        } else {
            wv.loadUrl(Constants.DISCORD_APP_URL);
        }
        wvInitialized = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (wvInitialized && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri data = intent.getData();
            if (Constants.isDiscordHost(data.getHost())) {
                wv.loadUrl(data.toString());
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && wv != null) {
            wv.evaluateJavascript("VencordMobile.onBackPress()", r -> {
                if ("false".equals(r)) {
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
                if (dataString != null) results = new Uri[]{Uri.parse(dataString)};
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
            wv.destroy();
            wv = null;
        }
        super.onDestroy();
    }
}
