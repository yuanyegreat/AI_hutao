package com.example.digitalhuman;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    // 你的蒲公英外网地址
    private static final String TARGET_URL = "http://172.16.2.211:8000";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === 修复 1：彻底全屏 + 解决挖孔屏黑边 ===
        // 去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏标志
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // 关键：允许内容延伸到刘海/挖孔区域 (解决左侧黑边)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // 隐藏底部导航栏（虚拟按键），进入沉浸模式
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 
                View.SYSTEM_UI_FLAG_FULLSCREEN | 
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        // ===========================================

        WebView myWebView = new WebView(this);
        setContentView(myWebView);

        // 权限检查
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // === 修复 2：布局适配（伪装成电脑） ===
        // 关键：把 UserAgent 设置成电脑 Chrome，骗过网页，让它按电脑版渲染
        String pcUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
        webSettings.setUserAgentString(pcUserAgent);

        // 关键：允许网页缩放以适应屏幕宽度（相当于把大网页缩小塞进手机屏幕）
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        // ======================================

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        myWebView.setWebViewClient(new WebViewClient());
        myWebView.loadUrl(TARGET_URL);
    }
}
