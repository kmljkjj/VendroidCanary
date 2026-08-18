package dev.vendicated.vencord;

import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class VChromeClient extends WebChromeClient {
    private final MainActivity activity;

    public VChromeClient(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onShowFileChooser(
            WebView webView,
            ValueCallback<Uri[]> filePathCallback,
            FileChooserParams fileChooserParams) {
        if (activity.filePathCallback != null) {
            activity.filePathCallback.onReceiveValue(null);
        }
        activity.filePathCallback = filePathCallback;
        try {
            activity.startActivityForResult(
                    fileChooserParams.createIntent(), MainActivity.FILECHOOSER_RESULTCODE);
        } catch (Exception e) {
            activity.filePathCallback = null;
            Logger.e("File chooser failed", e);
            return false;
        }
        return true;
    }
}
