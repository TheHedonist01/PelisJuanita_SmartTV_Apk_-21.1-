package com.pelisj.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private TvWebView webView;
    private WebChromeClient chromeClient;
    private View cursorView;
    private FrameLayout rootLayout;
    private View splashLayout;
    private FrameLayout videoContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean splashDismissed = false;

    private float cursorX = 0f;
    private float cursorY = 0f;

    private SharedPreferences prefs;
    private static final String PREF_URL      = "url";
    private static final String PREF_SPEED    = "speed";
    private static final String DEFAULT_URL   = "https://pelisjuanita.com/smart/";
    private static final int    DEFAULT_SPEED = 18;

    private final Handler moveHandler       = new Handler(Looper.getMainLooper());
    private final Handler cursorHideHandler = new Handler(Looper.getMainLooper());
    private Runnable moveRunnable;
    private Runnable hideRunnable;
    private boolean keyHeld  = false;
    private int     pendingDx = 0;
    private int     pendingDy = 0;
    private int     cursorSize;

    private static final long CURSOR_HIDE_DELAY = 3000;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Aceleración de Hardware para máxima fluidez en video
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        prefs      = getSharedPreferences("pelisjuanita", MODE_PRIVATE);
        cursorSize = dpToPx(28);

        setContentView(R.layout.activity_main);
        rootLayout     = findViewById(R.id.root_layout);
        videoContainer = findViewById(R.id.video_container);

        // Reemplazar el WebView del layout por nuestro TvWebView personalizado
        webView = new TvWebView(this);
        FrameLayout.LayoutParams wvLp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        // Insertar en posición 0 (detrás del cursor)
        rootLayout.addView(webView, 0, wvLp);

        // Quitar el WebView original del XML (ya no lo usamos)
        View xmlWebView = findViewById(R.id.webview);
        if (xmlWebView != null) rootLayout.removeView(xmlWebView);

        rootLayout.post(() -> {
            cursorX = rootLayout.getWidth()  / 2f;
            cursorY = rootLayout.getHeight() / 2f;
            updateCursorPosition();
        });

        setupCursor();
        setupWebView();
        showSplash();
        loadUrlWithHeaders(getSavedUrl());
    }

    // ─── TvWebView — captura flechas ANTES de que lleguen al WebView ───────────
    // (clase interna estática para evitar memory leaks)

    public static class TvWebView extends WebView {
        public TvWebView(android.content.Context ctx) {
            super(ctx);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            // Ya no manejamos nada aquí, todo se hace en la Activity para mayor poder
            return super.dispatchKeyEvent(event);
        }
    }

    // ─── Splash ────────────────────────────────────────────────────────────────

    private void showSplash() {
        splashLayout = getLayoutInflater().inflate(R.layout.splash, rootLayout, false);
        rootLayout.addView(splashLayout);
        if (cursorView != null) cursorView.setVisibility(View.INVISIBLE);

        View content = splashLayout.findViewById(R.id.splash_content);
        content.animate()
            .alpha(1f)
            .setDuration(700)
            .withEndAction(() ->
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                    splashLayout.animate()
                        .alpha(0f)
                        .setDuration(700)
                        .withEndAction(() -> {
                            rootLayout.removeView(splashLayout);
                            splashDismissed = true;
                            showCursorTemporarily();
                        }).start()
                , 3000))
            .start();
    }

    // ─── WebView setup ─────────────────────────────────────────────────────────

    @SuppressLint({"SetJavaScriptEnabled", "HardwareIds"})
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);

        // Optimización de rendimiento para Smart TV
        s.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // ── Cloudflare fix ──────────────────────────────────────────────────
        // 1. UA de Chrome Mobile estable (menos sospechoso que Desktop en Android)
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        );
        // 2. Cookies (CF las necesita para el challenge)
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);
        // 3. Deshabilitar el flag que delata al WebView ante CF
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(false);
        }
        // ────────────────────────────────────────────────────────────────────

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                loadUrlWithHeaders(url);
                return true;
            }
        });

        chromeClient = new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    onHideCustomView();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                
                videoContainer.addView(customView);
                videoContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                
                // CRÍTICO: Asegurar que el cursor esté por encima de todo y recupere el foco
                if (cursorView != null) {
                    rootLayout.bringChildToFront(cursorView);
                    cursorView.setVisibility(View.VISIBLE);
                    cursorView.setAlpha(1f);
                }
                rootLayout.requestFocus();
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;

                videoContainer.removeView(customView);
                customView = null;
                videoContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                if (cursorView != null) cursorView.setVisibility(View.VISIBLE);
                
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
            }
        };
        webView.setWebChromeClient(chromeClient);
    }

    // ─── Cursor ────────────────────────────────────────────────────────────────

    private void setupCursor() {
        cursorView = new View(this) {
            { setBackgroundResource(R.drawable.cursor_shape); }
        };
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cursorSize, cursorSize);
        cursorView.setVisibility(View.INVISIBLE);
        rootLayout.addView(cursorView, lp);
    }

    private void updateCursorPosition() {
        if (cursorView == null) return;
        cursorView.setX(cursorX - cursorSize / 2f);
        cursorView.setY(cursorY - cursorSize / 2f);

        // Si estamos en video, despertamos los controles del reproductor
        if (customView != null) {
            String js = "(function(){" +
                    "var v = document.querySelector('video');" +
                    "if(v) {" +
                    "  v.dispatchEvent(new MouseEvent('mousemove', {bubbles:true, clientX: " + cursorX + ", clientY: " + cursorY + "}));" +
                    "  v.dispatchEvent(new MouseEvent('mouseover', {bubbles:true}));" +
                    "}" +
                    "})()";
            webView.evaluateJavascript(js, null);
        }
    }

    private void clampCursor() {
        int w = rootLayout.getWidth();
        int h = rootLayout.getHeight();
        if (w > 0) cursorX = Math.max(0, Math.min(cursorX, w));
        if (h > 0) cursorY = Math.max(0, Math.min(cursorY, h));
    }

    private void showCursorTemporarily() {
        if (!splashDismissed || cursorView == null) return;
        cursorHideHandler.removeCallbacks(hideRunnable != null ? hideRunnable : () -> {});
        cursorView.animate().cancel();
        cursorView.setAlpha(1f);
        cursorView.setVisibility(View.VISIBLE);
        scheduleCursorHide();
    }

    private void scheduleCursorHide() {
        if (hideRunnable != null) cursorHideHandler.removeCallbacks(hideRunnable);
        hideRunnable = () -> {
            if (cursorView != null) {
                cursorView.animate().alpha(0f).setDuration(400)
                    .withEndAction(() -> cursorView.setVisibility(View.INVISIBLE))
                    .start();
            }
        };
        cursorHideHandler.postDelayed(hideRunnable, CURSOR_HIDE_DELAY);
    }

    private void clickAtCursor() {
        // Simulación de toque físico real
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        
        // Si hay un video en pantalla completa, enviamos el toque al videoContainer
        View targetView = (customView != null) ? videoContainer : webView;

        MotionEvent eventDown = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0);
        targetView.dispatchTouchEvent(eventDown);

        MotionEvent eventUp = MotionEvent.obtain(downTime, eventTime + 50, MotionEvent.ACTION_UP, cursorX, cursorY, 0);
        targetView.dispatchTouchEvent(eventUp);

        eventDown.recycle();
        eventUp.recycle();

        // Feedback visual
        cursorView.setAlpha(0.3f);
        rootLayout.postDelayed(() -> cursorView.setAlpha(1f), 150);
        scheduleCursorHide();
    }

    // ─── Movimiento continuo ───────────────────────────────────────────────────

    private void startContinuousMove() {
        stopContinuousMove();
        int speed = prefs.getInt(PREF_SPEED, DEFAULT_SPEED);
        moveRunnable = new Runnable() {
            @Override public void run() {
                if (!keyHeld) return;
                
                int w = rootLayout.getWidth();
                int h = rootLayout.getHeight();
                int scrollMargin = dpToPx(30);

                cursorX += pendingDx * speed;
                cursorY += pendingDy * speed;

                // Scroll automático si el cursor llega a los bordes
                if (cursorX >= w - scrollMargin && pendingDx > 0) webView.scrollBy(20, 0);
                if (cursorX <= scrollMargin     && pendingDx < 0) webView.scrollBy(-20, 0);
                if (cursorY >= h - scrollMargin && pendingDy > 0) webView.scrollBy(0, 20);
                if (cursorY <= scrollMargin     && pendingDy < 0) webView.scrollBy(0, -20);

                clampCursor();
                updateCursorPosition();
                moveHandler.postDelayed(this, 16);
            }
        };
        moveHandler.post(moveRunnable);
    }

    private void stopContinuousMove() {
        if (moveRunnable != null) {
            moveHandler.removeCallbacks(moveRunnable);
            moveRunnable = null;
        }
    }

    // ─── Teclas globales (MENU, ATRÁS) ─────────────────────────────────────────

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        int action = event.getAction();

        // CAPTURA TOTAL: Si son flechas o Enter, las manejamos nosotros SIEMPRE
        if (code == KeyEvent.KEYCODE_DPAD_LEFT  ||
            code == KeyEvent.KEYCODE_DPAD_RIGHT ||
            code == KeyEvent.KEYCODE_DPAD_UP    ||
            code == KeyEvent.KEYCODE_DPAD_DOWN  ||
            code == KeyEvent.KEYCODE_DPAD_CENTER ||
            code == KeyEvent.KEYCODE_ENTER       ||
            code == KeyEvent.KEYCODE_NUMPAD_ENTER) {

            if (action == KeyEvent.ACTION_DOWN) {
                if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    if (event.getRepeatCount() == 0) {
                        if (splashDismissed) {
                            showCursorTemporarily();
                            clickAtCursor();
                        }
                    }
                } else {
                    int dx = 0, dy = 0;
                    if (code == KeyEvent.KEYCODE_DPAD_LEFT)  dx = -1;
                    if (code == KeyEvent.KEYCODE_DPAD_RIGHT) dx =  1;
                    if (code == KeyEvent.KEYCODE_DPAD_UP)    dy = -1;
                    if (code == KeyEvent.KEYCODE_DPAD_DOWN)  dy =  1;

                    pendingDx = dx;
                    pendingDy = dy;
                    if (!keyHeld) {
                        keyHeld = true;
                        if (splashDismissed) {
                            showCursorTemporarily();
                            startContinuousMove();
                        }
                    }
                }
            } else if (action == KeyEvent.ACTION_UP) {
                if (code != KeyEvent.KEYCODE_DPAD_CENTER && code != KeyEvent.KEYCODE_ENTER && code != KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    keyHeld = false;
                    stopContinuousMove();
                    scheduleCursorHide();
                }
            }
            return true; // BLOQUEO ABSOLUTO: La tecla no llega a ningún otro lado
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showSettingsDialog();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (customView != null) {
                chromeClient.onHideCustomView();
                return true;
            }
            if (webView.canGoBack()) { webView.goBack(); return true; }
        }
        return super.onKeyDown(keyCode, event);
    }

    // ─── Ajustes ───────────────────────────────────────────────────────────────

    private void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(20);
        layout.setPadding(pad, pad, pad, pad);

        TextView urlLabel = new TextView(this);
        urlLabel.setText("URL de la página:");
        urlLabel.setTextSize(16);
        layout.addView(urlLabel);

        EditText urlField = new EditText(this);
        urlField.setText(getSavedUrl());
        urlField.setSingleLine(true);
        layout.addView(urlField);

        View sep = new View(this);
        sep.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(16)));
        layout.addView(sep);

        TextView speedLabel = new TextView(this);
        int curSpeed = prefs.getInt(PREF_SPEED, DEFAULT_SPEED);
        speedLabel.setText("Velocidad del cursor: " + curSpeed);
        speedLabel.setTextSize(16);
        layout.addView(speedLabel);

        SeekBar speedBar = new SeekBar(this);
        speedBar.setMax(40);
        speedBar.setProgress(curSpeed);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int v, boolean u) {
                speedLabel.setText("Velocidad del cursor: " + Math.max(4, v));
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        layout.addView(speedBar);

        new AlertDialog.Builder(this)
            .setTitle("⚙ Ajustes — Pelis Juanita")
            .setView(layout)
            .setPositiveButton("Guardar y recargar", (d, w) -> {
                String newUrl = urlField.getText().toString().trim();
                if (newUrl.isEmpty()) newUrl = DEFAULT_URL;
                if (!newUrl.startsWith("http")) newUrl = "https://" + newUrl;
                int newSpeed = Math.max(4, speedBar.getProgress());
                prefs.edit()
                    .putString(PREF_URL, newUrl)
                    .putInt(PREF_SPEED, newSpeed)
                    .apply();
                loadUrlWithHeaders(newUrl);
                Toast.makeText(this, "Guardado ✓", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void loadUrlWithHeaders(String url) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Requested-With", "com.android.chrome");
        headers.put("Accept-Language", "es-ES,es;q=0.9,en;q=0.8");
        webView.loadUrl(url, headers);
    }

    private String getSavedUrl() { return prefs.getString(PREF_URL, DEFAULT_URL); }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override protected void onResume()  { super.onResume();  webView.onResume(); }
    @Override protected void onPause()   { super.onPause();   webView.onPause(); stopContinuousMove(); }
    @Override protected void onDestroy() { super.onDestroy(); stopContinuousMove(); webView.destroy(); }
}
