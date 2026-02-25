package com.t3xturedstudios.cinnamon.overlay

import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import com.t3xturedstudios.cinnamon.R
import com.t3xturedstudios.cinnamon.keyboard.MainKeyboardView
import com.t3xturedstudios.cinnamon.latin.LatinIME
import com.t3xturedstudios.cinnamon.QuickSettings
import com.t3xturedstudios.cinnamon.StatsPanel

class CinnamonOverlayController(
    private val root: View,
    private val ime: LatinIME   // ⭐ referencja do LatinIME
) {

    private val keyboardView: MainKeyboardView = root.findViewById(R.id.keyboard_view)
    private val layer: FrameLayout = root.findViewById(R.id.cinnamon_layer)
    private val fab: ImageButton = root.findViewById(R.id.cinnamon_fab)

    private val browserContainer: LinearLayout = root.findViewById(R.id.browser_container)
    private val browserControls: LinearLayout = root.findViewById(R.id.browser_controls)
    private val browserWebContainer: FrameLayout = root.findViewById(R.id.browser_web_container)

    private val btnBack: ImageButton = root.findViewById(R.id.btn_back)
    private val btnClose: ImageButton = root.findViewById(R.id.btn_close)
    private val btnKeyboard: ImageButton = root.findViewById(R.id.btn_keyboard)
    private val btnHideControls: ImageButton = root.findViewById(R.id.btn_hide_controls)
    private val btnEye: ImageButton = root.findViewById(R.id.btn_eye)

    private var webView: WebView? = null
    private var isLayerVisible = false

    init {
        layer.visibility = View.GONE
        layer.isClickable = false
        layer.isFocusable = false
        layer.isFocusableInTouchMode = false

        browserContainer.visibility = View.GONE
        browserControls.visibility = View.GONE
        btnEye.visibility = View.GONE

        setupFab()
        setupControls()
        setupDynamicLayerButtons()
    }

    fun init() {}

    // ============================================================
    // FAB + LAYER
    // ============================================================

    private fun setupFab() {
        fab.setOnClickListener { toggleLayer() }
    }

    private fun toggleLayer() {
        isLayerVisible = !isLayerVisible

        if (isLayerVisible) {
            keyboardView.visibility = View.GONE
            fab.visibility = View.GONE

            layer.visibility = View.VISIBLE
            layer.isClickable = true
            layer.isFocusable = true
            layer.isFocusableInTouchMode = true
        } else {
            hideBrowser()

            layer.visibility = View.GONE
            layer.isClickable = false
            layer.isFocusable = false
            layer.isFocusableInTouchMode = false

            keyboardView.visibility = View.VISIBLE
            fab.visibility = View.VISIBLE
        }
    }

    // ============================================================
    // DYNAMICZNE PRZYCISKI
    // ============================================================

    private fun setupDynamicLayerButtons() {
        val container = com.nex3z.flowlayout.FlowLayout(root.context).apply {
            setPadding(20, 20, 20, 20)
            isClickable = true
            isFocusable = true

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )

            // ⭐ spacing musi być float
            setChildSpacing(20)
            setRowSpacing(20f)
        }


        val googleBtn = createCircleButton(R.drawable.ic_google).apply {
            setOnClickListener { openBrowser("https://www.google.com") }
        }
        container.addView(googleBtn)



        val quickSettingsBtn = createCircleButton(R.drawable.ic_settings).apply {
            setOnClickListener { QuickSettings(root.context, ime).show() }
        }
        container.addView(quickSettingsBtn)
        val clockBtn = createCircleButton(R.drawable.ic_clock).apply {
            setOnClickListener { openSystemClock() }
        }
        container.addView(clockBtn)
        val flashlightBtn = createCircleButton(R.drawable.ic_flashlight).apply {
            setOnClickListener { toggleFlashlight() }
        }
        container.addView(flashlightBtn)
        val statsBtn = createCircleButton(R.drawable.ic_stats).apply {
            setOnClickListener { StatsPanel(root.context, ime).show() }
        }
        container.addView(statsBtn)



        repeat(0) {
            val placeholder = createCircleButton(R.drawable.ic_eye).apply {
                alpha = 0.4f
            }
            container.addView(placeholder)
        }
        val exitBtn = createCircleButton(R.drawable.ic_close).apply {
            setOnClickListener { exitOverlay() }
        }
        container.addView(exitBtn)


        layer.addView(container)
    }
    private var isFlashOn = false

    private fun toggleFlashlight() {
        try {
            val cameraManager = root.context.getSystemService(android.content.Context.CAMERA_SERVICE)
                    as android.hardware.camera2.CameraManager

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val flashAvailable = chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val lensFacing = chars.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                flashAvailable == true && lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } ?: return

            isFlashOn = !isFlashOn
            cameraManager.setTorchMode(cameraId, isFlashOn)

        } catch (e: Exception) {
            Log.e("Cinnamon", "Flashlight toggle failed", e)
        }
    }

    private fun openSystemClock() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            root.context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("Cinnamon", "Cannot open system clock", e)
        }
    }


    fun exitOverlay() {
        // Jeśli przeglądarka otwarta → zamknij
        hideBrowser()

        // Ukryj overlay
        layer.visibility = View.GONE
        layer.isClickable = false
        layer.isFocusable = false
        layer.isFocusableInTouchMode = false

        // Pokaż klawiaturę
        keyboardView.visibility = View.VISIBLE

        // Pokaż FAB
        fab.visibility = View.VISIBLE

        // Wyłącz tryb browserActive
        ime.setBrowserActive(false)
    }

    private fun createCircleButton(iconRes: Int): ImageButton {
        return ImageButton(root.context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                marginStart = 20
                marginEnd = 20
            }
            setImageResource(iconRes)
            background = root.context.getDrawable(R.drawable.circle_button_bg)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        }
    }

    // ============================================================
    // PANEL KONTROLNY
    // ============================================================

    private fun setupControls() {

        btnBack.setOnClickListener {
            webView?.takeIf { it.canGoBack() }?.goBack()
        }

        btnClose.setOnClickListener {
            hideBrowser()
            layer.visibility = View.VISIBLE
        }

        // przycisk: pokaż/ukryj klawiaturę NAD WebView + wymuś focus inputa
        btnKeyboard.setOnClickListener {
            if (keyboardView.visibility == View.VISIBLE) {
                keyboardView.visibility = View.GONE
            } else {
                keyboardView.visibility = View.VISIBLE
                keyboardView.bringToFront()
                fab.bringToFront()

                webView?.requestFocus()
                webView?.requestFocusFromTouch()

                webView?.evaluateJavascript(
                    """
                    (function() {
                        var el = document.activeElement;
                        if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA')) {
                            var first = document.querySelector('input, textarea');
                            if (first) first.focus();
                        }
                    })();
                    """.trimIndent(),
                    null
                )
            }
        }

        btnHideControls.setOnClickListener {
            browserControls.visibility = View.GONE
            btnEye.visibility = View.VISIBLE
            btnEye.bringToFront()
        }

        btnEye.setOnClickListener {
            browserControls.visibility = View.VISIBLE
            btnEye.visibility = View.GONE
        }

    }

    // ============================================================
    // WEBVIEW
    // ============================================================

    fun openBrowser(initialUrl: String) {
        layer.visibility = View.GONE

        if (webView == null) {
            webView = WebView(root.context).apply {

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
                        // 🔥 Pozwól WebView używać mikrofonu, głośnika, kamery, audio capture
                        request.grant(request.resources)
                    }
                }

                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(this, true)

                webViewClient = WebViewClient()

                addJavascriptInterface(
                    WebAppBridge { showKeyboardOverWebView() },
                    "CinnamonIME"
                )
            }

            browserWebContainer.addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        injectFocusDetectionJS()

        webView?.loadUrl(initialUrl)
        webView?.requestFocus()
        webView?.requestFocusFromTouch()

        browserContainer.visibility = View.VISIBLE
        browserControls.visibility = View.VISIBLE

        // ⭐ powiedz LatinIME, że przeglądarka jest aktywna (do hijacku klawiszy)
        ime.setBrowserActive(true)
    }

    private fun injectFocusDetectionJS() {
        webView?.evaluateJavascript(
            """
            (function() {
                document.addEventListener('focusin', function(e) {
                    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
                        CinnamonIME.onInputFocused();
                    }
                });
            })();
            """.trimIndent(),
            null
        )
    }

    private fun showKeyboardOverWebView() {
        keyboardView.visibility = View.VISIBLE
        keyboardView.bringToFront()
        fab.bringToFront()
    }

    private fun hideBrowser() {
        webView?.let {
            browserWebContainer.removeView(it)
            it.destroy()
        }
        webView = null
        browserContainer.visibility = View.GONE
        browserControls.visibility = View.GONE
        btnEye.visibility = View.GONE

        // ⭐ przeglądarka nieaktywna → LatinIME wraca do normalnego trybu
        ime.setBrowserActive(false)
    }

    // ============================================================
    // API do hijackowania klawiszy z LatinIME
    // ============================================================

    fun sendCharToWebView(ch: Char) {
        if (webView == null) return

        val safe = ch.toString().replace("\"", "\\\"")
        webView?.evaluateJavascript(
            """
            (function() {
                var el = document.activeElement;
                if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
                    el.value += "$safe";
                    var event = new Event('input', { bubbles: true });
                    el.dispatchEvent(event);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    fun sendBackspaceToWebView() {
        if (webView == null) return

        webView?.evaluateJavascript(
            """
            (function() {
                var el = document.activeElement;
                if (el && el.value) {
                    el.value = el.value.slice(0, -1);
                    var event = new Event('input', { bubbles: true });
                    el.dispatchEvent(event);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    fun sendEnterToWebView() {
        if (webView == null) return

        webView?.evaluateJavascript(
            """
            (function() {
                var el = document.activeElement;
                if (!el) return;
                if (el.form) el.form.submit();
            })();
            """.trimIndent(),
            null
        )
    }

    // ============================================================
    // JS → Kotlin bridge
    // ============================================================

    class WebAppBridge(val onFocus: () -> Unit) {
        @android.webkit.JavascriptInterface
        fun onInputFocused() {
            onFocus()
        }
    }
}