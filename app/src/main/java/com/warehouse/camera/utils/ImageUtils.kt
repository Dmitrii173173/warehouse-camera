package com.warehouse.camera.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for image processing operations
 */
object ImageUtils {

    /**
     * Adds a circular marker to the top-right corner of an image
     * @param bitmap The original bitmap to modify
     * @param circleColor The color of the circle (e.g., Color.GREEN)
     * @param text The text to display in the circle (e.g., "1")
     * @return The modified bitmap with the circle marker
     */
    fun addCircleMarker(bitmap: Bitmap, circleColor: Int, text: String): Bitmap {
        // Create a mutable copy of the bitmap
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        
        // Calculate circle size based on bitmap dimensions (5% of the width)
        val circleRadius = (bitmap.width * 0.05).toInt()
        
        // Position in top-right corner with some padding
        val circleX = bitmap.width - circleRadius - 40
        val circleY = circleRadius + 40
        
        // Draw the colored circle
        val circlePaint = Paint().apply {
            color = circleColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), circleRadius.toFloat(), circlePaint)
        
        // Add a border to the circle for better visibility
        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = (circleRadius * 0.1).toFloat()
            isAntiAlias = true
        }
        canvas.drawCircle(circleX.toFloat(), circleY.toFloat(), circleRadius.toFloat(), borderPaint)
        
        // Draw the text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = circleRadius * 1.2f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        // Measure text to center it vertically
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textX = circleX.toFloat()
        val textY = circleY.toFloat() + (textBounds.height() / 2)
        
        canvas.drawText(text, textX, textY, textPaint)
        
        return resultBitmap
    }
    
    /**
     * Alias for addCircleMarker for better code readability
     */
    fun addCircleToImage(bitmap: Bitmap, circleColor: Int, text: String): Bitmap {
        return addCircleMarker(bitmap, circleColor, text)
    }
    
    /**
     * Processes and saves both original and marked versions of the captured photo
     * @param originalFilePath Path to the original captured photo
     * @param circleColor Color of the marker circle to add
     * @param circleText Text to display in the circle ("1", "2", or "3")
     * @return Pair of file paths (original, marked) or null if processing failed
     */
    fun processAndSavePhotos(originalFilePath: String, circleColor: Int, circleText: String): Pair<String, String>? {
        try {
            val originalFile = File(originalFilePath)
            if (!originalFile.exists()) return null
            
            // Create path for the marked copy
            val fileName = originalFile.name
            val directory = originalFile.parentFile
            val markedFileName = fileName.replace(".jpg", "_marked.jpg")
            val markedFile = File(directory, markedFileName)
            val markedFilePath = markedFile.absolutePath
            
            // Load the image for processing
            val originalBitmap = BitmapFactory.decodeFile(originalFilePath)
            if (originalBitmap != null) {
                // Create marked version
                val markedBitmap = addCircleMarker(originalBitmap, circleColor, circleText)
                
                // Save the marked bitmap
                val success = saveBitmapToFile(markedBitmap, markedFile)
                
                // Clean up
                markedBitmap.recycle()
                originalBitmap.recycle()
                
                if (success) {
                    return Pair(originalFilePath, markedFilePath)
                }
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Saves a bitmap to a file
     * @param bitmap Bitmap to save
     * @param filePath Destination file path
     * @return true if successful, false otherwise
     */
    fun saveBitmapToFile(bitmap: Bitmap, filePath: String): Boolean {
        return try {
            FileOutputStream(filePath).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Saves a bitmap to a file
     * @param bitmap Bitmap to save
     * @param file Destination file
     * @return true if successful, false otherwise
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}