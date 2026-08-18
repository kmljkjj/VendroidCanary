package dev.vendicated.vencord;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HttpClient {
    public static String VencordRuntime;
    public static String VencordMobileRuntime;

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    public static final class HttpException extends IOException {
        private final HttpURLConnection conn;
        private String message;

        public HttpException(HttpURLConnection conn) {
            this.conn = conn;
        }

        @Override
        @NonNull
        public String getMessage() {
            if (message == null) {
                try (var es = conn.getErrorStream()) {
                    message = String.format(
                            Locale.ENGLISH,
                            "%d: %s (%s)\n%s",
                            conn.getResponseCode(),
                            conn.getResponseMessage(),
                            conn.getURL().toString(),
                            es != null ? readAsText(es) : "");
                } catch (IOException ex) {
                    message = "HTTP error; url=" + conn.getURL();
                }
            }
            return message;
        }
    }

    /** Fetch Vencord on a background thread, then run callback on UI thread. */
    public static void fetchVencordAsync(Activity activity, Runnable onSuccess, Runnable onError) {
        IO.execute(() -> {
            try {
                fetchVencord(activity);
                activity.runOnUiThread(onSuccess);
            } catch (IOException e) {
                Logger.e("Failed to fetch Vencord", e);
                activity.runOnUiThread(onError);
            }
        });
    }

    public static synchronized void fetchVencord(Activity activity) throws IOException {
        if (VencordRuntime != null && VencordMobileRuntime != null) return;

        var res = activity.getResources();
        try (var is = res.openRawResource(R.raw.vencord_mobile)) {
            VencordMobileRuntime = readAsText(is);
        }

        var conn = fetch(Constants.JS_BUNDLE_URL);
        try (var is = conn.getInputStream()) {
            VencordRuntime = readAsText(is);
        }
        Logger.d("Vencord runtime loaded (" + VencordRuntime.length() + " chars)");
    }

    private static HttpURLConnection fetch(String url) throws IOException {
        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "VendroidCanary/1.1");
        conn.setInstanceFollowRedirects(true);
        if (conn.getResponseCode() >= 300) {
            throw new HttpException(conn);
        }
        return conn;
    }

    static String readAsText(InputStream is) throws IOException {
        try (var baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[32768];
            int n;
            while ((n = is.read(buf)) > -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString("UTF-8");
        }
    }
}
