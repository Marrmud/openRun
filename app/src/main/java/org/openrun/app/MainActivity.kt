package org.openrun.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Placeholder until T3 brings the real screens. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "openRun"; textSize = 24f; setPadding(48, 96, 48, 48) })
    }
}
