package si.f5.luna3419.script.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

object ScriptUtils {
    data class TimeParams(val ms: Int, val easing: String)

    fun parseTime(str: String): TimeParams {
        val parts = str.split("@")
        val time = parts.firstOrNull()?.toIntOrNull() ?: 0
        val easing = parts.getOrNull(1) ?: "linear"
        return TimeParams(time, easing)
    }

    fun getEasingValue(progress: Float, type: String): Float {
        return when (type) {
            "ease-in" -> progress * progress
            "ease-out" -> progress * (2 - progress)
            "ease-in-out" -> if (progress < 0.5f) 2 * progress * progress else -1 + (4 - 2 * progress) * progress
            else -> progress
        }
    }

    fun parseHexColor(hexInput: String): Color? {
        if (!hexInput.startsWith("#")) return null
        return try {
            val hex = hexInput.removePrefix("#")
            when (hex.length) {
                6 -> {
                    val rgb = hex.toLong(16)
                    Color(0xFF000000 or rgb)
                }
                8 -> {
                    val argb = hex.toLong(16)
                    Color(argb)
                }
                else -> {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun checkCondition(varName: String, op: String, targetVal: String, variables: Map<String, Any>): Boolean {
        val currentRaw = if (varName.startsWith("$")) variables[varName] else varName
        val currentStr = currentRaw?.toString() ?: ""

        val currentNum = currentStr.toDoubleOrNull()
        val targetNum = targetVal.toDoubleOrNull()

        if (currentNum != null && targetNum != null) {
            return when (op) {
                "==" -> currentNum == targetNum
                "!=" -> currentNum != targetNum
                ">" -> currentNum > targetNum
                "<" -> currentNum < targetNum
                ">=" -> currentNum >= targetNum
                "<=" -> currentNum <= targetNum
                else -> false
            }
        }

        return when (op) {
            "==" -> currentStr == targetVal
            "!=" -> currentStr != targetVal
            else -> false
        }
    }

    private fun isSupportedTag(tagName: String): Boolean {
        return when (tagName) {
            "b", "i", "u", "s", "color", "size" -> true
            else -> false
        }
    }

    fun parseRichText(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            val regex = "<(/?)(\\w+)(?:=([^>]+))?>".toRegex()
            val matches = regex.findAll(text)

            val tagStack = java.util.ArrayDeque<String>()

            for (match in matches) {
                if (match.range.first > currentIndex) {
                    append(text.substring(currentIndex, match.range.first))
                }

                val isClose = match.groupValues[1] == "/"
                val tagName = match.groupValues[2].lowercase()
                val param = match.groupValues[3]

                if (isSupportedTag(tagName)) {
                    if (isClose) {
                        if (tagStack.isNotEmpty() && tagStack.peekFirst() == tagName) {
                            try {
                                pop()
                                tagStack.removeFirst()
                            } catch (_: Exception) {
                            }
                        } else {
                            append(match.value)
                        }
                    } else {
                        val style = when (tagName) {
                            "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                            "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                            "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
                            "s" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                            "color" -> {
                                val color = parseHexColor(param) ?: Color.Unspecified
                                SpanStyle(color = color)
                            }

                            "size" -> {
                                val size = param.toFloatOrNull()
                                if (size != null) {
                                    SpanStyle(fontSize = size.sp)
                                } else {
                                    null
                                }
                            }

                            else -> null
                        }

                        if (style != null) {
                            pushStyle(style)
                            tagStack.push(tagName)
                        } else {
                            append(match.value)
                        }
                    }
                } else {
                    append(match.value)
                }

                currentIndex = match.range.last + 1
            }

            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }

    fun getVisibleTextLength(text: String): Int {
        val regex = "<(/?)(\\w+)(?:=([^>]+))?>".toRegex()
        val matches = regex.findAll(text)
        var length = 0
        var currentIndex = 0
        val tagStack = java.util.ArrayDeque<String>()

        for (match in matches) {
            length += match.range.first - currentIndex

            val isClose = match.groupValues[1] == "/"
            val tagName = match.groupValues[2].lowercase()

            var treatedAsTag = false
            if (isSupportedTag(tagName)) {
                if (isClose) {
                    if (tagStack.isNotEmpty() && tagStack.peekFirst() == tagName) {
                        tagStack.removeFirst()
                        treatedAsTag = true
                    }
                } else {
                    tagStack.push(tagName)
                    treatedAsTag = true
                }
            }

            if (!treatedAsTag) {
                length += match.value.length
            }

            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            length += text.length - currentIndex
        }
        return length
    }

    fun getRawIndexForVisibleIndex(text: String, visibleIndex: Int): Int {
        val regex = "<(/?)(\\w+)(?:=([^>]+))?>".toRegex()
        val matches = regex.findAll(text)
        var currentVisible = 0
        var currentIndex = 0
        val tagStack = java.util.ArrayDeque<String>()

        for (match in matches) {
            val textBefore = match.range.first - currentIndex
            if (currentVisible + textBefore >= visibleIndex) {
                return currentIndex + (visibleIndex - currentVisible)
            }
            currentVisible += textBefore

            val isClose = match.groupValues[1] == "/"
            val tagName = match.groupValues[2].lowercase()

            var treatedAsTag = false
            if (isSupportedTag(tagName)) {
                if (isClose) {
                    if (tagStack.isNotEmpty() && tagStack.peekFirst() == tagName) {
                        tagStack.removeFirst()
                        treatedAsTag = true
                    }
                } else {
                    tagStack.push(tagName)
                    treatedAsTag = true
                }
            }

            if (!treatedAsTag) {
                val tagLen = match.value.length
                if (currentVisible + tagLen >= visibleIndex) {
                    return match.range.first + (visibleIndex - currentVisible)
                }
                currentVisible += tagLen
            }

            currentIndex = match.range.last + 1
        }

        if (currentVisible + (text.length - currentIndex) >= visibleIndex) {
            return currentIndex + (visibleIndex - currentVisible)
        }

        return text.length
    }
}
