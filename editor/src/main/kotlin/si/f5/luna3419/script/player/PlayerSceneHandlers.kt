package si.f5.luna3419.script.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import kotlinx.coroutines.*
import si.f5.luna3419.script.player.ScriptUtils.parseTime
import si.f5.luna3419.script.player.ScriptUtils.getEasingValue
import si.f5.luna3419.script.player.ScriptUtils.parseHexColor

fun PlayerViewModel.handleBg(scene: SceneData, instant: Boolean, p: (Int) -> String) {
    if (scene.type != SceneType.BG) return

    val fileName = p(0)
    val fadeRaw = p(1)
    val tParams = parseTime(fadeRaw)
    val fadeTime = tParams.ms

    if (fileName.isNotEmpty() && fileName != currentBgiName) {
        var nextImg: ImageBitmap? = null
        var found = false

        if (fileName == "_") {
            nextImg = null
            found = true
        } else {
            val url = AssetLoader.findAssetUrl(rootDir, "bg", fileName)
            if (url != null) {
                nextImg = AssetLoader.loadImage(url)
                found = true
            }
        }

        if (found) {
            if (!instant && fadeTime > 0) {
                scope?.launch {
                    val halfTime = fadeTime / 2
                    val steps = (halfTime / 16).coerceAtLeast(1)

                    for (i in 1..steps) {
                        if (!isActive) break
                        transitionAlpha = i.toFloat() / steps
                        delay(16)
                    }
                    currentBgiName = fileName
                    currentBgi = nextImg

                    for (i in 1..steps) {
                        if (!isActive) break
                        transitionAlpha = 1f - (i.toFloat() / steps)
                        delay(16)
                    }
                    transitionAlpha = 0f
                }
            } else {
                currentBgiName = fileName
                currentBgi = nextImg
            }
        }
    }
}

fun PlayerViewModel.handleText(scene: SceneData, instant: Boolean) {
    if (scene.type != SceneType.TEXT) return

    isMessageBoxVisible = true
    nameText = scene.charName
    updateTargetMessage(scene.text)

    if (instant) {
        messageText = scene.text
    } else {
        isWaitingInput = true
        messageText = ""
        startTypeWriter(scene.text)
    }
}

fun PlayerViewModel.handleCharacter(scene: SceneData, instant: Boolean, p: (Int) -> String, pInt: (Int) -> Int) {
    val isShow = scene.type == SceneType.CHARA_IN || scene.type == SceneType.IMAGE_IN
    if (!isShow) {
        val id = p(0)
        val fadeRaw = p(1)
        val tParams = parseTime(fadeRaw)
        val fadeTime = tParams.ms
        val ease = tParams.easing

        if (!instant && fadeTime > 0) {
            scope?.launch {
                val steps = (fadeTime / 16).coerceAtLeast(1)
                val obj = activeObjects[id]
                if (obj != null) {
                    for (i in 1..steps) {
                        if (!isActive) break
                        val progress = i.toFloat() / steps
                        val alpha = 1f - getEasingValue(progress, ease)
                        activeObjects[id] = obj.copy(alpha = alpha)
                        delay(16)
                    }
                }
                activeObjects.remove(id)
                objectListRevision++
            }
        } else {
            activeObjects.remove(id)
            objectListRevision++
        }
    } else {
        val id = p(0)
        if (id.isEmpty()) return

        val isChara = scene.type == SceneType.CHARA_IN
        val faceOrFile = if (isChara) p(1) else ""
        val xIndex = if (isChara) 2 else 1
        val yIndex = if (isChara) 3 else 2
        val fadeIndex = 4

        val x = pInt(xIndex)
        val y = pInt(yIndex)
        val fadeRaw = p(fadeIndex)
        val tParams = parseTime(fadeRaw)
        val fadeTime = tParams.ms
        val ease = tParams.easing

        val assetName = if (isChara) "${id}_${faceOrFile}" else id
        val folder = if (isChara) "characters" else "image"

        var url = AssetLoader.findAssetUrl(rootDir, folder, assetName)
        if (url == null && isChara) url = AssetLoader.findAssetUrl(rootDir, folder, faceOrFile)
        if (url == null) url = AssetLoader.findAssetUrl(rootDir, folder, id)

        val bmp = if (url != null) AssetLoader.loadImage(url) else null

        val newState = PlayerViewModel.ObjectState(
            name = id,
            type = "chara",
            image = bmp,
            x = x,
            y = y,
            visible = true,
            alpha = if (instant || fadeTime <= 0) 1f else 0f
        )
        activeObjects[id] = newState
        objectListRevision++

        if (!instant && fadeTime > 0) {
            scope?.launch {
                val steps = (fadeTime / 16).coerceAtLeast(1)
                for (i in 1..steps) {
                    if (!isActive) break
                    val progress = i.toFloat() / steps
                    val alpha = getEasingValue(progress, ease)
                    val current = activeObjects[id]
                    if (current != null) {
                        activeObjects[id] = current.copy(alpha = alpha)
                    }
                    delay(16)
                }
            }
        }
    }
}

fun PlayerViewModel.handleEffect(scene: SceneData, instant: Boolean, p: (Int) -> String, pInt: (Int) -> Int) {
    if (scene.type == SceneType.MOVE) {
        val id = p(0)
        val x = pInt(1)
        val y = pInt(2)
        val time = pInt(3)
        val easeStr = p(4)

        val obj = activeObjects[id]
        if (obj != null) {
            if (time > 0 && !instant) {
                scope?.launch {
                    val startX = obj.x
                    val startY = obj.y
                    val steps = (time / 16).coerceAtLeast(1)
                    for (i in 1..steps) {
                        if (!isActive) break
                        val progress = i.toFloat() / steps
                        val eVal = getEasingValue(progress, easeStr)

                        val cx = (startX + (x - startX) * eVal).toInt()
                        val cy = (startY + (y - startY) * eVal).toInt()
                        activeObjects[id] = obj.copy(x = cx, y = cy)
                        delay(16)
                    }
                    activeObjects[id] = obj.copy(x = x, y = y)
                }
            } else {
                activeObjects[id] = obj.copy(x = x, y = y)
            }
        }
    } else if (scene.type == SceneType.SCALE) {
        val id = p(0)
        val tx = pInt(1) / 100f
        val ty = pInt(2) / 100f
        val tParams = parseTime(p(3))

        val obj = activeObjects[id]
        if (obj != null) {
            if (tParams.ms > 0 && !instant) {
                scope?.launch {
                    val startX = obj.scaleX
                    val startY = obj.scaleY
                    val steps = (tParams.ms / 16).coerceAtLeast(1)
                    for (i in 1..steps) {
                        if (!isActive) break
                        val progress = i.toFloat() / steps
                        val ev = getEasingValue(progress, tParams.easing)
                        activeObjects[id] = obj.copy(
                            scaleX = startX + (tx - startX) * ev, scaleY = startY + (ty - startY) * ev
                        )
                        delay(16)
                    }
                    activeObjects[id] = obj.copy(scaleX = tx, scaleY = ty)
                }
            } else {
                activeObjects[id] = obj.copy(scaleX = tx, scaleY = ty)
            }
        }
    } else if (scene.type == SceneType.ROTATE) {
        val id = p(0)
        val targetAngle = p(1).toFloatOrNull() ?: 0f
        val tParams = parseTime(p(2))

        val obj = activeObjects[id]
        if (obj != null) {
            if (!instant && tParams.ms > 0) {
                scope?.launch {
                    val start = obj.rotation
                    val steps = (tParams.ms / 16).coerceAtLeast(1)
                    for (i in 1..steps) {
                        if (!isActive) break
                        val progress = i.toFloat() / steps
                        val ev = getEasingValue(progress, tParams.easing)
                        activeObjects[id] = obj.copy(rotation = start + (targetAngle - start) * ev)
                        delay(16)
                    }
                    activeObjects[id] = obj.copy(rotation = targetAngle)
                }
            } else {
                activeObjects[id] = obj.copy(rotation = targetAngle)
            }
        }
    } else if (scene.type == SceneType.TINT) {
        val id = p(0)
        val p1 = p(1)
        var targetColor: Color?

        if (p1.startsWith("#")) {
            targetColor = parseHexColor(p1)
        } else {
            val r = p1.toIntOrNull() ?: 255
            val g = p(2).toIntOrNull() ?: 255
            val b = p(3).toIntOrNull() ?: 255
            targetColor = Color(r, g, b)
        }

        val obj = activeObjects[id]
        if (obj != null) {
            activeObjects[id] = obj.copy(colorFilter = targetColor)
        }
    } else if (scene.type == SceneType.SHAKE_OBJ) {
        val id = p(0)
        val intensity = p(1).toFloatOrNull() ?: 10f
        val time = pInt(2)

        val obj = activeObjects[id]
        if (obj != null && !instant) {
            scope?.launch {
                activeObjects[id] = obj.copy(isShaking = true, shakeAmount = intensity)
                delay(time.toLong())
                val current = activeObjects[id]
                if (current != null) activeObjects[id] = current.copy(isShaking = false)
            }
        }
    }
}

fun PlayerViewModel.handleAudio(scene: SceneData, p: (Int) -> String) {
    if (scene.type == SceneType.BGM_PLAY) {
        val fileName = p(0)
        val url = AssetLoader.findAssetUrl(rootDir, "bgm", fileName)
        if (url != null) {
            CoroutineScope(Dispatchers.IO).launch {
                SoundPlayer.play(url, 100)
            }
        }
    } else if (scene.type == SceneType.BGM_STOP) {
        SoundPlayer.stop()
    } else if (scene.type == SceneType.SE) {
        val fileName = p(0)
        val url = AssetLoader.findAssetUrl(rootDir, "se", fileName)
        if (url != null) {
            CoroutineScope(Dispatchers.IO).launch {
                SoundPlayer.play(url, 100)
            }
        }
    }
}
