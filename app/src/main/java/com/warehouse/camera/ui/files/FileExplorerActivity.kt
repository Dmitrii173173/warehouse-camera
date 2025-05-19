package com.warehouse.camera.ui.files

import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import com.warehouse.camera.utils.FileUtils
import com.warehouse.camera.utils.LanguageUtils
import java.io.File

class FileExplorerActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var pathTextView: TextView
    private lateinit var emptyTextView: TextView
    private lateinit var fileAdapter: FileAdapter
    
    private var currentPath: File? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language
        LanguageUtils.applyLanguage(this)
        
        setContentView(R.layout.activity_file_explorer)
        
        // Setup toolbar with back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.file_explorer_title)
        
        pathTextView = findViewById(R.id.text_current_path)
        recyclerView = findViewById(R.id.recycler_files)
        emptyTextView = findViewById(R.id.text_empty_directory)
        
        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        fileAdapter = FileAdapter(
            onItemClick = { file ->
                if (file.isDirectory) {
                    navigateToDirectory(file)
                }
            },
            onDeleteClick = { file ->
                showDeleteConfirmationDialog(file)
            }
        )
        recyclerView.adapter = fileAdapter
        
        // Initialize with base directory
        val baseDir = FileUtils.getBaseDirectory(this)
        navigateToDirectory(baseDir)
    }
    
    private fun navigateToDirectory(directory: File) {
        currentPath = directory
        pathTextView.text = directory.absolutePath
        
        // List all files in the directory
        val files = if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        } else {
            emptyList()
        }
        
        // Update the adapter
        fileAdapter.submitList(files)
        
        // Show empty view if needed
        emptyTextView.visibility = if (files.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Navigate to parent directory or finish if at root
                val parent = currentPath?.parentFile
                if (parent != null && 
                    parent.absolutePath.startsWith(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath)) {
                    navigateToDirectory(parent)
                } else {
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        // Navigate to parent directory or finish if at root
        val parent = currentPath?.parentFile
        if (parent != null && 
            parent.absolutePath.startsWith(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath)) {
            navigateToDirectory(parent)
        } else {
            super.onBackPressed()
        }
    }
    
    /**
     * Показать диалог подтверждения удаления файла или папки
     */
    private fun showDeleteConfirmationDialog(file: File) {
        val message = if (file.isDirectory) {
            getString(R.string.confirm_delete_folder, file.name)
        } else {
            getString(R.string.confirm_delete_file, file.name)
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(message)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
                // Use our FileUtils method to delete files or directories
                val success = FileUtils.deleteFileOrDirectory(file)
                
                if (success) {
                    Toast.makeText(this, R.string.delete_success, Toast.LENGTH_SHORT).show()
                    // Обновить список файлов в текущей папке
                    currentPath?.let { navigateToDirectory(it) }
                } else {
                    Toast.makeText(this, R.string.delete_error, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}