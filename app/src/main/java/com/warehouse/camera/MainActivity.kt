package com.warehouse.camera

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.warehouse.camera.ui.base.BaseActivity
import com.warehouse.camera.ui.FileStructureActivity
import com.warehouse.camera.ui.help.HelpActivity
import com.warehouse.camera.ui.reception.ReceptionSelectionActivity
import com.warehouse.camera.ui.gallery.GalleryBrowserActivity
import com.warehouse.camera.utils.LanguageUtils
import com.warehouse.camera.utils.PermissionUtils
import com.warehouse.camera.utils.FileUtils

class MainActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language
        LanguageUtils.applyLanguage(this)
        
        setContentView(R.layout.activity_main)
        
        // Initialize FileUtils with diagnostic (IMPORTANT FIX FOR FILE SAVE ERRORS)
        FileUtils.initialize(this)
        
        // Request necessary permissions
        if (!PermissionUtils.hasCameraPermission(this) || !PermissionUtils.hasStoragePermission(this)) {
            PermissionUtils.requestAllPermissions(this)
        }
        
        // Start documenting button
        findViewById<Button>(R.id.btn_start_documenting).setOnClickListener {
            val intent = Intent(this, ReceptionSelectionActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        // File Structure button
        findViewById<Button>(R.id.btn_file_structure).setOnClickListener {
            val intent = Intent(this, FileStructureActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        // Help button
        findViewById<Button>(R.id.btn_help).setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        // Setup language buttons
        findViewById<Button>(R.id.btn_english).setOnClickListener {
            LanguageUtils.setLanguage(this, LanguageUtils.Language.ENGLISH)
            updateUIAfterLanguageChange()
        }
        
        findViewById<Button>(R.id.btn_russian).setOnClickListener {
            LanguageUtils.setLanguage(this, LanguageUtils.Language.RUSSIAN)
            updateUIAfterLanguageChange()
        }
        
        findViewById<Button>(R.id.btn_chinese).setOnClickListener {
            LanguageUtils.setLanguage(this, LanguageUtils.Language.CHINESE)
            updateUIAfterLanguageChange()
        }
        
        // Update all UI elements with proper labels
        updateUIAfterLanguageChange()
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
    
    /**
     * Update UI after language change
     */
    private fun updateUIAfterLanguageChange() {
        // Update all text labels
        findViewById<Button>(R.id.btn_start_documenting).text = getString(R.string.menu_start)
        findViewById<Button>(R.id.btn_file_structure).text = getString(R.string.file_structure)
        findViewById<Button>(R.id.btn_help).text = getString(R.string.menu_help)
        findViewById<TextView>(R.id.textView_language).text = getString(R.string.menu_language)
        
        // Update language buttons
        findViewById<Button>(R.id.btn_english).text = getString(R.string.language_english)
        findViewById<Button>(R.id.btn_russian).text = getString(R.string.language_russian)
        findViewById<Button>(R.id.btn_chinese).text = getString(R.string.language_chinese)
        
        // Highlight current language
        highlightCurrentLanguage()
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
            R.id.menu_file_structure -> {
                val intent = Intent(this, FileStructureActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                true
            }
            R.id.menu_help -> {
                val intent = Intent(this, HelpActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                true
            }
            R.id.menu_storage_info -> {
                showStorageInfoDialog()
                true
            }
            R.id.menu_diagnostic -> {
                // Запуск диагностики файловой системы (для отладки)
                FileUtils.runDiagnostics(this)
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Диагностика завершена")
                    .setMessage("Проверьте логи Android (тег: FileUtils, FileSystemDiagnostic) для подробной информации")
                    .setPositiveButton("ОК", null)
                    .show()
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
                updateUIAfterLanguageChange()
            }
            .show()
    }
    
    private fun showStorageInfoDialog() {
        val storageInfo = FileUtils.getStorageLocationInfo(this)
        
        AlertDialog.Builder(this)
            .setTitle("Местоположение файлов")
            .setMessage(storageInfo)
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Открыть файловый менеджер") { _, _ ->
                val intent = Intent(this, FileStructureActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}