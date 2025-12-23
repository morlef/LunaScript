package si.f5.luna3419.script.data

enum class SceneType(val value: String) {
    TEXT("text"),
    LABEL("label"),
    BG("@bg"),
    CHARA_IN("@chara_in"),
    CHARA_OUT("@chara_out"),
    IMAGE_IN("@image_in"),
    IMAGE_OUT("@image_out"),
    BGM_PLAY("@bgm"),
    BGM_STOP("@stop_bgm"),
    SE("@se"),
    WAIT("@wait"),
    WAIT_CLICK("@wait_click"),
    SHAKE("@shake"),
    FLASH("@flash"),
    MOVE("@move"),
    SCALE("@scale"),
    ROTATE("@rotate"),
    SHAKE_OBJ("@shake_obj"),
    TINT("@tint"),
    GOTO("@goto"),
    CHOICE("@choice"),
    END_CHOICE("@end_choice"),
    SCRIPT("@script"),
    SET("@set"),
    IF("@if"),
    ELSE("@else"),
    ENDIF("@endif"),
    UNKNOWN("");

    companion object {
        fun fromString(value: String): SceneType {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}

data class SceneData(
    var id: String = "",
    var num: Int = 0,
    var type: SceneType = SceneType.TEXT,

    var charName: String = "",
    var text: String = "",
    var labelName: String = "",
    var commandParams: MutableList<String> = mutableListOf(),

    var choices: MutableList<Pair<String, String>> = mutableListOf(),

    var commandName: String = "",

    var trueBlock: MutableList<SceneData> = mutableListOf(),
    var falseBlock: MutableList<SceneData> = mutableListOf(),

    val uuid: String = java.util.UUID.randomUUID().toString()
) {
    init {
        if (commandName.isEmpty()) {
            commandName = type.value
        }
    }

    fun toScriptString(): String {
        return when (type) {
            SceneType.TEXT -> {
                if (charName.isNotBlank()) "[$charName] \"$text\""
                else text
            }

            SceneType.LABEL -> "*$labelName"
            SceneType.CHOICE -> "@choice"
            SceneType.END_CHOICE -> "@end_choice"
            else -> {
                val cmd = commandName.ifEmpty { type.value }
                if (cmd.startsWith("@")) {
                    "$cmd${if (commandParams.isNotEmpty()) " " + commandParams.joinToString(" ") else ""}"
                } else {
                    ""
                }
            }
        }
    }
}

