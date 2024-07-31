package ru.vafeen.test_telegrambot.utils

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import ru.vafeen.test_telegrambot.createReplyKeyboardColumn

fun SendMessage.offKeyboard(): SendMessage = apply {
    replyMarkup = ReplyKeyboardRemove().apply {
        removeKeyboard = true
    }

}

fun SendMessage.withKeyboard(keyboard: ReplyKeyboard? = createReplyKeyboardColumn(buttonsText = null)) =
    apply { replyMarkup = keyboard }