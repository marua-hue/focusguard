package dev.focusguard

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val apps = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }

        status = TextView(this).apply { textSize = 16f }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Ouvrir les réglages d'accessibilité"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(4))
        }
        header.addView(TextView(this).apply { text = "Appli"; textSize = 13f },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(cell(TextView(this).apply { text = "Pause ${Prefs.WAIT_SECONDS} s"; textSize = 13f; gravity = Gravity.CENTER }))
        header.addView(cell(TextView(this).apply { text = "Scroll bloqué"; textSize = 13f; gravity = Gravity.CENTER }))
        root.addView(header)

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for ((pkg, label) in apps) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(2))
            }
            row.addView(TextView(this).apply { text = label; textSize = 16f },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(cell(CheckBox(this).apply {
                isChecked = pkg in Prefs.pauseSet(this@MainActivity)
                setOnCheckedChangeListener { _, on -> Prefs.setPause(this@MainActivity, pkg, on) }
            }))
            row.addView(cell(CheckBox(this).apply {
                isChecked = pkg in Prefs.noScrollSet(this@MainActivity)
                setOnCheckedChangeListener { _, on -> Prefs.setNoScroll(this@MainActivity, pkg, on) }
            }))
            list.addView(row)
        }
        root.addView(ScrollView(this).apply { addView(list) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (serviceEnabled()) "✅ Service actif" else "⚠️ Service désactivé : active-le dans Accessibilité > Applications installées"
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val cn = ComponentName(this, GuardService::class.java)
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun cell(v: View) = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        addView(v)
        layoutParams = LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
