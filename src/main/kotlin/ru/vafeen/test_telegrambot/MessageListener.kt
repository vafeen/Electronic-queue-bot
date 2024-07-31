package ru.vafeen.test_telegrambot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import ru.vafeen.test_telegrambot.bot_info.Commands
import ru.vafeen.test_telegrambot.main.numberOfStudents

fun List<Int>.freePlacesByNumberOfStudents(): List<Int> {
    val sorted = this.sorted()
    val result = mutableListOf<Int>()
    for (index in 0..<numberOfStudents) {
        if (index !in this)
            result.add(index)
    }
    return result
}

fun Message.getUsername(): String = this.from.userName

fun placesStringMessage(usersList: MutableList<User?>): String {
    var resultText = ""

    usersList.forEachIndexed { index, user ->
        resultText += if (user != null) "\n$user"
        else "\n$index. Free"
    }

    return resultText
}


fun <T> Collection<User?>.mapNotNull(lambda: (User) -> T): List<T> {
    val result = mutableListOf<T>()
    for (i in this) {
        if (i != null) {
            result.add(lambda(i))
        }
    }
    return result
}

private var usersList: MutableList<User?> = mutableListOf<User?>().let {
    val result = it
    for (i in 1..numberOfStudents) {
        result.add(null)
    }
    result
}

class MessageListener {

    private fun SendMessage.offKeyboard(): SendMessage = apply {
        replyMarkup = ReplyKeyboardRemove().apply {
            removeKeyboard = true
        }

    }

    private fun SendMessage.withKeyboard(keyboard: ReplyKeyboard? = createReplyKeyboardColumn(buttonsText = null)) =
        apply { replyMarkup = keyboard }


    fun processMessage(message: Message, sendMessage: (SendMessage) -> Unit) {
        val chatID = message.chatId.toString()

        val messageForSend: SendMessage = when (message.text) {
            Commands.START -> {
                val resultText =
                    "Choose a place:\n" + placesStringMessage(usersList = usersList)
                val buttonsList = (1..numberOfStudents).toList().let { numberOfStudentsL ->
                    if (usersList.isNotEmpty())
                        usersList.mapNotNull {
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
                    (usersList.mapNotNull { it.username }).count {
                        it == message.getUsername()
                    } == 0 && id < usersList.size && usersList[id] == null -> {
                        usersList[id] = (User(id = id, username = message.getUsername()))
                        SendMessage(chatID, placesStringMessage(usersList = usersList)).offKeyboard()
                    }

                    (usersList.mapNotNull { it.username }).count {
                        it == message.getUsername()
                    } == 0 && id < usersList.size && usersList[id] != null -> {
                        SendMessage(chatID, "The place is taken")
                    }

                    (usersList.mapNotNull { it.username }).count {
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