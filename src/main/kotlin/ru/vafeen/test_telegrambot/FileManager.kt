package ru.vafeen.test_telegrambot

import java.io.File

object FileManager {
    private fun openNewFile(fileName: String): File {
        val file = File("$fileName.txt")
        file.createNewFile()
        return file
    }

}