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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    // ⚠️ 重要：为了用麦克风，请尝试尽量使用 https (即使证书是自签名的也没关系，下面代码会忽略错误)
    // 如果你的服务器实在没有 https，只能保留 http，但麦克风大概率会被浏览器内核拦截
    private static final String TARGET_URL = "https://172.16.2.211:8000"; 
    // ↑↑↑ 请记得把这里改成 https (端口如果变了也要改)

    private WebView myWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === 修复 1：彻底消灭白边 (背景黑 + 铺满摄像头) ===
        // 去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置全屏 Flag
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // 关键：把窗口背景设为纯黑，防止刘海处露出白色底色
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        // 适配刘海屏/挖孔屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        // 隐藏导航栏，并开启"Layout"标志，确保内容延伸到系统栏后面
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN  // 关键：让布局延伸到状态栏区域
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // 关键：让布局延伸到导航栏区域
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        // ===========================================

        myWebView = new WebView(this);
        // 防止 WebView 自身有背景色导致闪烁
        myWebView.setBackgroundColor(Color.BLACK);
        setContentView(myWebView);

        // 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        
        // 允许 HTTP 和 HTTPS 混合内容 (为了兼容性)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // === 修复 2：布局适配微调 ===
        // 伪装成电脑 Chrome
        String pcUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
        webSettings.setUserAgentString(pcUserAgent);

        // 开启宽视口（让网页以为自己在宽屏上）
        webSettings.setUseWideViewPort(true);
        // 开启概览模式（自动缩小网页以适应手机屏幕宽度，解决按钮错位）
        webSettings.setLoadWithOverviewMode(true);
        
        // 强制初始缩放比例，防止系统字号影响布局
        webSettings.setTextZoom(100);
        // ======================================

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // 自动批准麦克风权限
                request.grant(request.getResources());
            }
        });

        myWebView.setWebViewClient(new WebViewClient() {
            // === 修复 3：忽略 HTTPS 证书错误 (允许自签名/IP地址的 HTTPS) ===
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // ⚠️ 这一行非常关键：告诉 WebView "不要管证书不对，继续加载！"
                // 这样你就可以用 https://172.x.x.x 而不报错
                handler.proceed();
            }
        });

        myWebView.loadUrl(TARGET_URL);
    }
}
