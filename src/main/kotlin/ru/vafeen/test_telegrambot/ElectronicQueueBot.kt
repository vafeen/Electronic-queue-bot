package ru.vafeen.test_telegrambot

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import ru.vafeen.test_telegrambot.bot_info.BotInfo

class ElectronicQueueBot : TelegramLongPollingBot() {
    private val messageListener = MessageListener()

    override fun getBotUsername(): String = BotInfo.USER_NAME

    @Deprecated("Deprecated in Java")
    override fun getBotToken(): String = BotInfo.BOT_TOKEN

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            // Обрабатываем обычное сообщение
            messageListener.processMessage(update.message) { response ->
                try {
                    // Отправка сообщения
                    execute(response)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
