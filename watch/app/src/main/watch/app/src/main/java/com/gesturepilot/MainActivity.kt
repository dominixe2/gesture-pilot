
package com.gesturepilot

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.view.Gravity

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            text = """
                GesturePilot

                Aplikace je spuštěna.

                Další krok:
                Ovládání gesty
                a propojení s počítačem.
            """.trimIndent()

            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        setContentView(text)
    }
}
