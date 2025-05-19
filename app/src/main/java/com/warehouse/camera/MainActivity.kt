package com.warehouse.camera

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.warehouse.camera.ui.base.BaseActivity
import com.warehouse.camera.ui.FileStructureActivity
import com.warehouse.camera.ui.help.HelpActivity
import com.warehouse.camera.ui.reception.ReceptionSelectionActivity
import com.warehouse.camera.ui.gallery.GalleryBrowserActivity
import com.warehouse.camera.utils.LanguageUtils
import com.warehouse.camera.utils.PermissionUtils

class MainActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language
        LanguageUtils.applyLanguage(this)
        
        setContentView(R.layout.activity_main)
        
        // Request necessary permissions
        if (!PermissionUtils.hasCameraPermission(this) || !PermissionUtils.hasStoragePermission(this)) {
            PermissionUtils.requestAllPermissions(this)
        }
        
        // Start documenting button
        findViewById<Button>(R.id.btn_start_documenting).setOnClickListener {
            startActivity(Intent(this, ReceptionSelectionActivity::class.java))
        }
        
        // Gallery button
        findViewById<Button>(R.id.btn_gallery).setOnClickListener {
            startActivity(Intent(this, GalleryBrowserActivity::class.java))
        }
        
        // Setup language buttons
        findViewById<Button>(R.id.btn_english).setOnClickListener {
            LanguageUtils.setLanguage(this, LanguageUtils.Language.ENGLISH)
        }
        
        findViewById<Button>(R.id.btn_russian).setOnClickListener {
            LanguageUtils.setLanguage(this, LanguageUtils.Language.RUSSIAN)
        }
        
        findViewById<Button>(R.id.btn_chinese).setOnClickListener {
            LanguageUtils.setLanguage(this, LanguageUtils.Language.CHINESE)
        }
        
        // Highlight current language button
        highlightCurrentLanguage()
    }
    
    private fun highlightCurrentLanguage() {
        val currentLanguage = LanguageUtils.getSelectedLanguage(this)
        
        // Reset all buttons
        findViewById<Button>(R.id.btn_english).alpha = 0.6f
        findViewById<Button>(R.id.btn_russian).alpha = 0.6f
        findViewById<Button>(R.id.btn_chinese).alpha = 0.6f
        
        // Highlight selected language
        when (currentLanguage) {
            LanguageUtils.Language.ENGLISH -> findViewById<Button>(R.id.btn_english).alpha = 1.0f
            LanguageUtils.Language.RUSSIAN -> findViewById<Button>(R.id.btn_russian).alpha = 1.0f
            LanguageUtils.Language.CHINESE -> findViewById<Button>(R.id.btn_chinese).alpha = 1.0f
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_language -> {
                showLanguageDialog()
                true
            }
            R.id.menu_gallery -> {
                startActivity(Intent(this, GalleryBrowserActivity::class.java))
                true
            }
            R.id.menu_file_structure -> {
                startActivity(Intent(this, FileStructureActivity::class.java))
                true
            }
            R.id.menu_help -> {
                startActivity(Intent(this, HelpActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.language_english),
            getString(R.string.language_russian),
            getString(R.string.language_chinese)
        )
        
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_language)
            .setItems(languages) { _, which ->
                val language = when (which) {
                    0 -> LanguageUtils.Language.ENGLISH
                    1 -> LanguageUtils.Language.RUSSIAN
                    2 -> LanguageUtils.Language.CHINESE
                    else -> LanguageUtils.Language.ENGLISH
                }
                LanguageUtils.setLanguage(this, language)
                highlightCurrentLanguage()
            }
            .show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PermissionUtils.REQUEST_CAMERA_PERMISSION) {
            // Проверка, что все необходимые разрешения получены
            val allPermissionsGranted = PermissionUtils.hasCameraPermission(this) && 
                    PermissionUtils.hasStoragePermission(this)
            
            if (!allPermissionsGranted) {
                // Если разрешения не получены, показываем предупреждение
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.error_permissions_required)
                    .setPositiveButton(android.R.string.ok) { dialog, _ -> 
                        dialog.dismiss()
                        // Даем пользователю возможность повторно запросить разрешения
                        PermissionUtils.requestAllPermissions(this)
                    }
                    .show()
            }
        }
    }
}