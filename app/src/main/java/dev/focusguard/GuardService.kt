package dev.focusguard

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class GuardService : AccessibilityService() {

    companion object {
        /** Après l'ouverture d'une appli, les scrolls pendant ce délai sont ignorés (mise en page initiale). */
        private const val SCROLL_GRACE_MS = 1500L

        /** Fenêtres système qui ne doivent jamais être considérées comme "une autre appli". */
        private val ALWAYS_IGNORED = setOf(
            "com.android.systemui",
            "android",
            "com.samsung.android.app.cocktailbarservice" // Edge panel Samsung
        )
    }

    private val handler = Handler(Looper.getMainLooper())

    private var overlay: View? = null
    private var overlayPkg: String? = null
    private var countdown: Runnable? = null

    private var allowedPkg: String? = null   // appli que j'ai choisi d'ouvrir après l'attente
    private var foregroundPkg: String? = null
    private var foregroundSince = 0L
    private var lastKick = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindowChanged(pkg)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> onScrolled(pkg)
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- fenêtre au premier plan

    private fun onWindowChanged(pkg: String) {
        if (isIgnored(pkg)) return

        if (pkg != foregroundPkg) {
            foregroundPkg = pkg
            foregroundSince = SystemClock.uptimeMillis()
        }
        // J'ai quitté l'appli autorisée : la prochaine ouverture redemandera l'attente.
        if (allowedPkg != null && allowedPkg != pkg) allowedPkg = null

        if (pkg in Prefs.pauseSet(this) && pkg != allowedPkg) showOverlay(pkg) else hideOverlay()
    }

    private fun isIgnored(pkg: String): Boolean {
        if (pkg == packageName || pkg in ALWAYS_IGNORED) return true
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == pkg } // clavier
    }

    // ---------------------------------------------------------------- scroll

    private fun onScrolled(pkg: String) {
        if (overlay != null || pkg != foregroundPkg) return
        if (pkg !in Prefs.noScrollSet(this)) return

        val now = SystemClock.uptimeMillis()
        if (now - foregroundSince < SCROLL_GRACE_MS || now - lastKick < 1000) return
        lastKick = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, "Scroll bloqué", Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------- écran d'attente

    private fun showOverlay(pkg: String) {
        if (overlay != null && overlayPkg == pkg) return
        hideOverlay()

        val label = labelOf(pkg)

        val title = TextView(this).apply {
            text = "Tu ouvres $label"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "Est-ce vraiment nécessaire maintenant ?"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(32))
        }
        val back = Button(this).apply {
            text = "Retour"
            setOnClickListener {
                hideOverlay()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
        val go = Button(this).apply {
            isEnabled = false
            text = "Continuer (${Prefs.WAIT_SECONDS})"
            setOnClickListener {
                allowedPkg = pkg
                hideOverlay()
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(245, 16, 16, 24))
            setPadding(dp(32), dp(32), dp(32), dp(32))
            isClickable = true // absorbe les touches : l'appli derrière reste inaccessible
            addView(title)
            addView(sub)
            addView(back)
            addView(go)
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(root, lp)
        } catch (e: Exception) {
            return
        }
        overlay = root
        overlayPkg = pkg

        var left = Prefs.WAIT_SECONDS
        val tick = object : Runnable {
            override fun run() {
                left--
                if (left <= 0) {
                    go.isEnabled = true
                    go.text = "Continuer"
                } else {
                    go.text = "Continuer ($left)"
                    handler.postDelayed(this, 1000)
                }
            }
        }
        countdown = tick
        handler.postDelayed(tick, 1000)
    }

    private fun hideOverlay() {
        countdown?.let { handler.removeCallbacks(it) }
        countdown = null
        overlay?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (_: Exception) {
            }
        }
        overlay = null
        overlayPkg = null
    }

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
