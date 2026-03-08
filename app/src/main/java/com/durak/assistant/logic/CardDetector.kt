package com.durak.assistant.logic

import android.graphics.Bitmap
// import org.opencv.android.Utils
// import org.opencv.core.*
// import org.opencv.imgproc.Imgproc

class CardDetector {

    /**
     * Анализирует скриншот и возвращает список найденных карт.
     * 
     * ИНСТРУКЦИЯ ДЛЯ ПОЛЬЗОВАТЕЛЯ:
     * 1. Подключите OpenCV Android SDK в build.gradle.
     * 2. Подготовьте "шаблоны" для каждой карты (маленькие изображения ранга и масти).
     * 3. Используйте Imgproc.matchTemplate для поиска совпадений.
     */
    fun detectCards(bitmap: Bitmap): List<GameCard> {
        val foundCards = mutableListOf<GameCard>()
        
        // В реальной реализации здесь будет:
        // 1. Определение ROI (Region of Interest) - рука игрока и центр стола.
        // 2. Преобразование в оттенки серого и бинаризация.
        // 3. Сопоставление с шаблонами рангов (6, 7, 8... A) и мастей.
        
        // Пример логики (заглушка для теста):
        // Если мы видим много белых прямоугольников снизу - это карты в руке.
        
        return foundCards
    }
}

