package com.warehouse.camera.utils

import android.content.Context
import com.warehouse.camera.R

/**
 * Утилитарный класс для перевода основных терминов между языками
 */
object TranslationUtils {
    
    /**
     * Словарь переводов для терминов "Reason" (Причина)
     */
    private val REASON_TRANSLATIONS = mapOf(
        // Русские термины
        "Повреждение" to mapOf("en" to "Damage", "zh" to "损坏"),
        "Заводской диалект" to mapOf("en" to "Factory dialect", "zh" to "工厂方言"),
        "Неверное прикрепление" to mapOf("en" to "Invalid attachment", "zh" to "无效附件"),
        "Другое" to mapOf("en" to "Others", "zh" to "其他"),
        "Возврат поставщику" to mapOf("en" to "Returned to supplier", "zh" to "退回供应商"),
        "Короткая доставка" to mapOf("en" to "Short delivery", "zh" to "短交货"),
        "Подвеска" to mapOf("en" to "Undercarriage", "zh" to "底盘"),
        
        // Английские термины
        "Damage" to mapOf("ru" to "Повреждение", "zh" to "损坏"),
        "Factory dialect" to mapOf("ru" to "Заводской диалект", "zh" to "工厂方言"),
        "Invalid attachment" to mapOf("ru" to "Неверное прикрепление", "zh" to "无效附件"),
        "Others" to mapOf("ru" to "Другое", "zh" to "其他"),
        "Returned to supplier" to mapOf("ru" to "Возврат поставщику", "zh" to "退回供应商"),
        "Short delivery" to mapOf("ru" to "Короткая доставка", "zh" to "短交货"),
        "Undercarriage" to mapOf("ru" to "Подвеска", "zh" to "底盘"),
        
        // Китайские термины
        "损坏" to mapOf("ru" to "Повреждение", "en" to "Damage"),
        "工厂方言" to mapOf("ru" to "Заводской диалект", "en" to "Factory dialect"),
        "无效附件" to mapOf("ru" to "Неверное прикрепление", "en" to "Invalid attachment"),
        "其他" to mapOf("ru" to "Другое", "en" to "Others"),
        "退回供应商" to mapOf("ru" to "Возврат поставщику", "en" to "Returned to supplier"),
        "短交货" to mapOf("ru" to "Короткая доставка", "en" to "Short delivery"),
        "底盘" to mapOf("ru" to "Подвеска", "en" to "Undercarriage")
    )
    
    /**
     * Словарь переводов для терминов "Template" (Шаблон)
     */
    private val TEMPLATE_TRANSLATIONS = mapOf(
        // Русские термины
        "Сломано" to mapOf("en" to "Broken", "zh" to "破损"),
        "Повреждение при транспортировке" to mapOf("en" to "Damage during transportation", "zh" to "运输过程中损坏"),
        "Повреждение при разгрузке" to mapOf("en" to "Damage during unloading", "zh" to "卸货过程中损坏"),
        "Деформация" to mapOf("en" to "Deformation", "zh" to "变形"),
        "Вмятина" to mapOf("en" to "Dent", "zh" to "凹痕"),
        "Механическое повреждение" to mapOf("en" to "Mechanical damage", "zh" to "机械损伤"),
        "Другое" to mapOf("en" to "Others", "zh" to "其他"),
        "Ошибка упаковки" to mapOf("en" to "Packing error", "zh" to "包装错误"),
        "Возврат как пересортица" to mapOf("en" to "Returned as re-sorted", "zh" to "返回为重新分类"),
        "Возврат по браку" to mapOf("en" to "Returned due to marriage", "zh" to "因婚姻而退货"),
        "Царапина" to mapOf("en" to "Scratch", "zh" to "划痕"),
        "Потертости" to mapOf("en" to "Scuffs", "zh" to "擦伤"),
        "Скол" to mapOf("en" to "Chip", "zh" to "碎片"),
        
        // Английские термины
        "Broken" to mapOf("ru" to "Сломано", "zh" to "破损"),
        "Damage during transportation" to mapOf("ru" to "Повреждение при транспортировке", "zh" to "运输过程中损坏"),
        "Damage during unloading" to mapOf("ru" to "Повреждение при разгрузке", "zh" to "卸货过程中损坏"),
        "Deformation" to mapOf("ru" to "Деформация", "zh" to "变形"),
        "Dent" to mapOf("ru" to "Вмятина", "zh" to "凹痕"),
        "Mechanical damage" to mapOf("ru" to "Механическое повреждение", "zh" to "机械损伤"),
        "Others" to mapOf("ru" to "Другое", "zh" to "其他"),
        "Packing error" to mapOf("ru" to "Ошибка упаковки", "zh" to "包装错误"),
        "Returned as re-sorted" to mapOf("ru" to "Возврат как пересортица", "zh" to "返回为重新分类"),
        "Returned due to marriage" to mapOf("ru" to "Возврат по браку", "zh" to "因婚姻而退货"),
        "Scratch" to mapOf("ru" to "Царапина", "zh" to "划痕"),
        "Scuffs" to mapOf("ru" to "Потертости", "zh" to "擦伤"),
        "Chip" to mapOf("ru" to "Скол", "zh" to "碎片"),
        
        // Китайские термины
        "破损" to mapOf("ru" to "Сломано", "en" to "Broken"),
        "运输过程中损坏" to mapOf("ru" to "Повреждение при транспортировке", "en" to "Damage during transportation"),
        "卸货过程中损坏" to mapOf("ru" to "Повреждение при разгрузке", "en" to "Damage during unloading"),
        "变形" to mapOf("ru" to "Деформация", "en" to "Deformation"),
        "凹痕" to mapOf("ru" to "Вмятина", "en" to "Dent"),
        "机械损伤" to mapOf("ru" to "Механическое повреждение", "en" to "Mechanical damage"),
        "其他" to mapOf("ru" to "Другое", "en" to "Others"),
        "包装错误" to mapOf("ru" to "Ошибка упаковки", "en" to "Packing error"),
        "返回为重新分类" to mapOf("ru" to "Возврат как пересортица", "en" to "Returned as re-sorted"),
        "因婚姻而退货" to mapOf("ru" to "Возврат по браку", "en" to "Returned due to marriage"),
        "划痕" to mapOf("ru" to "Царапина", "en" to "Scratch"),
        "擦伤" to mapOf("ru" to "Потертости", "en" to "Scuffs"),
        "碎片" to mapOf("ru" to "Скол", "en" to "Chip")
    )
    
    /**
     * Определить язык текста по первым символам
     */
    fun detectLanguage(text: String): String {
        if (text.isEmpty()) return "en"
        
        // Проверяем первый символ текста
        val firstChar = text.codePointAt(0)
        
        // Кириллица (русский)
        if (firstChar in 0x0400..0x04FF) {
            return "ru"
        }
        
        // Китайские иероглифы
        if (firstChar in 0x4E00..0x9FFF) {
            return "zh"
        }
        
        // По умолчанию английский
        return "en"
    }
    
    /**
     * Перевести термин "Reason" (Причина) на заданный язык
     */
    fun translateReason(term: String, targetLang: String): String {
        return REASON_TRANSLATIONS[term]?.get(targetLang) ?: term
    }
    
    /**
     * Перевести термин "Template" (Шаблон) на заданный язык
     */
    fun translateTemplate(term: String, targetLang: String): String {
        return TEMPLATE_TRANSLATIONS[term]?.get(targetLang) ?: term
    }
    
    /**
     * Получить доступные языки
     */
    fun getAvailableLanguages(): List<String> {
        return listOf("ru", "en", "zh")
    }
    
    /**
     * Получить название языка для отображения
     */
    fun getLanguageDisplayName(context: Context, langCode: String): String {
        return when (langCode) {
            "ru" -> context.getString(R.string.language_russian)
            "zh" -> context.getString(R.string.language_chinese)
            else -> context.getString(R.string.language_english)
        }
    }
}