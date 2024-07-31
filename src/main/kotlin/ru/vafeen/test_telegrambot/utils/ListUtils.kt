package ru.vafeen.test_telegrambot.utils

import ru.vafeen.test_telegrambot.User
import ru.vafeen.test_telegrambot.main.numberOfStudents

fun <T> Collection<User?>.mapProcessNotNull(lambda: (User) -> T): List<T> {
    val result = mutableListOf<T>()
    for (i in this) if (i != null) result.add(lambda(i))
    return result
}

fun placesStringMessage(usersList: MutableList<User?>): String {
    var resultText = ""

    usersList.forEachIndexed { index, user ->
        resultText += if (user != null) "\n$user"
        else "\n$index. Free"
    }

    return resultText
}

fun List<Int>.freePlacesByNumberOfStudents(): List<Int> {
    val result = mutableListOf<Int>()
    for (index in 0..<numberOfStudents) {
        if (index !in this)
            result.add(index)
    }
    return result
}
