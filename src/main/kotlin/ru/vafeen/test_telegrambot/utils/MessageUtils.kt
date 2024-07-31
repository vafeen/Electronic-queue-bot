package ru.vafeen.test_telegrambot.utils

import org.telegram.telegrambots.meta.api.objects.Message

fun Message.getUsername(): String = this.from.userName
