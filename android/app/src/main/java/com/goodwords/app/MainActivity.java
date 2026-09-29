package com.goodwords.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.webkit.WebResourceErrorCompat;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 把「好词好句」网页包进 APK。
 *
 * 更新策略（在线 + 离线兜底）：
 *  - 启动后先探测在线版是否可达；可达则加载在线版，我改完重新发布后 app 下次打开即自动更新。
 *  - 在线不可达（断网）则回退到装机时打包进 assets 的本地版本（含离线词典，可离线使用）。
 *
 * 本地资源通过 WebViewAssetLoader 映射为 https://appassets.androidplatform.net/assets/，
 * 这样网页里的相对路径（pinyin-pro.js、idiom.json 的 fetch、图标等）都能正常加载；
 * 远程请求（在线版、GitHub Gist 云同步）则放行由 WebView 自行联网加载。
 */
public class MainActivity extends AppCompatActivity {

    // 在线版地址：我重新发布后内容即更新，app 打开自动拉取最新版
    private static final String ONLINE_URL = "https://a145a492201afc491.app.workbuddy.host/";
    // 本地兜底：打包进 APK 的网页（离线可用）
    private static final String LOCAL_URL  = "https://appassets.androidplatform.net/assets/index.html";
    private static final String ASSET_DOMAIN = "appassets.androidplatform.net";

    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean usingOnline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(ASSET_DOMAIN)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = findViewById(R.id.webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // 仅拦截本地 assets 域；在线版、GitHub Gist 等远程请求放行
                if (ASSET_DOMAIN.equals(request.getUrl().getHost())) {
                    return assetLoader.shouldInterceptRequest(request.getUrl());
                }
                return null;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceErrorCompat error) {
                // 在线主文档加载失败（如打开时已断网）时，回退到本地打包版本
                if (usingOnline && request.isForMainFrame()
                        && !LOCAL_URL.equals(request.getUrl().toString())) {
                    usingOnline = false;
                    view.loadUrl(LOCAL_URL);
                }
                super.onReceivedError(view, request, error);
            }
        });

        hideSystemUI();

        if (savedInstanceState == null) {
            decideAndLoad();
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    /** 探测在线版可达性后决定加载在线还是本地兜底 */
    private void decideAndLoad() {
        executor.execute(() -> {
            boolean online = ping(ONLINE_URL);
            mainHandler.post(() -> loadUrl(online ? ONLINE_URL : LOCAL_URL));
        });
    }

    private void loadUrl(String url) {
        usingOnline = ONLINE_URL.equals(url);
        webView.loadUrl(url);
    }

    /** 轻量探测：在线版根路径返回 2xx/3xx 视为可达 */
    private boolean ping(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** 沉浸式但不隐藏状态栏/导航栏：内容延伸到系统栏下方，状态栏半透明悬浮可见 */
    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    /** 返回键：先让网页内返回，再退出应用 */
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}
