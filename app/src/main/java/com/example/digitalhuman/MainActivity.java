package com.example.digitalhuman;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // 🔴 这里的地址保持为你服务器的 HTTPS 地址
    // 网页里的资源引用保持相对路径（例如 src="bg.png"）
    private static final String TARGET_URL = "https://172.16.2.211:8000";

    private WebView myWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === 界面设置 (保持不变) ===
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        myWebView = new WebView(this);
        myWebView.setBackgroundColor(Color.BLACK);
        setContentView(myWebView);

        // 权限申请
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        
        // 允许跨域和混合内容（为了兼容性）
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 伪装成电脑浏览器（解决布局问题）
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
            // 忽略 SSL 错误
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            // 🌟🌟🌟 核心修改：拦截资源请求，替换为本地文件 🌟🌟🌟
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // 1. 拦截背景图片
                if (url.contains("bg.png")) {
                    try {
                        // 打开本地 assets 里的 bg.png
                        InputStream is = getAssets().open("bg.png");
                        // 伪造一个 HTTP 响应返回给网页
                        return new WebResourceResponse("image/png", "UTF-8", is);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // 2. 拦截待机视频 (hao_wait.mp4)
                if (url.contains("hao_wait.mp4")) {
                    try {
                        InputStream is = getAssets().open("hao_wait.mp4");
                        // 注意：MIME 类型要是 video/mp4
                        return new WebResourceResponse("video/mp4", "UTF-8", is);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // 3. 拦截说话视频 (hutao_talking.mp4)
                if (url.contains("hutao_talking.mp4")) {
                    try {
                        InputStream is = getAssets().open("hutao_talking.mp4");
                        return new WebResourceResponse("video/mp4", "UTF-8", is);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // 其他请求（如 API 接口）走正常网络
                return super.shouldInterceptRequest(view, request);
            }
        });

        myWebView.loadUrl(TARGET_URL);
    }
}
