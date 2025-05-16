package com.warehouse.camera.ui.help

import android.os.Bundle
import android.widget.Button
import com.warehouse.camera.R
import com.warehouse.camera.ui.base.BaseActivity

class HelpActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        
        findViewById<Button>(R.id.button_close).setOnClickListener {
            finish()
        }
    }
}