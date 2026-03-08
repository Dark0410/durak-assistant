package com.durak.assistant.logic

class DurakEngine {
    private val cardsInGraveyard = mutableSetOf<GameCard>()
    private val myCards = mutableSetOf<GameCard>()
    private var trumpSuit: String? = null

    fun updateGameState(onTable: List<GameCard>, inHand: List<GameCard>, trump: String) {
        trumpSuit = trump
        myCards.clear()
        myCards.addAll(inHand)
        
        // Добавляем карты со стола в "биту", если они там больше не лежат (в реальной логике)
        // placeholder: логика отслеживания вышедших карт
    }

    fun generatePrompt(tableCards: List<GameCard>): String {
        return """
            Ты ИИ-ассистент в игре Дурак.
            Козырь: $trumpSuit.
            Мои карты: ${myCards.joinToString { "${it.rank}${it.suit}" }}.
            Карты на столе: ${tableCards.joinToString { "${it.rank}${it.suit}" }}.
            Вышедшие карты (бита): ${cardsInGraveyard.size} шт.
            
            Твоя задача: Посоветуй кратко лучший ход или стоит ли "брать" карты. 
            Ответь на русском, максимально лаконично.
        """.trimIndent()
    }
}
