package ru.vafeen.test_telegrambot

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

fun createReplyKeyboardColumn(buttonsText: List<String>?): ReplyKeyboardMarkup {
    val rows = buttonsText?.map { buttonText ->
        KeyboardRow().apply {
            add(buttonText)
        }
    }

    return ReplyKeyboardMarkup().apply {
        if (rows != null) keyboard = rows
        resizeKeyboard = rows != null
        oneTimeKeyboard = rows != null
    }
}