package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.StoreEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptUtils {

    fun generateReceiptText(
        store: StoreEntity,
        itemsSold: Map<Int, Int>,
        total: Double,
        cash: Double,
        remaining: Double,
        newTotalDebt: Double,
        nextDate: Long?
    ): String {
        val sb = StringBuilder()
        sb.append("🧾 *فاتورة تسليم كروت شبكة*\n")
        sb.append("🏪 المحل: ${store.name}\n")
        sb.append("----------------------------\n")

        for ((cat, qty) in itemsSold.toSortedMap()) {
            if (qty > 0) {
                sb.append("🔹 فئة $cat : $qty كرت = ${(cat * qty)} ريال\n")
            }
        }

        sb.append("----------------------------\n")
        sb.append("💵 إجمالي الفاتورة: ${total.toInt()} ريال\n")
        sb.append("✅ النقدية الواصلة: ${cash.toInt()} ريال\n")
        sb.append("⏳ الباقي آجل: ${remaining.toInt()} ريال\n")
        sb.append("🔴 إجمالي الدين الكلي: ${newTotalDebt.toInt()} ريال\n")

        if (nextDate != null) {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            sb.append("📅 موعد التوزيع القادم: ${sdf.format(Date(nextDate))}\n")
        }

        sb.append("\nشكراً لتعاملكم معنا!")
        return sb.toString()
    }

    fun sendViaWhatsApp(context: Context, phone: String, message: String) {
        // Clean phone number (strip spaces, +, -, etc.)
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val encodedMsg = Uri.encode(message)
        
        try {
            val url = if (cleanPhone.isNotEmpty()) {
                "https://wa.me/$cleanPhone?text=$encodedMsg"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share chooser if WhatsApp URL cannot be handled
            shareAsText(context, message)
        }
    }

    fun shareAsText(context: Context, message: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, "مشاركة الفاتورة عبر").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(shareIntent)
    }

    fun dialPhoneNumber(context: Context, phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(context, "لا يوجد رقم هاتف مسجل", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
        }
    }
}
