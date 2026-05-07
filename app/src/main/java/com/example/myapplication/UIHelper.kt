package com.example.myapplication

import android.view.View
import android.widget.TextView
import java.util.Locale

object UIHelper {

    private var currentBatteryPercent: Int = -1

    fun syncClockWithBattery(view: View, prefs: android.content.SharedPreferences) {
        // No-op for current clock styles
    }

    fun updateBatteryUI(pages: List<View>, percent: Int) {
        currentBatteryPercent = percent
        pages.forEach { page ->
            val batteryText = page.findViewById<TextView>(R.id.batteryText)
            batteryText?.let {
                val currentText = it.text.toString()
                if (currentText.contains("--%")) {
                    it.text = currentText.replace("--%", "$percent%")
                } else if (currentText.contains("%")) {
                    it.text = currentText.replace(Regex("\\d+%"), "$percent%")
                } else {
                    it.text = "$percent%"
                }
            }

            updateInspirationalQuote(page)
        }
    }

    private val quotes = listOf(
        "Believe you can and you're halfway there.",
        "Your only limit is your mind.",
        "Dream big. Work hard. Stay focused.",
        "Stay positive, work hard, make it happen.",
        "Don't stop when you're tired. Stop when you're done.",
        "Focus on being productive instead of busy.",
        "Success is not final; failure is not fatal.",
        "Make each day your masterpiece.",
        "Everything you've ever wanted is on the other side of fear.",
        "The best way to predict the future is to create it."
    )

    fun updateInspirationalQuote(page: View) {
        val prefs = page.context.getSharedPreferences("launcher_settings", android.content.Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("quotes_enabled", true)
        val visibility = if (isEnabled) View.VISIBLE else View.GONE

        val quote = quotes.random()

        page.findViewById<TextView>(R.id.quoteText)?.let {
            it.visibility = visibility
            it.text = quote
        }

        page.findViewById<TextView>(R.id.quoteHeader)?.let {
            it.visibility = visibility
            it.text = "STAY\nFOCUSED"
        }
    }
}
