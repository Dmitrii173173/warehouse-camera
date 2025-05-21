package com.warehouse.camera.ui.reception

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import com.warehouse.camera.model.project.ProductReception
import com.warehouse.camera.model.project.ProductReceptionRepository
import com.warehouse.camera.ui.ManufacturerInfoActivity
import com.warehouse.camera.ui.base.BaseActivity

class ReceptionSelectionActivity : BaseActivity(), ReceptionAdapter.ReceptionClickListener {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var createButton: Button
    private lateinit var receptionRepository: ProductReceptionRepository
    private lateinit var adapter: ReceptionAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reception_selection)
        
        recyclerView = findViewById(R.id.recyclerView_receptions)
        emptyTextView = findViewById(R.id.textView_empty)
        createButton = findViewById(R.id.button_create_new)
        
        receptionRepository = ProductReceptionRepository(this)
        
        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ReceptionAdapter(this, emptyList(), this)
        recyclerView.adapter = adapter
        
        createButton.setOnClickListener {
            val intent = Intent(this, CreateReceptionActivity::class.java)
            startActivity(intent)
        }
        
        loadReceptions()
    }
    
    override fun onResume() {
        super.onResume()
        // Force repository to synchronize with file system before loading receptions
        receptionRepository.synchronizeWithFileSystem()
        loadReceptions()
    }
    
    private fun loadReceptions() {
        val receptions = receptionRepository.getAllReceptions()
        adapter.updateData(receptions)
        
        if (receptions.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    override fun onReceptionClicked(reception: ProductReception) {
        // Proceed to ManufacturerInfoActivity with pre-filled manufacturer code
        val intent = Intent(this, ManufacturerInfoActivity::class.java)
        intent.putExtra("reception", reception as Parcelable)
        startActivity(intent)
    }
}