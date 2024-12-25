package ru.vafeen.test_telegrambot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.*

data class User(
    val id: Long,
    val username: String
)

enum class UserState {
    NONE,
    CHOOSING_PLACE,
    CHOOSING_SWAP_PLACE,
    WAITING_FOR_CONFIRMATION
}

data class UserSession(
    val username: String,
    var state: UserState = UserState.NONE,
    var pendingSwapWith: Int? = null,
    var hasRespondedToSwap: Boolean = false
)

fun createReplyKeyboard(buttons: List<String>): ReplyKeyboardMarkup {
    val rows = buttons.map { KeyboardRow().apply { add(it) } }
    return ReplyKeyboardMarkup().apply {
        keyboard = rows
        resizeKeyboard = true
        oneTimeKeyboard = false
    }
}

fun Message.getUsername(): String = this.from.userName.orEmpty()

fun SendMessage.withKeyboard(keyboard: ReplyKeyboard? = null): SendMessage =
    apply { replyMarkup = keyboard }

class MessageListener {
    private val usersList = Collections.synchronizedList(MutableList(10) { null as User? }) // 10 мест
    private val userSessions = mutableMapOf<String, UserSession>()

    fun processMessage(
        message: Message,
        sendMessage: (SendMessage) -> Unit
    ) {
        val chatId = message.chatId.toString()
        val username = message.getUsername()
        val session = userSessions.getOrPut(username) { UserSession(username) }
        val currentUserPlace = usersList.indexOfFirst { it?.username == username }

        val responseMessage: SendMessage = when {
            message.text == "/start" -> {
                SendMessage(chatId, "Добро пожаловать! Выберите действие:")
                    .withKeyboard(
                        createReplyKeyboard(
                            listOf(
                                "Выбрать место",
                                "Список мест",
                                "Освободить место",
                                "Поменяться местами"
                            )
                        )
                    )
            }

            message.text == "Назад" -> {
                session.state = UserState.NONE
                SendMessage(chatId, "Выберите действие:")
                    .withKeyboard(
                        createReplyKeyboard(
                            listOf(
                                "Выбрать место",
                                "Список мест",
                                "Освободить место",
                                "Поменяться местами"
                            )
                        )
                    )
            }

            message.text == "Выбрать место" -> {
                if (currentUserPlace >= 0) {
                    SendMessage(chatId, "Вы уже заняли место №$currentUserPlace. Освободите его перед выбором нового.")
                        .withKeyboard(createReplyKeyboard(listOf("Назад")))
                } else {
                    val freePlaces = usersList.mapIndexedNotNull { index, user ->
                        if (user == null) index.toString() else null
                    }
                    if (freePlaces.isEmpty()) {
                        SendMessage(chatId, "Нет свободных мест.")
                            .withKeyboard(createReplyKeyboard(listOf("Назад")))
                    } else {
                        session.state = UserState.CHOOSING_PLACE
                        SendMessage(chatId, "Выберите свободное место:\n${freePlaces.joinToString(", ")}")
                            .withKeyboard(createReplyKeyboard(freePlaces + "Назад"))
                    }
                }
            }

            session.state == UserState.CHOOSING_PLACE -> {
                val chosenPlace = message.text.toIntOrNull()
                if (chosenPlace != null && chosenPlace in usersList.indices && usersList[chosenPlace] == null) {
                    usersList[chosenPlace] = User(message.from.id, username)
                    session.state = UserState.NONE
                    SendMessage(chatId, "Вы выбрали место №$chosenPlace.")
                        .withKeyboard(
                            createReplyKeyboard(
                                listOf(
                                    "Выбрать место",
                                    "Список мест",
                                    "Освободить место",
                                    "Поменяться местами"
                                )
                            )
                        )
                } else {
                    SendMessage(chatId, "Место недоступно. Попробуйте снова.")
                        .withKeyboard(createReplyKeyboard(listOf("Назад")))
                }
            }

            message.text == "Список мест" -> {
                val places = usersList.mapIndexed { index, user ->
                    "Место $index: ${user?.username?.let { "@$it" } ?: "Свободно"}"
                }.joinToString("\n")
                SendMessage(chatId, "Список мест:\n$places")
                    .withKeyboard(createReplyKeyboard(listOf("Назад")))
            }

            message.text == "Освободить место" -> {
                if (currentUserPlace >= 0) {
                    usersList[currentUserPlace] = null
                    SendMessage(chatId, "Вы освободили место №$currentUserPlace.")
                        .withKeyboard(createReplyKeyboard(listOf("Выбрать место", "Список мест", "Поменяться местами")))
                } else {
                    SendMessage(chatId, "Вы не занимаете никакое место.")
                        .withKeyboard(createReplyKeyboard(listOf("Назад")))
                }
            }

            message.text == "Поменяться местами" -> {
                val occupiedPlaces = usersList.mapIndexedNotNull { index, user ->
                    if (user != null && user.username != username) index.toString() else null
                }
                if (occupiedPlaces.isEmpty()) {
                    SendMessage(chatId, "Нет доступных пользователей для обмена.")
                        .withKeyboard(createReplyKeyboard(listOf("Назад")))
                } else {
                    session.state = UserState.CHOOSING_SWAP_PLACE
                    SendMessage(chatId, "Выберите место для обмена:\n${occupiedPlaces.joinToString(", ")}")
                        .withKeyboard(createReplyKeyboard(occupiedPlaces + "Назад"))
                }
            }

            session.state == UserState.CHOOSING_SWAP_PLACE -> {
                val targetPlace = message.text.toIntOrNull()
                if (targetPlace != null && targetPlace in usersList.indices && usersList[targetPlace] != null) {
                    val targetUser = usersList[targetPlace]
                    session.state = UserState.WAITING_FOR_CONFIRMATION
                    session.pendingSwapWith = targetPlace

                    sendMessage(
                        SendMessage(
                            targetUser!!.id.toString(),
                            "Пользователь @$username предлагает вам поменяться местами. Подтвердите обмен."
                        ).withKeyboard(createReplyKeyboard(listOf("Да", "Нет")))
                    )

                    SendMessage(chatId, "Запрос на обмен отправлен пользователю @${targetUser.username}.")
                        .withKeyboard(createReplyKeyboard(listOf("Назад")))
                } else {
                    SendMessage(chatId, "Неверный ввод. Попробуйте снова.")
                        .withKeyboard(createReplyKeyboard(listOf("Назад")))
                }
            }

            message.text == "Да" && session.state == UserState.WAITING_FOR_CONFIRMATION -> {
                if (!session.hasRespondedToSwap) {
                    session.hasRespondedToSwap = true

                    val targetPlace = session.pendingSwapWith
                    if (targetPlace != null) {
                        val targetUser = usersList[targetPlace]
                        if (currentUserPlace >= 0 && targetUser != null) {
                            usersList[currentUserPlace] = targetUser
                            usersList[targetPlace] = User(message.from.id, username)

                            session.state = UserState.NONE
                            session.pendingSwapWith = null

                            sendMessage(
                                SendMessage(
                                    targetUser.id.toString(),
                                    "Вы успешно поменялись местами с пользователем @$username."
                                )
                            )

                            SendMessage(chatId, "Вы успешно поменялись местами с @${targetUser.username}.")
                                .withKeyboard(
                                    createReplyKeyboard(
                                        listOf(
                                            "Выбрать место",
                                            "Список мест",
                                            "Освободить место",
                                            "Поменяться местами"
                                        )
                                    )
                                )
                        } else {
                            SendMessage(chatId, "Произошла ошибка. Повторите попытку.")
                                .withKeyboard(createReplyKeyboard(listOf("Назад")))
                        }
                    } else {
                        SendMessage(chatId, "Нет активного запроса на обмен.")
                            .withKeyboard(createReplyKeyboard(listOf("Назад")))
                    }
                } else {
                    SendMessage(chatId, "Хватит жмякать!")
                        .withKeyboard(createReplyKeyboard(listOf("Назад")))
                }
            }

            message.text == "Нет" && session.state == UserState.WAITING_FOR_CONFIRMATION -> {
                if (!session.hasRespondedToSwap) {
                    session.hasRespondedToSwap = true

                    val targetPlace = session.pendingSwapWith
                    if (targetPlace != null) {
                        val targetUser = usersList[targetPlace]
                        if (targetUser != null) {
                            session.state = UserState.NONE
                            session.pendingSwapWith = null

                            sendMessage(
                                SendMessage(
                                    targetUser.id.toString(),
                                    "Пользователь @$username отклонил запрос на обмен местами."
                                )
                            )

                            SendMessage(chatId, "Вы отклонили запрос на обмен местами.")
                                .withKeyboard(
                                    createReplyKeyboard(
                                        listOf(
                                            "Выбрать место",
                                            "Список мест",
                                            "Освободить место",
                                            "Поменяться местами"
                                        )
                                    )
                                )
                        } else {
                            SendMessage(chatId, "Произошла ошибка. Повторите попытку.")
                                .withKeyboard(createReplyKeyboard(listOf("Назад")))
                        }
                    } else {
                        SendMessage(chatId, "Нет активного запроса на обмен.")
                            .withKeyboard(createReplyKeyboard(listOf("Назад")))
                    }
                } else {
                    SendMessage(chatId, "Хватит жмякать!")
                        .withKeyboard(createReplyKeyboard(listOf("Назад")))
                }
            }

            else -> SendMessage(chatId, "Неверный ввод. Попробуйте снова.")
                .withKeyboard(createReplyKeyboard(listOf("Назад")))
        }

        sendMessage(responseMessage)
    }

    fun removeKeyboardFromMessage(chatId: String, messageId: Int, sendMessage: (EditMessageReplyMarkup) -> Unit) {
        try {
            val editMessage = EditMessageReplyMarkup()
            editMessage.chatId = chatId
            editMessage.messageId = messageId
            editMessage.replyMarkup = null
            sendMessage(editMessage)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}
