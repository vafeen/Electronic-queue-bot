package ru.vafeen.test_telegrambot

//data class Subject(
//    val name: String,
//    val users: List<User>
//) {
//    override fun toString(): String = "$name\n" +
//            users.joinToString { user ->
//                "$user\n"
//            }
//}
//
//fun String.toSubject(): Subject? {
//    val list = this.split("\n")
//    return try {
//        Subject(name = list[0], users = list.subList(1, list.size).map { str ->
//            val strList = str.split(". @")
//            User(
//                strList[0].toInt(),
//                strList[1]
//            )
//        })
//    } catch (e: Exception) {
//        null
//    }
//}