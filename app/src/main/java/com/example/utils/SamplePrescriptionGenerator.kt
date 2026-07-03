package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

object SamplePrescriptionGenerator {

    fun generateSamplePrescriptionBitmap(index: Int): Bitmap {
        val width = 1200
        val height = 1500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background (off-white crumpled paper/clean Rx pad style)
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FBFBFD")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        // Draw decorative border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E3E3E9")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)
        
        // Hospital / Doctor header
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1A237E") // Deep Blue
            isAntiAlias = true
            textSize = 48f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("DR. TAIMUR ALAM, MD", 80f, 130f, headerPaint)
        
        val subHeaderPaint = Paint().apply {
            color = Color.DKGRAY
            isAntiAlias = true
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("Cardiologist & General Medicine Specialist", 80f, 175f, subHeaderPaint)
        canvas.drawText("Reg No: MC-998124 | Dhaka Medical College", 80f, 215f, subHeaderPaint)
        
        // Line separator
        val linePaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            strokeWidth = 4f
        }
        canvas.drawLine(80f, 250f, width - 80f, 250f, linePaint)
        
        // Draw Rx symbol
        val rxPaint = Paint().apply {
            color = Color.parseColor("#C2185B") // Medical Pink/Magenta
            isAntiAlias = true
            textSize = 100f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }
        canvas.drawText("Rx", 80f, 380f, rxPaint)
        
        // Draw medicines (large, hand-printed style using Serif)
        val textPaint = Paint().apply {
            color = Color.parseColor("#111111")
            isAntiAlias = true
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        
        val signPaint = Paint().apply {
            color = Color.parseColor("#1565C0") // Blue ink color for prescription directions
            isAntiAlias = true
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        
        if (index == 1) {
            val meds = listOf(
                "Cap. Cilnidip 25 mg" to "Take 1 capsule daily in the morning",
                "Cap. Olmesart 40 mg" to "Take 1 capsule daily after lunch",
                "Tab. Bisoprol 20 mg" to "Take 1 tablet once daily",
                "Tab. Diltiaz 10 mg" to "Take 1 tablet in morning and evening",
                "Tab. Diltiaz 20 mg" to "Take 1 tablet at night",
                "Tab. Hepar 10 mg" to "Take as directed by doctor"
            )
            
            var y = 470f
            for ((med, sig) in meds) {
                canvas.drawText(med, 120f, y, textPaint)
                y += 50f
                canvas.drawText("   Sig: $sig", 120f, y, signPaint)
                y += 90f
            }
        } else {
            val meds = listOf(
                "Cap. Dabigat 40 mg" to "Take 1 capsule daily",
                "Tab. Bisop 2.5 mg" to "Take 1/2 tablet daily before food",
                "Tab. Bisop 25 mg" to "Take 1 tablet once daily",
                "Tab. Diltia 5 mg" to "Take 1 tablet twice daily",
                "Tab. Valgac 100 mg" to "Take 1 tablet after dinner"
            )
            
            var y = 470f
            for ((med, sig) in meds) {
                canvas.drawText(med, 120f, y, textPaint)
                y += 50f
                canvas.drawText("   Sig: $sig", 120f, y, signPaint)
                y += 90f
            }
        }
        
        // Draw Footer / Sign at the bottom
        canvas.drawLine(80f, height - 200f, width - 80f, height - 200f, linePaint)
        canvas.drawText("Date: 02-Jul-2026", 80f, height - 130f, subHeaderPaint)
        canvas.drawText("Doctor's Signature: __________________", width - 600f, height - 130f, subHeaderPaint)
        
        return bitmap
    }

    fun generateSamplePrescriptionBytes(index: Int): ByteArray {
        val bitmap = generateSamplePrescriptionBitmap(index)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val bytes = stream.toByteArray()
        bitmap.recycle()
        return bytes
    }
}
