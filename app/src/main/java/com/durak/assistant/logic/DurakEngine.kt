package com.durak.assistant.logic

import android.content.Context

class DurakEngine(context: Context) {
    private val prefs = context.getSharedPreferences("durak_prefs", Context.MODE_PRIVATE)
    
    private val cardsInGraveyard = mutableSetOf<GameCard>()
    private val myCards = mutableSetOf<GameCard>()
    private var trumpSuit: String? = null
    
    // Новые параметры из настроек
    private val deckSize: Int get() = prefs.getInt("deck_size", 36)
    private val playersCount: Int get() = prefs.getInt("players_count", 2)

    /**
     * Обновляет состояние игры на основе распознанных карт.
     * @param onTable Карты, лежащие в центре стола.
     * @param inHand Карты в руке игрока.
     * @param trump Текущая масть козыря.
     * @param isBitoEnabled Флаг, нажата ли кнопка "Бито" (карты уходят в сброс).
     */
    fun updateGameState(onTable: List<GameCard>, inHand: List<GameCard>, trump: String, isBitoEnabled: Boolean = false) {
        if (trump.isNotEmpty()) trumpSuit = trump
        myCards.clear()
        myCards.addAll(inHand)
        
        if (isBitoEnabled) {
            // Если раунд завершен (нажато Бито), добавляем все карты со стола в сброс
            cardsInGraveyard.addAll(onTable)
        }
    }

    fun getRemainingCardsCount(tableCardsCount: Int): Int {
        val remaining = deckSize - cardsInGraveyard.size - myCards.size - tableCardsCount
        return if (remaining < 0) 0 else remaining
    }

    fun getTrumpSuit(): String = trumpSuit ?: "Неизвестно"

    fun generatePrompt(tableCards: List<GameCard>): String {
        val cardsRemaining = deckSize - cardsInGraveyard.size - myCards.size - (tableCards.size)
        
        return """
            Ты эксперт-помощник в карточной игре "Дурак".
            
            ПАРАМЕТРЫ ИГРЫ:
            - Размер колоды: $deckSize карт.
            - Игроков за столом: $playersCount.
            - Козырь: $trumpSuit.
            
            ТЕКУЩАЯ СИТУАЦИЯ:
            - Мои карты: ${myCards.joinToString { "${it.rank}${it.suit}" }}.
            - Карты на столе: ${tableCards.joinToString { "${it.rank}${it.suit}" }}.
            - Вышедшие карты (БИТА): ${cardsInGraveyard.size} шт.
            - Примерно осталось в колоде и у других игроков: $cardsRemaining карт.
            
            ЗАДАЧА:
            1. Проанализируй ситуацию.
            2. Дай ПРЕДЕЛЬНО КРАТКИЙ совет (не более 10 слов).
            3. Примеры: "Бей 10 Пик", "Ходи с 6 Бубни", "Бери карты", "Сбрось мелких".
            
            Ответь только сутью, без вежливости и вступлений, на русском языке.
        """.trimIndent()
    }
    
    fun clearMemory() {
        cardsInGraveyard.clear()
    }
}
