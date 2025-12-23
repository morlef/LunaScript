package si.f5.luna3419.script.io

import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import java.io.File

object ScriptManager {
    data class SceneFile(
        val title: String, val scenes: List<SceneData>
    )

    fun loadSceneData(file: File): SceneFile {
        if (!file.exists()) return SceneFile("", emptyList())
        val lines = file.readLines()
        val parsed = parseLines(lines, 0, lines.size)
        return SceneFile(file.nameWithoutExtension, parsed.first)
    }

    private fun parseLines(lines: List<String>, startIndex: Int, endIndex: Int): Pair<List<SceneData>, Int> {
        val scenes = mutableListOf<SceneData>()
        var i = startIndex
        var currentNum = 1

        while (i < endIndex) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("//")) {
                i++
                continue
            }

            if (line.startsWith("@endif") || line.startsWith("@else") || line.startsWith("@end_choice")) {
                return scenes to i
            }

            var scene: SceneData? = null

            if (line.startsWith("*")) {
                scene = SceneData(num = currentNum++, type = SceneType.LABEL, labelName = line.substring(1).trim())
            } else if (line.startsWith("@choice")) {
                val choiceScene = SceneData(num = currentNum++, type = SceneType.CHOICE)
                i++
                val choices = mutableListOf<Pair<String, String>>()
                while (i < endIndex) {
                    val choiceLine = lines[i].trim()
                    if (choiceLine.startsWith("@end_choice")) {
                        break
                    }
                    if (choiceLine.contains(">>")) {
                        val parts = choiceLine.split(">>", limit = 2)
                        var text = parts[0].trim()
                        if (text.startsWith("\"") && text.endsWith("\"")) {
                            text = text.substring(1, text.length - 1)
                        }
                        val label = parts[1].trim()
                        choices.add(text to label)
                    }
                    i++
                }
                choiceScene.choices = choices
                scene = choiceScene
            } else if (line.startsWith("@if")) {
                val parts = splitCommand(line)
                val command = parts[0]
                val params = if (parts.size > 1) parts.drop(1).toMutableList() else mutableListOf()

                val ifScene = SceneData(
                    num = currentNum++, type = SceneType.IF, commandName = command, commandParams = params
                )
                i++

                val trueResult = parseLines(lines, i, endIndex)
                ifScene.trueBlock = trueResult.first.toMutableList()
                i = trueResult.second

                if (i < endIndex && lines[i].trim().startsWith("@else")) {
                    i++
                    val falseResult = parseLines(lines, i, endIndex)
                    ifScene.falseBlock = falseResult.first.toMutableList()
                    i = falseResult.second
                }

                if (i < endIndex && lines[i].trim().startsWith("@endif")) {
                    i++
                }
                scene = ifScene
                i--
            } else if (line.startsWith("@")) {
                val parts = splitCommand(line)
                val command = parts[0]
                val type = SceneType.fromString(command)
                val params = if (parts.size > 1) parts.drop(1).toMutableList() else mutableListOf()

                if (type != SceneType.ELSE && type != SceneType.ENDIF && type != SceneType.END_CHOICE) {
                    scene = SceneData(
                        num = currentNum++, type = type, commandName = command, commandParams = params
                    )
                }
            } else {
                val textScene = SceneData(num = currentNum++, type = SceneType.TEXT)
                if (line.startsWith("[") && line.contains("]")) {
                    val closeBracket = line.indexOf("]")
                    textScene.charName = line.substring(1, closeBracket)
                    val content = line.substring(closeBracket + 1).trim()
                    if (content.startsWith("\"") && content.endsWith("\"")) textScene.text =
                        content.substring(1, content.length - 1)
                    else textScene.text = content
                } else {
                    textScene.text = line
                }
                scene = textScene
            }

            if (scene != null) {
                scenes.add(scene)
            }
            i++
        }
        return scenes to i
    }

    fun saveSceneData(file: File, scenes: List<SceneData>) {
        val sb = StringBuilder()
        appendScenesRecursively(sb, scenes)
        file.writeText(sb.toString())
    }

    private fun appendScenesRecursively(sb: StringBuilder, scenes: List<SceneData>) {
        for (scene in scenes) {
            when (scene.type) {
                SceneType.CHOICE -> {
                    sb.append("@choice\n")
                    for ((text, label) in scene.choices) {
                        sb.append("\"$text\" >> $label\n")
                    }
                    sb.append("@end_choice\n")
                }

                SceneType.IF -> {
                    sb.append(scene.toScriptString()).append("\n")
                    appendScenesRecursively(sb, scene.trueBlock)
                    if (scene.falseBlock.isNotEmpty()) {
                        sb.append("@else\n")
                        appendScenesRecursively(sb, scene.falseBlock)
                    }
                    sb.append("@endif\n")
                }

                else -> {
                    sb.append(scene.toScriptString()).append("\n")
                }
            }
        }
    }

    private fun splitCommand(line: String): List<String> {
        return line.split("\\s+".toRegex())
    }
}
