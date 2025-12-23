package si.f5.luna3419.script.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.*
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import java.io.File
import javax.sound.sampled.Clip
import si.f5.luna3419.script.player.ScriptUtils.checkCondition

class PlayerViewModel {
    var currentBgi by mutableStateOf<ImageBitmap?>(null)
    var currentBgiName by mutableStateOf("")

    var nameText by mutableStateOf("")
    var messageText by mutableStateOf("")
    var isMessageBoxVisible by mutableStateOf(false)
    var isWaitingInput by mutableStateOf(false)

    data class ObjectState(
        val name: String,
        val type: String,
        val image: ImageBitmap?,
        val x: Int = 0,
        val y: Int = 0,
        val visible: Boolean = true,
        val diffParams: String = "",
        val zIndex: Int = 0,

        val rotation: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val alpha: Float = 1f,
        val blur: Float = 0f,
        val colorFilter: Color? = null,

        val isShaking: Boolean = false,
        val shakeAmount: Float = 0f
    )

    var activeObjects = mutableStateMapOf<String, ObjectState>()
    var objectListRevision by mutableStateOf(0)

    internal var scenes: List<SceneData> = emptyList()
    internal var rootScenes: List<SceneData> = emptyList()
    internal var currentIndex = 0
    internal var rootDir: File = File(".")

    private val callStack = java.util.Stack<Pair<List<SceneData>, Int>>()

    var variables = mutableStateMapOf<String, Any>()

    internal var scope: CoroutineScope? = null

    internal var targetMessage = ""
    internal var typeWriterJob: Job? = null

    var transitionAlpha by mutableStateOf(0f)
    var shouldClose by mutableStateOf(false)

    var activeChoices by mutableStateOf<List<Pair<String, String>>>(emptyList())
    var isWaitingChoice by mutableStateOf(false)

    var screenOverlayColor by mutableStateOf(Color.Transparent)

    var currentSceneUuid by mutableStateOf<String?>(null)
    internal var letterSePool: List<Clip> = emptyList()

    internal val labelMap = mutableMapOf<String, Int>()

    fun updateTargetMessage(msg: String) {
        targetMessage = msg
    }

    fun startTypeWriter(msg: String) {
        typeWriterJob = scope?.launch {
            val visibleLength = ScriptUtils.getVisibleTextLength(msg)
            for (i in 1..visibleLength) {
                if (!isActive) break
                val rawIndex = ScriptUtils.getRawIndexForVisibleIndex(msg, i)
                messageText = msg.take(rawIndex)
                if (i % 2 == 1 && letterSePool.isNotEmpty()) {
                    SoundPlayer.playFromPool(letterSePool, 60)
                }
                delay(30)
            }
        }
    }

    fun start(allScenes: List<SceneData>, startIndex: Int, projectRoot: File) {
        println("Player Start: TotalScenes=${allScenes.size}, StartIndex=$startIndex")

        scenes = allScenes
        rootScenes = allScenes
        rootDir = projectRoot
        callStack.clear()
        variables.clear()

        labelMap.clear()
        allScenes.forEachIndexed { index, scene ->
            if (scene.type == SceneType.LABEL) {
                labelMap[scene.labelName] = index
            }
        }

        scope?.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        scope?.launch {
            val realIndex = startIndex.coerceIn(scenes.indices)
            currentIndex = realIndex

            withContext(Dispatchers.IO) {
                var resUrl = PlayerViewModel::class.java.classLoader.getResource("assets/letter_se.wav")
                if (resUrl == null) resUrl = PlayerViewModel::class.java.classLoader.getResource("assets/letter_se.ogg")

                if (resUrl != null) {
                    letterSePool = SoundPlayer.loadClipPool(resUrl, count = 20)
                } else {
                    val candidates = listOf(
                        File(rootDir, "src/main/resources/assets/letter_se.wav"),
                        File(rootDir, "src/main/resources/assets/letter_se.ogg"),
                        File(rootDir, "assets/letter_se.wav"),
                        File(rootDir, "assets/letter_se.ogg")
                    )
                    val seFile = candidates.firstOrNull { it.exists() }
                    if (seFile != null) {
                        letterSePool = SoundPlayer.loadClipPool(seFile.toURI().toURL(), count = 20)
                    } else {
                        println("Letter SE Not Found")
                    }
                }
            }

            transitionAlpha = 1f

            gameLoop()
            fadeIn()
        }
    }

    private suspend fun gameLoop() {
        while ((currentIndex < scenes.size || callStack.isNotEmpty()) && scope?.isActive == true && !shouldClose) {

            if (currentIndex >= scenes.size) {
                if (callStack.isNotEmpty()) {
                    val (parentScenes, returnIndex) = callStack.pop()
                    scenes = parentScenes
                    currentIndex = returnIndex
                    continue
                } else {
                    break
                }
            }

            val scene = scenes[currentIndex]

            if (scene.type == SceneType.LABEL) {
                currentIndex++
                continue
            }

            if (scene.type == SceneType.GOTO) {
                val label = scene.commandParams.getOrNull(0) ?: ""
                val targetIndex = labelMap[label]
                if (targetIndex != null) {
                    callStack.clear()
                    scenes = rootScenes
                    currentIndex = targetIndex
                    continue
                } else {
                    println("GOTO Target Not Found: $label")
                }
            }

            val wait = processScene(scene, instant = false)

            if (wait && (isWaitingInput || isWaitingChoice)) {
                return
            }

            if (scene.type == SceneType.IF) {
                val varName = scene.commandParams.getOrNull(0) ?: ""
                val op = scene.commandParams.getOrNull(1) ?: "=="
                val value = scene.commandParams.getOrNull(2) ?: ""
                val conditionMet = checkCondition(varName, op, value, variables)

                val block = if (conditionMet) scene.trueBlock else scene.falseBlock
                if (block.isNotEmpty()) {
                    callStack.push(scenes to currentIndex + 1)
                    scenes = block
                    currentIndex = 0
                    continue
                }
            }

            currentIndex++
        }

        if (!shouldClose) {
            cleanupAndClose()
        }
    }

    private suspend fun fadeOut() {
        val steps = 20
        for (i in 1..steps) {
            transitionAlpha = i.toFloat() / steps
            delay(16)
        }
        transitionAlpha = 1f
    }

    private suspend fun fadeIn() {
        val steps = 20
        for (i in 1..steps) {
            transitionAlpha = 1f - (i.toFloat() / steps)
            delay(16)
        }
        transitionAlpha = 0f
    }

    private suspend fun cleanupAndClose() {
        if (transitionAlpha < 1f) fadeOut()

        SoundPlayer.stop()
        activeObjects.clear()
        currentBgi = null

        delay(500)
        shouldClose = true
    }

    fun onClick() {
        if (isWaitingChoice) return

        if (isWaitingInput) {
            if (typeWriterJob?.isActive == true) {
                typeWriterJob?.cancel()
                messageText = targetMessage
                return
            }

            isWaitingInput = false
            currentIndex++
            scope?.launch { gameLoop() }
        }
    }

    fun onChoiceSelected(nextLabel: String) {
        isWaitingChoice = false
        activeChoices = emptyList()

        if (nextLabel.startsWith("*")) {
            val labelName = nextLabel.substring(1)
            val targetIndex = labelMap[labelName]
            if (targetIndex != null) {
                currentIndex = targetIndex
                callStack.clear()
                scenes = rootScenes
                scope?.launch { gameLoop() }
                return
            }
        }

        currentIndex++
        scope?.launch { gameLoop() }
    }

    private suspend fun processScene(scene: SceneData, instant: Boolean): Boolean {
        return processSceneImpl(scene, instant)
    }

    private suspend fun processSceneImpl(scene: SceneData, instant: Boolean): Boolean {
        if (!instant) {
            currentSceneUuid = scene.uuid
        }

        fun p(i: Int) = scene.commandParams.getOrNull(i) ?: ""
        fun pInt(i: Int, def: Int = 0) = p(i).toIntOrNull() ?: def

        val cmd = scene.commandName

        when (scene.type) {
            SceneType.BG -> handleBg(scene, instant, ::p)
            SceneType.TEXT -> {
                handleText(scene, instant)
                if (!instant) return true
            }

            SceneType.CHARA_IN, SceneType.CHARA_OUT, SceneType.IMAGE_IN, SceneType.IMAGE_OUT -> handleCharacter(
                scene,
                instant,
                ::p,
                ::pInt
            )

            SceneType.MOVE, SceneType.SCALE, SceneType.ROTATE, SceneType.TINT, SceneType.SHAKE_OBJ -> handleEffect(
                scene,
                instant,
                ::p,
                ::pInt
            )

            SceneType.BGM_PLAY, SceneType.BGM_STOP, SceneType.SE -> handleAudio(scene, ::p)

            SceneType.WAIT -> {
                if (cmd == "@wait_click" && !instant) {
                    isWaitingInput = true
                } else {
                    val ms = pInt(0)
                    if (ms > 0 && !instant) {
                        delay(ms.toLong())
                    }
                }
            }

            SceneType.CHOICE -> {
                if (scene.choices.isNotEmpty() && !instant) {
                    activeChoices = scene.choices
                    isWaitingChoice = true
                    return true
                }
            }

            SceneType.IF -> {
                val varName = scene.commandParams.getOrNull(0) ?: ""
                val op = scene.commandParams.getOrNull(1) ?: "=="
                val value = scene.commandParams.getOrNull(2) ?: ""
                val conditionMet = checkCondition(varName, op, value, variables)

                val block = if (conditionMet) scene.trueBlock else scene.falseBlock
                if (block.isNotEmpty()) {
                    callStack.push(scenes to currentIndex + 1)
                    scenes = block
                    currentIndex = 0
                }
            }

            SceneType.SET -> {
                val varName = p(0)
                val op = p(1)
                val valueStr = p(2)

                if (varName.startsWith("$")) {
                    when (op) {
                        "=" -> variables[varName] = valueStr
                        "+=" -> {
                            val curr = variables[varName]?.toString()?.toDoubleOrNull()
                            val add = valueStr.toDoubleOrNull()
                            if (curr != null && add != null) {
                                val res = curr + add
                                variables[varName] = if (res % 1.0 == 0.0) res.toInt().toString() else res.toString()
                            } else {
                                val cStr = variables[varName]?.toString() ?: ""
                                variables[varName] = cStr + valueStr
                            }
                        }

                        "-=" -> {
                            val curr = variables[varName]?.toString()?.toDoubleOrNull()
                            val sub = valueStr.toDoubleOrNull()
                            if (curr != null && sub != null) {
                                val res = curr - sub
                                variables[varName] = if (res % 1.0 == 0.0) res.toInt().toString() else res.toString()
                            }
                        }
                    }
                }
            }

            else -> {}
        }
        return false
    }

    fun dispose() {
        shouldClose = true
        scope?.cancel()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                SoundPlayer.stop()
                letterSePool.forEach { it.close() }

                currentBgi = null
                activeObjects.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
