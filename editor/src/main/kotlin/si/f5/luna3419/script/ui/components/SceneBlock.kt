package si.f5.luna3419.script.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import si.f5.luna3419.script.ui.EditorViewModel
import si.f5.luna3419.script.ui.editors.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SceneBlock(
    scene: SceneData,
    onUpdate: (SceneData) -> Unit,
    onDelete: () -> Unit,
    getAssetSuggestions: (String) -> List<String>,
    characterNames: List<String>,
    onDebug: () -> Unit,
    onEditCoordinates: () -> Unit,
    isDebugActive: Boolean = false,
    viewModel: EditorViewModel
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(isDebugActive) {
        if (isDebugActive) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Card(
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDebugActive) 8.dp else 1.dp),
        border = if (isDebugActive) BorderStroke(
            3.dp, Color(0xFFFFD700)
        ) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isDebugActive) MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = 0.9f
            ) else MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDebug, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        "Debug",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "#${scene.num}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                if (scene.type == SceneType.LABEL) {
                    OutlinedTextField(
                        value = scene.labelName,
                        onValueChange = { onUpdate(scene.copy(labelName = it)) },
                        label = { Text("Label Name") },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Text(
                            scene.type.name,
                            modifier = Modifier.clickable { expanded = true }.padding(4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            SceneType.entries.filter {
                                it != SceneType.UNKNOWN && it != SceneType.LABEL && it != SceneType.END_CHOICE && it != SceneType.ELSE && it != SceneType.ENDIF && it != SceneType.CHARA_OUT && it != SceneType.IMAGE_OUT && it != SceneType.BGM_STOP && it != SceneType.WAIT_CLICK && it != SceneType.FLASH && it != SceneType.SCALE && it != SceneType.ROTATE && it != SceneType.SHAKE_OBJ && it != SceneType.TINT
                            }.forEach { type ->
                                val displayName = when (type) {
                                    SceneType.CHARA_IN -> "CHARACTER"
                                    SceneType.IMAGE_IN -> "IMAGE"
                                    SceneType.BGM_PLAY -> "BGM"
                                    SceneType.WAIT -> "WAIT"
                                    SceneType.SHAKE -> "SCREEN EFFECT"
                                    SceneType.MOVE -> "TRANSFORM"
                                    else -> type.name
                                }
                                DropdownMenuItem(text = { Text(displayName) }, onClick = {
                                    onUpdate(scene.copy(type = type, commandName = type.value)); expanded = false
                                })
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            when (scene.type) {
                SceneType.TEXT -> TextBlockEditor(scene, onUpdate, characterNames)
                SceneType.LABEL -> {}
                SceneType.CHARA_IN, SceneType.CHARA_OUT, SceneType.IMAGE_IN, SceneType.IMAGE_OUT -> CharacterBlockEditor(
                    scene,
                    onUpdate,
                    getAssetSuggestions,
                    onEditCoordinates,
                    getCharacterIdSuggestions = { viewModel.getCharacterIdSuggestions() },
                    getCharacterFaceSuggestions = { id -> viewModel.getCharacterFaceSuggestions(id) })

                SceneType.BGM_PLAY, SceneType.BGM_STOP -> BgmBlockEditor(scene, onUpdate, getAssetSuggestions)

                SceneType.SE -> SeBlockEditor(scene, onUpdate, getAssetSuggestions)

                SceneType.BG -> BgBlockEditor(scene, onUpdate, getAssetSuggestions)

                SceneType.WAIT, SceneType.WAIT_CLICK -> WaitBlockEditor(scene, onUpdate)

                SceneType.SHAKE, SceneType.FLASH -> ScreenEffectBlockEditor(scene, onUpdate)

                SceneType.MOVE, SceneType.SCALE, SceneType.ROTATE, SceneType.SHAKE_OBJ, SceneType.TINT -> ObjectEffectBlockEditor(
                    scene, onUpdate, getAssetSuggestions, onEditCoordinates
                )

                SceneType.SET -> SetBlockEditor(scene, onUpdate)
                SceneType.IF -> {
                    IfBlockEditor(scene, onUpdate, getAssetSuggestions, onEditCoordinates, viewModel)
                }

                else -> GenericCommandEditor(scene, onUpdate)
            }
        }
    }
}
