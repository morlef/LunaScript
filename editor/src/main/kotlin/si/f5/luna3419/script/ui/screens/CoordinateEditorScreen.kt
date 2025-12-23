package si.f5.luna3419.script.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import si.f5.luna3419.script.player.AssetLoader
import androidx.compose.ui.draw.clipToBounds
import si.f5.luna3419.script.data.SceneType
import si.f5.luna3419.script.ui.EditorViewModel
import java.io.File

@Composable
fun CoordinateEditorScreen(viewModel: EditorViewModel, targetUuid: String) {
    val targetGroup = viewModel.groups.find { g -> g.blocks.any { it.uuid == targetUuid } }
    val targetBlock = targetGroup?.blocks?.find { it.uuid == targetUuid }

    if (targetBlock == null) {
        return
    }

    val currentBgName = remember(targetUuid, viewModel.groups.size) {
        var foundBg = ""
        run loop@{
            viewModel.groups.forEach { group ->
                for (block in group.blocks) {
                    if (block.uuid == targetUuid) return@loop
                    if (block.type == SceneType.BG) {
                        foundBg = block.commandParams.getOrNull(0) ?: ""
                    }
                }
            }
        }
        foundBg
    }

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredSize(1152.dp, 648.dp)
                .clipToBounds()
                .background(Color.Black)
        ) {

            if (currentBgName.isNotEmpty() && currentBgName != "_") {
                val root = viewModel.currentFile?.parentFile?.parentFile ?: File(".")
                val url = AssetLoader.findAssetUrl(root, "bg", currentBgName)
                val bmp = remember(currentBgName) { if (url != null) AssetLoader.loadImage(url) else null }
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            val objName = targetBlock.commandParams.getOrNull(0) ?: ""

            var xIndex = -1
            var yIndex = -1
            var isCharacter = false

            val type = targetBlock.type

            if (type == SceneType.CHARA_IN) {
                xIndex = 2
                yIndex = 3
                isCharacter = true
            } else if (type == SceneType.IMAGE_IN) {
                xIndex = 1
                yIndex = 2
            } else if (targetBlock.type == SceneType.MOVE) {
                xIndex = 1
                yIndex = 2
            }

            if (objName.isNotEmpty() && xIndex != -1) {
                val root = viewModel.currentFile?.parentFile?.parentFile ?: File(".")

                var assetName = objName
                if (isCharacter) {
                    val face = targetBlock.commandParams.getOrNull(1) ?: ""
                    if (face.isNotEmpty()) {
                        assetName = "${objName}_${face}"
                    }
                }

                var url: java.net.URL? = null
                if (isCharacter) {
                    val face = targetBlock.commandParams.getOrNull(1) ?: ""
                    url = AssetLoader.findAssetUrl(root, "characters", assetName)
                    if (url == null) url = AssetLoader.findAssetUrl(root, "characters", face)
                    if (url == null) url = AssetLoader.findAssetUrl(root, "characters", objName)
                } else {
                    url = AssetLoader.findAssetUrl(root, "characters", assetName)
                    if (url == null) url = AssetLoader.findAssetUrl(root, "image", assetName)
                }

                val currentBlockState = rememberUpdatedState(targetBlock)
                val bmp = remember(assetName) { if (url != null) AssetLoader.loadImage(url) else null }

                if (bmp != null) {
                    val widthDp = bmp.width.dp
                    val heightDp = bmp.height.dp

                    val xVal = targetBlock.commandParams.getOrNull(xIndex)?.toIntOrNull() ?: 0
                    val yVal = targetBlock.commandParams.getOrNull(yIndex)?.toIntOrNull() ?: 0

                    val xOffset = xVal.dp - (widthDp / 2f)
                    val yOffset = yVal.dp - (heightDp / 2f)

                    Image(
                        bitmap = bmp,
                        contentDescription = objName,
                        contentScale = ContentScale.None,
                        alignment = Alignment.TopStart,
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .requiredSize(width = widthDp, height = heightDp)
                            .pointerInput(density) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()

                                    val dx = dragAmount.x / density.density
                                    val dy = dragAmount.y / density.density

                                    val block = currentBlockState.value
                                    val curX = block.commandParams.getOrNull(xIndex)?.toIntOrNull() ?: 0
                                    val curY = block.commandParams.getOrNull(yIndex)?.toIntOrNull() ?: 0

                                    val newX = curX + dx.toInt()
                                    val newY = curY + dy.toInt()
                                    viewModel.updateBlockCoordinates(targetUuid, newX, newY)
                                }
                            }
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                with(density) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(36.dp.toPx(), 398.dp.toPx()),
                        size = Size(1080.dp.toPx(), 200.dp.toPx())
                    )
                    drawRect(
                        color = Color(0xFF2196F3).copy(alpha = 0.2f),
                        topLeft = Offset(216.dp.toPx(), 398.dp.toPx()),
                        size = Size(720.dp.toPx(), 40.dp.toPx())
                    )
                    drawRect(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        topLeft = Offset(216.dp.toPx(), 443.dp.toPx()),
                        size = Size(720.dp.toPx(), 120.dp.toPx())
                    )
                }
            }
        }
    }
}
