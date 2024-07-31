package ru.vafeen.test_telegrambot

class User(
    val id: Int,
    val username: String
) {
    override fun toString(): String {
        return "${id}. @${username}"
    }
}

