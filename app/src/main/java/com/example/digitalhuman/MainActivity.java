package com.example.digitalhuman;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    // 你的服务器地址
    private static final String TARGET_URL = "https://172.16.2.211:8000";

    private WebView myWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trustAllHosts();

        // === 竖屏沉浸式设置 ===
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        
        // 隐藏导航栏
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        myWebView = new WebView(this);
        myWebView.setBackgroundColor(Color.BLACK);
        setContentView(myWebView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 🌟 删除掉了伪装成电脑的代码 (setUserAgentString)
        // 🌟 删除掉了强制缩放的代码 (setUseWideViewPort, setLoadWithOverviewMode)
        // 这样网页会以原生的 100% 比例渲染，显示效果最清晰

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            // 🌟🌟🌟 缓存拦截逻辑 (保持不变) 🌟🌟🌟
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.endsWith("bg.png")) return checkUpdateAndDownload(url, "bg.png", "image/png");
                if (url.endsWith("hao_wait.mp4")) return checkUpdateAndDownload(url, "hao_wait.mp4", "video/mp4");
                if (url.endsWith("hutao_talking.mp4")) return checkUpdateAndDownload(url, "hutao_talking.mp4", "video/mp4");
                return super.shouldInterceptRequest(view, request);
            }
        });

        myWebView.loadUrl(TARGET_URL);
    }

    // 🛠️ 智能缓存下载器 (逻辑保持不变)
    private WebResourceResponse checkUpdateAndDownload(String urlString, String fileName, String mimeType) {
        try {
            File cacheDir = new File(getFilesDir(), "smart_cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File localFile = new File(cacheDir, fileName);

            SharedPreferences prefs = getSharedPreferences("CachePrefs", Context.MODE_PRIVATE);
            long localLastModified = prefs.getLong(fileName + "_last_mod", 0);

            long serverLastModified = 0;
            boolean needDownload = false;

            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) conn).setSSLSocketFactory(trustAllSslSocketFactory);
                    ((HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
                }
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(2000); 
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    serverLastModified = conn.getLastModified();
                    if (serverLastModified > localLastModified || !localFile.exists() || localFile.length() == 0) {
                        needDownload = true;
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                if (!localFile.exists()) return null;
            }

            if (needDownload) {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    if (conn instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) conn).setSSLSocketFactory(trustAllSslSocketFactory);
                        ((HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
                    }
                    conn.setRequestMethod("GET");
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(localFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close(); is.close(); conn.disconnect();
                    prefs.edit().putLong(fileName + "_last_mod", serverLastModified).apply();
                } catch (Exception e) {
                    if (!localFile.exists()) return null;
                }
            }

            if (localFile.exists()) {
                FileInputStream fis = new FileInputStream(localFile);
                return new WebResourceResponse(mimeType, "UTF-8", fis);
            }

        } catch (Exception e) { e.printStackTrace(); }
        return null; 
    }

    private static javax.net.ssl.SSLSocketFactory trustAllSslSocketFactory;
    private void trustAllHosts() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            trustAllSslSocketFactory = sc.getSocketFactory();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
