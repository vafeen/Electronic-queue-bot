package ru.vafeen.test_telegrambot

fun <T> List<T>.minusIf(lambda: (T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    this.forEach {
        if (!lambda(it))
            result.add(it)
    }
    return result
}