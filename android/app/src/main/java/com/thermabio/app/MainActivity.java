package com.thermabio.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Runs the supplied self-contained bioreactor HTML as the whole app. */
public final class MainActivity extends Activity {
    private static final String APP_PAGE = "file:///android_asset/biorreactor-v6.html";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Keep the HTML inside the usable area, below Android's status and navigation bars.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }
        getWindow().setStatusBarColor(Color.rgb(18, 19, 22));
        getWindow().setNavigationBarColor(Color.rgb(18, 19, 22));
        getWindow().getDecorView().setSystemUiVisibility(0);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(18, 19, 22));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setOnApplyWindowInsetsListener((view, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(0, top, 0, bottom);
            return insets;
        });
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // Required by the supplied HTML calculator and model.
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(true); // Pinch-to-zoom for the 3D model and diagrams.
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (!APP_PAGE.equals(url)) return;
                float density = getResources().getDisplayMetrics().density;
                int statusId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                int navId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                float topDp = statusId == 0 ? 24f : getResources().getDimension(statusId) / density;
                float bottomDp = navId == 0 ? 24f : getResources().getDimension(navId) / density;
                String top = String.format(java.util.Locale.US, "%.1f", topDp);
                String bottom = String.format(java.util.Locale.US, "%.1f", bottomDp);
                String safeArea = "(function(){var s=document.getElementById('nativeSafeArea');"
                        + "if(!s){s=document.createElement('style');s.id='nativeSafeArea';document.head.appendChild(s);}"
                        + "s.textContent='body{padding-top:" + top + "px!important;padding-bottom:" + bottom + "px!important}';})()";
                view.evaluateJavascript(safeArea, null);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !APP_PAGE.equals(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !APP_PAGE.equals(url);
            }
        });
        setContentView(webView);
        webView.requestApplyInsets();
        webView.loadUrl(APP_PAGE);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
