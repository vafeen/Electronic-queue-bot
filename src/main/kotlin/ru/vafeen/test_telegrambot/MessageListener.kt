package ru.vafeen.test_telegrambot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import ru.vafeen.test_telegrambot.bot_info.Commands
import ru.vafeen.test_telegrambot.main.numberOfStudents
import ru.vafeen.test_telegrambot.utils.freePlacesByNumberOfStudents
import ru.vafeen.test_telegrambot.utils.getUsername
import ru.vafeen.test_telegrambot.utils.mapProcessNotNull
import ru.vafeen.test_telegrambot.utils.offKeyboard
import ru.vafeen.test_telegrambot.utils.placesStringMessage
import ru.vafeen.test_telegrambot.utils.withKeyboard


class MessageListener {
    private var usersList: MutableList<User?> = mutableListOf<User?>().let {
        val result = it
        for (i in 1..numberOfStudents) {
            result.add(null)
        }
        result
    }




    fun processMessage(message: Message, sendMessage: (SendMessage) -> Unit) {
        val chatID = message.chatId.toString()

        val messageForSend: SendMessage = when (message.text) {
            Commands.START -> {
                val resultText =
                    "Choose a place:\n" + placesStringMessage(usersList = usersList)
                val buttonsList = (1..numberOfStudents).toList().let { numberOfStudentsL ->
                    if (usersList.isNotEmpty())
                        usersList.mapProcessNotNull {
                            it.id
                        }.freePlacesByNumberOfStudents()
                            .toMutableList()
                    else numberOfStudentsL.toMutableList()
                }.map {
                    it.toString()
                }
                SendMessage(
                    chatID,
                    resultText
                ).withKeyboard(keyboard = createReplyKeyboardColumn(buttonsText = buttonsList))
            }

            Commands.LIST -> {
                SendMessage(
                    chatID,
                    "See the queue:\n" + placesStringMessage(usersList = usersList)
                ).offKeyboard()

            }

            else -> {
                val id = message.text.toInt()
                when {
                    (usersList.mapProcessNotNull { it.username }).count {
                        it == message.getUsername()
                    } == 0 && id < usersList.size && usersList[id] == null -> {
                        usersList[id] = (User(id = id, username = message.getUsername()))
                        SendMessage(chatID, placesStringMessage(usersList = usersList)).offKeyboard()
                    }

                    (usersList.mapProcessNotNull { it.username }).count {
                        it == message.getUsername()
                    } == 0 && id < usersList.size && usersList[id] != null -> {
                        SendMessage(chatID, "The place is taken")
                    }

                    (usersList.mapProcessNotNull { it.username }).count {
                        it == message.getUsername()
                    } > 0 && id < usersList.size -> {
                        SendMessage(chatID, "Stop playing!").offKeyboard()
                    }

                    else -> {
                        SendMessage(chatID, "What do you think about buttons?")
                    }
                }
            }
        }
        sendMessage(messageForSend)
    }
}