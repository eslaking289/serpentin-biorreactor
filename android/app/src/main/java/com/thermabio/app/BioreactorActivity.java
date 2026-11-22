package com.thermabio.app;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

/** Local, offline bioreactor explainer and preliminary sizing tool. */
public final class BioreactorActivity extends Activity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(252, 250, 227));
        getWindow().setNavigationBarColor(Color.rgb(245, 243, 220));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(252, 250, 227));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(false);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setBlockNetworkLoads(true);
        webView.getSettings().setSupportMultipleWindows(false);
        webView.addJavascriptInterface(new ModelBridge(), "ThermaBioAndroid");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !"file".equalsIgnoreCase(request.getUrl().getScheme());
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/bioreactor/index.html");
    }

    private final class ModelBridge {
        @JavascriptInterface public void closeModule() { runOnUiThread(BioreactorActivity.this::finish); }

        @JavascriptInterface public String getInitialData() {
            JSONObject data = new JSONObject();
            try {
                for (String key : new String[]{"qMet", "volume", "agitation", "tBroth", "tIn", "tOut", "coolantFlow", "rho", "cp", "mu", "kf", "hBroth", "wallK", "fouling", "tubeD", "wallT", "coilD", "tankD", "clearance", "pitch", "tankHeight"}) {
                    if (getIntent().hasExtra(key)) data.put(key, getIntent().getDoubleExtra(key, Double.NaN));
                }
            } catch (JSONException ignored) { }
            return data.toString();
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) { webView.removeJavascriptInterface("ThermaBioAndroid"); webView.destroy(); }
        super.onDestroy();
    }
}
