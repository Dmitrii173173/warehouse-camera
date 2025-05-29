package com.warehouse.camera.ui.scanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.warehouse.camera.R
import com.warehouse.camera.utils.ScannerUtils
import com.warehouse.camera.utils.TSDDevice
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var statusText: TextView
    private lateinit var switchScannerButton: Button
    
    // ТСД сканер
    private var tsdDevice: TSDDevice? = null
    private var usingTSDScanner = false
    private var tsdScannerReceiver: BroadcastReceiver? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)
        
        viewFinder = findViewById(R.id.viewFinder)
        statusText = findViewById(R.id.status_text)
        switchScannerButton = findViewById(R.id.switch_scanner_button)
        
        // Проверяем, является ли устройство ТСД
        tsdDevice = ScannerUtils.getTSDDeviceConfig()
        
        if (tsdDevice != null) {
            // Устройство поддерживает встроенный сканер
            statusText.text = "Обнаружен встроенный сканер ${tsdDevice!!.name}"
            switchScannerButton.visibility = View.VISIBLE
            switchScannerButton.text = "Использовать встроенный сканер"
            switchScannerButton.setOnClickListener {
                toggleScannerMode()
            }
            
            // По умолчанию используем встроенный сканер если он доступен
            initializeTSDScanner()
        } else {
            // Обычное устройство - используем камеру
            statusText.text = "Используется камера для сканирования"
            switchScannerButton.visibility = View.GONE
            initializeCameraScanner()
        }
        
        barcodeScanner = BarcodeScanning.getClient()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun toggleScannerMode() {
        if (usingTSDScanner) {
            // Переключаемся на камеру
            disableTSDScanner()
            initializeCameraScanner()
            switchScannerButton.text = "Использовать встроенный сканер"
            statusText.text = "Режим: Камера"
        } else {
            // Переключаемся на ТСД сканер
            initializeTSDScanner()
            switchScannerButton.text = "Использовать камеру"
            statusText.text = "Режим: Встроенный сканер ${tsdDevice?.name}"
        }
    }
    
    private fun initializeTSDScanner() {
        if (tsdDevice == null) return
        
        usingTSDScanner = true
        viewFinder.visibility = View.GONE
        
        // Создаем и регистрируем receiver для получения данных сканера
        tsdScannerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    val barcode = ScannerUtils.extractBarcodeFromIntent(intent, tsdDevice!!)
                    if (!barcode.isNullOrEmpty()) {
                        Log.d(TAG, "TSD Scanner detected barcode: $barcode")
                        returnScanResult(barcode)
                    }
                }
            }
        }
        
        val filter = ScannerUtils.createScannerIntentFilter(tsdDevice!!)
        registerReceiver(tsdScannerReceiver, filter)
        
        // Включаем ТСД сканер
        ScannerUtils.enableTSDScanner(this, tsdDevice!!)
        
        statusText.text = "Нажмите кнопку сканирования на устройстве или используйте триггер"
    }
    
    private fun disableTSDScanner() {
        if (tsdDevice != null && usingTSDScanner) {
            ScannerUtils.disableTSDScanner(this, tsdDevice!!)
        }
        
        tsdScannerReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering TSD scanner receiver", e)
            }
            tsdScannerReceiver = null
        }
        
        usingTSDScanner = false
        viewFinder.visibility = View.VISIBLE
    }
    
    private fun initializeCameraScanner() {
        usingTSDScanner = false
        viewFinder.visibility = View.VISIBLE
        
        // Проверка разрешений
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun returnScanResult(barcode: String) {
        // Возвращаем результат сканирования и закрываем активность
        val resultIntent = Intent()
        resultIntent.putExtra("scanned_barcode", barcode)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            
            // Настройка анализатора изображений
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes: List<Barcode> ->
                        if (barcodes.isNotEmpty()) {
                            // Берем первый найденный штрих-код
                            val barcode = barcodes[0]
                            barcode.rawValue?.let { barcodeValue ->
                                returnScanResult(barcodeValue)
                            }
                        }
                    })
                }
            
            // Используем заднюю камеру по умолчанию
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Освобождаем все связанные ранее use cases
                cameraProvider.unbindAll()
                
                // Привязываем use cases к камере
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
                
            } catch (exc: Exception) {
                Log.e(TAG, "Не удалось привязать use cases", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Разрешения на использование камеры не предоставлены",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        disableTSDScanner()
    }
    
    override fun onPause() {
        super.onPause()
        // Отключаем ТСД сканер при сворачивании приложения
        if (usingTSDScanner && tsdDevice != null) {
            ScannerUtils.disableTSDScanner(this, tsdDevice!!)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Включаем ТСД сканер при возвращении в приложение
        if (usingTSDScanner && tsdDevice != null) {
            ScannerUtils.enableTSDScanner(this, tsdDevice!!)
        }
    }
    
    private class BarcodeAnalyzer(private val barcodeListener: (List<Barcode>) -> Unit) : ImageAnalysis.Analyzer {
        
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            barcodeListener(barcodes)
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Ошибка распознавания баркода", it)
                    }
                    .addOnCompleteListener { 
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
        
        companion object {
            private const val TAG = "BarcodeAnalyzer"
        }
    }
    
    companion object {
        private const val TAG = "BarcodeScannerActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}