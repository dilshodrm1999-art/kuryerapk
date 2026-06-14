package uz.kuryer.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.DownloadListener;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Kuryer ilovasi — testercol.xo.je saytining kuryer panelini WebView orqali ochadi.
 * Geolokatsiya (GPS), mikrofon (ratsiya), fayl yuklash va pull-to-refresh qo'llab-quvvatlanadi.
 */
public class MainActivity extends AppCompatActivity {

    // Kuryer paneli manzili. Domeningiz o'zgarsa shu yerni o'zgartiring.
    private static final String START_URL = "https://testercol.xo.je/login.php";
    private static final String HOST = "testercol.xo.je";

    private WebView webView;
    private SwipeRefreshLayout swipe;
    private ValueCallback<Uri[]> filePathCallback;
    private PermissionRequest pendingPermissionRequest;

    private ActivityResultLauncher<String[]> permLauncher;
    private ActivityResultLauncher<Intent> fileLauncher;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipe = findViewById(R.id.swipe);
        webView = findViewById(R.id.webview);

        // Ruxsatlar uchun launcher
        permLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // Mikrofon ruxsati so'ralgan bo'lsa, WebView so'roviga javob beramiz
                    if (pendingPermissionRequest != null) {
                        boolean micGranted = ContextCompat.checkSelfPermission(this,
                                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                        if (micGranted) {
                            pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
                        } else {
                            pendingPermissionRequest.deny();
                        }
                        pendingPermissionRequest = null;
                    }
                });

        // Fayl tanlash (rasm yuklash) uchun launcher
        fileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (filePathCallback == null) return;
                    Uri[] uris = null;
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri data = result.getData().getData();
                        if (data != null) uris = new Uri[]{data};
                    }
                    filePathCallback.onReceiveValue(uris);
                    filePathCallback = null;
                });

        setupWebView();

        // Boshlang'ich ruxsatlarni so'raymiz (GPS + mikrofon)
        requestRuntimePermissions();

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        // Pull-to-refresh
        swipe.setOnRefreshListener(() -> webView.reload());

        // Orqaga tugmasi: WebView tarixida orqaga
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); // ratsiya ovozi avto-chalinadi
        s.setAllowFileAccess(true);
        s.setSupportZoom(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setUserAgentString(s.getUserAgentString() + " KuryerApp/1.0");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                // tel: va boshqa tashqi havolalarni tashqi ilovaga uzatamiz
                String scheme = uri.getScheme();
                if (scheme != null && (scheme.equals("tel") || scheme.equals("mailto")
                        || scheme.equals("geo") || scheme.startsWith("whatsapp"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {}
                    return true;
                }
                // O'z domenimiz — WebView ichida; tashqi sayt — tashqi brauzerda
                if (host != null && (host.equals(HOST) || host.endsWith("." + HOST))) {
                    return false;
                }
                // Google Maps yo'l ko'rsatish kabi tashqi havolalar
                if (host != null && (host.contains("google.com") || host.contains("openstreetmap"))) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipe.setRefreshing(false);
                // Ilova ichida ko'rinmasligi kerak bo'lgan elementlarni (havola/URL) yashiramiz
                String js =
                    "(function(){try{" +
                    "var s=document.getElementById('kuryerAppStyle');" +
                    "if(!s){s=document.createElement('style');s=document.head.appendChild(s);s.id='kuryerAppStyle';" +
                    "s.innerHTML='" +
                    " .leaflet-control-attribution{display:none!important;}" +   // xaritadagi havolalar
                    " a[href^=\"http\"][target=\"_blank\"].raw-link{display:none!important;}" +
                    "';}" +
                    "}catch(e){}})();";
                view.evaluateJavascript(js, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // Geolokatsiya so'rovi (kuryer GPS)
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                boolean granted = ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                callback.invoke(origin, granted, false);
                if (!granted) requestRuntimePermissions();
            }

            // Mikrofon so'rovi (ratsiya — ovoz yozish)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    for (String res : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res)) {
                            if (ContextCompat.checkSelfPermission(MainActivity.this,
                                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                request.grant(request.getResources());
                            } else {
                                pendingPermissionRequest = request;
                                permLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
                            }
                            return;
                        }
                    }
                    request.grant(request.getResources());
                });
            }

            // Fayl tanlash (rasm/pasport yuklash)
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    fileLauncher.launch(intent);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Fayl tanlab bo'lmadi", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        // Fayllarni yuklab olish (masalan pasport rasmi)
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                    String mimetype, long contentLength) {
                try {
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "kuryer_file");
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) dm.enqueue(req);
                    Toast.makeText(MainActivity.this, "Yuklab olinmoqda...", Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {}
            }
        });
    }

    private void requestRuntimePermissions() {
        java.util.List<String> need = new java.util.ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!need.isEmpty()) {
            ActivityCompat.requestPermissions(this, need.toArray(new String[0]), 100);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}
