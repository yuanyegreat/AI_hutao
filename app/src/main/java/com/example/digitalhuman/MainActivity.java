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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    // 🔴 你的服务器地址 (保持 HTTPS)
    private static final String TARGET_URL = "https://172.16.2.211:8000";

    private WebView myWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === 1. 初始化信任所有证书的下载器 (为了解决自签名证书无法下载的问题) ===
        trustAllHosts();

        // === 2. 沉浸式窗口设置 ===
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        myWebView = new WebView(this);
        myWebView.setBackgroundColor(Color.BLACK);
        setContentView(myWebView);

        // 权限申请
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }

        // === 3. WebView 设置 ===
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 伪装电脑浏览器
        String pcUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
        webSettings.setUserAgentString(pcUserAgent);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // 忽略 WebView 里的证书错误
            }

            // 🌟🌟🌟 核心逻辑：智能拦截缓存 🌟🌟🌟
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // 🎯 拦截目标：背景图、两个视频
                if (url.endsWith("bg.png")) {
                    return checkUpdateAndDownload(url, "bg.png", "image/png");
                }
                if (url.endsWith("hao_wait.mp4")) {
                    return checkUpdateAndDownload(url, "hao_wait.mp4", "video/mp4");
                }
                if (url.endsWith("hutao_talking.mp4")) {
                    return checkUpdateAndDownload(url, "hutao_talking.mp4", "video/mp4");
                }

                return super.shouldInterceptRequest(view, request);
            }
        });

        myWebView.loadUrl(TARGET_URL);
    }

    // 🛠️ 智能缓存下载器
    private WebResourceResponse checkUpdateAndDownload(String urlString, String fileName, String mimeType) {
        try {
            File cacheDir = new File(getFilesDir(), "smart_cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File localFile = new File(cacheDir, fileName);

            // 获取本地保存的版本号（上次修改时间）
            SharedPreferences prefs = getSharedPreferences("CachePrefs", Context.MODE_PRIVATE);
            long localLastModified = prefs.getLong(fileName + "_last_mod", 0);

            // 1. 询问服务器：文件变了吗？(HEAD 请求，极快)
            long serverLastModified = 0;
            boolean needDownload = false;

            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn instanceof HttpsURLConnection) {
                    // 允许 HTTPS 自签名
                    ((HttpsURLConnection) conn).setSSLSocketFactory(trustAllSslSocketFactory);
                    ((HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
                }
                conn.setRequestMethod("HEAD"); // 只拿头信息，不下载内容
                conn.setConnectTimeout(3000); // 3秒超时，避免卡顿
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    serverLastModified = conn.getLastModified();
                    // 如果服务器时间 > 本地记录时间，或者本地文件不存在，说明需要下载
                    if (serverLastModified > localLastModified || !localFile.exists() || localFile.length() == 0) {
                        needDownload = true;
                        Log.d("SmartCache", "发现更新: " + fileName);
                    } else {
                        Log.d("SmartCache", "使用缓存: " + fileName);
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                // 如果联网失败（比如断网），优先用本地文件
                Log.e("SmartCache", "联网检查失败，尝试使用本地: " + e.getMessage());
                if (!localFile.exists()) return null; // 本地也没有，放弃拦截，让WebView自己处理
            }

            // 2. 如果需要下载（初次启动 或 有更新）
            if (needDownload) {
                try {
                    Log.d("SmartCache", "开始下载: " + fileName);
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    if (conn instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) conn).setSSLSocketFactory(trustAllSslSocketFactory);
                        ((HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
                    }
                    conn.setRequestMethod("GET");
                    conn.connect();

                    // 下载流写入文件
                    InputStream is = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(localFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    is.close();
                    conn.disconnect();

                    // 下载成功，更新本地版本号
                    prefs.edit().putLong(fileName + "_last_mod", serverLastModified).apply();
                    Log.d("SmartCache", "下载完成: " + fileName);

                } catch (Exception e) {
                    Log.e("SmartCache", "下载失败: " + e.getMessage());
                    // 如果下载失败但本地有旧文件，勉强先用旧的
                    if (!localFile.exists()) return null;
                }
            }

            // 3. 返回本地文件流给 WebView
            if (localFile.exists()) {
                FileInputStream fis = new FileInputStream(localFile);
                return new WebResourceResponse(mimeType, "UTF-8", fis);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // 发生意外，让 WebView 自己去网络加载
    }

    // === SSL 辅助工具：允许自签名证书 (关键！) ===
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
