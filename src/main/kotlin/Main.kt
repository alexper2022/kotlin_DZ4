import java.io.File
import java.util.*
import kotlin.system.exitProcess

const val HELP_MESSAGE: String = """Перечень команд:
        
exit - прекращение работы

help - справка

add <Имя> phone <Номер телефона>
- сохранение записи с введенными именем и номером телефона
  или добавление нового номера телефона к уже имеющейся
  записи
 
add <Имя> email <Адрес электронной почты>
- сохранение записи с введенными именем и адрес
  электронной почты или добавление нового адреса
  электронной почты к уже имеющейся записи

show <Имя>
- выводит по введенному имени его телефоны
  и адреса электронной почты

find <параметр>
- выводит по введенному параметру (телефон или E-mail) имеющиеся записи

export </path/file.json>
- экспорт значений в JSON файл в директории </path/<file.json>"""
const val COMMON_ERROR_MESSAGE: String = "Ошибка! Команда введена неверно. Список команд ниже"

var phoneBook = mutableMapOf<String, Person>()


sealed interface Command {
    fun execute()
    fun isValid(): Boolean
}

data object ExitCommand : Command {

    override fun execute() {
        exitProcess(0)
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return """Введена команда "exit""""
    }
}

data object HelpCommand : Command {

    override fun execute() {
        println(HELP_MESSAGE)
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return "Вывод справочной информации"
    }
}

class AddUserPhoneCommand(private val entryData: List<String>) : Command {

    private val phonePattern = Regex("[+]+\\d+")
    private val entryPhone = entryData[entryData.indexOf("phone") + 1]

    override fun execute() {
        if (phoneBook.containsKey(entryData[0])) {
            phoneBook[entryData[0]]?.contacts?.get("phone")?.add(entryPhone)
        } else {
            val person = Person(
                entryData[0],
                contacts = mutableMapOf(Pair("phone", mutableListOf(entryPhone)), Pair("email", mutableListOf()))
            )
            phoneBook[entryData[0]] = person
        }
    }

    override fun isValid(): Boolean {
        return entryPhone.matches(phonePattern) && entryData.size <= 3
    }

    override fun toString(): String {
        return "Введена команда записи нового пользователя ${entryData[0]} с номером телефона $entryPhone"
    }
}

class AddUserEmailCommand(private val entryData: List<String>) : Command {

    private val emailPattern = Regex("[a-zA-z0-9]+@[a-zA-z0-9]+[.]([a-zA-z0-9]{2,4})")
    private val entryEmail = entryData[entryData.indexOf("email") + 1]

    override fun execute() {
        if (phoneBook.containsKey(entryData[0])) {
            phoneBook[entryData[0]]?.contacts?.get("email")?.add(entryEmail)
        } else {
            val person = Person(
                entryData[0],
                contacts = mutableMapOf(Pair("phone", mutableListOf()), Pair("email", mutableListOf(entryEmail)))
            )
            phoneBook[entryData[0]] = person
        }
    }

    override fun isValid(): Boolean {
        return entryEmail.matches(emailPattern) && entryData.size <= 3
    }

    override fun toString(): String {
        return "Введена команда записи нового пользователя ${entryData[0]} с адресом электронной почты $entryEmail"
    }
}

class ShowCommand(private val name: String) : Command {
    override fun execute() {
        if (phoneBook.isEmpty()) {
            println("Phonebook is not initialized")
        } else if (phoneBook.containsKey(name)) {
            println(phoneBook[name])
        } else {
            println("Person with name $name was not found")
        }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return """Введена команда "show""""
    }

}

data class Person(
    var name: String,
    var contacts: MutableMap<String, MutableList<String>> = mutableMapOf(
        "phone" to mutableListOf(),
        "email" to mutableListOf()
    )
) {
    override fun toString(): String {
        return buildString {
            append("Пользователь: ")
            append(name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            append(buildString {
                if (contacts["phone"]?.isNotEmpty() == true) {
                    append("\n\t")
                    append("phone(s): ")
                    append(
                        contacts["phone"].toString()
                            .replace("[", "")
                            .replace("]", "")
                    )
                }
            })
            append(buildString {
                if (contacts["email"]?.isNotEmpty() == true) {
                    append("\n\t")
                    append("email(s): ")
                    append(
                        contacts["email"].toString()
                            .replace("[", "")
                            .replace("]", "")
                    )
                }
            })
            append("\n")
        }
    }
}

class FindCommand(private val value: String) : Command {
    override fun execute() {
        val persons = mutableListOf<Person>()
        if (phoneBook.isEmpty()) {
            println("Phonebook is not initialized")
        } else {
            for (person in phoneBook.values) {
                if (person.contacts["phone"]!!.contains(value) or person.contacts["email"]!!.contains(value)) {
                    persons.add(person)
                }
            }
        }
        if (persons.isEmpty()) {
            println("Person with $value was not found")
        } else {
            persons.forEach { person ->
                println(person)
            }
        }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return """Введена команда "find""""
    }

}

class ExportCommand(private val path: String) : Command {
    override fun execute() {
        if (phoneBook.isEmpty()) {
            println("Phonebook is not initialized")
        } else {

            val personsJson = phoneBook.values.map { person ->
                json {
                    addGroup("name", person.name)

                    person.contacts["phone"]?.let { addGroup("phone", it) }
                    person.contacts["email"]?.let { addGroup("email", it) }
                }
            }
            val jsonFile = "[${personsJson.joinToString(", ")}]"
            File(path).writeText(jsonFile)
            println("JSON file $path was created")
        }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return """Введена команда "export""""
    }
}

class MyJson {
    private val obj = mutableMapOf<String, Any>()

    fun addGroup(key: String, value: Any) {
        obj[key] = value
    }

    override fun toString(): String {
        val result = obj.entries.joinToString(",\n    ") { (key, value) ->
            if (value is String) {
                "\"$key\": \"$value\""
            } else if (value is List<*> && value.size >= 1) {
                "\"$key\": [\n  ${value.joinToString(",\n  ") { data -> "${if (data is String) "    \"$data\"" else data}" }}\n    ]"
            } else if (value is List<*>) {
                "\"$key\": []"
            } else {
                "\"$key\": $value"
            }
        }
        return "\n  {\n    $result\n  }\n"
    }
}

fun json(init: MyJson.() -> Unit): MyJson {
    return MyJson().apply(init)
}


fun readCommand(): Command {
    val entryData: List<String> = readln().lowercase().split(' ')

    return when (entryData[0]) {
        "add" -> {
            if (entryData.size > 3 && "phone" in entryData && "email" !in entryData) {
                AddUserPhoneCommand(entryData.subList(1, entryData.size))
            } else if (entryData.size > 3 && "phone" !in entryData && "email" in entryData) {
                AddUserEmailCommand(entryData.subList(1, entryData.size))
            } else {
                println(COMMON_ERROR_MESSAGE)
                HelpCommand
            }
        }

        "show" -> {
            if (entryData.size > 1) {
                ShowCommand(entryData[1])
            } else {
                println(COMMON_ERROR_MESSAGE)
                HelpCommand
            }
        }

        "find" -> {
            if (entryData.size > 1) {
                FindCommand(entryData[1])
            } else {
                println(COMMON_ERROR_MESSAGE)
                HelpCommand
            }
        }

        "export" -> {
            if (entryData.size > 1) {
                ExportCommand(entryData[1])
            } else {
                println(COMMON_ERROR_MESSAGE)
                HelpCommand
            }
        }

        "help" -> HelpCommand
        "exit" -> ExitCommand
        else -> {
            println(COMMON_ERROR_MESSAGE)
            return HelpCommand
        }
    }
}


fun main() {


    while (true) {
        print("\nВведите \"help\" для помощи\nили команду: ")
        val command: Command = readCommand()
        if (command.isValid()) {
            println(command)
            command.execute()
        } else {
            println(COMMON_ERROR_MESSAGE)
            println(HELP_MESSAGE)
        }
    }

}
