package com.warehouse.camera.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Build
import android.util.Log
import android.widget.Toast

/**
 * Утилита для работы с встроенными сканерами ТСД (терминалов сбора данных)
 * Поддерживает Ranger2K и другие ТСД устройства
 */
object ScannerUtils {
    private const val TAG = "ScannerUtils"
    
    // Список известных ТСД устройств с их характеристиками
    private val KNOWN_TSD_DEVICES = mapOf(
        "ranger2k" to TSDDevice(
            name = "Ranger2K",
            model = "ranger2k",
            scannerAction = "scan.rcv.message",
            barcodeExtra = "barocode",
            triggerAction = "com.android.server.scannerservice.broadcast",
            triggerExtraKey = "scannerService"
        ),
        "idata" to TSDDevice(
            name = "iData",
            model = "idata",
            scannerAction = "android.intent.action.DECODE_DATA",
            barcodeExtra = "barcode_string",
            triggerAction = "android.intent.action.SCANNER_ON",
            triggerExtraKey = "scanner"
        ),
        "chainway" to TSDDevice(
            name = "Chainway",
            model = "chainway",
            scannerAction = "com.rscja.deviceapi.BARCODE_SCAN",
            barcodeExtra = "barocode",
            triggerAction = "com.rscja.deviceapi.scanner.power",
            triggerExtraKey = "power"
        ),
        "urovo" to TSDDevice(
            name = "Urovo",
            model = "urovo",
            scannerAction = "urovo.rcv.message",
            barcodeExtra = "barocode",
            triggerAction = "com.android.urovo.scanner.SCAN",
            triggerExtraKey = "scan"
        )
    )
    
    /**
     * Проверяет, является ли текущее устройство ТСД с встроенным сканером
     */
    fun isTSDDevice(): Boolean {
        val deviceModel = Build.MODEL.lowercase()
        val deviceManufacturer = Build.MANUFACTURER.lowercase()
        val deviceProduct = Build.PRODUCT.lowercase()
        
        Log.d(TAG, "Device info - Model: $deviceModel, Manufacturer: $deviceManufacturer, Product: $deviceProduct")
        
        // Проверяем по модели устройства
        for (tsdDevice in KNOWN_TSD_DEVICES.values) {
            if (deviceModel.contains(tsdDevice.model) || 
                deviceProduct.contains(tsdDevice.model) ||
                deviceManufacturer.contains(tsdDevice.model)) {
                Log.d(TAG, "Detected TSD device: ${tsdDevice.name}")
                return true
            }
        }
        
        // Дополнительные проверки для распространенных ТСД
        val tsdKeywords = listOf("ranger", "pda", "handheld", "terminal", "scanner", "idata", "chainway", "urovo", "newland")
        for (keyword in tsdKeywords) {
            if (deviceModel.contains(keyword) || deviceProduct.contains(keyword)) {
                Log.d(TAG, "Detected TSD device by keyword: $keyword")
                return true
            }
        }
        
        return false
    }
    
    /**
     * Получает конфигурацию ТСД устройства
     */
    fun getTSDDeviceConfig(): TSDDevice? {
        if (!isTSDDevice()) return null
        
        val deviceModel = Build.MODEL.lowercase()
        val deviceProduct = Build.PRODUCT.lowercase()
        val deviceManufacturer = Build.MANUFACTURER.lowercase()
        
        // Находим подходящую конфигурацию
        for (tsdDevice in KNOWN_TSD_DEVICES.values) {
            if (deviceModel.contains(tsdDevice.model) || 
                deviceProduct.contains(tsdDevice.model) ||
                deviceManufacturer.contains(tsdDevice.model)) {
                return tsdDevice
            }
        }
        
        // Возвращаем конфигурацию по умолчанию для Ranger2K если не найдено
        return KNOWN_TSD_DEVICES["ranger2k"]
    }
    
    /**
     * Создает IntentFilter для прослушивания сканера ТСД
     */
    fun createScannerIntentFilter(tsdDevice: TSDDevice): IntentFilter {
        return IntentFilter(tsdDevice.scannerAction)
    }
    
    /**
     * Извлекает штрихкод из Intent сканера ТСД
     */
    fun extractBarcodeFromIntent(intent: Intent, tsdDevice: TSDDevice): String? {
        return try {
            // Пробуем разные возможные ключи для штрихкода
            val possibleKeys = listOf(
                tsdDevice.barcodeExtra,
                "barocode", // Часто используется в ТСД
                "barcode_string",
                "scan_result",
                "decode_result",
                "barcode"
            )
            
            for (key in possibleKeys) {
                val barcode = intent.getStringExtra(key)
                if (!barcode.isNullOrEmpty()) {
                    Log.d(TAG, "Found barcode '$barcode' with key '$key'")
                    return barcode
                }
            }
            
            // Также проверяем данные в разных форматах
            val data = intent.getStringExtra("data")
            if (!data.isNullOrEmpty()) {
                Log.d(TAG, "Found barcode in data field: $data")
                return data
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting barcode from intent", e)
            null
        }
    }
    
    /**
     * Включает встроенный сканер ТСД
     */
    fun enableTSDScanner(context: Context, tsdDevice: TSDDevice) {
        try {
            val intent = Intent(tsdDevice.triggerAction).apply {
                putExtra(tsdDevice.triggerExtraKey, "on")
                putExtra("enable", true)
                putExtra("power", true)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "Enabled TSD scanner for ${tsdDevice.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling TSD scanner", e)
        }
    }
    
    /**
     * Отключает встроенный сканер ТСД
     */
    fun disableTSDScanner(context: Context, tsdDevice: TSDDevice) {
        try {
            val intent = Intent(tsdDevice.triggerAction).apply {
                putExtra(tsdDevice.triggerExtraKey, "off")
                putExtra("enable", false)
                putExtra("power", false)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "Disabled TSD scanner for ${tsdDevice.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling TSD scanner", e)
        }
    }
    
    /**
     * Показывает информацию об устройстве для отладки
     */
    fun showDeviceInfo(context: Context) {
        val info = """
            Модель: ${Build.MODEL}
            Производитель: ${Build.MANUFACTURER}
            Продукт: ${Build.PRODUCT}
            Устройство: ${Build.DEVICE}
            ТСД: ${if (isTSDDevice()) "Да" else "Нет"}
        """.trimIndent()
        
        Toast.makeText(context, info, Toast.LENGTH_LONG).show()
        Log.d(TAG, "Device info: $info")
    }
}

/**
 * Конфигурация ТСД устройства
 */
data class TSDDevice(
    val name: String,
    val model: String,
    val scannerAction: String,
    val barcodeExtra: String,
    val triggerAction: String,
    val triggerExtraKey: String
)